package org.iplantc.service.profile.manager;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.iplantc.service.common.persistence.HibernateUtil;
import org.iplantc.service.profile.ModelTestCommon;
import org.iplantc.service.profile.TestDataHelper;
import org.iplantc.service.profile.dao.InternalUserDao;
import org.iplantc.service.profile.exceptions.ProfileException;
import org.iplantc.service.profile.model.InternalUser;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@SuppressWarnings("unused")
@Test(groups={"integration"})
public class InternalUserManagerTest extends ModelTestCommon {
	
	private static final String INTERNAL_USER_CREATOR = "testuser";
	
	private static final String INTERNAL_USER_STRANGER = "bob";
	private static final String SYSTEM_PUBLIC_USER = "public";
	private static final String SYSTEM_UNSHARED_USER = "dan";
	
	private InternalUserManager manager;
	private InternalUserDao dao;
	
	private InternalUser internalUser;
	
	@BeforeClass
    public void setUp() throws Exception 
    {
        super.setUp();
        
        initDb();
        
        manager = new InternalUserManager();
        
        internalUser = InternalUser.fromJSON( dataHelper.getTestDataObject(
        		TestDataHelper.TEST_INTERNAL_USER_FILE));
        internalUser.setCreatedBy(INTERNAL_USER_CREATOR);
                
    }
	
	private void initDb() throws Exception 
	{
		Configuration configuration = new Configuration().configure();
		HibernateUtil.rebuildSessionFactory(configuration);
		dao = new InternalUserDao();
		
		try
        {
            HibernateUtil.beginTransaction();
            Session session = HibernateUtil.getSession();
            session.clear();
            HibernateUtil.disableAllFilters();
            session.createQuery("DELETE InternalUser").executeUpdate();
            session.flush();
        }
        catch (HibernateException ex)
        {
            try
            {
                if (HibernateUtil.getSession().isOpen()) {
                    HibernateUtil.rollbackTransaction();
                }
            }
            catch (Exception e) {}

            throw new ProfileException(ex);
        }
        finally {
            try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
        }
	}
    
//    @BeforeMethod
//	public void setUpMethod() throws Exception {
//		
//	}
//    
//    @Test
//    public void addInternalUser() {
//      throw new RuntimeException("Test not implemented");
//    }
//
//    @Test
//    public void deleteInternalUser() {
//      throw new RuntimeException("Test not implemented");
//    }
//
//    @Test
//    public void getActiveInternalUsers() {
//      throw new RuntimeException("Test not implemented");
//    }
//
//    @Test
//    public void getInternalUser() {
//      throw new RuntimeException("Test not implemented");
//    }
//
//    @Test
//    public void updateInternalUser() {
//      throw new RuntimeException("Test not implemented");
//    }

//	@DataProvider(name="isManageableByUserProvider")
//	public Object[][] isManageableByUserProvider() throws Exception
//	{manager.
//		return new Object[][] {
//			{ internalUser, privateSystem.getOwner(), "Owner can manage their system", true },
//			{ privateSystem, SYSTEM_PUBLIC_USER, "Guest cannot manage a private system", false },
//			{ privateSystem, SYSTEM_SHARE_USER, "User cannot manage a private system", false },
//			
//			{ publicSystem, publicSystem.getOwner(), "No one can manage a public system", false },
//			{ publicSystem, SYSTEM_PUBLIC_USER, "Guest cannot manage a private system", false },
//			{ publicSystem, SYSTEM_SHARE_USER, "User cannot manage a private system", false },
//			
//			{ sharedSystem, SYSTEM_UNSHARED_USER, "Unshared user cannot manage a shared system", false },
//		};
//	}
//	
//	@Test(dataProvider = "isManageableByUserProvider")
//	public void isManageableByUser(RemoteSystem system, String username, String message, boolean userCanManager)
//	{
//		Assert.assertEquals(manager.isManageableByUser(system, username), userCanManager, message);
//	}
//	
//	@DataProvider(name="isManageableBySharedUserProvider")
//	public Object[][] isManageableBySharedUserProvider() throws Exception
//	{
//		return new Object[][] {
//				{ sharedSystem, RoleType.NONE, "User cannot manage a system they have no role on", false },
//				{ sharedSystem, RoleType.USER, "User cannot manage a system in which they are only a user", false },
//				{ sharedSystem, RoleType.PUBLISHER, "User cannot manage a system they have publisher role on", false },
//				{ sharedSystem, RoleType.OWNER, "User cannot manage a system they have owner role on", true },
//				{ sharedSystem, RoleType.ADMIN, "User can manage a system they have admin role on", true },
//		};
//	}
//	
//	@Test(dataProvider = "isManageableBySharedUserProvider")
//	public void isManageableBySharedUserTest(RemoteSystem system, RoleType testType, String message, boolean userCanManager)
//	{
//		RoleType previousType = system.getRoles().get(0).getRole();
//		system.getRoles().get(0).setRole(testType);
//		
//		Assert.assertEquals(manager.isManageableByUser(system, SYSTEM_SHARE_USER), userCanManager, message);
//
//		// restore type
//		system.getRoles().get(0).setRole(previousType);
//	}
//	
//	@DataProvider(name="isVisibleByUserProvider")
//	public Object[][] isVisibleByUserProvider() throws Exception
//	{
//		return new Object[][] {
//				{ privateSystem, privateSystem.getOwner(), "Owner can see their own system", true },
//				{ privateSystem, SYSTEM_PUBLIC_USER, "Guest cannot see private system", false },
//				{ privateSystem, SYSTEM_SHARE_USER, "non-owner cannot see private system", false },
//				
//				{ publicSystem, publicSystem.getOwner(), "Original owner can see public system", true },
//				{ publicSystem, SYSTEM_PUBLIC_USER, "Guest can see public system", true },
//				{ publicSystem, SYSTEM_SHARE_USER, "Users can see public system", true },
//				
//				{ sharedSystem, sharedSystem.getOwner(), "Owner can see their shared system", true },
//				{ sharedSystem, SYSTEM_PUBLIC_USER, "Guest cannot see shared system", false },
//				{ sharedSystem, SYSTEM_UNSHARED_USER, "non-shared user cannot see shared system", false },
//		};
//	}
//	
//	@Test(dataProvider = "isVisibleByUserProvider")
//	public void isVisibleByUser(RemoteSystem system, String username, String message, boolean userCanManager)
//	{
//		Assert.assertEquals(manager.isVisibleByUser(system, username), userCanManager, message);
//	}
//
//	//@Test(dataProvider = "cloneSystemProvider")
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
//			publicSystem = manager.makePublic(system);
//			
//			Assert.assertTrue(publicSystem.isPubliclyAvailable(), "System is public");
//			Assert.assertTrue(manager.isVisibleByUser(publicSystem, SYSTEM_PUBLIC_USER), "System is public");
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
//		RemoteSystem system = manager.parseSystem(json, SYSTEM_OWNER, null);
//		
//		Assert.assertEquals(system.getOwner(), SYSTEM_OWNER, message);
//		
//		Assert.assertFalse(system.isPubliclyAvailable(), "System was published as private");
//		
//		Assert.assertEquals(system.getClass(), expectedClass, message);
//	}
}
