/**
 * 
 */
package org.iplantc.service.metadata.model;

import org.apache.commons.lang.StringUtils;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.Filters;
import org.hibernate.annotations.ParamDef;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.metadata.exceptions.MetadataException;
import org.iplantc.service.metadata.model.enumerations.PermissionType;
import org.iplantc.service.metadata.Settings;
import org.json.JSONException;
import org.json.JSONStringer;
import org.json.JSONWriter;

import javax.persistence.*;

import java.util.Date;

/**
 * Class to represent individual shared permissions for jobs
 * 
 * @author dooley
 * 
 */
@Entity
@Table(name = "metadata_permissions", uniqueConstraints=
@UniqueConstraint(columnNames={"uuid","username"}))
@FilterDef(name="metadataPemTenantFilter", parameters=@ParamDef(name="tenantId", type="string"))
@Filters(@Filter(name="metadataPemTenantFilter", condition="tenant_id=:tenantId"))
public class MetadataPermission {

	private Long				id;
	private String				uuid;
	private String				username;
	private PermissionType		permission;
	private Date				lastUpdated = new Date();
	private String 				tenantId;		
	
	public MetadataPermission() {
		this.setTenantId(TenancyHelper.getCurrentTenantId());
	}

	public MetadataPermission(String uuid, String username, PermissionType permissionType) throws MetadataException
	{
		this();
		setUuid(uuid);
		setUsername(username);
		setPermission(permissionType);
	}

	/**
	 * @return the id
	 */
	@Id
	@GeneratedValue
	@Column(name = "id", unique = true, nullable = false)
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
	 * @return the uuid
	 */
	@Column(name = "uuid", nullable = false)
	public String getUuid()
	{
		return uuid;
	}

	/**
	 * @param uuid
	 *            the jobId to set
	 */
	public void setUuid(String uuid)
	{
		this.uuid = uuid;
	}

	/**
	 * @return the username
	 */
	@Column(name = "username", nullable = false, length = 32)
	public String getUsername()
	{
		return username;
	}

	/**
	 * @param username
	 *            the username to set
     * @throws MetadataException
	 */
	public void setUsername(String username) throws MetadataException
	{
		if (!StringUtils.isEmpty(username) && username.length() > 32) {
			throw new MetadataException("'permission.username' must be less than 32 characters");
		}
		
		this.username = username;
	}

	/**
	 * @return the permission
	 */
	@Enumerated(EnumType.STRING)
	@Column(name = "permission", nullable = false, length = 16)
	public PermissionType getPermission()
	{
		return permission;
	}

	/**
	 * @param permission
	 *            the permission to set
	 */
	public void setPermission(PermissionType permission)
	{
		this.permission = permission;
	}

	public String getTenantId() {
		return tenantId;
	}

	public void setTenantId(String tenantId) {
		this.tenantId = tenantId;
	}

	/**
	 * @param lastUpdated
	 *            the lastUpdated to set
	 */
	public void setLastUpdated(Date lastUpdated)
	{
		this.lastUpdated = lastUpdated;
	}

	/**
	 * @return the lastUpdated
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "lastUpdated", nullable = false, length = 19)
	public Date getLastUpdated()
	{
		return lastUpdated;
	}

	public boolean canRead()
	{
		return permission.canRead();
	}

	public boolean canWrite()
	{
		return permission.canWrite();
	}
	
	public boolean canExecute()
	{
		return permission.canExecute();
	}

	public String toJSON() throws JSONException 
	{
		JSONWriter writer = new JSONStringer();
		writer.object()
			.key("username").value(username)
			.key("permission").object()
				.key("read").value(canRead())
				.key("write").value(canWrite())
			.endObject()
			.key("_links").object()
	        	.key("self").object()
	        		.key("href").value(TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_METADATA_SERVICE) + uuid + "/pems/" + username)
	        	.endObject()
	        	.key("parent").object()
	        		.key("href").value(TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_METADATA_SERVICE) + uuid)
	        	.endObject()
	        	.key("profile").object()
	        		.key("href").value(TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_METADATA_SERVICE) + username)
	        	.endObject()
	        .endObject()
        .endObject();
			
		return writer.toString();
	}
	
	public String toString()
	{
		return "[" + uuid + "] " + username + " " + permission;
	}

	public boolean equals(Object o)
	{
		if (o instanceof MetadataPermission) {
			return ( 
				( (MetadataPermission) o ).uuid.equals(uuid) &&
				( (MetadataPermission) o ).username.equals(username) &&
				( (MetadataPermission) o ).permission.equals(permission) );
		}
		return false;
	}

}
