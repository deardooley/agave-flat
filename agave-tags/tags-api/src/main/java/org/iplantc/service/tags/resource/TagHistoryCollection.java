/**
 * 
 */
package org.iplantc.service.tags.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.iplantc.service.tags.model.Tag;

/**
 * Returns a collection of history records for a {@link Tag}. Search
 * and pagination are support. All actions are readonly.
 * @author dooley
 *
 */
@Path("{entityId}/history")
public interface TagHistoryCollection {
    
    @GET
    public Response getTagEvents(@PathParam("entityId") String entityId);
    
}
