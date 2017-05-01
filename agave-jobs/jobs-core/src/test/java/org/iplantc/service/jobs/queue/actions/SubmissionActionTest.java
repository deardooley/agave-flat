/**
 * 
 */
package org.iplantc.service.jobs.queue.actions;

import org.iplantc.service.apps.dao.SoftwareDao;
import org.iplantc.service.apps.model.Software;
import org.iplantc.service.jobs.dao.JobDao;
import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.jobs.model.enumerations.JobStatusType;
import org.iplantc.service.jobs.submission.AbstractJobSubmissionTest;
import org.iplantc.service.systems.model.ExecutionSystem;
import org.iplantc.service.systems.model.StorageSystem;
import org.iplantc.service.systems.model.enumerations.SystemStatusType;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * @author dooley
 *
 */
@Test(groups={"integration"})
public class SubmissionActionTest extends AbstractJobSubmissionTest {
    
    
    @BeforeClass
    @Override
    public void beforeClass() throws Exception {
        
        super.beforeClass();
        
        for (Software software: SoftwareDao.getAll()) {
            stageSofwareAssets(software);
            stageSoftwareInputDefaultData(software);
        }
    }
    
    @AfterClass
    @Override
    public void afterClass() throws Exception {
        
        super.afterClass();
        
        for (Software software: SoftwareDao.getAll()) {
            deleteSoftwareAssets(software);
        }
    }
    
    
    @DataProvider
    public Object[][] submitProvider() throws Exception
    {
        // for each execution system
        //      create software for that system
        //      create job for that system
        //      stage data to that system's work folder
        //      pass in job 
        
        // this should cover all combinations of protocols and schedulers
        Job testJob = createJob(JobStatusType.STAGED, SYSTEM_OWNER);
        testJob.setArchiveOutput(false);
        
        return new Object[][] {
                { testJob, false, "Job submission should succeed" } 
        };
    }
    

    /**
     * Test job submission using all combinations of protocols and schedulers
     * 
     * @param job
     * @param shouldThrowException
     * @param message
     */
    @Test(dataProvider = "submitProvider", enabled=true)
    public void submit(Job job, boolean shouldThrowException, String message) 
    throws Exception
    {
        try {
            JobDao.persist(job);
            stageJobInputs(job);
            genericRemoteSubmissionTestCase(job, JobStatusType.QUEUED, message, shouldThrowException);
        }
        finally {
            try { 
                JobDao.refresh(job); 
                JobDao.delete(job); 
            } catch (Exception e) {}
        }
    }
    

    /**
     * Tests that submission fails and the job requeues when the execution 
     * system is not available.
     * 
     * @throws Exception
     */
    @Test(enabled=true)//, dependsOnMethods = { "submit" })
    public void submitJobStaysStagedWhenExecutionSystemIsUnavailable() 
    throws Exception
    {
        Software software = null;
        ExecutionSystem executionSystem = null;
        Job job = null;
        try
        {
            executionSystem = getNewInstanceOfExecutionSystem("submitTestApplicationExecutionSystemChecks");
            executionSystem.setSystemId("submitTestApplicationExecutionSystemChecks");
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
            
                try 
                {
                    job = createJob(JobStatusType.STAGED, software, SYSTEM_OWNER);
                    
                    JobDao.persist(job);
                    
                    job = genericRemoteSubmissionTestCase(job, JobStatusType.STAGED, 
                            "Job submission should stay in a STAGED state while the execution system is " 
                                    + systemStatus.name(), false);
                    Assert.assertEquals(job.getStatus(), JobStatusType.STAGED, 
                            "Job should remain STAGED until the execution system comes " 
                                    + systemStatus.name() + " again.");
                } catch (Throwable e) {
                    Assert.fail("Job submission should stay in a STAGED state when the execution system is " 
                            + systemStatus.name(), e);
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
    
    /**
     * Tests that submission fails and the job requeues when the app deployment 
     * system is not available.
     * 
     * @throws Exception
     */
    @Test(enabled=true)//, dependsOnMethods = { "submitJobStaysStagedWhenExecutionSystemIsUnavailable" })
    public void submitJobStaysStagedWhenSoftwareDeploymentSystemIsUnavailable() 
    throws Exception
    {
        
        Software software = null;
        StorageSystem storageSystem = null;
        Job job = null;
        try
        {
            storageSystem = getNewInstanceOfStorageSystem("submitTestApplicationStorageSystemChecks");
            storageSystem.setSystemId("submitTestApplicationStorageSystemChecks");
            systemDao.persist(storageSystem);
            
            software = SoftwareDao.getUserApps(SYSTEM_OWNER, true).get(0).clone();
            software.setName("submitTestApplicationStorageSystemChecks");
            software.setStorageSystem(storageSystem);
            SoftwareDao.persist(software);
            
            for (SystemStatusType systemStatus: SystemStatusType.values()) {
                
                if (systemStatus == SystemStatusType.UP) continue; 
                storageSystem.setStatus(systemStatus);
                systemDao.merge(storageSystem);
            
                try 
                {
                    job = createJob(JobStatusType.STAGED, software, SYSTEM_OWNER);
                    
                    JobDao.persist(job);
                    
                    job = genericRemoteSubmissionTestCase(job, JobStatusType.STAGED, 
                            "Job submission should stay in a STAGED state while the software deployment system is " 
                                    + systemStatus.name(), false);
                    Assert.assertEquals(job.getStatus(), JobStatusType.STAGED, 
                            "Job should remain STAGED until the software deployment system comes " 
                                    + systemStatus.name() + " again.");
                } catch (Throwable e) {
                    Assert.fail("Job submission should stay in a STAGED state when the software storage system is down", e);
                }
                finally {
                    try { deleteJobWorkDirectory(job); } catch (Exception e) {}
                    try { JobDao.delete(job); } catch (Exception e) {}
                }
            }
        }
        finally {
            try { SoftwareDao.delete(software); } catch (Exception e) {}
            try { systemDao.remove(storageSystem); } catch (Exception e) {}
        }
    }
    
    /**
     * Tests that submission fails and the job requeues when the app is not available.
     */
    @Test(enabled=true)//, dependsOnMethods = { "submitJobStaysStagedWhenSoftwareDeploymentSystemIsUnavailable" })
    public void submitJobStaysStagedWhenSoftwareIsUnavailable() 
    {
        Job job = null;
        Software software = null;
        try {
            software = SoftwareDao.getUserApps(SYSTEM_OWNER, true).get(0).clone();
            software.setName("submitTestApplicationChecks");
            software.setAvailable(false);
            SoftwareDao.persist(software);
        
            job = createJob(JobStatusType.STAGED, software, SYSTEM_OWNER);
            
            JobDao.persist(job);
            
            job = genericRemoteSubmissionTestCase(job, JobStatusType.STAGED, 
                    "Job submission should fail when the application is not available", 
                    false);
            
            JobDao.refresh(job);
            Assert.assertEquals(job.getStatus(), JobStatusType.STAGED,
                    "Job should be returned to STAGED status when the app is unavailable.");
        } 
        catch (Throwable e) {
            Assert.fail("Job should be returned to STAGED status when the app is unavailable.", e);
        }
        finally {
            try { SoftwareDao.delete(software); } catch (Exception e) {}
            try { deleteJobWorkDirectory(job); } catch (Exception e) {}
            try { JobDao.delete(job); } catch (Exception e) {}
        }
    }

}
