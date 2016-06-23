/**
 * 
 */
package org.iplantc.service.realtime.model;

import java.sql.Timestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Version;

import org.apache.commons.lang.StringUtils;
import org.iplantc.service.common.Settings;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.common.uuid.AgaveUUID;
import org.iplantc.service.common.uuid.UUIDType;
import org.iplantc.service.realtime.exceptions.PermissionValidationException;
import org.iplantc.service.realtime.model.enumerations.PermissionType;
import org.json.JSONException;
import org.json.JSONStringer;
import org.json.JSONWriter;

/**
 * Class to represent individual shared permissions for persistable entities
 * 
 * @author dooley
 * 
 */
@Entity
public class RealtimeChannelPermission {

    @Id
    @GeneratedValue
    @Column(name = "id", unique = true, nullable = false)
    private Long id;
	
    @Column(name = "resource_id", length = 128, nullable = false, unique = true)
    private String resourceId;
    
    @Column(name = "username", nullable = false, length = 32)
    private String username;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "permission", nullable = false, length = 256)
    private PermissionType     permission;
	
    @Column(name = "uuid", length = 128, nullable = false, unique = true)
    private String uuid;
	
	@Version
    @Column(name="OPTLOCK")
    private Integer version = 0;
	
	@Column(name = "created", nullable = false, length = 19)
    private Timestamp created = new Timestamp(System.currentTimeMillis());
	
	@Column(name = "last_updated", nullable = false, length = 19)
    private Timestamp lastUpdated = new Timestamp(System.currentTimeMillis());

	public RealtimeChannelPermission() {
	    this.uuid = new AgaveUUID(UUIDType.PERMISSION).toString();
	}

	public RealtimeChannelPermission(String resourceId, String username,
	        PermissionType permissionType)
	{
		this();
		this.resourceId = resourceId;
		this.username = username;
		this.permission = permissionType;
	}
	
	public RealtimeChannelPermission(RealtimeChannel resource, String username,
	        PermissionType permissionType)
	{
		this();
		this.resourceId = resource.getUuid();
		this.username = username;
		this.permission = permissionType;
	}

	/**
	 * @return the id
	 */
	public Long getId()
	{
		return id;
	}

	/**
	 * @param id
	 *            the id to set
	 */
	public void setId(Long id)
	{
		this.id = id;
	}

	/**
	 * @return the uuid of the resource to which this permission applies
	 */
	public String getResourceId()
	{
		return this.resourceId;
	}

	/**
	 * @param resourceId
	 *            the uuid of the resource to which this permission applies
	 */
	public void setResourceId(String resourceId)
	{
		this.resourceId = resourceId;
	}

	/**
	 * @return the username
	 */
	public String getUsername()
	{
		return username;
	}

	/**
	 * @param username
	 *            the username to set
	 */
	public void setUsername(String username)
	{
		if (!StringUtils.isEmpty(username) && username.length() > 32) {
			throw new PermissionValidationException("'permission.username' must be less than 32 characters");
		}
		
		this.username = username;
	}

	/**
	 * @return the permission
	 */
	public PermissionType getPermission() {
		return permission;
	}

	/**
	 * @param permission
	 *            the permission to set
	 */
	public void setPermission(PermissionType permission) {
		this.permission = permission;
	}

	/**
	 * @param lastUpdated
	 *            the lastUpdated to set
	 */
	public void setLastUpdated(Timestamp lastUpdated) {
		this.lastUpdated = lastUpdated;
	}

	/**
	 * @return the lastUpdated
	 */
	public Timestamp getLastUpdated() {
		return lastUpdated;
	}
	
	/**
     * @return the version
     */
    public Integer getVersion() {
        return version;
    }
    
    /**
     * The permission uuid
     * @return
     */
    public String getUuid()
    {
        return uuid;
    }

    /**
     * @param uuid the uuid to set
     */
    public void setUuid(String uuid)
    {
        this.uuid = uuid;
    }

    
    /**
     * @param version the current version
     */
    public void setVersion(Integer version) {
        this.version = version;
    }

	public boolean canRead()
	{
		if (permission == null)
			return false;
		else 
			return permission.canRead();
	}

	public boolean canWrite()
	{
		if (permission == null)
			return false;
		else 
			return permission.canWrite();
	}
	
	public boolean canExecute()
	{
		if (permission == null)
			return false;
		else 
			return permission.canExecute();
	}
	
	public RealtimeChannelPermission clone()
	{
		RealtimeChannelPermission pem = new RealtimeChannelPermission();
		pem.setLastUpdated(getLastUpdated());
		pem.setPermission(getPermission());
		pem.setUsername(getUsername());
		return pem;
	}

	public String toJSON() throws JSONException 
	{
		JSONWriter writer = new JSONStringer();
		writer.object()
			.key("username").value(username)
			.key("permission").object()
				.key("read").value(canRead())
				.key("write").value(canWrite())
				.key("execute").value(canExecute())
			.endObject()
			.key("_links").object()
			  	.key("self").object()
	        		.key("href").value(TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_REALTIME_SERVICE) + getResourceId() + "/pems/" + getUuid())
	        	.endObject()
	        	.key("channel").object()
	        		.key("href").value(TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_REALTIME_SERVICE) + getResourceId())
	        	.endObject()
	        	.key("profile").object()
	        		.key("href").value(TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_PROFILE_SERVICE) + getUsername())
	        	.endObject()
	        .endObject()
		.endObject();
		return writer.toString();
	}
	
	public String toString()
	{
		return "[" + getResourceId() + "] " + username + " " + permission;
	}
}
