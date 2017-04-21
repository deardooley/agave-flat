/**
 * 
 */
package org.iplantc.service.monitor.resources.impl;

import static org.iplantc.service.common.clients.AgaveLogServiceClient.ActivityKeys.MonitorChecksList;
import static org.iplantc.service.common.clients.AgaveLogServiceClient.ActivityKeys.MonitorTrigger;
import static org.iplantc.service.common.clients.AgaveLogServiceClient.ServiceKeys.MONITORS02;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.codehaus.plexus.util.StringUtils;
import org.iplantc.service.common.clients.AgaveLogServiceClient;
import org.iplantc.service.common.representation.AgaveSuccessRepresentation;
import org.iplantc.service.common.restlet.resource.AbstractAgaveResource;
import org.iplantc.service.common.util.StringToTime;
import org.iplantc.service.monitor.Settings;
import org.iplantc.service.monitor.dao.MonitorCheckDao;
import org.iplantc.service.monitor.dao.MonitorDao;
import org.iplantc.service.monitor.events.MonitorEventProcessor;
import org.iplantc.service.monitor.exceptions.MonitorException;
import org.iplantc.service.monitor.managers.MonitorManager;
import org.iplantc.service.monitor.managers.MonitorPermissionManager;
import org.iplantc.service.monitor.model.Monitor;
import org.iplantc.service.monitor.model.MonitorCheck;
import org.iplantc.service.monitor.model.enumeration.MonitorCheckType;
import org.iplantc.service.monitor.model.enumeration.MonitorStatusType;
import org.iplantc.service.monitor.resources.MonitorCheckCollection;
import org.iplantc.service.monitor.util.ServiceUtils;
import org.joda.time.DateTime;
import org.restlet.Request;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;

/**
 * @author dooley
 *
 */
@Path("{monitorUuid}/checks")
@Produces(MediaType.APPLICATION_JSON)
public class MonitorCheckCollectionImpl extends AbstractAgaveResource implements MonitorCheckCollection
{
	protected MonitorDao monitorDao = new MonitorDao();
	protected MonitorCheckDao checkDao = new MonitorCheckDao();
	protected MonitorPermissionManager pm = null;
	
	

	/* (non-Javadoc)
	 * @see org.iplantc.service.monitor.resources.MonitorResource#fireMonitor()
	 */
	@Override
	@POST
	public Response runMonitor(@PathParam("monitorUuid") String monitorUuid)
	{
		AgaveLogServiceClient.log(MONITORS02.name(), 
				MonitorTrigger.name(), 
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
					if (new Date().getTime() > (monitor.getLastUpdated().getTime() + (1000 *  Settings.MINIMUM_MONITOR_REPEAT_INTERVAL)) || 
						ServiceUtils.isAdmin(getAuthenticatedUsername())) {
						
						// send an event stating that a forced check was being made
						MonitorEventProcessor eventProcessor = new MonitorEventProcessor();
						eventProcessor.processForceCheckEvent(monitor, getAuthenticatedUsername());
						
						MonitorCheck check = new MonitorManager().check(monitor, getAuthenticatedUsername());
						if (check != null) {
							return Response.ok(new AgaveSuccessRepresentation(check.toJSON())).build();
						} else {
							throw new MonitorException("Failed to invoke monitor after " + Settings.MAX_MONITOR_RETRIES + " attempts.");
						}
					}
					else {
						throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
								"Monitor last ran within the minimum check threshold of " + Settings.MINIMUM_MONITOR_REPEAT_INTERVAL + 
								" seconds. The next available run time is " + new DateTime(monitor.getNextUpdateTime()).toString());
					}
				} else {
					throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
							"User does not have permission to invoke this monitor");
				}
			}
		}
		catch (ResourceException e) {
			throw e;
		} 
		catch (Exception e )
		{
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
					"Failed to invoke monitor: " + e.getMessage(), e);
		}
	}

	@Override
	@GET
	public Response getChecks(@PathParam("monitorUuid") String monitorUuid,
								@QueryParam("startDate") String startDate,
								@QueryParam("endDate") String endDate,	
								@QueryParam("result") String checkResult,
								@QueryParam("type") String type)
	{
		AgaveLogServiceClient.log(MONITORS02.name(), 
				MonitorChecksList.name(), 
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
					MonitorStatusType result = null;
					if (!StringUtils.isEmpty(checkResult))
					{
						try {
							result = MonitorStatusType.valueOf(checkResult.toUpperCase());
						} catch (Exception e) {
							throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
									"Invalid result value. If provided, please specify one of " + 
									Arrays.toString(MonitorStatusType.values()));
						}
					}
					
					MonitorCheckType checkType = null;
                    if (!StringUtils.isEmpty(type))
                    {
                        try {
                            checkType = MonitorCheckType.valueOf(type.toUpperCase());
                        } catch (Exception e) {
                            throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
                                    "Invalid type value. If provided, please specify one of " + 
                                    Arrays.toString(MonitorCheckType.values()));
                        }
                    }
						
					StringToTime stt = new StringToTime();
					Date start = null;
					if (!StringUtils.isEmpty(startDate)) {
						stt = new StringToTime(startDate);
						
						start = new Date(stt.getTime());
					}
					
					Date end = null;
					if (!StringUtils.isEmpty(endDate)) {
						stt = new StringToTime(endDate);
						end = new Date(stt.getTime());
					}
					
					List<MonitorCheck> checks = checkDao.getPaginatedByIdAndRange(
							monitor.getId(), checkType, start, end, result, getLimit(), getOffset() );

					StringBuilder builder = new StringBuilder();
					for (MonitorCheck check: checks) {
						builder.append(check.toJSON() + ",");
					}
					
					return Response.ok(new AgaveSuccessRepresentation(
							"[" + StringUtils.substring(builder.toString(), 0, -1) + "]")).build();
				} 
				else 
				{
					throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
							"User does not have access to view checks for this monitor");
				}
			}
		}
		catch (ResourceException e) {
			throw e;
		} 
		catch (Exception e )
		{
			e.printStackTrace();
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
					"Failed to retrieve monitor checks: " + e.getMessage(), e);
		}
	}
}
