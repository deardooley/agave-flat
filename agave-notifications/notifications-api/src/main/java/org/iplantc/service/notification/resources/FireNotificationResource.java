/**
 * 
 */
package org.iplantc.service.notification.resources;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.restlet.representation.Representation;

/**
 * @author dooley
 *
 */
public interface FireNotificationResource {

	/**
	 * Forces a notification to fire upon user request. Anything other than a 
	 * simple {@code FORCED_EVENT} event requires admin permissions.
	 * 
	 * @param uuid
	 * @param input
	 * @return
	 */
//	@POST
	public Response fireNotification(@PathParam("uuid") String uuid, Representation input);
	
}
