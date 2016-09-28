package org.iplantc.service.notification.resources.impl;

import org.restlet.data.Status;
import org.iplantc.service.common.clients.AgaveLogServiceClient;
import org.iplantc.service.common.clients.AgaveLogServiceClient.ServiceKeys;
import org.iplantc.service.common.restlet.resource.AbstractAgaveResource;
import org.iplantc.service.notification.dao.NotificationDao;
import org.iplantc.service.notification.exceptions.NotificationException;
import org.iplantc.service.notification.managers.NotificationPermissionManager;
import org.iplantc.service.notification.model.Notification;
import org.restlet.resource.ResourceException;

/**
 * @author dooley
 *
 */
public class AbstractNotificationResource extends AbstractAgaveResource {

    /**
     * 
     */
    public AbstractNotificationResource() {}
    
    /**
     * Fetches the {@link Notification} object for the uuid in the URL or throws 
     * an exception that can be re-thrown from the route method.
     * @param softwareId
     * @return Software object referenced in the path
     * @throws ResourceException
     */
    protected Notification getResourceFromPathValue(String uuid)
    throws NotificationException
    {
    	Notification existingNotification = new NotificationDao().findByUuid(uuid);
        
    	
        if (existingNotification == null) {
            throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
                    "No notification found matching " + uuid);
        } 
        else if (!existingNotification.isVisible()) {
            throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
                    "Notification " + uuid + " has been deleted.");
        } else {
        	NotificationPermissionManager pm = new NotificationPermissionManager(existingNotification);
        	if (!pm.canRead(getAuthenticatedUsername())) {
        		throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
                        "User does not have permission to view this notification");
        	}
        }
        
        return existingNotification;
    }

    /**
     * Convenience class to log usage info per request
     * @param action
     */
    protected void logUsage(AgaveLogServiceClient.ActivityKeys activityKey) {
        AgaveLogServiceClient.log(getServiceKey().name(), activityKey.name(),
                getAuthenticatedUsername(), "", org.restlet.Request.getCurrent().getClientInfo().getUpstreamAddress());
    }
    
    protected ServiceKeys getServiceKey() {
        return AgaveLogServiceClient.ServiceKeys.NOTIFICATIONS02;
    }
    

}