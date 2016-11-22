package org.iplantc.service.systems.model;

import java.net.URLEncoder;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.common.uuid.AgaveUUID;
import org.iplantc.service.common.uuid.UUIDType;
import org.iplantc.service.remote.RemoteSubmissionClient;
import org.iplantc.service.remote.RemoteSubmissionClientFactory;
import org.iplantc.service.systems.Settings;
import org.iplantc.service.systems.exceptions.SystemArgumentException;
import org.iplantc.service.systems.exceptions.SystemException;
import org.iplantc.service.systems.model.enumerations.AuthConfigType;
import org.iplantc.service.systems.model.enumerations.ExecutionType;
import org.iplantc.service.systems.model.enumerations.RemoteSystemType;
import org.iplantc.service.systems.model.enumerations.SchedulerType;
import org.iplantc.service.systems.model.enumerations.StorageProtocolType;
import org.iplantc.service.systems.model.enumerations.SystemStatusType;
import org.iplantc.service.systems.util.ServiceUtils;
import org.joda.time.DateTime;
import org.json.JSONArray;
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
@Table(name = "executionsystems")
@PrimaryKeyJoinColumn(name="id")
public class ExecutionSystem extends RemoteSystem implements SerializableSystem {
	
	private static final Logger log = Logger.getLogger(ExecutionSystem.class);
	
	private LoginConfig 		loginConfig;
	private SchedulerType		scheduler = SchedulerType.UNKNOWN;
	private ExecutionType		executionType;
	private String				workDir = "";
	private String				scratchDir = "";
	private Set<BatchQueue> 	batchQueues = new HashSet<BatchQueue>();
	private Integer				maxSystemJobs = Integer.MAX_VALUE;
	private Integer				maxSystemJobsPerUser = Integer.MAX_VALUE;
	private String				environment;
	private String				startupScript;
	
	public ExecutionSystem() {
		this.type = RemoteSystemType.EXECUTION;
	}

	/**
	 * @return the login
	 */
	@OneToOne(fetch = FetchType.EAGER, cascade={CascadeType.ALL, CascadeType.REMOVE})
	@JoinColumn(name = "login_config")
	public LoginConfig getLoginConfig()
	{
		return loginConfig;
	}

	/**
	 * @param loginConfig the login to set
	 */
	public void setLoginConfig(LoginConfig loginConfig)
	{
		this.loginConfig = loginConfig;
	}

	/**
	 * @return the scheduler
	 */
	@Enumerated(EnumType.STRING)
	@Column(name = "scheduler_type", nullable = false, length = 16)
	public SchedulerType getScheduler()
	{
		return scheduler;
	}

	/**
	 * @param scheduler the scheduler to set
	 */
	public void setScheduler(SchedulerType scheduler)
	{
		this.scheduler = scheduler;
	}
	
	@Override
	@Enumerated(EnumType.STRING)
	@Column(name = "type", nullable = false, length = 16)
	public RemoteSystemType getType() {
		return RemoteSystemType.EXECUTION;
	}
	
	public void setType(RemoteSystemType type) throws SystemArgumentException {
		if (!type.equals(RemoteSystemType.EXECUTION)) {
			throw new SystemArgumentException("ExecutionSystem must be type EXECUTION");
		}
	}

	/**
	 * @return the executionType
	 */
	@Enumerated(EnumType.STRING)
	@Column(name = "execution_type", nullable = false, length = 16)
	public ExecutionType getExecutionType()
	{
		return executionType;
	}

	/**
	 * @param executionType the executionType to set
	 */
	public void setExecutionType(ExecutionType executionType)
	{
		this.executionType = executionType;
	}

	/**
	 * @return the workDir
	 */
	@Column(name = "work_dir", nullable=true, length=255)
	public String getWorkDir()
	{
		return workDir;
	}

	/**
	 * @param workDir the workDir to set
	 * @throws SystemException 
	 */
	public void setWorkDir(String workDir) 
	{
		if (!StringUtils.isEmpty(workDir) && workDir.length() > 255) {
			throw new SystemException("'system.workDir' must be less than 255 characters.");
		}
		
		this.workDir = workDir;
	}

	/**
	 * @return the scratchDir
	 */
	@Column(name = "scratch_dir", nullable=true, length=255)
	public String getScratchDir()
	{
		return scratchDir;
	}

	/**
	 * @param scratchDir the scratchDir to set
	 * @throws SystemException 
	 */
	public void setScratchDir(String scratchDir)
	{
		if (!StringUtils.isEmpty(scratchDir) && scratchDir.length() > 255) {
			throw new SystemException("'system.scratchDir' must be less than 255 characters.");
		}
		
		this.scratchDir = scratchDir;
	}

	/**
	 * @return the queues
	 */
	@OneToMany(cascade = {CascadeType.ALL}, mappedBy = "executionSystem", fetch=FetchType.EAGER, orphanRemoval=true)
	public Set<BatchQueue> getBatchQueues()
	{
		return batchQueues;
	}

	/**
	 * @param queues the queues to set
	 */
	public void setBatchQueues(Set<BatchQueue> queues)
	{
		this.batchQueues = queues;
	}
	
	public void addBatchQueue(BatchQueue queue)
	{
		queue.setExecutionSystem(this);
		if (queue.isSystemDefault()) {
			for (BatchQueue q: this.batchQueues) {
				q.setSystemDefault(false);
			}
		}
		this.batchQueues.add(queue);
	}
	
	public void removeBatchQueue(BatchQueue queue)
	{
		queue.setExecutionSystem(null);
		this.batchQueues.remove(queue);
	}

    /**
	 * @return the maxSystemJobs
	 */
	@Column(name = "max_system_jobs")
	public Integer getMaxSystemJobs()
	{
		return maxSystemJobs;
	}

	/**
	 * @param maxSystemJobs the maxSystemJobs to set
	 */
	public void setMaxSystemJobs(Integer maxSystemJobs)
	{
		if (maxSystemJobs != null && maxSystemJobs < 1) {
			throw new SystemException("system.maxSystemJobs' must be > 0");
		}
		
		this.maxSystemJobs = maxSystemJobs;
	}

	/**
	 * @return the maxSystemJobsPerUser
	 */
	@Column(name = "max_system_jobs_per_user")
	public Integer getMaxSystemJobsPerUser()
	{
		return maxSystemJobsPerUser;
	}

	/**
	 * @param maxSystemJobsPerUser the maxSystemJobsPerUser to set
	 */
	public void setMaxSystemJobsPerUser(Integer maxSystemJobsPerUser)
	{
		if (maxSystemJobsPerUser != null && maxSystemJobsPerUser < 1) {
			throw new SystemException("system.maxSystemJobsPerUser' must be > 0");
		}
		
		this.maxSystemJobsPerUser = maxSystemJobsPerUser;
	}

	@Transient
	public BatchQueue getDefaultQueue()
	{
		for (BatchQueue q: batchQueues) {
			if (q.isSystemDefault()) {
				return q;
			}
		}
        // todo begin spawned from condor lack of default queue
        if(batchQueues.iterator().hasNext()){
            return batchQueues.iterator().next();
        }
        return null;
        // todo end spawned from condor lack of default queue
	}
	
	@Transient
	public BatchQueue getQueue(String queueName) 
	{
	    if (StringUtils.isEmpty(queueName)) return null;
	    
		for (BatchQueue q: batchQueues) {
			if (StringUtils.equals(q.getName(), queueName)) {
				return q;
			}
		}
		return null;
	}

	/**
	 * @return the environment
	 */
	@Column(name = "environment", length = 16384)
	public String getEnvironment()
	{
		return environment;
	}

	/**
	 * @param environment the environment to set
	 */
	public void setEnvironment(String environment)
	{
		if (!StringUtils.isEmpty(environment) && environment.length() > 16384) {
			throw new SystemException("'system.environment' must be less than 16384 characters.");
		}
		
		this.environment = environment;
	}

	/**
	 * @return the startupScript
	 */
	@Column(name = "startup_script", nullable=true, length=255)
	public String getStartupScript()
	{
		return startupScript;
	}

	/**
	 * @param startupScript the startupScript to set
	 */
	public void setStartupScript(String startupScript)
	{
		if (!StringUtils.isEmpty(startupScript) && startupScript.length() > 255) {
			throw new SystemException("'system.startupScript' must be less than 255 characters.");
		}
		
		this.startupScript = startupScript;
	}
	
	public String toString() {
		return this.getSystemId() + " " + scheduler.name() + " " + 
				(loginConfig != null ? loginConfig.getProtocol().name() : ""); 
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
				.key("executionType").value(getExecutionType().name())
				.key("scheduler").value(this.getScheduler())
				.key("environment").value(this.environment)
				.key("startupScript").value(this.startupScript)
				.key("maxSystemJobs").value(this.maxSystemJobs)
				.key("maxSystemJobsPerUser").value(this.maxSystemJobsPerUser)
				.key("workDir").value(this.workDir)
				.key("scratchDir").value(this.scratchDir)
				.key("owner").value(getOwner())
				.key("queues").array();	
					for(BatchQueue queue: getBatchQueues()) {
						js.object()
							.key("name").value(queue.getName())
							.key("default").value(queue.isSystemDefault())
							.key("description").value(queue.getDescription())
							.key("mappedName").value(queue.getMappedName())
							.key("maxJobs").value(queue.getMaxJobs())
							.key("maxUserJobs").value(queue.getMaxUserJobs())
							.key("maxNodes").value(queue.getMaxNodes())
							.key("maxProcessorsPerNode").value(queue.getMaxProcessorsPerNode())
							.key("maxRequestedTime").value(queue.getMaxRequestedTime())
							.key("maxMemoryPerNode").value(queue.getMaxMemoryPerNode())
							.key("customDirectives").value(queue.getCustomDirectives())
						.endObject();
					}
				js.endArray();
				if (loginConfig != null) 
				{
					js.key("login").object()
						.key("host").value(this.loginConfig.getHost())
						.key("port").value(this.loginConfig.getPort())
						.key("protocol").value(this.loginConfig.getProtocol().name());
						if (this.loginConfig.proxyServer != null) {
							js.key("proxy").object()
								.key("name").value(this.loginConfig.getProxyServer().getName())
								.key("host").value(this.loginConfig.getProxyServer().getHost())
								.key("port").value(this.loginConfig.getProxyServer().getPort())
							.endObject();
						} else {
							js.key("proxy").value(null);
						}
						AuthConfig authConfig = loginConfig.getDefaultAuthConfig();
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
				if (storageConfig != null) 
				{
					js.key("storage").object()
						.key("host").value(this.storageConfig.getHost())
						.key("port").value(this.storageConfig.getPort())
						.key("protocol").value(this.storageConfig.getProtocol().name())
						.key("rootDir").value(this.storageConfig.getRootDir())
						.key("homeDir").value(this.storageConfig.getHomeDir());
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
		        	.key("credentials").object()
		        		.key("href").value(TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_SYSTEM_SERVICE) + getSystemId() + "/credentials")
		        	.endObject()
		        	.key("history").object()
		    			.key("href").value(TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_SYSTEM_SERVICE) + getSystemId() + "/history")
		    		.endObject()
		        	.key("metadata").object()
		    			.key("href").value(TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_METADATA_SERVICE) + "data/?q=" + URLEncoder.encode("{\"associationIds\":\"" + getUuid() + "\"}"))
		    		.endObject()
					.key("roles").object()
		        		.key("href").value(TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_SYSTEM_SERVICE) + getSystemId() + "/roles")
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
		catch (Exception e) {
			log.error("Error producing JSON output for system " + getSystemId());
		}

		return output;
	}

	public static ExecutionSystem fromJSON(JSONObject jsonConfig) throws SystemArgumentException
	{
		return fromJSON(jsonConfig, null);
	}
	
	@SuppressWarnings("unused")
	public static ExecutionSystem fromJSON(JSONObject jsonSystem, ExecutionSystem system) throws SystemException
	{
		if (system == null) {
			system = new ExecutionSystem();
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
							"Please specify one of: " + ServiceUtils.explode(",",Arrays.asList(SystemStatusType.values())));
				}
			}
			else {
				system.setStatus(SystemStatusType.UP);
			}
			
			/*
			// should be done at a layer above this
			if (ServiceUtils.isValidString(jsonSystem, "owner"))
			{
				try {
					String owner = jsonSystem.getString("owner");
					LDAPClient client = new LDAPClient(Settings.IRODS_USERNAME, Settings.IRODS_PASSWORD);
					if (client.getUserEmail(owner) == null) {
						throw new SystemArgumentException("No user matching 'owner' was found. " +
								"Please specify a valid API user as the owner of this system");
					} else {
						system.setOwner(owner);
					}
				} catch (JSONException e) {
					throw new SystemArgumentException("Invalid 'owner' value. Please specify one of: " + ServiceUtils.explode(",",Arrays.asList(SystemStatusType.values())));
				}
			} else {
				throw new SystemArgumentException(
						"Please specify a valid API user as the 'owner' of this system");
			}*/
			
			if (ServiceUtils.isNonEmptyString(jsonSystem, "type"))
			{
				try {
					RemoteSystemType systemType = RemoteSystemType.valueOf(jsonSystem.getString("type").toUpperCase());
					system.setType(systemType);
				} catch (Exception e) {
					throw new SystemArgumentException("Invalid 'type' value. " +
							"Please specify one of: " + ServiceUtils.explode(",",Arrays.asList(RemoteSystemType.values())));
				}
			} 
			else
			{
				throw new SystemArgumentException("No 'type' value specified. " +
						"Please specify one of: " + ServiceUtils.explode(",",Arrays.asList(RemoteSystemType.values())));
			}
			
			if (ServiceUtils.isValidString(jsonSystem, "description")) {
				system.setDescription(jsonSystem.getString("description"));
			}
			else {
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
			else {
				system.setSite(null);
			}
			
			if (jsonSystem.has("available") && !jsonSystem.isNull("available")) 
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
			
			if (ServiceUtils.isValidString(jsonSystem, "executionType"))
			{
				try {
					ExecutionType exType = ExecutionType.valueOf(jsonSystem.getString("executionType").toUpperCase());
					system.setExecutionType(exType);
				} catch (JSONException e) {
					throw new SystemArgumentException("Invalid 'executionType' value. " +
							"Please specify one of: " + ServiceUtils.explode(",",Arrays.asList(ExecutionType.values())));
				}
			} 
			else
			{
				throw new SystemArgumentException("No 'executionType' value specified. " +
						"Please specify one of: " + ServiceUtils.explode(",",Arrays.asList(ExecutionType.values())));
			}
			
			if (jsonSystem.has("login"))
			{
				JSONObject jsonLogin = jsonSystem.getJSONObject("login");
				
				if (jsonLogin == null) {
					throw new SystemArgumentException("Invalid 'login' value. Please specify a " +
							"JSON object representing a valid 'login' configuration.");
				}
				else
				{
					// if the system already exists, the current passwords and credentials are all encrypted
					// using information from the current system. After updating the login config, we will
					// not be able to decrypt the encrypted fields with the new system information, so
					// we must first decrypt all the passwords, then we can go back and encrypt them again
					// after updating the login config
					if (system.getLoginConfig() != null) {
						for (AuthConfig authConfig: system.getLoginConfig().getAuthConfigs()) 
						{
							String salt = system.getEncryptionKeyForAuthConfig(authConfig);
							if (!StringUtils.isEmpty(authConfig.getPassword())) {
//								System.out.println("Decrypting login auth config password for " + system.getSystemId());
								authConfig.setPassword(authConfig.getClearTextPassword(salt));
							}
							
							if (!StringUtils.isEmpty(authConfig.getCredential())) {
//								System.out.println("Decrypting login auth config credential for " + system.getSystemId());
								authConfig.setPassword(authConfig.getClearTextCredential(salt));
							}
							
							if (!StringUtils.isEmpty(authConfig.getPublicKey())) {
								authConfig.setPublicKey(authConfig.getClearTextPublicKey(salt));
							}
							
							if (!StringUtils.isEmpty(authConfig.getPrivateKey())) {
								authConfig.setPrivateKey(authConfig.getClearTextPrivateKey(salt));
							}
						}
						//system.getLoginConfig().setAuthConfigs(authConfigs);
					}
					
					LoginConfig loginConfig = LoginConfig.fromJSON(jsonLogin, system.getLoginConfig());
					
					// we encrypt the password after parsing the object. Technically the password
					// needs to be updated every time the loginConfig's hostname changes.
					// In practice we need to updated it on every update, so we do that here.
					int iteration = 0;
					for (AuthConfig authConfig: loginConfig.getAuthConfigs()) 
					{
						
						String salt = system.getEncryptionKeyForAuthConfig(authConfig);
						if (!StringUtils.isEmpty(authConfig.getPassword())) {
//							System.out.println(iteration + ". Encrypting login auth config[" + authConfig.getId() + "] password for " + system.getSystemId());
							authConfig.encryptCurrentPassword(salt);
						}
						
						if (!StringUtils.isEmpty(authConfig.getCredential())) {
//							System.out.println(iteration + ". Encrypting login auth config[" + authConfig.getId() + "] credential for " + system.getSystemId());
							authConfig.encryptCurrentCredential(salt);
						}
						
						if (!StringUtils.isEmpty(authConfig.getPublicKey())) {
							authConfig.encryptCurrentPublicKey(salt);
						}
						
						if (!StringUtils.isEmpty(authConfig.getPrivateKey())) {
							authConfig.encryptCurrentPrivateKey(salt);
						}
						
						iteration++;
					}
					
					//loginConfig.setParentSystem(system);
					system.setLoginConfig(loginConfig);
				}
			} 
			else { 
				throw new SystemArgumentException("No 'login' value specified. Please specify a " +
							"JSON object representing a valid 'login' configuration.");
			}
			
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
//								System.out.println("Decrypting storage auth config password for " + system.getSystemId());
								authConfig.setPassword(authConfig.getClearTextPassword(salt));
							}
							
							if (!StringUtils.isEmpty(authConfig.getCredential())) {
//								System.out.println("Decrypting storage auth config credential for " + system.getSystemId());
								authConfig.setPassword(authConfig.getClearTextCredential(salt));
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
					int iteration = 0;
					for (AuthConfig authConfig: storageConfig.getAuthConfigs()) 
					{
						String salt = system.getEncryptionKeyForAuthConfig(authConfig);
						if (!StringUtils.isEmpty(authConfig.getPassword())) {
//							System.out.println(iteration + ". Encrypting storage auth config[" + authConfig.getId() + "] password for " + system.getSystemId());
							authConfig.encryptCurrentPassword(salt);
						}
						
						if (!StringUtils.isEmpty(authConfig.getCredential())) {
//							System.out.println(iteration + ". Encrypting storage auth config[" + authConfig.getId() + "] credential for " + system.getSystemId());
							authConfig.encryptCurrentCredential(salt);
						}
						
						if (!StringUtils.isEmpty(authConfig.getPublicKey())) {
							authConfig.encryptCurrentPublicKey(salt);
						}
						
						if (!StringUtils.isEmpty(authConfig.getPrivateKey())) {
							authConfig.encryptCurrentPrivateKey(salt);
						}
						
						iteration++;
					}
					//storageConfig.setParentSystem(system);
					system.setStorageConfig(storageConfig);
				}
			} 
			else { 
				throw new SystemArgumentException("No 'storage' value specified. Please specify a " +
							"JSON object representing a valid 'storage' configuration.");
			}
			
			if (ServiceUtils.isValidString(jsonSystem, "scheduler")) 
			{
				try {
					SchedulerType schedulerType = SchedulerType.valueOf(jsonSystem.getString("scheduler").toUpperCase());
					if (system.getExecutionType().equals(ExecutionType.CLI))
					{ 
						if (!schedulerType.equals(SchedulerType.FORK)) {
							throw new SystemArgumentException("Invalid 'scheduler' value. Systems with execution type CLI should have a FORK scheduler type.");
						}
					}
					else if (system.getExecutionType().equals(ExecutionType.CONDOR)) 
					{
						if (!schedulerType.equals(SchedulerType.CONDOR)) {
							throw new SystemArgumentException("Invalid 'scheduler' value. Systems with execution type CONDOR should have a CONDOR scheduler type.");
						}
					}
					else if (system.getExecutionType().equals(ExecutionType.HPC)) 
					{
						if (schedulerType.equals(SchedulerType.CONDOR)) {
							throw new SystemArgumentException("Invalid 'scheduler' value. Systems with scheduler type CONDOR should have a CONDOR execution type.");
						} else if (schedulerType.equals(SchedulerType.FORK)) {
							throw new SystemArgumentException("Invalid 'scheduler' value. Systems with scheduler type FORK should have a CLI execution type.");
						}
					}
					else if (system.getExecutionType().equals(ExecutionType.HPC)) {
						throw new SystemArgumentException("Invalid 'scheduler' value. Atmosphere execution is not supported.");
					}
					
					system.setScheduler(schedulerType);
				} 
				catch (Exception e) 
				{
					throw new SystemArgumentException("Invalid 'scheduler' value. Please specify one of: " + ServiceUtils.explode(",",Arrays.asList(SchedulerType.values())));
				}
			} 
			else if (system.getExecutionType().equals(ExecutionType.CLI))
			{
				system.setScheduler(SchedulerType.FORK);
			}
			else
			{
				throw new SystemArgumentException("Please specify a valid string value for the 'scheduler' field.");
			}
			
			if (jsonSystem.has("workDir") && !jsonSystem.isNull("workDir"))
			{
				if (ServiceUtils.isValidString(jsonSystem, "workDir")) 
				{
					String workDir = jsonSystem.getString("workDir");
					workDir = workDir.trim();
					if (StringUtils.isEmpty(workDir)) {
						workDir = "";
					} else if (!workDir.endsWith("/")) {
						workDir += "/";
					}
					system.setWorkDir(workDir);
				}
				else 
				{
					throw new SystemArgumentException("Invalid 'workDir' value. " +
							"Please specify a valid path to the work directory on " + system.getName());
				}
			} 
			else {
				system.setWorkDir("");
			}
			
			if (jsonSystem.has("scratchDir") && !jsonSystem.isNull("scratchDir"))
			{
				if (ServiceUtils.isValidString(jsonSystem, "scratchDir")) 
				{
					String scratchDir = jsonSystem.getString("scratchDir");
					scratchDir = scratchDir.trim();
					if (StringUtils.isEmpty(scratchDir)) {
						scratchDir = "";
					} else if (!scratchDir.endsWith("/")) {
						scratchDir += "/";
					}
					system.setScratchDir(scratchDir);
				}
				else {
					throw new SystemArgumentException("Invalid 'scratchDir' value. " +
							"Please specify a valid path to the scratch directory on " + system.getName());
				}
			} else {
				system.setScratchDir("");
			}
			
			if (jsonSystem.has("queues"))
			{
				JSONArray jsonQueues = jsonSystem.getJSONArray("queues");
				
				if (jsonQueues == null) {
					throw new SystemArgumentException("Invalid 'queues' value. Please specify a " +
							"JSON array of one or more queue configuration objects.");
				}
				else
				{
					for (BatchQueue queue: system.getBatchQueues()) {
						queue.setExecutionSystem(null);
					}
					system.getBatchQueues().clear();
					
					Set<BatchQueue> queues = new HashSet<BatchQueue>();
					for (int i=0; i<jsonQueues.length(); i++)
					{
						JSONObject jsonQueue = jsonQueues.getJSONObject(i);
						
						if (jsonQueue == null) {
							throw new SystemArgumentException("Invalid value for queue entry " + i + ". Please specify a " +
									"JSON array of batch queue objects.");
						}
						else
						{
							BatchQueue batchQueue = BatchQueue.fromJSON(jsonQueue);
							batchQueue.setExecutionSystem(system);
							// guarantee a single system default
							if (batchQueue.isSystemDefault()) {
								for (BatchQueue q: queues) {
									q.setSystemDefault(false);
								}
							}
							queues.add(batchQueue);
//							system.addBatchQueue(batchQueue);
						}
					}
					system.setBatchQueues(queues);
				}
			} 
			else if (system.getExecutionType().equals(ExecutionType.CLI))
			{
				Set<BatchQueue> queues = new HashSet<BatchQueue>();
				queues.add(new BatchQueue("default"));
				system.setBatchQueues(queues);
			}
			else 
			{ 
				throw new SystemArgumentException("No 'queues' value specified. Please specify a " +
							"JSON array of one or more queue configuration objects.");
			}
			
			if (jsonSystem.has("maxSystemJobs") && !jsonSystem.isNull("maxSystemJobs"))
			{
				try 
				{
					int maxSystemJobs = new Integer(jsonSystem.getString("maxSystemJobs"));
					if (maxSystemJobs > 0) {
						system.setMaxSystemJobs(maxSystemJobs);
					} else {
						throw new SystemArgumentException("Invalid 'maxSystemJobs' value. " +
								"If specified, please specify a valid postive integer value for the " +
								"maximum number of simultaneous jobs allowed on this system.");
					}
				}
				catch (Exception e) {
					throw new SystemArgumentException("Invalid 'maxSystemJobs' value. " +
							"If specified, please specify a valid postive integer value for the " +
							"maximum number of simultaneous jobs allowed on this system.");
				}
			} else {
				system.setMaxSystemJobs(Integer.MAX_VALUE);
			}
			
			if (jsonSystem.has("maxSystemJobsPerUser") && !jsonSystem.isNull("maxSystemJobsPerUser"))
			{
				try 
				{
					int maxSystemJobsPerUser = new Integer(jsonSystem.getString("maxSystemJobsPerUser"));
					if (maxSystemJobsPerUser > 0) {
						system.setMaxSystemJobsPerUser(maxSystemJobsPerUser);
					} else {
						throw new SystemArgumentException("Invalid 'maxSystemJobsPerUser' value. " +
								"If specified, please specify a valid postive integer value for the " +
								"maximum number of simultaneous jobs per user allowed on this system.");
					}
				}
				catch (Exception e) {
					throw new SystemArgumentException("Invalid 'maxSystemJobsPerUser' value. " +
							"If specified, please specify a valid postive integer value for the " +
							"maximum number of simultaneous jobs per user allowed on this system.");
				}
			} else {
				system.setMaxSystemJobsPerUser(Integer.MAX_VALUE);
			}
			
			if (jsonSystem.has("startupScript") && !jsonSystem.isNull("startupScript"))
			{
				if (ServiceUtils.isNonEmptyString(jsonSystem, "startupScript")) {
					system.setStartupScript(jsonSystem.getString("startupScript"));
				} else {
					throw new SystemArgumentException("Invalid 'startupScript' value. " +
							"If provided, please specify a valid path to the startup script " +
							"that should be run prior to execution.");
				}
			}
			else {
				system.setStartupScript(null);
			}
		}
		catch (JSONException e) {
            throw new SystemException("Failed to parse execution system.", e);
		}
		catch (SystemArgumentException e) {
            throw new SystemException(e.getMessage(), e);
		}
		catch (Exception e) {
            throw new SystemException("Failed to parse execution system: " + e.getMessage(), e);
		}
		
		return system;
	}
	
	public ExecutionSystem clone() 
	{
		ExecutionSystem system = new ExecutionSystem();
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
		system.loginConfig = loginConfig.clone();
		system.storageConfig = storageConfig.clone();
		system.scheduler = scheduler;
		system.executionType = executionType;
		system.workDir = workDir;
		system.scratchDir = scratchDir;
		system.environment = environment;
		system.startupScript = startupScript;
		system.maxSystemJobs = maxSystemJobs;
		system.maxSystemJobsPerUser = maxSystemJobsPerUser;
		
		for (BatchQueue queue: getBatchQueues()) {
			BatchQueue q = queue.clone();
			q.setExecutionSystem(system);
			system.getBatchQueues().add(q.clone());
		}
		
		return system;
	}

	@Transient
	public RemoteSubmissionClient getRemoteSubmissionClient(String internalUsername) throws Exception
	{
		return new RemoteSubmissionClientFactory().getInstance(this, internalUsername);
	}
}
