/**
 * 
 */
package org.iplantc.service.jobs.resources;

import java.io.FileNotFoundException;

import org.apache.log4j.Logger;
import org.hibernate.ObjectNotFoundException;
import org.iplantc.service.common.clients.AgaveLogServiceClient;
import org.iplantc.service.common.exceptions.PermissionException;
import org.iplantc.service.common.representation.IplantErrorRepresentation;
import org.iplantc.service.common.representation.IplantSuccessRepresentation;
import org.iplantc.service.jobs.callbacks.JobCallback;
import org.iplantc.service.jobs.callbacks.JobCallbackManager;
import org.iplantc.service.jobs.exceptions.JobCallbackException;
import org.iplantc.service.jobs.exceptions.JobProcessingException;
import org.iplantc.service.jobs.managers.JobRequestProcessor;
import org.iplantc.service.jobs.model.Job;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Class to handle get and post requests for jobs
 * 
 * @author dooley
 * 
 */
public class JobUpdateResource extends AbstractJobResource 
{
	private static final Logger	log	= Logger.getLogger(JobUpdateResource.class);

	private String				uuid;
	private String				updateToken;
	private String				status;
	private String				localSchedulerId;

	/**
	 * @param context
	 * @param request
	 * @param response
	 */
	public JobUpdateResource(Context context, Request request, Response response)
	{
		super(context, request, response);

		this.uuid = (String) request.getAttributes().get("jobid");
		this.updateToken = (String) request.getAttributes().get("token");
		this.status = (String) request.getAttributes().get("status");
		this.localSchedulerId = (String) request.getAttributes().get("localId");

		getVariants().add(new Variant(MediaType.APPLICATION_JSON));
	}

	/**
	 * This method represents the HTTP GET action. It provides a simple,
	 * convenient way for jobs to update their own status information while
	 * running. If the job id, token, or status included in the URL are invalid
	 * or the form data is invalid, a HTTP
	 * {@link org.restlet.data.Status#CLIENT_ERROR_BAD_REQUEST 400} code is
	 * sent. If a problem occurs looking up the job a HTTP
	 * {@link org.restlet.data.Status#SERVER_ERROR_INTERNAL 500} code is sent.
	 */
	@Override
	public Representation represent(Variant variant) throws ResourceException
	{
		try
		{
		    JobCallback jobCallback = new JobCallback(uuid, status, updateToken, localSchedulerId);
            JobCallbackManager callbackManager = new JobCallbackManager();
            Job job = callbackManager.process(jobCallback);
            
            return new IplantSuccessRepresentation(job.toJSON());
		}
		catch (ObjectNotFoundException e) {
		    getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND);
            return new IplantErrorRepresentation("No job found with the given id and key");
		}
		catch (JobCallbackException e) {
		    getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
            return new IplantErrorRepresentation(e.getMessage());
		}
		catch (PermissionException e) {
		    getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN);
		    return new IplantErrorRepresentation(e.getMessage());
		}
		catch (Throwable e)
		{
			log.debug("Callback failed: " + e.getMessage());
			// can't set a stopped job back to running. Bad request
			getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
			return new IplantErrorRepresentation("Failed to process job callback.");
		}
	}
	
	/**
	 * Handles POST requests from a running job. The content will get forwarded to
	 * as a user-defined event on the job.
	 */
	@Override
	public void acceptRepresentation(Representation entity) throws ResourceException
	{
		AgaveLogServiceClient.log(AgaveLogServiceClient.ServiceKeys.JOBS02.name(),
				AgaveLogServiceClient.ActivityKeys.JobsCustomRuntimeEvent.name(),
				username, "", getRequest().getClientInfo().getUpstreamAddress());

		try
		{
			JsonNode customRuntimeJsonBody = getPostedEntityAsObjectNode(false);
			
		    JobCallback jobCallback = new JobCallback(uuid, status, updateToken);
            JobCallbackManager callbackManager = new JobCallbackManager();
            callbackManager.processCustomRuntimeEvent(jobCallback, customRuntimeJsonBody);
            
            getResponse().setStatus(Status.SUCCESS_ACCEPTED);
            getResponse().setEntity(new IplantSuccessRepresentation(customRuntimeJsonBody.toString()));
		}
		catch (ObjectNotFoundException e) {
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
					"No job found with the given id and key", e);
		}
		catch (JobCallbackException e) {
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
					e.getMessage(), e);
		}
		catch (PermissionException e) {
			throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN, 
					e.getMessage(), e);
		}
		catch (FileNotFoundException e) {
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND);
		}
		catch (Throwable e)
		{
			log.debug("Custom runtime callback event " + status + " failed unexpectedly for job " + 
					uuid + ". " + e.getMessage());
			// can't set a stopped job back to running. Bad request
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL, 
					"Failed to process job callback", e);
		}
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.restlet.resource.Resource#allowDelete()
	 */
	@Override
	public boolean allowDelete()
	{
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.restlet.resource.Resource#allowGet()
	 */
	@Override
	public boolean allowGet()
	{
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.restlet.resource.Resource#allowPost()
	 */
	@Override
	public boolean allowPost()
	{
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.restlet.resource.Resource#allowPut()
	 */
	@Override
	public boolean allowPut()
	{
		return false;
	}

}
