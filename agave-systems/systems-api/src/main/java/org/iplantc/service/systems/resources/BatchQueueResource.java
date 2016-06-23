/**
 * 
 */
package org.iplantc.service.systems.resources;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.iplantc.service.common.clients.AgaveLogServiceClient;
import org.iplantc.service.common.representation.IplantErrorRepresentation;
import org.iplantc.service.common.representation.IplantSuccessRepresentation;
import org.iplantc.service.common.resource.AgaveResource;
import org.iplantc.service.common.search.SearchTerm;
import org.iplantc.service.systems.dao.BatchQueueDao;
import org.iplantc.service.systems.dao.SystemDao;
import org.iplantc.service.systems.exceptions.SystemArgumentException;
import org.iplantc.service.systems.manager.SystemManager;
import org.iplantc.service.systems.model.BatchQueue;
import org.iplantc.service.systems.model.ExecutionSystem;
import org.iplantc.service.systems.model.RemoteSystem;
import org.iplantc.service.systems.model.SystemRole;
import org.iplantc.service.systems.search.SystemSearchFilter;
import org.json.JSONObject;
import org.restlet.Context;
import org.restlet.data.Form;
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
public class BatchQueueResource extends AgaveResource 
{
	private String username;
	private String systemId;
	private SystemDao dao;
	private BatchQueueDao batchQueueDao;
	
	/**
	 * @param context
	 * @param request
	 * @param response
	 */
	public BatchQueueResource(Context context, Request request, Response response)
	{
		super(context, request, response);

		this.systemId = (String) request.getAttributes().get("systemid");
		
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
		
		if (StringUtils.isEmpty(systemId))
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
						"No system found matching " + systemId);
			}
			else if (!system.getUserRole(username).canRead()) {
				throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
						"User does not have the necessary role to view this system.");
			}
			else if ( ! (system instanceof ExecutionSystem))
            {
                throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
                        "Invalid system id. " + systemId + " is a storage system. "
                      + "Only execution systems have batch queues.");
            }
			else {
				SystemManager systemManager = new SystemManager();
			    
			    Map<SearchTerm, Object> searchCriteria = getQueryParameters();
                
			    List<BatchQueue> queues = batchQueueDao.findMatching(system.getId(), searchCriteria, limit, offset);
                
//				List<BatchQueue> queues = batchQueueDao.findBySystemId(systemId, offset, limit);
				
				StringBuilder builder = new StringBuilder();
				for (BatchQueue queue: queues) {
				    builder.append("," + queue.toJSON());
				}
				
				return new IplantSuccessRepresentation(
				        "[" + StringUtils.removeStart(builder.toString(), ",") + "]");
			}
		}
		catch (IllegalArgumentException e) {
            getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
            return new IplantErrorRepresentation(e.getMessage());
        }
        catch (ResourceException e) 
		{
			getResponse().setStatus(e.getStatus());
			return new IplantErrorRepresentation(e.getMessage());
		}
		catch (Throwable e)
		{
			getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
			return new IplantErrorRepresentation("Failed to retrieve system queues: " + e.getMessage());
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
				AgaveLogServiceClient.ActivityKeys.SystemBatchQueueAdd.name(), 
				username, "", getRequest().getClientInfo().getUpstreamAddress());
		
		try
		{
			if (StringUtils.isEmpty(systemId))
			{
				throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
						"Please specify an system using its system id. ");
			}

			RemoteSystem system = dao.findActiveAndInactiveSystemBySystemId(systemId);
			
			if (system == null)
			{
				throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
						"No system found matching " + systemId);
			}
			else {
				SystemRole userRole = system.getUserRole(username);
				
				if (!userRole.canAdmin()) {
					throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
							"User does not have the necessary role to edit this system.");
				}
				else if (system.isPubliclyAvailable()) 
				{
					throw new ResourceException(Status.SERVER_ERROR_NOT_IMPLEMENTED,
							"Public system queues cannot be updated. "
							+ "Please unpublish this system before updating its queues.");
				}
			}
			
			SystemManager systemManager = new SystemManager();
			
			if (systemManager.isManageableByUser(system, username))
			{
				// parse the form to get the job specs
				JSONObject json = getPostedEntityAsJsonObject(true);
				
				if (json == null || json.length() == 0) {
				    throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
	                        "No queue description found in the request.");
				}
				
				try
				{
				    BatchQueue existingQueue = null;
				    String queueName = null;
					
				    if (json.has("name")) 
					{
					    queueName = json.getString("name");
					    existingQueue = ((ExecutionSystem)system).getQueue(queueName);
					}
				    
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
                            e.getMessage());
				}
				catch (Throwable e) 
				{
					throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
							"Error occurred updating system queue.");
				}
			} else {
				throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
						"User does not have the necessary role to modify this system queue");
			}

		}
		catch (ResourceException e) {
			getResponse().setEntity(
					new IplantErrorRepresentation(e.getMessage()));
			getResponse().setStatus(e.getStatus());
		}
		catch (Throwable e) 
        {
		    getResponse().setEntity(
                    new IplantErrorRepresentation("Error occurred updating system queue."));
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
		return false;
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
	
	/**
     * Parses url query looking for a search string
     * @return
     */
    protected Map<SearchTerm, Object> getQueryParameters() 
    {
        Form form = getRequest().getOriginalRef().getQueryAsForm();
        if (form != null && !form.isEmpty()) {
            return new SystemSearchFilter().filterCriteria(form.getValuesMap());
        } else {
            return new HashMap<SearchTerm, Object>();
        }
    }
}
