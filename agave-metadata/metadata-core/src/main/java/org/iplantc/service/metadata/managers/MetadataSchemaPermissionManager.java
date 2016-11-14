package org.iplantc.service.metadata.managers;

import org.apache.commons.lang.StringUtils;
import org.iplantc.service.common.auth.AuthorizationHelper;
import org.iplantc.service.common.exceptions.PermissionException;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.metadata.Settings;
import org.iplantc.service.metadata.dao.MetadataSchemaPermissionDao;
import org.iplantc.service.metadata.exceptions.MetadataException;
import org.iplantc.service.metadata.model.MetadataSchemaPermission;
import org.iplantc.service.metadata.model.enumerations.PermissionType;
import org.iplantc.service.metadata.util.ServiceUtils;
import org.iplantc.service.notification.managers.NotificationManager;

public class MetadataSchemaPermissionManager {

	private String schemaId;
    private String owner;

	public MetadataSchemaPermissionManager(String schemaId, String owner) throws MetadataException
	{
		if (schemaId == null) { throw new MetadataException("Schema ID cannot be null"); }
		this.schemaId = schemaId;
        if (owner == null) { throw new MetadataException("Schema owner cannot be null"); }
        this.owner = owner;
	}

	public boolean hasPermission(String username,
			PermissionType jobPermissionType) throws MetadataException
	{

		if (!ServiceUtils.isValid(username)) { return false; }

		if (owner.equals(username) || ServiceUtils.isAdmin(username))
			return true;

		for (MetadataSchemaPermission pem : MetadataSchemaPermissionDao.getBySchemaId(schemaId))
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

		if (!ServiceUtils.isValid(username)) { return false; }

		if (StringUtils.equals(owner, username) || ServiceUtils.isAdmin(username))
			return true;

		for (MetadataSchemaPermission pem : MetadataSchemaPermissionDao.getBySchemaId(schemaId))
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

		if (owner.equals(username) || ServiceUtils.isAdmin(username))
			return true;

		for (MetadataSchemaPermission pem : MetadataSchemaPermissionDao.getBySchemaId(schemaId))
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
		if (!ServiceUtils.isValid(username)) { throw new MetadataException(
				"Invalid username"); }

		if (owner.equals(username))
			return;
		
		if (StringUtils.equals(Settings.PUBLIC_USER_USERNAME, username) || 
				StringUtils.equals(Settings.WORLD_USER_USERNAME, username)) {
			if (!AuthorizationHelper.isTenantAdmin(TenancyHelper.getCurrentEndUser())) {
				throw new PermissionException("User does not have permission to edit public metadata item permissions");
			}
		}
		
		// if the permission is empty or null, delete it
		if (!ServiceUtils.isValid(sPermission) || sPermission.equalsIgnoreCase("none"))
		{
			for (MetadataSchemaPermission pem : MetadataSchemaPermissionDao.getBySchemaId(schemaId))
			{
				if (pem.getUsername().equals(username))
				{
					MetadataSchemaPermissionDao.delete(pem);
					return;
				}
			}
			return;
		}

		PermissionType permissionType = PermissionType
				.valueOf(sPermission.toUpperCase());

		for (MetadataSchemaPermission pem : MetadataSchemaPermissionDao.getBySchemaId(schemaId))
		{
			if (pem.getUsername().equals(username))
			{
				pem.setPermission(permissionType);
				MetadataSchemaPermissionDao.persist(pem);
				return;
			}
		}
		MetadataSchemaPermission pem = new MetadataSchemaPermission(schemaId, username, permissionType);
		MetadataSchemaPermissionDao.persist(pem);
		
		NotificationManager.process(schemaId, "PERMISSION_UPDATE", username);

	}
	
	public void clearPermissions() throws MetadataException
	{
		if (schemaId == null) {
			throw new MetadataException("No object ID specified");
		}
		
		MetadataSchemaPermissionDao.deleteBySchemaId(schemaId);
		
		NotificationManager.process(schemaId, "PERMISSION_UPDATE", owner);
	}
}
