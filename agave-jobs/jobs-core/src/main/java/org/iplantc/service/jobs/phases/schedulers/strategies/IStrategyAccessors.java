package org.iplantc.service.jobs.phases.schedulers.strategies;

/** This interface defines the strategy accessors used by phase schedulers
 * to order and filter the tenants, users or jobs to process.
 * 
 * @author rcardone
 */
public interface IStrategyAccessors
{
    ITenantStrategy getTenantStrategy();
    IUserStrategy   getUserStrategy();
    IJobStrategy    getJobStrategy();
}
