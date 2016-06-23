/**
 *
 */
package org.iplantc.service.systems.model;

import java.io.IOException;
import java.util.Arrays;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringUtils;
import org.iplantc.service.systems.exceptions.SystemArgumentException;
import org.iplantc.service.systems.model.enumerations.StorageProtocolType;
import org.iplantc.service.systems.util.ServiceUtils;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author dooley
 *
 */
@Entity
@Table(name = "storageconfigs")
@PrimaryKeyJoinColumn(name="id")
public class StorageConfig extends RemoteConfig implements LastUpdatable
{
	private StorageProtocolType 	protocol;
	private String					rootDir;
	private String					homeDir;
	private String					publicAppsDir;
	private String 					zone;
	private String 					resource;
	private String					containerName;
	private boolean					mirrorPermissions = true;

//	public static final StorageConfig IPLANT_DATA_STORE =
//			new StorageConfig(Settings.IRODS_HOST,
//					Settings.IRODS_PORT,
//					StorageProtocolType.IRODS,
//					Settings.IRODS_STAGING_DIRECTORY,
//					Settings.IRODS_ZONE,
//					Settings.IRODS_DEFAULT_RESOURCE,
//					AuthConfig.IPLANT_IRODS_AUTH_CONFIG);
//
	public StorageConfig() {}

	public StorageConfig(String host, int port,
			StorageProtocolType protocol, AuthConfig authConfig)
	{
		this.host = host;
		this.port = port;
		this.protocol = protocol;
		this.authConfigs.add(authConfig);
	}

	public StorageConfig(String host, int port,
			StorageProtocolType protocol,
			String rootDir,
			String zone,
			String resource,
			AuthConfig authConfig)
	throws SystemArgumentException
	{
		setHost(host);
		setPort(port);
		setProtocol(protocol);
		setZone(zone);
		setResource(resource);
		setRootDir(rootDir);
		addAuthConfig(authConfig);
	}

	/**
	 * @return the protocol
	 */
	@Enumerated(EnumType.STRING)
	@Column(name = "protocol", nullable = false, length = 16)
	public StorageProtocolType getProtocol()
	{
		return protocol;
	}
	/**
	 * @param connectionType the protocol to set
	 */
	public void setProtocol(StorageProtocolType connectionType)
	{
		this.protocol = connectionType;
	}

	/**
	 * @return the rootDir
	 */
	@Column(name = "root_dir", length = 255)
	public String getRootDir()
	{
		return rootDir;
	}

	/**
	 * @param rootDir the rootDir to set
	 */
	public void setRootDir(String rootDir)
	{
		this.rootDir = rootDir;
	}

	/**
	 * @return the mirrorPermissions
	 */
	@Column(name = "mirror_permissions", nullable=false, columnDefinition = "TINYINT(1)")
	public boolean isMirrorPermissions()
	{
		return mirrorPermissions;
	}

	/**
	 * @param mirrorPermissions the mirrorPermissions to set
	 */
	public void setMirrorPermissions(boolean mirrorPermissions)
	{
		this.mirrorPermissions = mirrorPermissions;
	}

	/**
	 * @return the homeDir
	 */
	@Column(name = "home_dir", length = 255)
	public String getHomeDir()
	{
		return homeDir;
	}

	/**
	 * @param homeDir the homeDir to set
	 */
	public void setHomeDir(String homeDir)
	{
		this.homeDir = homeDir;
	}

	/**
	 * @return the publicAppsDir
	 */
	@Column(name = "public_apps_dir", length = 255)
	public String getPublicAppsDir() {
		return publicAppsDir;
	}

	/**
	 * @param publicAppsDir
	 */
	public void setPublicAppsDir(String publicAppsDir) {
		this.publicAppsDir = publicAppsDir;
	}

	/**
	 * @return the zone
	 */
	@Column(name = "zone", length = 255)
	public String getZone()
	{
		return zone;
	}

	/**
	 * @param zone the zone to set
	 */
	public void setZone(String zone)
	{
		this.zone = zone;
	}

	/**
	 * @return the resource
	 */
	@Column(name = "resource")
	public String getResource()
	{
		return resource;
	}

	/**
	 * @param resource the resource to set
	 */
	public void setResource(String resource)
	{
		this.resource = resource;
	}

	/**
	 * Container for a blob/object store account. This can be a bucket,
	 * container, bag, etc depending on the terminology of the provider.
	 * @return
	 */
	@Column(name = "container")
	public String getContainerName()
	{
		return containerName;
	}

	/**
	 * @param containerName
	 */
	public void setContainerName(String containerName)
	{
		this.containerName = containerName;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		return protocol.name() + "  " + host + ":" + port;
	}

	@Override
	public boolean testConnection() throws IOException
	{
		throw new NotImplementedException();
	}

	public static StorageConfig fromJSON(JSONObject jsonConfig) throws SystemArgumentException
	{
		return fromJSON(jsonConfig, null);
	}

	public static StorageConfig fromJSON(JSONObject jsonConfig, StorageConfig config) throws SystemArgumentException
	{
		if (config == null) {
			config = new StorageConfig();
		}

		try {
			if (ServiceUtils.isNonEmptyString(jsonConfig, "host")) {
				config.setHost(jsonConfig.getString("host"));
			} else {
				throw new SystemArgumentException("Please specify a valid string " +
						"value for the 'storage.host' field.");
			}

			if (jsonConfig.has("port") && !jsonConfig.isNull("port"))
			{
				try
				{
					int portNumber = jsonConfig.getInt("port");
					if (portNumber > 0) {
						config.setPort(portNumber);
					} else {
						throw new SystemArgumentException("Invalid 'storage.port' value. " +
								"If provided, please specify a positive integer value for the port number.");
					}
				}
				catch (Exception e) {
					throw new SystemArgumentException("Invalid 'storage.port' value. " +
							"If provided, please specify a positive integer value for the port number.");
				}
			}
			else if (jsonConfig.has("protocol") &&
					!StringUtils.equalsIgnoreCase(jsonConfig.getString("protocol"), StorageProtocolType.LOCAL.name()))
			{
				throw new SystemArgumentException("Invalid 'storage.port' value. " +
						"If provided, please specify a positive integer value for the port number.");
			}
			else {
				config.setPort(null);
			}

			if (jsonConfig.has("rootDir"))
			{
				if (ServiceUtils.isNonEmptyString(jsonConfig, "rootDir")) {
					config.setRootDir(jsonConfig.getString("rootDir"));
				}
				else {
					throw new SystemArgumentException("Invalid 'rootDir' value. " +
							"If provided, please specify a valid root directory path.");
				}
			}
			else {
				config.setRootDir("/");
			}

			if (jsonConfig.has("publicAppsDir"))
			{
				if (ServiceUtils.isNonEmptyString(jsonConfig, "publicAppsDir")) {
					config.setPublicAppsDir(jsonConfig.getString("publicAppsDir"));
				}
				else if (jsonConfig.isNull("publicAppsDir"))
				{
					config.setPublicAppsDir(null);
				}
				else
				{
					throw new SystemArgumentException("Invalid 'publicAppsDir' value. " +
							"If provided, please specify a valid directory path where public "
							+ "apps can be stored on this system if it is made the global default "
							+ "storage system.");
				}
			}
			else {
				config.setPublicAppsDir(null);
			}

			if (jsonConfig.has("homeDir"))
			{
				if (ServiceUtils.isNonEmptyString(jsonConfig, "homeDir")) {
					config.setHomeDir(jsonConfig.getString("homeDir"));
				}
				else {
					throw new SystemArgumentException("Invalid 'homeDir' value. " +
							"If provided, please specify a valid home directory path. " +
							"Home directories cannot be outside of the 'rootDir' if provided.");
				}
			}
			else {
				config.setHomeDir(null);
			}

			if (jsonConfig.has("protocol") && !StringUtils.isEmpty(jsonConfig.getString("protocol")))
			{
				try {
					StorageProtocolType protocolType = StorageProtocolType.valueOf(jsonConfig.getString("protocol").toUpperCase());
					config.setProtocol(protocolType);
				} catch (Exception e) {
					throw new SystemArgumentException("Invalid 'storage.protocol' value. " +
							"Please specify one of: " + ServiceUtils.explode(",",Arrays.asList(StorageProtocolType.values())));
				}

				if (config.getProtocol() == StorageProtocolType.IRODS 
				        || config.getProtocol() == StorageProtocolType.IRODS4) {
					if (jsonConfig.has("zone"))
					{
						if (ServiceUtils.isNonEmptyString(jsonConfig, "zone")) {
							config.setZone(jsonConfig.getString("zone"));
						}
						else {
							throw new SystemArgumentException("Invalid 'storage.zone' value. " +
									"Please specify a valid zone for this irods configuration.");
						}
					} else {
						throw new SystemArgumentException("Invalid 'storage.zone' value. " +
								"Please specify a valid zone for this irods configuration.");
					}

					if (jsonConfig.has("resource"))
					{
						if (ServiceUtils.isNonEmptyString(jsonConfig, "resource")) {
							config.setResource(jsonConfig.getString("resource"));
						}
						else {
							throw new SystemArgumentException("Invalid 'storage.resource' value. " +
									"Please specify a valid default resource for this irods configuration.");
						}
					} else {
						throw new SystemArgumentException("Invalid 'storage.resource' value. " +
								"Please specify a valid default resource for this irods configuration.");
					}

					if (jsonConfig.has("mirror")) {
						config.setMirrorPermissions(jsonConfig.getBoolean("mirror"));
					} else {
						config.setMirrorPermissions(false);
					}

					if (jsonConfig.has("container") && !jsonConfig.isNull("container")) {
						throw new SystemArgumentException("Invalid 'storage.container' value. " +
								"This parameter is only supported on cloud storage systems.");
					} else {
						config.setContainerName(null);
					}
				}
				else
				{
					if (config.getProtocol() == StorageProtocolType.AZURE ||
					        config.getProtocol() == StorageProtocolType.S3 ||
			                config.getProtocol() == StorageProtocolType.SWIFT)
					{
					    if (!StringUtils.startsWithIgnoreCase(config.getHost(), "http")) {
					        config.setHost("https://" + config.getHost());
					    }
//					        throw new SystemArgumentException("Invalid 'storage.host' value. " +
//                                    "Please specify a valid http(s) url endpoint for this cloud storage configuration.");
//					    }

						if (jsonConfig.has("container"))
						{
							if (ServiceUtils.isNonEmptyString(jsonConfig, "container")) {
								config.setContainerName(jsonConfig.getString("container"));
							}
							else {
								throw new SystemArgumentException("Invalid 'storage.container' value. " +
										"Please specify a valid default container for this cloud storage configuration.");
							}
						} else {
							throw new SystemArgumentException("Invalid 'storage.container' value. " +
									"Please specify a valid default container for this cloud storage configuration.");
						}
					}
					else
					{
						if (jsonConfig.has("container") && !jsonConfig.isNull("container")) {
							throw new SystemArgumentException("Invalid 'storage.container' value. " +
									"This parameter is only supported on cloud storage systems.");
						} else {
							config.setContainerName(null);
						}
					}

					if (jsonConfig.has("mirror") && !jsonConfig.isNull("mirror")) {
						throw new SystemArgumentException("Invalid 'storage.mirror' value. " +
								"This parameter is only supported on iRODS systems.");
					} else {
						config.setMirrorPermissions(false);
					}

					if (jsonConfig.has("resource") && !jsonConfig.isNull("resource")) {
						throw new SystemArgumentException("Invalid 'storage.resource' value. " +
								"This parameter is only supported on iRODS systems.");
					} else {
						config.setResource(null);
					}

					if (jsonConfig.has("zone") && !jsonConfig.isNull("zone")) {
						throw new SystemArgumentException("Invalid 'storage.zone' value. " +
								"This parameter is only supported on iRODS systems.");
					} else {
						config.setZone(null);
					}
				}
			} else {
				throw new SystemArgumentException("No 'storage.protocol' value specified. " +
						"Please specify one of: " + ServiceUtils.explode(",",Arrays.asList(StorageProtocolType.values())));
			}

			if (jsonConfig.has("proxy"))
			{
				if (config.getProtocol() == StorageProtocolType.SFTP)
				{
					JSONObject jsonProxy = jsonConfig.getJSONObject("proxy");

					if (jsonProxy == null) {
						throw new SystemArgumentException("Invalid 'storage.proxy' value. Please specify a " +
								"JSON object representing a valid 'storage.proxy' configuration.");
					}
					else
					{
						ProxyServer proxyServer = ProxyServer.fromJSON(jsonProxy, config.getProxyServer());
						proxyServer.setRemoteConfig(config);
						config.setProxyServer(proxyServer);
					}
				}
				else
				{
					throw new SystemArgumentException("Proxy servers are not supported when using " +
							config.getProtocol().name() +
							". To specify a proxy server you must set 'storage.protocol' to 'SFTP'.");
				}
			}
			else
			{
				config.setProxyServer(null);
			}

			if (jsonConfig.has("auth"))
			{
				JSONObject jsonAuth = jsonConfig.getJSONObject("auth");

				if (jsonAuth == null) {
					throw new SystemArgumentException("Invalid 'storage.auth' value. Please specify a " +
							"JSON object representing a valid 'storage.auth' configuration.");
				}
				else
				{
//					// delete old default auth config
//					AuthConfig defaultAuthConfig = config.getDefaultAuthConfig();
//					if (defaultAuthConfig != null) {
//						config.getAuthConfigs().remove(defaultAuthConfig);
//					}

					// add new one
					AuthConfig defaultAuthConfig = config.getDefaultAuthConfig();
					//config.getAuthConfigs().remove(defaultAuthConfig);

					AuthConfig authConfig = AuthConfig.fromJSON(jsonAuth, defaultAuthConfig);
					authConfig.setSystemDefault(true);
					authConfig.setRemoteConfig(config);
					if (defaultAuthConfig == null) {
						config.addAuthConfig(authConfig);
					}
				}
			}
			else if (config.getProtocol() != StorageProtocolType.LOCAL) {
				throw new SystemArgumentException("No 'storage.auth' value specified. Please specify a " +
							"JSON object representing a valid 'storage.auth' configuration.");
			}
		}
		catch (JSONException e) {
			throw new SystemArgumentException("Failed to parse 'storage' object", e);
		}
		catch (Exception e) {
			throw new SystemArgumentException("Failed to parse 'storage' object: " + e.getMessage(), e);
		}

		return config;
	}

	/*
	 * Performs a shallow clone of the StorageConfig. AuthConfigs are not carried over.
	 */
	public StorageConfig clone()
	{
		StorageConfig config = new StorageConfig();
		config.containerName = getContainerName();
        config.homeDir = getHomeDir();
        config.host = getHost();
		config.mirrorPermissions = isMirrorPermissions();
        config.port = getPort();
        config.protocol = getProtocol();
        config.publicAppsDir = getPublicAppsDir();
        config.rootDir = getRootDir();
		config.resource = getResource();
        config.zone = getZone();

        if (proxyServer != null)
            config.proxyServer = proxyServer.clone();

		return config;
	}

	@Override
	@Transient
	public String getType()
	{
		return "storage";
	}
}
