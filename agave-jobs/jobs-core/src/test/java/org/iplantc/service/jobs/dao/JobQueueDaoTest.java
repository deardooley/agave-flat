package org.iplantc.service.jobs.dao;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import org.iplantc.service.jobs.exceptions.JobQueueException;
import org.iplantc.service.jobs.exceptions.JobQueuePriorityException;
import org.iplantc.service.jobs.model.JobQueue;
import org.iplantc.service.jobs.model.enumerations.JobPhaseType;
import org.testng.Assert;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

public class JobQueueDaoTest {
    
    /* ********************************************************************** */
    /*                             Static Fields                              */
    /* ********************************************************************** */   
    // Special purpose queue names used in multiple places.
    private static final String MONITORING_QUEUENAME = "MONITORING.iplantc.org.Joan";
    
    // Maping of queue names to priorities.
    private static LinkedHashMap<String,Integer> _queueMap;
    
    /* ********************************************************************** */
    /*                            Set Up / Tear Down                          */
    /* ********************************************************************** */    
    /* ---------------------------------------------------------------------- */
    /* setup:                                                                 */
    /* ---------------------------------------------------------------------- */
    @BeforeSuite
    private void setup()
    {
        // Create the standard queues for the iplant tenant.
        _queueMap = new LinkedHashMap<>();  // name->priority
        _queueMap.put("ARCHIVING.iplantc.org", 1);
        _queueMap.put("MONITORING.iplantc.org", 1);
        _queueMap.put("STAGING.iplantc.org", 1);
        _queueMap.put("SUBMITTING.iplantc.org", 1);
        
        // Create any number of queues for the iplant tenant.
        _queueMap.put("STAGING.iplantc.org.Bob", 5);
        _queueMap.put("STAGING.iplantc.org.Harry", 10);
        _queueMap.put(MONITORING_QUEUENAME, 6); 
    }
    
    /* ---------------------------------------------------------------------- */
    /* teardown:                                                              */
    /* ---------------------------------------------------------------------- */
    @AfterSuite
    private void teardown()
    {
        // Delete all the queues in _queueMap
        int deleted = 0;
        JobQueueDao dao = new JobQueueDao();
        Iterator<Entry<String, Integer>> it = _queueMap.entrySet().iterator();
        while (it.hasNext()) {
            Entry<String, Integer> entry = it.next();
            try {
                deleted += dao.deleteQueueByName(entry.getKey(), "iplantc.org");
            } catch (JobQueueException e) {}
        }
        System.out.println("Queues deleted: " + deleted);
    }
    
    /* ********************************************************************** */
    /*                              Test Methods                              */
    /* ********************************************************************** */    
    /* ---------------------------------------------------------------------- */
    /* createIplantQueues:                                                    */
    /* ---------------------------------------------------------------------- */
    @Test(enabled=true)
    public void createIplantQueues()
    {   
        // Create all the queues in _queueMap
        Iterator<Entry<String, Integer>> it = _queueMap.entrySet().iterator();
        while (it.hasNext()) {
            Entry<String, Integer> entry = it.next();
            createIplantQueue(entry.getKey(), entry.getValue());
        }
    }

    /* ---------------------------------------------------------------------- */
    /* queryIplantQueues1AtATime:                                             */
    /* ---------------------------------------------------------------------- */
    /** Query each of the queues created in the depends-on method one at a time. */
    @Test(enabled=true, dependsOnMethods={"createIplantQueues"})
    public void queryIplantQueues1AtATime()
    {
        // Query all the queues in _queueMap
        JobQueueDao dao = new JobQueueDao();
        Iterator<Entry<String, Integer>> it = _queueMap.entrySet().iterator();
        while (it.hasNext()) {
            Entry<String, Integer> entry = it.next();
            String curQueueName = entry.getKey();
            
            JobQueue jobQueue = null;
            try {jobQueue = dao.getQueueByName(curQueueName, "iplantc.org");}
             catch (Exception e) 
             {
                Assert.fail("Unable to query queue: " + curQueueName, e);
             }
            
            Assert.assertNotNull(jobQueue, 
                    "Null returned when querying queue: " + curQueueName);
            
            System.out.println("Found queue: " + jobQueue.getName());
        }
    }
    
    /* ---------------------------------------------------------------------- */
    /* queryIplantQueueNegative:                                              */
    /* ---------------------------------------------------------------------- */
    @Test(enabled=true, dependsOnMethods={"createIplantQueues"})
    public void queryIplantQueueNegative()
    {
      JobQueue jobQueue = null;
      JobQueueDao dao = new JobQueueDao();
      try {jobQueue = dao.getQueueByName("STAGING.iplantc.org.NOTDEFINED", "iplantc.org");}
       catch (Exception e) 
       {
          Assert.fail("Unable to query queue: STAGING.iplantc.org.NOTDEFINED.", e);
       }
      
      Assert.assertNull(jobQueue, 
              "Did not expect to find queue: STAGING.iplantc.org.NOTDEFINED.");
    }
    
    /* ---------------------------------------------------------------------- */
    /* queryIplantQueuesAllAtOnce:                                            */
    /* ---------------------------------------------------------------------- */
    /** Query the queues created in the depends-on method all at once. */
    @Test(enabled=true, dependsOnMethods={"createIplantQueues"})
    public void queryIplantQueuesAllAtOnce()
    {
        // ----------------- Retrieve All Iplant Queues
        // Query all the queues in _queueMap
        JobQueueDao dao = new JobQueueDao();
        List<JobQueue> jobQueues = null;
        try {jobQueues = dao.getQueues("iplantc.org");}
        catch (Exception e) 
         {
            Assert.fail("Unable to query iplant queues all at once", e);
         }
            
        Assert.assertEquals(jobQueues.size(), _queueMap.size(), 
                            "Retrieved an unexpected number of queues.");
        System.out.println("Found " + jobQueues.size() + " queues.");
        
        // ----------------- Retrieve Iplant STAGING Queues
        // Count the number of STAGING queues returned above.
        int stagingQueueCount = 0;
        for (JobQueue jobQueue : jobQueues)
            if (jobQueue.getPhase() == JobPhaseType.STAGING)
                stagingQueueCount++;
        
        // Query all STAGING queues in _queueMap.
        List<JobQueue> stagingQueues = null;
        try {stagingQueues = dao.getQueues(JobPhaseType.STAGING, "iplantc.org");}
        catch (Exception e) 
         {
            Assert.fail("Unable to query iplant queues all at once", e);
         }
            
        Assert.assertEquals(stagingQueues.size(), stagingQueueCount, 
                            "Retrieved an unexpected number of STAGING queues.");
        System.out.println("Found " + stagingQueues.size() + " STAGING queues.");
    }
    
    /* ---------------------------------------------------------------------- */
    /* uuidExercise:                                                          */
    /* ---------------------------------------------------------------------- */
    /** Query the queues created in the depends-on method all at once. */
    @Test(enabled=true, dependsOnMethods={"createIplantQueues"})
    public void uuidExercise()
    {
        // ----------------- Retrieve a Queue by Name
        String queueName = MONITORING_QUEUENAME;
        JobQueue jobQueue1 = null;
        JobQueueDao dao = new JobQueueDao();
        try {jobQueue1 = dao.getQueueByName(queueName, "iplantc.org");}
         catch (Exception e) 
         {
            Assert.fail("Unable to query queue: " + queueName, e);
         }
        
        // ----------------- Retrieve a Queue by UUID
        JobQueue jobQueue2 = null;
        try {jobQueue2 = dao.getQueueByUUID(jobQueue1.getUuid(), "iplantc.org");}
        catch (Exception e) 
        {
           Assert.fail("Unable to query queue with UUID: " + jobQueue1.getUuid(), e);
        }
        Assert.assertEquals(jobQueue1.getUuid(), jobQueue2.getUuid(),
                     "Unexpected UUID returned");
        
        // ----------------- Delete the Queue by UUID
        int deleted = 0;
        try {deleted += dao.deleteQueueByUUID(jobQueue1.getUuid(), "iplantc.org");}
        catch (JobQueueException e) 
        {
            Assert.fail("Unable to delete queue with UUID: " + jobQueue1.getUuid(), e);
        }
        Assert.assertEquals(deleted, 1, "Expected to delete 1 queue by UUID.");
        
        // ----------------- Recreate the Queue
        createIplantQueue(MONITORING_QUEUENAME, _queueMap.get(MONITORING_QUEUENAME));
    }
    
    /* ---------------------------------------------------------------------- */
    /* createQueueNegative:                                                   */
    /* ---------------------------------------------------------------------- */
    /** Query the queues created in the depends-on method all at once. */
    @Test(enabled=true, dependsOnMethods={"createIplantQueues"})
    public void createQueueNegative()
    {
        // ----------------- Create a duplicate queue
        // Define a queue with an in-use name
        JobQueue jobQueue = new JobQueue();
        jobQueue.setName(MONITORING_QUEUENAME);
        jobQueue.setPhase(JobPhaseType.MONITORING);
        jobQueue.setTenantId("iplantc.org");
        jobQueue.setMaxMessages(12);
        jobQueue.setNumWorkers(33);
        jobQueue.setPriority(55);
        jobQueue.setFilter(null);
        
        // Try to create the queue.
        int rows = 0;
        JobQueueDao dao = new JobQueueDao();
        try {rows = dao.createQueue(jobQueue);}
         catch (Exception e) 
         {
             Assert.assertEquals((e instanceof JobQueueException), true,
                                 "1 - Unexpected exception thrown.");
         }
        Assert.assertEquals(rows, 0, "Duplicate queue name!");
        
        // ----------------- Create a queue with duplicate priority
        int usedPriority = _queueMap.get(MONITORING_QUEUENAME);
        jobQueue.setName(jobQueue.getName() + ".UNDEFINED");
        jobQueue.setPriority(usedPriority);
        try {rows = dao.createQueue(jobQueue);}
         catch (Exception e) 
         {
            Assert.assertEquals((e instanceof JobQueueException), true,
                                "2 - Unexpected exception thrown.");
         }
        Assert.assertEquals(rows, 0, "Duplicate priority!");
        
        // ----------------- Create a queue with invalid priority
        jobQueue.setPriority(0);
        try {rows = dao.createQueue(jobQueue);}
         catch (Exception e) 
         {
            Assert.assertEquals((e instanceof JobQueueException), true,
                                "3 - Unexpected exception thrown.");
         }
        Assert.assertEquals(rows, 0, "Invalid priority!");
        jobQueue.setPriority(200); // reset to something valid
        
        // ----------------- Create a queue with invalid num_workers
        jobQueue.setNumWorkers(-30);
        try {rows = dao.createQueue(jobQueue);}
         catch (Exception e) 
         {
            Assert.assertEquals((e instanceof JobQueueException), true,
                                "4 - Unexpected exception thrown.");
         }
        Assert.assertEquals(rows, 0, "Invalid number of workers!");
        jobQueue.setNumWorkers(7); // reset to something valid
        
        // ----------------- Create a queue with invalid max_messages
        jobQueue.setMaxMessages(0);
        try {rows = dao.createQueue(jobQueue);}
         catch (Exception e) 
         {
            Assert.assertEquals((e instanceof JobQueueException), true,
                                "5 - Unexpected exception thrown.");
         }
        Assert.assertEquals(rows, 0, "Invalid maximum messages!");
        jobQueue.setMaxMessages(31); // reset to something valid
    }
    
    /* ---------------------------------------------------------------------- */
    /* updateQueue:                                                           */
    /* ---------------------------------------------------------------------- */
    /** Query the queues created in the depends-on method all at once. */
    @Test(enabled=true, dependsOnMethods={"createIplantQueues"})
    public void updateQueue()
    {
        // ----------------- Retrieve a Queue by Name
        String queueName = MONITORING_QUEUENAME;
        JobQueue jobQueue = null;
        JobQueueDao dao = new JobQueueDao();
        try {jobQueue = dao.getQueueByName(queueName, "iplantc.org");}
         catch (Exception e) 
         {
            Assert.fail("Unable to query queue: " + queueName, e);
         }
        
        // Get original values
        int oldPriority = jobQueue.getPriority();
        int oldNumWorkers = jobQueue.getNumWorkers();
        int oldMaxMessages = jobQueue.getMaxMessages();
        JobQueue jobQueue2 = null;
        
        // ----------------- Update Priority
        int rows = 0;
        int newPriority = 3299;
        try {rows = dao.updatePriority(jobQueue.getName(), newPriority, "iplantc.org");}
        catch (JobQueueException e) {
            Assert.fail("Unable to update priority on " + queueName + " with value " + newPriority, e);
        }        
        Assert.assertEquals(rows, 1, "Priority not changed!");
        
        // Check that the update went through.
        try {jobQueue2 = dao.getQueueByName(queueName, "iplantc.org");}
        catch (Exception e) 
        {
           Assert.fail("Unable to query queue: " + queueName, e);
        }
        Assert.assertEquals(jobQueue2.getPriority(), newPriority, "Priority not updated!");
        
        // ----------------- Revert Priority
        rows = 0;
        try {rows = dao.updatePriority(jobQueue.getName(), oldPriority, "iplantc.org");}
        catch (JobQueueException e) {
            Assert.fail("Unable to revert priority on " + queueName + " with value " + oldPriority, e);
        }        
        Assert.assertEquals(rows, 1, "Priority not reverted!");
        
        // ----------------- Update Number of Workers
        rows = 0;
        int newNumWorkers = 56;
        try {rows = dao.updateNumWorkers(jobQueue.getName(), newNumWorkers, "iplantc.org");}
        catch (JobQueueException e) {
            Assert.fail("Unable to update num_workers on " + queueName + " with value " + newNumWorkers, e);
        }        
        Assert.assertEquals(rows, 1, "num_workers not changed!");
        
        // Check that the update went through.
        try {jobQueue2 = dao.getQueueByName(queueName, "iplantc.org");}
        catch (Exception e) 
        {
           Assert.fail("Unable to query queue: " + queueName, e);
        }
        Assert.assertEquals(jobQueue2.getNumWorkers(), newNumWorkers, "num_workers not updated!");
        
        // ----------------- Revert Number of Workers
        rows = 0;
        try {rows = dao.updateNumWorkers(jobQueue.getName(), oldNumWorkers, "iplantc.org");}
        catch (JobQueueException e) {
            Assert.fail("Unable to revert num_workers on " + queueName + " with value " + oldNumWorkers, e);
        }        
        Assert.assertEquals(rows, 1, "num_workers not reverted!");
        
        // ----------------- Update Maximum Messages
        rows = 0;
        int newMaxMessages = 90;
        try {rows = dao.updateMaxMessages(jobQueue.getName(), newMaxMessages, "iplantc.org");}
        catch (JobQueueException e) {
            Assert.fail("Unable to update max_messages on " + queueName + " with value " + newMaxMessages, e);
        }        
        Assert.assertEquals(rows, 1, "max_messages not changed!");
        
        // Check that the update went through.
        try {jobQueue2 = dao.getQueueByName(queueName, "iplantc.org");}
        catch (Exception e) 
        {
           Assert.fail("Unable to query queue: " + queueName, e);
        }
        Assert.assertEquals(jobQueue2.getMaxMessages(), newMaxMessages, "max_messages not updated!");
        
        // ----------------- Revert Maximum Messages
        rows = 0;
        try {rows = dao.updateMaxMessages(jobQueue.getName(), oldMaxMessages, "iplantc.org");}
        catch (JobQueueException e) {
            Assert.fail("Unable to revert max_messages on " + queueName + " with value " + oldMaxMessages, e);
        }        
        Assert.assertEquals(rows, 1, "max_messages not reverted!");
    }
    
    /* ---------------------------------------------------------------------- */
    /* updateQueueNegative:                                                   */
    /* ---------------------------------------------------------------------- */
    /** Query the queues created in the depends-on method all at once. */
    @Test(enabled=true, dependsOnMethods={"createIplantQueues"})
    public void updateQueueNegative()
    {
        // ----------------- Retrieve a Queue by Name
        String queueName = MONITORING_QUEUENAME;
        JobQueue jobQueue = null;
        JobQueueDao dao = new JobQueueDao();
        try {jobQueue = dao.getQueueByName(queueName, "iplantc.org");}
         catch (Exception e) 
         {
            Assert.fail("Unable to query queue: " + queueName, e);
         }
        
        // ----------------- Update Priority with Duplicate
        int rows = 0;
        int newPriority = 1;
        try {rows = dao.updatePriority(jobQueue.getName(), newPriority, "iplantc.org");}
        catch (Exception e) {
            Assert.assertEquals((e instanceof JobQueuePriorityException), true,
                    "1 - Unexpected exception thrown.");
        }        
        Assert.assertEquals(rows, 0, "Priority changed!");
        
        // ----------------- Update Priority
        rows = 0;
        newPriority = -1;
        try {rows = dao.updatePriority(jobQueue.getName(), newPriority, "iplantc.org");}
        catch (Exception e) {
            Assert.assertEquals((e instanceof JobQueueException), true,
                    "2 - Unexpected exception thrown.");
        }        
        Assert.assertEquals(rows, 0, "Priority changed!");
        
        // ----------------- Update Number of Workers
        rows = 0;
        int newNumWorkers = 0;
        try {rows = dao.updateNumWorkers(jobQueue.getName(), newNumWorkers, "iplantc.org");}
        catch (Exception e) {
            Assert.assertEquals((e instanceof JobQueueException), true,
                    "3 - Unexpected exception thrown.");
        }        
        Assert.assertEquals(rows, 0, "num_workers changed!");
        
        // ----------------- Update Maximum Messages
        rows = 0;
        int newMaxMessages = -5;
        try {rows = dao.updateMaxMessages(jobQueue.getName(), newMaxMessages, "iplantc.org");}
        catch (Exception e) {
            Assert.assertEquals((e instanceof JobQueueException), true,
                    "4 - Unexpected exception thrown.");
        }        
        Assert.assertEquals(rows, 0, "max_messages changed!");
    }

    /* ********************************************************************** */
    /*                            Private Methods                             */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* createIplantQueue:                                                     */
    /* ---------------------------------------------------------------------- */
    private void createIplantQueue(String queueName, int priority)
    {
        // Take the first dot separated component in 
        // the queueName to be the designated phase.
        String[] components = queueName.split("[.]");
        
        // Create the queue.
        createQueue(queueName, JobPhaseType.valueOf(components[0]), 
                    "iplantc.org", 2, 3, priority, null);
    }
    
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
