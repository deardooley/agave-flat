package org.iplantc.service.jobs.queue;

import static org.iplantc.service.jobs.model.enumerations.JobStatusType.CLEANING_UP;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

import java.io.File;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.apps.dao.SoftwareDao;
import org.iplantc.service.apps.model.Software;
import org.iplantc.service.jobs.dao.JobDao;
import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.jobs.model.enumerations.JobStatusType;
import org.iplantc.service.jobs.submission.AbstractJobSubmissionTest;
import org.iplantc.service.transfer.RemoteDataClient;
import org.joda.time.DateTime;
import org.json.JSONObject;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobListener;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.KeyMatcher;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Tests end to end integration of a job submission by manually pushing
 * through each stage of each queue.
 */
@Test(groups={"broken"})
public class ArchiveWatchTest extends AbstractJobSubmissionTest
{
    private static final Logger log = Logger.getLogger(ArchiveWatchTest.class);

	
	@BeforeMethod
	private void beforeMethod() throws Exception {
		// create job work directory on local system and put input file there
		clearJobs();
	}
	
	@AfterMethod
	private void afterMethod() throws Exception {
		// create job work directory on local system and put input file there
//		clearJobs();
	}
	
	protected void initSoftware() throws Exception {
		File softwareDir = new File(SOFTWARE_SYSTEM_TEMPLATE_DIR, "/system-software.json");
		JSONObject json = jtd.getTestDataObject(softwareDir.getPath());
		Software software = Software.fromJSON(json, SYSTEM_OWNER);
		software.setDefaultQueue(executionSystem.getDefaultQueue().getName());
		software.setOwner(SYSTEM_OWNER);
		
		SoftwareDao.persist(software);
	}
	
	/**
	 * Uploads the test job output folder to the remote execution system
	 * for use in archiving tasks.
	 * 
	 * @param localArchiveFile
	 * @param job
	 * @param useBigfiles
	 * @throws Exception
	 */
	protected void stageTestData(String localArchiveFile, Job job) 
	throws Exception {
	    stageTestData(localArchiveFile, job, false);
	}
	
	/**
	 * Uploads the test job output folder to the remote execution system
     * for use in archiving tasks. This differs in the other {@link #stageTestData(String, Job)}
     * method in that it also creates multiple > 250MB+ files on the remote 
     * execution system for use in testing long-running transfers.
     * 
	 * @param localArchiveFile
	 * @param job
	 * @param useBigfiles
	 * @throws Exception
	 */
	protected void stageTestData(String localArchiveFile, Job job, boolean useBigfiles) throws Exception {
		
		RemoteDataClient exeClient = null;
		OutputStream out = null;
		try {
			exeClient = executionSystem.getRemoteDataClient(job.getInternalUsername());
			exeClient.authenticate();
			
			// set up the work folder like it's a completed job
			exeClient.mkdirs(job.getWorkPath());
			
			if (useBigfiles) {
			    // create a ~300MB file for testing interrupts of longer transfers
			    if (!exeClient.doesExist("bigfile.txt")) {
                    out = exeClient.getOutputStream("bigfile.txt", false, false);
                    byte[] buf = new byte[exeClient.getMaxBufferSize()];
                    for (int i=0;i<Math.ceil(Math.pow(2, 20)/exeClient.getMaxBufferSize()); i++) {
                        out.write(buf);
                    }
			    }
			    
			    exeClient.copy("bigfile.txt", job.getWorkPath());
            
                for (File localFile: new File(TEST_DATA_DIR).listFiles()) {
                    exeClient.put(localFile.getAbsolutePath(), job.getWorkPath() + "/" + localFile.getName());
                    exeClient.copy("bigfile.txt", job.getWorkPath() + "/beta-" + localFile.getName());
                }
			}
			else
			{
    			for (File localFile: new File(TEST_DATA_DIR).listFiles()) {
    			    exeClient.put(localFile.getAbsolutePath(), job.getWorkPath() + "/" + localFile.getName());
    			    exeClient.copy(job.getWorkPath() + "/" + localFile.getName(), job.getWorkPath() + "/beta-" + localFile.getName());
    			}
			}
			
			
			// copy over the .agave.archive file
			exeClient.put(localArchiveFile, job.getWorkPath() + "/.agave.archive");
			Assert.assertTrue(exeClient.doesExist(job.getWorkPath()), 
					"Failed to copy work files to execution system");
		} catch (Exception e) {
			Assert.fail("Failed to work files to execution system", e);
		} finally {
		    try { if (out != null) out.close(); } catch (Exception e) {}
		    exeClient.disconnect();
		}
	}
	
	@DataProvider(name="testArchiveWatchProvider")
	public Object[][] testArchiveWatchProvider() throws Exception
	{
		List<Software> testApps = SoftwareDao.getUserApps(SYSTEM_OWNER, false);
		File tmpArchiveFile = new File(System.currentTimeMillis() + "");
		tmpArchiveFile.createNewFile();
		
		FileUtils.write(tmpArchiveFile, ".agave.archive\n", true);
		for(File workFile: new File(TEST_DATA_DIR).listFiles()) {
			FileUtils.write(tmpArchiveFile, workFile.getName() + "\n", true);
		}
		
		Object[][] testData = new Object[testApps.size()][3];
		for(int i=0; i< testApps.size(); i++) {
			testData[i] = new Object[] { testApps.get(i), tmpArchiveFile, "Archiving of job output failed.", false };
		}
		
		return testData;
	}
	
//	@Test (groups={"archiving"}, dataProvider="testArchiveWatchProvider", enabled=true)
//	public void testArchiveWatch(Software software, File tmpArchiveFile, String message, boolean shouldThrowException) throws Exception
//	{
//		RemoteDataClient remoteDataClient = null;
//		
//		job = createJob(CLEANING_UP, software, SYSTEM_OWNER);
//		
//		// archive the job when it's done
//		try 
//		{
//			stageTestData(tmpArchiveFile.getAbsolutePath(), job);
//			
//			WorkerWatch archiveWatch = new ArchiveWatch();
//			archiveWatch.setJob(job);
//			archiveWatch.doExecute();
//			
//			job = JobDao.getById(job.getId());
//			Assert.assertEquals(job.getStatus(), JobStatusType.FINISHED, 
//					"Job status was not FINISHED after ArchiveWatch completed.");
//			
//			remoteDataClient = job.getArchiveSystem().getRemoteDataClient(job.getInternalUsername());
//			remoteDataClient.authenticate();
//			Assert.assertTrue(remoteDataClient.doesExist(job.getArchivePath()), 
//					"Archive folder does not exist on remote system.");
//			
//			String[] excludedFiles = FileUtils.readFileToString(tmpArchiveFile).split("\n");
//			
//			for (String localFile: excludedFiles) {
//				Assert.assertFalse(remoteDataClient.doesExist(job.getArchivePath() + "/" + localFile), 
//						"Excluded file was archived " + job.getArchivePath() + "/" + localFile);
//			}
//		} 
//		catch (Exception e) {
//			Assert.fail("Failed to archive job data to " + job.getArchiveSystem().getSystemId(), e);
//		} 
//		finally {
//			try { tmpArchiveFile.delete(); } catch (Exception e) {}
//			try { remoteDataClient.disconnect(); } catch (Exception e) {}
//		}
//	}
	
//	@Test (groups={"archiving"}, dataProvider="testArchiveWatchProvider")
//	public void concurrentQueueTerminationTest(Software software, File tmpArchiveFile, String message, boolean shouldThrowException) 
//	throws Exception 
//	{
//	    clearJobs();
//        Scheduler sched = StdSchedulerFactory.getDefaultScheduler();
//        
//        JobDetail jobDetail = newJob(ArchiveWatch.class)
//                .withIdentity("primary", "Archiving")
//                .requestRecovery(true)
//                .storeDurably()
//                .build();
//        
//        sched.addJob(jobDetail, true);
//        
//        // start a block of worker processes to pull pre-staged file references
//        // from the db and apply the appropriate transforms to them.
//        for (int i = 0; i < 15; i++)
//        {
//            
//            Trigger trigger = newTrigger()
//                    .withIdentity("trigger"+i, "Archiving")
//                    .startAt(new DateTime().plusSeconds(i).toDate())
//                    .withSchedule(simpleSchedule()
//                            .withMisfireHandlingInstructionIgnoreMisfires()
//                            .withIntervalInSeconds(2)
//                            .repeatForever())
//                    .forJob(jobDetail)
//                    .withPriority(5)
//                    .build();
//            
//            sched.scheduleJob(trigger);
//        }
//        
//        final AtomicInteger jobsComplete = new AtomicInteger(0);
//        sched.getListenerManager().addJobListener(
//                new JobListener() {
//
//                    @Override
//                    public String getName() {
//                        return "Unit Test Listener";
//                    }
//
//                    @Override
//                    public void jobToBeExecuted(JobExecutionContext context) {
//                        log.debug("working on a new job");                        
//                    }
//
//                    @Override
//                    public void jobExecutionVetoed(JobExecutionContext context) {
//                        // no idea here
//                    }
//
//                    @Override
//                    public void jobWasExecuted(JobExecutionContext context, JobExecutionException e) {
//                        if (e == null) {
//                            log.error(jobsComplete.addAndGet(1) + "/100 Completed jobs ",e);
//                        } else {
////                            log.error("Transfer failed",e);
//                        }
//                    }
//                    
//                }, KeyMatcher.keyEquals(jobDetail.getKey())
//            );
//        
//	    // archive the job when it's done
//        try 
//        {
//            for (int i=0;i<25;i++) {
//                Job job = createJob(CLEANING_UP, software, SYSTEM_OWNER);
//                stageTestData(tmpArchiveFile.getAbsolutePath(), job, true);
//            }
//            
//            sched.start();
//            
//            log.debug("Sleeping to allow scheduler to run for a bit...");
//            try { Thread.sleep(7000); } catch (Exception e) {}
//            
//            log.debug("Resuming test run and pausing all archiving triggers...");
//            sched.pauseAll();
//            log.debug("All triggers stopped. Interrupting executing jobs...");
//            
//            for (JobExecutionContext context: sched.getCurrentlyExecutingJobs()) {
//                log.debug("Interrupting job " + context.getJobDetail().getKey() + "...");
//                sched.interrupt(context.getJobDetail().getKey());
//                log.debug("Interrupt of job " + context.getJobDetail().getKey() + " complete.");
//            }
//            log.debug("Shutting down scheduler...");
//            sched.shutdown(false);
//            log.debug("Scheduler shut down...");
//            
//            for (Job job: JobDao.getAll())
//            {
//                Assert.assertTrue(job.getStatus() == JobStatusType.CLEANING_UP || job.getStatus() == JobStatusType.FINISHED, 
//                        "Job status was not rolled back upon interrupt.");
//            }
//        } 
//        catch (Exception e) {
//            Assert.fail("Failed to archive job data due to unexpected error", e);
//        } 
//        finally {
//            try { tmpArchiveFile.delete(); } catch (Exception e) {}
//        }
//        
//	}
}