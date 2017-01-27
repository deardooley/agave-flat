package org.iplantc.service.jobs.phases.schedulers;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.iplantc.service.jobs.exceptions.JobException;
import org.iplantc.service.jobs.exceptions.JobSchedulerException;
import org.iplantc.service.jobs.model.enumerations.JobPhaseType;
import org.iplantc.service.jobs.model.enumerations.JobStatusType;
import org.iplantc.service.jobs.phases.schedulers.filters.ReadyJobs;
import org.iplantc.service.jobs.phases.schedulers.strategies.impl.JobCreateOrder;
import org.iplantc.service.jobs.phases.schedulers.strategies.impl.TenantRandom;
import org.iplantc.service.jobs.phases.schedulers.strategies.impl.UserRandom;

/** Concrete job phase scheduler.
 * 
 * @author rcardone
 */
public final class SubmittingScheduler 
 extends AbstractPhaseScheduler
{
    /* ********************************************************************** */
    /*                               Constants                                */
    /* ********************************************************************** */
    // Tracing.
    private static final Logger _log = Logger.getLogger(SubmittingScheduler.class);

    // A place to squirrel away our triggers.
    private List<JobStatusType> _phaseTriggerStatuses;
    
    /* ********************************************************************** */
    /*                              Constructors                              */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* constructor:                                                           */
    /* ---------------------------------------------------------------------- */
    public SubmittingScheduler() throws JobException
    {
        super(JobPhaseType.SUBMITTING,
              new TenantRandom(),
              new UserRandom(),
              new JobCreateOrder());
    }

    /* ********************************************************************** */
    /*                            Protected Methods                           */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* getPhaseTriggerStatus:                                                 */
    /* ---------------------------------------------------------------------- */
    @Override
    protected List<JobStatusType> getPhaseTriggerStatuses()
    {
        if (_phaseTriggerStatuses == null)
        {
            _phaseTriggerStatuses = new ArrayList<>(3);
            _phaseTriggerStatuses.add(JobStatusType.SUBMITTING);
            _phaseTriggerStatuses.add(JobStatusType.STAGED);
            _phaseTriggerStatuses.add(JobStatusType.STAGING_JOB);
        }
        return _phaseTriggerStatuses;
    }
    
    /* ---------------------------------------------------------------------- */
    /* getPhaseCandidateJobs:                                                 */
    /* ---------------------------------------------------------------------- */
    /** Provide in a phase-specific way a list of candidate jobs to be processed
     * the superclass's generic scheduling code. 
     * 
     * @param statuses phase-specific trigger statuses
     * @return the list of jobs that do not violate any quotas
     * @throws JobSchedulerException on error
     */
    @Override
    protected ReadyJobs getPhaseCandidateJobs(List<JobStatusType> statuses) 
      throws JobSchedulerException
    {
        // Staging and Submitting phases perform the same 
        // quota filtering before scheduling jobs.
        return getQuotaCheckedJobs(statuses);
    }
}
