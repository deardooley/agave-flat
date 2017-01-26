package org.iplantc.service.jobs.phases.schedulers.Strategies;

import java.util.List;

import org.iplantc.service.jobs.model.Job;

/** This interface defines the strategy method used by phase schedulers
 * to order and filter the jobs for processing.
 * 
 * @author rcardone
 */
public interface IJobStrategy
{
    /** This method must always return a non-null result.
     * 
     * @param tenantId the tenant whose jobs will be processed
     * @param user the user whose jobs will be processed
     * @param jobs the complete list of unprioritized jobs for all tenants and users
     * @return a list of jobs in priority order
     */
    List<Job> prioritizeJobs(String tenantId, String user, List<Job> jobs);
}
