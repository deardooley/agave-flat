/**
 * 
 */
package org.iplantc.service.jobs.resources;

import java.util.Hashtable;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.apps.util.ServiceUtils;
import org.iplantc.service.common.auth.AuthorizationHelper;
import org.iplantc.service.common.clients.AgaveLogServiceClient;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.common.representation.IplantErrorRepresentation;
import org.iplantc.service.common.representation.IplantSuccessRepresentation;
import org.iplantc.service.jobs.dao.JobDao;
import org.iplantc.service.jobs.exceptions.JobDependencyException;
import org.iplantc.service.jobs.exceptions.JobException;
import org.iplantc.service.jobs.exceptions.JobProcessingException;
import org.iplantc.service.jobs.exceptions.JobTerminationException;
import org.iplantc.service.jobs.managers.JobManager;
import org.iplantc.service.jobs.managers.JobPermissionManager;
import org.iplantc.service.jobs.model.Job;
import org.restlet.Context;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Parameter;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;

/**
 * The JobManageResource is the job management interface for users. Through the
 * actions bound to this class, users can obtain individual job
 * description(GET) and kill jobs (DELETE).
 * 
 * @author dooley
 * 
 */
public class JobManageResource extends AbstractJobResource {
	private static final Logger	log	= Logger.getLogger(JobManageResource.class);

	private String				sJobId;
	private String				internalUsername;
	private Job					job;

	/**
	 * @param context
	 * @param request
	 * @param response
	 */
	public JobManageResource(Context context, Request request, Response response)
	{
		super(context, request, response);

		this.username = getAuthenticatedUsername();
		
		this.sJobId = (String) request.getAttributes().get("jobid");
		
		this.internalUsername = (String) context.getAttributes().get("internalUsername");
		
		getVariants().add(new Variant(MediaType.APPLICATION_JSON));
	}
	
	

	/* (non-Javadoc)
	 * @see org.restlet.resource.Resource#acceptRepresentation(org.restlet.resource.Representation)
	 */
	@Override
	public void acceptRepresentation(Representation entity)
			throws ResourceException
	{

		if (!ServiceUtils.isValid(sJobId))
		{
			getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
			getResponse().setEntity(new IplantErrorRepresentation("Job id cannot be empty"));
			return;
		}
		

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
			
			
			if (new JobPermissionManager(job, username).canWrite(username))
			{
				Map<String,String> pTable = super.getPostedEntityAsMap();
				
				if (!pTable.containsKey("action")) {
					throw new JobException("No action specified");
				}
				
//				if (pTable.get("action").equalsIgnoreCase("archive"))
//				{
//					if (!job.isFinished()) {
//						throw new JobException("Job has not reached a finished state. " +
//								"Please wait until the job stops to archive its output");
//					}
//					
//					String itemsToArchive = null;
//					if (!pTable.containsKey("itemsToArchive")) {
//						throw new JobException("itemsToArchive not specified");
//					} else if (!ServiceUtils.isValid(pTable.get("itemsToArchive"))) {
//						throw new JobException("itemsToArchive cannot be empty");
//					}
//					
//					String path = null;
//					if (!pTable.containsKey("path")) {
//						throw new JobException("path not specified");
//					} else if (!ServiceUtils.isValid(pTable.get("path"))) {
//						throw new JobException("path cannot be empty");
//					}
//				}
				else if (pTable.get("action").equalsIgnoreCase("restore")) {
					
					AgaveLogServiceClient.log(AgaveLogServiceClient.ServiceKeys.JOBS02.name(), 
							AgaveLogServiceClient.ActivityKeys.JobRestore.name(), 
							username, "", getRequest().getClientInfo().getUpstreamAddress());
					
					try {
						Job restoredJob = JobManager.restore(job.getId(), getAuthenticatedUsername());
						
						getResponse().setEntity(new IplantSuccessRepresentation(restoredJob.toJSON()));
						getResponse().setStatus(Status.SUCCESS_OK);
					}
					catch (JobException e) {
						getResponse().setEntity(
								new IplantErrorRepresentation(e.getMessage()));
						getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
						return;
					}
			    	catch (Exception e) {
						getResponse().setEntity(
								new IplantErrorRepresentation("Failed to restore job: " + e.getMessage()));
						getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
						log.error("Failed to restore job " + job.getUuid(), e);
						return;
					}
				}
				if (pTable.get("action").equalsIgnoreCase("resubmit"))
				{
					AgaveLogServiceClient.log(AgaveLogServiceClient.ServiceKeys.JOBS02.name(), 
							AgaveLogServiceClient.ActivityKeys.JobsResubmit.name(), 
							username, "", getRequest().getClientInfo().getUpstreamAddress());
					
					try
					{
						boolean ignoreInputConflicts = true;
						if (pTable.containsKey("ignoreInputConflicts")) {
							String sIgnoreInputConficts = pTable.get("ignoreInputConflicts");
							ignoreInputConflicts = StringUtils.equalsIgnoreCase("true", sIgnoreInputConficts) || 
									StringUtils.equalsIgnoreCase("1", sIgnoreInputConficts);
						}
						
						boolean ignoreParameterConflicts = true;
						if (pTable.containsKey("ignoreParameterConflicts")) {
							String sIgnoreParameterConflicts = pTable.get("ignoreParameterConflicts");
							ignoreParameterConflicts = StringUtils.equalsIgnoreCase("true", sIgnoreParameterConflicts) || 
									StringUtils.equalsIgnoreCase("1", sIgnoreParameterConflicts);
						}
						
						boolean preserveNotifications = true;
						if (pTable.containsKey("preserveNotifications")) {
							String sPreserveNotifications = pTable.get("preserveNotifications");
							preserveNotifications = StringUtils.equalsIgnoreCase("true", sPreserveNotifications) || 
									StringUtils.equalsIgnoreCase("1", sPreserveNotifications);
						}
						
				         
						Job jobToResubmit = JobManager.resubmitJob(job, 
																   username, 
																   internalUsername,
																   ignoreInputConflicts,
																   ignoreParameterConflicts);
						
						getResponse().setEntity(
								new IplantSuccessRepresentation(jobToResubmit.toJSON()));
						getResponse().setStatus(Status.SUCCESS_ACCEPTED);
						
					}
					catch (JobProcessingException e) {
						getResponse().setEntity(
								new IplantErrorRepresentation(e.getMessage()));
						getResponse().setStatus(Status.valueOf(e.getStatus()));
						return;
					}
			    	catch (Exception e) {
						getResponse().setEntity(
								new IplantErrorRepresentation("Failed to submit job: " + e.getMessage()));
						getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
						log.error("Job resubmission failed for user " + username, e);
						return;
					}
				}
				else if (pTable.get("action").equalsIgnoreCase("stop"))
				{
					AgaveLogServiceClient.log(AgaveLogServiceClient.ServiceKeys.JOBS02.name(), 
							AgaveLogServiceClient.ActivityKeys.JobsKill.name(), 
							username, "", getRequest().getClientInfo().getUpstreamAddress());
					
					try
					{
						JobManager.kill(job);
							
						getResponse().setEntity(new IplantSuccessRepresentation());
						getResponse().setStatus(Status.SUCCESS_OK);
					}
					catch (JobException e)
					{
						getResponse().setEntity(
								new IplantErrorRepresentation("Failed to kill remote job. " + e.getMessage()));
						getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);

					}
					catch (Exception e)
					{
						getResponse().setEntity(
								new IplantErrorRepresentation("Job deletion failed"));
						getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
						log.error("Job deletion failed for user " + username, e);
					}
				}
				// reset the job to the previous active status
				else if (pTable.get("action").equalsIgnoreCase("reset"))
				{
					AgaveLogServiceClient.log(AgaveLogServiceClient.ServiceKeys.JOBS02.name(), 
							AgaveLogServiceClient.ActivityKeys.JobReset.name(), 
							username, "", getRequest().getClientInfo().getUpstreamAddress());
					
					try
					{
						// only tenant admins can roll back jobs atm.
						if (AuthorizationHelper.isTenantAdmin(getAuthenticatedUsername())) {
							Job updatedJob = JobManager.resetToPreviousState(job, getAuthenticatedUsername());
								
							getResponse().setEntity(new IplantSuccessRepresentation(updatedJob.toJSON()));
							getResponse().setStatus(Status.SUCCESS_OK);
						}
						else {
							getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN);
							getResponse().setEntity(new IplantErrorRepresentation(
									"User does not have permission to roll back this job"));
							return;
						}
					}
					catch (JobDependencyException e) {
						getResponse().setEntity(
								new IplantErrorRepresentation(e.getMessage()));
						getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
					}
					catch (JobException e)
					{
						getResponse().setEntity(
								new IplantErrorRepresentation(e.getMessage()));
						getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
						log.error("Failed to reset job to previous active state.", e);
					}
					catch (Exception e)
					{
						getResponse().setEntity(
								new IplantErrorRepresentation("Job deletion failed"));
						getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
						log.error("Job reset failed for user " + username, e);
					}
				}
				
				else
				{
					throw new JobException("Invalid action " + pTable.get("action"));
				}
			}
			else
			{
				getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN);
				getResponse().setEntity(new IplantErrorRepresentation(
						"User does not have permission to view this job"));
				return;
			}
		}
		catch (ResourceException e)
		{
			getResponse().setStatus(e.getStatus());
			getResponse().setEntity(new IplantErrorRepresentation(
					e.getMessage()));
			return;
		}
		catch (JobException e)
		{
			getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
			getResponse().setEntity(new IplantErrorRepresentation(e.getMessage()));
		}
		catch (Exception e)
		{
			// can't set a stopped job back to running. Bad request
			getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
			getResponse().setEntity(new IplantErrorRepresentation(e.getMessage()));
		}
	}



	/**
	 * This method represents the HTTP GET action. Using the job id from the
	 * URL, the job information is retrieved from the databse and sent to the
	 * user as a {@link org.json.JSONObject JSONObject}. If the job id is
	 * invalid for any reason, a HTTP
	 * {@link org.restlet.data.Status#CLIENT_ERROR_BAD_REQUEST 400} code is
	 * sent. If an internal error occurs due to connectivity issues, etc, a
	 * {@link org.restlet.data.Status#SERVER_ERROR_INTERNAL 500} code is sent.
	 */
	@Override
	public Representation represent(Variant variant) throws ResourceException
	{

		AgaveLogServiceClient.log(AgaveLogServiceClient.ServiceKeys.JOBS02.name(), 
				AgaveLogServiceClient.ActivityKeys.JobsGetByID.name(), 
				username, "", getRequest().getClientInfo().getUpstreamAddress());
		
		if (!ServiceUtils.isValid(sJobId))
		{
			getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
			return new IplantErrorRepresentation("Job id cannot be empty");
		}

		try
		{
			job = JobDao.getByUuid(sJobId);
			if (job == null || !job.isVisible()) 
			{
				getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND);
				return new IplantErrorRepresentation("No job found with job id " + sJobId);
			}
			else if (new JobPermissionManager(job, username).canRead(username))
			{
				return new IplantSuccessRepresentation(job.toJSON());
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

	/**
	 * This method represents the HTTP DELETE action. Using this method, the
	 * user can effectively kill a running job. If the job id included in the
	 * URL is invalid, a HTTP
	 * {@link org.restlet.data.Status#CLIENT_ERROR_BAD_REQUEST 400} code is
	 * sent. If a problem occurs killing the job a HTTP
	 * {@link org.restlet.data.Status#SERVER_ERROR_INTERNAL 500} code is sent.
	 */
	@Override
	public void removeRepresentations()
	{
		AgaveLogServiceClient.log(AgaveLogServiceClient.ServiceKeys.JOBS02.name(), 
				AgaveLogServiceClient.ActivityKeys.JobsDelete.name(), 
				username, "", getRequest().getClientInfo().getUpstreamAddress());
		
		if (!ServiceUtils.isValid(sJobId))
		{
			getResponse().setEntity(
					new IplantErrorRepresentation("Job id cannot be empty"));
			getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
			return;
		}
		
		try
		{
			job = JobDao.getByUuid(sJobId);
			
			if (job == null || !job.isVisible()) {
				getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND);
				getResponse().setEntity(new IplantErrorRepresentation(
						"No job found with job id " + sJobId));
			}
			else if (new JobPermissionManager(job, username).canWrite(username))
			{
				JobManager.hide(job.getId(), username);
				
				getResponse().setEntity(new IplantSuccessRepresentation());
				getResponse().setStatus(Status.SUCCESS_OK);
			}
			else
			{
				getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN);
				getResponse().setEntity(new IplantErrorRepresentation(
						"User does not have permission to view this job"));
			}
		}
		catch (JobException | JobTerminationException e)
		{
			getResponse().setEntity(
					new IplantErrorRepresentation(e.getMessage()));
			getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
			log.error("Failed to hide job " + sJobId, e);
		}
		catch (Throwable e)
		{
			getResponse().setEntity(
					new IplantErrorRepresentation("Job deletion failed"));
			getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
			log.error("Failed to hide job " + sJobId, e);
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

	@SuppressWarnings("unused")
	private Hashtable<String, String> parseForm(Form form)
	{
		Hashtable<String, String> table = new Hashtable<String, String>();

		for (Parameter p : form)
		{
			// boolean foundKey = false;
			String key = "";
			String[] lines = p.getValue().split("\\n");
			for (String line : lines)
			{
				if (line.indexOf(",") == 0)
				{
					line = line.substring(2);
				}
				line = line.replaceAll("\\r", "");
				if (line.startsWith("--") || line.equals(""))
				{
					continue;
				}
				else
				{
					if (line.startsWith("\""))
					{
						key = line.replaceAll("\"", "");
					}
					else if (line.startsWith("Content-Disposition"))
					{
						key = line.substring(line.indexOf("=") + 1);
						key = key.replaceAll("\"", "");
						// foundKey = true;
					}
					else
					{
						table.put(key, line);
					}
				}
			}
		}
		return table;
	}

}
