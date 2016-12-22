/**
 * 
 */
package org.iplantc.service.jobs.notifications;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang.math.NumberUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.apps.dao.SoftwareDao;
import org.iplantc.service.apps.model.SoftwareInput;
import org.iplantc.service.apps.model.SoftwareParameter;
import org.iplantc.service.common.Settings;
import org.iplantc.service.common.clients.RequestBin;
import org.iplantc.service.common.exceptions.MessagingException;
import org.iplantc.service.jobs.dao.AbstractDaoTest;
import org.iplantc.service.jobs.dao.JobDao;
import org.iplantc.service.jobs.exceptions.JobProcessingException;
import org.iplantc.service.jobs.managers.JobManager;
import org.iplantc.service.jobs.model.JSONTestDataUtil;
import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.jobs.model.enumerations.JobStatusType;
import org.iplantc.service.notification.dao.NotificationDao;
import org.iplantc.service.notification.model.Notification;
import org.iplantc.service.notification.model.enumerations.NotificationStatusType;
import org.iplantc.service.notification.queue.NewNotificationQueueProcessor;
import org.iplantc.service.transfer.exceptions.RemoteDataException;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobListener;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.KeyMatcher;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.surftools.BeanstalkClientImpl.ClientImpl;

/**
 * @author dooley
 *
 */
public class JobNotificationTest extends AbstractDaoTest
{
	private static final Logger log = Logger.getLogger(JobNotificationTest.class);
	
	private static String TEST_NOTIFICATION_EMAIL = "foo@example.com";
	private String TEST_NOTIFICATION_URL = "http://httpbin.org/status/200";
	private JSONTestDataUtil util;
	private NotificationDao notificationDao = new NotificationDao();
	private JobDao jobDao = new JobDao();
	private ObjectMapper mapper = new ObjectMapper();
	private Scheduler sched;
	private SimpleTrigger trigger;
	private RequestBin requestBin;
	
	final AtomicBoolean notificationProcessed = new AtomicBoolean(false);
	
	@BeforeClass
	public void beforeClass() throws Exception {
		super.beforeClass();
		stageRemoteSoftwareAssets();
        SoftwareDao.persist(software);
        drainQueue();
		
		startTestNotificationScheduler();
	}

	@AfterClass
	public void afterClass() throws Exception {
		clearJobs();
		clearSoftware();
		clearSystems();
		drainQueue();
		sched.clear();
		sched.shutdown();
	}
	
	@BeforeMethod
	public void beforeMethod(Method m) throws Exception {
		clearJobs();
		sched.clear();
		startNotificationQueue(m.getName());
	}
	
	/**
     * Fetches the custom quartz scheduler needed to manage notification processing.
     * @throws SchedulerException
     */
    private void startTestNotificationScheduler() throws SchedulerException {
        StdSchedulerFactory schedulerFactory = new StdSchedulerFactory();
        Properties props = new Properties();
        props.put("org.quartz.scheduler.instanceName", "AgaveNotificationScheduler");
        props.put("org.quartz.threadPool.threadCount", "1");
        props.put("org.quartz.jobStore.class", "org.quartz.simpl.RAMJobStore");
        props.put("org.quartz.plugin.shutdownhook.class", "org.quartz.plugins.management.ShutdownHookPlugin");
        props.put("org.quartz.plugin.shutdownhook.cleanShutdown", "true");
        props.put("org.quartz.scheduler.skipUpdateCheck","true");
        schedulerFactory.initialize(props);
        
        sched = schedulerFactory.getScheduler();
        sched.start();
    }
	
	private void startNotificationQueue(String name) throws SchedulerException {
		
		JobDetail jobDetail = newJob(NewNotificationQueueProcessor.class)
			.withIdentity("test-" + name)
			.build();
		
		trigger = (SimpleTrigger)newTrigger()
				.withIdentity("trigger-JobNotificationTest")
				.startNow()
				.withSchedule(simpleSchedule()
		            .withIntervalInMilliseconds(500)
		            .repeatForever()
		            .withMisfireHandlingInstructionNextWithExistingCount())
		        .build();
	        
        sched.scheduleJob(jobDetail, trigger);
        
        sched.getListenerManager().addJobListener(
            new JobListener() {

                @Override
                public String getName() {
                    return getClass().getSimpleName() + " Unit Test Listener";
                }

                @Override
                public void jobToBeExecuted(JobExecutionContext context) {
                    log.debug("working on a new notification event");                        
                }

                @Override
                public void jobExecutionVetoed(JobExecutionContext context) {
                    // no idea here
                }

                @Override
                public void jobWasExecuted(JobExecutionContext context, JobExecutionException e) {
                    notificationProcessed.set(true);
                }
                
            }, KeyMatcher.keyEquals(jobDetail.getKey())
        );
	}
	
	public String createRequestBin() throws IOException, RemoteDataException {
		requestBin = RequestBin.getInstance();
		return requestBin.toString() + "?status=${JOB_STATUS}&jobid=${JOB_ID}";
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
	public int getMessageCount(String queueName) throws MessagingException
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
	 * Creates a bare bones Job object.
	 * @return Job with minimal set of attributes.
	 * @throws JobProcessingException
	 */
	private Job createJob() throws JobProcessingException
	{
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode json = mapper.createObjectNode();
		
		try 
		{
			json.put("name", "processJsonJobWithNotifications");
			json.put("appId", software.getUniqueName());
			ObjectNode jsonInput = mapper.createObjectNode();
			for (SoftwareInput input: software.getInputs()) {
				jsonInput.put(input.getKey(), input.getDefaultValueAsJsonArray());
			}
			json.put("inputs", jsonInput);
			
			ObjectNode jsonParameter = mapper.createObjectNode();
			for (SoftwareParameter parameter: software.getParameters()) {
				jsonParameter.put(parameter.getKey(), parameter.getDefaultValueAsJsonArray());
			}
			json.put("parameters", jsonParameter);
		} catch (Exception e) {
			Assert.fail("Failed to read in software description to create json job object", e);
		}
		
		return JobManager.processJob(json, software.getOwner(), null);
	}

	@Test
	public void testJobSetStatusNotificationProcessed() throws Exception
	{
		Job job = createJob();
		job = JobManager.updateStatus(job, JobStatusType.QUEUED, JobStatusType.QUEUED.getDescription());
		Map<JobStatusType, String> nids = new HashMap<JobStatusType, String>();
		NotificationDao nDao = new NotificationDao();
		for(JobStatusType status: JobStatusType.values()) {
			Notification notification = new Notification(
					job.getUuid(), job.getOwner(), status.name(), TEST_NOTIFICATION_EMAIL, false);
			nDao.persist(notification);
			nids.put(status, notification.getUuid());
		}
		
		for(JobStatusType status: JobStatusType.values()) 
		{
		    notificationProcessed.set(false);
	        
			// update job status
			job = JobManager.updateStatus(job, status, "TEST: " + status.getDescription());
			
			// force the queue listener to fire. This should pull the job message off the queue and notifiy us
			//queueListener.execute(null);
			int i=0;
			while (!notificationProcessed.get() && i<3) {
			    Thread.sleep(1000);
			    i++;
            }
			
			List<Notification> notifications = notificationDao.getActiveForAssociatedUuidAndEvent(job.getUuid(), status.name());
			Assert.assertTrue(notifications.isEmpty(), "No notifications for job status " + status.name() + " should exist after a successful delivery");
			
			Notification n = nDao.findByUuidAcrossTenants(nids.get(status));
			Assert.assertEquals(n.getStatus(), NotificationStatusType.COMPLETE, "Message for status " + status.name() + " failed to send successfully.");
		}
	}
	
	@Test(dependsOnMethods={"testJobSetStatusNotificationProcessed"})
	public void testJobManagerUpdateStatusNotificationProcessed() throws Exception
	{
		Job job = createJob();
		job = JobManager.updateStatus(job, JobStatusType.PENDING, "We are just getting started");
		Map<JobStatusType, String> nids = new HashMap<JobStatusType, String>();
		NotificationDao nDao = new NotificationDao();
		for(JobStatusType status: JobStatusType.values()) {
			Notification notification = new Notification(
					job.getUuid(), job.getOwner(), status.name(), TEST_NOTIFICATION_EMAIL, false);
			nDao.persist(notification);
			nids.put(status, notification.getUuid());
		}
		
		for(JobStatusType status: JobStatusType.values()) 
		{
		    notificationProcessed.set(false);
            
            // update job status
			job = JobManager.updateStatus(job, status);
			
			int i=0;
            while (!notificationProcessed.get() && i<3) {
                Thread.sleep(1000);
                i++;
            }
            
            List<Notification> notifications = notificationDao.getActiveForAssociatedUuidAndEvent(job.getUuid(), status.name());
			Assert.assertTrue(notifications.isEmpty(), "No notifications for job status " + status.name() + " should exist after a successful delivery");
			
			Notification n = nDao.findByUuidAcrossTenants(nids.get(status));
			Assert.assertEquals(n.getStatus(), NotificationStatusType.COMPLETE, "Message for status " + status.name() + " failed to send successfully.");
		}
	}
	
	@Test(dependsOnMethods={"testJobManagerUpdateStatusNotificationProcessed"})
	public void testJobManagerUpdateStatusWithMessageEmailNotificationProcessed() throws Exception
	{
		Job job = createJob();
		job = JobManager.updateStatus(job, JobStatusType.PENDING, "We are just getting started");
		Map<JobStatusType, String> nids = new HashMap<JobStatusType, String>();
		NotificationDao nDao = new NotificationDao();
		for(JobStatusType status: JobStatusType.values()) {
			Notification notification = new Notification(
					job.getUuid(), job.getOwner(), status.name(), TEST_NOTIFICATION_EMAIL, false);
			nDao.persist(notification);
			nids.put(status, notification.getUuid());
		}
		
		for(JobStatusType status: JobStatusType.values()) 
		{
		    notificationProcessed.set(false);
            
            // update job status
			job = JobManager.updateStatus(job, status, "TEST: " + status.getDescription());
			
			int i=0;
            while (!notificationProcessed.get() && i<3) {
                Thread.sleep(1000);
                i++;
            }
            
            List<Notification> notifications = notificationDao.getActiveForAssociatedUuidAndEvent(job.getUuid(), status.name());
			Assert.assertTrue(notifications.isEmpty(), "No notifications for job status " + status.name() + " should exist after a successful delivery");
			
			Notification n = nDao.findByUuidAcrossTenants(nids.get(status));
			Assert.assertEquals(n.getStatus(), NotificationStatusType.COMPLETE, "Message for status " + status.name() + " failed to send successfully.");
		}
	}
	
	@Test(dependsOnMethods={"testJobManagerUpdateStatusWithMessageEmailNotificationProcessed"})
	public void testJobManagerUpdateStatusWithMessageURLNotificationProcessed() throws Exception
	{
		Job job = createJob();
		job = JobManager.updateStatus(job, JobStatusType.QUEUED, JobStatusType.QUEUED.getDescription());
		Map<JobStatusType, String> nids = new HashMap<JobStatusType, String>();
		NotificationDao nDao = new NotificationDao();
		for(JobStatusType status: JobStatusType.values()) {
			Notification notification = new Notification(
					job.getUuid(), job.getOwner(), status.name(), TEST_NOTIFICATION_URL, false);
			nDao.persist(notification);
			nids.put(status, notification.getUuid());
		}
		
		for(JobStatusType status: JobStatusType.values()) 
		{
		    notificationProcessed.set(false);
            
            // update job status
			job = JobManager.updateStatus(job, status, "TEST: " + status.getDescription());
			
			int i=0;
            while (!notificationProcessed.get() && i<3) {
                Thread.sleep(1000);
                i++;
            }
            
            List<Notification> notifications = notificationDao.getActiveForAssociatedUuidAndEvent(job.getUuid(), status.name());
			Assert.assertTrue(notifications.isEmpty(), "No notifications for job status " + status.name() + " should exist after a successful delivery");
			
			Notification n = nDao.findByUuidAcrossTenants(nids.get(status));
			Assert.assertEquals(n.getStatus(), NotificationStatusType.COMPLETE, "Message for status " + status.name() + " failed to send successfully.");
		}
	}
	
	@Test(dependsOnMethods={"testJobManagerUpdateStatusWithMessageURLNotificationProcessed"})
	public void testJobManagerUpdateStatusWithWildcardEmailNotificationProcessed() throws Exception
	{
		Job job = createJob();
		job = JobManager.updateStatus(job, JobStatusType.PENDING, "We are just getting started");
		
		NotificationDao nDao = new NotificationDao();
		
		for(JobStatusType status: JobStatusType.values()) 
		{
			Notification notification = new Notification(
					job.getUuid(), job.getOwner(), "*", TEST_NOTIFICATION_EMAIL, false);
			nDao.persist(notification);
			
		    notificationProcessed.set(false);
            
            // update job status
			job = JobManager.updateStatus(job, status, "TEST: " + status.getDescription());
			
			int i=0;
            while (!notificationProcessed.get() && i<3) {
                Thread.sleep(1000);
                i++;
            }
            
            List<Notification> notifications = notificationDao.getActiveForAssociatedUuidAndEvent(job.getUuid(), status.name());
			Assert.assertTrue(notifications.isEmpty(), "No notifications for job status " + status.name() + " should exist after a successful delivery");
			
			Notification n = nDao.findByUuidAcrossTenants(notification.getUuid());
			Assert.assertEquals(n.getStatus(), NotificationStatusType.COMPLETE, "Message for status " + status.name() + " failed to send successfully.");
		}
	}
	
	@Test(dependsOnMethods={"testJobManagerUpdateStatusWithWildcardEmailNotificationProcessed"})
	public void testJobManagerUpdateStatusWithWildcardURLNotificationProcessed() throws Exception
	{
		Job job = createJob();
		job = JobManager.updateStatus(job, JobStatusType.QUEUED, JobStatusType.QUEUED.getDescription());
		
		NotificationDao nDao = new NotificationDao();
		
		for(JobStatusType status: JobStatusType.values()) 
		{
			Notification notification = new Notification(
					job.getUuid(), job.getOwner(), "*", TEST_NOTIFICATION_URL, false);
			nDao.persist(notification);
			
		    notificationProcessed.set(false);
            
            // update job status
			job = JobManager.updateStatus(job, status, "TEST: " + status.getDescription());
			
			int i=0;
            while (!notificationProcessed.get() && i<3) {
                Thread.sleep(1000);
                i++;
            }
            
            List<Notification> notifications = notificationDao.getActiveForAssociatedUuidAndEvent(job.getUuid(), status.name());
			Assert.assertTrue(notifications.isEmpty(), "No notifications for job status " + status.name() + " should exist after a successful delivery");
			
			Notification n = nDao.findByUuidAcrossTenants(notification.getUuid());
			Assert.assertEquals(n.getStatus(), NotificationStatusType.COMPLETE, "Message for status " + status.name() + " failed to send successfully.");
		}
	}
}
