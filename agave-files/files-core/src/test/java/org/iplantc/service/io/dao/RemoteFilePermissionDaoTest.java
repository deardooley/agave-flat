package org.iplantc.service.io.dao;

import java.util.List;

import org.hibernate.HibernateException;
import org.iplantc.service.io.BaseTestCase;
import org.iplantc.service.io.model.JSONTestDataUtil;
import org.iplantc.service.io.model.LogicalFile;
import org.iplantc.service.io.model.enumerations.StagingTaskStatus;
import org.iplantc.service.systems.dao.SystemDao;
import org.iplantc.service.systems.model.StorageSystem;
import org.iplantc.service.transfer.dao.RemoteFilePermissionDao;
import org.iplantc.service.transfer.model.RemoteFilePermission;
import org.iplantc.service.transfer.model.enumerations.PermissionType;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class RemoteFilePermissionDaoTest extends BaseTestCase {

	private LogicalFile file;
	private SystemDao systemDao = new SystemDao();
	private StorageSystem system;
	
	@BeforeClass
	protected void beforeClass() throws Exception 
	{
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
	
	@BeforeMethod
	protected void setUp() throws Exception 
	{	
		file = new LogicalFile(username, system, httpUri, destPath);
		file.setStatus(StagingTaskStatus.STAGING_QUEUED);
		//LogicalFileDao.persist(file);
	}
	
	@AfterMethod
	protected void tearDown() throws Exception
	{
		clearLogicalFiles();
	}
	
	@AfterClass
	protected void afterClass() throws Exception 
	{
		clearSystems();
		clearLogicalFiles();
	}

	@Test
	public void persist() throws Exception{
		try {
			LogicalFileDao.persist(file);
			RemoteFilePermission pem = new RemoteFilePermission(file.getId(), SYSTEM_OWNER, "", PermissionType.READ, true);
			RemoteFilePermissionDao.persist(pem);
			Assert.assertNotNull(pem.getId(), "Failed to save permission");
		} catch (HibernateException e) {
			Assert.fail("Persisting file permission should not throw an exception", e);
		}
	} 
	
	@Test(dependsOnMethods={"persist"})
	public void getBylogicalFileId() throws Exception{
		try {
			LogicalFileDao.persist(file);
			RemoteFilePermission pem = new RemoteFilePermission(file.getId(), SYSTEM_OWNER, "", PermissionType.READ, true);
			RemoteFilePermissionDao.persist(pem);
			Assert.assertNotNull(pem.getId(), "Failed to save permission");
			
			List<RemoteFilePermission> pems = RemoteFilePermissionDao.getBylogicalFileId(file.getId());
			Assert.assertNotNull(pems, "getBylogicalFileId No permissions retrieved.");
			Assert.assertTrue(pems.contains(pem), "getBylogicalFileId results did not contain the originally applied permissions");
		} catch (HibernateException e) {
			Assert.fail("getBylogicalFileId should not throw an exception", e);
		}
	} 
	
	@Test(dependsOnMethods={"persist"})
	public void getByUsernameAndlogicalFileId() throws Exception{
		try {
			LogicalFileDao.persist(file);
			RemoteFilePermission pem = new RemoteFilePermission(file.getId(), SYSTEM_OWNER, "", PermissionType.READ, true);
			RemoteFilePermissionDao.persist(pem);
			Assert.assertNotNull(pem.getId(), "Failed to save permission");
			
			RemoteFilePermission savedPem = RemoteFilePermissionDao.getByUsernameAndlogicalFileId(SYSTEM_OWNER, file.getId());
			Assert.assertNotNull(savedPem, "getByUsernameAndlogicalFileId No permission retrieved.");
			Assert.assertEquals(pem, savedPem, "getByUsernameAndlogicalFileId results did not contain the originally applied permissions");
		} catch (HibernateException e) {
			Assert.fail("getByUsernameAndlogicalFileId should not throw an exception", e);
		}
	} 
	
	@Test(dependsOnMethods={"getByUsernameAndlogicalFileId"})
	public void delete() throws Exception {
		try {
			LogicalFileDao.persist(file);
			RemoteFilePermission pem = new RemoteFilePermission(file.getId(), SYSTEM_OWNER, "", PermissionType.READ, true);
			RemoteFilePermissionDao.persist(pem);
			Assert.assertNotNull(pem.getId(), "Failed to save permission");
			
			RemoteFilePermissionDao.delete(pem);
			Assert.assertNull(RemoteFilePermissionDao.getByUsernameAndlogicalFileId(SYSTEM_OWNER, file.getId()), "System user permission was not removed.");
			
		} catch (HibernateException e) {
			Assert.fail("Removing file permission should not throw an exception", e);
		}
	} 
	
	@Test(dependsOnMethods={"persist", "delete"})
	public void deleteBylogicalFileId() throws Exception {
		try {
			LogicalFileDao.persist(file);
			RemoteFilePermission pem1 = new RemoteFilePermission(file.getId(), SYSTEM_OWNER, null, PermissionType.READ, true);
			RemoteFilePermissionDao.persist(pem1);
			Assert.assertNotNull(pem1.getId(), "Failed to save first permission");
			
			RemoteFilePermission pem2 = new RemoteFilePermission(file.getId(), SHARED_SYSTEM_USER, null, PermissionType.READ, true);
			RemoteFilePermissionDao.persist(pem2);
			Assert.assertNotNull(pem2.getId(), "Failed to save second permission");
			
			RemoteFilePermissionDao.deleteBylogicalFileId(file.getId());
			
			List<RemoteFilePermission> pems = RemoteFilePermissionDao.getBylogicalFileId(file.getId());
			Assert.assertNotNull(pems, "getBylogicalFileId No permissions retrieved.");
			Assert.assertTrue(pems.isEmpty(), "deleteBylogicalFileId did not remove all permissions for the logical file");			
		} catch (HibernateException e) {
			Assert.fail("Clearing file permissions should not throw an exception", e);
		}
	}
}
