/**
 * 
 */
package org.iplantc.service.systems.model;

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

import org.apache.commons.lang.StringUtils;
import org.iplantc.service.systems.exceptions.SystemException;
import org.iplantc.service.transfer.model.enumerations.PermissionType;
import org.json.JSONException;
import org.json.JSONStringer;
import org.json.JSONWriter;

/**
 * Class to represent individual shared permissions for systems. This has
 * been deprecated in favor of support for user roles.
 * 
 * @see org.iplantc.service.systems.manager.SystemRole
 * @author dooley
 * @deprecated
 * 
 */
@Entity
@Table(name = "systempermissions")
public class SystemPermission implements Comparable<SystemPermission>, LastUpdatable {

	private Long				id;
	private String				username;
	private PermissionType		permission;
	private Date				lastUpdated = new Date();
	private Date				created = new Date();
	
	public SystemPermission() {}

	public SystemPermission(String username, PermissionType permissionType)
	{
		this.username = username;
		this.permission = permissionType;
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
		if (!StringUtils.isEmpty(username) && username.length() > 32) {
			throw new SystemException("'permission.username' must be less than 32 characters.");
		}
		
		this.username = username;
	}

	/**
	 * @return the permission
	 */
	@Column(name="permission",nullable = false, length=32)
	@Enumerated(EnumType.STRING)
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
		.endObject();
		return writer.toString();
	}
	
	public String toString()
	{
		return username + " " + permission;
	}

	public boolean equals(SystemPermission o)
	{
		if (o instanceof SystemPermission) { 
			return ( //( (SystemPermission) o ).system.getId().equals(system.getId()) && 
					( (SystemPermission) o ).username.equals(username) && 
					( (SystemPermission) o ).permission.equals(permission) ); 
		}

		return false;
	}
	
	@Override
	public int compareTo(SystemPermission o)
	{
		if (o == null) {
			throw new NullPointerException();
		} 
		else if (o instanceof SystemPermission) 
		{ 
			if (((SystemPermission) o ).username.equals(username)) {
				if (((SystemPermission) o ).permission.equals(permission) ) {
					return 0;
				} else {
					return Integer.valueOf(((SystemPermission) o ).permission.getUnixValue())
						.compareTo(Integer.valueOf(permission.getUnixValue()));
				}
			}
			else {
				return ((SystemPermission) o ).username.compareTo(username);
			}
		} else {
			throw new ClassCastException("Cannot compare " + o.getClass() + " with " + this.getClass());
		}
	}
}
