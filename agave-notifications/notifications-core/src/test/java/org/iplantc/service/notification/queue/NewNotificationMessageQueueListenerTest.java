package org.iplantc.service.notification.queue;

import java.io.IOException;
import java.lang.reflect.Method;

import org.iplantc.service.common.messaging.MessageClientFactory;
import org.iplantc.service.common.messaging.MessageQueueClient;
import org.iplantc.service.common.persistence.HibernateUtil;
import org.iplantc.service.notification.AbstractNotificationTest;
import org.iplantc.service.notification.Settings;
import org.iplantc.service.notification.TestDataHelper;
import org.iplantc.service.notification.dao.NotificationDao;
import org.iplantc.service.notification.exceptions.NotificationException;
import org.iplantc.service.notification.model.Notification;
import org.iplantc.service.notification.model.enumerations.NotificationStatusType;
import org.iplantc.service.notification.util.ServiceUtils;
import org.json.JSONException;
import org.quartz.JobExecutionContext;
import org.quartz.SchedulerException;
import org.quartz.impl.JobExecutionContextImpl;
import org.quartz.impl.StdSchedulerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Tests the listeners that pull messages off the notification queue and process
 * them.
 * 
 * @author dooley
 *
 */
public class NewNotificationMessageQueueListenerTest extends AbstractNotificationTest 
{

	@BeforeClass
	public void beforeClass()
	{
		try
		{
			dataHelper = TestDataHelper.getInstance();
			
			HibernateUtil.getConfiguration();
			
			dao = new NotificationDao();
			
			drainQueue();
			
			sched = StdSchedulerFactory.getDefaultScheduler();
			if (!sched.isStarted()) {
				sched.start();
			}
		}
		catch (Exception e)
		{	
			e.printStackTrace();
		}
	}

	@AfterClass
	public void afterClass() throws NotificationException, SchedulerException
	{
		clearNotifications();
		drainQueue();
		sched.clear();
		sched.shutdown();
	}
	
	

	@BeforeMethod
	public void beforeMethod(Method m) throws NotificationException, SchedulerException
	{
		clearNotifications();
		sched.clear();
		startNotificationQueue(m.getName());
	}
	
	@DataProvider(name="executeProvider")
	private Object[][] executeProvider() throws NotificationException, JSONException, IOException
	{
		Notification validEmail = createEmailNotification();
		Notification validURL = createWebhookNotification();
		Notification fourofourURL = createWebhookNotification();
		fourofourURL.setCallbackUrl("https://github.com/404");
		Notification fivehundredURL = createWebhookNotification();
		fivehundredURL.setCallbackUrl("https://github.com/500");
	
		return new Object[][] {
			{ validEmail, "Valid email address failed to send", true, false },
			{ validURL, "Valid url address failed to send", true, false },
			{ fourofourURL, "404 url should throw exception", false, true },
			{ fivehundredURL, "500 url should throw exception", false, true },
		};
	}
	
	@Test(dataProvider="executeProvider")
	public void execute(Notification notification, String errorMessage, boolean shouldSucceed, boolean shouldThrowException) 
	throws NotificationException
	{
		NewNotificationQueueProcessor listener = null;
		try
		{
			notification.setTenantId("foo.test");
			dao.persist(notification);
			
			Assert.assertNotNull(notification.getId(), "Failed to persist notification.");
			
			JobExecutionContext context = new JobExecutionContextImpl(null, null, null);
			listener = new NewNotificationQueueProcessor();
			listener.setContext(context);
			
			JsonNode json = new ObjectMapper().createObjectNode()
					.put("uuid", notification.getUuid())
					.put("event", "SENT")
					.put("tenant", notification.getTenantId())
					.put("owner", notification.getOwner());
			
			listener.processMessage(json.toString());
			
			notification = dao.findByUuidAcrossTenants(notification.getUuid());
			
			Assert.assertFalse(shouldThrowException, "Notification should have thrown an exception.");
			Assert.assertEquals(notification.getStatus() == NotificationStatusType.COMPLETE, shouldSucceed, "Notification was not a success.");
//			Assert.assertNotNull(notification.getLastSent(), "Notification last sent time was not updated.");
			if (shouldSucceed) {
				Assert.assertEquals(notification.getStatus(), NotificationStatusType.COMPLETE, "Notification status should be complete.");
//				Assert.assertEquals(notification.getResponseCode(), new Integer(200), 
//						"Response shuold be 200 on success.");
			} else {
				Assert.assertEquals(notification.getStatus(), NotificationStatusType.FAILED, "Notification status should be failed.");
//				Assert.assertNotEquals(notification.getResponseCode(), new Integer(200), 
//						"Response shuold not be 200 on failure.");
			}
//			Assert.assertEquals(notification.getAttempts().intValue(), 1, "Number of attempts was not incremented.");
			
			if (!ServiceUtils.isEmailAddress(notification.getCallbackUrl())) {
				Assert.assertTrue(isWebhookSent(notification.getCallbackUrl()));
			}
			
		}
		catch (Exception e) 
		{
			if (!shouldThrowException) {
				Assert.fail(errorMessage, e);
			} else {
				notification = dao.findByUuidAcrossTenants(notification.getUuid());
				
//				Assert.assertEquals(notification.getAttempts().intValue(), Settings.MAX_NOTIFICATION_RETRIES, 
//						"Invalid number of retries before failure.");
				Assert.assertFalse(notification.getStatus() == NotificationStatusType.FAILED, "Notification reported success, but failed.");
//				Assert.assertNotNull(notification.getLastSent(), "Notification last sent time was not updated on failure.");
//				Assert.assertNotEquals(notification.getResponseCode(), new Integer(200), 
//						"Response shuold not be 200 on failure.");
			}
		}
		finally {
			try { listener.stop(); } catch (Exception e) {}
		}
	}
	
	@Test(dataProvider="executeProvider", dependsOnMethods={"execute"})
	public void executeJobExecutionContext(Notification notification, String errorMessage, boolean shouldSucceed, boolean shouldThrowException) 
	throws NotificationException
	{
		MessageQueueClient messageClient = null;
		try
		{
			// create new messaging client
			messageClient = MessageClientFactory.getMessageClient();
						
			dao.persist(notification);
			
			Assert.assertNotNull(notification.getId(), "Failed to persist notification.");
			
			JsonNode json = new ObjectMapper().createObjectNode()
					.put("uuid", notification.getUuid())
					.put("event", notification.getEvent())
					.put("tenant", notification.getTenantId())
					.put("owner", notification.getOwner());
			
			messageClient.push(Settings.NOTIFICATION_TOPIC, Settings.NOTIFICATION_QUEUE, json.toString());
			try { messageClient.stop(); } catch (Exception e) {}
			
			Thread.sleep(3000);
			
			notification = dao.findByUuidAcrossTenants(notification.getUuid());
			
//			Assert.assertEquals(notification.isSuccess(), shouldSucceed, "Notification was not a success.");
//			Assert.assertNotNull(notification.getLastSent(), "Notification last sent time was not updated.");
//			if (shouldSucceed) {
//				Assert.assertEquals(notification.getResponseCode(), new Integer(200), 
//						"Response shuold be 200 on success.");
//				Assert.assertEquals(notification.getAttempts().intValue(), 1, "Number of attempts was not incremented.");
//			} else {
//				Assert.assertNotEquals(notification.getResponseCode(), new Integer(200), 
//						"Response shuold not be 200 on failure.");
//				Assert.assertEquals(notification.getAttempts().intValue(), Settings.MAX_NOTIFICATION_RETRIES, 
//						"Number of attempts was not incremented.");
//			}
			
			if (!ServiceUtils.isEmailAddress(notification.getCallbackUrl())) {
				Assert.assertTrue(isWebhookSent(notification.getCallbackUrl()));
			}
			
		}
		catch (Exception e) 
		{
			Assert.fail(errorMessage, e);
		}
		finally {
			try { messageClient.stop(); } catch (Exception e) {}
		}
	}
}