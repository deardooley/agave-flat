package org.iplantc.service.monitor.dao;

import java.util.List;

import org.iplantc.service.monitor.AbstractMonitorTest;
import org.iplantc.service.monitor.TestDataHelper;
import org.iplantc.service.monitor.exceptions.MonitorException;
import org.iplantc.service.monitor.model.Monitor;
import org.joda.time.DateTime;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

@Test(singleThreaded=true, groups={"integration"})
public class MonitorDaoTest extends AbstractMonitorTest 
{
	private int totalMonitors = 6;
	
	private int totalActiveMonitors = 3;
	private int totalActiveMonitorsForUser = 2;
	private int totalActiveMonitorsForStranger = 1;
	
	private int totalInactiveMonitors = 3;
	private int totalInactiveMonitorsForUser = 2;
	private int totalInactiveMonitorsForStranger = 1;
	
	
	private void loadTestData() throws Exception
	{
		// totalActiveMonitorsForUser
		addMonitor(true, false, true, false);
		try { Thread.sleep(500); } catch (Exception e) {}
		addMonitor(true, false, false, false);
		try { Thread.sleep(500); } catch (Exception e) {}
		// totalActiveMonitorsForStranger
		addMonitor(true, true, true, false);
		try { Thread.sleep(500); } catch (Exception e) {}
		// totalInactiveMonitorsForUser
		addMonitor(false, false, true, true);
		try { Thread.sleep(500); } catch (Exception e) {}
		addMonitor(false, false, false, true);
		try { Thread.sleep(500); } catch (Exception e) {}
		// totalInactiveMonitorsForStranger
		addMonitor(false, true, true, true);
	}
	
	@AfterMethod
	protected void afterMethod() throws Exception{
		clearMonitors();
		clearNotifications();
		clearQueues();
	}

	@DataProvider(name="persistProvider")
	protected Object[][] persistProvider() throws Exception
	{
		return new Object[][] { 
				{ createStorageMonitor(), "Failed to persist monitor", false },
				{ createExecutionMonitor(), "Failed to persist monitor", false }
		};
	}
	
	@Test(dataProvider="persistProvider")
	public void persist(Monitor n, String errorMessage, boolean shouldThrowException)
	{
		try 
		{
			dao.persist(n);
			Assert.assertNotNull(n.getId(), "Failed to generate an monitor ID.");
		} 
		catch(Exception e) 
		{
			if (!shouldThrowException) {
				Assert.fail(errorMessage, e);
			}
		}
	}
	
	@Test(dependsOnMethods={"persist"})
	public void persistFailsOnDuplicateEntry()
	{
		try 
		{
			// add a valid monitor
			Monitor m1 = createStorageMonitor();
			dao.persist(m1);
			Assert.assertNotNull(m1.getId(), "Failed to generate an monitor ID.");
		} 
		catch (Exception e) {
			Assert.fail("Inserting a valid montior should not fail", e);
		}
		
		try {
			// now add a duplicate. this should fail
			Monitor m2 = createStorageMonitor();
			dao.persist(m2);
			Assert.fail("Duplicate monitors should not be allowed");
		} 
		catch(MonitorException e) {
			// happy path
		}
		catch(Exception e) {
			Assert.fail("A MonitorException should be thrown on duplicate entry failure.", e);
		}
	}
	
	@Test(dependsOnMethods={"persistFailsOnDuplicateEntry"})
	public void getAll()
	{
		try 
		{
			loadTestData();
			
			List<Monitor> notifs = dao.getAll();
			
			Assert.assertFalse(notifs.isEmpty(), "None of the " + totalMonitors + " test monitors found.");
			Assert.assertEquals(notifs.size(), totalMonitors, "Wrong number of monitors found. " +
					totalMonitors + " saved, " + notifs.size() + " found.");
		} 
		catch(Exception e) 
		{
			Assert.fail("Failed to retrieve monitors from the db.", e);
		}
	}
	
	@DataProvider(name="findByUuidProvider")
	protected Object[][] findByUuidProvider() throws Exception
	{
		return new Object[][] { 
				{ createStorageMonitor(), "Failed to find monitor by uuid", false },
				{ createExecutionMonitor(), "Failed to find monitor by uuid", false }
		};
	}
	
	@Test(dataProvider="findByUuidProvider", dependsOnMethods={"getAll"})
	public void findByUuid(Monitor n, String errorMessage, boolean shouldThrowException)
	{
		try 
		{
			dao.persist(n);
			Assert.assertNotNull(n.getId(), "Failed to generate an monitor ID.");
			
			String monitorUuid = n.getUuid();
			
			n = dao.findByUuid(monitorUuid);
			
			Assert.assertNotNull(n, "Notifiation was not found in db.");
		} 
		catch(Exception e) 
		{
			if (!shouldThrowException) {
				Assert.fail(errorMessage, e);
			}
		}
	}
	
	@DataProvider(name="deleteProvider")
	protected Object[][] deleteProvider() throws Exception
	{
		return new Object[][] { 
				{ createStorageMonitor(), "Failed to delete monitor", false },
				{ createExecutionMonitor(), "Failed to delete monitor", false }
		};
	}

	@Test(dataProvider="deleteProvider",dependsOnMethods={"findByUuid"})
	public void delete(Monitor n, String errorMessage, boolean shouldThrowException)
	{
		try 
		{
			dao.persist(n);
			Assert.assertNotNull(n.getId(), "Failed to generate an monitor ID.");
			
			String notifUuid = n.getUuid();
			
			n = dao.findByUuid(notifUuid);
			
			Assert.assertNotNull(n, "Notifiation was not found in db.");
			
			dao.delete(n);
			
			n = dao.findByUuid(notifUuid);
			
			Assert.assertNull(n, "Monitor was not found deleted from the db.");
		} 
		catch(Exception e) 
		{
			if (!shouldThrowException) {
				Assert.fail(errorMessage, e);
			}
		}
	}
	
	@Test(dependsOnMethods={"delete"})
	public void getActiveUserMonitors()
	{
		try 
		{
			loadTestData();
			
			// check we can pull monitors for primary user 
			List<Monitor> monitors = dao.getActiveUserMonitors(TEST_USER);
			
			Assert.assertFalse(monitors.isEmpty(), "None of the " + totalActiveMonitorsForUser + 
					" active test monitors for " + TEST_USER + " found.");
			Assert.assertEquals(monitors.size(), totalActiveMonitorsForUser, "Wrong number of monitors found for " +
					TEST_USER + ". " + totalActiveMonitorsForUser + " saved, " + monitors.size() + " found.");
			
			// check we can pull monitors for other user as well
			monitors = dao.getActiveUserMonitors(TestDataHelper.SYSTEM_SHARE_USER);
			
			Assert.assertFalse(monitors.isEmpty(), "None of the " + totalActiveMonitorsForStranger + 
					" active test monitors for " + TestDataHelper.SYSTEM_SHARE_USER + " found.");
			Assert.assertEquals(monitors.size(), totalActiveMonitorsForStranger, "Wrong number of active monitors found for " +
					TestDataHelper.SYSTEM_SHARE_USER + ". " + totalActiveMonitorsForStranger + " saved, " + monitors.size() + " found.");
		} 
		catch (Exception e) {
			Assert.fail("Failed to retrive active monitors for user", e);
		}
	}
	
	@Test(dependsOnMethods={"getActiveUserMonitors"})
	public void getActiveUserMonitorsOnSystem()
	{
		try 
		{
			loadTestData();
			
			// check we can pull monitors for primary user 
			List<Monitor> monitors = dao.getActiveUserMonitorsOnSystem(TEST_USER, privateStorageSystem);
			
			Assert.assertFalse(monitors.isEmpty(), "None of the " + 1 + 
					" active test monitors on " + privateStorageSystem.getSystemId() + " for " + TEST_USER + " found.");
			Assert.assertEquals(monitors.size(), 1, "Wrong number of monitors "
					+ "on " + privateStorageSystem.getSystemId() + " found for " +
					TEST_USER + ". " + 1 + " saved, " + monitors.size() + " found.");
			
			// check we can pull monitors for other user as well
			monitors = dao.getActiveUserMonitorsOnSystem(TestDataHelper.SYSTEM_SHARE_USER, sharedStorageSystem);
			
			Assert.assertFalse(monitors.isEmpty(), "None of the " + 1 + 
					" active test monitors for " + TestDataHelper.SYSTEM_SHARE_USER + " found.");
			Assert.assertEquals(monitors.size(), 1, "Wrong number of active monitors "
					+ "on " + sharedStorageSystem.getSystemId() + " found for " +
					TestDataHelper.SYSTEM_SHARE_USER + ". " + 1 + " saved, " + monitors.size() + " found.");
		} 
		catch (Exception e) {
			Assert.fail("Failed to retrive active monitors for user on system", e);
		}
	}
	
	@Test(dependsOnMethods={"getActiveUserMonitorsOnSystem"})
	public void getAllPendingActiveMonitor()
	{
		try 
		{
			loadTestData();
			
			// check we can pull monitors for other user as well
			List<Monitor> monitors = dao.getActiveUserMonitors(TEST_USER);
			for (Monitor monitor: monitors) {
				monitor.setNextUpdateTime(new DateTime().minusYears(1).toDate());
				dao.persist(monitor);
			}
			List<Monitor> pendingMonitors = dao.getAllPendingActiveMonitor();
			Assert.assertFalse(pendingMonitors.isEmpty(), "None of the " + monitors.size() + 
					" active pending monitors found.");
			Assert.assertEquals(pendingMonitors.size(), monitors.size(), "Wrong number of active pending monitors " + 
					"found. " + monitors.size() + " due, " + pendingMonitors.size() + " found.");
		} 
		catch (Exception e) {
			Assert.fail("Failed to retrive all pending monitors", e);
		}
	}
	
	@Test(dependsOnMethods={"getAllPendingActiveMonitor"})
	public void getNextPendingActiveMonitor()
	{
		try 
		{
			loadTestData();
			
			// check we can pull monitors for other user as well
			List<Monitor> monitors = dao.getActiveUserMonitors(TEST_USER);
			Monitor nextMonitor = monitors.get(0);
			nextMonitor.setNextUpdateTime(new DateTime().minusYears(1).toDate());
			dao.persist(nextMonitor);
			
			Monitor nextPendingMonitor = dao.getNextPendingActiveMonitor();
			Assert.assertNotNull(nextPendingMonitor, "No pending monitor found.");
			Assert.assertEquals(nextPendingMonitor.getUuid(), nextMonitor.getUuid(), "Wrong pending monitor found.");
		} 
		catch (Exception e) {
			Assert.fail("Failed to retrive next pending monitor", e);
		}
	}
}
