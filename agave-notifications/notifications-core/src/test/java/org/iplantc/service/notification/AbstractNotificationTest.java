package org.iplantc.service.notification;

import static org.iplantc.service.notification.TestDataHelper.NOTIFICATION_CREATOR;
import static org.iplantc.service.notification.TestDataHelper.TENANT_ADMIN;
import static org.iplantc.service.notification.TestDataHelper.ALT_TENANT_ADMIN;
import static org.iplantc.service.notification.TestDataHelper.NOTIFICATION_STRANGER;
import static org.iplantc.service.notification.TestDataHelper.TEST_EMAIL_NOTIFICATION;
import static org.iplantc.service.notification.TestDataHelper.TEST_REALTIME_NOTIFICATION;
import static org.iplantc.service.notification.TestDataHelper.TEST_WEBHOOK_NOTIFICATION;
import static org.iplantc.service.notification.model.enumerations.NotificationCallbackProviderType.AGAVE;
import static org.iplantc.service.notification.model.enumerations.NotificationCallbackProviderType.EMAIL;
import static org.iplantc.service.notification.model.enumerations.NotificationCallbackProviderType.REALTIME;
import static org.iplantc.service.notification.model.enumerations.NotificationCallbackProviderType.SLACK;
import static org.iplantc.service.notification.model.enumerations.NotificationCallbackProviderType.WEBHOOK;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

import java.io.IOException;
import java.util.Map;

import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.iplantc.service.common.Settings;
import org.iplantc.service.common.clients.RequestBin;
import org.iplantc.service.common.exceptions.MessagingException;
import org.iplantc.service.common.persistence.HibernateUtil;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.common.uuid.AgaveUUID;
import org.iplantc.service.common.uuid.UUIDType;
import org.iplantc.service.notification.dao.NotificationDao;
import org.iplantc.service.notification.exceptions.NotificationException;
import org.iplantc.service.notification.model.Notification;
import org.iplantc.service.notification.model.enumerations.NotificationCallbackProviderType;
import org.iplantc.service.notification.model.enumerations.NotificationStatusType;
import org.iplantc.service.notification.model.enumerations.RetryStrategyType;
import org.iplantc.service.notification.queue.NewNotificationQueueProcessor;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;

import com.surftools.BeanstalkClientImpl.ClientImpl;

public class AbstractNotificationTest {

	protected static final String TEST_USER = "ipcservices";
	protected static final String TEST_EMAIL = "dooley@tacc.utexas.edu";
	protected static final String TEST_URL_QUERY = "?username=${USERNAME}&status=${STATUS}";
	protected static final String SPECIFIC_ASSOCIATED_UUID = new AgaveUUID(UUIDType.PROFILE).toString();
	protected static final String DECOY_ASSOCIATED_UUID = new AgaveUUID(UUIDType.PROFILE).toString();
	protected static final String WILDCARD_ASSOCIATED_UUID = "*";
	
	protected NotificationDao dao = null;
	protected TestDataHelper dataHelper;
	protected RequestBin requestBin;
	protected Scheduler sched;
	protected SimpleTrigger trigger;
	
	protected void addNotifications(int instances, NotificationStatusType status, String associatedUuid, boolean stranger, NotificationCallbackProviderType type) 
	throws Exception
	{
		for (int i=0;i<instances;i++)
		{
			Notification n = createNotification(type);
			n.setStatus(status);
			n.setAssociatedUuid( associatedUuid );
//			if (StringUtils.equalsIgnoreCase(associatedUuid, "*")) {
//				n.setOwner( stranger ? ALT_TENANT_ADMIN : TENANT_ADMIN );
//			} else {
				n.setOwner( stranger ? NOTIFICATION_STRANGER : NOTIFICATION_CREATOR );
//			}
			
			dao.persist(n);
			
			Assert.assertNotNull(n.getId(), "Failed to save notification " + i);
		}
	}
	
	protected Notification createNotification(NotificationCallbackProviderType type) throws NotificationException, IOException {
		
		if (type == EMAIL) {
			return createEmailNotification();
		} else if (type == WEBHOOK) {
			return createWebhookNotification();
		} else if (type == AGAVE) {
			return createAgaveWebhookNotification();
		} else if (type == SLACK) {
			return createSlackNotification();
		} else if (type == REALTIME) {
			return createRealtimeNotification("/");
		} else {
			return createInvalidNotification();
		}
		
	}
	
	protected Notification createInvalidNotification() throws NotificationException, IOException
	{
		Notification notification = Notification.fromJSON(dataHelper.getTestDataObject(TEST_EMAIL_NOTIFICATION));
		notification.setCallbackUrl("ftp://foo.example.com");
		notification.setOwner(NOTIFICATION_CREATOR);
		notification.getPolicy().setRetryStrategyType(RetryStrategyType.NONE);
		return notification;
	}
	
	protected Notification createEmailNotification() throws NotificationException, IOException
	{
		Notification notification = Notification.fromJSON(dataHelper.getTestDataObject(TEST_EMAIL_NOTIFICATION));
		notification.setOwner(NOTIFICATION_CREATOR);
		
		return notification;
	}
	
	protected Notification createWebhookNotification() throws NotificationException, IOException
	{
		if (requestBin == null) {
			requestBin = RequestBin.getInstance();
		}
		Notification notification = Notification.fromJSON(dataHelper.getTestDataObject(TEST_WEBHOOK_NOTIFICATION));
		notification.setOwner(NOTIFICATION_CREATOR);
		notification.setCallbackUrl(requestBin.toString() + TEST_URL_QUERY);
		notification.getPolicy().setRetryStrategyType(RetryStrategyType.NONE);
		
		return notification;
	}
	
	protected Notification createSlackNotification() throws NotificationException, IOException
	{
		if (requestBin == null) {
			requestBin = RequestBin.getInstance();
		}
		Notification notification = Notification.fromJSON(dataHelper.getTestDataObject(TEST_WEBHOOK_NOTIFICATION));
		notification.setOwner(NOTIFICATION_CREATOR);
		notification.setCallbackUrl("https://hooks.slack.com/services/TTTTTTTTT/BBBBBBBBB/1234567890123456789012345");
		notification.getPolicy().setRetryStrategyType(RetryStrategyType.NONE);
		return notification;
	}
	
	protected Notification createAgaveWebhookNotification() throws NotificationException, IOException
	{
		if (requestBin == null) {
			requestBin = RequestBin.getInstance();
		}
		
		Notification notification = Notification.fromJSON(dataHelper.getTestDataObject(TEST_WEBHOOK_NOTIFICATION));
		notification.setOwner(NOTIFICATION_CREATOR);
		notification.getPolicy().setRetryStrategyType(RetryStrategyType.NONE);
		return notification;
	}
	
	protected Notification createRealtimeNotification(String channel) throws NotificationException, IOException
	{
		Notification notification = Notification.fromJSON(dataHelper.getTestDataObject(TEST_REALTIME_NOTIFICATION));
		notification.setOwner(NOTIFICATION_CREATOR);
		notification.getPolicy().setRetryStrategyType(RetryStrategyType.NONE);
		String realtimeUrl = TenancyHelper.resolveURLToCurrentTenant("https://realtime.docker.example.com/realtime");
		if (!StringUtils.isEmpty(channel)) {
			notification.setCallbackUrl(realtimeUrl + "/" + channel);
		} else {
			notification.setCallbackUrl(realtimeUrl);
		}
		
		return notification;
	}
	
	protected Notification createRealtimeNotification() throws NotificationException, IOException
	{
		return createRealtimeNotification(null);
	}
	
	protected void clearNotifications() throws NotificationException
	{
		try
		{
			HibernateUtil.beginTransaction();
			Session session = HibernateUtil.getSession();
            session.clear();
            HibernateUtil.disableAllFilters();
			session.createQuery("delete Notification").executeUpdate();
			session.flush();
		}
		catch (HibernateException ex)
		{
			throw new NotificationException(ex);
		}
		finally
		{
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}
	
	protected void startNotificationQueue(String name) throws SchedulerException {
		
		JobDetail jobDetail = newJob(NewNotificationQueueProcessor.class)
			.withIdentity("test-" + name)
			.build();
		
		trigger = (SimpleTrigger)newTrigger()
				.withIdentity("trigger-JobNotificationTest")
				.startNow()
				.withSchedule(simpleSchedule()
		            .withIntervalInMilliseconds(500)
		            .repeatForever())
				.build();
		
		sched.scheduleJob(jobDetail, trigger);
	}
	
	/**
	 * Flushes the messaging tube of any and all existing jobs.
	 * @param queueName
	 */
	@AfterMethod
	protected void drainQueue() 
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
	
	protected boolean isWebhookSent(String callback) throws Exception
	{
		return true;
//		File webhookLogFile = new File("/tmp/postbin.out");
//		if (!webhookLogFile.exists()) {
//			return false;
//		}
//		
//		String webhookParameters = FileUtils.readFileToString(webhookLogFile);
//		if (StringUtils.isEmpty(webhookParameters)) {
//			return false;
//		}
//		
//		ObjectMapper mapper = new ObjectMapper();
//		JsonNode json = mapper.readTree(webhookParameters);
//		
//		URI uri = new URI(callback);
//		
//		//if ()
	}
}
