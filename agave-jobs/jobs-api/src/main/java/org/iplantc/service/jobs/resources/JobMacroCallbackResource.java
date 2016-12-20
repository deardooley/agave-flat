/**
 * 
 */
package org.iplantc.service.jobs.resources;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.common.representation.IplantErrorRepresentation;
import org.iplantc.service.common.representation.IplantSuccessRepresentation;
import org.iplantc.service.jobs.dao.JobDao;
import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.jobs.model.JobEvent;
import org.iplantc.service.jobs.model.JobUpdateParameters;
import org.iplantc.service.jobs.model.enumerations.JobMacroType;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;

/**
 * Processes user-requested callbacks on running jobs.
 * 
 * @author dooley
 * 
 */
public class JobMacroCallbackResource extends AbstractJobResource 
{
	private static final Logger	log	= Logger.getLogger(JobMacroCallbackResource.class);

	private String				jobUuid;
	private String				token;
	private String				sMacro;

	/**
	 * @param context
	 * @param request
	 * @param response
	 */
	public JobMacroCallbackResource(Context context, Request request, Response response)
	{
		super(context, request, response);

		this.jobUuid = (String) request.getAttributes().get("jobid");
		this.token = (String) request.getAttributes().get("token");
		this.sMacro = (String) request.getAttributes().get("macro");
		
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

//		Long jobId = null;
		if (!StringUtils.isEmpty(jobUuid))
		{
			getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
			return new IplantErrorRepresentation("Invalid job id.");
		} 

		if (!StringUtils.isEmpty(token))
		{
			getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
			return new IplantErrorRepresentation("Invalid token.");
		}
		
		// The HEARTBEAT macro type is used to update the job timestamp and is used
		// by app developers just to keep aware of a job being alive.  The notification
        // macro type is used on job status updates. 
		JobMacroType macro;
		try
		{
			macro = JobMacroType.valueOf(sMacro.toUpperCase());
			if (macro == null)
			{
				getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
				return new IplantErrorRepresentation("Invalid callback macro value " + sMacro);
			}
		}
		catch (Exception e)
		{
			getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
			return new IplantErrorRepresentation("Invalid callback macro value " + sMacro);
		}

		// update the job status if the tokens match
		try
		{
			Job job = JobDao.getByUuid(jobUuid);

			if (job == null) {
				throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, 
						"No job found with job id " + jobUuid);
			}
			else if (job.getUpdateToken().equals(token))
			{
				TenancyHelper.setCurrentEndUser(job.getOwner());
				TenancyHelper.setCurrentTenantId(job.getTenantId());

				if (!job.isRunning())
				{
					// can't set a stopped job back to running. Bad request
					throw new ResourceException(
							Status.CLIENT_ERROR_BAD_REQUEST, "Job " + jobUuid + " is not running.");
				}

                // Update the lastUpdated timestamp on the job record.
                JobDao.update(job.getUuid(), job.getTenantId(), new JobUpdateParameters());
                JobDao.refresh(job);
                
				// Send an event.
				job.addEvent(new JobEvent(macro.name(), macro.getDescription(), job.getOwner()));
				
				getResponse().setStatus(Status.SUCCESS_ACCEPTED);
				return new IplantSuccessRepresentation();
			}
			else
			{
				// can't set a stopped job back to running. Bad request
				throw new ResourceException(Status.CLIENT_ERROR_UNAUTHORIZED,
						"Invalid job id or key");
			}
		}
		catch (ResourceException e)
		{
			log.debug("Callback failed: " + e.getMessage());
			getResponse().setStatus(e.getStatus());
			return new IplantErrorRepresentation(e.getMessage());
		}
		catch (Exception e)
		{
			log.error("Failed to process callback macro " + sMacro + " for job " + jobUuid, e);
			return new IplantErrorRepresentation("Failed to process callback macro " + sMacro + " for job " + jobUuid);
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
