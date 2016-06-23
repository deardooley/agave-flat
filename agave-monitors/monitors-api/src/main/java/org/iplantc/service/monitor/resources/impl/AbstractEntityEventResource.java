/**
 * 
 */
package org.iplantc.service.monitor.resources.impl;

import java.io.FileNotFoundException;

import org.iplantc.service.common.clients.AgaveLogServiceClient;
import org.iplantc.service.common.clients.AgaveLogServiceClient.ServiceKeys;
import org.iplantc.service.common.dao.EntityEventDao;
import org.iplantc.service.common.exceptions.PermissionException;
import org.iplantc.service.common.model.AgaveEntityEvent;
import org.iplantc.service.common.restlet.resource.AbstractAgaveResource;
import org.iplantc.service.monitor.dao.MonitorDao;
import org.iplantc.service.monitor.exceptions.MonitorException;
import org.iplantc.service.monitor.managers.MonitorPermissionManager;
import org.iplantc.service.monitor.model.Monitor;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;

/**
 * @author dooley
 *
 */
public abstract class AbstractEntityEventResource<T extends AgaveEntityEvent, V extends Enum<?>> extends AbstractAgaveResource {

    /**
     * No args constructor
     */
    public AbstractEntityEventResource() {}
    
    /**
     * Returns concrete instance of a {@link EntityEventDao} appropriate
     * for the implementing class.
     * 
     * @return
     */
    public abstract EntityEventDao<T,V> getEntityEventDao();
    
    protected EntityEventDao<T, V> entityEventDao;
    
    
//    /**
//     * Returns concrete instance of a {@link PermissionManager} appropriate
//     * for the implementing class.
//     * 
//     * @return
//     */
//    public abstract EntityPermissionManager<T> getEntityPermissionManager();
//    
    /**
     * Fetches the {@link AgaveEntity} object for the id in the URL or throws 
     * an exception that can be re-thrown from the route method.
     * @param entityId
     * @return entity with the given uuid
     * @throws ResourceException if permission is invalid or not found
     */
    protected Monitor getEntityFromPathValue(String entityId)
    throws ResourceException
    {
    	try {
	        Monitor entity = new MonitorDao().findByUuidWithinSessionTenant(entityId);
	        
	        // update if the existing software belongs to the user, otherwise throw an exception
	        if (entity == null) {
	            throw new FileNotFoundException();
	        } 
	        
	        // check permission
	        MonitorPermissionManager pm = new MonitorPermissionManager(entity);
	        if (!pm.canRead(getAuthenticatedUsername())) {
	        	throw new PermissionException();
	        }
	        
	        return entity;
    	} 
    	catch (FileNotFoundException e) {
    		throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
                    "No resurce found matching " + entityId);
    	}
    	catch (PermissionException e) {
    		throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
                    "User does not have permission to view the given resource");
    	}
    	catch (MonitorException e) {
    		throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
                    "Failed to fetch monitor record " + entityId + 
                    ". If this persists, please contact your tenant admin.");
    	}
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
        return AgaveLogServiceClient.ServiceKeys.MONITORS02;
    }
    

}
