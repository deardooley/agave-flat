/**
 * 
 */
package org.iplantc.service.tags.resource;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.restlet.representation.Representation;

/**
 * @author dooley
 *
 */
@Path("{entityId}/pems")
public interface TagPermissionsCollection {
    
    @GET
	public Response getEntityPermissions(@PathParam("entityId") String entityId);
	
	@POST
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response addEntityPermission(@PathParam("entityId") String entityId,
                                    Representation input);
	
	@DELETE
    public Response clearAllEntityPermissions(@PathParam("entityId") String entityId);
    
}
