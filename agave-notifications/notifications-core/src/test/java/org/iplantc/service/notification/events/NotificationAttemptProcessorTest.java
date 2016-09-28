package org.iplantc.service.notification.events;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Date;

import org.iplantc.service.common.exceptions.UUIDException;
import org.iplantc.service.common.persistence.HibernateUtil;
import org.iplantc.service.common.uuid.AgaveUUID;
import org.iplantc.service.notification.AbstractNotificationTest;
import org.iplantc.service.notification.TestDataHelper;
import org.iplantc.service.notification.dao.NotificationDao;
import org.iplantc.service.notification.exceptions.DisabledNotificationException;
import org.iplantc.service.notification.exceptions.MissingNotificationException;
import org.iplantc.service.notification.exceptions.NotificationException;
import org.iplantc.service.notification.model.Notification;
import org.iplantc.service.notification.model.NotificationAttempt;
import org.iplantc.service.notification.model.NotificationAttemptResponse;
import org.iplantc.service.notification.model.enumerations.RetryStrategyType;
import org.iplantc.service.notification.providers.NotificationAttemptProvider;
import org.iplantc.service.notification.providers.email.EmailNotificationAttemptProvider;
import org.iplantc.service.notification.providers.http.clients.HttpWebhookClient;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class NotificationAttemptProcessorTest extends AbstractNotificationTest {

	@BeforeClass
	public void beforeClass()
	{
		try
		{
			dataHelper = TestDataHelper.getInstance();
			
			HibernateUtil.getConfiguration();
			
			dao = new NotificationDao();
		}
		catch (Exception e)
		{	
			e.printStackTrace();
		}
	}
	
	@AfterClass
	public void afterClass() throws NotificationException, IOException
	{
		clearNotifications();
//		clearDeadLetterQueue();
	}
	
	@DataProvider(name = "fireProvider")
	private Object[][] fireProvider() throws Exception
	{
		Notification validEmail = createEmailNotification();
		Notification validURL = createWebhookNotification();
		Notification validPort = createWebhookNotification();
		validPort.setCallbackUrl(requestBin.toString().replace("requestb.in", "requestb.in:80") + TEST_URL_QUERY);
	
		
		return new Object[][] {
			{ validEmail, "Valid email address failed to send", true },
			{ validURL, "Valid url address failed to send", true },
			{ validPort, "Valid url with port failed to send", true },
		};
	}
	
  @Test()
  public void fire() throws DisabledNotificationException, NotificationException, MissingNotificationException, IOException, UUIDException {
	 
    Notification notification = createEmailNotification();
    notification.getPolicy().setSaveOnFailure(false);
    dao.persist(notification);
    
    AgaveUUID uuid = new AgaveUUID(notification.getAssociatedUuid());
	
    EventFilter event = 
			EventFilterFactory.getInstance(uuid, notification, "SENT", TEST_USER);
	
	event.setCustomNotificationMessageContextData(notification.toJSON());
	
	NotificationAttempt attempt = spy(NotificationMessageProcessor.createNotificationAttemptFromEvent(event));
	
    // build successful notification attempt
    
    NotificationAttemptProcessor processor = spy(new NotificationAttemptProcessor(attempt));
    
    // mock the actual remote call
    NotificationAttemptProvider provider = mock(EmailNotificationAttemptProvider.class);
	when(provider.publish()).thenReturn(new NotificationAttemptResponse(200, "OK success"));
	doReturn(provider).when(processor).getNotificationAttemptProvider();
    
	boolean fireResult = processor.fire();
    
    verify(provider).publish();
    verify(processor).isNotificationStillActive();
    verify(attempt).setStartTime(any(Date.class));
    verify(attempt).setEndTime(any(Date.class));
    verify(processor).handleSuccess();
    verify(attempt).setAttemptNumber(1);
    
    Assert.assertTrue(fireResult, "false was returned from message delivery when it should have succeeded.");
    
  }
  
  @Test()
  public void fireSavesNotificationWhenItFails() throws DisabledNotificationException, NotificationException, MissingNotificationException, IOException, UUIDException {
		 
	    Notification notification = createWebhookNotification();
	    notification.setCallbackUrl("http://httpbin.agaveapi.co/status/500");
	    notification.getPolicy().setRetryStrategyType(RetryStrategyType.DELAYED);
	    notification.getPolicy().setRetryDelay(5);
	    notification.getPolicy().setRetryRate(10);
	    notification.getPolicy().setRetryLimit(3);
	    notification.getPolicy().setSaveOnFailure(true);
	    
	    dao.persist(notification);
	    
	    AgaveUUID uuid = new AgaveUUID(notification.getAssociatedUuid());
		
	    EventFilter event = 
				EventFilterFactory.getInstance(uuid, notification, "SENT", TEST_USER);
		
		event.setCustomNotificationMessageContextData(notification.toJSON());
		
		NotificationAttempt attempt = spy(NotificationMessageProcessor.createNotificationAttemptFromEvent(event));
		
		
		// build successful notification attempt
	    NotificationAttemptProcessor processor = spy(new NotificationAttemptProcessor(attempt));
	    
	    // mock the actual remote call
	    NotificationAttemptProvider provider = mock(HttpWebhookClient.class);
		when(provider.publish()).thenReturn(new NotificationAttemptResponse(500, "INTERNAL SERVER ERROR"));
		doReturn(provider).when(processor).getNotificationAttemptProvider();
	    
		
	    boolean fireResult = processor.fire();
	    
	    verify(processor, times(2)).fire();
	    verify(processor, times(2)).isNotificationStillActive();
	    verify(attempt, times(2)).setStartTime(any(Date.class));
	    verify(attempt, times(2)).setEndTime(any(Date.class));
	    verify(processor).handleFailure(attempt);
	    verify(processor).pushNotificationAttemptToRetryQueue(attempt, 5);
	    verify(attempt).setAttemptNumber(1);
	    
	    Assert.assertFalse(fireResult, "true was returned from message delivery when it should have failed.");
	    
	  }

//  @Test
//  public void getAttempt() {
//    throw new RuntimeException("Test not implemented");
//  }
//
//  @Test
//  public void getNotification() {
//    throw new RuntimeException("Test not implemented");
//  }
//
//  @Test
//  public void handleCancelledNotification() {
//    throw new RuntimeException("Test not implemented");
//  }
//
//  @Test
//  public void handleFailure() {
//    throw new RuntimeException("Test not implemented");
//  }
//
//  @Test
//  public void handlePolicyViolation() {
//    throw new RuntimeException("Test not implemented");
//  }
//
//  @Test
//  public void handleSuccess() {
//    throw new RuntimeException("Test not implemented");
//  }
//
//  @Test
//  public void pushNotificationAttemptToRetryQueue() {
//    throw new RuntimeException("Test not implemented");
//  }
//
//  @Test
//  public void saveFailedAttempt() {
//    throw new RuntimeException("Test not implemented");
//  }
//
//  @Test
//  public void setAttempt() {
//    throw new RuntimeException("Test not implemented");
//  }
//
//  @Test
//  public void setNotification() {
//    throw new RuntimeException("Test not implemented");
//  }
//
//  @Test
//  public void validateActiveNotification() {
//    throw new RuntimeException("Test not implemented");
//  }
}
