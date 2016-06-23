/**
 * 
 */
package org.iplantc.service.monitor.resources;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

/**
 * @author dooley
 *
 */
@Path("{monitorUuid}/checks/{checkUuid}")
public interface MonitorCheckResource {

	@GET
	public Response getCheck(@PathParam("monitorUuid") String monitorUuid, 
								@PathParam("checkUuid") String checkUuid);
}
