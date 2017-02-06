package org.iplantc.service.jobs.dao;

import static org.iplantc.service.jobs.model.JSONTestDataUtil.TEST_OWNER;
import static org.iplantc.service.jobs.model.JSONTestDataUtil.TEST_SHARED_OWNER;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.iplantc.service.apps.dao.SoftwareDao;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.jobs.managers.JobManager;
import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.jobs.model.JobUpdateParameters;
import org.iplantc.service.jobs.model.enumerations.JobPhaseType;
import org.iplantc.service.jobs.model.enumerations.JobStatusType;
import org.iplantc.service.jobs.phases.schedulers.dto.JobQuotaInfo;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
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
		Assert.assertNotNull(job.getId(), "Failed to generate a job ID.");
	}

	@Test(dependsOnMethods={"persist"})
	public void delete() throws Exception
	{
		Job job = createJob(JobStatusType.PENDING);
		Assert.assertNotNull(job.getId(), "Failed to generate a job ID.");
		long jobId = job.getId();
		
		JobDao.delete(job);
		
		Job deletedJob = JobDao.getById(jobId);
		Assert.assertNull(deletedJob, "Failed to delete job.");
	}

	@Test(dependsOnMethods={"countActiveJobsOnSystem"})
	public void getById() throws Exception
	{

		Job job = createJob(JobStatusType.PENDING);
		Assert.assertNotNull(job.getId(), "Failed to generate a job ID.");
		
		Job savedJob = JobDao.getById(job.getId());
		Assert.assertNotNull(savedJob, "Failed to find job by id.");
		Assert.assertNotNull(savedJob.getArchiveSystem(), "Failed to save archive system.");
	}

	@Test(dependsOnMethods={"getByUserAndId"})
	public void getByUsernameAndStatus() throws Exception
	{
		Job job1 = createJob(JobStatusType.RUNNING);
		Assert.assertNotNull(job1.getId(), "Failed to save job 1.");
		
		Job job2 = createJob(JobStatusType.RUNNING);
		Assert.assertNotNull(job2.getId(), "Failed to save job 2.");
		
		Job job3 = createJob(JobStatusType.PENDING);
		Assert.assertNotNull(job3.getId(), "Failed to save job 3.");
		
		Job job4 = createJob(JobStatusType.PENDING);
		job4.setOwner(TEST_SHARED_OWNER);
		
	    JobUpdateParameters jobUpdateParameters = new JobUpdateParameters();
	    jobUpdateParameters.setOwner(job4.getOwner());
	    JobDao.update(job4, jobUpdateParameters);
	        
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
		Assert.assertNotNull(job1.getId(), "Failed to save job 1.");
		
		Job job2 = createJob(JobStatusType.PENDING);
		Assert.assertNotNull(job2.getId(), "Failed to save job 2.");
		
		Job job3 = createJob(JobStatusType.RUNNING);
		Assert.assertNotNull(job3.getId(), "Failed to save job 3.");
		
		Job job4 = createJob(JobStatusType.ARCHIVING_FINISHED);
		job4.setOwner(TEST_SHARED_OWNER);
		
        JobUpdateParameters jobUpdateParameters = new JobUpdateParameters();
        jobUpdateParameters.setOwner(job4.getOwner());
        JobDao.update(job4, jobUpdateParameters);
            
		Assert.assertNotNull(job4.getId(), "Failed to save job 4.");
		
		LinkedList<JobStatusType> statusList = new LinkedList<>();
		statusList.add(JobStatusType.PENDING);
        List<JobQuotaInfo> quotaInfoList = 
            JobDao.getSchedulerJobQuotaInfo(JobPhaseType.STAGING, statusList);
		
		Assert.assertEquals(2, quotaInfoList.size(), "Expected 2 jobs with quota information");
		for (JobQuotaInfo info : quotaInfoList) {
		    boolean found = info.getUuid().equals(job1.getUuid()) ||
		                    info.getUuid().equals(job2.getUuid());
		    Assert.assertTrue(found, "Unexpected job uuid encountered: " + info.getUuid());
		}
	}
	
	private String formatDate(Date date) {
		return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
	}
	
	@Test(dependsOnMethods={"getNextQueuedJob"})
	public void setStatusPersistsJobEventTest() throws Exception
	{
		Job job = createJob(JobStatusType.PENDING);
		
		Assert.assertNotNull(job.getId(), "Failed to generate a job ID.");
		
		Assert.assertFalse(job.getEvents().isEmpty(), "Job event was not saved.");
		Assert.assertTrue(job.getEvents().size() == 1, "Incorrect number of job events after saving");
		
		job = JobManager.updateStatus(job, JobStatusType.STAGED, JobStatusType.STAGED.getDescription());
		
		Assert.assertFalse(job.getEvents().isEmpty(), "Job event was not saved.");
		Assert.assertTrue(job.getEvents().size() == 2, "Incorrect number of job events after adding status");
		
	}
}
