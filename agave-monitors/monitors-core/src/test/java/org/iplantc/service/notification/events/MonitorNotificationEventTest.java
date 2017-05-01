package org.iplantc.service.notification.events;

import org.iplantc.service.monitor.AbstractMonitorTest;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test(groups={"broken", "integration"})
public class MonitorNotificationEventTest extends AbstractMonitorTest
{
	@BeforeMethod
	public void beforeMethod() throws Exception {
		clearMonitors();
		clearNotifications();
		clearQueues();
	}

	@AfterMethod
	public void afterMethod() throws Exception{
		clearMonitors();
		clearNotifications();
		clearQueues();
	}
	
//	@Test
//	public void processWebhookNotificationEvent() 
//	{
//		try
//		{
//			Monitor monitor = createStorageMonitor();
//			dao.persist(monitor);
//			
//			RequestBin requestBin = RequestBin.getInstance();
//			
//			Notification notification = new Notification(monitor.getUuid(), monitor.getOwner(), "RESULT_CHANGE", requestBin.toString() + "?system=${TARGET}&status=${EVENT}", false);
//			MonitorNotificationEvent event = new MonitorNotificationEvent(new AgaveUUID(monitor.getUuid()), notification, "RESULT_CHANGE", monitor.getOwner());
//			event.setCustomNotificationMessageContextData(monitor.toJSON());
//			NotificationMessageProcessor.process(notification, "RESULT_CHANGE", monitor.getOwner(), monitor.getUuid(), monitor.toJSON());
//			
//			Assert.assertTrue(notification.isSuccess(), "Notification failed to update to true after sending");
//			
//			Assert.assertEquals(((ArrayNode)requestBin.getRequests()).size(), 1, "Requestbin should have 1 request after monitor fires.");
//		} 
//		catch (Exception e) {
//			Assert.fail("Test failed unexpectedly");
//		}
//	}

	//@Test
//	public void processEmailNotificationEvent() 
//	{
//		try
//		{
//			Monitor monitor = createStorageMonitor();
//			dao.persist(monitor);
//			Notification notification = new Notification(monitor.getUuid(), monitor.getOwner(), "RESULT_CHANGE", "dooley@tacc.utexas.edu", false);
//			MonitorNotificationEvent event = new MonitorNotificationEvent(new AgaveUUID(monitor.getUuid()), notification, "RESULT_CHANGE", monitor.getOwner());
//			event.setCustomNotificationMessageContextData(monitor.toJSON());
//			
//			NotificationMessageProcessor.process(notification, "RESULT_CHANGE", monitor.getOwner(), monitor.getUuid(), monitor.toJSON());
//			
//			Assert.assertTrue(notification.isSuccess(), "Notification failed to update to true after sending");
//		} 
//		catch (Exception e) {
//			Assert.fail("Test failed unexpectedly");
//		}
//	}
}
