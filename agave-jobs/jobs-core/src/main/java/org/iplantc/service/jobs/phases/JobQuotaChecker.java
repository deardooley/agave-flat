package org.iplantc.service.jobs.phases;

import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.iplantc.service.jobs.dao.JobDao;
import org.iplantc.service.jobs.exceptions.JobException;
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
    
    /* ********************************************************************** */
    /*                              Constructors                              */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* constructor:                                                           */
    /* ---------------------------------------------------------------------- */
    public JobQuotaChecker() 
     throws JobException 
    {
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
        if (info.getMaxSystemJobs() == 0) return true;  // no capacity
        
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
              else return true;
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
         else return true; // exceeded quota
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
        if (info.getMaxSystemUserJobs() == 0) return true;  // no capacity 
        
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
              else return true;
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
         else return true; // exceeded quota
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
        if (info.getMaxQueueJobs() == 0) return true;  // no capacity
        
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
              else return true;
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
         else return true; // exceeded quota
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
        if (info.getMaxQueueUserJobs() == 0) return true;  // no capacity
        
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
              else return true;
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
         else return true; // exceeded quota
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
}
