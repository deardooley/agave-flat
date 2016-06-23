/**
 * 
 */
package org.iplantc.service.jobs.resources;

import org.iplantc.service.apps.util.ServiceUtils;
import org.iplantc.service.common.clients.AgaveLogServiceClient;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.common.representation.IplantErrorRepresentation;
import org.iplantc.service.common.representation.IplantSuccessRepresentation;
import org.iplantc.service.jobs.Settings;
import org.iplantc.service.jobs.dao.JobDao;
import org.iplantc.service.jobs.exceptions.JobException;
import org.iplantc.service.jobs.managers.JobPermissionManager;
import org.iplantc.service.jobs.model.Job;
import org.json.JSONObject;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;

/**
 * Class to handle job listings for the authenticated user.
 * 
 * @author dooley
 * 
 */
public class JobStatusResource extends AbstractJobResource 
{
	private String	sJobId;

	private Job		job;
	/**
	 * @param context
	 * @param request
	 * @param response
	 */
	public JobStatusResource(Context context, Request request,
			Response response)
	{
		super(context, request, response);

		this.username = getAuthenticatedUsername();
		
		this.sJobId = (String) request.getAttributes().get("jobid");

		getVariants().add(new Variant(MediaType.APPLICATION_JSON));
		
		AgaveLogServiceClient.log(AgaveLogServiceClient.ServiceKeys.JOBS02.name(), 
				AgaveLogServiceClient.ActivityKeys.JobStatus.name(), 
				username, "", request.getClientInfo().getUpstreamAddress());
	}

	/**
	 * This method represents the HTTP GET action. A list of jobs is retrieved
	 * from the service database and serialized to a {@link org.json.JSONArray
	 * JSONArray} of {@link org.json.JSONObject JSONObject}. On error, a HTTP
	 * {@link org.restlet.data.Status#SERVER_ERROR_INTERNAL 500} code is sent.
	 * <p>
	 * The format for the job list will be:
	 * <p>
	 * [{<br>
	 * &nbsp "created":"2010-03-01 15:01:37.0",<br>
	 * &nbsp "localId":"4936233",<br>
	 * &nbsp "submitTime":"2010-03-01 15:01:38.0",<br>
	 * &nbsp "updateToken":"d9463c48-f86b-469f-a037-b00892464d78",<br>
	 * &nbsp "status":"SUBMITTING",<br>
	 * &nbsp "id":2,<br>
	 * &nbsp "endTime":null,<br>
	 * &nbsp "ownerDn":"uid=dooley,ou=People,dc=iplantcollaborative,dc=org",<br>
	 * &nbsp "inputListing":null,<br>
	 * &nbsp "running":true,<br>
	 * &nbsp "lastUpdated":"2010-03-01 15:01:38.0",<br>
	 * &nbsp "system":"slogin2",<br>
	 * &nbsp "startTime":null,<br>
	 * &nbsp "outputListing":null,<br>
	 * &nbsp "name":"test_job"<br>
	 * },{<br>
	 * &nbsp "created":"2010-03-01 15:01:37.0",<br>
	 * &nbsp "localId":"4936232",<br>
	 * &nbsp "submitTime":"2010-03-01 15:01:38.0",<br>
	 * &nbsp "updateToken":"d9463c48-f86b-469f-a037-b00892464d78",<br>
	 * &nbsp "status":"FINISHED",<br>
	 * &nbsp "id":1,<br>
	 * &nbsp "endTime":2010-03-01 15:03:21.0,<br>
	 * &nbsp "ownerDn":"uid=dooley,ou=People,dc=iplantcollaborative,dc=org",<br>
	 * &nbsp "inputListing":null,<br>
	 * &nbsp "running":false,<br>
	 * &nbsp "lastUpdated":"2010-03-01 15:03:21.0",<br>
	 * &nbsp "system":"slogin2",<br>
	 * &nbsp "startTime":"2010-03-01 15:01:40.0",<br>
	 * &nbsp "outputListing":null,<br>
	 * &nbsp "name":"test_job"<br>
	 * }]<br>
	 */
	@Override
	public Representation represent(Variant variant) throws ResourceException
	{

//		Long jobId = null;

		if (!ServiceUtils.isValid(sJobId))
		{
			getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
			return new IplantErrorRepresentation("Job id cannot be empty");
		}
		else
		{
//			try
//			{
//				jobId = Long.valueOf(sJobId);
//			}
//			catch (Exception e)
//			{
//				getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
//				return new IplantErrorRepresentation("Invalid job id");
//			}
		}

		try
		{
			job = JobDao.getByUuid(sJobId, true);
			
			if (job == null || !job.isVisible()) 
			{
			    
				getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND);
				return new IplantErrorRepresentation(
						"Not job found with job id " + sJobId);
			}
			else if (new JobPermissionManager(job, username).canRead(username))
			{
				JSONObject json = new JSONObject();
				
				json.put("id", sJobId);
				json.put("status", job.getStatus().name());
				json.put("_links",
						new JSONObject().put("self", 
								new JSONObject().put("href", TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_JOB_SERVICE) + job.getUuid())));
				return new IplantSuccessRepresentation(json.toString());
			}
			else
			{
				getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN);
				return new IplantErrorRepresentation(
						"User does not have permission to view this job");
			}
		}
		catch (JobException e)
		{
			getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
			return new IplantErrorRepresentation("Invalid job id "
					+ e.getMessage());
		}
		catch (Exception e)
		{
			// can't set a stopped job back to running. Bad request
			getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
			return new IplantErrorRepresentation(e.getMessage());
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
		return false;
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
