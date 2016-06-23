package org.iplantc.service.apps.managers;

import java.io.IOException;
import java.util.LinkedHashSet;

import org.iplantc.service.apps.dao.AbstractDaoTest;
import org.iplantc.service.apps.model.JSONTestDataUtil;
import org.iplantc.service.apps.model.Software;
import org.iplantc.service.apps.model.SoftwarePermission;
import org.iplantc.service.systems.dao.SystemDao;
import org.iplantc.service.systems.exceptions.SystemArgumentException;
import org.iplantc.service.systems.exceptions.SystemUnknownException;
import org.iplantc.service.systems.model.ExecutionSystem;
import org.iplantc.service.systems.model.RemoteSystem;
import org.iplantc.service.systems.model.StorageSystem;
import org.iplantc.service.systems.model.SystemRole;
import org.iplantc.service.systems.model.enumerations.RoleType;
import org.iplantc.service.transfer.model.enumerations.PermissionType;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class ApplicationManagerPermissionTest extends AbstractDaoTest {
	
	
	private static final String ADMIN_USER = "dooley";
	private static final String SYSTEM_OWNER = "sysowner";
	private static final String SYSTEM_SHARE_USER = "shareduser";
	private static final String SYSTEM_PUBLIC_USER = "publicuser";
	private static final String SYSTEM_UNSHARED_USER = "unshareduser";
	
	private Software softwarePrivateSystem;
	private Software softwarePublicSystem;
	private Software softwareSharedSystem;
	
	private Software privateSoftware;
	private Software publicSoftware;
	private Software sharedReadSoftware;
	private Software sharedWriteSoftware;
	private Software sharedAllSoftware;
	
	@BeforeClass
    public void beforeClass() throws Exception {
        super.beforeClass();

        softwarePrivateSystem = createSoftware(SYSTEM_OWNER, false, false, null, null);
        softwarePublicSystem = createSoftware(SYSTEM_OWNER, false, true, null, null);
        softwareSharedSystem = createSoftware(SYSTEM_OWNER, false, false, new SystemRole(SYSTEM_SHARE_USER, RoleType.ADMIN ), null);
            
        privateSoftware = createSoftware(SYSTEM_OWNER, true, false, null, null);
        publicSoftware = createSoftware(SYSTEM_OWNER, true, true, null, new SoftwarePermission(SYSTEM_SHARE_USER, PermissionType.READ));
        sharedReadSoftware = createSoftware(SYSTEM_OWNER, true, false, null, new SoftwarePermission(SYSTEM_SHARE_USER, PermissionType.READ));
        sharedWriteSoftware = createSoftware(SYSTEM_OWNER, true, false, null, new SoftwarePermission(SYSTEM_SHARE_USER, PermissionType.WRITE));
        sharedAllSoftware = createSoftware(SYSTEM_OWNER, true, false, null, new SoftwarePermission(SYSTEM_SHARE_USER, PermissionType.ALL));
    }
	
	@AfterClass
	public void tearDown() throws Exception 
	{	
		clearSystems();
	}
	
	private Software createSoftware(String owner, boolean shouldSystemBePublic, boolean shouldSoftwareBePublic, SystemRole userRole, SoftwarePermission userPermission) 
	throws SystemArgumentException, JSONException, IOException
	{
		SystemDao dao = new SystemDao();
		
		JSONObject systemJson = jtd.getTestDataObject(JSONTestDataUtil.TEST_EXECUTION_SYSTEM_FILE);
		
		ExecutionSystem existingSystem = (ExecutionSystem)dao.findBySystemId(systemJson.getString("id"));
		if (existingSystem == null) { 
			existingSystem =  ExecutionSystem.fromJSON(systemJson);
			existingSystem.setOwner(owner);
			existingSystem.setPubliclyAvailable(shouldSystemBePublic);
			dao.persist(existingSystem);
			existingSystem = (ExecutionSystem)dao.findById(existingSystem.getId());
		}
		
		ExecutionSystem testSystem = existingSystem.clone();
		testSystem.setOwner(owner);
		testSystem.setPubliclyAvailable(shouldSystemBePublic);
		testSystem.getRoles().clear();
        if (userRole != null) {
//        	userRole.setSystem(testSystem);
        	testSystem.getRoles().add(userRole);
        }
        
        JSONObject storageJson = jtd.getTestDataObject(JSONTestDataUtil.TEST_STORAGE_SYSTEM_FILE);
        StorageSystem storageSystem = (StorageSystem)dao.findBySystemId(storageJson.getString("id"));
		if (storageSystem == null) { 
			storageSystem = StorageSystem.fromJSON(storageJson, null);
			storageSystem.setOwner(owner);
			storageSystem.setPubliclyAvailable(shouldSystemBePublic);
			dao.persist(storageSystem);
			storageSystem = (StorageSystem)dao.findById(storageSystem.getId());
		}
        
        JSONObject appJson = jtd.getTestDataObject(JSONTestDataUtil.TEST_SOFTWARE_SYSTEM_FILE);
        appJson.put("executionHost", existingSystem.getSystemId());
        appJson.put("deploymentSystem", storageSystem.getSystemId());
        Software software = Software.fromJSON(appJson, owner);
        
        software.setExecutionSystem(testSystem);
        software.setOwner(owner);
        software.setPubliclyAvailable(shouldSoftwareBePublic);
        if (userPermission != null) {
        	LinkedHashSet<SoftwarePermission> pem = new LinkedHashSet<SoftwarePermission>();
        	pem.add(userPermission);
        	software.setPermissions(pem);
        }
        return software;
	}
	
	@BeforeMethod
	public void setUpMethod() throws Exception {
		
	}

	@DataProvider(name="isPublishableByUserProvider")
	public Object[][] isPublishableByUserProvider() throws Exception
	{
		return new Object[][] {
			{ softwarePrivateSystem, SYSTEM_OWNER, "Owner can publish an app on their system", true },
			{ softwarePrivateSystem, SYSTEM_PUBLIC_USER, "Guest cannot publish an app on a private system", false },
			{ softwarePrivateSystem, SYSTEM_SHARE_USER, "User cannot publish an app on a private system", false },
			{ softwarePublicSystem, SYSTEM_OWNER, "No one can publish an app on a public system", false },
			{ softwarePublicSystem, SYSTEM_PUBLIC_USER, "Guest cannot publish an app on a private system", false },
			{ softwarePublicSystem, SYSTEM_SHARE_USER, "User cannot publish an app on a private system", false },
			{ softwareSharedSystem, SYSTEM_UNSHARED_USER, "Unshared user cannot publish an app on a shared system", false },
		};
	}
	
	@Test(dataProvider = "isPublishableByUserProvider")
	public void isPublishableByUser(Software software, String username, String message, boolean userCanPublish) 
	throws SystemUnknownException
	{
		Assert.assertEquals(ApplicationManager.userCanPublish(username, software), userCanPublish, message);
	}
	
	@DataProvider(name="isManageableProvider")
	public Object[][] isManageableByUserProvider() throws Exception
	{
		return new Object[][] {
				{ privateSoftware, ADMIN_USER, "Admin can manage private software", true },
				{ privateSoftware, SYSTEM_OWNER, "User can manage their own software", true },
				{ privateSoftware, SYSTEM_SHARE_USER, "User cannot manage software not shared with them", false },
				{ privateSoftware, SYSTEM_PUBLIC_USER, "User cannot manage public software", false },
				
				{ sharedWriteSoftware, ADMIN_USER, "Admin can manage shared software not shared with them", true },
				{ sharedWriteSoftware, SYSTEM_SHARE_USER, "Shared user can manage software write shared with them", true },
				{ sharedReadSoftware, SYSTEM_SHARE_USER, "Shared user can manage software read shared with them", false },
				{ sharedAllSoftware, SYSTEM_SHARE_USER, "Shared user can manage software all shared with them", true },
				{ sharedWriteSoftware, SYSTEM_PUBLIC_USER, "Public user cannot manage shared software", false },
				
				{ publicSoftware, ADMIN_USER, "Admin can manage public software", true },
				{ publicSoftware, SYSTEM_OWNER, "User cannot manage public software they used to own", false },
				{ publicSoftware, SYSTEM_SHARE_USER, "Shared user cannot manage public software that was once shared with them", false },
				{ publicSoftware, SYSTEM_PUBLIC_USER, "Public user cannot manage public software", false },
		};
	}
	
	@Test(dataProvider = "isManageableProvider")
	public void isManageableTest(Software software, String username, String message, boolean userCanManager)
	{
		Assert.assertEquals(ApplicationManager.isManageableByUser(software, username), userCanManager, message);
	}
	
	@DataProvider(name="isVisibleProvider")
	public Object[][] isVisibleProvider() throws Exception
	{
		return new Object[][] {
				{ privateSoftware, ADMIN_USER, "Admin can view private software", true },
				{ privateSoftware, SYSTEM_OWNER, "User can view their own software", true },
				{ privateSoftware, SYSTEM_SHARE_USER, "User cannot view software not shared with them", false },
				{ privateSoftware, SYSTEM_PUBLIC_USER, "User cannot view public software", false },
				
				{ sharedWriteSoftware, ADMIN_USER, "Admin can view shared software not shared with them", true },
				{ sharedReadSoftware, SYSTEM_SHARE_USER, "Shared user can view software read shared with them", true },
				{ sharedWriteSoftware, SYSTEM_SHARE_USER, "Shared user can view software write shared with them", false },
				{ sharedAllSoftware, SYSTEM_SHARE_USER, "Shared user can view software all shared with them", true },
				{ sharedWriteSoftware, SYSTEM_PUBLIC_USER, "Public user cannot view shared software", false },
				
				{ publicSoftware, ADMIN_USER, "Admin can view public software", true },
				{ publicSoftware, SYSTEM_OWNER, "User cannot view public software they used to own", true },
				{ publicSoftware, SYSTEM_SHARE_USER, "Shared user can view public software that was once shared with them", true },
				{ publicSoftware, SYSTEM_PUBLIC_USER, "Public user cannot view public software", true },
		};
	}
	
	@Test(dataProvider = "isVisibleProvider")
	public void isVisibleByUser(Software software, String username, String message, boolean userCanView)
	{
		Assert.assertEquals(ApplicationManager.isVisibleByUser(software, username), userCanView, message);
	}

//	@Test(dataProvider = "cloneSystemProvider")
//	public void cloneSystem()
//	{
//		throw new RuntimeException("Test not implemented");
//	}
//
//	@DataProvider(name="makePublicProvider")
//	public Object[][] makePublicProvider() throws Exception
//	{
//		return new Object[][] {
//				{ privateSystem, "Original owner can see a public system" },
//		};
//	}
//				
//	@Test(dataProvider = "makePublicProvider")
//	public void makePublic(RemoteSystem system, String message)
//	{
//		RemoteSystem publicSystem = null;
//		try 
//		{
//			publicSystem = ApplicationManager.makePublic(system);
//			
//			Assert.assertTrue(publicSystem.isPubliclyAvailable(), "System is public");
//			Assert.assertTrue(ApplicationManager.isVisibleByUser(publicSystem, SYSTEM_PUBLIC_USER), "System is public");
//		} 
//		catch (Exception e) {
//			try {new SystemDao().remove(publicSystem); } catch (Exception e1) {}
//		}
//	}
//
//	@DataProvider(name="parseSystemProvider")
//	public Object[][] parseSystemProvider() throws Exception
//	{
//		return new Object[][] {
//				{ jtd.getTestDataObject(JSONSystemsTestData.TEST_EXECUTION_SYSTEM_FILE), ExecutionSystem.class, "Execution system is parsed from json" },
//				{ jtd.getTestDataObject(JSONSystemsTestData.TEST_STORAGE_SYSTEM_FILE), StorageSystem.class, "Storage system is parsed from json" },
//		};
//	}
//	
//	
//	@Test(dataProvider = "parseSystemProvider")
//	public void parseSystem(JSONObject json, Class expectedClass, String message)
//	{
//		RemoteSystem system = ApplicationManager.parseSystem(json, SYSTEM_OWNER, null);
//		
//		Assert.assertEquals(system.getOwner(), SYSTEM_OWNER, message);
//		
//		Assert.assertFalse(system.isPubliclyAvailable(), "System was published as private");
//		
//		Assert.assertEquals(system.getClass(), expectedClass, message);
//	}
}
