package org.iplantc.service.metadata.managers;

import org.apache.commons.lang.StringUtils;
import org.iplantc.service.common.auth.AuthorizationHelper;
import org.iplantc.service.common.exceptions.PermissionException;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.metadata.Settings;
import org.iplantc.service.metadata.dao.MetadataPermissionDao;
import org.iplantc.service.metadata.exceptions.MetadataException;
import org.iplantc.service.metadata.model.MetadataPermission;
import org.iplantc.service.metadata.model.enumerations.PermissionType;
import org.iplantc.service.metadata.util.ServiceUtils;
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
    private String owner;

	/**
	 * Base constructor binding a {@link MetadataItem} by {@code uuid} to a new
	 * instance of this {@link MetadataPermissionManager}. 
	 * @param uuid the uuid of the {@link MetadataItem} to which permission checks apply
	 * @param owner the username of the user responsible for invoking methods on the {@code uuid}
	 * @throws MetadataException
	 */
	public MetadataPermissionManager(String uuid, String owner) throws MetadataException
	{
		if (uuid == null) { throw new MetadataException("UUID cannot be null"); }
		this.setUuid(uuid);
        if (owner == null) { throw new MetadataException("UUID owner cannot be null"); }
        this.setOwner(owner);
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

		if (getOwner().equals(username) || AuthorizationHelper.isTenantAdmin(username))
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

		if (StringUtils.equals(getOwner(), username) || AuthorizationHelper.isTenantAdmin(username))
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

		if (getOwner().equals(username) || AuthorizationHelper.isTenantAdmin(username))
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

		if (getOwner().equals(username))
			return;
		
		if (StringUtils.equals(Settings.PUBLIC_USER_USERNAME, username) || 
				StringUtils.equals(Settings.WORLD_USER_USERNAME, username)) {
			if (!AuthorizationHelper.isTenantAdmin(TenancyHelper.getCurrentEndUser())) {
				throw new PermissionException("User does not have permission to edit public metadata item permissions");
			}
		}
		
		// if the permission is empty or null, delete it
		if (StringUtils.isEmpty(sPermission) || sPermission.equalsIgnoreCase("none"))
		{
			for (MetadataPermission pem : MetadataPermissionDao.getByUuid(getUuid(), 0, -1))
			{
				if (pem.getUsername().equals(username))
				{
					MetadataPermissionDao.delete(pem);
					
					NotificationManager.process(getUuid(), "PERMISSION_REVOKE", username );
					
					return;
				}
			}
			return;
		}

		PermissionType permissionType = PermissionType
				.valueOf(sPermission.toUpperCase());

		for (MetadataPermission pem : MetadataPermissionDao.getByUuid(getUuid(), 0, -1))
		{
			if (pem.getUsername().equals(username))
			{
				pem.setPermission(permissionType);
				MetadataPermissionDao.persist(pem);
				
				NotificationManager.process(getUuid(), "PERMISSION_GRANT", username);
				
				return;
			}
		}
		MetadataPermission pem = new MetadataPermission(getUuid(), username, permissionType);
		MetadataPermissionDao.persist(pem);
		
		NotificationManager.process(getUuid(), "PERMISSION_GRANT", username);

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
		
		MetadataPermissionDao.deleteByUuid(getUuid());
		
		NotificationManager.process(getUuid(), "PERMISSION_REVOKE", getOwner());
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
		else if (getOwner().equals(username) || AuthorizationHelper.isTenantAdmin(username)) {
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
	 * @return the owner
	 */
	public String getOwner() {
		return owner;
	}

	/**
	 * @param owner the owner to set
	 */
	public void setOwner(String owner) {
		this.owner = owner;
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
}
