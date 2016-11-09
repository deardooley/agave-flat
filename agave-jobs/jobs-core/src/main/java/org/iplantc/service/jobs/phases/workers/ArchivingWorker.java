package org.iplantc.service.jobs.phases.workers;

import org.apache.log4j.Logger;
import org.iplantc.service.jobs.exceptions.JobWorkerException;
import org.iplantc.service.jobs.managers.JobManager;
import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.jobs.model.enumerations.JobStatusType;
import org.iplantc.service.jobs.phases.PhaseWorkerParms;

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
        // Assign job field for the duration of this method
        // to maintain compatibility with legacy code.
        _job = job;
        
        // See if we need to do any archiving.
        if (_job.isArchiveOutput())
        {
            if (_log.isDebugEnabled())
                _log.debug("Job " + _job.getUuid() + 
                           " completed. Skipping archiving at user request.");
            
            try {
                _job = JobManager.updateStatus(_job, JobStatusType.FINISHED,
                        "Job completed. Skipping archiving at user request.");
            }
            catch (Exception e)
            {
                _log.error("Archive worker unable to update job " + 
                           _job.getUuid() + " status to FINISHED.", e);
            }
            
            // End processing.
            return;
        }
        
        // TODO: increase recognized scheduler selection states 
        
    }
}
