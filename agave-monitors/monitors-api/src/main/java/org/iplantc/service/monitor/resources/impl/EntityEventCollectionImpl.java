/**
 * 
 */
package org.iplantc.service.monitor.resources.impl;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;
import org.iplantc.service.common.dao.EntityEventDao;
import org.iplantc.service.common.model.AgaveEntityEvent;
import org.iplantc.service.monitor.events.DomainEntityEvent;
import org.iplantc.service.monitor.events.DomainEntityEventDao;
import org.iplantc.service.monitor.model.enumeration.MonitorEventType;
import org.iplantc.service.monitor.resources.EntityEventCollection;

/**
 * Returns a collection of {@link AgaveEntityEvent} for the given resource uuid. 
 * Search and pagination are support. All actions are readonly.
 * @author dooley
 *
 */
@Path("{entityId}/history")
public class EntityEventCollectionImpl extends AbstractEntityEventCollection<DomainEntityEvent, MonitorEventType> implements EntityEventCollection {
    
    private static final Logger log = Logger.getLogger(EntityEventCollectionImpl.class);
    
    @GET
	public Response getEntityEvents(@PathParam("entityId") String entityId)
	{
    	return super.getEntityEvents(entityId);
	}
    
    @Override
    public EntityEventDao<DomainEntityEvent, MonitorEventType> getEntityEventDao() {
		if (entityEventDao == null) {
			entityEventDao = new DomainEntityEventDao();
		}
		return entityEventDao;
	}
}
