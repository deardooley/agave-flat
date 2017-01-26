package org.iplantc.service.jobs.phases.schedulers.strategies;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.iplantc.service.jobs.exceptions.JobException;
import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.jobs.phases.schedulers.strategies.impl.JobCreateOrder;
import org.iplantc.service.jobs.phases.schedulers.strategies.impl.TenantRandom;
import org.iplantc.service.jobs.phases.schedulers.strategies.impl.UserRandom;
import org.testng.Assert;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

/** Very basic test program to see if we can read and write topics and queues.
 * This program depends on certain queues existing in the server and the 
 * server running.
 * 
 * This test needs to be formalized as part of a test suite that checks that
 * the positive and negative behaviors are exhibited as expected.  Currently, 
 * one checks that the tests passed by looking at the server logs to
 * see what requests were processed.
 * 
 * @author rcardone
 *
 */
public class PrioritizedJobsTest 
{
    /* ********************************************************************** */
    /*                               Constants                                */
    /* ********************************************************************** */ 
    private static final int NUM_ITERATIONS = 1000;
    
    /* ********************************************************************** */
    /*                                 Fields                                 */
    /* ********************************************************************** */   
    // Jobs list used by tests.
    private List<Job> _unprioritizedJobs;
    
    /* ********************************************************************** */
    /*                            Set Up / Tear Down                          */
    /* ********************************************************************** */    
    /* ---------------------------------------------------------------------- */
    /* setup:                                                                 */
    /* ---------------------------------------------------------------------- */
    @BeforeSuite
    private void setup() throws Exception
    {
        _unprioritizedJobs = createJobs();
    }
    
    /* ---------------------------------------------------------------------- */
    /* teardown:                                                              */
    /* ---------------------------------------------------------------------- */
    @AfterSuite
    private void teardown() throws IOException
    {
    }
    
    /* ********************************************************************** */
    /*                              Test Methods                              */
    /* ********************************************************************** */    
    /* ---------------------------------------------------------------------- */
    /* randomStategy:                                                         */
    /* ---------------------------------------------------------------------- */
    @Test(enabled=true)
    public void randomStategy() 
    {   
        // Create a map to tally the job ordering.
        // The keys are job ordering strings and the values
        // the number of times that ordering appeared.
        TreeMap<String,Integer> tallyMap = new TreeMap<>();
        
        for (int i = 0; i < NUM_ITERATIONS; i++)
        {
            // Create other result fields.
            String jobOrder = "";
            boolean seenJob1 = false;
            boolean seenJob3 = false;
            boolean seenJob5 = false;
            
            // Create job prioritizer. Each new instance randomizes
            // in its own way, so we'd expect to see a distribution
            // across different orderings.
            PrioritizedJobs prioritizedJobs = 
                new PrioritizedJobs(new TenantRandom(), 
                                    new UserRandom(), 
                                    new JobCreateOrder(), 
                                    _unprioritizedJobs);
            
            // Get the jobs with the current prioritization.
            Iterator<Job> it = prioritizedJobs.iterator();
            while (it.hasNext()) {
                Job job = it.next();
                
                // Time order check.
                if (job.getUuid().equals("job1")) 
                    seenJob1 = true;
                else if (job.getUuid().equals("job2"))
                    Assert.assertTrue(seenJob1, "Job 2 appeared before Job1!");
                else if (job.getUuid().equals("job3")) 
                    seenJob3 = true;
                else if (job.getUuid().equals("job4"))
                    Assert.assertTrue(seenJob3, "Job 4 appeared before Job3!");
                else if (job.getUuid().equals("job5")) 
                    seenJob5 = true;
                else if (job.getUuid().equals("job6"))
                    Assert.assertTrue(seenJob5, "Job 6 appeared before Job5!");
                
                // Accumulate the ordering string.
                jobOrder += job.getUuid() + " ";
            }
            
            // Add the new ordering to the tally map.
            Integer count = tallyMap.get(jobOrder);
            if (count == null) tallyMap.put(jobOrder, 1);
             else tallyMap.put(jobOrder, count + 1);
        }
        
        // Print tally. The 6 jobs that we define have their ordering constrained
        // by their create times. The first element in each pair ((1,2), (3,4), (5,6)) 
        // must appear before the second element, and pair elements appear consectutively.
        // The pairs themselves can occur in any order.  Since there are 3 pairs,
        // there are 6 possible permutations.  It is very likely that all 6 permutations
        // will appear in the tally map as long as the number of iterations is fairly
        // large (on the order of 1000).
        Set<Entry<String, Integer>> entries = tallyMap.entrySet();
        for (Entry<String, Integer> entry : entries)
            System.out.println(entry.getKey() + ": " + entry.getValue());
        
        // Play the odds.
        Assert.assertEquals(tallyMap.size(), 6, 
                            "Run again to see if we don't get all 6 permutations.");
    }

    /* ********************************************************************** */
    /*                            Private Methods                             */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* createJobs:                                                            */
    /* ---------------------------------------------------------------------- */
    /** Initialize a test jobs list. 
     * @throws JobException */
    private List<Job> createJobs() throws JobException
    {
        // Result list.
        ArrayList<Job> jobs = new ArrayList<>();
        
        // Get current time as starting point for time calculations.
        Date baseDate = new Date();
        
        // We fill in the fields required by current strategy classes
        // plus some other descriptive fields.
        
        // --- Tenant 1 jobs
        Job job = new Job();
        job.setUuid("job1");
        job.setTenantId("tenant1");
        job.setOwner("owner1");
        job.setCreated(new Date(baseDate.getTime() - 9000));
        job.setLastUpdated(job.getCreated());
        jobs.add(job);
        
        job = new Job();
        job.setUuid("job2");
        job.setTenantId("tenant1");
        job.setOwner("owner1");
        job.setCreated(new Date(baseDate.getTime() - 8000));
        job.setLastUpdated(job.getCreated());
        jobs.add(job);

        // --- Tenant 2 jobs
        job = new Job();
        job.setUuid("job3");
        job.setTenantId("tenant2");
        job.setOwner("owner2");
        job.setCreated(new Date(baseDate.getTime() - 7000));
        job.setLastUpdated(job.getCreated());
        jobs.add(job);

        job = new Job();
        job.setUuid("job4");
        job.setTenantId("tenant2");
        job.setOwner("owner2");
        job.setCreated(new Date(baseDate.getTime() - 6000));
        job.setLastUpdated(job.getCreated());
        jobs.add(job);
        
        job = new Job();
        job.setUuid("job5");
        job.setTenantId("tenant3");
        job.setOwner("owner3");
        job.setCreated(new Date(baseDate.getTime() - 5000));
        job.setLastUpdated(job.getCreated());
        jobs.add(job);
        
        job = new Job();
        job.setUuid("job6");
        job.setTenantId("tenant3");
        job.setOwner("owner3");
        job.setCreated(new Date(baseDate.getTime() - 4000));
        job.setLastUpdated(job.getCreated());
        jobs.add(job);
        
        return jobs;
    }

}
