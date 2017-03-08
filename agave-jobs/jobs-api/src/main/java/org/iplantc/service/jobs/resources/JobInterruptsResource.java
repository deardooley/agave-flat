package org.iplantc.service.jobs.resources;

import java.util.List;

import org.apache.log4j.Logger;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.common.representation.IplantErrorRepresentation;
import org.iplantc.service.common.representation.IplantSuccessRepresentation;
import org.iplantc.service.jobs.dao.JobInterruptDao;
import org.iplantc.service.jobs.model.JobInterrupt;
import org.json.JSONException;
import org.json.JSONStringer;
import org.json.JSONWriter;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;

/** Read or delete interrupt records from the job_interrrupts table.
 * 
 * @author rcardone
 */
public class JobInterruptsResource
 extends AbstractJobResource
{
    /* ********************************************************************** */
    /*                                Constants                               */
    /* ********************************************************************** */
    // Tracing.
    private static final Logger _log = Logger.getLogger(JobInterruptsResource.class);

    /* ********************************************************************** */
    /*                                 Fields                                 */
    /* ********************************************************************** */
    // The target queue name or uuid when one is provided.
    private String _jobId;
    
    /* ********************************************************************** */
    /*                              Constructors                              */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* constructor:                                                           */
    /* ---------------------------------------------------------------------- */
    public JobInterruptsResource(Context context, Request request, Response response)
    {
        super(context, request, response);
        this.username = getAuthenticatedUsername();
        getVariants().add(new Variant(MediaType.APPLICATION_JSON));
        setModifiable(true); // allows POST, PUT and DELETE.
        
        // Get the queue name from the URL when it is present.
        _jobId = (String) request.getAttributes().get("jobid");
    }

    /* ********************************************************************** */
    /*                             Public Methods                             */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* represent:                                                             */
    /* ---------------------------------------------------------------------- */
    /** Handles GET. Returns all interrupts or only those associated with a 
     * specific job depending on whether a job ID is provided. 
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
            _log.debug(getClass().getSimpleName() + 
                       ".represent called with jobid equal to " + 
                       (_jobId == null ? "null" : _jobId) + ".");
        
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
        Representation result = getInterrupts(_jobId, tenantId);
        
        // The json result can represent an array of claims,
        // a single claim, an empty json object or an exception.
        return result;
    }
    
    /* ---------------------------------------------------------------------- */
    /* removeRepresentations:                                                 */
    /* ---------------------------------------------------------------------- */
    /** Handles DELETE.  Delete either all interrupts or only those associated
     * with a specific job depending whether a selector is provided.
     */
    @Override
    public void removeRepresentations()
    {
        // Tracing.
        // Tracing.
        if (_log.isDebugEnabled())
            _log.debug(getClass().getSimpleName() + 
                       ".represent called with jobid equal to " + 
                       (_jobId == null ? "null" : _jobId) + ".");
        
        // Get the current tenant.
        String tenantId = TenancyHelper.getCurrentTenantId();
        if (tenantId == null) {
            // Prohibit cross tenant queries.
            String msg = "Unable to determine current user's tenant.";
            _log.error(msg);
            getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, msg);
            return;
        }
        
        try {
            // Rows deleted.
            int deleted = 0;
            
            // We delete all interrupts or all interrupts for a specific job.
            if (_jobId != null) {
                // Get the ids of the interrupts of the specified job.
                List<JobInterrupt> interrupts = JobInterruptDao.getInterrupts(_jobId, tenantId);
                
                // Delete each interrupt found for the job.
                for (JobInterrupt interrupt : interrupts)
                    deleted += JobInterruptDao.deleteInterrupt(interrupt.getId(), tenantId);
            }
            // Delete all interrupts.
            else deleted = JobInterruptDao.clearInterrupts();
            
            if (_log.isInfoEnabled())
                _log.info(getClass().getSimpleName() + ".removeRepresentations deleted " +
                          deleted + " interrupts.");
        }
        catch (Exception e) {
            
            // Log message.
            String msg = "Failed to delete job claim(s).";
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
    /* getInterrupts:                                                         */
    /* ---------------------------------------------------------------------- */
    /** Retrieve zero or more interrupt records.
     * 
     * @return a representation of a json array or an exception 
     */
    private Representation getInterrupts(String jobId, String tenantId) 
    {
        // Get interrupts.
        try {
            // Issue the database query.
            List<JobInterrupt> interrupts;
            if (jobId == null) interrupts = JobInterruptDao.getInterrupts();
              else interrupts = JobInterruptDao.getInterrupts(jobId, tenantId);
            
            // Create the output json writer.
            JSONWriter writer = new JSONStringer();
            writer.array();

            // Format each interrupts's information as json.
            for (JobInterrupt interrupt: interrupts) writeInterruptToJson(interrupt, writer);

            // Complete the json array.
            writer.endArray();

            // Return the json response.
            return new IplantSuccessRepresentation(writer.toString());
        }
        catch (Exception e) {
            String msg = "Failed to retrieve job interrupts from database ";
            if (jobId == null) msg += "specifying no parameters.";
              else msg += "using parameters jobId = " + jobId + ", tenantId = " + tenantId + ".";
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
    /* writeInterruptToJson:                                                  */
    /* ---------------------------------------------------------------------- */
    /** Write the interrupt to a json object using the provided writer.
     * 
     * @param interrupt the interrupt object to be formatted as json
     * @param writer the json writer that accepts the queue information 
     * @throws JSONException on error 
     */
    private void writeInterruptToJson(JobInterrupt interrupt, JSONWriter writer) 
     throws JSONException
    {
        // Allow null results.
        if (interrupt == null) {
            writer.object().endObject();
            return;
        }
        
        // Serialize the claim.
        writer.object()
               .key("id").value(interrupt.getId())
               .key("jobUuid").value(interrupt.getJobUuid())
               .key("tenantId").value(interrupt.getTenantId())
               .key("epoch").value(interrupt.getEpoch())
               .key("interruptType").value(interrupt.getInterruptType().name())
               .key("created").value(interrupt.getCreated())
               .key("expiresAt").value(interrupt.getExpiresAt());  
        writer.endObject();
    }
}
