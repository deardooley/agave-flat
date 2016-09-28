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
public interface NotificationAttemptCollection {

	/**
	 * Lists all {@link NotificationAttempt}s from the dead message queue for this {@link Notification}
	 * @param notificationUuid
	 * @return
	 */
	@GET
	public Response getNotificationAttempts(@PathParam("uuid") String notificationUuid);
	
	/**
	 * Forces a notification to fire upon user request. Anything other than a 
	 * simple {@code FORCED_EVENT} event requires admin permissions.
	 * 
	 * @param uuid
	 * @param input
	 * @return
	 */
	@POST
	public Response fireNotification(@PathParam("uuid") String uuid, Representation input);
	
	/**
	 * Removes all {@link NotificationAttempt}s from the dead message queue for this {@link Notification}
	 * @param notificationUuid
	 * @return
	 */
	@DELETE
	public Response clearNotificationAttempts(@PathParam("uuid") String notificationUuid);

	

}
