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
import org.iplantc.service.common.auth.AuthorizationHelper;
import org.iplantc.service.common.clients.AgaveLogServiceClient;
import org.iplantc.service.common.exceptions.PermissionException;
import org.iplantc.service.common.exceptions.UUIDException;
import org.iplantc.service.common.representation.AgaveSuccessRepresentation;
import org.iplantc.service.common.uuid.AgaveUUID;
import org.iplantc.service.notification.dao.NotificationDao;
import org.iplantc.service.notification.events.NotificationMessageProcessor;
import org.iplantc.service.notification.exceptions.BadCallbackException;
import org.iplantc.service.notification.exceptions.NotificationException;
import org.iplantc.service.notification.managers.NotificationManager;
import org.iplantc.service.notification.managers.NotificationPermissionManager;
import org.iplantc.service.notification.model.Notification;
import org.iplantc.service.notification.model.NotificationAttempt;
import org.iplantc.service.notification.model.enumerations.NotificationEventType;
import org.iplantc.service.notification.resources.FireNotificationResource;
import org.iplantc.service.notification.util.ServiceUtils;
import org.restlet.Request;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.resource.ResourceException;
import org.testng.log4testng.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeType;

/**
 * @author dooley
 *
 */
//@Produces(MediaType.APPLICATION_JSON)
public class FireNotificationResourceImpl extends AbstractNotificationResource implements FireNotificationResource
{
	private static final Logger log = Logger.getLogger(FireNotificationResourceImpl.class);
	protected NotificationDao dao = new NotificationDao();
	protected NotificationPermissionManager pm = null;
	
	/* (non-Javadoc)
	 * @see org.iplantc.service.notification.resources.NotificationResource#fireNotification()
	 */
	@Override
//	@POST
//	@Path("{uuid}/attempts")
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
			
			if (userHasAdminPrivileges) {
				
				if (json.hasNonNull("event")) {
					if (json.getNodeType() == JsonNodeType.STRING) {
						event = json.get("event").asText("FORCED_EVENT");
						if (StringUtils.equalsIgnoreCase(event, "*")) {
							throw new NotificationException("Event must not be a wildcard value");
						}
						// if the notification has a wildcard event, let whatever go
						else if (StringUtils.equalsIgnoreCase(notification.getEvent(), "*")) {
							// let it go!!!
						}
						// otherwise, make sure the events match
						else if (!StringUtils.equalsIgnoreCase(notification.getEvent(), event)) {
							throw new NotificationException("event must match the " + notification.getEvent() + 
									" event of the registered notification.");
						}
					} 
					else {
						throw new NotificationException("Event must be a string value");
					}
				}
				
				AgaveUUID associatedAgaveUUID = null;
				if (json.hasNonNull("associatedUuid")) {
					if (json.getNodeType() == JsonNodeType.STRING) {
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
			}
			else {
				throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
						"User does not have access to trigger custom event notifications", 
						new PermissionException());
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
}
