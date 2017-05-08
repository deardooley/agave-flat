package org.iplantc.service.io.permissions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.iplantc.service.io.BaseTestCase;
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
import org.iplantc.service.systems.model.SystemRole;
import org.iplantc.service.systems.model.enumerations.RemoteSystemType;
import org.iplantc.service.systems.model.enumerations.RoleType;
import org.iplantc.service.transfer.RemoteDataClient;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

@Test(groups={"integration"})
public abstract class AbstractPermissionManagerTest extends BaseTestCase
{
	private SystemDao dao = new SystemDao();

	@BeforeClass
	protected void beforeClass() throws Exception
	{
		super.beforeClass();
	}

	@AfterClass
	protected void afterClass() throws Exception
	{
		clearSystems();
		clearLogicalFiles();
	}

	protected void beforeTestData() throws Exception
	{
		clearLogicalFiles();
		clearSystems();
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
		path = StringUtils.replace(path, "//", "/");

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

	protected abstract RemoteSystem getTestSystemDescription(RemoteSystemType type) throws Exception;

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

	/************************************************************************
	/*						ABSTRACT PERMISSION TESTS      					*
	/************************************************************************/

	/**
	 * Generic test whether the given user can read to the given path on the given system
	 *
	 * @param system
	 * @param path
	 * @param owner
	 * @param internalUsername
	 * @param expectedResult
	 * @param shouldThrowException
	 */
	public void abstractTestCanRead(RemoteSystem system, String path, String owner, String internalUsername, boolean expectedResult, boolean shouldThrowException)
	{
		try
		{
			// resolve the relative path against the actual system path
			RemoteDataClient remoteDataClient = system.getRemoteDataClient(internalUsername);
			String absolutePath = remoteDataClient.resolvePath(path);

			LogicalFile logicalFile = LogicalFileDao.findBySystemAndPath(system, absolutePath);

			PermissionManager pm = new PermissionManager(system, remoteDataClient, logicalFile, owner);

			boolean actualResult = pm.canRead(absolutePath);

			String errorMessage = String.format("User %s %s have permission to read %s on a %s%s%s",
					owner,
					expectedResult ? "should have" : "should not have",
					path,
					system.isPubliclyAvailable() ? "public" : "private",
					system.getUserRole(Settings.WORLD_USER_USERNAME).canRead() ? " readonly" : "",
					system.getType().name().toLowerCase() + " system");

			Assert.assertEquals( actualResult, expectedResult, errorMessage );

		}
		catch (Exception e)
		{
			if (!shouldThrowException) Assert.fail(e.getMessage(), e);
		}
	}

	/**
	 * Generic test whether the given user can write to the given path on the given system
	 *
	 * @param system
	 * @param path
	 * @param owner
	 * @param internalUsername
	 * @param expectedResult
	 * @param shouldThrowException
	 */
	protected void abstractTestCanWrite(RemoteSystem system, String path, String owner, String internalUsername, boolean expectedResult, boolean shouldThrowException)
	{
		try
		{
			//	dao.persist(system);
			RemoteDataClient remoteDataClient = system.getRemoteDataClient(internalUsername);
			String absolutePath = remoteDataClient.resolvePath(path);

			LogicalFile logicalFile = LogicalFileDao.findBySystemAndPath(system, absolutePath);

			PermissionManager pm = new PermissionManager(system, remoteDataClient, logicalFile, owner);

			boolean actualResult = pm.canWrite(absolutePath);

			String errorMessage = String.format("User %s %s have permission to write %s on a %s%s%s",
					owner,
					expectedResult ? "should have" : "should not have",
					path,
					system.isPubliclyAvailable() ? "public" : "private",
					system.getUserRole(Settings.WORLD_USER_USERNAME).canRead() ? " readonly" : "",
					system.getType().name().toLowerCase() + " system");

			Assert.assertEquals( actualResult, expectedResult, errorMessage );

		}
		catch (Exception e)
		{
			if (!shouldThrowException) Assert.fail(e.getMessage(), e);
		}
	}

	/**
	 * Generic test whether the given user has read and write permissions for the given path on the given system
	 *
	 * @param system
	 * @param path
	 * @param owner
	 * @param internalUsername
	 * @param expectedResult
	 * @param shouldThrowException
	 */
	protected void abstractTestCanReadWrite(RemoteSystem system, String path, String owner, String internalUsername, boolean expectedResult, boolean shouldThrowException)
	{
		try
		{
			//	dao.persist(system);
			RemoteDataClient remoteDataClient = system.getRemoteDataClient(internalUsername);
			String absolutePath = remoteDataClient.resolvePath(path);

			LogicalFile logicalFile = LogicalFileDao.findBySystemAndPath(system, absolutePath);

			PermissionManager pm = new PermissionManager(system, remoteDataClient, logicalFile, owner);

			boolean actualResult = pm.canReadWrite(absolutePath);

			String errorMessage = String.format("User %s %s have permission to read and write %s on a %s%s%s",
					owner,
					expectedResult ? "should have" : "should not have",
					path,
					system.isPubliclyAvailable() ? "public" : "private",
					system.getUserRole(Settings.WORLD_USER_USERNAME).canRead() ? " readonly" : "",
					system.getType().name().toLowerCase() + " system");

			Assert.assertEquals( actualResult, expectedResult, errorMessage );

		}
		catch (Exception e)
		{
			if (!shouldThrowException) Assert.fail(e.getMessage(), e);
		}
	}

	/**
	 * Generic test whether the given user has read and execute permissions for the given path on the given system
	 *
	 * @param system
	 * @param path
	 * @param owner
	 * @param internalUsername
	 * @param expectedResult
	 * @param shouldThrowException
	 */
	protected void abstractTestCanReadExecute(RemoteSystem system, String path, String owner, String internalUsername, boolean expectedResult, boolean shouldThrowException)
	{
		try
		{
			//	dao.persist(system);
			RemoteDataClient remoteDataClient = system.getRemoteDataClient(internalUsername);
			String absolutePath = remoteDataClient.resolvePath(path);

			LogicalFile logicalFile = LogicalFileDao.findBySystemAndPath(system, absolutePath);

			PermissionManager pm = new PermissionManager(system, remoteDataClient, logicalFile, owner);

			boolean actualResult = pm.canReadExecute(absolutePath);

			String errorMessage = String.format("User %s %s have permission to read and execute %s on a %s%s%s",
					owner,
					expectedResult ? "should have" : "should not have",
					path,
					system.isPubliclyAvailable() ? "public" : "private",
					system.getUserRole(Settings.WORLD_USER_USERNAME).canRead() ? " readonly" : "",
					system.getType().name().toLowerCase() + " system");

			Assert.assertEquals( actualResult, expectedResult, errorMessage );

		}
		catch (Exception e)
		{
			if (!shouldThrowException) Assert.fail(e.getMessage(), e);
		}
	}

	/**
	 * Generic test whether the given user has write and execute permissions for the given path on the given system
	 *
	 * @param system
	 * @param path
	 * @param owner
	 * @param internalUsername
	 * @param expectedResult
	 * @param shouldThrowException
	 */
	protected void abstractTestCanWriteExecute(RemoteSystem system, String path, String owner, String internalUsername, boolean expectedResult, boolean shouldThrowException)
	{
		try
		{
			//	dao.persist(system);
			RemoteDataClient remoteDataClient = system.getRemoteDataClient(internalUsername);
			String absolutePath = remoteDataClient.resolvePath(path);

			LogicalFile logicalFile = LogicalFileDao.findBySystemAndPath(system, absolutePath);

			PermissionManager pm = new PermissionManager(system, remoteDataClient, logicalFile, owner);

			boolean actualResult = pm.canWriteExecute(absolutePath);

			String errorMessage = String.format("User %s %s have permission to write and execute %s on a %s%s%s",
					owner,
					expectedResult ? "should have" : "should not have",
					path,
					system.isPubliclyAvailable() ? "public" : "private",
					system.getUserRole(Settings.WORLD_USER_USERNAME).canRead() ? " readonly" : "",
					system.getType().name().toLowerCase() + " system");

			Assert.assertEquals( actualResult, expectedResult, errorMessage );

		}
		catch (Exception e)
		{
			if (!shouldThrowException) Assert.fail(e.getMessage(), e);
		}
	}

	/**
	 * Generic test whether the given user has all permissions for the given path on the given system
	 *
	 * @param system
	 * @param path
	 * @param owner
	 * @param internalUsername
	 * @param expectedResult
	 * @param shouldThrowException
	 */
	protected void abstractTestCanAll(RemoteSystem system, String path, String owner, String internalUsername, boolean expectedResult, boolean shouldThrowException)
	{
		try
		{
			//	dao.persist(system);
			RemoteDataClient remoteDataClient = system.getRemoteDataClient(internalUsername);
			String absolutePath = remoteDataClient.resolvePath(path);

			LogicalFile logicalFile = LogicalFileDao.findBySystemAndPath(system, absolutePath);

			PermissionManager pm = new PermissionManager(system, remoteDataClient, logicalFile, owner);

			boolean actualResult = pm.canAll(absolutePath);

			String errorMessage = String.format("User %s %s have permission to read, write, and execute %s on a %s%s%s",
					owner,
					expectedResult ? "should have" : "should not have",
					path,
					system.isPubliclyAvailable() ? "public" : "private",
					system.getUserRole(Settings.WORLD_USER_USERNAME).canRead() ? " readonly" : "",
					system.getType().name().toLowerCase() + " system");

			Assert.assertEquals( actualResult, expectedResult, errorMessage );

		}
		catch (Exception e)
		{
			if (!shouldThrowException) Assert.fail(e.getMessage(), e);
		}
	}

	/**
	 * Generic test whether the given user can execute the given path on the given system
	 *
	 * @param system
	 * @param path
	 * @param owner
	 * @param internalUsername
	 * @param expectedResult
	 * @param shouldThrowException
	 */
	protected void abstractTestCanExecute(RemoteSystem system, String path, String owner, String internalUsername, boolean expectedResult, boolean shouldThrowException)
	{
		try
		{
			//	dao.persist(system);
			RemoteDataClient remoteDataClient = system.getRemoteDataClient(internalUsername);
			String absolutePath = remoteDataClient.resolvePath(path);

			LogicalFile logicalFile = LogicalFileDao.findBySystemAndPath(system, absolutePath);

			PermissionManager pm = new PermissionManager(system, remoteDataClient, logicalFile, owner);

			boolean actualResult = pm.canExecute(absolutePath);

			String errorMessage = String.format("User %s %s have permission to execute %s on a %s%s%s",
					owner,
					expectedResult ? "should have" : "should not have",
					path,
					system.isPubliclyAvailable() ? "public" : "private",
					system.getUserRole(Settings.WORLD_USER_USERNAME).canRead() ? " readonly" : "",
					system.getType().name().toLowerCase() + " system");

			Assert.assertEquals( actualResult, expectedResult, errorMessage );

		}
		catch (Exception e)
		{
			if (!shouldThrowException) Assert.fail(e.getMessage(), e);
		}
	}


	/************************************************************************
	/*							 WRITE TESTS      							*
	/************************************************************************/

//	@DataProvider
//	private Object[][] testCanWriteRootProvider() throws Exception
//	{
//		beforeTestData();
//		RemoteSystem publicStorageSystem = getPublicSystem();
//		RemoteSystem publicMirroredStorageSystem = getPublicMirroredSystem();
//		RemoteSystem publicGuestStorageSystem = getPublicGuestSystem();
//		RemoteSystem privateStorageSystem = getPrivateSystem();
//		RemoteSystem privateSharedGuestStorageSystem = getPrivateSharedGuestSystem();
//		RemoteSystem privateSharedUserStorageSystem = getPrivateSharedUserSystem();
//		RemoteSystem privateSharedPublisherStorageSystem = getPrivateSharedPublisherSystem();
//		RemoteSystem privateSharedAdminStorageSystem = getPrivateSharedAdminSystem();
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
//				{ publicGuestStorageSystem, "/", SYSTEM_SHARE_USER, null, false, false },
//				{ privateStorageSystem, "/", SYSTEM_SHARE_USER, null, false, false },
//				{ privateSharedGuestStorageSystem, "/", SYSTEM_SHARE_USER, null, false, false },
//				{ privateSharedUserStorageSystem, "/", SYSTEM_SHARE_USER, null, true, false },
//				{ privateSharedPublisherStorageSystem, "/", SYSTEM_SHARE_USER, null, true, false },
//				{ privateSharedAdminStorageSystem, "/", SYSTEM_SHARE_USER, null, true, false },
//
//				{ publicStorageSystem, "/", SYSTEM_UNSHARED_USER, null, false, false },
//				{ publicMirroredStorageSystem, "/", SYSTEM_UNSHARED_USER, null, false, false },
//				{ publicGuestStorageSystem, "/", SYSTEM_UNSHARED_USER, null, false, false },
//				{ privateStorageSystem, "/", SYSTEM_UNSHARED_USER, null, false, false },
//				{ privateSharedGuestStorageSystem, "/", SYSTEM_UNSHARED_USER, null, false, false },
//				{ privateSharedUserStorageSystem, "/", SYSTEM_UNSHARED_USER, null, false, false },
//				{ privateSharedPublisherStorageSystem, "/", SYSTEM_UNSHARED_USER, null, false, false },
//				{ privateSharedAdminStorageSystem, "/", SYSTEM_UNSHARED_USER, null, false, false },
//		};
//	}
//
//	@Test(dataProvider="testCanWriteRootProvider")
//	public void testCanWriteRoot(RemoteSystem system, String path, String owner, String internalUsername, boolean expectedResult, boolean shouldThrowException)
//	{
//		abstractTestCanWrite(system, path, owner, internalUsername, expectedResult, shouldThrowException);
//	}
//
//
//	@DataProvider
//	private Object[][] testCanWriteHomeProvider() throws Exception
//	{
//		beforeTestData();
//		RemoteSystem publicStorageSystem = getPublicSystem();
//		RemoteSystem publicMirroredStorageSystem = getPublicMirroredSystem();
//		RemoteSystem publicGuestStorageSystem = getPublicGuestSystem();
//		RemoteSystem privateStorageSystem = getPrivateSystem();
//		RemoteSystem privateSharedGuestStorageSystem = getPrivateSharedGuestSystem();
//		RemoteSystem privateSharedUserStorageSystem = getPrivateSharedUserSystem();
//		RemoteSystem privateSharedPublisherStorageSystem = getPrivateSharedPublisherSystem();
//		RemoteSystem privateSharedAdminStorageSystem = getPrivateSharedAdminSystem();
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
//				{ publicGuestStorageSystem, "", SYSTEM_SHARE_USER, null, false, false },
//				{ privateStorageSystem, "", SYSTEM_SHARE_USER, null, false, false },
//				{ privateSharedGuestStorageSystem, "", SYSTEM_SHARE_USER, null, false, false },
//				{ privateSharedUserStorageSystem, "", SYSTEM_SHARE_USER, null, true, false },
//				{ privateSharedPublisherStorageSystem, "", SYSTEM_SHARE_USER, null, true, false },
//				{ privateSharedAdminStorageSystem, "", SYSTEM_SHARE_USER, null, true, false },
//
//				{ publicStorageSystem, "", SYSTEM_UNSHARED_USER, null, false, false },
//				{ publicMirroredStorageSystem, "", SYSTEM_UNSHARED_USER, null, false, false },
//				{ publicGuestStorageSystem, "", SYSTEM_UNSHARED_USER, null, false, false },
//				{ privateStorageSystem, "", SYSTEM_UNSHARED_USER, null, false, false },
//				{ privateSharedGuestStorageSystem, "", SYSTEM_UNSHARED_USER, null, false, false },
//				{ privateSharedUserStorageSystem, "", SYSTEM_UNSHARED_USER, null, false, false },
//				{ privateSharedPublisherStorageSystem, "", SYSTEM_UNSHARED_USER, null, false, false },
//				{ privateSharedAdminStorageSystem, "", SYSTEM_UNSHARED_USER, null, false, false },
//		};
//	}
//
//	@Test(dataProvider="testCanWriteHomeProvider")
//	public void testCanWriteHome(RemoteSystem system, String path, String owner, String internalUsername, boolean expectedResult, boolean shouldThrowException)
//	{
//		abstractTestCanWrite(system, path, owner, internalUsername, expectedResult, shouldThrowException);
//	}
//
//
	/************************************************************************
	/*							EXECUTE TESTS      							*
	/************************************************************************/

//	@Test(dataProvider="testCanWriteRootProvider")
//	public void testCanExecuteRoot(RemoteSystem system, String path, String owner, String internalUsername, boolean expectedResult, boolean shouldThrowException)
//	{
//		abstractTestCanExecute(system, path, owner, internalUsername, expectedResult, shouldThrowException);
//	}
//
//	@Test(dataProvider="testCanWriteHomeProvider")
//	public void testCanExecuteHome(RemoteSystem system, String path, String owner, String internalUsername, boolean expectedResult, boolean shouldThrowException)
//	{
//		abstractTestCanExecute(system, path, owner, internalUsername, expectedResult, shouldThrowException);
//	}

	/*
	 * 	For each storage system protocol
	 * 		For each public/readonly/shared/private
	 * 			For each root, home, folder, file
	 * 				for each user home, public, world, guest, shared
	 * 					For each global admin, tenant admin, owner, shared user, unshared user
	 * 						for logical file known, unknown, parent known, home known
	 * 							check read, write, execute, rw, rx, wx, all, none
	 */

}
