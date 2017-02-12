package org.iplantc.service.jobs.phases.schedulers;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.jobs.dao.JobDao;
import org.iplantc.service.jobs.exceptions.JobException;
import org.iplantc.service.jobs.exceptions.JobSchedulerException;
import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.jobs.model.enumerations.JobPhaseType;
import org.iplantc.service.jobs.model.enumerations.JobStatusType;
import org.iplantc.service.jobs.phases.schedulers.filters.ReadyJobs;
import org.iplantc.service.jobs.phases.schedulers.strategies.impl.JobCreateOrder;
import org.iplantc.service.jobs.phases.schedulers.strategies.impl.TenantRandom;
import org.iplantc.service.jobs.phases.schedulers.strategies.impl.UserRandom;
import org.iplantc.service.jobs.phases.utils.ZombieJobUtils;

/** Concrete job phase scheduler
 * 
 * @author rcardone
 */
public final class RollingbackScheduler 
 extends AbstractPhaseScheduler
{
    /* ********************************************************************** */
    /*                               Constants                                */
    /* ********************************************************************** */
    // Tracing.
    private static final Logger _log = Logger.getLogger(RollingbackScheduler.class);

    // A place to squirrel away our triggers.
    private List<JobStatusType> _phaseTriggerStatuses;
    
    /* ********************************************************************** */
    /*                              Constructors                              */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* constructor:                                                           */
    /* ---------------------------------------------------------------------- */
    public RollingbackScheduler() throws JobException
    {
        super(JobPhaseType.ROLLINGBACK,
              new TenantRandom(),
              new UserRandom(),
              new JobCreateOrder());
    }

    /* ********************************************************************** */
    /*                           Protected Methods                            */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* getPhaseTriggerStatus:                                                 */
    /* ---------------------------------------------------------------------- */
    @Override
    protected List<JobStatusType> getPhaseTriggerStatuses()
    {
        if (_phaseTriggerStatuses == null)
        {
            // Populate trigger statuses from executing statuses.
            _phaseTriggerStatuses = new ArrayList<>(1);
            _phaseTriggerStatuses.add(JobStatusType.ROLLINGBACK);
        }
        return _phaseTriggerStatuses;
    }

    /* ---------------------------------------------------------------------- */
    /* getPhaseCandidateJobs:                                                 */
    /* ---------------------------------------------------------------------- */
    @Override
    protected ReadyJobs getPhaseCandidateJobs(List<JobStatusType> statuses) 
      throws JobSchedulerException
    {
        return getUnfilteredJobs(statuses);
    }

    /* ********************************************************************** */
    /*                            Package Methods                             */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* monitorZombies:                                                       */
    /* ---------------------------------------------------------------------- */
    /** This method detects zombie jobs (i.e., jobs that have not made 
     * progress within a certain time period) and attempts to roll back those
     * jobs to a prior status.  If roll back is not possible, the job is put
     * into a finished state.  In flight transfers are cancelled and notifications
     * sent when job status changes. 
     * 
     * This method is called from our parent class, so it cannot be private.
     */
    void monitorZombies()
    {
        // Check for zombie jobs indefinitely.
        while (true) 
        {
            // Detect and process zombies.
            try {
                int numZombies = processZombies();
                if (_log.isDebugEnabled()) 
                    _log.debug("Scheduler " + getSchedulerName() + 
                               " processed " + numZombies + " zombie jobs.");
            }
            catch (Exception e) {
                // Just log the problem.
                String msg = getZombieCleanUpThreadName() + 
                             " failed to process zombie jobs but will try again.";
                _log.error(msg);
            }
            
            // Check for interrupts before sleeping.
            if (Thread.interrupted()) {
                if (_log.isInfoEnabled()) {
                    String msg = getZombieCleanUpThreadName() + 
                                 " terminating because of an interrupt during processing.";
                    _log.info(msg);
                }
                break;
            }
            
            // Sleep for the prescribed amount of time before trying again.
            try {Thread.sleep(ZOMBIE_MONITOR_DELAY);}
            catch (InterruptedException e) {
                // Terminate this thread.
                if (_log.isInfoEnabled()) {
                    String msg = getZombieCleanUpThreadName() + 
                                 " terminating because of an interrupt during sleep.";
                    _log.info(msg);
                }
                break;
            }
        }
    }

    /* ********************************************************************** */
    /*                            Private Methods                             */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* processZombies:                                                        */
    /* ---------------------------------------------------------------------- */
    /** Detect and manage zombie jobs.
     * 
     * @return number of zombie jobs detected
     * @throws JobSchedulerException on error
     */
    private int processZombies()
     throws JobSchedulerException
    {
        // Get the list of zombie jobs.
        List<Job> zombies = null;
        try {zombies = JobDao.getSchedulerZombieJobs();}
            catch (Exception e) {
                String msg = "Unable to query database for zombie jobs.";
                _log.error(msg, e);
                throw new JobSchedulerException(msg, e);
            }
        if (zombies.isEmpty()) return 0;
        
        // Rollback each job.
        for (Job job : zombies)
        {
            // Set up thread local values.
            TenancyHelper.setCurrentTenantId(job.getTenantId());
            TenancyHelper.setCurrentEndUser(job.getOwner());
            
            // Attempt to rollback the zombie job.
            try {ZombieJobUtils.rollbackJob(job, job.getOwner());}
            catch (Exception e) {
                // Log problem and continue to next job.
                String msg = "Unable to rollback job " + job.getUuid() +
                             " (" + job.getName() + ").";
                _log.error(msg, e);
            }
        }
        
        // Clear the thread local values before returning.
        TenancyHelper.setCurrentTenantId(null);
        TenancyHelper.setCurrentEndUser(null);
        return zombies.size();
    }
}
