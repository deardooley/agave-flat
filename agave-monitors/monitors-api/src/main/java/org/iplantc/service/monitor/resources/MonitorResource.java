/**
 * 
 */
package org.iplantc.service.monitor.resources;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.restlet.representation.Representation;

/**
 * @author dooley
 *
 */
public interface MonitorResource {

	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Path("{monitorId}")
	public Response updateMonitorFromForm(@PathParam("monitorId") String monitorId, 
										@FormParam("systemId") String systemId,
										@FormParam("frequency") String frequency,
										@FormParam("updateSystemStatus") String updateSystemStatus,
										@FormParam("internalUsername") String internalUsername,
										@FormParam("active") String active);
	
	@POST
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Path("{monitorId}")
	public Response updateMonitor(@PathParam("monitorId") String monitorId, Representation input);
	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("{monitorId}")
	public Response updateMonitor(@PathParam("monitorId") String monitorId, byte[] bytes);
	
	@DELETE
	@Path("{monitorId}")
	public Response deleteMonitor(@PathParam("monitorId") String monitorId);

	@GET
	@Path("{monitorId}")
	public Response getMonitor(@PathParam("monitorId") String monitorId);

	@PUT
	@Path("{monitorId}")
	Response store(String monitorUuid);

}
