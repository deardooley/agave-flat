package org.iplantc.service.notification.events;

import org.iplantc.service.common.uuid.AgaveUUID;
import org.iplantc.service.common.uuid.UUIDType;
import org.iplantc.service.notification.model.Notification;

public class EventFilterFactory {

	public static EventFilter getInstance(AgaveUUID uuid, Notification notification, String event, String owner)
	{
		UUIDType eventResourceType = uuid.getResourceType();
		
		if (eventResourceType.equals(UUIDType.APP)) {
			return new SoftwareNotificationEvent(uuid, notification, event, owner);
		} else if (eventResourceType.equals(UUIDType.FILE)) {
			return new FileNotificationEvent(uuid, notification,event, owner);
		} else if (eventResourceType.equals(UUIDType.INTERNALUSER)) {
			return new InternalUserNotificationEvent(uuid, notification,event, owner);
		} else if (eventResourceType.equals(UUIDType.JOB)) {
			return new JobNotificationEvent(uuid, notification,event, owner);
		} else if (eventResourceType.equals(UUIDType.METADATA)) {
			return new MetadataNotificationEvent(uuid, notification,event, owner);
		} else if (eventResourceType.equals(UUIDType.MONITOR)) {
			return new MonitorNotificationEvent(uuid, notification,event, owner);
		} else if (eventResourceType.equals(UUIDType.NOTIFICATION)) {
			return new NotificationNotificationEvent(uuid, notification,event, owner);
		} else if (eventResourceType.equals(UUIDType.POSTIT)) {
			return new PostItNotificationEvent(uuid, notification,event, owner);
		} else if (eventResourceType.equals(UUIDType.PROFILE)) {
			return new ProfileNotificationEvent(uuid, notification,event, owner);
		} else if (eventResourceType.equals(UUIDType.REALTIME_CHANNEL)) {
			return new RealtimeChannelNotificationEvent(uuid, notification,event, owner);
		} else if (eventResourceType.equals(UUIDType.SCHEMA)) {
			return new MetadataSchemaNotificationEvent(uuid, notification,event, owner);
		} else if (eventResourceType.equals(UUIDType.SYSTEM)) {
			return new SystemNotificationEvent(uuid, notification,event, owner);
		} else if (eventResourceType.equals(UUIDType.TAG)) {
			return new TagNotificationEvent(uuid, notification,event, owner);
		} else if (eventResourceType.equals(UUIDType.TOKEN)) {
			return new TokenNotificationEvent(uuid, notification,event, owner);
		} else if (eventResourceType.equals(UUIDType.TRANSFER)) {
			return new TransferNotificationEvent(uuid, notification,event, owner);
		} else if (eventResourceType.equals(UUIDType.TRANSFORM)) {
			return new TransformNotificationEvent(uuid, notification,event, owner);
		} else {
			return null;
		}
	}

}
