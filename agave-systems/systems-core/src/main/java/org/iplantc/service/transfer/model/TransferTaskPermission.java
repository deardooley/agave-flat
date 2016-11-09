/**
 *
 */
package org.iplantc.service.transfer.model;

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.codehaus.plexus.util.StringUtils;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.Filters;
import org.hibernate.annotations.ParamDef;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.transfer.Settings;
import org.iplantc.service.systems.model.LastUpdatable;
import org.iplantc.service.transfer.model.enumerations.PermissionType;
import org.json.JSONException;
import org.json.JSONStringer;
import org.json.JSONWriter;

/**
 * Class to represent individual shared permissions for files.
 *
 * @author james
 *
 */
@Entity
@Table(name = "transfertaskpermissions")
@FilterDef(name="transferTaskPermissionTenantFilter", parameters=@ParamDef(name="tenantId", type="string"))
@Filters(@Filter(name="transferTaskPermissionTenantFilter", condition="tenant_id=:tenantId"))
public class TransferTaskPermission implements Comparable<TransferTaskPermission>, LastUpdatable {

	private Long				id;
	private Long				transferTaskId;
	private String				username;
	private PermissionType      permission;
    private String				tenantId;
	private boolean				recursive = false;
	private Date				lastUpdated = new Date();
	private Date				created = new Date();

	public TransferTaskPermission() {
		tenantId = TenancyHelper.getCurrentTenantId();
	}

	public TransferTaskPermission(String username, PermissionType permissionType, boolean recursive)
	{
		this();
		this.username = username;
		this.permission = permissionType;
		this.setRecursive(recursive);
	}

	public TransferTaskPermission(Long transferTaskId, String username, PermissionType permissionType, boolean recursive)
	{
		this();
		this.transferTaskId = transferTaskId;
		this.username = username;
		this.permission = permissionType;
		this.setRecursive(recursive);
	}

	/**
	 * @return the id
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
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
	 * @return the username
	 */
	@Column(name="username",nullable = false, length=32)
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
		this.username = username;
	}

	/**
	 * @return the permission
	 */
	@Column(name="permission", nullable = false, length=32)
	@Enumerated(EnumType.STRING)
	public PermissionType getPermission()
	{
		return permission == null ? PermissionType.NONE : permission;
	}

	/**
	 * @param permission
	 *            the permission to set
	 */
	public void setPermission(PermissionType permission)
	{
		this.permission = permission;
	}

	/**
	 * @return the transferTaskId
	 */
	@Column( name="transfertask_id", nullable = false, length=20)
	public Long getTransferTaskId()
	{
		return transferTaskId;
	}

	/**
	 * @param transferTaskId the transferTaskId to set
	 */
	public void setTransferTaskId(Long transferTaskId)
	{
		this.transferTaskId = transferTaskId;
	}

	/**
	 * @return the tenantId
	 */
	@Column(name = "tenant_id", nullable=false, length = 128)
	public String getTenantId()
	{
		return tenantId;
	}

	/**
	 * @param tenantId the tenantId to set
	 */
	public void setTenantId(String tenantId)
	{
		this.tenantId = tenantId;
	}

	/**
	 * @return whether the permission should be recursively applied
	 */
	@Column(name = "is_recursive", columnDefinition = "TINYINT(1)")
	public boolean isRecursive() {
		return recursive;
	}

	/**
	 * @param recursive whether to apply this permission recursively
	 */
	public void setRecursive(boolean recursive) {
		this.recursive = recursive;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "last_updated", nullable = false, length = 19)
	public Date getLastUpdated()
	{
		return this.lastUpdated;
	}

	public void setLastUpdated(Date lastUpdated)
	{
		this.lastUpdated = lastUpdated;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "created", nullable = false, length = 19)
	public Date getCreated()
	{
		return this.created;
	}

	public void setCreated(Date created)
	{
		this.created = created;
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

	public String toJSON(String path, String systemId) throws JSONException
	{
		JSONWriter writer = new JSONStringer();
		writer.object()
			.key("username").value(getUsername())
			.key("permission").object()
				.key("read").value(canRead())
				.key("write").value(canWrite())
				.key("execute").value(canExecute())
			.endObject()
			.key("recursive").value(isRecursive())
			.key("_links").object()
            	.key("transferTask").object()
            		.key("href").value(TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_TRANSFER_SERVICE) + "/" + getTransferTaskId())
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
		return getUsername() + " " + getPermission().name();
	}

	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(TransferTaskPermission p)
	{
		if (p == null) {
			throw new NullPointerException();
		}
		else
		{
			if ( StringUtils.equals(getUsername(),  p.getUsername())) {
			    return getPermission().compareUnixValueTo(p.getPermission());
			}
			else {
				return getUsername().compareTo(p.getUsername());
			}
		}
	}

	public TransferTaskPermission clone()
	{
		TransferTaskPermission pem = new TransferTaskPermission(username, permission, recursive);
		pem.setTransferTaskId(transferTaskId);
		return pem;
	}

	public static TransferTaskPermission merge(TransferTaskPermission source, TransferTaskPermission dest)
	{
		if (source == null && dest == null) {
			return null;
		}
		else if (source == null) {
			return dest;
		}
		else if (dest == null) {
			return source;
		}
		else
		{
			TransferTaskPermission mergedPem = source.clone();
			mergedPem.setTransferTaskId(source.getTransferTaskId());
			mergedPem.setPermission(mergedPem.getPermission().add(dest.getPermission()));
			mergedPem.setRecursive(mergedPem.isRecursive() || dest.isRecursive());
			return mergedPem;
		}
	}
}
