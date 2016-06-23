/**
 * 
 */
package org.iplantc.service.notification.resources;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.restlet.representation.Representation;

/**
 * @author dooley
 *
 */
@Path("{uuid}/attempts")
public interface NotificationAttemptCollection {

	@GET
	@Path("{uuid}/attempts")
	public Response getNotificationAttempts(@PathParam("uuid") String notificationUuid);
	
	@DELETE
	@Path("{uuid}/attempts")
	public Response clearNotificationAttempts(@PathParam("uuid") String notificationUuid);

	

}
