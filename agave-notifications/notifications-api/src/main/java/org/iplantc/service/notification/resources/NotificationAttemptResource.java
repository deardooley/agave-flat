/**
 * 
 */
package org.iplantc.service.notification.resources;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

/**
 * @author dooley
 *
 */
public interface NotificationAttemptResource {

	@GET
	public Response getNotificationAttempt(@PathParam("notificationUuid") String notificationUuid, 
										   @PathParam("failureUuid") String failureUuid);
	
	@DELETE
	
	public Response deleteNotificationAttempt(@PathParam("notificationUuid") String notificationUuid, 
									   		  @PathParam("failureUuid") String failureUuid);
}
