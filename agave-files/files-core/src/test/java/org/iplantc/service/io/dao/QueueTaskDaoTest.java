package org.iplantc.service.io.dao;

import org.iplantc.service.io.BaseTestCase;
import org.iplantc.service.io.model.EncodingTask;
import org.iplantc.service.io.model.JSONTestDataUtil;
import org.iplantc.service.io.model.LogicalFile;
import org.iplantc.service.io.model.QueueTask;
import org.iplantc.service.io.model.StagingTask;
import org.iplantc.service.io.model.enumerations.StagingTaskStatus;
import org.iplantc.service.systems.dao.SystemDao;
import org.iplantc.service.systems.model.StorageSystem;
import org.testng.Assert;
import org.testng.AssertJUnit;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test(groups={"integration"})
public class QueueTaskDaoTest extends BaseTestCase {
	
	
	private StorageSystem system;
	private QueueTask task;
	private LogicalFile file;
	private SystemDao systemDao = new SystemDao();
	
	@BeforeClass
	protected void beforeClass() throws Exception {
		super.beforeClass();
		
		clearSystems();
		clearLogicalFiles();
		
		system = StorageSystem.fromJSON(jtd.getTestDataObject(JSONTestDataUtil.TEST_STORAGE_SYSTEM_FILE));
		system.setOwner(SYSTEM_OWNER);
		system.setPubliclyAvailable(true);
		system.setGlobalDefault(true);
		system.setAvailable(true);
		
		systemDao.persist(system);
	}
	
	@AfterClass
	protected void afterClass() throws Exception {
		clearSystems();
		clearLogicalFiles();
	}
	
	@BeforeMethod
	protected void setUp() throws Exception 
	{
		clearLogicalFiles();
		
		file = new LogicalFile(username, system, httpUri, destPath);
		file.setStatus(StagingTaskStatus.STAGING_QUEUED);
		LogicalFileDao.persist(file);
	}
	
	@AfterMethod
	protected void tearDown() throws Exception {
		clearLogicalFiles();
	}

	@Test
	public void testPersistNull() {
		try {
			QueueTaskDao.persist(null);
			Assert.fail("null task should throw an exception");
		} catch (Exception e) {
			// null task should throw an exception
		}
	}
	
//	@Test
//	public void testPersistStagingJobEventNull() {
//		task = new StagingTask(file);
//		try {
//			QueueTaskDao.persist(task);
//			AssertJUnit.assertNotNull("Null event in staging task should be persisted", ((StagingTask)task).getId());
//		} catch (Exception e) {
//			e.printStackTrace();
//			Assert.fail("Null event should not throw an exception");
//		}
//	}
	
//	@Test
//	public void testPersistTransformJobEventNull() {
//		task = new EncodingTask(file, system, destPath, destPath, "contrast.traits", "traits.pl");
//		try {
//			QueueTaskDao.persist(task);
//			AssertJUnit.assertNotNull("Null event in staging task should be persisted", ((EncodingTask)task).getId());
//		} catch (Exception e) {
//			e.printStackTrace();
//			Assert.fail("Null event should not throw an exception");
//		}
//	}
//	
//	@Test
//	public void testPersistTransformJobEventEmpty() {
//		task = new EncodingTask(file, system, destPath, destPath, "contrast.traits", "traits.pl");
//		try {
//			QueueTaskDao.persist(task);
//			AssertJUnit.assertNotNull("Empty event in staging task should be persisted", ((EncodingTask)task).getId());
//		} catch (Exception e) {
//			e.printStackTrace();
//			Assert.fail("Empty event should not throw an exception");
//		}
//	}
	
	@Test
	public void testPersistTransformJobTransformNull() {
		try {
			task = new EncodingTask(file, system, destPath, destPath, null, "traits.pl", file.getOwner());
			QueueTaskDao.persist(task);
			Assert.fail("Null handle in transform task should throw an exception");
		} catch (Exception e) {
			// Null transform in staging task should throw an exception
		}
	}
	
	@Test
	public void testPersistTransformJobTransformEmpty() {
		try {
			task = new EncodingTask(file, system, destPath, destPath, null, "traits.pl", file.getOwner());
			QueueTaskDao.persist(task);
			Assert.fail("Empty handle in transform task should throw an exception");
		} catch (Exception e) {
			// Empty transform in transform task should throw an exception
		}
	}
	
	@Test
	public void testPersistTransformJobFilterNull() {
		try {
			task = new EncodingTask(file, system, destPath, destPath, "contrast.traits", null, file.getOwner());
			QueueTaskDao.persist(task);
			Assert.fail("Null handle in transform task should throw an exception");
		} catch (Exception e) {
			// Null handle in staging task should throw an exception
		}
	}
	
	@Test
	public void testPersistTransformJobFilterEmpty() {
		try {
			task = new EncodingTask(file, system, destPath, destPath, "contrast.traits", "", file.getOwner());
			QueueTaskDao.persist(task);
			Assert.fail("Empty handle in transform task should throw an exception");
		} catch (Exception e) {
			// Empty handle in transform task should throw an exception
		}
	}
	
	@Test
	public void testPersistTransformJobFile() {
		task = new EncodingTask(file, system, destPath, destPath, "contrast.traits", "traits.pl", file.getOwner());
		try {
			QueueTaskDao.persist(task);
			AssertJUnit.assertNotNull("Transfer task should be persisted", ((EncodingTask)task).getId());
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail("Transfer task should be persisted");
		}
	}
	
	@Test
	public void testRemoveNull() {
		try {
			QueueTaskDao.remove(null);
			Assert.fail("null task should throw an exception");
		} catch (Exception e) {
			// null task should throw an exception
		}
	}
	
	@Test
	public void testGetNextStagingTask() {
		try {
			task = new StagingTask(file, file.getOwner());
			QueueTaskDao.persist(task);
			
			Long nextTask = QueueTaskDao.getNextStagingTask(null);
			AssertJUnit.assertNotNull("Next staging task should not be null", nextTask);
			
		} catch (Exception e) {
			Assert.fail("Retrieving next staging task should not throw an exception", e);
		}
	}
	
	@Test
	public void testGetNextStagingTaskForTenantOfLogicalFile() {
		try {
			task = new StagingTask(file, file.getOwner());
			QueueTaskDao.persist(task);
			
			Long nextTask = QueueTaskDao.getNextStagingTask(file.getTenantId());
			AssertJUnit.assertNotNull("Next staging task should not be null", nextTask);
			
		} catch (Exception e) {
			Assert.fail("Retrieving next staging task should not throw an exception", e);
		}
	}
	
	@Test
	public void testGetNextStagingTaskIsNullForTenantWithNoTask() {
		try {
			task = new StagingTask(file, file.getOwner());
			QueueTaskDao.persist(task);
			
			Long nextTask = QueueTaskDao.getNextStagingTask("asdfasdfasdfasd");
			AssertJUnit.assertNull("Next staging task should be null for tenant without any tasks", nextTask);
			
		} catch (Exception e) {
			Assert.fail("Retrieving next staging task should not throw an exception", e);
		}
	}

	@Test
	public void testGetNextTransformTask() {
		try {
			task = new EncodingTask(file, system, destPath, destPath, "contrast.traits", "traits.pl", file.getOwner());
			QueueTaskDao.persist(task);
			
			Long nextTask = QueueTaskDao.getNextTransformTask(null);
			AssertJUnit.assertNotNull("Next transform task should not be null", nextTask);
		} catch (Exception e) {
			Assert.fail("Retrieving next transform task should not throw an exception", e);
		}
	}
	
	@Test
	public void testGetNextTransformTaskForTenantOfLogicalFile() {
		try {
			task = new EncodingTask(file, system, destPath, destPath, "contrast.traits", "traits.pl", file.getOwner());
			QueueTaskDao.persist(task);
			
			Long nextTask = QueueTaskDao.getNextTransformTask(file.getTenantId());
			AssertJUnit.assertNotNull("Next transform task should not be null", nextTask);
		} catch (Exception e) {
			Assert.fail("Retrieving next transform task should not throw an exception", e);
		}
	}
	
	@Test
	public void testGetNextTransformTaskIsNullForTenantWithNoTask() {
		try {
			task = new EncodingTask(file, system, destPath, destPath, "contrast.traits", "traits.pl", file.getOwner());
			QueueTaskDao.persist(task);
			
			Long nextTask = QueueTaskDao.getNextTransformTask("asdasdfasdfa");
			AssertJUnit.assertNull("Next transform task should be null for tenant with no tasks", nextTask);
		} catch (Exception e) {
			Assert.fail("Retrieving next transform task should not throw an exception", e);
		}
	}

	@Test
	public void testGetTransformTaskByCallBackKeyNull() {
		try {
			QueueTaskDao.getTransformTaskByCallBackKey(null);
			Assert.fail("null callback key should throw an exception");
		} catch (Exception e) {
			// null callback key should throw an exception
		}
	}
	
	@Test
	public void testGetTransformTaskByCallBackKeyEmpty() {
		try {
			QueueTaskDao.getTransformTaskByCallBackKey("");
			Assert.fail("null callback key should throw an exception");
		} catch (Exception e) {
			// null callback key should throw an exception
		}
	}
	
	@Test
	public void testGetTransformTaskByCallBackKey() {
		try {
			task = new EncodingTask(file, system, destPath, destPath, "contrast.traits", "traits.pl", file.getOwner());
			QueueTaskDao.persist(task);
			
			EncodingTask transform = QueueTaskDao.getTransformTaskByCallBackKey(((EncodingTask)task).getCallbackKey());
			AssertJUnit.assertNotNull("Callback key should reference a persisted transform", transform);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail("Retrieving transform task by callback key should not throw an exception");
		}
	} 
}
