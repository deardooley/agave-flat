/**
 * 
 */
package org.iplantc.service.systems.resources;

import java.io.FileNotFoundException;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.exception.ConstraintViolationException;
import org.iplantc.service.common.clients.AgaveLogServiceClient;
import org.iplantc.service.common.exceptions.PermissionException;
import org.iplantc.service.common.persistence.HibernateUtil;
import org.iplantc.service.common.representation.IplantErrorRepresentation;
import org.iplantc.service.common.representation.IplantSuccessRepresentation;
import org.iplantc.service.common.search.SearchTerm;
import org.iplantc.service.systems.Settings;
import org.iplantc.service.systems.dao.SystemDao;
import org.iplantc.service.systems.events.RemoteSystemEventProcessor;
import org.iplantc.service.systems.exceptions.SystemArgumentException;
import org.iplantc.service.systems.exceptions.SystemException;
import org.iplantc.service.systems.exceptions.SystemUnavailableException;
import org.iplantc.service.systems.manager.SystemManager;
import org.iplantc.service.systems.model.RemoteSystem;
import org.iplantc.service.systems.model.SystemRole;
import org.iplantc.service.systems.model.enumerations.SystemEventType;
import org.iplantc.service.systems.util.ServiceUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;

/**
 * Resource to handle get, posts, and deletes of system.
 * 
 * @author dooley
 *
 */
public class SystemManagementResource extends AbstractSystemListResource 
{
	private static final Logger log = Logger.getLogger(SystemManagementResource.class);
	
	private String systemId;
	private SystemDao dao;
	private SystemManager systemManager;
	private RemoteSystemEventProcessor eventProcessor;
	
	/**
	 * @param context
	 * @param request
	 * @param response
	 */
	public SystemManagementResource(Context context, Request request,
			Response response)
	{
		super(context, request, response);
		
		this.username = getAuthenticatedUsername();
		
		this.systemId = (String) request.getAttributes().get("systemid");
		
		this.dao = new SystemDao();
		
		systemManager = new SystemManager();
		
		eventProcessor = new RemoteSystemEventProcessor();
		
		getVariants().add(new Variant(MediaType.APPLICATION_JSON));	
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.restlet.resource.Resource#represent(org.restlet.resource.Variant)
	 */
	@Override
	public Representation represent(Variant variant) throws ResourceException
	{
		username = getAuthenticatedUsername();
		AgaveLogServiceClient.log(AgaveLogServiceClient.ServiceKeys.SYSTEMS02.name(), 
				AgaveLogServiceClient.ActivityKeys.SystemsGetByID.name(), 
				username, "", getRequest().getClientInfo().getUpstreamAddress());
		
		try 
		{
			if (StringUtils.isEmpty(systemId))
			{
			    Map<SearchTerm, Object> searchCriteria = getQueryParameters();
	            
	            systems = dao.findMatching(getAuthenticatedUsername(), searchCriteria, limit, offset, hasJsonPathFilters());
	            
				return super.represent(variant);
			} 
			
			RemoteSystem system = dao.findActiveAndInactiveSystemBySystemId(systemId);
			
			if (system == null)
			{
				throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
						"No system found matching " + systemId);
			} 
			else 
			{
				SystemRole userRole = system.getUserRole(username);
				if (!userRole.canRead()) {
					throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
							"Permission denied. You do not have permission to view this system");
				}
				else {
					
					JSONObject jSystem = new JSONObject(system.toJSON());
					
					// determine at run time if this is the user's default system of this type
					jSystem.put("default", system.equals(systemManager.getUserDefaultStorageSystem(username)));
					
					if (system.isAvailable() || userRole.canUse()) {
					
						return new IplantSuccessRepresentation(jSystem.toString());
					} 
					else {
						throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
							"This system has been removed by the administrator.");
					}
				}
			}
		}
		catch (ResourceException e) {
			getResponse().setStatus(e.getStatus());
			return new IplantErrorRepresentation(e.getMessage());
		}
		catch (HibernateException e)
		{
			getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
			return new IplantErrorRepresentation("Internal error retrieving the system description");
		}
		catch (IllegalArgumentException e) {
		    getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
            return new IplantErrorRepresentation(e.getMessage());
		}
		catch (Throwable e)
		{
			getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
			return new IplantErrorRepresentation(
					"Unexpected retrieving system description");
		}
	}
	
	/* (non-Javadoc)
	 * @see org.restlet.resource.Resource#acceptRepresentation(org.restlet.resource.Representation)
	 */
	@Override
	public void acceptRepresentation(Representation entity)
			throws ResourceException
	{
		this.username = getAuthenticatedUsername();

		AgaveLogServiceClient.log(AgaveLogServiceClient.ServiceKeys.SYSTEMS02.name(), 
				StringUtils.isEmpty(systemId) ? AgaveLogServiceClient.ActivityKeys.SystemsAdd.name() : AgaveLogServiceClient.ActivityKeys.SystemsUpdate.name(), 
				username, "", getRequest().getClientInfo().getUpstreamAddress());
		
		try 
		{
			// parse form data, url encoded is not supported due to the need for
			// structure in the posted object
			JSONObject json = getPostedEntityAsJsonObject(false);
			
			RemoteSystem newSystem = doProcessSystemPost(json);
    		
    		if (StringUtils.isEmpty(systemId)) {
				getResponse().setStatus(Status.SUCCESS_CREATED);
			} else {
				getResponse().setStatus(Status.SUCCESS_OK);
			}
    		
    		JSONObject jSystem = new JSONObject(newSystem.toJSON());
			
			// determine at run time if this is the user's default system of this type
			jSystem.put("default", newSystem.equals(systemManager.getUserDefaultStorageSystem(username)));
            
    		getResponse().setEntity(new IplantSuccessRepresentation(jSystem.toString()));
		} 
		catch (ConstraintViolationException e) {
			if (e.getCause().getMessage().contains("systemId")) {
				getResponse().setEntity(new IplantErrorRepresentation("The given: " + e.getMessage()));
	            
			}
 			getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
            log.error(e);
		}
		catch (PermissionException e) {
			throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
					e.getMessage(), e);
		}
		catch (SystemArgumentException | SystemException e) {
			getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
			getResponse().setEntity(new IplantErrorRepresentation(e.getMessage()));
			log.error(e);
		} 
		catch (ResourceException e) {
			getResponse().setStatus(e.getStatus());
			getResponse().setEntity(new IplantErrorRepresentation(
					e.getMessage()));
			log.error(e);
		} 
		catch (HibernateException e) {
        	getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
            getResponse().setEntity(new IplantErrorRepresentation("Internal error while saving system."));
            log.error(e);
	    } 
		catch (JSONException e) {
			getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
			getResponse().setEntity(new IplantErrorRepresentation(
					"Unexpected error writing system description"));
			log.error(e);
		}
		catch (Throwable e) {
			getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
			getResponse().setEntity(new IplantErrorRepresentation(
					"Unexpected error deleting system"));
			log.error(e);
		}
	}
	
	/**
	 * Handles mechanics of validating and persisting the posted system 
	 * description.
	 * 
	 * TODO: need to check the registered internal user auth configs and 
	 * remove any that are no longer valid after the update.
	 *  
	 * @param json
	 * @return
	 * @throws ResourceException 
	 * @throws SystemArgumentException 
	 * @throws PermissionException 
	 * @throws SystemException 
	 */
	private RemoteSystem doProcessSystemPost(JSONObject json) 
	throws ResourceException, SystemArgumentException, SystemException, PermissionException
	{
		RemoteSystem newSystem = null; 
		RemoteSystem existingSystem = null; 
		
		try 
		{
	    	if (!StringUtils.isEmpty(systemId))
	    	{
	        	existingSystem = dao.findActiveAndInactiveSystemBySystemId(systemId);
		        
		        // update if the existing system belongs to the user, otherwise throw an exception
	        	if (existingSystem == null) {
	        		throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
	        				"No system found matching " + systemId, 
							new FileNotFoundException());
	        	}
	        	// force the system to be reenabled before it's updated
	        	else if (!existingSystem.isAvailable()) 
				{
					throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
							"System has been disabled by the administrator. "
							+ "The system must be re-enabled before updating its description.",
							new SystemUnavailableException());
				}
	        	// check user access rights
	        	else if (systemManager.isManageableByUser(existingSystem, username))
	        	{
	        		if (existingSystem.isPubliclyAvailable()) {
	        			throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
	    						"Permission denied. Public systems cannot be updated. " +
	        					"The system must be re-enabled before updating its description.",
								new PermissionException());
	        		}
	        	}
	        	// 403 if you don't have the rights
	        	else 
	        	{
	        		throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
							"Permission denied. You do not have permission to update this system",
							new PermissionException());
		    	}
	        
	        	// now we can merge/update
	        	if (json.has("id")) {
	    			if (!json.getString("id").equalsIgnoreCase(existingSystem.getSystemId())) {
						throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
							"Uploaded system description does not match target uri. You uploaded an " +
							"system description for " + json.getString("id") + " to the uri " +
							"for " + existingSystem.getSystemId() + ". Please either register a new " +
							"system or post your update to " + 
							Settings.IPLANT_JOB_SERVICE + "systems/" + json.getString("id") +
							" to update that system.", 
							new SystemArgumentException());
					}
	    		} else {
	    			throw new SystemArgumentException("Please specify a valid string value for the 'id' field.");
	    		}
	        	
	            // parse and validate the json
	        	HibernateUtil.getSession().evict(existingSystem);
	        	newSystem = systemManager.parseSystem(json, existingSystem.getOwner(), existingSystem);
	        	
	        	newSystem = dao.merge(existingSystem);
	            
	        	// TODO: should we be invalidating any auth configs that aren't valid after this update?
	        	// TODO: what is the proper way of notifying the user that things have gone awry here?
	            newSystem.setRevision(existingSystem.getRevision() + 1);
	            
	//            dao.persist(newSystem);
	            
	            eventProcessor.processSystemUpdateEvent(newSystem, SystemEventType.UPDATED, username);
//	            NotificationManager.process(newSystem.getUuid(), "UPDATED", username);
	    	}
	    	else
	    	{
	    		// parse and validate the json
	    		if (json.has("id")) {
	    			existingSystem = dao.findBySystemId(json.getString("id"));
	    		} 
	    		
	    		if (existingSystem != null) 
	            {
	    			HibernateUtil.getSession().evict(existingSystem);
	    			
	    			newSystem = systemManager.parseSystem(json, username, existingSystem);
	                
	        		newSystem.setRevision(existingSystem.getRevision() + 1);
	        		
	        		newSystem = dao.merge(newSystem);
	        		
	        		dao.persist(newSystem);
	        		
	        		eventProcessor.processSystemUpdateEvent(newSystem, SystemEventType.UPDATED, username);
//	        		NotificationManager.process(newSystem.getUuid(), "UPDATED", username);
	            } 
	            else 
	            {
	            	newSystem = systemManager.parseSystem(json, username);
	                
	            	if (!dao.isSystemIdUnique(newSystem.getSystemId())) {
	            		throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "The id " + newSystem.getSystemId() + 
	            				" is already taken. Please select a different identifier for this system.",
	            				new SystemException("System id is already taken"));
	            	}
	            	
	            	eventProcessor.processSystemUpdateEvent(newSystem, SystemEventType.CREATED, username);
	            	
//	            	NotificationManager.process(newSystem.getUuid(), "CREATED", username);
	            	
	            	dao.persist(newSystem);
	            }
	    	}
			
			return newSystem;
		} catch (JSONException e) {
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
					"Unable to parse JSON object", e);
		}
		catch (PermissionException e) {
			throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
					e.getMessage(), e);
		}
		catch (SystemException e) {
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
					e.getMessage(), e);
		}
		catch (ResourceException e) {
			throw e;
		}
		catch (Throwable e)
		{
			log.error(e);
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL, 
					"An unexpected error occurred while saving system.", e);
		}
	}

	/* 
	 * Handles delete operations on RemoteSystem objects. Deleting a system marks
	 * it as hidden in the database so we don't break references made by apps.
	 * There is currently no service endpoint to unhide and reactivate apps.
	 */
	@Override
	public void removeRepresentations() throws ResourceException
	{
		AgaveLogServiceClient.log(AgaveLogServiceClient.ServiceKeys.SYSTEMS02.name(), 
				AgaveLogServiceClient.ActivityKeys.SystemsDelete.name(), 
				username, "", getRequest().getClientInfo().getUpstreamAddress());
		
		if (!ServiceUtils.isValid(systemId))
		{
			getResponse().setEntity(
					new IplantErrorRepresentation("Invalid system name. " +
							"Please specify an system using its system id."));
			getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
			return;
		} 
		
		try
		{
			RemoteSystem system = dao.findActiveAndInactiveSystemBySystemId(systemId);

			try {
				// we can't delete systems due to their associated app-dependencies, so we
				// hide them so we can keep accurate history and throw proper exception messages. 
				if (systemManager.isManageableByUser(system, username)) 
				{
					system.setAvailable(false);
					dao.persist(system);
					
					eventProcessor.processSystemUpdateEvent(system, SystemEventType.DELETED, username);
					
//					NotificationManager.process(system.getUuid(), "DELTED", username);
					
					getResponse().setEntity(new IplantSuccessRepresentation());
					getResponse().setStatus(Status.SUCCESS_OK);
				} 
				else {
					throw new SystemException("User does not have permission to delete this system");
				}
			} catch (SystemException e) {
				getResponse().setEntity(
						new IplantErrorRepresentation(e.getMessage()));
				getResponse().setStatus(Status.CLIENT_ERROR_UNAUTHORIZED);
				return;
			}
			
			if ((system.isOwnedBy(username) && !system.isPubliclyAvailable()) || ServiceUtils.isAdmin(username)) {
				
			} else {
				getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN);
				getResponse().setEntity(new IplantErrorRepresentation(
						"User does not have permission to delete this system"));
			}
		}
		catch (HibernateException e)
		{
			log.error(e);
			getResponse().setEntity(
					new IplantErrorRepresentation("Internal error while deleting system."));
			getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);

		}
		catch (Exception e)
		{
			log.error(e);
			getResponse().setEntity(
					new IplantErrorRepresentation("Unexpected error deleting system"));
			getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
		}
	}
	
	
	
	/* 
	 * Handles PUT on individual system. Admin permissions are checked with roles
	 * If this system is already public, an exception is thrown. If it is private, the 
	 * public bit is flipped and the revision reset. To update a public system, one must have
	 * global admin privileges.
	 * 
	 */
	@Override
	public void storeRepresentation(Representation entity)
			throws ResourceException
	{
		this.username = getAuthenticatedUsername();

		try 
		{	
			if (!ServiceUtils.isValid(systemId))
			{
				getResponse().setEntity(
						new IplantErrorRepresentation("Invalid system name. " +
								"Please specify an system using its system id."));
				getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
				return;
			} 
			
			RemoteSystem system = dao.findActiveAndInactiveSystemBySystemId(systemId);
			
			if (system != null)
			{
				Map<String,String> postData = getPostedEntityAsMap();
				
				String action = null;
				String systemId = null;
				
				if (postData != null && !postData.isEmpty()) {
					
					if (postData.containsKey("action")) {
						action = postData.get("action");
					}
				}
						
				if (StringUtils.equalsIgnoreCase(action, "publish")) 
				{
					AgaveLogServiceClient.log(AgaveLogServiceClient.ServiceKeys.SYSTEMS02.name(), 
							AgaveLogServiceClient.ActivityKeys.SystemsPublish.name(), 
							username, "", getRequest().getClientInfo().getUpstreamAddress());
					
					if (system.isPubliclyAvailable()) {
						throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
								"Cannot republish a public system.");
					}
					if (ServiceUtils.isAdmin(username)) 
					{	
						RemoteSystem updatedSystem = systemManager.publish(system, username);
		
						// determine at run time if this is the user's default system of this type
						JSONObject jSystem = new JSONObject(updatedSystem.toJSON());
						jSystem.put("default", updatedSystem.equals(systemManager.getUserDefaultStorageSystem(username)));
						
						getResponse().setEntity(new IplantSuccessRepresentation(jSystem.toString()));
						getResponse().setStatus(Status.SUCCESS_OK);
					} else {
						throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
								"Permission denied. You do not have permission to publish this system");
					}
				}
				else if (StringUtils.equalsIgnoreCase(action, "unpublish")) 
				{
					AgaveLogServiceClient.log(AgaveLogServiceClient.ServiceKeys.SYSTEMS02.name(), 
							AgaveLogServiceClient.ActivityKeys.SystemsUnpublish.name(), 
							username, "", getRequest().getClientInfo().getUpstreamAddress());
					
					if (!system.isPubliclyAvailable()) {
						throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
								"System is not currently published.");
					}
					if (ServiceUtils.isAdmin(username)) 
					{	
						RemoteSystem updatedSystem = systemManager.unpublish(system, username);
		
						// determine at run time if this is the user's default system of this type
						JSONObject jSystem = new JSONObject(updatedSystem.toJSON());
						jSystem.put("default", updatedSystem.equals(systemManager.getUserDefaultStorageSystem(username)));
						
						getResponse().setEntity(new IplantSuccessRepresentation(jSystem.toString()));
						getResponse().setStatus(Status.SUCCESS_OK);
					} else {
						throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
								"Permission denied. You do not have permission to publish this system");
					}
				}
				else if (StringUtils.equalsIgnoreCase(action, "setdefault")) 
				{
					AgaveLogServiceClient.log(AgaveLogServiceClient.ServiceKeys.SYSTEMS02.name(), 
							AgaveLogServiceClient.ActivityKeys.SystemsSetDefault.name(), 
							username, "", getRequest().getClientInfo().getUpstreamAddress());
					
					RemoteSystem updatedSystem = systemManager.setUserDefaultSystem(username, system);
					
					// determine at run time if this is the user's default system of this type
					JSONObject jSystem = new JSONObject(updatedSystem.toJSON());
					jSystem.put("default", true);
					
					getResponse().setEntity(new IplantSuccessRepresentation(jSystem.toString()));
					getResponse().setStatus(Status.SUCCESS_OK);
				}
				else if (StringUtils.equalsIgnoreCase(action, "unsetDefault")) 
				{
					AgaveLogServiceClient.log(AgaveLogServiceClient.ServiceKeys.SYSTEMS02.name(), 
							AgaveLogServiceClient.ActivityKeys.SystemsUnsetDefault.name(), 
							username, "", getRequest().getClientInfo().getUpstreamAddress());
					
					RemoteSystem updatedSystem = systemManager.unsetUserDefaultSystem(username, system);
					
					// determine at run time if this is the user's default system of this type
					JSONObject jSystem = new JSONObject(updatedSystem.toJSON());
					jSystem.put("default", false);
					
					getResponse().setEntity(new IplantSuccessRepresentation(jSystem.toString()));
					getResponse().setStatus(Status.SUCCESS_OK);
				}
				else if (StringUtils.equalsIgnoreCase(action, "setGlobalDefault")) 
				{
					AgaveLogServiceClient.log(AgaveLogServiceClient.ServiceKeys.SYSTEMS02.name(), 
							AgaveLogServiceClient.ActivityKeys.SystemsSetGlobalDefault.name(), 
							username, "", getRequest().getClientInfo().getUpstreamAddress());
					
					if (system.isPubliclyAvailable()) {
						if (ServiceUtils.isAdmin(username)) 
						{	
							RemoteSystem updatedSystem = systemManager.setGlobalDefault(system, username);
							
							// determine at run time if this is the user's default system of this type
							JSONObject jSystem = new JSONObject(updatedSystem.toJSON());
							jSystem.put("default", updatedSystem.equals(systemManager.getUserDefaultStorageSystem(username)));
							
							getResponse().setEntity(new IplantSuccessRepresentation(jSystem.toString()));
							getResponse().setStatus(Status.SUCCESS_OK);
						} else {
							throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
									"Permission denied. You do not have permission to globally publish this system");
						}
					} 
					else
					{
						throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
								"Cannot set a private system as a global default. " +
								"Please make the system public before assigning it as a default.");
					}
				}
				else if (StringUtils.equalsIgnoreCase(action, "unsetGlobalDefault")) 
				{
					AgaveLogServiceClient.log(AgaveLogServiceClient.ServiceKeys.SYSTEMS02.name(), 
							AgaveLogServiceClient.ActivityKeys.SystemsUnsetGlobalDefault.name(), 
							username, "", getRequest().getClientInfo().getUpstreamAddress());
					
					if (system.isPubliclyAvailable()) {
						if (ServiceUtils.isAdmin(username)) 
						{	
							RemoteSystem updatedSystem = systemManager.unsetGlobalDefault(system, username);
			
							// determine at run time if this is the user's default system of this type
							JSONObject jSystem = new JSONObject(updatedSystem.toJSON());
							jSystem.put("default", updatedSystem.equals(systemManager.getUserDefaultStorageSystem(username)));
							
							getResponse().setEntity(new IplantSuccessRepresentation(jSystem.toString()));
							getResponse().setStatus(Status.SUCCESS_OK);
						} else {
							throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
									"Permission denied. You do not have permission to globally publish this system");
						}
					} 
					else
					{
						throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
								"Cannot set a private system as a global default. " +
								"Please make the system public before assigning it as a default.");
					}
				}
				else if (StringUtils.equalsIgnoreCase(action, "clone"))
				{
					AgaveLogServiceClient.log(AgaveLogServiceClient.ServiceKeys.SYSTEMS02.name(), 
							AgaveLogServiceClient.ActivityKeys.SystemsClone.name(), 
							username, "", getRequest().getClientInfo().getUpstreamAddress());
					
					
					if (systemManager.isVisibleByUser(system, username)) 
					{
						systemId = postData.get("id");
						RemoteSystem clonedSystem = systemManager.cloneSystem(system, username, systemId);
						
						// determine at run time if this is the user's default system of this type
						JSONObject jSystem = new JSONObject(clonedSystem.toJSON());
						jSystem.put("default", clonedSystem.equals(systemManager.getUserDefaultStorageSystem(username)));
						
						getResponse().setEntity(new IplantSuccessRepresentation(jSystem.toString()));
						getResponse().setStatus(Status.SUCCESS_OK);
					}
					else 
					{
						throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
								"Permission denied. Only public systems and systems you on which you have been granted a role may be cloned.");
					}
				}
                else if (StringUtils.equalsIgnoreCase(action, "enable"))
                {
                    AgaveLogServiceClient.log(AgaveLogServiceClient.ServiceKeys.SYSTEMS02.name(), 
                            AgaveLogServiceClient.ActivityKeys.SystemsEnable.name(), 
                            username, "", getRequest().getClientInfo().getUpstreamAddress());
                    
                    try {
                    	RemoteSystem updatedSystem = systemManager.enableSystem(system, username);
                    	
                    	// determine at run time if this is the user's default system of this type
						JSONObject jSystem = new JSONObject(updatedSystem.toJSON());
						jSystem.put("default", updatedSystem.equals(systemManager.getUserDefaultStorageSystem(username)));
						
						getResponse().setEntity(new IplantSuccessRepresentation(jSystem.toString()));
                        getResponse().setStatus(Status.SUCCESS_OK);
                    }
                    catch (PermissionException e) {
                        throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
                                e.getMessage());
                    }
                }
                else if (StringUtils.equalsIgnoreCase(action, "disable"))
                {
                    AgaveLogServiceClient.log(AgaveLogServiceClient.ServiceKeys.SYSTEMS02.name(), 
                            AgaveLogServiceClient.ActivityKeys.SystemsDisable.name(), 
                            username, "", getRequest().getClientInfo().getUpstreamAddress());
                    
                    try {
                    	RemoteSystem updatedSystem = systemManager.disableSystem(system, username);
                    	
                    	// determine at run time if this is the user's default system of this type
						JSONObject jSystem = new JSONObject(updatedSystem.toJSON());
						jSystem.put("default", updatedSystem.equals(systemManager.getUserDefaultStorageSystem(username)));
						
						getResponse().setEntity(new IplantSuccessRepresentation(jSystem.toString()));
                        getResponse().setStatus(Status.SUCCESS_OK);
                    }
                    catch (PermissionException e) {
                        throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
                                e.getMessage());
                    }
                }
                else if (StringUtils.equalsIgnoreCase(action, "erase"))
				{
					AgaveLogServiceClient.log(AgaveLogServiceClient.ServiceKeys.SYSTEMS02.name(), 
							AgaveLogServiceClient.ActivityKeys.SystemsErase.name(), 
							username, "", getRequest().getClientInfo().getUpstreamAddress());
					
					
					if (ServiceUtils.isAdmin(username)) 
					{
						systemManager.eraseSystem(system, username);
						getResponse().setEntity(new IplantSuccessRepresentation());
						getResponse().setStatus(Status.SUCCESS_OK);
					}
					else 
					{
						throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
								"Permission denied. Only tenant administrators may erase systems.");
					}
				} else {
                    throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
                            "Unrecognized action. Valid actions are: publish, setDefault, unsetDefault, setGlobalDefault, unsetGlobalDefault, enable, disable, and clone.");
                }
			}
			else
			{
				throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
						"No system found matching " + systemId);
			}
		}
		catch (ResourceException e) {
			getResponse().setEntity(
					new IplantErrorRepresentation(e.getMessage()));
			getResponse().setStatus(e.getStatus());
		}
		catch (HibernateException e)
		{
			log.error(e);
			getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
			getResponse().setEntity(new IplantErrorRepresentation(
					"Internal error retrieving the system description"));
		}
		catch (Throwable e)
		{
			log.error(e);
			getResponse().setEntity(
					new IplantErrorRepresentation("Unexpected error processing the request. " + e.getMessage()));
			getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
		}
	}

	@Override
	public boolean allowGet()
	{
		return true;
	}

	@Override
	public boolean allowPut()
	{
		return true;
	}
	
	@Override
	public boolean allowDelete()
	{
		return true;
	}

	@Override
	public boolean allowPost()
	{
		return true;
	}
}
