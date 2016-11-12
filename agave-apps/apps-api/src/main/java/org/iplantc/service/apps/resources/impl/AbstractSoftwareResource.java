/**
 * 
 */
package org.iplantc.service.apps.resources.impl;

import java.net.URLDecoder;

import org.iplantc.service.apps.dao.SoftwareDao;
import org.iplantc.service.apps.model.Software;
import org.iplantc.service.common.clients.AgaveLogServiceClient;
import org.iplantc.service.common.clients.AgaveLogServiceClient.ServiceKeys;
import org.iplantc.service.common.restlet.resource.AbstractAgaveResource;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;

/**
 * @author dooley
 *
 */
public class AbstractSoftwareResource extends AbstractAgaveResource {

    /**
     * 
     */
    public AbstractSoftwareResource() {}
    
    /**
     * Fetches the {@link Software} object for the id in the URL or throws 
     * an exception that can be re-thrown from the route method.
     * @param softwareId
     * @return Software object referenced in the path
     * @throws ResourceException
     */
    protected Software getSoftwareFromPathValue(String softwareId)
    throws ResourceException
    {
    	Software existingSoftware = SoftwareDao.getSoftwareByUniqueName(softwareId);
        
        //
        // update if the existing software belongs to the user, otherwise throw an exception
        if (existingSoftware == null) {
            throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
                    "No software found matching " + softwareId);
        } 
        
        return existingSoftware;
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
        return AgaveLogServiceClient.ServiceKeys.APPS02;
    }
    

}
