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
public interface MonitorCheckCollection {

	@POST
	public Response runMonitor(@PathParam("monitorUuid") String monitorUuid);
	
	@GET
	public Response getChecks(@PathParam("monitorUuid") String monitorUuid,
								@QueryParam("startDate") String startDate,
								@QueryParam("endDate") String endDate,	
								@QueryParam("result") String checkResult,
								@QueryParam("type") String type);
}
