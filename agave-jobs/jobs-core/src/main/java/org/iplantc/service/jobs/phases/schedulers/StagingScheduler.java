package org.iplantc.service.jobs.phases.schedulers;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import org.apache.log4j.Logger;
import org.iplantc.service.jobs.dao.JobDao;
import org.iplantc.service.jobs.exceptions.JobException;
import org.iplantc.service.jobs.exceptions.JobSchedulerException;
import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.jobs.model.JobQuotaInfo;
import org.iplantc.service.jobs.model.enumerations.JobPhaseType;
import org.iplantc.service.jobs.model.enumerations.JobStatusType;
import org.iplantc.service.jobs.phases.JobQuotaChecker;
import org.iplantc.service.jobs.phases.schedulers.Strategies.impl.JobCreateOrder;
import org.iplantc.service.jobs.phases.schedulers.Strategies.impl.TenantRandom;
import org.iplantc.service.jobs.phases.schedulers.Strategies.impl.UserRandom;

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
    public StagingScheduler() throws JobException
    {
        super(JobPhaseType.STAGING,
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
    @Override
    protected List<Job> getPhaseCandidateJobs(List<JobStatusType> statuses) 
      throws JobSchedulerException
    {
        // Get candidate job uuids with quota information.
        List<JobQuotaInfo> quotaInfoList = null;
        try {quotaInfoList = JobDao.getSchedulerJobQuotaInfo(_phaseType, statuses);}
        catch (Exception e) {
            String msg = _phaseType.name() + " scheduler unable to retrieve job quota information.";
            _log.error(msg, e);
            throw new JobSchedulerException(msg, e);
        }
        
        // Retrieve active job summary information to check quotas.
        try {
            // Create the checker object used to check quotas.
            JobQuotaChecker quotaChecker = new JobQuotaChecker();
        
            // Remove records that exceed their quotas.
            ListIterator<JobQuotaInfo> it = quotaInfoList.listIterator();
            while (it.hasNext()) {
                JobQuotaInfo info = it.next();
                if (quotaChecker.exceedsQuota(info)) it.remove(); 
            }
        }
        catch (JobException e) {
            String msg = _phaseType.name() + " scheduler unable to retrieve active job information.";
            _log.error(msg, e);
            throw new JobSchedulerException(msg, e);
        }
        
        // Quit now if there's nothing to do.
        if (quotaInfoList.isEmpty()) return new LinkedList<Job>();
        
        // Create list of job uuids still in play. 
        List<String> uuids = new ArrayList<String>(quotaInfoList.size());
        for (JobQuotaInfo info : quotaInfoList) uuids.add(info.getUuid());
            
        // Retrieve all jobs that passed quota.   Note that the called routine will
        // silently limit on the number of uuids per request.  We'll pick up any 
        // unserviced jobs on the next cycle, so this shouldn't be a problem.  
        // We will, however, have to revisit the issue in the unlikely event that
        // certain jobs starve because they always appear beyond the cut off point.
        List<Job> jobs = null;
        try {jobs = JobDao.getSchedulerJobsByUuids(uuids);}
            catch (Exception e) {
                // Log and continue.
                String msg = "Scheduler for phase " + _phaseType.name() +
                             " is unable to retrieve jobs by UUID for " + 
                             jobs.size() + " jobs.";
                _log.error(msg, e);
            }
        
        return jobs;
    }
}
