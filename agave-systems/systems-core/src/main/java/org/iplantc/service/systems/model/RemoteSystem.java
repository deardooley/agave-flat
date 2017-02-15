/**
 *
 */
package org.iplantc.service.systems.model;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import org.apache.commons.lang.StringUtils;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.Filters;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.hibernate.annotations.ParamDef;
import org.iplantc.service.common.auth.TrustedCALocation;
import org.iplantc.service.common.exceptions.PermissionException;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.common.util.Slug;
import org.iplantc.service.common.uuid.AgaveUUID;
import org.iplantc.service.common.uuid.UUIDType;
import org.iplantc.service.notification.managers.NotificationManager;
import org.iplantc.service.systems.Settings;
import org.iplantc.service.systems.dao.SystemRoleDao;
import org.iplantc.service.systems.exceptions.RemoteCredentialException;
import org.iplantc.service.systems.exceptions.RolePersistenceException;
import org.iplantc.service.systems.exceptions.SystemArgumentException;
import org.iplantc.service.systems.exceptions.SystemException;
import org.iplantc.service.systems.exceptions.SystemRoleException;
import org.iplantc.service.systems.manager.SystemRoleManager;
import org.iplantc.service.systems.model.enumerations.RemoteSystemType;
import org.iplantc.service.systems.model.enumerations.RoleType;
import org.iplantc.service.systems.model.enumerations.SystemStatusType;
import org.iplantc.service.systems.util.ServiceUtils;
import org.iplantc.service.transfer.RemoteDataClient;
import org.iplantc.service.transfer.RemoteDataClientFactory;
import org.iplantc.service.transfer.exceptions.RemoteDataException;
import org.json.JSONException;

/**
 * @author dooley
 *
 */
@Entity
@Table(name = "systems", uniqueConstraints=
	@UniqueConstraint(columnNames={"system_id", "tenant_id"}))
@Inheritance(strategy=InheritanceType.JOINED)
@FilterDef(name="systemTenantFilter", parameters=@ParamDef(name="tenantId", type="string"))
@Filters(@Filter(name="systemTenantFilter", condition="tenant_id=:tenantId"))
public abstract class RemoteSystem implements LastUpdatable, Comparable<RemoteSystem>
{
	protected Long					id;
	protected String				name;
	protected String				systemId;
	protected String				description;
	protected String				site;
	protected String 				owner;
	protected Date 					lastUpdated = new Date();
	protected Date 					created = new Date();
	protected SystemStatusType		status = SystemStatusType.UP;
	protected RemoteSystemType		type;
	protected StorageConfig			storageConfig;
	protected boolean				globalDefault = false;
	protected int					revision = 1;
	protected boolean 				publiclyAvailable = false;
	protected boolean 				available = true;
	protected String				uuid;
	protected String				tenantId;
//	private Set<SystemRole> 	    roles = new HashSet<SystemRole>();
	private Set<String>				usersUsingAsDefault = new HashSet<String>();

	public RemoteSystem() {
		this.tenantId = TenancyHelper.getCurrentTenantId();
		this.uuid = new AgaveUUID(UUIDType.SYSTEM).toString();
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
	 * @return the status
	 */
	@Enumerated(EnumType.STRING)
	@Column(name = "`status`", nullable = false, length = 16)
	public SystemStatusType getStatus()
	{
		return status;
	}

	/**
	 * @param status
	 *            the status to set
	 */
	public void setStatus(SystemStatusType status)
	{
		this.status = status;
	}

	/**
	 * @return the description
	 */
	@Column(name = "description", length = 4096)
	public String getDescription()
	{
		return description;
	}

	/**
	 * @param description the description to set
	 * @throws SystemException
	 */
	public void setDescription(String description)
	{
		if (StringUtils.length(description) > 4096) {
			throw new SystemException("'system.description' must be less than 4096 characters.");
		}

		this.description = description;
	}

	/**
	 * @return the owner
	 */
	@Column(name = "`owner`", nullable = false, length = 32)
	public String getOwner()
	{
		return owner;
	}

	/**
	 * @param owner the owner to set
	 * @throws SystemException
	 */
	public void setOwner(String owner)
	{
		if (StringUtils.length(owner) > 32) {
			throw new SystemException("'system.owner' must be less than 32 characters.");
		}

		this.owner = owner;
	}

	/**
	 * @return site
	 */
	@Column(name = "site", length = 64)
	public String getSite()
	{
		return this.site;
	}

	/**
	 * @param site the site to set
	 * @throws SystemException
	 */
	public void setSite(String site)
	{
		if (StringUtils.length(site) > 64) {
			throw new SystemException("'system.site' must be less than 64 characters.");
		}

		this.site = site;
	}

	/**
	 * @return the name
	 */
	@Column(name = "`name`", nullable = false, length = 64)
	public String getName()
	{
		return name;
	}

	/**
	 * @param name the name to set
	 * @throws SystemException
	 */
	public void setName(String name)
	{
		if (StringUtils.length(name) > 64) {
			throw new SystemException("'system.name' must be less than 64 characters.");
		}

		this.name = name;
	}

	/**
	 * @return the systemId
	 */
	@Column(name = "system_id", nullable = false, length = 64)
	public String getSystemId()
	{
		return systemId;
	}

	/**
	 * @param systemId the systemId to set
	 * @throws SystemArgumentException
	 */
	public void setSystemId(String systemId) throws SystemArgumentException
	{
		if (StringUtils.length(systemId) > 64) {
			throw new SystemArgumentException("'system.id' must be less than 64 characters.");
		}

		if (systemId.matches("(execution|storage|authentication|vizualization|instrument)")) {
			throw new SystemArgumentException("System id cannot be the reserve words 'execution', 'storage', or 'auth.'");
		}

		if (!systemId.equals(systemId.replaceAll( "[^0-9a-zA-Z\\.\\-]" , ""))) {
			throw new SystemArgumentException("System id may only contain alphanumeric characters, periods, and dashes.");
		}

		this.systemId = systemId;
	}

	/**
	 * @return the type
	 */
	@Enumerated(EnumType.STRING)
	@Column(name = "`type`", nullable = false, length = 32)
	public RemoteSystemType getType()
	{
		return type;
	}

	/**
	 * @param type the type to set
	 * @throws SystemArgumentException
	 */
	public void setType(RemoteSystemType type) throws SystemArgumentException
	{
		this.type = type;
	}

	/**
	 * @return the storage
	 */
	@OneToOne(fetch = FetchType.EAGER, cascade={CascadeType.ALL,CascadeType.REMOVE})
	@JoinColumn(name = "storage_config")
	public StorageConfig getStorageConfig()
	{
		return storageConfig;
	}

	/**
	 * @param storageConfig the storageConfig to set
	 */
	public void setStorageConfig(StorageConfig storageConfig)
	{
		this.storageConfig = storageConfig;
	}

	/**
	 * @return the globalDefault
	 */
	@Column(name = "global_default", columnDefinition = "TINYINT(1)")
	public boolean isGlobalDefault()
	{
		return globalDefault;
	}

	/**
	 * Should this system be used as a global default.
	 *
	 * @param globalDefault the globalDefault to set
	 */
	public void setGlobalDefault(boolean globalDefault)
	{
		this.globalDefault = globalDefault;
	}

	/**
	 * @return the uuid
	 */
	@Column(name = "uuid", length = 128, nullable = false, unique = true)
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
	 * Returns a set of UserDefaultSystem objects representing the users who have
	 * set this system as their default for systems of this type.
	 * @return the usersUsingAsDefault
	 */
	@ElementCollection
	@CollectionTable(name="userdefaultsystems", joinColumns=@JoinColumn(name="system_id"))
	@Column(name="username")
	@LazyCollection(LazyCollectionOption.FALSE)
	public Set<String> getUsersUsingAsDefault()
	{
		return usersUsingAsDefault;
	}

	/**
	 * @param usersUsingAsDefault the usersUsingAsDefault to set
	 */
	public void setUsersUsingAsDefault(Set<String> usersUsingAsDefault)
	{
		this.usersUsingAsDefault = usersUsingAsDefault;
	}

	/**
	 * @param username the username to set
	 * @throws SystemException
	 */
	public void addUserUsingAsDefault(String username)
	{
		if (!StringUtils.isEmpty(username) && username.length() > 64) {
			throw new SystemException("default username must be less than 32 characters.");
		}

		this.usersUsingAsDefault.add(username);
		NotificationManager.process(uuid, "SET_DEFAULT_SYSTEM", username);
	}

	/**
	 * @return the permissions
	 */
//	@OneToMany (cascade = CascadeType.ALL, orphanRemoval=true)
//	@org.hibernate.annotations.Cascade(org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
//	@LazyCollection(LazyCollectionOption.FALSE)
//	@JoinTable(
//		name="systems_systemroles",
//		joinColumns={ @JoinColumn(name="systems", referencedColumnName="id") },
//		inverseJoinColumns={ @JoinColumn(name="roles", referencedColumnName="id", unique=true) }
//	)
//	@OneToMany(cascade = CascadeType.ALL, mappedBy = "remoteSystem", fetch=FetchType.EAGER, orphanRemoval=true)
//	@org.hibernate.annotations.Cascade(org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
	public List<SystemRole> getRoles()
	{
		try {
			return SystemRoleDao.getSystemRoles(getId());
		}
		catch (Exception e) {
			return new ArrayList<SystemRole>();
		}
//		return roles;
	}
	
	@Transient
	public boolean removeRole(SystemRole role)
	{
		try {
			SystemRoleDao.delete(role);
			return true;
		} catch (RolePersistenceException e) {
			return false;
		}
//		if (getRoles().remove(role)) {
//			role.setRemoteSystem(null);
//			return true;
//		}
//		else {
//			return false;
//		}	
	}

	/**
	 * Returns effective {@link SystemRole} of user after adjusting for 
	 * resource scope, public, and world user roles.
	 * 
	 * @param username
	 * @return a {@link SystemRole} will be returned in all situations. 
	 * Users without a role on the system will get a {@link SystemRole} 
	 * back with {@link RoleType#NONE}.
	 * @deprecated 
	 * @see {@link SystemRoleManager#getUserRole(String)}
	 */
	@Transient
	public SystemRole getUserRole(String username)
	{
		try {
			return new SystemRoleManager(this).getUserRole(username);
		}
		catch (Throwable e) {
			return new SystemRole(username, RoleType.NONE, this);
		}
//		
//		if (StringUtils.isEmpty(username)) {
//			return new SystemRole(username, RoleType.NONE, this);
//		}
//		else if (username.equals(owner))
//		{
//			return new SystemRole(username, RoleType.OWNER, this);
//		}
//		else if (ServiceUtils.isAdmin(username))
//		{
//			return new SystemRole(username, RoleType.ADMIN, this);
//		}
//		else
//		{
//			SystemRole worldRole = new SystemRole(Settings.WORLD_USER_USERNAME, RoleType.NONE);
////			SystemRole publicRole = new SystemRole(Settings.PUBLIC_USER_USERNAME, RoleType.NONE);
//			for(SystemRole role: getRoles()) {
//				if(role.getUsername().equals(username)) {
//					if (role.getRole() == RoleType.PUBLISHER && getType() == RemoteSystemType.STORAGE) {
//						return new SystemRole(username, RoleType.USER, this);
//					} else {
//						return role;
//					}
//				} else if (role.getUsername().equals(Settings.WORLD_USER_USERNAME)) {
//					worldRole = role;
////				} else if (role.getUsername().equals(Settings.PUBLIC_USER_USERNAME)) {
////					publicRole = role;
//				}
//			}
//
//			if ( isPubliclyAvailable())
//			{
//				if (!getType().equals(RemoteSystemType.EXECUTION) && worldRole.canRead())
//				{
//					return new SystemRole(username, RoleType.GUEST, this);
//				}
////				else if (worldRole.canRead() || publicRole.canRead())
////				{
////					if (worldRole.getRole().intVal() >= publicRole.getRole().intVal()) {
////						return worldRole;
////					} else {
////						return publicRole;
////					}
////				}
////				else if (worldRole.canRead())
////				{
////					return worldRole;
////				}
//				else
//				{
//					return new SystemRole(username, RoleType.USER, this);
//				}
//			}
//			else
//			{
//				return new SystemRole(username, RoleType.NONE, this);
//			}
//		}
	}

	/**
	 * @param role the roles to add
	 * @return true if the role was uniquely added to the set
	 * @throws SystemRoleException 
	 * @deprecated
	 * @see {@link SystemRoleManager#setRole(String, RoleType, String)}
	 */
	public boolean addRole(SystemRole role) throws SystemRoleException
	{
		if (role == null) return false;
		
//		role.setRemoteSystem(this);
//		
//		// set will catch duplicates, return whether a 
//		// change was made.
//		return this.roles.add(role);
		
		new SystemRoleManager(this).setRole(role.getUsername(), role.getRole(), TenancyHelper.getCurrentEndUser());
		
		return true;
		
		
	}

	/**
	 * Replaces all existing roles with the given ones. 
	 * Providing a null or empty set will clear all roles
	 * on this {@link RemoteSystem}.
	 * 
	 * @param roles the roles to set
	 * @throws RolePersistenceException 
	 */
	public void setRoles(Set<SystemRole> roles) 
	throws RolePersistenceException
	{
		// remove all existing roles
		SystemRoleDao.clearSystemRoles(getId());
		
		if (roles != null) {
			for (SystemRole role: roles) {
				role.setRemoteSystem(this);
				SystemRoleDao.persist(role);
			}
		}
		
//		this.roles = roles;
	}

	/**
	 * @return the revision
	 */
	@Column(name = "revision")
	public int getRevision()
	{
		return revision;
	}

	/**
	 * @param revision the revision to set
	 */
	public void setRevision(int revision)
	{
		this.revision = revision;
	}

	/**
	 * @return the publiclyAvailable
	 */
	@Column(name = "publicly_available", columnDefinition = "TINYINT(1)")
	public boolean isPubliclyAvailable()
	{
		return publiclyAvailable;
	}

	/**
	 * @param publiclyAvailable the publiclyAvailable to set
	 */
	public void setPubliclyAvailable(boolean publiclyAvailable)
	{
		this.publiclyAvailable = publiclyAvailable;
	}

	/**
	 * @return the available
	 */
	@Column(name = "available", nullable=false, columnDefinition = "TINYINT(1)")
	public boolean isAvailable()
	{
		return available;
	}

	/**
	 * @param available the available to set
	 */
	public void setAvailable(boolean available)
	{
		this.available = available;
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


	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ( ( systemId == null ) ? 0 : systemId.hashCode() );
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
		RemoteSystem other = (RemoteSystem) obj;
		if (systemId == null)
		{
			if (other.systemId != null)
				return false;
		}
		else if (!systemId.equals(other.systemId))
			return false;
		return true;
	}

	public String toString()
	{
		return systemId;
	}

	public abstract String toJSON() throws JSONException;

	public abstract RemoteSystem clone();

	public boolean isOwnedBy(String username)
	{
		if (ServiceUtils.isValid(getOwner()) && ServiceUtils.isValid(username)) {
			return getOwner().equals(username);
		} else {
			return false;
		}
	}
	@Transient
	public RemoteDataClient getRemoteDataClient()
	throws RemoteDataException, RemoteCredentialException
	{
		return getRemoteDataClient(null);
	}

	@Transient
	public RemoteDataClient getRemoteDataClient(String internalUsername)
	throws RemoteDataException, RemoteCredentialException
	{
		return new RemoteDataClientFactory().getInstance(this, internalUsername);
	}

	@Override
	public int compareTo(RemoteSystem obj)
	{
		return this.systemId.compareTo(obj.systemId);
	}

	@Transient
	public String getPublicLink()
	{
		return TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_SYSTEM_SERVICE) + systemId;
	}

	@Transient
	public String getEncryptionKeyForAuthConfig(AuthConfig authConfig) {
		return getSystemId() + authConfig.getRemoteConfig().getHost() + authConfig.getUsername();
	}

	/**
	 * Creates a unique temp directory to hold the trusted ca certs for this
	 * system.
	 *
	 * @param authConfig
	 * @return TrustedCALocation containing location of temp ca cert directory
	 * @throws MalformedURLException
	 * @throws IOException
	 */
	public TrustedCALocation installTrustedCACertifcates(AuthConfig authConfig)
	throws MalformedURLException, IOException
	{
		String trustedCADir = null;

		if (StringUtils.isEmpty(authConfig.getTrustedCaLocation())) {
			trustedCADir = null;
		} else {
			trustedCADir = System.getProperty("java.io.tmpdir")
				+ File.separator + Slug.toSlug(getTenantId())
				+ File.separator + Slug.toSlug(getSystemId());
		}

		TrustedCALocation trustedCALocation = new TrustedCALocation(trustedCADir);
		trustedCALocation.fetchRemoteCACertBundle(authConfig.getTrustedCaLocation());
		return trustedCALocation;
	}

}
