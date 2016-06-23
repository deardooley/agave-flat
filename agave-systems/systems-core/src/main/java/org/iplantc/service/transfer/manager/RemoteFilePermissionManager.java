package org.iplantc.service.transfer.manager;

import org.apache.commons.lang.StringUtils;
import org.iplantc.service.common.exceptions.PermissionException;
import org.iplantc.service.systems.model.RemoteSystem;
import org.iplantc.service.transfer.dao.RemoteFilePermissionDao;
import org.iplantc.service.transfer.model.RemoteFilePermission;
import org.iplantc.service.transfer.model.enumerations.PermissionType;
import org.iplantc.service.transfer.util.ServiceUtils;

/**
 * Handles permissions for a file on a registered RemoteSystem. 
 * 
 * @author dooley
 */
public class RemoteFilePermissionManager 
{
	private RemoteSystem system;
	private Long logicalFileId;
    private String internalUsername;
	
	public RemoteFilePermissionManager(RemoteSystem system, Long logicalFileId, String internalUsername) 
	throws Exception
	{
		if (system == null) {
			throw new PermissionException("System cannot be null");
		}

		if (logicalFileId == null) {
            throw new PermissionException("Logical file reference cannot be null");
        }
		
		this.logicalFileId = logicalFileId;
		this.internalUsername = internalUsername;
		this.system = system;
	}

	public boolean hasPermission(String username, PermissionType permissionType) 
	throws PermissionException
	{
		RemoteFilePermission pem = getPermissionForUser(username);
		return pem.getPermission().getUnixValue() <= permissionType.getUnixValue();
	}

	public boolean canRead(String username) throws PermissionException
	{
		RemoteFilePermission pem = getPermissionForUser(username);
		return pem.getPermission().canRead();
	}

	public boolean canWrite(String username) throws PermissionException 
	{
		RemoteFilePermission pem = getPermissionForUser(username);
		return pem.getPermission().canWrite();
	}
	
	public boolean canExecute(String username) throws PermissionException
	{
		RemoteFilePermission pem = getPermissionForUser(username);
		return pem.getPermission().canExecute();
	}

	public void setPermission(String username, PermissionType type, boolean recursive)
	throws PermissionException
	{
		if (!ServiceUtils.isValid(username)) { 
			throw new PermissionException("Invalid username");
		}
		
		// old permission will be replaced.
		RemoteFilePermission pem = getPermissionForUser(username);
    	if (pem == null) {
    		pem = new RemoteFilePermission(logicalFileId, username, internalUsername, type, recursive);
    	} else {
    		pem.setPermission(type);
    	}
    	RemoteFilePermissionDao.persist(pem);
	}

	public void clearPermissions(String username) throws PermissionException
	{
		RemoteFilePermission pem = getPermissionForUser(username);
		
		if (!pem.getPermission().equals(PermissionType.NONE)) {
			RemoteFilePermissionDao.delete(pem);
		}
	}
	
	public RemoteFilePermission getPermissionForUser(String username)
	throws PermissionException 
	{
		if (StringUtils.isEmpty(username)) 
		{
			return new RemoteFilePermission(logicalFileId, username, internalUsername, PermissionType.NONE, false);
		} 
		else if (system.getUserRole(username).canAdmin() || ServiceUtils.isAdmin(username)) 
		{
			return new RemoteFilePermission(logicalFileId, username, internalUsername, PermissionType.ALL, true);
		} 
		else 
		{
			for (RemoteFilePermission pem: RemoteFilePermissionDao.getBylogicalFileId(logicalFileId)) {
				if (pem.getUsername().equals(username)) {
					return pem;
				}
			}
			return new RemoteFilePermission(logicalFileId, username, internalUsername, PermissionType.NONE, false);
		}
	}
	
}
