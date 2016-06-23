package org.iplantc.service.monitor.queue;

import org.iplantc.service.monitor.AbstractMonitorTest;
import org.iplantc.service.monitor.Settings;
import org.iplantc.service.monitor.exceptions.MonitorException;
import org.iplantc.service.monitor.model.Monitor;
import org.iplantc.service.systems.model.RemoteSystem;
import org.iplantc.service.systems.model.enumerations.SystemStatusType;
import org.joda.time.DateTime;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test(singleThreaded=true)
public class MonitorCronListenerTest extends AbstractMonitorTest 
{	
	@BeforeMethod
	protected void beforeMethod() throws Exception {
		clearMonitors();
		clearNotifications();
		clearQueues();
		initSystems();
	}
	
	@AfterMethod
	protected void afterMethod() throws Exception{
		clearMonitors();
		clearNotifications();
		clearQueues();
		clearSystems();
	}

	@SuppressWarnings("unused")
	@Test
	public void execute() 
	{
		MonitorCronListener listener = null;
		try 
		{
			Monitor storageMonitor = createAndSavePendingStorageMonitor();
			Monitor executionMonitor = createAndSavePendingExecutionMonitor();

			listener = new MonitorCronListener();

			listener.execute(null);

			// both monitor tasks should be in the message queue now
			int monitorQueueSize = getMessageQueueSize(Settings.MONITOR_QUEUE);
			
			Assert.assertEquals(monitorQueueSize, 2,
					"Incorrect number of monitors found in queue.");
			
			for(Monitor processedMonitor: dao.getAll())
			{
				Assert.assertEquals(processedMonitor.getNextUpdateTime().getTime(), 
						new DateTime(processedMonitor.getLastUpdated()).plusMinutes(processedMonitor.getFrequency()).getMillis(),
						"Monitor next processing time was not updated during cron processing");
			}
		} catch (MonitorException e) {
			Assert.fail("Failed to process monitor message queue.", e);
		} catch (Throwable e) {
			Assert.fail("Unexpected exception thrown", e);
		} 
	}
}
