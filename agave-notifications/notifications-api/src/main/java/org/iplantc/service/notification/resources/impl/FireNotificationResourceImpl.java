/**
 * 
 */
package org.iplantc.service.notification.resources.impl;

import static org.iplantc.service.common.clients.AgaveLogServiceClient.ActivityKeys.NotifTrigger;
import static org.iplantc.service.common.clients.AgaveLogServiceClient.ServiceKeys.NOTIFICATIONS02;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.iplantc.service.common.clients.AgaveLogServiceClient;
import org.iplantc.service.common.representation.AgaveSuccessRepresentation;
import org.iplantc.service.notification.dao.NotificationDao;
import org.iplantc.service.notification.events.NotificationMessageProcessor;
import org.iplantc.service.notification.managers.NotificationManager;
import org.iplantc.service.notification.managers.NotificationPermissionManager;
import org.iplantc.service.notification.model.Notification;
import org.iplantc.service.notification.model.enumerations.NotificationEventType;
import org.iplantc.service.notification.resources.FireNotificationResource;
import org.iplantc.service.notification.util.ServiceUtils;
import org.restlet.Request;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;
import org.testng.log4testng.Logger;

/**
 * @author dooley
 *
 */
@Produces(MediaType.APPLICATION_JSON)
public class FireNotificationResourceImpl extends AbstractNotificationResource implements FireNotificationResource
{
	private static final Logger log = Logger.getLogger(FireNotificationResourceImpl.class);
	protected NotificationDao dao = new NotificationDao();
	protected NotificationPermissionManager pm = null;
	
	/* (non-Javadoc)
	 * @see org.iplantc.service.notification.resources.NotificationResource#fireNotification()
	 */
	@Override
	@POST
	@Path("fire/{uuid}")
	public Response fireNotification(@PathParam("uuid") String uuid,
	                                 @QueryParam("event") String event)
	{
		AgaveLogServiceClient.log(NOTIFICATIONS02.name(), 
				NotifTrigger.name(), 
				getAuthenticatedUsername(), "", 
				Request.getCurrent().getClientInfo().getUpstreamAddress());
		
		if (!ServiceUtils.isAdmin(getAuthenticatedUsername())) 
		{
			throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
					"User does not have access to invoke this notification");
		}
		
		try
		{
			Notification notification = getResourceFromPathValue(uuid);
			if (StringUtils.isEmpty(event) ) {
				event = "FORCED_EVENT";
			}

			NotificationMessageProcessor.process(notification, event, getAuthenticatedUsername(), null, null);
			
			NotificationManager.process(notification.getUuid(), 
					NotificationEventType.FORCED_ATTEMPT.name(), 
					getAuthenticatedUsername(), 
					notification.toJSON());
			
			return Response.ok(new AgaveSuccessRepresentation(notification.toJSON())).build();
		}
		catch (ResourceException e) {
			throw e;
		} 
		catch (Exception e )
		{
			log.error(e);
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
					e.getMessage(), e);
		}
	}
}
