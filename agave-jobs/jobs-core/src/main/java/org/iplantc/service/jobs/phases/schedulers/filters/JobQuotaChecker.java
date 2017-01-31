package org.iplantc.service.jobs.phases.schedulers.filters;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.iplantc.service.jobs.dao.JobDao;
import org.iplantc.service.jobs.exceptions.JobException;
import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.jobs.model.JobActiveCount;
import org.iplantc.service.jobs.model.JobQuotaInfo;

/** This class initializes with a list of JobActionCount objects, which is a
 * snapshot of the active jobs in the system at instance creation time.  The
 * main quota-checking method is exceedsQuota(JobQuotaInfo).  It checks the 
 * active snapshot values against the four quota limits in the JobQuotaInfo 
 * parameter.  
 * 
 * @author rcardone
 */
public final class JobQuotaChecker
 implements IPostPriorityJobFilter
{
    /* ********************************************************************** */
    /*                                Constants                               */
    /* ********************************************************************** */
    // Tracing.
    private static final Logger _log = Logger.getLogger(JobQuotaChecker.class);
    
    // Search tree cache separation character.
    private static final String PATH_SEPARATOR = "|";
    
    // Dummy queue name used in cache key when counting jobs
    // for a specific user across all queues on the system.
    private static final String ALL_QUEUES = "<*all-queues*>";
    
    /* ********************************************************************** */
    /*                                 Fields                                 */
    /* ********************************************************************** */
    // Quota info objects arranged in a searchable hierarchy of trees.  From top
    // to bottom, the hierarchy is tenant->system->queue->owner with JobActiveCount
    // objects as leaves.
    private final 
        HashMap<String,HashMap<String,HashMap<String,HashMap<String,JobActiveCount>>>>
        _tree = createSearchTree();
    
    // Cache counts so that we don't have recalculate them.  The keys are 
    // search tree paths with components separated with the separation character
    // and values are the calculated counts.  A count of -1 means there is no
    // quota limit.
    private final HashMap<String,Long> _countCache = new HashMap<String,Long>();
    
    // The input quota information used in prioritized filtering. During that 
    // filtering phase, jobs are passed in without their quota information.  We
    // use this mapping from job uuid to info object to reconnect a job to its
    // quota bounds.
    private final HashMap<String, JobQuotaInfo> _quotaInfoMap;
    
    // Counters used to filter low priority jobs from being scheduled by
    // PrioritizedJobs.  See keep() method.
    private ActiveAndScheduledJobCounters _counters;
    
    /* ********************************************************************** */
    /*                              Constructors                              */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* constructor:                                                           */
    /* ---------------------------------------------------------------------- */
    /** Extract active job information from the database and populate data
     * structures with that information for convenient searching.  The quota
     * information list should contain all quota information for all jobs that
     * may be passed to any public method of this class.
     * 
     * Note that we could have collected the JobQuotaInfo objects dynamically on
     * exceedsQuota() calls, but bulk load on construction offers the advantage
     * of avoiding any rehashing. The dynamic approach would also have to contend
     * with the fact that the public interface includes several exceeds*() calls.
     * Finally, no extra burden is placed on the caller since the complete 
     * JobQuotaInfo list available when constructing this object.
     * 
     * @param quotaInfoList the quota objects for all jobs that will be submitted
     *                      to this class instance 
     * @throws JobException on error
     */
    public JobQuotaChecker(List<JobQuotaInfo> quotaInfoList) 
     throws JobException 
    {
        // Make sure we have a non-empty input list.
        if (quotaInfoList == null || quotaInfoList.isEmpty()) {
            String msg = "Invalid quota information list used to initialize quota checker.";
            _log.error(msg);
            throw new JobException(msg);
        } 
        
        // Save the quota information for the final filtering stage.
        _quotaInfoMap = new HashMap<>(1 + quotaInfoList.size() * 2);
        for (JobQuotaInfo info : quotaInfoList) _quotaInfoMap.put(info.getUuid(), info);
        
        // Retrieve the active job count from the database.
        List<JobActiveCount> activeCountList;
        try {activeCountList = JobDao.getSchedulerActiveJobCount();}
        catch (Exception e) {
            String msg = "Unable to retrieve active job count from database.";
            _log.error(msg, e);
            throw new JobException(msg, e);
        }
        
        // Populate the quota search tree..
        populateTree(activeCountList);
    }

    /* ********************************************************************** */
    /*                             Public Methods                             */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* exceedsQuota:                                                          */
    /* ---------------------------------------------------------------------- */
    /** Determine if any quota would be violated if the job described in the info 
     * record was submitted.   
     * 
     * @param info job quota information
     * @return true if at least one quota check fails, false if no quota is exceeded
     */
    public boolean exceedsQuota(JobQuotaInfo info)
    {
        // Check all individual quotas until
        // encountering one that is exceeded.
        return exceedsSystemQuota(info)      ||
               exceedsSystemUserQuota(info)  ||
               exceedsQueueQuota(info)       ||
               exceedsQueueUserQuota(info);
    }
    
    /* ---------------------------------------------------------------------- */
    /* exceedsSystemQuota:                                                    */
    /* ---------------------------------------------------------------------- */
    /** Determine if submitting the job described in the info record would exceed
     * the tenant's system quota.
     * 
     * @param info job quota information
     * @return true if submitting job would exceed quota, false otherwise
     */
    public boolean exceedsSystemQuota(JobQuotaInfo info)
    {
        // -------------------------- Simple Checks ------------------------
        // Check to see if there's any set limit.
        if (info.getMaxSystemJobs() < 0)  return false; // no limit
        if (info.getMaxSystemJobs() == 0) {
            _log.warn(info.getExecutionSystem() + " has no capacity for new jobs.");
            return true;  // no capacity
        }
        
        // -------------------------- Cache Check --------------------------
        // Check the cache in case we already counted the required information.
        String cacheKey = getCacheKey(info.getTenantId(), info.getExecutionSystem());
        Long cacheValue = _countCache.get(cacheKey);
        if (cacheValue != null)
        {
            // Maybe there's no limit.
            if (cacheValue < 0) return false;
            
            // Compare this job's information with the calculated count.
            if (cacheValue < info.getMaxSystemJobs()) return false;
              else {
                  _log.warn("> System " + info.getExecutionSystem() + 
                            " is currently at capacity for new jobs.");
                  return true;
              }
        }
        
        // -------------------------- Count Calculation --------------------
        // Get the tenant's current activity.
        HashMap<String, HashMap<String, HashMap<String, JobActiveCount>>> 
            tenantSubtree = _tree.get(info.getTenantId());
        if (tenantSubtree == null || tenantSubtree.isEmpty()) {
            // No activity at this level.
            _countCache.put(cacheKey, 0L);
            return false;
        }
        
        // Get the system's current activity.
        HashMap<String, HashMap<String, JobActiveCount>> 
            systemSubtree = tenantSubtree.get(info.getExecutionSystem());
        if (systemSubtree == null || systemSubtree.isEmpty()) {
            // No activity at this level.
            _countCache.put(cacheKey, 0L);
            return false;
        }        
        
        // Count all the active jobs in the system, on all queues, for all users.
        long jobCount = 0;
        for (HashMap<String, JobActiveCount> queue : systemSubtree.values()) {
            for (JobActiveCount activeCount : queue.values()) {
                // Increment the active job count by the aggregated count from the db.
                jobCount += activeCount.getCount();
            }
        }
        
        // -------------------------- Count Check --------------------------
        // Cache the count for future use.
        _countCache.put(cacheKey, jobCount);
        
        // Check the quota.
        if (jobCount < info.getMaxSystemJobs()) return false;
         else {
             _log.warn("System " + info.getExecutionSystem() + " is currently at capacity for new jobs.");
             return true; // exceeded quota
         }
    }

    /* ---------------------------------------------------------------------- */
    /* exceedsSystemUserQuota:                                                */
    /* ---------------------------------------------------------------------- */
    /** Determine if submitting the job described in the info record would exceed
     * the tenant's system quota for the job's owner.
     * 
     * @param info job quota information
     * @return true if submitting job would exceed quota, false otherwise
     */
    public boolean exceedsSystemUserQuota(JobQuotaInfo info)
    {
        // -------------------------- Simple Checks ------------------------
        // Check to see if there's any set limit.
        if (info.getMaxSystemUserJobs() < 0)  return false; // no limit
        if (info.getMaxSystemUserJobs() == 0) {
            _log.warn("User " + info.getOwner() + " has no capacity for jobs on " +
                      info.getExecutionSystem() + ".");
            return true;  // no capacity 
        }
        
        // -------------------------- Cache Check --------------------------
        // Check the cache in case we already counted the required information.
        String cacheKey = getCacheKey(info.getTenantId(), info.getExecutionSystem(),
                                      ALL_QUEUES, info.getOwner());
        Long cacheValue = _countCache.get(cacheKey);
        if (cacheValue != null)
        {
            // Maybe there's no limit.
            if (cacheValue < 0) return false;
            
            // Compare this job's information with the calculated count.
            if (cacheValue < info.getMaxSystemUserJobs()) return false;
              else {
                  _log.warn("> User " + info.getOwner() + " has reached its quota for " +
                            "concurrent active jobs on " + info.getExecutionSystem() + ".");
                  return true;
              }
        }
        
        // -------------------------- Count Calculation --------------------
        // Get the tenant's current activity.
        HashMap<String, HashMap<String, HashMap<String, JobActiveCount>>> 
            tenantSubtree = _tree.get(info.getTenantId());
        if (tenantSubtree == null || tenantSubtree.isEmpty()) {
            // No activity at this level.
            _countCache.put(cacheKey, 0L);
            return false;
        }
        
        // Get the system's current activity.
        HashMap<String, HashMap<String, JobActiveCount>> 
            systemSubtree = tenantSubtree.get(info.getExecutionSystem());
        if (systemSubtree == null || systemSubtree.isEmpty()) {
            // No activity at this level.
            _countCache.put(cacheKey, 0L);
            return false;
        }        
        
        // Count all the active jobs in the system, on all queues, for the job owner.
        long jobCount = 0;
        for (HashMap<String, JobActiveCount> queue : systemSubtree.values()) {
            JobActiveCount activeCount = queue.get(info.getOwner());
            
            // Increment the active job count by the number of jobs in the list.
            if (activeCount != null) jobCount += activeCount.getCount();
        }
        
        // -------------------------- Count Check --------------------------
        // Cache the count for future use.
        _countCache.put(cacheKey, jobCount);
        
        // Check the quota.
        if (jobCount < info.getMaxSystemUserJobs()) return false;
         else {
             _log.warn("User " + info.getOwner() + " has reached its quota for " +
                       "concurrent active jobs on " + info.getExecutionSystem() + ".");
             return true; // exceeded quota
         }
    }
    
    /* ---------------------------------------------------------------------- */
    /* exceedsQueueQuota:                                                     */
    /* ---------------------------------------------------------------------- */
    /** Determine if submitting the job described in the info record would exceed
     * the tenant's system quota for the batchqueue.
     * 
     * @param info job quota information
     * @return true if submitting job would exceed quota, false otherwise
     */
    public boolean exceedsQueueQuota(JobQuotaInfo info)
    {
        // -------------------------- Simple Checks ------------------------
        // Check to see if there's any set limit.
        if (info.getMaxQueueJobs() < 0)  return false; // no limit
        if (info.getMaxQueueJobs() == 0) {
            _log.warn("System " + info.getExecutionSystem() + 
                      " has no capacity for jobs on queue " + info.getQueueRequest() + ".");
            return true;  // no capacity
        }
        
        // -------------------------- Cache Check --------------------------
        // Check the cache in case we already counted the required information.
        String cacheKey = getCacheKey(info.getTenantId(), info.getExecutionSystem(),
                                      info.getQueueRequest());
        Long cacheValue = _countCache.get(cacheKey);
        if (cacheValue != null)
        {
            // Maybe there's no limit.
            if (cacheValue < 0) return false;
            
            // Compare this job's information with the calculated count.
            if (cacheValue < info.getMaxQueueJobs()) return false;
              else {
                  _log.warn("> System " + info.getExecutionSystem() + " is at maximum capacity for " +
                            "concurrent active jobs on queue " + info.getQueueRequest() + ".");
                  return true;
              }
        }
        
        // -------------------------- Count Calculation --------------------
        // Get the tenant's current activity.
        HashMap<String, HashMap<String, HashMap<String, JobActiveCount>>> 
            tenantSubtree = _tree.get(info.getTenantId());
        if (tenantSubtree == null || tenantSubtree.isEmpty()) {
            // No activity at this level.
            _countCache.put(cacheKey, 0L);
            return false;
        }
        
        // Get the system's current activity.
        HashMap<String, HashMap<String, JobActiveCount>> 
            systemSubtree = tenantSubtree.get(info.getExecutionSystem());
        if (systemSubtree == null || systemSubtree.isEmpty()) {
            // No activity at this level.
            _countCache.put(cacheKey, 0L);
            return false;
        }        
        
        // Get the queue's current activity.
        HashMap<String, JobActiveCount> queueSubTree = systemSubtree.get(info.getQueueRequest());
        if (queueSubTree == null || queueSubTree.isEmpty()) {
            // No activity at this level.
            _countCache.put(cacheKey, 0L);
            return false;
        }        
        
        // Count all the active jobs in the system, on this queue, for all users.
        long jobCount = 0;
        for (JobActiveCount activeCount : queueSubTree.values()) {
                // Increment the active job count by the number of jobs in the list.
                jobCount += activeCount.getCount();
        }
        
        // -------------------------- Count Check --------------------------
        // Cache the count for future use.
        _countCache.put(cacheKey, jobCount);
        
        // Check the quota.
        if (jobCount < info.getMaxQueueJobs()) return false;
         else {
             _log.warn("System " + info.getExecutionSystem() + " is at maximum capacity for " +
                       "concurrent active jobs on queue " + info.getQueueRequest() + ".");
             return true; // exceeded quota
         }
    }
    
    /* ---------------------------------------------------------------------- */
    /* exceedsQueueUserQuota:                                                 */
    /* ---------------------------------------------------------------------- */
    /** Determine if submitting the job described in the info record would exceed
     * the tenant's system quota for the batchqueue for the job owner.
     * 
     * @param info job quota information
     * @return true if submitting job would exceed quota, false otherwise
     */
    public boolean exceedsQueueUserQuota(JobQuotaInfo info)
    {
        // -------------------------- Simple Checks ------------------------
        // Check to see if there's any set limit.
        if (info.getMaxQueueUserJobs() < 0)  return false; // no limit
        if (info.getMaxQueueUserJobs() == 0) {
            _log.warn("User " + info.getOwner() + " has no capacity for jobs on the " +
                      info.getQueueRequest() + " queue on system " + info.getExecutionSystem() + ".");
            return true;  // no capacity
        }
        
        // -------------------------- Cache Check --------------------------
        // Check the cache in case we already counted the required information.
        String cacheKey = getCacheKey(info.getTenantId(), info.getExecutionSystem(),
                                      info.getQueueRequest(), info.getOwner());
        Long cacheValue = _countCache.get(cacheKey);
        if (cacheValue != null)
        {
            // Maybe there's no limit.
            if (cacheValue < 0) return false;
            
            // Compare this job's information with the calculated count.
            if (cacheValue < info.getMaxQueueUserJobs()) return false;
              else {
                  _log.warn("> User " + info.getOwner() + " has reached its quota for " +
                            "concurrent active jobs on the " + info.getQueueRequest() + 
                            " queue on system " + info.getExecutionSystem() + ".");
                  return true;
              }
        }
        
        // -------------------------- Count Calculation --------------------
        // Get the tenant's current activity.
        HashMap<String, HashMap<String, HashMap<String, JobActiveCount>>> 
            tenantSubtree = _tree.get(info.getTenantId());
        if (tenantSubtree == null || tenantSubtree.isEmpty()) {
            // No activity at this level.
            _countCache.put(cacheKey, 0L);
            if (info.getMaxQueueUserJobs() != 0L) return false;
              else return true;
        }
        
        // Get the system's current activity.
        HashMap<String, HashMap<String, JobActiveCount>> 
            systemSubtree = tenantSubtree.get(info.getExecutionSystem());
        if (systemSubtree == null || systemSubtree.isEmpty()) {
            // No activity at this level.
            _countCache.put(cacheKey, 0L);
            return false;
        }        
        
        // Get the queue's current activity.
        HashMap<String, JobActiveCount> queueSubTree = systemSubtree.get(info.getQueueRequest());
        if (queueSubTree == null || queueSubTree.isEmpty()) {
            // No activity at this level.
            _countCache.put(cacheKey, 0L);
            return false;
        }        
        
        // Count all the active jobs in the system, on this queue, for the job owner.
        long jobCount = 0;
        JobActiveCount activeCount = queueSubTree.get(info.getOwner());
        if (activeCount != null) jobCount += activeCount.getCount();
        
        // -------------------------- Count Check --------------------------
        // Cache the count for future use.
        _countCache.put(cacheKey, jobCount);
        
        // Check the quota.
        if (jobCount < info.getMaxQueueUserJobs()) return false;
         else {
             _log.warn("User " + info.getOwner() + " has reached its quota for " +
                       "concurrent active jobs on the " + info.getQueueRequest() + 
                       " queue on system " + info.getExecutionSystem() + ".");
             return true; // exceeded quota
         }
    }
    
    /* ---------------------------------------------------------------------- */
    /* keep:                                                                  */
    /* ---------------------------------------------------------------------- */
    /** Post-priority check to determine if job should be scheduled now or if
     * scheduling it would exceed some quota.  Before this method is called
     * with the given job, it was called once for all higher priority jobs. 
     * We update internal counters that indicate when a threshold would be exceeded 
     * given the number of higher priority jobs.  Basically, we cut off lower
     * priority jobs once the higher priority ones have filled a quota.
     * 
     * @param job the job that was preceded by all higher priority jobs
     * @return true if this job should be scheduled, false if scheduling this
     *         job could cause a quota to be exceeded
     */
    @Override
    public boolean keep(Job job)
    {
        // Make sure we have an initialized counter object.
        if (_counters == null) resetCounters();
        
        // Get the job's quota information before doing anything.
        JobQuotaInfo info = _quotaInfoMap.get(job.getUuid());
        if (info == null) {
            String msg = "Did not find job " + job.getUuid() + 
               " in quota info mapping.  Scheduling this job without quota checking.";
            _log.warn(msg);
            return true;  // Cause this unknown job to be scheduled.
        }
        
        // Generate the keys for each of the four quota values.
        String systemKey = getCacheKey(job.getTenantId(), job.getSystem());
        String systemUserKey = getCacheKey(job.getTenantId(), job.getSystem(),
                                           ALL_QUEUES, job.getOwner());
        String queueKey = getCacheKey(job.getTenantId(), job.getSystem(),
                                      job.getBatchQueue());
        String queueUserKey = getCacheKey(job.getTenantId(), job.getSystem(),
                                      job.getBatchQueue(), job.getOwner());
        
        // Check each quota.  Nulls shouldn't happen, 
        // but we correct them if they do.
        Long systemValue = _counters.getKey(systemKey);
        if (systemValue == null) systemValue = 0L;
        if (systemValue >= info.getMaxSystemJobs())
            return false;
        Long systemUserValue = _counters.getKey(systemUserKey);
        if (systemUserValue == null) systemUserValue = 0L;
        if (systemUserValue >= info.getMaxSystemUserJobs())
            return false;
        Long queueValue = _counters.getKey(queueKey);
        if (queueValue == null) queueValue = 0L;
        if (queueValue >= info.getMaxQueueJobs())
            return false;
        Long queueUserValue = _counters.getKey(queueUserKey);
        if (queueUserValue == null) queueUserValue = 0L;
        if (queueUserValue >= info.getMaxQueueUserJobs())
            return false;
        
        // Increment and replace each running quota value.
        HashMap<String, Long> countMap = _counters.getCountMap();
        countMap.put(systemKey, systemValue + 1);
        countMap.put(systemUserKey, systemUserValue + 1);
        countMap.put(queueKey, queueValue + 1);
        countMap.put(queueUserKey, queueUserValue + 1);
        
        // Allow this job to be scheduled.
        return true;
    }
    
    /* ---------------------------------------------------------------------- */
    /* resetCounters:                                                         */
    /* ---------------------------------------------------------------------- */
    /** Reset the quota counters to zero before filtering.
     */
    @Override
    public void resetCounters()
    {
        // Reset the counters object to only include the active jobs 
        // we obtained on construction from the database.
        _counters = new ActiveAndScheduledJobCounters();
    }
    
    /* ********************************************************************** */
    /*                            Private Methods                             */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* createSearchTree:                                                      */
    /* ---------------------------------------------------------------------- */
    /** Create a tree structure that allows JobQueueInfo objects to be arranged
     * to facilitate quota checking.  The idea is to use the hierarchical 
     * structure to quickly locate the actual job counts when checking system, 
     * queue and user quotas. 
     * 
     * @return the tree structure
     */
    private HashMap<String,HashMap<String,HashMap<String,HashMap<String,JobActiveCount>>>> 
        createSearchTree()
    {
        return  new HashMap<String,                           // Tenant
                        HashMap<String,                       // System
                            HashMap<String,                   // Queue
                                HashMap<String,               // Owner (User)
                                        JobActiveCount>>>>(); // Count info
    }

    /* ---------------------------------------------------------------------- */
    /* populateTree:                                                          */
    /* ---------------------------------------------------------------------- */
    /** Insert each record into its slot in the search tree.  The tree hierarchy,
     * from top to bottom, is tenant->system->queue->user with active count objects 
     * inserted as leaves.  Each record contains 5 non-null fields, 4 of which 
     * are defined as non-null in the jobs table and a summation field that is 
     * at least 1.
     * 
     * @param activeCountList the active job count records to be inserted into search tree
     */
    private void populateTree(List<JobActiveCount> activeCountList)
    {
        // Insert each quota record into the tree.
        for (JobActiveCount activeCount : activeCountList)
        {
            // Get the tenant subtree.
            HashMap<String, HashMap<String, HashMap<String, JobActiveCount>>> 
                tenantSubtree = _tree.get(activeCount.getTenantId());
            if (tenantSubtree == null) {
                tenantSubtree = new HashMap<String, HashMap<String, HashMap<String, JobActiveCount>>>();
                _tree.put(activeCount.getTenantId(), tenantSubtree);
            }
            
            // Get the system subtree.
            HashMap<String, HashMap<String, JobActiveCount>> 
                systemSubtree = tenantSubtree.get(activeCount.getExecutionSystem());
            if (systemSubtree == null) {
                systemSubtree = new HashMap<String, HashMap<String, JobActiveCount>>();
                tenantSubtree.put(activeCount.getExecutionSystem(), systemSubtree);
            }
            
            // Get the queue subtree.
            HashMap<String, JobActiveCount> queueSubtree = systemSubtree.get(activeCount.getQueueRequest());
            if (queueSubtree == null) {
                queueSubtree = new HashMap<String, JobActiveCount>();
                systemSubtree.put(activeCount.getQueueRequest(), queueSubtree);
            }
            
            // Insert the info object into the queue subtree for a specific user.
            queueSubtree.put(activeCount.getOwner(), activeCount);
        }
    }

    /* ---------------------------------------------------------------------- */
    /* getCacheKey:                                                           */
    /* ---------------------------------------------------------------------- */
    /** Create a cache key using the search tree path levels to construct a
     * unique string.  For example, a key that incorporates a tenantId and 
     * system name would be constructed using <tenant>|<system>, where actual
     * values replace their respective placeholders.
     * 
     * @param components 1 or more path component values
     * @return a unique string constructed from the components
     */
    private String getCacheKey(String... components)
    {
        // Concatenate a string from path components.
        String s = "";
        for (String component : components) 
            if (s.isEmpty()) s += component;
             else s += PATH_SEPARATOR + component;
        return s;
    }

    /* ********************************************************************** */
    /*                    ActiveAndScheduledJobCounters Class                 */
    /* ********************************************************************** */
    /** Track currently scheduled jobs plus jobs that are candidates for
     * scheduling.  Jobs passed to the keep() method are identified as candidates
     * for scheduling.
     */
    private final class ActiveAndScheduledJobCounters
    {
        // --- Fields
        private final HashMap<String,Long> _countMap = new HashMap<>();
        
        // --- Constructor
        private ActiveAndScheduledJobCounters()
        {
            // Initialize with active job counts.
            initActiveSystemCounts(_countMap);
        }
        
        // Accessor.
        private HashMap<String,Long> getCountMap(){return _countMap;}
        
        /* ---------------------------------------------------------------------- */
        /* getKey:                                                                */
        /* ---------------------------------------------------------------------- */
        /** Get the count associated with the specified key.  The key represents 
         * one of the four quotas for a job.  The count value represents the active
         * jobs contributing to that quota plus the number of jobs recommended for
         * scheduling in this round of quota checking (i.e., this object's filter
         * processing).  
         * 
         * If the job was represented with a job quota information object when the
         * enclosing class object was instantiated, then a non-null numeric value
         * will be returned.  We don't strictly enforce this initialization 
         * requirement, so when it is not observed, this method returns null. 
         * 
         * @param key a quota key (same as cache key in parent class)
         * @return a numeric value or null 
         */
        private Long getKey(String key)
        {
            return _countMap.get(key);
        }
        
        /* ---------------------------------------------------------------------- */
        /* initActiveSystemCounts:                                                */
        /* ---------------------------------------------------------------------- */
        /** Get the count of active and provisionally scheduled jobs for a system.  
         * 
         * This method will hit the cache if the info record has been seen before.  
         * Cache hits are the usual case since quotas are checked before any filtering 
         * is done, and this methodis only called by the filtering code (i.e., keep()).
         * 
         * @param info job quota information
         * @return the count of active and prospectively scheduled jobs.
         */
        private void initActiveSystemCounts(HashMap<String,Long> countMap)
        {
            // The tenants occupy the top level of the tree (tenant->system->queue->user).
            Collection<Entry<String, HashMap<String, HashMap<String, HashMap<String, JobActiveCount>>>>> tenants =
                _tree.entrySet();
            
            // Iterate through each of the tenants.
            for (Entry<String, HashMap<String, HashMap<String, HashMap<String, JobActiveCount>>>> tenantEntry : tenants)
            {
                // Unpack the entry.
                HashMap<String, HashMap<String, HashMap<String, JobActiveCount>>> systems = tenantEntry.getValue();
                
                // Iterate through systems.
                for (Entry<String, HashMap<String, HashMap<String, JobActiveCount>>> systemEntry : systems.entrySet())
                {
                    // Unpack the entry.
                    HashMap<String, HashMap<String, JobActiveCount>> queues = systemEntry.getValue();
                    
                    // Iterate through queues.
                    for (Entry<String, HashMap<String, JobActiveCount>> queueEntry : queues.entrySet())
                    {
                        // Unpack the entry.
                        HashMap<String, JobActiveCount> users = queueEntry.getValue();
                        
                        // Iterate through users.
                        for (Entry<String, JobActiveCount> userEntry : users.entrySet())
                        {
                            // Unpack the entry.
                            JobActiveCount activeCount = userEntry.getValue();
                            
                            // Increment the system count.
                            String systemKey = getCacheKey(activeCount.getTenantId(), activeCount.getExecutionSystem());
                            Long count = countMap.get(systemKey);
                            if (count == null) countMap.put(systemKey, 1L);
                             else countMap.put(systemKey, count + 1);
                            
                            // Increment the system/user count.
                            String systemUserKey = getCacheKey(activeCount.getTenantId(), activeCount.getExecutionSystem(),
                                                               ALL_QUEUES, activeCount.getOwner());
                            count = countMap.get(systemUserKey);
                            if (count == null) countMap.put(systemUserKey, 1L);
                             else countMap.put(systemUserKey, count + 1);

                            // Increment the queue count.
                            String queueKey = getCacheKey(activeCount.getTenantId(), activeCount.getExecutionSystem(),
                                                          activeCount.getQueueRequest());
                            count = countMap.get(queueKey);
                            if (count == null) countMap.put(queueKey, 1L);
                             else countMap.put(queueKey, count + 1);

                            // Increment the queue/user count.
                            String queueUserKey = getCacheKey(activeCount.getTenantId(), activeCount.getExecutionSystem(),
                                                              activeCount.getQueueRequest(), activeCount.getOwner());
                            count = countMap.get(queueUserKey);
                            if (count == null) countMap.put(queueUserKey, 1L);
                             else countMap.put(queueUserKey, count + 1);
                        }
                        
                    }
                }
            }
        }
    }
    
    /* ********************************************************************** */
    /*                            Test Constructor                            */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* Constructor:                                                           */
    /* ---------------------------------------------------------------------- */
    /**   ****** TEST-ONLY CONSTRUCTOR  --  NOT FOR PRODUCTION USE ******s 
     * 
     * This package scope test-only constructor can be called by unit tests
     * that are in the same package (split directories).  It allows the test
     * program to pass in an already populated active job count list to avoid
     * making a database call.  The 
     * 
     * @param quotaInfoList the quota objects for all jobs that will be submitted
     *                      to this class instance
     * @param activeCountList the prefabricated, non-null, active job count list
     * @throws JobException on error
     */
    JobQuotaChecker(List<JobQuotaInfo> quotaInfoList, List<JobActiveCount> activeCountList) 
            throws JobException 
           {
               // Make sure we have a non-empty input list.
               if (quotaInfoList == null || quotaInfoList.isEmpty()) {
                   String msg = "Invalid quota information list used to initialize quota checker.";
                   _log.error(msg);
                   throw new JobException(msg);
               } 
               
               // Save the quota information for the final filtering stage.
               _quotaInfoMap = new HashMap<>(1 + quotaInfoList.size() * 2);
               for (JobQuotaInfo info : quotaInfoList) _quotaInfoMap.put(info.getUuid(), info);
               
               // Populate the quota search tree..
               populateTree(activeCountList);
           }
}
