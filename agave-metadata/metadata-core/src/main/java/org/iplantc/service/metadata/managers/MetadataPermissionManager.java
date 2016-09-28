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

public class MetadataPermissionManager {

	private String uuid;
    private String owner;

	public MetadataPermissionManager(String uuid, String owner) throws MetadataException
	{
		if (uuid == null) { throw new MetadataException("UUID cannot be null"); }
		this.uuid = uuid;
        if (owner == null) { throw new MetadataException("UUID owner cannot be null"); }
        this.owner = owner;
	}

	public boolean hasPermission(String username,
			PermissionType jobPermissionType) throws MetadataException
	{

		if (!ServiceUtils.isValid(username)) { return false; }

		if (owner.equals(username) || AuthorizationHelper.isTenantAdmin(username))
			return true;

		for (MetadataPermission pem : MetadataPermissionDao.getByUuid(uuid))
		{
			if (pem.getUsername().equals(username) || 
					pem.getUsername().equals(Settings.WORLD_USER_USERNAME) || 
					pem.getUsername().equals(Settings.PUBLIC_USER_USERNAME)) { 
				return pem.getPermission().equals(jobPermissionType); 
			}
		}

		return false;
	}

	public boolean canRead(String username) throws MetadataException
	{

		if (StringUtils.isEmpty(username)) { return false; }

		if (StringUtils.equals(owner, username) || AuthorizationHelper.isTenantAdmin(username))
			return true;

		for (MetadataPermission pem : MetadataPermissionDao.getByUuid(uuid))
		{
			if (pem.getUsername().equals(username) || 
					pem.getUsername().equals(Settings.WORLD_USER_USERNAME) || 
					pem.getUsername().equals(Settings.PUBLIC_USER_USERNAME)) { 
				return pem.canRead(); 
			}
		}

		return false;
	}

	public boolean canWrite(String username) throws MetadataException
	{

		if (!ServiceUtils.isValid(username)) { return false; }

		if (owner.equals(username) || AuthorizationHelper.isTenantAdmin(username))
			return true;

		for (MetadataPermission pem : MetadataPermissionDao.getByUuid(uuid))
		{
			if (pem.getUsername().equals(username) || 
					pem.getUsername().equals(Settings.WORLD_USER_USERNAME) || 
					pem.getUsername().equals(Settings.PUBLIC_USER_USERNAME)) { 
				return pem.canWrite(); 
			}
		}

		return false;
	}

	public void setPermission(String username, String sPermission)
			throws MetadataException, PermissionException
	{
		if (!ServiceUtils.isValid(username)) { 
			throw new MetadataException("Invalid username"); 
		}

		if (owner.equals(username))
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
			for (MetadataPermission pem : MetadataPermissionDao.getByUuid(uuid, 0, -1))
			{
				if (pem.getUsername().equals(username))
				{
					MetadataPermissionDao.delete(pem);
					
					NotificationManager.process(uuid, "PERMISSION_REVOKE", username );
					
					return;
				}
			}
			return;
		}

		PermissionType permissionType = PermissionType
				.valueOf(sPermission.toUpperCase());

		for (MetadataPermission pem : MetadataPermissionDao.getByUuid(uuid, 0, -1))
		{
			if (pem.getUsername().equals(username))
			{
				pem.setPermission(permissionType);
				MetadataPermissionDao.persist(pem);
				
				NotificationManager.process(uuid, "PERMISSION_GRANT", username);
				
				return;
			}
		}
		MetadataPermission pem = new MetadataPermission(uuid, username, permissionType);
		MetadataPermissionDao.persist(pem);
		
		NotificationManager.process(uuid, "PERMISSION_GRANT", username);

	}
	
	public void clearPermissions() throws MetadataException
	{
		if (uuid == null) {
			throw new MetadataException("No object ID specified");
		}
		
		MetadataPermissionDao.deleteByUuid(uuid);
		
		NotificationManager.process(uuid, "PERMISSION_REVOKE", owner);
	}
}
