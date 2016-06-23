/**
 * 
 */
package org.iplantc.service.notification.resources;

import javax.ws.rs.GET;
import javax.ws.rs.core.Response;

/**
 * @author dooley
 *
 */
public interface QuartzResource {

	@GET
	public Response getSummary();
	
}
