/**
 *
 */
package org.iplantc.service.jobs.managers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.StaleObjectStateException;
import org.hibernate.UnresolvableObjectException;
import org.iplantc.service.apps.dao.SoftwareDao;
import org.iplantc.service.apps.model.Software;
import org.iplantc.service.apps.model.SoftwareInput;
import org.iplantc.service.apps.model.SoftwareParameter;
import org.iplantc.service.apps.util.ServiceUtils;
import org.iplantc.service.common.util.TimeUtils;
import org.iplantc.service.jobs.Settings;
import org.iplantc.service.jobs.dao.JobDao;
import org.iplantc.service.jobs.dao.JobEventDao;
import org.iplantc.service.jobs.exceptions.JobDependencyException;
import org.iplantc.service.jobs.exceptions.JobException;
import org.iplantc.service.jobs.exceptions.JobProcessingException;
import org.iplantc.service.jobs.exceptions.JobTerminationException;
import org.iplantc.service.jobs.managers.killers.JobKiller;
import org.iplantc.service.jobs.managers.killers.JobKillerFactory;
import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.jobs.model.JobEvent;
import org.iplantc.service.jobs.model.JobUpdateParameters;
import org.iplantc.service.jobs.model.enumerations.JobEventType;
import org.iplantc.service.jobs.model.enumerations.JobStatusType;
import org.iplantc.service.jobs.phases.queuemessages.StopJobMessage;
import org.iplantc.service.jobs.phases.utils.TopicMessageSender;
import org.iplantc.service.jobs.phases.utils.ZombieJobUtils;
import org.iplantc.service.jobs.util.Slug;
import org.iplantc.service.remote.exceptions.RemoteExecutionException;
import org.iplantc.service.systems.dao.SystemDao;
import org.iplantc.service.systems.exceptions.SystemUnavailableException;
import org.iplantc.service.systems.exceptions.SystemUnknownException;
import org.iplantc.service.systems.model.BatchQueue;
import org.iplantc.service.systems.model.ExecutionSystem;
import org.iplantc.service.systems.model.RemoteSystem;
import org.iplantc.service.systems.model.enumerations.SystemStatusType;
import org.iplantc.service.transfer.RemoteDataClient;
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

		if (log.isDebugEnabled())
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
				if (log.isDebugEnabled())
				    log.debug("Successfully deleted remote work directory " + remoteWorkPath + " for failed job " + job.getUuid());
				job = JobManager.updateStatus(job, JobStatusType.STAGING_INPUTS, "Completed cleaning up remote work directory.");
			} else {
			    if (log.isDebugEnabled())
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
			// if it's not in queue, just update the status.
			job = stopRunningJob(job, "Job cancelled by user.");
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
                            + ". Response from " + job.getSystem() + ": " + e.getMessage());

                    throw new JobTerminationException(message, e);
			    }
                catch (RemoteExecutionException e) {

                    job = killer.getJob();

                    String message = "Failed to kill job " + job.getUuid()
                            + " identified by id " + job.getLocalJobId() + " on " + job.getSystem()
                            + ". Response from " + job.getSystem() + ": " + e.getMessage();

                    log.debug(message);

                    job = JobManager.updateStatus(job, JobStatusType.FAILED, "Failed to kill job "
                            + " identified by id " + job.getLocalJobId() + " on " + job.getSystem()
                            + ". Response from " + job.getSystem() + ": " + e.getMessage());

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
			// exception.
			job = stopRunningJob(job, null);
			return job;
		}
	}

	/** Stop a running job by doing the following: 
	 * 
	 *     1. Change the job status
	 *     2. Cancel transfers
	 *     3. Interrupt the job's worker thread
	 * 
	 * @param job to the job to be stopped
	 * @param stopMsg a custom message saved with the status change or 
	 *         null to save the standard message
	 * @return the refreshed job object
	 * @throws JobException on error
	 */
	private static Job stopRunningJob(Job job, String stopMsg) throws JobException
	{
	    // ----- Update the job status.
	    if (stopMsg == null) stopMsg = JobStatusType.STOPPED.getDescription();
        job = JobManager.updateStatus(job, JobStatusType.STOPPED, stopMsg);

        // ----- Cancel transfers.
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
        
        // ----- Interrupt the job's worker thread.
        StopJobMessage message = new StopJobMessage(job.getName(), job.getUuid(), job.getTenantId());
        try {TopicMessageSender.sendJobMessage(message);}
        catch (Exception e) {
            String msg = "Unable to send job interrupt message: " + message.toString();
            log.error(msg, e);
        }

        return job;
	}
	
    /** Rollback a job to a prior status by performing the following: 
     * 
     *     1. Atomically 
     *          a. change the job status 
     *          b. remove existing publish records
     *          c. remove existing interrupt records
     *     2. Cancel job transfers
     *     3. Interrupt the job's worker thread
     * 
     * @param job to the job to be rolled back
     * @param rollbackMessage a custom message saved with the status change or 
     *                        null to save the standard message
     * @return the refreshed job object
     * @throws JobException on error
     */
    public static Job rollbackJob(Job job, String rollbackMessage) throws JobException
    {
        // Make sure we have a job.
        if (job == null) {
            String msg = "Cannot rollback null job.";
            log.error(msg);
            throw new JobException(msg);
        }
        
        // ----- Stop the job.
        // Stop the job if it's not in a finished or an archiving finished state.
        // After the statement below executes, the job will not be picked up 
        // by any scheduler because it will be in a non-trigger status.  If 
        // stopRunningJob() is called, it will issue an interrupt.  In the 
        // interim between this finished state and the upcoming call DAO call
        // to rollback the job, a worker processing the job may or may not 
        // process an interrupt that causes it to abandon processing the job. 
        // that causes it to stop processing.  
        //
        // Not that it is not safe to assume no worker is processing a job during 
        // rollback processing.  More precisely, a job can be in one of the 
        // following states AFTER the statement below executes:
        //
        //   The job is:
        //      - in a phase scheduler queue, or
        //      - about to be put into a phase scheduler queue, or
        //      - processing in a worker, or
        //      - being ignored by all schedulers and not in any queue.
        //
        // The last case requires no extra work.  The first three cases require
        // a current or future worker to ignore or stop processing the job being 
        // rolled back.
        if (!JobStatusType.isFinished(job.getStatus()) && 
            !JobStatusType.isArchived(job.getStatus()))
           job = stopRunningJob(job, "Preparing for rollback by stopping job");

        // ----- Update the job status and remove publish records.
        // Rollback routine will provide the default rollback message if necessary.  
        // Once the dao rolls back the job, it can be rescheduled by the rollback
        // scheduler.  There is no need for a rollback interrupt message since either
        // (1) the previous stopRunningJob() call issued one or (2) the job was 
        // already in a finished state and, therefore, any necessary interrupt has
        // already been sent.
        job = JobDao.rollback(job, rollbackMessage);

        return job;
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
		
			if ((job != null) && !job.isVisible()) {
				try {
				    // Update visible flag.
				    JobUpdateParameters jobUpdateParameters = new JobUpdateParameters();
				    jobUpdateParameters.setVisible(true);
					JobDao.update(job, jobUpdateParameters);
					
					// Add event to job.
					job.addEvent(new JobEvent(
							JobEventType.RESTORED.name(), 
							"Job was restored by " + invokingUsername,
							invokingUsername));
					
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
            if ((job != null) && !job.isRunning())
            {
                try {
                    // Update visible flag.
                    JobUpdateParameters jobUpdateParameters = new JobUpdateParameters();
                    jobUpdateParameters.setVisible(false);
                    JobDao.update(job, jobUpdateParameters);
                    
                    // Add event to job.
                    job.addEvent(new JobEvent(
                            JobEventType.DELETED.name(), 
                            "Job was deleted by user " + invokingUsername,
                            invokingUsername));
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
                    // Update visible flag and status.
                    JobUpdateParameters jobUpdateParameters = new JobUpdateParameters();
                    jobUpdateParameters.setVisible(false);
                    jobUpdateParameters.setStatus(JobStatusType.STOPPED);
                    jobUpdateParameters.setErrorMessage("Job stopped by user " + invokingUsername);
                    
                    // Update dates.
                    Date jobHiddenDate = new DateTime().toDate();
                    jobUpdateParameters.setLastUpdated(jobHiddenDate);
                    jobUpdateParameters.setEndTime(jobHiddenDate);
                    JobDao.update(job, jobUpdateParameters);
                    
                    // Add event to job.
                    job.addEvent(new JobEvent(
                            JobEventType.DELETED.name(), 
                            "Job was deleted by user " + invokingUsername,
                            invokingUsername));
                    
                    // Interrupt the worker thread that might be processing this job now.
                    StopJobMessage message = new StopJobMessage(job.getName(), job.getUuid(), job.getTenantId());
                    try {TopicMessageSender.sendJobMessage(message);}
                    catch (Exception e) {
                        String msg = "Unable to send job interrupt message: " + message.toString();
                        log.error(msg, e);
                    }
                }
            }
        }
        
        return job;
    }
    
    
	/**
	 * Updates the status of a job, updates the timestamps as appropriate
	 * based on the status, and writes a new JobEvent to the job's history.
	 * 
	 * If present, the optional extraUpdates object can contain updates to job fields
	 * other than the status field.  These updates will be applied in the same 
	 * transaction as the status update if that update is actually performed.  The
	 * method's status parameter takes precedence over a status setting in the 
	 * extraUpdates object.
	 *
	 * @param job
	 * @param status
	 * @param extraUpdates optional update parameters other than status
	 * @throws JobException
	 */
	public static Job updateStatus(Job job, JobStatusType status, JobUpdateParameters... extraUpdates)
			throws JobException
	{
		return updateStatus(job, status, status.getDescription(), extraUpdates);
	}

    /**
     * Updates the status of a job, its timestamps, and writes a new
     * JobEvent to the job's history with the given status and message.
     *
     * If present, the optional extraUpdates object can contain updates to job fields
     * other than the status and errorMessage fields.  These updates will be applied in 
     * the same transaction as the status update if that update is actually performed.  
     * The method's status and error message parameters take precedence over settings of
     * those field in the extraUpdates object.
     *
     * @param job
     * @param status
     * @param errorMessage
     * @param extraUpdates optional update parameters other than status and errorMessage
     * @return Updated job object
     * @throws JobException
     */
    public static Job updateStatus(Job job, JobStatusType status, String errorMessage, 
                                   JobUpdateParameters... extraUpdates)
    throws JobException
    {
        // The called method determine if the event gets sent.
        JobEvent event = new JobEvent(job, status, errorMessage, job.getOwner());
        return updateStatus(job, status, event, extraUpdates);
    }

	/**
	 * Updates the status of a job, its timestamps, and writes a new
	 * JobEvent to the job's history with the given status and message.
	 *
     * This method and JobDao.update() comprise the two ways that status updates
     * can be safely performed.  This method calls JobManager.update(), which  
     * is ultimately responsible for validating status changes and locking
     * the job record during status transitions.  Use this method if you require 
     * visibility and event processing; use JobDao.update() if you only need
     * to update a job record and don't want side-effects.
     *   
     * If present, the optional extraUpdates object can contain updates to job fields
     * other than the status field.  These updates will be applied in the same 
     * transaction as the status update if that update is actually performed.  The
     * method's status parameter takes precedence over a status setting in the 
     * extraUpdates object; the event's description field takes precedence over
     * an errorMessage setting in the extraUpdates object.
     *
	 * @param job
	 * @param status
	 * @param event
	 * @param extraUpdates optional update parameters other than status and errorMessage
	 * @return Updated job object
	 * @throws JobException
	 */
	public static Job updateStatus(Job job, JobStatusType status, JobEvent event, 
	                               JobUpdateParameters... extraUpdates)
	throws JobException
	{
	    // ----------------- Initialize Parms ----------------------
	    // Create a new parms object if the user did not provide one.
        JobUpdateParameters parms;
        if (extraUpdates.length > 0) parms = extraUpdates[0];
          else parms = new JobUpdateParameters();
        
        // ----------------- Job Processing ------------------------ 
	    // Determine if the status and message have changed on visible jobs.
	    if (job.isVisible() &&
	        ((status != job.getStatus()) ||
	        !StringUtils.equals(job.getErrorMessage(), event.getDescription())))
	    {
	        // We only change the status on visible jobs 
	        // when the status or message has changed.
	        parms.setStatus(status);
	        parms.setErrorMessage(event.getDescription());
	    }
	    else 
	    {
            // Avoid conflicting signals between explicit 
            // and implicit method parameters when we do
	        // not want the status or message updated. 
            parms.unsetStatus();
            parms.unsetErrorMessage();
	    }
	    
        // Always update dates and timestamps without overwriting existing ones.
        assignStatusDates(job, parms, status);
        
        // Write the job record and read it back.  If the status was changed
        // and the transition is not legal, an exception will be thrown.
        JobDao.update(job, parms);
        
        // ----------------- Event Processing ---------------------- 
        // Either process the event or simply record it 
        // depending on whether the job is deleted.
        if (job.isVisible()) job.addEvent(event);
            else 
            {
                // Indicate that event is ignored before recording it.
                event.setDescription(event.getDescription() + 
                        " Event will be ignored because job has been deleted.");
                JobEventDao.persist(event);
            }
	    
        // Return updated job object.
		return job;
	}
	
    /** Set various job date fields when changing status.  Date fields
     * in the update parameter are assigned based on the new status and
     * whether the job already has the field assigned.  If the job has
     * a timestamp field assigned, this method will NOT attempt to update
     * it to avoid overwriting a previously assigned value in the database. 
     * 
     * @param job the job who timestamps are not to be overwritten or rewrittens
     * @param jobParms the job update parameter specified how to update the job record
     * @param status the new status being assigned to the job
     */
    private static void assignStatusDates(Job job, JobUpdateParameters jobParms, 
                                          JobStatusType status)
    {
        // Get current timestamp.
        Date date = new DateTime().toDate();
        
        // Make sure the lastUpdated time was always explicitly set.
        if (!jobParms.isLastUpdatedFlag()) jobParms.setLastUpdated(date);
        
        // Determine if any of the other timestamps should be updated.
        // Don't overwrite timestamps already in the job unless explicitly
        // set in the parameter object.
        switch (status)
        {
            case QUEUED:
                if ((job.getSubmitTime() == null) && !jobParms.isSubmitTimeFlag()) 
                    jobParms.setSubmitTime(date);
                break;
                
            case RUNNING:
                if ((job.getStartTime() == null) && !jobParms.isStartTimeFlag())
                    jobParms.setStartTime(date);
                break;
                
            case FINISHED:
            case KILLED:
            case STOPPED:
            case FAILED:
                if ((job.getEndTime() == null) && !jobParms.isEndTimeFlag())
                    jobParms.setEndTime(date);
                break;
                
            default:
                break;
        }
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
		ExecutionSystem executionSystem = (ExecutionSystem) new SystemDao().findBySystemId(job.getSystem());

		if (executionSystem == null || !executionSystem.isAvailable() || !executionSystem.getStatus().equals(SystemStatusType.UP))
		{
			throw new SystemUnavailableException("Job execution system " + executionSystem.getSystemId() + " is not available.");
		}

		if (log.isDebugEnabled())
		    log.debug("Beginning archive inputs for job " + job.getUuid() + " " + job.getName());

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
				    if (log.isDebugEnabled())
				        log.debug("No archive file found for job " + job.getUuid() + " on system " +
							executionSystem.getSystemId() + " at " + remoteArchiveFile +
							". Entire job directory will be archived.");
					job = JobManager.updateStatus(job, JobStatusType.ARCHIVING,
							"No archive file found. Entire job directory will be archived.");
				}
			}
			catch (Exception e)
			{
			    if (log.isDebugEnabled())
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

			// Add an event to the job.
			job.addEvent(new JobEvent(
					job.getStatus(),
					"Archiving " + rootTask.getSource() + " to " + rootTask.getDest(),
					rootTask,
					job.getOwner()));

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
    				executionDataClient.delete(job.getWorkPath());
    			    JobManager.updateStatus(job, JobStatusType.ARCHIVING_FINISHED,
                            "Job archiving completed successfully.");
			}
			catch (Exception e) {
				log.error("Archiving of job " + job.getUuid() + " completed, "
					+ "but an error occurred deleting the remote work directory "
					+ job.getUuid(), e);
			}
		}
		catch (StaleObjectStateException e) {
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
     *  TODO: fix this shit
     * @param job
     * @return
     * @throws JobException 
     */
    public static boolean isJobDataFullyArchived(Job job)
    {
        if (job.isArchiveOutput())
        {
            if (job.getStatus() == JobStatusType.ARCHIVING_FINISHED) {
                return true;
            }
            else if (job.getStatus() == JobStatusType.FINISHED) {
                try {
                    for (JobEvent event: JobEventDao.getByJobId(job.getId())) {
                        if (StringUtils.equalsIgnoreCase(event.getStatus(), JobStatusType.ARCHIVING_FINISHED.name())) {
                            return true;
                        }
                    }
                }
                catch (Exception e)
                {
                    // Log, swallow the exception, and then quit.
                    String msg = "Unable to access job events for job " + job.getId() + 
                                 " with unique id " + job.getUuid() + ".";
                    log.error(msg, e);
                }
            }
        }

        // anything else means the job failed, hasn't reached a point of
        // archiving, is in process, or something happened.
        return false;
    }
    
    /**
     * Determines whether the job ever began archiving
     * @param job
     * @return
     * @throws JobException 
     */
    public static boolean isJobDataPartiallyArchived(Job job) throws JobException
    {
        if (job.isArchiveOutput())
        {
            if (job.getStatus() == JobStatusType.ARCHIVING || 
            		job.getStatus() == JobStatusType.ARCHIVING_FINISHED || 
            		job.getStatus() == JobStatusType.ARCHIVING_FAILED) {
                return true;
            }
            else if (job.getStatus() == JobStatusType.FINISHED || 
            		job.getStatus() == JobStatusType.FAILED) {
                for (JobEvent event: JobEventDao.getByJobId(job.getId())) {
                    if (StringUtils.equalsIgnoreCase(event.getStatus(), JobStatusType.ARCHIVING.name())) {
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
		
		Job updatedJob = null;
		try {
			updatedJob = ZombieJobUtils.rollbackJob(job, requestedBy);
			
			JobEvent event = new JobEvent("RESET", "Job was manually reset to " + 
					updatedJob.getStatus().name() + " by " + requestedBy, requestedBy);
			updatedJob.addEvent(event);
			
			return updatedJob;
		}
		catch (JobException e) {
			throw new JobException("Failed to reset job to previous state.", e);
		}
	}
}
