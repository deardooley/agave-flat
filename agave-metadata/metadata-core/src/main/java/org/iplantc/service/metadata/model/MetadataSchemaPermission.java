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
import org.iplantc.service.metadata.Settings;
import org.iplantc.service.metadata.exceptions.MetadataException;
import org.iplantc.service.metadata.model.enumerations.PermissionType;
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
@Table(name = "metadata_schema_permissions", uniqueConstraints=
@UniqueConstraint(columnNames={"schemaId","username"}))
@FilterDef(name="schemaPemTenantFilter", parameters=@ParamDef(name="tenantId", type="string"))
@Filters(@Filter(name="schemaPemTenantFilter", condition="tenant_id=:tenantId"))
public class MetadataSchemaPermission {

	private Long				id;
	private String				schemaId;
	private String				username;
	private PermissionType		permission;
	private Date				lastUpdated = new Date();
	private String 				tenantId;		
	
	public MetadataSchemaPermission() {
		this.setTenantId(TenancyHelper.getCurrentTenantId());
	}
	
	public MetadataSchemaPermission(String schemaId, String username, PermissionType permissionType) throws MetadataException
	{
		this();
		setSchemaId(schemaId);
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
	 * @return the schemaId
	 */
	@Column(name = "schemaId", nullable = false)
	public String getSchemaId()
	{
		return schemaId;
	}

	/**
	 * @param schemaId
	 *            the jobId to set
	 */
	public void setSchemaId(String schemaId)
	{
		this.schemaId = schemaId;
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
     * @throws org.iplantc.service.metadata.exceptions.MetadataException
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
	        		.key("href").value(TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_METADATA_SERVICE) + "schema/" + schemaId + "/pems/" + username)
	        	.endObject()
	        	.key("parent").object()
	        		.key("href").value(TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_METADATA_SERVICE) + "schema/" + schemaId)
	        	.endObject()
	        	.key("profile").object()
	        		.key("href").value(TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_PROFILE_SERVICE) + username)
	        	.endObject()
	        .endObject()
        .endObject();
			
		return writer.toString();
	}
	
	public String toString()
	{
		return "[" + schemaId + "] " + username + " " + permission;
	}

	public boolean equals(Object o)
	{
		if (o instanceof MetadataSchemaPermission) {
			return ( 
				( (MetadataSchemaPermission) o ).schemaId.equals(schemaId) &&
				( (MetadataSchemaPermission) o ).username.equals(username) &&
				( (MetadataSchemaPermission) o ).permission.equals(permission) );
		}
		return false;
	}

}
