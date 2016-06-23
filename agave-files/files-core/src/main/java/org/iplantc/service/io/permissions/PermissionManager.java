/**
 * 
 */
package org.iplantc.service.io.permissions;

import java.math.BigInteger;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.plexus.util.FileUtils;
import org.iplantc.service.common.exceptions.AgaveNamespaceException;
import org.iplantc.service.common.exceptions.PermissionException;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.common.uri.AgaveUriRegex;
import org.iplantc.service.common.uri.AgaveUriUtil;
import org.iplantc.service.io.Settings;
import org.iplantc.service.io.dao.FileEventDao;
import org.iplantc.service.io.dao.LogicalFileDao;
import org.iplantc.service.io.exceptions.LogicalFileException;
import org.iplantc.service.io.model.FileEvent;
import org.iplantc.service.io.model.LogicalFile;
import org.iplantc.service.io.model.enumerations.FileEventType;
import org.iplantc.service.io.util.PathResolver;
import org.iplantc.service.systems.dao.SystemDao;
import org.iplantc.service.systems.exceptions.SystemException;
import org.iplantc.service.systems.manager.SystemManager;
import org.iplantc.service.systems.model.RemoteSystem;
import org.iplantc.service.systems.model.SystemRole;
import org.iplantc.service.systems.util.ApiUriUtil;
import org.iplantc.service.transfer.RemoteDataClient;
import org.iplantc.service.transfer.RemoteDataClientFactory;
import org.iplantc.service.transfer.dao.RemoteFilePermissionDao;
import org.iplantc.service.transfer.exceptions.RemoteDataException;
import org.iplantc.service.transfer.model.RemoteFilePermission;
import org.iplantc.service.transfer.model.enumerations.PermissionType;

/**
 * Management class for file and folder permissions. This class determines
 * whether a user other than the owner has permission to view/modify a 
 * file item.
 *  
 * @author dooley
 *
 */
public class PermissionManager {

	private String remoteUsername;
	private String apiUsername;
	private RemoteSystem remoteSystem;
	
	//private IrodsClient irodsClient;

    private RemoteDataClient remoteDataClient = null;
    private LogicalFile logicalFile = null;
	
	public PermissionManager(RemoteSystem remoteSystem, RemoteDataClient client, 
			LogicalFile logicalFile, String apiUsername) 
	{
		this.remoteSystem = remoteSystem;
		if (client == null) {
			this.remoteDataClient = null;
    		this.logicalFile = logicalFile;
    		this.apiUsername = apiUsername;
    		this.remoteUsername = apiUsername;
		} 
		else
		{
			if (remoteSystem.isPubliclyAvailable()) { // assume tenant idp is lined up with system
	        	if (client.isPermissionMirroringRequired()) {
	        		// permissions are on the remote system and the usernames are the same
	        		this.remoteDataClient = client;
	        		this.logicalFile = logicalFile;
	        		this.apiUsername = apiUsername;
	        		this.remoteUsername = apiUsername;
	        	} else {
	        		// permissions are only in the api, so check in the db
	        		this.remoteDataClient = null;
	        		this.logicalFile = logicalFile;
	        		this.apiUsername = apiUsername;
	        		this.remoteUsername = client.getUsername();
	        	}
	        } else { // tenant idp may not be lined up with system 
	        	if (client.isPermissionMirroringRequired()) {
	        		this.remoteDataClient = client;
	        		this.logicalFile = logicalFile;
	        		this.apiUsername = apiUsername;
	        		this.remoteUsername = client.getUsername();
	        	} else {
	        		// permissions are only in the api, so check in the db
	        		this.remoteDataClient = null;
	        		this.logicalFile = logicalFile;
	        		this.apiUsername = apiUsername;
	        		this.remoteUsername = apiUsername;
	        	}
	        }
		}
	}
	
	/**
	 * Returns all permissions for a given path. This method handles the merge of permissions
	 * between those defined at the api level and, when appropriate, those defined at the system
	 * level. In all cases, system level permissions trump api permissions when mirroring
	 * is turned on. 
	 * 
	 * @param agavePath
	 * @return
	 * @throws PermissionException
	 */
	public Collection<RemoteFilePermission> getAllPermissions(String agavePath) throws PermissionException
	{
		Map<String, RemoteFilePermission> pemMap = new TreeMap<String, RemoteFilePermission>();
		
		LogicalFile logicalFile = this.logicalFile;
		RemoteDataClient remoteDataClient = this.remoteDataClient;
		boolean addedImpliedSystemOwnerPermission = false;
		boolean isUserSystemOwner = StringUtils.equals(apiUsername, remoteSystem.getOwner());
		
		try 
		{
			// We always add implicit file permissions from system ownership
			pemMap.put(remoteSystem.getOwner(), new RemoteFilePermission(remoteSystem.getOwner(), null, PermissionType.ALL, true));
			
			// now iterate through the other system roles and decide on a case-by-case basis.
			for (SystemRole role: remoteSystem.getRoles()) {
				// we automatically add system admins to the ownership permissions
				if (role.canAdmin()) {
					pemMap.put(role.getUsername(), 
						new RemoteFilePermission(role.getUsername(), null, PermissionType.ALL, true));
				}
				// we add user system roles for private systems because these reflect effective file permission
				else if (!remoteSystem.isPubliclyAvailable()) {
					pemMap.put(role.getUsername(), 
							new RemoteFilePermission(role.getUsername(), null, PermissionType.ALL, true));
				}
				else {
					// on public systems, we don't present the implied system ownership over the data because the system user
					// roles do not retain elevated permissions after they are made public
					
					// TODO: add system and tenant admin groups to this list when the group api launches
				}
			} 
			
			if (logicalFile == null) 
	        {	
				// add the remote permissions
				if (remoteDataClient != null)
				{
					logicalFile = new LogicalFile(apiUsername, remoteSystem, remoteDataClient.resolvePath(agavePath));
					
					// determine and add the permission of the calling user
					pemMap.put(apiUsername, getUserPermission(logicalFile.getPath()));
					
					if (remoteDataClient.isPermissionMirroringRequired())  
					{	
						for (RemoteFilePermission agavePem: remoteDataClient.getAllPermissionsWithUserFirst(agavePath, apiUsername)) {
							pemMap.put(agavePem.getUsername(), agavePem);
						}
						
						pemMap.put(apiUsername, new RemoteFilePermission(apiUsername, null, remoteDataClient.getPermissionForUser(apiUsername, agavePath), false));
					}
				}
				else
				{
					try {
						remoteDataClient = remoteSystem.getRemoteDataClient();
						logicalFile = new LogicalFile(apiUsername, remoteSystem, remoteDataClient.resolvePath(agavePath));
						
						// determine and add the permission of the calling user
						pemMap.put(apiUsername, getUserPermission(logicalFile.getPath()));
					} 
					finally {
						// clean up so we don't leave open connections
						remoteDataClient.disconnect();
					}
				}
				
				// if a public system, and the path is under a user home directory, add the user as an owner.
				if (remoteSystem.isPubliclyAvailable() && !remoteSystem.getUserRole(Settings.WORLD_USER_USERNAME).isGuest()) 
				{
					// logical file owner doens't matter here since we're just createing it for the relative path hacking
					String publicUserFromPath = PathResolver.getImpliedOwnerFromSystemPath(agavePath, remoteSystem, remoteDataClient);
					
					if (StringUtils.isNotEmpty(publicUserFromPath) && !pemMap.containsKey(publicUserFromPath)){
						pemMap.put(publicUserFromPath, new RemoteFilePermission(publicUserFromPath, null, PermissionType.ALL, true));
					}
				}
				
				
				// if we created a RemoteDataClient just for this check, disconnect here to clean up threads
				if (this.remoteDataClient == null && remoteDataClient != null) {
					try { remoteDataClient.disconnect(); } catch (Exception e) {}
				}
	        }
			else
			{
				// determine and add the permission of the calling user
				pemMap.put(apiUsername, getUserPermission(logicalFile.getPath()));
				
				// add all agave pems
				for (RemoteFilePermission agavePem: RemoteFilePermissionDao.getBylogicalFileId(logicalFile.getId()))
				{
					pemMap.put(agavePem.getUsername(), agavePem);
				}
				
//				String resolvedPath = logicalFile.getAgaveRelativePathFromAbsolutePath();
				
				// if a public system, and the path is under a user home directory, add the user as an owner.
				if (remoteSystem.isPubliclyAvailable() && !remoteSystem.getUserRole(Settings.WORLD_USER_USERNAME).isGuest())
				{
					// logical file owner doens't matter here since we're just creating it for the relative path hacking
					String publicUserFromPath = PathResolver.getImpliedOwnerFromSystemPath(agavePath, remoteSystem, remoteDataClient == null ?  remoteSystem.getRemoteDataClient() : remoteDataClient);
					
					if (StringUtils.isNotEmpty(publicUserFromPath) && !pemMap.containsKey(publicUserFromPath)){
						pemMap.put(publicUserFromPath, new RemoteFilePermission(publicUserFromPath, null, PermissionType.ALL, true));
					}
				}
				
				if (remoteDataClient != null && remoteDataClient.isPermissionMirroringRequired()) 
				{
					// merge the system pems with the api pems. system pems trump. if the public user is 
					// preseetn, skip as this can cause issues with irods groups and all users have read permission anyway.
					if (!pemMap.containsKey(Settings.PUBLIC_USER_USERNAME))
					{
						for (RemoteFilePermission remotePem: remoteDataClient.getAllPermissionsWithUserFirst(agavePath, apiUsername)) {
							pemMap.put(remotePem.getUsername(), remotePem);
						}
						
						pemMap.put(apiUsername, new RemoteFilePermission(apiUsername, null, remoteDataClient.getPermissionForUser(apiUsername, agavePath), false));
					}
				}
			}
			
			return pemMap.values();
		} 
		catch (Exception e) 
		{
			throw new PermissionException(e);
		}
	}
	
	/**
	 * Returns the calculated user permission for a given path on the
	 * system assigned to this PermissionManager. There is no guarantee
	 * that the returned permission has been explicitly set, thus the
	 * PermissionType.add() and PermissionType.remove() methods should
	 * be used to update the actual saved permissions.
	 * 
	 * @param systemAbsolutePath Absolute path to the file on the remote system.
	 * 
	 * @return RemoteFilePermission
	 * @throws PermissionException
	 */
	public RemoteFilePermission getUserPermission(String systemAbsolutePath) throws PermissionException
	{
		//God users even have read permissions on files that don't exist.
		String internalUsername = logicalFile == null ? null : logicalFile.getInternalUsername();
		Long logicalFileId = logicalFile == null ? null : logicalFile.getId();
        SystemRole userRole = remoteSystem.getUserRole(apiUsername);
        
        if (StringUtils.isEmpty(apiUsername))// || userRole.getRole().equals(RoleType.NONE)) 
		{
        	return new RemoteFilePermission(logicalFileId, apiUsername, internalUsername, PermissionType.NONE, true);
		}
		// admins have total control
		else if (userRole.canAdmin())
		{
			return new RemoteFilePermission(logicalFileId, apiUsername, internalUsername, PermissionType.ALL, true);
		}
        // if outside of system rootDir. We need this due to the recursive calls to this method
        // when searching for a known parent
		else if (!isUnderSystemRootDir(systemAbsolutePath)) 
		{
			return new RemoteFilePermission(logicalFileId, apiUsername, internalUsername, PermissionType.NONE, true);
		}
        // if we are inspecting a public system, we need to check against the forced
        // user home directories in system.storageConfig.homeDir + "/" + apiUsername
		else if (remoteSystem.isPubliclyAvailable())
        {
			// mirroring is turned on, so we should delegate to the system
        	// in this situation, the user home directories we virutalize will
        	// actually be present on the remote system and it will have 
        	// corresponding acl support, so we can safely access the data.
			// The exception being if the path or one of its parent folders
			// was given public or world permissions. In this case, the 
			// RemoteFilePermissions trump the system permissions.
			if (remoteDataClient != null)
        	{
				// no logical file, check for public or world pems, or readonly system
				if (logicalFile == null) 
				{
					// find the closest known parent and check for recursive permisisons
					LogicalFile parent = LogicalFileDao.findClosestParent(remoteSystem, systemAbsolutePath);
					if (parent != null) 
					{
						// make sure parent is still within the system root directory tree
						if (isUnderSystemRootDir(parent.getPath() + "/"))
						{
							// get the public user permissions on the parent. this will resolve user,
							// public, and world permissions along with readonly system status.
							// we use the public user because we are mirroring and do not want
							// to use the user specific api permissions since we will be deferring
							// to the remote system.
							RemoteFilePermission parentPem = getGrantedUserPermissionForLogicalFile(parent, Settings.PUBLIC_USER_USERNAME);
							// if permissions are found and they are recursive, apply here
							// otherwise defer to the system.
							if (!parentPem.getPermission().equals(PermissionType.NONE) && parentPem.isRecursive())
							{
								return parentPem;
							}
						}
						// file has no pems, and the parent is outside the current system root
    					else
    					{
    						return new RemoteFilePermission(logicalFileId, apiUsername, internalUsername, PermissionType.NONE, false);
    					}
					}
				}
				// logical file existed already, check permissions
				else
				{
					// look up public user permissions for the path. this will resolve user,
					// public, and world permissions along with readonly system status.
					// we use the public user because we are mirroring and do not want
					// to use the user specific api permissions since we will be deferring
					// to the remote system.
					RemoteFilePermission userPem = getGrantedUserPermissionForLogicalFile(logicalFile, apiUsername);
					if (userPem.getPermission().equals(PermissionType.NONE))
					{
						// find the closest known parent and check for recursive permisisons
						LogicalFile parent = LogicalFileDao.findClosestParent(remoteSystem, systemAbsolutePath);
						if (parent != null) {
							if (isUnderSystemRootDir(parent.getPath() + "/"))
							{
								// get the public user permissions on the parent. this will resolve user,
								// public, and world permissions along with readonly system status.
								// we use the public user because we are mirroring and do not want
								// to use the user specific api permissions since we will be deferring
								// to the remote system.
								RemoteFilePermission parentPem = getGrantedUserPermissionForLogicalFile(parent, Settings.PUBLIC_USER_USERNAME);
								// if permissions are found and they are recursive, apply here
								// otherwise defer to the system.
								if (!parentPem.getPermission().equals(PermissionType.NONE) && parentPem.isRecursive())
								{
									return parentPem;
								}
							}
							// file has no pems, and the parent is outside the current system root
	    					else
	    					{
	    						return new RemoteFilePermission(logicalFileId, apiUsername, internalUsername, PermissionType.NONE, false);
	    					}
						}
					}
					// public or world user had pems. extend these to the user
					else
					{
						return userPem;
					}
				}
				
				try 
				{
					// at this point we know the data wasn't exposed via the API through publishing or 
					// as a readonly file system. We can defer to the remote system for the user's permissions
					// on this path
					String agavePath = null;
					if (logicalFile == null) {
						agavePath = new LogicalFile(apiUsername, remoteSystem, systemAbsolutePath).getAgaveRelativePathFromAbsolutePath();
					} else {
						agavePath = logicalFile.getAgaveRelativePathFromAbsolutePath();
					}
        			PermissionType permissionType = remoteDataClient.getPermissionForUser(remoteUsername, agavePath);
        			if (permissionType.equals(PermissionType.NONE) && userRole.isGuest()) {
						return new RemoteFilePermission(logicalFileId, apiUsername, internalUsername, PermissionType.READ, true);
					} else {
						return new RemoteFilePermission(logicalFileId, apiUsername, internalUsername, permissionType, true);
					}
				} 
				catch (Exception e) 
				{
					throw new PermissionException(e);
				}
        	}
			// no permission mirroring on this public system
        	else 
        	{
    			// user should have access to all files and folders in their 
        		// public system home directory tree unless explicitly denied
        		if (isUserHomeDirOnPublicSystem(systemAbsolutePath) || isUnderUserHomeDirOnPublicSystem(systemAbsolutePath)) 
        		{
        			RemoteFilePermission userPem = null;
        			// check to see if there is a permission for this file or directory
        			if (logicalFile != null) 
        			{
        				userPem = getGrantedUserPermissionForLogicalFile(logicalFile, apiUsername);
        			} 
        			// no entry for the path, look up the parent
        			else 
        			{
        				LogicalFile parent = LogicalFileDao.findClosestParent(remoteSystem, systemAbsolutePath);
        				if (parent != null && isUnderSystemRootDir(parent.getPath() + "/") && 
        						(isUserHomeDirOnPublicSystem(parent.getPath()) || isUnderUserHomeDirOnPublicSystem(parent.getPath())))
        				{
        					// get the user permissions on the parent. this will resolve user,
							// public, and world permissions along with readonly system status.
							RemoteFilePermission parentPem = getGrantedUserPermissionForLogicalFile(parent, apiUsername);
							// if permissions are found and they are recursive, apply here
							if (parentPem.isRecursive()) {
								userPem = parentPem;
							}
        				}
        			}
        			
        			// if not, then we assign the guest scope as appropriate
        			if (userRole.isGuest()) {
        				return RemoteFilePermission.merge(userPem, new RemoteFilePermission(logicalFileId, apiUsername, internalUsername, PermissionType.READ, true));
        			} 
        			// otherwise, this is the user's home directory on a public system, so they have ALL permissions
        			else {
        				return RemoteFilePermission.merge(userPem, new RemoteFilePermission(logicalFileId, apiUsername, internalUsername, PermissionType.ALL, true));
        			}
        		}
        		// the path is known, so look up the permissions of the path and, if necessary,
        		// the closest parent to check for recursive permissions
        		else if (logicalFile != null)
	        	{	
        			RemoteFilePermission userPem = getGrantedUserPermissionForLogicalFile(logicalFile, apiUsername);
        			// if the user has a pem for this file, use it.
        			if (!userPem.getPermission().equals(PermissionType.NONE)) 
        			{
        				return userPem;
        			}
        			// otherwise check for the next known parent permission and check for recursive permissions
        			else
        			{
        				LogicalFile parent = LogicalFileDao.findClosestParent(remoteSystem, systemAbsolutePath);
        				if (parent == null) {
    						// no parent found and no pems for the file. we just need to check for 
    						// guest role at this point
    						if (userRole.isGuest()) {
    							return new RemoteFilePermission(logicalFileId, apiUsername, internalUsername, PermissionType.READ, true);
    						}
    						// no parent found, return NON pems from original file 
    						else
    						{
    							return new RemoteFilePermission(logicalFileId, apiUsername, internalUsername, PermissionType.NONE, true);
    						}
    					}
    					// parent was found. make sure parent is still within the system root directory tree
    					else if (isUnderSystemRootDir(parent.getPath() + "/"))
						{
							// get the user permissions on the parent. this will resolve user,
							// public, and world permissions along with readonly system status.
							RemoteFilePermission parentPem = getGrantedUserPermissionForLogicalFile(parent, apiUsername);
							// if permissions are found and they are recursive, apply here
							if (!parentPem.getPermission().equals(PermissionType.NONE) && parentPem.isRecursive())
							{
								return parentPem;
							}
							// parent permissions did not grant access or were not recursive
							else
							{
								return new RemoteFilePermission(logicalFileId, apiUsername, internalUsername, PermissionType.NONE, false);
							}
						}
        				// file has no pems, and the parent is outside the current system root
    					else
    					{
    						return new RemoteFilePermission(logicalFileId, apiUsername, internalUsername, PermissionType.NONE, false);
    					}
        			}
	        	}
        		// the path is unknown and it's outside of the user's virtual home directory 
        		// on a public system. Look for the closest parent to check for recursive
        		// permissions
        		else 
        		{
        			// find the closest known parent and check for recursive permisisons
					LogicalFile parent = LogicalFileDao.findClosestParent(remoteSystem, systemAbsolutePath);
					if (parent == null)
					{
						// no parent found and no pems for the file. we just need to check for 
						// guest role at this point
						if (userRole.isGuest()) 
						{
							return new RemoteFilePermission(logicalFileId, apiUsername, internalUsername, PermissionType.READ, true);
						} 
						// no known parent and the path is outside of their home directory, so deny permission
						else 
						{
							return new RemoteFilePermission(logicalFileId, apiUsername, internalUsername, PermissionType.NONE, false);
						}
					}
					// parent was found. make sure parent is still within the system root directory tree
					else if (isUnderSystemRootDir(parent.getPath() + "/"))
					{
						// get the public user permissions on the parent. this will resolve user,
						// public, and world permissions along with readonly system status.
						// we use the public user because we are mirroring and do not want
						// to use the user specific api permissions since we will be deferring
						// to the remote system.
						RemoteFilePermission parentPem = getGrantedUserPermissionForLogicalFile(parent, apiUsername);
						// if permissions are found and they are recursive, apply here
						// otherwise defer to the system.
						if (parentPem.isRecursive()) {
							return parentPem;
						} else {
							return new RemoteFilePermission(logicalFileId, apiUsername, internalUsername, PermissionType.NONE, false);
						}
						
					}
					// file has no pems, and the parent is outside the current system root
					else
					{
						return new RemoteFilePermission(logicalFileId, apiUsername, internalUsername, PermissionType.NONE, false);
					}
        			
//        			LogicalFile parent = LogicalFileDao.findBySystemAndPath(remoteSystem, getParentPath(path));
//        			PermissionManager pm = new PermissionManager(remoteSystem, remoteDataClient, parent, apiUsername);
//        			RemoteFilePermission parentPem = pm.getUserPermission(getParentPath(path));
//	        		if (!parentPem.getPermission().equals(PermissionType.NONE) && parentPem.isRecursive()) {
//	        			return new RemoteFilePermission(logicalFileId, apiUsername, internalUsername, parentPem.getPermission(), false);
//	        		} else {
//	        			// deny them access to files outside of their home they have not
//	        			// been granted permission to
//	        			return new RemoteFilePermission(logicalFileId, apiUsername, internalUsername, PermissionType.NONE, false);
//	        		}
        		}
        	}
        }
        else // not publicly available system
        {
        	// mirroring is turned on, so we should delegate to the system
        	// in this situation. The exception being if the path or one of its parent folders
			// was given public or world permissions. In this case, the 
			// RemoteFilePermissions trump the system permissions.
			if (remoteDataClient != null)
        	{
				// no logical file, check for public or world pems, or readonly system
				if (logicalFile == null) 
				{
					// find the closest known parent and check for recursive permisisons
					LogicalFile parent = LogicalFileDao.findClosestParent(remoteSystem, systemAbsolutePath);
					// parent was found
					if (parent != null)
					{
						// make sure parent is still within the system root directory tree
						if (isUnderSystemRootDir(parent.getPath() + "/"))
						{
							// get the public user permissions on the parent. this will resolve user,
							// public, and world permissions along with readonly system status.
							// we use the public user because we are mirroring and do not want
							// to use the user specific api permissions since we will be deferring
							// to the remote system.
							RemoteFilePermission parentPem = getGrantedUserPermissionForLogicalFile(parent, Settings.PUBLIC_USER_USERNAME);
							// if permissions are found and they are recursive, apply here
							// otherwise defer to the system.
							if (!parentPem.getPermission().equals(PermissionType.NONE) && parentPem.isRecursive())
							{
								return parentPem;
							}
						}
						// file has no pems, and the parent is outside the current system root
    					else
    					{
    						return new RemoteFilePermission(logicalFileId, apiUsername, internalUsername, PermissionType.NONE, false);
    					}
					}
				}
				// logical file existed already, check permissions
				else
				{
					// look up public user permissions for the path. this will resolve user,
					// public, and world permissions along with readonly system status.
					// we use the public user because we are mirroring and do not want
					// to use the user specific api permissions since we will be deferring
					// to the remote system.
					RemoteFilePermission userPem = getGrantedUserPermissionForLogicalFile(logicalFile, Settings.PUBLIC_USER_USERNAME);
					if (userPem.getPermission().equals(PermissionType.NONE))
					{
						// find the closest known parent and check for recursive permisisons
						LogicalFile parent = LogicalFileDao.findClosestParent(remoteSystem, systemAbsolutePath);
						if (parent != null)
						{
							// make sure parent is still within the system root directory tree
							if (isUnderSystemRootDir(parent.getPath() + "/"))
							{
								// get the public user permissions on the parent. this will resolve user,
								// public, and world permissions along with readonly system status.
								// we use the public user because we are mirroring and do not want
								// to use the user specific api permissions since we will be deferring
								// to the remote system.
								RemoteFilePermission parentPem = getGrantedUserPermissionForLogicalFile(parent, Settings.PUBLIC_USER_USERNAME);
								// if permissions are found and they are recursive, apply here
								// otherwise defer to the system.
								if (!parentPem.getPermission().equals(PermissionType.NONE) && parentPem.isRecursive())
								{
									return parentPem;
								}
							}
							// file has no pems, and the parent is outside the current system root
	    					else
	    					{
	    						return new RemoteFilePermission(logicalFileId, apiUsername, internalUsername, PermissionType.NONE, false);
	    					}
						}
					}
				}
        		
				try 
				{
					// at this point we know the data wasn't exposed via the API through publishing or 
					// as a readonly file system. We can defer to the remote system for the user's permissions
					// on this path
					String agavePath = null;
					if (logicalFile == null) {
						agavePath = new LogicalFile(apiUsername, remoteSystem, systemAbsolutePath).getAgaveRelativePathFromAbsolutePath();
					} else {
						agavePath = logicalFile.getAgaveRelativePathFromAbsolutePath();
					}
        			PermissionType permissionType = remoteDataClient.getPermissionForUser(remoteUsername, agavePath);
        			if (!permissionType.equals(PermissionType.NONE) && userRole.isGuest()) {
						return new RemoteFilePermission(logicalFileId, apiUsername, internalUsername, PermissionType.READ, true);
					} else {
						return new RemoteFilePermission(logicalFileId, apiUsername, internalUsername, permissionType, true);
					}
				} 
				catch (Exception e) 
				{
					throw new PermissionException(e);
				}
        	}
			// the path is known, so look up the permissions of the path and, if necessary,
    		// the closest parent to check for recursive permissions
    		else if (logicalFile != null)
        	{	
    			// because this is a private system, the user will always have at least
    			// read permission on any system path. If they did not have access to this
    			// system, they would not have gotten this far.
    			RemoteFilePermission userPem = getGrantedUserPermissionForLogicalFile(logicalFile, apiUsername);
    			LogicalFile parent = LogicalFileDao.findClosestParent(remoteSystem, systemAbsolutePath);
				if (parent == null) 
				{
					return userPem;
				} 
				// parent was found. make sure parent is still within the system root directory tree
				else if (isUnderSystemRootDir(parent.getPath() + "/"))
				{
					// get the user permissions on the parent. this will resolve user,
					// public, and world permissions along with readonly system status.
					RemoteFilePermission parentPem = getGrantedUserPermissionForLogicalFile(parent, apiUsername);
					// if permissions are found and they are recursive, apply here
					if (parentPem.isRecursive())
					{
						if (userPem.getPermission().getUnixValue() >= parentPem.getPermission().getUnixValue()) {
							return userPem;
						} else {
							return parentPem;
						}
					}
					// parent permissions were not recursive. use child pems instead
					else
					{
						return userPem;
					}
				}
				// file has no pems, and the parent is outside the current system root
				else
				{
					return userPem;
				}
        	}
    		// no logical file found
    		else 
    		{
    			// find the closest known parent and check for recursive permisisons
				LogicalFile parent = LogicalFileDao.findClosestParent(remoteSystem, systemAbsolutePath);
				if (parent == null)
				{
					// no parent found and no pems for the file. we just need to check for 
					// guest role at this point
					if (userRole.isGuest()) 
					{
						return new RemoteFilePermission(logicalFileId, apiUsername, internalUsername, PermissionType.READ, true);
					} 
					// no known parent or path. user has at least user access to the system. grant all pems on the path
					else 
					{
						return new RemoteFilePermission(logicalFileId, apiUsername, internalUsername, PermissionType.ALL, false);
					}
				}
				// parent was found. make sure parent is still within the system root directory tree
				else if (isUnderSystemRootDir(parent.getPath() + "/"))
				{
					// get the user permissions on the parent. this will resolve user,
					// public, and world permissions along with readonly system status.
					RemoteFilePermission parentPem = getGrantedUserPermissionForLogicalFile(parent, apiUsername);
					// if permissions are found and they are recursive, apply here
					// otherwise defer to the system.
					if (parentPem.isRecursive()) 
					{
						return parentPem;
					} 
					// parent should have recursive guest pems, but if for whatever reason they 
					// do not, we give read access if the user has the guest role
					else if (userRole.isGuest()) 
					{
						return new RemoteFilePermission(logicalFileId, apiUsername, internalUsername, PermissionType.READ, true);
					} 
					// parent pems don't inherit. user has at least user access to the system. grant all pems on the path
					else 
					{
						return new RemoteFilePermission(logicalFileId, apiUsername, internalUsername, PermissionType.ALL, false);
					}
				}
				// we give read access if the user has the guest role
				else if (userRole.isGuest()) 
				{
					return new RemoteFilePermission(logicalFileId, apiUsername, internalUsername, PermissionType.READ, true);
				} 
				// parent pems don't inherit. user has at least user access to the system. grant all pems on the path
				else 
				{
					return new RemoteFilePermission(logicalFileId, apiUsername, internalUsername, PermissionType.ALL, false);
				}
    		}
        }
	}
	
	/**
	 * Checks whether path is the virtual home directory of the user
	 * on the system assigned to the current PermissionManager. The user's
	 * virtual home is defined as:
	 * 
	 * system.storageConfig.rootDir + / + system.storageConfig.homeDir + / username
	 * 
	 * @param path
	 * @return
	 */
	public boolean isUserHomeDirOnPublicSystem(String path)
	{
		if (remoteSystem.isPubliclyAvailable())
		{
			String homeDir = remoteSystem.getStorageConfig().getHomeDir();
			try {
				if (remoteDataClient == null) {
					homeDir = remoteSystem.getRemoteDataClient().resolvePath("");
				} else {
					homeDir = remoteDataClient.resolvePath("");
				}
			} catch (Exception e) {
				return false;
			}
			
			homeDir += "/" + apiUsername;
	        
			homeDir = homeDir.replaceAll("/+", "/");
	        
			return StringUtils.equalsIgnoreCase(cleanPath(path) , homeDir);
		}
		else
		{
			return false;
		}
	}
	
	/**
	 * Checks whether path is under the current root directory of the system 
	 * assigned to the current PermissionManager. The system root directory
	 * is defined as, system.storageConfig.rootDir
	 * 
	 * @param systemAbsolutePath
	 * @return
	 */
	public boolean isUnderSystemRootDir(String systemAbsolutePath)
	{
		String rootDir = remoteSystem.getStorageConfig().getRootDir();
		
		try {
			if (remoteDataClient == null) {
				rootDir = remoteSystem.getRemoteDataClient().resolvePath("/");
			} else {
				rootDir = remoteDataClient.resolvePath("/");
			}
		} catch (Exception e) {
			return false;
		}
		return StringUtils.startsWithIgnoreCase(cleanPath(systemAbsolutePath) , rootDir);
	}
	
	/**
	 * Checks whether path is under the virtual home directory of the user
	 * on the system assigned to the current PermissionManager. The user's
	 * virtual home is defined as:
	 * 
	 * system.storageConfig.rootDir + / + system.storageConfig.homeDir + / username
	 * 
	 * @param systemAbsolutePath
	 * @return
	 */
	public boolean isUnderUserHomeDirOnPublicSystem(String systemAbsolutePath)
	{
		if (remoteSystem.isPubliclyAvailable())
		{
			String homeDir = remoteSystem.getStorageConfig().getHomeDir();
			
			try {
				if (remoteDataClient == null) {
					homeDir = remoteSystem.getRemoteDataClient().resolvePath("");
				} else {
					homeDir = remoteDataClient.resolvePath("");
				}
			} catch (Exception e) {
				return false;
			}
			
			homeDir += "/" + apiUsername;
	        
			homeDir = homeDir.replaceAll("/+", "/");
			return StringUtils.startsWithIgnoreCase(cleanPath(systemAbsolutePath) , homeDir);
		}
		else
		{
			return false;
		}
	}
	
	/**
	 * Returns the parent folder of the current path without the trailing
	 * slash. If the parent is root, / is returned.
	 * 
	 * @param path
	 * @return
	 */
	@SuppressWarnings("unused")
	private String getParentPath(String path)
	{
		String cleanedPath = cleanPath(path);
		if (StringUtils.isEmpty(cleanedPath)) {
			return "/";
		} else {
			return FilenameUtils.getFullPathNoEndSeparator(cleanedPath);
		}
	}
	
	/**
	 * Cleans up path by normalizing and removing trailing slash. OS safe.
	 * 
	 * @param path
	 * @return Normalized path without trailing slash.
	 */
	private String cleanPath(String path)
	{
		if (StringUtils.isEmpty(path) || StringUtils.equals(path, "/")) {
			return "/";
		}
		
		String adjustedPath = path;
		if (StringUtils.endsWith(adjustedPath, "/..") || StringUtils.endsWith(adjustedPath, "/.")) {
			adjustedPath += "/";
		}
		
		if (StringUtils.startsWith(adjustedPath,"/")) {
			adjustedPath = FileUtils.normalize(adjustedPath);
		} else {
			adjustedPath = FilenameUtils.normalize(adjustedPath);
		}
		
		return adjustedPath;
	}
	
	/**
	 * Returns the known permission for this file or folder without consideration of parent permissions.
	 * The known permission is a derived value based on the user's role on the associated system and
	 * ownership of the file, and any RemoteFilePermission assigned.
	 * @param username
	 * @return RemoteFilePermission
	 * @throws PermissionException
	 */
	private RemoteFilePermission getGrantedUserPermissionForLogicalFile(LogicalFile logicalFile, String username) 
	throws PermissionException
	{
		SystemRole userRole = logicalFile.getSystem().getUserRole(username);
		
		// default to no permissions for empty users and other tenants
		if (StringUtils.isEmpty(username) || !StringUtils.equals(logicalFile.getTenantId(), TenancyHelper.getCurrentTenantId())) 
		{
			return new RemoteFilePermission(logicalFile.getId(), username, logicalFile.getInternalUsername(), PermissionType.NONE, true);
		}
		// admins have total control
		else if (userRole.canAdmin())
		{
			return new RemoteFilePermission(logicalFile.getId(), username, logicalFile.getInternalUsername(), PermissionType.ALL, true);
		}
		else
		{
			RemoteFilePermission userPem = RemoteFilePermissionDao.getByUsernameAndlogicalFileId(username, logicalFile.getId());
			RemoteFilePermission publicPem = StringUtils.equals(username, Settings.PUBLIC_USER_USERNAME) ? 
					userPem : RemoteFilePermissionDao.getByUsernameAndlogicalFileId(Settings.PUBLIC_USER_USERNAME, logicalFile.getId());
			RemoteFilePermission worldPem = StringUtils.equals(username, Settings.WORLD_USER_USERNAME) ? 
					userPem : RemoteFilePermissionDao.getByUsernameAndlogicalFileId(Settings.WORLD_USER_USERNAME, logicalFile.getId());
			
			// add up the permissions to get the aggregate permission on the file/folder. Recursion is blown here, so we just
			// set the resulting permission as recursive if any permissions are recursive.
			RemoteFilePermission aggregatePermission = RemoteFilePermission.merge(userPem, publicPem);
			aggregatePermission = RemoteFilePermission.merge(aggregatePermission, worldPem);
			
			// they must have permission on the system in order to leverage
			// any RemoteFilePermission set for them
			if (userRole.canRead())
			{
				// if they own the file, they have full permission
				if (StringUtils.equals(logicalFile.getOwner(), username))
				{
					return new RemoteFilePermission(logicalFile.getId(), username, logicalFile.getInternalUsername(), PermissionType.ALL, true);
				}
				// if they have a RemoteFilePermission set, return that
				else if (userPem != null)
				{
					// add the guest read permission of the user to the aggregate permission 
					if (userRole.isGuest()) 
					{
						RemoteFilePermission guestPem = new RemoteFilePermission(logicalFile.getId(), username, logicalFile.getInternalUsername(), PermissionType.READ, true);
						return RemoteFilePermission.merge(aggregatePermission, guestPem);
					} 
					else 
					{
						return userPem;
					}
				}
				// otherwise check for public or world permissions
				else
				{
					// if the system is readonly, meaning
					if (userRole.isGuest()) 
					{
						RemoteFilePermission guestPem = new RemoteFilePermission(logicalFile.getId(), username, logicalFile.getInternalUsername(), PermissionType.READ, true);
						return RemoteFilePermission.merge(aggregatePermission, guestPem);
					}
					// return the aggregatePermission if it is not null
					else if (aggregatePermission != null) 
					{
						return aggregatePermission;
					}
					// otherwise assign role based permissions
					else 
					{
						if (logicalFile.getSystem().isPubliclyAvailable()) {
							if (isUnderUserHomeDirOnPublicSystem(logicalFile.getPath()) || isUserHomeDirOnPublicSystem(logicalFile.getPath())) {
								return userPem = new RemoteFilePermission(logicalFile.getId(), username, logicalFile.getInternalUsername(), PermissionType.ALL, true);
							} else {
								return userPem = new RemoteFilePermission(logicalFile.getId(), username, logicalFile.getInternalUsername(), PermissionType.NONE, true);
							}
						} else {
							return userPem = new RemoteFilePermission(logicalFile.getId(), username, logicalFile.getInternalUsername(), PermissionType.ALL, true);
						}
					}
				}
			}
			// if they don't have permission on the system return the aggregate permission including public and world permissions
			else if (aggregatePermission != null) 
			{
				return aggregatePermission;
			} 
			// if they don't have permission on the system and there are no 
			else 
			{
				return new RemoteFilePermission(logicalFile.getId(), username, logicalFile.getInternalUsername(), PermissionType.NONE, true); 
			}
		}
	}
	
	/**
	 * Returns whether the user can read the given path on the system
	 * assigned to this PermissionManager. Delegates to 
	 * PermissionManager.getUserPermission(path).
	 * 
	 * @param systemAbsolutePath
	 * @return
	 * @throws PermissionException
	 */
	public boolean canRead(String systemAbsolutePath) throws PermissionException 
	{
        return getUserPermission(systemAbsolutePath).canRead();
	}
	
	/**
	 * Returns whether the user can read and write the given path on the system
	 * assigned to this PermissionManager. Delegates to 
	 * PermissionManager.getUserPermission(path).
	 * 
	 * @param systemAbsolutePath
	 * @return
	 * @throws PermissionException
	 */
	public boolean canReadWrite(String systemAbsolutePath) throws PermissionException 
	{
        RemoteFilePermission pem = getUserPermission(systemAbsolutePath);
        return pem.canRead() && pem.canWrite();
	}
	
	/**
	 * Returns whether the user can read and execute the given path on the system
	 * assigned to this PermissionManager. Delegates to 
	 * PermissionManager.getUserPermission(path).
	 * 
	 * @param systemAbsolutePath
	 * @return
	 * @throws PermissionException
	 */
	public boolean canReadExecute(String systemAbsolutePath) throws PermissionException 
	{
        RemoteFilePermission pem = getUserPermission(systemAbsolutePath);
        return pem.canRead() && pem.canExecute();
	}
	
	/**
	 * Returns whether the user can write and execute the given path on the system
	 * assigned to this PermissionManager. Delegates to 
	 * PermissionManager.getUserPermission(path).
	 * 
	 * @param systemAbsolutePath
	 * @return
	 * @throws PermissionException
	 */
	public boolean canWriteExecute(String systemAbsolutePath) throws PermissionException 
	{
        RemoteFilePermission pem = getUserPermission(systemAbsolutePath);
        return pem.canWrite() && pem.canExecute();
	}
	
	/**
	 * Returns whether the user can write the given path on the system
	 * assigned to this PermissionManager. Delegates to 
	 * PermissionManager.getUserPermission(path).
	 * 
	 * @param systemAbsolutePath
	 * @return
	 * @throws PermissionException
	 */
	public boolean canWrite(String systemAbsolutePath) throws PermissionException 
	{
		return getUserPermission(systemAbsolutePath).canWrite();
	}
	
	/**
	 * Returns whether the user can execute the given path on the system
	 * assigned to this PermissionManager. Delegates to 
	 * PermissionManager.getUserPermission(path).
	 * 
	 * @param systemAbsolutePath
	 * @return
	 * @throws PermissionException
	 */
	public boolean canExecute(String systemAbsolutePath) throws PermissionException 
	{
		return getUserPermission(systemAbsolutePath).canExecute();
	}
	
	/**
	 * Returns whether the user has all permissions for the given path 
	 * on the system assigned to this PermissionManager. Delegates to 
	 * PermissionManager.getUserPermission(systemAbsolutePath).
	 * 
	 * @param systemAbsolutePath
	 * @return
	 * @throws PermissionException
	 */
	public boolean canAll(String systemAbsolutePath) throws PermissionException 
	{
		return getUserPermission(systemAbsolutePath).getPermission().equals(PermissionType.ALL);
	}
	
	public void addReadPermission(String path, boolean recursive) throws PermissionException, LogicalFileException 
	{
		Date lastUpdatedDate = new Date();
    	
		if (logicalFile != null) 
        {
        	RemoteFilePermission pem = RemoteFilePermissionDao.getByUsernameAndlogicalFileId(apiUsername, logicalFile.getId());
        	RemoteFilePermission resolvedPem = getGrantedUserPermissionForLogicalFile(logicalFile, apiUsername);
        	PermissionType newPermissionType = resolvedPem.getPermission().add(PermissionType.READ);
        	
        	if (pem == null) {
        		pem = new RemoteFilePermission(logicalFile.getId(), apiUsername, logicalFile.getInternalUsername(), newPermissionType, recursive);
        	} else {
        		pem.setPermission(newPermissionType);
        		pem.setRecursive(recursive);
        		pem.setLastUpdated(lastUpdatedDate);
        	}
        	
        	RemoteFilePermissionDao.persist(pem);
        	logicalFile.addPermissionEvent(new FileEvent(FileEventType.PERMISSION_GRANT, 
        			"READ permission granted to " + apiUsername, 
					TenancyHelper.getCurrentEndUser()),
					pem);
        	LogicalFileDao.persist(logicalFile);
        } 
        else if (remoteDataClient == null) {
        	throw new PermissionException("Permission cannot be set on unknown file");
        }
        
        if (recursive) 
    	{
        	List<BigInteger> childIds = LogicalFileDao.findChildIds(logicalFile.getPath(), remoteSystem.getId());
        	List<Object[]> bulkEvents = new ArrayList<Object[]>();
        	for (BigInteger childId: childIds) 
        	{
        		if (childId.longValue() == logicalFile.getId()) continue;
        		
        		// we intentionally update only the db entry if it exists since the effective permission will resolve at runtime still
        		RemoteFilePermission childPem = RemoteFilePermissionDao.getByUsernameAndlogicalFileId(apiUsername, childId.longValue());
        		if (childPem == null) 
        		{
        			childPem = new RemoteFilePermission(childId.longValue(), apiUsername, null, PermissionType.READ, recursive);
        		}
        		else 
        		{
        			PermissionType resolvedChildPermissionType = childPem.getPermission().add(PermissionType.READ);
	        		childPem.setPermission(resolvedChildPermissionType);
	        		childPem.setRecursive(true);
	        		childPem.setLastUpdated(lastUpdatedDate);
        		}
        		
        		RemoteFilePermissionDao.persist(childPem);
        		
        		// send the child event as well
//            	child.addPermissionEvent(new FileEvent("PERMISSION_GRANT", 
//            			"READ permission granted to " + apiUsername,
//            			TenancyHelper.getCurrentEndUser()));
//            	LogicalFileDao.persist(child);
        		bulkEvents.add(new Object[] {
      				  childId.longValue(), 
					  FileEventType.PERMISSION_GRANT.name(), 
					  "READ permission granted for " + apiUsername,
					  apiUsername});
        	}
        	
        	FileEventDao.persistAllRaw(bulkEvents);
    	}
        
        try {
        	if (remoteDataClient !=null && remoteDataClient.isPermissionMirroringRequired()) {
        		remoteDataClient.setReadPermission(remoteUsername, path, recursive);
        	}
        } catch (RemoteDataException e) {
        	if (! (e.getCause() instanceof org.irods.jargon.core.exception.InvalidUserException)) {
        		throw new PermissionException(e);
        	}
        } catch (Exception e) {
            throw new PermissionException(e);
        }
	}
	
	public void addWritePermission(String path, boolean recursive) throws PermissionException, LogicalFileException 
	{
		Date lastUpdatedDate = new Date();
    	
		if (logicalFile != null) 
        {
        	RemoteFilePermission pem = RemoteFilePermissionDao.getByUsernameAndlogicalFileId(apiUsername, logicalFile.getId());
        	RemoteFilePermission resolvedPem = getGrantedUserPermissionForLogicalFile(logicalFile, apiUsername);
        	PermissionType newPermissionType = resolvedPem.getPermission().add(PermissionType.WRITE);
        	
        	if (pem == null) {
        		pem = new RemoteFilePermission(logicalFile.getId(), apiUsername, logicalFile.getInternalUsername(), newPermissionType, recursive);
        	} else {
        		pem.setPermission(newPermissionType);
        		pem.setRecursive(recursive);
        		pem.setLastUpdated(lastUpdatedDate);
        	}
        	
        	RemoteFilePermissionDao.persist(pem);
        	logicalFile.addPermissionEvent(new FileEvent(FileEventType.PERMISSION_GRANT, 
        			"WRITE permission granted to " + apiUsername,
        			TenancyHelper.getCurrentEndUser()), 
        			pem);
        	LogicalFileDao.persist(logicalFile);
        } 
        else if (remoteDataClient == null) {
        	throw new PermissionException("Permission cannot be set on unknown file");
        }
		
		if (recursive) 
    	{
        	List<BigInteger> childIds = LogicalFileDao.findChildIds(logicalFile.getPath(), logicalFile.getSystem().getId());
        	List<Object[]> bulkEvents = new ArrayList<Object[]>();
        	for (BigInteger childId: childIds)  
        	{
        		if (childId.longValue() == logicalFile.getId()) continue;
        		
        		// we intentionally update only the db entry if it exists since the effective permission will resolve at runtime still
        		RemoteFilePermission childPem = RemoteFilePermissionDao.getByUsernameAndlogicalFileId(apiUsername, childId.longValue());
        		if (childPem == null) 
        		{
        			childPem = new RemoteFilePermission(childId.longValue(), apiUsername, null, PermissionType.WRITE, recursive);
        		}
        		else 
        		{
        			PermissionType resolvedChildPermissionType = childPem.getPermission().add(PermissionType.WRITE);
	        		childPem.setPermission(resolvedChildPermissionType);
	        		childPem.setRecursive(true);
	        		childPem.setLastUpdated(lastUpdatedDate);
        		}
        		RemoteFilePermissionDao.persist(childPem);
        		
        		// send the child event as well
//            	child.addPermissionEvent(new FileEvent(FileEventType.PERMISSION_GRANT, 
//            			"WRITE permission granted to " + apiUsername,
//            			TenancyHelper.getCurrentEndUser()),
//            			childPem);
//            	LogicalFileDao.persist(child);
        		bulkEvents.add(new Object[] {
      				  childId.longValue(),
					  FileEventType.PERMISSION_GRANT.name(), 
					  "WRITE permission granted for " + apiUsername,
					  apiUsername});
        	}
        	
        	FileEventDao.persistAllRaw(bulkEvents);
    	}
    	
        try {
        	if (remoteDataClient !=null && remoteDataClient.isPermissionMirroringRequired()) {
        		remoteDataClient.setWritePermission(remoteUsername, path, recursive);
        	}
        } catch (RemoteDataException e) {
        	if (! (e.getCause() instanceof org.irods.jargon.core.exception.InvalidUserException)) {
        		throw new PermissionException(e);
        	}
        } catch (Exception e) {
			throw new PermissionException(e);
		}
	}
	
	public void addAllPermission(String path, boolean recursive) throws PermissionException, LogicalFileException 
	{
		Date lastUpdatedDate = new Date();
    	if (logicalFile != null) 
        {
        	RemoteFilePermission pem = RemoteFilePermissionDao.getByUsernameAndlogicalFileId(apiUsername, logicalFile.getId());
        	
        	if (pem == null) {
        		pem = new RemoteFilePermission(logicalFile.getId(), apiUsername, logicalFile.getInternalUsername(), PermissionType.ALL, recursive);
        	} else {
        		pem.setPermission(PermissionType.ALL);
        		pem.setRecursive(recursive);
        		pem.setLastUpdated(lastUpdatedDate);
        	}
        	
        	RemoteFilePermissionDao.persist(pem);
        	logicalFile.addPermissionEvent(new FileEvent(FileEventType.PERMISSION_GRANT, 
        			"OWNER permission granted to " + apiUsername,
        			TenancyHelper.getCurrentEndUser()),
        			pem);
        	LogicalFileDao.persist(logicalFile);
        } 
        else if (remoteDataClient == null) {
        	throw new PermissionException("Permission cannot be set on unknown file");
        }
    	
    	if (recursive) 
    	{
    		List<BigInteger> childIds = LogicalFileDao.findChildIds(logicalFile.getPath(), logicalFile.getSystem().getId());
        	List<Object[]> bulkEvents = new ArrayList<Object[]>();
        	for (BigInteger childId: childIds) 
        	{
        		if (childId.longValue() == logicalFile.getId()) continue;
        		
        		// we intentionally update only the db entry if it exists since the effective permission will resolve at runtime still
        		RemoteFilePermission childPem = RemoteFilePermissionDao.getByUsernameAndlogicalFileId(apiUsername, childId.longValue());
        		if (childPem == null) 
        		{
        			childPem = new RemoteFilePermission(childId.longValue(), apiUsername, null, PermissionType.ALL, recursive);
        		} 
        		else 
        		{
	        		childPem.setPermission(PermissionType.ALL);
	        		childPem.setRecursive(true);
	        		childPem.setLastUpdated(lastUpdatedDate);
        		}
        		
        		RemoteFilePermissionDao.persist(childPem);
        		
        		// send the child event as well
//            	child.addPermissionEvent(new FileEvent(FileEventType.PERMISSION_GRANT, 
//            			"OWNER permission granted to " + apiUsername,
//            			TenancyHelper.getCurrentEndUser()),
//            			childPem);
//            	LogicalFileDao.persist(child);
        		bulkEvents.add(new Object[] {
      				  childId.longValue(),
					  FileEventType.PERMISSION_GRANT.name(), 
					  "OWNER permission granted for " + apiUsername,
					  apiUsername});
        	}
        	
        	FileEventDao.persistAllRaw(bulkEvents);
    	}
    	
        try {
        	if (remoteDataClient !=null && remoteDataClient.isPermissionMirroringRequired()) {
        		remoteDataClient.setOwnerPermission(remoteUsername, path, recursive);
        	}
        } catch (RemoteDataException e) {
        	if (! (e.getCause() instanceof org.irods.jargon.core.exception.InvalidUserException)) {
        		throw new PermissionException(e);
        	}
        } catch (Exception e) {
			throw new PermissionException(e);
		}
	}
	
	public void addExecutePermission(String path, boolean recursive) throws PermissionException, LogicalFileException 
	{
		Date lastUpdatedDate = new Date();
    	
        if (logicalFile != null) 
        {
        	RemoteFilePermission pem = RemoteFilePermissionDao.getByUsernameAndlogicalFileId(apiUsername, logicalFile.getId());
        	RemoteFilePermission resolvedPem = getGrantedUserPermissionForLogicalFile(logicalFile, apiUsername);
        	PermissionType newPermissionType = resolvedPem.getPermission().add(PermissionType.EXECUTE);
        	
        	if (pem == null) {
        		pem = new RemoteFilePermission(logicalFile.getId(), apiUsername, logicalFile.getInternalUsername(), newPermissionType, recursive);
        	} else {
        		pem.setPermission(newPermissionType);
        		pem.setRecursive(recursive);
        		pem.setLastUpdated(lastUpdatedDate);
        	}
        	
        	RemoteFilePermissionDao.persist(pem);
        	logicalFile.addPermissionEvent(new FileEvent(FileEventType.PERMISSION_GRANT, 
        			"EXECUTE permission granted to " + apiUsername,
        			TenancyHelper.getCurrentEndUser()),
        			pem);
        	LogicalFileDao.persist(logicalFile);
        } 
        else if (remoteDataClient == null) { 
        	throw new PermissionException("Permission cannot be set on unknown file");
        }
        
        if (recursive) 
    	{
        	List<BigInteger> childIds = LogicalFileDao.findChildIds(logicalFile.getPath(), logicalFile.getSystem().getId());
        	List<Object[]> bulkEvents = new ArrayList<Object[]>();
        	for (BigInteger childId: childIds) 
        	{
        		if (childId.longValue() == logicalFile.getId()) continue;
        		
        		// we intentionally update only the db entry if it exists since the effective permission will resolve at runtime still
        		RemoteFilePermission childPem = RemoteFilePermissionDao.getByUsernameAndlogicalFileId(apiUsername, childId.longValue());
        		if (childPem == null) {
        			childPem = new RemoteFilePermission(childId.longValue(), apiUsername, null, PermissionType.EXECUTE, recursive);
        		}
        		else 
        		{
        			PermissionType resolvedChildPermissionType = childPem.getPermission().add(PermissionType.EXECUTE);
            		childPem.setPermission(resolvedChildPermissionType);
            		childPem.setRecursive(true);
            		childPem.setLastUpdated(lastUpdatedDate);
        		}
        		RemoteFilePermissionDao.persist(childPem);
        		
        		// send the child event as well
//            	child.addPermissionEvent(new FileEvent(FileEventType.PERMISSION_GRANT, 
//            			"EXECUTE permission granted to " + apiUsername,
//            			TenancyHelper.getCurrentEndUser()),
//            			childPem);
//            	LogicalFileDao.persist(child);
        		bulkEvents.add(new Object[] {
      				  childId.longValue(),
					  FileEventType.PERMISSION_GRANT.name(), 
					  "EXECUTE permission granted for " + apiUsername,
					  apiUsername});
        		
        	}
        	
        	FileEventDao.persistAllRaw(bulkEvents);
    	}
        
        try {
        	if (remoteDataClient !=null && remoteDataClient.isPermissionMirroringRequired()) {
        		remoteDataClient.setExecutePermission(remoteUsername, path, recursive);
        	}
        } catch (RemoteDataException e) {
        	if (! (e.getCause() instanceof org.irods.jargon.core.exception.InvalidUserException)) {
        		throw new PermissionException(e);
        	}
        } catch (Exception e) {
            throw new PermissionException(e);
        }
	}
	
	public void removeReadPermission(String path) throws PermissionException, LogicalFileException
	{
		removeReadPermission(path, true);
	}
	
	public void removeReadPermission(String path, boolean recursive) throws PermissionException, LogicalFileException 
	{
        if (logicalFile != null) 
        {
        	RemoteFilePermission pem = RemoteFilePermissionDao.getByUsernameAndlogicalFileId(apiUsername, logicalFile.getId());
        	RemoteFilePermission resolvedPem = getGrantedUserPermissionForLogicalFile(logicalFile, apiUsername);
    		PermissionType newType = resolvedPem.getPermission().remove(PermissionType.READ);
        	
        	RemoteFilePermissionDao.delete(pem);
        	if (!newType.equals(PermissionType.NONE)) {
        		RemoteFilePermissionDao.persist(new RemoteFilePermission(logicalFile.getId(), apiUsername, logicalFile.getInternalUsername(), newType, recursive));
        	}
        	logicalFile.addPermissionEvent(new FileEvent(FileEventType.PERMISSION_REVOKE, 
        			"READ permission revoked for " + apiUsername,
        			TenancyHelper.getCurrentEndUser()),
        			pem);
        	LogicalFileDao.persist(logicalFile);
        } 
        else if (remoteDataClient == null) 
        {
        	throw new PermissionException("Permission cannot be set on unknown file");
        }
        
        if (recursive) 
    	{
        	List<BigInteger> childIds = LogicalFileDao.findChildIds(logicalFile.getPath(), remoteSystem.getId());
        	List<Object[]> bulkEvents = new ArrayList<Object[]>();
        	for (BigInteger childId: childIds) 
        	{
        		if (childId.longValue() == logicalFile.getId()) continue;
        		
        		// we intentionally update only the db entry if it exists since the effective permission will resolve at runtime still
        		RemoteFilePermission childPem = RemoteFilePermissionDao.getByUsernameAndlogicalFileId(apiUsername, childId.longValue());
        		if (childPem != null)
        		{
	        		PermissionType resolvedChildPermissionType = childPem.getPermission().remove(PermissionType.READ);
	        		RemoteFilePermissionDao.delete(childPem);
	        		if (!resolvedChildPermissionType.equals(PermissionType.NONE))
	        		{
	        			RemoteFilePermission newChildPem = new RemoteFilePermission(childId.longValue(), apiUsername, null, resolvedChildPermissionType, childPem.isRecursive());
	        			RemoteFilePermissionDao.persist(newChildPem);
//	        			child.addPermissionEvent(new FileEvent(FileEventType.PERMISSION_REVOKE, 
//	        					"READ permission revoked for " + apiUsername,
//	                			TenancyHelper.getCurrentEndUser()),
//	                			newChildPem);
//	        			LogicalFileDao.persist(child);
	        			bulkEvents.add(new Object[] {
		        				  childId.longValue(),
    							  FileEventType.PERMISSION_REVOKE.name(), 
    							  "READ permission revoked for " + apiUsername,
    							  apiUsername});
	        		}
        		}
        	}
        	
        	FileEventDao.persistAllRaw(bulkEvents);
    	}

		try {
			if (remoteDataClient !=null && remoteDataClient.isPermissionMirroringRequired()) {
				remoteDataClient.removeReadPermission(remoteUsername, path, recursive);
			}
		} catch (RemoteDataException e) {
        	if (! (e.getCause() instanceof org.irods.jargon.core.exception.InvalidUserException)) {
        		throw new PermissionException(e);
        	}
        } catch (Exception e) {
			throw new PermissionException(e);
		}
	}
	
	public void removeWritePermission(String path) throws PermissionException, LogicalFileException {
		removeWritePermission(path, true);
	}
	
	public void removeWritePermission(String path, boolean recursive) throws PermissionException, LogicalFileException 
	{
        if (logicalFile != null) 
        {
        	RemoteFilePermission pem = RemoteFilePermissionDao.getByUsernameAndlogicalFileId(apiUsername, logicalFile.getId());
        	RemoteFilePermission resolvedPem = getGrantedUserPermissionForLogicalFile(logicalFile, apiUsername);
    		PermissionType newType = resolvedPem.getPermission().remove(PermissionType.WRITE);
        	
        	RemoteFilePermissionDao.delete(pem);
        	if (!newType.equals(PermissionType.NONE)) {
        		RemoteFilePermissionDao.persist(new RemoteFilePermission(logicalFile.getId(), apiUsername, logicalFile.getInternalUsername(), newType, recursive));
        		
        	}
//            LogicalFileDao.persist(logicalFile);
            logicalFile.addPermissionEvent(new FileEvent(FileEventType.PERMISSION_REVOKE, 
            		"WRITE permission revoked for " + apiUsername,
        			TenancyHelper.getCurrentEndUser()),
        			new RemoteFilePermission(logicalFile.getId(), apiUsername, logicalFile.getInternalUsername(), newType, recursive));
        	LogicalFileDao.persist(logicalFile);
        } 
        else if (remoteDataClient == null)
        {
        	throw new PermissionException("Permission cannot be set on unknown file");
        }

        if (recursive) 
    	{
        	List<BigInteger> childIds = LogicalFileDao.findChildIds(logicalFile.getPath(), remoteSystem.getId());
        	List<Object[]> bulkEvents = new ArrayList<Object[]>();
        	for (BigInteger childId: childIds) 
        	{
        		if (childId.longValue() == logicalFile.getId()) continue;
        		
        		// we intentionally update only the db entry if it exists since the effective permission will resolve at runtime still
        		RemoteFilePermission childPem = RemoteFilePermissionDao.getByUsernameAndlogicalFileId(apiUsername, childId.longValue());
        		if (childPem != null)
        		{
	        		PermissionType resolvedChildPermissionType = childPem.getPermission().remove(PermissionType.WRITE);
	        		RemoteFilePermissionDao.delete(childPem);
	        		RemoteFilePermission newChildPem = new RemoteFilePermission(childId.longValue(), apiUsername, null, resolvedChildPermissionType, childPem.isRecursive());
	        		if (!resolvedChildPermissionType.equals(PermissionType.NONE))
	        		{
	        			RemoteFilePermissionDao.persist(newChildPem);
	        		}
//	        		child.addPermissionEvent(new FileEvent(FileEventType.PERMISSION_REVOKE, 
//        					"WRITE permission revoked for " + apiUsername,
//                			TenancyHelper.getCurrentEndUser()),
//                			resolvedChildPermission);
//        			LogicalFileDao.persist(child);
	        		bulkEvents.add(new Object[] {
	        				  childId.longValue(),
							  FileEventType.PERMISSION_REVOKE.name(), 
							  "WRITE permission revoked for " + apiUsername,
							  apiUsername});
        		}
        	}
        	
        	FileEventDao.persistAllRaw(bulkEvents);
    	}

		try {
			if (remoteDataClient !=null && remoteDataClient.isPermissionMirroringRequired()) {
				remoteDataClient.removeWritePermission(remoteUsername, path, recursive);
			}
		} catch (RemoteDataException e) {
        	if (! (e.getCause() instanceof org.irods.jargon.core.exception.InvalidUserException)) {
        		throw new PermissionException(e);
        	}
        } catch (Exception e) {
			throw new PermissionException(e);
		}
	}
	
	public void removeExecutePermission(String path) throws PermissionException, LogicalFileException {
		removeExecutePermission(path, true);
	}

    public void removeExecutePermission(String path, boolean recursive) throws PermissionException, LogicalFileException 
    {
        if (logicalFile != null) 
        {
        	RemoteFilePermission pem = RemoteFilePermissionDao.getByUsernameAndlogicalFileId(apiUsername, logicalFile.getId());
        	RemoteFilePermission resolvedPem = getGrantedUserPermissionForLogicalFile(logicalFile, apiUsername);
    		PermissionType newType = resolvedPem.getPermission().remove(PermissionType.EXECUTE);
        	
        	RemoteFilePermissionDao.delete(pem);
        	RemoteFilePermission newPem = new RemoteFilePermission(logicalFile.getId(), apiUsername, logicalFile.getInternalUsername(), newType, recursive);
        	if (!newType.equals(PermissionType.NONE)) {
        		RemoteFilePermissionDao.persist(newPem);
        	}
            logicalFile.addPermissionEvent(new FileEvent(FileEventType.PERMISSION_REVOKE, 
            		"EXECUTE permission revoked for " + apiUsername,
        			TenancyHelper.getCurrentEndUser()),
        			newPem);
        	LogicalFileDao.persist(logicalFile);
        } 
        else if (remoteDataClient == null)
        {
        	throw new PermissionException("Permission cannot be set on unknown file");
        }

        if (recursive) 
    	{
        	List<BigInteger> childIds = LogicalFileDao.findChildIds(logicalFile.getPath(), remoteSystem.getId());
        	List<Object[]> bulkEvents = new ArrayList<Object[]>();
        	for (BigInteger childId: childIds) 
        	{
        		if (childId.longValue() == logicalFile.getId()) continue;
        		
        		// we intentionally update only the db entry if it exists since the effective permission will resolve at runtime still
        		RemoteFilePermission childPem = RemoteFilePermissionDao.getByUsernameAndlogicalFileId(apiUsername, childId.longValue());
        		if (childPem != null)
        		{
	        		PermissionType resolvedChildPermissionType = childPem.getPermission().remove(PermissionType.EXECUTE);
	        		RemoteFilePermissionDao.delete(childPem);
	        		RemoteFilePermission newPermission = new RemoteFilePermission(childId.longValue(), apiUsername, null, resolvedChildPermissionType, childPem.isRecursive());
	        		if (!resolvedChildPermissionType.equals(PermissionType.NONE))
	        		{
	        			RemoteFilePermissionDao.persist(newPermission);
	        		}
	        		
//        			child.addPermissionEvent(new FileEvent(FileEventType.PERMISSION_REVOKE, 
//        					"EXECUTE permission revoked for " + apiUsername,
//                			TenancyHelper.getCurrentEndUser()),
//                			newPermission);
//        			LogicalFileDao.persist(child);
	        		
	        		bulkEvents.add(new Object[] {
	        				  childId.longValue(),
							  FileEventType.PERMISSION_REVOKE.name(), 
							  "EXECUTE permission revoked for " + apiUsername,
							  apiUsername});
        		}
        		
        		
        	}
        	
        	FileEventDao.persistAllRaw(bulkEvents);
    	}

		try {
        	if (remoteDataClient != null && remoteDataClient.isPermissionMirroringRequired()) {
        		remoteDataClient.removeExecutePermission(remoteUsername, path, recursive);
        	}
        } catch (RemoteDataException e) {
        	if (! (e.getCause() instanceof org.irods.jargon.core.exception.InvalidUserException)) {
        		throw new PermissionException(e);
        	}
        } catch (Exception e) {
            throw new PermissionException(e);
        }
    }

    public void clearPermissions(boolean recursive) throws PermissionException, LogicalFileException 
    {
        if (logicalFile != null) 
        {
            if (logicalFile.getOwner().equals(apiUsername)) 
            {
            	RemoteFilePermissionDao.deleteBylogicalFileId(logicalFile.getId());
            	logicalFile.addPermissionEvent(new FileEvent(FileEventType.PERMISSION_REVOKE, 
            			"All permissions revoked",
            			TenancyHelper.getCurrentEndUser()),
            			new RemoteFilePermission(logicalFile.getId(), apiUsername, logicalFile.getInternalUsername(), PermissionType.NONE, recursive));
            	LogicalFileDao.persist(logicalFile);
            } 
            else 
            {
            	RemoteFilePermissionDao.deleteByUsernameAndlogicalFileId(apiUsername, logicalFile.getId());
            	logicalFile.addPermissionEvent(new FileEvent(FileEventType.PERMISSION_REVOKE, 
            			"All permissions revoked for " + apiUsername,
            			TenancyHelper.getCurrentEndUser()),
            			new RemoteFilePermission(logicalFile.getId(), apiUsername, logicalFile.getInternalUsername(), PermissionType.NONE, recursive)
            			);
                LogicalFileDao.persist(logicalFile);
            }
        } 
        else 
        {
        	throw new PermissionException("Permissions cannot be cleared on unknown file");
        }

        if (recursive) 
    	{
        	List<BigInteger> childIds = LogicalFileDao.findChildIds(logicalFile.getPath(), remoteSystem.getId());
        	List<Object[]> bulkEvents = new ArrayList<Object[]>();
        	for (BigInteger childId: childIds) 
        	{
        		if (childId.longValue() == logicalFile.getId()) continue;
        		
        		// we intentionally update only the db entry if it exists since the effective permission will resolve at runtime still
//        		RemoteFilePermissionDao.deleteByUsernameAndlogicalFileId(apiUsername, childId.longValue());
        		
        		bulkEvents.add(new Object[] {
        					  childId.longValue(), 
							  FileEventType.PERMISSION_REVOKE.name(), 
							  "All permissions revoked for " + apiUsername,
							  apiUsername});
//        		child.addPermissionEvent(new FileEvent(FileEventType.PERMISSION_REVOKE, 
//        				"All permissions revoked for " + apiUsername,
//            			TenancyHelper.getCurrentEndUser()),
//            			new RemoteFilePermission(child.getId(), apiUsername, child.getInternalUsername(), PermissionType.NONE, recursive));
//            	LogicalFileDao.persist(child);
        	}
        	RemoteFilePermissionDao.bulkDeleteByUsernameAndlogicalFileId(apiUsername, childIds);
        	FileEventDao.persistAllRaw(bulkEvents);
    	}

		try {
        	if (remoteDataClient != null && remoteDataClient.isPermissionMirroringRequired()) {
        		remoteDataClient.clearPermissions(remoteUsername, logicalFile.getAgaveRelativePathFromAbsolutePath(), recursive);
        	}
        } catch (RemoteDataException e) {
        	if (! (e.getCause() instanceof org.irods.jargon.core.exception.InvalidUserException)) {
        		throw new PermissionException(e);
        	}
        } catch (Exception e) {
            throw new PermissionException(e);
        }
    }

	/**
	 * Verifies the user's permission to view the file or directory at the given URI.
	 * The URI is validated for an acceptable schema. If the schema is agave, the 
	 * hostname represents a system which is used with the path to check the user's 
	 * role on the system and permissions on the file.
	 *  
	 * @param username
	 * @param internalUsername
	 * @param inputUri
	 * @return
	 * @throws PermissionException
	 */
	public static boolean canUserReadUri(String username, String internalUsername, URI inputUri) throws PermissionException 
	{
		RemoteDataClient remoteDataClient = null;
		try 
		{
			String scheme = inputUri.getScheme();
			RemoteSystem system = null;
			String path = null;
			if (AgaveUriUtil.isInternalURI(inputUri)) 
			{
				if (StringUtils.isEmpty(scheme)) {
					system = new SystemManager().getUserDefaultStorageSystem(username);
					if (system == null) {
						throw new PermissionException("No system was specified in the URL for the file "
								+ "or directory at " + inputUri + " and no default system is defined.");
					}
					// path is relative to the default storage system. Nothing to do.
					if (StringUtils.isEmpty(inputUri.getPath()) || StringUtils.equals(inputUri.getPath(), "/")) {
						path = "";
					} else {
						path = StringUtils.substring(inputUri.getPath(), 1);
					}
				}
				else if (scheme.equalsIgnoreCase("agave"))
				{
					if (StringUtils.isEmpty(inputUri.getHost())) {
						system = new SystemManager().getUserDefaultStorageSystem(username);
						if (system == null) {
							throw new PermissionException("No system was specified in the URL for the file "
									+ "or directory at " + inputUri + " and no default system is defined.");
						}
					} else {
						system = new SystemDao().findBySystemId(inputUri.getHost());
						if (system == null) {
							throw new PermissionException("No system was found matchin the hostname in the "
									+ "URL for the file or directory at " + inputUri);
						}
					}
					
					// path is relative to the default storage system. Nothing to do.
					if (StringUtils.isEmpty(inputUri.getPath()) || StringUtils.equals(inputUri.getPath(), "/")) {
						path = "";
					} else {
						path = StringUtils.substring(inputUri.getPath(), 1);
					}
				}
				else if (AgaveUriRegex.JOBS_URI.matches(inputUri)) {
				    try {
				        ApiUriUtil.getRemoteSystem(username, inputUri);
				        return true;
				    } catch (PermissionException e) {
				        throw e;
				    } catch (SystemException e) {
				        throw new PermissionException("No storage system was found matchin the "
				                + "one associated with the job output in " + inputUri);
				    }
				}
				else 
				{   
					// will throw an exception if the path isn't valid
					String systemId = PathResolver.getSystemIdFromPath(inputUri.getPath(), username);
					
					// TODO: this first condition should be dead code. if it's null, we can just throw an exception
					// since the system would have been resolved in the path resolution already.
					if (systemId == null) {
						system = new SystemManager().getUserDefaultStorageSystem(username);
						if (system == null) {
							throw new PermissionException("No system was specified in the URL for the file "
									+ "or directory at " + inputUri + " and no default system is defined.");
						}
					}
					else if (StringUtils.isEmpty(systemId))
					{
						throw new PermissionException("No system was specified in the URL for the file "
								+ "or directory at " + inputUri + " and no default system is defined.");
					} 
					else 
					{
						system = new SystemDao().findBySystemId(systemId);
						if (system == null) {
							throw new PermissionException("No system was found matchin the hostname in the "
									+ "URL for the file or directory at " + inputUri);
						}
					}
					
					path = PathResolver.resolve(null, inputUri.getPath());
				}
				
				remoteDataClient = system.getRemoteDataClient(internalUsername);
				LogicalFile lf = LogicalFileDao.findBySystemAndPath(system, remoteDataClient.resolvePath(path));
				
				PermissionManager pm = new PermissionManager(system, remoteDataClient, lf, username);
				return pm.canRead(remoteDataClient.resolvePath(path));
			}
			else if (RemoteDataClientFactory.isSchemeSupported(inputUri))
			{
				remoteDataClient = new RemoteDataClientFactory().getInstance(username, internalUsername, inputUri);
				remoteDataClient.authenticate();
				return true;
			}
			else { 
				return false;
			}
		}
		catch (AgaveNamespaceException e) {
			return false;
		}
		catch (PermissionException e) {
			throw e;
		}
		catch (RemoteDataException e) {
			throw new PermissionException("The server experienced an error attempting to "
					+ "identify the file or directory at " + inputUri, e);
		}
		catch (Exception e) {
			throw new PermissionException("Unable to verify that the user has permission "
					+ "to access the file or directory at " + inputUri, e);
		}
		finally {
			try { remoteDataClient.disconnect();} catch (Exception e) {}
		}
	}
}
