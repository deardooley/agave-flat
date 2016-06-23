/**
 * 
 */
package org.iplantc.service.apps.resources;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.iplantc.service.apps.model.Software;
import org.restlet.representation.Representation;

/**
 * Returns a collection of {@link Software} records for the authenticated user. Search
 * and pagination are support. Accepts new Software registrations.
 * @author dooley
 *
 */
@Path("")
public interface SoftwareCollection {
    
	@GET
	public Response getSoftwareCollection();
	
	@POST
	public Response addSoftware(Representation input);
}
