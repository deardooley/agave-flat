package org.iplantc.service.io.permissions;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.lang.StringUtils;
import org.iplantc.service.common.uuid.UUIDType;
import org.iplantc.service.io.Settings;
import org.iplantc.service.io.dao.LogicalFileDao;
import org.iplantc.service.io.model.JSONTestDataUtil;
import org.iplantc.service.io.model.LogicalFile;
import org.iplantc.service.metadata.model.enumerations.PermissionType;
import org.iplantc.service.systems.dao.SystemDao;
import org.iplantc.service.systems.exceptions.SystemException;
import org.iplantc.service.systems.model.ExecutionSystem;
import org.iplantc.service.systems.model.RemoteSystem;
import org.iplantc.service.systems.model.StorageSystem;
import org.iplantc.service.systems.model.enumerations.RemoteSystemType;
import org.iplantc.service.transfer.RemoteDataClient;
import org.iplantc.service.transfer.model.RemoteFilePermission;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class ImplicitFilePermissionsFromSystemRolesTest extends AbstractPermissionManagerTest 
{
	class PermissionPredicate implements Predicate {
		private RemoteSystem system;
		
		public PermissionPredicate(RemoteSystem system) {
			this.system = system;
		}
		
		@Override
		public boolean evaluate(Object remoteFilePermission) {
			return ((RemoteFilePermission)remoteFilePermission).getUsername().equals(system.getOwner());
		}
	}
	
	@BeforeClass
	protected void beforeClass() throws Exception 
	{
		super.beforeClass();
	}
	
	@Override
	protected RemoteSystem getTestSystemDescription(RemoteSystemType type) throws Exception 
	{
		if (type.equals(RemoteSystemType.EXECUTION)) {
			return ExecutionSystem.fromJSON(jtd.getTestDataObject(JSONTestDataUtil.TEST_EXECUTION_SYSTEM_FILE));
		} else if (type.equals(RemoteSystemType.STORAGE)) {
			return StorageSystem.fromJSON(jtd.getTestDataObject(JSONTestDataUtil.TEST_STORAGE_SYSTEM_FILE));
		} else {
			throw new SystemException("RemoteSystem type " + type + " is not supported.");
		}
	}
	
	
	
	@DataProvider
	public Object[][] getAllPermissionsReturnsSystemOwnerProvider() throws Exception
	{
		List<Object[]> testList = new ArrayList<Object[]>();
		
		RemoteSystem publicStorageSystem = getPublicSystem(RemoteSystemType.STORAGE);
		RemoteSystem publicMirroredStorageSystem = getPublicMirroredSystem(RemoteSystemType.STORAGE);
		RemoteSystem publicGuestStorageSystem = getPublicGuestSystem(RemoteSystemType.STORAGE);
		RemoteSystem privateStorageSystem = getPrivateSystem(RemoteSystemType.STORAGE);
		RemoteSystem privateSharedGuestStorageSystem = getPrivateSharedGuestSystem(RemoteSystemType.STORAGE);
		RemoteSystem privateSharedUserStorageSystem = getPrivateSharedUserSystem(RemoteSystemType.STORAGE);
		RemoteSystem privateSharedPublisherStorageSystem = getPrivateSharedPublisherSystem(RemoteSystemType.STORAGE);
		RemoteSystem privateSharedAdminStorageSystem = getPrivateSharedAdminSystem(RemoteSystemType.STORAGE);
		
		RemoteSystem[] systems = {
				publicStorageSystem, 
				publicMirroredStorageSystem,
				publicGuestStorageSystem,
				privateStorageSystem,
				privateSharedGuestStorageSystem,
				privateSharedUserStorageSystem,
				privateSharedPublisherStorageSystem,
				privateSharedAdminStorageSystem
		};
		
		for (RemoteSystem system: systems) 
		{
			if (system.isPubliclyAvailable()) {
				testList.add(new Object[]{ system, SYSTEM_UNSHARED_USER, SYSTEM_UNSHARED_USER, 			null, true, 	false });
				testList.add(new Object[]{ system, "/", 				 SYSTEM_UNSHARED_USER, 			null, true, 	false });
				testList.add(new Object[]{ system, SYSTEM_SHARE_USER, 	 SYSTEM_SHARE_USER, 			null, true, 	false });
				testList.add(new Object[]{ system, "/", 				 SYSTEM_SHARE_USER, 			null, true, 	false });
				testList.add(new Object[]{ system, "/", 				 SYSTEM_OWNER, 					null, true, 	false });
				testList.add(new Object[]{ system, SYSTEM_OWNER, 		 SYSTEM_OWNER, 					null, true, 	false });
				testList.add(new Object[]{ system, "/", 				 Settings.PUBLIC_USER_USERNAME, null, true, 	false });
				testList.add(new Object[]{ system, Settings.PUBLIC_USER_USERNAME, Settings.PUBLIC_USER_USERNAME, null, true, 	false });
				testList.add(new Object[]{ system, Settings.PUBLIC_USER_USERNAME, SYSTEM_UNSHARED_USER, null, true, 	false });
				testList.add(new Object[]{ system, Settings.PUBLIC_USER_USERNAME, SYSTEM_SHARE_USER, 	null, true, 	false });
				testList.add(new Object[]{ system, Settings.PUBLIC_USER_USERNAME, SYSTEM_OWNER, 		null, true, 	false });
				testList.add(new Object[]{ system, "/", 				 SYSTEM_OWNER, 					null, true, 	false });
			} else {
				testList.add(new Object[]{ system, "/", 				 SYSTEM_UNSHARED_USER, 			null, true, 	false });
				testList.add(new Object[]{ system, "/", 				 SYSTEM_OWNER, 					null, true, 	false });
				testList.add(new Object[]{ system, "/", 				 SYSTEM_OWNER, 					null, true, 	false });
				testList.add(new Object[]{ system, "", 				 	 SYSTEM_OWNER, 					null, true, 	false });
				testList.add(new Object[]{ system, "", 				 	 SYSTEM_OWNER, 					null, true, 	false });
			}
		}
		
		return testList.toArray(new Object[][]{});
	}
	
	/**
	 * Test that private systems return system owner in file permission lists
	 * 
	 * @param system
	 * @param path
	 * @param owner
	 * @param internalUsername
	 * @param expectedResult
	 * @param shouldThrowException
	 */
	@Test(dataProvider="getAllPermissionsReturnsSystemOwnerProvider")
	public void getAllPermissionsReturnsSystemOwner(RemoteSystem system, String path, String userForWhomToCheckPermissions, String internalUsername, boolean systemOwnerPermissionShouldBePresent, boolean shouldThrowException)
	{
		try 
		{
			// resolve the relative path against the actual system path
			RemoteDataClient remoteDataClient = system.getRemoteDataClient(internalUsername);
			String absolutePath = remoteDataClient.resolvePath(path);
			
//			LogicalFile logicalFile = LogicalFileDao.findBySystemAndPath(system, absolutePath);

			PermissionManager pm = new PermissionManager(system, remoteDataClient, null, userForWhomToCheckPermissions);

			Collection<RemoteFilePermission> allPermissions = pm.getAllPermissions(absolutePath);
			
			CollectionUtils.filter(allPermissions, new PermissionPredicate(system));
			
			if (systemOwnerPermissionShouldBePresent) {
				Assert.assertFalse(allPermissions.isEmpty(), "Owner permission should always be present, but were missing.");
			} else {
				Assert.assertTrue(allPermissions.isEmpty(), "Owner permission should not present, but were found.");
			}
			
			if (systemOwnerPermissionShouldBePresent) {
				Assert.assertEquals(allPermissions.size(), 1, "There should be exactly one owner permission returned from private systems.");
			}
		}
		catch (Exception e)
		{
			if (!shouldThrowException) Assert.fail(e.getMessage(), e);
		}
	}
	
//	/**
//	 * Test that private systems return system owner in file permission lists
//	 * 
//	 * @param system
//	 * @param path
//	 * @param owner
//	 * @param internalUsername
//	 * @param expectedResult
//	 * @param shouldThrowException
//	 */
//	@Test(dataProvider="getAllPermissionsReturnsSystemOwnerProvider")
//	public void getAllPermissionsOnPublicSystemsOnlyReturnsSystemOwner(RemoteSystem system, String path, String owner, String internalUsername, boolean expectedResult, boolean shouldThrowException)
//	{
//		try 
//		{
//			// resolve the relative path against the actual system path
//			RemoteDataClient remoteDataClient = system.getRemoteDataClient(internalUsername);
//			String absolutePath = remoteDataClient.resolvePath(path);
//			
//			LogicalFile logicalFile = LogicalFileDao.findBySystemAndPath(system, absolutePath);
//
//			PermissionManager pm = new PermissionManager(system, remoteDataClient, null, owner);
//
//			Collection<RemoteFilePermission> allPermissions = pm.getAllPermissions(absolutePath);
//			
//			CollectionUtils.filter(allPermissions, new PermissionPredicate(system));
//			
//			Assert.assertEquals(allPermissions.isEmpty(), "Owner permission should always be present in a private system.");
//			Assert.assertEquals(allPermissions.size(), 1, "There should be exactly one owner permission returned from private systems.");
//		}
//		catch (Exception e)
//		{
//			if (!shouldThrowException) Assert.fail(e.getMessage(), e);
//		}
//	}
//	
//	public void abstractTestCanReadUri(String uri, String owner, String internalUsername, boolean expectedResult, boolean shouldThrowException)
//	{
//		try 
//		{
//			// create a semantically correct uri to test from the system and path
//			URI testUri = new URI(uri);
//			
//			boolean actualResult = PermissionManager.canUserReadUri(owner, internalUsername, testUri);
//			
//			String errorMessage = String.format("User %s %s have permission to read %s", 
//					owner,
//					expectedResult ? "should have" : "should not have",
//					uri);
//
//			Assert.assertEquals( actualResult, expectedResult, errorMessage );
//		
//		}
//		catch (Exception e)
//		{
//			if (!shouldThrowException) Assert.fail(e.getMessage(), e);
//		}
//	}
//	
//	/************************************************************************
//	/*							 SCHEMA TESTS      							*
//	/************************************************************************/
//	
//	@DataProvider
//	protected Object[][] testCanUserReadUriSchemaProvider() throws Exception
//	{
//		beforeTestData();
//		RemoteSystem system = getPrivateSystem(RemoteSystemType.STORAGE);
//		
//		return new Object[][] {
//				{ "agave://" + system.getSystemId() + "//", SYSTEM_OWNER, null, true, false },
//				{ "http://storage.example.com//", SYSTEM_OWNER, null, true, false },
//				{ "https://storage.example.com//", SYSTEM_OWNER, null, true, false },
//				{ Settings.IPLANT_IO_SERVICE, SYSTEM_OWNER, null, true, false },
//				{ Settings.IPLANT_IO_SERVICE + "/listings", SYSTEM_OWNER, null, true, false },
//				{ Settings.IPLANT_IO_SERVICE + "listings", SYSTEM_OWNER, null, true, false },
//				{ Settings.IPLANT_IO_SERVICE + "listings/", SYSTEM_OWNER, null, true, false },
//				{ Settings.IPLANT_IO_SERVICE + "/media", SYSTEM_OWNER, null, true, false },
//				{ Settings.IPLANT_IO_SERVICE + "media", SYSTEM_OWNER, null, true, false },
//				{ Settings.IPLANT_IO_SERVICE + "media/", SYSTEM_OWNER, null, true, false },
//				{ Settings.IPLANT_IO_SERVICE + "media//", SYSTEM_OWNER, null, true, false },
//				{ Settings.IPLANT_IO_SERVICE + "media/system", SYSTEM_OWNER, null, false, true },
//				{ Settings.IPLANT_IO_SERVICE + "media/system/", SYSTEM_OWNER, null, false, true },
//				{ Settings.IPLANT_IO_SERVICE + "media/system//", SYSTEM_OWNER, null, false, true },
//				{ "ftp://storage.example.com//", SYSTEM_OWNER, null, false, false },
//				{ "gsissh://storage.example.com//", SYSTEM_OWNER, null, false, false },
//				{ "s3://storage.example.com//", SYSTEM_OWNER, null, false, false },
//				{ "irods://storage.example.com//", SYSTEM_OWNER, null, false, false },
//				{ "sftp://storage.example.com//", SYSTEM_OWNER, null, false, false },
//				{ "azure://storage.example.com//", SYSTEM_OWNER, null, false, false },
//				{ "gsiftp://storage.example.com//", SYSTEM_OWNER, null, false, false },
//				{ "booya://storage.example.com//", SYSTEM_OWNER, null, false, false },
//				{ "http://storage.example.com", SYSTEM_OWNER, null, true, false },
//				{ "://storage.example.com//", SYSTEM_OWNER, null, false, true },
//				{ "//storage.example.com//", SYSTEM_OWNER, null, false, true },
//				{ "C:\\storage.example.com\\", SYSTEM_OWNER, null, false, true },
//				{ "file://storage.example.com//", SYSTEM_OWNER, null, false, false },
//				{ "/", SYSTEM_OWNER, null, true, true },
//				{ "", SYSTEM_OWNER, null, false, true },
//				{ null, SYSTEM_OWNER, null, false, true },
//		};
//	}
//	
//	@Test(dataProvider="testCanUserReadUriSchemaProvider")
//	public void testCanUserReadUriSchema(String uri, String owner, String internalUsername, boolean expectedResult, boolean shouldThrowException)
//	{
//		abstractTestCanReadUri(uri, owner, internalUsername, expectedResult, shouldThrowException);
//	}
//	
//	@DataProvider
//	protected Object[][] testCanUserReadUriPathProvider() throws Exception
//	{
//		beforeTestData();
//		RemoteSystem system = getPrivateSystem(RemoteSystemType.STORAGE);
//		
//		return new Object[][] {
//				{ "agave://" + system.getSystemId() + "/", SYSTEM_OWNER, null, true, false },
//				{ "agave://" + system.getSystemId() + "/" + SYSTEM_OWNER, SYSTEM_OWNER, null, true, false },
//				{ "agave://" + system.getSystemId() + "//", SYSTEM_OWNER, null, true, false },
//		};
//	}
//	
//	@Test(dataProvider="testCanUserReadUriPathProvider")
//	public void testCanUserReadUriPath(String uri, String owner, String internalUsername, boolean expectedResult, boolean shouldThrowException)
//	{
//		abstractTestCanReadUri(uri, owner, internalUsername, expectedResult, shouldThrowException);
//	}
//	
//	@DataProvider
//	protected Object[][] testCanUserReadUriHostProvider() throws Exception
//	{
//		beforeTestData();
//		RemoteSystem system = getPrivateSystem(RemoteSystemType.STORAGE);
//		
//		return new Object[][] {
//				{ "agave://" + system.getSystemId() + "//", SYSTEM_OWNER, null, true, false },
//				{ "agave:////", SYSTEM_OWNER, null, false, true }, // should find default storage system
//				{ "agave://asmldkjfapsodufapojk//", SYSTEM_OWNER, null, false, true },
//				{ "agave://a..b//", SYSTEM_OWNER, null, false, true },
//				{ "agave://g^739//", SYSTEM_OWNER, null, false, true },
//				{ "agave://127.0.0.1//", SYSTEM_OWNER, null, false, true },
//				{ "agave://some-new-system" + System.currentTimeMillis() + "//", SYSTEM_OWNER, null, false, true },
//		};
//	}
//	
//	@Test(dataProvider="testCanUserReadUriHostProvider")
//	public void testCanUserReadUriHost(String uri, String owner, String internalUsername, boolean expectedResult, boolean shouldThrowException)
//	{
//		abstractTestCanReadUri(uri, owner, internalUsername, expectedResult, shouldThrowException);
//	}
//	
//	public void testCanUserReadUriNullHostNoDefaultFails(String uri, String owner, String internalUsername, boolean expectedResult, boolean shouldThrowException)
//	throws Exception
//	{
//		beforeTestData();
//		RemoteSystem privateSystem = getPrivateSystem(RemoteSystemType.STORAGE);
//		
//		// no hostname in an internal url and no default system should throw exception
//		abstractTestCanReadUri("agave:////", SYSTEM_OWNER, null, false, true);
//		abstractTestCanReadUri("agave:////", SYSTEM_SHARE_USER, null, false, true);
//		abstractTestCanReadUri("agave:////", SYSTEM_UNSHARED_USER, null, false, true);
//		
//	}
//	
//	public void testCanUserReadUriNullHostPrivateDefault(String uri, String owner, String internalUsername, boolean expectedResult, boolean shouldThrowException)
//	throws Exception
//	{
//		beforeTestData();
//		RemoteSystem privateSystem = getPrivateSystem(RemoteSystemType.STORAGE);
//		privateSystem.addUserUsingAsDefault(SYSTEM_OWNER);
//		SystemDao dao = new SystemDao();
//		dao.persist(privateSystem);
//		
//		// no hostname in an internal url and no default system should throw exception
//		abstractTestCanReadUri("agave:////", SYSTEM_OWNER, null, true, false);
//		abstractTestCanReadUri("agave:////", SYSTEM_SHARE_USER, null, false, true);
//		abstractTestCanReadUri("agave:////", SYSTEM_UNSHARED_USER, null, false, true);
//	}
//	
//	public void testCanUserReadUriNullHostPrivateSharedDefault(String uri, String owner, String internalUsername, boolean expectedResult, boolean shouldThrowException)
//	throws Exception
//	{
//		beforeTestData();
//		RemoteSystem privateSystem = getPrivateSharedUserSystem(RemoteSystemType.STORAGE);
//		privateSystem.addUserUsingAsDefault(SYSTEM_OWNER);
//		privateSystem.addUserUsingAsDefault(SYSTEM_SHARE_USER);
//		SystemDao dao = new SystemDao();
//		dao.persist(privateSystem);
//		
//		// no hostname in an internal url and no default system should throw exception
//		abstractTestCanReadUri("agave:////", SYSTEM_OWNER, null, true, false);
//		abstractTestCanReadUri("agave:////", SYSTEM_SHARE_USER, null, true, false);
//		abstractTestCanReadUri("agave:////", SYSTEM_UNSHARED_USER, null, false, true);
//	}
//	
//	public void testCanUserReadUriNullHostPublicNonDefault(String uri, String owner, String internalUsername, boolean expectedResult, boolean shouldThrowException)
//	throws Exception
//	{
//		beforeTestData();
//		RemoteSystem system = getPublicSystem(RemoteSystemType.STORAGE);
//		system.setGlobalDefault(false);
//		system.getUsersUsingAsDefault().clear();
//		SystemDao dao = new SystemDao();
//		dao.persist(system);
//		
//		// no hostname in an internal url and no default system should throw exception
//		abstractTestCanReadUri("agave:////", SYSTEM_OWNER, null, false, true);
//		abstractTestCanReadUri("agave:////", SYSTEM_SHARE_USER, null, false, true);
//		abstractTestCanReadUri("agave:////", SYSTEM_UNSHARED_USER, null, false, true);
//	}
//	
//	public void testCanUserReadUriNullHostPublicDefault(String uri, String owner, String internalUsername, boolean expectedResult, boolean shouldThrowException)
//	throws Exception
//	{
//		beforeTestData();
//		RemoteSystem system = getPublicSystem(RemoteSystemType.STORAGE);
//		system.addUserUsingAsDefault(SYSTEM_OWNER);
//		system.addUserUsingAsDefault(SYSTEM_SHARE_USER);
//		SystemDao dao = new SystemDao();
//		dao.persist(system);
//		
//		// no hostname in an internal url and no default system should throw exception
//		abstractTestCanReadUri("agave:////", SYSTEM_OWNER, null, true, false);
//		abstractTestCanReadUri("agave:///" + SYSTEM_SHARE_USER, SYSTEM_SHARE_USER, null, true, false);
//		abstractTestCanReadUri("agave:////", SYSTEM_UNSHARED_USER, null, false, true);
//	}
//	
//	public void testCanUserReadUriNullHostPublicGlobalDefault(String uri, String owner, String internalUsername, boolean expectedResult, boolean shouldThrowException)
//	throws Exception
//	{
//		beforeTestData();
//		RemoteSystem system = getPublicSystem(RemoteSystemType.STORAGE);
//		system.setGlobalDefault(true);
//		SystemDao dao = new SystemDao();
//		dao.persist(system);
//		
//		// no hostname in an internal url and no default system should throw exception
//		abstractTestCanReadUri("agave:////", SYSTEM_OWNER, null, true, false);
//		abstractTestCanReadUri("agave:///" + SYSTEM_SHARE_USER, SYSTEM_SHARE_USER, null, true, false);
//		abstractTestCanReadUri("agave:///" + SYSTEM_UNSHARED_USER, SYSTEM_UNSHARED_USER, null, true, false);
//	}
//	
//	
//	/************************************************************************
//	/*							 READ TESTS      							*
//	/************************************************************************/
//	
//	@DataProvider
//	protected Object[][] testCanReadRootProvider() throws Exception
//	{
//		beforeTestData();
//		RemoteSystem publicStorageSystem = getPublicSystem(RemoteSystemType.STORAGE);
//		RemoteSystem publicMirroredStorageSystem = getPublicMirroredSystem(RemoteSystemType.STORAGE);
//		RemoteSystem publicGuestStorageSystem = getPublicGuestSystem(RemoteSystemType.STORAGE);
//		RemoteSystem privateStorageSystem = getPrivateSystem(RemoteSystemType.STORAGE);
//		RemoteSystem privateSharedGuestStorageSystem = getPrivateSharedGuestSystem(RemoteSystemType.STORAGE);
//		RemoteSystem privateSharedUserStorageSystem = getPrivateSharedUserSystem(RemoteSystemType.STORAGE);
//		RemoteSystem privateSharedPublisherStorageSystem = getPrivateSharedPublisherSystem(RemoteSystemType.STORAGE);
//		RemoteSystem privateSharedAdminStorageSystem = getPrivateSharedAdminSystem(RemoteSystemType.STORAGE);
//		
//		return new Object[][] {
//				{ publicStorageSystem, "/", SYSTEM_OWNER, null, true, false },
//				{ publicMirroredStorageSystem, "/", SYSTEM_OWNER, null, true, false },
//				{ publicGuestStorageSystem, "/", SYSTEM_OWNER, null, true, false },
//				{ privateStorageSystem, "/", SYSTEM_OWNER, null, true, false },
//				{ privateSharedGuestStorageSystem, "/", SYSTEM_OWNER, null, true, false },
//				{ privateSharedUserStorageSystem, "/", SYSTEM_OWNER, null, true, false },
//				{ privateSharedPublisherStorageSystem, "/", SYSTEM_OWNER, null, true, false },
//				{ privateSharedAdminStorageSystem, "/", SYSTEM_OWNER, null, true, false },
//				
//				{ publicStorageSystem, "/", SYSTEM_SHARE_USER, null, false, false },
//				{ publicMirroredStorageSystem, "/", SYSTEM_SHARE_USER, null, false, false },
//				{ publicGuestStorageSystem, "/", SYSTEM_SHARE_USER, null, true, false },
//				{ privateStorageSystem, "/", SYSTEM_SHARE_USER, null, false, false },
//				{ privateSharedGuestStorageSystem, "/", SYSTEM_SHARE_USER, null, true, false },
//				{ privateSharedUserStorageSystem, "/", SYSTEM_SHARE_USER, null, true, false },
//				{ privateSharedPublisherStorageSystem, "/", SYSTEM_SHARE_USER, null, true, false },
//				{ privateSharedAdminStorageSystem, "/", SYSTEM_SHARE_USER, null, true, false },
//				
//				{ publicStorageSystem, "/", SYSTEM_UNSHARED_USER, null, false, false },
//				{ publicMirroredStorageSystem, "/", SYSTEM_UNSHARED_USER, null, false, false },
//				{ publicGuestStorageSystem, "/", SYSTEM_UNSHARED_USER, null, true, false },
//				{ privateStorageSystem, "/", SYSTEM_UNSHARED_USER, null, false, false },
//				{ privateSharedGuestStorageSystem, "/", SYSTEM_UNSHARED_USER, null, false, false },
//				{ privateSharedUserStorageSystem, "/", SYSTEM_UNSHARED_USER, null, false, false },
//				{ privateSharedPublisherStorageSystem, "/", SYSTEM_UNSHARED_USER, null, false, false },
//				{ privateSharedAdminStorageSystem, "/", SYSTEM_UNSHARED_USER, null, false, false },
//		};
//	}
//	
//	@Test(dataProvider="testCanReadRootProvider")
//	public void testCanReadRoot(RemoteSystem system, String path, String owner, String internalUsername, boolean expectedResult, boolean shouldThrowException)
//	{
//		abstractTestCanRead(system, path, owner, internalUsername, expectedResult, shouldThrowException);
//	}
//	
//	@DataProvider
//	protected Object[][] testCanReadSystemHomeProvider() throws Exception
//	{
//		beforeTestData();
//		RemoteSystem publicStorageSystem = getPublicSystem(RemoteSystemType.STORAGE);
//		RemoteSystem publicMirroredStorageSystem = getPublicMirroredSystem(RemoteSystemType.STORAGE);
//		RemoteSystem publicGuestStorageSystem = getPublicGuestSystem(RemoteSystemType.STORAGE);
//		RemoteSystem privateStorageSystem = getPrivateSystem(RemoteSystemType.STORAGE);
//		RemoteSystem privateSharedGuestStorageSystem = getPrivateSharedGuestSystem(RemoteSystemType.STORAGE);
//		RemoteSystem privateSharedUserStorageSystem = getPrivateSharedUserSystem(RemoteSystemType.STORAGE);
//		RemoteSystem privateSharedPublisherStorageSystem = getPrivateSharedPublisherSystem(RemoteSystemType.STORAGE);
//		RemoteSystem privateSharedAdminStorageSystem = getPrivateSharedAdminSystem(RemoteSystemType.STORAGE);
//		
//		return new Object[][] {
//				{ publicStorageSystem, "", SYSTEM_OWNER, null, true, false },
//				{ publicMirroredStorageSystem, "", SYSTEM_OWNER, null, true, false },
//				{ publicGuestStorageSystem, "", SYSTEM_OWNER, null, true, false },
//				{ privateStorageSystem, "", SYSTEM_OWNER, null, true, false },
//				{ privateSharedGuestStorageSystem, "", SYSTEM_OWNER, null, true, false },
//				{ privateSharedUserStorageSystem, "", SYSTEM_OWNER, null, true, false },
//				{ privateSharedPublisherStorageSystem, "", SYSTEM_OWNER, null, true, false },
//				{ privateSharedAdminStorageSystem, "", SYSTEM_OWNER, null, true, false },
//				
//				{ publicStorageSystem, "", SYSTEM_SHARE_USER, null, false, false },
//				{ publicMirroredStorageSystem, "", SYSTEM_SHARE_USER, null, false, false },
//				{ publicGuestStorageSystem, "", SYSTEM_SHARE_USER, null, true, false },
//				{ privateStorageSystem, "", SYSTEM_SHARE_USER, null, false, false },
//				{ privateSharedGuestStorageSystem, "", SYSTEM_SHARE_USER, null, true, false },
//				{ privateSharedUserStorageSystem, "", SYSTEM_SHARE_USER, null, true, false },
//				{ privateSharedPublisherStorageSystem, "", SYSTEM_SHARE_USER, null, true, false },
//				{ privateSharedAdminStorageSystem, "", SYSTEM_SHARE_USER, null, true, false },
//				
//				{ publicStorageSystem, "", SYSTEM_UNSHARED_USER, null, false, false },
//				{ publicMirroredStorageSystem, "", SYSTEM_UNSHARED_USER, null, false, false },
//				{ publicGuestStorageSystem, "", SYSTEM_UNSHARED_USER, null, true, false },
//				{ privateStorageSystem, "", SYSTEM_UNSHARED_USER, null, false, false },
//				{ privateSharedGuestStorageSystem, "", SYSTEM_UNSHARED_USER, null, false, false },
//				{ privateSharedUserStorageSystem, "", SYSTEM_UNSHARED_USER, null, false, false },
//				{ privateSharedPublisherStorageSystem, "", SYSTEM_UNSHARED_USER, null, false, false },
//				{ privateSharedAdminStorageSystem, "", SYSTEM_UNSHARED_USER, null, false, false },
//		};
//	}
//	
//	@Test(dataProvider="testCanReadSystemHomeProvider")
//	public void testCanReadImplicitSystemHome(RemoteSystem system, String path, String owner, String internalUsername, boolean expectedResult, boolean shouldThrowException)
//	{
//		abstractTestCanRead(system, path, owner, internalUsername, expectedResult, shouldThrowException);
//	}
//	
//	@Test(dataProvider="testCanReadSystemHomeProvider")
//	public void testCanReadExplicitSystemHome(RemoteSystem system, String path, String owner, String internalUsername, boolean expectedResult, boolean shouldThrowException)
//	{
//		abstractTestCanRead(system, system.getStorageConfig().getHomeDir(), owner, internalUsername, expectedResult, shouldThrowException);
//	}
//	
//	@DataProvider
//	protected Object[][] testCanReadUserHomeProvider() throws Exception
//	{
//		beforeTestData();
//		RemoteSystem publicStorageSystem = getPublicSystem(RemoteSystemType.STORAGE);
//		RemoteSystem publicMirroredStorageSystem = getPublicMirroredSystem(RemoteSystemType.STORAGE);
//		RemoteSystem publicGuestStorageSystem = getPublicGuestSystem(RemoteSystemType.STORAGE);
//		RemoteSystem privateStorageSystem = getPrivateSystem(RemoteSystemType.STORAGE);
//		RemoteSystem privateSharedGuestStorageSystem = getPrivateSharedGuestSystem(RemoteSystemType.STORAGE);
//		RemoteSystem privateSharedUserStorageSystem = getPrivateSharedUserSystem(RemoteSystemType.STORAGE);
//		RemoteSystem privateSharedPublisherStorageSystem = getPrivateSharedPublisherSystem(RemoteSystemType.STORAGE);
//		RemoteSystem privateSharedAdminStorageSystem = getPrivateSharedAdminSystem(RemoteSystemType.STORAGE);
//		
//		return new Object[][] {
//				{ publicStorageSystem, SYSTEM_OWNER, SYSTEM_OWNER, null, true, false },
//				{ publicMirroredStorageSystem, SYSTEM_OWNER, SYSTEM_OWNER, null, true, false },
//				{ publicGuestStorageSystem, SYSTEM_OWNER, SYSTEM_OWNER, null, true, false },
//				{ privateStorageSystem, SYSTEM_OWNER, SYSTEM_OWNER, null, true, false },
//				{ privateSharedGuestStorageSystem, SYSTEM_OWNER, SYSTEM_OWNER, null, true, false },
//				{ privateSharedUserStorageSystem, SYSTEM_OWNER, SYSTEM_OWNER, null, true, false },
//				{ privateSharedPublisherStorageSystem, SYSTEM_OWNER, SYSTEM_OWNER, null, true, false },
//				{ privateSharedAdminStorageSystem, SYSTEM_OWNER, SYSTEM_OWNER, null, true, false },
//				
//				{ publicStorageSystem, SYSTEM_OWNER, SYSTEM_SHARE_USER, null, false, false },
//				{ publicMirroredStorageSystem, SYSTEM_OWNER, SYSTEM_SHARE_USER, null, false, false },
//				{ publicGuestStorageSystem, SYSTEM_OWNER, SYSTEM_SHARE_USER, null, true, false },
//				{ privateStorageSystem, SYSTEM_OWNER, SYSTEM_SHARE_USER, null, false, false },
//				{ privateSharedGuestStorageSystem, SYSTEM_OWNER, SYSTEM_SHARE_USER, null, true, false },
//				{ privateSharedUserStorageSystem, SYSTEM_OWNER, SYSTEM_SHARE_USER, null, true, false },
//				{ privateSharedPublisherStorageSystem, SYSTEM_OWNER, SYSTEM_SHARE_USER, null, true, false },
//				{ privateSharedAdminStorageSystem, SYSTEM_OWNER, SYSTEM_SHARE_USER, null, true, false },
//				
//				{ publicStorageSystem, SYSTEM_SHARE_USER, SYSTEM_SHARE_USER, null, true, false },
//				{ publicMirroredStorageSystem, SYSTEM_SHARE_USER, SYSTEM_SHARE_USER, null, true, false },
//				{ publicGuestStorageSystem, SYSTEM_SHARE_USER, SYSTEM_SHARE_USER, null, true, false },
//				{ privateStorageSystem, SYSTEM_SHARE_USER, SYSTEM_SHARE_USER, null, false, false },
//				{ privateSharedGuestStorageSystem, SYSTEM_SHARE_USER, SYSTEM_SHARE_USER, null, true, false },
//				{ privateSharedUserStorageSystem, SYSTEM_SHARE_USER, SYSTEM_SHARE_USER, null, true, false },
//				{ privateSharedPublisherStorageSystem, SYSTEM_SHARE_USER, SYSTEM_SHARE_USER, null, true, false },
//				{ privateSharedAdminStorageSystem, SYSTEM_SHARE_USER, SYSTEM_SHARE_USER, null, true, false },
//				
//				{ publicStorageSystem, SYSTEM_UNSHARED_USER, SYSTEM_SHARE_USER, null, false, false },
//				{ publicMirroredStorageSystem, SYSTEM_UNSHARED_USER, SYSTEM_SHARE_USER, null, false, false },
//				{ publicGuestStorageSystem, SYSTEM_UNSHARED_USER, SYSTEM_SHARE_USER, null, true, false },
//				{ privateStorageSystem, SYSTEM_UNSHARED_USER, SYSTEM_SHARE_USER, null, false, false },
//				{ privateSharedGuestStorageSystem, SYSTEM_UNSHARED_USER, SYSTEM_SHARE_USER, null, true, false },
//				{ privateSharedUserStorageSystem, SYSTEM_UNSHARED_USER, SYSTEM_SHARE_USER, null, true, false },
//				{ privateSharedPublisherStorageSystem, SYSTEM_UNSHARED_USER, SYSTEM_SHARE_USER, null, true, false },
//				{ privateSharedAdminStorageSystem, SYSTEM_UNSHARED_USER, SYSTEM_SHARE_USER, null, true, false },
//				
//				{ publicStorageSystem, SYSTEM_OWNER, SYSTEM_UNSHARED_USER, null, false, false },
//				{ publicMirroredStorageSystem, SYSTEM_OWNER, SYSTEM_UNSHARED_USER, null, false, false },
//				{ publicGuestStorageSystem, SYSTEM_OWNER, SYSTEM_UNSHARED_USER, null, true, false },
//				{ privateStorageSystem, SYSTEM_OWNER, SYSTEM_UNSHARED_USER, null, false, false },
//				{ privateSharedGuestStorageSystem, SYSTEM_OWNER, SYSTEM_UNSHARED_USER, null, false, false },
//				{ privateSharedUserStorageSystem, SYSTEM_OWNER, SYSTEM_UNSHARED_USER, null, false, false },
//				{ privateSharedPublisherStorageSystem, SYSTEM_OWNER, SYSTEM_UNSHARED_USER, null, false, false },
//				{ privateSharedAdminStorageSystem, SYSTEM_OWNER, SYSTEM_UNSHARED_USER, null, false, false },
//				
//				{ publicStorageSystem, SYSTEM_SHARE_USER, SYSTEM_UNSHARED_USER, null, false, false },
//				{ publicMirroredStorageSystem, SYSTEM_SHARE_USER, SYSTEM_UNSHARED_USER, null, false, false },
//				{ publicGuestStorageSystem, SYSTEM_SHARE_USER, SYSTEM_UNSHARED_USER, null, true, false },
//				{ privateStorageSystem, SYSTEM_SHARE_USER, SYSTEM_UNSHARED_USER, null, false, false },
//				{ privateSharedGuestStorageSystem, SYSTEM_SHARE_USER, SYSTEM_UNSHARED_USER, null, false, false },
//				{ privateSharedUserStorageSystem, SYSTEM_SHARE_USER, SYSTEM_UNSHARED_USER, null, false, false },
//				{ privateSharedPublisherStorageSystem, SYSTEM_SHARE_USER, SYSTEM_UNSHARED_USER, null, false, false },
//				{ privateSharedAdminStorageSystem, SYSTEM_SHARE_USER, SYSTEM_UNSHARED_USER, null, false, false },
//				
//				{ publicStorageSystem, SYSTEM_UNSHARED_USER, SYSTEM_UNSHARED_USER, null, true, false },
//				{ publicMirroredStorageSystem, SYSTEM_UNSHARED_USER, SYSTEM_UNSHARED_USER, null, true, false },
//				{ publicGuestStorageSystem, SYSTEM_UNSHARED_USER, SYSTEM_UNSHARED_USER, null, true, false },
//				{ privateStorageSystem, SYSTEM_UNSHARED_USER, SYSTEM_UNSHARED_USER, null, false, false },
//				{ privateSharedGuestStorageSystem, SYSTEM_UNSHARED_USER, SYSTEM_UNSHARED_USER, null, false, false },
//				{ privateSharedUserStorageSystem, SYSTEM_UNSHARED_USER, SYSTEM_UNSHARED_USER, null, false, false },
//				{ privateSharedPublisherStorageSystem, SYSTEM_UNSHARED_USER, SYSTEM_UNSHARED_USER, null, false, false },
//				{ privateSharedAdminStorageSystem, SYSTEM_UNSHARED_USER, SYSTEM_UNSHARED_USER, null, false, false },
//		};
//	}
//	
//	@Test(dataProvider="testCanReadUserHomeProvider")
//	public void testCanReadImplicitUserHome(RemoteSystem system, String path, String owner, String internalUsername, boolean expectedResult, boolean shouldThrowException)
//	{
//		abstractTestCanRead(system, path, owner, internalUsername, expectedResult, shouldThrowException);
//	}
//	
//	@Test(dataProvider="testCanReadUserHomeProvider")
//	public void testCanReadExplicitUserHome(RemoteSystem system, String path, String owner, String internalUsername, boolean expectedResult, boolean shouldThrowException)
//	{
//		abstractTestCanRead(system, system.getStorageConfig().getHomeDir() + "/" + path, owner, internalUsername, expectedResult, shouldThrowException);
//	}
//	
//	@DataProvider
//	protected Object[][] testCanReadUnSharedDataProvider() throws Exception
//	{
//		beforeTestData();
//		List<Object[]> testList = new ArrayList<Object[]>();
//		
//		RemoteSystem publicStorageSystem = getPublicSystem(RemoteSystemType.STORAGE);
//		RemoteSystem publicMirroredStorageSystem = getPublicMirroredSystem(RemoteSystemType.STORAGE);
//		RemoteSystem publicGuestStorageSystem = getPublicGuestSystem(RemoteSystemType.STORAGE);
//		RemoteSystem privateStorageSystem = getPrivateSystem(RemoteSystemType.STORAGE);
//		RemoteSystem privateSharedGuestStorageSystem = getPrivateSharedGuestSystem(RemoteSystemType.STORAGE);
//		RemoteSystem privateSharedUserStorageSystem = getPrivateSharedUserSystem(RemoteSystemType.STORAGE);
//		RemoteSystem privateSharedPublisherStorageSystem = getPrivateSharedPublisherSystem(RemoteSystemType.STORAGE);
//		RemoteSystem privateSharedAdminStorageSystem = getPrivateSharedAdminSystem(RemoteSystemType.STORAGE);
//		
//		RemoteSystem[] systems = { 
//				publicStorageSystem, 
//				publicMirroredStorageSystem,
//				publicGuestStorageSystem,
//				privateStorageSystem,
//				privateSharedGuestStorageSystem,
//				privateSharedUserStorageSystem,
//				privateSharedPublisherStorageSystem,
//				privateSharedAdminStorageSystem
//		};
//		
//		// system owner shares with self only
//		String path = "/unknownfolder";
//		for (RemoteSystem system: systems) 
//		{ 
//			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
//		}
//		
//		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, false,	false });
//		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, false, 	false });
//		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
//		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		// public readonly systems and systems where the user is admin should always be readable
//		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		path = "/unknownfolder/shelfshared.dat";
//		for (RemoteSystem system: systems) 
//		{ 
//			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
//		}
//		
//		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, false,	false });
//		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, false, 	false });
//		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
//		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		// public readonly systems and systems where the user is admin should always be readable
//		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		path = "/unknownfile.dat";
//		for (RemoteSystem system: systems) 
//		{ 
//			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
//		}
//		
//		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, false,	false });
//		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, false, 	false });
//		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
//		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		// public readonly systems and systems where the user is admin should always be readable
//		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		return testList.toArray(new Object[][]{});
//	}
//	
//	@Test(dataProvider="testCanReadUnSharedDataProvider")
//	public void testCanReadUnSharedDirectory(RemoteSystem system, String path, String owner, String internalUsername, boolean expectedResult, boolean shouldThrowException)
//	{
//		abstractTestCanRead(system, path, owner, internalUsername, expectedResult, shouldThrowException);
//	}
//	
//	@DataProvider
//	protected Object[][] testCanReadUnSharedDataInUserHomeProvider() throws Exception
//	{
//		beforeTestData();
//		List<Object[]> testList = new ArrayList<Object[]>();
//		
//		RemoteSystem publicStorageSystem = getPublicSystem(RemoteSystemType.STORAGE);
//		RemoteSystem publicMirroredStorageSystem = getPublicMirroredSystem(RemoteSystemType.STORAGE);
//		RemoteSystem publicGuestStorageSystem = getPublicGuestSystem(RemoteSystemType.STORAGE);
//		RemoteSystem privateStorageSystem = getPrivateSystem(RemoteSystemType.STORAGE);
//		RemoteSystem privateSharedGuestStorageSystem = getPrivateSharedGuestSystem(RemoteSystemType.STORAGE);
//		RemoteSystem privateSharedUserStorageSystem = getPrivateSharedUserSystem(RemoteSystemType.STORAGE);
//		RemoteSystem privateSharedPublisherStorageSystem = getPrivateSharedPublisherSystem(RemoteSystemType.STORAGE);
//		RemoteSystem privateSharedAdminStorageSystem = getPrivateSharedAdminSystem(RemoteSystemType.STORAGE);
//		
//		RemoteSystem[] systems = { 
//				publicStorageSystem, 
//				publicMirroredStorageSystem,
//				publicGuestStorageSystem,
//				privateStorageSystem,
//				privateSharedGuestStorageSystem,
//				privateSharedUserStorageSystem,
//				privateSharedPublisherStorageSystem,
//				privateSharedAdminStorageSystem
//		};
//		
//		// system owner shares with self only
//		String path = SYSTEM_OWNER + "/unknownfolder";
//		for (RemoteSystem system: systems) 
//		{ 
//			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
//		}
//		
//		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, false,	false });
//		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, false, 	false });
//		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
//		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		// public readonly systems and systems where the user is admin should always be readable
//		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		path = SYSTEM_OWNER + "/unknownfolder/shelfshared.dat";
//		for (RemoteSystem system: systems) 
//		{ 
//			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
//		}
//		
//		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, false,	false });
//		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, false, 	false });
//		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
//		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		// public readonly systems and systems where the user is admin should always be readable
//		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		path = SYSTEM_OWNER + "/unknownfile.dat";
//		for (RemoteSystem system: systems) 
//		{ 
//			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
//		}
//		
//		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, false,	false });
//		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, false, 	false });
//		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
//		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		// public readonly systems and systems where the user is admin should always be readable
//		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//	
//		path = SYSTEM_OWNER + "/some/deep/path/to/unknownfile.dat";
//		for (RemoteSystem system: systems) 
//		{ 
//			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
//		}
//		
//		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, false,	false });
//		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, false, 	false });
//		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
//		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		// public readonly systems and systems where the user is admin should always be readable
//		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		return testList.toArray(new Object[][]{});
//	}
//	
//	@Test(dataProvider="testCanReadUnSharedDataInUserHomeProvider")
//	public void testCanReadUnSharedDataInUserHome(RemoteSystem system, String path, String owner, String internalUsername, boolean expectedResult, boolean shouldThrowException)
//	{
//		abstractTestCanRead(system, path, owner, internalUsername, expectedResult, shouldThrowException);
//	}
//	
//	@DataProvider
//	protected Object[][] testCanReadUnSharedDataInOwnHomeProvider() throws Exception
//	{
//		beforeTestData();
//		List<Object[]> testList = new ArrayList<Object[]>();
//		
//		RemoteSystem publicStorageSystem = getPublicSystem(RemoteSystemType.STORAGE);
//		RemoteSystem publicMirroredStorageSystem = getPublicMirroredSystem(RemoteSystemType.STORAGE);
//		RemoteSystem publicGuestStorageSystem = getPublicGuestSystem(RemoteSystemType.STORAGE);
//		RemoteSystem privateStorageSystem = getPrivateSystem(RemoteSystemType.STORAGE);
//		RemoteSystem privateSharedGuestStorageSystem = getPrivateSharedGuestSystem(RemoteSystemType.STORAGE);
//		RemoteSystem privateSharedUserStorageSystem = getPrivateSharedUserSystem(RemoteSystemType.STORAGE);
//		RemoteSystem privateSharedPublisherStorageSystem = getPrivateSharedPublisherSystem(RemoteSystemType.STORAGE);
//		RemoteSystem privateSharedAdminStorageSystem = getPrivateSharedAdminSystem(RemoteSystemType.STORAGE);
//		
//		RemoteSystem[] systems = { 
//				publicStorageSystem, 
//				publicMirroredStorageSystem,
//				publicGuestStorageSystem,
//				privateStorageSystem,
//				privateSharedGuestStorageSystem,
//				privateSharedUserStorageSystem,
//				privateSharedPublisherStorageSystem,
//				privateSharedAdminStorageSystem
//		};
//		
//		// system owner shares with self only
//		String path = SYSTEM_SHARE_USER + "/unknownfolder";
//		for (RemoteSystem system: systems) 
//		{ 
//			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
//		}
//		
//		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, true,	false });
//		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
//		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		// public readonly systems and systems where the user is admin should always be readable
//		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		path = SYSTEM_SHARE_USER + "/unknownfolder/shelfshared.dat";
//		for (RemoteSystem system: systems) 
//		{ 
//			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
//		}
//		
//		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, true,	false });
//		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
//		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		// public readonly systems and systems where the user is admin should always be readable
//		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		path = SYSTEM_SHARE_USER + "/unknownfile.dat";
//		for (RemoteSystem system: systems) 
//		{ 
//			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
//		}
//		
//		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, true,	false });
//		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
//		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		// public readonly systems and systems where the user is admin should always be readable
//		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		path = SYSTEM_SHARE_USER + "/some/deep/path/to/unknownfile.dat";
//		for (RemoteSystem system: systems) 
//		{ 
//			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
//		}
//		
//		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, true,	false });
//		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
//		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		// public readonly systems and systems where the user is admin should always be readable
//		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		return testList.toArray(new Object[][]{});
//	}
//	
//	@Test(dataProvider="testCanReadUnSharedDataInOwnHomeProvider")
//	public void testCanReadUnSharedDataInOwnHome(RemoteSystem system, String path, String owner, String internalUsername, boolean expectedResult, boolean shouldThrowException)
//	{
//		abstractTestCanRead(system, path, owner, internalUsername, expectedResult, shouldThrowException);
//	}
//	
//	@DataProvider
//	protected Object[][] testCanReadDataSharedWithSelfProvider() throws Exception
//	{
//		beforeTestData();
//		List<Object[]> testList = new ArrayList<Object[]>();
//		
//		RemoteSystem publicStorageSystem = getPublicSystem(RemoteSystemType.STORAGE);
//		RemoteSystem publicMirroredStorageSystem = getPublicMirroredSystem(RemoteSystemType.STORAGE);
//		RemoteSystem publicGuestStorageSystem = getPublicGuestSystem(RemoteSystemType.STORAGE);
//		RemoteSystem privateStorageSystem = getPrivateSystem(RemoteSystemType.STORAGE);
//		RemoteSystem privateSharedGuestStorageSystem = getPrivateSharedGuestSystem(RemoteSystemType.STORAGE);
//		RemoteSystem privateSharedUserStorageSystem = getPrivateSharedUserSystem(RemoteSystemType.STORAGE);
//		RemoteSystem privateSharedPublisherStorageSystem = getPrivateSharedPublisherSystem(RemoteSystemType.STORAGE);
//		RemoteSystem privateSharedAdminStorageSystem = getPrivateSharedAdminSystem(RemoteSystemType.STORAGE);
//		
//		RemoteSystem[] systems = { 
//				publicStorageSystem, 
//				publicMirroredStorageSystem,
//				publicGuestStorageSystem,
//				privateStorageSystem,
//				privateSharedGuestStorageSystem,
//				privateSharedUserStorageSystem,
//				privateSharedPublisherStorageSystem,
//				privateSharedAdminStorageSystem
//		};
//		
//		String rootDir = StringUtils.substring(getSystemRoot(systems[0]), 0, -1);
//		
//		// system owner shares with self only
//		String path = "/systemownerselfsharednotrecursive";
//		for (RemoteSystem system: systems) 
//		{ 
//			createSharedLogicalFile(SYSTEM_OWNER, PermissionType.READ, SYSTEM_OWNER, system, rootDir + path, LogicalFile.DIRECTORY, false); 
//			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
//		}
//		
//		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, false,	false });
//		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, false, 	false });
//		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
//		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		// public readonly systems and systems where the user is admin should always be readable
//		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		path = "/systemownerselfsharednotrecursive/systemownershelfshared.dat";
//		for (RemoteSystem system: systems) 
//		{ 
//			createSharedLogicalFile(SYSTEM_OWNER, PermissionType.READ, SYSTEM_OWNER, system, rootDir + path, LogicalFile.DIRECTORY, false); 
//			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
//		}
//		
//		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, false,	false });
//		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, false, 	false });
//		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
//		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		// public readonly systems and systems where the user is admin should always be readable
//		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		path = rootDir + "systemownershelfshared.dat";
//		for (RemoteSystem system: systems) 
//		{ 
//			createSharedLogicalFile(SYSTEM_OWNER, PermissionType.READ, SYSTEM_OWNER, system, rootDir + path, LogicalFile.DIRECTORY, false); 
//			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
//		}
//		
//		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, false,	false });
//		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, false, 	false });
//		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
//		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		// public readonly systems and systems where the user is admin should always be readable
//		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		return testList.toArray(new Object[][]{});
//	}
//	
//	@Test(dataProvider="testCanReadDataSharedWithSelfProvider")
//	public void testCanReadDataSharedWithSelf(RemoteSystem system, String path, String owner, String internalUsername, boolean expectedResult, boolean shouldThrowException)
//	{
//		abstractTestCanRead(system, path, owner, internalUsername, expectedResult, shouldThrowException);
//	}
//	
//	@DataProvider
//	protected Object[][] testCanReadDataSharedWithUserProvider() throws Exception
//	{
//		beforeTestData();
//		List<Object[]> testList = new ArrayList<Object[]>();
//		
//		RemoteSystem publicStorageSystem = getPublicSystem(RemoteSystemType.STORAGE);
//		RemoteSystem publicMirroredStorageSystem = getPublicMirroredSystem(RemoteSystemType.STORAGE);
//		RemoteSystem publicGuestStorageSystem = getPublicGuestSystem(RemoteSystemType.STORAGE);
//		RemoteSystem privateStorageSystem = getPrivateSystem(RemoteSystemType.STORAGE);
//		RemoteSystem privateSharedGuestStorageSystem = getPrivateSharedGuestSystem(RemoteSystemType.STORAGE);
//		RemoteSystem privateSharedUserStorageSystem = getPrivateSharedUserSystem(RemoteSystemType.STORAGE);
//		RemoteSystem privateSharedPublisherStorageSystem = getPrivateSharedPublisherSystem(RemoteSystemType.STORAGE);
//		RemoteSystem privateSharedAdminStorageSystem = getPrivateSharedAdminSystem(RemoteSystemType.STORAGE);
//		
//		RemoteSystem[] systems = { 
//				publicStorageSystem, 
//				publicMirroredStorageSystem,
//				publicGuestStorageSystem,
//				privateStorageSystem,
//				privateSharedGuestStorageSystem,
//				privateSharedUserStorageSystem,
//				privateSharedPublisherStorageSystem,
//				privateSharedAdminStorageSystem
//		};
//		
//		String rootDir = StringUtils.substring(getSystemRoot(systems[0]), 0, -1);
//		
//		// system owner shares with self only
//		String path = "/sharedfoldernotrecursive";
//		for (RemoteSystem system: systems) 
//		{ 
//			createSharedLogicalFile(SYSTEM_OWNER, PermissionType.READ, SYSTEM_SHARE_USER, system, rootDir + path, LogicalFile.DIRECTORY, false); 
//			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
//		}
//		
//		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, true,	false });
//		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
//		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		// public readonly systems and systems where the user is admin should always be readable
//		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		path = "/sharedfoldernotrecursive/sharedfile.dat";
//		for (RemoteSystem system: systems) 
//		{ 
//			createSharedLogicalFile(SYSTEM_OWNER, PermissionType.READ, SYSTEM_SHARE_USER, system, rootDir + path, LogicalFile.DIRECTORY, false); 
//			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
//		}
//		
//		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, true,	false });
//		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
//		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		// public readonly systems and systems where the user is admin should always be readable
//		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		path = "/sharedfoldernotrecursive/unsharedfile.dat";
//		for (RemoteSystem system: systems) 
//		{ 
//			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
//		}
//		
//		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, false,	false });
//		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, false, 	false });
//		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
//		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
////		
//		// public readonly systems and systems where the user is admin should always be readable
//		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		path = "/sharedfolderrecursive";
//		for (RemoteSystem system: systems) 
//		{ 
//			createSharedLogicalFile(SYSTEM_OWNER, PermissionType.READ, SYSTEM_SHARE_USER, system, rootDir + path, LogicalFile.DIRECTORY, true); 
//			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
//		}
//		
//		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, true,	false });
//		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
//		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		// public readonly systems and systems where the user is admin should always be readable
//		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		path = "/sharedfolderrecursive/unsharedfile.dat";
//		for (RemoteSystem system: systems) 
//		{ 
//			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
//		}
//		
//		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, true,	false });
//		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
//		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		// public readonly systems and systems where the user is admin should always be readable
//		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		
//		path = "/sharedfolderrecursive/sharedfile.dat";
//		for (RemoteSystem system: systems) 
//		{ 
//			createSharedLogicalFile(SYSTEM_OWNER, PermissionType.READ, SYSTEM_SHARE_USER, system, rootDir + path, LogicalFile.DIRECTORY, false); 
//			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
//		}
//		
//		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, true,	false });
//		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
//		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		// public readonly systems and systems where the user is admin should always be readable
//		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		
//		path = "/unsharedfolder";
//		for (RemoteSystem system: systems) 
//		{ 
//			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
//		}
//		
//		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, false,	false });
//		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, false, 	false });
//		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
//		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		// public readonly systems and systems where the user is admin should always be readable
//		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		path = "/unsharedfolder/unsharedfile.dat";
//		for (RemoteSystem system: systems) 
//		{ 
//			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
//		}
//		
//		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, false,	false });
//		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, false, 	false });
//		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
//		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		// public readonly systems and systems where the user is admin should always be readable
//		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		
//		path = "/unsharedfolder/sharedfile.dat";
//		for (RemoteSystem system: systems) 
//		{ 
//			createSharedLogicalFile(SYSTEM_OWNER, PermissionType.READ, SYSTEM_SHARE_USER, system, rootDir + path, LogicalFile.DIRECTORY, false); 
//			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
//		}
//		
//		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, true,	false });
//		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
//		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		// public readonly systems and systems where the user is admin should always be readable
//		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		
//		path = "/systemownershelfshared.dat";
//		for (RemoteSystem system: systems) 
//		{ 
//			createSharedLogicalFile(SYSTEM_OWNER, PermissionType.READ, SYSTEM_SHARE_USER, system, rootDir + path, LogicalFile.DIRECTORY, false); 
//			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
//		}
//		
//		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, true,	false });
//		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
//		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		// public readonly systems and systems where the user is admin should always be readable
//		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		return testList.toArray(new Object[][]{});
//	}
//	
//	@Test(dataProvider="testCanReadDataSharedWithUserProvider")
//	public void testCanReadDataSharedWithUser(RemoteSystem system, String path, String owner, String internalUsername, boolean expectedResult, boolean shouldThrowException)
//	{
//		abstractTestCanRead(system, path, owner, internalUsername, expectedResult, shouldThrowException);
//	}
//	
//	@DataProvider
//	protected Object[][] testCanReadHomeDirectorySharedWithUserProvider() throws Exception
//	{
//		beforeTestData();
//		List<Object[]> testList = new ArrayList<Object[]>();
//		
//		RemoteSystem publicStorageSystem = getPublicSystem(RemoteSystemType.STORAGE);
//		RemoteSystem publicMirroredStorageSystem = getPublicMirroredSystem(RemoteSystemType.STORAGE);
//		RemoteSystem publicGuestStorageSystem = getPublicGuestSystem(RemoteSystemType.STORAGE);
//		RemoteSystem privateStorageSystem = getPrivateSystem(RemoteSystemType.STORAGE);
//		RemoteSystem privateSharedGuestStorageSystem = getPrivateSharedGuestSystem(RemoteSystemType.STORAGE);
//		RemoteSystem privateSharedUserStorageSystem = getPrivateSharedUserSystem(RemoteSystemType.STORAGE);
//		RemoteSystem privateSharedPublisherStorageSystem = getPrivateSharedPublisherSystem(RemoteSystemType.STORAGE);
//		RemoteSystem privateSharedAdminStorageSystem = getPrivateSharedAdminSystem(RemoteSystemType.STORAGE);
//		
//		RemoteSystem[] systems = { 
//				publicStorageSystem, 
//				publicMirroredStorageSystem,
//				publicGuestStorageSystem,
//				privateStorageSystem,
//				privateSharedGuestStorageSystem,
//				privateSharedUserStorageSystem,
//				privateSharedPublisherStorageSystem,
//				privateSharedAdminStorageSystem
//		};
//		
//		String homeDir = getSystemHome(systems[0]);
//		
//		String path = SYSTEM_OWNER;
//		for (RemoteSystem system: systems) 
//		{ 
//			createSharedLogicalFile(SYSTEM_OWNER, PermissionType.READ, SYSTEM_SHARE_USER, system, homeDir + path, LogicalFile.DIRECTORY, true); 
//			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
//		}
//		
//		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, true,	false });
//		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
//		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		// public readonly systems and systems where the user is admin should always be readable
//		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		path = SYSTEM_OWNER + "/someunsharedfile.txt";
//		for (RemoteSystem system: systems) 
//		{ 
//			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
//		}
//		
//		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, true,	false });
//		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
//		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		// public readonly systems and systems where the user is admin should always be readable
//		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		return testList.toArray(new Object[][]{});
//	}
//	
//	@Test(dataProvider="testCanReadHomeDirectorySharedWithUserProvider")
//	public void testCanReadHomeDirectorySharedWithUser(RemoteSystem system, String path, String owner, String internalUsername, boolean expectedResult, boolean shouldThrowException)
//	{
//		abstractTestCanRead(system, path, owner, internalUsername, expectedResult, shouldThrowException);
//	}
//	
//	@DataProvider
//	protected Object[][] testCanReadHomeDirectoryDataSharedWithUserProvider() throws Exception
//	{
//		beforeTestData();
//		List<Object[]> testList = new ArrayList<Object[]>();
//		
//		RemoteSystem publicStorageSystem = getPublicSystem(RemoteSystemType.STORAGE);
//		RemoteSystem publicMirroredStorageSystem = getPublicMirroredSystem(RemoteSystemType.STORAGE);
//		RemoteSystem publicGuestStorageSystem = getPublicGuestSystem(RemoteSystemType.STORAGE);
//		RemoteSystem privateStorageSystem = getPrivateSystem(RemoteSystemType.STORAGE);
//		RemoteSystem privateSharedGuestStorageSystem = getPrivateSharedGuestSystem(RemoteSystemType.STORAGE);
//		RemoteSystem privateSharedUserStorageSystem = getPrivateSharedUserSystem(RemoteSystemType.STORAGE);
//		RemoteSystem privateSharedPublisherStorageSystem = getPrivateSharedPublisherSystem(RemoteSystemType.STORAGE);
//		RemoteSystem privateSharedAdminStorageSystem = getPrivateSharedAdminSystem(RemoteSystemType.STORAGE);
//		
//		RemoteSystem[] systems = { 
//				publicStorageSystem, 
//				publicMirroredStorageSystem,
//				publicGuestStorageSystem,
//				privateStorageSystem,
//				privateSharedGuestStorageSystem,
//				privateSharedUserStorageSystem,
//				privateSharedPublisherStorageSystem,
//				privateSharedAdminStorageSystem
//		};
//		
//		String homeDir = getSystemHome(systems[0]);
//		
//		// system owner shares with self only
//		String path = SYSTEM_OWNER + "/sharedfoldernotrecursive";
//		for (RemoteSystem system: systems) 
//		{ 
//			createSharedLogicalFile(SYSTEM_OWNER, PermissionType.READ, SYSTEM_SHARE_USER, system, homeDir + path, LogicalFile.DIRECTORY, false); 
//			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
//		}
//		
//		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, true,	false });
//		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
//		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		// public readonly systems and systems where the user is admin should always be readable
//		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		path = SYSTEM_OWNER + "/sharedfoldernotrecursive/sharedfile.dat";
//		for (RemoteSystem system: systems) 
//		{ 
//			createSharedLogicalFile(SYSTEM_OWNER, PermissionType.READ, SYSTEM_SHARE_USER, system, homeDir + path, LogicalFile.DIRECTORY, false); 
//			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
//		}
//		
//		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, true,	false });
//		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
//		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		// public readonly systems and systems where the user is admin should always be readable
//		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		path = SYSTEM_OWNER + "/sharedfoldernotrecursive/unsharedfile.dat";
//		for (RemoteSystem system: systems) 
//		{ 
//			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
//		}
//		
//		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, false,	false });
//		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, false, 	false });
//		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
//		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		// public readonly systems and systems where the user is admin should always be readable
//		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		path = SYSTEM_OWNER + "/sharedfolderrecursive";
//		for (RemoteSystem system: systems) 
//		{ 
//			createSharedLogicalFile(SYSTEM_OWNER, PermissionType.READ, SYSTEM_SHARE_USER, system, homeDir + path, LogicalFile.DIRECTORY, true); 
//			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
//		}
//		
//		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, true,	false });
//		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
//		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		// public readonly systems and systems where the user is admin should always be readable
//		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		path = SYSTEM_OWNER + "/sharedfolderrecursive/unsharedfile.dat";
//		for (RemoteSystem system: systems) 
//		{ 
//			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
//		}
//		
//		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, true,	false });
//		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
//		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		// public readonly systems and systems where the user is admin should always be readable
//		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		
//		path = SYSTEM_OWNER + "/sharedfolderrecursive/sharedfile.dat";
//		for (RemoteSystem system: systems) 
//		{ 
//			createSharedLogicalFile(SYSTEM_OWNER, PermissionType.READ, SYSTEM_SHARE_USER, system, homeDir + path, LogicalFile.DIRECTORY, false); 
//			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
//		}
//		
//		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, true,	false });
//		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
//		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		// public readonly systems and systems where the user is admin should always be readable
//		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		
//		path = SYSTEM_OWNER + "/unsharedfolder";
//		for (RemoteSystem system: systems) 
//		{ 
//			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
//		}
//		
//		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, false,	false });
//		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, false, 	false });
//		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
//		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		// public readonly systems and systems where the user is admin should always be readable
//		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		path = SYSTEM_OWNER + "/unsharedfolder/unsharedfile.dat";
//		for (RemoteSystem system: systems) 
//		{ 
//			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
//		}
//		
//		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, false,	false });
//		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, false, 	false });
//		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
//		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		// public readonly systems and systems where the user is admin should always be readable
//		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		
//		path = SYSTEM_OWNER + "/unsharedfolder/sharedfile.dat";
//		for (RemoteSystem system: systems) 
//		{ 
//			createSharedLogicalFile(SYSTEM_OWNER, PermissionType.READ, SYSTEM_SHARE_USER, system, homeDir + path, LogicalFile.DIRECTORY, false); 
//			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
//		}
//		
//		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, true,	false });
//		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
//		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		// public readonly systems and systems where the user is admin should always be readable
//		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		
//		path = SYSTEM_OWNER + "/systemownershelfshared.dat";
//		for (RemoteSystem system: systems) 
//		{ 
//			createSharedLogicalFile(SYSTEM_OWNER, PermissionType.READ, SYSTEM_SHARE_USER, system, homeDir + path, LogicalFile.DIRECTORY, false); 
//			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
//		}
//		
//		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, true,	false });
//		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
//		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		// public readonly systems and systems where the user is admin should always be readable
//		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		return testList.toArray(new Object[][]{});
//	}
//	
//	@Test(dataProvider="testCanReadHomeDirectoryDataSharedWithUserProvider")
//	public void testCanReadHomeDirectoryDataSharedWithUser(RemoteSystem system, String path, String owner, String internalUsername, boolean expectedResult, boolean shouldThrowException)
//	{
//		abstractTestCanRead(system, path, owner, internalUsername, expectedResult, shouldThrowException);
//	}
//	
//	@DataProvider
//	protected Object[][] testCanReadPublicDirectoryInRootDirectoryProvider() throws Exception
//	{
//		beforeTestData();
//		List<Object[]> testList = new ArrayList<Object[]>();
//		
//		RemoteSystem publicStorageSystem = getPublicSystem(RemoteSystemType.STORAGE);
//		RemoteSystem publicMirroredStorageSystem = getPublicMirroredSystem(RemoteSystemType.STORAGE);
//		RemoteSystem publicGuestStorageSystem = getPublicGuestSystem(RemoteSystemType.STORAGE);
//		RemoteSystem privateStorageSystem = getPrivateSystem(RemoteSystemType.STORAGE);
//		RemoteSystem privateSharedGuestStorageSystem = getPrivateSharedGuestSystem(RemoteSystemType.STORAGE);
//		RemoteSystem privateSharedUserStorageSystem = getPrivateSharedUserSystem(RemoteSystemType.STORAGE);
//		RemoteSystem privateSharedPublisherStorageSystem = getPrivateSharedPublisherSystem(RemoteSystemType.STORAGE);
//		RemoteSystem privateSharedAdminStorageSystem = getPrivateSharedAdminSystem(RemoteSystemType.STORAGE);
//		
//		RemoteSystem[] systems = { 
//				publicStorageSystem, 
//				publicMirroredStorageSystem,
//				publicGuestStorageSystem,
//				privateStorageSystem,
//				privateSharedGuestStorageSystem,
//				privateSharedUserStorageSystem,
//				privateSharedPublisherStorageSystem,
//				privateSharedAdminStorageSystem
//		};
//		
//		String rootDir = getSystemRoot(systems[0]);
//		String sharedUser = Settings.PUBLIC_USER_USERNAME;
//		
//		// system owner shares with self only
//		String path = "/publicfoldernotrecursive";
//		for (RemoteSystem system: systems) 
//		{ 
//			createSharedLogicalFile(SYSTEM_OWNER, PermissionType.READ, sharedUser, system, rootDir + path, LogicalFile.DIRECTORY, false); 
//			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
//		}
//		
//		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, true,	false });
//		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
//		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		// public readonly systems and systems where the user is admin should always be readable
//		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		path = "/publicfoldernotrecursive/sharedfile.dat";
//		for (RemoteSystem system: systems) 
//		{ 
//			createSharedLogicalFile(SYSTEM_OWNER, PermissionType.READ, sharedUser, system, rootDir + path, LogicalFile.DIRECTORY, false); 
//			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
//		}
//		
//		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, true,	false });
//		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
//		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		// public readonly systems and systems where the user is admin should always be readable
//		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		path = "/publicfoldernotrecursive/unsharedfile.dat";
//		for (RemoteSystem system: systems) 
//		{ 
//			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
//		}
//		
//		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, false,	false });
//		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, false, 	false });
//		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
//		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		// public readonly systems and systems where the user is admin should always be readable
//		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		path = "/sharedfolderrecursive";
//		for (RemoteSystem system: systems) 
//		{ 
//			createSharedLogicalFile(SYSTEM_OWNER, PermissionType.READ, sharedUser, system, rootDir + path, LogicalFile.DIRECTORY, true); 
//			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
//		}
//		
//		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, true,	false });
//		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
//		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		// public readonly systems and systems where the user is admin should always be readable
//		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		path = "/sharedfolderrecursive/unsharedfile.dat";
//		for (RemoteSystem system: systems) 
//		{ 
//			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
//		}
//		
//		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, true,	false });
//		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
//		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		// public readonly systems and systems where the user is admin should always be readable
//		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		
//		path = "/sharedfolderrecursive/sharedfile.dat";
//		for (RemoteSystem system: systems) 
//		{ 
//			createSharedLogicalFile(SYSTEM_OWNER, PermissionType.READ, sharedUser, system, rootDir + path, LogicalFile.DIRECTORY, false); 
//			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
//		}
//		
//		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, true,	false });
//		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
//		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		// public readonly systems and systems where the user is admin should always be readable
//		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		
//		path = "/unsharedfolder";
//		for (RemoteSystem system: systems) 
//		{ 
//			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
//		}
//		
//		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, false,	false });
//		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, false, 	false });
//		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
//		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		// public readonly systems and systems where the user is admin should always be readable
//		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		path = "/unsharedfolder/unsharedfile.dat";
//		for (RemoteSystem system: systems) 
//		{ 
//			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
//		}
//		
//		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, false,	false });
//		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, false, 	false });
//		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
//		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		// public readonly systems and systems where the user is admin should always be readable
//		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		
//		path = "/unsharedfolder/sharedfile.dat";
//		for (RemoteSystem system: systems) 
//		{ 
//			createSharedLogicalFile(SYSTEM_OWNER, PermissionType.READ, sharedUser, system, rootDir + path, LogicalFile.DIRECTORY, false); 
//			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
//		}
//		
//		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, true,	false });
//		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
//		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		// public readonly systems and systems where the user is admin should always be readable
//		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		
//		path = "/systemownerpublicshared.dat";
//		for (RemoteSystem system: systems) 
//		{ 
//			createSharedLogicalFile(SYSTEM_OWNER, PermissionType.READ, sharedUser, system, rootDir + path, LogicalFile.DIRECTORY, false); 
//			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
//		}
//		
//		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, true,	false });
//		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
//		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		// public readonly systems and systems where the user is admin should always be readable
//		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		return testList.toArray(new Object[][]{});
//	}
//	
//	@Test(dataProvider="testCanReadPublicDirectoryInRootDirectoryProvider")
//	public void testCanReadPublicDirectoryInRootDirectory(RemoteSystem system, String path, String owner, String internalUsername, boolean expectedResult, boolean shouldThrowException)
//	{
//		abstractTestCanRead(system, path, owner, internalUsername, expectedResult, shouldThrowException);
//	}
//	
//	@DataProvider
//	protected Object[][] testCanReadWorldDirectoryInRootDirectoryProvider() throws Exception
//	{
//		beforeTestData();
//		List<Object[]> testList = new ArrayList<Object[]>();
//		
//		RemoteSystem publicStorageSystem = getPublicSystem(RemoteSystemType.STORAGE);
//		RemoteSystem publicMirroredStorageSystem = getPublicMirroredSystem(RemoteSystemType.STORAGE);
//		RemoteSystem publicGuestStorageSystem = getPublicGuestSystem(RemoteSystemType.STORAGE);
//		RemoteSystem privateStorageSystem = getPrivateSystem(RemoteSystemType.STORAGE);
//		RemoteSystem privateSharedGuestStorageSystem = getPrivateSharedGuestSystem(RemoteSystemType.STORAGE);
//		RemoteSystem privateSharedUserStorageSystem = getPrivateSharedUserSystem(RemoteSystemType.STORAGE);
//		RemoteSystem privateSharedPublisherStorageSystem = getPrivateSharedPublisherSystem(RemoteSystemType.STORAGE);
//		RemoteSystem privateSharedAdminStorageSystem = getPrivateSharedAdminSystem(RemoteSystemType.STORAGE);
//		
//		RemoteSystem[] systems = { 
//				publicStorageSystem, 
//				publicMirroredStorageSystem,
//				publicGuestStorageSystem,
//				privateStorageSystem,
//				privateSharedGuestStorageSystem,
//				privateSharedUserStorageSystem,
//				privateSharedPublisherStorageSystem,
//				privateSharedAdminStorageSystem
//		};
//		
//		String rootDir = getSystemRoot(systems[0]);
//		String sharedUser = Settings.WORLD_USER_USERNAME;
//		
//		// system owner shares with self only
//		String path = "/publicfoldernotrecursive";
//		for (RemoteSystem system: systems) 
//		{ 
//			createSharedLogicalFile(SYSTEM_OWNER, PermissionType.READ, sharedUser, system, rootDir + path, LogicalFile.DIRECTORY, false); 
//			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
//		}
//		
//		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, true,	false });
//		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
//		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		// public readonly systems and systems where the user is admin should always be readable
//		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		path = "/publicfoldernotrecursive/sharedfile.dat";
//		for (RemoteSystem system: systems) 
//		{ 
//			createSharedLogicalFile(SYSTEM_OWNER, PermissionType.READ, sharedUser, system, rootDir + path, LogicalFile.DIRECTORY, false); 
//			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
//		}
//		
//		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, true,	false });
//		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
//		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		// public readonly systems and systems where the user is admin should always be readable
//		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		path = "/publicfoldernotrecursive/unsharedfile.dat";
//		for (RemoteSystem system: systems) 
//		{ 
//			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
//		}
//		
//		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, false,	false });
//		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, false, 	false });
//		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
//		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		// public readonly systems and systems where the user is admin should always be readable
//		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		path = "/sharedfolderrecursive";
//		for (RemoteSystem system: systems) 
//		{ 
//			createSharedLogicalFile(SYSTEM_OWNER, PermissionType.READ, sharedUser, system, rootDir + path, LogicalFile.DIRECTORY, true); 
//			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
//		}
//		
//		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, true,	false });
//		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
//		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		// public readonly systems and systems where the user is admin should always be readable
//		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		path = "/sharedfolderrecursive/unsharedfile.dat";
//		for (RemoteSystem system: systems) 
//		{ 
//			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
//		}
//		
//		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, true,	false });
//		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
//		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		// public readonly systems and systems where the user is admin should always be readable
//		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		
//		path = "/sharedfolderrecursive/sharedfile.dat";
//		for (RemoteSystem system: systems) 
//		{ 
//			createSharedLogicalFile(SYSTEM_OWNER, PermissionType.READ, sharedUser, system, rootDir + path, LogicalFile.DIRECTORY, false); 
//			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
//		}
//		
//		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, true,	false });
//		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
//		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		// public readonly systems and systems where the user is admin should always be readable
//		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		
//		path = "/unsharedfolder";
//		for (RemoteSystem system: systems) 
//		{ 
//			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
//		}
//		
//		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, false,	false });
//		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, false, 	false });
//		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
//		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		// public readonly systems and systems where the user is admin should always be readable
//		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		path = "/unsharedfolder/unsharedfile.dat";
//		for (RemoteSystem system: systems) 
//		{ 
//			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
//		}
//		
//		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, false,	false });
//		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, false, 	false });
//		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
//		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		// public readonly systems and systems where the user is admin should always be readable
//		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		
//		path = "/unsharedfolder/sharedfile.dat";
//		for (RemoteSystem system: systems) 
//		{ 
//			createSharedLogicalFile(SYSTEM_OWNER, PermissionType.READ, sharedUser, system, rootDir + path, LogicalFile.DIRECTORY, false); 
//			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
//		}
//		
//		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, true,	false });
//		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
//		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		// public readonly systems and systems where the user is admin should always be readable
//		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		
//		path = "/systemownerpublicshared.dat";
//		for (RemoteSystem system: systems) 
//		{ 
//			createSharedLogicalFile(SYSTEM_OWNER, PermissionType.READ, sharedUser, system, rootDir + path, LogicalFile.DIRECTORY, false); 
//			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
//		}
//		
//		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, true,	false });
//		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
//		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		// public readonly systems and systems where the user is admin should always be readable
//		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		return testList.toArray(new Object[][]{});
//	}
//	
//	@Test(dataProvider="testCanReadWorldDirectoryInRootDirectoryProvider")
//	public void testCanReadWorldDirectoryInRootDirectory(RemoteSystem system, String path, String owner, String internalUsername, boolean expectedResult, boolean shouldThrowException)
//	{
//		abstractTestCanRead(system, path, owner, internalUsername, expectedResult, shouldThrowException);
//	}
//	
//	@DataProvider
//	protected Object[][] testCanReadPublicDirectoryInUserHomeProvider() throws Exception
//	{
//		beforeTestData();
//		List<Object[]> testList = new ArrayList<Object[]>();
//		
//		RemoteSystem publicStorageSystem = getPublicSystem(RemoteSystemType.STORAGE);
//		RemoteSystem publicMirroredStorageSystem = getPublicMirroredSystem(RemoteSystemType.STORAGE);
//		RemoteSystem publicGuestStorageSystem = getPublicGuestSystem(RemoteSystemType.STORAGE);
//		RemoteSystem privateStorageSystem = getPrivateSystem(RemoteSystemType.STORAGE);
//		RemoteSystem privateSharedGuestStorageSystem = getPrivateSharedGuestSystem(RemoteSystemType.STORAGE);
//		RemoteSystem privateSharedUserStorageSystem = getPrivateSharedUserSystem(RemoteSystemType.STORAGE);
//		RemoteSystem privateSharedPublisherStorageSystem = getPrivateSharedPublisherSystem(RemoteSystemType.STORAGE);
//		RemoteSystem privateSharedAdminStorageSystem = getPrivateSharedAdminSystem(RemoteSystemType.STORAGE);
//		
//		RemoteSystem[] systems = { 
//				publicStorageSystem, 
//				publicMirroredStorageSystem,
//				publicGuestStorageSystem,
//				privateStorageSystem,
//				privateSharedGuestStorageSystem,
//				privateSharedUserStorageSystem,
//				privateSharedPublisherStorageSystem,
//				privateSharedAdminStorageSystem
//		};
//		
//		String homeDir = getSystemHome(systems[0]);
//		String sharedUser = Settings.PUBLIC_USER_USERNAME;
//		
//		// system owner shares with self only
//		String path = SYSTEM_OWNER + "/publicfoldernotrecursive";
//		for (RemoteSystem system: systems) 
//		{ 
//			createSharedLogicalFile(SYSTEM_OWNER, PermissionType.READ, sharedUser, system, homeDir + path, LogicalFile.DIRECTORY, false); 
//			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
//		}
//		
//		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, true,	false });
//		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
//		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		// public readonly systems and systems where the user is admin should always be readable
//		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		path = SYSTEM_OWNER + "/publicfoldernotrecursive/sharedfile.dat";
//		for (RemoteSystem system: systems) 
//		{ 
//			createSharedLogicalFile(SYSTEM_OWNER, PermissionType.READ, sharedUser, system, homeDir + path, LogicalFile.DIRECTORY, false); 
//			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
//		}
//		
//		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, true,	false });
//		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
//		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		// public readonly systems and systems where the user is admin should always be readable
//		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		path = SYSTEM_OWNER + "/publicfoldernotrecursive/unsharedfile.dat";
//		for (RemoteSystem system: systems) 
//		{ 
//			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
//		}
//		
//		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, false,	false });
//		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, false, 	false });
//		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
//		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		// public readonly systems and systems where the user is admin should always be readable
//		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		path = SYSTEM_OWNER + "/sharedfolderrecursive";
//		for (RemoteSystem system: systems) 
//		{ 
//			createSharedLogicalFile(SYSTEM_OWNER, PermissionType.READ, sharedUser, system, homeDir + path, LogicalFile.DIRECTORY, true); 
//			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
//		}
//		
//		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, true,	false });
//		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
//		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		// public readonly systems and systems where the user is admin should always be readable
//		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		path = SYSTEM_OWNER + "/sharedfolderrecursive/unsharedfile.dat";
//		for (RemoteSystem system: systems) 
//		{ 
//			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
//		}
//		
//		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, true,	false });
//		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
//		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		// public readonly systems and systems where the user is admin should always be readable
//		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		
//		path = SYSTEM_OWNER + "/sharedfolderrecursive/sharedfile.dat";
//		for (RemoteSystem system: systems) 
//		{ 
//			createSharedLogicalFile(SYSTEM_OWNER, PermissionType.READ, sharedUser, system, homeDir + path, LogicalFile.DIRECTORY, false); 
//			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
//		}
//		
//		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, true,	false });
//		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
//		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		// public readonly systems and systems where the user is admin should always be readable
//		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		
//		path = SYSTEM_OWNER + "/unsharedfolder";
//		for (RemoteSystem system: systems) 
//		{ 
//			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
//		}
//		
//		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, false,	false });
//		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, false, 	false });
//		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
//		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		// public readonly systems and systems where the user is admin should always be readable
//		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		path = SYSTEM_OWNER + "/unsharedfolder/unsharedfile.dat";
//		for (RemoteSystem system: systems) 
//		{ 
//			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
//		}
//		
//		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, false,	false });
//		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, false, 	false });
//		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
//		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		// public readonly systems and systems where the user is admin should always be readable
//		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		
//		path = SYSTEM_OWNER + "/unsharedfolder/sharedfile.dat";
//		for (RemoteSystem system: systems) 
//		{ 
//			createSharedLogicalFile(SYSTEM_OWNER, PermissionType.READ, sharedUser, system, homeDir + path, LogicalFile.DIRECTORY, false); 
//			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
//		}
//		
//		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, true,	false });
//		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
//		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		// public readonly systems and systems where the user is admin should always be readable
//		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		
//		path = SYSTEM_OWNER + "/systemownerpublicshared.dat";
//		for (RemoteSystem system: systems) 
//		{ 
//			createSharedLogicalFile(SYSTEM_OWNER, PermissionType.READ, sharedUser, system, homeDir + path, LogicalFile.DIRECTORY, false); 
//			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
//		}
//		
//		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, true,	false });
//		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
//		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		// public readonly systems and systems where the user is admin should always be readable
//		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		return testList.toArray(new Object[][]{});
//	}
//	
//	@Test(dataProvider="testCanReadPublicDirectoryInUserHomeProvider")
//	public void testCanReadPublicDirectoryInUserHome(RemoteSystem system, String path, String owner, String internalUsername, boolean expectedResult, boolean shouldThrowException)
//	{
//		abstractTestCanRead(system, path, owner, internalUsername, expectedResult, shouldThrowException);
//	}
//	
//	@DataProvider
//	protected Object[][] testCanReadWorldDirectoryInUserHomeProvider() throws Exception
//	{
//		beforeTestData();
//		List<Object[]> testList = new ArrayList<Object[]>();
//		
//		RemoteSystem publicStorageSystem = getPublicSystem(RemoteSystemType.STORAGE);
//		RemoteSystem publicMirroredStorageSystem = getPublicMirroredSystem(RemoteSystemType.STORAGE);
//		RemoteSystem publicGuestStorageSystem = getPublicGuestSystem(RemoteSystemType.STORAGE);
//		RemoteSystem privateStorageSystem = getPrivateSystem(RemoteSystemType.STORAGE);
//		RemoteSystem privateSharedGuestStorageSystem = getPrivateSharedGuestSystem(RemoteSystemType.STORAGE);
//		RemoteSystem privateSharedUserStorageSystem = getPrivateSharedUserSystem(RemoteSystemType.STORAGE);
//		RemoteSystem privateSharedPublisherStorageSystem = getPrivateSharedPublisherSystem(RemoteSystemType.STORAGE);
//		RemoteSystem privateSharedAdminStorageSystem = getPrivateSharedAdminSystem(RemoteSystemType.STORAGE);
//		
//		RemoteSystem[] systems = { 
//				publicStorageSystem, 
//				publicMirroredStorageSystem,
//				publicGuestStorageSystem,
//				privateStorageSystem,
//				privateSharedGuestStorageSystem,
//				privateSharedUserStorageSystem,
//				privateSharedPublisherStorageSystem,
//				privateSharedAdminStorageSystem
//		};
//		
//		String homeDir = getSystemHome(systems[0]);
//		String sharedUser = Settings.WORLD_USER_USERNAME;
//		
//		// system owner shares with self only
//		String path = SYSTEM_OWNER + "/publicfoldernotrecursive";
//		for (RemoteSystem system: systems) 
//		{ 
//			createSharedLogicalFile(SYSTEM_OWNER, PermissionType.READ, sharedUser, system, homeDir + path, LogicalFile.DIRECTORY, false); 
//			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
//		}
//		
//		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, true,	false });
//		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
//		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		// public readonly systems and systems where the user is admin should always be readable
//		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		path = SYSTEM_OWNER + "/publicfoldernotrecursive/sharedfile.dat";
//		for (RemoteSystem system: systems) 
//		{ 
//			createSharedLogicalFile(SYSTEM_OWNER, PermissionType.READ, sharedUser, system, homeDir + path, LogicalFile.DIRECTORY, false); 
//			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
//		}
//		
//		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, true,	false });
//		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
//		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		// public readonly systems and systems where the user is admin should always be readable
//		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		path = SYSTEM_OWNER + "/publicfoldernotrecursive/unsharedfile.dat";
//		for (RemoteSystem system: systems) 
//		{ 
//			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
//		}
//		
//		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, false,	false });
//		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, false, 	false });
//		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
//		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		// public readonly systems and systems where the user is admin should always be readable
//		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		path = SYSTEM_OWNER + "/sharedfolderrecursive";
//		for (RemoteSystem system: systems) 
//		{ 
//			createSharedLogicalFile(SYSTEM_OWNER, PermissionType.READ, sharedUser, system, homeDir + path, LogicalFile.DIRECTORY, true); 
//			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
//		}
//		
//		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, true,	false });
//		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
//		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		// public readonly systems and systems where the user is admin should always be readable
//		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		path = SYSTEM_OWNER + "/sharedfolderrecursive/unsharedfile.dat";
//		for (RemoteSystem system: systems) 
//		{ 
//			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
//		}
//		
//		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, true,	false });
//		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
//		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		// public readonly systems and systems where the user is admin should always be readable
//		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		
//		path = SYSTEM_OWNER + "/sharedfolderrecursive/sharedfile.dat";
//		for (RemoteSystem system: systems) 
//		{ 
//			createSharedLogicalFile(SYSTEM_OWNER, PermissionType.READ, sharedUser, system, homeDir + path, LogicalFile.DIRECTORY, false); 
//			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
//		}
//		
//		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, true,	false });
//		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
//		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		// public readonly systems and systems where the user is admin should always be readable
//		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		
//		path = SYSTEM_OWNER + "/unsharedfolder";
//		for (RemoteSystem system: systems) 
//		{ 
//			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
//		}
//		
//		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, false,	false });
//		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, false, 	false });
//		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
//		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		// public readonly systems and systems where the user is admin should always be readable
//		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		path = SYSTEM_OWNER + "/unsharedfolder/unsharedfile.dat";
//		for (RemoteSystem system: systems) 
//		{ 
//			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
//		}
//		
//		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, false,	false });
//		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, false, 	false });
//		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
//		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		// public readonly systems and systems where the user is admin should always be readable
//		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		
//		path = SYSTEM_OWNER + "/unsharedfolder/sharedfile.dat";
//		for (RemoteSystem system: systems) 
//		{ 
//			createSharedLogicalFile(SYSTEM_OWNER, PermissionType.READ, sharedUser, system, homeDir + path, LogicalFile.DIRECTORY, false); 
//			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
//		}
//		
//		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, true,	false });
//		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
//		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		// public readonly systems and systems where the user is admin should always be readable
//		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		
//		path = SYSTEM_OWNER + "/systemownerpublicshared.dat";
//		for (RemoteSystem system: systems) 
//		{ 
//			createSharedLogicalFile(SYSTEM_OWNER, PermissionType.READ, sharedUser, system, homeDir + path, LogicalFile.DIRECTORY, false); 
//			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
//		}
//		
//		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, true,	false });
//		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
//		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		// public readonly systems and systems where the user is admin should always be readable
//		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
//		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
//		
//		return testList.toArray(new Object[][]{});
//	}
//	
//	@Test(dataProvider="testCanReadWorldDirectoryInUserHomeProvider")
//	public void testCanReadWorldDirectoryInUserHome(RemoteSystem system, String path, String owner, String internalUsername, boolean expectedResult, boolean shouldThrowException)
//	{
//		abstractTestCanRead(system, path, owner, internalUsername, expectedResult, shouldThrowException);
//	}

}
