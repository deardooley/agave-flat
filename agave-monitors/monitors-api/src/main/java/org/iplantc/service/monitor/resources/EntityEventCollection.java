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
 * Returns a collection of {@link AgaveEntityEvent}s for a given resource uuid. Search
 * and pagination are support. All actions are readonly.
 * @author dooley
 *
 */
@Path("{entityId}/history")
public interface EntityEventCollection {
    
    @GET
    public Response getEntityEvents(@PathParam("entityId") String entityId);
    
}
