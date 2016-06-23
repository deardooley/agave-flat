package org.iplantc.service.systems.manager;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.cfg.NotYetImplementedException;
import org.iplantc.service.common.Settings;
import org.iplantc.service.common.auth.AuthorizationHelper;
import org.iplantc.service.common.exceptions.EntityEventPersistenceException;
import org.iplantc.service.common.exceptions.PermissionException;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.common.uuid.AgaveUUID;
import org.iplantc.service.common.uuid.UUIDType;
import org.iplantc.service.remote.RemoteSubmissionClient;
import org.iplantc.service.remote.exceptions.RemoteExecutionException;
import org.iplantc.service.systems.dao.SystemDao;
import org.iplantc.service.systems.events.RemoteSystemEventProcessor;
import org.iplantc.service.systems.events.SystemHistoryEventDao;
import org.iplantc.service.systems.exceptions.EncryptionException;
import org.iplantc.service.systems.exceptions.SystemArgumentException;
import org.iplantc.service.systems.exceptions.SystemException;
import org.iplantc.service.systems.model.AuthConfig;
import org.iplantc.service.systems.model.CredentialServer;
import org.iplantc.service.systems.model.ExecutionSystem;
import org.iplantc.service.systems.model.LoginConfig;
import org.iplantc.service.systems.model.RemoteSystem;
import org.iplantc.service.systems.model.StorageConfig;
import org.iplantc.service.systems.model.StorageSystem;
import org.iplantc.service.systems.model.SystemRole;
import org.iplantc.service.systems.model.enumerations.AuthConfigType;
import org.iplantc.service.systems.model.enumerations.CredentialServerProtocolType;
import org.iplantc.service.systems.model.enumerations.RemoteSystemType;
import org.iplantc.service.systems.model.enumerations.RoleType;
import org.iplantc.service.systems.model.enumerations.SystemEventType;
import org.iplantc.service.systems.model.enumerations.SystemStatusType;
import org.iplantc.service.systems.util.ServiceUtils;
import org.json.JSONObject;

public class SystemManager {
	
	private static final Logger log = Logger.getLogger(SystemManager.class);
	
	private SystemDao dao;
	private RemoteSystemEventProcessor eventProcessor;
	
	public SystemManager() {
		setDao(new SystemDao());
		setEventProcessor(new RemoteSystemEventProcessor());
	}
	
	/**
	 * @return the dao
	 */
	public SystemDao getDao() {
		return dao;
	}

	/**
	 * @param dao the dao to set
	 */
	public void setDao(SystemDao dao) {
		this.dao = dao;
	}

	/**
	 * @return the eventProcessor
	 */
	public RemoteSystemEventProcessor getEventProcessor() {
		return eventProcessor;
	}

	/**
	 * @param eventProcessor the eventProcessor to set
	 */
	public void setEventProcessor(RemoteSystemEventProcessor eventProcessor) {
		this.eventProcessor = eventProcessor;
	}

	/**
	 * Returns true if the login configuration is valid and can be used
	 * to connect to the remote system.
	 * 
	 * @param loginConfig
	 * @return true if a connection can be established, false otherwise
	 * @throws SystemException
	 */
	public boolean isLoginConfigValid(LoginConfig loginConfig) 
	{
		if (loginConfig == null) {
			throw new SystemException("No login configuration provided.");
		} 
		
		try {
			loginConfig.testConnection();
			
			return true;
		} catch (Exception e) {
			return false;
		}
	}
	
	/**
	 * Returns true if the storage configuration is valid and can be used
	 * to connect to the remote system.
	 *
	 * @param storageConfig
	 * @return true if a connection can be established, false otherwise
	 * @throws SystemException
	 */
	public boolean isDataConfigValid(StorageConfig storageConfig) 
	{
		if (storageConfig == null) {
			throw new SystemException("No storage configuration provided.");
		} 
		
		try {
			storageConfig.testConnection();
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Returns true if the api user has a role sufficient to perform management
	 * tasks such as updating and granting roles on this system.
	 * 
	 * @param system
	 * @param apiUsername
	 * @return
	 */
	public boolean isManageableByUser(RemoteSystem system, String apiUsername)
	{
		if (StringUtils.isEmpty(apiUsername)) {
			throw new SystemException("No username provided.");
		}
		else if (system == null) {
			throw new SystemException("No shared system found matching that name");
		}
		else if (StringUtils.equals(apiUsername, Settings.PUBLIC_USER_USERNAME) ||
    			StringUtils.equals(apiUsername, Settings.WORLD_USER_USERNAME)) {
    		return false;
    	}
		else {
			SystemRole role = system.getUserRole(apiUsername);
			if (system.isPubliclyAvailable()) {
				return ServiceUtils.isAdmin(apiUsername);
			} 
			else {
				return (ServiceUtils.isAdmin(apiUsername) || 
						system.isOwnedBy(apiUsername) ||
						role.canAdmin());
			}
		}
	}
	
	/**
	 * Creates a new RemoteSystem object from the json object.
	 * 
	 * @param json
	 * @param apiUsername
	 * @return RemoteSystem
	 * @throws SystemException
	 * @throws PermissionException 
	 */
	public RemoteSystem parseSystem(JSONObject json, String apiUsername) 
	throws SystemException, PermissionException
	{
		return parseSystem(json, apiUsername, null);
	}
	
	/**
	 * Creates a new RemoteSystem object from the json object. If the passed in system is 
	 * not null, it is updated with the values from the json object and id is preserved.
	 * @param json
	 * @param apiUsername
	 * @param system
	 * @return RemoteSystem
	 * @throws SystemException
	 * @throws PermissionException 
	 */
	public RemoteSystem parseSystem(JSONObject json, String apiUsername, RemoteSystem system) 
	throws SystemException, PermissionException
	{
		if (StringUtils.isEmpty(apiUsername)) {
			throw new SystemException("No username provided.");
		}
		
		if (system != null && system.getUserRole(apiUsername).getRole().equals(RoleType.NONE)) {
			throw new PermissionException("User does not have permission to update this system.");
		}
		
		if (json == null) 
		{
			throw new SystemException("No system description provided");
		} 
		else if (json.has("type")) {
			try 
			{
				RemoteSystemType type = RemoteSystemType.valueOf(json.getString("type").toUpperCase());
			
				if (type.equals(RemoteSystemType.EXECUTION)) 
				{
					if (system == null) 
					{
						system = new ExecutionSystem();
						system.setOwner(apiUsername);
						system.setUuid(new AgaveUUID(UUIDType.SYSTEM).toString());
					} else if (!(system instanceof ExecutionSystem)) {
						throw new SystemException("System " + system.getSystemId() + 
								" already exists as a storage system. Once declared, "
								+ "systems cannot be redefined as a different type.");
					} 
					
					system = ExecutionSystem.fromJSON(json, (ExecutionSystem)system);
				}
				else if (type.equals(RemoteSystemType.STORAGE)) 
				{
					if (system == null) 
					{
						system = new StorageSystem();
						system.setOwner(apiUsername);
						system.setUuid(new AgaveUUID(UUIDType.SYSTEM).toString());
					} 
					else if (!(system instanceof StorageSystem)) 
					{
						throw new SystemException("System " + system.getSystemId() + 
								" already exists as an execution system. Once declared, "
								+ "systems cannot be redefined as a different type.");
					}
					
					system = StorageSystem.fromJSON(json, (StorageSystem)system);
					
				} 
				else 
				{
					throw new SystemException("Invalid 'type' value. " +
							"Please specify one of: " + ServiceUtils.explode(",",Arrays.asList(RemoteSystemType.values())));
				}
				
//				system.setOwner(owner);
				system.setLastUpdated(new Date());
				return system;
			} 
			catch (SystemException e) {
				throw e;
			} 
			catch (Exception e) {
				throw new SystemException("Invalid 'type' value. " +
						"Please specify one of: " + ServiceUtils.explode(",",Arrays.asList(RemoteSystemType.values())));
			}
		} else {
			throw new SystemException("No 'type' value specified. " +
					"Please specify one of: " + ServiceUtils.explode(",",Arrays.asList(RemoteSystemType.values())));
		}
	}

	/**
	 * Returns true if the api user has a role of RoleType.USER or higher.
	 * @param system
	 * @param apiUsername
	 * @return true if the api user has a role of RoleType.USER or higher. false otherwise
	 * @throws SystemException
	 */
	public boolean isVisibleByUser(RemoteSystem system, String apiUsername)
	{
		if (StringUtils.isEmpty(apiUsername)) {
			throw new SystemException("No username provided.");
		} 
		
		if (system == null) {
			throw new SystemException("No shared system found matching that name");
		} 
		else 
		{	
			return (ServiceUtils.isAdmin(apiUsername) || 
					system.getOwner().equalsIgnoreCase(apiUsername) ||
					system.isPubliclyAvailable() || 
					system.getUserRole(apiUsername).canUse());
		}
	}
	
	/**
	 * Sets the publiclyAvailable flag on the RemoteSystem to true. This is a 
	 * pure db operation and no apps or data are associated with it. Also, it
	 * does not automatically make this the global default system of this type. 
	 * That must be done separately by a super admin using the SystemManager.updateSystem()
	 * method.
	 * 
	 * @param system system to be published
	 * @param apiUsername user calling this action
	 * @return
	 * @throws SystemException
	 */
	public RemoteSystem publish(RemoteSystem system, String apiUsername) 
	throws SystemException
	{
		if (system == null) {
			throw new SystemException("No system provided.");
		} else if (system.isPubliclyAvailable()) {
			return system;
		} 
		
		system.setPubliclyAvailable(true);
		system.setLastUpdated(new Date());
		system.setRevision(system.getRevision() + 1);
		getDao().persist(system);
		
		getEventProcessor().processPublishEvent(system, SystemEventType.PUBLISH, apiUsername);
		
		return system;
	}
	
	/**
	 * Sets the publiclyAvailable flag on the RemoteSystem to false and triggers an event. This is a 
	 * pure db operation and no apps or data are associated with it. Also, it does not set a default 
	 * replacement system. That must be done separately by a super admin using the 
	 * SystemManager.updateSystem() method. If the system is the globalDefault, it will not be after 
	 * this occurs.
	 * 
	 * @param system system to be unpublished
	 * @param apiUsername user calling this action
	 * @throws SystemException
	 */
	public RemoteSystem unpublish(RemoteSystem system, String apiUsername) 
	throws SystemException
	{
		if (system == null) {
			throw new SystemException("No system provided.");
		} else if (!system.isPubliclyAvailable()) {
			return system;
		} 
		
		system.setPubliclyAvailable(false);
		system.setGlobalDefault(false);
		system.setLastUpdated(new Date());
		system.setRevision(system.getRevision() + 1);
		getDao().persist(system);
		
		getEventProcessor().processPublishEvent(system, SystemEventType.UNPUBLISH, apiUsername);
//		NotificationManager.process(system.getUuid(), "UNPUBLISH", apiUsername);
		
		return system;
	}

	/**
	 * Creates a deep clone of the given RemoteSystem and persists it as the
	 * user's own. This is only a database operation and no applications or
	 * data will be replicated in this process.
	 * 
	 * @param system
	 * @param apiUsername
	 * @param systemId
	 * @return
	 * @throws SystemArgumentException
	 * @throws PermissionException 
	 */
	public RemoteSystem cloneSystem(RemoteSystem system, String apiUsername, String systemId) 
	throws SystemArgumentException, PermissionException
	{
		if (system == null) {
			throw new SystemArgumentException("No system provided.");
		} else if (system.getUserRole(apiUsername).getRole().equals(RoleType.NONE)) {
			throw new PermissionException("User does not have permission to clone this system.");
		}
		
		if (StringUtils.isEmpty(apiUsername)) {
			throw new SystemArgumentException("No username provided.");
		}
		
		if (StringUtils.isEmpty(systemId)) {
			throw new SystemArgumentException("No new system id provided. Please supply a unique id for the new system.");
		} else if (!getDao().isSystemIdUnique(systemId)) {
			throw new SystemArgumentException("Cloned system id is already assigned to another system. " +
					"Please supply a unique id for the new system.");
		}
		
		RemoteSystem cloneSystem = system.clone();
		
		cloneSystem.setOwner(apiUsername);
		cloneSystem.setSystemId(systemId);
		cloneSystem.setPubliclyAvailable(false);
		cloneSystem.setRevision(1);
		cloneSystem.getUsersUsingAsDefault().clear();
		cloneSystem.getRoles().clear();
		cloneSystem.setGlobalDefault(false);
		
		getDao().persist(cloneSystem);
		
		return cloneSystem;
	}
	
	/**
	 * Updates the existing system with the values from the new one. The revision flag will
	 * be updated and all passwords will be reencrypted. The id and system id will remain unchanged.
	 * 
	 * @param existingSystem
	 * @param newSystem
	 * @return
	 * @throws SystemException
	 */
	public RemoteSystem udpateExistingSystem(RemoteSystem existingSystem, RemoteSystem newSystem) throws SystemException
	{
		if (existingSystem == null) {
			throw new SystemException("No existing system provided.");
		}
		
		if (newSystem == null) {
			throw new SystemException("No new system provided.");
		}
		
		if (existingSystem.getType().equals(RemoteSystemType.EXECUTION)) {
			AuthConfig loginAuthConfig = ((ExecutionSystem)existingSystem).getLoginConfig().getDefaultAuthConfig();
			if (loginAuthConfig.getCredentialServer() != null) {
				((ExecutionSystem)existingSystem).getLoginConfig().getAuthConfigs().remove(loginAuthConfig);
			}
			AuthConfig storageAuthConfig = existingSystem.getStorageConfig().getDefaultAuthConfig();
			if (storageAuthConfig.getCredentialServer() != null) {
				existingSystem.getStorageConfig().getAuthConfigs().remove(storageAuthConfig);
			}
			getDao().persist(existingSystem);
		} else if (existingSystem.getType().equals(RemoteSystemType.STORAGE)) {
			AuthConfig storageAuthConfig = existingSystem.getStorageConfig().getDefaultAuthConfig();
			if (storageAuthConfig.getCredentialServer() != null) {
				existingSystem.getStorageConfig().getAuthConfigs().remove(storageAuthConfig);
			}
			getDao().persist(existingSystem);
		}
		 
		newSystem.setId(existingSystem.getId());
 		newSystem.setRevision(existingSystem.getRevision() + 1);
 		newSystem.setCreated(existingSystem.getCreated());
 		newSystem.setPubliclyAvailable(existingSystem.isPubliclyAvailable());
 		newSystem.setLastUpdated(new Date());
 		getDao().merge(newSystem);
 		getEventProcessor().processSystemUpdateEvent(newSystem, SystemEventType.UPDATED, newSystem.getOwner());
// 		NotificationManager.process(newSystem.getUuid(), "UPDATE", newSystem.getOwner());
 		return newSystem;
	}

	/**
	 * Returns the default storage system for the given api user. If they have
	 * not defined one of their own, it defaults to the API default storage system,
	 * currently the iPlant Data Store.
	 * 
	 * @param apiUsername
	 * @return StorageSystem
	 * @throws SystemException
	 */
	public StorageSystem getUserDefaultStorageSystem(String apiUsername)
	throws SystemException
	{
		if (StringUtils.isEmpty(apiUsername)) {
			return getDefaultStorageSystem();
			//throw new SystemException("No username provided.");
		}
		
		RemoteSystem system = getDao().findUserDefaultSystem(apiUsername, RemoteSystemType.STORAGE);
		if (system == null) {
			system = getDao().getGlobalDefaultSystemForTenant(RemoteSystemType.STORAGE, TenancyHelper.getCurrentTenantId());
		}
		
		return (StorageSystem)system;
	}
	
	/**
	 * Returns the default execution system for the given api user. If they have
	 * not defined one of their own, it defaults to the API default execution system,
	 * currently Lonestar.
	 * 
	 * @param apiUsername
	 * @return StorageSystem
	 * @throws SystemException
	 */
	public ExecutionSystem getUserDefaultExecutionSystem(String apiUsername)
	throws SystemException
	{
		if (StringUtils.isEmpty(apiUsername)) {
			return getDefaultExecutionSystem();
			//throw new SystemException("No username provided.");
		}
		
		RemoteSystem system = getDao().findUserDefaultSystem(apiUsername, RemoteSystemType.EXECUTION);
		
		if (system == null) {
			system = getDao().getGlobalDefaultSystemForTenant(RemoteSystemType.EXECUTION, TenancyHelper.getCurrentTenantId());
		}
		
		return (ExecutionSystem)system;
	}
	
	/**
	 * Returns the default storage and execution system for the given api user. If they have
	 * not defined one of their own, it defaults to the API default systems,
	 * currently the iPlant Data Store and Lonestar.
	 * 
	 * @param apiUsername
	 * @return StorageSystem
	 * @throws SystemException
	 */
	public List<RemoteSystem> getUserDefaultSystems(String apiUsername)
	throws SystemException
	{
		if (StringUtils.isEmpty(apiUsername)) {
			throw new SystemException("No username provided.");
		}
		
		RemoteSystem defaultStorage = getDao().findUserDefaultSystem(apiUsername, RemoteSystemType.STORAGE);
		if (defaultStorage == null) {
			defaultStorage = getDao().getGlobalDefaultSystemForTenant(RemoteSystemType.STORAGE, TenancyHelper.getCurrentTenantId());
		}
		
		RemoteSystem defaultExecution = getDao().findUserDefaultSystem(apiUsername, RemoteSystemType.EXECUTION);
		if (defaultExecution == null) {
			defaultExecution = getDao().getGlobalDefaultSystemForTenant(RemoteSystemType.EXECUTION, TenancyHelper.getCurrentTenantId());
		}
		
		return Arrays.asList(defaultStorage, defaultExecution);
	}
	
	public RemoteSystem getPlatformStorageSystem()
	throws SystemException
	{
		return getDao().getPlatformStorageSystem();
	}

	/**
	 * Updates the user's default system by setting it to the given system. The
	 * entry in the previous system's RemtoeSystem.usersUsingAsDefault list will
	 * be deleted and one will be added to the new system.
	 * 
	 * @param apiUsername
	 * @param newDefaultSystem
	 * @throws PermissionException 
	 * @throws SystemArgumentException 
	 */
	public RemoteSystem setUserDefaultSystem(String apiUsername, RemoteSystem newDefaultSystem) 
	throws SystemException, PermissionException, SystemArgumentException
	{
		if (newDefaultSystem == null) {
			throw new SystemException("No new default system specified");
		} else if (!newDefaultSystem.getUserRole(apiUsername).getRole().canUse()) {
			throw new PermissionException("User does not have permission to use this system.");
		}
		
		if (StringUtils.isEmpty(apiUsername)) {
			throw new SystemException("No username provided.");
		}
		
		RemoteSystem currentDefaultSystem = getDao().findUserDefaultSystem(apiUsername, newDefaultSystem.getType());
		
		if (currentDefaultSystem == null) {
			if (newDefaultSystem.isPubliclyAvailable()) {
				// don't need to record this, it's the default
				return newDefaultSystem;
			} else {
				// just add the new permission
				newDefaultSystem.addUserUsingAsDefault(apiUsername);
				getDao().merge(newDefaultSystem);
			}
		} else if (!newDefaultSystem.equals(currentDefaultSystem)) {
			// now delete the old one and add the new one
			currentDefaultSystem.getUsersUsingAsDefault().remove(apiUsername);
			getEventProcessor().processSystemUpdateEvent(currentDefaultSystem, SystemEventType.UNSET_USER_DEFAULT, apiUsername);
//			NotificationManager.process(currentDefaultSystem.getUuid(), "UNSET_DEFAULT_SYSTEM", apiUsername);
			getDao().merge(currentDefaultSystem);
			newDefaultSystem.getUsersUsingAsDefault().add(apiUsername);
			getEventProcessor().processSystemUpdateEvent(currentDefaultSystem, SystemEventType.SET_USER_DEFAULT, apiUsername);
//			NotificationManager.process(newDefaultSystem.getUuid(), "SET_DEFAULT_SYSTEM", apiUsername);
			getDao().merge(newDefaultSystem);
		}
		
		return newDefaultSystem;
	}
	
	
	public RemoteSystem unsetUserDefaultSystem(String apiUsername, RemoteSystem currentDefaultSystem) 
	throws SystemException, PermissionException, SystemArgumentException
	{
		if (currentDefaultSystem == null) {
			throw new SystemException("No new default system specified");
		} 
		
		if (StringUtils.isEmpty(apiUsername)) {
			throw new SystemException("No username provided.");
		}
		
		RemoteSystem savedDefaultSystem = null;
		if (currentDefaultSystem.getType() == RemoteSystemType.STORAGE) {
			savedDefaultSystem = getUserDefaultStorageSystem(apiUsername);
		} else {
			savedDefaultSystem = getUserDefaultExecutionSystem(apiUsername);
		}
		
		if (savedDefaultSystem != null && StringUtils.equals(savedDefaultSystem.getSystemId(), currentDefaultSystem.getSystemId())) 
		{
			currentDefaultSystem.getUsersUsingAsDefault().remove(apiUsername);
			getDao().merge(currentDefaultSystem);
			getEventProcessor().processSystemUpdateEvent(currentDefaultSystem, SystemEventType.UNSET_USER_DEFAULT, apiUsername);
//			NotificationManager.process(currentDefaultSystem.getUuid(), "UNSET_DEFAULT_SYSTEM", apiUsername);
		}
		else
		{
			throw new SystemException("Cannot change user default status of " + currentDefaultSystem.getSystemId() + 
					". The system is not the default system of the user.");
		}
		
		return currentDefaultSystem;
	}
	
	
	/**
	 * Removes the user from all RemtoeSystem.usersUsingAsDefault lists and
	 * thus resets them to the system defaults.
	 * 
	 * @param apiUsername
	 * @throws SystemArgumentException 
	 */
	public void clearUserDefaultSystems(String apiUsername) throws SystemArgumentException
	{
		if (StringUtils.isEmpty(apiUsername)) {
			throw new SystemArgumentException("No username provided.");
		}
		
		for(RemoteSystem currentDefaultSystem: getDao().findUserDefaultSystems(apiUsername)) 
		{
			currentDefaultSystem.getUsersUsingAsDefault().remove(apiUsername);
			getDao().merge(currentDefaultSystem);
			getEventProcessor().processSystemUpdateEvent(currentDefaultSystem, SystemEventType.UNSET_USER_DEFAULT, apiUsername);
//			NotificationManager.process(currentDefaultSystem.getUuid(), "UNSET_DEFAULT_SYSTEM", apiUsername);
		}
	}
	

	/**
	 * Looks up the system $WORK directly through direct login if not available as a 
	 * static attribute of the system description.
	 * 
	 * @param system
	 * @return
	 * @throws Exception
	 * @deprecated This is no longer the preferred method of finding the work directory since it is not consistent and the RemoteDataClient needs to resolve the path independently.
	 */
	public static String getSystemWorkDir(ExecutionSystem system, String internalUsername) 
	throws Exception
	{
		if (StringUtils.isEmpty(system.getWorkDir()))
		{
			
			RemoteSubmissionClient client = null;
			try {
				client = system.getRemoteSubmissionClient(internalUsername);
				String workDir = client.runCommand(". .bashrc 2&>1 /dev/null; `which echo` $WORK");
				
				if (!ServiceUtils.isValid(workDir)) {
					throw new IOException("Failed to determine remote scrach directory on " + 
							system.getSystemId());
				} 
				else
				{
					workDir = workDir.replaceAll("\n", "").replaceAll("\\n", "");
					
					if (!workDir.endsWith("/")) workDir += "/";
					
					return workDir;
				}
			} catch (Exception e) {
				throw new RemoteExecutionException("Failed to retrieve $WORK directory of " + 
						system.getSystemId(), e);
			} finally {
				try { client.close(); } catch (Exception e) {}
			}
		} else {
			return system.getWorkDir();
		}
	}
	
	/**
	 * Looks up the system $SCRATCH directly through direct login if not available as a 
	 * static attribute of the system description.
	 * 
	 * @param system
	 * @return
	 * @throws Exception
	 * @deprecated This is no longer the preferred method of finding the scratch directory since it is not consistent and the RemoteDataClient needs to resolve the path independently.
	 */
	public static String getSystemScratchDir(ExecutionSystem system, String internalUsername) 
	throws Exception
	{
		if (system == null) {
			throw new SystemArgumentException("No system provided.");
		} else if (system.isPubliclyAvailable()) {
			throw new SystemArgumentException("Internal users are not supported on public systems.");
		}
		
//		if (StringUtils.isEmpty(internalUsername)) {
//			throw new SystemArgumentException("No internal username provided.");
//		}
		
		if (StringUtils.isEmpty(system.getScratchDir()))
		{
			
			RemoteSubmissionClient client = null;
			try {
				client = system.getRemoteSubmissionClient(internalUsername);
				String scratchDir = client.runCommand(". .bashrc 2&>1 /dev/null; `which echo` $SCRATCH");
				
				if (!ServiceUtils.isValid(scratchDir)) {
					throw new IOException("Failed to determine remote scrach directory on " + system.getSystemId());
				} 
				else
				{
					scratchDir = scratchDir.replaceAll("\n", "").replaceAll("\\n", "");
					
					if (!scratchDir.endsWith("/")) scratchDir += "/";
					
					return scratchDir;
				}
			} catch (Exception e) {
				throw new RemoteExecutionException("Failed to retrieve $SCRATCH directory of " + system.getSystemId(), e);
			} finally {
				try { client.close(); } catch (Exception e) {}
			}
		} else {
			return system.getScratchDir();
		}
	}
	
	public StorageSystem getDefaultStorageSystem()
	{
		return (StorageSystem)getDao().getGlobalDefaultSystemForTenant(RemoteSystemType.STORAGE, TenancyHelper.getCurrentTenantId());
	}
	
	public ExecutionSystem getDefaultExecutionSystem()
	{
		return (ExecutionSystem)getDao().getGlobalDefaultSystemForTenant(RemoteSystemType.EXECUTION, TenancyHelper.getCurrentTenantId());
	}
	
	
	/**
	 * Deletes the stored AuthConfig for an internal user on every system of the 
	 * given api user.
	 * 
	 * @param apiUsername
	 * @param internalUsername
	 * @throws SystemArgumentException
	 */
	public void removeAllInternalUserAuthConfig(String apiUsername, String internalUsername) 
	throws SystemArgumentException 
	{
		removeAllInternalUserAuthConfigOfType(apiUsername, internalUsername, "storage");
		removeAllInternalUserAuthConfigOfType(apiUsername, internalUsername, "login");
	}
	
	/**
	 * Deletes the stored AuthConfig for a given RemoteConfig type for an internal 
	 * user on every system of the given api user.
	 * 
	 * @param apiUsername
	 * @param internalUsername
	 * @throws SystemArgumentException
	 */
	public void removeAllInternalUserAuthConfigOfType(String apiUsername, 
			String internalUsername, String remoteConfigType) 
	throws SystemArgumentException 
	{
		if (StringUtils.isEmpty(apiUsername)) {
			throw new SystemArgumentException("No username provided.");
		}
		
		if (StringUtils.isEmpty(internalUsername)) {
			throw new SystemArgumentException("No internal username provided.");
		}
		
		if (remoteConfigType == null) {
			throw new SystemArgumentException("No credential type provided. " +
					"Please specify either 'login' or 'storage'");
		} else if (!(remoteConfigType.equalsIgnoreCase("storage") || 
				remoteConfigType.equalsIgnoreCase("login"))) {
			throw new SystemArgumentException("Invalid credential type '" + 
				remoteConfigType + "'. Please specify either 'login' or 'storage'");
		} 
		
		for(RemoteSystem system: getDao().getUserSystems(apiUsername, false)) 
		{
			if (!system.getUserRole(apiUsername).getRole().canAdmin()) continue;
			
			if (remoteConfigType.equalsIgnoreCase("login"))
			{
				if (system instanceof ExecutionSystem) 
				{
					AuthConfig loginAuthConfig = ((ExecutionSystem)system).getLoginConfig().getAuthConfigForInternalUsername(internalUsername);
					if (!loginAuthConfig.isSystemDefault()) {
						for (AuthConfig config: ((ExecutionSystem)system).getLoginConfig().getAuthConfigs()) {
							if (config.getInternalUsername() != null && internalUsername.equals(config.getInternalUsername())) {
								((ExecutionSystem)system).getLoginConfig().getAuthConfigs().remove(config);
								getEventProcessor().processSystemUpdateEvent(system, SystemEventType.REMOVE_CREDENTIAL, internalUsername);
//								NotificationManager.process(system.getUuid(), "REMOVE_CREDENTIAL", internalUsername);
							}
						}
						getDao().persist(system);
					}
				}
			} 
			else 
			{
				AuthConfig storageAuthConfig = system.getStorageConfig().getAuthConfigForInternalUsername(internalUsername);
				if (!storageAuthConfig.isSystemDefault()) {
					for (AuthConfig config: system.getStorageConfig().getAuthConfigs()) {
						if (config.getInternalUsername() != null && internalUsername.equals(config.getInternalUsername())) {
							system.getStorageConfig().getAuthConfigs().remove(config);
							getEventProcessor().processSystemUpdateEvent(system, SystemEventType.REMOVE_CREDENTIAL, internalUsername);
//							NotificationManager.process(system.getUuid(), "REMOVE_CREDENTIAL", internalUsername);
						}
					}
					getDao().persist(system);
				}
			}
		}
	}
	
	/**
	 * Deletes the stored AuthConfig for an internal user on the given system.
	 *  
	 * @param system
	 * @param apiUsername
	 * @param internalUsername
	 * @throws SystemArgumentException
	 * @throws PermissionException 
	 */
	public void removeAllInternalUserAuthConfigOnSystem(RemoteSystem system, 
			String apiUsername, String internalUsername) 
	throws SystemArgumentException, PermissionException 
	{
		removeInternalUserAuthConfigOnSystemOfType(system, "storage", apiUsername, internalUsername);
		
		if (system instanceof ExecutionSystem) {
			removeInternalUserAuthConfigOnSystemOfType(system, "login", apiUsername, internalUsername);
		}
	}
	
	/**
	 * Deletes an AuthConfig for the given internal user.
	 *  
	 * @param system
	 * @param remoteConfigType
	 * @param apiUsername
	 * @param internalUsername
	 * @throws SystemArgumentException
	 * @throws PermissionException 
	 */
	public void removeInternalUserAuthConfigOnSystemOfType(RemoteSystem system, 
			String remoteConfigType, String apiUsername, String internalUsername) 
	throws SystemArgumentException, PermissionException 
	{
		if (system == null) {
			throw new SystemArgumentException("No system provided.");
		} else if (system.isPubliclyAvailable()) {
			throw new SystemArgumentException("Internal users are not supported on public systems.");
		} else if (!system.getUserRole(apiUsername).getRole().canAdmin()) {
			throw new PermissionException("User does not have permission to manage internal user credentials on this system.");
		}
		
		if (StringUtils.isEmpty(apiUsername)) {
			throw new SystemArgumentException("No username provided.");
		}
		
		if (StringUtils.isEmpty(internalUsername)) {
			throw new SystemArgumentException("No internal username provided.");
		}
		
		// make sure they're requesting a valid config type for this auth config
		if (remoteConfigType == null) {
			throw new SystemArgumentException("No credential type provided. " +
					"Please specify either 'lobin' or 'storage'");
		} else if (!remoteConfigType.equalsIgnoreCase("storage") && 
				!remoteConfigType.equalsIgnoreCase("login")) {
			throw new SystemArgumentException("Invalid credential type '" + 
				remoteConfigType + "'. Please specify either 'login' or 'storage'");
		} else if (remoteConfigType.equalsIgnoreCase("login")) {
			if (!(system instanceof ExecutionSystem)) {
				throw new SystemArgumentException("Invalid credential type. " + 
						 "Only execution systems can have login credentials.");
			}
		}
		
		// delete the current auth config for the given internal user.
		if (remoteConfigType.equalsIgnoreCase("storage")) 
		{
			AuthConfig userAuthConfig = system.getStorageConfig().getAuthConfigForInternalUsername(internalUsername);
			if (!userAuthConfig.isSystemDefault()) {
				for (AuthConfig config: system.getStorageConfig().getAuthConfigs()) {
					if (StringUtils.equals(internalUsername, config.getInternalUsername())) {
						system.getStorageConfig().getAuthConfigs().remove(config);
						getDao().persist(system);
						getEventProcessor().processSystemUpdateEvent(system, SystemEventType.REMOVE_CREDENTIAL, internalUsername);
//						NotificationManager.process(system.getUuid(), "REMOVE_CREDENTIAL", internalUsername);
						break;
					}
				}
			}
		} 
		else 
		{
			AuthConfig userAuthConfig = ((ExecutionSystem)system).getLoginConfig().getAuthConfigForInternalUsername(internalUsername);
			if (!userAuthConfig.isSystemDefault()) {
				for (AuthConfig config: ((ExecutionSystem)system).getLoginConfig().getAuthConfigs()) {
					if (config.getInternalUsername() != null && internalUsername.equals(config.getInternalUsername())) {
						((ExecutionSystem)system).getLoginConfig().getAuthConfigs().remove(config);
						getDao().persist(system);
						getEventProcessor().processSystemUpdateEvent(system, SystemEventType.REMOVE_CREDENTIAL, internalUsername);
//						NotificationManager.process(system.getUuid(), "REMOVE_CREDENTIAL", internalUsername);
						break;
					}
				}
			}
		}
	}
	
	/**
	 * Adds or updates an auth config for the provided internal user. If an AuthConfig is
	 * already present for the internal user, it will be deleted and replaced with the new
	 * one. If a credential server is provided or a grid credential is provided, it will be
	 * validated before persisting. 
	 * 
	 * @param system
	 * @param remoteConfigType
	 * @param apiUsername
	 * @param internalUsername
	 * @param username
	 * @param password
	 * @param credentialType
	 * @param credential
	 * @param irodsResource Unused at the moment. Placeholder in case we give internal users ability to specify their own storage/login config.
	 * @param irodsZone Placeholder in case we give internal users ability to specify their own storage/login config.
	 * @param endpoint
	 * @param port
	 * @param authProtocol
	 * @throws SystemArgumentException
	 * @throws PermissionException 
	 */
	public void updateInternalUserAuthConfigOnSystemOfType(
			RemoteSystem system, String remoteConfigType, String apiUsername, 
			String internalUsername, String username, String password, String credentialType,
			String credential, String irodsResource, String irodsZone, String endpoint, 
			int port, String authProtocol) 
	throws SystemArgumentException, PermissionException
	{
		if (system == null) {
			throw new SystemArgumentException("No system provided.");
		} else if (system.isPubliclyAvailable()) {
			throw new SystemArgumentException("Internal users are not supported on public systems.");
		} else if (!system.getUserRole(apiUsername).getRole().canAdmin()) {
			throw new PermissionException("User does not have permission to manage internal user credentials on this system.");
		}
		
		if (StringUtils.isEmpty(apiUsername)) {
			throw new SystemArgumentException("No username provided.");
		}
		
		if (StringUtils.isEmpty(internalUsername)) {
			throw new SystemArgumentException("No internal username provided.");
		}
		
		// make sure they're requesting a valid config type for this auth config
		if (remoteConfigType == null) {
			throw new SystemArgumentException("No credential type provided. " +
					"Please specify either 'login' or 'storage'");
		} else if (!(remoteConfigType.equalsIgnoreCase("storage") || 
				remoteConfigType.equalsIgnoreCase("login"))) {
			throw new SystemArgumentException("Invalid credential type '" + 
				remoteConfigType + "'. Please specify either 'login' or 'storage'");
		} else if (remoteConfigType.equalsIgnoreCase("login")) {
			if (!(system instanceof ExecutionSystem)) {
				throw new SystemArgumentException("Invalid credential type. " + 
						 "Only execution systems can have login credentials.");
			}
		}
		
		// get the default config to fill in the blanks
		AuthConfigType credType = null;
		if (remoteConfigType.equalsIgnoreCase("storage")) {
			AuthConfig defaultAuthConfig = system.getStorageConfig().getDefaultAuthConfig();
			if (defaultAuthConfig != null) {
				credType = defaultAuthConfig.getType();
			}
		} else {
			AuthConfig defaultAuthConfig = ((ExecutionSystem)system).getLoginConfig().getDefaultAuthConfig();
			if (defaultAuthConfig != null) {
				credType = defaultAuthConfig.getType();
			}
		}
		
		// verify they specified a valid auth type
		if (!StringUtils.isEmpty(credentialType))
		{
			try {
				credType = AuthConfigType.valueOf(credentialType);
			}
			catch (Exception e) {
				throw new SystemArgumentException("Invalid" +
						" authentication type '" + credentialType + "'. " +
						"If provided, please specify one of: '" + 
						ServiceUtils.explode("', '", Arrays.asList(AuthConfigType.values())) + "'.", e);
			}
		}
		
		// get the existing auth config if present
		AuthConfig authConfig = null;
		if (remoteConfigType.equalsIgnoreCase("storage")) {
			authConfig = system.getStorageConfig().getAuthConfigForInternalUsername(internalUsername);
		} else {
			authConfig = ((ExecutionSystem)system).getLoginConfig().getAuthConfigForInternalUsername(internalUsername);
		}
		
		if (authConfig == null || authConfig.isSystemDefault()) {
			CredentialServer server = null;
			// if they provide a credential server, construct it here
			if (!StringUtils.isEmpty(endpoint))
			{
				String serverName = internalUsername + " credential server";
				server = new CredentialServer(serverName, endpoint, port, 
						CredentialServerProtocolType.valueOf(authProtocol));
			}
			authConfig = new AuthConfig(internalUsername, 
										username, 
										password, 
										credential,
										credType, 
										server);
			authConfig.setRemoteConfig(system.getStorageConfig());
		} 
		else 
		{
			authConfig.setUsername(username);
			authConfig.setPassword(password);
			authConfig.setCredential(credential);
			authConfig.setType(credType);
			
			if (StringUtils.isEmpty(endpoint)) {
				authConfig.setCredentialServer(null);
			} else if (authConfig.getCredentialServer() != null) {
				authConfig.getCredentialServer().setEndpoint(endpoint);
				authConfig.getCredentialServer().setPort(port);
				authConfig.getCredentialServer().setProtocol(
						CredentialServerProtocolType.valueOf(authProtocol));
				authConfig.getCredentialServer().setName(
						internalUsername + " credential server");
			} else {
				String serverName = internalUsername + " credential server";
				CredentialServer server = new CredentialServer(serverName, endpoint, port, 
						CredentialServerProtocolType.valueOf(authProtocol));
				authConfig.setCredentialServer(server);
			}
		}
		
		// validate the auth config makes sense
		AuthConfig.validateConfiguration(authConfig);
		
		// verify the auth type and system config type go together
		if (remoteConfigType.equalsIgnoreCase("storage")) 
		{
			if (!system.getStorageConfig().getProtocol().accepts(authConfig.getType())) {
				throw new SystemArgumentException("Invalid authentication configuration. " +
						"The system storage protocol value of " + system.getStorageConfig().getProtocol() + 
						" does not support an auth type of " + authConfig.getType());
			}
		} 
		else 
		{
			if (!((ExecutionSystem)system).getLoginConfig().getProtocol().accepts(authConfig.getType())) {
				throw new SystemArgumentException("Invalid authentication configuration. " +
						"The system login protocol value of " + system.getStorageConfig().getProtocol() + 
						" does not support an auth type of " + authConfig.getType());
			}
		}
		
		String salt = system.getEncryptionKeyForAuthConfig(authConfig);
		
		// make sure a credential, if provided, is valid
		if (authConfig.getCredentialRemainingTime(salt) == 0) {
			throw new SystemArgumentException("Provided credential is expired.");
		}
		
		if (!StringUtils.isEmpty(authConfig.getPassword())) {
			try {
				authConfig.encryptCurrentPassword(salt);
			} catch (EncryptionException e) {
				throw new SystemArgumentException("Unable to encrypt password.");	
			}
		}
		
		if (!StringUtils.isEmpty(authConfig.getCredential())) {
			try {
				authConfig.encryptCurrentCredential(salt);
			} catch (EncryptionException e) {
				throw new SystemArgumentException("Unable to encrypt password.");	
			}
		}
		
		// verify, if necessary, that a credential can be obtained remotely
		try 
		{
			authConfig.retrieveCredential(salt);
		} 
		catch (NotYetImplementedException e) {
			throw new SystemArgumentException("Credential verification failed. " + 
					e.getMessage(), e);
		} catch (Exception e) {
			throw new SystemArgumentException(
					"Unable to retrieve credential with configuration provided.", e);
		}
		
		// save the auth config, replacing the old one as needed.
		if (remoteConfigType.equalsIgnoreCase("storage")) 
		{
//			AuthConfig currentAuthConfig = system.getStorageConfig().getAuthConfigForInternalUsername(internalUsername);
//			system.getStorageConfig().getAuthConfigs().remove(currentAuthConfig);
			system.getStorageConfig().addAuthConfig(authConfig);
			getDao().merge(system);
			if (authConfig.isSystemDefault()) {
				getEventProcessor().processSystemUpdateEvent(system, SystemEventType.UPDATE_DEFAULT_CREDENTIAL, apiUsername);
//				NotificationManager.process(system.getUuid(), "UPDATE_DEFAULT_CREDENTIAL", system.getOwner());
			} else {
				getEventProcessor().processSystemUpdateEvent(system, SystemEventType.UPDATE_CREDENTIAL, apiUsername);
//				NotificationManager.process(system.getUuid(), "UPDATE_CREDENTIAL", system.getOwner());
			}
		} 
		else 
		{
//			AuthConfig currentAuthConfig = ((ExecutionSystem)system).getLoginConfig().getAuthConfigForInternalUsername(internalUsername);
//			((ExecutionSystem)system).getLoginConfig().getAuthConfigs().remove(currentAuthConfig);
			((ExecutionSystem)system).getLoginConfig().addAuthConfig(authConfig);
			getDao().merge(system);
			if (authConfig.isSystemDefault()) {
				getEventProcessor().processSystemUpdateEvent(system, SystemEventType.UPDATE_DEFAULT_CREDENTIAL, apiUsername);
//				NotificationManager.process(system.getUuid(), "UPDATE_DEFAULT_CREDENTIAL", system.getOwner());
			} else {
				getEventProcessor().processSystemUpdateEvent(system, SystemEventType.UPDATE_CREDENTIAL, apiUsername);
//				NotificationManager.process(system.getUuid(), "UPDATE_CREDENTIAL", system.getOwner());
			}
		}
	}
	
	/**
	 * Adds or updates both login and storage AuthConfig for the provided internal user on
	 * a given system. If the LoginConfig or StorageConfig don't support the provided 
	 * LoginCredentialType, an exception is thrown. 
	 * 
	 * Any AuthConfig already present for the internal user, will be deleted and replaced 
	 * with the new one. If a credential server is provided or a grid credential is provided, 
	 * it will be validated before persisting. 
	 * 
	 * @param system
	 * @param apiUsername
	 * @param internalUsername
	 * @param username
	 * @param password
	 * @param credentialType
	 * @param credential
	 * @param irodsResource Unused at the moment. Placeholder in case we give internal users ability to specify their own storage/login config.
	 * @param irodsZone Placeholder in case we give internal users ability to specify their own storage/login config.
	 * @param endpoint
	 * @param port
	 * @param authProtocol
	 * @throws SystemArgumentException
	 * @throws PermissionException 
	 */
	public void updateAllInternalUserAuthConfigOnSystem(RemoteSystem system, String apiUsername, 
			String internalUsername, String username, String password, String credentialType,
			String credential, String irodsResource, String irodsZone, String endpoint, 
			int port, String authProtocol) 
	throws SystemArgumentException, PermissionException
	{
		updateInternalUserAuthConfigOnSystemOfType(system, "storage", apiUsername, internalUsername, 
				username, password, credentialType, credential, irodsResource, irodsZone, 
				endpoint, port, authProtocol);
		
		if (system instanceof ExecutionSystem) 
		{
			updateInternalUserAuthConfigOnSystemOfType(system, "login", apiUsername, internalUsername,
				username, password, credentialType, credential, irodsResource, irodsZone,
				endpoint, port, authProtocol);
		}
	}
	
	/**
	 * Adds or updates both login and storage AuthConfig for the provided internal user on
	 * all of the api user's systems. If the LoginConfig or StorageConfig don't support the 
	 * provided LoginCredentialType, an exception is thrown. 
	 * 
	 * Any AuthConfig already present for the internal user, will be deleted and replaced 
	 * with the new one. If a credential server is provided or a grid credential is provided, 
	 * it will be validated before persisting. 
	 * 
	 * @param apiUsername
	 * @param internalUsername
	 * @param username
	 * @param password
	 * @param credentialType
	 * @param credential
	 * @param endpoint
	 * @param port
	 * @param authProtocol
	 * @throws SystemArgumentException
	 * @throws PermissionException 
	 */
	public void updateAllInternalUserAuthConfig(String apiUsername, 
			String internalUsername, String username, String password, 
			String credentialType, String credential, String irodsResource, 
			String irodsZone, String endpoint, int port, 
			String authProtocol) 
	throws SystemArgumentException, PermissionException
	{
		for(RemoteSystem system: getDao().getUserSystems(apiUsername, false)) 
		{
			updateInternalUserAuthConfigOnSystemOfType(system, "storage", apiUsername, internalUsername, 
				username, password, credentialType, credential, irodsResource, irodsZone,
				endpoint, port, authProtocol);
		
			if (system instanceof ExecutionSystem) 
			{
				updateInternalUserAuthConfigOnSystemOfType(system, "login", apiUsername, internalUsername,
					username, password, credentialType, credential, irodsResource, irodsZone,
					endpoint, port, authProtocol);
			}
		}
	}
	
	/**
	 * Adds or updates both login and storage AuthConfig for the provided internal user on
	 * all of the api user's systems. If the LoginConfig or StorageConfig don't support the 
	 * provided LoginCredentialType, an exception is thrown. 
	 * 
	 * Any AuthConfig already present for the internal user, will be deleted and replaced 
	 * with the new one. If a credential server is provided or a grid credential is provided, 
	 * it will be validated before persisting. 
	 * 
	 * @param remoteConfigType
	 * @param apiUsername
	 * @param internalUsername
	 * @param username
	 * @param password
	 * @param credentialType
	 * @param credential
	 * @param irodsResource Unused at the moment. Placeholder in case we give internal users ability to specify their own storage/login config.
	 * @param irodsZone Placeholder in case we give internal users ability to specify their own storage/login config.
	 * @param endpoint
	 * @param port
	 * @param authProtocol
	 * @throws SystemArgumentException
	 * @throws PermissionException 
	 */
	public void updateAllInternalUserAuthConfigOfType(String remoteConfigType, String apiUsername, 
			String internalUsername, String username, String password, 
			String credentialType, String credential, String irodsResource, 
			String irodsZone, String endpoint, int port, 
			String authProtocol) 
	throws SystemArgumentException, PermissionException
	{
		for(RemoteSystem system: getDao().getUserSystems(apiUsername, false)) 
		{
			if (remoteConfigType.equalsIgnoreCase("login"))
			{
				if (system instanceof ExecutionSystem) 
				{
					updateInternalUserAuthConfigOnSystemOfType(system, remoteConfigType, apiUsername, internalUsername,
						username, password, credentialType, credential, irodsResource, irodsZone,
						endpoint, port, authProtocol);
				}
			} 
			else {
				updateInternalUserAuthConfigOnSystemOfType(system, remoteConfigType, apiUsername, internalUsername, 
						username, password, credentialType, credential, irodsResource, irodsZone,
						endpoint, port, authProtocol);
			}
		}
	}

	/**
	 * Clears all internal user credentials stored by the api user on any system.
	 * Only the default AuthConfigs will be preserved.
	 * 
	 * @param apiUsername
	 * @throws SystemArgumentException
	 */
	public void clearAllInternalUserAuthConfig(String apiUsername) throws SystemArgumentException
	{
		clearAllInternalUserAuthConfigOfType(apiUsername, "storage");
		clearAllInternalUserAuthConfigOfType(apiUsername, "login");
	}

	/**
	 * Clears all internal user credentials for the given RemoteConfig type stored 
	 * on any system. The defaults will be preserved.
	 * 
	 * @param apiUsername
	 * @param remoteConfigType The config type for which you want to remove credentials "login" or "storage"
	 * @throws SystemArgumentException
	 */
	public void clearAllInternalUserAuthConfigOfType(String apiUsername,
			String remoteConfigType) throws SystemArgumentException
	{
		if (StringUtils.isEmpty(apiUsername)) {
			throw new SystemArgumentException("No username provided.");
		}
		
		// make sure they're requesting a valid config type for this auth config
		if (remoteConfigType == null) {
			throw new SystemArgumentException("No credential type provided. " +
					"Please specify either 'login' or 'storage'");
		} else if (!(remoteConfigType.equalsIgnoreCase("storage") || 
				remoteConfigType.equalsIgnoreCase("login"))) {
			throw new SystemArgumentException("Invalid credential type '" + 
				remoteConfigType + "'. Please specify either 'login' or 'storage'");
		}
		
		for(RemoteSystem system: getDao().getUserSystems(apiUsername, false)) 
		{
			if (!system.getUserRole(apiUsername).getRole().canAdmin()) continue;
			
			if (remoteConfigType.equalsIgnoreCase("storage"))
			{
				AuthConfig defaultStorageAuthConfig = system.getStorageConfig().getDefaultAuthConfig();
				system.getStorageConfig().getAuthConfigs().clear();
				system.getStorageConfig().addAuthConfig(defaultStorageAuthConfig);
				getDao().persist(system);
			} 
			else if (system instanceof ExecutionSystem) 
			{
				AuthConfig defaultLoginAuthConfig = ((ExecutionSystem)system).getLoginConfig().getDefaultAuthConfig();
				((ExecutionSystem)system).getLoginConfig().getAuthConfigs().clear();
				((ExecutionSystem)system).getLoginConfig().addAuthConfig(defaultLoginAuthConfig);
				getDao().persist(system);
			}
			
			getEventProcessor().processSystemUpdateEvent(system, SystemEventType.CLEAR_CREDENTIALS, apiUsername);
//			NotificationManager.process(system.getUuid(), "CLEAR_CREDENTIALS", system.getOwner());
		}
		
	}

	/**
	 * Clears all credentials stored for any internal users on the given system.
	 * The defaults will be preserved.
	 * 
	 * @param system The RemoteSystem
	 * @param apiUsername
	 * @throws SystemArgumentException
	 * @throws PermissionException 
	 */
	public void clearAllInternalUserAuthConfigOnSystem(RemoteSystem system,
			String apiUsername) throws SystemArgumentException, PermissionException
	{
		
		clearAllInternalUserAuthConfigOnSystemOfType(system, apiUsername, "storage");
		
		if (system instanceof ExecutionSystem) {
			clearAllInternalUserAuthConfigOnSystemOfType(system, apiUsername, "login");
		}
	}

	/**
	 * Removes all internal user credentials for the given RemoteConfig type on the 
	 * give system. The defaults will be preserved
	 * 
	 * @param system The RemoteSystem
	 * @param apiUsername
	 * @param remoteConfigType The config type for which you want to remove credentials "login" or "storage"
	 * @throws SystemArgumentException
	 * @throws PermissionException 
	 */
	public void clearAllInternalUserAuthConfigOnSystemOfType(
			RemoteSystem system, String apiUsername, String remoteConfigType) 
	throws SystemArgumentException, PermissionException
	{
		if (system == null) {
			throw new SystemArgumentException("No system provided.");
		} else if (system.isPubliclyAvailable()) {
			throw new SystemArgumentException("Internal users are not supported on public systems.");
		} else if (!system.getUserRole(apiUsername).getRole().canAdmin()) {
			throw new PermissionException("User does not have permission to manage internal user credentials on this system.");
		}
		
		if (StringUtils.isEmpty(apiUsername)) {
			throw new SystemArgumentException("No username provided.");
		}
		
		// make sure they're requesting a valid config type for this auth config
		if (remoteConfigType == null) {
			throw new SystemArgumentException("No credential type provided. " +
					"Please specify either 'login' or 'storage'");
		} else if (!remoteConfigType.equalsIgnoreCase("storage") && 
				!remoteConfigType.equalsIgnoreCase("login")) {
			throw new SystemArgumentException("Invalid credential type '" + 
				remoteConfigType + "'. Please specify either 'login' or 'storage'");
		} else if (remoteConfigType.equalsIgnoreCase("login")) {
			if (!(system instanceof ExecutionSystem)) {
				throw new SystemArgumentException("Invalid credential type. " + 
						 "Only execution systems can have login credentials.");
			}
		}
		
		if (remoteConfigType.equalsIgnoreCase("storage"))
		{
			AuthConfig defaultStorageAuthConfig = system.getStorageConfig().getDefaultAuthConfig();
			system.getStorageConfig().getAuthConfigs().clear();
			system.getStorageConfig().addAuthConfig(defaultStorageAuthConfig);
			getDao().persist(system);
		} 
		else if (system instanceof ExecutionSystem) 
		{
			AuthConfig defaultLoginAuthConfig = ((ExecutionSystem)system).getLoginConfig().getDefaultAuthConfig();
			((ExecutionSystem)system).getLoginConfig().getAuthConfigs().clear();
			((ExecutionSystem)system).getLoginConfig().addAuthConfig(defaultLoginAuthConfig);
			getDao().persist(system);
		}
		getEventProcessor().processSystemUpdateEvent(system, SystemEventType.CLEAR_CREDENTIALS, apiUsername);
		
//		NotificationManager.process(system.getUuid(), "CLEAR_CREDENTIALS", system.getOwner());
	}

	/**
	 * Sets a system as the global default and unsets the previous default. If the system 
	 * is not public, an exception is thrown.
	 * 
	 * @param system
	 * @return
	 * @throws SystemException
	 */
	public RemoteSystem setGlobalDefault(RemoteSystem system, String apiUsername)
	throws SystemException, PermissionException
	{
		if (system == null) {
			throw new SystemException("No system provided.");
		} 
		else if (!system.isAvailable()) {
			throw new SystemException("Please enable system before assigning it as a global default.");
		}
		else if (!system.isPubliclyAvailable()) {
			throw new SystemException("Please make system public before assigning it as a global default.");	
		} 
		else if (system.isGlobalDefault()) {
			return system;
		} 
		else if (!AuthorizationHelper.isTenantAdmin(apiUsername)) {
			throw new PermissionException("User does not have permission to configure global default systems.");
		}
		
		RemoteSystem currentDefault = getDao().getGlobalDefaultSystemForTenant(system.getType(), TenancyHelper.getCurrentTenantId());
		
		if (currentDefault != null)
		{
			if (currentDefault.equals(system)) 
			{
				// nothing to do here
				return system;
			} 
			else 
			{
				// unset current default
				currentDefault.setGlobalDefault(false);
				currentDefault.setRevision(currentDefault.getRevision() + 1);
				currentDefault.setLastUpdated(new Date());
				getDao().persist(currentDefault);
				getEventProcessor().processSystemUpdateEvent(currentDefault, SystemEventType.UNSET_PUBLIC_DEFAULT, apiUsername);
				
//				NotificationManager.process(system.getUuid(), "UNSET_PUBLIC_DEFAULT", apiUsername);
			}
		}
		
		// now configure the new system as the default.
		system.setGlobalDefault(true);
		system.setRevision(system.getRevision() + 1);
		system.setLastUpdated(new Date());
		getDao().merge(system);
		
		getEventProcessor().processSystemUpdateEvent(system, SystemEventType.SET_PUBLIC_DEFAULT, apiUsername);
		
//		NotificationManager.process(system.getUuid(), "SET_PUBLIC_DEFAULT", apiUsername);
		
		return system;
	}
	
//	/**
//	 * Sets a system as the global default and unsets the previous default. If the system 
//	 * is not public, an exception is thrown.
//	 * 
//	 * @param system
//	 * @return
//	 * @throws SystemException
//	 */
//	private RemoteSystem setGlobalDefaultExecutionSystem(ExecutionSystem system, String apiUsername)
//	throws SystemException, PermissionException
//	{
//		if (system == null) {
//			throw new SystemException("No system provided.");
//		} else if (!system.isPubliclyAvailable()) {
//			throw new SystemException("Please make system public before assigning it as a global default.");
//		} else if (system.isGlobalDefault()) {
//			return system;
//		} else if (!ServiceUtils.isAdmin(apiUsername)) {
//			throw new PermissionException("User does not have permission to edit global defaults.");
//		}
//		
//		ExecutionSystem currentDefault = (ExecutionSystem)dao.getDefaultSystem(RemoteSystemType.EXECUTION);
//		
//		if (currentDefault != null)
//		{
//			if (currentDefault.equals(system)) 
//			{
//				// nothing to do here
//				return system;
//			} 
//			else 
//			{
//				// unset current default
//				currentDefault.setGlobalDefault(false);
//				currentDefault.setRevision(currentDefault.getRevision() + 1);
//				currentDefault.setLastUpdated(new Date());
//				dao.persist(currentDefault);
//				NotificationManager.process(system.getUuid(), "UNSET_PUBLIC_DEFAULT", apiUsername);
//			}
//		}
//		
//		// now configure the new system as the default.
//		system.setGlobalDefault(true);
//		system.setRevision(system.getRevision() + 1);
//		system.setLastUpdated(new Date());
//		dao.persist(system);
//		NotificationManager.process(system.getUuid(), "SET_PUBLIC_DEFAULT", apiUsername);
//		
//		return system;
//	}
	
	/**
	 * Sets a system as the global default and unsets the previous default. If the system 
	 * is not public, an exception is thrown.
	 * 
	 * @param system
	 * @return
	 * @throws SystemException
	 */
	public RemoteSystem unsetGlobalDefault(RemoteSystem currentDefault, String apiUsername)
	throws SystemException, PermissionException
	{
		if (currentDefault == null) {
			throw new SystemException("No system provided.");
		} 
		else if (!currentDefault.isGlobalDefault()) {
			throw new SystemException("System " + currentDefault.getSystemId() + 
					" is not currently the global default " + currentDefault.getType() + " system.");
		} 
		else if (!ServiceUtils.isAdmin(apiUsername)) {
			throw new PermissionException("User does not have permission to configure global default");
		}
		
		
		// unset current default
		currentDefault.setGlobalDefault(false);
		currentDefault.setRevision(currentDefault.getRevision() + 1);
		currentDefault.setLastUpdated(new Date());
		getDao().persist(currentDefault);
		
		getEventProcessor().processSystemUpdateEvent(currentDefault, SystemEventType.UNSET_PUBLIC_DEFAULT, apiUsername);
		
//		NotificationManager.process(currentDefault.getUuid(), "UNSET_PUBLIC_DEFAULT", apiUsername);
		
		return currentDefault;
	}

	/**
	 * Erases system completely from the database. This is an admin-only action and will not 
	 * clean up the registered apps.
	 * 
	 * @param system
	 * @param apiUsername
	 * @throws PermissionException 
	 */
	public void eraseSystem(RemoteSystem system, String apiUsername) throws PermissionException {
		if (system == null) {
			throw new SystemException("No system provided.");
		} else if (system.isPubliclyAvailable()) {
			throw new SystemException("System " + system.getSystemId() + 
					" is currently a public system. Please remove from public scope before erasing.");
		} else if (system.isGlobalDefault()) {
			throw new SystemException("System " + system.getSystemId() + 
					" is currently the global default " + system.getType() + " system. "
					+ "Please set another system as the global default and remove this "
					+ "system from public scope before erasing.");
		} else if (!ServiceUtils.isAdmin(apiUsername)) {
			throw new PermissionException("User does not have permission to edit global defaults.");
		}
		
		
		// stuff will break if we do this!
		
		try {
			getDao().remove(system);
		} catch (Throwable e) {
			throw new SystemException("Failed to erase system " + system.getSystemId(), e);
		}
		
		try {
			new SystemHistoryEventDao().deleteByEntityId(system.getUuid());
		} catch (EntityEventPersistenceException e) {
			log.error("Failed to clean up event data when system system " + 
					system.toString() + " was erased", e);
		}
		
		// remove the system from the db
		getEventProcessor().processSystemUpdateEvent(system, SystemEventType.ERASED, apiUsername);
				
	}

    /**
     * Sets the system availability to true and notifies subscribers
     * @param system the system to make available
     * @param username user who made the request
     */
    public RemoteSystem enableSystem(RemoteSystem system, String username) 
    throws SystemException, PermissionException
    {
    	if (StringUtils.equals(username, Settings.PUBLIC_USER_USERNAME) ||
    			StringUtils.equals(username, Settings.WORLD_USER_USERNAME)) {
    		throw new PermissionException("Permission denied. Public users may not manage systems.");
    	}
    	else if (system.isPubliclyAvailable() && !ServiceUtils.isAdmin(username)) {
            throw new PermissionException("Permission denied. Only tenant administrators may enable public systems.");
        }
        else if (!system.getUserRole(username).canAdmin()) {
            throw new PermissionException("Permission denied. Only users with the ADMIN role may enable this system.");
        }
        else 
        {
            if (system.isAvailable()) {
                throw new SystemException(system.getSystemId() + " is already available.");
            } else {
                system.setAvailable(true);
                getDao().persist(system);
                getEventProcessor().processSystemUpdateEvent(system, SystemEventType.ENABLED, username);
                return system;
            }
        }
    }
    
    /**
     * Sets the system availability to false and notifies subscribers
     * @param system the system to make available
     * @param username user who made the request
     */
    public void disableSystem(RemoteSystem system, String createdBy) 
    throws SystemException, PermissionException
    {
    	if (StringUtils.equalsIgnoreCase(createdBy, Settings.PUBLIC_USER_USERNAME) ||
    			StringUtils.equalsIgnoreCase(createdBy, Settings.WORLD_USER_USERNAME)) {
    		throw new PermissionException("Permission denied. Public users may not manage systems.");
    	}
    	else if (system.isPubliclyAvailable() && !ServiceUtils.isAdmin(createdBy)) {
            throw new PermissionException("Permission denied. Only tenant administrators may disable public systems.");
        }
        else if (!system.getUserRole(createdBy).canAdmin()) {
            throw new PermissionException("Permission denied. Only users with the ADMIN role may disable this system.");
        }
        else 
        {
            if (!system.isAvailable()) {
                throw new SystemException(system.getSystemId() + " is already disabled.");
            } else {
                system.setAvailable(false);
                getDao().persist(system);
                getEventProcessor().processSystemUpdateEvent(system, SystemEventType.DISABLED, createdBy);
//                NotificationManager.process(system.getUuid(), "DISABLED", username);
            }
        }
    }

	/**
	 * Updates the system status and propagates events by calling the
	 * {@link RemoteSystemEventProcessor#processStatusChangeEvent(RemoteSystem, SystemStatusType, String))}
	 * method.
	 * 
	 * @param system
	 * @param newSystemStatus
	 * @param username
	 */
	public void updateSystemStatus(RemoteSystem system, SystemStatusType newSystemStatus, String username) {
		
		getDao().updateStatus(system, newSystemStatus);
		SystemStatusType oldSystemStatus = system.getStatus();
		
		getEventProcessor().processStatusChangeEvent(system, oldSystemStatus, newSystemStatus, username);
	}
}
