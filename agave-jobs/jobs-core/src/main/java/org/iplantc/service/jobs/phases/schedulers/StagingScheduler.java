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
import org.restlet.Application;

/** Concrete job phase scheduler
 * 
 * @author rcardone
 */
public final class StagingScheduler 
 extends AbstractPhaseScheduler
{
    /* ********************************************************************** */
    /*                               Constants                                */
    /* ********************************************************************** */
    // Tracing.
    private static final Logger _log = Logger.getLogger(StagingScheduler.class);

    // A place to squirrel away our triggers.
    private List<JobStatusType> _phaseTriggerStatuses;
    
    /* ********************************************************************** */
    /*                              Constructors                              */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* constructor:                                                           */
    /* ---------------------------------------------------------------------- */
    /** Initialize the phase scheduler.
     * 
     * @param application the restlet application that we run under
     * @throws JobException when construction fails
     */
    public StagingScheduler(Application application) throws JobException
    {
        super(JobPhaseType.STAGING,
              new TenantRandom(),
              new UserRandom(),
              new JobCreateOrder(),
              application);
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
            _phaseTriggerStatuses = new ArrayList<>(3);
            _phaseTriggerStatuses.add(JobStatusType.PENDING);
            _phaseTriggerStatuses.add(JobStatusType.PROCESSING_INPUTS);
            _phaseTriggerStatuses.add(JobStatusType.STAGING_INPUTS);
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
