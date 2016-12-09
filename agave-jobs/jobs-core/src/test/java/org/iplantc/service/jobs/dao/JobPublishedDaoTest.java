package org.iplantc.service.jobs.dao;

import java.util.ArrayList;
import java.util.List;

import org.iplantc.service.common.util.AgaveStringUtils;
import org.iplantc.service.jobs.exceptions.JobException;
import org.iplantc.service.jobs.model.JobPublished;
import org.iplantc.service.jobs.model.enumerations.JobPhaseType;
import org.testng.Assert;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

/** Tests in this class run when jobs subsystem is NOT RUNNING and the
 * job_published table is empty.  These tests access the database directly 
 * through a DAO and will conflict with concurrently running application code.
 * 
 * @author rcardone
 */
public class JobPublishedDaoTest 
{
    /* ********************************************************************** */
    /*                                Constants                               */
    /* ********************************************************************** */
    // Total number of interrupts generated by test.
    private final static int NUM_PUBLISHED = 3;
    
    /* ********************************************************************** */
    /*                            Set Up / Tear Down                          */
    /* ********************************************************************** */
    // NOTE: If you add more tests, you may need revisit before/after time.s 
    
    /* ---------------------------------------------------------------------- */
    /* setup:                                                                 */
    /* ---------------------------------------------------------------------- */
    @BeforeSuite
    private void setup()
    {
        // Clear the interrupts table for testing.
        JobPublishedDao.clearPublishedJobs();
    }
    
    /* ---------------------------------------------------------------------- */
    /* teardown:                                                              */
    /* ---------------------------------------------------------------------- */
    @AfterSuite
    private void teardown()
    {
        // Clear the interrupts table for testing.
        JobPublishedDao.clearPublishedJobs();
    }
    
    /* ********************************************************************** */
    /*                              Test Methods                              */
    /* ********************************************************************** */    
    /* ---------------------------------------------------------------------- */
    /* interruptTest:                                                         */
    /* ---------------------------------------------------------------------- */
    @Test(enabled=true)
    public void interruptTest() throws JobException
    {   
        // Create a bunch of interrupts for a phase.
        List<JobPublished> publishedList1 = generatePublishedList(JobPhaseType.STAGING);
        for (JobPublished jobPublished : publishedList1)
        {
            int rows = JobPublishedDao.publish(jobPublished.getPhase(), 
                                               jobPublished.getJobUuid(),
                                               jobPublished.getCreator());
            Assert.assertEquals(rows, 1, 
                "Unable to publish job: " + AgaveStringUtils.toString(jobPublished));
        }
        
        // Create a bunch of interrupts for another phase.
        List<JobPublished> publishedList2 = generatePublishedList(JobPhaseType.ARCHIVING);
        for (JobPublished jobPublished : publishedList2)
        {
            int rows = JobPublishedDao.publish(jobPublished.getPhase(), 
                                               jobPublished.getJobUuid(),
                                               jobPublished.getCreator());
            Assert.assertEquals(rows, 1, 
                "Unable to publish job: " + AgaveStringUtils.toString(jobPublished));
        }
        
        // -------- STAGING Phase
        // Retrieve all published job records for the first phase.
        List<JobPublished> list1 = JobPublishedDao.getPublishedJobs(JobPhaseType.STAGING);
        Assert.assertEquals(list1.size(), NUM_PUBLISHED, 
                            "Unexpected number of published jobs for phase " + 
                              JobPhaseType.STAGING.name());
        
        // Check for an individual job record's existence.
        boolean exists = JobPublishedDao.hasPublishedJob(list1.get(0).getPhase(), 
                                                         list1.get(0).getJobUuid());
        Assert.assertTrue(exists, "Expected job record's existence.");
        
        // Delete a published job for the first phase.
        int deleted = JobPublishedDao.deletePublishedJob(list1.get(0).getPhase(), 
                                                         list1.get(0).getJobUuid());
        Assert.assertEquals(deleted, 1, 
                "Unable to delete job: " + AgaveStringUtils.toString(list1.get(0)));
        
        // Check for an individual job record's existence.
        exists = JobPublishedDao.hasPublishedJob(list1.get(0).getPhase(), 
                                                 list1.get(0).getJobUuid());
        Assert.assertFalse(exists, "Expected job record's existence.");
        
        // Retrieve all published job records for the first phase.
        list1 = JobPublishedDao.getPublishedJobs(JobPhaseType.STAGING);
        Assert.assertEquals(list1.size(), NUM_PUBLISHED - 1, 
                            "Unexpected number of published jobs for phase " + 
                              JobPhaseType.STAGING.name());
        
        // -------- ARCHIVING Phase
        // Retrieve all published job records for the second phase.
        List<JobPublished> list2 = JobPublishedDao.getPublishedJobs(JobPhaseType.ARCHIVING);
        Assert.assertEquals(list2.size(), NUM_PUBLISHED, 
                            "Unexpected number of published jobs for phase " + 
                              JobPhaseType.ARCHIVING.name());
        
        // Check for an individual job record's existence.
        exists = JobPublishedDao.hasPublishedJob(list2.get(0).getPhase(), 
                                                 list2.get(0).getJobUuid());
        Assert.assertTrue(exists, "Expected job record's existence.");
        
        // Delete a published job for the second phase.
        deleted = JobPublishedDao.deletePublishedJob(list2.get(0).getPhase(), 
                                                     list2.get(0).getJobUuid());
        Assert.assertEquals(deleted, 1, 
                "Unable to delete job: " + AgaveStringUtils.toString(list2.get(0)));
        
        // Check for an individual job record's existence.
        exists = JobPublishedDao.hasPublishedJob(list2.get(0).getPhase(), 
                                                 list2.get(0).getJobUuid());
        Assert.assertFalse(exists, "Expected job record's existence.");
        
        // Retrieve all published job records for the second phase.
        list2 = JobPublishedDao.getPublishedJobs(JobPhaseType.ARCHIVING);
        Assert.assertEquals(list2.size(), NUM_PUBLISHED - 1, 
                            "Unexpected number of published jobs for phase " + 
                              JobPhaseType.ARCHIVING.name());
    }

    /* ********************************************************************** */
    /*                            Private Methods                             */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* generateInterruptMap:                                                  */
    /* ---------------------------------------------------------------------- */
    private List<JobPublished> generatePublishedList(JobPhaseType phase)
    {
        // Result map.
        List<JobPublished> list = new ArrayList<JobPublished>();
        
        // Create the prescribed number of interrupts for each uniquely named 
        // job.  The keys are from 0 to NUM_INTERRUPTS - 1.
        for (int i = 0; i < NUM_PUBLISHED; i++)
        {
            JobPublished published = 
               new JobPublished(phase, getJobUuid(i), phase.name() + "-scheduler");
            list.add(published);
        }
        
        return list;
    }
    
    /* ---------------------------------------------------------------------- */
    /* getJobUuid:                                                            */
    /* ---------------------------------------------------------------------- */
    private String getJobUuid(int i)
    {
        return "PublishedJob-" + i;
    }
    
}
