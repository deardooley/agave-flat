/**
 * 
 */
package org.iplantc.service.apps.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.apache.commons.lang.StringUtils;
import org.iplantc.service.apps.Settings;
import org.iplantc.service.apps.exceptions.SoftwareException;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.transfer.model.enumerations.PermissionType;
import org.json.JSONException;
import org.json.JSONStringer;
import org.json.JSONWriter;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Class to represent individual shared permissions for jobs
 * 
 * @author dooley
 * 
 */
@Entity
@Table(name = "software_permissions")
public class SoftwarePermission {

	private Long				id;
	private Software			software;
	private String				username;
	private PermissionType		permission;
	private Date				lastUpdated = new Date();

	public SoftwarePermission() {}

	public SoftwarePermission(String username,
			PermissionType permissionType)
	{
		this.username = username;
		this.permission = permissionType;
	}
	
	public SoftwarePermission(Software software, String username,
			PermissionType permissionType)
	{
		this.software = software;
		this.username = username;
		this.permission = permissionType;
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
	 * @return the software
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "software_id")
	public Software getSoftware()
	{
		return software;
	}

	/**
	 * @param software
	 *            the software to set
	 */
	public void setSoftware(Software software)
	{
		this.software = software;
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
	 */
	public void setUsername(String username)
	{
		if (!StringUtils.isEmpty(username) && username.length() > 32) {
			throw new SoftwareException("'permission.username' must be less than 32 characters");
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
	@Column(name = "last_updated", nullable = false, length = 19)
	public Date getLastUpdated()
	{
		return lastUpdated;
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
	
	public SoftwarePermission clone()
	{
		SoftwarePermission pem = new SoftwarePermission();
		pem.setLastUpdated(getLastUpdated());
		pem.setPermission(getPermission());
		pem.setUsername(getUsername());
		return pem;
	}

	@JsonValue
	public String toJSON() throws JSONException 
	{
		String sofwareUniqueName = software.getUniqueName();
		
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
	        		.key("href").value(TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_APPS_SERVICE) + sofwareUniqueName + "/pems/" + username)
	        	.endObject()
	        	.key("app").object()
	        		.key("href").value(TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_APPS_SERVICE) + sofwareUniqueName)
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
		return "[" + software.getUniqueName() + "] " + getUsername() + " " + getPermission();
	}
}
