package org.iplantc.service.notification.managers;

import org.apache.commons.lang.StringUtils;
import org.iplantc.service.notification.model.Notification;
import org.iplantc.service.notification.util.ServiceUtils;

public class NotificationPermissionManager {
	
	private Notification notification;
	
	public NotificationPermissionManager(Notification trigger) {
		this.notification = trigger;
	}
	
	public boolean canRead(String username) 
	{
		if (notification == null || StringUtils.isEmpty(username)) 
		{
			return false;
		} 
		else 
		{
			return (ServiceUtils.isAdmin(username) || 
					StringUtils.equals(username, notification.getOwner()));
		}
	}
	
	public boolean canWrite(String username) 
	{
		if (notification == null || StringUtils.isEmpty(username)) 
		{
			return false;
		} 
		else 
		{
			return (ServiceUtils.isAdmin(username) || 
					StringUtils.equals(username, notification.getOwner()));
		}
	}

}
