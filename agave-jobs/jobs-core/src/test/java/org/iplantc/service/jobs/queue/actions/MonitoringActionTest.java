/**
 * 
 */
package org.iplantc.service.jobs.queue.actions;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.iplantc.service.apps.dao.SoftwareDao;
import org.iplantc.service.apps.model.Software;
import org.iplantc.service.jobs.dao.JobDao;
import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.jobs.model.enumerations.JobStatusType;
import org.iplantc.service.jobs.phases.workers.IPhaseWorker;
import org.iplantc.service.jobs.submission.AbstractJobSubmissionTest;
import org.iplantc.service.systems.manager.SystemManager;
import org.iplantc.service.systems.model.ExecutionSystem;
import org.iplantc.service.systems.model.enumerations.SystemStatusType;
import org.json.JSONObject;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * @author dooley
 *
 */
public class MonitoringActionTest extends AbstractJobSubmissionTest {
    
    
    @BeforeClass
    @Override
    public void beforeClass() throws Exception {
        
        super.beforeClass();
    }
    
    @AfterClass
    @Override
    public void afterClass() throws Exception {
        
        super.afterClass();
    }
    @Test
    protected void garbage() {
        System.out.println(URI.create("/Users/dooley").toString());
        System.out.println(URI.create("//Users/dooley").toString());
        System.out.println(URI.create("/Users//dooley/").toString());
        System.out.println(URI.create("/Users/dooley//").toString());
        System.out.println(URI.create("Users/dooley").toString());
        System.out.println(URI.create("Users/dooley/").toString());
        System.out.println(URI.create("Users//dooley/").toString());
        System.out.println(URI.create("Users/dooley//").toString());
        
    }
    
    /**
     * Generic submission test used by all the methods testing job submission is some
     * form or fashion.
     * 
     * @param job
     * @param message
     * @param shouldThrowException
     */
    protected Job genericRemoteMonitoringTestCase(Job job, JobStatusType expectedStatus, String message, boolean shouldThrowException) 
    {
        IPhaseWorker worker = Mockito.mock(IPhaseWorker.class);
        MonitoringAction action = new MonitoringAction(job, worker);
        
        try
        {
            long lastUpdated = job.getLastUpdated().getTime();
            int statusChecks = job.getStatusChecks();
            
            action.setJob(job);
            
            action.run();
            
            job = action.getJob();
            
            Assert.assertEquals(job.getStatus(), expectedStatus,
                    "Job status did not match " + expectedStatus + " after submission");
            
            Assert.assertNotNull(action.getJob().getLocalJobId(),
                    "Local job id was not obtained during submission");
            
            Assert.assertFalse(job.getStatusChecks() == statusChecks,
                    "Status check count was not updated.");
            
            Assert.assertTrue(job.getLastUpdated().getTime() > lastUpdated,
                    "Timestamp was not updated.");
            
            if (expectedStatus == JobStatusType.RUNNING) 
            {
                Assert.assertNotNull(job.getStartTime(),
                    "Job submit time should be set during job submission");
                
                Assert.assertNull(job.getEndTime(),
                        "Job end time should not be set on queued jobs");
            }
            else if (expectedStatus == JobStatusType.QUEUED) 
            {
                Assert.assertNull(job.getStartTime(),
                        "Job submit time was should not be set for a queued job");
                
                Assert.assertNull(job.getEndTime(),
                        "Job end time was should not be set for a queued job");
            }
            else if (expectedStatus == JobStatusType.FAILED) 
            {
                Assert.assertNotNull(job.getStartTime(),
                        "Job submit time was not updated during job submission");
                
                Assert.assertNotNull(job.getEndTime(),
                        "Job end time was not set on failed job");
            }
            else if (expectedStatus == JobStatusType.CLEANING_UP) 
            {
                Assert.assertNotNull(job.getEndTime(),
                        "Job end time was not set during by monitoring process");
                
                Assert.assertNotNull(job.getStartTime(),
                        "Job start time was not set during job submission");
            }
            else 
            {
                Assert.assertNull(job.getLocalJobId(),
                        "Local job id should be null if submission fails");
                Assert.assertNull(job.getSubmitTime(),
                    "Job submit time should not be updated if the job did not submit.");
            }
        }
        catch (Exception e)
        {
            if (shouldThrowException) {
                Assert.assertEquals(action.getJob().getStatus(), expectedStatus, message);
            } else {
                Assert.fail("Unexpected exception thrown checking remote job status", e);
            }
            
        }
        
        return job;
    }
    
    @DataProvider
    public Object[][] monitorProvider() throws Exception
    {
        // for each execution type
        //   for each scheduler
        //      for each login protocol
        //          create software for that system
        //          create job for that system
        //      stage data to that system's work folder
        //      pass in job 
        
        // this should cover all happy path checks resulting in status change events.
        Job testJob = createJob(JobStatusType.STAGED, SYSTEM_OWNER);
        testJob.setArchiveOutput(false);
        

        List<Object[]> testCases = new ArrayList<Object[]>();
        
        for (Software software: SoftwareDao.getAll()) {
            testCases.add(new Object[]{ createJob(JobStatusType.QUEUED, software, SYSTEM_OWNER), false, "Job submission should succeed" });
            testCases.add(new Object[]{ createJob(JobStatusType.RUNNING, software, SYSTEM_OWNER), false, "Job submission should succeed" });
            break;
        }
        
        return testCases.toArray(new Object[][] {});
                
    }
    

    /**
     * Test job submission using all combinations of protocols and schedulers
     * 
     * @param job
     * @param shouldThrowException
     * @param message
     */
    @Test(dataProvider = "monitorProvider", enabled=true)
    public void monitor(Job job, boolean shouldThrowException, String message) 
    throws Exception
    {
        try {
            JobDao.create(job);
            genericRemoteMonitoringTestCase(job, JobStatusType.CLEANING_UP, message, shouldThrowException);
        }
        finally {
            try { JobDao.delete(job); } catch (Exception e) {}
        }
    }
    

    /**
     * Tests that submission fails and the job requeues when the execution 
     * system is not available.
     * 
     * @throws Exception
     */
    @Test(enabled=true)//, dependsOnMethods = { "submit" })
    public void monitorJobHoldsStatusWhenExecutionSystemIsUnavailable() 
    throws Exception
    {
        Software software = null;
        ExecutionSystem executionSystem = null;
        Job job = null;
        try
        {
            executionSystem = getNewInstanceOfExecutionSystem("submitTestApplicationExecutionSystemChecks");
            systemDao.persist(executionSystem);
            
            software = SoftwareDao.getUserApps(SYSTEM_OWNER, true).get(0).clone();
            software.setName("submitTestApplicationStorageSystemChecks");
            software.setExecutionSystem(executionSystem);
            software.setDefaultQueue(executionSystem.getDefaultQueue().getName());
            SoftwareDao.persist(software);
            
            
            for (SystemStatusType systemStatus: SystemStatusType.values()) {
                
                if (systemStatus == SystemStatusType.UP) continue; 
                
                executionSystem = (ExecutionSystem)systemDao.findById(executionSystem.getId());
                executionSystem.setStatus(systemStatus);
                systemDao.merge(executionSystem);
            
                for (JobStatusType testStatus: new JobStatusType[] {JobStatusType.RUNNING, JobStatusType.QUEUED}) 
                {
                    try 
                    {
                        job = createJob(testStatus, software, SYSTEM_OWNER);
                        
                        JobDao.create(job);
                        
                        job = genericRemoteMonitoringTestCase(job, testStatus, 
                                "Monitored job should stay in a " + testStatus + " state while the execution system is " 
                                        + systemStatus.name(), true);
                    }
                    finally {
                        try { JobDao.delete(job); } catch (Exception e) {}
                    }
                }
            }
        }
        finally {
            try { SoftwareDao.delete(software); } catch (Exception e) {}
            try { systemDao.remove(executionSystem); } catch (Exception e) {}
        }
    }
    
    /**
     * Tests that submission fails and the job requeues when the app deployment 
     * system is not available.
     * 
     * @throws Exception
     */
    @Test(enabled=true)//, dependsOnMethods = { "submitJobStaysStagedWhenExecutionSystemIsUnavailable" })
    public void submitJobStaysStagedWhenExectuionSystemIsOffline() 
    throws Exception
    {
        
        Software software = null;
        ExecutionSystem executionSystem = null;
        Job job = null;
        try
        {
            SystemManager systemManager = new SystemManager();
            JSONObject json = jtd.getTestDataObject(EXECUTION_SYSTEM_TEMPLATE_DIR + 
                        File.separator + "ssh.example.com.json");
            
            json.put("id", "submitJobStaysStagedWhenExectuionSystemIsOffline");
            json.getJSONObject("login").put("host", "thiscannotbeavalidaddress.seriously.itsnot");
            
            executionSystem = (ExecutionSystem)systemManager.parseSystem(json, SYSTEM_OWNER, null);
            executionSystem.getBatchQueues().clear();
            executionSystem.addBatchQueue(unlimitedQueue.clone());
            systemDao.persist(executionSystem);
            
            software = SoftwareDao.getUserApps(SYSTEM_OWNER, true).get(0).clone();
            software.setName("submitTestApplicationStorageSystemChecks");
            software.setExecutionSystem(executionSystem);
            software.setDefaultQueue(executionSystem.getDefaultQueue().getName());
            SoftwareDao.persist(software);
            
            for (JobStatusType testStatus: new JobStatusType[] {JobStatusType.RUNNING, JobStatusType.QUEUED}) 
            {
                try 
                {
                    job = createJob(testStatus, software, SYSTEM_OWNER);
                    
                    JobDao.create(job);
                    
                    job = genericRemoteMonitoringTestCase(job, testStatus, 
                            "Job submission should stay in a " + testStatus 
                            + " state while the execution system is offline", true);
                }
                finally {
                    try { deleteJobWorkDirectory(job); } catch (Exception e) {}
                    try { JobDao.delete(job); } catch (Exception e) {}
                }
            }
        }
        finally {
            try { SoftwareDao.delete(software); } catch (Exception e) {}
            try { systemDao.remove(executionSystem); } catch (Exception e) {}
        }
    }
}
