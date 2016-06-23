/**
 * 
 */
package org.iplantc.service.notification.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.iplantc.service.common.Settings;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.notification.model.Notification;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Serializes an entity and adds an {@link ArrayNode} of references to the {@link Notification}
 * provided in the constructor.
 * 
 * @author dooley  
 * @param string
 * @param notifications
 * @return
 */
public class EntityWithNotificationReferenceSerializer<T> extends JsonSerializer<T> {

	private List<Notification> notifications;
	
	/**
	 * Default constructor. This sets the notifications to inject into the hypermedia
	 * response.
	 * @param notifications
	 */
	public EntityWithNotificationReferenceSerializer(List<Notification> notifications) {
		setNotifications(notifications);
	}
	
	/* (non-Javadoc)
	 * @see com.fasterxml.jackson.databind.JsonSerializer#serialize(java.lang.Object, com.fasterxml.jackson.core.JsonGenerator, com.fasterxml.jackson.databind.SerializerProvider)
	 */
	@Override
    public void serialize(T value, JsonGenerator jgen, SerializerProvider provider) 
	throws IOException, JsonProcessingException 
	{
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(mapper.writeValueAsString(value));
		
		if (rootNode.has("_links")) {

			String baseNotificationUrl = TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_NOTIFICATION_SERVICE);
			
			ArrayNode notifArray = mapper.createArrayNode();
			for(Notification n: getNotifications()) {
				notifArray.add(mapper.createObjectNode()
						.put("href", baseNotificationUrl + n.getUuid())
						.put("event", n.getEvent()));
			}
			
			
			((ObjectNode)rootNode.get("_links")).set("notification", notifArray);
		}
		
		jgen.writeRaw(rootNode.toString());
	}
	
	/**
	 * @return
	 */
	public List<Notification> getNotifications() {
		return this.notifications;
	}
	
	/**
	 * @param notifications
	 */
	public void setNotifications(List<Notification> notifications) {
		if (notifications == null) {
			this.notifications = new ArrayList<Notification>();
		}
		else {
			this.notifications = notifications;
		}
	}
}
