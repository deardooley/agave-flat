/**
 *
 */
package org.iplantc.service.jobs.managers;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.log4j.Logger;
import org.hibernate.StaleObjectStateException;
import org.hibernate.UnresolvableObjectException;
import org.iplantc.service.apps.dao.SoftwareDao;
import org.iplantc.service.apps.exceptions.SoftwareException;
import org.iplantc.service.apps.managers.ApplicationManager;
import org.iplantc.service.apps.model.Software;
import org.iplantc.service.apps.model.SoftwareInput;
import org.iplantc.service.apps.model.SoftwareParameter;
import org.iplantc.service.apps.model.enumerations.SoftwareParameterType;
import org.iplantc.service.apps.util.ServiceUtils;
import org.iplantc.service.common.exceptions.PermissionException;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.common.util.TimeUtils;
import org.iplantc.service.io.dao.LogicalFileDao;
import org.iplantc.service.io.model.LogicalFile;
import org.iplantc.service.io.permissions.PermissionManager;
import org.iplantc.service.jobs.Settings;
import org.iplantc.service.jobs.dao.JobDao;
import org.iplantc.service.jobs.exceptions.JobDependencyException;
import org.iplantc.service.jobs.exceptions.JobException;
import org.iplantc.service.jobs.exceptions.JobProcessingException;
import org.iplantc.service.jobs.exceptions.JobTerminationException;
import org.iplantc.service.jobs.managers.killers.JobKiller;
import org.iplantc.service.jobs.managers.killers.JobKillerFactory;
import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.jobs.model.JobEvent;
import org.iplantc.service.jobs.model.enumerations.JobArchivePathMacroType;
import org.iplantc.service.jobs.model.enumerations.JobEventType;
import org.iplantc.service.jobs.model.enumerations.JobMacroType;
import org.iplantc.service.jobs.model.enumerations.JobStatusType;
import org.iplantc.service.jobs.queue.ZombieJobWatch;
import org.iplantc.service.jobs.util.Slug;
import org.iplantc.service.notification.exceptions.NotificationException;
import org.iplantc.service.notification.model.Notification;
import org.iplantc.service.remote.exceptions.RemoteExecutionException;
import org.iplantc.service.systems.dao.SystemDao;
import org.iplantc.service.systems.exceptions.SystemUnavailableException;
import org.iplantc.service.systems.exceptions.SystemUnknownException;
import org.iplantc.service.systems.manager.SystemManager;
import org.iplantc.service.systems.model.AuthConfig;
import org.iplantc.service.systems.model.BatchQueue;
import org.iplantc.service.systems.model.ExecutionSystem;
import org.iplantc.service.systems.model.RemoteSystem;
import org.iplantc.service.systems.model.enumerations.RemoteSystemType;
import org.iplantc.service.systems.model.enumerations.SystemStatusType;
import org.iplantc.service.transfer.RemoteDataClient;
import org.iplantc.service.transfer.RemoteDataClientFactory;
import org.iplantc.service.transfer.RemoteFileInfo;
import org.iplantc.service.transfer.URLCopy;
import org.iplantc.service.transfer.dao.TransferTaskDao;
import org.iplantc.service.transfer.exceptions.RemoteDataException;
import org.iplantc.service.transfer.exceptions.TransferException;
import org.iplantc.service.transfer.model.TransferTask;
import org.iplantc.service.transfer.model.enumerations.TransferStatusType;
import org.joda.time.DateTime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author dooley
 *
 */
/**
 * @author dooley
 *
 */
public class JobManager {
	private static final Logger	log	= Logger.getLogger(JobManager.class);

	/**
	 * Returns the {@link ExecutionSystem} for the given {@code job}.
	 * @param job
	 * @return a valid exection system or null of it no longer exists.
	 */
	public static ExecutionSystem getJobExecutionSystem(Job job) throws SystemUnavailableException {
	    RemoteSystem jobExecutionSystem = new SystemDao().findBySystemId(job.getSystem());
	    if (jobExecutionSystem == null) {
	        throw new SystemUnavailableException("Job execution system "
                    + job.getSystem() + " is not currently available");
	    } else {
	        return (ExecutionSystem)jobExecutionSystem;
	    }
	}

	/**
     * Returns the {@link Software} for the given {@code job}.
     * @param job
     * @return a valid {@link Software} object or null of it no longer exists.
     */
    public static Software getJobSoftwarem(Job job) {
        return SoftwareDao.getSoftwareByUniqueName(job.getSoftwareName());
    }

	/**
	 * Removes the job work directory in the event staging fails too many times.
	 *
	 * @param job
	 * @throws SystemUnavailableException
	 * @throws JobException
	 */
	public static Job deleteStagedData(Job job) throws JobException
	{
		ExecutionSystem system = (ExecutionSystem) new SystemDao().findBySystemId(job.getSystem());

		if (system == null || !system.isAvailable() || !system.getStatus().equals(SystemStatusType.UP))
		{
			throw new JobException("System " + system.getName() + " is not available for staging.");
		}

		log.debug("Cleaning up staging directory for failed job " + job.getUuid());
		job = JobManager.updateStatus(job, JobStatusType.STAGING_INPUTS, "Cleaning up remote work directory.");

		ExecutionSystem remoteExecutionSystem = null;
		RemoteDataClient remoteExecutionDataClient = null;
		String remoteWorkPath = null;
        try
		{
			// copy to remote execution work directory
			remoteExecutionSystem = (ExecutionSystem)new SystemDao().findBySystemId(job.getSystem());
			remoteExecutionDataClient = remoteExecutionSystem.getRemoteDataClient(job.getInternalUsername());
			remoteExecutionDataClient.authenticate();

			Software software = SoftwareDao.getSoftwareByUniqueName(job.getSoftwareName());

			if (!StringUtils.isEmpty(software.getExecutionSystem().getScratchDir())) {
				remoteWorkPath = software.getExecutionSystem().getScratchDir();
			} else if (!StringUtils.isEmpty(software.getExecutionSystem().getWorkDir())) {
				remoteWorkPath = software.getExecutionSystem().getWorkDir();
			}

			if (!StringUtils.isEmpty(remoteWorkPath)) {
				if (!remoteWorkPath.endsWith("/")) remoteWorkPath += "/";
			} else {
				remoteWorkPath = "";
			}

			remoteWorkPath += job.getOwner() +
					"/job-" + job.getUuid() + "-" + Slug.toSlug(job.getName());

			if (remoteExecutionDataClient.doesExist(remoteWorkPath))
			{
				remoteExecutionDataClient.delete(remoteWorkPath);
				log.debug("Successfully deleted remote work directory " + remoteWorkPath + " for failed job " + job.getUuid());
				job = JobManager.updateStatus(job, JobStatusType.STAGING_INPUTS, "Completed cleaning up remote work directory.");
			} else {
				log.debug("Skipping deleting remote work directory " + remoteWorkPath + " for failed job " + job.getUuid() + ". Directory not present.");
				job = JobManager.updateStatus(job, JobStatusType.STAGING_INPUTS, "Completed cleaning up remote work directory.");
			}

			return job;
		}
		catch (RemoteDataException e) {
			throw new JobException(e.getMessage(), e);
		}
		catch (Exception e)
		{
			throw new JobException("Failed to delete remote work directory " + remoteWorkPath, e);
		}
		finally
		{
			try { remoteExecutionDataClient.disconnect(); } catch (Exception e) {}
		}
	}

	/**
	 * Kills a running job by updating its status and using the remote
	 * scheduler command and local id to stop it forcefully.
	 *
	 * @param job
	 * @throws Exception
	 */
	public static Job kill(Job job) throws Exception
	{
		if (!JobStatusType.hasQueued(job.getStatus()) || job.getStatus() == JobStatusType.ARCHIVING)
		{
			// if it's not in queue, just update the status
//			JobDao.refresh(job);
			job = JobManager.updateStatus(job, JobStatusType.STOPPED, "Job cancelled by user.");

			for (JobEvent event: job.getEvents()) {
				if (event.getTransferTask() != null)
				{
					if (event.getTransferTask().getStatus() == TransferStatusType.PAUSED ||
							event.getTransferTask().getStatus() == TransferStatusType.QUEUED ||
							event.getTransferTask().getStatus() == TransferStatusType.RETRYING ||
							event.getTransferTask().getStatus() == TransferStatusType.TRANSFERRING) {
						try {
							TransferTaskDao.cancelAllRelatedTransfers(event.getTransferTask().getId());
						} catch (Exception e ) {
							log.error("Failed to cancel transfer task " +
									event.getTransferTask().getUuid() + " while stopping job " +
									job.getUuid(), e);
						}
					}
				}
			}

			return job;
		}
		else if (!job.isRunning())
		{
			// nothing to be done for jobs that are not running
			return job;
		}
		else
		{
		    JobKiller killer = null;

			int retries = 0;
			while (retries < Settings.MAX_SUBMISSION_RETRIES)
			{
			    try
                {
                    log.debug("Attempt " + (retries+1) + " to kill job " + job.getUuid() +
                            " and clean up assets");

                    killer = JobKillerFactory.getInstance(job);
                    killer.attack();

                    log.debug("Successfully killed remaining processes of job " + job.getUuid());

                    job = JobManager.updateStatus(job, JobStatusType.FAILED,
                            "Successfully killed remote job process.");

                    return job;
                }
			    catch (SystemUnavailableException  e) {
			    	
			    	String message = "Failed to kill job " + job.getUuid()
                            + " identified by id " + job.getLocalJobId() + " on " + job.getSystem()
                            + ". The system is currently unavailable.";

                    log.debug(message);
                    
                    job = JobManager.updateStatus(job, job.getStatus(), "Failed to kill job "
                            + " identified by id " + job.getLocalJobId() + " on " + job.getSystem()
                            + " Response from " + job.getSystem() + ": " + e.getMessage());

                    throw new JobTerminationException(message, e);
			    }
                catch (RemoteExecutionException e) {

                    job = killer.getJob();

                    String message = "Failed to kill job " + job.getUuid()
                            + " identified by id " + job.getLocalJobId() + " on " + job.getSystem()
                            + " Response from " + job.getSystem() + ": " + e.getMessage();

                    log.debug(message);

                    job = JobManager.updateStatus(job, JobStatusType.FAILED, "Failed to kill job "
                            + " identified by id " + job.getLocalJobId() + " on " + job.getSystem()
                            + " Response from " + job.getSystem() + ": " + e.getMessage());

                    throw new JobTerminationException(message, e);
                }
                catch (JobTerminationException e) {

                    retries++;

                    job = killer.getJob();

                    String message = "Failed to kill job " + job.getUuid() +
                            " on attempt " + retries + ". Response from " + job.getSystem() + ": " + e.getMessage();

                    log.debug(message);

                    job = JobManager.updateStatus(job, job.getStatus(), message);

                    if (retries == Settings.MAX_SUBMISSION_RETRIES) {

                        message = "Failed to kill job " + job.getUuid() +
                                " after " + retries + "  attempts. Terminating job.";

                        log.debug(message);

                        job = JobManager.updateStatus(job, JobStatusType.FAILED, message);

                        return job;
                    }
                }

			}

			// Occasionally the status check will have run or the job will actually complete
			// prior to this being called. That will invalidate the current object. Here we
			// refresh with job prior to updating the status so we don't get a stale state
			// exception
//			JobDao.refresh(job);
			job = JobManager.updateStatus(job, JobStatusType.STOPPED);
//			job.setStatus(JobStatusType.STOPPED,  JobStatusType.STOPPED.getDescription());
//			job.setLastUpdated(new DateTime().toDate());
//			job.setEndTime(job.getLastUpdated());
//			JobDao.persist(job);

			for (JobEvent event: job.getEvents()) {
				if (event.getTransferTask() != null)
				{
					if (event.getTransferTask().getStatus() == TransferStatusType.PAUSED ||
							event.getTransferTask().getStatus() == TransferStatusType.QUEUED ||
							event.getTransferTask().getStatus() == TransferStatusType.RETRYING ||
							event.getTransferTask().getStatus() == TransferStatusType.TRANSFERRING) {
						try {
							TransferTaskDao.cancelAllRelatedTransfers(event.getTransferTask().getId());
						} catch (Exception e ) {
							log.error("Failed to cancel transfer task " +
									event.getTransferTask().getUuid() + " while stopping job " +
									job.getUuid(), e);
						}
					}
				}
			}

			return job;
		}
	}
	
	/**
	 * Sets {@link Job#setVisible(Boolean)} to true and
	 * updates the timestamp. A {@link JobEventType#RESTORED} event
	 * is thrown.
	 *
	 * @param jobId
	 * @throws JobException
	 */
	public static Job restore(long jobId, String invokingUsername) throws JobTerminationException, JobException
	{
		Job job = null; 
				
		try {
			job = JobDao.getById(jobId);
		
			if (!job.isVisible()) {
				try {
					job.setVisible(Boolean.TRUE);
					
					job.addEvent(new JobEvent(
							JobEventType.RESTORED.name(), 
							"Job was restored by " + invokingUsername,
							invokingUsername));
					
					JobDao.persist(job);
					
					return job;
				}
				catch (Throwable e) {
					throw new JobException("Failed to restore job " + job.getUuid() + ".", e);
				}
			}
			else {
				throw new JobException("Job is already visible.");
			}
		}
		catch (UnresolvableObjectException e) {
			throw new JobException("Unable to restore job. If this persists, please contact your tenant administrator.", e);
		}
		catch (JobException e) {
			throw e;
		}
	}

	/**
	 * Sets the job's visibility attribute to false and
	 * updates the timestamp. A {@link JobEventType#DELETED} event
	 * is thrown. If the job was running, a {@link JobEventType#STOPPED} event
	 * is also thrown.
	 *
	 * @param jobId
	 * @throws JobException
	 */
	public static Job hide(long jobId, String invokingUsername) throws JobTerminationException, JobException
	{
		Job job = JobDao.getById(jobId);
		
		// make sure the job is visible
		if (job.isVisible()) {
			
			// if the job isn't running, we can just flip the visibility flag and move on
			if (!job.isRunning())
			{
				try {
					job.setVisible(Boolean.FALSE);
					
					job.addEvent(new JobEvent(
							JobEventType.DELETED.name(), 
							"Job was deleted by user " + invokingUsername,
							invokingUsername));
					
					JobDao.persist(job);
				}
				catch (Throwable e) {
					throw new JobException("Failed to update job " + job.getUuid() + ".", e);
				}
			}
			// otherwise, we need to attempt to kill the remote job
			else
			{
				JobKiller killer = null;
				try {
					killer = JobKillerFactory.getInstance(job);
					killer.attack();
				} 
				catch (SystemUnavailableException e) {
					throw new JobTerminationException("Failed to stop job " + job.getUuid() + 
							". Execution system is unavailable.", e);
				}
				catch (RemoteExecutionException e) {
					throw new JobTerminationException("Failed to stop " + job.getUuid()
							+ " at user's request. An error occurred communicating "
							+ "with the remote host", e);
				}
				catch (JobTerminationException e) {
					throw e;
				}
				catch (Throwable t) {
					throw new JobException("Unexpected error stopping job " + job.getUuid() + 
							" at user's request.", t);
				}
				finally {
					job.setVisible(Boolean.FALSE);
					job.setStatus(JobStatusType.STOPPED, "Job stopped by user " + invokingUsername);
					
					Date jobHiddenDate = new DateTime().toDate();
					job.setLastUpdated(jobHiddenDate);
					job.setEndTime(jobHiddenDate);
					
					job.addEvent(new JobEvent(
							JobEventType.DELETED.name(), 
							"Job was deleted by user " + invokingUsername,
							invokingUsername));
					
					JobDao.persist(job);
				}
			}
		}
		
		return job;
	}

	/**
	 * Updates the status of a job, updates the timestamps as appropriate
	 * based on the status, and writes a new JobEvent to the job's history.
	 *
	 * @param job
	 * @param status
	 * @throws JobException
	 */
	public static Job updateStatus(Job job, JobStatusType status)
			throws JobException
	{
		return updateStatus(job, status, status.getDescription());
	}

	/**
	 * Updates the status of a job, its timestamps, and writes a new
	 * JobEvent to the job's history with the given status and message.
	 *
	 * @param job
	 * @param status
	 * @param errorMessage
	 * @return Updated job object
	 * @throws JobException
	 */
	public static Job updateStatus(Job job, JobStatusType status, String errorMessage)
	throws JobException
	{
		job.setStatus(status, errorMessage);

		Date date = new DateTime().toDate();
		job.setLastUpdated(date);
		if (status.equals(JobStatusType.QUEUED))
		{
		    if (job.getSubmitTime() == null) {
		        job.setSubmitTime(new DateTime().toDate());
		    }
		}
		else if (status.equals(JobStatusType.RUNNING))
		{
			if (job.getStartTime() == null) {
			    job.setStartTime(new DateTime().toDate());
			}
		}
		else if (status.equals(JobStatusType.FINISHED)
				|| status.equals(JobStatusType.KILLED)
				|| status.equals(JobStatusType.STOPPED)
				|| status.equals(JobStatusType.FAILED))
		{
		    if (job.getEndTime() == null) {
		        job.setEndTime(new DateTime().toDate());
		    }
		}
		else if (status.equals(JobStatusType.STAGED)) {
			//
		}

		JobDao.persist(job, false);

		return job;
	}

	/**
	 * This method attempts to archive a job's output by retrieving the
	 * .agave.archive shadow file from the remote job directory and staging
	 * everything not in there to the user-supplied Job.archivePath on the
	 * Job.archiveSystem
	 *
	 * @param job
	 * @throws SystemUnavailableException
	 * @throws SystemUnknownException
	 * @throws JobException
	 */
	public static void archive(Job job)
	throws SystemUnavailableException, SystemUnknownException, JobException
	{
		// flag to ignore deleting of archiving folder in the event of a race condition
		boolean skipCleanup = false;

		ExecutionSystem executionSystem = (ExecutionSystem) new SystemDao().findBySystemId(job.getSystem());

		if (executionSystem == null || !executionSystem.isAvailable() || !executionSystem.getStatus().equals(SystemStatusType.UP))
		{
			throw new SystemUnavailableException("Job execution system " + executionSystem.getSystemId() + " is not available.");
		}

		log.debug("Beginning archive inputs for job " + job.getUuid() + " " + job.getName());
//		JobManager.updateStatus(job, JobStatusType.ARCHIVING);

		RemoteDataClient archiveDataClient = null;
		RemoteDataClient executionDataClient = null;
		RemoteSystem remoteArchiveSystem = null;

		// we should be able to archive from anywhere. Given that we can stage in condor
		// job data from remote systems, we should be able to stage it out as well. At
		// this point we are guaranteed that the worker running this bit of code has
		// access to the job output folder. The RemoteDataClient abstraction will handle
		// the rest.
		File archiveFile = null;
		try
		{
			try
			{
				executionDataClient = executionSystem.getRemoteDataClient(job.getInternalUsername());
				executionDataClient.authenticate();
			}
			catch (Exception e)
			{
				throw new JobException("Failed to authenticate to the execution system "
						+ executionSystem.getSystemId());
			}

			// copy remote archive file to temp space
			String remoteArchiveFile = job.getWorkPath() + File.separator + ".agave.archive";

			String localArchiveFile = FileUtils.getTempDirectoryPath() + File.separator +
					"job-" + job.getUuid() + "-" + System.currentTimeMillis();

			// pull remote .archive file and parse it for a list of paths relative
			// to the job.workDir to exclude from archiving. Generally this will be
			// the application binaries, but the app itself may have added or removed
			// things from this file, so we need to process it anyway.
			List<String> jobFileList = new ArrayList<String>();
			try
			{
				if (executionDataClient.doesExist(remoteArchiveFile))
				{
					executionDataClient.get(remoteArchiveFile, localArchiveFile);

					// read it in to find the original job files
					archiveFile = new File(localArchiveFile);
					if (archiveFile.exists())
					{
						if (archiveFile.isFile())
						{
							jobFileList.addAll(FileUtils.readLines(archiveFile));
						}
						else
						{
							archiveFile = new File(localArchiveFile, ".agave.archive");
							if (archiveFile.exists() && archiveFile.isFile()) {
								jobFileList.addAll(FileUtils.readLines(archiveFile));
							}
						}
					}
				}
				else
				{
					log.debug("No archive file found for job " + job.getUuid() + " on system " +
							executionSystem.getSystemId() + " at " + remoteArchiveFile +
							". Entire job directory will be archived.");
					JobManager.updateStatus(job, JobStatusType.ARCHIVING,
							"No archive file found. Entire job directory will be archived.");
				}
			}
			catch (Exception e)
			{
				log.debug("Unable to parse archive file for job " + job.getUuid() + " on system " +
						executionSystem.getSystemId() + " at " + remoteArchiveFile +
						". Entire job directory will be archived.");
				JobManager.updateStatus(job, JobStatusType.ARCHIVING,
						"Unable to parse job archive file. Entire job directory will be archived.");
			}

			remoteArchiveSystem = job.getArchiveSystem();

			if (remoteArchiveSystem == null)
			{
				throw new SystemUnknownException("Unable to archive job output. No archive system could be found.");
			}
			else if (!remoteArchiveSystem.isAvailable() || !remoteArchiveSystem.getStatus().equals(SystemStatusType.UP))
			{
				throw new SystemUnavailableException("Unable to archive job output from system " +
						remoteArchiveSystem.getSystemId() + ". The system is currently unavailable.");
			}
			else
			{
				try
				{
					archiveDataClient = remoteArchiveSystem.getRemoteDataClient(job.getInternalUsername());
					archiveDataClient.authenticate();
				}
				catch (Exception e)
				{
					throw new JobException("Failed to authenticate to the archive system "
							+ remoteArchiveSystem.getSystemId(), e);
				}
			}

			try
			{
				if (!archiveDataClient.doesExist(job.getArchivePath())) {
					archiveDataClient.mkdirs(job.getArchivePath());
					if (archiveDataClient.isPermissionMirroringRequired() && StringUtils.isEmpty(job.getInternalUsername())) {
					    archiveDataClient.setOwnerPermission(job.getOwner(), job.getArchivePath(), true);
                    }
				}
			}
			catch (Exception e)
			{
				throw new JobException("Failed to create archive directory "
						+ job.getArchivePath() + " on " + remoteArchiveSystem.getSystemId(), e);
			}

			// read in remote job work directory listing
			List<RemoteFileInfo> outputFiles = null;
			try
			{
				outputFiles = executionDataClient.ls(job.getWorkPath());
			}
			catch (Exception e) {
				throw new JobException("Failed to retrieve directory listing of "
						+ job.getWorkPath() + " from " + executionSystem.getSystemId(), e);
			}

			// iterate over the work folder and archive everything that wasn't
			// listed in the archive file. We use URL copy here to abstract the
			// third party transfer we would like to do. If possible, URLCopy will
			// do a 3rd party transfer. When not possible, such as when we're going
			// cross-protocol, it will proxy the transfer.
			TransferTask rootTask = new TransferTask(
					"agave://" + job.getSystem() + "/" + job.getWorkPath(),
					"agave://" + job.getArchiveSystem().getSystemId() + "/" +job.getArchivePath(),
					job.getOwner(),
					null,
					null);
			TransferTaskDao.persist(rootTask);

			job.addEvent(new JobEvent(
					job.getStatus(),
					"Archiving " + rootTask.getSource() + " to " + rootTask.getDest(),
					rootTask,
					job.getOwner()));

			JobDao.persist(job);

			for (RemoteFileInfo outputFile: outputFiles)
			{
			    JobDao.refresh(job);

			    if (job.getStatus() != JobStatusType.ARCHIVING) break;

				if (StringUtils.equals(outputFile.getName(), ".") || StringUtils.equals(outputFile.getName(), "..")) continue;

				String workFileName = job.getWorkPath() + File.separator + outputFile.getName();
				String archiveFileName = job.getArchivePath() + File.separator + outputFile.getName();
				if (!jobFileList.contains(outputFile.getName()))
				{
					final URLCopy urlCopy = new URLCopy(executionDataClient, archiveDataClient);
					TransferTask childTransferTask = new TransferTask(
							"agave://" + job.getSystem() + "/" + workFileName,
							"agave://" + job.getArchiveSystem().getSystemId() + "/" + archiveFileName,
							job.getOwner(),
							rootTask,
							rootTask);
					try
					{
						TransferTaskDao.persist(childTransferTask);
						urlCopy.copy(workFileName, archiveFileName, childTransferTask);
						rootTask.updateSummaryStats(childTransferTask);
						TransferTaskDao.persist(rootTask);
					}
					catch (TransferException e) {
						throw new JobException("Failed to archive file " + workFileName +
								" to " + childTransferTask.getDest() +
								" due to an error persisting the transfer record.", e);
					}
					catch (Exception e) {
						throw new JobException("Failed to archive file " + workFileName +
								" to " + childTransferTask.getDest() +
								" due to an error during transfer ", e);
					}
				}
			}

			try
			{
			    if (job.getStatus() == JobStatusType.ARCHIVING) {
			        rootTask.setStatus(TransferStatusType.COMPLETED);
			    } else {
			        rootTask.setStatus(TransferStatusType.FAILED);
			    }

			    rootTask.setEndTime(new DateTime().toDate());

				TransferTaskDao.persist(rootTask);
			}
			catch (Exception e) {

			}

			// if it all worked as expected, then delete the job work directory
			try
			{
//			    if (!skipCleanup) {
    				executionDataClient.delete(job.getWorkPath());
    			    JobManager.updateStatus(job, JobStatusType.ARCHIVING_FINISHED,
                            "Job archiving completed successfully.");
//    			}
			}
			catch (Exception e) {
				log.error("Archiving of job " + job.getUuid() + " completed, "
					+ "but an error occurred deleting the remote work directory "
					+ job.getUuid(), e);
			}
		}
		catch (StaleObjectStateException e) {
			skipCleanup = true;
			log.error(e);
			throw e;
		}
		catch (SystemUnavailableException e)
		{
			throw e;
		}
		catch (SystemUnknownException e)
		{
			throw e;
		}
		catch (JobException e)
		{
			throw e;
		}
		catch (Exception e) {
			throw new JobException("Failed to archive data due to internal failure.", e);
		}
		finally
		{
			// clean up the local archive file
			FileUtils.deleteQuietly(archiveFile);
			try {
				if (archiveDataClient.isPermissionMirroringRequired() && StringUtils.isEmpty(job.getInternalUsername())) {
					archiveDataClient.setOwnerPermission(job.getOwner(), job.getArchivePath(), true);
				}
			} catch (Exception e) {}
			try { archiveDataClient.disconnect(); } catch (Exception e) {}
			try { executionDataClient.disconnect(); } catch (Exception e) {}
		}
	}
	
	/**
	 * Takes an existing {@link Job} and sanitizes it for resubmission. During this process
	 * the {@link Job#archivePath}, {@link Job#archiveSystem}, etc. will be updated. In the 
	 * event that {@link SoftwareParameter} or {@link SoftwareInput} are no longer valid, the 
	 * job will fail to validate. This is a good thing as it ensures reproducibility. In 
	 * situations where reproducibility is not critical, the {@code ignoreInputConflicts} and 
	 * {@code ignoreParameterConflicts} flags can be set to true to update any hidden field 
	 * defaults or inject them if not previously present.
	 *
	 * @param originalJob the job to resubmit
	 * @param newJobOwner the owner of the new job
	 * @param newJobInternalUsername the internal username of the new job
	 * @param ignoreInputConflicts if true, ignore hidden input conflicts and update accordingly
	 * @param ignoreParameterConflicts if true, ignore hidden parameter conflicts and update accordingly
	 * @return a validated {@link Job} representing the resubmitted job with a unique id.
	 * @throws JobProcessingException
	 * @throws JsonProcessingException
	 * @throws IOException
	 */
	public static Job resubmitJob(Job originalJob, String newJobOwner, String newJobInternalUsername,
			boolean ignoreInputConflicts, boolean ignoreParameterConflicts)
	throws JobProcessingException, JsonProcessingException, IOException
	{
		boolean preserveNotifications = false;
		
		JobRequestProcessor processor = 
				new JobResubmissionRequestProcessor(newJobOwner, 
													newJobInternalUsername,
													ignoreInputConflicts,
													ignoreParameterConflicts,
													preserveNotifications);
		
		JsonNode originalJobJson = new ObjectMapper().readTree(originalJob.toJSON());
		
		Job newJob = processor.processJob(originalJobJson);
		
		return newJob;
	}

	/**
	 * Takes a JsonNode representing a job request and parses it into a job object.
	 *
	 * @param json a JsonNode containing the job request
	 * @return validated job object ready for submission
	 * @throws JobProcessingException
	 */
	public static Job processJob(JsonNode json, String username, String internalUsername)
	throws JobProcessingException
	{
		JobRequestProcessor processor = new JobRequestProcessor(username, internalUsername);
		return processor.processJob(json);
		
//	    HashMap<String, Object> jobRequestMap = new HashMap<String, Object>();
//
//		String currentKey = null;
//
//		try
//		{
//			Iterator<String> fields = json.fieldNames();
//   			while(fields.hasNext()) {
//				String key = fields.next();
//
//				if (StringUtils.isEmpty(key)) continue;
//
//				currentKey = key;
//
//				if (key.equals("notifications")) {
//					continue;
//				}
//
//				if (key.equals("dependencies"))
//				{
//					throw new JobProcessingException(400,
//							"Job dependencies are not yet supported.");
//				}
//
//				JsonNode child = json.get(key);
//
//				if (child.isNull()) {
//				    jobRequestMap.put(key, null);
//				}
//				else if (child.isNumber())
//				{
//				    jobRequestMap.put(key, child.asText());
//				}
//				else if (child.isObject())
//				{
//					Iterator<String> childFields = child.fieldNames();
//					while(childFields.hasNext())
//					{
//						String childKey = childFields.next();
//						JsonNode childchild = child.path(childKey);
//						if (StringUtils.isEmpty(childKey) || childchild.isNull() || childchild.isMissingNode()) {
//							continue;
//						}
//						else if (childchild.isDouble()) {
//						    jobRequestMap.put(childKey, childchild.decimalValue().toPlainString());
//						}
//						else if (childchild.isNumber())
//						{
//						    jobRequestMap.put(childKey, new Long(childchild.longValue()).toString());
//						}
//						else if (childchild.isArray()) {
//						    List<String> arrayValues = new ArrayList<String>();
//							for (Iterator<JsonNode> argIterator = childchild.iterator(); argIterator.hasNext();)
//							{
//								JsonNode argValue = argIterator.next();
//								if (argValue.isNull() || argValue.isMissingNode()) {
//									continue;
//								} else {
//								    arrayValues.add(argValue.asText());
//								}
//							}
//							jobRequestMap.put(childKey, StringUtils.join(arrayValues, ";"));
//						}
//						else if (childchild.isTextual()) {
//						    jobRequestMap.put(childKey, childchild.textValue());
//						}
//						else if (childchild.isBoolean()) {
//						    jobRequestMap.put(childKey, childchild.asBoolean() ? "true" : "false");
//						}
//					}
//				}
//				else
//				{
//				    jobRequestMap.put(key, json.get(key).asText());
//				}
//			}
//
//   			List<Notification> notifications = new ArrayList<Notification>();
//
////   			if (json.has("dependencies"))
////			{
////   				if (!json.get("dependencies").isArray())
////				{
////					throw new NotificationException("Invalid " + currentKey + " value given. "
////							+ "dependencies must be an array of dependency objects one or more "
////							+ "valid dependency constraints.");
////				}
////				else
////				{
////					currentKey = "dependencies";
////
////					ArrayNode jsonDependencies = (ArrayNode)json.get("dependencies");
////					for (int i=0; i<jsonDependencies.size(); i++)
////					{
////						currentKey = "dependencies["+i+"]";
////						JsonNode jsonDependency = jsonDependencies.get(i);
////						JobDependency dependency = JobDependency.fromJson(dependency);
////
////					}
////				}
////			}
//
//			if (json.has("notifications"))
//			{
//				if (!json.get("notifications").isArray())
//				{
//					throw new NotificationException("Invalid " + currentKey + " value given. "
//							+ "notifications must be an array of notification objects specifying a "
//							+ "valid url, event, and an optional boolean persistence attribute.");
//				}
//				else
//				{
//					currentKey = "notifications";
//
//					ArrayNode jsonNotifications = (ArrayNode)json.get("notifications");
//					for (int i=0; i<jsonNotifications.size(); i++)
//					{
//						currentKey = "notifications["+i+"]";
//						JsonNode jsonNotif = jsonNotifications.get(i);
//						if (!jsonNotif.isObject())
//						{
//							throw new NotificationException("Invalid " + currentKey + " value given. "
//								+ "Each notification objects should specify a "
//								+ "valid url, event, and an optional boolean persistence attribute.");
//						}
//						else
//						{
//							
//							Notification notification = Notification.fromJSON(jsonNotif);
//							
////							currentKey = "notifications["+i+"].url";
////							if (!jsonNotif.has("url")) {
////								throw new NotificationException("No " + currentKey + " attribute given. "
////										+ "Notifications must have valid url and event attributes.");
////							}
////							else
////							{
////								notification.setCallbackUrl(jsonNotif.get("url").textValue());
////							}
////
////							currentKey = "notifications["+i+"].event";
////							if (!jsonNotif.has("event")) {
////								throw new NotificationException("No " + currentKey + " attribute given. "
////										+ "Notifications must have valid url and event attributes.");
////							}
////							else
////							{
////								String event = jsonNotif.get("event").textValue();
////								try {
////									if (!StringUtils.equals("*", event)) {
////										try {
////											JobStatusType.valueOf(event.toUpperCase());
////										} catch (IllegalArgumentException e) {
////											JobMacroType.valueOf(event.toUpperCase());
////										}
////										notification.setEvent(StringUtils.upperCase(event));
////									}
////									else {
////										notification.setEvent("*");
////									}
////								} catch (Throwable e) {
////									throw new NotificationException("Valid values are: *, " +
////											ServiceUtils.explode(", ", Arrays.asList(JobStatusType.values())) + ", " +
////											ServiceUtils.explode(", ", Arrays.asList(JobMacroType.values())));
////								}
////							}
////
////
////							if (jsonNotif.has("persistent"))
////							{
////								currentKey = "notifications["+i+"].persistent";
////								if (jsonNotif.get("persistent").isNull()) {
////									throw new NotificationException(currentKey + " cannot be null");
////								}
////								else if (!jsonNotif.get("persistent").isBoolean())
////								{
////									throw new NotificationException("Invalid value for " + currentKey + ". "
////											+ "If provided, " + currentKey + " must be a boolean value.");
////								} else {
////									notification.setPersistent(jsonNotif.get("persistent").asBoolean());
////								}
////							}
//							notifications.add(notification);
////							Thread.sleep(5);
//						}
//					}
//				}
//			}
//
//			Job job = processJob(jobRequestMap, username, internalUsername);
//
//			for (Notification notification: notifications) {
//				job.addNotification(notification);
//			}
//
//			// If the job request had notification configured for job creation
//			// they could not have fired yet. Here we explicitly add them.
//			for (JobEvent jobEvent: job.getEvents()) {
//			    JobEventProcessor eventProcessor = new JobEventProcessor(jobEvent);
//			    eventProcessor.process();
//			}
//
//			return job;
//		}
//		catch (JobProcessingException e) {
//			throw e;
//		}
//		catch (SoftwareException e) {
//			throw new JobProcessingException(400, e.getMessage());
//		}
//		catch (Throwable e) {
//			throw new JobProcessingException(400,
//					"Failed to parse json job description. Invalid value for " +
//							currentKey + ". " + e.getMessage());
//		}
	}

	/**
	 * Takes a Form representing a job request and parses it into a job object. This is a
	 * stripped down, unstructured version of the other processJob method.
	 *
	 * @param json a JsonNode containing the job request
	 * @return validated job object ready for submission
	 * @throws JobProcessingException
	 */
	public static Job processJob(Map<String, Object> jobRequestMap, String username, String internalUsername)
	throws JobProcessingException
	{
		JobRequestProcessor processor = new JobRequestProcessor(username, internalUsername);
		return processor.processJob(jobRequestMap);
		
//	    Job job = new Job();
//
//		SystemManager systemManager = new SystemManager();
//
//		String name = null;
//		if (jobRequestMap.containsKey("name")) {
//			name = (String)jobRequestMap.get("name");
//		} else {
//			name = (String)jobRequestMap.get("jobName");
//		}
//
//		if (StringUtils.isEmpty(name) || StringUtils.length(name) > 64)
//		{
//			throw new JobProcessingException(400,
//					"Job name cannot be empty.");
//		}
//		else if (StringUtils.length(name) > 64) {
//			throw new JobProcessingException(400,
//					"Job name must be less than 64 characters.");
//		}
//		else
//		{
//			name = name.trim();
//		}
//
//		String softwareName = null;
//		if (jobRequestMap.containsKey("appId")) {
//			softwareName = (String)jobRequestMap.get("appId");
//		} else {
//			softwareName = (String)jobRequestMap.get("softwareName");
//		}
//
//		if (StringUtils.isEmpty(softwareName))
//		{
//			throw new JobProcessingException(400,
//					"appId cannot be empty");
//		}
//		else if (StringUtils.length(name) > 80) {
//			throw new JobProcessingException(400,
//					"appId must be less than 80 characters");
//		}
//		else if (!softwareName.contains("-") || softwareName.endsWith("-"))
//		{
//			throw new JobProcessingException(400,
//					"Invalid appId. " +
//					"Please specify an app using its unique id. " +
//					"The unique id is defined by the app name " +
//					"and version separated by a hyphen. eg. example-1.0");
//		}
//
//		Software software = SoftwareDao.getSoftwareByUniqueName(softwareName.trim());
//
//		if (software == null) {
//			throw new JobProcessingException(400, "No app found matching " + softwareName + " for " + username);
//		}
//		else if (!ApplicationManager.isInvokableByUser(software, username)) {
//			throw new JobProcessingException(403, "Permission denied. You do not have permission to access this app");
//		}
//
//		// validate the optional execution system matches the software execution system
//		String exeSystem = (String)jobRequestMap.get("executionSystem");
//		if (jobRequestMap.containsKey("executionSystem")) {
//			softwareName = (String)jobRequestMap.get("executionSystem");
//		} else {
//			softwareName = (String)jobRequestMap.get("executionHost");
//		}
//		ExecutionSystem executionSystem = software.getExecutionSystem();
//		if (StringUtils.length(exeSystem) > 80) {
//			throw new JobProcessingException(400,
//					"executionSystem must be less than 80 characters");
//		}
//		else if (!StringUtils.isEmpty(exeSystem) && !StringUtils.equals(exeSystem, executionSystem.getSystemId())) {
//			throw new JobProcessingException(403,
//					"Invalid execution system. Apps are registered to run on a specific execution system. If specified, " +
//					"the execution system must match the execution system in the app description. The execution system " +
//					"for " + software.getName() + " is " + software.getExecutionSystem().getSystemId() + ".");
//		}
//
//		/***************************************************************************
//		 **						Batch Parameter Selection 						  **
//		 ***************************************************************************/
//
//		String currentParameter = null;
//		String queueName = null;
//		BatchQueue jobQueue = null;
//		Long nodeCount = null;
//		Double memoryPerNode = null;
//		String requestedTime = null;
//		Long processorsPerNode = null;
//
//		try
//		{
//			/********************************** Queue Selection *****************************************/
//
//			currentParameter = "batchQueue";
//			String userBatchQueue = (String)jobRequestMap.get("batchQueue");
//			if (StringUtils.isEmpty(userBatchQueue)) {
//				userBatchQueue = (String)jobRequestMap.get("queue");
//			}
//
//			if (StringUtils.length(userBatchQueue) > 128) {
//				throw new JobProcessingException(400,
//						"batchQueue must be less than 128 characters");
//			}
//			else if (StringUtils.isEmpty(userBatchQueue))
//			{
//				// use the software default queue if present, otherwise we'll pick it for them in a bit
//				if (!StringUtils.isEmpty(software.getDefaultQueue()))
//				{
//					queueName = software.getDefaultQueue();
//					jobQueue = executionSystem.getQueue(queueName);
//					if (jobQueue == null)
//					{
//						throw new JobProcessingException(400,
//								"Invalid default batchQueue. No batchQueue named " + queueName +
//								" is defined on system " + executionSystem.getSystemId());
//					}
//				}
//			}
//			else
//			{
//				// user gave a queue. see if it's a valid one
//				jobQueue = executionSystem.getQueue(userBatchQueue);
//				queueName = userBatchQueue;
//				if (jobQueue == null) {
//					throw new JobProcessingException(400,
//							"Invalid batchQueue. No batchQueue named " + queueName +
//							" is defined on system " + executionSystem.getSystemId());
//				}
//			}
//
//			/********************************** Node Count Selection *****************************************/
//
//			currentParameter = "nodeCount";
//			String userNodeCount = (String)jobRequestMap.get("nodeCount");
//			if (StringUtils.isEmpty(userNodeCount))
//			{
//				// use the software default queue if present
//				if (software.getDefaultNodes() != null && software.getDefaultNodes() != -1) {
//					nodeCount = software.getDefaultNodes();
//				}
//				else
//				{
//					// use a single node otherwise
//					nodeCount = new Long(1);
//				}
//			}
//			else
//			{
//				nodeCount = NumberUtils.toLong(userNodeCount);
//			}
//
//			if (nodeCount < 1)
//			{
//				throw new JobProcessingException(400,
//						"Invalid " + (StringUtils.isEmpty(userNodeCount) ? "" : "default ") +
//						"nodeCount. If specified, nodeCount must be a positive integer value.");
//			}
//
////			nodeCount = pTable.containsKey("nodeCount") ? Long.parseLong(pTable.get("nodeCount")) : software.getDefaultNodes();
////			if (nodeCount < 1) {
////				throw new JobProcessingException(400,
////						"Invalid nodeCount value. nodeCount must be a positive integer value.");
////			}
//
//			// if the queue wasn't specified by the user or app, pick a queue with just node count info
//			if (jobQueue == null) {
//				jobQueue = selectQueue(executionSystem, nodeCount, -1.0, (long)-1, BatchQueue.DEFAULT_MIN_RUN_TIME);
//			}
//
//			if (jobQueue == null) {
//				throw new JobProcessingException(400, "Invalid " +
//						(StringUtils.isEmpty(userNodeCount) ? "" : "default ") +
//						"nodeCount. No queue found on " +
//						executionSystem.getSystemId() + " that support jobs with " +
//						nodeCount + " nodes.");
//			} else if (!validateBatchSubmitParameters(jobQueue, nodeCount, (long)-1, -1.0, BatchQueue.DEFAULT_MIN_RUN_TIME)) {
//				throw new JobProcessingException(400, "Invalid " +
//						(StringUtils.isEmpty(userNodeCount) ? "" : "default ") +
//						"nodeCount. The " + jobQueue.getName() + " queue on " +
//						executionSystem.getSystemId() + " does not support jobs with " + nodeCount + " nodes.");
//			}
//
//			/********************************** Max Memory Selection *****************************************/
//
//			currentParameter = "memoryPerNode";
//			String userMemoryPerNode = (String)jobRequestMap.get("memoryPerNode");
//			if (StringUtils.isEmpty(userMemoryPerNode)) {
//				userMemoryPerNode = (String)jobRequestMap.get("maxMemory");
//			}
//
//			if (StringUtils.isEmpty(userMemoryPerNode))
//			{
//				if (software.getDefaultMemoryPerNode() != null) {
//					memoryPerNode = software.getDefaultMemoryPerNode();
//				}
//				else if (jobQueue.getMaxMemoryPerNode() != null && jobQueue.getMaxMemoryPerNode() > 0) {
//					memoryPerNode = jobQueue.getMaxMemoryPerNode();
//				}
//				else {
//					memoryPerNode = (double)0;
//				}
//			}
//			else // memory was given, validate
//			{
//				try {
//					// try to parse it as a number in GB first
//					memoryPerNode = Double.parseDouble(userMemoryPerNode);
//				}
//				catch (Throwable e)
//				{
//					// Otherwise parse it as a string matching ###.#[EPTGM]B
//					try
//					{
//						memoryPerNode = BatchQueue.parseMaxMemoryPerNode(userMemoryPerNode);
//					}
//					catch (NumberFormatException e1)
//					{
//						memoryPerNode = (double)0;
//					}
//				}
//			}
//
//			if (memoryPerNode <= 0) {
//				throw new JobProcessingException(400,
//						"Invalid " + (StringUtils.isEmpty(userMemoryPerNode) ? "" : "default ") +
//						"memoryPerNode. memoryPerNode should be a postive value specified in ###.#[EPTGM]B format.");
//			}
//
//			// if the queue wasn't specified by the user or app, reselect with node and memory info
//			if (StringUtils.isEmpty(queueName)) {
//				jobQueue = selectQueue(executionSystem, nodeCount, memoryPerNode, (long)-1, BatchQueue.DEFAULT_MIN_RUN_TIME);
//			}
//
//			if (jobQueue == null) {
//				throw new JobProcessingException(400, "Invalid " +
//						(StringUtils.isEmpty(userMemoryPerNode) ? "" : "default ") +
//						"memoryPerNode. No queue found on " +
//						executionSystem.getSystemId() + " that support jobs with " + nodeCount + " nodes and " +
//						memoryPerNode + "GB memory per node");
//			} else if (!validateBatchSubmitParameters(jobQueue, nodeCount, (long)-1, memoryPerNode, BatchQueue.DEFAULT_MIN_RUN_TIME)) {
//				throw new JobProcessingException(400, "Invalid " +
//						(StringUtils.isEmpty(userMemoryPerNode) ? "" : "default ") +
//						"memoryPerNode. The " + jobQueue.getName() + " queue on " +
//						executionSystem.getSystemId() + " does not support jobs with " + nodeCount + " nodes and " +
//						memoryPerNode + "GB memory per node");
//			}
//
//			/********************************** Run Time Selection *****************************************/
//
//			currentParameter = "requestedTime";
//			//requestedTime = pTable.containsKey("requestedTime") ? pTable.get("requestedTime") : software.getDefaultMaxRunTime();
//
//			String userRequestedTime = (String)jobRequestMap.get("maxRunTime");
//			if (StringUtils.isEmpty(userRequestedTime)) {
//				// legacy compatibility
//				userRequestedTime = (String)jobRequestMap.get("requestedTime");
//			}
//
//			if (StringUtils.isEmpty(userRequestedTime))
//			{
//				if (!StringUtils.isEmpty(software.getDefaultMaxRunTime())) {
//					requestedTime = software.getDefaultMaxRunTime();
//				} else if (!StringUtils.isEmpty(jobQueue.getMaxRequestedTime())) {
//					requestedTime = jobQueue.getMaxRequestedTime();
//				}
//			}
//			else
//			{
//				requestedTime = userRequestedTime;
//			}
//
//			if (!org.iplantc.service.systems.util.ServiceUtils.isValidRequestedJobTime(requestedTime)) {
//				throw new JobProcessingException(400,
//						"Invalid maxRunTime. maxRunTime should be the maximum run time " +
//							"time for this job in hh:mm:ss format.");
//			} else if (org.iplantc.service.systems.util.ServiceUtils.compareRequestedJobTimes(requestedTime, BatchQueue.DEFAULT_MIN_RUN_TIME) == -1) {
//				throw new JobProcessingException(400,
//						"Invalid maxRunTime. maxRunTime should be greater than 00:00:00.");
//			}
//
//			// if the queue wasn't specified by the user or app, reselect with node and memory info
//			if (StringUtils.isEmpty(queueName)) {
//				jobQueue = selectQueue(executionSystem, nodeCount, memoryPerNode, (long)-1, requestedTime);
//			}
//
//			if (jobQueue == null) {
//				throw new JobProcessingException(400, "Invalid " +
//						(StringUtils.isEmpty(userRequestedTime) ? "" : "default ") +
//						"maxRunTime. No queue found on " +
//						executionSystem.getSystemId() + " that supports jobs with " + nodeCount + " nodes, " +
//						memoryPerNode + "GB memory per node, and a run time of " + requestedTime);
//			} else if (!validateBatchSubmitParameters(jobQueue, nodeCount, (long)-1, memoryPerNode, requestedTime)) {
//				throw new JobProcessingException(400, "Invalid " +
//						(StringUtils.isEmpty(userRequestedTime) ? "" : "default ") +
//						"maxRunTime. The " + jobQueue.getName() + " queue on " +
//						executionSystem.getSystemId() + " does not support jobs with " + nodeCount + " nodes, " +
//						memoryPerNode + "GB memory per node, and a run time of " + requestedTime);
//			}
//
//			/********************************** Max Processors Selection *****************************************/
//
//			currentParameter = "processorsPerNode";
//			String userProcessorsPerNode = (String)jobRequestMap.get("processorsPerNode");
//			if (StringUtils.isEmpty(userProcessorsPerNode)) {
//				userProcessorsPerNode = (String)jobRequestMap.get("processorCount");
//			}
//			if (StringUtils.isEmpty(userProcessorsPerNode))
//			{
//				if (software.getDefaultProcessorsPerNode() != null) {
//					processorsPerNode = software.getDefaultProcessorsPerNode();
//				} else if (jobQueue.getMaxProcessorsPerNode() != null && jobQueue.getMaxProcessorsPerNode() > 0) {
//					processorsPerNode = jobQueue.getMaxProcessorsPerNode();
//				} else {
//					processorsPerNode = new Long(1);
//				}
//			}
//			else
//			{
//				processorsPerNode = NumberUtils.toLong(userProcessorsPerNode);
//			}
//
//			if (processorsPerNode < 1) {
//				throw new JobProcessingException(400,
//						"Invalid " + (StringUtils.isEmpty(userProcessorsPerNode) ? "" : "default ") +
//						"processorsPerNode value. processorsPerNode must be a positive integer value.");
//			}
//
//			// if the queue wasn't specified by the user or app, reselect with node and memory info
//			if (StringUtils.isEmpty(queueName)) {
//				jobQueue = selectQueue(executionSystem, nodeCount, memoryPerNode, processorsPerNode, requestedTime);
//			}
//
//			if (jobQueue == null) {
//				throw new JobProcessingException(400, "Invalid " +
//						(StringUtils.isEmpty(userProcessorsPerNode) ? "" : "default ") +
//						"processorsPerNode. No queue found on " +
//						executionSystem.getSystemId() + " that supports jobs with " + nodeCount + " nodes, " +
//						memoryPerNode + "GB memory per node, a run time of " + requestedTime + " and " +
//						processorsPerNode + " processors per node");
//			} else if (!validateBatchSubmitParameters(jobQueue, nodeCount, processorsPerNode, memoryPerNode, requestedTime)) {
//				throw new JobProcessingException(400, "Invalid " +
//						(StringUtils.isEmpty(userProcessorsPerNode) ? "" : "default ") +
//						"processorsPerNode. The " + jobQueue.getName() + " queue on " +
//						executionSystem.getSystemId() + " does not support jobs with " + nodeCount + " nodes, " +
//						memoryPerNode + "GB memory per node, a run time of " + requestedTime + " and " +
//						processorsPerNode + " processors per node");
//			}
//		}
//		catch (JobProcessingException e)
//		{
//			throw e;
//		}
//		catch (Exception e) {
//			throw new JobProcessingException(400, "Invalid " + currentParameter + " value.", e);
//		}
//
//		/***************************************************************************
//		 **						End Batch Queue Selection 						  **
//		 ***************************************************************************/
//
//
//		String defaultNotificationCallback = null;
//		List<Notification> notifications = new ArrayList<Notification>();
//		if (jobRequestMap.containsKey("callbackUrl")) {
//			defaultNotificationCallback = (String)jobRequestMap.get("callbackUrl");
//		} else if (jobRequestMap.containsKey("callbackURL")) {
////			throw new JobProcessingException(400,
////					"The callbackUrl attribute is no longer supported. Please specify " +
////					"one or more notification objects, including valid url and event attributes, " +
////					"in a notifications array instead.");
//			defaultNotificationCallback = (String)jobRequestMap.get("callbackURL");
//		} else if (jobRequestMap.containsKey("notifications")) {
//			defaultNotificationCallback = (String)jobRequestMap.get("notifications");
//		}
//
//		if (!StringUtils.isEmpty(defaultNotificationCallback))
//		{
//			try {
//				notifications.add(new Notification(JobStatusType.FINISHED.name(), defaultNotificationCallback));
//				// uuid generation was happening too fast here. we need to pause since this runs in the same
//				// thread and processor.
//				Thread.sleep(5);
//				notifications.add(new Notification(JobStatusType.FAILED.name(), defaultNotificationCallback));
//			} catch (NotificationException e) {
//				throw new JobProcessingException(400, e.getMessage());
//			} catch (InterruptedException e) {
//				throw new JobProcessingException(500, "Failed to verify notication callback url");
//			}
//		}
//
//		/***************************************************************************
//		 **						Verifying remote connectivity 					  **
//		 ***************************************************************************/
//
//		AuthConfig authConfig = executionSystem.getLoginConfig().getAuthConfigForInternalUsername(internalUsername);
//		String salt = executionSystem.getEncryptionKeyForAuthConfig(authConfig);
//		if (authConfig.isCredentialExpired(salt))
//		{
//			throw new JobProcessingException(412,
//					(authConfig.isSystemDefault() ? "Default " : "Internal user " + internalUsername) +
//					" credential for " + software.getExecutionSystem().getSystemId() + " is not active." +
//					" Please add a valid " + software.getExecutionSystem().getLoginConfig().getType() +
//					" execution credential for the execution system and resubmit the job.");
//		}
//
//		try
//		{
//			if (!executionSystem.getRemoteSubmissionClient(internalUsername).canAuthentication()) {
//				throw new RemoteExecutionException("Unable to authenticate to " + executionSystem.getSystemId());
//			}
//		}
//		catch (Exception e)
//		{
//			throw new JobProcessingException(412,
//					"Unable to authenticate to " + executionSystem.getSystemId() + " with the " +
//					(authConfig.isSystemDefault() ? "default " : "internal user " + internalUsername) +
//					"credential. Please check the " + executionSystem.getLoginConfig().getType() +
//					" execution credential for the execution system and resubmit the job.");
//		}
//
//		authConfig = executionSystem.getStorageConfig().getAuthConfigForInternalUsername(internalUsername);
//		salt = executionSystem.getEncryptionKeyForAuthConfig(authConfig);
//		if (authConfig.isCredentialExpired(salt))
//		{
//			throw new JobProcessingException(412,
//					"Credential for " + software.getExecutionSystem().getSystemId() + " is not active." +
//					" Please add a valid " + software.getExecutionSystem().getStorageConfig().getType() +
//					" storage credential for the execution system and resubmit the job.");
//		}
//
//		RemoteDataClient remoteExecutionDataClient = null;
//		try {
//			remoteExecutionDataClient = executionSystem.getRemoteDataClient(internalUsername);
//			remoteExecutionDataClient.authenticate();
//		} catch (Exception e) {
//			throw new JobProcessingException(412,
//					"Unable to authenticate to " + executionSystem.getSystemId() + " with the " +
//					(authConfig.isSystemDefault() ? "default " : "internal user " + internalUsername) +
//					"credential. Please check the " + executionSystem.getLoginConfig().getType() +
//					" execution credential for the execution system and resubmit the job.");
//		} finally {
//			try { remoteExecutionDataClient.disconnect(); } catch (Exception e) {}
//		}
//
//		/***************************************************************************
//		 **						Verifying Input Parmaeters						  **
//		 ***************************************************************************/
//
//		// Verify the inputs by their keys given in the SoftwareInputs
//		// in the Software object. We should also be inserting any other
//		// hidden inputs here
////		HashMap<String, String> inputTable = new HashMap<String, String>();
//		ObjectNode jobInputs = new ObjectMapper().createObjectNode();
//		for (SoftwareInput softwareInput : software.getInputs())
//		{
//			try
//			{
//				// add hidden inputs into the input array so we have a full record
//				// of all inputs for this job in the history.
//				if (!softwareInput.isVisible())
//				{
//					if (jobRequestMap.containsKey(softwareInput.getKey())) {
//						throw new JobProcessingException(400,
//								"Invalid value for " + softwareInput.getKey() +
//								". " + softwareInput.getKey() + " is a fixed value that "
//								+ "cannot be set manually. ");
//					} else {
//						jobInputs.put(softwareInput.getKey(), softwareInput.getDefaultValueAsJsonArray());
//					}
//				}
//				else if (!jobRequestMap.containsKey(softwareInput.getKey()))
//				{
//					if (softwareInput.isRequired())
//					{
//						throw new JobProcessingException(400,
//								"No input specified for " + softwareInput.getKey());
//					}
//					else
//					{
//						continue;
//					}
//				}
//				else if ((jobRequestMap.get(softwareInput.getKey()) instanceof String) &&
//						StringUtils.isEmpty((String)jobRequestMap.get(softwareInput.getKey())))
//				{
//					throw new JobProcessingException(400, "No input specified for " + softwareInput.getKey());
//				}
//				else
//				{
//					String[] explodedInputs = null;
//					if (jobRequestMap.get(softwareInput.getKey()) == null) {
//						explodedInputs = new String[]{};
//					} else if (jobRequestMap.get(softwareInput.getKey()) instanceof String[]) {
//						explodedInputs = (String[])jobRequestMap.get(softwareInput.getKey());
//					} else {
//						explodedInputs = StringUtils.split((String)jobRequestMap.get(softwareInput.getKey()), ";");
//					}
//
//					if (softwareInput.getMinCardinality() > explodedInputs.length)
//					{
//						throw new JobProcessingException(400,
//								softwareInput.getKey() + " requires at least " +
//								softwareInput.getMinCardinality() + " values");
//					}
//					else if (softwareInput.getMaxCardinality() != -1 &&
//							softwareInput.getMaxCardinality() < explodedInputs.length)
//					{
//						throw new JobProcessingException(400,
//								softwareInput.getKey() + " may have at most " +
//								softwareInput.getMaxCardinality() + " values");
//					}
//					else
//					{
//						for(String singleInput: explodedInputs)
//						{
//							singleInput = StringUtils.trim(singleInput);
//
//							if (StringUtils.isEmpty(singleInput)) continue;
//
//							if (StringUtils.isNotEmpty(softwareInput.getValidator()) &&
//									!ServiceUtils.doesValueMatchValidatorRegex(singleInput, softwareInput.getValidator()))
//							{
//								throw new JobProcessingException(400,
//										"Invalid input value, " + singleInput + ", for " + softwareInput.getKey() +
//										". Value must match the following expression: " +
//										softwareInput.getValidator());
//							}
//							else
//							{
//								URI inputUri = new URI(singleInput);
//								if (!RemoteDataClientFactory.isSchemeSupported(inputUri))
//								{
//									throw new JobProcessingException(400,
//											"Invalid value for " + softwareInput.getKey() +
//											". URI with the " + inputUri.getScheme() + " scheme are not currently supported. " +
//											"Please specify your input as a relative path, an Agave Files service endpoint, " +
//											"or a URL with one of the following schemes: http, https, sftp, or agave.");
//								}
//								else if (!PermissionManager.canUserReadUri(username, internalUsername, inputUri))
//								{
//									throw new JobProcessingException(403,
//											"You do not have permission to access this the input file or directory "
//											+ "at " + singleInput);
//								}
//							}
//						}
//
//						// serialize the inputs to a string or array
//						if (explodedInputs.length == 1)
//						{
//							jobInputs.put(softwareInput.getKey(), explodedInputs[0]);
//						}
//						else
//						{
//							ArrayNode jsonInputValueArrayNode = new ObjectMapper().createArrayNode();
//							for (String input: explodedInputs) {
//								if (StringUtils.isNotBlank(input)) {
//									jsonInputValueArrayNode.add(input);
//								}
//							}
//							jobInputs.put(softwareInput.getKey(), jsonInputValueArrayNode);
//						}
//					}
//				}
//			}
//			catch (PermissionException e)
//			{
//				throw new JobProcessingException(400,
//						e.getMessage(), e);
//			}
//			catch (JobProcessingException e)
//			{
//				throw e;
//			}
//			catch (Exception e)
//			{
//				throw new JobProcessingException(400,
//						"Failed to parse input for " + softwareInput.getKey(), e);
//			}
//		}
//
//		/***************************************************************************
//		 **						Verifying  Parameters							  **
//		 ***************************************************************************/
//
//		// Verify the parameters by their keys given in the
//		// SoftwareParameter in the Software object.
//		ObjectMapper mapper = new ObjectMapper();
//		ObjectNode jobParameters = mapper.createObjectNode();
//		for (SoftwareParameter softwareParameter : software.getParameters())
//		{
//			ArrayNode validatedJobParamValueArray = mapper.createArrayNode();
//
//			try
//			{
//				// add hidden parameters into the input array so we have a full record
//				// of all parameters for this job in the history.
//				if (!softwareParameter.isVisible())
//				{
//					if (jobRequestMap.containsKey(softwareParameter.getKey())) {
//						throw new JobProcessingException(400,
//								"Invalid parameter value for " + softwareParameter.getKey() +
//								". " + softwareParameter.getKey() + " is a fixed value that "
//								+ "cannot be set manually. ");
//					} else if (softwareParameter.getType().equals(SoftwareParameterType.bool) ||
//							softwareParameter.getType().equals(SoftwareParameterType.flag)) {
//						if (softwareParameter.getDefaultValueAsJsonArray().size() > 0) {
//							jobParameters.put(softwareParameter.getKey(), softwareParameter.getDefaultValueAsJsonArray().get(0));
//						} else {
//							jobParameters.put(softwareParameter.getKey(), false);
//						}
//					} else {
//						jobParameters.put(softwareParameter.getKey(), softwareParameter.getDefaultValueAsJsonArray());
//					}
//				}
//				else if (!jobRequestMap.containsKey(softwareParameter.getKey()))
//				{
//					if (softwareParameter.isRequired())
//					{
//						throw new JobProcessingException(400,
//								"No input parameter specified for " + softwareParameter.getKey());
//					}
//					else
//					{
//						continue;
//					}
//				}
//				else
//				{
//					String[] explodedParameters = null;
//					if (jobRequestMap.get(softwareParameter.getKey()) == null) {
////						explodedParameters = new String[]{};
//						continue;
//					} else if (jobRequestMap.get(softwareParameter.getKey()) instanceof String[]) {
//						explodedParameters = (String[])jobRequestMap.get(softwareParameter.getKey());
//
//					} else {
//						explodedParameters = StringUtils.split((String)jobRequestMap.get(softwareParameter.getKey()), ";");
////						explodedParameters = new String[]{(String)pTable.get(softwareParameter.getKey())};
//					}
//
//					if (softwareParameter.getMinCardinality() > explodedParameters.length)
//					{
//						throw new JobProcessingException(400,
//								softwareParameter.getKey() + " requires at least " +
//										softwareParameter.getMinCardinality() + " values");
//					}
//					else if (softwareParameter.getMaxCardinality() != -1 &&
//						softwareParameter.getMaxCardinality() < explodedParameters.length)
//					{
//						throw new JobProcessingException(400,
//								softwareParameter.getKey() + " may have at most " +
//								softwareParameter.getMaxCardinality() + " values");
//					}
//					else if (softwareParameter.getType().equals(SoftwareParameterType.enumeration))
//					{
//						List<String> validParamValues = null;
//						try {
//							validParamValues = softwareParameter.getEnumeratedValuesAsList();
//						} catch (SoftwareException e) {
//							throw new JobProcessingException(400,
//									"Unable to validate parameter value for " + softwareParameter.getKey() +
//									" against the enumerated values defined for this parameter.", e);
//						}
//
//						if (validParamValues.isEmpty())
//						{
//							throw new JobProcessingException(400,
//									"Invalid parameter value for " + softwareParameter.getKey() +
//									". Value must be one of: " + ServiceUtils.explode(",  ", validParamValues));
//						}
//						else if (explodedParameters.length == 0) {
//							continue;
//						}
//						else
//						{
//							for (String jobParam: explodedParameters)
//							{
//								if (validParamValues.contains(jobParam))
//								{
//									if (explodedParameters.length == 1) {
//										jobParameters.put(softwareParameter.getKey(), jobParam);
//									} else {
//										validatedJobParamValueArray.add(jobParam);
//									}
//								}
//								else
//								{
//									throw new JobProcessingException(400,
//											"Invalid parameter value, " + jobParam + ", for " + softwareParameter.getKey() +
//											". Value must be one of: " + ServiceUtils.explode(",  ", validParamValues));
//								}
//							}
//
//							if (validatedJobParamValueArray.size() > 1) {
//								jobParameters.put(softwareParameter.getKey(), validatedJobParamValueArray);
//							}
//						}
//					}
//					else if (softwareParameter.getType().equals(SoftwareParameterType.bool) ||
//							softwareParameter.getType().equals(SoftwareParameterType.flag))
//					{
//						if (explodedParameters.length > 1)
//						{
//							throw new JobProcessingException(400,
//									"Invalid parameter value for " + softwareParameter.getKey() +
//									". Boolean and flag parameters do not support multiple values.");
//						}
//						else if (explodedParameters.length == 0) {
//							continue;
//						}
//						else
//						{
//							String inputValue = explodedParameters[0];
//							if (inputValue.toString().equalsIgnoreCase("true")
//									|| inputValue.toString().equals("1")
//									|| inputValue.toString().equalsIgnoreCase("on"))
//							{
//								jobParameters.put(softwareParameter.getKey(), true);
//							}
//							else if (inputValue.toString().equalsIgnoreCase("false")
//									|| inputValue.toString().equals("0")
//									|| inputValue.toString().equalsIgnoreCase("off"))
//							{
//								jobParameters.put(softwareParameter.getKey(), false);
//							}
//							else
//							{
//								throw new JobProcessingException(400,
//										"Invalid parameter value for " + softwareParameter.getKey() +
//										". Value must be a boolean value. Use 1,0 or true/false as available values.");
//							}
//						}
//					}
//					else if (softwareParameter.getType().equals(SoftwareParameterType.number))
//					{
//						if (explodedParameters.length == 0) {
//							continue;
//						}
//						else
//						{
//							for (String jobParam: explodedParameters)
//							{
//								try
//								{
//									if (NumberUtils.isDigits(jobParam))
//									{
//										if (ServiceUtils.doesValueMatchValidatorRegex(jobParam, softwareParameter.getValidator())) {
//											if (explodedParameters.length == 1) {
//												jobParameters.put(softwareParameter.getKey(), new Long(jobParam));
//											} else {
//												validatedJobParamValueArray.add(new Long(jobParam));
//											}
//										} else {
//											throw new JobProcessingException(400,
//													"Invalid parameter value for " + softwareParameter.getKey() +
//													". Value must match the regular expression " +
//													softwareParameter.getValidator());
//										}
//
//									}
//									else if (NumberUtils.isNumber(jobParam))
//									{
//										if (ServiceUtils.doesValueMatchValidatorRegex(jobParam, softwareParameter.getValidator())) {
//											if (explodedParameters.length == 1) {
//												jobParameters.put(softwareParameter.getKey(), new BigDecimal(jobParam).toPlainString());
//											} else {
//												validatedJobParamValueArray.add(new BigDecimal(jobParam).toPlainString());
//											}
//
//										} else {
//											throw new JobProcessingException(400,
//													"Invalid parameter value for " + softwareParameter.getKey() +
//													". Value must match the regular expression " +
//													softwareParameter.getValidator());
//										}
//									}
//								} catch (NumberFormatException e) {
//									throw new JobProcessingException(400,
//											"Invalid parameter value for " + softwareParameter.getKey() +
//											". Value must be a number.");
//								}
//							}
//
//							if (validatedJobParamValueArray.size() > 1) {
//								jobParameters.put(softwareParameter.getKey(), validatedJobParamValueArray);
//							}
//						}
//					}
//					else // string parameter
//					{
//						if (explodedParameters.length == 0) {
//							continue;
//						}
//						else
//						{
//							for (String jobParam: explodedParameters)
//							{
//								if (jobParam == null)
//								{
//									continue;
//								}
//								else
//								{
//									if (ServiceUtils.doesValueMatchValidatorRegex(jobParam, softwareParameter.getValidator()))
//									{
//										validatedJobParamValueArray.add(jobParam);
//									}
//									else
//									{
//										throw new JobProcessingException(400,
//												"Invalid parameter value for " + softwareParameter.getKey() +
//												". Value must match the regular expression " +
//												softwareParameter.getValidator());
//									}
//								}
//							}
//
//							if (validatedJobParamValueArray.size() == 1) {
//								jobParameters.put(softwareParameter.getKey(), validatedJobParamValueArray.iterator().next().asText());
//							} else {
//								jobParameters.put(softwareParameter.getKey(), validatedJobParamValueArray);
//							}
//						}
//					}
//				}
//			}
//			catch (JobProcessingException e) {
//				throw e;
//			}
//			catch (Exception e)
//			{
//				throw new JobProcessingException(500,
//						"Failed to parse parameter "+ softwareParameter.getKey(), e);
//			}
//		}
//
//		/***************************************************************************
//         **                 Create and assign job data                            **
//         ***************************************************************************/
//
//        try
//        {
//            // create a job object
//            job.setName(name);
//            job.setOwner(username);
//            job.setSoftwareName(software.getUniqueName());
//            job.setInternalUsername(internalUsername);
//            job.setSystem(software.getExecutionSystem().getSystemId());
//            job.setBatchQueue(jobQueue.getName());
//            job.setNodeCount(nodeCount);
//            job.setProcessorsPerNode(processorsPerNode);
//            job.setMemoryPerNode(memoryPerNode);
//            job.setMaxRunTime(requestedTime);
//            // bridget between the old callback urls and the new multiple webhook support.
//            for (Notification n: notifications) {
//                job.addNotification(n);
//            }
//            job.setInputsAsJsonObject(jobInputs);
//            job.setParametersAsJsonObject(jobParameters);
//            job.setSubmitTime(new DateTime().toDate());
//        }
//        catch (JobException e) {
//            throw new JobProcessingException(500, e.getMessage(), e);
//        }
//        catch (NotificationException e) {
//            throw new JobProcessingException(500, "Failed to assign notification to job", e);
//        }
//
//
//		/***************************************************************************
//		 **						Verifying archive configuration					  **
//		 ***************************************************************************/
//
//        // default to archiving the output
//		job.setArchiveOutput(true);
//
//		if (jobRequestMap.containsKey("archive") && StringUtils.isNotEmpty((String)jobRequestMap.get("archive")))
//		{
//		    String doArchive = (String)jobRequestMap.get("archive");
//		    if (!BooleanUtils.toBoolean(doArchive) && !doArchive.equals("1")) {
//			    job.setArchiveOutput(false);
//			}
//		}
//
//		RemoteSystem archiveSystem;
//		if (job.isArchiveOutput() && jobRequestMap.containsKey("archiveSystem"))
//	    {
//			// lookup the user system
//			String archiveSystemId = (String)jobRequestMap.get("archiveSystem");
//			archiveSystem = new SystemDao().findUserSystemBySystemId(username, archiveSystemId, RemoteSystemType.STORAGE);
//			if (archiveSystem == null) {
//				throw new JobProcessingException(400,
//						"No storage system found matching " + archiveSystem + " for " + username);
//			}
//		}
//		else
//		{
//		    // grab the user's default storage system
//            archiveSystem = systemManager.getUserDefaultStorageSystem(username);
//
//            if (job.isArchiveOutput() && archiveSystem == null) {
//				throw new JobProcessingException(400,
//						"Invalid archiveSystem. No archiveSystem was provided and you "
//						+ "have no public or private default storage system configured. "
//						+ "Please specify a valid system id for archiveSystem or configure "
//						+ "a default storage system.");
//			}
//		}
//
//		job.setArchiveSystem(archiveSystem);
//
//		String archivePath = null;
//		if (job.isArchiveOutput())
//		{
//		    if (jobRequestMap.containsKey("archivePath"))
//		    {
//    		    archivePath = (String)jobRequestMap.get("archivePath");
//
//    		    if (StringUtils.isNotEmpty(archivePath))
//                {
//    //	            if (!archivePath.startsWith("/"))
//    //	                archivePath = "/" + archivePath;
//
//                    // resolve any macros from the user-supplied archive path into valid values based
//                    // on the job request and use those
//                    archivePath = JobArchivePathMacroType.resolveMacrosInPath(job, archivePath);
//
//                    RemoteDataClient remoteDataClient = null;
//                    try
//                    {
//                        remoteDataClient = archiveSystem.getRemoteDataClient(internalUsername);
//                        remoteDataClient.authenticate();
//
//                        LogicalFile logicalFile = LogicalFileDao.findBySystemAndPath(archiveSystem, archivePath);
//                        PermissionManager pm = new PermissionManager(archiveSystem, remoteDataClient, logicalFile, username);
//
//                        if (!pm.canWrite(remoteDataClient.resolvePath(archivePath)))
//                        {
//                            throw new JobProcessingException(403,
//                                    "User does not have permission to access the provided archive path " + archivePath);
//                        }
//                        else
//                        {
//                            if (!remoteDataClient.doesExist(archivePath))
//                            {
//                                if (!remoteDataClient.mkdirs(archivePath, username)) {
//                                    throw new JobProcessingException(400,
//                                            "Unable to create job archive directory " + archivePath);
//                                }
//                            }
//                            else
//                            {
//                                if (!remoteDataClient.isDirectory(archivePath))
//                                {
//                                    throw new JobProcessingException(400,
//                                            "Archive path is not a folder");
//                                }
//                            }
//                        }
//                    }
//                    catch (JobProcessingException e) {
//                        throw e;
//                    }
//                    catch (RemoteDataException e) {
//                        int httpcode = 500;
//                        if (e.getMessage().contains("No credentials associated")) {
//                            httpcode = 400;
//                        }
//                        throw new JobProcessingException(httpcode, e.getMessage(), e);
//                    }
//                    catch (Exception e) {
//                        throw new JobProcessingException(500, "Could not verify archive path", e);
//                    }
//                    finally {
//                        try { remoteDataClient.disconnect(); } catch (Exception e) {}
//                    }
//    			}
//		    }
//		}
//
//		if (StringUtils.isEmpty(archivePath)) {
//		    archivePath = username + "/archive/jobs/job-" + job.getUuid();
//        }
//
//		try
//        {
//		    job.setArchivePath(archivePath);
//
//            // persisting the job makes it available to the job queue
//            // for submission
//            JobDao.persist(job);
//            job.setStatus(JobStatusType.PENDING, JobStatusType.PENDING.getDescription());
//            JobDao.persist(job);
//
//            return job;
//        }
//        catch (Exception e)
//        {
//            throw new JobProcessingException(500, e.getMessage(), e);
//        }
	}

	/**
	 * Finds queue on the given executionSystem that supports the given number of nodes and
	 * memory per node given.
	 *
	 * @param nodes a positive integer value or -1 for no limit
	 * @param processors positive integer value or -1 for no limit
	 * @param memory memory in GB or -1 for no limit
	 * @param requestedTime time in hh:mm:ss format
	 * @return a BatchQueue matching the given parameters or null if no match can be found
	 */
	public static BatchQueue selectQueue(ExecutionSystem executionSystem, Long nodes, Double memory, String requestedTime)
	{

		return selectQueue(executionSystem, nodes, memory, (long)-1, requestedTime);
	}

	/**
	 * Finds queue on the given executionSystem that supports the given number of nodes and
	 * memory per node given.
	 *
	 * @param nodes a positive integer value or -1 for no limit
	 * @param processors positive integer value or -1 for no limit
	 * @param memory memory in GB or -1 for no limit
	 * @param requestedTime time in hh:mm:ss format
	 * @return a BatchQueue matching the given parameters or null if no match can be found
	 */
	public static BatchQueue selectQueue(ExecutionSystem executionSystem, Long nodes, Double memory, Long processors, String requestedTime)
	{

		if (validateBatchSubmitParameters(executionSystem.getDefaultQueue(), nodes, processors, memory, requestedTime))
		{
			return executionSystem.getDefaultQueue();
		}
		else
		{
			BatchQueue[] queues = executionSystem.getBatchQueues().toArray(new BatchQueue[]{});
			Arrays.sort(queues);
			for (BatchQueue queue: queues)
			{
				if (queue.isSystemDefault())
					continue;
				else if (validateBatchSubmitParameters(queue, nodes, processors, memory, requestedTime))
					return queue;
			}
		}

		return null;
	}


	/**
	 * Validates that the queue supports the number of nodes, processors per node, memory and
	 * requestedTime provided. If any of these values are null or the given values exceed the queue
	 * limits, it returns false.
	 *
	 * @param queue the BatchQueue to check against
	 * @param nodes a positive integer value or -1 for no limit
	 * @param processors positive integer value or -1 for no limit
	 * @param memory memory in GB or -1 for no limit
	 * @param requestedTime time in hh:mm:ss format
	 * @return true if all the values are non-null and within the limits of the queue
	 */
	public static boolean validateBatchSubmitParameters(BatchQueue queue, Long nodes, Long processors, Double memory, String requestedTime)
	{
		if (queue == null ||
			nodes == null ||  nodes == 0 || nodes < -1 ||
			processors == null || processors == 0 || processors < -1 ||
			memory == null || memory == 0 || memory < -1 ||
			StringUtils.isEmpty(requestedTime) || StringUtils.equals("00:00:00", requestedTime))
		{
			return false;
		}

		if (queue.getMaxNodes() > 0 && queue.getMaxNodes() < nodes) {
			return false;
		}

		if (queue.getMaxProcessorsPerNode() > 0 && queue.getMaxProcessorsPerNode() < processors) {
			return false;
		}

		if (queue.getMaxMemoryPerNode() > 0 && queue.getMaxMemoryPerNode() < memory) {
			return false;
		}

		if (queue.getMaxRequestedTime() != null &&
				TimeUtils.compareRequestedJobTimes(queue.getMaxRequestedTime(), requestedTime) == -1)

		{
			return false;
		}

		return true;
	}

	/**
	 * Returns a map of all inputs needed to run the job comprised of the user-supplied
	 * inputs as well as the default values for hidden and unspecified, but required inputs.
	 * This is needed during staging and job submission because the original job submission
	 * may not contain all the inputs actually needed to run the job depending on whether
	 * or not there are hidden fields in the app description.
	 *
	 * @param job
	 * @return
	 * @throws JobException
	 */
	public static Map<String, String[]> getJobInputMap(Job job) throws JobException
	{
		try
		{
			Map<String, String[]> map = new HashMap<String, String[]>();

			JsonNode jobInputJson = job.getInputsAsJsonObject();
			Software software = SoftwareDao.getSoftwareByUniqueName(job.getSoftwareName());

			for (SoftwareInput input: software.getInputs())
			{
				if (jobInputJson.has(input.getKey()))
				{
					JsonNode inputJson = jobInputJson.get(input.getKey());
					String[] inputValues = null;
					if (inputJson == null || inputJson.isNull() || (inputJson.isArray() && inputJson.size() == 0))
					{
						// no inputs, don't even include it in the map
						continue;
					}
					else if (inputJson.isArray())
					{
						// should be an array of
						inputValues = ServiceUtils.getStringValuesFromJsonArray((ArrayNode)inputJson, false);
					}
					else
					{
						inputValues = new String[]{ inputJson.textValue() };
					}

					map.put(input.getKey(), inputValues);
				}
				else if (!input.isVisible())
 				{
					String[] inputValues = ServiceUtils.getStringValuesFromJsonArray(input.getDefaultValueAsJsonArray(), false);
					map.put(input.getKey(), inputValues);
 				}
			}

			return map;
		}
		catch (Throwable e)
		{
			throw new JobException("Unable to parse job and app inputs", e);
		}

	}

    /**
     * Determines whether the job has completed archiving and can thus
     * refer to the archive location for requests for its output data.
     *
     * @param job
     * @return
     */
    public static boolean isJobDataFullyArchived(Job job)
    {
        if (job.isArchiveOutput())
        {
            if (job.getStatus() == JobStatusType.ARCHIVING_FINISHED) {
                return true;
            }
            else if (job.getStatus() == JobStatusType.FINISHED) {
                for (JobEvent event: job.getEvents()) {
                    if (StringUtils.equalsIgnoreCase(event.getStatus(), JobStatusType.ARCHIVING_FINISHED.name())) {
                        return true;
                    }
                }
            }
        }

        // anything else means the job failed, hasn't reached a point of
        // archiving, is in process, or something happened.
        return false;
    }

	/**
	 * Rolls a {@link Job} back to the previously active state based on its current {@link JobStatusType}.
	 *  
	 * @param job the job to reset
	 * @param requestedBy the principal requesting the job be reset
	 * @throws JobException 
	 * @throws JobDependencyException 
	 */
	public static Job resetToPreviousState(Job job, String requestedBy) 
	throws JobException, JobDependencyException 
	{
		if (job == null) {
			throw new JobException("Job cannot be null");
		}
		
		ZombieJobWatch zombieJob = new ZombieJobWatch();
		Job updatedJob = null;
		try {
			updatedJob = zombieJob.rollbackJob(job, requestedBy);
			
			JobEvent event = new JobEvent("RESET", "Job was manually reset to " + 
					updatedJob.getStatus().name() + " by " + requestedBy, requestedBy);
			event.setJob(updatedJob);
			updatedJob.addEvent(event);
			
			JobDao.persist(updatedJob);
			return updatedJob;
		}
		catch (JobException e) {
			throw new JobException("Failed to reset job to previous state.", e);
		}
	}
}
