package org.iplantc.service.jobs.phases.schedulers.strategies.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.jobs.phases.schedulers.strategies.ITenantStrategy;

/**
 * @author rcardone
 */
public final class TenantRandom
 implements ITenantStrategy
{
    /* ********************************************************************** */
    /*                             Public Methods                             */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* prioritizeTenants:                                                     */
    /* ---------------------------------------------------------------------- */
    @Override
    public List<String> prioritizeTenants(List<Job> jobs)
    {
        // Check input.
        if (jobs == null || jobs.isEmpty()) return new LinkedList<String>();
        
        // Get the set of all tenants.
        HashSet<String> tenantSet = new HashSet<>(1 + jobs.size() * 2);
        for (Job job : jobs) tenantSet.add(job.getTenantId());
        
        // Put the tenants ids into a list and then randomize the list.
        ArrayList<String> tenants = new ArrayList<>(tenantSet);
        Collections.shuffle(tenants);
        return tenants;
    }

}
