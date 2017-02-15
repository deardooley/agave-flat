package org.iplantc.service.metadata.managers;

import org.apache.commons.lang.StringUtils;
import org.iplantc.service.common.auth.AuthorizationHelper;
import org.iplantc.service.common.exceptions.PermissionException;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.metadata.Settings;
import org.iplantc.service.metadata.dao.MetadataPermissionDao;
import org.iplantc.service.metadata.events.MetadataEventProcessor;
import org.iplantc.service.metadata.exceptions.MetadataException;
import org.iplantc.service.metadata.model.MetadataItem;
import org.iplantc.service.metadata.model.MetadataPermission;
import org.iplantc.service.metadata.model.enumerations.MetadataEventType;
import org.iplantc.service.metadata.model.enumerations.PermissionType;
import org.iplantc.service.notification.managers.NotificationManager;

/**
 * Management class for handling operations on {@link MetadataItem} objects. 
 * The constructor assigns a calling user to whom all events resulting from 
 * permission operations will be assigned. Each permission event results in a
 * unique event being thrown.
 * 
 * @author dooley
 *
 */
public class MetadataPermissionManager {

	private String uuid;
    private String authenticatedUsername;
    private MetadataEventProcessor eventProcessor = new MetadataEventProcessor();
    
	/**
	 * Base constructor binding a {@link MetadataItem} by {@code uuid} to a new
	 * instance of this {@link MetadataPermissionManager}. 
	 * @param uuid the uuid of the {@link MetadataItem} to which permission checks apply
	 * @param authenticatedUsername the username of the user responsible for invoking methods on the {@code uuid}
	 * @throws MetadataException
	 */
	public MetadataPermissionManager(String uuid, String authenticatedUsername) throws MetadataException
	{
		if (uuid == null) { throw new MetadataException("UUID cannot be null"); }
		this.setUuid(uuid);
        if (authenticatedUsername == null) { throw new MetadataException("UUID owner cannot be null"); }
        this.setAuthenticatedUsername(authenticatedUsername);
	}

	/**
	 * Checks whether the given {@code username} has the given {@code jobPermissionType} 
	 * for the the {@link MetadataItem} associated with this permission manager.
	 * 
	 * @param username the user to whom the permission will be checked
	 * @param jobPermissionType
	 * @return
	 * @throws MetadataException
	 */
	public boolean hasPermission(String username,
			PermissionType jobPermissionType) throws MetadataException
	{

		if (StringUtils.isBlank(username)) { return false; }

		if (getAuthenticatedUsername().equals(username) || AuthorizationHelper.isTenantAdmin(username))
			return true;

		for (MetadataPermission pem : MetadataPermissionDao.getByUuid(getUuid()))
		{
			if (pem.getUsername().equals(username) || 
					pem.getUsername().equals(Settings.WORLD_USER_USERNAME) || 
					pem.getUsername().equals(Settings.PUBLIC_USER_USERNAME)) { 
				return pem.getPermission().equals(jobPermissionType); 
			}
		}

		return false;
	}

	/**
	 * Checks whether the given {@code username} has {@link PermissionType#READ}, 
	 * {@link PermissionType#READ_WRITE}, or {@link PermissionType#ALL} 
	 * for the the {@link MetadataItem} associated with this permission manager.
	 * @param username the user to whom the permission will be checked
	 * @return true if they have read permission, false otherwise
	 * @throws MetadataException
	 */
	public boolean canRead(String username) throws MetadataException
	{

		if (StringUtils.isBlank(username)) { return false; }

		if (StringUtils.equals(getAuthenticatedUsername(), username) || AuthorizationHelper.isTenantAdmin(username))
			return true;

		for (MetadataPermission pem : MetadataPermissionDao.getByUuid(getUuid()))
		{
			if (pem.getUsername().equals(username) || 
					pem.getUsername().equals(Settings.WORLD_USER_USERNAME) || 
					pem.getUsername().equals(Settings.PUBLIC_USER_USERNAME)) { 
				return pem.canRead(); 
			}
		}

		return false;
	}

	/**
	 * Checks whether the given {@code username} has {@link PermissionType#WRITE}, 
	 * {@link PermissionType#READ_WRITE}, or {@link PermissionType#ALL} 
	 * for the the {@link MetadataItem} associated with this permission manager.
	 * @param username the user to whom the permission will be checked
	 * @return true if they have write permission, false otherwise
	 * @throws MetadataException
	 */
	public boolean canWrite(String username) throws MetadataException
	{

		if (StringUtils.isBlank(username)) { return false; }

		if (getAuthenticatedUsername().equals(username) || AuthorizationHelper.isTenantAdmin(username))
			return true;

		for (MetadataPermission pem : MetadataPermissionDao.getByUuid(getUuid()))
		{
			if (pem.getUsername().equals(username) || 
					pem.getUsername().equals(Settings.WORLD_USER_USERNAME) || 
					pem.getUsername().equals(Settings.PUBLIC_USER_USERNAME)) { 
				return pem.canWrite(); 
			}
		}

		return false;
	}

	
	/**
	 * Assigns the given {@code sPermission} to the given {@code username} 
	 * for the the {@link MetadataItem} associated with this permission manager.
	 * 
	 * @param username the user to whom the permission will be granted
	 * @param sPermission the permission to set 
	 * @throws MetadataException
	 * @throws PermissionException if the permission value is invalid 
	 */
	public void setPermission(String username, String sPermission)
			throws MetadataException, PermissionException
	{
		if (StringUtils.isBlank(username)) { 
			throw new MetadataException("Invalid username"); 
		}

		if (getAuthenticatedUsername().equals(username))
			return;
		
		if (StringUtils.equals(Settings.PUBLIC_USER_USERNAME, username) || 
				StringUtils.equals(Settings.WORLD_USER_USERNAME, username)) {
			if (!AuthorizationHelper.isTenantAdmin(TenancyHelper.getCurrentEndUser())) {
				throw new PermissionException("User does not have permission to edit public metadata item permissions");
			}
		}
		
		MetadataPermission userPermission = MetadataPermissionDao.getByUsernameAndUuid(username, getUuid());
		
		// if the permission is empty or null, delete it
		if (StringUtils.isEmpty(sPermission) || sPermission.equalsIgnoreCase("none"))
		{
			// delete the permission if it exists
			if (userPermission != null) {
				MetadataPermissionDao.delete(userPermission);
				// getEventProcessor().processPermissionEvent(getUuid(), pem, MetadataEventType.PERMISSION_REVOKE, getAuthenticatedUsername(), new MetadataDao().getByUuid(getUuid()).toJSON());
				NotificationManager.process(getUuid(), MetadataEventType.PERMISSION_REVOKE.name(), username );
			}
			else {
				// otherwise do nothing, no permission existed before or after
			}
		}
		// they're updating/adding a permission, so resolve the permission and  
		// and alert the appropriate subscriptions
		else {
			PermissionType permissionType = PermissionType
					.valueOf(sPermission.toUpperCase());
	
			// if not present, add it
			if (userPermission == null) {
				userPermission = new MetadataPermission(getUuid(), username, permissionType);
				MetadataPermissionDao.persist(userPermission);
				// getEventProcessor().processPermissionEvent(getUuid(), pem, MetadataEventType.PERMISSION_GRANT, getAuthenticatedUsername(), new MetadataDao().getByUuid(getUuid()).toJSON());
				NotificationManager.process(getUuid(), MetadataEventType.PERMISSION_GRANT.name(), username);
			}
			// otherwise, update the existing permission
			else {
				userPermission.setPermission(permissionType);
				MetadataPermissionDao.persist(userPermission);
				// getEventProcessor().processPermissionEvent(getUuid(), pem, MetadataEventType.PERMISSION_UPDATE, getAuthenticatedUsername(), new MetadataDao().getByUuid(getUuid()).toJSON());
				NotificationManager.process(getUuid(), MetadataEventType.PERMISSION_UPDATE.name(), username);
			}
		}
	}
	
	/**
	 * Removes all permissions, save ownership
	 * @throws MetadataException
	 */
	public void clearPermissions() throws MetadataException
	{
		if (getUuid() == null) {
			throw new MetadataException("No object ID specified");
		}
		
//		List<MetadataPermission> currentPems = MetadataPermissionDao.getByUuid(uuid);
			
		MetadataPermissionDao.deleteByUuid(getUuid());
		
//		for (MetadataPermission currentPem: currentPems) {
//			getEventProcessor().processPermissionEvent(getUuid(), 
//													   currentPem, 
//													   MetadataEventType.PERMISSION_REVOKE, 
//													   getAuthenticatedUsername(), 
//													   new MetadataDao().getByUuid(getUuid()).toJSON());
//		}
		
		NotificationManager.process(getUuid(), "PERMISSION_REVOKE", getAuthenticatedUsername());
		
	}

	/**
	 * Fetches the user permission for the the {@link MetadataItem} associated with this
	 * permission manager. 
	 * @param username
	 * @return the assigned permission for the  
	 * @throws MetadataException
	 */
	public MetadataPermission getPermission(String username) throws MetadataException {
		MetadataPermission pem = new MetadataPermission(getUuid(), username, PermissionType.NONE); 
		
		if (StringUtils.isBlank(username)) { 
			return pem; 
		}
		else if (getAuthenticatedUsername().equals(username) || AuthorizationHelper.isTenantAdmin(username)) {
			pem.setPermission(PermissionType.ALL);
			return pem; 
		}
		else {
			for (MetadataPermission dbPems : MetadataPermissionDao.getByUuid(getUuid()))
			{
				if (dbPems.getUsername().equals(username)) {
					return pem;
				}
				else if (dbPems.getUsername().equals(Settings.WORLD_USER_USERNAME) ||
						dbPems.getUsername().equals(Settings.PUBLIC_USER_USERNAME)) { 
					pem = dbPems;
				}
			}
			return pem;
		}
	}

	/**
	 * The user responsible for making the permission manager requests
	 * @return the authenticatedUsername
	 */
	public String getAuthenticatedUsername() {
		return authenticatedUsername;
	}

	/**
	 * @param authenticatedUsername the authenticatedUsername to set
	 */
	public void setAuthenticatedUsername(String authenticatedUsername) {
		this.authenticatedUsername = authenticatedUsername;
	}

	/**
	 * @return the uuid
	 */
	public String getUuid() {
		return uuid;
	}

	/**
	 * @param uuid the uuid to set
	 */
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	/**
	 * @return the eventProcessor
	 */
	public MetadataEventProcessor getEventProcessor() {
		return eventProcessor;
	}

	/**
	 * @param eventProcessor the eventProcessor to set
	 */
	public void setEventProcessor(MetadataEventProcessor eventProcessor) {
		this.eventProcessor = eventProcessor;
	}
}
