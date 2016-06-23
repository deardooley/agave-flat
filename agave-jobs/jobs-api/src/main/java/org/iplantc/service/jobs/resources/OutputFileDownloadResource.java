/**
 *
 */
package org.iplantc.service.jobs.resources;


import java.util.List;

import javax.activation.MimetypesFileTypeMap;

import org.apache.commons.io.FilenameUtils;
import org.iplantc.service.apps.dao.SoftwareDao;
import org.iplantc.service.apps.model.Software;
import org.iplantc.service.apps.model.enumerations.ExecutionType;
import org.iplantc.service.apps.util.ServiceUtils;
import org.iplantc.service.common.clients.AgaveLogServiceClient;
import org.iplantc.service.common.representation.IplantErrorRepresentation;
import org.iplantc.service.common.representation.RemoteDataWriterRepresentation;
import org.iplantc.service.io.dao.LogicalFileDao;
import org.iplantc.service.io.manager.FileEventProcessor;
import org.iplantc.service.io.model.FileEvent;
import org.iplantc.service.io.model.LogicalFile;
import org.iplantc.service.io.model.enumerations.FileEventType;
import org.iplantc.service.jobs.dao.JobDao;
import org.iplantc.service.jobs.exceptions.JobException;
import org.iplantc.service.jobs.managers.JobManager;
import org.iplantc.service.jobs.managers.JobPermissionManager;
import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.jobs.util.DataLocator;
import org.iplantc.service.systems.exceptions.SystemException;
import org.iplantc.service.systems.model.RemoteSystem;
import org.iplantc.service.systems.model.enumerations.SystemStatusType;
import org.iplantc.service.transfer.RemoteDataClient;
import org.iplantc.service.transfer.RemoteFileInfo;
import org.iplantc.service.transfer.dao.TransferTaskDao;
import org.iplantc.service.transfer.exceptions.RemoteDataException;
import org.iplantc.service.transfer.model.TransferTask;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Range;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;


/**
 * Class to handle get and post requests for jobs
 *
 * @author dooley
 *
 */
public class OutputFileDownloadResource extends AbstractJobResource
{
	private String				path;
	private String				sJobId;
	private List<Range> ranges = null;	// ranges of the file to return, given by byte index for start and a size.

	/**
	 * @param context
	 * @param request
	 * @param response
	 */
	public OutputFileDownloadResource(Context context, Request request,
			Response response)
	{
		super(context, request, response);

		this.username = getAuthenticatedUsername();

		sJobId = (String) request.getAttributes().get("jobid");

		path = getFilePathFromURL();

		this.ranges = (List<Range>)request.getRanges();

		getVariants().add(new Variant(MediaType.APPLICATION_JSON));

		AgaveLogServiceClient.log(AgaveLogServiceClient.ServiceKeys.JOBS02.name(),
				AgaveLogServiceClient.ActivityKeys.JobsGetOutput.name(),
				username, "", getRequest().getClientInfo().getUpstreamAddress());

	}

	/**
	 * This method represents the HTTP GET action. Using the job id and output
	 * file name from the URL, the file streamed from the file system to the
	 * user. If the job id is invalid for any reason, a HTTP
	 * {@link org.restlet.data.Status#CLIENT_ERROR_NOT_FOUND 404} code is sent.
	 * If the file is not present, a
	 * {@link org.restlet.data.Status#SERVER_ERROR_INTERNAL 500} is sent.
	 */
	@Override
	public Representation represent(Variant variant) throws ResourceException
	{
		if (!ServiceUtils.isValid(path))
		{
			getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
			return new IplantErrorRepresentation("Invalid file name");
		}

		RemoteDataClient remoteDataClient = null;
		try
		{
			Job job = JobDao.getByUuid(sJobId, true);

			if (job == null || !job.isVisible())
			{
				throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Invalid job id");
			}

			JobPermissionManager pemManager = new JobPermissionManager(job, username);
			if (!pemManager.canRead(username)) { throw new ResourceException(
					Status.CLIENT_ERROR_UNAUTHORIZED,
					"User does not have permission to view this resource."); }

			String remotePath = null;

			DataLocator dataLocator = new DataLocator(job);

			RemoteSystem jobDataSystem = dataLocator.findOutputSystemForJobData();
			
			if (!jobDataSystem.isAvailable()) {
			    throw new ResourceException(Status.SERVER_ERROR_GATEWAY_TIMEOUT, "Job output system " 
	                    + jobDataSystem.getSystemId() + " is not currently available");
	        } 
	        // if the system is in down time or otherwise unavailable...
	        else if (jobDataSystem.getStatus() != SystemStatusType.UP)
	        {
	            throw new ResourceException(Status.SERVER_ERROR_GATEWAY_TIMEOUT, "Job output system " 
	                    + jobDataSystem.getSystemId() + " is currently " 
	                    + jobDataSystem.getStatus());
	        }
			
			remoteDataClient = jobDataSystem.getRemoteDataClient(job.getInternalUsername());
			remoteDataClient.authenticate();

			if (!JobManager.isJobDataFullyArchived(job))
			{
				Software software = SoftwareDao.getSoftwareByUniqueName(job.getSoftwareName());

				if (software == null) { throw new JobException(
						"Failed to find the software record for this job"); }

				if (software.getExecutionType().name().equals(ExecutionType.HPC.name()) ||
						software.getExecutionType().name().equals(ExecutionType.CLI.name()))
				{
					try
					{
						if (!ServiceUtils.isValid(job.getWorkPath()))
						{
							throw new RemoteDataException("No work directory specified for this job. Usually this occurs when a job failed during submission.");
						}
						else
						{
							if (remoteDataClient.doesExist(job.getWorkPath() + path))
							{
								remotePath = job.getWorkPath() + path;
							} else if (!remoteDataClient.doesExist(job.getWorkPath())) {
								throw new RemoteDataException("File/folder does not exist");
							} else {
								throw new RemoteDataException(
									"Unable to locate job data. Work folder no longer exists.");
							}
						}
					}
					catch (RemoteDataException e)
					{
						throw e;
					}
					catch (Exception e)
					{
						throw new RemoteDataException(
								"Failed to list output folder " + job.getWorkPath() + path
										+ " for job " + job.getUuid(), e);
					}
				}
				else if (software.getExecutionType().name().equals(ExecutionType.ATMOSPHERE.name()))
				{ // job is an atmo job
					throw new RemoteDataException(
							"Data cannot be retrieved from Atmosphere systems.");
				}
				else if (software.getExecutionType().name().equals(ExecutionType.CONDOR.name()))
				{
					if (job.isFinished() && !job.isArchiveOutput()) {
						throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
							"Job was not archived and data cannot be retrieved from condor systems.");
					} else {
						throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
							"Data cannot be retrieved from condor systems.");
					}
				}
				else
				{
					throw new RemoteDataException("Unknown execution system type.");
				}
			}
			else if (job.isArchiveOutput())
			{
				remotePath = job.getArchivePath() + path;
			}

			String mimetype = new MimetypesFileTypeMap().getContentType(FilenameUtils.getName(remotePath));

			if (jobDataSystem != null && jobDataSystem.isAvailable())
			{
				if (ranges.size() > 0)
				{
                    Range range = ranges.get(0);

                    if (range.getSize() < 0 && range.getSize() != -1) {
                    	throw new ResourceException(
                    			new Status(416, "Requested Range Not Satisfiable",
                    					"Upper bound less than lower bound",
                    					"http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html"),
                    			"Specified Range upper bound less than lower bound");
                    }

                    if (range.getIndex() > remoteDataClient.length(remotePath)) {
                    	throw new ResourceException(
                        		new Status(416, "Requested Range Not Satisfiable",
                        				"Lower bound out of range of file",
                        				"http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html"),
                    			"Specified Range lower bound outside bounds of file");
                    }

                    if ((range.getIndex() + Math.abs(range.getSize())) > remoteDataClient.length(remotePath)) {
                        getResponse().setStatus(
                        		new Status(416, "Requested Range Not Satisfiable",
	                            		"Upper bound out of range of file",
	                            		"http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html"),
                        		"Specified Range upper bound outside bounds of file");
                    }
                }

				TransferTask transferTask = new TransferTask(
						"agave://" + jobDataSystem.getSystemId() + "/" + remotePath,
						"http://" + getRequest().getClientInfo().getUpstreamAddress() + "/",
						username, null, null);
				
				TransferTaskDao.persist(transferTask);
				
				LogicalFile lf = LogicalFileDao.findBySystemAndPath(jobDataSystem, remoteDataClient.resolvePath(remotePath));
				
				if (lf != null) {
				    FileEventProcessor.processAndSaveContentEvent(lf, new FileEvent(FileEventType.DOWNLOAD, "File downloaded", username, transferTask));
				}

				return new RemoteDataWriterRepresentation(
						jobDataSystem, null, remotePath, new MediaType(mimetype), ranges.isEmpty() ? null : ranges.get(0), transferTask);
			} else {
				throw new SystemException("The submission system for this job is no longer available");
			}
		}
		catch (ResourceException e)
		{
			getResponse().setStatus(e.getStatus());
			return new IplantErrorRepresentation(e.getMessage());
		}
		catch (Exception e)
		{
			getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
			return new IplantErrorRepresentation(e.getMessage());
		}
		finally {
			try { remoteDataClient.disconnect(); } catch (Exception e) {}
		}
	}
	
	/* (non-Javadoc)
	 * @see org.restlet.Handler#handleHead()
	 */
	@Override
	public void handleHead() {
		
		RemoteDataClient remoteDataClient = null;
		try
		{
			if (!ServiceUtils.isValid(path)) {
				throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Invalid file name");
			}

			
			Job job = JobDao.getByUuid(sJobId, true);

			if (job == null || !job.isVisible()) {
				throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Invalid job id");
			}

			JobPermissionManager pemManager = new JobPermissionManager(job, username);
			if (!pemManager.canRead(username)) { throw new ResourceException(
					Status.CLIENT_ERROR_UNAUTHORIZED,
					"User does not have permission to view this resource."); }

			String remotePath = null;

			DataLocator dataLocator = new DataLocator(job);

			RemoteSystem jobDataSystem = dataLocator.findOutputSystemForJobData();
			
			if (!jobDataSystem.isAvailable()) {
			    throw new ResourceException(Status.SERVER_ERROR_GATEWAY_TIMEOUT, "Job output system " 
	                    + jobDataSystem.getSystemId() + " is not currently available");
	        } 
	        // if the system is in down time or otherwise unavailable...
	        else if (jobDataSystem.getStatus() != SystemStatusType.UP)
	        {
	            throw new ResourceException(Status.SERVER_ERROR_GATEWAY_TIMEOUT, "Job output system " 
	                    + jobDataSystem.getSystemId() + " is currently " 
	                    + jobDataSystem.getStatus());
	        }
			
			remoteDataClient = jobDataSystem.getRemoteDataClient(job.getInternalUsername());
			remoteDataClient.authenticate();

			if (!JobManager.isJobDataFullyArchived(job))
			{
				Software software = SoftwareDao.getSoftwareByUniqueName(job.getSoftwareName());

				if (software == null) { throw new JobException(
						"Failed to find the software record for this job"); }

				if (software.getExecutionType().name().equals(ExecutionType.HPC.name()) ||
						software.getExecutionType().name().equals(ExecutionType.CLI.name()))
				{
					try
					{
						if (!ServiceUtils.isValid(job.getWorkPath()))
						{
							throw new RemoteDataException("No work directory specified for this job. Usually this occurs when a job failed during submission.");
						}
						else
						{
							if (remoteDataClient.doesExist(job.getWorkPath() + path))
							{
								remotePath = job.getWorkPath() + path;
							} else if (!remoteDataClient.doesExist(job.getWorkPath())) {
								throw new RemoteDataException("File/folder does not exist");
							} else {
								throw new RemoteDataException(
									"Unable to locate job data. Work folder no longer exists.");
							}
						}
					}
					catch (RemoteDataException e)
					{
						throw e;
					}
					catch (Exception e)
					{
						throw new RemoteDataException(
								"Failed to list output folder " + job.getWorkPath() + path
										+ " for job " + job.getUuid(), e);
					}
				}
				else if (software.getExecutionType().name().equals(ExecutionType.ATMOSPHERE.name()))
				{ // job is an atmo job
					throw new RemoteDataException(
							"Data cannot be retrieved from Atmosphere systems.");
				}
				else if (software.getExecutionType().name().equals(ExecutionType.CONDOR.name()))
				{
					if (job.isFinished() && !job.isArchiveOutput()) {
						throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
							"Job was not archived and data cannot be retrieved from condor systems.");
					} else {
						throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
							"Data cannot be retrieved from condor systems.");
					}
				}
				else
				{
					throw new RemoteDataException("Unknown execution system type.");
				}
			}
			else if (job.isArchiveOutput())
			{
				remotePath = job.getArchivePath() + path;
			}

			String mimetype = new MimetypesFileTypeMap().getContentType(FilenameUtils.getName(remotePath));

			if (jobDataSystem != null && jobDataSystem.isAvailable())
			{
				RemoteFileInfo remoteFileInfo = remoteDataClient.getFileInfo(remotePath);
				if (ranges.size() > 0)
				{
                    Range range = ranges.get(0);

                    if (range.getSize() < 0 && range.getSize() != -1) {
                    	throw new ResourceException(
                    			new Status(416, "Requested Range Not Satisfiable",
                    					"Upper bound less than lower bound",
                    					"http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html"),
                    			"Specified Range upper bound less than lower bound");
                    }

                    if (range.getIndex() > remoteFileInfo.getSize()) {
                    	throw new ResourceException(
                        		new Status(416, "Requested Range Not Satisfiable",
                        				"Lower bound out of range of file",
                        				"http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html"),
                    			"Specified Range lower bound outside bounds of file");
                    }

                    if ((range.getIndex() + Math.abs(range.getSize())) > remoteFileInfo.getSize()) {
                        getResponse().setStatus(
                        		new Status(416, "Requested Range Not Satisfiable",
	                            		"Upper bound out of range of file",
	                            		"http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html"),
                        		"Specified Range upper bound outside bounds of file");
                    }
                }

				TransferTask transferTask = new TransferTask(
						"agave://" + jobDataSystem.getSystemId() + "/" + remotePath,
						"http://" + getRequest().getClientInfo().getUpstreamAddress() + "/",
						username, null, null);
				
				TransferTaskDao.persist(transferTask);
				
				LogicalFile lf = LogicalFileDao.findBySystemAndPath(jobDataSystem, remoteDataClient.resolvePath(remotePath));
				
				if (lf != null) {
				    FileEventProcessor.processAndSaveContentEvent(lf, new FileEvent(FileEventType.DOWNLOAD, "File downloaded", username, transferTask));
				}
				
				Representation wrep = Representation.createEmpty();
				wrep.setSize(remoteFileInfo.getSize());
				wrep.setMediaType(MediaType.valueOf(mimetype));
				// Only supporting the first range specified for each GET request
				if (!ranges.isEmpty())
					wrep.setRange(ranges.get(0));
				
				wrep.setModificationDate(remoteFileInfo.getLastModified());
				wrep.setDownloadable(true);
				wrep.setDownloadName(remoteFileInfo.getName());
				getResponse().setEntity(wrep);
			} else {
				throw new SystemException("The submission system for this job is no longer available");
			}
		}
		catch (ResourceException e)
		{
			getResponse().setEntity(new IplantErrorRepresentation(e.getMessage()));
			getResponse().setStatus(e.getStatus());
		}
		catch (Exception e)
		{
			getResponse().setEntity(new IplantErrorRepresentation(e.getMessage()));
			getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
		}
		finally {
			try {remoteDataClient.disconnect();} catch (Exception e) {}
		}
	}
	

	private String getFilePathFromURL()
	{
		String path = getRequest().getOriginalRef().toUri().getPath();

		path = path.substring(path.indexOf("outputs/media") + "outputs/media".length());

		path = "/" + path;

		path = path.replaceAll("\\.\\.", "").replaceAll("//", "/").replaceAll("~","");

		return path;
	}
	
	/* (non-Javadoc)
	 * @see org.restlet.resource.Resource#allowDelete()
	 */
	@Override
	public boolean allowHead() {
		return true;
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
