/**
 * 
 */
package org.iplantc.service.systems.resources;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.common.clients.AgaveLogServiceClient;
import org.iplantc.service.common.clients.AgaveProfileServiceClient;
import org.iplantc.service.common.exceptions.PermissionException;
import org.iplantc.service.common.representation.IplantErrorRepresentation;
import org.iplantc.service.common.representation.IplantSuccessRepresentation;
import org.iplantc.service.common.resource.AgaveResource;
import org.iplantc.service.systems.Settings;
import org.iplantc.service.systems.dao.SystemDao;
import org.iplantc.service.systems.exceptions.SystemArgumentException;
import org.iplantc.service.systems.exceptions.SystemException;
import org.iplantc.service.systems.exceptions.SystemUnavailableException;
import org.iplantc.service.systems.exceptions.SystemUnknownException;
import org.iplantc.service.systems.manager.SystemManager;
import org.iplantc.service.systems.manager.SystemRoleManager;
import org.iplantc.service.systems.model.RemoteSystem;
import org.iplantc.service.systems.model.SystemRole;
import org.iplantc.service.systems.model.enumerations.RoleType;
import org.iplantc.service.systems.util.ServiceUtils;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;

/**
 * Handles system sharing interfaces
 * 
 * @author dooley
 * 
 */
public class SystemRoleResource extends AgaveResource 
{
	private static final Logger log = Logger.getLogger(SystemRoleResource.class);	
	
	private String username;
	private String systemId;
	private String sharedUsername;
	private SystemDao dao;
	
	/**
	 * @param context
	 * @param request
	 * @param response
	 */
	public SystemRoleResource(Context context, Request request, Response response)
	{
		super(context, request, response);

		this.systemId = (String) request.getAttributes().get("systemid");
		
		this.sharedUsername = (String) request.getAttributes().get("user");
		
		dao = new SystemDao();
		
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
				AgaveLogServiceClient.ActivityKeys.SystemListRoles.name(), 
				username, "", getRequest().getClientInfo().getUpstreamAddress());
		
		if (!ServiceUtils.isValid(systemId))
		{
			getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
			return new IplantErrorRepresentation("Invalid system id. " +
							"Please specify an system using its system id. ");
		} 
		
		try
		{
			RemoteSystem system = dao.findActiveAndInactiveSystemBySystemId(systemId);
		
			if (system == null)
			{
				throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
						"No shared system found matching " + systemId);
			}
			else if (!system.isAvailable()) 
			{
				throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
						"System has been disabled by the administrator. "
						+ "No roles changes may be applied to a disabled system.");
			}
			
			SystemManager systemManager = new SystemManager();
			
			if (systemManager.isManageableByUser(system, username))
			{
				String jsonPermissions = "";
				
				if (StringUtils.isEmpty(sharedUsername) ) 
				{
					// add the owner 
					jsonPermissions = new SystemRole(system.getOwner(), RoleType.OWNER).toJSON(system);
						
					List<SystemRole> roles = new ArrayList<SystemRole>(system.getRoles());
					
					Collections.sort(roles);
					
					for (int i=offset; i< Math.min((limit+offset), roles.size()); i++)
					{
						SystemRole role = roles.get(i);
						
						jsonPermissions += "," + role.toJSON(system);
					}
					
					jsonPermissions = "[" + jsonPermissions + "]";
				} 
				else 
				{
					SystemRole role = system.getUserRole(sharedUsername);
					
					try 
					{
						// check validate username
						// we back off this for now to prevent people using this as a lookup service
						AgaveProfileServiceClient authClient = new AgaveProfileServiceClient(Settings.IPLANT_PROFILE_SERVICE, Settings.IRODS_USERNAME, Settings.IRODS_PASSWORD);
						if (authClient.getUser(sharedUsername) == null) 
						{
							throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
									"No roles found for user " + sharedUsername);
						} 
						else 
						{
							if (role == null || !role.canRead()) {
								if (ServiceUtils.isAdmin(sharedUsername)) {
									role = new SystemRole(sharedUsername, RoleType.ADMIN);
								} else {
									throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
											"No roles found for user " + sharedUsername);
								}
							}
							
							jsonPermissions = role.toJSON(system);
						}
					} catch (Exception e) {
						throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
								"No roles found for user " + sharedUsername);
					}
				}
				
				return new IplantSuccessRepresentation(jsonPermissions);
			}
			else if (system.isPubliclyAvailable()) 
			{
				if (StringUtils.isEmpty(sharedUsername)) {
					SystemRole role = system.getUserRole(username);
					return new IplantSuccessRepresentation("[" + role.toJSON(system) + "]");
				} else {
					throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
							"User does not have the necessary role to view other user roles on this system.");
				}
			}
			else if (system.getUserRole(username).canRead()) {
			    if (StringUtils.isEmpty(sharedUsername)) {
                    SystemRole role = system.getUserRole(username);
                    return new IplantSuccessRepresentation("[" + role.toJSON(system) + "]");
                } else {
                    throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
                            "User does not have the necessary role to view other user roles on this system.");
                }
			}
			else
			{
				throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
						"User does not have the necessary role to view this system.");
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
			return new IplantErrorRepresentation("Failed to retrieve system roles: " + e.getMessage());
		}
	}

	/**
	 * Post action for adding (and overwriting) roles on a system object
	 */
	@Override
	public void acceptRepresentation(Representation entity)
	{
		this.username = getAuthenticatedUsername();

		AgaveLogServiceClient.log(AgaveLogServiceClient.ServiceKeys.SYSTEMS02.name(), 
				AgaveLogServiceClient.ActivityKeys.SystemEditRoles.name(), 
				username, "", getRequest().getClientInfo().getUpstreamAddress());
		
		SystemManager systemManager = new SystemManager();
        
		try
		{
			if (!ServiceUtils.isValid(systemId))
			{
				throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
						"Please specify an system using its system id. ");
			}

			RemoteSystem system = dao.findActiveAndInactiveSystemBySystemId(systemId);
		
			if (system == null)
			{
				throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
						"No shared system found matching " + systemId,
						new SystemUnknownException());
			}
			else if (!system.isAvailable()) 
			{
				throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
						"System has been disabled by the administrator. "
						+ "No roles changes may be applied to a disabled system.", 
						new SystemUnavailableException());
			}
			else if (system.isPubliclyAvailable() && !systemManager.isManageableByUser(system, username)) 
			{
				throw new ResourceException(Status.SERVER_ERROR_NOT_IMPLEMENTED,
						"Share roles are not suported on public systems.",
						new NotImplementedException());
			}
			
			if (systemManager.isManageableByUser(system, username))
			{
				// parse the form to get the job specs
				Map<String, String> postData = getPostedEntityAsMap();
	
				try
				{
					String name = null;
					if (StringUtils.isEmpty(sharedUsername)) 
					{
						 name = postData.get("username");
					} 
					else if (postData.containsKey("username") && 
							!StringUtils.equalsIgnoreCase(postData.get("username"), sharedUsername)) 
					{
						throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
								"The username value in the POST body, " + postData.get("username") + 
	                			", does not match the username in the URL, " + sharedUsername,
								new SystemArgumentException());         
					}
					else
					{
						name = sharedUsername;
					}
				
					
					if (StringUtils.isEmpty(name) || StringUtils.equals(name, "null")) { 
						throw new ResourceException(
							Status.CLIENT_ERROR_BAD_REQUEST, "No user found matching " + name,
							new FileNotFoundException()); 
					} 
					else 
					{
						// validate the user they are giving roles to exists
						AgaveProfileServiceClient profileServiceClient = 
								new AgaveProfileServiceClient(Settings.IPLANT_PROFILE_SERVICE, Settings.IRODS_USERNAME, Settings.IRODS_PASSWORD);
						
						if (profileServiceClient.getUser(name) == null) {
							throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
								"No user found matching " + name,
								new FileNotFoundException());
						}
					}
	
					String sRole = null;
					if (postData.containsKey("role")) 
					{
						sRole = postData.get("role");
					} 
					else {
						throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
								"Missing role field.",
								new SystemArgumentException());
					}
					
					// if the role is null or empty, the role
					// will be removed
					try 
					{
						SystemRoleManager roleManager = new SystemRoleManager(system);
						RoleType roleType = null;
						if (StringUtils.isEmpty(sRole) || StringUtils.equals(sRole, "null")) {
							roleType = RoleType.NONE;
						} else {
							roleType = RoleType.valueOf(sRole.toUpperCase());
						}
						
						roleManager.setRole(name, roleType, username);
						
						if (roleType == null || roleType.equals(RoleType.NONE)) {
							getResponse().setStatus(Status.SUCCESS_OK);
						} else {
							getResponse().setStatus(Status.SUCCESS_CREATED);
						}
						
						SystemRole role = system.getUserRole(name);
					
						getResponse().setEntity(new IplantSuccessRepresentation("[" + role.toJSON(system) + "]"));
					} 
					catch (IllegalArgumentException iae) {
						throw new ResourceException(
								Status.CLIENT_ERROR_BAD_REQUEST,
								"Invalid role value. Valid values are: " + RoleType.supportedValuesAsString(),
								new SystemArgumentException());
					}
				}
				catch (ResourceException e) {
					throw e;
				}
				catch (SystemException e) {
					throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
							e.getMessage(), e);
				}
				catch (Exception e)  {
					log.error("Unexpected error updating user permission", e);
					throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
							"Error occurred updating user role.", e);
				}
			} else {
				throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
						"User does not have the necessary role to modify this system", new PermissionException());
			}

		}
		catch (ResourceException e) {
			getResponse().setEntity(
					new IplantErrorRepresentation(e.getMessage()));
			getResponse().setStatus(e.getStatus());
		}
//		catch (Exception e)
//		{
//			getResponse().setEntity(
//					new IplantErrorRepresentation("Failed to update system roles: " + e.getMessage()));
//			getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
//		}
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
				AgaveLogServiceClient.ActivityKeys.SystemRemoveRoles.name(), 
				username, "", getRequest().getClientInfo().getUpstreamAddress());
		
		if (!ServiceUtils.isValid(systemId))
		{
			getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
			getResponse().setEntity(new IplantErrorRepresentation("Invalid system id. " +
							"Please specify an system using its system id. "));
			
		}
		
		try
		{
			RemoteSystem system = dao.findBySystemId(systemId);
		
			if (system == null)
			{
				throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
						"No shared system found matching " + systemId);
			} 
			else if (!system.isAvailable()) 
			{
				throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
						"System has been already been disabled.");
			}
			else if (system.isPubliclyAvailable()) 
			{
				throw new ResourceException(Status.SERVER_ERROR_NOT_IMPLEMENTED,
						"Share roles are not suported on public systems.");
			}
			
			SystemManager systemManager = new SystemManager();
			
			if (systemManager.isManageableByUser(system, username))
			{
				if (!StringUtils.isEmpty(sharedUsername)) 
				{
					// validate the user they are giving roles to exists
					AgaveProfileServiceClient authClient = new AgaveProfileServiceClient(
							Settings.IPLANT_PROFILE_SERVICE, Settings.IRODS_USERNAME, Settings.IRODS_PASSWORD);
					
					if (authClient.getUser(sharedUsername) == null) 
					{
						throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
							"No roles found for user " + sharedUsername);
					} else {
						SystemRole userRole = system.getUserRole(sharedUsername);
						system.getRoles().remove(userRole);
						dao.persist(system);
					}
				} 
				else // delete all roles for this system 
				{
					system.getRoles().clear();
					dao.persist(system);
				}	
					
				getResponse().setEntity(new IplantSuccessRepresentation());
			} 
			else 
			{
				throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
						"User does not have the necessary role to update this system", 
						new PermissionException());
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
					new IplantErrorRepresentation("Failed to remove system roles: " + e.getMessage()));
			getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.restlet.resource.Resource#allowDelete()
	 */
	@Override
	public boolean allowDelete()
	{
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.restlet.resource.Resource#allowGet()
	 */
	@Override
	public boolean allowGet()
	{
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.restlet.resource.Resource#allowPost()
	 */
	@Override
	public boolean allowPost()
	{
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.restlet.resource.Resource#allowPut()
	 */
	@Override
	public boolean allowPut()
	{
		return false;
	}
}
