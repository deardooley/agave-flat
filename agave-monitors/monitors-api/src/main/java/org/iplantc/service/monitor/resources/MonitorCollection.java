/**
 * 
 */
package org.iplantc.service.monitor.resources;

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
public interface MonitorCollection {

	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public Response addMonitorFromForm(@FormParam("systemId") String systemId,
										@FormParam("internalUsername") String internalUsername,	
										@FormParam("frequency") String frequency,
										@FormParam("updateSystemStatus") String updateSystemStatus,
										@FormParam("active") String active);
	
	@POST
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response addMonitor(Representation input);
	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	public Response addMonitor(byte[] bytes);
	
	@GET
	public Response getMonitors(@QueryParam("systemId") String systemId, @QueryParam("active") String active);
	
}
