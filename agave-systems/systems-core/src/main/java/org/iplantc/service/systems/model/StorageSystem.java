package org.iplantc.service.systems.model;

import java.net.URLEncoder;
import java.util.Arrays;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.common.uuid.AgaveUUID;
import org.iplantc.service.common.uuid.UUIDType;
import org.iplantc.service.systems.Settings;
import org.iplantc.service.systems.exceptions.SystemArgumentException;
import org.iplantc.service.systems.exceptions.SystemException;
import org.iplantc.service.systems.model.enumerations.AuthConfigType;
import org.iplantc.service.systems.model.enumerations.RemoteSystemType;
import org.iplantc.service.systems.model.enumerations.StorageProtocolType;
import org.iplantc.service.systems.model.enumerations.SystemStatusType;
import org.iplantc.service.systems.util.ServiceUtils;
import org.joda.time.DateTime;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

/**
 * Represents a remote system available for exeuction of applications.
 * 
 * @author dooley
 *
 */
@Entity
@Table(name = "storagesystems")
@PrimaryKeyJoinColumn(name="id")
public class StorageSystem extends RemoteSystem implements SerializableSystem {

//	private StorageProtocolType		protocol;
	private static final Logger log = Logger.getLogger(StorageSystem.class);
	
	public StorageSystem() {
		type = RemoteSystemType.STORAGE;
	}

//	/**
//	 * @return the protocol
//	 */
//	@Enumerated(EnumType.STRING)
//	@Column(name = "protocol", nullable = false, length = 16)
//	public StorageProtocolType getProtocol()
//	{
//		return protocol;
//	}
//
//	/**
//	 * @param protocol the protocol to set
//	 */
//	public void setProtocol(StorageProtocolType protocol)
//	{
//		this.protocol = protocol;
//	}
	
	@Override
	@Enumerated(EnumType.STRING)
	@Column(name = "type", nullable = false, length = 16)
	public RemoteSystemType getType() {
		return RemoteSystemType.STORAGE;
	}
	
	public void setType(RemoteSystemType type) throws SystemArgumentException {
		if (!type.equals(RemoteSystemType.STORAGE)) {
			throw new SystemArgumentException("StorageSystem must be type STORAGE");
		}
	}

	public String toString() {
		return this.getName() + " (" + this.getSystemId() + ") " + storageConfig.getProtocol().name(); 
	}

	@Override
	public String toJSON()
	{
		String output = null;
		JSONStringer js = new JSONStringer();
		try
		{	
			js.object()
				.key("id").value(this.systemId)
				.key("uuid").value(this.uuid)
				.key("name").value(this.name)
				.key("status").value(this.status)
				.key("available").value(this.isAvailable())
				.key("type").value(getType().name())
				.key("description").value(this.description)
				.key("site").value(this.site)
				.key("revision").value(this.revision)
				.key("public").value(isPubliclyAvailable())
//				.key("default").value((isAvailable() && isPubliclyAvailable() && isGlobalDefault()) || this.getUsersUsingAsDefault().contains(TenancyHelper.getCurrentEndUser()))
				.key("globalDefault").value(isGlobalDefault())
				.key("lastModified").value(new DateTime(this.lastUpdated).toString())
				.key("owner").value(getOwner());
				if (storageConfig != null) 
				{
					js.key("storage").object()
						.key("host").value(this.storageConfig.getHost())
						.key("port").value(this.storageConfig.getPort())
						.key("protocol").value(this.storageConfig.getProtocol().name())
						.key("rootDir").value(this.storageConfig.getRootDir())
						.key("homeDir").value(this.storageConfig.getHomeDir())
						.key("publicAppsDir").value(this.storageConfig.getPublicAppsDir());
						if (this.storageConfig.getProtocol() == StorageProtocolType.IRODS || 
								this.storageConfig.getProtocol() == StorageProtocolType.IRODS4) {
							js.key("zone").value(this.storageConfig.getZone());
							js.key("resource").value(this.storageConfig.getResource());
						} 
                        else if (this.storageConfig.getProtocol() == StorageProtocolType.S3 
						        || this.storageConfig.getProtocol() == StorageProtocolType.AZURE) {
						    js.key("container").value(this.storageConfig.getContainerName());
						}
						    
					    js.key("mirror").value(this.storageConfig.isMirrorPermissions());
						if (this.storageConfig.proxyServer != null) {
							js.key("proxy").object()
								.key("name").value(this.storageConfig.getProxyServer().getName())
								.key("host").value(this.storageConfig.getProxyServer().getHost())
								.key("port").value(this.storageConfig.getProxyServer().getPort())
							.endObject();
						} else {
							js.key("proxy").value(null);
						}
						AuthConfig authConfig = storageConfig.getDefaultAuthConfig();
						if (authConfig != null) 
						{
							js.key("auth").object()
								.key("type").value(authConfig.getType());
								if (authConfig.getCredentialServer() != null) 
								{
									js.key("server").object()
										.key("name").value(authConfig.getCredentialServer().getName())
										.key("endpoint").value(authConfig.getCredentialServer().getEndpoint())
										.key("port").value(authConfig.getCredentialServer().getPort())
										.key("protocol").value(authConfig.getCredentialServer().getProtocol().name())
									.endObject();
								}
								if (authConfig.getType() == AuthConfigType.X509) {
		                            js.key("caCerts").value(authConfig.getTrustedCaLocation());
		                        }
							js.endObject();
						}
					js.endObject();
				}
				js.key("_links").object()
	            	.key("self").object()
		        		.key("href").value(TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_SYSTEM_SERVICE) + getSystemId())
		        	.endObject()
		        	.key("roles").object()
		        		.key("href").value(TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_SYSTEM_SERVICE) + getSystemId() + "/roles")
		        	.endObject()
		        	.key("credentials").object()
		        		.key("href").value(TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_SYSTEM_SERVICE) + getSystemId() + "/credentials")
		        	.endObject()
		        	.key("metadata").object()
		    			.key("href").value(TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_METADATA_SERVICE) + "data/?q=" + URLEncoder.encode("{\"associationIds\":\"" + getUuid() + "\"}"))
		    		.endObject();
					if (!isPubliclyAvailable()) {
						js.key("owner").object()
							.key("href").value(TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_PROFILE_SERVICE) + getOwner())
			    		.endObject();
					}
		        js.endObject()
			.endObject();
			
			output = js.toString();
		}
		catch (Exception e)
		{
			log.error("Error producing JSON output for system " + getSystemId());
		}

		return output;
	}
	
	public static StorageSystem fromJSON(JSONObject jsonConfig) throws SystemArgumentException
	{
		return fromJSON(jsonConfig, null);
	}

	public static StorageSystem fromJSON(JSONObject jsonSystem, StorageSystem system) throws SystemException
	{
		if (system == null) {
			system = new StorageSystem();
			system.setUuid(new AgaveUUID(UUIDType.SYSTEM).toString());
		}
		try {
			if (ServiceUtils.isNonEmptyString(jsonSystem, "id")) {
				system.setSystemId(jsonSystem.getString("id"));
			} else {
				throw new SystemArgumentException("Please specify a valid string value for the 'id' field.");
			}
			
			if (ServiceUtils.isNonEmptyString(jsonSystem, "name")) {
				system.setName(jsonSystem.getString("name"));
			} else {
				throw new SystemArgumentException("Please specify a valid string value for the 'name' field.");
			}
			
			if (ServiceUtils.isValidString(jsonSystem, "status"))
			{
				try {
					SystemStatusType statusType = SystemStatusType.valueOf(jsonSystem.getString("status").toUpperCase());
					system.setStatus(statusType);
				} catch (Exception e) {
					throw new SystemArgumentException("Invalid 'status' value. " +
							"Please specify one of: '" + ServiceUtils.explode(", '", Arrays.asList(SystemStatusType.values())) + "'");
				}
			}
			else
			{
				system.setStatus(SystemStatusType.UP);
			}
			
			if (ServiceUtils.isNonEmptyString(jsonSystem, "type"))
			{
				try {
					RemoteSystemType systemType = RemoteSystemType.valueOf(jsonSystem.getString("type").toUpperCase());
					system.setType(systemType);
				} catch (Exception e) {
					throw new SystemArgumentException("Invalid 'type' value. " +
							"Please specify one of: '" + ServiceUtils.explode(", '", Arrays.asList(RemoteSystemType.values())) + "'");
				}
			} 
			else
			{
				throw new SystemArgumentException("No 'type' value specified. " +
						"Please specify one of: '" + ServiceUtils.explode(", '", Arrays.asList(RemoteSystemType.values())) + "'");
			}
			
			if (ServiceUtils.isValidString(jsonSystem, "description")) {
				system.setDescription(jsonSystem.getString("description"));
			} 
			else
			{
				system.setDescription(null);
			}
			
			if (jsonSystem.has("site")) 
			{
				if (ServiceUtils.isNonEmptyString(jsonSystem, "site")) {
					system.setSite(jsonSystem.getString("site"));
				} else {
					throw new SystemArgumentException("Invalid 'site' value. " +
							"If specified, please specify a valid name for the site where this system lives");
				}
			}
			else
			{
				system.setSite(null);
			}
			
			if (jsonSystem.has("available")) 
			{
				try
				{
					boolean isAvailable = jsonSystem.getBoolean("available");
					system.setAvailable(isAvailable);
				} catch (Exception e) {
					throw new SystemArgumentException("Invalid 'available' value. If provided, " +
							"please specify either true or false depending on whether you would " +
							"like this system to be available for use. Default: true");
				}
			} 
			else {
				system.setAvailable(true);
			}
			
//			if (jsonSystem.has("default") && !jsonSystem.isNull("default")) 
//			{
//				try
//				{
//					boolean isDefaultSystem = jsonSystem.getBoolean("default");
//					system.setGlobalDefault(isDefaultSystem);
//				} catch (Exception e) {
//					throw new SystemArgumentException("Invalid 'default' value. If provided, " +
//							"please specify either true or false depending on whether you would " +
//							"like to use this as your default execution system. Default: false");
//				}
//			} 
			
			if (jsonSystem.has("storage"))
			{
				JSONObject jsonStorage = jsonSystem.getJSONObject("storage");
				
				if (jsonStorage == null) {
					throw new SystemArgumentException("Invalid 'storage' value. Please specify a " +
							"JSON object representing a valid 'storage' configuration.");
				} 
				else
				{
					// if the system already exists, the current passwords and credentials are all encrypted
					// using information from the current system. After updating the storage config, we will
					// not be able to decrypt the encrypted fields with the new system information, so
					// we must first decrypt all the passwords, then we can go back and encrypt them again
					// after updating the storage config
					if (system.getStorageConfig() != null) {
						for (AuthConfig authConfig: system.getStorageConfig().getAuthConfigs()) 
						{
							String salt = system.getEncryptionKeyForAuthConfig(authConfig);
							
							if (!StringUtils.isEmpty(authConfig.getPassword())) {
								authConfig.setPassword(authConfig.getClearTextPassword(salt));
							}
							
							if (!StringUtils.isEmpty(authConfig.getCredential())) {
								authConfig.setCredential(authConfig.getClearTextCredential(salt));
							}
							
							if (!StringUtils.isEmpty(authConfig.getPublicKey())) {
								authConfig.setPublicKey(authConfig.getClearTextPublicKey(salt));
							}
							
							if (!StringUtils.isEmpty(authConfig.getPrivateKey())) {
								authConfig.setPrivateKey(authConfig.getClearTextPrivateKey(salt));
							}
						}
					}
					
					StorageConfig storageConfig = StorageConfig.fromJSON(jsonStorage, system.getStorageConfig());
					
					// we encrypt the password after parsing the object. Technically he password
					// needs to be updated every time the loginConfig's hostname changes.
					// In practice we need to updated it on every update, so we do that here.
					for (AuthConfig authConfig: storageConfig.getAuthConfigs()) 
					{
						String salt = system.getEncryptionKeyForAuthConfig(authConfig);
						if (!StringUtils.isEmpty(authConfig.getPassword())) {
							authConfig.encryptCurrentPassword(salt);
						}
						
						if (!StringUtils.isEmpty(authConfig.getCredential())) {
							authConfig.encryptCurrentCredential(salt);
						}
						
						if (!StringUtils.isEmpty(authConfig.getPublicKey())) {
							authConfig.encryptCurrentPublicKey(salt);
						}
						
						if (!StringUtils.isEmpty(authConfig.getPrivateKey())) {
							authConfig.encryptCurrentPrivateKey(salt);
						}
					}
					system.setStorageConfig(storageConfig);
				}
			} 
			else { 
				throw new SystemArgumentException("No 'storage' value specified. Please specify a " +
							"JSON object representing a valid 'storage' configuration.");
			}
			
//			if (jsonSystem.has("protocol") && !StringUtils.isEmpty(jsonSystem.getString("protocol"))) {
//				try {
//					StorageProtocolType protocolType = StorageProtocolType.valueOf(jsonSystem.getString("protocol").toUpperCase());
//					system.setProtocol(protocolType);
//				} catch (JSONException e) {
//					throw new SystemArgumentException("Invalid 'protocol' value. Please specify one of: '" + ServiceUtils.explode(", '", Arrays.asList(StorageProtocolType.values())) + "'");
//				}
//				
//			} else {
//				throw new SystemArgumentException("No 'protocol' value specified. " +
//						"Please specify one of: '" + ServiceUtils.explode(", '", Arrays.asList(StorageProtocolType.values())) + "'");
//			}
		}
		catch (JSONException e) {
            throw new SystemException("Failed to parse storage system.", e);
		}
		catch (SystemArgumentException e) {
            throw new SystemException(e.getMessage(), e);
		}
		catch (Exception e) {
            throw new SystemException("Failed to parse storage system: " + e.getMessage(), e);
		}
		
		return system;
	}
	
	public StorageSystem clone() 
	{
		StorageSystem system = new StorageSystem();
		system.name = name;
		system.description = description;
		system.site = site;
		system.owner = owner;
		system.status = status;
		system.type = type;
		system.globalDefault = globalDefault;
		system.revision = revision;
		system.publiclyAvailable = false;
		system.available = available;
		system.storageConfig = storageConfig.clone();
		
		return system;
	}
}
