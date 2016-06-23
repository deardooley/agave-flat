package org.iplantc.service.io.permissions;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.iplantc.service.io.BaseTestCase;
import org.iplantc.service.io.Settings;
import org.iplantc.service.io.dao.LogicalFileDao;
import org.iplantc.service.io.model.JSONTestDataUtil;
import org.iplantc.service.io.model.LogicalFile;
import org.iplantc.service.io.model.enumerations.StagingTaskStatus;
import org.iplantc.service.transfer.model.enumerations.PermissionType;
import org.iplantc.service.systems.dao.SystemDao;
import org.iplantc.service.systems.exceptions.SystemException;
import org.iplantc.service.systems.model.ExecutionSystem;
import org.iplantc.service.systems.model.RemoteSystem;
import org.iplantc.service.systems.model.StorageSystem;
import org.iplantc.service.systems.model.SystemRole;
import org.iplantc.service.systems.model.enumerations.RemoteSystemType;
import org.iplantc.service.systems.model.enumerations.RoleType;
import org.iplantc.service.transfer.RemoteDataClient;
import org.iplantc.service.transfer.model.RemoteFilePermission;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class PermissionManagerSetPermissionsTest extends BaseTestCase 
{
	private SystemDao dao = new SystemDao();
	private RemoteSystem publicStorageSystem;
	private RemoteSystem publicMirroredStorageSystem;
	private RemoteSystem publicGuestStorageSystem;
	private RemoteSystem privateStorageSystem;
	private RemoteSystem privateSharedGuestStorageSystem;
	private RemoteSystem privateSharedUserStorageSystem;
	private RemoteSystem privateSharedPublisherStorageSystem;
	private RemoteSystem privateSharedAdminStorageSystem;
	
	
	@BeforeClass
	protected void beforeClass() throws Exception 
	{
		super.beforeClass();
		
		publicStorageSystem = getPublicSystem(RemoteSystemType.STORAGE);
		publicMirroredStorageSystem = getPublicMirroredSystem(RemoteSystemType.STORAGE);
		publicGuestStorageSystem = getPublicGuestSystem(RemoteSystemType.STORAGE);
		privateStorageSystem = getPrivateSystem(RemoteSystemType.STORAGE);
		privateSharedGuestStorageSystem = getPrivateSharedGuestSystem(RemoteSystemType.STORAGE);
		privateSharedUserStorageSystem = getPrivateSharedUserSystem(RemoteSystemType.STORAGE);
		privateSharedPublisherStorageSystem = getPrivateSharedPublisherSystem(RemoteSystemType.STORAGE);
		privateSharedAdminStorageSystem = getPrivateSharedAdminSystem(RemoteSystemType.STORAGE);
		
	}
	
	@AfterClass
	protected void afterClass() throws Exception 
	{
		clearSystems();
		clearLogicalFiles();
	}
	
	@BeforeMethod
	protected void beforeMethod() throws Exception
	{
		clearLogicalFiles();
	}
	
	@AfterMethod
	protected void afterMethod() throws Exception
	{
		clearLogicalFiles();
	}
	
	private List<LogicalFile> initTestFiles(RemoteSystem system, String path) {
		
		List<LogicalFile> srcFiles = new ArrayList<LogicalFile>();
		//srcFiles.add(new LogicalFile(system.getOwner(), system, null, path, "folder", "PROCESSING", LogicalFile.DIRECTORY));
		srcFiles.add(new LogicalFile(system.getOwner(), system, null, path + "/folder", "folder", "PROCESSING", LogicalFile.DIRECTORY));
		srcFiles.add(new LogicalFile(system.getOwner(), system, null, path + "/folder/foo.dat", "foo.dat", "PROCESSING", LogicalFile.RAW));
		srcFiles.add(new LogicalFile(system.getOwner(), system, null, path + "/folder/bar.dat", "bar.dat", "PROCESSING", LogicalFile.RAW));
		srcFiles.add(new LogicalFile(system.getOwner(), system, null, path + "/folder/subfolder", "subfolder", "PROCESSING", LogicalFile.DIRECTORY));
		srcFiles.add(new LogicalFile(system.getOwner(), system, null, path + "/folder/subfolder/alpha.txt", "alpha.txt", "PROCESSING", LogicalFile.RAW));
		srcFiles.add(new LogicalFile(system.getOwner(), system, null, path + "/file.dat", "file.dat", "PROCESSING", LogicalFile.RAW));
		srcFiles.add(new LogicalFile(system.getOwner(), system, null, path + "/emptyfolder", "emptyfolder", "PROCESSING", LogicalFile.DIRECTORY));
		
		for (LogicalFile file: srcFiles) {
			file.setStatus(StagingTaskStatus.STAGING_QUEUED);
			file.setUuid(file.getPath());
			LogicalFileDao.save(file);
		}
		
		return srcFiles;
	}
	
	protected String getSystemRoot(RemoteSystem system) throws Exception
	{
		String rootDir = FilenameUtils.normalize(system.getStorageConfig().getRootDir());
		if (!StringUtils.isEmpty(rootDir)) {
			if (!rootDir.endsWith("/")) {
				rootDir += "/";
			}
		} else {
			rootDir = "/";
		}
		
		return rootDir.replaceAll("/+", "/");
	}
	
	protected String getSystemHome(RemoteSystem system) throws Exception
	{
		String rootDir = getSystemRoot(system);
		String homeDir = FilenameUtils.normalize(system.getStorageConfig().getHomeDir());
        if (!StringUtils.isEmpty(homeDir)) {
            homeDir = rootDir +  homeDir;
            if (!homeDir.endsWith("/")) {
                homeDir += "/";
            }
        } else {
            homeDir = rootDir;
        }

        return homeDir.replaceAll("/+", "/");
	}
	
	protected LogicalFile createSharedLogicalFile(String owner, PermissionType permissionType, String sharedUsername, RemoteSystem system, String path, String nativeFormat, boolean recursive) 
	throws Exception
	{
		path = path.replaceAll("//", "/");
		
		LogicalFile file = new LogicalFile( owner, system, path );
		file.setNativeFormat(nativeFormat);		
		LogicalFileDao.persist(file);
		
		PermissionManager pm = new PermissionManager(system, system.getRemoteDataClient(), file, sharedUsername);
		
		if (permissionType.equals(PermissionType.ALL)) {
			pm.addAllPermission(path, recursive);
		}
		else
		{
			if (permissionType.canRead()) {
				pm.addReadPermission(path, recursive);
			} 
			if (permissionType.canWrite()) {
				pm.addWritePermission(path, recursive);
			} 
			if (permissionType.canExecute()) {
				pm.addExecutePermission(path, recursive);
			} 
		}
		
		return file;
	}
	
	/************************************************************************
	/*						STORAGE SYSTEM CONSTRUCTORS		   				*
	/************************************************************************/
	
	protected RemoteSystem getBaseStorageSystem(String systemId) throws Exception
	{
		return getBaseSystem(systemId, RemoteSystemType.STORAGE);
	}
	
	protected RemoteSystem getBaseExecutionSystem(String systemId) throws Exception
	{
		return getBaseSystem(systemId, RemoteSystemType.EXECUTION);
	}
	
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
	/*						EXECUTION SYSTEM CONSTRUCTORS	 				*
	/************************************************************************/
	
	private RemoteSystem getBaseSystem(String systemId, RemoteSystemType type) throws Exception
	{
		RemoteSystem system = getTestSystemDescription(type);

		String storageEncryptionKey = system.getSystemId() + 
    			system.getStorageConfig().getHost() + 
    			system.getStorageConfig().getDefaultAuthConfig().getUsername();
		
		system.getStorageConfig().getDefaultAuthConfig().setPassword(
				system.getStorageConfig().getDefaultAuthConfig().getClearTextPassword(storageEncryptionKey));
		system.getStorageConfig().getDefaultAuthConfig().setCredential(
				system.getStorageConfig().getDefaultAuthConfig().getClearTextCredential(storageEncryptionKey));
		system.getStorageConfig().getDefaultAuthConfig().setPublicKey(
				system.getStorageConfig().getDefaultAuthConfig().getClearTextPublicKey(storageEncryptionKey));
		system.getStorageConfig().getDefaultAuthConfig().setPrivateKey(
				system.getStorageConfig().getDefaultAuthConfig().getClearTextPrivateKey(storageEncryptionKey));
		
		if (type.equals(RemoteSystemType.EXECUTION))
		{
			String loginEncryptionKey = system.getSystemId() + 
    			((ExecutionSystem)system).getLoginConfig().getHost() + 
    			((ExecutionSystem)system).getLoginConfig().getDefaultAuthConfig().getUsername();
		
			((ExecutionSystem)system).getLoginConfig().getDefaultAuthConfig().setPassword(
					((ExecutionSystem)system).getLoginConfig().getDefaultAuthConfig().getClearTextPassword(loginEncryptionKey));
			((ExecutionSystem)system).getLoginConfig().getDefaultAuthConfig().setCredential(
					((ExecutionSystem)system).getLoginConfig().getDefaultAuthConfig().getClearTextCredential(loginEncryptionKey));
			((ExecutionSystem)system).getLoginConfig().getDefaultAuthConfig().setPublicKey(
					((ExecutionSystem)system).getLoginConfig().getDefaultAuthConfig().getClearTextPublicKey(loginEncryptionKey));
			((ExecutionSystem)system).getLoginConfig().getDefaultAuthConfig().setPrivateKey(
					((ExecutionSystem)system).getLoginConfig().getDefaultAuthConfig().getClearTextPrivateKey(loginEncryptionKey));
		}
		
		system.setSystemId(systemId);
		
		storageEncryptionKey = system.getSystemId() + 
    			system.getStorageConfig().getHost() + 
    			system.getStorageConfig().getDefaultAuthConfig().getUsername();
		
		if (!StringUtils.isEmpty(system.getStorageConfig().getDefaultAuthConfig().getPassword()))
    		system.getStorageConfig().getDefaultAuthConfig().encryptCurrentPassword(storageEncryptionKey);
    	
    	if (!StringUtils.isEmpty(system.getStorageConfig().getDefaultAuthConfig().getCredential()))
    		system.getStorageConfig().getDefaultAuthConfig().encryptCurrentCredential(storageEncryptionKey);
    	
    	if (!StringUtils.isEmpty(system.getStorageConfig().getDefaultAuthConfig().getPublicKey()))
    		system.getStorageConfig().getDefaultAuthConfig().encryptCurrentPublicKey(storageEncryptionKey);
    	
    	if (!StringUtils.isEmpty(system.getStorageConfig().getDefaultAuthConfig().getPrivateKey()))
    		system.getStorageConfig().getDefaultAuthConfig().encryptCurrentPrivateKey(storageEncryptionKey);
    	
    	if (type.equals(RemoteSystemType.EXECUTION))
		{
    		String loginEncryptionKey = system.getSystemId() + 
	    			((ExecutionSystem)system).getLoginConfig().getHost() + 
	    			((ExecutionSystem)system).getLoginConfig().getDefaultAuthConfig().getUsername();
			
	    	if (!StringUtils.isEmpty(((ExecutionSystem)system).getLoginConfig().getDefaultAuthConfig().getPassword()))
	    		((ExecutionSystem)system).getLoginConfig().getDefaultAuthConfig().encryptCurrentPassword(loginEncryptionKey);
	    	
	    	if (!StringUtils.isEmpty(((ExecutionSystem)system).getLoginConfig().getDefaultAuthConfig().getCredential()))
	    		((ExecutionSystem)system).getLoginConfig().getDefaultAuthConfig().encryptCurrentCredential(loginEncryptionKey);
	    	
	    	if (!StringUtils.isEmpty(((ExecutionSystem)system).getLoginConfig().getDefaultAuthConfig().getPublicKey()))
	    		((ExecutionSystem)system).getLoginConfig().getDefaultAuthConfig().encryptCurrentPublicKey(loginEncryptionKey);
	    	
	    	if (!StringUtils.isEmpty(((ExecutionSystem)system).getLoginConfig().getDefaultAuthConfig().getPrivateKey()))
	    		((ExecutionSystem)system).getLoginConfig().getDefaultAuthConfig().encryptCurrentPrivateKey(loginEncryptionKey);
		}
    	
		return system;
	}
	
	protected RemoteSystem getPublicMirroredSystem(RemoteSystemType type) throws Exception 
	{
		// public global default system
		RemoteSystem system = getBaseStorageSystem("public-mirrored-" + type.name().toLowerCase() + "-system");
		system.setOwner(SYSTEM_OWNER);
		system.setPubliclyAvailable(true);
		system.setGlobalDefault(true);
		system.getStorageConfig().setMirrorPermissions(true);
		dao.persist(system);
        return system;
	}
	
	protected RemoteSystem getPublicSystem(RemoteSystemType type) throws Exception 
	{
		// public global default system
		RemoteSystem system = getBaseStorageSystem("public-" + type.name().toLowerCase() + "-system");
    	system.setOwner(SYSTEM_OWNER);
    	system.setPubliclyAvailable(true);
    	system.setGlobalDefault(true);
    	system.getStorageConfig().setMirrorPermissions(false);
    	dao.persist(system);
    	return system;
	}
        
	protected RemoteSystem getPublicGuestSystem(RemoteSystemType type) throws Exception 
	{
		// public readonly system
		RemoteSystem system = getBaseStorageSystem("public-guest-" + type.name().toLowerCase() + "-system");
		system.setOwner(SYSTEM_OWNER);
        system.addRole(new SystemRole(Settings.WORLD_USER_USERNAME, RoleType.GUEST));
        system.setPubliclyAvailable(true);
        system.setGlobalDefault(false);
        dao.persist(system);
        return system;
	}
	
	protected RemoteSystem getPrivateSystem(RemoteSystemType type) throws Exception 
	{
        // private shared system
		RemoteSystem system = getBaseStorageSystem("private-" + type.name().toLowerCase() + "-system");
		system.setOwner(SYSTEM_OWNER);
		dao.persist(system);
        return system;
	}
	
	protected RemoteSystem getPrivateSharedGuestSystem(RemoteSystemType type) throws Exception 
	{
        // private shared readonly system
		RemoteSystem system = getBaseStorageSystem("private-guest-" + type.name().toLowerCase() + "-system");
		system.setOwner(SYSTEM_OWNER);
		system.addRole(new SystemRole(SYSTEM_SHARE_USER, RoleType.GUEST));
		dao.persist(system);
        return system;
	}
	
	protected RemoteSystem getPrivateSharedUserSystem(RemoteSystemType type) throws Exception 
	{
        // private shared system
		RemoteSystem system = getBaseStorageSystem("private-user-" + type.name().toLowerCase() + "-system");
		system.setOwner(SYSTEM_OWNER);
		system.addRole(new SystemRole(SYSTEM_SHARE_USER, RoleType.USER));
		dao.persist(system);
        return system;
	}
	
	protected RemoteSystem getPrivateSharedPublisherSystem(RemoteSystemType type) throws Exception 
	{
        // private shared system
		RemoteSystem system = getBaseStorageSystem("private-publisher-" + type.name().toLowerCase() + "-system");
		system.setOwner(SYSTEM_OWNER);
		system.addRole(new SystemRole(SYSTEM_SHARE_USER, RoleType.PUBLISHER));
		dao.persist(system);
        return system;
	}
	
	protected RemoteSystem getPrivateSharedAdminSystem(RemoteSystemType type) throws Exception 
	{
        // private shared system
		RemoteSystem system = getBaseStorageSystem("private-admin-" + type.name().toLowerCase() + "-system");
		system.setOwner(SYSTEM_OWNER);
		system.addRole(new SystemRole(SYSTEM_SHARE_USER, RoleType.ADMIN));
		dao.persist(system);
        return system;
	}
	
	/**
	 * Generates an all pairs of test users on system and path test set
	 * @param system
	 * @param path
	 * @return
	 * @throws Exception
	 */
	private Object[][] createAllPairsTests(RemoteSystem system, String path, PermissionType permission) throws Exception 
	{
		return new Object[][] {
			{ system, path, SYSTEM_OWNER, SYSTEM_OWNER, PermissionType.ALL, false },
			{ system, path, SYSTEM_OWNER, SYSTEM_SHARE_USER, PermissionType.NONE, false },
			{ system, path, SYSTEM_OWNER, SYSTEM_UNSHARED_USER, PermissionType.NONE, false },
			{ system, path, SYSTEM_OWNER, Settings.WORLD_USER_USERNAME, PermissionType.NONE, false },
			{ system, path, SYSTEM_OWNER, Settings.PUBLIC_USER_USERNAME, PermissionType.NONE, false },
			
			{ system, path, SYSTEM_SHARE_USER, SYSTEM_OWNER, PermissionType.ALL, false },
			{ system, path, SYSTEM_SHARE_USER, SYSTEM_SHARE_USER, permission, false },
			{ system, path, SYSTEM_SHARE_USER, SYSTEM_UNSHARED_USER, PermissionType.NONE, false },
			{ system, path, SYSTEM_SHARE_USER, Settings.WORLD_USER_USERNAME, PermissionType.NONE, false },
			{ system, path, SYSTEM_SHARE_USER, Settings.PUBLIC_USER_USERNAME, PermissionType.NONE, false },
			
			{ system, path, SYSTEM_UNSHARED_USER, SYSTEM_OWNER, PermissionType.ALL, false },
			{ system, path, SYSTEM_UNSHARED_USER, SYSTEM_SHARE_USER, PermissionType.NONE, false },
			{ system, path, SYSTEM_UNSHARED_USER, SYSTEM_UNSHARED_USER, permission, false },
			{ system, path, SYSTEM_UNSHARED_USER, Settings.WORLD_USER_USERNAME, PermissionType.NONE, false },
			{ system, path, SYSTEM_UNSHARED_USER, Settings.PUBLIC_USER_USERNAME, PermissionType.NONE, false },
			
			{ system, path, Settings.WORLD_USER_USERNAME, SYSTEM_OWNER, PermissionType.ALL, false },
			{ system, path, Settings.WORLD_USER_USERNAME, SYSTEM_SHARE_USER, permission, false },
			{ system, path, Settings.WORLD_USER_USERNAME, SYSTEM_UNSHARED_USER, permission, false },
			{ system, path, Settings.WORLD_USER_USERNAME, Settings.WORLD_USER_USERNAME, permission, false },
			{ system, path, Settings.WORLD_USER_USERNAME, Settings.PUBLIC_USER_USERNAME, permission, false },
			
			{ system, path, Settings.PUBLIC_USER_USERNAME, SYSTEM_OWNER, PermissionType.ALL, false },
			{ system, path, Settings.PUBLIC_USER_USERNAME, SYSTEM_SHARE_USER, permission, false },
			{ system, path, Settings.PUBLIC_USER_USERNAME, SYSTEM_UNSHARED_USER, permission, false },
			{ system, path, Settings.PUBLIC_USER_USERNAME, Settings.WORLD_USER_USERNAME, permission, false },
			{ system, path, Settings.PUBLIC_USER_USERNAME, Settings.PUBLIC_USER_USERNAME, permission, false },
		};
	}
	
	/********************************************************************
	 *																	*
	 *						READ PERMISSION TESTS						*
	 *																	*
	 ********************************************************************/
	
	/*************** SET READ PERMISSIONS ***********************/
	
	@DataProvider
	protected Object[][] setReadPermissionFolderProvider() throws Exception
	{
		return createAllPairsTests(publicStorageSystem, getSystemRoot(publicStorageSystem) + "unittest/folder", PermissionType.READ);
	}
	
	@Test(dataProvider="setReadPermissionFolderProvider")
	public void testSetReadPermissionFolder(RemoteSystem system, String path, String permissionUsername, String testUsername, PermissionType expectedPermission, boolean recursive) throws Exception
	{
		setReadPermission(system, path, permissionUsername, testUsername, expectedPermission, false);
	}
	
	@Test(dataProvider="setReadPermissionFolderProvider")
	public void testSetReadPermissionFolderRecursive(RemoteSystem system, String path, String permissionUsername, String testUsername, PermissionType expectedPermission, boolean recursive) throws Exception
	{
		setReadPermission(system, path, permissionUsername, testUsername, expectedPermission, true);
	}
	
	@DataProvider
	protected Object[][] setReadPermissionFileProvider() throws Exception
	{
		return createAllPairsTests(publicStorageSystem, getSystemRoot(publicStorageSystem) + "unittest/folder/foo.dat", PermissionType.READ);
	}
	
	@Test(dataProvider="setReadPermissionFileProvider")
	public void testSetReadPermissionFile(RemoteSystem system, String path, String permissionUsername, String testUsername, PermissionType expectedPermission, boolean recursive) throws Exception
	{
		setReadPermission(system, path, permissionUsername, testUsername, expectedPermission, false);
	}
	
	@Test(dataProvider="setReadPermissionFileProvider")
	public void testSetReadPermissionFileRecursive(RemoteSystem system, String path, String permissionUsername, String testUsername, PermissionType expectedPermission, boolean recursive) throws Exception
	{		
		setReadPermission(system, path, permissionUsername, testUsername, expectedPermission, true);
	}

	public void testSetReadPermissionSubsequentFile(RemoteSystem system, String path, String permissionUsername, String testUsername, PermissionType expectedPermission, boolean recursive) throws Exception
	{
		@SuppressWarnings("unused")
		List<LogicalFile> testLogicalFiles = initTestFiles(publicStorageSystem, getSystemRoot(publicStorageSystem) + "unittest");
		
		RemoteDataClient remoteDataClient = system.getRemoteDataClient();//internalUsername);
		LogicalFile logicalFile = LogicalFileDao.findBySystemAndPath(system, path);
		
		PermissionManager pm = new PermissionManager(system, remoteDataClient, logicalFile, permissionUsername);
		
		pm.addReadPermission(path, false);
		boolean worldReadable = system.getUserRole(Settings.WORLD_USER_USERNAME).canRead();
		String errorMessage = String.format("User %s should have %s permission on %s after setting %s READ permission on a %s%s%s", 
				permissionUsername,
				expectedPermission.name(),
				path,
				recursive ? "recursive" : "non-recursive",
				system.isPubliclyAvailable() ? "public" : "private",
				worldReadable ? " readonly" : "",
				system.getType().name() + " system");

		PermissionManager testPm = new PermissionManager(system, remoteDataClient, logicalFile, testUsername);
		RemoteFilePermission actualPermission = testPm.getUserPermission(path);
		Assert.assertEquals( actualPermission.getPermission(), expectedPermission, errorMessage );
		
		if (recursive) {
			List<LogicalFile> children = LogicalFileDao.findChildren(path, system.getId());
			for (LogicalFile child: children) {
				errorMessage = String.format("User %s should have %s permission on %s after setting recursive READ permission on a %s%s%s", 
						permissionUsername,
						expectedPermission.name(),
						child.getPath(),
						system.isPubliclyAvailable() ? "public" : "private",
						worldReadable ? " readonly" : "",
						system.getType().name() + " system");
				RemoteFilePermission childPermission = testPm.getUserPermission(child.getPath());
				Assert.assertEquals( childPermission.getPermission().equals(expectedPermission), recursive, errorMessage );
			}
		}
	}
	
	public void setReadPermission(RemoteSystem system, String path, String permissionUsername, String testUsername, PermissionType expectedPermission, boolean recursive) throws Exception 
	{
		@SuppressWarnings("unused")
		List<LogicalFile> testLogicalFiles = initTestFiles(publicStorageSystem, getSystemRoot(publicStorageSystem) + "unittest");
		
		RemoteDataClient remoteDataClient = system.getRemoteDataClient();//internalUsername);
		LogicalFile logicalFile = LogicalFileDao.findBySystemAndPath(system, path);
		
		PermissionManager pm = new PermissionManager(system, remoteDataClient, logicalFile, permissionUsername);
		
		pm.addReadPermission(path, recursive);
		boolean worldReadable = system.getUserRole(Settings.WORLD_USER_USERNAME).canRead();
		PermissionManager testPm = new PermissionManager(system, remoteDataClient, logicalFile, testUsername);
		RemoteFilePermission actualPermission = testPm.getUserPermission(path);
		String errorMessage = String.format("User %s should have %s permission on %s after setting %s READ permission on a %s%s%s. Found %s instead.", 
				permissionUsername,
				expectedPermission.name(),
				path,
				recursive ? "recursive" : "non-recursive",
				system.isPubliclyAvailable() ? "public" : "private",
				worldReadable ? " readonly " : "",
				system.getType().name() + " system",
				actualPermission.getPermission().name());

		Assert.assertEquals( actualPermission.getPermission().name(), expectedPermission.name(), errorMessage );
		
		if (recursive) {
			List<LogicalFile> children = LogicalFileDao.findChildren(path, system.getId());
			for (LogicalFile child: children) {
				RemoteFilePermission childPermission = testPm.getUserPermission(child.getPath());
				errorMessage = String.format("User %s should have %s permission on %s after setting recursive READ permission on a %s%s%s", 
						permissionUsername,
						expectedPermission.name(),
						child.getPath(),
						system.isPubliclyAvailable() ? "public" : "private",
						worldReadable ? " readonly " : "",
						system.getType().name() + " system",
						childPermission.getPermission().name());
				Assert.assertEquals( childPermission.getPermission().name(), expectedPermission.name(), errorMessage );
			}
		}
	}
	
	/*************** REMOVE READ PERMISSIONS ***********************/
	
	@Test(dataProvider="setReadPermissionFileProvider")
	public void testRemoveReadPermissionFile(RemoteSystem system, String path, String permissionUsername, String testUsername, PermissionType expectedPermission, boolean recursive) throws Exception
	{
		removeReadPermission(system, path, permissionUsername, testUsername, expectedPermission, false);
	}
	
	@Test(dataProvider="setReadPermissionFileProvider")
	public void testRemoveReadPermissionFileRecursive(RemoteSystem system, String path, String permissionUsername, String testUsername, PermissionType expectedPermission, boolean recursive) throws Exception
	{	
		removeReadPermission(system, path, permissionUsername, testUsername, expectedPermission, true);
	}
	
	@Test(dataProvider="setReadPermissionFolderProvider")
	public void testRemoveReadPermissionFolder(RemoteSystem system, String path, String permissionUsername, String testUsername, PermissionType expectedPermission, boolean recursive) throws Exception
	{
		removeReadPermission(system, path, permissionUsername, testUsername, expectedPermission, false);
	}
	
	@Test(dataProvider="setReadPermissionFolderProvider")
	public void testRemoveReadPermissionFolderRecursive(RemoteSystem system, String path, String permissionUsername, String testUsername, PermissionType expectedPermission, boolean recursive) throws Exception
	{	
		removeReadPermission(system, path, permissionUsername, testUsername, expectedPermission, true);
	}
	
	public void removeReadPermission(RemoteSystem system, String path, String permissionUsername, String testUsername, PermissionType expectedPermission, boolean recursive) throws Exception 
	{
		
		@SuppressWarnings("unused")
		List<LogicalFile> testLogicalFiles = initTestFiles(publicStorageSystem, getSystemRoot(publicStorageSystem) + "unittest");
		
		RemoteDataClient remoteDataClient = system.getRemoteDataClient();//internalUsername);
		LogicalFile logicalFile = LogicalFileDao.findBySystemAndPath(system, path);
		
		PermissionManager pm = new PermissionManager(system, remoteDataClient, logicalFile, permissionUsername);
		
		pm.addReadPermission(path, recursive);
		boolean worldReadable = system.getUserRole(Settings.WORLD_USER_USERNAME).canRead();
		PermissionManager testPm = new PermissionManager(system, remoteDataClient, logicalFile, testUsername);
		RemoteFilePermission actualPermission = testPm.getUserPermission(path);
		String errorMessage = String.format("User %s should have %s permission on %s after adding %s READ permission on a %s%s%s. Found %s instead.", 
				testUsername,
				expectedPermission.name(),
				path,
				recursive ? "recursive" : "non-recursive",
				system.isPubliclyAvailable() ? "public" : "private",
				worldReadable ? " readonly " : "",
				system.getType().name() + " system",
				actualPermission.getPermission().name());

		Assert.assertEquals( actualPermission.getPermission().name(), expectedPermission.name(), errorMessage );
		
		if (recursive) {
			List<LogicalFile> children = LogicalFileDao.findChildren(path, system.getId());
			for (LogicalFile child: children) {
				RemoteFilePermission childPermission = testPm.getUserPermission(child.getPath());
				
				errorMessage = String.format("User %s should have %s permission on %s after removing adding READ permission on a %s%s%s. Found %s instead.", 
						testUsername,
						expectedPermission.name(),
						child.getPath(),
						system.isPubliclyAvailable() ? "public" : "private",
						worldReadable ? " readonly " : "",
						system.getType().name() + " system",
						childPermission.getPermission().name());
				
				Assert.assertEquals( childPermission.getPermission().name().equals(expectedPermission.name()), true, errorMessage );
			}
		}
		
		pm.removeReadPermission(path, recursive);
		logicalFile = LogicalFileDao.findBySystemAndPath(system, path);
		testPm = new PermissionManager(system, remoteDataClient, logicalFile, testUsername);
		actualPermission = testPm.getUserPermission(path);
		errorMessage = String.format("User %s should not have %s permission on %s after removing %s READ permission on a %s%s%s. Found %s instead.", 
				testUsername,
				expectedPermission.name(),
				path,
				recursive ? "recursive" : "non-recursive",
				system.isPubliclyAvailable() ? "public" : "private",
				worldReadable ? " readonly " : "",
				system.getType().name() + " system",
				StringUtils.equals(testUsername, SYSTEM_OWNER) ? PermissionType.ALL.name() : PermissionType.NONE.name());
		
		if (StringUtils.equals(testUsername, SYSTEM_OWNER)) {
			Assert.assertEquals( actualPermission.getPermission().name(), PermissionType.ALL.name(), errorMessage );
		} else {
			Assert.assertEquals( actualPermission.getPermission().name(), PermissionType.NONE.name(), errorMessage );
		}
		
		
		if (recursive) {
			List<LogicalFile> children = LogicalFileDao.findChildren(path, system.getId());
			for (LogicalFile child: children) {
				RemoteFilePermission childPermission = testPm.getUserPermission(child.getPath());
				
				errorMessage = String.format("User %s should not have %s permission on %s after removing recursive READ permission on a %s%s%s. Found %s instead.", 
						testUsername,
						expectedPermission.name(),
						child.getPath(),
						system.isPubliclyAvailable() ? "public" : "private",
						worldReadable ? " readonly " : "",
						system.getType().name() + " system",
						StringUtils.equals(testUsername, SYSTEM_OWNER) ? PermissionType.ALL.name() : PermissionType.NONE.name());
				
				if (StringUtils.equals(testUsername, SYSTEM_OWNER)) 
				{
					Assert.assertEquals( childPermission.getPermission().name().equals(PermissionType.ALL.name()), true, errorMessage );
				} 
				else
				{
					Assert.assertEquals( childPermission.getPermission().name().equals(PermissionType.NONE.name()), true, errorMessage );
				}
			}
		}
	}
//	
//	/********************************************************************
//	 *																	*
//	 *						WRITE PERMISSION TESTS						*
//	 *																	*
//	 ********************************************************************/
//
//	/*************** ADD WRITE PERMISSIONS ***********************/
//	
//	@DataProvider
//	protected Object[][] setWritePermissionFolderProvider() throws Exception
//	{
//		return createAllPairsTests(publicStorageSystem, getSystemRoot(publicStorageSystem) + "unittest/folder", PermissionType.WRITE);
//	}
//	
//	@Test(dataProvider="setWritePermissionFolderProvider")
//	public void testSetWritePermissionFolder(RemoteSystem system, String path, String permissionUsername, String testUsername, PermissionType expectedPermission, boolean recursive) throws Exception
//	{
//		setWritePermission(system, path, permissionUsername, testUsername, expectedPermission, false);
//	}
//	
//	@Test(dataProvider="setWritePermissionFolderProvider")
//	public void testSetWritePermissionFolderRecursive(RemoteSystem system, String path, String permissionUsername, String testUsername, PermissionType expectedPermission, boolean recursive) throws Exception
//	{
//		setWritePermission(system, path, permissionUsername, testUsername, expectedPermission, true);
//	}
//	
//	@DataProvider
//	protected Object[][] setWritePermissionFileProvider() throws Exception
//	{
//		return createAllPairsTests(publicStorageSystem, getSystemRoot(publicStorageSystem) + "unittest/folder/foo.dat", PermissionType.WRITE);
//	}
//	
//	@Test(dataProvider="setWritePermissionFileProvider")
//	public void testSetWritePermissionFile(RemoteSystem system, String path, String permissionUsername, String testUsername, PermissionType expectedPermission, boolean recursive) throws Exception
//	{
//		setWritePermission(system, path, permissionUsername, testUsername, expectedPermission, false);
//	}
//	
//	@Test(dataProvider="setWritePermissionFileProvider")
//	public void testSetWritePermissionFileRecursive(RemoteSystem system, String path, String permissionUsername, String testUsername, PermissionType expectedPermission, boolean recursive) throws Exception
//	{		
//		setWritePermission(system, path, permissionUsername, testUsername, expectedPermission, true);
//	}
//
//	public void setWritePermission(RemoteSystem system, String path, String permissionUsername, String testUsername, PermissionType expectedPermission, boolean recursive) throws Exception 
//	{
//		@SuppressWarnings("unused")
//		List<LogicalFile> testLogicalFiles = initTestFiles(publicStorageSystem, getSystemRoot(publicStorageSystem) + "unittest");
//		
//		RemoteDataClient remoteDataClient = system.getRemoteDataClient();//internalUsername);
//		LogicalFile logicalFile = LogicalFileDao.findBySystemAndPath(system, path);
//		
//		PermissionManager pm = new PermissionManager(system, remoteDataClient, logicalFile, permissionUsername);
//		
//		pm.addWritePermission(path, recursive);
//		boolean worldReadable = system.getUserRole(Settings.WORLD_USER_USERNAME).canRead();
//		PermissionManager testPm = new PermissionManager(system, remoteDataClient, logicalFile, testUsername);
//		RemoteFilePermission actualPermission = testPm.getUserPermission(path);
//		String errorMessage = String.format("User %s should have %s permission on %s after setting %s WRITE permission on a %s%s%s. Found %s instead.", 
//				permissionUsername,
//				expectedPermission.name(),
//				path,
//				recursive ? "recursive" : "non-recursive",
//				system.isPubliclyAvailable() ? "public" : "private",
//				worldReadable ? " readonly " : "",
//				system.getType().name() + " system",
//				actualPermission.getPermission().name());
//
//		Assert.assertEquals( actualPermission.getPermission().name(), expectedPermission.name(), errorMessage );
//		
//		if (recursive) {
//			List<LogicalFile> children = LogicalFileDao.findChildren(path, system.getId());
//			for (LogicalFile child: children) {
//				RemoteFilePermission childPermission = testPm.getUserPermission(child.getPath());
//				
//				errorMessage = String.format("User %s should have %s permission on %s after setting recursive WRITE permission on a %s%s%s. Found %s instead.", 
//						permissionUsername,
//						expectedPermission.name(),
//						child.getPath(),
//						system.isPubliclyAvailable() ? "public" : "private",
//						worldReadable ? " readonly " : "",
//						system.getType().name() + " system",
//						childPermission.getPermission().name());
//				
//				Assert.assertEquals( childPermission.getPermission().name().equals(expectedPermission.name()), true, errorMessage );
//			}
//		}
//	}
//	
//	/*************** REMOVE WRITE PERMISSIONS ***********************/
//	
//	@Test(dataProvider="setWritePermissionFileProvider")
//	public void testRemoveWritePermissionFile(RemoteSystem system, String path, String permissionUsername, String testUsername, PermissionType expectedPermission, boolean recursive) throws Exception
//	{
//		removeWritePermission(system, path, permissionUsername, testUsername, expectedPermission, false);
//	}
//	
//	@Test(dataProvider="setWritePermissionFileProvider")
//	public void testRemoveWritePermissionFileRecursive(RemoteSystem system, String path, String permissionUsername, String testUsername, PermissionType expectedPermission, boolean recursive) throws Exception
//	{	
//		removeWritePermission(system, path, permissionUsername, testUsername, expectedPermission, true);
//	}
//	
//	@Test(dataProvider="setWritePermissionFolderProvider")
//	public void testRemoveWritePermissionFolder(RemoteSystem system, String path, String permissionUsername, String testUsername, PermissionType expectedPermission, boolean recursive) throws Exception
//	{
//		removeWritePermission(system, path, permissionUsername, testUsername, expectedPermission, false);
//	}
//	
//	@Test(dataProvider="setWritePermissionFolderProvider")
//	public void testRemoveWritePermissionFolderRecursive(RemoteSystem system, String path, String permissionUsername, String testUsername, PermissionType expectedPermission, boolean recursive) throws Exception
//	{	
//		removeWritePermission(system, path, permissionUsername, testUsername, expectedPermission, true);
//	}
//	
//	public void removeWritePermission(RemoteSystem system, String path, String permissionUsername, String testUsername, PermissionType expectedPermission, boolean recursive) throws Exception 
//	{
//		
//		@SuppressWarnings("unused")
//		List<LogicalFile> testLogicalFiles = initTestFiles(publicStorageSystem, getSystemRoot(publicStorageSystem) + "unittest");
//		
//		RemoteDataClient remoteDataClient = system.getRemoteDataClient();//internalUsername);
//		LogicalFile logicalFile = LogicalFileDao.findBySystemAndPath(system, path);
//		
//		PermissionManager pm = new PermissionManager(system, remoteDataClient, logicalFile, permissionUsername);
//		
//		pm.addWritePermission(path, recursive);
//		boolean worldReadable = system.getUserRole(Settings.WORLD_USER_USERNAME).canRead();
//		PermissionManager testPm = new PermissionManager(system, remoteDataClient, logicalFile, testUsername);
//		RemoteFilePermission actualPermission = testPm.getUserPermission(path);
//		String errorMessage = String.format("User %s should have %s permission on %s after adding %s WRITE permission on a %s%s%s. Found %s instead.", 
//				permissionUsername,
//				expectedPermission.name(),
//				path,
//				recursive ? "recursive" : "non-recursive",
//				system.isPubliclyAvailable() ? "public" : "private",
//				worldReadable ? " readonly " : "",
//				system.getType().name() + " system",
//				actualPermission.getPermission().name());
//
//		Assert.assertEquals( actualPermission.getPermission().name(), expectedPermission.name(), errorMessage );
//		
//		if (recursive) {
//			List<LogicalFile> children = LogicalFileDao.findChildren(path, system.getId());
//			for (LogicalFile child: children) {
//				RemoteFilePermission childPermission = testPm.getUserPermission(child.getPath());
//				
//				errorMessage = String.format("User %s should have %s permission on %s after removing adding WRITE permission on a %s%s%s. Found %s instead.", 
//						permissionUsername,
//						expectedPermission.name(),
//						child.getPath(),
//						system.isPubliclyAvailable() ? "public" : "private",
//						worldReadable ? " readonly " : "",
//						system.getType().name() + " system",
//						childPermission.getPermission().name());
//				
//				Assert.assertEquals( childPermission.getPermission().name().equals(expectedPermission.name()), true, errorMessage );
//			}
//		}
//		
//		pm.removeWritePermission(path, recursive);
//		logicalFile = LogicalFileDao.findBySystemAndPath(system, path);
//		testPm = new PermissionManager(system, remoteDataClient, logicalFile, testUsername);
//		actualPermission = testPm.getUserPermission(path);
//		errorMessage = String.format("User %s should not have %s permission on %s after removing %s WRITE permission on a %s%s%s. Found %s instead.", 
//				permissionUsername,
//				expectedPermission.name(),
//				path,
//				recursive ? "recursive" : "non-recursive",
//				system.isPubliclyAvailable() ? "public" : "private",
//				worldReadable ? " readonly " : "",
//				system.getType().name() + " system",
//				StringUtils.equals(testUsername, SYSTEM_OWNER) ? PermissionType.ALL.name() : PermissionType.NONE.name());
//		
//		if (StringUtils.equals(testUsername, SYSTEM_OWNER)) {
//			Assert.assertEquals( actualPermission.getPermission().name(), PermissionType.ALL.name(), errorMessage );
//		} else {
//			Assert.assertEquals( actualPermission.getPermission().name(), PermissionType.NONE.name(), errorMessage );
//		}
//		
//		if (recursive) {
//			List<LogicalFile> children = LogicalFileDao.findChildren(path, system.getId());
//			for (LogicalFile child: children) {
//				RemoteFilePermission childPermission = testPm.getUserPermission(child.getPath());
//				
//				errorMessage = String.format("User %s should not have %s permission on %s after removing recursive WRITE permission on a %s%s%s. Found %s instead.", 
//						permissionUsername,
//						expectedPermission.name(),
//						child.getPath(),
//						system.isPubliclyAvailable() ? "public" : "private",
//						worldReadable ? " readonly " : "",
//						system.getType().name() + " system",
//						StringUtils.equals(testUsername, SYSTEM_OWNER) ? PermissionType.ALL.name() : PermissionType.NONE.name());
//				
//				if (StringUtils.equals(testUsername, SYSTEM_OWNER)) {
//					Assert.assertEquals( childPermission.getPermission().name(), PermissionType.ALL.name(), errorMessage );
//				} else {
//					Assert.assertEquals( childPermission.getPermission().name(), PermissionType.NONE.name(), errorMessage );
//				}
//			}
//		}
//	}
//	
//	/********************************************************************
//	 *																	*
//	 *					EXECUTE PERMISSION TESTS						*
//	 *																	*
//	 ********************************************************************/
//	
//	/*************** SET EXECUTE PERMISSIONS ***********************/
//	
//	@DataProvider
//	protected Object[][] setExecutePermissionFolderProvider() throws Exception
//	{
//		return createAllPairsTests(publicStorageSystem, getSystemRoot(publicStorageSystem) + "unittest/folder", PermissionType.EXECUTE);
//	}
//	
//	@Test(dataProvider="setExecutePermissionFolderProvider")
//	public void testSetExecutePermissionFolder(RemoteSystem system, String path, String permissionUsername, String testUsername, PermissionType expectedPermission, boolean recursive) throws Exception
//	{
//		setExecutePermission(system, path, permissionUsername, testUsername, expectedPermission, false);
//	}
//	
//	@Test(dataProvider="setExecutePermissionFolderProvider")
//	public void testSetExecutePermissionFolderRecursive(RemoteSystem system, String path, String permissionUsername, String testUsername, PermissionType expectedPermission, boolean recursive) throws Exception
//	{
//		setExecutePermission(system, path, permissionUsername, testUsername, expectedPermission, true);
//	}
//	
//	@DataProvider
//	protected Object[][] setExecutePermissionFileProvider() throws Exception
//	{
//		return createAllPairsTests(publicStorageSystem, getSystemRoot(publicStorageSystem) + "unittest/folder/foo.dat", PermissionType.EXECUTE);
//	}
//	
//	@Test(dataProvider="setExecutePermissionFileProvider")
//	public void testSetExecutePermissionFile(RemoteSystem system, String path, String permissionUsername, String testUsername, PermissionType expectedPermission, boolean recursive) throws Exception
//	{
//		setExecutePermission(system, path, permissionUsername, testUsername, expectedPermission, false);
//	}
//	
//	@Test(dataProvider="setExecutePermissionFileProvider")
//	public void testSetExecutePermissionFileRecursive(RemoteSystem system, String path, String permissionUsername, String testUsername, PermissionType expectedPermission, boolean recursive) throws Exception
//	{		
//		setExecutePermission(system, path, permissionUsername, testUsername, expectedPermission, true);
//	}
//	
//	public void setExecutePermission(RemoteSystem system, String path, String permissionUsername, String testUsername, PermissionType expectedPermission, boolean recursive) throws Exception 
//	{
//		@SuppressWarnings("unused")
//		List<LogicalFile> testLogicalFiles = initTestFiles(publicStorageSystem, getSystemRoot(publicStorageSystem) + "unittest");
//		
//		RemoteDataClient remoteDataClient = system.getRemoteDataClient();//internalUsername);
//		LogicalFile logicalFile = LogicalFileDao.findBySystemAndPath(system, path);
//		
//		PermissionManager pm = new PermissionManager(system, remoteDataClient, logicalFile, permissionUsername);
//		
//		pm.addExecutePermission(path, recursive);
//		boolean worldReadable = system.getUserRole(Settings.WORLD_USER_USERNAME).canRead();
//		PermissionManager testPm = new PermissionManager(system, remoteDataClient, logicalFile, testUsername);
//		RemoteFilePermission actualPermission = testPm.getUserPermission(path);
//		String errorMessage = String.format("User %s should have %s permission on %s after setting %s EXECUTE permission on a %s%s%s. Found %s instead.", 
//				permissionUsername,
//				expectedPermission.name(),
//				path,
//				recursive ? "recursive" : "non-recursive",
//				system.isPubliclyAvailable() ? "public" : "private",
//				worldReadable ? " readonly " : "",
//				system.getType().name() + " system",
//				actualPermission.getPermission().name());
//
//		Assert.assertEquals( actualPermission.getPermission().name(), expectedPermission.name(), errorMessage );
//		
//		if (recursive) {
//			List<LogicalFile> children = LogicalFileDao.findChildren(path, system.getId());
//			for (LogicalFile child: children) {
//				RemoteFilePermission childPermission = testPm.getUserPermission(child.getPath());
//				
//				errorMessage = String.format("User %s should have %s permission on %s after setting recursive EXECUTE permission on a %s%s%s. Found %s instead.", 
//						permissionUsername,
//						expectedPermission.name(),
//						child.getPath(),
//						system.isPubliclyAvailable() ? "public" : "private",
//						worldReadable ? " readonly " : "",
//						system.getType().name() + " system",
//						childPermission.getPermission().name());
//				
//				Assert.assertEquals( childPermission.getPermission().name().equals(expectedPermission.name()), true, errorMessage );
//			}
//		}
//	}
//	
//	/*************** REMOVE EXECUTE PERMISSIONS ***********************/
//	
//	@Test(dataProvider="setExecutePermissionFileProvider")
//	public void testRemoveExecutePermissionFile(RemoteSystem system, String path, String permissionUsername, String testUsername, PermissionType expectedPermission, boolean recursive) throws Exception
//	{
//		removeExecutePermission(system, path, permissionUsername, testUsername, expectedPermission, false);
//	}
//	
//	@Test(dataProvider="setExecutePermissionFileProvider")
//	public void testRemoveExecutePermissionFileRecursive(RemoteSystem system, String path, String permissionUsername, String testUsername, PermissionType expectedPermission, boolean recursive) throws Exception
//	{	
//		removeExecutePermission(system, path, permissionUsername, testUsername, expectedPermission, true);
//	}
//	
//	@Test(dataProvider="setExecutePermissionFolderProvider")
//	public void testRemoveExecutePermissionFolder(RemoteSystem system, String path, String permissionUsername, String testUsername, PermissionType expectedPermission, boolean recursive) throws Exception
//	{
//		removeExecutePermission(system, path, permissionUsername, testUsername, expectedPermission, false);
//	}
//	
//	@Test(dataProvider="setExecutePermissionFolderProvider")
//	public void testRemoveExecutePermissionFolderRecursive(RemoteSystem system, String path, String permissionUsername, String testUsername, PermissionType expectedPermission, boolean recursive) throws Exception
//	{	
//		removeExecutePermission(system, path, permissionUsername, testUsername, expectedPermission, true);
//	}
//	
//	public void removeExecutePermission(RemoteSystem system, String path, String permissionUsername, String testUsername, PermissionType expectedPermission, boolean recursive) throws Exception 
//	{
//		
//		@SuppressWarnings("unused")
//		List<LogicalFile> testLogicalFiles = initTestFiles(publicStorageSystem, getSystemRoot(publicStorageSystem) + "unittest");
//		
//		RemoteDataClient remoteDataClient = system.getRemoteDataClient();//internalUsername);
//		LogicalFile logicalFile = LogicalFileDao.findBySystemAndPath(system, path);
//		
//		PermissionManager pm = new PermissionManager(system, remoteDataClient, logicalFile, permissionUsername);
//		
//		pm.addExecutePermission(path, recursive);
//		boolean worldReadable = system.getUserRole(Settings.WORLD_USER_USERNAME).canRead();
//		PermissionManager testPm = new PermissionManager(system, remoteDataClient, logicalFile, testUsername);
//		RemoteFilePermission actualPermission = testPm.getUserPermission(path);
//		String errorMessage = String.format("User %s should have %s permission on %s after adding %s EXECUTE permission on a %s%s%s. Found %s instead.", 
//				permissionUsername,
//				expectedPermission.name(),
//				path,
//				recursive ? "recursive" : "non-recursive",
//				system.isPubliclyAvailable() ? "public" : "private",
//				worldReadable ? " readonly " : "",
//				system.getType().name() + " system",
//				actualPermission.getPermission().name());
//
//		Assert.assertEquals( actualPermission.getPermission().name(), expectedPermission.name(), errorMessage );
//		
//		if (recursive) {
//			List<LogicalFile> children = LogicalFileDao.findChildren(path, system.getId());
//			for (LogicalFile child: children) {
//				RemoteFilePermission childPermission = testPm.getUserPermission(child.getPath());
//				
//				errorMessage = String.format("User %s should have %s permission on %s after removing adding EXECUTE permission on a %s%s%s. Found %s instead.", 
//						permissionUsername,
//						expectedPermission.name(),
//						child.getPath(),
//						system.isPubliclyAvailable() ? "public" : "private",
//						worldReadable ? " readonly " : "",
//						system.getType().name() + " system",
//						childPermission.getPermission().name());
//				
//				Assert.assertEquals( childPermission.getPermission().name().equals(expectedPermission.name()), true, errorMessage );
//			}
//		}
//		
//		pm.removeExecutePermission(path, recursive);
//		logicalFile = LogicalFileDao.findBySystemAndPath(system, path);
//		testPm = new PermissionManager(system, remoteDataClient, logicalFile, testUsername);
//		actualPermission = testPm.getUserPermission(path);
//		errorMessage = String.format("User %s should not have %s permission on %s after removing %s EXECUTE permission on a %s%s%s. Found %s instead.", 
//				permissionUsername,
//				expectedPermission.name(),
//				path,
//				recursive ? "recursive" : "non-recursive",
//				system.isPubliclyAvailable() ? "public" : "private",
//				worldReadable ? " readonly " : "",
//				system.getType().name() + " system",
//				StringUtils.equals(testUsername, SYSTEM_OWNER) ? PermissionType.ALL.name() : PermissionType.NONE.name());
//		
//		if (StringUtils.equals(testUsername, SYSTEM_OWNER)) {
//			Assert.assertEquals( actualPermission.getPermission().name(), PermissionType.ALL.name(), errorMessage );
//		} else {
//			Assert.assertEquals( actualPermission.getPermission().name(), PermissionType.NONE.name(), errorMessage );
//		}
//		
//		if (recursive) {
//			List<LogicalFile> children = LogicalFileDao.findChildren(path, system.getId());
//			for (LogicalFile child: children) {
//				RemoteFilePermission childPermission = testPm.getUserPermission(child.getPath());
//				
//				errorMessage = String.format("User %s should not have %s permission on %s after removing recursive EXECUTE permission on a %s%s%s. Found %s instead.", 
//						permissionUsername,
//						expectedPermission.name(),
//						child.getPath(),
//						system.isPubliclyAvailable() ? "public" : "private",
//						worldReadable ? " readonly " : "",
//						system.getType().name() + " system",
//						StringUtils.equals(testUsername, SYSTEM_OWNER) ? PermissionType.ALL.name() : PermissionType.NONE.name());
//				
//				if (StringUtils.equals(testUsername, SYSTEM_OWNER)) {
//					Assert.assertEquals( childPermission.getPermission().name(), PermissionType.ALL.name(), errorMessage );
//				} else {
//					Assert.assertEquals( childPermission.getPermission().name(), PermissionType.NONE.name(), errorMessage );
//				}
//			}
//		}
//	}
//	
	public void testSetOwnerPermissionRecursive(RemoteSystem system, 
			String path, 
			String owner, 
			String internalUsername, 
			boolean recursive,
			PermissionType expectedPermission, 
			boolean shouldThrowException) throws Exception
	{
		try 
		{
			//	dao.persist(system);
			RemoteDataClient remoteDataClient = system.getRemoteDataClient(internalUsername);
			String absolutePath = remoteDataClient.resolvePath(path);
			
			LogicalFile logicalFile = LogicalFileDao.findBySystemAndPath(system, absolutePath);
			
			PermissionManager pm = new PermissionManager(system, remoteDataClient, logicalFile, owner);
			
			pm.addAllPermission(absolutePath, recursive);
			
			String errorMessage = String.format("User %s should have %s permission on %s after setting ALL permission on a %s%s%s", 
				owner,
				expectedPermission.name(),
				path,
				system.isPubliclyAvailable() ? "public" : "private",
				system.getUserRole(Settings.WORLD_USER_USERNAME).canRead() ? " readonly" : "",
				system.getType().name().toLowerCase() + " system");
			
			RemoteFilePermission actualPermission = pm.getUserPermission(absolutePath);
			Assert.assertEquals( actualPermission.getPermission(), expectedPermission, errorMessage );
		
		}
		catch (Exception e)
		{
			if (!shouldThrowException) Assert.fail(e.getMessage(), e);
		}
	}
	
}
