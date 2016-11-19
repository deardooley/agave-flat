package org.iplantc.service.jobs.managers;

import static org.iplantc.service.jobs.model.JSONTestDataUtil.TEST_EXECUTION_SYSTEM_FILE;
import static org.iplantc.service.jobs.model.JSONTestDataUtil.TEST_OWNER;
import static org.iplantc.service.jobs.model.JSONTestDataUtil.TEST_SOFTWARE_SYSTEM_FILE;
import static org.mockito.Mockito.*;
import static org.mockito.Matchers.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.iplantc.service.apps.dao.SoftwareDao;
import org.iplantc.service.apps.model.Software;
import org.iplantc.service.apps.model.SoftwareInput;
import org.iplantc.service.apps.model.SoftwareParameter;
import org.iplantc.service.apps.model.SoftwareParameterEnumeratedValue;
import org.iplantc.service.apps.model.enumerations.SoftwareParameterType;
import org.iplantc.service.common.Settings;
import org.iplantc.service.common.exceptions.MessagingException;
import org.iplantc.service.common.messaging.Message;
import org.iplantc.service.common.messaging.MessageClientFactory;
import org.iplantc.service.common.messaging.MessageQueueClient;
import org.iplantc.service.common.uuid.AgaveUUID;
import org.iplantc.service.common.uuid.UUIDType;
import org.iplantc.service.jobs.dao.AbstractDaoTest;
import org.iplantc.service.jobs.dao.JobDao;
import org.iplantc.service.jobs.dao.JobEventDao;
import org.iplantc.service.jobs.exceptions.JobProcessingException;
import org.iplantc.service.jobs.model.JSONTestDataUtil;
import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.jobs.model.JobEvent;
import org.iplantc.service.jobs.model.enumerations.JobEventType;
import org.iplantc.service.jobs.model.enumerations.JobStatusType;
import org.iplantc.service.notification.dao.NotificationDao;
import org.iplantc.service.notification.model.Notification;
import org.iplantc.service.systems.dao.SystemDao;
import org.iplantc.service.systems.model.BatchQueue;
import org.iplantc.service.systems.model.ExecutionSystem;
import org.iplantc.service.systems.model.RemoteSystem;
import org.iplantc.service.systems.model.enumerations.RemoteSystemType;
import org.json.JSONException;
import org.json.JSONObject;
import org.mockito.Answers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.surftools.BeanstalkClientImpl.ClientImpl;

@Test(groups={"broken"})
public class JobManagerTest extends AbstractDaoTest 
{
	private boolean pass = false;
	private boolean fail = true;
	private ObjectMapper mapper = new ObjectMapper();
	
	private static final Answer<Boolean> ANSWER_TRUE = new Answer<Boolean>() {
		 public Boolean answer(InvocationOnMock invocation) throws Throwable {
	         return true;
	     }
	};
	
	@BeforeClass
	public void beforeClass() throws Exception {
		super.beforeClass();
		stageRemoteSoftwareAssets();
		SoftwareDao.persist(software);
		drainQueue();
	}

	@AfterClass
	public void afterClass() throws Exception {
		clearJobs();
		clearSoftware();
		clearSystems();
		drainQueue();
		deleteRemoteSoftwareAssets();
	}	
	
	/**
	 * Flushes the messaging tube of any and all existing jobs.
	 * @param queueName
	 */
	@AfterMethod
	public void drainQueue() 
	{
		ClientImpl client = null;
	
		try {
			// drain the message queue
			client = new ClientImpl(Settings.MESSAGING_SERVICE_HOST,
					Settings.MESSAGING_SERVICE_PORT);
			client.watch(Settings.NOTIFICATION_QUEUE);
			client.useTube(Settings.NOTIFICATION_QUEUE);
			client.kick(Integer.MAX_VALUE);
			
			com.surftools.BeanstalkClient.Job beanstalkJob = null;
			do {
				try {
					beanstalkJob = client.peekReady();
					if (beanstalkJob != null)
						client.delete(beanstalkJob.getJobId());
				} catch (Throwable e) {
					e.printStackTrace();
				}
			} while (beanstalkJob != null);
			do {
				try {
					beanstalkJob = client.peekBuried();
					if (beanstalkJob != null)
						client.delete(beanstalkJob.getJobId());
				} catch (Throwable e) {
					e.printStackTrace();
				}
			} while (beanstalkJob != null);
			do {
				try {
					beanstalkJob = client.peekDelayed();
					
					if (beanstalkJob != null)
						client.delete(beanstalkJob.getJobId());
				} catch (Throwable e) {
					e.printStackTrace();
				}
			} while (beanstalkJob != null);
		} catch (Throwable e) {
			e.printStackTrace();
		}
		finally {
			try { client.ignore(Settings.NOTIFICATION_QUEUE); } catch (Throwable e) {}
			try { client.close(); } catch (Throwable e) {}
			client = null;
		}
	}
	
	/**
	 * Counts number of messages in the queue.
	 * 
	 * @param queueName
	 * @return int totoal message count
	 */
	protected int getMessageCount(String queueName) throws MessagingException
	{
		ClientImpl client = null;
		
		try {
			// drain the message queue
			client = new ClientImpl(Settings.MESSAGING_SERVICE_HOST,
					Settings.MESSAGING_SERVICE_PORT);
			client.watch(queueName);
			client.useTube(queueName);
			Map<String,String> stats = client.statsTube(queueName);
			String totalJobs = stats.get("current-jobs-ready");
			if (NumberUtils.isNumber(totalJobs)) {
				return NumberUtils.toInt(totalJobs);
			} else {
				throw new MessagingException("Failed to find total job count for queue " + queueName);
			}
		} catch (MessagingException e) {
			throw e;
		} catch (Throwable e) {
			throw new MessagingException("Failed to read jobs from queue " + queueName, e);
		}
		finally {
			try { client.ignore(Settings.NOTIFICATION_QUEUE); } catch (Throwable e) {}
			try { client.close(); } catch (Throwable e) {}
			client = null;
		}
	}
	
	/**
	 * Creates a test array of jobs job with every {@link JobStatusType}.
	 * 
	 * @return an array of object arrays with the following structure: 
	 * <ul>
	 * 	<li>Job job: a visible job with {@link JobStatusType}</li>
	 * 	<li>boolean shouldThrowException: {@code false} this should never throw an exception</li> 
	 * 	<li>String message: a meaningful message of why the test should not have failed.</li>
	 * </ul> 
	 * @throws Exception
	 */
	@DataProvider
	protected Object[][] hideVisibleJobProvider() throws Exception {
		List<Object[]> testCases = new ArrayList<Object[]>();
	
		for (JobStatusType status: JobStatusType.values()) {
//			if (JobStatusType.isRunning(status)) {
				testCases.add( new Object[]{ createJob(status), 
										false, 
										"Hiding " + status.name() + " job should not throw exception"});
		}
	
		return testCases.toArray(new Object[][]{});
	}

	@Test(dataProvider="hideVisibleJobProvider", groups="jobManagement", enabled=true)
	public void hideVisibleJob(Job job, boolean shouldThrowException, String message) 
	throws Exception 
	{
		try {
			
			int eventCount = job.getEvents().size();
			
			boolean hasRunningStatus = job.isRunning();
			
			JobManager.hide(job.getId(), job.getOwner());
			
			Job hiddenJob = JobDao.getById(job.getId());
			
			Assert.assertNotNull(hiddenJob, "Hidden job should return when queried by ID");
			
			Assert.assertFalse(hiddenJob.isVisible(), "Hidden job should not be visible.");
			
			int expectedEventCount = eventCount + (hasRunningStatus ? 2 : 1);
			
			Assert.assertEquals(hiddenJob.getEvents().size(), expectedEventCount, 
					"Unexpected number of events present after hiding the job.");
			
			JobEvent restoredJobEvent = hiddenJob.getEvents().get(hiddenJob.getEvents().size()-1);
			
			Assert.assertTrue(restoredJobEvent.getStatus().equalsIgnoreCase(JobEventType.DELETED.name()), 
					"DELETED event was not written to the job event history after being restored.");
			
			if (hasRunningStatus) {
				JobEvent stoppedJobEvent = hiddenJob.getEvents().get(hiddenJob.getEvents().size()-2);
				
				Assert.assertTrue(stoppedJobEvent.getStatus().equalsIgnoreCase(JobEventType.STOPPED.name()), 
						"STOPPED event was not written to the job event history prior to being restored.");
			}
		}
		catch (Exception e) {
			Assert.fail(message);
		}
	}
	
	@Test(dataProvider="restoreHiddenJobProvider", groups="jobManagement", enabled=false)
	protected void hideHiddenJob(Job job, boolean shouldThrowException, String message) 
	throws Exception 
	{
		try {
			
			int eventCount = job.getEvents().size();
			
			JobManager.hide(job.getId(), job.getOwner());
			
			Job hiddenJob = JobDao.getById(job.getId());
			
			Assert.assertNotNull(hiddenJob, "Hidden job should return when queried by ID");
			
			Assert.assertFalse(hiddenJob.isVisible(), "Hidden job should not be visible.");
			
			int expectedEventCount = eventCount + 1;
			
			Assert.assertEquals(hiddenJob.getEvents().size(), expectedEventCount, 
					"Unexpected number of events present after hiding the job.");
			
			JobEvent restoredJobEvent = hiddenJob.getEvents().get(hiddenJob.getEvents().size()-1);
			
			Assert.assertTrue(restoredJobEvent.getStatus().equalsIgnoreCase(JobEventType.RESTORED.name()), 
					"RESTORED event was not written to the job event history after being restored.");
			
//			if (hasRunningStatus) {
//				JobEvent stoppedJobEvent = hiddenJob.getEvents().get(hiddenJob.getEvents().size()-2);
//				
//				Assert.assertTrue(stoppedJobEvent.getStatus().equalsIgnoreCase(JobEventType.STOPPED.name()), 
//						"STOPPED event was not written to the job event history prior to being restored.");
//			}
		}
		catch (Exception e) {
			Assert.fail(message);
		}
	}
	
	/**
	 * Creates a test array of jobs job with every {@link JobStatusType}.
	 * 
	 * @return an array of object arrays with the following structure: 
	 * <ul>
	 * 	<li>Job job: a hidden job with {@link JobStatusType}</li>
	 * 	<li>boolean shouldThrowException: {@code false} this should never throw an exception</li> 
	 * 	<li>String message: a meaningful message of why the test should not have failed.</li>
	 * </ul> 
	 * @throws Exception
	 */
	@DataProvider
	protected Object[][] restoreHiddenJobProvider() throws Exception {
		List<Object[]> testCases = new ArrayList<Object[]>();
	
		for (JobStatusType status: JobStatusType.values()) {
			Job job = createJob(status);
			job.setVisible(false);
			JobDao.persist(job);
			
			job.setVisible(false);
				testCases.add( new Object[]{ job, 
										false, 
										"Restoring " + status.name() + " job should not throw exception"});
		}
	
		return testCases.toArray(new Object[][]{});
	}
	
	@Test(dataProvider="restoreHiddenJobProvider", groups="jobManagement")
	public void restoreHiddenJob(Job job, boolean shouldThrowException, String message) {
		try {
			int eventCount = job.getEvents().size();
			
			JobManager.restore(job.getId(), job.getOwner());
			
			Job restoredJob = JobDao.getById(job.getId());
			
			Assert.assertNotNull(restoredJob, "Restored job should return when queried by ID");
			
			Assert.assertTrue(restoredJob.isVisible(), "Restored job should not be visible.");
			
			Assert.assertEquals(restoredJob.getEvents().size(), eventCount+1, 
					"Unexpected number of events present after restoring the job.");
			
			JobEvent restoredJobEvent = restoredJob.getEvents().get(restoredJob.getEvents().size()-1);
			
			Assert.assertTrue(restoredJobEvent.getStatus().equalsIgnoreCase(JobEventType.RESTORED.name()), 
					"RESTORED event was not written to the job event history after being restored.");
		}
		catch (Exception e) {
			Assert.fail(message);
		}
	}
	
	@Test(enabled=false)
	public void kill(Job job, boolean shouldThrowException, String message) {
		throw new RuntimeException("Test not implemented");
	}
	
	/**
	 * Tests that the job status is updated (if not redundant), and a 
	 * notification is sent when the job has one.
	 * 
	 * @param job
	 * @param status
	 * @param shouldThrowException
	 * @param message
	 */
	@Test(dataProvider="updateStatusJobJobStatusTypeProvider", enabled=false)
	public void updateStatusJobJobStatusType(JobStatusType jobStatus, JobStatusType newStatus, String notificatonEvent, boolean shouldThrowException, String message) 
	{
		Job job = null;
		try 
		{
			NotificationDao notificationDao = new NotificationDao();
		
			job = createJob(jobStatus);
			JobDao.persist(job);
			
			Notification notification = null;
			if (!StringUtils.isEmpty(notificatonEvent)) {
				notification = new Notification(job.getUuid(), job.getOwner(), notificatonEvent, "http://example.com", false);
				notificationDao.persist(notification);
			}
			
			JobManager.updateStatus(job, newStatus);
			
			// verify status update
			Assert.assertEquals(job.getStatus(), newStatus,
					"Job status did not update after status update.");
			Assert.assertEquals(job.getErrorMessage(), newStatus.getDescription(),
					"Job description did not update after status update.");

			// verify event creation
			List<JobEvent> events = JobEventDao.getByJobIdAndStatus(job.getId(), job.getStatus());
			Assert.assertEquals(events.size(), 1,
					"Wrong number of events found. Expected " + 
					1 + ", found " + events.size());
			
			JobEvent event = events.get(0);
			Assert.assertEquals(
					event.getDescription(),
					newStatus.getDescription(),
					"Wrong event description found. Expected '"
							+ newStatus.getDescription() + "', found '"
							+ event.getDescription() + "'");

			
			if (jobStatus != newStatus && 
					(StringUtils.equals(notificatonEvent, newStatus.name()) || 
							StringUtils.equals(notificatonEvent, "*")))
			{
				int messageCount = getMessageCount(Settings.NOTIFICATION_QUEUE);
				Assert.assertEquals(messageCount, 1, "Wrong number of messages found");
				
				// verify notification message was sent
				MessageQueueClient client = MessageClientFactory.getMessageClient();
				Message queuedMessage = null;
				try {
					queuedMessage = client.pop(Settings.NOTIFICATION_TOPIC,
							Settings.NOTIFICATION_QUEUE);
				} catch (Throwable e) { 
					Assert.fail("Failed to remove message from the queue. Further tests will fail.", e);
				}
				finally {
					client.delete(Settings.NOTIFICATION_TOPIC,
							Settings.NOTIFICATION_QUEUE, queuedMessage.getId());
				}
				Assert.assertNotNull(queuedMessage,
						"Null message found on the queue");
				JsonNode json = mapper.readTree(queuedMessage.getMessage());
				Assert.assertEquals(notification.getUuid(), json.get("uuid").textValue(),
						"Notification message has wrong uuid");
				Assert.assertEquals(job.getStatus().name(), json.get("event").textValue(),
						"Notification message has wrong event");
			}
			else
			{
				// check for messages in the queue?
				Assert.assertEquals(getMessageCount(Settings.NOTIFICATION_QUEUE), 0, 
						"Messages found in the queue when none should be there.");
			}
		} catch (Exception e) {
			if (!shouldThrowException) {
				Assert.fail(message, e);
			}
		} finally {
			try { clearJobs(); } catch (Exception e) {}
		}
	}
	

	@DataProvider
	protected Object[][] updateStatusJobJobStatusTypeStringProvider()
	{
		List<Object[]> testData = new ArrayList<Object[]>();
		String customStatusMessage = "This is a different new status message, so the same status should update";
		for (JobStatusType currentStatus: JobStatusType.values())
		{
			for (JobStatusType newStatus: JobStatusType.values()) {
				testData.add(new Object[]{ currentStatus, newStatus, newStatus.getDescription(), null, false, 
						String.format("Status update from %s to %s same message should not throw an exception", currentStatus.name(), newStatus.name()) } );
				testData.add(new Object[]{ currentStatus, newStatus, newStatus.getDescription(), newStatus.name(), false, 
						String.format("Status update from %s to %s same message should not throw an exception", currentStatus.name(), newStatus.name()) } );
				testData.add(new Object[]{ currentStatus, newStatus, newStatus.getDescription(), "NOTAREALEVENT", false, 
						String.format("Status update from %s to %s same message should not throw an exception", currentStatus.name(), newStatus.name()) } );
				testData.add(new Object[]{ currentStatus, newStatus, newStatus.getDescription(), "*", false, 
						String.format("Status update from %s to %s same message should not throw an exception", currentStatus.name(), newStatus.name()) } );
				
				if (currentStatus.equals(newStatus)) 
				{
					testData.add(new Object[]{ currentStatus, newStatus, customStatusMessage, null, false, 
							String.format("Status update from %s to %s different message should not throw an exception", currentStatus.name(), newStatus.name()) } );
					testData.add(new Object[]{ currentStatus, newStatus, customStatusMessage, newStatus.name(), false, 
							String.format("Status update from %s to %s different message should not throw an exception", currentStatus.name(), newStatus.name()) } );
					testData.add(new Object[]{ currentStatus, newStatus, customStatusMessage, "NOTAREALEVENT", false, 
							String.format("Status update from %s to %s different message should not throw an exception", currentStatus.name(), newStatus.name()) } );
					testData.add(new Object[]{ currentStatus, newStatus, customStatusMessage, "*", false, 
							String.format("Status update from %s to %s different message should not throw an exception", currentStatus.name(), newStatus.name()) } );
					
				}
			}	
		}
		return testData.toArray(new Object[][]{});
	}
	
	/**
	 * Tests that the job status is updated when a new status or message value is given. Also verifies a message
	 * is added to the queue if a notification for the job status is set. 
	 * @param jobStatus
	 * @param newStatus
	 * @param statusMessage
	 * @param addNotification
	 * @param shouldThrowException
	 * @param message
	 */
	@Test(dataProvider="updateStatusJobJobStatusTypeStringProvider", enabled=false)
	public void updateStatusJobJobStatusTypeString(JobStatusType jobStatus, JobStatusType newStatus, String statusMessage, String notificatonEvent, boolean shouldThrowException, String message) 
	{
		Job job = null;
		try 
		{
			NotificationDao notificationDao = new NotificationDao();
			
			job = createJob(jobStatus);
			JobDao.persist(job);
			
			Notification notification = null;
			if (!StringUtils.isEmpty(notificatonEvent)) {
				notification = new Notification(job.getUuid(), job.getOwner(), notificatonEvent, "http://example.com", false);
				notificationDao.persist(notification);
			}
			
			JobManager.updateStatus(job, newStatus, statusMessage);

			// verify status update
			Assert.assertEquals(job.getStatus(), newStatus,
					"Job status did not update after status update.");
			Assert.assertEquals(job.getErrorMessage(), statusMessage,
					"Job description did not update after status update.");

			// verify event creation
			List<JobEvent> events = JobEventDao.getByJobIdAndStatus(job.getId(), job.getStatus());
			int expectedEvents = (jobStatus.equals(newStatus) && !jobStatus.getDescription().equals(statusMessage)) ? 2 : 1;
			Assert.assertEquals(expectedEvents, events.size(),
					"Wrong number of events found. Expected " + expectedEvents + ", found " + events.size());

			// this test will fail if the events do not come back ordered by created asc 
			JobEvent event = events.get(events.size() -1);
			Assert.assertEquals(event.getDescription(), statusMessage,
					"Wrong event description found. Expected '" + statusMessage
							+ "', found '" + event.getDescription() + "'");

			if (!(jobStatus.equals(newStatus) && jobStatus.getDescription().equals(statusMessage)) && 
					(StringUtils.equals(notificatonEvent, newStatus.name()) || StringUtils.equals(notificatonEvent, "*"))) 
			{
				// verify notification message was sent
				MessageQueueClient client = MessageClientFactory.getMessageClient();
				Message queuedMessage = null;
				try {
					queuedMessage = client.pop(Settings.NOTIFICATION_TOPIC,
							Settings.NOTIFICATION_QUEUE);
				} catch (Throwable e) { 
					Assert.fail("Failed to remove message from the queue. Further tests will fail.", e);
				}
				finally {
					client.delete(Settings.NOTIFICATION_TOPIC,
							Settings.NOTIFICATION_QUEUE, queuedMessage.getId());
				}
				Assert.assertNotNull(queuedMessage,
						"Null message found on the queue");
				JsonNode json = mapper.readTree(queuedMessage.getMessage());
				Assert.assertEquals(notification.getUuid(), json.get("uuid").textValue(),
						"Notification message has wrong uuid");
				Assert.assertEquals(job.getStatus().name(), json.get("event").textValue(),
						"Notification message has wrong event");
			}
			else
			{
				// check for messages in the queue?
				Assert.assertEquals(getMessageCount(Settings.NOTIFICATION_QUEUE), 0, 
						"Messages found in the queue when none should be there.");
			}
		} catch (Exception e) {
			if (!shouldThrowException) {
				Assert.fail(message, e);
			}
		}
		finally {
			try { clearJobs(); } catch (Exception e) {}
		}
	}
	
	/*********************************************************************************
	 * 								NOT YET IMPLEMENTED
	 *********************************************************************************/
	
	
	@Test(enabled=false)
	public void checkStatus(Job job, boolean shouldThrowException, String message) {
		throw new RuntimeException("Test not implemented");
	}

	@Test(enabled=false)
	public void archive() {
		throw new RuntimeException("Test not implemented");
	}
	

}
