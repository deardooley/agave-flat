/**
 * 
 */
package org.iplantc.service.jobs.managers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.iplantc.service.apps.util.ServiceUtils;
import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.jobs.model.enumerations.JobMacroType;
import org.iplantc.service.jobs.model.enumerations.JobStatusType;
import org.iplantc.service.notification.exceptions.BadCallbackException;
import org.iplantc.service.notification.exceptions.NotificationException;
import org.iplantc.service.notification.model.Notification;
import org.iplantc.service.notification.model.enumerations.NotificationCallbackProviderType;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author dooley
 *
 */
public class JobRequestNotificationProcessor {

	private List<Notification> notifications;
	private String username;
	private Job job;
	
	public JobRequestNotificationProcessor(String jobRequestOwner, Job job) {
		this.setNotifications(new ArrayList<Notification>());
		this.setUsername(jobRequestOwner);
		this.job = job;
	}
	
	/**
	 * Processes a {@link String} passed in with a job request as a 
	 * notification configuration. Acceptable values are email addresses,
	 * URL, phone numbers, and agave uri. Anything else will throw 
	 * an exception.
	 *  
	 * @param json
	 * @throws NotificationException if an invalid callback URL value is given
	 */
	public void process(String callbackUrl) throws NotificationException {
		if (!StringUtils.isEmpty(callbackUrl)) {
			try {
				// validate the callback value they provided for support
				NotificationCallbackProviderType.getInstanceForUri(callbackUrl);
				
				// add default notifications for each terminal state
				this.notifications.add(new Notification(job.getUuid(), 
						username, JobStatusType.FINISHED.name(), callbackUrl, false));
				this.notifications.add(new Notification(job.getUuid(), 
						username, JobStatusType.FAILED.name(), callbackUrl, false));
				this.notifications.add(new Notification(job.getUuid(), 
						username, JobStatusType.STOPPED.name(), callbackUrl, false));
			}
			catch (BadCallbackException e) {
				throw new NotificationException("Invalid notification callback url provided", e);
			}
			catch (Exception e) {
				throw new NotificationException("Unable to create notification for the given value", e);
			}
		}
	}
	
	/**
	 * Processes a {@link JsonNode} passed in with a job request as a 
	 * notification configuration. Accepts an array of {@link Notification} 
	 * request objects or a simple string;
	 *  
	 * @param json
	 * @throws NotificationException
	 */
	public void process(JsonNode json) throws NotificationException {
		
		notifications.clear();
		
		if (json == null || json.isNull()) {
			// ignore the null value
			return;
		}
		else if (json.isValueNode()) {
			process(json.textValue());
		}
		else if (!json.isArray())
		{
			throw new NotificationException("Invalid notification value given. "
					+ "notifications must be an array of notification objects specifying a "
					+ "valid url, event, and an optional boolean persistence attribute.");
		}
		else
		{
			for (int i=0; i<json.size(); i++)
			{
				JsonNode jsonNotif = json.get(i);
				if (!jsonNotif.isObject())
				{
					throw new NotificationException("Invalid notifications["+i+"] value given. "
						+ "Each notification objects should specify a "
						+ "valid url, event, and an optional boolean persistence attribute.");
				}
				else
				{
					// here we reuse the validation built into the {@link Notification} model
					// itself to validate the embedded job notification subscriptions.
					Notification notification = new Notification();
					try {
						((ObjectNode)jsonNotif).put("associatedUuid", job.getUuid());
						notification = Notification.fromJSON(jsonNotif);
						notification.setOwner(job.getOwner());
					} 
					catch (NotificationException e) {
						throw e;
					} 
					catch (Throwable e) {
						throw new NotificationException("Unable to process notification.", e);
					}
					
//					Notification notification = new Notification();
//					currentKey = "notifications["+i+"].url";
//					if (!jsonNotif.has("url")) {
//						throw new NotificationException("No notifications["+i+"] attribute given. "
//								+ "Notifications must have valid url and event attributes.");
//					}
//					else
//					{
//						notification.setCallbackUrl(jsonNotif.get("url").textValue());
//					}
//
//					currentKey = "notifications["+i+"].event";
//					if (!jsonNotif.has("event")) {
//						throw new NotificationException("No notifications["+i+"] attribute given. "
//								+ "Notifications must have valid url and event attributes.");
//					}
//					else
//					{
//						String event = jsonNotif.get("event").textValue();
//						try {
//							if (!StringUtils.equals("*", event)) {
//								try {
//									JobStatusType.valueOf(event.toUpperCase());
//								} catch (IllegalArgumentException e) {
//									JobMacroType.valueOf(event.toUpperCase());
//								}
//								notification.setEvent(StringUtils.upperCase(event));
//							}
//							else {
//								notification.setEvent("*");
//							}
//						} catch (Throwable e) {
//							throw new NotificationException("Valid values are: *, " +
//									ServiceUtils.explode(", ", Arrays.asList(JobStatusType.values())) + ", " +
//									ServiceUtils.explode(", ", Arrays.asList(JobMacroType.values())));
//						}
//					}
//
//
//					if (jsonNotif.has("persistent"))
//					{
//						currentKey = "notifications["+i+"].persistent";
//						if (jsonNotif.get("persistent").isNull()) {
//							throw new NotificationException(currentKey + " cannot be null");
//						}
//						else if (!jsonNotif.get("persistent").isBoolean())
//						{
//							throw new NotificationException("Invalid value for " + currentKey + ". "
//									+ "If provided, " + currentKey + " must be a boolean value.");
//						} else {
//							notification.setPersistent(jsonNotif.get("persistent").asBoolean());
//						}
//					}
//					
//					notification.setOwner(getUsername());
//					notification.setAssociatedUuid(job.getUuid());
					
					getNotifications().add(notification);
				}
			}
		}
		
	}

	/**
	 * @return the notifications
	 */
	public List<Notification> getNotifications() {
		return notifications;
	}

	/**
	 * @param notifications the notifications to set
	 */
	public void setNotifications(List<Notification> notifications) {
		this.notifications = notifications;
	}

	/**
	 * @return the username
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * @param username the username to set
	 */
	public void setUsername(String username) {
		this.username = username;
	}

}
