package org.iplantc.service.systems.model;

import java.io.IOException;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import org.apache.commons.lang.StringUtils;
import org.iplantc.service.systems.exceptions.SystemArgumentException;
import org.iplantc.service.systems.exceptions.SystemException;
import org.iplantc.service.systems.model.enumerations.ProtocolType;

/**
 * @author wcs
 *
 */
@Entity
@Table(name = "remoteconfigs")
@Inheritance(strategy=InheritanceType.JOINED)
public abstract class RemoteConfig implements LastUpdatable
{
	protected Long 					id;
	protected String 				host;
	protected Integer				port;
	protected Set<AuthConfig>		authConfigs = new HashSet<AuthConfig>();
	protected Date 					lastUpdated = new Date();
	protected Date 					created = new Date();
	protected ProxyServer			proxyServer;
	
	//protected RemoteSystem 		parentSystem;
	
	public RemoteConfig() {}
	
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
	 * @param id the id to set
	 */
	public void setId(Long id)
	{
		this.id = id;
	}

	/**
	 * @return the host
	 */
	@Column(name = "`host`", nullable = false, length = 256)
	public String getHost()
	{
		return host;
	}
	/**
	 * @param host the host to set
	 * @throws SystemException 
	 */
	public void setHost(String host) 
	{
		if (!StringUtils.isEmpty(host) && host.length() > 256) {
			throw new SystemException("'system." + getType().toLowerCase() + ".host' must be less than 256 characters.");
		}
		
		this.host = host;
	}
	/**
	 * @return the port
	 */
	@Column(name = "`port`")
	public Integer getPort()
	{
		return port;
	}
	/**
	 * @param port the port to set
	 */
	public void setPort(Integer port)
	{
		this.port = port;
	}

	@Transient
	public abstract ProtocolType getProtocol();
	
	@Transient
	public abstract String getType();
	
	/**
	 * Fetches a list of all the AuthConfig for this RemoteConfig
	 * 
	 * @return Set of auth configs for this RemoteConfig
	 */
//	@OneToMany (cascade = CascadeType.ALL, orphanRemoval = true)
//	@JoinTable(
//		name="remoteconfigs_authconfigs",
//		joinColumns={ @JoinColumn(name="remoteconfigs", referencedColumnName="id") },
//		inverseJoinColumns={ @JoinColumn(name="auth_configs", referencedColumnName="id", unique=true) }
//	)
//	@LazyCollection(LazyCollectionOption.FALSE)
	@OneToMany(cascade = CascadeType.ALL, mappedBy = "remoteConfig", fetch=FetchType.EAGER)
	public Set<AuthConfig> getAuthConfigs()
	{
		return authConfigs;
	}
	
	/**
	 * Set the AuthConfigs for this RemoteConfig
	 * 
	 * @param authConfig the authConfig to set
	 */
	public void setAuthConfigs(Set<AuthConfig> authConfigs)
	{
		this.authConfigs = authConfigs;
	}
	
	/**
	 * Add an AuthConfig to the list of existing AuthConfigs. If
	 * a config is already present for the internal user associated 
	 * with this AuthConfig, it will be replaced with the new one.
	 * 
	 * @param authConfig
	 * @throws SystemArgumentException 
	 */
	public void addAuthConfig(AuthConfig authConfig) throws SystemArgumentException
	{
		if (authConfig == null) {
			return;
		} else if (getProtocol().accepts(authConfig.getType())) {
			authConfig.setRemoteConfig(this);
			this.authConfigs.remove(authConfig);
			this.authConfigs.add(authConfig);
		} else {
			throw new SystemArgumentException("Invalid '" + getType() + ".auth' configuration. " +
					"The '" + getType() + ".protocol' value of " + getProtocol() + " does not " +
					"support '" + getType() + ".auth.type' " + authConfig.getType());
		}
	}
	
	/**
	 * Returns the default AuthConfig to use with this system. Usually
	 * this will be the AuthConfig registered with the system. The 
	 * default AuthConfig is used when no internal user AuthConfig
	 * has been defined. 
	 * 
	 * @return default AuthConfig
	 */
	@Transient
    public AuthConfig getDefaultAuthConfig()
	{
		for(AuthConfig authConfig: authConfigs)
		{
			if (authConfig.isSystemDefault()) {
				return authConfig;
			}
		}
		return null;
	}

	/**
	 * Returns the AuthConfig registered for this user. If no AuthConfig
	 * has been added for this user, the default AuthConfig is returned.
	 * 
	 * @param internalUsername username of an InternalUser created by the 
	 * system owner.
	 * @return AuthConfig of the user or default if none has been provided
	 */
	@Transient
	public AuthConfig getAuthConfigForInternalUsername(String internalUsername)
	{
		if (!StringUtils.isEmpty(internalUsername))
		{
			for(AuthConfig authConfig: authConfigs)
			{
				if (StringUtils.equals(authConfig.getInternalUsername(), internalUsername)) {
					return authConfig;
				}
			}
		}
		
		return getDefaultAuthConfig();
	}

	/**
	 * @return the proxyServer
	 */
	@OneToOne(fetch = FetchType.EAGER, cascade={CascadeType.ALL,CascadeType.REMOVE}, targetEntity=ProxyServer.class, orphanRemoval=true )
	@JoinColumn(name = "proxy_server_id")
	public ProxyServer getProxyServer()
	{
		return proxyServer;
	}

	/**
	 * @param proxyServer the proxyServer to set
	 */
	public void setProxyServer(ProxyServer proxyServer)
	{
		this.proxyServer = proxyServer;
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
		result = prime * result + ( ( host == null ) ? 0 : host.hashCode() );
		result = prime * result
				+ ( ( getProtocol() == null ) ? 0 : getProtocol().hashCode() );
		result = prime * result + (getPort() == null ? 0 : getPort().hashCode());
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
		RemoteConfig other = (RemoteConfig) obj;
		if (host == null)
		{
			if (other.host != null)
				return false;
		}
		else if (!host.equals(other.host))
		{
			return false;
		}
		
		if (lastUpdated == null)
		{
			if (other.lastUpdated != null)
				return false;
		}
		else if (!getProtocol().equals(other.getProtocol()))
		{
			return false;
		}
		
		if (getPort() == null) {
			return (other.getPort() != null);
		} else if (other.getPort() == null) {
			return false;
		} else { 
			return (getPort().intValue() == other.getPort().intValue());
		}
	}
	
	@Override
	public String toString()
	{
		return getProtocol().name() + "  " + host + ":" + port;
	}
	
	/**
	 * Checks that the parameters given in the AuthConfig are valid and provide
	 * sufficient information for the service to connect to the remote system.
	 * 
	 * @return true if successful, false otherwise.
	 * @throws IOException
	 */
	public boolean testConnection() throws IOException
	{
		// TODO Auto-generated method stub
		return false;
	}
	
}
