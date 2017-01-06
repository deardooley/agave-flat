package org.iplantc.service.jobs.util;

import java.io.FileNotFoundException;

import org.apache.log4j.Logger;
import org.iplantc.service.jobs.dao.JobQueueDao;
import org.iplantc.service.jobs.exceptions.JobException;
import org.iplantc.service.jobs.model.JobQueue;
import org.iplantc.service.jobs.model.enumerations.JobPhaseType;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Class that reads the tenant queue configuration resource file
 * and updates the job_queues table with the latest information.
 * 
 * This test assumes a queue configuration as created by JobQueueDaoTest,
 * adds some more queues during setup, and processes the queue definitions
 * in the test resource file.
 * 
 * @author rcardone
 */
public final class TenantQueuesTest
{
    /* ********************************************************************** */
    /*                                Constants                               */
    /* ********************************************************************** */
    // Tracing.
    private static final Logger _log = Logger.getLogger(TenantQueuesTest.class);
    
    // Default tenant queue configuration file.
    private static final String TEST_TENANT = "iplantc.org";
    
    /* ********************************************************************** */
    /*                            Set Up / Tear Down                          */
    /* ********************************************************************** */    
    /* ---------------------------------------------------------------------- */
    /* setup:                                                                 */
    /* ---------------------------------------------------------------------- */
    @BeforeMethod
    private void setup()
    {
        // The tests in this file assume that some of the queues created by 
        // JobQueueDao.setup() exist.  Specifically, the following queues
        // are expected to be configured just as they were in JobQueueDaoTest:
        //
        //      ARCHIVING.iplantc.org
        //      MONITORING.iplantc.org
        //      STAGING.iplantc.org
        //      SUBMITTING.iplantc.org
        //
        // The following queues are created and then deleted by this suite.
        // By creating these queues here we configure them for update.
        createQueue("MONITORING.iplantc.org.UPDATE-1",
                    JobPhaseType.MONITORING,
                    "iplantc.org",
                    20,
                    21,
                    22,
                    "phase = 'MONITORING' AND tenant_id = 'iplantc.org'");
        createQueue("MONITORING.iplantc.org.UPDATE-2",
                    JobPhaseType.MONITORING,
                    "iplantc.org",
                    7,
                    7,
                    7,
                    "phase = 'MONITORING' AND tenant_id = 'iplantc.org'");
        createQueue("MONITORING.iplantc.org.UPDATE-FAILURE-1",
                    JobPhaseType.MONITORING,
                    "iplantc.org",
                    8,
                    8,
                    8,
                    "phase = 'MONITORING' AND tenant_id = 'iplantc.org'");
    }
    
    /* ---------------------------------------------------------------------- */
    /* teardown:                                                              */
    /* ---------------------------------------------------------------------- */
    @AfterMethod
    private void teardown()
    {
        // Delete the queues we created in setup and those created during
        // configuration file processing.
        JobQueueDao dao = new JobQueueDao();
        try {
            dao.deleteQueueByName("STAGING.iplantc.org.CREATE-1", "iplantc.org");
            dao.deleteQueueByName("STAGING.iplantc.org.CREATE-2", "iplantc.org");
            dao.deleteQueueByName("MONITORING.iplantc.org.UPDATE-1", "iplantc.org");
            dao.deleteQueueByName("MONITORING.iplantc.org.UPDATE-2", "iplantc.org");
            dao.deleteQueueByName("MONITORING.iplantc.org.UPDATE-FAILURE-1", "iplantc.org");
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /* ********************************************************************** */
    /*                             Public Methods                             */
    /* ********************************************************************** */ 
    /* ---------------------------------------------------------------------- */
    /* updateTest:                                                            */
    /* ---------------------------------------------------------------------- */
    @Test(enabled=true)
    public void updateTest()
     throws JobException, FileNotFoundException
    {
        // Update the queue configuration for a specific tenant.
        TenantQueues tenantQueues = new TenantQueues();
        TenantQueues.UpdateResult result = tenantQueues.update(TEST_TENANT);
        System.out.println(result.toString());
        
        // The results are a tight coupling between the queues expected to be 
        // defined before the test, queues defined in setup(), and queue 
        // definitions in the configuration file.
        Assert.assertEquals(result.queueDefinitionsRead, 14, "Unexpected number of read definitions.");
        Assert.assertEquals(result.queuesCreated.size(), 2, "Unexpected number of created queues.");
        Assert.assertEquals(result.queuesCreateFailed.size(), 2, "Unexpected number of create FAILED queues.");
        Assert.assertEquals(result.queuesUpdated.size(), 2, "Unexpected number of updated queues.");
        Assert.assertEquals(result.queuesUpdateFailed.size(), 1, "Unexpected number of update FAILED queues.");
        Assert.assertEquals(result.queuesNotChanged.size(), 4, "Unexpected number of skipped queues.");
        Assert.assertEquals(result.queuesRejected.size(), 3, "Unexpected number of rejected queues.");
    }
    
    /* ---------------------------------------------------------------------- */
    /* updateAllTest:                                                         */
    /* ---------------------------------------------------------------------- */
    @Test(enabled=true)
    public void updateAllTest()
     throws JobException, FileNotFoundException
    {
        // Update the queue configuration for all tenants.
        TenantQueues tenantQueues = new TenantQueues();
        TenantQueues.UpdateResult result = tenantQueues.updateAll();
        System.out.println(result.toString());
        
        // The results are a tight coupling between the queues expected to be 
        // defined before the test, queues defined in setup(), and queue 
        // definitions in the configuration file.
        Assert.assertEquals(result.queueDefinitionsRead, 14, "Unexpected number of read definitions.");
        Assert.assertEquals(result.queuesCreated.size(), 2, "Unexpected number of created queues.");
        Assert.assertEquals(result.queuesCreateFailed.size(), 2, "Unexpected number of create FAILED queues.");
        Assert.assertEquals(result.queuesUpdated.size(), 2, "Unexpected number of updated queues.");
        Assert.assertEquals(result.queuesUpdateFailed.size(), 1, "Unexpected number of update FAILED queues.");
        Assert.assertEquals(result.queuesNotChanged.size(), 4, "Unexpected number of skipped queues.");
        Assert.assertEquals(result.queuesRejected.size(), 3, "Unexpected number of rejected queues.");
    }
    
    /* ********************************************************************** */
    /*                            Private Methods                             */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* createQueue:                                                           */
    /* ---------------------------------------------------------------------- */
    /** General queue creation call that takes all parameters.
     * 
     * @param queueName
     * @param phase
     * @param tenantId
     * @param maxMessages
     * @param numWorkers
     * @param priority
     * @param filter
     */
    private void createQueue(String queueName,
                             JobPhaseType phase,
                             String tenantId,
                             int maxMessages,
                             int numWorkers,
                             int priority,
                             String filter)
    {
        // Define a basic queue.
        JobQueue jobQueue = new JobQueue();
        jobQueue.setName(queueName);
        jobQueue.setPhase(phase);
        jobQueue.setTenantId(tenantId);
        jobQueue.setMaxMessages(maxMessages);
        jobQueue.setNumWorkers(numWorkers);
        jobQueue.setPriority(priority);
        jobQueue.setFilter(filter);
        
        // Add queue to database.
        int rows = 0;
        JobQueueDao dao = new JobQueueDao();
        try {rows = dao.createQueue(jobQueue);}
         catch (Exception e) 
         {
             Assert.fail("Unable to define a new queue.", e);
         }
        
        // We expect one row to be inserted.
        Assert.assertEquals(rows, 1, "New queue not defined!");
        System.out.println("Queue created: " + queueName);
    }
}
