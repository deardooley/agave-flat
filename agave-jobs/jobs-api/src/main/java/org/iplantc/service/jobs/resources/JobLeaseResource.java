package org.iplantc.service.jobs.resources;

import java.util.List;

import org.apache.log4j.Logger;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.common.representation.IplantErrorRepresentation;
import org.iplantc.service.common.representation.IplantSuccessRepresentation;
import org.iplantc.service.jobs.dao.JobLeaseDao;
import org.iplantc.service.jobs.model.JobLease;
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

/** Read or release all leases.
 * 
 * @author rcardone
 */
public class JobLeaseResource
 extends AbstractJobResource
{
    /* ********************************************************************** */
    /*                                Constants                               */
    /* ********************************************************************** */
    // Tracing.
    private static final Logger _log = Logger.getLogger(JobLeaseResource.class);

    /* ********************************************************************** */
    /*                              Constructors                              */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* constructor:                                                           */
    /* ---------------------------------------------------------------------- */
    public JobLeaseResource(Context context, Request request, Response response)
    {
        super(context, request, response);
        this.username = getAuthenticatedUsername();
        getVariants().add(new Variant(MediaType.APPLICATION_JSON));
        setModifiable(true); // allows POST, PUT and DELETE.
    }

    /* ********************************************************************** */
    /*                             Public Methods                             */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* represent:                                                             */
    /* ---------------------------------------------------------------------- */
    /** Handles GET. Returns all leases. 
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
            _log.debug(getClass().getSimpleName() + ".represent called.");
        
        // Get the current tenant.
        String tenantId = TenancyHelper.getCurrentTenantId();
        if (tenantId == null) {
            // Prohibit cross tenant queries.
            String msg = "Unable to determine current user's tenant.";
            _log.error(msg);
            getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, msg);
            return new IplantErrorRepresentation(msg);
        }
        
        // Return the representation.
        Representation result = getLeases();
        return result;
    }
    
    /* ---------------------------------------------------------------------- */
    /* removeRepresentations:                                                 */
    /* ---------------------------------------------------------------------- */
    /** Handles DELETE.  Releases all leases.
     */
    @Override
    public void removeRepresentations()
    {
        // Tracing.
        _log.debug(getClass().getSimpleName() + ".removeRepresentations called.");      
        
        // Get the current tenant.
        String tenantId = TenancyHelper.getCurrentTenantId();
        if (tenantId == null) {
            // Prohibit cross tenant queries.
            String msg = "Unable to determine current user's tenant.";
            _log.error(msg);
            getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, msg);
            return;
        }
        
        // Perform the soft delete.
        try {
            // Release all leases.
            int released = JobLeaseDao.clearLeases();
            
            if (_log.isInfoEnabled())
                _log.info(getClass().getSimpleName() + ".removeRepresentations released " +
                        released + " leases.");
        }
        catch (Exception e) {
            
            // Log message.
            String msg = "Failed to release job leases.";
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
    /* getLeases:                                                             */
    /* ---------------------------------------------------------------------- */
    /** Retrieve zero or more claim records.
     * 
     * @return a representation of a json array or an exception 
     */
    private Representation getLeases() 
    {
        // Get all leases.
        try {
            // Issue the database query.
            List<JobLease> leases = JobLeaseDao.getLeases();
            
            // Create the output json writer.
            JSONWriter writer = new JSONStringer();
            writer.array();

            // Format each lease's information as json.
            for (JobLease lease: leases) writeLeaseToJson(lease, writer);

            // Complete the json array.
            writer.endArray();

            // Return the json response.
            return new IplantSuccessRepresentation(writer.toString());
        }
        catch (Exception e) {
            String msg = "Failed to retrieve job claims from database.";
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
    /* writeLeaseToJson:                                                      */
    /* ---------------------------------------------------------------------- */
    /** Write the claim to a json object using the provided writer.
     * 
     * @param claim the claim object to be formatted as json
     * @param writer the json writer that accepts the queue information 
     * @throws JSONException on error 
     */
    private void writeLeaseToJson(JobLease lease, JSONWriter writer) 
     throws JSONException
    {
        // Allow null results.
        if (lease == null) {
            writer.object().endObject();
            return;
        }
        
        // Serialize the lease.
        writer.object()
               .key("lease").value(lease.getLease())
               .key("lastUpdated").value(lease.getLastUpdated())
               .key("expiresAt").value(lease.getExpiresAt())
               .key("lessee").value(lease.getLessee());
        writer.endObject();
    }
}
