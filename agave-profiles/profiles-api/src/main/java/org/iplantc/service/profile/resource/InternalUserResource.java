/**
 * 
 */
package org.iplantc.service.profile.resource;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.iplantc.service.profile.model.InternalUser;
import org.restlet.representation.Representation;
/**
 * @author dooley
 *
 */
@Path("/{username}")
@Produces("application/json")
public interface InternalUserResource {

//	@GET
//	@Path("/users")
//    public Response getInternalUsers(@PathParam("username") String username);
   
	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Path("/users")
	public Response addInternalUserFromForm(@PathParam("username") String username,
												@MatrixParam("") InternalUser jsonInternalUser);
	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/users")
	public Response addInternalUser(@PathParam("username") String username,
									    byte[] bytes);
	
	@POST
	@Consumes(MediaType.MULTIPART_FORM_DATA)
    @Path("/users")
    public Response addInternalUsersFromFile(@PathParam("username") String username, 
    												   Representation input);
    
    @DELETE
	@Path("/users")
	public Response deleteInternalUsers(@PathParam("username") String username);
	
	@GET
	@Path("/users/{internalUsername}")
	public Response getInternalUser(@PathParam("username") String username,
								   		@PathParam("internalUsername") String internalUsername);
	
	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Path("/users/{internalUsername}")
	public Response updateInternalUserFromForm(@PathParam("username") String username,
												   @PathParam("internalUsername") String internalUsername,
												   @MatrixParam("") InternalUser jsonInternalUser);
	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/users/{internalUsername}")
	public Response updateInternalUserFromJSON(@PathParam("username") String username,
												   @PathParam("internalUsername") String internalUsername,
												   byte[] bytes);
	
	@POST
	@Consumes(MediaType.MULTIPART_FORM_DATA)
    @Path("/users/{internalUsername}")
    public Response updateInternalUsersFromFile(@PathParam("username") String username, 
    												@PathParam("internalUsername") String internalUsername, 
    												Representation input);
     
	@DELETE
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Path("/users/{internalUsername}")
	public Response deleteInternalUser(@PathParam("username") String username,
								   @PathParam("internalUsername") String internalUsername);
	
//    @GET
//    @Path("/users/search/{type}/{value}")
//    public Response findInternalUsersByPathTerms(@PathParam("username") String username,
//    													   @PathParam("type") SearchFieldType type, 
//    									   				   @PathParam("value") String value);
    
    @GET
    @Path("/users")
    public Response getInternalUsers(@PathParam("username") String username,
									@QueryParam("username") String internalUsername,
									@QueryParam("email") String email,
									@QueryParam("name") String name,
									@QueryParam("status") String status,
									@QueryParam("pretty") boolean pretty);
}
