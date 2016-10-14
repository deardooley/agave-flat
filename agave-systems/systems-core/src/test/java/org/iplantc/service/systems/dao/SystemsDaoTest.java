/**
 * 
 */
package org.iplantc.service.systems.dao;

import org.hibernate.HibernateException;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.systems.model.*;
import org.iplantc.service.systems.model.enumerations.RemoteSystemType;
import org.iplantc.service.systems.model.enumerations.RoleType;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.*;

import java.util.List;

/**
 * @author dooley
 *
 */
public class SystemsDaoTest extends SystemsModelTestCommon {

	private SystemDao dao = new SystemDao();
	private static final String SYSTEM_USER = "testuser";
	
	/**
	 * Initalizes the test db and adds the test app 
	 */
	@BeforeClass
	@Override
	public void beforeClass() throws Exception
	{
		super.beforeClass();
	}
	
	@BeforeMethod
	public void beforeMethod() throws Exception {
		clearSystems();
	}
	
	@AfterMethod
	public void afterMethod() throws Exception {
		clearSystems();
	}
	
	private ExecutionSystem createExecutionSystem() throws Exception {
		ExecutionSystem system = ExecutionSystem.fromJSON(jtd.getTestDataObject(JSONTestDataUtil.TEST_EXECUTION_SYSTEM_FILE));
		system.setOwner(SYSTEM_USER);
		
		return system;
	}
	
	@DataProvider(name="persistenceProvider")
	public Object[][] persistProvider() throws Exception
	{
	    ExecutionSystem gsisshMyproxySystem = ExecutionSystem.fromJSON(jtd.getTestDataObject(SystemsModelTestCommon.EXECUTION_SYSTEM_TEMPLATE_DIR + "/gsissh.example.com.json"));
	    gsisshMyproxySystem.setOwner(SYSTEM_OWNER);
	    
		return new Object[][] {
		    { gsisshMyproxySystem, "System will persist", false }
		};
	}
	
	@Test (dataProvider="persistenceProvider")
	public void persist(RemoteSystem system, String message, Boolean shouldThrowException) throws Exception {
		SystemDao dao = new SystemDao();
		boolean actuallyThrewException = false;
		String exceptionMsg = "";
		try 
		{
			dao.persist(system);
			Assert.assertNotNull(system.getId(), "Failed to generate a system ID.");
			
			ExecutionSystem savedSystem = (ExecutionSystem)dao.findById(system.getId());
			Assert.assertNotNull(savedSystem.getLoginConfig(), "Failed to save login association.");
			Assert.assertNotNull(savedSystem.getLoginConfig().getDefaultAuthConfig(), "Failed to save login auth config association.");
			Assert.assertNotNull(savedSystem.getLoginConfig().getDefaultAuthConfig().getCredentialServer(), "Failed to save login auth config auth system association.");
			Assert.assertNotNull(savedSystem.getStorageConfig(), "Failed to save storage association.");
			Assert.assertNotNull(savedSystem.getStorageConfig().getDefaultAuthConfig(), "Failed to save storage auth config association.");
			Assert.assertNotNull(savedSystem.getStorageConfig().getDefaultAuthConfig().getCredentialServer(), "Failed to save storage auth config auth system association.");
			Assert.assertFalse(savedSystem.getBatchQueues().isEmpty(), "Failed to save batch queue association.");
			
		} 
		catch(Exception e) 
		{
			actuallyThrewException = true;
            exceptionMsg = "Error persisting system " + system.getName() + ": " + message;
            if (actuallyThrewException != shouldThrowException) e.printStackTrace();
		}
		
        System.out.println(" exception thrown?  expected " + shouldThrowException + " actual " + actuallyThrewException);
		Assert.assertTrue(actuallyThrewException == shouldThrowException, exceptionMsg);
	}
	
	@DataProvider(name="updateProvider")
	public Object[][] updateProvider() throws Exception
	{
		return new Object[][] {
			{ createExecutionSystem(), "System will update", false }
		};
	}
	
	@Test (dataProvider="updateProvider", dependsOnMethods = {"persist"})
	public void update(RemoteSystem system, String message, Boolean shouldThrowException) throws Exception {
		SystemDao dao = new SystemDao();
		boolean actuallyThrewException = false;
		String exceptionMsg = "";
		try 
		{
			dao.persist(system);

			Assert.assertNotNull(system.getId(), "System got an id after persisting.");
			
			ExecutionSystem savedSystem = (ExecutionSystem)dao.findById(system.getId());
			
			Assert.assertNotNull(savedSystem, "System was found in db.");
			
			String name = "testname-" + System.currentTimeMillis();
			
			savedSystem.setName(name);
			
			dao.persist(savedSystem);
			
			ExecutionSystem updatedSystem = (ExecutionSystem)dao.findById(system.getId());
			
			Assert.assertNotNull(updatedSystem, "System was found in db.");
			
			Assert.assertTrue(updatedSystem.getName().equals(name), "Failed to generate a system ID.");
		} 
		catch(Exception e) 
		{
			actuallyThrewException = true;
            exceptionMsg = "Error persisting system " + system.getName() + ": " + message;
            if (actuallyThrewException != shouldThrowException) e.printStackTrace();
		}
		
        System.out.println(" exception thrown?  expected " + shouldThrowException + " actual " + actuallyThrewException);
		Assert.assertTrue(actuallyThrewException == shouldThrowException, exceptionMsg);
	}
	
//	@Test(dependsOnMethods={"persist"})
//	public void persistAuthConfig() 
//	{
//		RemoteSystem system = createExecutionSystem();
//		
//		dao.persist(system);
//		
//		
//	}
	
	@DataProvider(name="deleteProvider")
	public Object[][] deleteProvider() throws Exception
	{
		return new Object[][] {
			{ createExecutionSystem(), "System will delete", false }
		};
	}
	
	@Test (dataProvider="deleteProvider", dependsOnMethods = {"persist"})
	public void delete(RemoteSystem system, String message, Boolean shouldThrowException) throws Exception {
		SystemDao dao = new SystemDao();
		boolean actuallyThrewException = false;
		String exceptionMsg = "";
		try 
		{
			dao.persist(system);

			Assert.assertNotNull(system.getId(), "System got an id after persisting.");
			
			Long id = system.getId();
			
			RemoteSystem savedSystem = dao.findById(id);
			
			Assert.assertNotNull(savedSystem, "System was not found in db.");
			
			dao.remove(savedSystem);
			
			RemoteSystem deletedSystem = dao.findById(id);
			
			Assert.assertNull(deletedSystem, "System was found in db.");
		} 
		catch(Exception e) 
		{
			actuallyThrewException = true;
            exceptionMsg = "Error persisting system " + system.getName() + ": " + message;
            if (actuallyThrewException != shouldThrowException) e.printStackTrace();
		}
		
        System.out.println(" exception thrown?  expected " + shouldThrowException + " actual " + actuallyThrewException);
		Assert.assertTrue(actuallyThrewException == shouldThrowException, exceptionMsg);
	}
	
	@DataProvider(name="findByExampleSystemProvider")
	public Object[][] findByExampleSystemProvider() throws Exception
	{
		return new Object[][] {
			{ createExecutionSystem(), "System was found by example", false }
		};
	}
	
	@Test (dataProvider="findByExampleSystemProvider", dependsOnMethods = {"getUserSystems"})
	public void findByExampleSystemProviderTest(RemoteSystem system, String message, Boolean shouldThrowException) throws Exception {
		SystemDao dao = new SystemDao();
		boolean actuallyThrewException = false;
		String exceptionMsg = "";
		try 
		{
			dao.persist(system);

			Assert.assertNotNull(system.getId(), "System got an id after persisting.");
			
			List<RemoteSystem> systems = dao.findByExample("name", system.getName());
			
			Assert.assertNotNull(systems, "Systems were found matching example.");
			
			Assert.assertTrue(systems.size() == 1, "Exactly one match was found");
			
			Assert.assertTrue(systems.get(0).getId().equals(system.getId()), "Match found was the correct system");
		} 
		catch(Exception e) 
		{
			actuallyThrewException = true;
            exceptionMsg = "Error persisting system " + system.getName() + ": " + message;
            if (actuallyThrewException != shouldThrowException) e.printStackTrace();
		}
		
        System.out.println(" exception thrown?  expected " + shouldThrowException + " actual " + actuallyThrewException);
		Assert.assertTrue(actuallyThrewException == shouldThrowException, exceptionMsg);
	}
	
	public void udpateBatchQueue() throws Exception
	{
		SystemDao dao = new SystemDao();
		try {
			ExecutionSystem system = createExecutionSystem();
			dao.persist(system);
			Assert.assertNotNull(system.getId(), "System got an id after persisting.");
			
			system.getBatchQueues().clear();
			dao.persist(system);
			Assert.assertTrue(system.getBatchQueues().isEmpty(), "Batch queues were not deleted");
			
			BatchQueue queue = new BatchQueue("test", (long)10, 10.0);
			system.addBatchQueue(queue);
			dao.persist(system);
			Assert.assertEquals(system.getBatchQueues().size(), 1,  "Incorrect number of queues found");
			
			BatchQueue savedQueue = system.getBatchQueues().iterator().next();
			system.removeBatchQueue(savedQueue);
			dao.persist(system);
			Assert.assertTrue(system.getBatchQueues().isEmpty(), "Batch queue was not deleted");
		}
		catch (Throwable t) {
			Assert.fail("Failed to update batch queues", t);
		}
		
	}
	
	@DataProvider(name="persistenceSystemUserDefaultSystemProvider")
	public Object[][] persistenceSystemUserDefaultSystemProvider() throws Exception
	{
		return new Object[][] {
			{ "bob", "System will persist", false }
		};
	}
	
	@Test (dataProvider="persistenceSystemUserDefaultSystemProvider", dependsOnMethods = {"getUserSystems"})
	public void persistSystemUserDefaultSystem(String username, String message, Boolean shouldThrowException)
	throws Exception 
	{
		RemoteSystem system = createExecutionSystem();
		SystemDao dao = new SystemDao();
		
		boolean actuallyThrewException = false;
		String exceptionMsg = "";
		
		try 
		{
			system.getUsersUsingAsDefault().add(username);
			dao.persist(system);
			
			Assert.assertNotNull(system.getId(), "Failed to generate a system ID.");
			
			system = dao.findById(system.getId());
			
			Assert.assertTrue(system.getUsersUsingAsDefault().size() == 1, "Failed to save UserDefaultSystem.");
			Assert.assertTrue(system.getUsersUsingAsDefault().contains(username), "UserDefaultSystem was not saved.");
		} 
		catch(HibernateException e) {
			e.printStackTrace();
		}
		catch(Exception e) 
		{
			actuallyThrewException = true;
            exceptionMsg = "Error persisting system " + system.getName() + ": " + message;
            if (actuallyThrewException != shouldThrowException) e.printStackTrace();
		}
		
        System.out.println(" exception thrown?  expected " + shouldThrowException + " actual " + actuallyThrewException);
		Assert.assertTrue(actuallyThrewException == shouldThrowException, exceptionMsg);
	}
	
	@Test(dependsOnMethods = {"persistSystemUserDefaultSystem"})
	public void deleteSystemUserDefaultSystemTest()
	throws Exception 
	{
		RemoteSystem system = createExecutionSystem();
		SystemDao dao = new SystemDao();
		
		boolean actuallyThrewException = false;
		String exceptionMsg = "";
		
		try 
		{
			system.getUsersUsingAsDefault().add("bob");
			dao.persist(system);
			
			Assert.assertNotNull(system.getId(), "Failed to generate a system ID.");
			
			system = dao.findById(system.getId());
			
			Assert.assertTrue(system.getUsersUsingAsDefault().size() == 1, "Failed to save UserDefaultSystem.");
			Assert.assertTrue(system.getUsersUsingAsDefault().contains("bob"), "UserDefaultSystem was not saved.");
			
			system.getUsersUsingAsDefault().clear();
			dao.persist(system);
			
			system = dao.findById(system.getId());
			Assert.assertTrue(system.getUsersUsingAsDefault().size() == 0, "UserDefaultSystem were not deleted.");
			
			
		} 
		catch(Exception e) 
		{
			actuallyThrewException = true;
            exceptionMsg = "Error persisting system " + system.getName() + ": Failed to delete default user";
            e.printStackTrace();
		}
		
        System.out.println(" exception thrown?  expected false actual " + actuallyThrewException);
		Assert.assertFalse(actuallyThrewException, exceptionMsg);
	}
	
	@DataProvider(name="persistenceSystemRoleProvider")
	public Object[][] persistenceSystemRoleProvider() throws Exception
	{
		return new Object[][] {
			{ RoleType.USER, "System user role will persist", false },
			{ RoleType.PUBLISHER, "System publisher role will persist", false },
			{ RoleType.ADMIN, "System admin role will persist", false },
			{ RoleType.OWNER, "System owner role will persist", false },
		};
	}
	
	@Test (dataProvider="persistenceSystemRoleProvider", dependsOnMethods = {"persistSystemUserDefaultSystem", "deleteSystemUserDefaultSystemTest"})
	public void persistSystemRole(RoleType role, String message, Boolean shouldThrowException)
	throws Exception 
	{
		RemoteSystem system = createExecutionSystem();
		SystemDao dao = new SystemDao();
		
		boolean actuallyThrewException = false;
		String exceptionMsg = "";
		
		try 
		{
			SystemRole userRole = new SystemRole("bob", role);
			system.addRole(userRole);
			dao.persist(system);
			
			Assert.assertNotNull(system.getId(), "Failed to generate a system ID.");
			
			system = dao.findById(system.getId());
			
			Assert.assertTrue(system.getRoles().size() == 1, "Failed to save role.");
			//Assert.assertTrue(system.getRoles().contains(new SystemRole("bob", role)), "Role was not saved.");
		} 
		catch(Exception e) 
		{
			actuallyThrewException = true;
            exceptionMsg = "Error persisting system " + system.getName() + ": " + message;
            if (actuallyThrewException != shouldThrowException) e.printStackTrace();
		}
		
        System.out.println(" exception thrown?  expected " + shouldThrowException + " actual " + actuallyThrewException);
		Assert.assertTrue(actuallyThrewException == shouldThrowException, exceptionMsg);
	}
	
	@DataProvider(name="updateSystemRoleProvider")
	public Object[][] updateSystemRoleProvider() throws Exception
	{
		return new Object[][] {
			{ RoleType.USER, RoleType.PUBLISHER, "Role changes will persist", false }
		};
	}
	
	@Test (dataProvider="updateSystemRoleProvider", dependsOnMethods = {"persistSystemRole"})
	public void updateSystemRole(RoleType originalType, RoleType updateType, String message, Boolean shouldThrowException)
	throws Exception 
	{
		RemoteSystem system = createExecutionSystem();
		SystemDao dao = new SystemDao();
		
		boolean actuallyThrewException = false;
		String exceptionMsg = "";
		
		try 
		{
			SystemRole role = new SystemRole("bob", originalType);
			system.addRole(role);
			dao.persist(system);
			
			Assert.assertNotNull(system.getId(), "Failed to generate a system ID.");
			
			system = dao.findById(system.getId());
			
			Assert.assertTrue(system.getRoles().size() == 1, "Failed to save permission.");
			// Assert.assertTrue(system.getRoles().contains(new SystemRole("bob", originalType)), "Permission was not saved.");
			
			system.getRoles().remove(0);
			system.getRoles().add(new SystemRole("bob", updateType));
			dao.persist(system);
			
			system = dao.findById(system.getId());
			Assert.assertTrue(system.getRoles().size() == 1, "update did not created a new entry.");
			Assert.assertEquals(system.getRoles().iterator().next().getRole(), updateType, "updated role was not saved.");
			
		} 
		catch(Exception e) 
		{
			actuallyThrewException = true;
            exceptionMsg = "Error persisting system " + system.getName() + ": " + message;
            if (actuallyThrewException != shouldThrowException) e.printStackTrace();
		}
		
        System.out.println(" exception thrown?  expected " + shouldThrowException + " actual " + actuallyThrewException);
		Assert.assertTrue(actuallyThrewException == shouldThrowException, exceptionMsg);
	}
	
	@Test (dataProvider="updateSystemRoleProvider", dependsOnMethods = {"persistSystemRole", "updateSystemRole"})
	public void deleteSystemRoleTest(RoleType originalType, RoleType updateType, String message, Boolean shouldThrowException)
	throws Exception 
	{
		RemoteSystem system = createExecutionSystem();
		SystemDao dao = new SystemDao();
		
		boolean actuallyThrewException = false;
		String exceptionMsg = "";
		
		try 
		{
			SystemRole pem = new SystemRole("bob", originalType);
			system.addRole(pem);
			dao.persist(system);
			
			Assert.assertNotNull(system.getId(), "Failed to generate a system ID.");
			
			system = dao.findById(system.getId());
			
			Assert.assertTrue(system.getRoles().size() == 1, "Failed to save role.");
			Assert.assertTrue(system.getRoles().contains(pem), "Role was not saved.");
			
			system.getRoles().clear();
			dao.persist(system);
			
			system = dao.findById(system.getId());
			Assert.assertTrue(system.getRoles().size() == 0, "Roles were not deleted.");
			
			
		} 
		catch(Exception e) 
		{
			actuallyThrewException = true;
            exceptionMsg = "Error persisting system " + system.getName() + ": " + message;
            if (actuallyThrewException != shouldThrowException) e.printStackTrace();
		}
		
        System.out.println(" exception thrown?  expected " + shouldThrowException + " actual " + actuallyThrewException);
		Assert.assertTrue(actuallyThrewException == shouldThrowException, exceptionMsg);
	}
	
	@Test(dependsOnMethods = {"getUserSystems"})
	public void findUserDefaultSystem() throws Exception {
		
		ExecutionSystem privateSystem = null;
		ExecutionSystem publicSystem = null;
		try
		{
			privateSystem = ExecutionSystem.fromJSON(jtd.getTestDataObject(JSONTestDataUtil.TEST_EXECUTION_SYSTEM_FILE));
			privateSystem.setOwner(SYSTEM_USER);
			privateSystem.getUsersUsingAsDefault().add(SYSTEM_USER);
			dao.persist(privateSystem);
			Assert.assertNotNull(privateSystem.getId(), "Private system was not saved.");
			
			publicSystem = ExecutionSystem.fromJSON(jtd.getTestDataObject(JSONTestDataUtil.TEST_EXECUTION_SYSTEM_FILE));
			publicSystem.setOwner(SYSTEM_USER);
			publicSystem.setSystemId(publicSystem.getSystemId() + "-public");
			publicSystem.setPubliclyAvailable(true);
			publicSystem.setGlobalDefault(true);
			dao.persist(publicSystem);
			Assert.assertNotNull(privateSystem.getId(), "Public system was not saved.");
			
			Assert.assertEquals(dao.findUserDefaultSystem(SYSTEM_USER, RemoteSystemType.EXECUTION), privateSystem, 
					"Setting user default system did not result in it being returned as the user default system.");
			
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw e;
		}
	}
	
	@Test(dependsOnMethods = {"findUserDefaultSystem"})
    public void findUserDefaultSystemHonorsTenant() throws Exception {
        
        ExecutionSystem t1PrivateSystem = null;
        ExecutionSystem t1PublicSystem = null;
        ExecutionSystem t2PrivateSystem = null;
        ExecutionSystem t2PublicSystem = null;
        try
        {
            TenancyHelper.setCurrentTenantId("tenant1");
            t1PrivateSystem = ExecutionSystem.fromJSON(jtd.getTestDataObject(JSONTestDataUtil.TEST_EXECUTION_SYSTEM_FILE));
            t1PrivateSystem.setOwner(SYSTEM_USER);
            t1PrivateSystem.getUsersUsingAsDefault().add(SYSTEM_USER);
            dao.persist(t1PrivateSystem);
            Assert.assertNotNull(t1PrivateSystem.getId(), "Private system in tenant1 was not saved.");
            
            t1PublicSystem = ExecutionSystem.fromJSON(jtd.getTestDataObject(JSONTestDataUtil.TEST_EXECUTION_SYSTEM_FILE));
            t1PublicSystem.setOwner(SYSTEM_USER);
            t1PublicSystem.setSystemId(t1PublicSystem.getSystemId() + "-public");
            t1PublicSystem.setPubliclyAvailable(true);
            t1PublicSystem.setGlobalDefault(true);
            dao.persist(t1PublicSystem);
            Assert.assertNotNull(t1PublicSystem.getId(), "Public system in tenant1 was not saved.");
            
            TenancyHelper.setCurrentTenantId("tenant2");
            t2PrivateSystem = ExecutionSystem.fromJSON(jtd.getTestDataObject(JSONTestDataUtil.TEST_EXECUTION_SYSTEM_FILE));
            t2PrivateSystem.setOwner(SYSTEM_USER);
            t2PrivateSystem.getUsersUsingAsDefault().add(SYSTEM_USER);
            dao.persist(t2PrivateSystem);
            Assert.assertNotNull(t2PrivateSystem.getId(), "Private system in tenant2 was not saved.");
            
            t2PublicSystem = ExecutionSystem.fromJSON(jtd.getTestDataObject(JSONTestDataUtil.TEST_EXECUTION_SYSTEM_FILE));
            t2PublicSystem.setOwner(SYSTEM_USER);
            t2PublicSystem.setSystemId(t2PublicSystem.getSystemId() + "-public");
            t2PublicSystem.setPubliclyAvailable(true);
            t2PublicSystem.setGlobalDefault(true);
            dao.persist(t2PublicSystem);
            Assert.assertNotNull(t2PublicSystem.getId(), "Public system in tenant2 was not saved.");
            
            Assert.assertEquals(dao.findUserDefaultSystem(SYSTEM_USER, RemoteSystemType.EXECUTION), t2PrivateSystem, 
                    "Setting user default system in tenant2 did not result in it being returned as the user default system.");
            
            TenancyHelper.setCurrentTenantId("tenant1");
            Assert.assertEquals(dao.findUserDefaultSystem(SYSTEM_USER, RemoteSystemType.EXECUTION), t1PrivateSystem, 
                    "Setting user default system in tenant1 did not result in it being returned as the user default system.");
            
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw e;
        }
    }
	
	@Test(dependsOnMethods = {"persist", "delete", "isSystemIdUnique"})
	public void getUserSystems() throws Exception {
		
		ExecutionSystem privateExecutionSystem = null;
		ExecutionSystem publicExecutionSystem = null;
		StorageSystem privateStorageSystem = null;
		try
		{
			clearSystems();
			privateExecutionSystem = ExecutionSystem.fromJSON(jtd.getTestDataObject(JSONTestDataUtil.TEST_EXECUTION_SYSTEM_FILE));
			privateExecutionSystem.setOwner(SYSTEM_USER);
			privateExecutionSystem.getUsersUsingAsDefault().add(SYSTEM_USER);
			dao.persist(privateExecutionSystem);
			Assert.assertNotNull(privateExecutionSystem.getId(), "Private execution system was not saved.");
			
			privateStorageSystem = StorageSystem.fromJSON(jtd.getTestDataObject(JSONTestDataUtil.TEST_STORAGE_SYSTEM_FILE));
			privateStorageSystem.setOwner(SYSTEM_USER);
			privateStorageSystem.getUsersUsingAsDefault().add(SYSTEM_USER);
			dao.persist(privateStorageSystem);
			Assert.assertNotNull(privateStorageSystem.getId(), "Private storage system was not saved.");
			
			publicExecutionSystem = ExecutionSystem.fromJSON(jtd.getTestDataObject(JSONTestDataUtil.TEST_EXECUTION_SYSTEM_FILE));
			publicExecutionSystem.setOwner(SYSTEM_USER);
			publicExecutionSystem.setSystemId(publicExecutionSystem.getSystemId() + "-public");
			publicExecutionSystem.setPubliclyAvailable(true);
			publicExecutionSystem.setGlobalDefault(true);
			dao.persist(publicExecutionSystem);
			Assert.assertNotNull(privateExecutionSystem.getId(), "Public execution system was not saved.");
			
			List<RemoteSystem> systems = dao.getUserSystems(SYSTEM_USER, true);
			Assert.assertTrue(systems.size() == 3, "Not all public and private systems were returned.");
			
			systems = dao.getUserSystems(SYSTEM_USER, false);
			Assert.assertTrue(systems.size() == 2, "Exclusively private systems were not returned.");
			Assert.assertTrue(systems.contains(privateExecutionSystem), "Private execution system was not returned.");
			Assert.assertTrue(systems.contains(privateStorageSystem), "Private storage system was not returned.");
			
			systems = dao.getUserSystems(SYSTEM_USER, true, RemoteSystemType.EXECUTION);
			Assert.assertTrue(systems.size() == 2, "Incorrect number of results was returned.");
			Assert.assertTrue(systems.get(0).getType().equals(RemoteSystemType.EXECUTION), "Results were not only execution systems.");
			Assert.assertTrue(systems.get(1).getType().equals(RemoteSystemType.EXECUTION), "Results were not only execution systems.");
			
			systems = dao.getUserSystems(SYSTEM_USER, false, RemoteSystemType.EXECUTION);
			Assert.assertTrue(systems.size() == 1, "Incorrect number of results was returned.");
			Assert.assertTrue(systems.get(0).getType().equals(RemoteSystemType.EXECUTION), "Not only the private execution system was returned.");
			
			systems = dao.getUserSystems(SYSTEM_USER, true, RemoteSystemType.STORAGE);
			Assert.assertTrue(systems.size() == 1, "Incorrect number of results was returned.");
			Assert.assertTrue(systems.get(0).getType().equals(RemoteSystemType.STORAGE), "Result was not a storage system");
			
			systems = dao.getUserSystems(SYSTEM_USER, false, RemoteSystemType.STORAGE);
			Assert.assertTrue(systems.size() == 1, "Incorrect number of results was returned.");
			Assert.assertTrue(systems.get(0).getType().equals(RemoteSystemType.STORAGE), "Result was not a storage system");
		}
		catch (HibernateException e) {
			e.printStackTrace();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw e;
		}
	}
	
	@Test(dependsOnMethods = {"getUserSystems"})
	public void getSharedUserSystems() throws Exception {
		
		ExecutionSystem privateExecutionSystem = null;
		ExecutionSystem publicExecutionSystem = null;
		StorageSystem privateStorageSystem = null;
		try
		{
			for(RemoteSystem system: dao.getAll()) {
				dao.remove(system);
			}
			publicExecutionSystem = ExecutionSystem.fromJSON(jtd.getTestDataObject(JSONTestDataUtil.TEST_EXECUTION_SYSTEM_FILE));
			publicExecutionSystem.setOwner(SYSTEM_USER);
			publicExecutionSystem.setSystemId(publicExecutionSystem.getSystemId() + "-public");
			publicExecutionSystem.setPubliclyAvailable(true);
			publicExecutionSystem.setGlobalDefault(true);
			dao.persist(publicExecutionSystem);
			Assert.assertNotNull(publicExecutionSystem.getId(), "Public execution system was not saved.");
			
			List<RemoteSystem> publicSystems = dao.getUserSystems(SYSTEM_SHARE_USER, true);
			Assert.assertTrue(publicSystems.size() == 1, "Too many systems returned for shared user");
			Assert.assertTrue(publicSystems.get(0).getSystemId().equals(publicExecutionSystem.getSystemId()), "Public system not returned for shared user.");
			
			privateExecutionSystem = ExecutionSystem.fromJSON(jtd.getTestDataObject(JSONTestDataUtil.TEST_EXECUTION_SYSTEM_FILE));
			privateExecutionSystem.setOwner(SYSTEM_USER);
			privateExecutionSystem.addRole(new SystemRole(SYSTEM_SHARE_USER, RoleType.USER));
			dao.persist(privateExecutionSystem);
			Assert.assertNotNull(privateExecutionSystem.getId(), "Private execution system was not saved.");
			
			List<RemoteSystem> sharedSystems = dao.getUserSystems(SYSTEM_SHARE_USER, true);
			Assert.assertTrue(sharedSystems.size() == 2, "Too many systems returned for shared user");
			
			boolean found = false;
			for(RemoteSystem system: sharedSystems) {
				if (system.getSystemId().equals(privateExecutionSystem.getSystemId())) {
					found = true;
					Assert.assertEquals(system.getUserRole(SYSTEM_SHARE_USER).getRole(), RoleType.USER, 
							"Shared user returned with incorrect permissions on execution system.");
				}
			}
			Assert.assertTrue(found, "Shared execution system not returned for shared user.");
			
			privateStorageSystem = StorageSystem.fromJSON(jtd.getTestDataObject(JSONTestDataUtil.TEST_STORAGE_SYSTEM_FILE));
			privateStorageSystem.setOwner(SYSTEM_USER);
			privateStorageSystem.addRole(new SystemRole(SYSTEM_SHARE_USER, RoleType.USER));
			dao.persist(privateStorageSystem);
			Assert.assertNotNull(privateStorageSystem.getId(), "Private storage system was not saved.");
			
			List<RemoteSystem> sharedSystems2 = dao.getUserSystems(SYSTEM_SHARE_USER, true);
			Assert.assertTrue(sharedSystems2.size() == 3, "Too many systems returned for shared user");
			
			found = false;
			for(RemoteSystem system: sharedSystems2) {
				if (system.getSystemId().equals(privateStorageSystem.getSystemId())) {
					found = true;
					Assert.assertEquals(system.getUserRole(SYSTEM_SHARE_USER).getRole(), RoleType.USER, 
							"Shared user returned with incorrect permissions on shared system.");
				}
			}
			Assert.assertTrue(found, 
					"Shared storage system not returned for shared user.");
		}
		catch (HibernateException e) {
			e.printStackTrace();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw e;
		}
	}
	
	@Test(dependsOnMethods = {"persist", "delete"})
	public void isSystemIdUnique() throws Exception {
		
		ExecutionSystem privateSystem = null;
		try
		{
			privateSystem = ExecutionSystem.fromJSON(jtd.getTestDataObject(JSONTestDataUtil.TEST_EXECUTION_SYSTEM_FILE));
			privateSystem.setOwner(SYSTEM_USER);
			privateSystem.getUsersUsingAsDefault().add(SYSTEM_USER);
			dao.persist(privateSystem);
			
			Assert.assertNotNull(privateSystem.getId(), "Private system was not saved.");
			
			Assert.assertFalse(dao.isSystemIdUnique(privateSystem.getSystemId()),
					"Setting user default system did not result in it being returned as the user default system.");
			
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw e;
		}
	}
	
	@Test(dependsOnMethods = {"findUserDefaultSystem"})
    public void getGlobalDefaultSystemHonorsTenant() throws Exception {
        
        ExecutionSystem t1PrivateSystem = null;
        ExecutionSystem t1PublicSystem = null;
        ExecutionSystem t2PrivateSystem = null;
        ExecutionSystem t2PublicSystem = null;
        try
        {
            TenancyHelper.setCurrentTenantId("tenant1");
            t1PrivateSystem = ExecutionSystem.fromJSON(jtd.getTestDataObject(JSONTestDataUtil.TEST_EXECUTION_SYSTEM_FILE));
            t1PrivateSystem.setOwner(SYSTEM_USER);
            t1PrivateSystem.getUsersUsingAsDefault().add(SYSTEM_USER);
            dao.persist(t1PrivateSystem);
            Assert.assertNotNull(t1PrivateSystem.getId(), "Private system in tenant1 was not saved.");
            
            t1PublicSystem = ExecutionSystem.fromJSON(jtd.getTestDataObject(JSONTestDataUtil.TEST_EXECUTION_SYSTEM_FILE));
            t1PublicSystem.setOwner(SYSTEM_USER);
            t1PublicSystem.setSystemId(t1PublicSystem.getSystemId() + "-public");
            t1PublicSystem.setPubliclyAvailable(true);
            t1PublicSystem.setGlobalDefault(true);
            dao.persist(t1PublicSystem);
            Assert.assertNotNull(t1PublicSystem.getId(), "Public system in tenant1 was not saved.");
            
            TenancyHelper.setCurrentTenantId("tenant2");
            t2PrivateSystem = ExecutionSystem.fromJSON(jtd.getTestDataObject(JSONTestDataUtil.TEST_EXECUTION_SYSTEM_FILE));
            t2PrivateSystem.setOwner(SYSTEM_USER);
            t2PrivateSystem.getUsersUsingAsDefault().add(SYSTEM_USER);
            dao.persist(t2PrivateSystem);
            Assert.assertNotNull(t2PrivateSystem.getId(), "Private system in tenant2 was not saved.");
            
            t2PublicSystem = ExecutionSystem.fromJSON(jtd.getTestDataObject(JSONTestDataUtil.TEST_EXECUTION_SYSTEM_FILE));
            t2PublicSystem.setOwner(SYSTEM_USER);
            t2PublicSystem.setSystemId(t2PublicSystem.getSystemId() + "-public");
            t2PublicSystem.setPubliclyAvailable(true);
            t2PublicSystem.setGlobalDefault(true);
            dao.persist(t2PublicSystem);
            Assert.assertNotNull(t2PublicSystem.getId(), "Public system in tenant2 was not saved.");
            
            Assert.assertEquals(dao.getGlobalDefaultSystemForTenant(RemoteSystemType.EXECUTION, "tenant2"), t2PublicSystem, 
                    "Global default system for tenant2 was not returned while in tenant2.");
            Assert.assertEquals(dao.getGlobalDefaultSystemForTenant(RemoteSystemType.EXECUTION, "tenant1"), t1PublicSystem, 
                    "Global default system for tenant1 was not returned while in tenant 2.");
            
            TenancyHelper.setCurrentTenantId("tenant1");
            Assert.assertEquals(dao.getGlobalDefaultSystemForTenant(RemoteSystemType.EXECUTION, "tenant2"), t2PublicSystem, 
                    "Global default system for tenant2 was not returned while in tenant1.");
            Assert.assertEquals(dao.getGlobalDefaultSystemForTenant(RemoteSystemType.EXECUTION, "tenant1"), t1PublicSystem, 
                    "Global default system for tenant1 was not returned while in tenant1.");
            
            
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw e;
        }
    }
    
	
	@Test(dependsOnMethods = {"getUserSystems"})
	public void findUserSystemBySystemId() throws Exception {
		
		ExecutionSystem privateExecutionSystem = null;
		ExecutionSystem publicExecutionSystem = null;
		StorageSystem privateStorageSystem = null;
		StorageSystem publicStorageSystem = null;
		try
		{
			privateExecutionSystem = ExecutionSystem.fromJSON(jtd.getTestDataObject(JSONTestDataUtil.TEST_EXECUTION_SYSTEM_FILE));
			privateExecutionSystem.setOwner(SYSTEM_USER);
			privateExecutionSystem.getUsersUsingAsDefault().add(SYSTEM_USER);
			dao.persist(privateExecutionSystem);
			Assert.assertNotNull(privateExecutionSystem.getId(), "Private execution system was not saved.");
			
			publicExecutionSystem = ExecutionSystem.fromJSON(jtd.getTestDataObject(JSONTestDataUtil.TEST_EXECUTION_SYSTEM_FILE));
			publicExecutionSystem.setOwner(SYSTEM_USER + "-public");
			publicExecutionSystem.setSystemId(publicExecutionSystem.getSystemId() + "-public");
			publicExecutionSystem.setPubliclyAvailable(true);
			publicExecutionSystem.setGlobalDefault(true);
			dao.persist(publicExecutionSystem);
			Assert.assertNotNull(privateExecutionSystem.getId(), "Public execution system was not saved.");
			
			privateStorageSystem = StorageSystem.fromJSON(jtd.getTestDataObject(JSONTestDataUtil.TEST_STORAGE_SYSTEM_FILE));
			privateStorageSystem.setOwner(SYSTEM_USER);
			privateStorageSystem.getUsersUsingAsDefault().add(SYSTEM_USER);
			dao.persist(privateStorageSystem);
			Assert.assertNotNull(privateStorageSystem.getId(), "Private storage system was not saved.");
			
			publicStorageSystem = StorageSystem.fromJSON(jtd.getTestDataObject(JSONTestDataUtil.TEST_STORAGE_SYSTEM_FILE));
			publicStorageSystem.setOwner(SYSTEM_USER + "-public");
			publicStorageSystem.setSystemId(publicStorageSystem.getSystemId() + "-public");
			publicStorageSystem.setPubliclyAvailable(true);
			publicStorageSystem.setGlobalDefault(true);
			dao.persist(publicStorageSystem);
			Assert.assertNotNull(publicStorageSystem.getId(), "Public storage system was not saved.");
			
			RemoteSystem userSystem = dao.findUserSystemBySystemId(SYSTEM_USER, privateExecutionSystem.getSystemId());
			Assert.assertNotNull(userSystem, "findUserSystemBySystemId Failed to retrieve private user execution system");
			Assert.assertEquals(userSystem.getSystemId(), privateExecutionSystem.getSystemId(), "findUserSystemBySystemId returned the wrong private execution system");
			
			userSystem = dao.findUserSystemBySystemId(SYSTEM_USER, publicExecutionSystem.getSystemId());
			Assert.assertNotNull(userSystem, "findUserSystemBySystemId Failed to retrieve public user execution system");
			Assert.assertEquals(userSystem.getSystemId(), publicExecutionSystem.getSystemId(), "findUserSystemBySystemId returned the wrong public execution system");
			
			userSystem = dao.findUserSystemBySystemId(SYSTEM_USER, privateStorageSystem.getSystemId());
			Assert.assertNotNull(userSystem, "findUserSystemBySystemId Failed to retrieve private user storage system");
			Assert.assertEquals(userSystem.getSystemId(), privateStorageSystem.getSystemId(), "findUserSystemBySystemId returned the wrong private storage system");
			
			userSystem = dao.findUserSystemBySystemId(SYSTEM_USER, publicStorageSystem.getSystemId());
			Assert.assertNotNull(userSystem, "findUserSystemBySystemId Failed to retrieve public user storage system");
			Assert.assertEquals(userSystem.getSystemId(), publicStorageSystem.getSystemId(), "findUserSystemBySystemId returned the wrong public storage system");
			
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw e;
		}
	}
	
	@Test
    public void executionUpdateSystemLoginPasswordEncryptionTest()
    {
    	try 
		{
    		JSONObject systemJson = jtd.getTestDataObject(JSONTestDataUtil.TEST_EXECUTION_SYSTEM_FILE);
    		
    		// get password from json
    		JSONObject authJson = systemJson.getJSONObject("login").getJSONObject("auth");
	    	Assert.assertNotNull(authJson, "No auth config associated with this login config.");
	    	
	    	String originalPassword  = authJson.getString("password");
	    	Assert.assertNotNull(authJson, "No password associated with this auth config.");
	    	
	    	// get password from deserialized auth config
	    	ExecutionSystem originalSystem = createExecutionSystem();
	    	dao.persist(originalSystem);
	    	
	    	ExecutionSystem originalSavedSystem = (ExecutionSystem)dao.findBySystemId(originalSystem.getSystemId());
	    	// verify original encryption after persisting
	    	AuthConfig originalSavedauthConfig = originalSavedSystem.getLoginConfig().getDefaultAuthConfig();
	    	Assert.assertNotNull(originalSavedauthConfig, "No login config associated with original system.");
	    	
	    	Assert.assertNotEquals(originalSavedSystem, originalSavedauthConfig.getPassword(), "Login password was not encrypted during original deserialization");
	    	
	    	String originalSavedSalt = originalSystem.getEncryptionKeyForAuthConfig(originalSavedauthConfig);
	    	String originalSavedclearTextPassword = originalSavedauthConfig.getClearTextPassword(originalSavedSalt);
	    
	    	Assert.assertEquals(originalPassword, originalSavedclearTextPassword, "Decrypted original login password does not match original.");
	    	
	    	// update the execution system and verify original encryption after persisting
	    	ExecutionSystem updatedSystem = ExecutionSystem.fromJSON(systemJson, originalSavedSystem);
	    	dao.merge(updatedSystem);
	    	ExecutionSystem updatedSavedSystem = (ExecutionSystem)dao.findBySystemId(updatedSystem.getSystemId());
	    	AuthConfig updatedSavedAuthConfig = updatedSavedSystem.getLoginConfig().getDefaultAuthConfig();
	    	Assert.assertNotNull(updatedSavedAuthConfig, "No login config associated with updated system.");
	    	
	    	Assert.assertNotEquals(originalPassword, updatedSavedAuthConfig.getPassword(), "Login password was not encrypted during updated deserialization");
	    	
	    	String updatedSavedSalt = updatedSavedSystem.getEncryptionKeyForAuthConfig(updatedSavedAuthConfig);
	    	String updatedSavedClearTextPassword = updatedSavedAuthConfig.getClearTextPassword(updatedSavedSalt);
	    
	    	Assert.assertEquals(originalPassword, updatedSavedClearTextPassword, "Decrypted updated login password does not match original.");
	    	
	    	
		} 
    	catch (Exception e) {
			Assert.fail("Encryption test failed to match login passwords after update.", e);
		}
    }
}
