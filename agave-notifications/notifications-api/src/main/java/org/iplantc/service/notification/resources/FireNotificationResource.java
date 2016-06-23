/**
 * 
 */
package org.iplantc.service.notification.resources;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

/**
 * @author dooley
 *
 */
public interface FireNotificationResource {

	@POST
	public Response fireNotification(@PathParam("uuid") String uuid,
            						 @QueryParam("event") String event);
	
}
