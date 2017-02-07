package org.iplantc.service.jobs.phases.utils;

import static org.iplantc.service.jobs.model.enumerations.JobStatusType.ARCHIVING;
import static org.iplantc.service.jobs.model.enumerations.JobStatusType.PENDING;
import static org.iplantc.service.jobs.model.enumerations.JobStatusType.STAGED;
import static org.iplantc.service.jobs.model.enumerations.JobStatusType.isFinished;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.jobs.dao.JobEventDao;
import org.iplantc.service.jobs.exceptions.JobDependencyException;
import org.iplantc.service.jobs.exceptions.JobException;
import org.iplantc.service.jobs.managers.JobManager;
import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.jobs.model.JobEvent;
import org.iplantc.service.jobs.model.enumerations.JobStatusType;
import org.iplantc.service.notification.managers.NotificationManager;
import org.iplantc.service.systems.dao.SystemDao;
import org.iplantc.service.systems.model.RemoteSystem;
import org.iplantc.service.transfer.dao.TransferTaskDao;
import org.iplantc.service.transfer.model.enumerations.TransferTaskEventType;

/** Utilities shared by front-end request handling code and scheduler code.
 * 
 * @author rcardone
 */
public class ZombieJobUtils
{
    /* ********************************************************************** */
    /*                               Constants                                */
    /* ********************************************************************** */
    // Tracing.
    private static final Logger _log = Logger.getLogger(ZombieJobUtils.class);

    /* ********************************************************************** */
    /*                             Public Methods                             */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* rollbackJob:                                                            */
    /* ---------------------------------------------------------------------- */
    /**
     * Rolls back a {@link Job} status to the last stable status prior to the 
     * current one. This is akin to release a job back to the queue from which 
     * it was previously taken. This is helpful in situations where a job is 
     * stuck due to a failed worker or other abandoned process which left the 
     * job is a zombie state from which it would not otherwise recover.
     * 
     * @param job the job that will be rolled back
     * @param callingUsername the principal requesting the rollback
     * @throws JobException if the job cannot be rolled back due to invalid status
     * @throws JobDependencyException 
     */
    public static Job rollbackJob(Job job, String callingUsername) 
     throws JobException 
    {   
        try 
        {   
            // --------------------- System Availability ------------------
            // Get the execution system.
            RemoteSystem executionSystem = new SystemDao().findBySystemId(job.getSystem());
            
            // Check systems.
            if (executionSystem == null) 
            {
                if (job.getStatus() == ARCHIVING) 
                {
                    job = JobManager.updateStatus(job, JobStatusType.ARCHIVING_FAILED, 
                            "Execution system is no longer present.");
                    job = JobManager.updateStatus(job, JobStatusType.FINISHED, 
                            "Job completed, but failed to archive.");
                }
                else
                {
                    job = JobManager.updateStatus(job, JobStatusType.FAILED, 
                            "Job failed. Execution system is no longer present.");
                }
                
                if (_log.isDebugEnabled())
                    _log.debug("Zombie reaper thread is setting status of job " + job.getUuid() + 
                        " to " + job.getStatus() + " because the execution system " + 
                        job.getSystem() + " is no longer present.");
            }
            else if (job.getArchiveSystem() == null) 
            {
                if (job.getStatus() == ARCHIVING) 
                {
                    job = JobManager.updateStatus(job, JobStatusType.ARCHIVING_FAILED, 
                            "Archive system is no longer present.");
                    job = JobManager.updateStatus(job, JobStatusType.FINISHED, 
                            "Job completed, but failed to archive.");
                } 
                else
                {
                    job = JobManager.updateStatus(job, JobStatusType.FAILED, 
                            "Job failed. Archive system is no longer present.");
                }
                
                if (_log.isDebugEnabled())
                    _log.debug("Zombie reaper thread is setting status of job " + job.getUuid() + 
                        " to " + job.getStatus() + " because the archive system "
                        + "is no longer present.");
            }
            
            // --------------------- Cancel Transfers ---------------------
            // Shutdown current and future transfers.
            cancelCurrentTransfers(job, callingUsername);
            
            // If the job is already in a finished status, there's nothing more to do.
            if (isFinished(job.getStatus())) return job;
            
            // --------------------- Roll Back Status ---------------------
            // Get the target rollback status.  There are only 3 possible rollback
            // statuses:  PENDING, STAGED and CLEANING_UP.
            JobStatusType rollbackJobStatus = job.getStatus().rollbackState();
            
            // Tracing.
            if (_log.isDebugEnabled())
                _log.debug("Zombie reaper thread is rolling back status of job " + job.getUuid() + 
                           " from " + job.getStatus() + " to " + rollbackJobStatus);
            
            // We know all the possible rollback statuses, so we can process them by case.
            switch (rollbackJobStatus) 
            {
                // -- Case Cleaning_Up
                case CLEANING_UP:
                {
                    // Prefix for all cleaning up messages.
                    String msg = "Archiving task for this job was " +
                            "found in a zombie state. Job will be rolled back to the previous state and ";
                    
                    // Select message suffix based on system availability.
                    if (executionSystem.isAvailable()) 
                    {   
                        // roll back the status so it will be picked back up
                        if (job.getArchiveSystem().isAvailable())
                            msg += "archiving to " + job.getArchiveSystem().getSystemId()  + " will resume.";
                        else
                            msg += "archiving will resume when the " + job.getArchiveSystem().getSystemId() +
                                   " becomes available.";
                    }
                    else
                        msg += "archiving will resume when " + job.getArchiveSystem().getSystemId() +
                               " the execution system becomes available.";
                    
                    // Update job status.
                    job = JobManager.updateStatus(job, JobStatusType.CLEANING_UP, msg);
                }
                break;

                // -- Case Staged
                case STAGED:
                {
                    // Update job status and set descriptive message.
                    String msg = "Submission task for this job was " +
                                 "found in a zombie state. Job will be rolled back to the previous state and " +
                                 "submission to " + job.getSystem() + " will resume";
                    msg  += executionSystem.isAvailable() ? "." : " when the system becomes available.";
                    job = JobManager.updateStatus(job, STAGED, msg);
                }
                break;

                // -- Case Pending
                case PENDING:
                {
                    // Update job status and set descriptive message.
                    String msg = "Input data staging for this job was " +
                                 "found in a zombie state. Job will be rolled back to the previous state and " +
                                 "input staging to " + job.getSystem() + " will resume";
                    msg  += executionSystem.isAvailable() ? "." : " when the system becomes available.";
                    job = JobManager.updateStatus(job, PENDING, msg);
                }
                break;

                // -- This should never happen!  It is a compile-time problem if
                // -- an unrecognized rollback status is received. 
                default:
                {
                    // Save the original status.
                    JobStatusType originalStatus = job.getStatus();
                    
                    // Update job status and set descriptive message.
                    String msg = StringUtils.capitalize(job.getStatus().name()) +
                                 " for this job was found in a zombie state. Job will be rolled back " +
                                 "to the previous state and and resume";
                    msg  += executionSystem.isAvailable() ? "." : (" when " + job.getSystem() + " becomes available.");
                    job = JobManager.updateStatus(job, rollbackJobStatus, msg);
                    
                    // Log the occurrence.
                    _log.warn("Unexpected rollback status " + rollbackJobStatus.name() + 
                              " received when original status was " + originalStatus + 
                              ".  FIX JobStatusType.rollbackState() OR ITS CALLERS!");
                }
                break;
            }
            
        }
        catch (JobException e) {
            throw e;
        }
        catch (Throwable e)
        {
            throw new JobException("Failed to roll back job " + job.getUuid() + " to previous state", e);
        }
        
        return job;
    }
    
    /* ********************************************************************** */
    /*                            Private Methods                             */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* cancelCurrentTransfers:                                                */
    /* ---------------------------------------------------------------------- */
    /**
     * Cancel all transfer tasks for this job prior to rolling
     * back the status so there won't be new transfers
     * started before the status is updated.
     * 
     * @param job the job for which to cancel transfers
     * @param callingUsername the principal canceling transfers for the job
     * @throws JobException 
     */
    private static void cancelCurrentTransfers(Job job, String callingUsername) 
     throws JobException 
    {   
        // iterate over all job events
        for (JobEvent event: JobEventDao.getByJobId(job.getId())) 
        {
            // Maybe there's nothing to cancel.
            if (event.getTransferTask() == null) continue;
            
            // wherever a transfer task is found for an event, cancel it. This will 
            // issue a single SQL update query to set {@link TransferTask#status} to 
            // {@link TransferStatusType#CANCELLED}
            try { 
                if (_log.isDebugEnabled())
                    _log.debug("Zombie reaper thread is cancelling transfer task " 
                               + event.getTransferTask().getUuid() + " for job " + job.getUuid());
                    
                    TransferTaskDao.cancelAllRelatedTransfers(event.getTransferTask().getId());
                    
                    NotificationManager.process(event.getTransferTask().getUuid(), 
                                                TransferTaskEventType.CANCELLED.name(), 
                                                callingUsername, 
                                                event.getTransferTask().toJSON());
            } 
            catch (Throwable e) {
                    if (_log.isDebugEnabled())
                        _log.error("Failed to cancel transfer task " + 
                            event.getTransferTask().getUuid() + " associated with job " + job.getUuid(), e);
            }
        }
    }       
}
