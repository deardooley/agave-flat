package org.iplantc.service.monitor.queue;

import java.io.IOException;

import org.iplantc.service.common.messaging.MessageClientFactory;
import org.iplantc.service.common.messaging.clients.MessageQueueClient;
import org.iplantc.service.monitor.AbstractMonitorTest;
import org.iplantc.service.monitor.Settings;
import org.iplantc.service.monitor.dao.MonitorDao;
import org.iplantc.service.monitor.exceptions.MonitorException;
import org.iplantc.service.monitor.managers.MonitorManager;
import org.iplantc.service.monitor.model.Monitor;
import org.iplantc.service.monitor.model.MonitorCheck;
import org.iplantc.service.monitor.model.enumeration.MonitorStatusType;
import org.joda.time.DateTime;
import org.json.JSONException;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
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
@Test(groups={"broken"})
public class MonitorQueueListenerTest extends AbstractMonitorTest 
{
	protected MonitorManager manager = new MonitorManager();
	
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
	
	@DataProvider(name="processMessageProvider")
	private Object[][] processMessageProvider() throws MonitorException, JSONException, IOException
	{
		Monitor storageMonitor = createStorageMonitor();
		Monitor executionMonitor = createExecutionMonitor();
	
		return new Object[][] {
			{ storageMonitor, "Valid storage monitor failed", false },
			{ executionMonitor, "Valid execution monitor failed", false },
		};
	}
	
	@Test(dataProvider="processMessageProvider")
	public void processMessage(Monitor monitor, String errorMessage, boolean shouldThrowException)
	{
		MonitorQueueListener listener = null;
		try
		{
			dao.persist(monitor);
			manager.resetNextUpdateTime(monitor);
			
			Assert.assertNotNull(monitor.getId(), "Failed to persist monitor.");
			
			JsonNode json = new ObjectMapper().createObjectNode()
					.put("uuid", monitor.getUuid())
					.put("target", monitor.getSystem().getSystemId())
					.put("owner", monitor.getOwner());
			
			listener = new MonitorQueueListener();
			
			listener.processMessage(json.toString());
			
			MonitorCheck check = checkDao.getLastMonitorCheck(monitor.getId());
			
			Assert.assertNotNull(check, "No check found for monitor");
			Assert.assertEquals(check.getResult(), MonitorStatusType.PASSED, "Monitor check did not pass");
			Assert.assertTrue(monitor.isActive(), "Monitor is still active.");
			
			monitor = new MonitorDao().findByUuid(monitor.getUuid());
			
			Assert.assertNotEquals(monitor.getLastUpdated().getTime(), 
									monitor.getCreated().getTime(), 
									"Monitor last updated time was not updated.");
			
			// this is updated on the cron to avoid it being reset if a forced check occurs
//			Assert.assertTrue(monitor.getNextUpdateTime().getTime() >= 
//								new DateTime(monitor.getLastUpdated()).plusMinutes(monitor.getFrequency()).toDate().getTime(), 
//								"Monitor last sent time was not updated.");
		}
		catch (MonitorException e) 
		{
			if (!shouldThrowException) {
				Assert.fail(errorMessage, e);
			}
		}
		catch (Exception e) {
			Assert.fail("Unexpected exception thrown", e);
		}
		finally {
			try { listener.stop(); } catch (Exception e) {}
		}
		
	}
	
	@Test(dependsOnMethods={"processMessage"})
	public void execute()
	{
		MonitorQueueListener listener = null;
		MessageQueueClient messageClient = null;
		try
		{
			Monitor monitor = createAndSavePendingStorageMonitor();
			
			Assert.assertNotNull(monitor.getId(), "Failed to persist monitor.");
			
			messageClient = MessageClientFactory.getMessageClient();
			
			JsonNode json = new ObjectMapper().createObjectNode()
					.put("uuid", monitor.getUuid())
					.put("target", monitor.getSystem().getSystemId())
					.put("owner", monitor.getOwner());
					
			messageClient.push(Settings.MONITOR_TOPIC, Settings.MONITOR_QUEUE, json.toString());
			
			listener = new MonitorQueueListener();
			
			listener.execute(null);
			
			MonitorCheck check = checkDao.getLastMonitorCheck(monitor.getId());
			
			Assert.assertNotNull(check, "No check found for monitor");
			Assert.assertEquals(check.getResult(), MonitorStatusType.PASSED, "Monitor check did not pass");
			Assert.assertTrue(check.getMonitor().isActive(), "Monitor is still active.");
			
			Assert.assertNotEquals(check.getMonitor().getLastUpdated().getTime(), 
									check.getMonitor().getCreated().getTime(), 
									"Monitor last updated time was not updated.");

			// this is updated on the cron to avoid it being reset if a forced check occurs
//			Assert.assertTrue(check.getMonitor().getNextUpdateTime().getTime() >= 
//								new DateTime(check.getMonitor().getLastUpdated()).plusMinutes(check.getMonitor().getFrequency()).toDate().getTime(), 
//								"Monitor last sent time was not updated.");
		}
		catch (MonitorException e) 
		{
			Assert.fail("Failed to process monitor message queue.", e);
		}
		catch (Throwable e) {
			Assert.fail("Unexpected exception thrown", e);
		}
		finally {
			try { messageClient.stop(); } catch (Exception e) {}
			try { listener.stop(); } catch (Exception e) {}
		}
	}
}