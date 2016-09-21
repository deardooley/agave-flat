package org.iplantc.service.systems.manager;

import org.iplantc.service.common.exceptions.PermissionException;
import org.iplantc.service.common.persistence.HibernateUtil;
import org.iplantc.service.common.uuid.UniqueId;
import org.iplantc.service.systems.Settings;
import org.iplantc.service.systems.dao.SystemDao;
import org.iplantc.service.systems.exceptions.SystemException;
import org.iplantc.service.systems.model.AuthConfig;
import org.iplantc.service.systems.model.ExecutionSystem;
import org.iplantc.service.systems.model.JSONTestDataUtil;
import org.iplantc.service.systems.model.RemoteSystem;
import org.iplantc.service.systems.model.StorageSystem;
import org.iplantc.service.systems.model.SystemRole;
import org.iplantc.service.systems.model.SystemsModelTestCommon;
import org.iplantc.service.systems.model.enumerations.AuthConfigType;
import org.iplantc.service.systems.model.enumerations.CredentialServerProtocolType;
import org.iplantc.service.systems.model.enumerations.RoleType;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class SystemManagerTest extends SystemsModelTestCommon 
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
//		clearSystems();
	}
	
	@AfterMethod
	public void afterMethod() throws Exception {
//		clearSystems();
	}
	
    private RemoteSystem getPrivateStorageSystem() throws Exception {	
        RemoteSystem privateStorageSystem = StorageSystem.fromJSON( jtd.getTestDataObject(
                JSONTestDataUtil.TEST_STORAGE_SYSTEM_FILE));
        privateStorageSystem.setOwner(SYSTEM_OWNER);
        privateStorageSystem.setSystemId(new UniqueId().getStringId());
        return privateStorageSystem;
    }
    
    private RemoteSystem getPublicStorageSystem() throws Exception {   
        RemoteSystem publicStorageSystem = StorageSystem.fromJSON( jtd.getTestDataObject(
                JSONTestDataUtil.TEST_STORAGE_SYSTEM_FILE));
        
        publicStorageSystem.setOwner(SYSTEM_OWNER);
        publicStorageSystem.setPubliclyAvailable(true);
        publicStorageSystem.setGlobalDefault(true);
//        publicStorageSystem.setSystemId(publicStorageSystem.getSystemId() + ".public");
        publicStorageSystem.setSystemId(new UniqueId().getStringId());
        
        return publicStorageSystem;
    }
    
    private RemoteSystem getPrivateExecutionSystem() throws Exception {   
        RemoteSystem privateExecutionSystem = ExecutionSystem.fromJSON( jtd.getTestDataObject(
                JSONTestDataUtil.TEST_EXECUTION_SYSTEM_FILE));
        privateExecutionSystem.setOwner(SYSTEM_OWNER);
        privateExecutionSystem.setSystemId(new UniqueId().getStringId());
        return privateExecutionSystem;
    }
    
    private RemoteSystem getPublicExecutionSystem() throws Exception {   
        RemoteSystem publicExecutionSystem = ExecutionSystem.fromJSON( jtd.getTestDataObject(
                JSONTestDataUtil.TEST_EXECUTION_SYSTEM_FILE));
        publicExecutionSystem.setOwner(SYSTEM_OWNER);
        publicExecutionSystem.setPubliclyAvailable(true);
        publicExecutionSystem.setGlobalDefault(true);
//        publicExecutionSystem.setSystemId(publicExecutionSystem.getSystemId() + ".public");
        publicExecutionSystem.setSystemId(new UniqueId().getStringId());
        return publicExecutionSystem;
        
    }
    
    private RemoteSystem getPublicReadonlyStorageSystem() throws Exception {   
        RemoteSystem publicReadonlyStorageSystem = StorageSystem.fromJSON( jtd.getTestDataObject(
                JSONTestDataUtil.TEST_STORAGE_SYSTEM_FILE));
        publicReadonlyStorageSystem.setOwner(SYSTEM_OWNER);
        publicReadonlyStorageSystem.setPubliclyAvailable(true);
        publicReadonlyStorageSystem.setGlobalDefault(false);
        publicReadonlyStorageSystem.getRoles().add(new SystemRole(Settings.PUBLIC_USER_USERNAME, RoleType.GUEST));
        publicReadonlyStorageSystem.setSystemId(new UniqueId().getStringId());
        return publicReadonlyStorageSystem;
        
    }
    
    private RemoteSystem getPrivateReadonlyStorageSystem() throws Exception {   
        RemoteSystem publicReadonlyStorageSystem = StorageSystem.fromJSON( jtd.getTestDataObject(
                JSONTestDataUtil.TEST_STORAGE_SYSTEM_FILE));
        publicReadonlyStorageSystem.setOwner(SYSTEM_OWNER);
        publicReadonlyStorageSystem.setPubliclyAvailable(false);
        publicReadonlyStorageSystem.setGlobalDefault(false);
        publicReadonlyStorageSystem.getRoles().add(new SystemRole(SYSTEM_SHARE_USER, RoleType.GUEST));
        publicReadonlyStorageSystem.setSystemId(new UniqueId().getStringId());
        return publicReadonlyStorageSystem;
        
    }
    
    private RemoteSystem getSharedExecutionSystem() throws Exception {
        RemoteSystem sharedExecutionSystem = ExecutionSystem.fromJSON( jtd.getTestDataObject(
                JSONTestDataUtil.TEST_EXECUTION_SYSTEM_FILE));
        sharedExecutionSystem.setOwner(SYSTEM_OWNER);
        sharedExecutionSystem.getRoles().add(new SystemRole(SYSTEM_SHARE_USER, RoleType.ADMIN));
//        sharedExecutionSystem.setSystemId(sharedExecutionSystem.getSystemId() + ".shared");
        sharedExecutionSystem.setSystemId(new UniqueId().getStringId());
        return sharedExecutionSystem;
    }
    
//    public void setUpSystems() throws Exception {
//    	privateStorageSystem = getPrivateStorageSystem();
//    	        
//        publicStorageSystem = getPublicStorageSystem();
//        
//        privateExecutionSystem = getPrivateExecutionSystem();
//        
//        publicExecutionSystem = getPublicExecutionSystem();
//        
//        sharedExecutionSystem = getSharedExecutionSystem();
//	}
//	
	@DataProvider(name="cloneSystemProvider")
	public Object[][] cloneSystemProvider() throws Exception
	{   
		return new Object[][] {
				{ null, getPrivateExecutionSystem().getOwner(), getPrivateExecutionSystem().getSystemId() + "-1", "Cloning null system throws exception", true },
				{ getPrivateExecutionSystem(), null, getPrivateExecutionSystem().getSystemId() + "-1", "Cloning system and providing a null api username for cloned system throws exception", true },
//				{ getPrivateExecutionSystem(), getPrivateExecutionSystem().getOwner(), getPrivateExecutionSystem().getSystemId(), "Cloning system and assigning a duplicate system id throws exception", true },
				{ getPrivateExecutionSystem(), getPrivateExecutionSystem().getOwner(), getPrivateExecutionSystem().getSystemId() + "-1", "Owner can clone a system they own", false },
				{ getPrivateExecutionSystem(), Settings.PUBLIC_USER_USERNAME, getPrivateExecutionSystem().getSystemId() + "-1", "Public user cannot clone a private system", true },
				{ getPrivateExecutionSystem(), Settings.WORLD_USER_USERNAME, getPrivateExecutionSystem().getSystemId() + "-1", "World user cannot clone a private system", true },
				{ getPrivateExecutionSystem(), SYSTEM_SHARE_USER, getPrivateExecutionSystem().getSystemId() + "-1", "non-owner cannot clone a private system", true },
				{ getPrivateExecutionSystem(), SYSTEM_UNSHARED_USER, getPrivateExecutionSystem().getSystemId() + "-1", "non-owner cannot clone a private system", true },
				
				{ getPrivateExecutionSystem(), getPublicExecutionSystem().getOwner(), getPrivateExecutionSystem().getSystemId() + "-1", "Original owner can clone a public system", false },
				{ getPrivateExecutionSystem(), Settings.PUBLIC_USER_USERNAME, getPrivateExecutionSystem().getSystemId() + "-1", "Public user can clone a public system", true },
				{ getPrivateExecutionSystem(), Settings.WORLD_USER_USERNAME, getPrivateExecutionSystem().getSystemId() + "-1", "World user can clone a public system", true },
				{ getPrivateExecutionSystem(), SYSTEM_SHARE_USER, getPrivateExecutionSystem().getSystemId() + "-1", "Users can clone a public system", true },
				{ getPrivateExecutionSystem(), SYSTEM_UNSHARED_USER, getPrivateExecutionSystem().getSystemId() + "-1", "Users can clone a public system", true },
				
				{ getPrivateExecutionSystem(), getSharedExecutionSystem().getOwner(), getPrivateExecutionSystem().getSystemId() + "-1", "Owner can clone a shared system", false },
				{ getPrivateExecutionSystem(), Settings.PUBLIC_USER_USERNAME, getPrivateExecutionSystem().getSystemId() + "-1", "Public user cannot clone a shared system they do not have access to", true },
				{ getPrivateExecutionSystem(), Settings.WORLD_USER_USERNAME, getPrivateExecutionSystem().getSystemId() + "-1", "World user cannot clone a shared system they do not have access to", true },
				{ getPrivateExecutionSystem(), SYSTEM_UNSHARED_USER, getPrivateExecutionSystem().getSystemId() + "-1", "non-shared user cannot clone a shared system they do not have access to", true },
		};
	}
	
	@Test(dataProvider = "cloneSystemProvider")
	public void cloneSystem(RemoteSystem system, String apiUsername, String newSystemId, String message, boolean shouldThrowException)
	{
		boolean exceptionFlag = false;
		String exceptionMsg = message;
		RemoteSystem updatedSystem = null;
		try 
		{
			if (system != null) 
			{
				dao.persist(system);
				Assert.assertNotNull(system.getId(), "Private storage system was not saved.");
			}
			
			updatedSystem = manager.cloneSystem(system, apiUsername, "cloneSystemTest");
			
			Assert.assertNotNull(updatedSystem.getId(), "Clone system was not saved.");
			
			Assert.assertTrue(updatedSystem.getStorageConfig().getAuthConfigs().size() == 0, "No auth configs should be present after a clone operation.");
			Assert.assertNull(updatedSystem.getStorageConfig().getDefaultAuthConfig(), "Auth config should be empty after cloning.");
//			Assert.assertNotEquals(updatedSystem.getStorageConfig().getDefaultAuthConfig(), 
//					updatedSystem.getStorageConfig().getAuthConfigForInternalUsername(SYSTEM_INTERNAL_USERNAME), 
//					"New internal user auth config was incorrectly set as default when added.");
		}
		catch(Exception se){
			exceptionFlag = true;
			exceptionMsg = system + " " + apiUsername + " " + message;
			if (!shouldThrowException) 
				Assert.fail(message, se);
		}
		finally {
			try {dao.remove(system); } catch (Exception e) {}
			try {dao.remove(updatedSystem); } catch (Exception e) {}
		}
		
		System.out.println(" exception thrown?  expected " + shouldThrowException + " actual " + exceptionFlag);

		Assert.assertTrue(exceptionFlag == shouldThrowException, exceptionMsg);
	}

	@DataProvider(name="parseSystemProvider")
	public Object[][] parseSystemProvider() throws Exception
	{
		return new Object[][] {
				{ jtd.getTestDataObject(JSONTestDataUtil.TEST_EXECUTION_SYSTEM_FILE), ExecutionSystem.class, "Execution system is parsed from json" },
				{ jtd.getTestDataObject(JSONTestDataUtil.TEST_STORAGE_SYSTEM_FILE), StorageSystem.class, "Storage system is parsed from json" },
		};
	}
	
	@Test(dataProvider = "parseSystemProvider", dependsOnMethods={"cloneSystem"})
	public void parseSystem(JSONObject json, Class<?> expectedClass, String message) throws SystemException, PermissionException
	{
		RemoteSystem system = manager.parseSystem(json, SYSTEM_OWNER, null);
		
		Assert.assertEquals(system.getOwner(), SYSTEM_OWNER, message);
		
		Assert.assertFalse(system.isPubliclyAvailable(), "System was published as private");
		
		Assert.assertEquals(system.getClass(), expectedClass, message);
		
		Assert.assertTrue(system.isOwnedBy(SYSTEM_OWNER), "Adding system should make that user the owner.");
		
		Assert.assertFalse(system.isPubliclyAvailable(), "Adding system should not make it public.");
		
		Assert.assertFalse(system.isGlobalDefault(), "Adding system should not make it a global default.");
	}
	
	@DataProvider(name="isVisibleByUserProvider")
	public Object[][] isVisibleByUserProvider() throws Exception
	{
		return new Object[][] {
				{ getPrivateExecutionSystem(), SYSTEM_OWNER, "Owner can see their own execution system", true },
				{ getPrivateExecutionSystem(), SYSTEM_PUBLIC_USER, "Guest cannot see private execution system", false },
				{ getPrivateExecutionSystem(), SYSTEM_SHARE_USER, "non-owner cannot see private execution system", false },
				
				{ getPublicExecutionSystem(), SYSTEM_OWNER, "Original owner can see public execution system", true },
				{ getPublicExecutionSystem(), SYSTEM_PUBLIC_USER, "Guest can see public execution system", true },
				{ getPublicExecutionSystem(), SYSTEM_SHARE_USER, "Users can see public execution system", true },
				
				{ getSharedExecutionSystem(), SYSTEM_OWNER, "Owner can see their shared execution system", true },
				{ getSharedExecutionSystem(), SYSTEM_PUBLIC_USER, "Guest cannot see shared execution system", false },
				{ getSharedExecutionSystem(), SYSTEM_UNSHARED_USER, "non-shared user cannot see shared execution system", false },
				
				{ getPrivateStorageSystem(), SYSTEM_OWNER, "Owner can clone their own storage system", true },
				{ getPrivateStorageSystem(), SYSTEM_OWNER, "Owner can clone their own storage system", true },
		};
	}
	
	@Test(dataProvider = "isVisibleByUserProvider", dependsOnMethods={"parseSystem"})
	public void isVisibleByUser(RemoteSystem system, String username, String message, boolean userCanManager)
	{
		Assert.assertEquals(manager.isVisibleByUser(system, username), userCanManager, message);
	}
	
	@DataProvider(name="isManageableByUserProvider")
	public Object[][] isManageableByUserProvider() throws Exception
	{
		return new Object[][] {
		     
			{ getPrivateExecutionSystem(), SYSTEM_OWNER, "Owner can manage their system", true },
			{ getPrivateExecutionSystem(), SYSTEM_PUBLIC_USER, "Guest cannot manage a private system", false },
			{ getPrivateExecutionSystem(), SYSTEM_SHARE_USER, "User cannot manage a private system", false },
			
			{ getPublicExecutionSystem(), SYSTEM_OWNER, "No one can manage a public system", false },
			{ getPublicExecutionSystem(), SYSTEM_PUBLIC_USER, "Guest cannot manage a private system", false },
			{ getPublicExecutionSystem(), SYSTEM_SHARE_USER, "User cannot manage a private system", false },
			
			{ getSharedExecutionSystem(), SYSTEM_UNSHARED_USER, "Unshared user cannot manage a shared system", false },
		};
	}
	
	@Test(dataProvider = "isManageableByUserProvider", dependsOnMethods={"isVisibleByUser"})
	public void isManageableByUser(RemoteSystem system, String username, String message, boolean userCanManager)
	{
		Assert.assertEquals(manager.isManageableByUser(system, username), userCanManager, message);
	}
	
	@DataProvider(name="isManageableBySharedUserProvider")
	public Object[][] isManageableBySharedUserProvider() throws Exception
	{
		return new Object[][] {
				{ getSharedExecutionSystem(), RoleType.NONE, "User cannot manage a system they have no role on", false },
				{ getSharedExecutionSystem(), RoleType.USER, "User cannot manage a system in which they are only a user", false },
				{ getSharedExecutionSystem(), RoleType.PUBLISHER, "User cannot manage a system they have publisher role on", false },
				{ getSharedExecutionSystem(), RoleType.OWNER, "User cannot manage a system they have owner role on", true },
				{ getSharedExecutionSystem(), RoleType.ADMIN, "User can manage a system they have admin role on", true },
		};
	}
	
	@Test(dataProvider = "isManageableBySharedUserProvider", dependsOnMethods={"isManageableByUser"})
	public void isManageableBySharedUserTest(RemoteSystem system, RoleType testType, String message, boolean userCanManager)
	{
		RoleType previousType = system.getRoles().get(0).getRole();
		system.getRoles().get(0).setRole(testType);
		
		Assert.assertEquals(manager.isManageableByUser(system, SYSTEM_SHARE_USER), userCanManager, message);

		// restore type
		system.getRoles().get(0).setRole(previousType);
	}
	
	@DataProvider(name="makePublicProvider")
	public Object[][] makePublicProvider() throws Exception
	{
		return new Object[][] {
				{ getPrivateExecutionSystem(), "Original owner can see a public system" },
		};
	}
				
	@Test(dataProvider = "makePublicProvider", dependsOnMethods={"isManageableBySharedUserTest"})
	public void makePublic(RemoteSystem system, String message)
	{
		RemoteSystem publicSystem = null;
		try 
		{
			publicSystem = manager.publish(system, SYSTEM_OWNER);
			
			Assert.assertTrue(publicSystem.isPubliclyAvailable(), "System is public");
			Assert.assertTrue(manager.isVisibleByUser(publicSystem, SYSTEM_PUBLIC_USER), "System is public");
		} 
		catch (Exception e) {
			try { dao.remove(publicSystem); } catch (Exception e1) {}
		}
	}
	
	@DataProvider(name="enableSystemProvider")
	public Object[][] enableSystemProvider() throws Exception
	{
		return new Object[][] {
				{ getPublicExecutionSystem(), TENANT_ADMIN, false, "Tenant admins should be able to enable a public system" },
				{ getPublicExecutionSystem(), SYSTEM_OWNER, true, "owner should not be able to enable a public system" },
				{ getPublicExecutionSystem(), SYSTEM_SHARE_USER, true, "shared users should not be able to enable a public system" },
				{ getPublicExecutionSystem(), SYSTEM_UNSHARED_USER, true, "unshared users should not be able to enable a public system" },
				{ getPublicExecutionSystem(), Settings.WORLD_USER_USERNAME, true, "public users should not be able to enable a public system" },
				{ getPublicExecutionSystem(), Settings.WORLD_USER_USERNAME, true, "world users should not be able to enable a public system" },

				{ getPublicStorageSystem(), TENANT_ADMIN, false, "Tenant admins should be able to enable a public system" },
				{ getPublicStorageSystem(), SYSTEM_OWNER, true, "owner should not be able to enable a public system" },
				{ getPublicStorageSystem(), SYSTEM_SHARE_USER, true, "shared users should not be able to enable a public system" },
				{ getPublicStorageSystem(), SYSTEM_UNSHARED_USER, true, "unshared users should not be able to enable a public system" },
				{ getPublicStorageSystem(), Settings.WORLD_USER_USERNAME, true, "public users should not be able to enable a public system" },
				{ getPublicStorageSystem(), Settings.WORLD_USER_USERNAME, true, "world users should not be able to enable a public system" },
				
				{ getPublicReadonlyStorageSystem(), TENANT_ADMIN, false, "Tenant admins should be able to enable a public readonly system" },
				{ getPublicReadonlyStorageSystem(), SYSTEM_OWNER, true, "owner should not be able to enable a public readonly system" },
				{ getPublicReadonlyStorageSystem(), SYSTEM_SHARE_USER, true, "shared users should not be able to enable a public readonly system" },
				{ getPublicReadonlyStorageSystem(), SYSTEM_UNSHARED_USER, true, "unshared users should not be able to enable a public readonly system" },
				{ getPublicReadonlyStorageSystem(), Settings.WORLD_USER_USERNAME, true, "public users should not be able to enable a public readonly system" },
				{ getPublicReadonlyStorageSystem(), Settings.WORLD_USER_USERNAME, true, "world users should not be able to enable a public readonly system" },
				
				{ getPrivateReadonlyStorageSystem(), TENANT_ADMIN, false, "Tenant admins should be able to enable a private readonly system" },
				{ getPrivateReadonlyStorageSystem(), SYSTEM_OWNER, false, "owner should be able to enable their private readonly system" },
				{ getPrivateReadonlyStorageSystem(), SYSTEM_SHARE_USER, true, "shared users should not be able to enable a private readonly system" },
				{ getPrivateReadonlyStorageSystem(), SYSTEM_UNSHARED_USER, true, "unshared users should not be able to enable a private readonly system" },
				{ getPrivateReadonlyStorageSystem(), Settings.WORLD_USER_USERNAME, true, "public users should not be able to enable a private readonly system" },
				{ getPrivateReadonlyStorageSystem(), Settings.WORLD_USER_USERNAME, true, "world users should not be able to enable a private readonly system" },
				
				{ getPrivateStorageSystem(), TENANT_ADMIN, false, "Tenant admins should be able to enable a private system" },
				{ getPrivateStorageSystem(), SYSTEM_OWNER, false, "owner should be able to enable their private system" },
				{ getPrivateStorageSystem(), SYSTEM_SHARE_USER, true, "shared standard user should not be able to enable an unshared private system" },
				{ getPrivateStorageSystem(), SYSTEM_UNSHARED_USER, true, "unshared user should not be able to enable an unshared private system" },
				{ getPrivateStorageSystem(), Settings.WORLD_USER_USERNAME, true, "public user should not be able to enable a private system" },
				{ getPrivateStorageSystem(), Settings.WORLD_USER_USERNAME, true, "world user should not be able to enable a private system" },
				
				{ getPrivateExecutionSystem(), TENANT_ADMIN, false, "Tenant admins should be able to enable a private system" },
				{ getPrivateExecutionSystem(), SYSTEM_OWNER, false, "owner should be able to enable their private system" },
				{ getPrivateExecutionSystem(), SYSTEM_SHARE_USER, true, "shared standard user should not be able to enable an unshared private system" },
				{ getPrivateExecutionSystem(), SYSTEM_UNSHARED_USER, true, "unshared user should not be able to enable an unshared private system" },
				{ getPrivateExecutionSystem(), Settings.WORLD_USER_USERNAME, true, "public user should not be able to enable a private system" },
				{ getPrivateExecutionSystem(), Settings.WORLD_USER_USERNAME, true, "world user should not be able to enable a private system" },
				
				{ getSharedExecutionSystem(), TENANT_ADMIN, false, "Tenant admins should be able to enable a private shared system" },
				{ getSharedExecutionSystem(), SYSTEM_OWNER, false, "owner should be able to enable their private shared system" },
				{ getSharedExecutionSystem(), SYSTEM_SHARE_USER, false, "shared standard user should be able to enable an shared private system" },
				{ getSharedExecutionSystem(), SYSTEM_UNSHARED_USER, true, "unshared user should not be able to enable an unshared private system" },
				{ getSharedExecutionSystem(), Settings.WORLD_USER_USERNAME, true, "public user should not be able to enable a private system" },
				{ getSharedExecutionSystem(), Settings.WORLD_USER_USERNAME, true, "world user should not be able to enable a private system" },
				
		};
	}
				
	@Test(dataProvider = "enableSystemProvider", dependsOnMethods={"makePublic"})
	public void enableSystem(RemoteSystem system, String authenticatedUser, boolean shouldThrowException, String message)
	{
		RemoteSystem enabledSystem = null;
		try 
		{
			system.setAvailable(false);
			enabledSystem = manager.enableSystem(system, authenticatedUser);
			
			Assert.assertTrue(enabledSystem.isAvailable(), "System should be enabled ");
		} 
		catch (Exception e) {
			if (!shouldThrowException) {
				Assert.fail(message, e);
			}
		}
		finally {
			try { dao.remove(enabledSystem); } catch (Exception e1) {}
		}
	}

	@Test(dependsOnMethods={"enableSystem"})
	public void getDefaultExecutionSystem() throws Exception
	{
	    RemoteSystem privateExecutionSystem = null;
	    RemoteSystem publicExecutionSystem = null;
	    RemoteSystem publicStorageSystem = null;
	    
		try {
		    privateExecutionSystem = getPrivateExecutionSystem();
		    publicExecutionSystem = getPublicExecutionSystem();
		    publicStorageSystem = getPublicStorageSystem();
            
		    dao.persist(privateExecutionSystem);
			Assert.assertNotNull(privateExecutionSystem.getId(), "Private execution system was not saved.");
			
			dao.persist(publicExecutionSystem);
			Assert.assertNotNull(publicExecutionSystem.getId(), "Public execution system was not saved.");
			
			dao.persist(publicStorageSystem);
			Assert.assertNotNull(publicStorageSystem.getId(), "Public storage system was not saved.");
			
			Assert.assertTrue(publicStorageSystem.isGlobalDefault(), "Public storage system is global default.");
			Assert.assertTrue(publicExecutionSystem.isGlobalDefault(), "Public execution system is global default.");
			Assert.assertFalse(privateExecutionSystem.isGlobalDefault(), "Adding execution system should not make it a global default.");
			
			Assert.assertNotEquals(manager.getUserDefaultStorageSystem(privateExecutionSystem.getOwner()).getSystemId(), privateExecutionSystem.getSystemId(), 
					"Private storage system should not be the default execution system after inserting private storage app.");
			Assert.assertNotEquals(manager.getUserDefaultExecutionSystem(privateExecutionSystem.getOwner()), privateExecutionSystem, 
					"Private execution system should not be the default execution system after inserting private storage app.");
			
			privateExecutionSystem.addUserUsingAsDefault(privateExecutionSystem.getOwner());
			
            try{
                dao.merge(privateExecutionSystem);
            }catch(Exception e){
                Assert.fail("Failed to save private execution system after adding default user", e);
            }

			Assert.assertNotEquals(manager.getUserDefaultStorageSystem(privateExecutionSystem.getOwner()), privateExecutionSystem, 
					"Private storage system should not be the default storage system after setting it as the user execution default.");
			Assert.assertEquals(manager.getUserDefaultExecutionSystem(privateExecutionSystem.getOwner()), privateExecutionSystem, 
					"Private execution system should be the default execution system after setting it as the user execution default.");
		} 
		finally {
		    try {dao.remove(privateExecutionSystem); } catch (Exception e) {}
			try {dao.remove(publicExecutionSystem); } catch (Exception e) {}
			try {dao.remove(publicStorageSystem); } catch (Exception e) {}
		}
	}
	
	@Test(dependsOnMethods={"getDefaultExecutionSystem"})
	public void getDefaultStorageSystem() throws Exception
	{
		RemoteSystem privateStorageSystem = null;
        RemoteSystem publicExecutionSystem = null;
        RemoteSystem publicStorageSystem = null;
        
		try {
		    privateStorageSystem = getPrivateStorageSystem();
            publicExecutionSystem = getPublicExecutionSystem();
            publicStorageSystem = getPublicStorageSystem();
            
            dao.persist(privateStorageSystem);
			Assert.assertNotNull(privateStorageSystem.getId(), "Private storage system was not saved.");
			
			dao.persist(publicStorageSystem);
			Assert.assertNotNull(publicStorageSystem.getId(), "Public storage system was not saved.");
			
			dao.persist(publicExecutionSystem);
			Assert.assertNotNull(publicExecutionSystem.getId(), "Public execution system was not saved.");
			
			Assert.assertTrue(publicStorageSystem.isGlobalDefault(), "Public storage system is global default.");
			Assert.assertTrue(publicExecutionSystem.isGlobalDefault(), "Public execution system is global default.");
			Assert.assertFalse(privateStorageSystem.isGlobalDefault(), "Adding storage system should not make it a global default.");
			
			Assert.assertNotEquals(manager.getUserDefaultStorageSystem(privateStorageSystem.getOwner()), privateStorageSystem, 
					"Private storage system should not be the default execution system after inserting private storage app.");
			Assert.assertNotEquals(manager.getUserDefaultExecutionSystem(privateStorageSystem.getOwner()), privateStorageSystem, 
					"Private storage system should not be the default execution system after inserting private storage app.");
			
			privateStorageSystem.addUserUsingAsDefault(privateStorageSystem.getOwner());
			try{
                dao.merge(privateStorageSystem);
            }catch(Exception e){
                System.out.println("failed to persist this storage system and not sure why ");
            }

			Assert.assertEquals(manager.getUserDefaultStorageSystem(privateStorageSystem.getOwner()).getSystemId(), privateStorageSystem.getSystemId(), 
					"Private storage system should be the default storage system after setting it as the user storage default.");
			Assert.assertNotEquals(manager.getUserDefaultExecutionSystem(privateStorageSystem.getOwner()).getSystemId(), privateStorageSystem.getSystemId(), 
					"Private storage system should not be the default execution system after setting it as the user storage default.");
			
		} 
		finally {
			try {dao.remove(privateStorageSystem); } catch (Exception e) {}
			try {dao.remove(publicExecutionSystem); } catch (Exception e) {}
			try {dao.remove(publicStorageSystem); } catch (Exception e) {}
		}
	}
	
	@DataProvider(name="updateInternalUserAuthConfigOnSystemOfTypeProvider")
	public Object[][] updateInternalUserAuthConfigOnSystemOfTypeProvider() throws Exception
	{
		return new Object[][] {
				{ getSharedExecutionSystem(), SYSTEM_SHARE_USER, "Shared storage system admin can add internal users", false },
				{ getPrivateStorageSystem(), SYSTEM_OWNER, "Private storage system owner can add internal users", false },
				{ getPrivateExecutionSystem(), SYSTEM_OWNER, "Private execution system owner can add internal users", false },
				{ getPrivateStorageSystem(), SYSTEM_SHARE_USER, "Private storage system non user cannot add internal users", true },
		};
	}
	
	@Test(dataProvider="updateInternalUserAuthConfigOnSystemOfTypeProvider", dependsOnMethods={"getDefaultStorageSystem"})
	public void updateInternalUserAuthConfigOnSystemOfType(RemoteSystem system, String apiUsername, String message, boolean shouldThrowException) throws Exception
	{
		boolean exceptionFlag = false;
		String exceptionMsg = message;
		
		try 
		{
			dao.persist(system);
			Assert.assertNotNull(system.getId(), "Private storage system was not saved.");
			
			Assert.assertTrue(system.getStorageConfig().getAuthConfigs().size() == 1, "More than one auth config present.");
			Assert.assertNotNull(system.getStorageConfig().getDefaultAuthConfig(), "No default auth config was found.");
			
			manager.updateInternalUserAuthConfigOnSystemOfType(system, "storage", apiUsername, SYSTEM_INTERNAL_USERNAME, 
			        SYSTEM_SHARE_USER, SYSTEM_SHARE_USER, AuthConfigType.PASSWORD.name(), "", null, null, 
					null, 0, null);
			
			RemoteSystem updatedSystem = dao.findById(system.getId());
			Assert.assertTrue(updatedSystem.getStorageConfig().getAuthConfigs().size() == 2, "Incorrect number of auth configs present after adding internal user config.");
			Assert.assertNotNull(updatedSystem.getStorageConfig().getDefaultAuthConfig(), "No default auth config was found.");
			Assert.assertNotEquals(updatedSystem.getStorageConfig().getDefaultAuthConfig(), 
					updatedSystem.getStorageConfig().getAuthConfigForInternalUsername(SYSTEM_INTERNAL_USERNAME), 
					"New internal user auth config was incorrectly set as default when added.");
		}
//		catch(SystemException e) {
//			exceptionFlag = true;
//			exceptionMsg = apiUsername + " " + message;
//			if (!shouldThrowException) 
//				e.printStackTrace();
//		}
		catch(Exception se){
			exceptionFlag = true;
			exceptionMsg = system.toString() + " " + apiUsername + " " + message;
			if (!shouldThrowException) 
				Assert.fail(message, se);
		}
		finally {
			try {dao.remove(system); } catch (Exception e) {e.printStackTrace();}
		}
		
		System.out.println(" exception thrown?  expected " + shouldThrowException + " actual " + exceptionFlag);

		Assert.assertTrue(exceptionFlag == shouldThrowException, exceptionMsg);
	}
	
	@DataProvider(name="updateInternalUserAuthConfigOnSystemOfTypeProviderB")
    public Object[][] updateInternalUserAuthConfigOnSystemOfTypeProviderB() throws Exception
    {
        return new Object[][] {
                { getSharedExecutionSystem(), SYSTEM_OWNER, "Private storage system owner can add internal users", false },
                { getSharedExecutionSystem(), SYSTEM_UNSHARED_USER, "Shared storage system non user cannot add internal users", true },
                { getPublicStorageSystem(), SYSTEM_OWNER, "Internal users cannot be added to public storage systems", true },
                { getPublicExecutionSystem(), SYSTEM_OWNER, "Internal users cannot be added to public execution systems", true },
        };
    }
    
	@Test(dataProvider="updateInternalUserAuthConfigOnSystemOfTypeProviderB", dependsOnMethods={"updateInternalUserAuthConfigOnSystemOfType"})
	public void updateInternalUserAuthConfigOnSystemOfTypeB(RemoteSystem system, String apiUsername, String message, boolean shouldThrowException) throws Exception
	{
		updateInternalUserAuthConfigOnSystemOfType(system, apiUsername, message, shouldThrowException);
	}
	
	
	@DataProvider(name="updateInternalUserAuthConfigOfTypeProvider")
	public Object[][] updateInternalUserAuthConfigOfTypeProvider() throws Exception
	{
		return new Object[][] {
				{ SYSTEM_OWNER, "Private storage system owner can add internal users to multiple systems at once", false },
		};
	}
	
	@Test(dataProvider="updateInternalUserAuthConfigOfTypeProvider", dependsOnMethods={"updateInternalUserAuthConfigOnSystemOfTypeB"})
	public void updateInternalUserAuthConfigOfType(String apiUsername, String message, boolean shouldThrowException) throws Exception
	{
		boolean exceptionFlag = false;
		String exceptionMsg = message;
		
		RemoteSystem privateStorageSystem = null;
        RemoteSystem privateExecutionSystem = null;
        
		try 
		{
		    privateStorageSystem = getPrivateStorageSystem();
		    privateExecutionSystem = getPrivateExecutionSystem();
            
            dao.persist(privateStorageSystem);
			Assert.assertNotNull(privateStorageSystem.getId(), "Private storage system was not saved.");
			Assert.assertTrue(privateStorageSystem.getStorageConfig().getAuthConfigs().size() == 1, "More than one storage auth config present.");
			Assert.assertNotNull(privateStorageSystem.getStorageConfig().getDefaultAuthConfig(), "No default auth storage config was found.");
			
			dao.persist(privateExecutionSystem);
			Assert.assertNotNull(privateExecutionSystem.getId(), "Private execution system was not saved.");
			Assert.assertTrue(privateExecutionSystem.getStorageConfig().getAuthConfigs().size() == 1, "More than one execution auth config present.");
			Assert.assertNotNull(privateExecutionSystem.getStorageConfig().getDefaultAuthConfig(), "No default auth execution config was found.");
			
			AuthConfig defaultAuthConfig = privateStorageSystem.getStorageConfig().getDefaultAuthConfig();
			manager.updateAllInternalUserAuthConfigOfType("storage", 
		                                                apiUsername, 
		                                                SYSTEM_INTERNAL_USERNAME, 
                                    			        apiUsername, 
                                    			        apiUsername, 
                                    			        defaultAuthConfig.getType().name(), 
                                    			        "", 
                                    			        privateStorageSystem.getStorageConfig().getResource(), 
                                    			        privateStorageSystem.getStorageConfig().getZone(), 
                                    			        defaultAuthConfig.getCredentialServer() == null ? null : defaultAuthConfig.getCredentialServer().getEndpoint(), 
                            			                defaultAuthConfig.getCredentialServer() == null ? 0 : defaultAuthConfig.getCredentialServer().getPort(),
                    			                        defaultAuthConfig.getCredentialServer() == null ? null : defaultAuthConfig.getCredentialServer().getProtocol().name());
			
			for (RemoteSystem system: dao.getUserSystems(apiUsername, false)) 
			{
				Assert.assertTrue(system.getStorageConfig().getAuthConfigs().size() == 2, "Incorrect number of auth configs present after adding internal user config.");
				Assert.assertNotNull(system.getStorageConfig().getDefaultAuthConfig(), "No default auth config was found.");
				Assert.assertNotEquals(system.getStorageConfig().getDefaultAuthConfig(), 
						system.getStorageConfig().getAuthConfigForInternalUsername(SYSTEM_INTERNAL_USERNAME), 
						"New internal user auth config was incorrectly set as default when added.");
			}
		}
		catch(SystemException e) {
			exceptionFlag = true;
			exceptionMsg = apiUsername + " " + message;
			if (!shouldThrowException) 
				e.printStackTrace();
		}
		catch(Exception se){
			exceptionFlag = true;
			exceptionMsg = apiUsername + " " + message;
			if (!shouldThrowException) 
				se.printStackTrace();
		}
		finally {
			try {dao.remove(privateStorageSystem); } catch (Exception e) {}
			try {dao.remove(privateExecutionSystem); } catch (Exception e) {}
		}
		
		System.out.println(" exception thrown?  expected " + shouldThrowException + " actual " + exceptionFlag);

		Assert.assertTrue(exceptionFlag == shouldThrowException, exceptionMsg);
	}
	
	@DataProvider(name="updateInternalUserAuthConfigProvider")
	public Object[][] updateInternalUserAuthConfigProvider() throws Exception
	{
		return new Object[][] {
				{ SYSTEM_OWNER, "Private execution system owner can add internal users to multiple systems at once", false },
		};
	}
	
	@Test(dataProvider="updateInternalUserAuthConfigOfTypeProvider", dependsOnMethods={"updateInternalUserAuthConfigOfType"})
	public void updateInternalUserAuthConfig(String apiUsername, String message, boolean shouldThrowException) throws Exception
	{
		boolean exceptionFlag = false;
		String exceptionMsg = message;
		
		RemoteSystem privateStorageSystem = null;
        RemoteSystem privateExecutionSystem = null;
        
        try 
        {
            privateStorageSystem = getPrivateStorageSystem();
            privateExecutionSystem = getPrivateExecutionSystem();
        
			dao.persist(privateStorageSystem);
			Assert.assertNotNull(privateStorageSystem.getId(), "Private storage system was not saved.");
			Assert.assertTrue(privateStorageSystem.getStorageConfig().getAuthConfigs().size() == 1, "More than one storage auth config present.");
			Assert.assertNotNull(privateStorageSystem.getStorageConfig().getDefaultAuthConfig(), "No default auth storage config was found.");
			
			dao.persist(privateExecutionSystem);
			Assert.assertNotNull(privateExecutionSystem.getId(), "Private execution system was not saved.");
			Assert.assertTrue(privateExecutionSystem.getStorageConfig().getAuthConfigs().size() == 1, "More than one execution auth config present.");
			Assert.assertNotNull(privateExecutionSystem.getStorageConfig().getDefaultAuthConfig(), "No default auth execution config was found.");
			
			AuthConfig defaultAuthConfig = privateStorageSystem.getStorageConfig().getDefaultAuthConfig();
			manager.updateAllInternalUserAuthConfig(
			        privateStorageSystem.getOwner(), 
			        SYSTEM_INTERNAL_USERNAME, 
					"foo", 
					"bar", 
					defaultAuthConfig.getType().name(), 
                    "", 
                    privateStorageSystem.getStorageConfig().getResource(), 
                    privateStorageSystem.getStorageConfig().getZone(), 
                    defaultAuthConfig.getCredentialServer() == null ? null : defaultAuthConfig.getCredentialServer().getEndpoint(), 
                    defaultAuthConfig.getCredentialServer() == null ? 0 : defaultAuthConfig.getCredentialServer().getPort(),
                    defaultAuthConfig.getCredentialServer() == null ? null : defaultAuthConfig.getCredentialServer().getProtocol().name());

			
			for (RemoteSystem system: dao.getUserSystems(apiUsername, false)) 
			{
				Assert.assertTrue(system.getStorageConfig().getAuthConfigs().size() == 2, "Incorrect number of storage auth configs present after adding internal user config.");
				Assert.assertNotNull(system.getStorageConfig().getDefaultAuthConfig(), "No default storage auth config was found.");
				Assert.assertNotEquals(system.getStorageConfig().getDefaultAuthConfig(), 
						system.getStorageConfig().getAuthConfigForInternalUsername(SYSTEM_INTERNAL_USERNAME), 
						"New internal user storage auth config was incorrectly set as default when added.");
				
				if (system instanceof ExecutionSystem)
				{
					Assert.assertTrue(((ExecutionSystem)system).getLoginConfig().getAuthConfigs().size() == 2, "Incorrect number of login auth configs present after adding internal user config.");
					Assert.assertNotNull(((ExecutionSystem)system).getLoginConfig().getDefaultAuthConfig(), "No default login auth config was found.");
					Assert.assertNotEquals(((ExecutionSystem)system).getLoginConfig().getDefaultAuthConfig(), 
							((ExecutionSystem)system).getLoginConfig().getAuthConfigForInternalUsername(SYSTEM_INTERNAL_USERNAME), 
							"New internal user login auth config was incorrectly set as default when added.");
				}
			}
		}
		catch(Exception e){
			exceptionFlag = true;
			exceptionMsg = apiUsername + " " + message;
			if (!shouldThrowException) 
			    Assert.fail(message, e);
		}
		finally {
			try {dao.remove(privateStorageSystem); } catch (Exception e) {}
			try {dao.remove(privateExecutionSystem); } catch (Exception e) {}
		}
		
		System.out.println(" exception thrown?  expected " + shouldThrowException + " actual " + exceptionFlag);

		Assert.assertTrue(exceptionFlag == shouldThrowException, exceptionMsg);
	}
	
	@DataProvider(name="updateInternalUserAuthConfigOnSystemProvider")
	public Object[][] updateInternalUserAuthConfigOnSystemProvider() throws Exception
	{
		return new Object[][] {
				{ getSharedExecutionSystem(), SYSTEM_SHARE_USER, "Shared storage system admin can add internal users", false },
				{ getPrivateStorageSystem(), SYSTEM_OWNER, "Private storage system owner can add internal users", false },
				{ getPrivateExecutionSystem(), SYSTEM_OWNER, "Private execution system owner can add internal users", false },
				{ getPrivateStorageSystem(), SYSTEM_SHARE_USER, "Private storage system non user cannot add internal users", true },
		};
	}
				
	@DataProvider(name="updateInternalUserAuthConfigOnSystemProviderB")
	public Object[][] updateInternalUserAuthConfigOnSystemProviderB() throws Exception
	{
		return new Object[][] {			
				{ getSharedExecutionSystem(), SYSTEM_OWNER, "Private storage system owner can add internal users", false },
				{ getSharedExecutionSystem(), SYSTEM_UNSHARED_USER, "Shared storage system non user cannot add internal users", true },
				{ getPublicStorageSystem(), SYSTEM_OWNER, "Internal users cannot be added to public storage systems", true },
				{ getPublicExecutionSystem(), SYSTEM_OWNER, "Internal users cannot be added to public execution systems", true },
		};
	}
	
	@Test(dataProvider="updateInternalUserAuthConfigOnSystemProvider", dependsOnMethods={"updateInternalUserAuthConfig"})
	public void updateInternalUserAuthConfigOnSystem(RemoteSystem system, String apiUsername, String message, boolean shouldThrowException) throws Exception
	{
		boolean exceptionFlag = false;
		String exceptionMsg = message;
		RemoteSystem updatedSystem = null;
		try 
		{
			dao.persist(system);
			Assert.assertNotNull(system.getId(), "Private storage system was not saved.");
			Assert.assertTrue(system.getStorageConfig().getAuthConfigs().size() == 1, "More than one storage auth config present.");
			Assert.assertNotNull(system.getStorageConfig().getDefaultAuthConfig(), "No default auth storage config was found.");
			
			AuthConfig defaultAuthConfig = system.getStorageConfig().getDefaultAuthConfig();
            manager.updateAllInternalUserAuthConfigOnSystem(
			        system, 
			        apiUsername, 
                    SYSTEM_INTERNAL_USERNAME, 
                    "internalusername", 
                    "password", 
                    defaultAuthConfig.getType().name(), 
                    "", 
                    system.getStorageConfig().getResource(), 
                    system.getStorageConfig().getZone(), 
                    defaultAuthConfig.getCredentialServer() == null ? null : defaultAuthConfig.getCredentialServer().getEndpoint(), 
                    defaultAuthConfig.getCredentialServer() == null ? 0 : defaultAuthConfig.getCredentialServer().getPort(),
                    defaultAuthConfig.getCredentialServer() == null ? null : defaultAuthConfig.getCredentialServer().getProtocol().name());

			
			updatedSystem = dao.findById(system.getId());
			
			Assert.assertTrue(updatedSystem.getStorageConfig().getAuthConfigs().size() == 2, "Incorrect number of storage auth configs present after adding internal user config.");
			Assert.assertNotNull(updatedSystem.getStorageConfig().getDefaultAuthConfig(), "No default storage auth config was found.");
			Assert.assertNotEquals(updatedSystem.getStorageConfig().getDefaultAuthConfig(), 
					updatedSystem.getStorageConfig().getAuthConfigForInternalUsername(SYSTEM_INTERNAL_USERNAME), 
					"New internal user storage auth config was incorrectly set as default when added.");
			
			if (updatedSystem instanceof ExecutionSystem)
			{
				Assert.assertTrue(((ExecutionSystem)updatedSystem).getLoginConfig().getAuthConfigs().size() == 2, "Incorrect number of login auth configs present after adding internal user config.");
				Assert.assertNotNull(((ExecutionSystem)updatedSystem).getLoginConfig().getDefaultAuthConfig(), "No default login auth config was found.");
				Assert.assertNotEquals(((ExecutionSystem)updatedSystem).getLoginConfig().getDefaultAuthConfig(), 
						((ExecutionSystem)updatedSystem).getLoginConfig().getAuthConfigForInternalUsername(SYSTEM_INTERNAL_USERNAME), 
						"New internal user login auth config was incorrectly set as default when added.");
			}
		}
		catch(Exception e){
			exceptionFlag = true;
			exceptionMsg = apiUsername + " " + message;
			if (!shouldThrowException) 
			    Assert.fail(message, e);
		}
		finally {
			try {dao.remove(system); } catch (Exception e) {}
			try {dao.remove(updatedSystem); } catch (Exception e) {}
			try { HibernateUtil.flush(); } catch (Exception e) {}
			try { HibernateUtil.closeSession(); } catch (Exception e) {}
		}
		
		System.out.println(" exception thrown?  expected " + shouldThrowException + " actual " + exceptionFlag);

		Assert.assertTrue(exceptionFlag == shouldThrowException, exceptionMsg);
	}
	
	@Test(dataProvider="updateInternalUserAuthConfigOnSystemProviderB", dependsOnMethods={"updateInternalUserAuthConfigOnSystem"})
	public void updateInternalUserAuthConfigOnSystemB(RemoteSystem system, String apiUsername, String message, boolean shouldThrowException) throws Exception
	{
		updateInternalUserAuthConfigOnSystem(system, apiUsername, message, shouldThrowException);
	}
}
