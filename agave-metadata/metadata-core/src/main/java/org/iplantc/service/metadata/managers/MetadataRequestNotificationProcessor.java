package org.iplantc.service.metadata.managers;

import java.util.ArrayList;
import java.util.List;

import org.iplantc.service.notification.dao.NotificationDao;
import org.iplantc.service.notification.exceptions.NotificationException;
import org.iplantc.service.notification.model.Notification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author dooley
 *
 */
public class MetadataRequestNotificationProcessor {

	private List<Notification> notifications;
	private String owner;
	private String uuid;
	
	public MetadataRequestNotificationProcessor(String owner, String uuid) {
		this.setNotifications(new ArrayList<Notification>());
		this.setOwner(owner);
		this.setUuid(uuid);
	}
	
	/**
	 * Processes a {@link JsonNode} passed in with a job request as a 
	 * notification configuration. Accepts an array of {@link Notification} 
	 * request objects or a simple string;
	 *  
	 * @param json
	 * @throws NotificationException
	 */
	public void process(ArrayNode json) throws NotificationException {
		
		getNotifications().clear();
		
		if (json == null || json.isNull()) {
			// ignore the null value
			return;
		}
		else
		{
			NotificationDao dao = new NotificationDao();
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
						((ObjectNode)jsonNotif).put("associatedUuid", getUuid());
						notification = Notification.fromJSON(jsonNotif);
						notification.setOwner(getOwner());
					} 
					catch (NotificationException e) {
						throw e;
					} 
					catch (Throwable e) {
						throw new NotificationException("Unable to process notification.", e);
					}
					
					dao.persist(notification);
					
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
	 * @return the owner
	 */
	public String getOwner() {
		return owner;
	}

	/**
	 * @param owner the owner to set
	 */
	public void setOwner(String owner) {
		this.owner = owner;
	}

	/**
	 * @return the uuid
	 */
	public String getUuid() {
		return uuid;
	}

	/**
	 * @param uuid the uuid to set
	 */
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

}
