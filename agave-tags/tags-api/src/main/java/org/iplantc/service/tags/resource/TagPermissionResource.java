/**
 * 
 */
package org.iplantc.service.tags.resource;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.restlet.representation.Representation;

/**
 * Handles {@link TagPermission} for a single user on a single
 * {@link Tag} object.
 * @author dooley
 *
 */
@Path("{entityId}/pems/{sharedUsername}")
public interface TagPermissionResource {
    
    @GET
    public Response getEntityPermissionForUser(@PathParam("entityId") String entityId,
                                    @PathParam("sharedUsername") String sharedUsername);
	
	@POST
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response updateEntityPermissionForUser(@PathParam("entityId") String entityId,
                                    @PathParam("sharedUsername") String sharedUsername,
                                    Representation input);
	
	@DELETE
	public Response removeEntityPermissionForUser(@PathParam("entityId") String entityId,
	                                       @PathParam("sharedUsername") String sharedUsername);
}
