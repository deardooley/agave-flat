package org.iplantc.service.jobs.dao;

import java.util.List;

import org.hibernate.HibernateException;
import org.iplantc.service.apps.dao.SoftwareDao;
import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.jobs.model.JobEvent;
import org.iplantc.service.jobs.model.enumerations.JobStatusType;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class JobEventDaoTest extends AbstractDaoTest
{
	JobEventDao dao = new JobEventDao();
	
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
	
	@AfterClass
	public void afterClass() throws Exception {
		clearJobs();
		clearSystems();
		clearSoftware();
	}

	@DataProvider
	public Object[][] persistProvider() throws Exception 
	{
		Job job = createJob(JobStatusType.PENDING);
		JobDao.persist(job);
		
		JobEvent eventNoJob = new JobEvent(JobStatusType.PENDING.name(), JobStatusType.PENDING.getDescription(), job.getOwner());
		JobEvent eventWithJob = new JobEvent(job, JobStatusType.PENDING, JobStatusType.PENDING.getDescription(), job.getOwner());
		
		
		return new Object[][] {
			{ eventNoJob, true, "Emtpy job event should fail to persist"},
			{ eventWithJob, false, "Valid job event should persist"},
			
		};
	}

	@Test(dataProvider="persistProvider")
	public void persist(JobEvent event, boolean shouldThrowException, String message) 
	{
		try 
		{
			JobEventDao.persist(event);
			
			Assert.assertFalse(shouldThrowException, message);
			Assert.assertNotNull(event.getId(), message);
		}
		catch (HibernateException e) {
			Assert.assertTrue(shouldThrowException, message);
		}
		catch (Exception e) {
			Assert.assertTrue(shouldThrowException, message);
		}
	}
	
	@Test(dependsOnMethods={"persist"})
	public void getById() throws Exception
	{
		try 
		{	
			Job job1 = createJob(JobStatusType.PENDING);
			JobDao.persist(job1);
			
			Assert.assertNotNull(job1.getId(), "Failed to generate a job 1 ID.");
			
			JobEvent event1 = new JobEvent(job1, JobStatusType.PENDING, JobStatusType.PENDING.getDescription(), job1.getOwner());
			
			JobEventDao.persist(event1);
			
			Assert.assertNotNull(event1.getId(), "Valid job event 1 should persist");
			
			JobEvent savedEvent = JobEventDao.getById(event1.getId());
			
			Assert.assertNotNull(savedEvent, "Failed to retrieve job event by id");
		} 
		catch (Throwable e) 
		{ 
			Assert.fail("Unexpected exception occurred", e); 
		}
	}
	
	
	@Test(dependsOnMethods={"getById"})
	public void getByJobId() 
	{
		try 
		{
			Job job1 = createJob(JobStatusType.PENDING);
			JobDao.persist(job1);
			
			Assert.assertNotNull(job1.getId(), "Failed to generate a job 1 ID.");
			
			Job job2 = createJob(JobStatusType.PENDING);
			JobDao.persist(job2);
			
			Assert.assertNotNull(job2.getId(), "Failed to generate a job 2 ID.");
			
			
			JobEvent event1 = new JobEvent(job1, JobStatusType.PENDING, JobStatusType.PENDING.getDescription(), job1.getOwner());
			
			JobEventDao.persist(event1);
			
			Assert.assertNotNull(event1.getId(), "Valid job event 1 should persist");
			
			
			JobEvent event2 = new JobEvent(job1, JobStatusType.RUNNING, JobStatusType.RUNNING.getDescription(), job1.getOwner());
			
			JobEventDao.persist(event2);
			
			Assert.assertNotNull(event2.getId(), "Valid job event 2 should persist");
			
			
			JobEvent event3 = new JobEvent(job2, JobStatusType.PENDING, JobStatusType.PENDING.getDescription(), job2.getOwner());
			
			JobEventDao.persist(event3);
			
			Assert.assertNotNull(event3.getId(), "Valid job event 3 should persist");
			
			
			List<JobEvent> savedEvents = JobEventDao.getByJobId(job1.getId());
			
			Assert.assertEquals(savedEvents.size(), 3, "Wrong number of job events found for job 1.");
			
			Assert.assertEquals(savedEvents.get(1).getId(), event1.getId(), "Saved job 1 event 1 not found.");
			
			Assert.assertEquals(savedEvents.get(2).getId(), event2.getId(), "Saved job 1 event 2 not found.");
			
//			JobEvent savedEvent3 = JobEventDao.getById(event3.getId());
//			Assert.assertFalse(savedEvents.contains(savedEvent3), "Saved job 2 event 3 not found.");
		}
		catch (HibernateException e) {
			Assert.fail("Unexpected failure occurred.", e);
		}
		catch (Exception e) {
			Assert.fail("Unexpected failure occurred.", e);
		}
	}

	@Test(dependsOnMethods={"getByJobId"})
	public void getByJobIdAndStatus() {
		try 
		{
			Job job1 = createJob(JobStatusType.PENDING);
			JobDao.persist(job1);
			
			Assert.assertNotNull(job1.getId(), "Failed to generate a job 1 ID.");
			
			Job job2 = createJob(JobStatusType.PENDING);
			JobDao.persist(job2);
			
			Assert.assertNotNull(job2.getId(), "Failed to generate a job 2 ID.");
			
			
			JobEvent event1 = new JobEvent(job1, JobStatusType.PENDING, JobStatusType.PENDING.getDescription(), job1.getOwner());
			
			JobEventDao.persist(event1);
			
			Assert.assertNotNull(event1.getId(), "Valid job event 1 should persist");
			
			
			JobEvent event2 = new JobEvent(job1, JobStatusType.RUNNING, JobStatusType.RUNNING.getDescription(), job1.getOwner());
			
			JobEventDao.persist(event2);
			
			Assert.assertNotNull(event2.getId(), "Valid job event 2 should persist");
			
			
			JobEvent event3 = new JobEvent(job2, JobStatusType.PENDING, JobStatusType.PENDING.getDescription(), job2.getOwner());
			
			JobEventDao.persist(event3);
			
			Assert.assertNotNull(event3.getId(), "Valid job event 3 should persist");
			
			
			List<JobEvent> runningEvents = JobEventDao.getByJobIdAndStatus(job1.getId(), JobStatusType.RUNNING);
			
			Assert.assertEquals(runningEvents.size(), 1, "Wrong number of job RUNNING events found for job 1.");
			
			Assert.assertEquals(runningEvents.get(0).getId(), event2.getId(), "Wrong RUNNING job event returned");
			
//			JobEvent savedEvent1 = JobEventDao.getById(event1.getId());
//			Assert.assertFalse(runningEvents.contains(savedEvent1), "Saved job 1 event 1 not found.");
//			
//			JobEvent savedEvent2 = JobEventDao.getById(event2.getId());
//			Assert.assertTrue(runningEvents.contains(savedEvent2), "Saved job 1 event 2 not found.");
//			
//			JobEvent savedEvent3 = JobEventDao.getById(event3.getId());
//			Assert.assertFalse(runningEvents.contains(savedEvent3), "Saved job 2 event 3 was found when it should not have been.");
		}
		catch (HibernateException e) {
			Assert.fail("Unexpected failure occurred.", e);
		}
		catch (Exception e) {
			Assert.fail("Unexpected failure occurred.", e);
		}
	}
	
	@Test(dependsOnMethods={"deleteByJobId"})
	public void delete() {
		try 
		{
			Job job1 = createJob(JobStatusType.PENDING);
			JobDao.persist(job1);
			
			Assert.assertNotNull(job1.getId(), "Failed to generate a job 1 ID.");
			
			Job job2 = createJob(JobStatusType.PENDING);
			JobDao.persist(job2);
			
			Assert.assertNotNull(job2.getId(), "Failed to generate a job 2 ID.");
			
			
			JobEvent event1 = new JobEvent(job1, JobStatusType.PENDING, JobStatusType.PENDING.getDescription(), job1.getOwner());
			
			JobEventDao.persist(event1);
			
			Long eventId1 = event1.getId();
			
			Assert.assertNotNull(eventId1, "Valid job event 1 should persist");
			
			
			JobEvent event2 = new JobEvent(job1, JobStatusType.RUNNING, JobStatusType.RUNNING.getDescription(), job1.getOwner());
			
			JobEventDao.persist(event2);
			
			Long eventId2 = event2.getId();
			
			Assert.assertNotNull(eventId2, "Valid job event 2 should persist");
			
			
			JobEvent event3 = new JobEvent(job2, JobStatusType.PENDING, JobStatusType.PENDING.getDescription(), job2.getOwner());
			
			JobEventDao.persist(event3);
			
			Long eventId3 = event3.getId();
			
			Assert.assertNotNull(eventId3, "Valid job event 3 should persist");
			
			
			JobEventDao.delete(event1);
			
			
			JobEvent savedEvent1 = JobEventDao.getById(eventId1);
			
			Assert.assertNull(savedEvent1, "Failed to delete event 1.");
			
			List<JobEvent> savedEvents = JobEventDao.getByJobId(job1.getId());
			
			Assert.assertEquals(savedEvents.size(), 2, "Wrong number of job events found for job 1.");
		}
		catch (HibernateException e) {
			Assert.fail("Unexpected failure occurred.", e);
		}
		catch (Exception e) {
			Assert.fail("Unexpected failure occurred.", e);
		}
	}

	@Test(dependsOnMethods={"getByJobIdAndStatus"})
	public void deleteByJobId() {
		try 
		{
			Job job1 = createJob(JobStatusType.PENDING);
			JobDao.persist(job1);
			
			Assert.assertNotNull(job1.getId(), "Failed to generate a job 1 ID.");
			
			Job job2 = createJob(JobStatusType.PENDING);
			JobDao.persist(job2);
			
			Assert.assertNotNull(job2.getId(), "Failed to generate a job 2 ID.");
			
			
			JobEvent event1 = new JobEvent(job1, JobStatusType.PENDING, JobStatusType.PENDING.getDescription(), job1.getOwner());
			
			JobEventDao.persist(event1);
			
			Long eventId1 = event1.getId();
			
			Assert.assertNotNull(eventId1, "Valid job event 1 should persist");
			
			
			JobEvent event2 = new JobEvent(job1, JobStatusType.RUNNING, JobStatusType.RUNNING.getDescription(), job1.getOwner());
			
			JobEventDao.persist(event2);
			
			Long eventId2 = event2.getId();
			
			Assert.assertNotNull(eventId2, "Valid job event 2 should persist");
			
			
			JobEvent event3 = new JobEvent(job2, JobStatusType.PENDING, JobStatusType.PENDING.getDescription(), job2.getOwner());
			
			JobEventDao.persist(event3);
			
			Long eventId3 = event3.getId();
			
			Assert.assertNotNull(eventId3, "Valid job event 3 should persist");
			
			
			JobEventDao.deleteByJobId(job1.getId());
			
			
			JobEvent savedEvent1 = JobEventDao.getById(eventId1);
			
			Assert.assertNull(savedEvent1, "Failed to delete event 1.");
			
			JobEvent savedEvent2 = JobEventDao.getById(eventId2);
			
			Assert.assertNull(savedEvent2, "Failed to delete event 2.");
			
			List<JobEvent> savedEvents = JobEventDao.getByJobId(job1.getId());
			
			Assert.assertTrue(savedEvents.isEmpty(), "Querying for job events did not return an empty list.");
			
			JobEvent savedEvent3 = JobEventDao.getById(eventId3);
			
			Assert.assertNotNull(savedEvent3, "Saved event 3 for job 2 was deleted when it should not have been.");
			
		}
		catch (HibernateException e) {
			Assert.fail("Unexpected failure occurred.", e);
		}
		catch (Exception e) {
			Assert.fail("Unexpected failure occurred.", e);
		}
	}
}	
