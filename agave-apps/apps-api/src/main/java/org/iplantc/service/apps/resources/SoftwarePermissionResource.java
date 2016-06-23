/**
 * 
 */
package org.iplantc.service.apps.resources;

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
 * Handles {@link SoftwarePermission} for a single user on a single
 * {@link Software} object.
 * @author dooley
 *
 */
@Path("{softwareId}/pems/{username}")
public interface SoftwarePermissionResource {
    
    @GET
	public Response getUserSoftwarePermission(@PathParam("softwareId") String softwareId,
                                    @PathParam("sharedUsername") String sharedUsername);
	
	@POST
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response updateUserSoftwarePermission(@PathParam("softwareId") String softwareId,
                                    @PathParam("sharedUsername") String sharedUsername,
                                    Representation input);
	
	@DELETE
	public Response deleteUserSoftwarePermission(@PathParam("softwareId") String softwareId,
	                                       @PathParam("sharedUsername") String sharedUsername);
//
//	@POST
//    @Consumes(MediaType.APPLICATION_JSON)
//    public Response addNotification(@PathParam("softwareId") String softwareId,
//                                    @PathParam("username") String username,
//                                    byte[] bytes);
//    
//    @POST
//    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
//    public Response addNotificationFromForm(@PathParam("softwareId") String softwareId,
//                                            @PathParam("username") String username,
//                                            @FormParam("permission") String permission);
//    
//    

}
