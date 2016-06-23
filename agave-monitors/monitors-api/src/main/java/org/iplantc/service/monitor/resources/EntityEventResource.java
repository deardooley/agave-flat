/**
 * 
 */
package org.iplantc.service.monitor.resources;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.iplantc.service.common.model.AgaveEntityEvent;

/**
 * Returns a single {@link AgaveEntityEvent} record for a given resource uuid
 * @author dooley
 *
 */
@Path("{entityId}/history/{entityEventId}")
public interface EntityEventResource {
    
    @GET
    public Response getEntityEvent(@PathParam("entityId") String entityId,
                                    @PathParam("entityEventId") String entityEventId);
}
