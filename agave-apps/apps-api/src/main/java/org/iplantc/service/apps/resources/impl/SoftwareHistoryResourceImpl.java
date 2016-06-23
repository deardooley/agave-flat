/**
 * 
 */
package org.iplantc.service.apps.resources.impl;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;
import org.iplantc.service.apps.dao.SoftwareEventDao;
import org.iplantc.service.apps.exceptions.SoftwareException;
import org.iplantc.service.apps.managers.ApplicationManager;
import org.iplantc.service.apps.model.Software;
import org.iplantc.service.apps.model.SoftwareEvent;
import org.iplantc.service.apps.resources.SoftwareHistoryResource;
import org.iplantc.service.common.clients.AgaveLogServiceClient;
import org.iplantc.service.common.representation.AgaveSuccessRepresentation;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;

/**
 * Returns a collection of history records for a {@link SoftwareEvent}. Search
 * and pagination are support. All actions are readonly.
 * @author dooley
 *
 */
@Path("{softwareId}/history/{uuid}")
public class SoftwareHistoryResourceImpl extends AbstractSoftwareCollection implements SoftwareHistoryResource {
    
    private static final Logger log = Logger.getLogger(SoftwareHistoryResourceImpl.class);
    
    @GET
    public Response getSofwareEvent(@PathParam("softwareId") String softwareId,
                                    @PathParam("uuid") String uuid) {
        logUsage(AgaveLogServiceClient.ActivityKeys.AppsHistoryList);
        
        try 
        {
            Software software = getSoftwareFromPathValue(softwareId);
            
            if (!ApplicationManager.isVisibleByUser(software, getAuthenticatedUsername()))
            {
                throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
                        "Permission denied. You do not have permission to view this application");
            } 
            else if (software.isPubliclyAvailable() && !software.isAvailable()) 
            {
                throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
                    "This application has been removed by the administrator.");
            }
            
            SoftwareEvent event = new SoftwareEventDao().getBySoftwareUuidAndUuid(software.getUuid(), uuid);
            
            if (event == null) {
                throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
                        String.format("No app event found for %s with id %s", softwareId, uuid));
            } else {            
                return Response.ok(new AgaveSuccessRepresentation(event.toJSON(software))).build();
            }
        }
        catch (SoftwareException e)
        {
            throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Failed to locate app history: " + e.getMessage(), e);
        }
        catch (ResourceException e) {
            throw e;
        }
        catch (Exception e)
        {
            log.error(e);
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL, "Failed to retrieve app history", e);
        }
    }
}
