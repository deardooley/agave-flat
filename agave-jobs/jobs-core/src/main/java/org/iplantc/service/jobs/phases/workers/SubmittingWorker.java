package org.iplantc.service.jobs.phases.workers;

import java.nio.channels.ClosedByInterruptException;

import org.apache.log4j.Logger;
import org.hibernate.StaleObjectStateException;
import org.hibernate.UnresolvableObjectException;
import org.iplantc.service.common.persistence.HibernateUtil;
import org.iplantc.service.jobs.dao.JobDao;
import org.iplantc.service.jobs.exceptions.JobException;
import org.iplantc.service.jobs.exceptions.JobFinishedException;
import org.iplantc.service.jobs.exceptions.JobWorkerException;
import org.iplantc.service.jobs.managers.JobManager;
import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.jobs.model.JobUpdateParameters;
import org.iplantc.service.jobs.model.enumerations.JobStatusType;
import org.iplantc.service.jobs.queue.actions.SubmissionAction;

/**
 * @author rcardone
 *
 */
public final class SubmittingWorker 
 extends AbstractPhaseWorker
{
    /* ********************************************************************** */
    /*                               Constants                                */
    /* ********************************************************************** */
    // Tracing.
    private static final Logger _log = Logger.getLogger(SubmittingWorker.class);
    
    /* ********************************************************************** */
    /*                              Constructors                              */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* constructor:                                                           */
    /* ---------------------------------------------------------------------- */
    public SubmittingWorker(PhaseWorkerParms parms) 
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
        // Assign job field for the duration of this method
        // to maintain compatibility with legacy code.
        _job = job;

        // Capture the epoch before hibernate can change it.
        setJobInitialEpoch(job.getEpoch());
        
        // Exceptions thrown by any of the called methods abort processing.
        // This structure maintains compatibility with legacy code.
        try {
            // ----- Check the execution system availability.
            checkStopped(true, JobStatusType.STAGED);
            checkSystemAvailability(7);
        
            // ----- Check storage locality.
            checkStopped(true, JobStatusType.STAGED);
            checkSoftwareLocality();
            
            // ----- Are we within the retry window?
            checkStopped(true, JobStatusType.STAGED);
            checkRetryPeriod(14);
            
            // ----- Stage the job input.
            submit();
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
            
            // Remove dangling references to job-specific data.
            reset();
        }
    }
    
    /* ********************************************************************** */
    /*                            Private Methods                             */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* submit:                                                                */
    /* ---------------------------------------------------------------------- */
    private void submit() throws JobWorkerException
    {
        try {
            // mark the job as submitting so no other process claims it
            // note: we should have jpa optimistic locking enabled, so
            // no race conditions should exist at this point.
            _job = JobManager.updateStatus(_job, JobStatusType.SUBMITTING, 
                                       "Preparing job for submission.");
        
            // Check for thread or job interruptions and throw one 
            // of two exceptions depending on interrupt type.
            checkStopped();
        
            setWorkerAction(new SubmissionAction(_job, this));
        
            try {
                // Wrap this in a try/catch so we can update the local reference. 
                getWorkerAction().run();
            }
            finally {_job = getWorkerAction().getJob();}
        
            if (!isJobExecutionSuspended() || _job.getStatus() == JobStatusType.QUEUED || 
                _job.getStatus() == JobStatusType.RUNNING)
            {       
                JobUpdateParameters jobUpdateParameters = new JobUpdateParameters();
                jobUpdateParameters.setRetries(0);
                JobDao.update(_job, jobUpdateParameters);
            }
        }
        catch (ClosedByInterruptException e) {
            if (_log.isDebugEnabled())
                _log.debug("Submission task for job " + _job.getUuid() + 
                           " aborted due to worker interrupt.");
            
            try {
                _job = JobManager.updateStatus(_job, JobStatusType.STAGED, 
                    "Job submission aborted due to worker shutdown. Job will be resubmitted automatically.");
            } catch (UnresolvableObjectException | JobException e1) {
                _log.error("Failed to roll back job status when archive task was interrupted.", e1);
            }
            throw new JobWorkerException("Submission task for job " + _job.getUuid() + 
                                         " aborted due to worker interrupt.", e);
        }
        catch (JobFinishedException e) {
            if (_log.isDebugEnabled())
                _log.debug("Submission task for job " + _job.getUuid() + 
                           " forced to stop by a job interrupt.", e);
        }
        catch (StaleObjectStateException | UnresolvableObjectException e) {
            String msg = "Job " + _job.getUuid() + " already being processed by another thread. Ignoring.";
            _log.error(msg, e);
            throw new JobWorkerException(msg, e);
        }
        catch (Throwable e)
        {
            if (e.getCause() instanceof StaleObjectStateException) {
                if (_log.isDebugEnabled())
                    _log.debug("Just avoided a job submission staging race condition for job " + _job.getUuid());
                throw new JobWorkerException("Job " + _job.getUuid() + 
                                                " already being processed by another thread. Ignoring.", e.getCause());
            }
            else if (_job == null)
            {
                String msg = "Failed to retrieve job information from db";
                _log.error(msg, e);
                throw new JobWorkerException(msg, e);
            }
            else
            {
                try
                {
                    _log.error("Failed to submit job " + _job.getUuid(), e);
                    _job = JobManager.updateStatus(_job, JobStatusType.FAILED,
                            "Failed to submit job " + _job.getUuid() + " due to internal errors");
                }
                catch (Exception e1)
                {
                    _log.error("Failed to update job " + _job.getUuid() + " status to failed", e1);
                }
                throw new JobWorkerException(e);
            }
        }
    }
}
