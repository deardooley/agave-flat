package org.iplantc.service.jobs.phases.schedulers.filters;

import org.iplantc.service.jobs.model.Job;

/** The PrioritizedJob class will call the keep(job) method for all
 * candidate jobs in priority order.  A class implementing this
 * interface can track the jobs presented to it and postpone the
 * processing of lower priority jobs when some threshold is exceeded.
 * For example, a quota enforcing class can allow the highest X 
 * number of jobs to proceed and delay lower priority jobs.
 *  
 * @author rcardone
 */
public interface IPostPriorityJobFilter
{
    /** Determine whether a particular job should be immediately scheduled
     * given that all higher priority jobs have previously been processed
     * by this method.
     * 
     * @param job the next highest priority job
     * @return true if the job should be immediately processed by the 
     *           calling scheduler, false if processing should be delayed
     *           for this job
     */
    boolean keep(Job job);
    
    /** Reset all counters to zero before calling keep() on a set of jobs.
     * This method only needs to be called if a filter is used more than
     * once, but it doesn't hurt to call it before any filtering exercise. 
     */
    void resetCounters();
}
