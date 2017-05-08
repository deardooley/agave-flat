package org.iplantc.service.notification.events;

import org.iplantc.service.common.clients.RequestBin;
import org.iplantc.service.common.uuid.AgaveUUID;
import org.iplantc.service.notification.events.MonitorNotificationEvent;
import org.iplantc.service.notification.model.Notification;
import org.iplantc.service.notification.model.NotificationAttempt;
import org.iplantc.service.tags.AbstractTagTest;
import org.iplantc.service.tags.model.Tag;
import org.iplantc.service.tags.model.enumerations.TagEventType;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.node.ArrayNode;

@Test(groups={"integration"})
public class TagNotificationEventTest extends AbstractTagTest
{
	@BeforeMethod
	public void beforeMethod() throws Exception {
		clearTags();
		clearNotifications();
		clearQueues();
	}

	@AfterMethod
	public void afterMethod() throws Exception{
		clearTags();
		clearNotifications();
		clearQueues();
	}
	
	@Test
	public void processWebhookNotificationEvent() 
	{
		try
		{
			Tag tag = createTag();
			dao.persist(tag);
			
			RequestBin requestBin = RequestBin.getInstance();
			
			Notification notification = new Notification(tag.getUuid(), tag.getOwner(), TagEventType.UPDATED.name(), requestBin.toString() + "?name=${TAG_NAME}&status=${EVENT}", false);
			EventFilter event = new TagNotificationEvent(new AgaveUUID(tag.getUuid()), notification, TagEventType.UPDATED.name(), tag.getOwner());
			event.setCustomNotificationMessageContextData(tag.toJSON().toString());
			NotificationAttempt attempt = NotificationMessageProcessor.createNotificationAttemptFromEvent(event);
			NotificationAttemptProcessor processor = new NotificationAttemptProcessor(attempt);
			
			processor.fire();
			
			NotificationAttempt processedAttempt = processor.getAttempt();
			
			Assert.assertTrue(processedAttempt.isSuccess(), "Email notification attempt should not fail.");
			Assert.assertTrue(notification.isSuccess(), "Notification failed to update to true after sending");
			
			Assert.assertEquals(((ArrayNode)requestBin.getRequests()).size(), 1, "Requestbin should have 1 request after monitor fires.");
		} 
		catch (Exception e) {
			Assert.fail("Test failed unexpectedly");
		}
	}

	//@Test
	public void processEmailNotificationEvent() 
	{
		try
		{
			Tag tag = createTag();
			dao.persist(tag);
			Notification notification = new Notification(tag.getUuid(), tag.getOwner(), "RESULT_CHANGE", "dooley@tacc.utexas.edu", false);
			MonitorNotificationEvent event = new MonitorNotificationEvent(new AgaveUUID(tag.getUuid()), notification, "RESULT_CHANGE", tag.getOwner());
			event.setCustomNotificationMessageContextData(tag.toJSON().toString());
			
			NotificationAttempt attempt = NotificationMessageProcessor.createNotificationAttemptFromEvent(event);
			NotificationAttemptProcessor processor = new NotificationAttemptProcessor(attempt);
			
			processor.fire();
			
			NotificationAttempt processedAttempt = processor.getAttempt();
			
			Assert.assertTrue(processedAttempt.isSuccess(), "Email notification attempt should not fail.");
			Assert.assertTrue(notification.isSuccess(), "Notification failed to update to true after sending");
		} 
		catch (Exception e) {
			Assert.fail("Test failed unexpectedly");
		}
	}
}
