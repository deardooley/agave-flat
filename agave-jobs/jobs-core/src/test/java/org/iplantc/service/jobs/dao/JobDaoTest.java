package org.iplantc.service.jobs.dao;

import static org.iplantc.service.jobs.model.JSONTestDataUtil.TEST_OWNER;
import static org.iplantc.service.jobs.model.JSONTestDataUtil.TEST_SHARED_OWNER;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;

import org.iplantc.service.apps.dao.SoftwareDao;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.jobs.model.enumerations.JobStatusType;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

@Test(groups={"broken"})
public class JobDaoTest extends AbstractDaoTest {

	@BeforeClass
	@Override
	public void beforeClass() throws Exception
	{
		super.beforeClass();
	}
	
	@AfterClass
	@Override
	public void afterClass() throws Exception
	{
		super.afterClass();
	}
	
	@BeforeMethod
	public void setUp() throws Exception {
		initSystems();
        initSoftware();
        SoftwareDao.persist(software);
		clearJobs();
	}
	
	@AfterMethod
	public void tearDown() throws Exception {
		clearJobs();
		clearSoftware();
		clearSystems();
	}
	
	@Test
	public void persist() throws Exception
	{
		Job job = createJob(JobStatusType.PENDING);
		JobDao.persist(job);
		Assert.assertNotNull(job.getId(), "Failed to generate a job ID.");
	}

	@Test(dependsOnMethods={"persist"})
	public void delete() throws Exception
	{
		Job job = createJob(JobStatusType.PENDING);
		JobDao.persist(job);
		Assert.assertNotNull(job.getId(), "Failed to generate a job ID.");
		long jobId = job.getId();
		
		JobDao.delete(job);
		
		Job deletedJob = JobDao.getById(jobId);
		Assert.assertNull(deletedJob, "Failed to delete job.");
	}

	@Test(dependsOnMethods={"delete"})
	public void countActiveUserJobsOnSystem() throws Exception
	{
		List<Job> testActiveJobs = new ArrayList<Job>();
		List<Job> testInactiveJobs = new ArrayList<Job>();
		Job otherUserJob = null; 
		
		for (JobStatusType status: JobStatusType.getActiveStatuses())
		{	
			Job testJob = createJob(status);
			JobDao.persist(testJob);
			Assert.assertNotNull(testJob.getId(), 
					"Failed to persist active job " + status.name() + ".");
			testActiveJobs.add(testJob);
		}
		
		for (JobStatusType status: JobStatusType.values())
		{	
			if (!Arrays.asList(JobStatusType.getActiveStatuses()).contains(status))
			{
				Job testJob = createJob(status);
				JobDao.persist(testJob);
				Assert.assertNotNull(testJob.getId(), 
						"Failed to persist inactive job " + status.name() + ".");
				testInactiveJobs.add(testJob);
			}
		}
		
		otherUserJob = createJob(JobStatusType.RUNNING);
		otherUserJob.setOwner(TEST_SHARED_OWNER);
		JobDao.persist(otherUserJob);
		Assert.assertNotNull(otherUserJob.getId(), "Failed to persist other user job.");
		
		long userSystemJobCount = JobDao.countActiveUserJobsOnSystem(TEST_OWNER, otherUserJob.getSystem());
		Assert.assertEquals(userSystemJobCount, testActiveJobs.size(), 
				"Number of user system jobs returned is not equal to the number " +
				"of active jobs added in the test.");
	}
	
	@Test(dependsOnMethods={"countActiveUserJobsOnSystem"})
	public void countActiveJobsOnSystem() throws Exception
	{
		List<Job> testActiveJobs = new ArrayList<Job>();
		List<Job> testInactiveJobs = new ArrayList<Job>();
		Job otherUserJob = null; 
		
		for (JobStatusType status: JobStatusType.getActiveStatuses())
		{	
			Job testJob = createJob(status);
			JobDao.persist(testJob);
			Assert.assertNotNull(testJob.getId(), 
					"Failed to persist active job " + status.name() + ".");
			testActiveJobs.add(testJob);
		}
		
		for (JobStatusType status: JobStatusType.values())
		{	
			if (!Arrays.asList(JobStatusType.getActiveStatuses()).contains(status))
			{
				Job testJob = createJob(status);
				JobDao.persist(testJob);
				Assert.assertNotNull(testJob.getId(), 
						"Failed to persist inactive job " + status.name() + ".");
				testInactiveJobs.add(testJob);
			}
		}
		
		otherUserJob = createJob(JobStatusType.RUNNING);
		otherUserJob.setOwner(TEST_SHARED_OWNER);
		JobDao.persist(otherUserJob);
		Assert.assertNotNull(otherUserJob.getId(), "Failed to persist other user job.");
		
		long systemJobCount = JobDao.countActiveJobsOnSystem(otherUserJob.getSystem());
		Assert.assertEquals(systemJobCount, testActiveJobs.size() + 1, 
				"Number of user system jobs returned is not equal to the number " +
				"of active jobs added in the test.");
	}

	@Test(dependsOnMethods={"countActiveJobsOnSystem"})
	public void getById() throws Exception
	{

//		try {
//			systemDao.persist(privateStorageSystem);
//		} catch (Throwable e) { e.printStackTrace(); }
//		
		Job job = createJob(JobStatusType.PENDING);
		JobDao.persist(job);
		Assert.assertNotNull(job.getId(), "Failed to generate a job ID.");
		
		Job savedJob = JobDao.getById(job.getId());
		Assert.assertNotNull(savedJob, "Failed to find job by id.");
		Assert.assertNotNull(savedJob.getArchiveSystem(), "Failed to save archive system.");
	}

	@Test(dependsOnMethods={"getById"})
	public void getByUserAndId() throws Exception
	{
		Job job = createJob(JobStatusType.PENDING);
		JobDao.persist(job);
		Assert.assertNotNull(job.getId(), "Failed to generate a job ID.");
		
		Job savedJob = JobDao.getByUsernameAndId(job.getOwner(), job.getId());
		Assert.assertNotNull(savedJob, "Failed to save job.");
		Assert.assertNotNull(savedJob.getArchiveSystem(), "Failed to save archive system.");
	}

	@Test(dependsOnMethods={"getByUserAndId"})
	public void getByUsernameAndStatus() throws Exception
	{
		Job job1 = createJob(JobStatusType.RUNNING);
		JobDao.persist(job1);
		Assert.assertNotNull(job1.getId(), "Failed to save job 1.");
		
		Job job2 = createJob(JobStatusType.RUNNING);
		JobDao.persist(job2);
		Assert.assertNotNull(job2.getId(), "Failed to save job 2.");
		
		Job job3 = createJob(JobStatusType.PENDING);
		JobDao.persist(job3);
		Assert.assertNotNull(job3.getId(), "Failed to save job 3.");
		
		Job job4 = createJob(JobStatusType.PENDING);
		job4.setOwner(TEST_SHARED_OWNER);
		JobDao.persist(job4);
		Assert.assertNotNull(job4.getId(), "Failed to save job 4.");
		
		List<Job> runningJobs = JobDao.getByUsernameAndStatus(TEST_OWNER, JobStatusType.RUNNING);
		Assert.assertNotNull(runningJobs, "getByUsernameAndStatus failed to find any running jobs.");
		Assert.assertEquals(runningJobs.size(), 2, "getByUsernameAndStatus returned the wrong number of jobs.");
		Assert.assertFalse(runningJobs.contains(job3), "getByUsernameAndStatus returned a job with the wrong status.");
		Assert.assertFalse(runningJobs.contains(job4), "getByUsernameAndStatus returned a job owned by another user.");
	}

	@Test(dependsOnMethods={"getByUsernameAndStatus"})
	public void getNextQueuedJob() throws Exception
	{
		Job job1 = createJob(JobStatusType.PENDING);
		JobDao.persist(job1);
		Assert.assertNotNull(job1.getId(), "Failed to save job 1.");
		
		Job job2 = createJob(JobStatusType.PENDING);
		JobDao.persist(job2);
		Assert.assertNotNull(job2.getId(), "Failed to save job 2.");
		
		Job job3 = createJob(JobStatusType.RUNNING);
		JobDao.persist(job3);
		Assert.assertNotNull(job3.getId(), "Failed to save job 3.");
		
		Job job4 = createJob(JobStatusType.ARCHIVING_FINISHED);
		job4.setOwner(TEST_SHARED_OWNER);
		JobDao.persist(job4);
		Assert.assertNotNull(job4.getId(), "Failed to save job 4.");
		
		String nextRunningJob = JobDao.getNextQueuedJobUuid(JobStatusType.PENDING, 
		        TenancyHelper.getDedicatedTenantIdForThisService(), null, null);
		Assert.assertNotNull(nextRunningJob, "getNextQueuedJob failed to find any PENDING jobs.");
		// we no longer get just the oldest job first
		//Assert.assertEquals(nextRunningJob, job1, "getByUsernameAndStatus did not return the oldest job.");
	}
	
	private String formatDate(Date date) {
		return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
	}
	
	@Test(dependsOnMethods={"getNextQueuedJob"})
	public void setStatusPersistsJobEventTest() throws Exception
	{
		Job job = createJob(JobStatusType.PENDING);
		
		JobDao.persist(job);
		Assert.assertNotNull(job.getId(), "Failed to generate a job ID.");
		
		Assert.assertFalse(job.getEvents().isEmpty(), "Job event was not saved.");
		Assert.assertTrue(job.getEvents().size() == 1, "Incorrect number of job events after saving");
		
		job.setStatus(JobStatusType.STAGED, JobStatusType.STAGED.getDescription());
		JobDao.persist(job);
		
		Assert.assertFalse(job.getEvents().isEmpty(), "Job event was not saved.");
		Assert.assertTrue(job.getEvents().size() == 2, "Incorrect number of job events after adding status");
		
	}
}
