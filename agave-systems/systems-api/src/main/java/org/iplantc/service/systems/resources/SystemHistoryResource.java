/**
 * 
 */
package org.iplantc.service.systems.resources;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.common.clients.AgaveLogServiceClient;
import org.iplantc.service.common.representation.IplantErrorRepresentation;
import org.iplantc.service.common.representation.IplantSuccessRepresentation;
import org.iplantc.service.common.resource.AgaveResource;
import org.iplantc.service.systems.dao.SystemDao;
import org.iplantc.service.systems.events.SystemHistoryEvent;
import org.iplantc.service.systems.events.SystemHistoryEventDao;
import org.iplantc.service.systems.model.RemoteSystem;
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
 * Handles system history listing
 * 
 * @author dooley
 * 
 */
public class SystemHistoryResource extends AgaveResource 
{	
	private static final Logger log = Logger.getLogger(SystemHistoryResource.class);
	
	private String username;
	private String systemId;
	private SystemDao dao;
	private SystemHistoryEventDao domainEntityDao;
	
	/**
	 * @param context
	 * @param request
	 * @param response
	 */
	public SystemHistoryResource(Context context, Request request, Response response)
	{
		super(context, request, response);

		this.systemId = (String) request.getAttributes().get("systemid");
		
		this.dao = new SystemDao();
		
		this.domainEntityDao = new SystemHistoryEventDao();
		
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
				AgaveLogServiceClient.ActivityKeys.SystemHistoryList.name(), 
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
						"User does not have the necessary role to view this system history.");
			}
			else {
				List<SystemHistoryEvent> events = this.domainEntityDao.getEntityEventByEntityUuid(system.getUuid(), limit, offset);
			    
	            ObjectMapper mapper = new ObjectMapper();
	            ArrayNode history = mapper.createArrayNode();
	            for(SystemHistoryEvent event: events) {
	                history.add(mapper.valueToTree(event));
	            }
			    
				return new IplantSuccessRepresentation(history.toString());
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
			log.error("Failed to fetch history for system " + systemId, e);
			getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
			return new IplantErrorRepresentation("Failed to retrieve system history. "
					+ "If this persists, please contact your tenant administrator.");
		} 
	}
}
