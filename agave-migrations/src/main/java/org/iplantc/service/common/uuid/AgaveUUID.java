package org.iplantc.service.common.uuid;

import org.flywaydb.core.internal.util.StringUtils;
import org.iplantc.service.common.exceptions.UUIDException;

public class AgaveUUID {
	private String uniqueId;
	private UUIDType resourceType;
	
	public AgaveUUID(UUIDType type) 
	{
		this.resourceType = type;
		this.uniqueId = new UniqueId().getStringId();
	}
	
	public AgaveUUID(String uuid) throws UUIDException 
	{
		setResourceType(uuid.substring(uuid.lastIndexOf("-")+1));
		setUniqueId(uuid.substring(0, uuid.lastIndexOf("-")));
	}
	
	/**
	 * @return the uniqueId
	 */
	public String getUniqueId()
	{
		return uniqueId;
	}

	/**
	 * @param uniqueId the uniqueId to set
	 */
	public void setUniqueId(String uniqueId)
	{
		this.uniqueId = uniqueId;
	}

	/**
	 * @param type
	 * @throws UUIDException
	 */
	public void setResourceType(String resourceType) throws UUIDException
	{
		if (StringUtils.hasLength(resourceType)) 
			throw new UUIDException("Resource type cannot be null");
		else {
			this.resourceType = UUIDType.getInstance(resourceType.toUpperCase());
		}
	}
	/**
	 * @return the resourceType
	 */
	public UUIDType getResourceType()
	{
		return resourceType;
	}

	/**
	 * @param objectType the objectType to set
	 */
	public void setObjectType(UUIDType objectType)
	{
		this.resourceType = objectType;
	}

	public String toString() 
	{
		return uniqueId + "-" + resourceType.getCode();
	}
	
	public String toLegacyString() 
	{
		return uniqueId + "-" + resourceType.name().toLowerCase();
	}
	
}
