package org.iplantc.service.jobs.phases.workers;

import java.nio.channels.ClosedByInterruptException;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.StaleObjectStateException;
import org.hibernate.UnresolvableObjectException;
import org.iplantc.service.common.persistence.HibernateUtil;
import org.iplantc.service.jobs.Settings;
import org.iplantc.service.jobs.dao.JobDao;
import org.iplantc.service.jobs.exceptions.JobException;
import org.iplantc.service.jobs.exceptions.JobFinishedException;
import org.iplantc.service.jobs.exceptions.JobWorkerException;
import org.iplantc.service.jobs.managers.JobManager;
import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.jobs.model.JobUpdateParameters;
import org.iplantc.service.jobs.model.enumerations.JobStatusType;
import org.iplantc.service.jobs.queue.actions.ArchiveAction;
import org.iplantc.service.systems.dao.SystemDao;
import org.iplantc.service.systems.exceptions.SystemUnavailableException;
import org.iplantc.service.systems.exceptions.SystemUnknownException;
import org.iplantc.service.systems.model.ExecutionSystem;
import org.iplantc.service.systems.model.enumerations.SystemStatusType;
import org.joda.time.DateTime;

/**
 * @author rcardone
 *
 */
public final class ArchivingWorker 
 extends AbstractPhaseWorker
{
    /* ********************************************************************** */
    /*                               Constants                                */
    /* ********************************************************************** */
    // Tracing.
    private static final Logger _log = Logger.getLogger(ArchivingWorker.class);
    
    /* ********************************************************************** */
    /*                              Constructors                              */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* constructor:                                                           */
    /* ---------------------------------------------------------------------- */
    public ArchivingWorker(PhaseWorkerParms parms) 
    {
        super(parms);
    }
    
    /* ********************************************************************** */
    /*                           Protected Methods                            */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* processJob:                                                            */
    /* ---------------------------------------------------------------------- */
    @Override
    protected void processJob(Job job) throws JobWorkerException
    {
        // Exceptions thrown by any of the called methods abort processing.
        // This structure maintains compatibility with legacy code.
        try {
            // ----- Check archiving.
            checkStopped(true, JobStatusType.ARCHIVING);
            isArchiving();
            
            // ----- Check storage locality
            checkStopped(true, JobStatusType.ARCHIVING);
            checkSoftwareLocality();
            
            // ----- Are we within the retry window?
            checkStopped(true, JobStatusType.ARCHIVING);
            checkExpirationDate(7);
            
            // ----- Check system availability
            checkStopped(true, JobStatusType.ARCHIVING);
            checkAvailability(7);
            
            // ----- Archive job
            archive();
        }
        catch (Exception e) {
            // All logging and state changes have been handled
            // by the called routine that threw the exception.
            // This thread lives on.
        }
        finally {
            
            // TODO: Check whether disconnect is a good idea.
            // Hibernate magic...
            try { HibernateUtil.flush(); } catch (Exception e) {}
            try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
            try { HibernateUtil.disconnectSession(); } catch (Exception e) {} 
        }
    }
    
    /* ---------------------------------------------------------------------- */
    /* isArchiving:                                                           */
    /* ---------------------------------------------------------------------- */
    private void isArchiving() throws JobWorkerException, JobException
    {
        // See if we need to do any archiving.
        if (!_job.isArchiveOutput())
        {
            if (_log.isDebugEnabled())
                _log.debug("Job " + _job.getUuid() + 
                           " completed. Skipping archiving at user request.");
        
            try {
                _job = JobManager.updateStatus(_job, JobStatusType.FINISHED,
                        "Job completed. Skipping archiving at user request.");
            }
            catch (JobException e)
            {
                _log.error("Archive worker unable to update job " + 
                           _job.getUuid() + " status to FINISHED.", e);
                throw e;
            }
            
            // Let the calling routine know that we are not archiving.
            throw new JobWorkerException("Not Archiving.");
        }
    }
   
    /* ---------------------------------------------------------------------- */
    /* checkExpirationDate:                                                   */
    /* ---------------------------------------------------------------------- */
    private void checkExpirationDate(int days) throws JobException
    {
        // We only retry for some number of days after the job should have stopped running
        DateTime jobExpirationDate = new DateTime(_job.calculateExpirationDate());

        if (jobExpirationDate.plusDays(days).isBeforeNow())
        {
            if (_log.isDebugEnabled())
                _log.debug("Terminating job " + _job.getUuid() + 
                           " after " + days + " days trying to archive output.");
            _job = JobManager.updateStatus(_job, JobStatusType.KILLED,
                "Removing job from queue after " + days + " days attempting to archive output.");
            _job = JobManager.updateStatus(_job, JobStatusType.FAILED,
                    "Unable to archive outputs for job after " + days + " days. Job cancelled.");
            
            // Let the calling routine know that we are not archiving.
            throw new JobWorkerException("Job expired.");
        }

    }
    
    /* ---------------------------------------------------------------------- */
    /* checkAvailability:                                                     */
    /* ---------------------------------------------------------------------- */
    public void checkAvailability(int days) throws JobException
    {
        // check for resource availability before updating status
        ExecutionSystem executionSystem = (ExecutionSystem) new SystemDao().findBySystemId(_job.getSystem());

        if (executionSystem != null && 
            (!executionSystem.isAvailable() || !executionSystem.getStatus().equals(SystemStatusType.UP)))
        {
            if (!StringUtils.contains(_job.getErrorMessage(), "paused waiting")) {
                if (_log.isDebugEnabled())
                    _log.debug("Archiving skipped for job " + _job.getUuid() + ". Execution system " +
                        executionSystem.getSystemId() + " is currently unavailable.");
            }
            
            // Update status.
            try {_job = JobManager.updateStatus(_job, JobStatusType.CLEANING_UP,
                "Archiving is current paused waiting for the execution system " + executionSystem.getSystemId() +
                " to become available. If the system becomes available again within " + days + " days, this job " +
                "will resume archiving. After " + days + " days it will be killed.");
            }
            catch (JobException e) {
                _log.error("Unable to update status for job " + _job.getName() + " (" + _job.getUuid() + ").");
                throw e;
            }
            
            // Stop phase processing.
            throw new JobWorkerException("Waiting for the execution system");
        }
        else if (_job.getArchiveSystem() != null && 
                 (!_job.getArchiveSystem().isAvailable() || !_job.getArchiveSystem().getStatus().equals(SystemStatusType.UP)))
        {
            if (!StringUtils.contains(_job.getErrorMessage(), "paused waiting")) {
                if (_log.isDebugEnabled())
                    _log.debug("Archiving skipped for job " + _job.getUuid() + ". Archive system " +
                        _job.getArchiveSystem().getSystemId() + " is currently unavailable. ");
            }
            
            // Update status.
            try {_job = JobManager.updateStatus(_job, JobStatusType.CLEANING_UP,
                "Archiving is current paused waiting for the archival system " + _job.getArchiveSystem().getSystemId() +
                " to become available. If the system becomes available again within " + days + " days, this job " +
                "will resume archiving. After " + days + " days it will be killed.");
            }
            catch (JobException e) {
                _log.error("Unable to update status for job " + _job.getName() + " (" + _job.getUuid() + ").");
                throw e;
            }
            
            // Stop phase processing.
            throw new JobWorkerException("Waiting for the archival system");
        }
    }
    
    /* ---------------------------------------------------------------------- */
    /* archive:                                                               */
    /* ---------------------------------------------------------------------- */
    private void archive() throws JobWorkerException
    {
        // Update the status message and the status we were triggered by 
        // something other than ARCHIVING status (like CLEANING_UP).
        try {
            _job = JobManager.updateStatus(_job, JobStatusType.ARCHIVING, "Beginning to archive output.");
        }
        catch (JobFinishedException e) {
            // Log the occurrence and stop all processing.
            if (_log.isDebugEnabled()) {
                String msg = "Unable to update status for job " + _job.getUuid() +
                        " (" + _job.getName() + ") from " + _job.getStatus() +
                        " to " + JobStatusType.ARCHIVING.name() + ".";
                _log.warn(msg, e);
            }
            return;
        }
        catch (JobException e) {
            // Log the occurrence and stop all processing.
            String msg = "Safe status update failed for job " + _job.getUuid() +
                         " (" + _job.getName() + ").";
            _log.error(msg, e);
            return;
        }

        int attempts = 0;
        boolean archived = false;

        // Attempt to stage the job several times
        while (!archived && !isJobExecutionSuspended() && attempts <= Settings.JOB_MAX_SUBMISSION_RETRIES)
        {
            _job.setRetries(attempts++);
            _log.debug("Attempt " + attempts + " to archive job " + _job.getUuid() + " output");

            // Mark the job as archiving.
            // TODO: We have to check if the transition is valid (i.e., not stopped, etc.) 
            try {
                // This updated message forces a database write.
                JobUpdateParameters parms = new JobUpdateParameters();
                parms.setRetries(_job.getRetries());
                _job = JobManager.updateStatus(_job, JobStatusType.ARCHIVING,
                    "Attempt " + attempts + " to archive job output", parms);
            }
            catch (JobFinishedException e) {
                // Log the occurrence and stop all processing.
                if (_log.isDebugEnabled()) {
                    String msg = "Unable to update status for job " + _job.getUuid() +
                            " (" + _job.getName() + ") from " + _job.getStatus() +
                            " to " + JobStatusType.ARCHIVING.name() + ".";
                    _log.warn(msg, e);
                }
                return;
            }
            catch (JobException e) {
                // Log the occurrence and stop all processing.
                String msg = "Safe status update failed for job " + _job.getUuid() +
                             " (" + _job.getName() + ").";
                _log.error(msg, e);
                return;
            }

            // Do the actual archiving.
            try
            {
                // Check for thread or job interruptions and throw one 
                // of two exceptions depending on interrupt type.
                checkStopped();

                setWorkerAction(new ArchiveAction(_job, this));

                try {
                    getWorkerAction().run();
                } catch (Throwable t) {
                    _job = getWorkerAction().getJob();
                    throw t;
                }

                if (!isJobExecutionSuspended() || _job.getStatus() == JobStatusType.ARCHIVING_FINISHED ||
                    _job.getStatus() == JobStatusType.ARCHIVING_FAILED)
                {
                    archived = true;
                    if (_log.isDebugEnabled()) _log.debug("Finished archiving job " + _job.getUuid() + " output");
                    _job = JobManager.updateStatus(_job, JobStatusType.FINISHED);
                }
            }
            catch (ClosedByInterruptException e) {
                if (_log.isDebugEnabled())
                    _log.debug("Archive task for job " + _job.getName() + " (" + _job.getUuid() + 
                               ") aborted due to worker interrupt.", e);
                
                // Update the status.
                try {
                    _job = JobManager.updateStatus(_job, JobStatusType.CLEANING_UP, 
                        "Job archiving reset due to worker shutdown. Archiving will resume in another worker automatically.");
                } catch (Exception e1) {
                    _log.error("Failed to roll back job status when archive task was interrupted.", e1);
                }
                
                // Exit method.
                throw new JobWorkerException("Staging task for job " + _job.getName() + " (" + _job.getUuid() + 
                                             ") aborted due to worker interrupt.");
            }
            catch (JobFinishedException e) {
                String msg = "Submission task for job " + _job.getUuid() + 
                             " forced to stop by a job interrupt.";
                _log.debug(msg, e);
                throw new JobWorkerException(msg, e);
            }
            catch (SystemUnknownException e)
            {
                try
                {
                    _log.error("System for job " + _job.getName() + " (" + _job.getUuid() + 
                               ") is currently unknown. ", e);
                    _job = JobManager.updateStatus(_job, JobStatusType.ARCHIVING_FAILED, e.getMessage());

                    _log.error("Job " + _job.getName() + " (" + _job.getUuid() + 
                               ") completed, but failed to archive output.");
                    _job = JobManager.updateStatus(_job, JobStatusType.FINISHED,
                            "Job completed, but failed to archive output.");
                }
                catch (Exception e1) {
                    _log.error("Failed to update job " + _job.getUuid() + " status to FINISHED");
                }
                
                // Exit method.
                throw new JobWorkerException("Unknown system exception.");
            }
            catch (SystemUnavailableException e)
            {
                try
                {
                    if (_log.isDebugEnabled())
                        _log.debug("System for job " + _job.getUuid() + " is currently unavailable. " + e.getMessage());
                    _job = JobManager.updateStatus(_job, JobStatusType.CLEANING_UP,
                        "Job output archiving is current paused waiting for a system containing " +
                        "input data to become available. If the system becomes available again within 7 days, this job " +
                        "will resume staging. After 7 days it will be killed.");
                }
                catch (Exception e1) {
                    _log.error("Failed to update job " + _job.getUuid() + " status to CLEANING_UP");
                }
                
                // Exit method.
                throw new JobWorkerException("Unavailable system exception.");
            }
            catch (JobException e)
            {
                if (attempts >= Settings.JOB_MAX_SUBMISSION_RETRIES )
                {
                    try {
                        _log.error("Failed to archive job " + _job.getName() + " (" + _job.getUuid() + 
                                   ") output after " + attempts + " attempts.", e);
                        _job = JobManager.updateStatus(_job, JobStatusType.CLEANING_UP, "Attempt " +
                                     attempts + " failed to archive job output. " + e.getMessage());

                        _log.error("Unable to archive output for job " + _job.getName() + " (" + _job.getUuid() + 
                                   ") after " + attempts + " attempts.");
                        _job = JobManager.updateStatus(_job, JobStatusType.ARCHIVING_FAILED,
                                "Unable to archive outputs for job" + " after " + attempts + " attempts.");

                        _log.error("Job " + _job.getName() + " (" + _job.getUuid() + 
                                   ") completed, but failed to archive output.");
                        _job = JobManager.updateStatus(_job, JobStatusType.FINISHED,
                                   "Job completed, but failed to archive output.");
                    }
                    catch (Exception e1) {
                        _log.error("Unable to update status for job " + 
                                   _job.getName() + " (" + _job.getUuid() + ").", e1);
                    }
                    
                    // Exit method.
                    throw new JobWorkerException("Maximum attempts exceeded.");
                }
                else
                {
                    // Log the failed attempt.
                    _log.error("Attempt " + attempts + " for job " + _job.getName() + 
                            " (" + _job.getUuid() + ") failed to archive output.", e);
                    
                    // We live to try again another day...
                    try {
                        _job = JobManager.updateStatus(_job, JobStatusType.CLEANING_UP, "Attempt "
                                + attempts + " failed to archive job output.");
                    } catch (Exception e1) {
                        _log.error("Attempt " + attempts + " for job " + _job.getName() + 
                                   " (" + _job.getUuid() + ") failed to update status.", e1);
                    }
                }
            }
            catch (StaleObjectStateException | UnresolvableObjectException e) {
                if (_log.isDebugEnabled())
                    _log.debug("Job " + _job.getName() + " (" + _job.getUuid() + 
                               ") already being processed by another archiving thread. Ignoring.");
                // Exit method.
                throw new JobWorkerException("Hibernate nonsense.");
            }
            catch (HibernateException e) {
                String msg = "Failed to retrieve job information from db";
                _log.error(msg, e);
                
                // Exit method.
                throw new JobWorkerException(msg);
            }
            catch (Throwable e)
            {
                if (e.getCause() instanceof StaleObjectStateException) {
                    if (_log.isDebugEnabled())
                        _log.debug("Job " + _job.getName() + " (" + _job.getUuid() + 
                                   ") already being processed by another thread. Ignoring.", e);
                    // Exit method.
                    throw new JobWorkerException("Nested hibernate nonsense.");
                }
                else {
                    String message = "Failed to archive job " + _job.getUuid() + " " + e.getMessage();
                    _log.error("Failed to archive output for job " + _job.getUuid(), e);

                    try {
                        _job = JobDao.getById(_job.getId());
                        _job = JobManager.updateStatus(_job, JobStatusType.ARCHIVING_FAILED, message);
                        _job = JobManager.updateStatus(_job, JobStatusType.FINISHED, "Job completed, but failed to archive.");
                    } catch (Exception e1) {}
                    
                    // Exit method.
                    throw new JobWorkerException("Job completed, but failed to archive.");
                }
            }
        } // while loop        
    }
}
