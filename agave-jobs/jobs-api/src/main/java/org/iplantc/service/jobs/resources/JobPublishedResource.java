package org.iplantc.service.jobs.resources;

import java.util.List;

import org.apache.log4j.Logger;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.common.representation.IplantErrorRepresentation;
import org.iplantc.service.common.representation.IplantSuccessRepresentation;
import org.iplantc.service.jobs.dao.JobPublishedDao;
import org.iplantc.service.jobs.model.JobPublished;
import org.iplantc.service.jobs.model.enumerations.JobPhaseType;
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

/**
 * @author rcardone
 *
 */
public class JobPublishedResource
 extends AbstractJobResource
{
    /* ********************************************************************** */
    /*                                Constants                               */
    /* ********************************************************************** */
    // Tracing.
    private static final Logger _log = Logger.getLogger(JobPublishedResource.class);

    /* ********************************************************************** */
    /*                                 Fields                                 */
    /* ********************************************************************** */
    // The target queue name or uuid when one is provided.
    private String _jobId;
    private String _phase;
    
    /* ********************************************************************** */
    /*                              Constructors                              */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* constructor:                                                           */
    /* ---------------------------------------------------------------------- */
    public JobPublishedResource(Context context, Request request, Response response)
    {
        super(context, request, response);
        this.username = getAuthenticatedUsername();
        getVariants().add(new Variant(MediaType.APPLICATION_JSON));
        setModifiable(true); // allows POST, PUT and DELETE.
        
        // Get the queue name from the URL when it is present.
        _phase = (String) request.getAttributes().get("phase");
        _jobId = (String) request.getAttributes().get("jobid");
    }

    /* ********************************************************************** */
    /*                             Public Methods                             */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* represent:                                                             */
    /* ---------------------------------------------------------------------- */
    /** Handles GET. Returns a single published record or an array of records
     * depending the url value. 
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
                       ".represent called with phase = " +
                       (_phase == null ? "null" : _phase) + ", jobId = " +
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
        
        // Validate phase.
        JobPhaseType phaseType = null;
        if (_phase != null) {
            try {phaseType = JobPhaseType.valueOf(_phase.toUpperCase());}
            catch (Exception e) {
                // Log message.
                String msg = "Invalid phase type received: " + _phase;
                _log.error(msg, e);
                
                // Set the response.
                Status restStatus = Status.CLIENT_ERROR_BAD_REQUEST;
                getResponse().setStatus(restStatus, msg + " [" + e.getMessage() + "]");
                return new IplantErrorRepresentation(e.getMessage());
            }
        }
        
        // Select the request processor depending on the input.
        Representation result = null;
        if (_jobId != null && phaseType != null) result = getJobInPhase(phaseType, _jobId);
        else if (phaseType != null) result = getPublishedInPhase(phaseType);
        else result = getAllPublished();
        
        // The json result can represent an array of claims,
        // a single claim, an empty json object or an exception.
        return result;
    }
    
    /* ---------------------------------------------------------------------- */
    /* removeRepresentations:                                                 */
    /* ---------------------------------------------------------------------- */
    /** Handles DELETE.  Delete one or more published records depending on input
     * parameters. 
     */
    @Override
    public void removeRepresentations()
    {
        // Tracing.
        if (_log.isDebugEnabled())
            _log.debug(getClass().getSimpleName() + 
                       ".represent called with phase = " +
                       (_phase == null ? "null" : _phase) + ", jobId = " +
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
        
        // Validate phase.
        JobPhaseType phaseType = null;
        if (_phase != null) {
            try {phaseType = JobPhaseType.valueOf(_phase.toUpperCase());}
            catch (Exception e) {
                // Log message.
                String msg = "Invalid phase type received: " + _phase;
                _log.error(msg, e);
                
                // Set the response.
                Status restStatus = Status.CLIENT_ERROR_BAD_REQUEST;
                getResponse().setStatus(restStatus, msg + " [" + e.getMessage() + "]");
                return;
            }
        }
        
        // Call the appropriate deletion method.
        if (_jobId != null && phaseType != null) deleteJobInPhase(phaseType, _jobId);
        else if (phaseType != null) deletePublishedInPhase(phaseType);
        else deleteAllPublished();
    }

    /* ********************************************************************** */
    /*                            Private Methods                             */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* getJobInPhase:                                                         */
    /* ---------------------------------------------------------------------- */
    /** Return the published record for a specific job in a specific phase if
     * it exists.
     * 
     * @param phaseType the phase to query
     * @param jobId the job uuid to query
     * @return a json object, possibly empty
     */
    private Representation getJobInPhase(JobPhaseType phaseType, String jobId)
    {
        // Get all published records for the phase.
        try {
            // Issue the database query.
            List<JobPublished> records = JobPublishedDao.getPublishedJobs(phaseType);
            
            // Find the requested job.
            JobPublished published = null;
            for (JobPublished current : records) 
                if (jobId.equals(current.getJobUuid())) {
                    published = current;
                    break;
                }
            
            // Create the output json writer.
            JSONWriter writer = new JSONStringer();

            // Format the record's information as json if it exists.
            writePublishedToJson(published, writer);

            // Return the json response.
            return new IplantSuccessRepresentation(writer.toString());
        }
        catch (Exception e) {
            String msg = "Failed to retrieve published job records from database.";
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
    /* getPublishedInPhase:                                                   */
    /* ---------------------------------------------------------------------- */
    /** Return a snapshot of the jobs published in the given phase.
     * 
     * @param phaseType the phase to query
     * @return a json array, possibly empty
     */
    private Representation getPublishedInPhase(JobPhaseType phaseType)
    {
        // Get all published records for the phase.
        try {
            // Issue the database query.
            List<JobPublished> records = JobPublishedDao.getPublishedJobs(phaseType);
            
            // Create the output json writer.
            JSONWriter writer = new JSONStringer();
            writer.array();

            // Format each record's information as json.
            for (JobPublished published: records) writePublishedToJson(published, writer);

            // Complete the json array.
            writer.endArray();

            // Return the json response.
            return new IplantSuccessRepresentation(writer.toString());
        }
        catch (Exception e) {
            String msg = "Failed to retrieve published job records from database.";
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
    /* getAllPublished:                                                       */
    /* ---------------------------------------------------------------------- */
    /** Return a snapshot of all published jobs.
     * 
     * @return a json array, possibly empty
     */
    private Representation getAllPublished()
    {
        // Get all published records for all phases.
        try {
            // Create the output json writer.
            JSONWriter writer = new JSONStringer();
            writer.array();

            // Get the published records for each phase type.  Note that since
            // multiple database calls are made, and since published records are
            // deleted in a lazy fashion, the response may occasionally contain
            // inconsistencies.  For example, the same job may be seen in more
            // than one phase.
            for (JobPhaseType phaseType : JobPhaseType.values()) {
                // Issue the database query.
                List<JobPublished> records = JobPublishedDao.getPublishedJobs(phaseType);
                
                // Format each record's information as json.
                for (JobPublished published: records) writePublishedToJson(published, writer);
            }

            // Complete the json array.
            writer.endArray();

            // Return the json response.
            return new IplantSuccessRepresentation(writer.toString());
        }
        catch (Exception e) {
            String msg = "Failed to retrieve published job records from database.";
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
    /* deleteJobInPhase:                                                      */
    /* ---------------------------------------------------------------------- */
    /** Delete the published record for a specific job in a specific phase if
     * it exists.
     * 
     * @param phaseType the phase of the job to delete
     * @param jobId the job uuid to be deleted
     */
    private void deleteJobInPhase(JobPhaseType phaseType, String jobId)
    {
        // Delete the one record for the phase/jobId if it exists.
        try {
            // Rows deleted.
            int deleted = JobPublishedDao.deletePublishedJob(phaseType, jobId);
            
            if (_log.isInfoEnabled())
                _log.info(getClass().getSimpleName() + ".deleteJobInPhase deleted " +
                          deleted + " published record(s) for job " + 
                          jobId + " in phase " + phaseType + ".");
        }
        catch (Exception e) {
            
            // Log message.
            String msg = "Failed to delete published record for job " + 
                         jobId + " in phase " + phaseType + ".";
            _log.error(msg, e);
            
            // Determine if our code threw the exception or some subsystem did.
            Status restStatus;
            if (e.getCause() == null) restStatus = Status.CLIENT_ERROR_BAD_REQUEST;
                else restStatus = Status.SERVER_ERROR_INTERNAL;
            
            // Set the response.
            getResponse().setStatus(restStatus, msg + " [" + e.getMessage() + "]");
        }
    }
    
    /* ---------------------------------------------------------------------- */
    /* deletePublishedInPhase:                                                */
    /* ---------------------------------------------------------------------- */
    /** Delete the jobs published in the given phase.
     * 
     * @param phaseType the phase whose jobs will be deleted
     */
    private void deletePublishedInPhase(JobPhaseType phaseType)
    {
        // Delete the one record for the phase/jobId if it exists.
        try {
            // Get a snapshot of the published records for specified phase.
            List<JobPublished> records = JobPublishedDao.getPublishedJobs(phaseType);
            
            // Rows deleted.
            int deleted = 0;
            
            // Delete each record.
            for (JobPublished published : records)
                deleted += JobPublishedDao.deletePublishedJob(phaseType, published.getJobUuid());
            
            if (_log.isInfoEnabled())
                _log.info(getClass().getSimpleName() + ".deletePublishedInPhase deleted " +
                          deleted + " published record(s) for phase " + phaseType + ".");
        }
        catch (Exception e) {
            
            // Log message.
            String msg = "Failed to delete published records in phase " + phaseType + ".";
            _log.error(msg, e);
            
            // Determine if our code threw the exception or some subsystem did.
            Status restStatus;
            if (e.getCause() == null) restStatus = Status.CLIENT_ERROR_BAD_REQUEST;
                else restStatus = Status.SERVER_ERROR_INTERNAL;
            
            // Set the response.
            getResponse().setStatus(restStatus, msg + " [" + e.getMessage() + "]");
        }
    }
    
    /* ---------------------------------------------------------------------- */
    /* deleteAllPublished:                                                    */
    /* ---------------------------------------------------------------------- */
    /** Delete all published jobs at a given moment in time.
     */
    private void deleteAllPublished()
    {
        // Delete the one record for the phase/jobId if it exists.
        try {
            // Rows deleted.
            int deleted = JobPublishedDao.clearPublishedJobs();
            
            if (_log.isInfoEnabled())
                _log.info(getClass().getSimpleName() + ".deleteAllPublished deleted " +
                          deleted + " published record(s).");
        }
        catch (Exception e) {
            
            // Log message.
            String msg = "Failed to delete all published records.";
            _log.error(msg, e);
            
            // Determine if our code threw the exception or some subsystem did.
            Status restStatus;
            if (e.getCause() == null) restStatus = Status.CLIENT_ERROR_BAD_REQUEST;
                else restStatus = Status.SERVER_ERROR_INTERNAL;
            
            // Set the response.
            getResponse().setStatus(restStatus, msg + " [" + e.getMessage() + "]");
        }
    }
    
    /* ---------------------------------------------------------------------- */
    /* writeClaimToJson:                                                      */
    /* ---------------------------------------------------------------------- */
    /** Write the published record to a json object using the provided writer.
     * 
     * @param published the published object to be formatted as json
     * @param writer the json writer that accepts the queue information 
     * @throws JSONException on error 
     */
    private void writePublishedToJson(JobPublished published, JSONWriter writer) 
     throws JSONException
    {
        // Allow null results.
        if (published == null) {
            writer.object().endObject();
            return;
        }
        
        // Serialize the claim.
        writer.object()
               .key("phase").value(published.getPhase().name())
               .key("jobUuid").value(published.getJobUuid())
               .key("created").value(published.getCreated())
               .key("creator").value(published.getCreator());
        writer.endObject();
    }
}
