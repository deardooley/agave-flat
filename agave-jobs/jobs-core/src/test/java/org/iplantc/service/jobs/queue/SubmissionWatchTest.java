package org.iplantc.service.jobs.queue;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.apps.dao.SoftwareDao;
import org.iplantc.service.apps.model.Software;
import org.iplantc.service.jobs.dao.JobDao;
import org.iplantc.service.jobs.exceptions.JobException;
import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.jobs.model.enumerations.JobStatusType;
import org.iplantc.service.jobs.submission.AbstractJobSubmissionTest;
import org.iplantc.service.systems.model.BatchQueue;
import org.iplantc.service.systems.model.ExecutionSystem;
import org.iplantc.service.systems.model.StorageSystem;
import org.iplantc.service.systems.model.enumerations.ExecutionType;
import org.iplantc.service.systems.model.enumerations.LoginProtocolType;
import org.iplantc.service.systems.model.enumerations.RemoteSystemType;
import org.iplantc.service.systems.model.enumerations.SchedulerType;
import org.iplantc.service.systems.model.enumerations.SystemStatusType;
import org.joda.time.DateTime;
import org.json.JSONException;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobListener;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.KeyMatcher;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

@Test(groups={"broken"})
public class SubmissionWatchTest extends AbstractJobSubmissionTest 
{
    private static final Logger log = Logger.getLogger(StagingWatch.class);
    
//	protected static String LOCAL_TXT_FILE = "target/test-classes/transfer/test_upload.bin";
	
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
	
	@Override
	public void initSystems() throws Exception
	{
		for (String protocol: new String[]{"sftp"}) {//, "gridftp", "irods", "ftp", "s3"}) {
			
			StorageSystem storageSystem = null;
			
			try 
			{
			    storageSystem = (StorageSystem)getNewInstanceOfRemoteSystem(RemoteSystemType.STORAGE, protocol);
				if (protocol.equals("sftp")) {
					storageSystem.setGlobalDefault(true);
					storageSystem.setPubliclyAvailable(true);
				}
				systemDao.persist(storageSystem);
			} 
			catch (Exception e) {
				Assert.fail("Unable to create " + protocol + 
						" storage system prior to test ", e);
			}
		}
		
		for (String protocol: new String[]{"ssh"})//, "gsissh"})
		{
			ExecutionSystem executionSystem = null;
//			for (int i=0;i<10;i++) {
    		    try {
    		        executionSystem = (ExecutionSystem) getNewInstanceOfRemoteSystem(RemoteSystemType.EXECUTION, protocol);
    		        executionSystem.setOwner(SYSTEM_OWNER);
        	        executionSystem.getBatchQueues().clear();
        	        BatchQueue q = unlimitedQueue.clone();
        	        q.setSystemDefault(true);
        	        executionSystem.addBatchQueue(q);
        	        executionSystem.addBatchQueue(longQueue.clone());
        	        executionSystem.addBatchQueue(mediumQueue.clone());
        	        executionSystem.addBatchQueue(shortQueue.clone());
        	        executionSystem.setExecutionType(ExecutionType.CLI);
        	        executionSystem.setScheduler(SchedulerType.FORK);
        	        executionSystem.setPubliclyAvailable(true);
        	        executionSystem.setType(RemoteSystemType.EXECUTION);
        			systemDao.persist(executionSystem);
    		    }
    		    catch (Exception e) {
                    Assert.fail("Unable to create " + protocol + 
                            " execution system prior to test ", e);
                }
//			}
		}
	}
	
	
	/**
     * Generic submission test used by all the methods testing job submission is some
     * form or fashion.
     * 
     * @param job
     * @param message
     * @param shouldThrowException
     */
	@Override
    protected Job genericRemoteSubmissionTestCase(Job job, JobStatusType expectedStatus, String message, boolean shouldThrowException) 
    {
        boolean actuallyThrewException = false;
        String exceptionMsg = message;
        
        SubmissionWatch submissionWatch = new SubmissionWatch();
        
        try
        {
//          job.setArchivePath(job.getOwner() + "/archive/jobs/job-" + job.getUuid() + "-" + Slug.toSlug(job.getName()));
//          
//          JobDao.persist(job);
//          
            submissionWatch.setJob(job);
            
            submissionWatch.doExecute();
            
            job = submissionWatch.getJob();
            
            Assert.assertEquals(job.getStatus(), expectedStatus,
                    "Job status did not match " + expectedStatus + " after submission");
            
            if (expectedStatus == JobStatusType.RUNNING || 
                    expectedStatus == JobStatusType.QUEUED ) {
                Assert.assertNotNull(job.getLocalJobId(),
                        "Local job id was not obtained during submission");
                Assert.assertTrue(job.getRetries() == 0,
                        "Job should only submit once when it does not fail during submission");
                Assert.assertNotNull(job.getSubmitTime(),
                    "Job submit time was not updated during job submission");
            } else {
                Assert.assertNull(job.getLocalJobId(),
                        "Local job id should be null if submission fails");
                Assert.assertNull(job.getSubmitTime(),
                    "Job submit time should not be updated if the job did not submit.");
            }
        }
        catch (Exception e)
        {
            actuallyThrewException = true;
            exceptionMsg = "Error placing job into queue on " + job.getSystem() + ": " + message;
            if (actuallyThrewException != shouldThrowException) e.printStackTrace();
            job = submissionWatch.getJob();
        }
        finally {
            
        }
        
        System.out.println(" exception thrown?  expected " + shouldThrowException + " actual " + actuallyThrewException);
        Assert.assertTrue(actuallyThrewException == shouldThrowException, exceptionMsg);
        
        return job;
        
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
    @Test(dataProvider = "submitProvider", enabled=false)
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
    @Test(enabled=false)//, dependsOnMethods = { "submit" })
    public void submitJobStaysStagedWhenExecutionSystemIsUnavailable() 
    throws Exception
    {
        Software software = null;
        ExecutionSystem executionSystem = null;
        
        try
        {
            executionSystem = getNewInstanceOfExecutionSystem("submitJobStaysStagedWhenExecutionSystemIsUnavailable");
            systemDao.persist(executionSystem);
            
            software = SoftwareDao.getUserApps(SYSTEM_OWNER, true).get(0).clone();
            software.setName("submitJobStaysStagedWhenExecutionSystemIsUnavailable");
            software.setExecutionSystem(executionSystem);
            software.setDefaultQueue(executionSystem.getDefaultQueue().getName());
            SoftwareDao.persist(software);
            
            
            for (SystemStatusType systemStatus: SystemStatusType.values()) {
                
                if (systemStatus == SystemStatusType.UP) continue; 
                executionSystem = (ExecutionSystem)systemDao.findById(executionSystem.getId());
                executionSystem.setStatus(systemStatus);
                
                systemDao.merge(executionSystem);
                Job job = null;
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
                    try { JobDao.delete(job); } catch (Exception e) {e.printStackTrace();}
                    
//                    try { JobDao.delete(job); } catch (Exception e) {e.printStackTrace();}
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
    @Test(enabled=false)//, dependsOnMethods = { "submitJobStaysStagedWhenExecutionSystemIsUnavailable" })
    public void submitJobStaysStagedWhenSoftwareDeploymentSystemIsUnavailable() 
    throws Exception
    {
        
        Software software = null;
        StorageSystem storageSystem = null;
        Job job = null;
        try
        {
            storageSystem = getNewInstanceOfStorageSystem("submitJobStaysStagedWhenSoftwareDeploymentSystemIsUnavailable");
            storageSystem.setSystemId("submitJobStaysStagedWhenSoftwareDeploymentSystemIsUnavailable");
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
                    try { JobDao.delete(job); } catch (Exception e) {e.printStackTrace();}
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
    @Test(enabled=false)//, dependsOnMethods = { "submitJobStaysStagedWhenSoftwareDeploymentSystemIsUnavailable" })
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
	
    @DataProvider(name="concurrentQueueTerminationTestProvider")
    public Object[][] concurrentQueueTerminationTestProvider() throws Exception
    {
        List<Software> testApps = SoftwareDao.getAll();
        List<Object[]> testCases = new ArrayList<Object[]>();
        
        for (Software software: testApps) 
        {
            if (software.getDefaultQueue().equals(unlimitedQueue.getName())) {
                testCases.add(new Object[] { software, "Failed to submit job to " + software.getExecutionSystem().getSystemId() });
                break;
            }
        }
        
        return testCases.toArray(new Object[][] {});
    }
    
	@Test (groups={"staging"}, dataProvider="concurrentQueueTerminationTestProvider", enabled=false)
    public void concurrentQueueTerminationTest(Software software, String message) 
    throws Exception 
    {
	    clearJobs();
        Scheduler sched = StdSchedulerFactory.getDefaultScheduler();
        
        JobDetail jobDetail = newJob(SubmissionWatch.class)
                .withIdentity("primary", "Submission")
                .requestRecovery(true)
                .storeDurably()
                .build();
        
        sched.addJob(jobDetail, true);
        
        // start a block of worker processes to pull pre-staged file references
        // from the db and apply the appropriate transforms to them.
        for (int i = 0; i < 15; i++)
        {
            
            Trigger trigger = newTrigger()
                    .withIdentity("trigger"+i, "Submission")
                    .startAt(new DateTime().plusSeconds(i).toDate())
                    .withSchedule(simpleSchedule()
                            .withMisfireHandlingInstructionIgnoreMisfires()
                            .withIntervalInSeconds(2)
                            .repeatForever())
                    .forJob(jobDetail)
                    .withPriority(5)
                    .build();
            
            sched.scheduleJob(trigger);
        }
        
        final AtomicInteger jobsComplete = new AtomicInteger(0);
        sched.getListenerManager().addJobListener(
                new JobListener() {

                    @Override
                    public String getName() {
                        return "Unit Test Listener";
                    }

                    @Override
                    public void jobToBeExecuted(JobExecutionContext context) {
                        log.debug("working on a new job");                        
                    }

                    @Override
                    public void jobExecutionVetoed(JobExecutionContext context) {
                        // no idea here
                    }

                    @Override
                    public void jobWasExecuted(JobExecutionContext context, JobExecutionException e) {
                        if (e == null) {
                            log.error(jobsComplete.addAndGet(1) + "/100 Completed jobs ",e);;
                        } else {
//                            log.error("Transfer failed",e);
                        }
                    }
                    
                }, KeyMatcher.keyEquals(jobDetail.getKey())
            );
        
        
        try 
        {
            for (int i=0;i<100;i++) {
                Job testJob = createJob(JobStatusType.STAGED, software, SYSTEM_OWNER);
                JobDao.persist(testJob);
            }
            
            sched.start();
            
            log.debug("Sleeping to allow scheduler to run for a bit...");
            try { Thread.sleep(3000); } catch (Exception e) {}
            
            log.debug("Resuming test run and pausing all staging triggers...");
            sched.pauseAll();
            log.debug("All triggers stopped. Interrupting executing jobs...");
            
            for (JobExecutionContext context: sched.getCurrentlyExecutingJobs()) {
                log.debug("Interrupting job " + context.getJobDetail().getKey() + "...");
                sched.interrupt(context.getJobDetail().getKey());
                log.debug("Interrupt of job " + context.getJobDetail().getKey() + " complete.");
            }
            log.debug("Shutting down scheduler...");
            sched.shutdown(true);
            log.debug("Scheduler shut down...");
            
            for (Job job: JobDao.getAll())
            {
                Assert.assertTrue(job.getStatus() == JobStatusType.STAGED || job.getStatus() == JobStatusType.PENDING, 
                        "Job status was not rolled back upon interrupt.");
            }
        } 
        catch (Exception e) {
            Assert.fail("Failed to stage job data due to unexpected error", e);
        }
        
    }
	
	@Test (groups={"submission"}, dataProvider="concurrentQueueTerminationTestProvider", enabled=true)
    public void concurrentQueueThroughputTest(Software software, String message) 
    throws Exception 
    {
        clearJobs();
        Scheduler sched = StdSchedulerFactory.getDefaultScheduler();
        
        JobDetail jobDetail = newJob(SubmissionWatch.class)
                .withIdentity("primary", "Submission")
                .requestRecovery(true)
                .storeDurably()
                .build();
        
        sched.addJob(jobDetail, true);
        
        // start a block of worker processes to pull pre-staged file references
        // from the db and apply the appropriate transforms to them.
        for (int i = 0; i < 15; i++)
        {
            
            Trigger trigger = newTrigger()
                    .withIdentity("trigger"+i, "Submission")
                    .startAt(new DateTime().plusSeconds(i).toDate())
                    .withSchedule(simpleSchedule()
                            .withMisfireHandlingInstructionIgnoreMisfires()
                            .withIntervalInSeconds(2)
                            .repeatForever())
                    .forJob(jobDetail)
                    .withPriority(5)
                    .build();
            
            sched.scheduleJob(trigger);
        }
        
        final AtomicInteger jobsComplete = new AtomicInteger(0);
        sched.getListenerManager().addJobListener(
                new JobListener() {

                    @Override
                    public String getName() {
                        return "Unit Test Listener";
                    }

                    @Override
                    public void jobToBeExecuted(JobExecutionContext context) {
                        log.debug("working on a new job " + context.getMergedJobDataMap().getString("uuid"));                        
                    }

                    @Override
                    public void jobExecutionVetoed(JobExecutionContext context) {
                        // no idea here
                    }

                    @Override
                    public void jobWasExecuted(JobExecutionContext context, JobExecutionException e) {
                        if (e == null) {
                            log.error("Completed job " + context.getMergedJobDataMap().getString("uuid") + 
                            		". " + jobsComplete.addAndGet(1) + "/100 Completed jobs ",e);;
                        } else {
                            log.error("Transfer failed",e);
                        }
                    }
                    
                }, KeyMatcher.keyEquals(jobDetail.getKey())
            );
        
        
        try 
        {
            int totalJobs = 100;
            for (int i=0;i<totalJobs;i++) {
                Job testJob = createJob(JobStatusType.STAGED, software, SYSTEM_OWNER);
                JobDao.persist(testJob);
            }
            
            log.debug("Starting scheduler and letting it rip...");
            sched.start();
            
            while (jobsComplete.get() < 1.2*totalJobs);
            
            log.debug("Shutting down scheduler...");
            sched.shutdown(true);
            log.debug("Scheduler shut down...");
            
            for (Job job: JobDao.getAll())
            {
                Assert.assertTrue(job.getStatus() == JobStatusType.QUEUED 
                        || job.getStatus() == JobStatusType.RUNNING
                        || job.getStatus() == JobStatusType.FAILED,
                        "Job status was not running or failed. Not all jobs were processed");
            }
        } 
        catch (Exception e) {
            Assert.fail("Failed to stage job data due to unexpected error", e);
        }
        
    }
//	
//	@Test(enabled=true)
//	public void testExistingJob() {
//		
//		Job job = null;
//		
//		
//		try {
//			job = JobDao.getByUuid("0001435752101463-e0bd34dffff8de6-0001-007");
//			StagingWatch watch = new StagingWatch();
//			Map<String, String[]> jobInputMap = JobManager.getJobInputMap(job);
//			watch.setJob(job);
//			watch.stageJobData(jobInputMap);
//		} catch (JobException | JobDependencyException
//				| SystemUnavailableException | QuotaViolationException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		
//	}
}
