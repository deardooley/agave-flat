/**
 * 
 */
package org.iplantc.service.jobs.resources;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.iplantc.service.apps.util.ServiceUtils;
import org.iplantc.service.common.clients.AgaveLogServiceClient;
import org.iplantc.service.common.clients.AgaveProfileServiceClient;
import org.iplantc.service.common.representation.IplantErrorRepresentation;
import org.iplantc.service.common.representation.IplantSuccessRepresentation;
import org.iplantc.service.jobs.Settings;
import org.iplantc.service.jobs.dao.JobDao;
import org.iplantc.service.jobs.dao.JobPermissionDao;
import org.iplantc.service.jobs.exceptions.JobException;
import org.iplantc.service.jobs.managers.JobPermissionManager;
import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.jobs.model.JobPermission;
import org.iplantc.service.jobs.model.enumerations.PermissionType;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;

/**
 * The JobResource object enables HTTP GET and POST actions on contrast jobs.
 * This resource is primarily used for submitting jobs and viewing a sample HTML
 * job submission form.
 * 
 * @author dooley
 * 
 */
public class JobPermissionsResource extends AbstractJobResource 
{
	private String	sJobId;  // job id
	private String sharedUsername; // user receiving permissions
	
	/**
	 * @param context
	 * @param request
	 * @param response
	 */
	public JobPermissionsResource(Context context, Request request, Response response)
	{
		super(context, request, response);

		this.username = getAuthenticatedUsername();
		
		this.sJobId = (String) request.getAttributes().get("jobid");

		this.sharedUsername = (String) request.getAttributes().get("user");
		
		getVariants().add(new Variant(MediaType.APPLICATION_JSON));

		AgaveLogServiceClient.log(AgaveLogServiceClient.ServiceKeys.JOBS02.name(), 
				AgaveLogServiceClient.ActivityKeys.JobsShare.name(), 
				username, "", getRequest().getClientInfo().getUpstreamAddress());
		
		
	}

	/**
	 * This method represents the HTTP GET action. Without specifying a job
	 * handle, there is no job information to retrieve, so we bind this action
	 * to simply serving a static HTML form for job submission.
	 */
	@Override
	public Representation represent(Variant variant) throws ResourceException
	{
		if (!ServiceUtils.isValid(sJobId))
		{
			getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
			return new IplantErrorRepresentation("Job id cannot be empty");
		}
		
		try
		{
			Job job = JobDao.getByUuid(sJobId, true);
			if (job == null || !job.isVisible()) {
				getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND);
				return new IplantErrorRepresentation("No job found with job id " + sJobId);
			}
			
			JobPermissionManager pm = new JobPermissionManager(job, username);
			if (pm.canRead(username))
			{
				List<JobPermission> permissions = JobPermissionDao.getByJobId(job.getId());
				
				String jPems = "";
				
				if (StringUtils.isEmpty(sharedUsername)) 
				{
					jPems = new JobPermission(job, job.getOwner(), PermissionType.ALL).toJSON(job);
					
					for (int i=offset; i< Math.min((limit+offset), permissions.size()); i++)
					{
						jPems += "," + permissions.get(i).toJSON(job);
					}
					
					jPems = "[" + jPems + "]";
				} 
				else 
				{
					boolean found = false;
					if (StringUtils.equals(sharedUsername, job.getOwner()))
					{
						jPems = new JobPermission(job, job.getOwner(), PermissionType.ALL).toJSON(job);
					}
					else
					{
						for (int i=offset; i< Math.min((limit+offset), permissions.size()); i++)
						{
							JobPermission pem = permissions.get(i);
							if (pem.getUsername().equalsIgnoreCase(sharedUsername)) {
								found = true;
								jPems = pem.toJSON(job);
								break;
							}
						}
						
						if (!found) {
							throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
									"No permissions found for user " + sharedUsername);
						}
					}
				}

				return new IplantSuccessRepresentation(jPems);
			}
			else
			{
				throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
						"User does not have permission to view this resource");
			}
		}
		catch (ResourceException e)
		{
			getResponse().setStatus(e.getStatus());
			return new IplantErrorRepresentation(e.getMessage());
		}
		catch (Exception e)
		{
			// can't set a stopped job back to running. Bad request
			getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
			return new IplantErrorRepresentation(e.getMessage());
		}
	}

	/**
	 * Post action for adding (and overwriting) permissions on a job object
	 * 
	 */
	@Override
	public void acceptRepresentation(Representation entity)
	{
		if (!ServiceUtils.isValid(sJobId))
		{
			getResponse().setEntity(
					new IplantErrorRepresentation("Job id cannot be empty"));
			getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
			return;

		}
		
		Job job = null;
		try
		{
			job = JobDao.getByUuid(sJobId);
			if (job == null) {
				getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND);
				getResponse().setEntity(new IplantErrorRepresentation(
						"No job found with job id " + sJobId));
			}

			if (job == null) { 
				throw new ResourceException(
					Status.CLIENT_ERROR_BAD_REQUEST, "Invalid job id"); 
			}
		}
		catch (Exception e)
		{
			getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
			getResponse().setEntity(
					new IplantErrorRepresentation(e.getMessage()));
			return;
		}

		if (entity != null)
		{	
			try
			{
				Map<String,String> postContentData = super.getPostedEntityAsMap();
			
				String name = null;
				if (StringUtils.isEmpty(sharedUsername)) 
				{
					 name = postContentData.get("username");
				} 
				else if (postContentData.containsKey("username") && 
						!StringUtils.equalsIgnoreCase(postContentData.get("username"), sharedUsername)) 
				{
					throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
							"The username value in the POST body, " + postContentData.get("username") + 
                			", does not match the username in the URL, " + sharedUsername);         
				}
				else
				{
					name = sharedUsername;
				}
				
				if (StringUtils.isEmpty(name) || StringUtils.equals(name, "null")) { 
					throw new ResourceException(
						Status.CLIENT_ERROR_BAD_REQUEST, "No user found matching " + name); 
				} 
				else 
				{
					// validate the user they are giving permissions to exists
					AgaveProfileServiceClient authClient = new AgaveProfileServiceClient(
							Settings.IPLANT_PROFILE_SERVICE, 
							Settings.IRODS_USERNAME, 
							Settings.IRODS_PASSWORD);
					
					if (authClient.getUser(name) == null) {
						throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
							"No user found matching " + name);
					}
				}
				
				String sPermission = null;
				if (postContentData.containsKey("permission")) {
					sPermission = postContentData.get("permission");
				} else {
					throw new ResourceException(
							Status.CLIENT_ERROR_BAD_REQUEST,
							"Missing permission field.");
				}
				
				JobPermissionManager pm = new JobPermissionManager(job, username);

				if (pm.canWrite(username))
				{
					// if the permission is null or empty, the permission
					// will be removed
					try 
					{
						pm.setPermission(name, sPermission);
						if (StringUtils.isEmpty(sPermission)) {
							getResponse().setStatus(Status.SUCCESS_OK);
						} else {
							getResponse().setStatus(Status.SUCCESS_CREATED);
						}
						
						JobPermission pem = JobPermissionDao.getByUsernameAndJobId(name, job.getId());
						if (pem == null) {
							pem = new JobPermission(job, name, PermissionType.NONE);
						}
						
						getResponse().setEntity(
							new IplantSuccessRepresentation(pem.toJSON(job)));
					} 
					catch (IllegalArgumentException iae) {
						throw new ResourceException(
								Status.CLIENT_ERROR_BAD_REQUEST,
								"Invalid permission value. Valid values are: " + PermissionType.supportedValuesAsString());
					}
				}
				else
				{
					throw new ResourceException(
							Status.CLIENT_ERROR_FORBIDDEN,
							"User does not have permission to modify this resource.");
				}
			}
			catch (ResourceException e)
			{
				getResponse().setEntity(
						new IplantErrorRepresentation(e.getMessage()));
				getResponse().setStatus(e.getStatus());
			}
			catch (Exception e)
			{
				getResponse().setEntity(
						new IplantErrorRepresentation("Failed to update job permissions: " + e.getMessage()));
				getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
			}
		}
		else
		{
			getResponse().setEntity(
					new IplantErrorRepresentation(
							"Post request with no entity"));
			getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.restlet.resource.Resource#removeRepresentations()
	 */
	@Override
	public void removeRepresentations() throws ResourceException
	{
		if (!ServiceUtils.isValid(sJobId))
		{
			getResponse().setEntity(
					new IplantErrorRepresentation("Job id cannot be empty"));
			getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
			return;

		}
		
		Job job = null;
		try
		{
			job = JobDao.getByUuid(sJobId);
			if (job == null || !job.isVisible()) 
			{
				getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND);
				getResponse().setEntity(new IplantErrorRepresentation(
						"No job found with job id " + sJobId));
				return;
			}
		}
		catch (Exception e)
		{
			getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
			getResponse().setEntity(
					new IplantErrorRepresentation(e.getMessage()));
			return;
		}
		
		try
		{
			JobPermissionManager pm = new JobPermissionManager(job, username);

			if (pm.canWrite(username))
			{
				if (StringUtils.isEmpty(sharedUsername)) {
					// clear all permissions
					pm.clearPermissions();
				} else { // clear pems for user
					pm.setPermission(sharedUsername, null);
				}
				
				getResponse().setEntity(new IplantSuccessRepresentation());
			}
			else
			{
				throw new ResourceException(
						Status.CLIENT_ERROR_FORBIDDEN,
						"User does not have permission to modify this resource.");
			}
		}
		catch (ResourceException e) {
			getResponse().setEntity(
					new IplantErrorRepresentation(e.getMessage()));
			getResponse().setStatus(e.getStatus());
		}
		catch (JobException e) {
			getResponse().setEntity(
					new IplantErrorRepresentation(e.getMessage()));
			getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
		}
		catch (Exception e)
		{
			getResponse().setEntity(
					new IplantErrorRepresentation("Failed to remove job permissions: " + e.getMessage()));
			getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
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
		return true;
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

	/**
	 * Allow the resource to be modified
	 * 
	 * @return
	 */
	public boolean setModifiable()
	{
		return true;
	}

	/**
	 * Allow the resource to be read
	 * 
	 * @return
	 */
	public boolean setReadable()
	{
		return true;
	}
}
