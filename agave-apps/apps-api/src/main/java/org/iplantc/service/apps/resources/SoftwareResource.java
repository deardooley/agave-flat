/**
 * 
 */
package org.iplantc.service.apps.resources;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.iplantc.service.apps.model.Software;
import org.restlet.representation.Representation;

/**
 * Supports management, publishing, etc of a {@link Software} records.
 * @author dooley
 *
 */
@Path("/{softwareId}")
@Produces("application/json")
public interface SoftwareResource {
    
    @GET
	public Response getSoftware(@PathParam("softwareId") String softwareId);
    
    @POST
    @Consumes({MediaType.MULTIPART_FORM_DATA, MediaType.APPLICATION_JSON, MediaType.APPLICATION_FORM_URLENCODED})
	public Response update(@PathParam("softwareId") String softwareId,
	                               Representation input);
	
	@PUT
	@Consumes({MediaType.MULTIPART_FORM_DATA, MediaType.APPLICATION_JSON, MediaType.APPLICATION_FORM_URLENCODED})
    public Response manage(@PathParam("softwareId") String softwareId);
    
	@DELETE
	public Response remove(@PathParam("uuid") String uuid);
}
