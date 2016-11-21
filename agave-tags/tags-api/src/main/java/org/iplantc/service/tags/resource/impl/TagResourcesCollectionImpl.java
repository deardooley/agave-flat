/**
 * 
 */
package org.iplantc.service.tags.resource.impl;

import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;
import org.iplantc.service.common.clients.AgaveLogServiceClient;
import org.iplantc.service.common.representation.AgaveSuccessRepresentation;
import org.iplantc.service.tags.exceptions.TagException;
import org.iplantc.service.tags.exceptions.TagValidationException;
import org.iplantc.service.tags.managers.TagManager;
import org.iplantc.service.tags.model.Tag;
import org.iplantc.service.tags.model.TaggedResource;
import org.iplantc.service.tags.resource.TagResourcesCollection;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.resource.ResourceException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author dooley
 *
 */
@Path("{entityId}/associatedIds")
public class TagResourcesCollectionImpl extends AbstractTagCollection implements TagResourcesCollection {
    
	private static final Logger log = Logger.getLogger(TagResourcesCollectionImpl.class);
	
    public TagResourcesCollectionImpl() {}
    
    /* (non-Javadoc)
     * @see org.iplantc.service.tags.resource.TagResourcesCollection#getTagAssociatedIds(java.lang.String)
     */
    @GET
	@Override
	public Response getTagAssociatedIds(@PathParam("entityId") String entityId) throws Exception {
		
		logUsage(AgaveLogServiceClient.ActivityKeys.TagResourcesList);
        
		try
        {
        	Tag tag = getResourceFromPathValue(entityId);
			
        	ObjectMapper mapper = new ObjectMapper();
    		String json = mapper.writerWithType(new TypeReference<List<TaggedResource>>() {})
					.writeValueAsString(tag.getTaggedResources());
        	
    		return Response.ok(new AgaveSuccessRepresentation(json)).build();
            
        }
        catch (ResourceException e) {
            throw e;
        }
        catch (Throwable e) {
        	log.error("Failed to retrieve tag " + entityId, e);
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
            		"An unexpected error occurred while fetching associatinoIds for tag  " + entityId + ". "
            				+ "If this continues, please contact your tenant administrator.", e);
        }
		
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.tags.resource.TagResourcesCollection#clearTagAssociatedIds(java.lang.String)
	 */
	@DELETE
	@Override
	public Response clearTagAssociatedIds(@PathParam("entityId") String entityId) throws Exception {
		
		logUsage(AgaveLogServiceClient.ActivityKeys.TagResourceDelete);
        
        try
        {
        	Tag tag = getResourceFromPathValue(entityId);
        	ObjectMapper mapper = new ObjectMapper();
        	TagManager manager = new TagManager();
        	manager.updateTagAssociatedUuid(tag, mapper.createArrayNode(), getAuthenticatedUsername());
        	
        	return Response.ok().entity(new AgaveSuccessRepresentation("[]")).build();
        }
        catch (TagException e) {
        	log.error(e);
        	throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
                    "Failed to add tag. If this problem persists, please contact your administrator.");
        }
        catch (TagValidationException e) {
        	throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, e.getMessage());
        }
        catch (Exception e) {
        	log.error("Failed to delete tag " + entityId, e);
        	throw new ResourceException(Status.SERVER_ERROR_INTERNAL, 
        			"An unexpected error occurred while removing associatinoIds for tag  " + entityId + ". "
            				+ "If this continues, please contact your tenant administrator.", e);
        }
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.tags.resource.TagResourcesCollection#addTagAssociatedIds(java.lang.String, org.restlet.representation.Representation)
	 */
	@POST
	@Override
	public Response addTagAssociatedIds(@PathParam("entityId") String entityId, Representation input) {
		
		logUsage(AgaveLogServiceClient.ActivityKeys.TagResourceUpdate);
        
		try
        {
        	Tag tag = getResourceFromPathValue(entityId);
        	JsonNode json = getPostedContentAsJsonNode(input);  	
        	
        	TagManager manager = new TagManager();
        	Tag updatedTag = manager.updateTagAssociatedUuid(tag, json, getAuthenticatedUsername());
        	
        	ObjectMapper mapper = new ObjectMapper();
    		String jsonTaggedResources = mapper.writerWithType(new TypeReference<List<TaggedResource>>() {})
					.writeValueAsString(updatedTag.getTaggedResources());
        	
        	return Response.ok(new AgaveSuccessRepresentation(jsonTaggedResources)).build();
            
        }
        catch (ResourceException e) {
            throw e;
        }
        catch (Throwable e) {
        	log.error("Failed to retrieve tag " + entityId, e);
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
            		"An unexpected error occurred while adding associatinoIds for tag  " + entityId + ". "
            				+ "If this continues, please contact your tenant administrator.", e);
        }
	}
}
