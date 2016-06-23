/**
 * 
 */
package org.iplantc.service.systems.resources;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.iplantc.service.common.clients.AgaveLogServiceClient;
import org.iplantc.service.common.representation.IplantErrorRepresentation;
import org.iplantc.service.common.representation.IplantSuccessRepresentation;
import org.iplantc.service.common.resource.AgaveResource;
import org.iplantc.service.profile.dao.InternalUserDao;
import org.iplantc.service.systems.dao.SystemDao;
import org.iplantc.service.systems.exceptions.SystemArgumentException;
import org.iplantc.service.systems.manager.SystemManager;
import org.iplantc.service.systems.model.AuthConfig;
import org.iplantc.service.systems.model.ExecutionSystem;
import org.iplantc.service.systems.model.RemoteConfig;
import org.iplantc.service.systems.model.RemoteSystem;
import org.iplantc.service.systems.model.StorageSystem;
import org.iplantc.service.systems.util.ServiceUtils;
import org.json.JSONObject;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

/**
 * Allows API consumers and system administrators to add credentials for InternalUsers
 * created via the Profile service. Credentials created here are bound to they system
 * specified in the request and the InternalUsers bound to their auth token.
 * 
 * @author dooley
 * 
 */
public class SystemCredentialResource extends AgaveResource 
{
	private static final Logger	log	= Logger.getLogger(SystemCredentialResource.class);

	private String username;
	private String systemId;
	private String internalUsername;
	private String type;
	private SystemDao systemDao;
	private SystemManager manager;
	
	/**
	 * @param context
	 * @param request
	 * @param response
	 */
	public SystemCredentialResource(Context context, Request request, Response response)
	{
		super(context, request, response);

		this.systemId = (String) request.getAttributes().get("systemid");
		
		this.internalUsername = (String) request.getAttributes().get("user");
		
		this.type = (String) request.getAttributes().get("type");
		
		systemDao = new SystemDao();
		
		manager = new SystemManager();
		
		getVariants().add(new Variant(MediaType.APPLICATION_JSON));

	}

	/**
	 * Get operation to list system roles
	 */
	@Override
	public Representation represent(Variant variant) throws ResourceException
	{
		this.username = getAuthenticatedUsername();

		AgaveLogServiceClient.log(AgaveLogServiceClient.ServiceKeys.SYSTEMS02.name(), 
				AgaveLogServiceClient.ActivityKeys.SystemGetCredentials.name(), 
				username, "", getRequest().getClientInfo().getUpstreamAddress());
		
		if (!ServiceUtils.isValid(systemId))
		{
			getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
			return new IplantErrorRepresentation("Invalid system id. " +
							"Please specify an system using its system id.");
		} 
		
		try
		{
			RemoteSystem system = systemDao.findActiveAndInactiveSystemBySystemId(systemId);
			
			if (system == null)
			{
				throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
						"No system found matching " + systemId);
			}
			else if (!system.isAvailable()) 
			{
				throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
						"System has been disabled by the administrator. "
						+ "No credential changes may be applied to a disabled system.");
			}
			else if (!system.getUserRole(username).canAdmin()) {
				throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
						"Individual user credentials may only be managed by system administrators.");
			} 
			
			
			List<RemoteConfig> remoteConfigs = new ArrayList<RemoteConfig>();
			
			if (manager.isManageableByUser(system, username))
			{
				// if specified, verify type is consistent with the system id 
				if (!StringUtils.isEmpty(type)) 
				{
					if (type.equalsIgnoreCase("execution")) {
						if (system instanceof StorageSystem) {
							throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
									"Execution credentials are not supported on storage systems.");
						} else {
							remoteConfigs.add(((ExecutionSystem)system).getLoginConfig());
						}
					} 
					else if (type.equalsIgnoreCase("storage")) 
					{
						remoteConfigs.add(system.getStorageConfig());
					} 
					else 
					{
						throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
								"Unknown system type '" + type + "' given. If  provided, please specify either 'execution' or 'storage'.");
					}
				} 
				else 
				{
					remoteConfigs.add(system.getStorageConfig());
					if (system instanceof ExecutionSystem) {
						remoteConfigs.add(((ExecutionSystem)system).getLoginConfig());
					}
				}
				
				List<String> credentials = new ArrayList<String>();
				
				for (RemoteConfig config: remoteConfigs)
				{
					if (StringUtils.isEmpty(internalUsername))
					{	
						String salt = "";
						AuthConfig defaultAuthConfig = config.getDefaultAuthConfig();
						if (defaultAuthConfig == null) {
							throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
									"No credentials registered for '" + systemId + ".' " +
									"You must registered at least a default credential " +
									"for this sytem before you can use it for anything useful.");
						} else {
							salt = system.getSystemId() + system.getStorageConfig().getHost() + 
									config.getDefaultAuthConfig().getUsername();
						}
						credentials.add(defaultAuthConfig.toJSON(config, salt, system));
						
						for (AuthConfig auth : config.getAuthConfigs())
						{
							// we prevent double entering the system default and exclude
							// any auth configs that are no longer valid for the current
							// remote config. This can happen when the api user updates 
							// the system's storage or login config after setting internal
							// user credentials. We could look for other ways to handle this,
							// but this may be the best way since they can do a quick check
							// whenever they want and if they update, it will simply overwrite
							// the existing config.
							if (!auth.isSystemDefault() && config.getProtocol().accepts(auth.getType()))
							{
								salt = system.getSystemId() + system.getStorageConfig().getHost() + auth.getUsername();
								
								credentials.add(auth.toJSON(config, salt, system));
							}
						}
					} 
					else 
					{
						try 
						{
							// check validate username
							if (new InternalUserDao().getInternalUserByAPIUserAndUsername(username, internalUsername) == null) {
								throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
										"No internal user found matching username '" + internalUsername + "'");
							} 
							else 
							{
								AuthConfig auth = config.getAuthConfigForInternalUsername(internalUsername);
								if (auth == null) {
									throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
											"No credentials registered for '" + internalUsername + "' " +
											"and no default credentials are registered for '" + systemId + ".' " +
											"You must registered at least a default credential " +
											"for this sytem before you can use it for anything useful.");
								}
								String salt = system.getSystemId() + system.getStorageConfig().getHost() + auth.getUsername();
								
								if (config.getProtocol().accepts(auth.getType())) {
									credentials.add(auth.toJSON(config, salt, system));
								}
							}
						} catch (ResourceException e) {
							throw e;
						} catch (Exception e) {
							throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
									"No credentials found for user " + internalUsername);
						}
					}
				}
				
				ObjectMapper mapper = new ObjectMapper();
				ArrayNode jsonArray = mapper.createArrayNode();
				for (int i=Math.min(offset, credentials.size()-1); i< Math.min((limit+offset), credentials.size()); i++)
				{
					jsonArray.add(mapper.readTree(credentials.get(i)));
				}
				
				return new IplantSuccessRepresentation(jsonArray.toString());
			}
			else
			{
				throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
						"User does not have the necessary role to view this system's credentials.");
			}
		}
		catch (ResourceException e) 
		{
			getResponse().setStatus(e.getStatus());
			return new IplantErrorRepresentation(e.getMessage());
		}
		catch (Exception e)
		{
			getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
			return new IplantErrorRepresentation(
					"Failed to retrieve system credentials: " + e.getMessage());
		}
	}

	/**
	 * Post action for adding and updating internal user credentials on a 
	 * registered system. 
	 * 
	 */
	@Override
	public void acceptRepresentation(Representation entity)
	{
		this.username = getAuthenticatedUsername();

		AgaveLogServiceClient.log(AgaveLogServiceClient.ServiceKeys.SYSTEMS02.name(), 
				AgaveLogServiceClient.ActivityKeys.SystemAddCredential.name(), 
				username, "", getRequest().getClientInfo().getUpstreamAddress());
		
		if (!ServiceUtils.isValid(systemId))
		{
			getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
			getResponse().setEntity(new IplantErrorRepresentation("Invalid system id. " +
							"Please specify an system using its system id."));
		}
		
		try
		{
			RemoteSystem system = systemDao.findActiveAndInactiveSystemBySystemId(systemId);
		
			if (system == null)
			{
				throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
						"No shared system found matching " + systemId);
			}
			else if (!system.isAvailable()) 
			{
				throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
						"System has been disabled by the administrator. "
						+ "No credential changes may be applied to a disabled system.");
			}
			else if (!system.getUserRole(username).canAdmin()) {
				throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
						"Individual user credentials may only be managed by system administrators.");
			} 
			
			SystemManager systemManager = new SystemManager();
			
			if (systemManager.isManageableByUser(system, username))
			{
				String name = null;
				String authUsername = null;
				String authPassword = null;
				String credentialType = null;
				String credential = null;
				String irodsZone = null;
				String irodsResource = null;
				String serverProtocol = null;
				String serverEndpoint = null;
				String port = null;
				int serverPort = 0;
				try 
				{
					JSONObject jsonAuthConfig = this.getPostedEntityAsJsonObject(false);
					
                	if (jsonAuthConfig.has("internalUsername"))
                	{
                    	if (StringUtils.isEmpty(internalUsername) && !jsonAuthConfig.isNull("internalUsername")) 
						{
                    		name = jsonAuthConfig.getString("internalUsername");
						} 
                    	else if (!jsonAuthConfig.isNull("internalUsername"))
						{
							if (!internalUsername.equals(jsonAuthConfig.getString("internalUsername"))) {
								throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
										"Internal user '" + internalUsername + "' given in the URL does " +
										"not match the internal user '" + 
										jsonAuthConfig.getString("internalUsername") + "' " +
										"given in the auth config.");
							} else {
								name = internalUsername;
							}
						}
                	}
                	else {
                		name = internalUsername;
                	}
					
					if (new InternalUserDao().getInternalUserByAPIUserAndUsername(username, name) == null) {
						throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
								"No internal user found matching username '" + internalUsername + "'");
					}
					 
					if (!jsonAuthConfig.isNull("username")) {
						authUsername = jsonAuthConfig.getString("username");
					}
					if (!jsonAuthConfig.isNull("password")) {
						authPassword = jsonAuthConfig.getString("password");
					}
					if (!jsonAuthConfig.isNull("type")) {
						credentialType = jsonAuthConfig.getString("type");
					}
					if (!jsonAuthConfig.isNull("credential")) {
						credential = jsonAuthConfig.getString("credential");
					}
					if (!jsonAuthConfig.isNull("irodsResource")) {
						irodsResource = jsonAuthConfig.getString("irodsResource");
					}
					if (!jsonAuthConfig.isNull("irodsZone")) {
						irodsZone = jsonAuthConfig.getString("irodsZone");
					}
					if (jsonAuthConfig.isNull("server")) 
					{
						JSONObject jsonServer = jsonAuthConfig.getJSONObject("server");
						
						if (jsonServer != null) 
						{
							if (!jsonServer.isNull("credentialServerProtocol")) {
								serverProtocol = jsonServer.getString("credentialServerProtocol");
							}
							
							if (!jsonServer.isNull("credentialServerEndpoint")) {
								serverEndpoint = jsonServer.getString("credentialServerEndpoint");
							}
							
							if (!jsonServer.isNull("port")) {
								try {
									serverPort = jsonServer.getInt("credentialServerPort");
								} catch (Exception e) {
									throw new ResourceException(
											Status.CLIENT_ERROR_BAD_REQUEST, 
											"Invalid value for 'port' specified "); 
								}
							}
						}
					}
						
					if (!StringUtils.isEmpty(port)) 
					{
						try {
							serverPort = Integer.parseInt(port);
						} 
						catch (Exception e) 
						{
							throw new ResourceException(
									Status.CLIENT_ERROR_BAD_REQUEST, 
									"Invalid value for 'port' specified "); 
						}
					}
					
					if (StringUtils.isEmpty(systemId)) {
						if (StringUtils.isEmpty(type)) {
							manager.updateAllInternalUserAuthConfig(username, 
									name, authUsername, authPassword, 
									credentialType, credential, irodsResource, 
									irodsZone, serverEndpoint, serverPort, 
									serverProtocol);
						} else {
							manager.updateAllInternalUserAuthConfigOfType(type, username, 
									name, authUsername, authPassword, 
									credentialType, credential, irodsResource, 
									irodsZone, serverEndpoint, serverPort, 
									serverProtocol);
						}
					} 
					else 
					{
						if (StringUtils.isEmpty(type)) {
							manager.updateAllInternalUserAuthConfigOnSystem(system, username, 
									name, authUsername, authPassword, 
									credentialType, credential, irodsResource, 
									irodsZone,serverEndpoint, serverPort, 
									serverProtocol);
						} else {
							manager.updateInternalUserAuthConfigOnSystemOfType(system, type,
									username, name, authUsername, authPassword, 
									credentialType, credential, irodsResource, 
									irodsZone, serverEndpoint, serverPort, 
									serverProtocol);
						}
					}
					
					getResponse().setStatus(Status.SUCCESS_CREATED);
					getResponse().setEntity(new IplantSuccessRepresentation());
				}
				catch (HibernateException e) {
		        	getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
		            getResponse().setEntity(new IplantErrorRepresentation("Unable to save updated internal user credential: \"" + e.getMessage() + "\""));
		            log.error(e);
		            e.printStackTrace();
			    } 
		        catch (SystemArgumentException e) {
					getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
					getResponse().setEntity(new IplantErrorRepresentation("Failed to update credential. " + e.getMessage()));
					log.error(e);
					
				} 
		        catch (ResourceException e) {
					getResponse().setStatus(e.getStatus());
					getResponse().setEntity(new IplantErrorRepresentation(
							e.getMessage()));
					log.error(e);
				}
				catch (Exception e) {
					getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
					getResponse().setEntity(new IplantErrorRepresentation(
							"Failed to update internal user: " + e.getMessage()));
					e.printStackTrace();
					log.error(e);
				}
			} else {
				throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
						"User does not have the necessary role to modify credentials on this system");
			}

		}
		catch (ResourceException e) {
			getResponse().setEntity(
					new IplantErrorRepresentation(e.getMessage()));
			getResponse().setStatus(e.getStatus());
		}
		catch (Exception e)
		{
			getResponse().setEntity(
					new IplantErrorRepresentation("Failed to update system credentials: " + e.getMessage()));
			getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
		}
		
	}

	/* 
	 * Deletes roles for a system. If no user is specified, it deletes all roles.
	 * Otherwise only the user roles are deleted.
	 */
	@Override
	public void removeRepresentations() throws ResourceException
	{
		this.username = getAuthenticatedUsername();

		AgaveLogServiceClient.log(AgaveLogServiceClient.ServiceKeys.SYSTEMS02.name(), 
				AgaveLogServiceClient.ActivityKeys.SystemRemoveCredential.name(), 
				username, "", getRequest().getClientInfo().getUpstreamAddress());
		
		if (!ServiceUtils.isValid(systemId))
		{
			getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
			getResponse().setEntity(new IplantErrorRepresentation("Invalid system id. " +
							"Please specify an system using its system id."));
			
		}
		
		try
		{
			RemoteSystem system = systemDao.findActiveAndInactiveSystemBySystemId(systemId);
		
			if (system == null)
			{
				throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
						"No system found matching " + systemId);
			}
			else if (!system.isAvailable()) 
			{
				throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
						"System has been disabled by the administrator. "
						+ "No credential changes may be applied to a disabled system.");
			}
			else if (!system.getUserRole(username).canAdmin()) {
				throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
						"Individual user credentials may only be managed by system administrators.");
			} 
			
			SystemManager systemManager = new SystemManager();
			
			if (systemManager.isManageableByUser(system, username))
			{
				if (!StringUtils.isEmpty(internalUsername)) 
				{
					if (new InternalUserDao().getInternalUserByAPIUserAndUsername(username, internalUsername) == null) {
						throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
								"No internal user found matching username '" + internalUsername + "'");
					}
					
					if (StringUtils.isEmpty(systemId)) {
						if (StringUtils.isEmpty(type)) {
							manager.removeAllInternalUserAuthConfig(username, internalUsername);
						} else {
							manager.removeAllInternalUserAuthConfigOfType(username, internalUsername, type);
						}
					} 
					else 
					{
						if (StringUtils.isEmpty(type)) {
							manager.removeAllInternalUserAuthConfigOnSystem(system, username, internalUsername);
						} else {
							manager.removeInternalUserAuthConfigOnSystemOfType(system, type, username, internalUsername);
						}
					}
				} 
				else // delete all roles for this system 
				{
					if (StringUtils.isEmpty(systemId)) {
						if (StringUtils.isEmpty(type)) {
							manager.clearAllInternalUserAuthConfig(username);
						} else {
							manager.clearAllInternalUserAuthConfigOfType(username, type);
						}
					} 
					else 
					{
						if (StringUtils.isEmpty(type)) {
							manager.clearAllInternalUserAuthConfigOnSystem(system, username);
						} else {
							manager.clearAllInternalUserAuthConfigOnSystemOfType(system, username, type);
						}
					}
				}	
					
				getResponse().setEntity(new IplantSuccessRepresentation());
			} 
			else 
			{
				throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
						"User does not have the necessary role to update this system");
			}
		}
		catch (ResourceException e) {
			getResponse().setEntity(
					new IplantErrorRepresentation(e.getMessage()));
			getResponse().setStatus(e.getStatus());
		}
		catch (Exception e)
		{
			getResponse().setEntity(
					new IplantErrorRepresentation("Failed to remove system credentials: " + e.getMessage()));
			getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
		}
	}

	@Override
	public boolean allowDelete() {
		return true;
	}

	@Override
	public boolean allowGet() {
		return true;
	}

	@Override
	public boolean allowPost() {
		return true;
	}

	@Override
	public boolean allowPut() {
		return false;
	}
}
