/**
 * 
 */
package org.iplantc.service.apps.resources;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.iplantc.service.apps.model.SoftwareEvent;

/**
 * Returns history record for a single {@link SoftwareEvent}
 * @author dooley
 *
 */
@Path("{softwareId}/history/{uuid}")
public interface SoftwareHistoryResource {
    
    @GET
    public Response getSofwareEvent(@PathParam("softwareId") String softwareId,
                                    @PathParam("uuid") String uuid);
}
