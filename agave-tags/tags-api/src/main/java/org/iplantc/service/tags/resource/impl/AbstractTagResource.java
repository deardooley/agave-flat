package org.iplantc.service.tags.resource.impl;

import org.iplantc.service.common.clients.AgaveLogServiceClient;
import org.iplantc.service.common.clients.AgaveLogServiceClient.ServiceKeys;
import org.iplantc.service.common.restlet.resource.AbstractAgaveResource;
import org.iplantc.service.tags.dao.TagDao;
import org.iplantc.service.tags.exceptions.TagException;
import org.iplantc.service.tags.exceptions.TagPermissionException;
import org.iplantc.service.tags.managers.TagPermissionManager;
import org.iplantc.service.tags.model.Tag;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;

/**
 * @author dooley
 *
 */
public class AbstractTagResource extends AbstractAgaveResource {

    /**
     * 
     */
    public AbstractTagResource() {}
    
    /**
     * Fetches the {@link Tag} object for the uuid in the URL or throws 
     * an exception that can be re-thrown from the route method.
     * @param tagId
     * @return Tag object referenced in the path
     * @throws ResourceException
     */
    protected Tag getResourceFromPathValue(String uuid)
    throws TagException
    {
    	Tag existingTag = new TagDao().findByNameOrUuid(uuid, getAuthenticatedUsername());
        
        if (existingTag == null) {
            throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
                    "No tag found matching " + uuid);
        } else {
        	TagPermissionManager pm = new TagPermissionManager(existingTag);
        	try {
        		if (!pm.canRead(getAuthenticatedUsername())) {
	        		throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
	                        "User does not have permission to view this tag");
	        	}
        	}
        	catch (TagPermissionException e) {
        		throw new ResourceException(Status.SERVER_ERROR_INTERNAL, 
        				"Unable to verify user permission for this tag.");
        	}
        }
        
        return existingTag;
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