package org.iplantc.service.jobs.phases.schedulers.strategies.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.jobs.phases.schedulers.strategies.IJobStrategy;

/**
 * @author rcardone
 */
public final class JobCreateOrder
 implements IJobStrategy
{
    /* ********************************************************************** */
    /*                             Public Methods                             */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* prioritizeJobs:                                                        */
    /* ---------------------------------------------------------------------- */
    @Override
    public List<Job> prioritizeJobs(String tenantId, String user, List<Job> jobs)
    {
        // Check input.
        if (tenantId == null || user == null || jobs == null || jobs.isEmpty()) 
            return new LinkedList<Job>();
        
        // Get the set of all job with the specified tenant and user.
        ArrayList<Job> jobList = new ArrayList<>(jobs.size());
        for (Job job : jobs) 
            if (tenantId.equals(job.getTenantId()) && user.equals(job.getOwner()))
                jobList.add(job);
        jobList.trimToSize();  // reclaim space
        
        // Put the jobs for the specified tenant/user into a list
        // and then order the list in ascending creation time.
        Collections.sort(jobList, new CreatedComparator());
        return jobList;
    }

    /* ********************************************************************** */
    /*                        CreatedComparator Class                         */
    /* ********************************************************************** */
    private static final class CreatedComparator
     implements Comparator<Job>
    {
        /** Compare the create times of two jobs.  Returns -1, 0, or 1 if the 
         * first job's time is less than, equal to, or greater than the second,
         * respectively.
         * 
         * Note: this comparator imposes orderings that are inconsistent with equals.
         *       The inconsistency derives from the fact that 
         *       (compare(x, y)==0) != (x.equals(y)).  That is, job with the
         *       same create time are not necessarily the same job.
         */
        @Override
        public int compare(Job job1, Job job2)
        {
            // Compare create times.
            if (job1.getCreated().before(job2.getCreated())) return -1;
            if (job2.getCreated().after(job2.getCreated()))  return  1;
            return 0;
        }
    }
}
