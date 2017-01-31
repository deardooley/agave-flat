package org.iplantc.service.jobs.phases.schedulers.filters;

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
import org.iplantc.service.jobs.phases.schedulers.filters.IPostPriorityJobFilter;
import org.iplantc.service.jobs.phases.schedulers.filters.PrioritizedJobs;
import org.iplantc.service.jobs.phases.schedulers.filters.ReadyJobs;
import org.iplantc.service.jobs.phases.schedulers.strategies.impl.JobCreateOrder;
import org.iplantc.service.jobs.phases.schedulers.strategies.impl.TenantRandom;
import org.iplantc.service.jobs.phases.schedulers.strategies.impl.UserRandom;
import org.testng.Assert;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

/** Basic job prioritization tests.  The nested ErsatzJobFilter class implements
 * the IPostPriorityJobFilter interface replacing the JobQuotaCheck implementation.
 * This substitution allows us to avoid interacting with the database, but the 
 * real IPostPriorityJobFilter.keep() method is not exercised. 
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
        // Announce ourselves.
        System.out.println("-------- Running randomStategy");
        
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
                                    new ReadyJobs(_unprioritizedJobs));
            
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

    /* ---------------------------------------------------------------------- */
    /* filteredRandomStategy:                                                 */
    /* ---------------------------------------------------------------------- */
    @Test(enabled=true)
    public void filteredRandomStategy() 
    {   
        // Announce ourselves.
        System.out.println("-------- Running filteredRandomStategy");
        
        // Create a map to tally the job ordering.
        // The keys are job ordering strings and the values
        // the number of times that ordering appeared.
        TreeMap<String,Integer> tallyMap = new TreeMap<>();
        
        for (int i = 0; i < NUM_ITERATIONS; i++)
        {
            // Create other result fields.
            String jobOrder = "";
            
            // Create job prioritizer. Each new instance randomizes
            // in its own way, so we'd expect to see a distribution
            // across different orderings.
            PrioritizedJobs prioritizedJobs = 
                new PrioritizedJobs(new TenantRandom(), 
                                    new UserRandom(), 
                                    new JobCreateOrder(), 
                                    new ReadyJobs(_unprioritizedJobs,
                                                  new ErsatzJobFilter()));
            
            // Get the jobs with the current prioritization.
            Iterator<Job> it = prioritizedJobs.iterator();
            while (it.hasNext()) {
                Job job = it.next();
                
                // Make sure the filtered jobs don't appear.
                String uuid = job.getUuid();
                Assert.assertFalse(uuid.equals("job1") || uuid.equals("job3") || uuid.equals("job5"), 
                                   "At least one filtered job was not filtered.");
                               
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

    /* ********************************************************************** */
    /*                         ErsatzJobFilter Class                          */
    /* ********************************************************************** */
    /** This class substitutes for JobQuotaChecker in the actual code by simply
     * filtering out some test jobs by hardcoding those jobs into the keep() method.
     * The idea is to simulate the effect of tallying active jobs plus previously
     * scheduled jobs as is done in JobQuotaChecker.keep().  
     */
    private static final class ErsatzJobFilter
     implements IPostPriorityJobFilter
    {
        @Override
        public boolean keep(Job job)
        {
            // Filter out some of the jobs.
            String uuid = job.getUuid();
            if (uuid.equals("job1") || uuid.equals("job3") || uuid.equals("job5"))
                return false;
            
            return true;
        }

        @Override
        public void resetCounters() {}
    }
}
