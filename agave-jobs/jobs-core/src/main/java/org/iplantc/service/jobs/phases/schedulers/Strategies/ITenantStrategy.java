package org.iplantc.service.jobs.phases.schedulers.Strategies;

import java.util.List;

import org.iplantc.service.jobs.model.Job;

/** This interface defines the strategy method used by phase schedulers
 * to order and filter the tenants for processing.
 * 
 * @author rcardone
 */
public interface ITenantStrategy
{
    /** This method must always return a non-null result.
     * 
     * @param jobs the complete list of unprioritized jobs for all tenants and users
     * @return a list of tenants in priority order
     */
    List<String> prioritizeTenants(List<Job> jobs);
}
