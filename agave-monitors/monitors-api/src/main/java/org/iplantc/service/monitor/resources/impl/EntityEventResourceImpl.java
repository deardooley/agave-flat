/**
 * 
 */
package org.iplantc.service.monitor.resources.impl;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;
import org.iplantc.service.common.clients.AgaveLogServiceClient;
import org.iplantc.service.common.dao.EntityEventDao;
import org.iplantc.service.common.exceptions.EntityEventPersistenceException;
import org.iplantc.service.common.representation.AgaveSuccessRepresentation;
import org.iplantc.service.monitor.events.DomainEntityEvent;
import org.iplantc.service.monitor.events.DomainEntityEventDao;
import org.iplantc.service.monitor.model.enumeration.MonitorEventType;
import org.iplantc.service.monitor.resources.EntityEventResource;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Returns a collection of history records for a {@link SoftwareEvent}. Search
 * and pagination are support. All actions are readonly.
 * @author dooley
 *
 */
@Path("{entityId}/history/{entityEventId}")
public class EntityEventResourceImpl extends AbstractEntityEventResource<DomainEntityEvent, MonitorEventType> implements EntityEventResource {
    
    private static final Logger log = Logger.getLogger(EntityEventCollectionImpl.class);

    @GET
    @Override
    public Response getEntityEvent(@PathParam("entityId") String entityId,
                                    @PathParam("entityEventId") String entityEventId) {
        logUsage(AgaveLogServiceClient.ActivityKeys.MonitorHistoryList);
        
        try 
        {
            getEntityFromPathValue(entityId);
            
            DomainEntityEvent entityEvent = getEntityEventDao().getByEntityUuidAndEntityEventUuid(entityId, entityEventId);
            
            if (entityEvent == null) {
                throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
                        String.format("No app event found for %s with id %s", entityId, entityEventId));
            } else {            
                return Response.ok(new AgaveSuccessRepresentation(new ObjectMapper().writeValueAsString(entityEvent))).build();
            }
        }
        catch (ResourceException e) {
            throw e;
        }
        catch (EntityEventPersistenceException e) {
        	log.error("Failed to query resource history for event " + entityEventId, e);
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL, 
            		"Failed to retrieve resource history event. If this persists, "
            				+ "please contact your system administrator", e);
        }
        catch (Exception e) {
        	log.error("Failed to query resource history for event " + entityEventId, e);
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL, 
            		"Failed to retrieve resource history event. If this persists, "
                    		+ "please contact your system administrator", e);
        }
    }

    @Override
	public EntityEventDao<DomainEntityEvent, MonitorEventType> getEntityEventDao() {
		if (entityEventDao == null) {
			entityEventDao = new DomainEntityEventDao();
		}
		return entityEventDao;
	}
}
