package org.iplantc.service.jobs.resources;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.common.representation.IplantErrorRepresentation;
import org.iplantc.service.common.representation.IplantSuccessRepresentation;
import org.iplantc.service.jobs.dao.JobQueueDao;
import org.iplantc.service.jobs.model.JobQueue;
import org.iplantc.service.jobs.model.enumerations.JobPhaseType;
import org.iplantc.service.jobs.phases.TopicMessageSender;
import org.iplantc.service.jobs.phases.queuemessages.AbstractQueueConfigMessage;
import org.iplantc.service.jobs.phases.queuemessages.ResetMaxMessagesMessage;
import org.iplantc.service.jobs.phases.queuemessages.ResetNumWorkersMessage;
import org.iplantc.service.jobs.phases.queuemessages.ResetPriorityMessage;
import org.json.JSONException;
import org.json.JSONStringer;
import org.json.JSONWriter;
import org.restlet.Context;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;

/** The resource that provides CRUD operations on job queue configuration.
 * Following Agave conventions, POST is used for both create and update of 
 * queue resources.  PUT is used for "actions" that are triggered by query
 * parameters on the URL.
 * 
 * The methods of this class currently only affect the static queue definitions
 * in the job_queues table.  For example, there are no immediate runtime
 * side-effects of changing the number of worker thread assigned to a queue or,
 * for that matter, deleting a queue.  For the changes to take effect, the 
 * jobs container must be restarted.
 * 
 * @author rcardone
 */
public class JobQueueResource
 extends AbstractJobResource
{
    /* ********************************************************************** */
    /*                                Constants                               */
    /* ********************************************************************** */
    // Tracing.
    private static final Logger _log = Logger.getLogger(JobQueueResource.class);

    /* ********************************************************************** */
    /*                                 Fields                                 */
    /* ********************************************************************** */
    // The target queue name or uuid when one is provided.
    private String _queueName;
    
    /* ********************************************************************** */
    /*                                 Enums                                  */
    /* ********************************************************************** */
    // Supported PUT actions as implemented in storeRepresentation().
    public enum Actions {
        AssignPriority,
        AssignNumWorkers,
        AssignMaxMessages
    }
    
    /* ********************************************************************** */
    /*                              Constructors                              */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* constructor:                                                           */
    /* ---------------------------------------------------------------------- */
    public JobQueueResource(Context context, Request request, Response response)
    {
        super(context, request, response);
        this.username = getAuthenticatedUsername();
        getVariants().add(new Variant(MediaType.APPLICATION_JSON));
        setModifiable(true); // allows POST, PUT and DELETE.
        
        // Get the queue name from the URL when it is present.
        _queueName = (String) request.getAttributes().get("queuename");
    }

    /* ********************************************************************** */
    /*                             Public Methods                             */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* represent:                                                             */
    /* ---------------------------------------------------------------------- */
    /** Handles GET. Returns the static configuration information for a list of
     * queues or a single queue depending the url value. 
     * 
     * Only the json variant is currently supported.
     * 
     * @param variant json variant
     */
    @Override
    public Representation represent(Variant variant) throws ResourceException
    {
        // Tracing.
        if (_log.isDebugEnabled())
            _log.debug("represent called and queue name is " + 
                       (_queueName == null ? "null" : _queueName) + ".");
        
        // Get the current tenant.
        String tenantId = TenancyHelper.getCurrentTenantId();
        if (tenantId == null) {
            // Prohibit cross tenant queries.
            String msg = "Unable to determine current user's tenant.";
            _log.error(msg);
            getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, msg);
            return new IplantErrorRepresentation(msg);
        }
        
        // Select the request processor depending on the input.
        Representation result = null;
        if (_queueName == null) result = getMultiple(tenantId);
        else if (isQueueName(_queueName)) result = getQueueByName(tenantId, _queueName);
        else result = getQueueByUUID(tenantId, _queueName);
        
        // The json result can represent an array of queues,
        // a single queue, an empty json object or an exception.
        return result;
    }
    
    /* ---------------------------------------------------------------------- */
    /* acceptRepresentation:                                                  */
    /* ---------------------------------------------------------------------- */
    /** Handles POST.  This method is used to both create new queues and update
     * existing queues.  The payload should contain a JSON object that defines
     * the key/value pairs used in either operation.  The keys use the same
     * camelCase names as fields in the JobQueue objects. See createQueue() and 
     * updateQueue() for details on required information.
     * 
     * @param entity the request payload
     * @throws ResourceException entity access errors
     */
    @Override
    public void acceptRepresentation(Representation entity) 
     throws ResourceException
    {
        // Tracing.
        if (_log.isDebugEnabled())
            _log.debug("acceptRepresentation called with queue name " + 
                       (_queueName == null ? "null" : _queueName) + ".");
        
        // Get the current tenant.
        String tenantId = TenancyHelper.getCurrentTenantId();
        if (tenantId == null) {
            // Prohibit cross tenant queries.
            String msg = "Unable to determine current user's tenant.";
            _log.error(msg);
            getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, msg);
            return;
        }
        
        // Get the payload or throw exception if there's a problem.
        // The entity is used implicitly here.
        Map<String, String> entityMap = getPostedEntityAsMap();
        
        // Basic validation useful for both create and update.
        if (_queueName == null) createQueue(entityMap);
         else updateQueue(entityMap);
    }
 
    /* ---------------------------------------------------------------------- */
    /* storeRepresentation:                                                   */
    /* ---------------------------------------------------------------------- */
    /** Handles PUT.  Query parameters are used to specify the type of action
     * and its parameters.  The defined operations are:
     * 
     *      assignNumWorkers=n, where n is an integer greater than 0
     *          - Reset the number of workers on a queue to n
     *      assignMaxMessages=n, where n is an integer greater than 0
     *          - Reset the maximum number of messages on a queue to n
     *      assignPriority=n, where n is an integer greater than 0
     *          - Reset a queue's priority
     * 
     * The above operations require URLs of the form jobs/admin/queue/{queuename}.
     * The queuename placeholder can be a queue name or the uuid of a queue.
     * 
     * @param entity the request payload, currently unused
     * @throws ResourceException 
     */
    @Override
    public void storeRepresentation(Representation entity) 
    {
        // Tracing.
        if (_log.isDebugEnabled())
            _log.debug("storeRepresentation called with queue name " + 
                       (_queueName == null ? "null" : _queueName) + ".");
        
        // -------------------------- Basic Checks --------------------------
        // Get the current tenant.
        String tenantId = TenancyHelper.getCurrentTenantId();
        if (tenantId == null) {
            // Prohibit cross tenant queries.
            String msg = "Unable to determine current user's tenant.";
            _log.error(msg);
            getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, msg);
            return;
        }
        
        // We queue or uuid must be specified.
        if (_queueName == null) {
            String msg = "No queue specified for deletion.";
            _log.error(msg);
            getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, msg);
            return;
        }
        
        // ------------------------- Input Processing -----------------------
        // Get the query parameters. 
        Form form = getQuery();
        String newPriorityStr    = form.getFirstValue(Actions.AssignPriority.name(), true);
        String newNumWorkersStr  = form.getFirstValue(Actions.AssignNumWorkers.name(), true);
        String newMaxMessagesStr = form.getFirstValue(Actions.AssignMaxMessages.name(), true);
        boolean hasNewPriority    = !StringUtils.isBlank(newPriorityStr);
        boolean hasNewNumWorkers  = !StringUtils.isBlank(newNumWorkersStr);
        boolean hasNewMaxMessages = !StringUtils.isBlank(newMaxMessagesStr);
        
        // Validate that there's something to do.
        if (!(hasNewPriority || hasNewNumWorkers || hasNewMaxMessages)) {
            String msg = "No value received for any query parameter (" + 
                         Actions.AssignPriority.name() + ", " + 
                         Actions.AssignNumWorkers.name() + " or " +
                         Actions.AssignMaxMessages.name() + ").";
            _log.error(msg);
            getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, msg);
            return;
        }
        
        // Convert input to proper types.
        int newPriority = -1;
        int newNumWorkers = -1;
        int newMaxMessages = -1;
        if (hasNewPriority)
            try {newPriority = Integer.valueOf(newPriorityStr);}
                catch (Exception e) {
                    String msg = "Invalid priority received: " + newPriorityStr;
                    _log.error(msg);
                    getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, msg);
                    return;
                }
        if (hasNewNumWorkers)
            try {newNumWorkers= Integer.valueOf(newNumWorkersStr);}
                catch (Exception e) {
                    String msg = "Invalid number of workers received: " + newNumWorkersStr;
                    _log.error(msg);
                    getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, msg);
                    return;
                }
        if (hasNewMaxMessages)
            try {newMaxMessages = Integer.valueOf(newMaxMessagesStr);}
                catch (Exception e) {
                    String msg = "Invalid maximum messages received: " + newMaxMessagesStr;
                    _log.error(msg);
                    getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, msg);
                    return;
                }
        
        // -------------------------- Queue Retrieval -----------------------
        // Let's get the current job queue.
        JobQueue queue;
        JobQueueDao dao = new JobQueueDao();
        try {
            // Attempt the deletion.
            if (isQueueName(_queueName)) queue = dao.getQueueByName(_queueName, tenantId);
                else queue = dao.getQueueByUUID(_queueName, tenantId);
        }
        catch (Exception e) {
            
            // Log message.
            String msg = "Failed to retrieve job queue.";
            _log.error(msg, e);
            
            // Determine if our code threw the exception or some subsytem did.
            Status restStatus;
            if (e.getCause() == null) restStatus = Status.CLIENT_ERROR_BAD_REQUEST;
                else restStatus = Status.SERVER_ERROR_INTERNAL;
            
            // Set the response.
            getResponse().setStatus(restStatus, msg + " [" + e.getMessage() + "]");
            return;
        }
        if (queue == null) {
            String msg = "Queue with " + (isQueueName(_queueName) ? "name " : "UUID ") +
                         "not found.";
            _log.error(msg);
            getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, msg);
            return;
        }
        
        // -------------------------- Object Update -------------------------
        // Assign the new values to the queue object if they changed.
        // Note that concurrently running updates to the same queue definition
        // can lead to inconsistent behavior since we do not lock the queue
        // record in the database between reading it and writing it.
        if (hasNewPriority && queue.getPriority() != newPriority) 
            queue.setPriority(newPriority);
          else hasNewPriority = false;
        if (hasNewNumWorkers && queue.getNumWorkers() != newNumWorkers) 
            queue.setNumWorkers(newNumWorkers);
          else hasNewNumWorkers = false;
        if (hasNewMaxMessages && queue.getMaxMessages() != newMaxMessages) 
            queue.setMaxMessages(newMaxMessages);
         else hasNewMaxMessages = false;
        
        // We're done if no changes are required.
        if (!(hasNewPriority || hasNewNumWorkers || hasNewMaxMessages)) return;
        
        // -------------------------- Queue Update --------------------------
        // Update the queue definition with all changes atomically.
        try {dao.updateFields(queue);}
        catch (Exception e) {
            String msg = "Failed to update job queue \"" + queue.getName() + "\".";
            _log.error(msg, e);
            
            // Determine if our code threw the exception or some subsytem did.
            Status restStatus;
            if (e.getCause() == null) restStatus = Status.CLIENT_ERROR_BAD_REQUEST;
                else restStatus = Status.SERVER_ERROR_INTERNAL;
            
            // Set the response.
            getResponse().setStatus(restStatus, msg + " [" + e.getMessage() + "]");
        }
        
        // -------------------------- Message Sending -----------------------
        // Make a best-effort attempt to post a message to the target phase scheduler.
        if (hasNewNumWorkers) sendConfigMessage(new ResetNumWorkersMessage(), queue);
        if (hasNewMaxMessages) sendConfigMessage(new ResetMaxMessagesMessage(), queue);
        if (hasNewPriority) sendConfigMessage(new ResetPriorityMessage(), queue);
    }
    
    /* ---------------------------------------------------------------------- */
    /* removeRepresentations:                                                 */
    /* ---------------------------------------------------------------------- */
    /** Handles DELETE.  Either a queue name or the uuid of the queue can be
     * used in the URL to identify the target queue.  Only a single queue at a
     * time can be deleted.
     * 
     */
    @Override
    public void removeRepresentations()
    {
        // Tracing.
        if (_log.isDebugEnabled())
            _log.debug("removeRepresentations called with queue name " + 
                       (_queueName == null ? "null" : _queueName) + ".");
        
        // Get the current tenant.
        String tenantId = TenancyHelper.getCurrentTenantId();
        if (tenantId == null) {
            // Prohibit cross tenant queries.
            String msg = "Unable to determine current user's tenant.";
            _log.error(msg);
            getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, msg);
            return;
        }
        
        // We only allow one queue to be deleted at a time.
        if (_queueName == null) {
            String msg = "No queue specified for deletion.";
            _log.error(msg);
            getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, msg);
            return;
        }
        
        // Issue the database call.
        try {
            // Attempt the deletion.
            JobQueueDao dao = new JobQueueDao();
            if (isQueueName(_queueName)) dao.deleteQueueByName(_queueName, tenantId);
                else dao.deleteQueueByUUID(_queueName, tenantId);
        }
        catch (Exception e) {
            
            // Log message.
            String msg = "Failed to delete job queue.";
            _log.error(msg, e);
            
            // Determine if our code threw the exception or some subsytem did.
            Status restStatus;
            if (e.getCause() == null) restStatus = Status.CLIENT_ERROR_BAD_REQUEST;
                else restStatus = Status.SERVER_ERROR_INTERNAL;
            
            // Set the response.
            getResponse().setStatus(restStatus, msg + " [" + e.getMessage() + "]");
        }
    }

    /* ********************************************************************** */
    /*                            Private Methods                             */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* getMultiple:                                                           */
    /* ---------------------------------------------------------------------- */
    /** Retrieve zero or more queue records.
     * 
     * @param tenantId the tenant id used in the retrieval
     * @return a representation of a json array or an exception 
     */
    private Representation getMultiple(String tenantId) 
    {
        // Get all queues for the current tenant.
        try {
            // Issue the database query.
            JobQueueDao dao = new JobQueueDao();
            List<JobQueue> queues = dao.getQueues(tenantId);
            
            // Create the output json writer.
            JSONWriter writer = new JSONStringer();
            writer.array();

            // Format each queue's information as json.
            for (JobQueue queue: queues) writeQueueToJson(queue, writer);

            // Complete the json array.
            writer.endArray();

            // Return the json response.
            return new IplantSuccessRepresentation(writer.toString());
        }
        catch (Exception e) {
            String msg = "Failed to retrieve job queues from database.";
            _log.error(msg, e);
            
            // Determine if our code threw the exception or some subsytem did.
            Status restStatus;
            if (e.getCause() == null) restStatus = Status.CLIENT_ERROR_BAD_REQUEST;
                else restStatus = Status.SERVER_ERROR_INTERNAL;
            
            // Set the response.
            getResponse().setStatus(restStatus,  msg + " [" + e.getMessage() + "]");
            return new IplantErrorRepresentation(e.getMessage());
        }
    }
    
    /* ---------------------------------------------------------------------- */
    /* getQueueByName:                                                        */
    /* ---------------------------------------------------------------------- */
    /** Retrieve zero or one queue from the database based on the input parameters.
     * 
     * @param tenantId the tenant id used in the retrieval
     * @param queueName a unique queue name
     * @return a representation of a json object or an exception
     */
    private Representation getQueueByName(String tenantId, String queueName)
    {
        // Get all queues for the current tenant.
        try {
            // Issue the database query.
            JobQueueDao dao = new JobQueueDao();
            JobQueue queue = dao.getQueueByName(queueName, tenantId);
            
            // Create the output json writer.
            JSONWriter writer = new JSONStringer();

            // Format the queue information as json.
            writeQueueToJson(queue, writer);

            // Return the json response.
            return new IplantSuccessRepresentation(writer.toString());
        }
        catch (Exception e) {
            String msg = "Failed to retrieve a job queue using name " + queueName + ".";
            _log.error(msg, e);
            
            // Determine if our code threw the exception or some subsytem did.
            Status restStatus;
            if (e.getCause() == null) restStatus = Status.CLIENT_ERROR_BAD_REQUEST;
                else restStatus = Status.SERVER_ERROR_INTERNAL;
            
            // Set the response.
            getResponse().setStatus(restStatus, msg + " [" + e.getMessage() + "]");
            return new IplantErrorRepresentation(e.getMessage());
        }
    }
    
    /* ---------------------------------------------------------------------- */
    /* getQueueByUUID:                                                        */
    /* ---------------------------------------------------------------------- */
    /** Retrieve zero or one queue from the database based on the input parameters.
     * 
     * @param tenantId the tenant id used in the retrieval
     * @param uuid a unique queue name
     * @return a representation of a json object or an exception
     */
    private Representation getQueueByUUID(String tenantId, String uuid)
    {
        // Get all queues for the current tenant.
        try {
            // Issue the database query.
            JobQueueDao dao = new JobQueueDao();
            JobQueue queue = dao.getQueueByUUID(uuid, tenantId);
            
            // Create the output json writer.
            JSONWriter writer = new JSONStringer();

            // Format the queue information as json.
            writeQueueToJson(queue, writer);

            // Return the json response.
            return new IplantSuccessRepresentation(writer.toString());
        }
        catch (Exception e) {
            String msg = "Failed to retrieve a job queue using UUID " + uuid + ".";
            _log.error(msg, e);
            
            // Determine if our code threw the exception or some subsytem did.
            Status restStatus;
            if (e.getCause() == null) restStatus = Status.CLIENT_ERROR_BAD_REQUEST;
                else restStatus = Status.SERVER_ERROR_INTERNAL;
            
            // Set the response.
            getResponse().setStatus(restStatus, msg + " [" + e.getMessage() + "]");
            return new IplantErrorRepresentation(e.getMessage());
        }
    }
    
    /* ---------------------------------------------------------------------- */
    /* writeQueueToJson:                                                      */
    /* ---------------------------------------------------------------------- */
    /** Write the queue to a json object using the provided writer.
     * 
     * @param queue format the queue object as json
     * @param writer the json writer that accepts the queue information 
     * @throws JSONException on error 
     */
    private void writeQueueToJson(JobQueue queue, JSONWriter writer) 
     throws JSONException
    {
        // Allow null results.
        if (queue == null) {
            writer.object().endObject();
            return;
        }
        
        // Serialize a job queue's static information.
        writer.object()
            .key("name").value(queue.getName())
            .key("priority").value(queue.getPriority())
            .key("numWorkers").value(queue.getNumWorkers())
            .key("maxMessages").value(queue.getMaxMessages())
            .key("phase").value(queue.getPhase().name())
            .key("tenantId").value(queue.getTenantId())
            .key("uuid").value(queue.getUuid())
            .key("id").value(queue.getId())
            .key("created").value(queue.getCreated())
            .key("lastUpdated").value(queue.getLastUpdated())
            .key("filter").value(queue.getFilter());
        writer.endObject();
    }
    
    /* ---------------------------------------------------------------------- */
    /* createQueue:                                                           */
    /* ---------------------------------------------------------------------- */
    /** Create a job queue using the values in the map parameter.  All of the
     * following keys except for "filter" are required:
     *
     *      name        - the name of the queue
     *      tenantId    - the tenant that uses this queue
     *      filter      - (optional) the queue selection filter
     *      phase       - the job processing phase assigned to this queue 
     *      priority    - the queue selection priority
     *      numWorkers  - the number of worker thread servicing this queue
     *      maxMessages - the queue capacity 
     * 
     * @param entityMap queue configuration information expressed as key/value pairs
     */
    private void createQueue(Map<String,String> entityMap)
    {
       // Set string fields.
       JobQueue jobQueue = new JobQueue();
       jobQueue.setName(entityMap.get("name"));
       jobQueue.setTenantId(entityMap.get("tenantId"));
       jobQueue.setFilter(entityMap.get("filter"));
       
       // Set phase.
       try {jobQueue.setPhase(JobPhaseType.valueOf(entityMap.get("phase")));}
       catch (Exception e) {
           String msg = "Invalid phase value.";
           _log.error(msg, e);
           getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, msg);
           return;
       }
       
       // Set priority.
       try {jobQueue.setPriority(Integer.valueOf(entityMap.get("priority")));}
       catch (Exception e) {
           String msg = "Invalid priority value.  An integer greater than zero is required.";
           _log.error(msg, e);
           getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, msg);
           return;
       }
       
       // Set number of worker threads.
       try {jobQueue.setNumWorkers(Integer.valueOf(entityMap.get("numWorkers")));}
       catch (Exception e) {
           String msg = "Invalid numWorkers value.  An integer greater than zero is required.";
           _log.error(msg, e);
           getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, msg);
           return;
       }
       
       // Set maximum queue messages.
       try {jobQueue.setMaxMessages(Integer.valueOf(entityMap.get("maxMessages")));}
       catch (Exception e) {
           String msg = "Invalid maxMessages value.  An integer greater than zero is required.";
           _log.error(msg, e);
           getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, msg);
           return;
       }
       
       // Issue the database call.
       JobQueueDao dao = new JobQueueDao();
       try {dao.createQueue(jobQueue);}
       catch (Exception e) {
           String msg = "Failed to create a job queue named \"" + jobQueue.getName() + "\".";
           _log.error(msg, e);
           
           // Determine if our code threw the exception or some subsytem did.
           Status restStatus;
           if (e.getCause() == null) restStatus = Status.CLIENT_ERROR_BAD_REQUEST;
               else restStatus = Status.SERVER_ERROR_INTERNAL;
           
           // Set the response.
           getResponse().setStatus(restStatus, msg + " [" + e.getMessage() + "]");
       }
    }
    
    /* ---------------------------------------------------------------------- */
    /* updateQueue:                                                           */
    /* ---------------------------------------------------------------------- */
    /** Update a job queue using the values in the map parameter.  All of the
     * following keys are required:
     *
     *      name        - the name of the queue
     *      tenantId    - the tenant that uses this queue
     *      filter      - the queue selection filter
     *      priority    - the queue selection priority
     *      numWorkers  - the number of worker thread servicing this queue
     *      maxMessages - the queue capacity 
     * 
     * @param entityMap queue configuration information expressed as key/value pairs
     */
    private void updateQueue(Map<String,String> entityMap)
    {
        // Set string fields.
        JobQueue jobQueue = new JobQueue();
        jobQueue.setName(entityMap.get("name"));
        jobQueue.setTenantId(entityMap.get("tenantId"));
        jobQueue.setFilter(entityMap.get("filter"));
        
        // Set priority.
        try {jobQueue.setPriority(Integer.valueOf(entityMap.get("priority")));}
        catch (Exception e) {
            String msg = "Invalid priority value.  An integer greater than zero is required.";
            _log.error(msg, e);
            getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, msg);
            return;
        }
        
        // Set number of worker threads.
        try {jobQueue.setNumWorkers(Integer.valueOf(entityMap.get("numWorkers")));}
        catch (Exception e) {
            String msg = "Invalid numWorkers value.  An integer greater than zero is required.";
            _log.error(msg, e);
            getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, msg);
            return;
        }
        
        // Set maximum queue messages.
        try {jobQueue.setMaxMessages(Integer.valueOf(entityMap.get("maxMessages")));}
        catch (Exception e) {
            String msg = "Invalid maxMessages value.  An integer greater than zero is required.";
            _log.error(msg, e);
            getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, msg);
            return;
        }
        
        // Issue the database call.
        JobQueueDao dao = new JobQueueDao();
        try {dao.updateFields(jobQueue);}
        catch (Exception e) {
            String msg = "Failed to update job queue \"" + jobQueue.getName() + "\".";
            _log.error(msg, e);
            
            // Determine if our code threw the exception or some subsytem did.
            Status restStatus;
            if (e.getCause() == null) restStatus = Status.CLIENT_ERROR_BAD_REQUEST;
                else restStatus = Status.SERVER_ERROR_INTERNAL;
            
            // Set the response.
            getResponse().setStatus(restStatus, msg + " [" + e.getMessage() + "]");
        }
    }
    
    /* ---------------------------------------------------------------------- */
    /* isQueueName:                                                           */
    /* ---------------------------------------------------------------------- */
    /** Determine if an identifier extracted from a url is a queue name or
     * some other type of identifier.
     * 
     * @param identifier the string to be inspected
     * @return true if a queue name is detected, false otherwise
     */
    private boolean isQueueName(String identifier) 
    {
        // ALL queue names begin with a phase string.
        if (identifier.startsWith(JobPhaseType.STAGING.name())    ||
            identifier.startsWith(JobPhaseType.SUBMITTING.name()) ||
            identifier.startsWith(JobPhaseType.MONITORING.name()) ||
            identifier.startsWith(JobPhaseType.ARCHIVING.name()))
           return true;
        
        // Not a queue name
        return false;
    }
    
    /* ---------------------------------------------------------------------- */
    /* sendConfigMessage:                                                     */
    /* ---------------------------------------------------------------------- */
    /** Populate the uninitialized configuration message with information from the 
     * queue and then post the message.  This method makes a best-effort attempt
     * to send the message, but does not throw exceptions or indicate failure (the
     * database has already been updated).
     * 
     * @param message an uninitialized configuration message
     * @param queue the queue object with its updated values
     */
    private void sendConfigMessage(AbstractQueueConfigMessage message, JobQueue queue) 
    {
        // Assign the common superclass fields.
        message.queueName = queue.getName();
        message.tenantId  = queue.getTenantId();
        message.phase     = queue.getPhase();
        
        // Assign the subclass fields.
        if (message instanceof ResetNumWorkersMessage) 
            ((ResetNumWorkersMessage)message).numWorkers = queue.getNumWorkers();
        else if (message instanceof ResetNumWorkersMessage)
            ((ResetMaxMessagesMessage)message).maxMessages = queue.getMaxMessages();
        else if (message instanceof ResetPriorityMessage)
            ((ResetPriorityMessage)message).priority = queue.getPriority();
        else {
            String msg = "Unknown configuration message type encountered: " + 
                         message.getClass().getName() + ".";
            _log.error(msg);
            return;
        }
        
        // Send the message and hope it gets there...
        try {TopicMessageSender.sendConfigMessage(message);}
        catch (Exception e) {
            String msg = "Unable to post " + message.getClass().getSimpleName() + 
                         " message for queue " + queue.getName() + ".";
            _log.error(msg);
        }
    }
}
