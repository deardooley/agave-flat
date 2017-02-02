package org.iplantc.service.jobs.phases.schedulers;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.iplantc.service.jobs.dao.JobDao;
import org.iplantc.service.jobs.exceptions.JobException;
import org.iplantc.service.jobs.exceptions.JobSchedulerException;
import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.jobs.model.enumerations.JobPhaseType;
import org.iplantc.service.jobs.model.enumerations.JobStatusType;
import org.iplantc.service.jobs.phases.schedulers.dto.JobArchiveInfo;
import org.iplantc.service.jobs.phases.schedulers.dto.JobMonitorInfo;
import org.iplantc.service.jobs.phases.schedulers.filters.ReadyJobs;
import org.iplantc.service.jobs.phases.schedulers.strategies.impl.JobCreateOrder;
import org.iplantc.service.jobs.phases.schedulers.strategies.impl.TenantRandom;
import org.iplantc.service.jobs.phases.schedulers.strategies.impl.UserRandom;

/** Concrete job phase scheduler
 * 
 * @author rcardone
 */
public final class ArchivingScheduler 
 extends AbstractPhaseScheduler
{
    /* ********************************************************************** */
    /*                               Constants                                */
    /* ********************************************************************** */
    // Tracing.
    private static final Logger _log = Logger.getLogger(ArchivingScheduler.class);
    
    // A place to squirrel away our triggers.
    private List<JobStatusType> _phaseTriggerStatuses;
    
    /* ********************************************************************** */
    /*                              Constructors                              */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* constructor:                                                           */
    /* ---------------------------------------------------------------------- */
    public ArchivingScheduler() throws JobException
    {
        super(JobPhaseType.ARCHIVING,
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
            _phaseTriggerStatuses = new ArrayList<>(1);
            _phaseTriggerStatuses.add(JobStatusType.CLEANING_UP);
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
        // Get the job monitor information.
        List<JobArchiveInfo> archiveInfoList = null;
        try {archiveInfoList = JobDao.getSchedulerJobArchiveInfo(_phaseType, statuses);}
        catch (Exception e) {
            String msg = _phaseType.name() + 
                         " scheduler unable to retrieve job archive information.";
            _log.error(msg, e);
            throw new JobSchedulerException(msg, e);
        }
        
        // Quit now if there's nothing to do.
        if (archiveInfoList.isEmpty()) return new ReadyJobs(new LinkedList<Job>());
        
        // Create list of job uuids. 
        List<String> uuids = new ArrayList<String>(archiveInfoList.size());
        for (JobArchiveInfo info : archiveInfoList) uuids.add(info.getUuid());
            
        // Retrieve all jobs that should be archived now.   Note that the called 
        // routine will silently limit on the number of uuids per request.  We'll 
        // pick up any unserviced jobs on the next cycle, so this shouldn't be a problem.  
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
                throw new JobSchedulerException(msg, e);
            }
        
        return new ReadyJobs(jobs);
    }
}
