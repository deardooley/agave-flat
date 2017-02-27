package org.iplantc.service.jobs.phases.workers;

import java.nio.channels.ClosedByInterruptException;

import org.apache.log4j.Logger;
import org.hibernate.StaleObjectStateException;
import org.hibernate.UnresolvableObjectException;
import org.iplantc.service.common.persistence.HibernateUtil;
import org.iplantc.service.jobs.Settings;
import org.iplantc.service.jobs.dao.JobDao;
import org.iplantc.service.jobs.exceptions.JobDependencyException;
import org.iplantc.service.jobs.exceptions.JobException;
import org.iplantc.service.jobs.exceptions.JobFinishedException;
import org.iplantc.service.jobs.exceptions.JobWorkerException;
import org.iplantc.service.jobs.managers.JobManager;
import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.jobs.model.JobUpdateParameters;
import org.iplantc.service.jobs.model.enumerations.JobStatusType;
import org.iplantc.service.jobs.queue.actions.StagingAction;
import org.iplantc.service.systems.exceptions.SystemUnavailableException;

/**
 * @author rcardone
 *
 */
public final class StagingWorker 
 extends AbstractPhaseWorker
{
    /* ********************************************************************** */
    /*                               Constants                                */
    /* ********************************************************************** */
    // Tracing.
    private static final Logger _log = Logger.getLogger(StagingWorker.class);
    
    /* ********************************************************************** */
    /*                              Constructors                              */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* constructor:                                                           */
    /* ---------------------------------------------------------------------- */
    public StagingWorker(PhaseWorkerParms parms) 
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
            // ----- Check the execution system availability.
            checkStopped(true, JobStatusType.PENDING);
            checkSystemAvailability(7);
        
            // ----- Check storage locality.
            checkStopped(true, JobStatusType.PENDING);
            checkSoftwareLocality();
            
            // ----- Are we within the retry window?
            checkStopped(true, JobStatusType.PENDING);
            checkRetryPeriod(7);
            
            // ----- Is there anything to stage?
            checkStopped(true, JobStatusType.PENDING);
            checkStagingInput();
            
            // ----- Stage the job input.
            stage();
        }
        catch (Exception e) {
            // All logging and state changes have been handled
            // by the called routine that threw the exception.
            // This thread lives on.
        }
        finally {
            
            // Hibernate magic...
            // Disconnect is less drastic than close and is the minimum that 
            // should occur to free up the JDBC connection.  Note that (1) we
            // are using an ancient version of Hibernate (3.6.10.Final) and 
            // (2) our workers are now long-lived threads so we may have to
            // revisit the disconnect vs. close discussion if problems arise.
            try { HibernateUtil.flush(); } catch (Exception e) {}
            try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
            try { HibernateUtil.disconnectSession(); } catch (Exception e) {} 
        }
    }
    
    /* ********************************************************************** */
    /*                            Private Methods                             */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* checkStagingInput:                                                     */
    /* ---------------------------------------------------------------------- */
    private void checkStagingInput() throws JobException, JobWorkerException
    {
        // if the job doesn't need to be staged, just move on with things.
        if (JobManager.getJobInputMap(_job).isEmpty()) 
        {
            // Move the job past staging.
            try {
                _job = JobManager.updateStatus(_job, JobStatusType.STAGED, 
                       "Skipping staging. No input data associated with this job.");
            }
            catch (Exception e) {
                String msg = "Unable to move job " + _job.getUuid() + " (" +
                             _job.getName() + ") to STAGED status.  Aborting job.";
                _log.error(msg, e);
                
                // Try one more time to change job status.
                forceJobCompletion(_job, JobStatusType.FAILED, msg);
                
                // Rethrow original exception.
                throw e;
            }
            
            // This is not really an error, but we need to throw some exception
            // to signal that this phase's processing should end for this job.
            String msg = "Job " + _job.getName() + " (" + _job.getUuid() +
                         ") has no associated input data.  Job status set to STAGED.";
            throw new JobWorkerException(msg);
        } 
    }
 
    /* ---------------------------------------------------------------------- */
    /* stage:                                                                 */
    /* ---------------------------------------------------------------------- */
    private void stage() 
     throws JobException, JobWorkerException, ClosedByInterruptException
    {
        // Loop variables.
        int attempts = 0;
        boolean staged = false;
        
        // Main staging loop.
        while (!staged && !isJobExecutionSuspended() && attempts <= Settings.JOB_MAX_SUBMISSION_RETRIES)
        {
            // Set the number of retries and attempts.
            _job.setRetries(attempts++);
            
            if (_log.isDebugEnabled())
                _log.debug("Attempt " + attempts + " to stage job " + _job.getUuid() + " inputs");
            
            // Mark the job as submitting and assign the _job field the updated content.
            // An exception here means the status could not be updated because the job 
            // has been stopped, which means we need to abort job processing.
            try {
                JobUpdateParameters jobUpdateParms = new JobUpdateParameters();
                jobUpdateParms.setRetries(_job.getRetries());
                _job = JobManager.updateStatus(_job, JobStatusType.PROCESSING_INPUTS, 
                                               "Attempt " + attempts + " to stage job inputs", 
                                               jobUpdateParms);
            }
            catch (JobFinishedException e) {
                // Log the occurrence and stop all processing.
                if (_log.isDebugEnabled()) {
                    String msg = "Unable to update status for job " + _job.getUuid() +
                            " (" + _job.getName() + ") from " + _job.getStatus() +
                            " to " + JobStatusType.PROCESSING_INPUTS.name() + ".";
                    _log.debug(msg, e);
                }
                return;
            }
            catch (JobException e) {
                // Log the occurrence and stop all processing.
                String msg = "Status update failed for job " + _job.getUuid() +
                             " (" + _job.getName() + ").";
                _log.error(msg, e);
                return;
            }
            
            // Perform the work of this phase.
            try 
            {
                // Check for thread or job interruptions and throw one 
                // of two exceptions depending on interrupt type.
                checkStopped(false, JobStatusType.PROCESSING_INPUTS);
                
                setWorkerAction(new StagingAction(_job, this));
                
                try {
                    // Wrap this in a try/catch so we can update the local reference.
                    // Note that we can receive an interrupt that stops processing
                    // during this method's execution.
                    getWorkerAction().run();
                }
                finally {_job = getWorkerAction().getJob();}
                
                // If we are stopped we will quietly exit the retry loop.
                if (!isJobExecutionSuspended() && _job.getStatus() == JobStatusType.STAGED)
                {       
                    staged = true;
                    JobUpdateParameters jobUpdateParameters = new JobUpdateParameters();
                    jobUpdateParameters.setRetries(0);
                    JobDao.update(_job, jobUpdateParameters);
                }
            }
            catch (StaleObjectStateException | UnresolvableObjectException e) {
                // This should never happen since only one worker thread processes
                // a job at a time.  We keep the code just in case...
                String msg = "Job " + _job.getUuid() + " already being processed by another thread. Ignoring.";
                _log.error(msg, e);
                throw new JobWorkerException(msg, e);
            }
            catch (ClosedByInterruptException e) {
                // This worker thread was interrupted using Thread.interrupt().  We set the
                // job back to its initial phase trigger before stopping work.
                if (_log.isDebugEnabled())
                    _log.debug("Staging task for job " + _job.getUuid() + 
                            " aborted due to worker interrupt.", e);
                throw e;
            }
            catch (JobFinishedException e) {
                if (_log.isDebugEnabled())
                    _log.debug("Staging task for job " + _job.getUuid() + 
                               " forced to stop by a job interrupt.", e);
                throw e;
            }
            catch (SystemUnavailableException e) 
            {
                try
                {
                    if (_log.isDebugEnabled())
                        _log.debug("System for job " + _job.getUuid() + 
                                   " is currently unavailable. " + e.getMessage());
                    _job = JobManager.updateStatus(_job, JobStatusType.PENDING, 
                        "Input staging is current paused waiting for a system containing " + 
                        "input data to become available. If the system becomes available " +
                        "again within 7 days, this job " + 
                        "will resume staging. After 7 days it will be killed.");
                }
                catch (Throwable e1) {
                    _log.error("Failed to update job " + _job.getUuid() + " status to PENDING");
                }   
                throw new JobWorkerException(e);
            }
            catch (JobDependencyException e) 
            {
                try
                {
                    _log.error("Failed to stage inputs for job " + _job.getUuid(), e);
                    _job = JobManager.updateStatus(_job, JobStatusType.FAILED, e.getMessage());
                }
                catch (Exception e1) {
                    _log.error("Failed to update job " + _job.getUuid() + " status to FAILED", e1);
                }
                throw new JobWorkerException(e);
            }
            catch (JobException e) 
            {
                if (attempts >= Settings.JOB_MAX_SUBMISSION_RETRIES ) 
                {
                    _log.error("Failed to stage job " + _job.getUuid() + 
                            " inputs after " + attempts + " attempts.", e);
                    _job = JobManager.updateStatus(_job, JobStatusType.STAGING_INPUTS, "Attempt " 
                            + attempts + " failed to stage job inputs. " + e.getMessage());
                    try 
                    {
                        _job = JobManager.deleteStagedData(_job);
                    } 
                    catch (Throwable e1)
                    {
                        _log.error("Failed to remove remote work directory for job " + _job.getUuid(), e1);
                        _job = JobManager.updateStatus(_job, JobStatusType.FAILED, 
                                "Failed to remove remote work directory.");
                    }
                    
                    String msg = "Unable to stage inputs for job " + _job.getUuid() + 
                                 " after " + attempts + " attempts. Job cancelled.";
                    _log.error(msg);
                    _job = JobManager.updateStatus(_job, JobStatusType.FAILED, msg);
                    
                    throw new JobWorkerException(e);
                } 
                else {
                    // TODO: Do we need to put this in a try block?
                    _job = JobManager.updateStatus(_job, JobStatusType.PENDING, "Attempt " 
                            + attempts + " failed to stage job inputs. " + e.getMessage());
                }
            }
            catch (Exception e) 
            {
                try
                {
                    _log.error("Failed to stage inputs for job " + _job.getUuid(), e);
                    _job = JobManager.updateStatus(_job, JobStatusType.FAILED, 
                            "Failed to stage file due to unexpected error.");
                }
                catch (Exception e1) {
                    _log.error("Failed to update job " + _job.getUuid() + " status to FAILED");
                }
                throw new JobWorkerException(e);
            }
        }
    }
    
}
