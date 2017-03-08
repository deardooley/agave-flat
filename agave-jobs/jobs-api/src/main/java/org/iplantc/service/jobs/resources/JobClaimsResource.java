package org.iplantc.service.jobs.resources;

import java.util.List;

import org.apache.log4j.Logger;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.common.representation.IplantErrorRepresentation;
import org.iplantc.service.common.representation.IplantSuccessRepresentation;
import org.iplantc.service.jobs.dao.JobWorkerDao;
import org.iplantc.service.jobs.model.JobClaim;
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

/** Read or delete claim records from the job_workers table using various selectors.
 * 
 * @author rcardone
 */
public class JobClaimsResource
 extends AbstractJobResource
{
    /* ********************************************************************** */
    /*                                Constants                               */
    /* ********************************************************************** */
    // Tracing.
    private static final Logger _log = Logger.getLogger(JobClaimsResource.class);

    /* ********************************************************************** */
    /*                                 Fields                                 */
    /* ********************************************************************** */
    // The target queue name or uuid when one is provided.
    private String _jobId;
    private String _workerId;
    private String _schedulerId;
    private String _containerId;
    
    /* ********************************************************************** */
    /*                              Constructors                              */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* constructor:                                                           */
    /* ---------------------------------------------------------------------- */
    public JobClaimsResource(Context context, Request request, Response response)
    {
        super(context, request, response);
        this.username = getAuthenticatedUsername();
        getVariants().add(new Variant(MediaType.APPLICATION_JSON));
        setModifiable(true); // allows POST, PUT and DELETE.
        
        // Get an identifier from the URL when one is present.
        _jobId = (String) request.getAttributes().get("jobid");
        _workerId = (String) request.getAttributes().get("workerid");
        _schedulerId = (String) request.getAttributes().get("schedulerid");
        _containerId = (String) request.getAttributes().get("containerid");
    }

    /* ********************************************************************** */
    /*                             Public Methods                             */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* represent:                                                             */
    /* ---------------------------------------------------------------------- */
    /** Handles GET. Returns the claim associated with a specific job or worker,
     * or those associated with a specific scheduler or container, or all claims. 
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
                       ".represent called with jobid = " + 
                       (_jobId == null ? "null" : _jobId) + ", workerid = " +
                       (_workerId == null ? "null" : _workerId) + ", containerid = " +
                       (_containerId == null ? "null" : _containerId) + ", schedulerid = " +
                       (_schedulerId == null ? "null" : _schedulerId) + ".");
        
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
        if (_jobId != null || _workerId != null) result = getClaim();
          else result = getClaims();
        
        // The json result can represent an array of claims,
        // a single claim, an empty json object or an exception.
        return result;
    }
    
    /* ---------------------------------------------------------------------- */
    /* removeRepresentations:                                                 */
    /* ---------------------------------------------------------------------- */
    /** Handles DELETE.  Delete one or more claims based on the selector provided.
     */
    @Override
    public void removeRepresentations()
    {
        // Tracing.
        if (_log.isDebugEnabled())
            _log.debug(getClass().getSimpleName() + 
                       ".removeRepresentations called with jobid = " + 
                       (_jobId == null ? "null" : _jobId) + ", workerid = " +
                       (_workerId == null ? "null" : _workerId) + ", containerid = " +
                       (_containerId == null ? "null" : _containerId) + ", schedulerid = " +
                       (_schedulerId == null ? "null" : _schedulerId) + ".");
        
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
            
            // Select the request processor depending on the input.
            if (_jobId != null) deleted = JobWorkerDao.unclaimJobByJobUuid(_jobId);
            else if (_workerId != null) deleted = JobWorkerDao.unclaimJobByWorkerUuid(_workerId);
            else if (_containerId != null) deleted = JobWorkerDao.unclaimJobsForContainer(_containerId);
            else if (_schedulerId != null) deleted = JobWorkerDao.unclaimJobsForScheduler(_schedulerId);
            else deleted = JobWorkerDao.clearClaims();
            
            if (_log.isInfoEnabled())
                _log.info(getClass().getSimpleName() + ".removeRepresentations deleted " +
                          deleted + " claims.");
        }
        catch (Exception e) {
            
            // Log message.
            String msg = "Failed to delete job claim(s).";
            _log.error(msg, e);
            
            // Determine if our code threw the exception or some subsystem did.
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
    /* getClaims:                                                             */
    /* ---------------------------------------------------------------------- */
    /** Retrieve zero or more claim records.
     * 
     * @return a representation of a json array or an exception 
     */
    private Representation getClaims() 
    {
        // Get queues.
        try {
            // Issue the database query.
            List<JobClaim> claims;
            if (_containerId != null) claims = JobWorkerDao.getJobClaimsForContainer(_containerId);
            else if (_schedulerId != null) claims = JobWorkerDao.getJobClaimsForScheduler(_schedulerId);
            else claims = JobWorkerDao.getJobClaims();
            
            // Create the output json writer.
            JSONWriter writer = new JSONStringer();
            writer.array();

            // Format each claim's information as json.
            for (JobClaim claim: claims) writeClaimToJson(claim, writer);

            // Complete the json array.
            writer.endArray();

            // Return the json response.
            return new IplantSuccessRepresentation(writer.toString());
        }
        catch (Exception e) {
            String msg = "Failed to retrieve job claims from database.";
            _log.error(msg, e);
            
            // Determine if our code threw the exception or some subsystem did.
            Status restStatus;
            if (e.getCause() == null) restStatus = Status.CLIENT_ERROR_BAD_REQUEST;
                else restStatus = Status.SERVER_ERROR_INTERNAL;
            
            // Set the response.
            getResponse().setStatus(restStatus,  msg + " [" + e.getMessage() + "]");
            return new IplantErrorRepresentation(e.getMessage());
        }
    }
    
    /* ---------------------------------------------------------------------- */
    /* getClaim:                                                              */
    /* ---------------------------------------------------------------------- */
    /** Retrieve zero or one claim record.
     * 
     * @return a representation of a json object or an exception 
     */
    private Representation getClaim()
    {
        // Get queues.
        try {
            // Issue the database query.
            JobClaim claim;
            if (_jobId != null) claim = JobWorkerDao.getJobClaimByJobUuid(_jobId);
             else claim = JobWorkerDao.getJobClaimByWorkerUuid(_workerId);
            
            // Create the output json writer.
            JSONWriter writer = new JSONStringer();

            // Format the claim information as json.
            writeClaimToJson(claim, writer);

            // Return the json response.
            return new IplantSuccessRepresentation(writer.toString());
        }
        catch (Exception e) {
            String msg;
            if (_jobId != null) msg = "Failed to retrieve a claim for job with UUID " + _jobId + ".";
             else msg = "Failed to retrieve a claim for worker with UUID " + _workerId + ".";
            _log.error(msg, e);
            
            // Determine if our code threw the exception or some subsystem did.
            Status restStatus;
            if (e.getCause() == null) restStatus = Status.CLIENT_ERROR_BAD_REQUEST;
                else restStatus = Status.SERVER_ERROR_INTERNAL;
            
            // Set the response.
            getResponse().setStatus(restStatus, msg + " [" + e.getMessage() + "]");
            return new IplantErrorRepresentation(e.getMessage());
        }
    }

    /* ---------------------------------------------------------------------- */
    /* writeClaimToJson:                                                      */
    /* ---------------------------------------------------------------------- */
    /** Write the claim to a json object using the provided writer.
     * 
     * @param claim the claim object to be formatted as json
     * @param writer the json writer that accepts the queue information 
     * @throws JSONException on error 
     */
    private void writeClaimToJson(JobClaim claim, JSONWriter writer) 
     throws JSONException
    {
        // Allow null results.
        if (claim == null) {
            writer.object().endObject();
            return;
        }
        
        // Serialize the claim.
        writer.object()
               .key("jobUuid").value(claim.getJobUuid())
               .key("workerUuid").value(claim.getWorkerUuid())
               .key("schedulerName").value(claim.getSchedulerName())
               .key("host").value(claim.getHost())
               .key("containerId").value(claim.getContainerId());
        writer.endObject();
    }
}
