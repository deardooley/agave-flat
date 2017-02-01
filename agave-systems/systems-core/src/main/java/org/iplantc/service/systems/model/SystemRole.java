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
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import org.apache.commons.lang.StringUtils;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.systems.Settings;
import org.iplantc.service.systems.exceptions.SystemException;
import org.iplantc.service.systems.model.enumerations.RoleType;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Class to represent individual user roles on a system. Roles
 * differ from permissions in that roles supercede permissions and
 * provide for the ability for an entity to be used by other services.
 * 
 * @author dooley
 * 
 */
@Entity
@Table(name = "systemroles")
public class SystemRole implements LastUpdatable, Comparable<SystemRole> {

	private Long				id;
	private String				username;
	private RoleType			role;
	private RemoteSystem		remoteSystem;
	private Date				lastUpdated = new Date();
	private Date				created = new Date();
	
	public SystemRole() {}

	
	public SystemRole(String username, RoleType roleType)
	{
		this.username = username;
		this.role = roleType;
	}
	
	public SystemRole(String username, RoleType roleType, RemoteSystem remoteSystem)
	{
		this.username = username;
		this.role = roleType;
		this.remoteSystem = remoteSystem;
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
	 * @throws SystemException 
	 */
	public void setUsername(String username)
	{
		if (!StringUtils.isEmpty(username) && username.length() > 32) {
			throw new SystemException("'role.username' must be less than 32 characters.");
		}
		
		this.username = username;
	}

	/**
	 * @return the permission
	 */
	@Column(name="role",nullable = false, length=32)
	@Enumerated(EnumType.STRING)
	public RoleType getRole()
	{
		return role;
	}

	/**
	 * @param permission
	 *            the permission to set
	 */
	public void setRole(RoleType role)
	{
		this.role = role;
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
	
	@Transient
	public boolean canUse()
	{
		if (role == null)
			return false;
		else 
			return role.canUse();
	}
	
	@Transient
	public boolean canPublish()
	{
		if (role == null)
			return false;
		else 
			return role.canPublish();
	}

	@Transient
	public boolean canAdmin()
	{
		if (role == null)
			return false;
		else 
			return role.canAdmin();
		
	}
	
	@Transient
	public boolean canRead()
	{
		if (role == null)
			return false;
		else 
			return role.canRead();
	}
	
	@Transient
	public boolean isGuest()
	{
		if (role == null)
			return false;
		else 
			return role.equals(RoleType.GUEST);
	}
	
	public SystemRole clone() {
	    return new SystemRole(getUsername(), getRole());
	}
	
	public String toJSON(RemoteSystem system)  
	{
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode json = mapper.createObjectNode()
			.put("username", username)
			.put("role", role.name());
		ObjectNode hypermedia = json.putObject("_links");
		hypermedia.putObject("self")
	        	  .put("href", TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_SYSTEM_SERVICE) + system.getSystemId() + "/roles/" + username);
	    hypermedia.putObject("parent")    	
	        	  .put("href", TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_SYSTEM_SERVICE) + system.getSystemId());
	    hypermedia.putObject("profile")
	        	  .put("href", TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_PROFILE_SERVICE) + username);
	        	
		return json.toString();
	}
	
	public String toString()
	{
		return username + " " + role;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SystemRole other = (SystemRole) obj;
		
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (role != other.role)
			return false;
		if (username == null) {
			if (other.username != null)
				return false;
		} else if (!username.equals(other.username))
			return false;
		return true;
	}


	@Override
	public int compareTo(SystemRole o) {
		int result = 0;
		
		if (this == o)
			return 0;
		if (o == null)
			return 1;
		if (username == null) {
			if (o.username != null)
				return -1;
		}
		else if (o.username == null) {
			return 1;
		}
		else {
			result = username.compareTo(o.username);
		}
		
		if (result != 0) {
			return result;
		}
		else {
			if (role == null) {
				if (o.role != null) {
					return -1;
				}
				else {
					return 0;
				}
			}
			else {
				if (this.role == null) {
					if (o.role != null)
						return -1;
					else
						return 0;
				}
				else if (o.role == null) {
					return 1;
				}
				else {
					return new Integer(role.intVal()).compareTo(new Integer(o.role.intVal()));
				}
			}
		}
	}


	/**
	 * @return the remoteSystem
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "remote_system_id")
	public RemoteSystem getRemoteSystem() {
		return remoteSystem;
	}


	/**
	 * @param remoteSystem the remoteSystem to set
	 */
	public void setRemoteSystem(RemoteSystem remoteSystem) {
		this.remoteSystem = remoteSystem;
	}
}
