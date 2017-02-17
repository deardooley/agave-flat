package org.iplantc.service.jobs.phases.workers;

import org.apache.log4j.Logger;
import org.iplantc.service.common.persistence.HibernateUtil;
import org.iplantc.service.jobs.exceptions.JobException;
import org.iplantc.service.jobs.exceptions.JobWorkerException;
import org.iplantc.service.jobs.managers.JobManager;
import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.jobs.model.enumerations.JobStatusType;

/**
 * @author rcardone
 *
 */
public final class RollingBackWorker 
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
    public RollingBackWorker(PhaseWorkerParms parms) 
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
            // ----- Rollback job
            rollback();
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
    /* rollback:                                                              */
    /* ---------------------------------------------------------------------- */
    /** Roll back is nothing more than updating the job status to one that occurs 
     * in the same or prior phase that the job was in when the roll back command
     * was received. 
     * 
     * @throws JobWorkerException on error
     */
    private void rollback() throws JobWorkerException
    {
        // We only process jobs in rollback state.
        if (_job.getStatus() != JobStatusType.ROLLINGBACK)
        {
            String msg = "Aborting rollback of job " + _job.getUuid() + 
                         " because it is not in rollback state.";
            _log.error(msg);
            
            // Let the calling routine know that we are not archiving.
            throw new JobWorkerException(msg);
        }
        
        // Get the target status to which we will roll back.
        JobStatusType targetStatus = _job.getStatus().rollbackState();
                
        // Update the job status and send an event.
        try {
            _job = JobManager.updateStatus(_job, targetStatus, "Rolling back job");
        }
        catch (JobException e) {
            // Log the occurrence.  This should not happen for semantic reasons: a job
            // in the rollingback state should always be able to move to any of the 
            // rollback target states.  Assuming the state machine is correctly 
            // configured, then this failure is either that the job was no longer
            // in the rollingback state or there's a database issue.  
            String msg = "Unable to update status for job " + _job.getUuid() +
                          " (" + _job.getName() + ") from " + _job.getStatus().name() +
                          " to " + targetStatus.name() + ".";
            _log.error(msg, e);
        }
    }
}
