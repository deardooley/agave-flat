/**
 *
 */
package org.iplantc.service.transfer.model;

import static javax.persistence.GenerationType.IDENTITY;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.Filters;
import org.hibernate.annotations.ParamDef;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.systems.Settings;
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
@Table(name = "REMOTEFILEPERMISSIONS")
@FilterDef(name="remoteFilePermissionTenantFilter", parameters=@ParamDef(name="tenantId", type="string"))
@Filters(@Filter(name="remoteFilePermissionTenantFilter", condition="tenant_id=:tenantId"))
public class RemoteFilePermission implements Comparable<RemoteFilePermission>, LastUpdatable {

	private Long				id;
	private Long				logicalFileId;
//	private LogicalFile			logicalFile;
	private String				username;
	private String				internalUsername;
	private PermissionType		permission;
	private String				tenantId;
	private boolean				recursive = false;
	private Date				lastUpdated = new Date();
	private Date				created = new Date();

	public RemoteFilePermission() {
		tenantId = TenancyHelper.getCurrentTenantId();
	}

	public RemoteFilePermission(String username, String internalUsername, PermissionType permissionType, boolean recursive)
	{
		this();
		this.username = username;
		this.permission = permissionType;
		this.internalUsername = internalUsername;
		this.setRecursive(recursive);
	}

	public RemoteFilePermission(Long logicalFileId, String username, String internalUsername, PermissionType permissionType, boolean recursive)
	{
		this();
		this.logicalFileId = logicalFileId;
		this.username = username;
		this.permission = permissionType;
		this.internalUsername = internalUsername;
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
	 * @return the internalUsername
	 */
	@Column(name="internal_username",nullable = true, length=32)
	public String getInternalUsername()
	{
		return internalUsername;
	}

	/**
	 * @param internalUsername the internalUsername to set
	 */
	public void setInternalUsername(String internalUsername)
	{
		this.internalUsername = internalUsername;
	}

	/**
	 * @return the permission
	 */
	@Column(name="permission", nullable = false, length=32)
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

//	/**
//	 * @return
//	 */
//	@ManyToOne ( fetch = FetchType.LAZY ) //employ lazy loading, you can put that on @OneToMany too
//	@JoinColumn( name="logicalFileId", nullable=false, insertable=false, updatable=false )
//	public LogicalFile getLogicalFile() {
//		return this.logicalFile;
//	}
//
//	/**
//	 * @param logicalFile
//	 */
//	public void setLogicalFile(LogicalFile logicalFile) {
//		this.logicalFile = logicalFile;
//	}

	/**
	 * @return the logicalFileId
	 */
	@Column( name="logical_file_id", nullable = false, length=20)
	public Long getLogicalFileId()
	{
		return logicalFileId;
	}

	/**
	 * @param logicalFileId the logicalFileId to set
	 */
	public void setLogicalFileId(Long logicalFileId)
	{
		this.logicalFileId = logicalFileId;
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
		String encodedPath = StringUtils.stripToEmpty(path);
		String[] tokens = StringUtils.split(encodedPath, "/");
		List<String> encodedTokens = new ArrayList<String>();
		for(String token:tokens) {
			try {
				encodedTokens.add(URLEncoder.encode(token, "UTF-8"));
			} catch (UnsupportedEncodingException e) {
				encodedTokens.add(URLEncoder.encode(token));
			}
		}
		
		encodedPath = StringUtils.join(encodedTokens, "/");
		encodedPath = StringUtils.replace(encodedPath, "+", "%20");
		
		JSONWriter writer = new JSONStringer();
		writer.object()
			.key("username").value(getUsername())
			.key("internalUsername").value(getInternalUsername())
			.key("permission").object()
				.key("read").value(canRead())
				.key("write").value(canWrite())
				.key("execute").value(canExecute())
			.endObject()
			.key("recursive").value(isRecursive())
			.key("_links").object()
            	.key("self").object()
            		.key("href").value(TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_IO_SERVICE) + "pems/system/" + systemId + "/" + encodedPath + "?username.eq=" + getUsername())
	        	.endObject()
	        	.key("file").object()
            		.key("href").value(TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_IO_SERVICE) + "media/system/" + systemId + "/" + encodedPath)
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
		return username + "[" + internalUsername + "] " + permission;
	}


	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime
				* result
				+ ( ( internalUsername == null ) ? 0 : internalUsername
						.hashCode() );
		result = prime * result
				+ ( ( permission == null ) ? 0 : permission.hashCode() );
		result = prime * result
				+ ( ( username == null ) ? 0 : username.hashCode() );
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RemoteFilePermission other = (RemoteFilePermission) obj;

		if (!StringUtils.equals(internalUsername, other.internalUsername)) {
			return false;
		}
		if (permission != other.permission) {
			return false;
		}
		return StringUtils.equals(username, other.username);
	}

	@Override
	public int compareTo(RemoteFilePermission o)
	{
		if (o == null) {
			throw new NullPointerException();
		}
		else
		{
			if (((RemoteFilePermission) o ).username.equals(username)) {
				if (((RemoteFilePermission) o ).internalUsername.equals(internalUsername) ) {
					if (((RemoteFilePermission) o ).permission == permission ) {
						return 0;
					} else {
						return Integer.valueOf(((RemoteFilePermission) o ).permission.getUnixValue())
							.compareTo(Integer.valueOf(permission.getUnixValue()));
					}
				}
				else
				{
					return ((RemoteFilePermission) o ).internalUsername.compareTo(internalUsername);
				}
			}
			else {
				return ((RemoteFilePermission) o ).username.compareTo(username);
			}
		}
	}

	public RemoteFilePermission clone()
	{
		RemoteFilePermission pem = new RemoteFilePermission(username, internalUsername, permission, recursive);
		pem.setLogicalFileId(logicalFileId);
		return pem;
	}

	public static RemoteFilePermission merge(RemoteFilePermission source, RemoteFilePermission dest)
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
			RemoteFilePermission mergedPem = source.clone();
			mergedPem.setLogicalFileId(source.getLogicalFileId());
			mergedPem.getPermission().add(dest.getPermission());
			mergedPem.setRecursive(mergedPem.isRecursive() || dest.isRecursive());
			return mergedPem;
		}
	}
}
