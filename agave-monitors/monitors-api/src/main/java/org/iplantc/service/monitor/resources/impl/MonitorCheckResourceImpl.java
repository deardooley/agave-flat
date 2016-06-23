/**
 * 
 */
package org.iplantc.service.monitor.resources.impl;

import static org.iplantc.service.common.clients.AgaveLogServiceClient.ActivityKeys.MonitorCheckGetById;
import static org.iplantc.service.common.clients.AgaveLogServiceClient.ServiceKeys.MONITORS02;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.iplantc.service.common.clients.AgaveLogServiceClient;
import org.iplantc.service.common.representation.AgaveSuccessRepresentation;
import org.iplantc.service.common.restlet.resource.AbstractAgaveResource;
import org.iplantc.service.monitor.dao.MonitorCheckDao;
import org.iplantc.service.monitor.dao.MonitorDao;
import org.iplantc.service.monitor.managers.MonitorPermissionManager;
import org.iplantc.service.monitor.model.Monitor;
import org.iplantc.service.monitor.model.MonitorCheck;
import org.iplantc.service.monitor.resources.MonitorCheckResource;
import org.restlet.Request;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;

/**
 * @author dooley
 *
 */
@Path("{monitorUuid}/checks/{checkUuid}")
@Produces(MediaType.APPLICATION_JSON)
public class MonitorCheckResourceImpl extends AbstractAgaveResource implements MonitorCheckResource
{
	protected MonitorDao monitorDao = new MonitorDao();
	protected MonitorCheckDao checkDao = new MonitorCheckDao();
	protected MonitorPermissionManager pm = null;
	
	
	@Override
	@GET
	public Response getCheck(@PathParam("monitorUuid") String monitorUuid, 
							 @PathParam("checkUuid") String checkUuid)
	{
		AgaveLogServiceClient.log(MONITORS02.name(), 
				MonitorCheckGetById.name(), 
				getAuthenticatedUsername(), "", 
				Request.getCurrent().getClientInfo().getAddress());
		
		try
		{
			Monitor monitor = monitorDao.findByUuidWithinSessionTenant(monitorUuid);
			
			if (monitor == null) 
			{
				throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
						"No monitor with the given id found");
			} 
			else
			{
				pm = new MonitorPermissionManager(monitor);
				if (pm.canRead(getAuthenticatedUsername())) 
				{
					MonitorCheck check = checkDao.findByUuid(checkUuid);
					if (check == null)
					{
						throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
								"No monitor check with the given id found");
					}
					else if (!check.getMonitor().getUuid().equals(monitorUuid))
					{
						throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
								"No check with the given id found for the given monitor found");
					}
					else
					{
						return Response.ok(new AgaveSuccessRepresentation(
								check.toJSON())).build();
					}
				} 
				else 
				{
					throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
							"User does not have permission to view checks for this monitor");
				}	
			}
		}
		catch (ResourceException e) {
			throw e;
		} 
		catch (Exception e )
		{
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
					"Failed to retrieve monitor check: " + e.getMessage(), e);
		}
	}
}
