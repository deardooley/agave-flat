package org.iplantc.service.io.permissions;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.iplantc.service.io.Settings;
import org.iplantc.service.io.model.JSONTestDataUtil;
import org.iplantc.service.io.model.LogicalFile;
import org.iplantc.service.metadata.model.enumerations.PermissionType;
import org.iplantc.service.systems.exceptions.SystemException;
import org.iplantc.service.systems.model.ExecutionSystem;
import org.iplantc.service.systems.model.RemoteSystem;
import org.iplantc.service.systems.model.StorageSystem;
import org.iplantc.service.systems.model.enumerations.RemoteSystemType;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class StoragePermissionManagerWriteTest extends AbstractPermissionManagerTest {

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
	
	/************************************************************************
	/*							 READ TESTS      							*
	/************************************************************************/
	
	@DataProvider
	protected Object[][] testCanWriteRootProvider() throws Exception
	{
		beforeTestData();
		RemoteSystem publicStorageSystem = getPublicSystem(RemoteSystemType.STORAGE);
		RemoteSystem publicMirroredStorageSystem = getPublicMirroredSystem(RemoteSystemType.STORAGE);
		RemoteSystem publicGuestStorageSystem = getPublicGuestSystem(RemoteSystemType.STORAGE);
		RemoteSystem privateStorageSystem = getPrivateSystem(RemoteSystemType.STORAGE);
		RemoteSystem privateSharedGuestStorageSystem = getPrivateSharedGuestSystem(RemoteSystemType.STORAGE);
		RemoteSystem privateSharedUserStorageSystem = getPrivateSharedUserSystem(RemoteSystemType.STORAGE);
		RemoteSystem privateSharedPublisherStorageSystem = getPrivateSharedPublisherSystem(RemoteSystemType.STORAGE);
		RemoteSystem privateSharedAdminStorageSystem = getPrivateSharedAdminSystem(RemoteSystemType.STORAGE);
		
		return new Object[][] {
				{ publicStorageSystem, "/", SYSTEM_OWNER, null, true, false },
				{ publicMirroredStorageSystem, "/", SYSTEM_OWNER, null, true, false },
				{ publicGuestStorageSystem, "/", SYSTEM_OWNER, null, true, false },
				{ privateStorageSystem, "/", SYSTEM_OWNER, null, true, false },
				{ privateSharedGuestStorageSystem, "/", SYSTEM_OWNER, null, true, false },
				{ privateSharedUserStorageSystem, "/", SYSTEM_OWNER, null, true, false },
				{ privateSharedPublisherStorageSystem, "/", SYSTEM_OWNER, null, true, false },
				{ privateSharedAdminStorageSystem, "/", SYSTEM_OWNER, null, true, false },
				
				{ publicStorageSystem, "/", SYSTEM_SHARE_USER, null, false, false },
				{ publicMirroredStorageSystem, "/", SYSTEM_SHARE_USER, null, false, false },
				{ publicGuestStorageSystem, "/", SYSTEM_SHARE_USER, null, false, false },
				{ privateStorageSystem, "/", SYSTEM_SHARE_USER, null, false, false },
				{ privateSharedGuestStorageSystem, "/", SYSTEM_SHARE_USER, null, false, false },
				{ privateSharedUserStorageSystem, "/", SYSTEM_SHARE_USER, null, true, false },
				{ privateSharedPublisherStorageSystem, "/", SYSTEM_SHARE_USER, null, true, false },
				{ privateSharedAdminStorageSystem, "/", SYSTEM_SHARE_USER, null, true, false },
				
				{ publicStorageSystem, "/", SYSTEM_UNSHARED_USER, null, false, false },
				{ publicMirroredStorageSystem, "/", SYSTEM_UNSHARED_USER, null, false, false },
				{ publicGuestStorageSystem, "/", SYSTEM_UNSHARED_USER, null, false, false },
				{ privateStorageSystem, "/", SYSTEM_UNSHARED_USER, null, false, false },
				{ privateSharedGuestStorageSystem, "/", SYSTEM_UNSHARED_USER, null, false, false },
				{ privateSharedUserStorageSystem, "/", SYSTEM_UNSHARED_USER, null, false, false },
				{ privateSharedPublisherStorageSystem, "/", SYSTEM_UNSHARED_USER, null, false, false },
				{ privateSharedAdminStorageSystem, "/", SYSTEM_UNSHARED_USER, null, false, false },
		};
	}
	
	@Test(dataProvider="testCanWriteRootProvider")
	public void testCanWriteRoot(RemoteSystem system, String path, String owner, String internalUsername, boolean expectedResult, boolean shouldThrowException)
	{
		abstractTestCanWrite(system, path, owner, internalUsername, expectedResult, shouldThrowException);
	}
	
	@DataProvider
	protected Object[][] testCanWriteSystemHomeProvider() throws Exception
	{
		beforeTestData();
		RemoteSystem publicStorageSystem = getPublicSystem(RemoteSystemType.STORAGE);
		RemoteSystem publicMirroredStorageSystem = getPublicMirroredSystem(RemoteSystemType.STORAGE);
		RemoteSystem publicGuestStorageSystem = getPublicGuestSystem(RemoteSystemType.STORAGE);
		RemoteSystem privateStorageSystem = getPrivateSystem(RemoteSystemType.STORAGE);
		RemoteSystem privateSharedGuestStorageSystem = getPrivateSharedGuestSystem(RemoteSystemType.STORAGE);
		RemoteSystem privateSharedUserStorageSystem = getPrivateSharedUserSystem(RemoteSystemType.STORAGE);
		RemoteSystem privateSharedPublisherStorageSystem = getPrivateSharedPublisherSystem(RemoteSystemType.STORAGE);
		RemoteSystem privateSharedAdminStorageSystem = getPrivateSharedAdminSystem(RemoteSystemType.STORAGE);
		
		return new Object[][] {
				{ publicStorageSystem, "", SYSTEM_OWNER, null, true, false },
				{ publicMirroredStorageSystem, "", SYSTEM_OWNER, null, true, false },
				{ publicGuestStorageSystem, "", SYSTEM_OWNER, null, true, false },
				{ privateStorageSystem, "", SYSTEM_OWNER, null, true, false },
				{ privateSharedGuestStorageSystem, "", SYSTEM_OWNER, null, true, false },
				{ privateSharedUserStorageSystem, "", SYSTEM_OWNER, null, true, false },
				{ privateSharedPublisherStorageSystem, "", SYSTEM_OWNER, null, true, false },
				{ privateSharedAdminStorageSystem, "", SYSTEM_OWNER, null, true, false },
				
				{ publicStorageSystem, "", SYSTEM_SHARE_USER, null, false, false },
				{ publicMirroredStorageSystem, "", SYSTEM_SHARE_USER, null, false, false },
				{ publicGuestStorageSystem, "", SYSTEM_SHARE_USER, null, false, false },
				{ privateStorageSystem, "", SYSTEM_SHARE_USER, null, false, false },
				{ privateSharedGuestStorageSystem, "", SYSTEM_SHARE_USER, null, false, false },
				{ privateSharedUserStorageSystem, "", SYSTEM_SHARE_USER, null, true, false },
				{ privateSharedPublisherStorageSystem, "", SYSTEM_SHARE_USER, null, true, false },
				{ privateSharedAdminStorageSystem, "", SYSTEM_SHARE_USER, null, true, false },
				
				{ publicStorageSystem, "", SYSTEM_UNSHARED_USER, null, false, false },
				{ publicMirroredStorageSystem, "", SYSTEM_UNSHARED_USER, null, false, false },
				{ publicGuestStorageSystem, "", SYSTEM_UNSHARED_USER, null, false, false },
				{ privateStorageSystem, "", SYSTEM_UNSHARED_USER, null, false, false },
				{ privateSharedGuestStorageSystem, "", SYSTEM_UNSHARED_USER, null, false, false },
				{ privateSharedUserStorageSystem, "", SYSTEM_UNSHARED_USER, null, false, false },
				{ privateSharedPublisherStorageSystem, "", SYSTEM_UNSHARED_USER, null, false, false },
				{ privateSharedAdminStorageSystem, "", SYSTEM_UNSHARED_USER, null, false, false },
		};
	}
	
	@Test(dataProvider="testCanWriteSystemHomeProvider")
	public void testCanWriteImplicitSystemHome(RemoteSystem system, String path, String owner, String internalUsername, boolean expectedResult, boolean shouldThrowException)
	{
		abstractTestCanWrite(system, path, owner, internalUsername, expectedResult, shouldThrowException);
	}
	
	@Test(dataProvider="testCanWriteSystemHomeProvider")
	public void testCanWriteExplicitSystemHome(RemoteSystem system, String path, String owner, String internalUsername, boolean expectedResult, boolean shouldThrowException)
	{
		abstractTestCanWrite(system, system.getStorageConfig().getHomeDir(), owner, internalUsername, expectedResult, shouldThrowException);
	}
	
	@DataProvider
	protected Object[][] testCanWriteUserHomeProvider() throws Exception
	{
		beforeTestData();
		RemoteSystem publicStorageSystem = getPublicSystem(RemoteSystemType.STORAGE);
		RemoteSystem publicMirroredStorageSystem = getPublicMirroredSystem(RemoteSystemType.STORAGE);
		RemoteSystem publicGuestStorageSystem = getPublicGuestSystem(RemoteSystemType.STORAGE);
		RemoteSystem privateStorageSystem = getPrivateSystem(RemoteSystemType.STORAGE);
		RemoteSystem privateSharedGuestStorageSystem = getPrivateSharedGuestSystem(RemoteSystemType.STORAGE);
		RemoteSystem privateSharedUserStorageSystem = getPrivateSharedUserSystem(RemoteSystemType.STORAGE);
		RemoteSystem privateSharedPublisherStorageSystem = getPrivateSharedPublisherSystem(RemoteSystemType.STORAGE);
		RemoteSystem privateSharedAdminStorageSystem = getPrivateSharedAdminSystem(RemoteSystemType.STORAGE);
		
		return new Object[][] {
				{ publicStorageSystem, SYSTEM_OWNER, SYSTEM_OWNER, null, true, false },
				{ publicMirroredStorageSystem, SYSTEM_OWNER, SYSTEM_OWNER, null, true, false },
				{ publicGuestStorageSystem, SYSTEM_OWNER, SYSTEM_OWNER, null, true, false },
				{ privateStorageSystem, SYSTEM_OWNER, SYSTEM_OWNER, null, true, false },
				{ privateSharedGuestStorageSystem, SYSTEM_OWNER, SYSTEM_OWNER, null, true, false },
				{ privateSharedUserStorageSystem, SYSTEM_OWNER, SYSTEM_OWNER, null, true, false },
				{ privateSharedPublisherStorageSystem, SYSTEM_OWNER, SYSTEM_OWNER, null, true, false },
				{ privateSharedAdminStorageSystem, SYSTEM_OWNER, SYSTEM_OWNER, null, true, false },
				
				{ publicStorageSystem, SYSTEM_OWNER, SYSTEM_SHARE_USER, null, false, false },
				{ publicMirroredStorageSystem, SYSTEM_OWNER, SYSTEM_SHARE_USER, null, false, false },
				{ publicGuestStorageSystem, SYSTEM_OWNER, SYSTEM_SHARE_USER, null, false, false },
				{ privateStorageSystem, SYSTEM_OWNER, SYSTEM_SHARE_USER, null, false, false },
				{ privateSharedGuestStorageSystem, SYSTEM_OWNER, SYSTEM_SHARE_USER, null, false, false },
				{ privateSharedUserStorageSystem, SYSTEM_OWNER, SYSTEM_SHARE_USER, null, true, false },
				{ privateSharedPublisherStorageSystem, SYSTEM_OWNER, SYSTEM_SHARE_USER, null, true, false },
				{ privateSharedAdminStorageSystem, SYSTEM_OWNER, SYSTEM_SHARE_USER, null, true, false },
				
				{ publicStorageSystem, SYSTEM_SHARE_USER, SYSTEM_SHARE_USER, null, true, false },
				{ publicMirroredStorageSystem, SYSTEM_SHARE_USER, SYSTEM_SHARE_USER, null, true, false },
				{ publicGuestStorageSystem, SYSTEM_SHARE_USER, SYSTEM_SHARE_USER, null, false, false },
				{ privateStorageSystem, SYSTEM_SHARE_USER, SYSTEM_SHARE_USER, null, false, false },
				{ privateSharedGuestStorageSystem, SYSTEM_SHARE_USER, SYSTEM_SHARE_USER, null, false, false },
				{ privateSharedUserStorageSystem, SYSTEM_SHARE_USER, SYSTEM_SHARE_USER, null, true, false },
				{ privateSharedPublisherStorageSystem, SYSTEM_SHARE_USER, SYSTEM_SHARE_USER, null, true, false },
				{ privateSharedAdminStorageSystem, SYSTEM_SHARE_USER, SYSTEM_SHARE_USER, null, true, false },
				
				{ publicStorageSystem, SYSTEM_UNSHARED_USER, SYSTEM_SHARE_USER, null, false, false },
				{ publicMirroredStorageSystem, SYSTEM_UNSHARED_USER, SYSTEM_SHARE_USER, null, false, false },
				{ publicGuestStorageSystem, SYSTEM_UNSHARED_USER, SYSTEM_SHARE_USER, null, false, false },
				{ privateStorageSystem, SYSTEM_UNSHARED_USER, SYSTEM_SHARE_USER, null, false, false },
				{ privateSharedGuestStorageSystem, SYSTEM_UNSHARED_USER, SYSTEM_SHARE_USER, null, false, false },
				{ privateSharedUserStorageSystem, SYSTEM_UNSHARED_USER, SYSTEM_SHARE_USER, null, true, false },
				{ privateSharedPublisherStorageSystem, SYSTEM_UNSHARED_USER, SYSTEM_SHARE_USER, null, true, false },
				{ privateSharedAdminStorageSystem, SYSTEM_UNSHARED_USER, SYSTEM_SHARE_USER, null, true, false },
				
				{ publicStorageSystem, SYSTEM_OWNER, SYSTEM_UNSHARED_USER, null, false, false },
				{ publicMirroredStorageSystem, SYSTEM_OWNER, SYSTEM_UNSHARED_USER, null, false, false },
				{ publicGuestStorageSystem, SYSTEM_OWNER, SYSTEM_UNSHARED_USER, null, false, false },
				{ privateStorageSystem, SYSTEM_OWNER, SYSTEM_UNSHARED_USER, null, false, false },
				{ privateSharedGuestStorageSystem, SYSTEM_OWNER, SYSTEM_UNSHARED_USER, null, false, false },
				{ privateSharedUserStorageSystem, SYSTEM_OWNER, SYSTEM_UNSHARED_USER, null, false, false },
				{ privateSharedPublisherStorageSystem, SYSTEM_OWNER, SYSTEM_UNSHARED_USER, null, false, false },
				{ privateSharedAdminStorageSystem, SYSTEM_OWNER, SYSTEM_UNSHARED_USER, null, false, false },
				
				{ publicStorageSystem, SYSTEM_SHARE_USER, SYSTEM_UNSHARED_USER, null, false, false },
				{ publicMirroredStorageSystem, SYSTEM_SHARE_USER, SYSTEM_UNSHARED_USER, null, false, false },
				{ publicGuestStorageSystem, SYSTEM_SHARE_USER, SYSTEM_UNSHARED_USER, null, false, false },
				{ privateStorageSystem, SYSTEM_SHARE_USER, SYSTEM_UNSHARED_USER, null, false, false },
				{ privateSharedGuestStorageSystem, SYSTEM_SHARE_USER, SYSTEM_UNSHARED_USER, null, false, false },
				{ privateSharedUserStorageSystem, SYSTEM_SHARE_USER, SYSTEM_UNSHARED_USER, null, false, false },
				{ privateSharedPublisherStorageSystem, SYSTEM_SHARE_USER, SYSTEM_UNSHARED_USER, null, false, false },
				{ privateSharedAdminStorageSystem, SYSTEM_SHARE_USER, SYSTEM_UNSHARED_USER, null, false, false },
				
				{ publicStorageSystem, SYSTEM_UNSHARED_USER, SYSTEM_UNSHARED_USER, null, true, false },
				{ publicMirroredStorageSystem, SYSTEM_UNSHARED_USER, SYSTEM_UNSHARED_USER, null, true, false },
				{ publicGuestStorageSystem, SYSTEM_UNSHARED_USER, SYSTEM_UNSHARED_USER, null, false, false },
				{ privateStorageSystem, SYSTEM_UNSHARED_USER, SYSTEM_UNSHARED_USER, null, false, false },
				{ privateSharedGuestStorageSystem, SYSTEM_UNSHARED_USER, SYSTEM_UNSHARED_USER, null, false, false },
				{ privateSharedUserStorageSystem, SYSTEM_UNSHARED_USER, SYSTEM_UNSHARED_USER, null, false, false },
				{ privateSharedPublisherStorageSystem, SYSTEM_UNSHARED_USER, SYSTEM_UNSHARED_USER, null, false, false },
				{ privateSharedAdminStorageSystem, SYSTEM_UNSHARED_USER, SYSTEM_UNSHARED_USER, null, false, false },
		};
	}
	
	@Test(dataProvider="testCanWriteUserHomeProvider")
	public void testCanWriteImplicitUserHome(RemoteSystem system, String path, String owner, String internalUsername, boolean expectedResult, boolean shouldThrowException)
	{
		abstractTestCanWrite(system, path, owner, internalUsername, expectedResult, shouldThrowException);
	}
	
	@Test(dataProvider="testCanWriteUserHomeProvider")
	public void testCanWriteExplicitUserHome(RemoteSystem system, String path, String owner, String internalUsername, boolean expectedResult, boolean shouldThrowException)
	{
		abstractTestCanWrite(system, system.getStorageConfig().getHomeDir() + "/" + path, owner, internalUsername, expectedResult, shouldThrowException);
	}
	
	@DataProvider
	protected Object[][] testCanWriteUnSharedDataProvider() throws Exception
	{
		beforeTestData();
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
		
		// system owner shares with self only
		String path = "/unknownfolder";
		for (RemoteSystem system: systems) 
		{ 
			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
		}
		
		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, false,	false });
		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
		
		// public readonly systems and systems where the user is admin should always be readable
		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		
		path = "/unknownfolder/shelfshared.dat";
		for (RemoteSystem system: systems) 
		{ 
			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
		}
		
		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, false,	false });
		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
		
		// public readonly systems and systems where the user is admin should always be readable
		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		
		path = "/unknownfile.dat";
		for (RemoteSystem system: systems) 
		{ 
			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
		}
		
		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, false,	false });
		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
		
		// public readonly systems and systems where the user is admin should always be readable
		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		
		return testList.toArray(new Object[][]{});
	}
	
	@Test(dataProvider="testCanWriteUnSharedDataProvider")
	public void testCanWriteUnSharedDirectory(RemoteSystem system, String path, String owner, String internalUsername, boolean expectedResult, boolean shouldThrowException)
	{
		abstractTestCanWrite(system, path, owner, internalUsername, expectedResult, shouldThrowException);
	}
	
	@DataProvider
	protected Object[][] testCanWriteUnSharedDataInUserHomeProvider() throws Exception
	{
		beforeTestData();
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
		
		// system owner shares with self only
		String path = SYSTEM_OWNER + "/unknownfolder";
		for (RemoteSystem system: systems) 
		{ 
			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
		}
		
		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, false,	false });
		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
		
		// public readonly systems and systems where the user is admin should always be readable
		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		
		path = SYSTEM_OWNER + "/unknownfolder/shelfshared.dat";
		for (RemoteSystem system: systems) 
		{ 
			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
		}
		
		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, false,	false });
		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
		
		// public readonly systems and systems where the user is admin should always be readable
		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		
		path = SYSTEM_OWNER + "/unknownfile.dat";
		for (RemoteSystem system: systems) 
		{ 
			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
		}
		
		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, false,	false });
		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
		
		// public readonly systems and systems where the user is admin should always be readable
		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
	
		path = SYSTEM_OWNER + "/some/deep/path/to/unknownfile.dat";
		for (RemoteSystem system: systems) 
		{ 
			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
		}
		
		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, false,	false });
		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
		
		// public readonly systems and systems where the user is admin should always be readable
		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		
		return testList.toArray(new Object[][]{});
	}
	
	@Test(dataProvider="testCanWriteUnSharedDataInUserHomeProvider")
	public void testCanWriteUnSharedDataInUserHome(RemoteSystem system, String path, String owner, String internalUsername, boolean expectedResult, boolean shouldThrowException)
	{
		abstractTestCanWrite(system, path, owner, internalUsername, expectedResult, shouldThrowException);
	}
	
	@DataProvider
	protected Object[][] testCanWriteUnSharedDataInOwnHomeProvider() throws Exception
	{
		beforeTestData();
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
		
		// system owner shares with self only
		String path = SYSTEM_SHARE_USER + "/unknownfolder";
		for (RemoteSystem system: systems) 
		{ 
			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
		}
		
		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, true,	false });
		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
		
		// public readonly systems and systems where the user is admin should always be readable
		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		
		path = SYSTEM_SHARE_USER + "/unknownfolder/shelfshared.dat";
		for (RemoteSystem system: systems) 
		{ 
			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
		}
		
		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, true,	false });
		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
		
		// public readonly systems and systems where the user is admin should always be readable
		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		
		path = SYSTEM_SHARE_USER + "/unknownfile.dat";
		for (RemoteSystem system: systems) 
		{ 
			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
		}
		
		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, true,	false });
		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
		
		// public readonly systems and systems where the user is admin should always be readable
		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		
		path = SYSTEM_SHARE_USER + "/some/deep/path/to/unknownfile.dat";
		for (RemoteSystem system: systems) 
		{ 
			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
		}
		
		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, true,	false });
		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
		
		// public readonly systems and systems where the user is admin should always be readable
		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		
		return testList.toArray(new Object[][]{});
	}
	
	@Test(dataProvider="testCanWriteUnSharedDataInOwnHomeProvider")
	public void testCanWriteUnSharedDataInOwnHome(RemoteSystem system, String path, String owner, String internalUsername, boolean expectedResult, boolean shouldThrowException)
	{
		abstractTestCanWrite(system, path, owner, internalUsername, expectedResult, shouldThrowException);
	}
	
	@DataProvider
	protected Object[][] testCanWriteDataSharedWithSelfProvider() throws Exception
	{
		beforeTestData();
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
		
		String rootDir = StringUtils.substring(getSystemRoot(systems[0]), 0, -1);
		
		// system owner shares with self only
		String path = "/systemownerselfsharednotrecursive";
		for (RemoteSystem system: systems) 
		{ 
			createSharedLogicalFile(SYSTEM_OWNER, PermissionType.WRITE, SYSTEM_OWNER, system, rootDir + path, LogicalFile.DIRECTORY, false); 
			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
		}
		
		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, false,	false });
		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
		
		// public readonly systems and systems where the user is admin should always be readable
		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		
		path = "/systemownerselfsharednotrecursive/systemownershelfshared.dat";
		for (RemoteSystem system: systems) 
		{ 
			createSharedLogicalFile(SYSTEM_OWNER, PermissionType.WRITE, SYSTEM_OWNER, system, rootDir + path, LogicalFile.DIRECTORY, false); 
			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
		}
		
		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, false,	false });
		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
		
		// public readonly systems and systems where the user is admin should always be readable
		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		
		path = rootDir + "systemownershelfshared.dat";
		for (RemoteSystem system: systems) 
		{ 
			createSharedLogicalFile(SYSTEM_OWNER, PermissionType.WRITE, SYSTEM_OWNER, system, rootDir + path, LogicalFile.DIRECTORY, false); 
			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
		}
		
		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, false,	false });
		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
		
		// public readonly systems and systems where the user is admin should always be readable
		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		
		return testList.toArray(new Object[][]{});
	}
	
	@Test(dataProvider="testCanWriteDataSharedWithSelfProvider")
	public void testCanWriteDataSharedWithSelf(RemoteSystem system, String path, String owner, String internalUsername, boolean expectedResult, boolean shouldThrowException)
	{
		abstractTestCanWrite(system, path, owner, internalUsername, expectedResult, shouldThrowException);
	}
	
	@DataProvider
	protected Object[][] testCanWriteDataSharedWithUserProvider() throws Exception
	{
		beforeTestData();
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
		
		String rootDir = StringUtils.substring(getSystemRoot(systems[0]), 0, -1);
		
		// system owner shares with self only
		String path = "/sharedfoldernotrecursive";
		for (RemoteSystem system: systems) 
		{ 
			createSharedLogicalFile(SYSTEM_OWNER, PermissionType.WRITE, SYSTEM_SHARE_USER, system, rootDir + path, LogicalFile.DIRECTORY, false); 
			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
		}
		
		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, true,	false });
		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
		
		// public readonly systems and systems where the user is admin should always be readable
		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		
		path = "/sharedfoldernotrecursive/sharedfile.dat";
		for (RemoteSystem system: systems) 
		{ 
			createSharedLogicalFile(SYSTEM_OWNER, PermissionType.WRITE, SYSTEM_SHARE_USER, system, rootDir + path, LogicalFile.DIRECTORY, false); 
			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
		}
		
		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, true,	false });
		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
		
		// public readonly systems and systems where the user is admin should always be readable
		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		
		path = "/sharedfoldernotrecursive/unsharedfile.dat";
		for (RemoteSystem system: systems) 
		{ 
			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
		}
		
		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, false,	false });
		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
		
		// public readonly systems and systems where the user is admin should always be readable
		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		
		path = "/sharedfolderrecursive";
		for (RemoteSystem system: systems) 
		{ 
			createSharedLogicalFile(SYSTEM_OWNER, PermissionType.WRITE, SYSTEM_SHARE_USER, system, rootDir + path, LogicalFile.DIRECTORY, true); 
			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
		}
		
		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, true,	false });
		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
		
		// public readonly systems and systems where the user is admin should always be readable
		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		
		path = "/sharedfolderrecursive/unsharedfile.dat";
		for (RemoteSystem system: systems) 
		{ 
			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
		}
		
		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, true,	false });
		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
		
		// public readonly systems and systems where the user is admin should always be readable
		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		
		
		path = "/sharedfolderrecursive/sharedfile.dat";
		for (RemoteSystem system: systems) 
		{ 
			createSharedLogicalFile(SYSTEM_OWNER, PermissionType.WRITE, SYSTEM_SHARE_USER, system, rootDir + path, LogicalFile.DIRECTORY, false); 
			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
		}
		
		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, true,	false });
		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
		
		// public readonly systems and systems where the user is admin should always be readable
		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		
		
		path = "/unsharedfolder";
		for (RemoteSystem system: systems) 
		{ 
			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
		}
		
		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, false,	false });
		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
		
		// public readonly systems and systems where the user is admin should always be readable
		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		
		path = "/unsharedfolder/unsharedfile.dat";
		for (RemoteSystem system: systems) 
		{ 
			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
		}
		
		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, false,	false });
		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
		
		// public readonly systems and systems where the user is admin should always be readable
		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		
		
		path = "/unsharedfolder/sharedfile.dat";
		for (RemoteSystem system: systems) 
		{ 
			createSharedLogicalFile(SYSTEM_OWNER, PermissionType.WRITE, SYSTEM_SHARE_USER, system, rootDir + path, LogicalFile.DIRECTORY, false); 
			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
		}
		
		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, true,	false });
		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
		
		// public readonly systems and systems where the user is admin should always be readable
		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		
		
		path = "/systemownershared.dat";
		for (RemoteSystem system: systems) 
		{ 
			createSharedLogicalFile(SYSTEM_OWNER, PermissionType.WRITE, SYSTEM_SHARE_USER, system, rootDir + path, LogicalFile.DIRECTORY, false); 
			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
		}
		
		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, true,	false });
		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
		
		// public readonly systems and systems where the user is admin should always be readable
		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		
		return testList.toArray(new Object[][]{});
	}
	
	@Test(dataProvider="testCanWriteDataSharedWithUserProvider")
	public void testCanWriteDataSharedWithUser(RemoteSystem system, String path, String owner, String internalUsername, boolean expectedResult, boolean shouldThrowException)
	{
		abstractTestCanWrite(system, path, owner, internalUsername, expectedResult, shouldThrowException);
	}
	
	@DataProvider
	protected Object[][] testCanWriteHomeDirectorySharedWithUserProvider() throws Exception
	{
		beforeTestData();
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
		
		String homeDir = getSystemHome(systems[0]);
		
		String path = SYSTEM_OWNER;
		for (RemoteSystem system: systems) 
		{ 
			createSharedLogicalFile(SYSTEM_OWNER, PermissionType.WRITE, SYSTEM_SHARE_USER, system, homeDir + path, LogicalFile.DIRECTORY, true); 
			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
		}
		
		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, true,	false });
		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
		
		// public readonly systems and systems where the user is admin should always be readable
		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		
		path = SYSTEM_OWNER + "/someunsharedfile.txt";
		for (RemoteSystem system: systems) 
		{ 
			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
		}
		
		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, true,	false });
		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
		
		// public readonly systems and systems where the user is admin should always be readable
		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		
		return testList.toArray(new Object[][]{});
	}
	
	@Test(dataProvider="testCanWriteHomeDirectorySharedWithUserProvider")
	public void testCanWriteHomeDirectorySharedWithUser(RemoteSystem system, String path, String owner, String internalUsername, boolean expectedResult, boolean shouldThrowException)
	{
		abstractTestCanWrite(system, path, owner, internalUsername, expectedResult, shouldThrowException);
	}
	
	@DataProvider
	protected Object[][] testCanWriteHomeDirectoryDataSharedWithUserProvider() throws Exception
	{
		beforeTestData();
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
		
		String homeDir = getSystemHome(systems[0]);
		
		// system owner shares with self only
		String path = SYSTEM_OWNER + "/sharedfoldernotrecursive";
		for (RemoteSystem system: systems) 
		{ 
			createSharedLogicalFile(SYSTEM_OWNER, PermissionType.WRITE, SYSTEM_SHARE_USER, system, homeDir + path, LogicalFile.DIRECTORY, false); 
			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
		}
		
		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, true,	false });
		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
		
		// public readonly systems and systems where the user is admin should always be readable
		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		
		path = SYSTEM_OWNER + "/sharedfoldernotrecursive/sharedfile.dat";
		for (RemoteSystem system: systems) 
		{ 
			createSharedLogicalFile(SYSTEM_OWNER, PermissionType.WRITE, SYSTEM_SHARE_USER, system, homeDir + path, LogicalFile.DIRECTORY, false); 
			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
		}
		
		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, true,	false });
		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
		
		// public readonly systems and systems where the user is admin should always be readable
		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		
		path = SYSTEM_OWNER + "/sharedfoldernotrecursive/unsharedfile.dat";
		for (RemoteSystem system: systems) 
		{ 
			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
		}
		
		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, false,	false });
		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
		
		// public readonly systems and systems where the user is admin should always be readable
		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		
		path = SYSTEM_OWNER + "/sharedfolderrecursive";
		for (RemoteSystem system: systems) 
		{ 
			createSharedLogicalFile(SYSTEM_OWNER, PermissionType.WRITE, SYSTEM_SHARE_USER, system, homeDir + path, LogicalFile.DIRECTORY, true); 
			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
		}
		
		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, true,	false });
		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
		
		// public readonly systems and systems where the user is admin should always be readable
		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		
		path = SYSTEM_OWNER + "/sharedfolderrecursive/unsharedfile.dat";
		for (RemoteSystem system: systems) 
		{ 
			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
		}
		
		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, true,	false });
		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
		
		// public readonly systems and systems where the user is admin should always be readable
		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		
		
		path = SYSTEM_OWNER + "/sharedfolderrecursive/sharedfile.dat";
		for (RemoteSystem system: systems) 
		{ 
			createSharedLogicalFile(SYSTEM_OWNER, PermissionType.WRITE, SYSTEM_SHARE_USER, system, homeDir + path, LogicalFile.DIRECTORY, false); 
			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
		}
		
		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, true,	false });
		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
		
		// public readonly systems and systems where the user is admin should always be readable
		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		
		
		path = SYSTEM_OWNER + "/unsharedfolder";
		for (RemoteSystem system: systems) 
		{ 
			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
		}
		
		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, false,	false });
		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
		
		// public readonly systems and systems where the user is admin should always be readable
		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		
		path = SYSTEM_OWNER + "/unsharedfolder/unsharedfile.dat";
		for (RemoteSystem system: systems) 
		{ 
			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
		}
		
		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, false,	false });
		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
		
		// public readonly systems and systems where the user is admin should always be readable
		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		
		
		path = SYSTEM_OWNER + "/unsharedfolder/sharedfile.dat";
		for (RemoteSystem system: systems) 
		{ 
			createSharedLogicalFile(SYSTEM_OWNER, PermissionType.WRITE, SYSTEM_SHARE_USER, system, homeDir + path, LogicalFile.DIRECTORY, false); 
			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
		}
		
		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, true,	false });
		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
		
		// public readonly systems and systems where the user is admin should always be readable
		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		
		
		path = SYSTEM_OWNER + "/systemownershelfshared.dat";
		for (RemoteSystem system: systems) 
		{ 
			createSharedLogicalFile(SYSTEM_OWNER, PermissionType.WRITE, SYSTEM_SHARE_USER, system, homeDir + path, LogicalFile.DIRECTORY, false); 
			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
		}
		
		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, true,	false });
		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
		
		// public readonly systems and systems where the user is admin should always be readable
		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		
		return testList.toArray(new Object[][]{});
	}
	
	@Test(dataProvider="testCanWriteHomeDirectoryDataSharedWithUserProvider")
	public void testCanWriteHomeDirectoryDataSharedWithUser(RemoteSystem system, String path, String owner, String internalUsername, boolean expectedResult, boolean shouldThrowException)
	{
		abstractTestCanWrite(system, path, owner, internalUsername, expectedResult, shouldThrowException);
	}
	
	@DataProvider
	protected Object[][] testCanWritePublicDirectoryInRootDirectoryProvider() throws Exception
	{
		beforeTestData();
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
		
		String rootDir = getSystemRoot(systems[0]);
		String sharedUser = Settings.PUBLIC_USER_USERNAME;
		
		// system owner shares with self only
		String path = "/publicfoldernotrecursive";
		for (RemoteSystem system: systems) 
		{ 
			createSharedLogicalFile(SYSTEM_OWNER, PermissionType.WRITE, sharedUser, system, rootDir + path, LogicalFile.DIRECTORY, false); 
			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
		}
		
		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, true,	false });
		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
		
		// public readonly systems and systems where the user is admin should always be readable
		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		
		path = "/publicfoldernotrecursive/sharedfile.dat";
		for (RemoteSystem system: systems) 
		{ 
			createSharedLogicalFile(SYSTEM_OWNER, PermissionType.WRITE, sharedUser, system, rootDir + path, LogicalFile.DIRECTORY, false); 
			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
		}
		
		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, true,	false });
		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
		
		// public readonly systems and systems where the user is admin should always be readable
		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		
		path = "/publicfoldernotrecursive/unsharedfile.dat";
		for (RemoteSystem system: systems) 
		{ 
			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
		}
		
		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, false,	false });
		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
		
		// public readonly systems and systems where the user is admin should always be readable
		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		
		path = "/sharedfolderrecursive";
		for (RemoteSystem system: systems) 
		{ 
			createSharedLogicalFile(SYSTEM_OWNER, PermissionType.WRITE, sharedUser, system, rootDir + path, LogicalFile.DIRECTORY, true); 
			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
		}
		
		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, true,	false });
		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
		
		// public readonly systems and systems where the user is admin should always be readable
		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		
		path = "/sharedfolderrecursive/unsharedfile.dat";
		for (RemoteSystem system: systems) 
		{ 
			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
		}
		
		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, true,	false });
		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
		
		// public readonly systems and systems where the user is admin should always be readable
		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		
		
		path = "/sharedfolderrecursive/sharedfile.dat";
		for (RemoteSystem system: systems) 
		{ 
			createSharedLogicalFile(SYSTEM_OWNER, PermissionType.WRITE, sharedUser, system, rootDir + path, LogicalFile.DIRECTORY, false); 
			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
		}
		
		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, true,	false });
		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
		
		// public readonly systems and systems where the user is admin should always be readable
		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		
		
		path = "/unsharedfolder";
		for (RemoteSystem system: systems) 
		{ 
			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
		}
		
		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, false,	false });
		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
		
		// public readonly systems and systems where the user is admin should always be readable
		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		
		path = "/unsharedfolder/unsharedfile.dat";
		for (RemoteSystem system: systems) 
		{ 
			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
		}
		
		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, false,	false });
		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
		
		// public readonly systems and systems where the user is admin should always be readable
		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		
		
		path = "/unsharedfolder/sharedfile.dat";
		for (RemoteSystem system: systems) 
		{ 
			createSharedLogicalFile(SYSTEM_OWNER, PermissionType.WRITE, sharedUser, system, rootDir + path, LogicalFile.DIRECTORY, false); 
			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
		}
		
		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, true,	false });
		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
		
		// public readonly systems and systems where the user is admin should always be readable
		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		
		
		path = "/systemownerpublicshared.dat";
		for (RemoteSystem system: systems) 
		{ 
			createSharedLogicalFile(SYSTEM_OWNER, PermissionType.WRITE, sharedUser, system, rootDir + path, LogicalFile.DIRECTORY, false); 
			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
		}
		
		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, true,	false });
		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
		
		// public readonly systems and systems where the user is admin should always be readable
		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		
		return testList.toArray(new Object[][]{});
	}
	
	@Test(dataProvider="testCanWritePublicDirectoryInRootDirectoryProvider")
	public void testCanWritePublicDirectoryInRootDirectory(RemoteSystem system, String path, String owner, String internalUsername, boolean expectedResult, boolean shouldThrowException)
	{
		abstractTestCanWrite(system, path, owner, internalUsername, expectedResult, shouldThrowException);
	}
	
	@DataProvider
	protected Object[][] testCanWriteWorldDirectoryInRootDirectoryProvider() throws Exception
	{
		beforeTestData();
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
		
		String rootDir = getSystemRoot(systems[0]);
		String sharedUser = Settings.WORLD_USER_USERNAME;
		
		// system owner shares with self only
		String path = "/publicfoldernotrecursive";
		for (RemoteSystem system: systems) 
		{ 
			createSharedLogicalFile(SYSTEM_OWNER, PermissionType.WRITE, sharedUser, system, rootDir + path, LogicalFile.DIRECTORY, false); 
			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
		}
		
		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, true,	false });
		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
		
		// public readonly systems and systems where the user is admin should always be readable
		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		
		path = "/publicfoldernotrecursive/sharedfile.dat";
		for (RemoteSystem system: systems) 
		{ 
			createSharedLogicalFile(SYSTEM_OWNER, PermissionType.WRITE, sharedUser, system, rootDir + path, LogicalFile.DIRECTORY, false); 
			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
		}
		
		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, true,	false });
		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
		
		// public readonly systems and systems where the user is admin should always be readable
		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		
		path = "/publicfoldernotrecursive/unsharedfile.dat";
		for (RemoteSystem system: systems) 
		{ 
			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
		}
		
		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, false,	false });
		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
		
		// public readonly systems and systems where the user is admin should always be readable
		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		
		path = "/sharedfolderrecursive";
		for (RemoteSystem system: systems) 
		{ 
			createSharedLogicalFile(SYSTEM_OWNER, PermissionType.WRITE, sharedUser, system, rootDir + path, LogicalFile.DIRECTORY, true); 
			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
		}
		
		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, true,	false });
		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
		
		// public readonly systems and systems where the user is admin should always be readable
		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		
		path = "/sharedfolderrecursive/unsharedfile.dat";
		for (RemoteSystem system: systems) 
		{ 
			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
		}
		
		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, true,	false });
		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
		
		// public readonly systems and systems where the user is admin should always be readable
		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		
		
		path = "/sharedfolderrecursive/sharedfile.dat";
		for (RemoteSystem system: systems) 
		{ 
			createSharedLogicalFile(SYSTEM_OWNER, PermissionType.WRITE, sharedUser, system, rootDir + path, LogicalFile.DIRECTORY, false); 
			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
		}
		
		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, true,	false });
		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
		
		// public readonly systems and systems where the user is admin should always be readable
		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		
		
		path = "/unsharedfolder";
		for (RemoteSystem system: systems) 
		{ 
			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
		}
		
		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, false,	false });
		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
		
		// public readonly systems and systems where the user is admin should always be readable
		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		
		path = "/unsharedfolder/unsharedfile.dat";
		for (RemoteSystem system: systems) 
		{ 
			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
		}
		
		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, false,	false });
		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
		
		// public readonly systems and systems where the user is admin should always be readable
		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		
		
		path = "/unsharedfolder/sharedfile.dat";
		for (RemoteSystem system: systems) 
		{ 
			createSharedLogicalFile(SYSTEM_OWNER, PermissionType.WRITE, sharedUser, system, rootDir + path, LogicalFile.DIRECTORY, false); 
			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
		}
		
		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, true,	false });
		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
		
		// public readonly systems and systems where the user is admin should always be readable
		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		
		
		path = "/systemownerpublicshared.dat";
		for (RemoteSystem system: systems) 
		{ 
			createSharedLogicalFile(SYSTEM_OWNER, PermissionType.WRITE, sharedUser, system, rootDir + path, LogicalFile.DIRECTORY, false); 
			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
		}
		
		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, true,	false });
		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
		
		// public readonly systems and systems where the user is admin should always be readable
		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		
		return testList.toArray(new Object[][]{});
	}
	
	@Test(dataProvider="testCanWriteWorldDirectoryInRootDirectoryProvider")
	public void testCanWriteWorldDirectoryInRootDirectory(RemoteSystem system, String path, String owner, String internalUsername, boolean expectedResult, boolean shouldThrowException)
	{
		abstractTestCanWrite(system, path, owner, internalUsername, expectedResult, shouldThrowException);
	}
	
	@DataProvider
	protected Object[][] testCanWritePublicDirectoryInUserHomeProvider() throws Exception
	{
		beforeTestData();
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
		
		String homeDir = getSystemHome(systems[0]);
		String sharedUser = Settings.PUBLIC_USER_USERNAME;
		
		// system owner shares with public user
		String path = SYSTEM_OWNER + "/publicfoldernotrecursive";
		for (RemoteSystem system: systems) 
		{ 
			createSharedLogicalFile(SYSTEM_OWNER, PermissionType.WRITE, sharedUser, system, homeDir + path, LogicalFile.DIRECTORY, false); 
			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
		}
		
		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, true,	false });
		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
		
		// public readonly systems and systems where the user is admin should always be readable
		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		
		path = SYSTEM_OWNER + "/publicfoldernotrecursive/sharedfile.dat";
		for (RemoteSystem system: systems) 
		{ 
			createSharedLogicalFile(SYSTEM_OWNER, PermissionType.WRITE, sharedUser, system, homeDir + path, LogicalFile.DIRECTORY, false); 
			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
		}
		
		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, true,	false });
		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
		
		// public readonly systems and systems where the user is admin should always be readable
		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		
		path = SYSTEM_OWNER + "/publicfoldernotrecursive/unsharedfile.dat";
		for (RemoteSystem system: systems) 
		{ 
			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
		}
		
		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, false,	false });
		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
		
		// public readonly systems and systems where the user is admin should always be readable
		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		
		path = SYSTEM_OWNER + "/sharedfolderrecursive";
		for (RemoteSystem system: systems) 
		{ 
			createSharedLogicalFile(SYSTEM_OWNER, PermissionType.WRITE, sharedUser, system, homeDir + path, LogicalFile.DIRECTORY, true); 
			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
		}
		
		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, true,	false });
		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
		
		// public readonly systems and systems where the user is admin should always be readable
		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		
		path = SYSTEM_OWNER + "/sharedfolderrecursive/unsharedfile.dat";
		for (RemoteSystem system: systems) 
		{ 
			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
		}
		
		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, true,	false });
		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
		
		// public readonly systems and systems where the user is admin should always be readable
		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		
		
		path = SYSTEM_OWNER + "/sharedfolderrecursive/sharedfile.dat";
		for (RemoteSystem system: systems) 
		{ 
			createSharedLogicalFile(SYSTEM_OWNER, PermissionType.WRITE, sharedUser, system, homeDir + path, LogicalFile.DIRECTORY, false); 
			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
		}
		
		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, true,	false });
		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
		
		// public readonly systems and systems where the user is admin should always be readable
		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		
		
		path = SYSTEM_OWNER + "/unsharedfolder";
		for (RemoteSystem system: systems) 
		{ 
			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
		}
		
		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, false,	false });
		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
		
		// public readonly systems and systems where the user is admin should always be readable
		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		
		path = SYSTEM_OWNER + "/unsharedfolder/unsharedfile.dat";
		for (RemoteSystem system: systems) 
		{ 
			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
		}
		
		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, false,	false });
		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
		
		// public readonly systems and systems where the user is admin should always be readable
		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		
		
		path = SYSTEM_OWNER + "/unsharedfolder/sharedfile.dat";
		for (RemoteSystem system: systems) 
		{ 
			createSharedLogicalFile(SYSTEM_OWNER, PermissionType.WRITE, sharedUser, system, homeDir + path, LogicalFile.DIRECTORY, false); 
			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
		}
		
		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, true,	false });
		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
		
		// public readonly systems and systems where the user is admin should always be readable
		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		
		
		path = SYSTEM_OWNER + "/systemownerpublicshared.dat";
		for (RemoteSystem system: systems) 
		{ 
			createSharedLogicalFile(SYSTEM_OWNER, PermissionType.WRITE, sharedUser, system, homeDir + path, LogicalFile.DIRECTORY, false); 
			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
		}
		
		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, true,	false });
		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
		
		// public readonly systems and systems where the user is admin should always be readable
		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		
		return testList.toArray(new Object[][]{});
	}
	
	@Test(dataProvider="testCanWritePublicDirectoryInUserHomeProvider")
	public void testCanWritePublicDirectoryInUserHome(RemoteSystem system, String path, String owner, String internalUsername, boolean expectedResult, boolean shouldThrowException)
	{
		abstractTestCanWrite(system, path, owner, internalUsername, expectedResult, shouldThrowException);
	}
	
	@DataProvider
	protected Object[][] testCanWriteWorldDirectoryInUserHomeProvider() throws Exception
	{
		beforeTestData();
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
		
		String homeDir = getSystemHome(systems[0]);
		String sharedUser = Settings.WORLD_USER_USERNAME;
		
		// system owner shares with self only
		String path = SYSTEM_OWNER + "/publicfoldernotrecursive";
		for (RemoteSystem system: systems) 
		{ 
			createSharedLogicalFile(SYSTEM_OWNER, PermissionType.WRITE, sharedUser, system, homeDir + path, LogicalFile.DIRECTORY, false); 
			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
		}
		
		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, true,	false });
		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
		
		// public readonly systems and systems where the user is admin should always be readable
		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		
		path = SYSTEM_OWNER + "/publicfoldernotrecursive/sharedfile.dat";
		for (RemoteSystem system: systems) 
		{ 
			createSharedLogicalFile(SYSTEM_OWNER, PermissionType.WRITE, sharedUser, system, homeDir + path, LogicalFile.DIRECTORY, false); 
			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
		}
		
		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, true,	false });
		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
		
		// public readonly systems and systems where the user is admin should always be readable
		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		
		path = SYSTEM_OWNER + "/publicfoldernotrecursive/unsharedfile.dat";
		for (RemoteSystem system: systems) 
		{ 
			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
		}
		
		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, false,	false });
		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
		
		// public readonly systems and systems where the user is admin should always be readable
		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		
		path = SYSTEM_OWNER + "/sharedfolderrecursive";
		for (RemoteSystem system: systems) 
		{ 
			createSharedLogicalFile(SYSTEM_OWNER, PermissionType.WRITE, sharedUser, system, homeDir + path, LogicalFile.DIRECTORY, true); 
			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
		}
		
		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, true,	false });
		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
		
		// public readonly systems and systems where the user is admin should always be readable
		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		
		path = SYSTEM_OWNER + "/sharedfolderrecursive/unsharedfile.dat";
		for (RemoteSystem system: systems) 
		{ 
			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
		}
		
		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, true,	false });
		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
		
		// public readonly systems and systems where the user is admin should always be readable
		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		
		
		path = SYSTEM_OWNER + "/sharedfolderrecursive/sharedfile.dat";
		for (RemoteSystem system: systems) 
		{ 
			createSharedLogicalFile(SYSTEM_OWNER, PermissionType.WRITE, sharedUser, system, homeDir + path, LogicalFile.DIRECTORY, false); 
			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
		}
		
		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, true,	false });
		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
		
		// public readonly systems and systems where the user is admin should always be readable
		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		
		
		path = SYSTEM_OWNER + "/unsharedfolder";
		for (RemoteSystem system: systems) 
		{ 
			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
		}
		
		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, false,	false });
		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
		
		// public readonly systems and systems where the user is admin should always be readable
		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		
		path = SYSTEM_OWNER + "/unsharedfolder/unsharedfile.dat";
		for (RemoteSystem system: systems) 
		{ 
			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
		}
		
		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, false,	false });
		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
		
		// public readonly systems and systems where the user is admin should always be readable
		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		
		
		path = SYSTEM_OWNER + "/unsharedfolder/sharedfile.dat";
		for (RemoteSystem system: systems) 
		{ 
			createSharedLogicalFile(SYSTEM_OWNER, PermissionType.WRITE, sharedUser, system, homeDir + path, LogicalFile.DIRECTORY, false); 
			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
		}
		
		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, true,	false });
		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
		
		// public readonly systems and systems where the user is admin should always be readable
		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		
		
		path = SYSTEM_OWNER + "/systemownerpublicshared.dat";
		for (RemoteSystem system: systems) 
		{ 
			createSharedLogicalFile(SYSTEM_OWNER, PermissionType.WRITE, sharedUser, system, homeDir + path, LogicalFile.DIRECTORY, false); 
			testList.add(new Object[]{ system, path, SYSTEM_OWNER, 		null, true, 	false });
		}
		
		testList.add(new Object[]{ publicStorageSystem, 				path, SYSTEM_SHARE_USER, null, true,	false });
		testList.add(new Object[]{ publicMirroredStorageSystem,			path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateStorageSystem, 				path, SYSTEM_SHARE_USER, null, false, 	false });
		testList.add(new Object[]{ privateSharedGuestStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedUserStorageSystem, 		path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedPublisherStorageSystem, path, SYSTEM_SHARE_USER, null, true, 	false });
		
		// public readonly systems and systems where the user is admin should always be readable
		testList.add(new Object[]{ publicGuestStorageSystem, 			path, SYSTEM_SHARE_USER, null, true, 	false });
		testList.add(new Object[]{ privateSharedAdminStorageSystem, 	path, SYSTEM_SHARE_USER, null, true, 	false });
		
		return testList.toArray(new Object[][]{});
	}
	
	@Test(dataProvider="testCanWriteWorldDirectoryInUserHomeProvider")
	public void testCanWriteWorldDirectoryInUserHome(RemoteSystem system, String path, String owner, String internalUsername, boolean expectedResult, boolean shouldThrowException)
	{
		abstractTestCanWrite(system, path, owner, internalUsername, expectedResult, shouldThrowException);
	}
}
