/**
 * 
 */
package org.iplantc.service.profile.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

/**
 * @author dooley
 *
 */
@Path("")
@Produces("application/json")
public interface ProfileResource {

	@GET
	public Response getProfiles(@QueryParam("username") String username,
								@QueryParam("email") String email,
								@QueryParam("name") String name,
								@QueryParam("status") String status,
								@QueryParam("pretty") boolean pretty);
   
	@GET
	@Path("/{username}")
	public Response getProfile(@PathParam("username") String username);
     
    @GET
    @Path("/search/{term}/{value}")
    public Response findProfilesByPathTerms(@PathParam("type") String type, 
    											 @PathParam("value") String value);
    
}
