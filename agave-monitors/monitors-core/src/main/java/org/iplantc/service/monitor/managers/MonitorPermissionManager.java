package org.iplantc.service.monitor.managers;

import org.apache.commons.lang.StringUtils;
import org.iplantc.service.monitor.model.Monitor;
import org.iplantc.service.monitor.util.ServiceUtils;

public class MonitorPermissionManager {
	
	private Monitor monitor;
	
	public MonitorPermissionManager(Monitor monitor) {
		this.monitor = monitor;
	}
	
	public boolean canRead(String username) 
	{
		if (monitor == null || StringUtils.isEmpty(username)) 
		{
			return false;
		} 
		else 
		{
			return (ServiceUtils.isAdmin(username) || 
					StringUtils.equals(username, monitor.getOwner()));
		}
	}
	
	public boolean canWrite(String username) 
	{
		if (monitor == null || StringUtils.isEmpty(username)) 
		{
			return false;
		} 
		else 
		{
			return (ServiceUtils.isAdmin(username) || 
					StringUtils.equals(username, monitor.getOwner()));
		}
	}

}
