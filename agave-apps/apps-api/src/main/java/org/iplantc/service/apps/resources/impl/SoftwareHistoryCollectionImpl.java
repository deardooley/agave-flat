/**
 * 
 */
package org.iplantc.service.apps.resources.impl;

import java.util.List;

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
import org.iplantc.service.apps.resources.SoftwareHistoryCollection;
import org.iplantc.service.common.clients.AgaveLogServiceClient;
import org.iplantc.service.common.representation.AgaveSuccessRepresentation;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

/**
 * Returns a collection of history records for a {@link SoftwareEvent}. Search
 * and pagination are support. All actions are readonly.
 * @author dooley
 *
 */
@Path("{softwareId}/history")
public class SoftwareHistoryCollectionImpl extends AbstractSoftwareCollection implements SoftwareHistoryCollection {
    
    private static final Logger log = Logger.getLogger(SoftwareHistoryCollectionImpl.class);
    
    @GET
    public Response getSofwareEvents(@PathParam("softwareId") String softwareId) {
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
                    "This app has been removed by the administrator.");
            }
            
            List<SoftwareEvent> events = new SoftwareEventDao().getBySoftwareUuid(software.getUuid(), getLimit(), getOffset());
            
            ObjectMapper mapper = new ObjectMapper();
            ArrayNode history = mapper.createArrayNode();
            for(SoftwareEvent event: events) {
                history.add(mapper.readTree(event.toJSON(software)));
            }
            
            return Response.ok(new AgaveSuccessRepresentation(history.toString())).build();
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
