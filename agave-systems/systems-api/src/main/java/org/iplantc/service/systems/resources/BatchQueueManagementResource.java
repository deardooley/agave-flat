/**
 * 
 */
package org.iplantc.service.systems.resources;

import java.io.FileNotFoundException;
import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.iplantc.service.common.clients.AgaveLogServiceClient;
import org.iplantc.service.common.exceptions.PermissionException;
import org.iplantc.service.common.representation.IplantErrorRepresentation;
import org.iplantc.service.common.representation.IplantSuccessRepresentation;
import org.iplantc.service.common.resource.AgaveResource;
import org.iplantc.service.systems.dao.BatchQueueDao;
import org.iplantc.service.systems.dao.SystemDao;
import org.iplantc.service.systems.exceptions.SystemArgumentException;
import org.iplantc.service.systems.exceptions.SystemException;
import org.iplantc.service.systems.manager.SystemManager;
import org.iplantc.service.systems.model.BatchQueue;
import org.iplantc.service.systems.model.ExecutionSystem;
import org.iplantc.service.systems.model.RemoteSystem;
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

/**
 * Handles CRUD endpoints for {@link BatchQueue}
 * 
 * @author dooley
 * 
 */
public class BatchQueueManagementResource extends AgaveResource 
{
	private String username;
	private String systemId;
	private SystemDao dao;
    private String queueId;
    private BatchQueueDao batchQueueDao;
    
	/**
	 * @param context
	 * @param request
	 * @param response
	 */
	public BatchQueueManagementResource(Context context, Request request, Response response)
	{
		super(context, request, response);

		this.systemId = (String) request.getAttributes().get("systemid");
		
		this.queueId = (String) request.getAttributes().get("queueid");
		
		this.dao = new SystemDao();
		
		this.batchQueueDao = new BatchQueueDao();
        
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
				AgaveLogServiceClient.ActivityKeys.SystemBatchQueueList.name(), 
				username, "", getRequest().getClientInfo().getUpstreamAddress());
		
		if (!ServiceUtils.isValid(systemId))
		{
			getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
			return new IplantErrorRepresentation("Invalid system id. " +
							"Please specify an system using its system id.");
		} 
		
		try
		{
			RemoteSystem system = dao.findActiveAndInactiveSystemBySystemId(systemId);
	        
            if (system == null)
            {
                throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
                        "No system found matching " + systemId,
						new FileNotFoundException());
            }
            else if ( ! (system instanceof ExecutionSystem))
            {
                throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
                        "Invalid system id. " + systemId + " is a storage system. "
                      + "Only execution systems have batch queues.", 
						new SystemArgumentException());
            }
            
            SystemManager systemManager = new SystemManager();
            
            if (systemManager.isVisibleByUser(system, username))
            {
				
				if (StringUtils.isEmpty(queueId) ) 
				{
				    throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
	                        "Invalid system queue id. Please specify an system using its system id. ", 
							new SystemArgumentException());
				} 
				else 
				{
				    // add the owner 
//                    BatchQueue batchQueue = batchQueueDao.findBySystemIdAndName(systemId, queueId);
					BatchQueue batchQueue = ((ExecutionSystem)system).getQueue(queueId);
					
					if (batchQueue == null) {
						throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
		                        "No queue found matching " + queueId + " on system " + systemId, 
								new FileNotFoundException());
					} else {
						return new IplantSuccessRepresentation(batchQueue.toJSON());
					}
				}
			}
			else
			{
				throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
						"User does not have the necessary role to view this system.",
						new PermissionException());
			}
		}
		catch (ResourceException e) 
		{
			getResponse().setStatus(e.getStatus());
			return new IplantErrorRepresentation(e.getMessage());
		}
		catch (SystemException e) {
			getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
			return new IplantErrorRepresentation(e.getMessage());
			
        }
		catch (Throwable e)
		{
			getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
			return new IplantErrorRepresentation("Failed to retrieve system roles: " + e.getMessage());
		}
	}

	/**
	 * Post action for adding (and overwriting) batch queues on a system object
	 */
	@Override
	public void acceptRepresentation(Representation entity)
	{
	    this.username = getAuthenticatedUsername();
        
        AgaveLogServiceClient.log(AgaveLogServiceClient.ServiceKeys.SYSTEMS02.name(), 
                AgaveLogServiceClient.ActivityKeys.SystemBatchQueueUpdate.name(), 
                username, "", getRequest().getClientInfo().getUpstreamAddress());
        
	    try
        {
    	    if (StringUtils.isEmpty(systemId))
            {
                throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
                        "Please specify an system using its system id. ", 
						new SystemArgumentException());
            }
    
    	    RemoteSystem system = dao.findActiveAndInactiveSystemBySystemId(systemId);
        
            if (system == null)
            {
                throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
                        "No system found matching " + systemId, 
						new FileNotFoundException());
            }
            else if ( ! (system instanceof ExecutionSystem))
            {
                throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
                        "Invalid system id. " + systemId + " is a storage system. "
                      + "Only execution systems have batch queues.", 
						new SystemArgumentException());
            }
            else if (system.isPubliclyAvailable()) 
            {
                throw new ResourceException(Status.SERVER_ERROR_NOT_IMPLEMENTED,
                        "Public system queues cannot be updated. "
                        + "Please unpublish this system before updating its queues.", 
						new SystemArgumentException());
            }
            
            SystemManager systemManager = new SystemManager();
            
            if (systemManager.isManageableByUser(system, username))
            {
                // parse the form to get the job specs
                JSONObject json = getPostedEntityAsJsonObject(true);
                
                if (json == null || json.length() == 0) {
                    throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
                            "No queue description found in the request.", 
							new SystemArgumentException());
                }
                
                try
    			{
    				String queueName = null;
    				if (StringUtils.isEmpty(queueId)) 
    				{
    				    throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
                                "Invalid system queue id. Please specify an system using its system id.", 
    							new SystemArgumentException());
    				} 
    				else if (json.has("name") && 
    						!StringUtils.equalsIgnoreCase(json.getString("name"), queueId)) 
    				{
    					throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
    							"The queue name value in the POST body, " + json.getString("name") + 
                    			", does not match the username in the URL, " + queueId, 
    							new SystemArgumentException());         
    				}
    			    
    				queueName = json.getString("name");
    				
    				BatchQueue existingQueue = ((ExecutionSystem)system).getQueue(queueName);
    				BatchQueue newQueue = BatchQueue.fromJSON(json, existingQueue);
                    
                    if (newQueue.isSystemDefault()) {
                        for (BatchQueue systemQueue: ((ExecutionSystem)system).getBatchQueues()) {
                            systemQueue.setSystemDefault(false);
                        }
                        newQueue.setSystemDefault(true);
                    }
                    
                    newQueue.setLastUpdated(new Date());
                    
                    dao.persist(system);
                    
                    getResponse().setEntity(new IplantSuccessRepresentation(newQueue.toJSON()));
                }
                catch (SystemArgumentException e) {
                    throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
                            e.getMessage(), e);
                }
                catch (Throwable e)  {
                    throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
                            "An unexpected error occurred updating system queue. "
                            + "If this persists, please contact your system admin", 
                            e);
                }
            } else {
    			throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
    					"User does not have the necessary role to modify this system queue",
    					new PermissionException());
    		}
        }
		catch (ResourceException e) {
			getResponse().setEntity(
					new IplantErrorRepresentation(e.getMessage()));
			getResponse().setStatus(e.getStatus());
		}
	    catch (SystemException e) {
        	getResponse().setEntity(
					new IplantErrorRepresentation(e.getMessage()));
			getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
        }
		catch (Throwable e)
        {
	        getResponse().setEntity(
                    new IplantErrorRepresentation("An unexpected error occurred updating the system queue. "
                            + "If this persists, please contact your system admin"));
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
				AgaveLogServiceClient.ActivityKeys.SystemBatchQueueDelete.name(), 
				username, "", getRequest().getClientInfo().getUpstreamAddress());
		
		if (StringUtils.isEmpty(systemId))
        {
            throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
                    "Invalid system id. Please specify an system using its system id.", 
					new SystemArgumentException());
        } 
        
        try
        {
        	RemoteSystem system = dao.findActiveAndInactiveSystemBySystemId(systemId);
            
            if (system == null)
            {
                throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
                        "No system found matching " + systemId, 
						new FileNotFoundException());
            }
            else if ( ! (system instanceof ExecutionSystem))
            {
                throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
                        "Invalid system id. " + systemId + " is a storage system. "
                      + "Only execution systems have batch queues.", 
						new SystemArgumentException());
            }
            
            SystemManager systemManager = new SystemManager();
            
            if (systemManager.isVisibleByUser(system, username))
            {
                
                if (StringUtils.isEmpty(queueId) ) 
                {
                    throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
                            "Invalid system queue id. Please specify an system queue using its id. ", 
							new SystemArgumentException());
                } 
                else 
                {
                    // add the owner 
                    BatchQueue batchQueue = batchQueueDao.findBySystemIdAndName(systemId, queueId);
                    batchQueueDao.delete(batchQueue);
				}	
					
				getResponse().setEntity(new IplantSuccessRepresentation());
			} 
			else 
			{
				throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
						"User does not have the necessary role to update this system queue",
						new PermissionException());
			}
		}
		catch (ResourceException e) {
			getResponse().setEntity(
					new IplantErrorRepresentation(e.getMessage()));
			getResponse().setStatus(e.getStatus());
		}
        catch (SystemException e) {
        	getResponse().setEntity(
					new IplantErrorRepresentation(e.getMessage()));
			getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
        }
		catch (Exception e)
		{
			getResponse().setEntity(
					new IplantErrorRepresentation("An unexpected error occurred removing the system queue. "
                            + "If this persists, please contact your system admin"));
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
