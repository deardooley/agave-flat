package org.iplantc.service.systems.manager;

import java.util.ArrayList;
import java.util.List;

import org.iplantc.service.common.exceptions.PermissionException;
import org.iplantc.service.common.uuid.UniqueId;
import org.iplantc.service.systems.Settings;
import org.iplantc.service.systems.dao.SystemDao;
import org.iplantc.service.systems.exceptions.SystemException;
import org.iplantc.service.systems.model.ExecutionSystem;
import org.iplantc.service.systems.model.JSONTestDataUtil;
import org.iplantc.service.systems.model.RemoteSystem;
import org.iplantc.service.systems.model.StorageSystem;
import org.iplantc.service.systems.model.SystemRole;
import org.iplantc.service.systems.model.SystemsModelTestCommon;
import org.iplantc.service.systems.model.enumerations.RoleType;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class SystemManagerDisableSystemTest extends SystemsModelTestCommon 
{	
	private SystemManager manager = new SystemManager();
	private SystemDao dao = new SystemDao();
	
	
	@BeforeClass
    public void beforeClass() throws Exception {
        super.beforeClass();
    }
	
	@AfterClass
    public void afterClass() throws Exception {
        clearSystems();
    }
    
	@BeforeMethod
	public void beforeMethod() throws Exception {

	}
	
	@AfterMethod
	public void afterMethod() throws Exception {
		clearSystems();
	}
	
    private RemoteSystem getPrivateStorageSystem() throws Exception {	
        RemoteSystem privateStorageSystem = StorageSystem.fromJSON( jtd.getTestDataObject(
                JSONTestDataUtil.TEST_STORAGE_SYSTEM_FILE));
        privateStorageSystem.setOwner(SYSTEM_OWNER);
        privateStorageSystem.setSystemId(new UniqueId().getStringId());
        return privateStorageSystem;
    }
    
    private RemoteSystem getPrivateExecutionSystem() throws Exception {   
        RemoteSystem privateExecutionSystem = ExecutionSystem.fromJSON( jtd.getTestDataObject(
                JSONTestDataUtil.TEST_EXECUTION_SYSTEM_FILE));
        privateExecutionSystem.setOwner(SYSTEM_OWNER);
        privateExecutionSystem.setSystemId(new UniqueId().getStringId());
        return privateExecutionSystem;
    }
    
    
	@DataProvider
	public Object[][] disableSharedSystemFailsWithoutAdminPrivilegesProvider() throws Exception
	{
		List<Object[]> testCases = new ArrayList<Object[]>();
		
		for (RoleType roleType: RoleType.values()) {
			testCases.add(new Object[]{ SYSTEM_SHARE_USER, 
										roleType , 
										(!roleType.canAdmin()), 
										"Shared user should be able to disable a system with " + roleType.name() + " role." });
		}
		
		return testCases.toArray(new Object[][]{});
	}
	
	@Test(dataProvider = "disableSharedSystemFailsWithoutAdminPrivilegesProvider")
	public void disableSharedSystemFailsWithoutAdminPrivileges(String callingUser, RoleType callingUserRole, boolean shouldThrowException, String message)
	{
		RemoteSystem sharedSystem = null;
		try 
		{
			sharedSystem = getPrivateExecutionSystem();
			sharedSystem.getRoles().add(new SystemRole(callingUser, callingUserRole));
			sharedSystem.setAvailable(true);
			
			dao.persist(sharedSystem);
			
			manager.disableSystem(sharedSystem, callingUser);
			
			Assert.assertFalse(shouldThrowException, message);
			
			Assert.assertFalse(sharedSystem.isAvailable(), message);
		} 
		catch (PermissionException e) {
			if (!shouldThrowException) {
				Assert.fail(message);
			}
		}
		catch (Exception e) {
			Assert.fail("PermissionException should be thrown when disabling a system without valid permissions", e);
		}
	}
	
	@DataProvider
	public Object[][] disablePublicSystemFailsWithoutTenantAdminPrivilegesProvider() throws Exception
	{
		List<Object[]> testCases = new ArrayList<Object[]>();
		
		for (RoleType roleType: RoleType.values()) {
			testCases.add(new Object[]{ SYSTEM_SHARE_USER, 
					roleType , 
					true, 
					"Only tenant admins should be able to undelete a public system role." });
			testCases.add(new Object[]{ SYSTEM_OWNER, 
					roleType , 
					true, 
					"Only tenant admins should be able to undelete a public system role." });
			
			testCases.add(new Object[]{ Settings.PUBLIC_USER_USERNAME, 
					roleType , 
					true, 
					"Only tenant admins should be able to undelete a public system role." });
			
			testCases.add(new Object[]{ Settings.WORLD_USER_USERNAME, 
					roleType , 
					true, 
					"Only tenant admins should be able to undelete a public system role." });
			
			testCases.add(new Object[]{ TENANT_ADMIN, 
					roleType , 
					false, 
					"Tenant admin should be able to undelete a system aregardless of assigned role." });
		}
		
		return testCases.toArray(new Object[][]{});
	}
	
	@Test(dataProvider = "disablePublicSystemFailsWithoutTenantAdminPrivilegesProvider")
	public void disablePublicStorageSystemFailsWithoutTenantAdminPrivileges(String callingUser, RoleType callingUserRole, boolean shouldThrowException, String message)
	{
		RemoteSystem sharedSystem = null;
		try 
		{
			sharedSystem = getPrivateStorageSystem();
			sharedSystem.getRoles().add(new SystemRole(callingUser, callingUserRole));
			sharedSystem.setPubliclyAvailable(true);
			sharedSystem.setAvailable(true);
			
			dao.persist(sharedSystem);
			
			manager.disableSystem(sharedSystem, callingUser);
			
			Assert.assertFalse(shouldThrowException, message);
			
			Assert.assertFalse(sharedSystem.isAvailable(), message);
		} 
		catch (PermissionException e) {
			if (!shouldThrowException) {
				Assert.fail(message);
			}
		}
		catch (Exception e) {
			Assert.fail("PermissionException should be thrown when disabling a system without valid permissions", e);
		}
	}
	
	
	
	@Test(dataProvider = "disablePublicSystemFailsWithoutTenantAdminPrivilegesProvider")
	public void disablePublicExecutionSystemFailsWithoutTenantAdminPrivileges(String callingUser, RoleType callingUserRole, boolean shouldThrowException, String message)
	{
		RemoteSystem sharedSystem = null;
		try 
		{
			sharedSystem = getPrivateExecutionSystem();
			sharedSystem.getRoles().add(new SystemRole(callingUser, callingUserRole));
			sharedSystem.setPubliclyAvailable(true);
			sharedSystem.setAvailable(true);
			
			dao.persist(sharedSystem);
			
			manager.disableSystem(sharedSystem, callingUser);
			
			Assert.assertFalse(shouldThrowException, message);
			
			Assert.assertFalse(sharedSystem.isAvailable(), message);
		} 
		catch (PermissionException e) {
			if (!shouldThrowException) {
				Assert.fail(message);
			}
		}
		catch (Exception e) {
			Assert.fail("PermissionException should be thrown when disabling a system without valid permissions", e);
		}
	}
			
	@DataProvider
	public Object[][] disableSharedSystemFailsForPublicAndWordUserProvider() throws Exception
	{
		List<Object[]> testCases = new ArrayList<Object[]>();
		
		for (RoleType roleType: RoleType.values()) {
			testCases.add(new Object[]{ 
					SYSTEM_UNSHARED_USER, 
					RoleType.NONE , 
					true, 
					"Unauthenticated users should never be able to disable a system." });
			
			testCases.add(new Object[]{ 
					Settings.PUBLIC_USER_USERNAME, 
					roleType, 
					true, 
					"public user should never be able to disable a system." });
			
			testCases.add(new Object[]{ 
					Settings.WORLD_USER_USERNAME, 
					roleType, 
					true, 
					"world user should never be able to disable a system." });
		}
		
		return testCases.toArray(new Object[][]{});
	}
	
	@Test(dataProvider = "disableSharedSystemFailsForPublicAndWordUserProvider")
	public void disableSharedStorageSystemFailsForPublicAndWordUser(String callingUser, RoleType callingUserRole, boolean shouldThrowException, String message)
	{
		RemoteSystem sharedSystem = null;
		try 
		{
			sharedSystem = getPrivateStorageSystem();
			sharedSystem.getRoles().add(new SystemRole(callingUser, callingUserRole));
			sharedSystem.setAvailable(true);
			
			dao.persist(sharedSystem);
			
			manager.disableSystem(sharedSystem, callingUser);
			
			Assert.assertFalse(shouldThrowException, message);
			
			Assert.assertFalse(sharedSystem.isAvailable(), message);
		} 
		catch (PermissionException e) {
			if (!shouldThrowException) {
				Assert.fail(message);
			}
		} catch (Exception e) {
			Assert.fail("PermissionException should be thrown when disabling a system without valid permissions", e);
		}
	}
	
	@Test(dataProvider = "disableSharedSystemFailsForPublicAndWordUserProvider")
	public void disableSharedExecutionSystemFailsForPublicAndWordUser(String callingUser, RoleType callingUserRole, boolean shouldThrowException, String message)
	{
		RemoteSystem sharedSystem = null;
		try 
		{
			sharedSystem = getPrivateExecutionSystem();
			sharedSystem.getRoles().add(new SystemRole(callingUser, callingUserRole));
			sharedSystem.setAvailable(true);
			
			dao.persist(sharedSystem);
			
			manager.disableSystem(sharedSystem, callingUser);
			
			Assert.assertFalse(shouldThrowException, message);
			
			Assert.assertFalse(sharedSystem.isAvailable(), message);
		} 
		catch (PermissionException e) {
			if (!shouldThrowException) {
				Assert.fail(message);
			}
		} catch (Exception e) {
			Assert.fail("PermissionException should be thrown when disabling a system without valid permissions", e);
		}
	}
	
	@Test
	public void disablePublicExecutionSystemRemovesGlobalDefault()
	{
		RemoteSystem sharedSystem = null;
		try 
		{
			sharedSystem = getPrivateExecutionSystem();
			sharedSystem.setPubliclyAvailable(true);
			sharedSystem.setGlobalDefault(true);
			sharedSystem.setAvailable(true);
			
			dao.persist(sharedSystem);
			
			manager.disableSystem(sharedSystem, TENANT_ADMIN);
			
			RemoteSystem disabledSystem = dao.findActiveAndInactiveSystemBySystemId(sharedSystem.getSystemId());
			Assert.assertNotNull(disabledSystem, "failed to fetch system after disabling");
			
			Assert.assertFalse(disabledSystem.isAvailable(), "System should have been disabled");
			
			Assert.assertTrue(disabledSystem.isGlobalDefault(), "System should retain global default setting after disabling");
			
			Assert.assertTrue(sharedSystem.isPubliclyAvailable(), "System should retain public setting after being disabled");
		} 
		catch (Exception e) {
			Assert.fail("No exception should be thrown when a tenant admin administers a system.", e);
		}
	}
	
	@Test
	public void disablePublicStorageSystemRemovesGlobalDefault()
	{
		RemoteSystem sharedSystem = null;
		try 
		{
			sharedSystem = getPrivateStorageSystem();
			sharedSystem.setPubliclyAvailable(true);
			sharedSystem.setGlobalDefault(true);
			sharedSystem.setAvailable(true);
			
			dao.persist(sharedSystem);
			
			manager.disableSystem(sharedSystem, TENANT_ADMIN);
			
			RemoteSystem disabledSystem = dao.findActiveAndInactiveSystemBySystemId(sharedSystem.getSystemId());
			Assert.assertNotNull(disabledSystem, "failed to fetch system after disabling");
			
			Assert.assertFalse(disabledSystem.isAvailable(), "System should have been disabled");
			
			Assert.assertTrue(disabledSystem.isGlobalDefault(), "System should retain global default setting after disabling");
			
			Assert.assertTrue(sharedSystem.isPubliclyAvailable(), "System should retain public setting after being disabled");
		} 
		catch (Exception e) {
			Assert.fail("No exception should be thrown when a tenant admin administers a system.", e);
		}
	}
	
	@DataProvider
	protected Object[][] unsetGlobalDefaultSucceedsWhenSystemIsDisabledProvider() 
	throws Exception 
	{
		return new Object[][] {
			{	getPrivateStorageSystem() },
			{	getPrivateExecutionSystem() }
		};
	}
	
	@Test(dataProvider="unsetGlobalDefaultSucceedsWhenSystemIsDisabledProvider")
	void unsetGlobalDefaultSucceedsWhenSystemIsDisabled(RemoteSystem publicGlobalDefaultSystem) {
		try 
		{
			publicGlobalDefaultSystem.setPubliclyAvailable(true);
			publicGlobalDefaultSystem.setGlobalDefault(true);
			publicGlobalDefaultSystem.setAvailable(false);
			publicGlobalDefaultSystem.setRevision(1);
			dao.persist(publicGlobalDefaultSystem);
			
			RemoteSystem notPublicGlobalDefaultSystem = manager.unsetGlobalDefault(publicGlobalDefaultSystem, TENANT_ADMIN);
			
			Assert.assertNotNull(notPublicGlobalDefaultSystem, "failed to fetch disabled system as global default");
			Assert.assertFalse(notPublicGlobalDefaultSystem.isAvailable(), "System should have been disabled");
			Assert.assertFalse(notPublicGlobalDefaultSystem.isGlobalDefault(), "System should retain global default setting after disabling");
			Assert.assertTrue(notPublicGlobalDefaultSystem.isPubliclyAvailable(), "System should retain public setting after being disabled");
			Assert.assertEquals(notPublicGlobalDefaultSystem.getRevision(), 2, "Revision should increment when toggling global default scope");
		} 
		catch (Exception e) {
			Assert.fail("No exception should be thrown when a tenant admin administers a system.", e);
		}
	}
	
	@DataProvider
	protected Object[][] setGlobalDefaultFailsWhenSystemIsDisabledProvider() 
	throws Exception 
	{
		return new Object[][] {
			{	getPrivateStorageSystem(), false },
			{	getPrivateExecutionSystem(), true },
			{	getPrivateStorageSystem(), true },
			{	getPrivateExecutionSystem(), false },
		};
	}
	
	@Test(dataProvider="setGlobalDefaultFailsWhenSystemIsDisabledProvider")
	void setGlobalDefaultFailsWhenSystemIsDisabled(RemoteSystem publicInactiveSystem, boolean setSystemAsGlobalDefaultBeforeTest) {
		try 
		{
			publicInactiveSystem.setPubliclyAvailable(true);
			publicInactiveSystem.setGlobalDefault(setSystemAsGlobalDefaultBeforeTest);
			publicInactiveSystem.setAvailable(false);
			dao.persist(publicInactiveSystem);
			
			manager.setGlobalDefault(publicInactiveSystem, TENANT_ADMIN);
			
			Assert.fail("SystemException should be thrown when a disabled system is set as global default.");
			
		} 
		catch (SystemException e) {
			//expected
		}
		catch (Exception e) {
			Assert.fail("SystemException should be thrown when a disabled system is set as global default.", e);
		}
	}
	
	@Test(dataProvider="unsetGlobalDefaultSucceedsWhenSystemIsDisabledProvider")
	void makePubliclyAvailableFailsWhenSystemIsDisabled(RemoteSystem publicGlobalDefaultSystem) {
		try 
		{
			publicGlobalDefaultSystem.setPubliclyAvailable(false);
			publicGlobalDefaultSystem.setGlobalDefault(false);
			publicGlobalDefaultSystem.setAvailable(false);
			dao.persist(publicGlobalDefaultSystem);
			
			manager.setGlobalDefault(publicGlobalDefaultSystem, TENANT_ADMIN);
			
			Assert.fail("SystemException should be thrown when a disabled system is set as global default.");
			
		} 
		catch (SystemException e) {
			//expected
		}
		catch (Exception e) {
			Assert.fail("SystemException should be thrown when a disabled system is set as global default.", e);
		}
	}
	
	@Test(dataProvider="unsetGlobalDefaultSucceedsWhenSystemIsDisabledProvider")
	void unpublishSystemUnsetsSystemAsTheGlobalDefault(RemoteSystem publicGlobalDefaultSystem) {
		try 
		{
			publicGlobalDefaultSystem.setPubliclyAvailable(true);
			publicGlobalDefaultSystem.setGlobalDefault(true);
			publicGlobalDefaultSystem.setAvailable(true);
			long startTime = publicGlobalDefaultSystem.getLastUpdated().getTime();
			dao.persist(publicGlobalDefaultSystem);
			
			RemoteSystem unpublishedSystem = manager.unpublish(publicGlobalDefaultSystem, TENANT_ADMIN);
			
			Assert.assertFalse(unpublishedSystem.isPubliclyAvailable(), "System should not be public after unpublishing.");
			Assert.assertFalse(unpublishedSystem.isGlobalDefault(), "Unpublishing a global default system should remove it as the global default.");
			Assert.assertTrue(startTime < unpublishedSystem.getLastUpdated().getTime(), "Unpublishing a systme should update the lastUpdated timestamp.");;
			
		} 
		catch (Exception e) {
			Assert.fail("No exception should be thorwn when a tenant admin unpublishes a system.", e);
		}
	}
}
