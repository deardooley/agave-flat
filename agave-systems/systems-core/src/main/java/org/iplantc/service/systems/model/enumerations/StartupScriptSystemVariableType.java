package org.iplantc.service.systems.model.enumerations;

import org.apache.log4j.Logger;
import org.hibernate.cfg.NotYetImplementedException;
import org.iplantc.service.systems.model.ExecutionSystem;


public enum StartupScriptSystemVariableType 
{
	// execution system template variables
	SYSTEM_ID,
	SYSTEM_UUID,
	SYSTEM_STORAGE_PROTOCOL,
	SYSTEM_STORAGE_HOST,
	SYSTEM_STORAGE_PORT,
	SYSTEM_STORAGE_RESOURCE,
	SYSTEM_STORAGE_ZONE,
	SYSTEM_STORAGE_ROOTDIR,
	SYSTEM_STORAGE_HOMEDIR,
	SYSTEM_STORAGE_AUTH_TYPE,
	SYSTEM_STORAGE_CONTAINER,
	SYSTEM_LOGIN_PROTOCOL,
	SYSTEM_LOGIN_HOST,
	SYSTEM_LOGIN_PORT,
	SYSTEM_LOGIN_AUTH_TYPE;
	
	private static final Logger log = Logger.getLogger(StartupScriptSystemVariableType.class);

	/**
	 * Resolves a template variable into the actual value for the
	 * system. Tenancy is honored with respect to the system.
	 * 
	 * @param job A valid job object
	 * @return resolved value of the variable.
	 */
	public String resolveForSystem(ExecutionSystem system)
	{
		
		if (this == SYSTEM_ID)
		{
			return system.getSystemId();
		}
		else if (this == SYSTEM_UUID)
		{
			return system.getUuid();
		}
		else if (this == SYSTEM_STORAGE_PROTOCOL)
		{
			return system.getStorageConfig().getProtocol().name();
		}
		else if (this == SYSTEM_STORAGE_HOST)
		{
			return system.getStorageConfig().getHost();
		}
		else if (this == SYSTEM_STORAGE_PORT)
		{
			return String.valueOf(system.getStorageConfig().getPort());
		}
		else if (this == SYSTEM_STORAGE_RESOURCE)
		{
			return system.getStorageConfig().getResource();
		}
		else if (this == SYSTEM_STORAGE_ZONE)
		{
			return system.getStorageConfig().getZone();
		}
		else if (this == SYSTEM_STORAGE_ROOTDIR)
		{
			return system.getStorageConfig().getRootDir();
		}
		else if (this == SYSTEM_STORAGE_HOMEDIR)
		{
			return system.getStorageConfig().getHomeDir();
		}
		else if (this == SYSTEM_STORAGE_AUTH_TYPE)
		{
			return system.getStorageConfig().getDefaultAuthConfig().getType().name();
		}
		else if (this == SYSTEM_STORAGE_CONTAINER)
		{
			return system.getStorageConfig().getContainerName(); 
		}
		else if (this == SYSTEM_LOGIN_PROTOCOL)
		{
			return system.getLoginConfig().getProtocol().name();
		}
		else if (this == SYSTEM_LOGIN_HOST)
		{
			return system.getLoginConfig().getHost();
		}
		else if (this == SYSTEM_LOGIN_PORT)
		{
			return String.valueOf(system.getLoginConfig().getPort());
		}
		else if (this == SYSTEM_LOGIN_AUTH_TYPE)
		{
			return system.getLoginConfig().getDefaultAuthConfig().getType().name();
		}
		else {
			throw new NotYetImplementedException("The startupScript variable " + name() + " is not yet supported.");
		}
	}
}
