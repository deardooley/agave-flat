/**
 * 
 */
package org.iplantc.service.tags.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;


/**
 * Returns history record for a single {@link TagEvent} on a {@link Tag}
 * @author dooley
 *
 */
@Path("{entityId}/history/{uuid}")
public interface TagHistoryResource {
    
    @GET
    public Response getTagEvent(@PathParam("entityId") String softwareId,
                                    @PathParam("uuid") String uuid);
}
