/**
 * 
 */
package org.iplantc.service.notification.resources.impl;

import static org.iplantc.service.common.clients.AgaveLogServiceClient.ActivityKeys.NotifAttemptDelete;
import static org.iplantc.service.common.clients.AgaveLogServiceClient.ActivityKeys.NotifAttemptDetails;
import static org.iplantc.service.common.clients.AgaveLogServiceClient.ServiceKeys.NOTIFICATIONS02;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.iplantc.service.common.clients.AgaveLogServiceClient;
import org.iplantc.service.common.exceptions.UUIDException;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.common.representation.AgaveSuccessRepresentation;
import org.iplantc.service.common.uuid.AgaveUUID;
import org.iplantc.service.notification.Settings;
import org.iplantc.service.notification.dao.NotificationAttemptDao;
import org.iplantc.service.notification.dao.NotificationDao;
import org.iplantc.service.notification.exceptions.NotificationException;
import org.iplantc.service.notification.managers.NotificationPermissionManager;
import org.iplantc.service.notification.model.Notification;
import org.iplantc.service.notification.model.NotificationAttempt;
import org.iplantc.service.notification.resources.NotificationAttemptResource;
import org.restlet.Request;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;
import org.testng.log4testng.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author dooley
 *
 */
@Produces(MediaType.APPLICATION_JSON)
@Path("{notificationUuid}/attempts/{attemptUuid}")
public class NotificationAttemptResourceImpl extends AbstractNotificationResource implements NotificationAttemptResource
{
	private static final Logger log = Logger.getLogger(NotificationAttemptResourceImpl.class);
	protected NotificationDao dao = new NotificationDao();
	protected NotificationPermissionManager pm = null;
	protected NotificationAttemptDao attemptDao = new NotificationAttemptDao();
	
	/* (non-Javadoc)
	 * @see org.iplantc.service.notification.resources.NotificationResource#getNotifications()
	 */
	@Override
	@GET
	public Response getNotificationAttempt(@PathParam("notificationUuid") String notificationUuid, 
		   							 	   @PathParam("attemptUuid") String attemptUuid)
	{	
		AgaveLogServiceClient.log(NOTIFICATIONS02.name(), 
				NotifAttemptDetails.name(), 
				getAuthenticatedUsername(), "", 
				Request.getCurrent().getClientInfo().getUpstreamAddress());
		
		try
		{
			Notification notification = getResourceFromPathValue(notificationUuid);
			
			NotificationAttempt attempt = attemptDao.findByUuid(attemptUuid);
			
			if (attempt == null) {
	            throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
	                    "No notification attempt found matching " + attemptUuid + " for notification " + 
	                    notificationUuid);
	        } 
			
			ObjectMapper mapper = new ObjectMapper();
			ObjectNode json = mapper.valueToTree(attempt);
			
			ObjectNode links = json.putObject("_links");
            links.putObject("self")
                 .put("href", TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_NOTIFICATION_SERVICE) + notification.getUuid() + "/attempts/" + attempt.getUuid());
            links.putObject("notification")
            	.put("href", TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_NOTIFICATION_SERVICE) + notification.getUuid() );
            links.putObject("profile")
        		.put("href", TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_PROFILE_SERVICE) + attempt.getOwner());
            
            try {
            	if (!StringUtils.contains("*", notification.getAssociatedUuid()) 
            			&& !StringUtils.isEmpty(notification.getAssociatedUuid())) {
            		AgaveUUID agaveUUID = new AgaveUUID(notification.getAssociatedUuid());
            		links.putObject(agaveUUID.getResourceType().name().toLowerCase())
            			.put("href", TenancyHelper.resolveURLToCurrentTenant(agaveUUID.getObjectReference()));
            	}
            } catch (UUIDException e) {
            	log.debug("Unknown associatedUuid found for notification attempt " + attempt.getUuid());
    		} 
				
            return Response.ok(new AgaveSuccessRepresentation()).build();
		}
		catch (NotificationException e)
		{
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
					"Failed to retrieve notifications.", e);
		}
		catch (ResourceException e) {
			throw e;
		} 
		
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.notification.resources.NotificationAttemptFailureResource#deleteNotificationAttempt(java.lang.String, java.lang.String)
	 */
	@Override
	@DELETE
	public Response deleteNotificationAttempt(@PathParam("notificationUuid") String notificationUuid, 
		 	   								  @PathParam("attemptUuid") String attemptUuid)
	{
		AgaveLogServiceClient.log(NOTIFICATIONS02.name(), 
				NotifAttemptDelete.name(), 
				getAuthenticatedUsername(), "", 
				Request.getCurrent().getClientInfo().getUpstreamAddress());
		
		try
		{
			Notification notification = getResourceFromPathValue(notificationUuid);
			
			NotificationAttempt attempt = attemptDao.findByUuid(attemptUuid);
			
			if (attempt == null) {
	            throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
	                    "No notification attempt found matching " + attemptUuid + " for notification " + 
	                    notificationUuid);
	        } 
			
			pm = new NotificationPermissionManager(notification);
			if (pm.canWrite(getAuthenticatedUsername())) 
			{
				attemptDao.delete(attempt);
				return Response.ok(new AgaveSuccessRepresentation()).build();
			} 
			else 
			{
				throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
						"User does not have access to delete this notification attempt");
			}
		}
		catch (ResourceException e) {
			throw e;
		} 
		catch (Exception e) {
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL, 
					"Failed to delete notification: " + e.getMessage());
		}
	}

}
