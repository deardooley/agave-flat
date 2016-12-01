/**
 * 
 */
package org.iplantc.service.tags.resource.impl;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;
import org.iplantc.service.common.clients.AgaveLogServiceClient;
import org.iplantc.service.common.representation.AgaveSuccessRepresentation;
import org.iplantc.service.tags.dao.TagEventDao;
import org.iplantc.service.tags.exceptions.TagException;
import org.iplantc.service.tags.model.Tag;
import org.iplantc.service.tags.model.TagEvent;
import org.iplantc.service.tags.resource.TagHistoryResource;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Returns a collection of history records for a {@link TagEvent}. Search
 * and pagination are support. All actions are readonly.
 * @author dooley
 *
 */
@Path("{entityId}/history/{uuid}")
public class TagHistoryResourceImpl extends AbstractTagCollection implements TagHistoryResource {
    
    private static final Logger log = Logger.getLogger(TagHistoryResourceImpl.class);
    
    @GET
    public Response getTagEvent(@PathParam("entityId") String entityId,
                                    @PathParam("uuid") String uuid) {
        logUsage(AgaveLogServiceClient.ActivityKeys.AppsHistoryList);
        
        try 
        {
            Tag tag = getResourceFromPathValue(entityId);
            
            TagEvent event = new TagEventDao().getByTagUuidAndUuid(tag.getUuid(), uuid);
            
            if (event == null) {
                throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
                        String.format("No tag event found for %s with id %s", entityId, uuid));
            } else {          
            	return Response.ok(new AgaveSuccessRepresentation(event.toJSON())).build();
            }
        }
        catch (ResourceException e) {
            throw e;
        }
        catch (Throwable e) {
        	log.error("Failed to retrieve history event " + uuid + " for tag " + entityId, e);
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
            		"An unexpected error occurred while fetching history event " + uuid + " for tag  " 
    				+ entityId + ". " + "If this continues, please contact your tenant administrator.", e);
        }
    }
}
