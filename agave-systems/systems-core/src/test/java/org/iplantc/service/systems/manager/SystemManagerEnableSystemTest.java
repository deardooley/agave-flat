package org.iplantc.service.systems.manager;

import java.util.ArrayList;
import java.util.List;

import org.iplantc.service.common.exceptions.PermissionException;
import org.iplantc.service.common.uuid.UniqueId;
import org.iplantc.service.systems.Settings;
import org.iplantc.service.systems.dao.SystemDao;
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

public class SystemManagerEnableSystemTest extends SystemsModelTestCommon 
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
	public Object[][] enableSharedSystemFailsWithoutAdminPrivilegesProvider() throws Exception
	{
		List<Object[]> testCases = new ArrayList<Object[]>();
		
		for (RoleType roleType: RoleType.values()) {
			testCases.add(new Object[]{ SYSTEM_SHARE_USER, 
										roleType , 
										(!roleType.canAdmin()), 
										"Shared user should be able to undelete a system with " + roleType.name() + " role." });
		}
		
		return testCases.toArray(new Object[][]{});
	}
	
	@Test(dataProvider = "enableSharedSystemFailsWithoutAdminPrivilegesProvider")
	public void enableSharedSystemFailsWithoutAdminPrivileges(String callingUser, RoleType callingUserRole, boolean shouldThrowException, String message)
	{
		RemoteSystem sharedSystem = null;
		try 
		{
			sharedSystem = getPrivateExecutionSystem();
			sharedSystem.setAvailable(false);
			
			dao.persist(sharedSystem);
			sharedSystem.addRole(new SystemRole(callingUser, callingUserRole, sharedSystem));
			
			manager.enableSystem(sharedSystem, callingUser);
			
			Assert.assertFalse(shouldThrowException, message);
			
			Assert.assertTrue(sharedSystem.isAvailable(), message);
		} 
		catch (PermissionException e) {
			if (!shouldThrowException) {
				Assert.fail(message);
			}
		}
		catch (Exception e) {
			Assert.fail("PermissionException should be thrown when enabling a system without valid permissions", e);
		}
	}
	
	@DataProvider
	public Object[][] enablePublicSystemFailsWithoutTenantAdminPrivilegesProvider() throws Exception
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
	
	@Test(dataProvider = "enablePublicSystemFailsWithoutTenantAdminPrivilegesProvider")
	public void enablePublicStorageSystemFailsWithoutTenantAdminPrivileges(String callingUser, RoleType callingUserRole, boolean shouldThrowException, String message)
	{
		RemoteSystem sharedSystem = null;
		try 
		{
			sharedSystem = getPrivateStorageSystem();
			sharedSystem.setPubliclyAvailable(true);
			sharedSystem.setAvailable(false);
			
			dao.persist(sharedSystem);
			sharedSystem.addRole(new SystemRole(callingUser, callingUserRole, sharedSystem));
			
			manager.enableSystem(sharedSystem, callingUser);
			
			Assert.assertFalse(shouldThrowException, message);
			
			Assert.assertTrue(sharedSystem.isAvailable(), message);
		} 
		catch (PermissionException e) {
			if (!shouldThrowException) {
				Assert.fail(message);
			}
		}
		catch (Exception e) {
			Assert.fail("PermissionException should be thrown when enabling a system without valid permissions", e);
		}
	}
	
	
	
	@Test(dataProvider = "enablePublicSystemFailsWithoutTenantAdminPrivilegesProvider")
	public void enablePublicExecutionSystemFailsWithoutTenantAdminPrivileges(String callingUser, RoleType callingUserRole, boolean shouldThrowException, String message)
	{
		RemoteSystem sharedSystem = null;
		try 
		{
			sharedSystem = getPrivateExecutionSystem();
			sharedSystem.setPubliclyAvailable(true);
			sharedSystem.setAvailable(false);
			
			dao.persist(sharedSystem);
			sharedSystem.addRole(new SystemRole(callingUser, callingUserRole, sharedSystem));
			
			manager.enableSystem(sharedSystem, callingUser);
			
			Assert.assertFalse(shouldThrowException, message);
			
			Assert.assertTrue(sharedSystem.isAvailable(), message);
		} 
		catch (PermissionException e) {
			if (!shouldThrowException) {
				Assert.fail(message);
			}
		}
		catch (Exception e) {
			Assert.fail("PermissionException should be thrown when enabling a system without valid permissions", e);
		}
	}
			
	@DataProvider
	public Object[][] enableSharedSystemFailsForPublicAndWordUserProvider() throws Exception
	{
		List<Object[]> testCases = new ArrayList<Object[]>();
		
		for (RoleType roleType: RoleType.values()) {
			testCases.add(new Object[]{ 
					SYSTEM_UNSHARED_USER, 
					RoleType.NONE , 
					true, 
					"Unauthenticated users should never be able to enable a system." });
			
			testCases.add(new Object[]{ 
					Settings.PUBLIC_USER_USERNAME, 
					roleType, 
					true, 
					"public user should never be able to enable a system." });
			
			testCases.add(new Object[]{ 
					Settings.WORLD_USER_USERNAME, 
					roleType, 
					true, 
					"world user should never be able to enable a system." });
		}
		
		return testCases.toArray(new Object[][]{});
	}
	
	@Test(dataProvider = "enableSharedSystemFailsForPublicAndWordUserProvider")
	public void enableSharedStorageSystemFailsForPublicAndWordUser(String callingUser, RoleType callingUserRole, boolean shouldThrowException, String message)
	{
		RemoteSystem sharedSystem = null;
		try 
		{
			sharedSystem = getPrivateStorageSystem();
			sharedSystem.setAvailable(false);
			
			dao.persist(sharedSystem);
			sharedSystem.addRole(new SystemRole(callingUser, callingUserRole, sharedSystem));
			
			manager.enableSystem(sharedSystem, callingUser);
			
			Assert.assertFalse(shouldThrowException, message);
			
			Assert.assertFalse(sharedSystem.isAvailable(), message);
		} 
		catch (PermissionException e) {
			if (!shouldThrowException) {
				Assert.fail(message);
			}
		} catch (Exception e) {
			Assert.fail("PermissionException should be thrown when enabling a system without valid permissions", e);
		}
	}
	
	@Test(dataProvider = "enableSharedSystemFailsForPublicAndWordUserProvider")
	public void enableSharedExecutionSystemFailsForPublicAndWordUser(String callingUser, RoleType callingUserRole, boolean shouldThrowException, String message)
	{
		RemoteSystem sharedSystem = null;
		try 
		{
			sharedSystem = getPrivateExecutionSystem();
			sharedSystem.setAvailable(false);
			
			dao.persist(sharedSystem);
			sharedSystem.addRole(new SystemRole(callingUser, callingUserRole, sharedSystem));
			
			manager.enableSystem(sharedSystem, callingUser);
			
			Assert.assertFalse(shouldThrowException, message);
			
			Assert.assertFalse(sharedSystem.isAvailable(), message);
		} 
		catch (PermissionException e) {
			if (!shouldThrowException) {
				Assert.fail(message);
			}
		} catch (Exception e) {
			Assert.fail("PermissionException should be thrown when enabling a system without valid permissions", e);
		}
	}
	
	
	

}
