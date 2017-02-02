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
import org.iplantc.service.jobs.model.JobMonitorInfo;
import org.iplantc.service.jobs.model.enumerations.JobPhaseType;
import org.iplantc.service.jobs.model.enumerations.JobStatusType;
import org.iplantc.service.jobs.phases.schedulers.filters.ReadyJobs;
import org.iplantc.service.jobs.phases.schedulers.strategies.impl.JobCreateOrder;
import org.iplantc.service.jobs.phases.schedulers.strategies.impl.TenantRandom;
import org.iplantc.service.jobs.phases.schedulers.strategies.impl.UserRandom;
import org.joda.time.DateTime;

/** Concrete job phase scheduler
 * 
 * @author rcardone
 */
public final class MonitoringScheduler 
 extends AbstractPhaseScheduler
{
    /* ********************************************************************** */
    /*                               Constants                                */
    /* ********************************************************************** */
    // Tracing.
    private static final Logger _log = Logger.getLogger(MonitoringScheduler.class);

    // A place to squirrel away our triggers.
    private List<JobStatusType> _phaseTriggerStatuses;
    
    // Construct the array of s
    private final int[][] _timeCheckArray = getTimeCheckArray();
    
    /* ********************************************************************** */
    /*                              Constructors                              */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* constructor:                                                           */
    /* ---------------------------------------------------------------------- */
    public MonitoringScheduler() throws JobException
    {
        super(JobPhaseType.MONITORING,
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
            // Populate trigger statuses from active statuses.
            JobStatusType[] activeStatuses = JobStatusType.getActiveStatuses();
            _phaseTriggerStatuses = new ArrayList<>(activeStatuses.length);
            for (JobStatusType status : activeStatuses) _phaseTriggerStatuses.add(status);
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
        List<JobMonitorInfo> monitorInfoList = null;
        try {monitorInfoList = JobDao.getSchedulerJobMonitorInfo(_phaseType, statuses);}
        catch (Exception e) {
            String msg = _phaseType.name() + 
                         " scheduler unable to retrieve job monitor information.";
            _log.error(msg, e);
            throw new JobSchedulerException(msg, e);
        }
        
        // Remove jobs that are not ready to be monitored.
        filterMonitorInfo(monitorInfoList);
        
        // Quit now if there's nothing to do.
        if (monitorInfoList.isEmpty()) return new ReadyJobs(new LinkedList<Job>());
        
        // Create list of job uuids still in play. 
        List<String> uuids = new ArrayList<String>(monitorInfoList.size());
        for (JobMonitorInfo info : monitorInfoList) uuids.add(info.getUuid());
            
        // Retrieve all jobs that should be monitored now.   Note that the called 
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

    /* ********************************************************************** */
    /*                            Private Methods                             */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* filterMonitorInfo:                                                     */
    /* ---------------------------------------------------------------------- */
    /** Filter out jobs that are not ready to be monitored based on their 
     * monitoring history.  The list is modified in place.
     * 
     * @param monitorInfoList a non-null but possibly empty list of monitor objects
     */
    private void filterMonitorInfo(List<JobMonitorInfo> monitorInfoList)
    {
        // Remove job references from list that are not due to be monitored.
        ListIterator<JobMonitorInfo> it = monitorInfoList.listIterator();
        while (it.hasNext()) {
            
            // Get the job's last update and number of previous status checks.
            JobMonitorInfo info = it.next();
            DateTime lastUpdated = new DateTime(info.getLastUpdated().getTime());
            int statusChecks = info.getStatusChecks();
            
            // Current time used in comparisons.
            DateTime now = new DateTime();
            
            // Check each condition that would allow this job to be monitored now.
            for (int[] intervalCheck : _timeCheckArray) {
                // The first element is the number of previous checks,
                // the second is the time period that has to elapse 
                // for the job to be monitored again.
                if ((statusChecks < intervalCheck[0]) && 
                    now.isAfter(lastUpdated.plusSeconds(intervalCheck[1])))
                   continue; // do not remove this job from the list
            }
            
            // One last test only checks the time since the last monitoring attempt.
            if (now.isAfter(lastUpdated.plusHours(1))) continue; // keep job
            
            // The job is not eligible for monitoring yet.
            it.remove();
        }
    }
    
    /* ---------------------------------------------------------------------- */
    /* getTimeCheckArray:                                                     */
    /* ---------------------------------------------------------------------- */
    /** Return an array of status checks and time intervals in seconds that can
     * be used to identify jobs ready for monitoring.
     * 
     * @return the 2 dimensional array of status checks and time in seconds
     */
    private int[][] getTimeCheckArray()
    {
        // Create and initialize a 2 dimensional array in which the first 
        // element in each row represents a number of checks and the second
        // element in each row represents a number of seconds.
        int[][] array = new int[6][2];
        
        // 4 checks, 15 seconds 
        array[0][0] = 4;
        array[0][1] = 15;
        
        // 14 checks, 30 seconds 
        array[1][0] = 14;
        array[1][1] = 30;
        
        // 44 checks, 60 seconds (1 minute)
        array[2][0] = 44;
        array[2][1] = 60;
        
        // 56 checks, 300 seconds (5 minutes) 
        array[3][0] = 56;
        array[3][1] = 300;
        
        // 104 checks, 900 seconds (15 minutes) 
        array[4][0] = 104;
        array[4][1] = 900;
        
        // 152 checks, 1800 seconds (30 minutes) 
        array[5][0] = 152;
        array[5][1] = 1800;
        
        return array;
    }
}
