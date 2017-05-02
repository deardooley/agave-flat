package org.iplantc.service.tags.managers;

import org.iplantc.service.tags.AbstractTagTest;
import org.iplantc.service.tags.TestDataHelper;
import org.iplantc.service.tags.exceptions.TagException;
import org.iplantc.service.tags.managers.TagManager;
import org.iplantc.service.tags.model.Tag;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

@Test(groups={"integration"})
public class TagPermissionManagerTest extends AbstractTagTest {

	@BeforeClass
	protected void beforeClass() throws Exception
	{
		super.beforeClass();
	}
	
	@AfterClass
	protected void afterClass() throws TagException {
		super.afterClass();
	}
	
	@BeforeMethod
	public void beforeMethod() throws Exception {
		
	}

	@AfterMethod
	public void afterMethod() throws Exception{
		clearTags();
	}
	
	@DataProvider
	private Object[][] addTagForUserProvider() throws Exception
	{	
		return new Object[][] {
			{ createTag(), "Tag should insert.", true },
		};
	}

	@Test(dataProvider = "addTagForUserProvider")
	public void addTagForUser(Tag tag, String errorMessage, boolean shouldThrowException)
	{
		try {
			dao.persist(tag);
			ObjectNode json = (ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_TAG);
			
			TagManager manager = new TagManager();
			Tag savedTag = manager.addTagForUser(json, TestDataHelper.TEST_USER);
			
			Assert.assertEquals(savedTag.getUuid(), tag.getUuid(), "Tag uuid should not change after saving");
			Assert.assertEquals(savedTag.getOwner(), tag.getOwner(), "Tag owner should not change after saving");
			Assert.assertFalse(shouldThrowException, errorMessage);
		}
		catch (Throwable e) {
			if (!shouldThrowException) {
				Assert.fail("Unexpected error occurred", e);
			}
		}
	}
	
//	@Test(dependsOnMethods={"check"})
//	public void checkNotificationsOnFailure()
//	{
//		try {
//			JsonNode storageJson = dataHelper.getTestDataObject(TestDataHelper.TEST_STORAGE_SYSTEM_FILE);
//			((ObjectNode)storageJson.get("storage")).put("host", "ssh.example.com");
//			((ObjectNode)storageJson).put("id", "checknotif.ssh.example.com");
//			
//			StorageSystem system = StorageSystem.fromJSON(new JSONObject(storageJson.toString()));
//			system.setOwner(TEST_USER);
//			systemDao.persist(system);
//			
//			Monitor monitor = createStorageMonitor();
//			monitor.setSystem(system);
//			dao.persist(monitor);
//			Assert.assertNotNull(monitor.getId(), "Failed to save monitor");
//			
//			NotificationDao notificationDao = new NotificationDao();
//			Notification n = new Notification(monitor.getUuid(), monitor.getOwner(), "STATUS_CHANGE", System.getProperty("user.name") + "@example.com", false);
//			notificationDao.persist(n);
//			
//			MonitorCheck check = MonitorManager.check(monitor);
//			Assert.assertEquals(check.getResult(), MonitorStatusType.FAILED, 
//					"Storage check on " + privateStorageSystem.getStorageConfig().getHost() + " should pass.");
//			
//			monitor = dao.findByUuid(monitor.getUuid());
//			Assert.assertNull(monitor.getLastSuccess(), 
//					"The test monitor's lastSuccess value should not be updated on a failed test.");
//			
//			// notification message for the event should have been placed in queue
//			Assert.assertEquals(getMessageQueueSize(Settings.NOTIFICATION_QUEUE),  1, 
//					"Invalid number of messages found on the notification queue after a failed monitor test.");
//		}
//		catch (MonitorException e) {
//			Assert.fail("Failed to process monitor check", e);
//		}
//		catch (Exception e) {
//			Assert.fail("Unexpected error occurred", e);
//		}
//	}
//	
//	@Test(dependsOnMethods={"checkNotificationsOnFailure"})
//	public void checkNotificationsOnSuccess()
//	{
//		try 
//		{
//			Monitor monitor = createStorageMonitor();
//			RemoteSystem system = monitor.getSystem();
//			system.setStatus(SystemStatusType.DOWN);
//			systemDao.persist(system);
//			
//			dao.persist(monitor);
//			Assert.assertNotNull(monitor.getId(), "Failed to save monitor");
//			
//			NotificationDao notificationDao = new NotificationDao();
//			Notification n = new Notification(monitor.getUuid(), monitor.getOwner(), "STATUS_CHANGE", System.getProperty("user.name") + "@example.com", false);
//			notificationDao.persist(n);
//		
//			MonitorCheck check = MonitorManager.check(monitor); // should succeed
//			Assert.assertEquals(check.getResult(), MonitorStatusType.PASSED, 
//					"Storage check on " + privateStorageSystem.getStorageConfig().getHost() + " should pass.");
//			
//			monitor = dao.findByUuid(monitor.getUuid());
//			Assert.assertTrue(Math.abs(monitor.getLastSuccess().getTime() - check.getCreated().getTime()) <= 1000, 
//					"The test monitor's lastSuccess value should be updated to the created date "
//					+ "of the last successfull check on success.");
//			
//			// notification message for the event should have been placed in queue
//			Assert.assertEquals(getMessageQueueSize(Settings.NOTIFICATION_QUEUE),  1, 
//					"Invalid number of messages found on the notification queue after a successful monitor test.");
//		}
//		catch (MonitorException e) {
//			Assert.fail("Failed to process monitor check", e);
//		}
//		catch (Exception e) {
//			Assert.fail("Unexpected error occurred", e);
//		}
//	}
}
