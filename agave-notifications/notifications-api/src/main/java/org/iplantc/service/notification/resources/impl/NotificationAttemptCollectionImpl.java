/**
 * 
 */
package org.iplantc.service.notification.resources.impl;

import static org.iplantc.service.common.clients.AgaveLogServiceClient.ActivityKeys.NotifAttemptClear;
import static org.iplantc.service.common.clients.AgaveLogServiceClient.ActivityKeys.NotifAttemptList;
import static org.iplantc.service.common.clients.AgaveLogServiceClient.ActivityKeys.NotifList;
import static org.iplantc.service.common.clients.AgaveLogServiceClient.ActivityKeys.NotifSearch;
import static org.iplantc.service.common.clients.AgaveLogServiceClient.ServiceKeys.NOTIFICATIONS02;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.hibernate.HibernateException;
import org.iplantc.service.common.clients.AgaveLogServiceClient;
import org.iplantc.service.common.exceptions.UUIDException;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.common.representation.AgaveSuccessRepresentation;
import org.iplantc.service.common.restlet.resource.AbstractAgaveResource;
import org.iplantc.service.common.search.SearchTerm;
import org.iplantc.service.common.uuid.AgaveUUID;
import org.iplantc.service.notification.Settings;
import org.iplantc.service.notification.dao.FailedNotificationAttemptQueue;
import org.iplantc.service.notification.dao.NotificationAttemptDao;
import org.iplantc.service.notification.dao.NotificationDao;
import org.iplantc.service.notification.events.NotificationMessageProcessor;
import org.iplantc.service.notification.exceptions.NotificationException;
import org.iplantc.service.notification.managers.NotificationPermissionManager;
import org.iplantc.service.notification.model.Notification;
import org.iplantc.service.notification.model.NotificationAttempt;
import org.iplantc.service.notification.model.NotificationPolicy;
import org.iplantc.service.notification.model.enumerations.NotificationStatusType;
import org.iplantc.service.notification.resources.NotificationAttemptCollection;
import org.iplantc.service.notification.resources.NotificationResource;
import org.iplantc.service.notification.util.ServiceUtils;
import org.joda.time.DateTime;
import org.restlet.Request;
import org.restlet.data.Status;
import org.restlet.ext.fileupload.RestletFileUpload;
import org.restlet.representation.Representation;
import org.restlet.resource.ResourceException;
import org.testng.log4testng.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author dooley
 *
 */
@Produces(MediaType.APPLICATION_JSON)
@Path("{uuid}/attempts")
public class NotificationAttemptCollectionImpl extends AbstractNotificationCollection implements NotificationAttemptCollection
{
	private static final Logger log = Logger.getLogger(NotificationAttemptCollectionImpl.class);
	protected NotificationDao dao = new NotificationDao();
	protected NotificationAttemptDao attemptDao = new NotificationAttemptDao();
	protected NotificationPermissionManager pm = null;
	
	/* (non-Javadoc)
	 * @see org.iplantc.service.notification.resources.NotificationAttemptCollection#getNotificationAttempts(java.lang.String)
	 */
	@Override
	@GET
	public Response getNotificationAttempts(@PathParam("uuid") String notificationUuid)
	{	
		try
		{
			Notification notification = getResourceFromPathValue(notificationUuid);
			
			List<NotificationAttempt> attempts = null;
			Map<SearchTerm, Object>  searchCriteria = getQueryParameters();
			
			if (searchCriteria.isEmpty()) {
				AgaveLogServiceClient.log(NOTIFICATIONS02.name(), 
						NotifList.name(), 
						getAuthenticatedUsername(), "", 
						Request.getCurrent().getClientInfo().getUpstreamAddress());
				attempts = FailedNotificationAttemptQueue.getInstance().findMatching(notification.getUuid(), searchCriteria, getLimit(), getOffset());
			} else {
				AgaveLogServiceClient.log(NOTIFICATIONS02.name(), 
						NotifSearch.name(), 
						getAuthenticatedUsername(), "", 
						Request.getCurrent().getClientInfo().getUpstreamAddress());
				attempts = FailedNotificationAttemptQueue.getInstance().findMatching(notification.getUuid(), searchCriteria, getLimit(), getOffset());
			}
			
			ObjectMapper mapper = new ObjectMapper();
	        ArrayNode jsonAttempts = mapper.createArrayNode();
	        
            for (NotificationAttempt attempt: attempts) 
            {   
                ObjectNode json = mapper.createObjectNode()
                    .put("id", attempt.getUuid())
                    .put("url", attempt.getCallbackUrl())
                    .put("event", attempt.getEventName())
                    .put("associatedUuid", attempt.getAssociatedUuid())
                    .put("startTime", attempt.getStartTime() == null ? null : new DateTime(attempt.getStartTime()).toString())
                    .put("endTime", attempt.getEndTime() == null ? null : new DateTime(attempt.getEndTime()).toString());
                json.put("response", mapper.valueToTree(attempt.getResponse()));
                
                
                ObjectNode links = json.putObject("_links");
                links.putObject("self")
                     .put("href", TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_NOTIFICATION_SERVICE) + notification.getUuid() + "/attempts/" + attempt.getUuid());
                links.putObject("notification")
                	.put("href", TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_NOTIFICATION_SERVICE) + notification.getUuid());
                links.putObject("profile")
            		.put("href", TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_PROFILE_SERVICE) + attempt.getOwner());
                
                try {
                	AgaveUUID agaveUUID = new AgaveUUID(notification.getAssociatedUuid());
                    links.putObject(agaveUUID.getResourceType().name().toLowerCase())
                         .put("href", TenancyHelper.resolveURLToCurrentTenant(agaveUUID.getObjectReference()));
                } catch (UUIDException e) {
                    log.debug("Unknown associatedUuid found for notification attempt " + attempt.getUuid());
                }
                
                jsonAttempts.add(json);
            }
            
            return Response.ok(new AgaveSuccessRepresentation(jsonAttempts.toString())).build();
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
	 * @see org.iplantc.service.notification.resources.NotificationAttemptCollection#clearNotificationAttempts(java.lang.String)
	 */
	@Override
	@DELETE
	public Response clearNotificationAttempts(@PathParam("uuid") String notificationUuid)
	{
		AgaveLogServiceClient.log(NOTIFICATIONS02.name(), 
				NotifAttemptClear.name(), 
				getAuthenticatedUsername(), "", 
				Request.getCurrent().getClientInfo().getUpstreamAddress());
		
		try
		{
			Notification notification = getResourceFromPathValue(notificationUuid);
			
			pm = new NotificationPermissionManager(notification);
			
			if (pm.canWrite(getAuthenticatedUsername())) 
			{
				attemptDao.clearNotificationAttemptsforUuid(notificationUuid);
				
				return Response.ok(new AgaveSuccessRepresentation()).build();
			} 
			else 
			{
				throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
						"User does not have access to clear attempts for this notification");
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
