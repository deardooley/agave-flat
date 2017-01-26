package org.iplantc.service.jobs.phases.schedulers.Strategies.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.jobs.phases.schedulers.Strategies.IUserStrategy;

/**
 * @author rcardone
 */
public final class UserRandom
 implements IUserStrategy
{
    /* ********************************************************************** */
    /*                             Public Methods                             */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* prioritizeUsers:                                                       */
    /* ---------------------------------------------------------------------- */
    @Override
    public List<String> prioritizeUsers(String tenantId, List<Job> jobs)
    {
        // Check input.
        if (tenantId == null || jobs == null || jobs.isEmpty()) 
            return new LinkedList<String>();
        
        // Get the set of all users under the specified tenant.
        HashSet<String> userSet = new HashSet<>(1 + jobs.size() * 2);
        for (Job job : jobs) 
            if (tenantId.equals(job.getTenantId()))
                userSet.add(job.getOwner());
        
        // Put the users for the specified tenants into 
        // a list and then randomize the list.
        ArrayList<String> users = new ArrayList<>(userSet);
        Collections.shuffle(users);
        return users;
    }

}
