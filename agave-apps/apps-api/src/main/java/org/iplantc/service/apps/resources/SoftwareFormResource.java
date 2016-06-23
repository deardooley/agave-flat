/**
 * 
 */
package org.iplantc.service.apps.resources;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

/**
 * @author dooley
 *
 */
@Path("/{softwareId}/form")
public interface SoftwareFormResource {
    @GET
	public Response getNotification(@PathParam("softwareId") String softwareId);
}
