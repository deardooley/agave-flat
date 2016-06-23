/**
 * 
 */
package org.iplantc.service.apps.resources;

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
@Path("{softwareId}/pems")
public interface SoftwarePermissionCollection {
    
    @GET
	public Response getSoftwarePermissions(@PathParam("softwareId") String softwareId);
	
	@POST
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response addSoftwarePermission(@PathParam("softwareId") String softwareId,
                                    Representation input);
	
	@DELETE
    public Response clearAllSoftwarePermissions(@PathParam("softwareId") String softwareId);
	
//	@POST
//    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
//    public Response addSoftwarePermissionFromForm(@PathParam("softwareId") String softwareId,
//                                            @FormParam("username") String callbackUrl,
//                                            @FormParam("permission") String permission);
//    @POST
//    @Consumes(MediaType.APPLICATION_JSON)
//    public Response addSoftwarePermission(@PathParam("softwareId") String softwareId,
//                                    byte[] bytes);
    
    
}
