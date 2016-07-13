/**
 * 
 */
package org.iplantc.service.notification.resources.impl;

import static org.iplantc.service.common.clients.AgaveLogServiceClient.ActivityKeys.NotifAttemptClear;
import static org.iplantc.service.common.clients.AgaveLogServiceClient.ActivityKeys.NotifList;
import static org.iplantc.service.common.clients.AgaveLogServiceClient.ActivityKeys.NotifSearch;
import static org.iplantc.service.common.clients.AgaveLogServiceClient.ActivityKeys.NotifTrigger;
import static org.iplantc.service.common.clients.AgaveLogServiceClient.ServiceKeys.NOTIFICATIONS02;

import java.util.List;
import java.util.Map;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.common.clients.AgaveLogServiceClient;
import org.iplantc.service.common.exceptions.PermissionException;
import org.iplantc.service.common.exceptions.UUIDException;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.common.representation.AgaveSuccessRepresentation;
import org.iplantc.service.common.search.SearchTerm;
import org.iplantc.service.common.uuid.AgaveUUID;
import org.iplantc.service.notification.Settings;
import org.iplantc.service.notification.dao.FailedNotificationAttemptQueue;
import org.iplantc.service.notification.dao.NotificationAttemptDao;
import org.iplantc.service.notification.dao.NotificationDao;
import org.iplantc.service.notification.events.NotificationMessageProcessor;
import org.iplantc.service.notification.exceptions.BadCallbackException;
import org.iplantc.service.notification.exceptions.NotificationException;
import org.iplantc.service.notification.managers.NotificationManager;
import org.iplantc.service.notification.managers.NotificationPermissionManager;
import org.iplantc.service.notification.model.Notification;
import org.iplantc.service.notification.model.NotificationAttempt;
import org.iplantc.service.notification.model.enumerations.NotificationEventType;
import org.iplantc.service.notification.resources.NotificationAttemptCollection;
import org.iplantc.service.notification.util.ServiceUtils;
import org.joda.time.DateTime;
import org.restlet.Request;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.resource.ResourceException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
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
	 * @see org.iplantc.service.notification.resources.NotificationAttemptCollection#fireNotification()
	 */
	@Override
	@POST
	public Response fireNotification(@PathParam("uuid") String uuid, Representation input)
	{
		AgaveLogServiceClient.log(NOTIFICATIONS02.name(), 
				NotifTrigger.name(), 
				getAuthenticatedUsername(), "", 
				Request.getCurrent().getClientInfo().getUpstreamAddress());
		
		boolean userHasAdminPrivileges = ServiceUtils.isAdmin(getAuthenticatedUsername()); 
		ObjectMapper mapper = new ObjectMapper();
		
		try
		{
			Notification notification = getResourceFromPathValue(uuid);
			
			JsonNode json = getPostedContentAsJsonNode(input);
			
			String event = "FORCED_EVENT";
			
			String message = null;
			String associatedUuid = "";
			
			if (json.hasNonNull("event")) {
				if (json.get("event").getNodeType() == JsonNodeType.STRING) {
					event = json.get("event").asText("FORCED_EVENT");
					if (StringUtils.equalsIgnoreCase(event, "*")) {
						throw new NotificationException("Event must not be a wildcard value");
					}
					// if the notification has a wildcard event, let whatever go
					else if (StringUtils.equalsIgnoreCase(notification.getEvent(), "*")) {
						if (!userHasAdminPrivileges && !StringUtils.equalsIgnoreCase(event, "FORCED_EVENT")) {
							throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
									"User does not have access to trigger custom event notifications", 
									new PermissionException());
						}
						// let it go!!!
					}
					else if (!userHasAdminPrivileges) {
						if (!StringUtils.equalsIgnoreCase(event, "FORCED_EVENT")) {
							throw new NotificationException("only the generic FORCED_EVENT is supported "
									+ "for manual invocation by non-privileged users.");
						}
					}
					// otherwise, make sure the events match
					else if (!StringUtils.equalsIgnoreCase(notification.getEvent(), event)) {
						throw new NotificationException("event must match the " + notification.getEvent() + 
								" event of the registered notification.");
					}
				} 
				else {
					throw new NotificationException("Event must be a string value, " + json.getNodeType() + " was found.");
				}
			}
				
			AgaveUUID associatedAgaveUUID = null;
			if (json.hasNonNull("associatedUuid") && userHasAdminPrivileges) {
				if (json.get("associatedUuid").getNodeType() == JsonNodeType.STRING) {
					associatedUuid = json.get("associatedUuid").asText();
					// verify that they gave a valid uuid or the notification associatedUuidwill be used
					if (StringUtils.equalsIgnoreCase(associatedUuid, "*")) {
						if (StringUtils.equalsIgnoreCase(notification.getAssociatedUuid(),  "*")) {
							throw new UUIDException("associatedUuid must be a valid Agave resource UUID "
									+ "when the notification references a wilcard associatedUuid");
						}
						// assign the notification uuid as a convenience
						else {
							associatedUuid = notification.getAssociatedUuid();
						}
					}
					// they provided a non-wildcard value, so make sure it's valid.
					else {
						try {
							associatedAgaveUUID = new AgaveUUID(associatedUuid);
							// resolve the uuid to make sure it's valid
							associatedAgaveUUID.getObjectReference();
							// no exception, so let it through
						}
						catch (Exception e) {
							throw new UUIDException();
						}
					}
 				} 
				// a valid uuid is required
				else {
 					throw new UUIDException("associatedUuid must be a valid Agave resource UUID ");
				}
			}
			// verify the notification has a non-wildcard value
			else if (StringUtils.equalsIgnoreCase(notification.getAssociatedUuid(),  "*")) {
				throw new UUIDException("associatedUuid must be provided"
						+ "when the notification references a wilcard associatedUuid");
			}
			// use the notification associatedUuid
			else {
				associatedUuid = notification.getAssociatedUuid();
			}
			
				
			if (json.hasNonNull("message")) {
				if (userHasAdminPrivileges) {
					// if they gave a message, make sure it's valid json
					if (json.get("message").getNodeType() == JsonNodeType.STRING) {
						message = json.get("message").asText();
						try {
							JsonNode jsonMessage = mapper.readTree(message);
							if (json.getNodeType() != JsonNodeType.OBJECT) {
								throw new BadCallbackException("Notification message body must be a valid JSON object.");
							}
						} catch (JsonProcessingException e) {
							throw new BadCallbackException("Failed to parse the notification message body. " 
									+ e.getMessage(), e);
						}
					}
					// if they gave a valid json message when posting a json body, use that
					else if (json.get("message").getNodeType() == JsonNodeType.OBJECT) {
						message = json.get("message").toString();
					}
					// otherwise go mutumbo on them
					else {
						throw new BadCallbackException("Notification message body must be a valid JSON object.");
					}
				} 
				else { 
					throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
							"User does not have access to send custom event messages", 
							new PermissionException());
				}
				
			}
			else {
				message = mapper.createObjectNode()
						.put("id", notification.getId())
						.put("event", event)
						.put("owner", getAuthenticatedUsername())
						.put("associatedUuid", associatedUuid)
						.put("created", new DateTime().toString())
						.toString();
			}
			
			NotificationAttempt attempt = NotificationMessageProcessor.process(notification, event, getAuthenticatedUsername(), associatedUuid, message);
			
			NotificationManager.process(notification.getUuid(), 
					NotificationEventType.FORCED_ATTEMPT.name(), 
					getAuthenticatedUsername(), 
					notification.toJSON());
			
			return Response.ok(new AgaveSuccessRepresentation(mapper.writeValueAsString(attempt))).build();
		}
		catch (UUIDException e) {
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
					"associatedUuid must be a valid Agave resource UUID", e);
		}
		catch (NotificationException e) {
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
					e.getMessage(), e);
		}
		catch (BadCallbackException e) {
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
				e.getMessage(), e);
		}
		catch (ResourceException e) {
			throw e;
		} 
		catch (Exception e ) {
			log.error(e);
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
					e.getMessage(), e);
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
