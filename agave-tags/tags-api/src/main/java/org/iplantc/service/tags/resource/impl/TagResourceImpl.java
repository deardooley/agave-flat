/**
 * 
 */
package org.iplantc.service.tags.resource.impl;

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
import org.iplantc.service.tags.resource.TagResource;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Put;
import org.restlet.resource.ResourceException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author dooley
 *
 */
@Path("{entityId}")
public class TagResourceImpl extends AbstractTagResource implements TagResource {

	private static final Logger log = Logger.getLogger(TagResourceImpl.class);
	
	public TagResourceImpl() {}

	/* (non-Javadoc)
	 * @see org.iplantc.service.tags.resource.TagsCollection#getTags()
	 */
	@Get
	@Override
	public Response represent(@PathParam("entityId") String entityId) throws Exception {
		
		logUsage(AgaveLogServiceClient.ActivityKeys.TagsGetByID);
        
		try
        {
        	Tag tag = getResourceFromPathValue(entityId);
			
        	ObjectMapper mapper = new ObjectMapper();
    		String json = mapper.writerWithType(new TypeReference<Tag>() {})
					.writeValueAsString(tag);
        	
    		return Response.ok(new AgaveSuccessRepresentation(json.toString())).build();
            
        }
        catch (ResourceException e) {
            throw e;
        }
        catch (Throwable e) {
        	log.error("Failed to retrieve tag " + entityId, e);
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
            		"An unexpected error occurred while fetching tag  " + entityId + ". "
                			+ "If this continues, please contact your tenant administrator.", e);
        }
		
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.tags.resource.TagsCollection#addTagFromForm(java.lang.String, java.util.List)
	 */
	@Delete
	@Override
	public Response remove(@PathParam("entityId") String entityId) throws Exception {
		
		logUsage(AgaveLogServiceClient.ActivityKeys.TagsDelete);
        
        try
        {
        	Tag tag = getResourceFromPathValue(entityId);  	
        	TagManager manager = new TagManager();
        	manager.deleteUserTag(tag, getAuthenticatedUsername());
        	
        	return Response.ok().entity(new AgaveSuccessRepresentation(tag.toJSON())).build();
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
        			"An unexpected error occurred while deleting tag  " + entityId + ". "
                			+ "If this continues, please contact your tenant administrator.", e);
        }
	}

	@Put
	@Override
	public Response store(@PathParam("entityId") String entityId, Representation input) throws Exception {
		
		logUsage(AgaveLogServiceClient.ActivityKeys.TagsUpdate);
        
		try
        {
        	Tag tag = getResourceFromPathValue(entityId);
        	JsonNode json = getPostedContentAsJsonNode(input);  	
        	TagManager manager = new TagManager();
        	Tag updatedTag = manager.updateTagAssociatedUuid(tag, json, getAuthenticatedUsername());
        	
        	return Response.ok(new AgaveSuccessRepresentation(updatedTag.toJSON())).build();
            
        }
        catch (ResourceException e) {
            throw e;
        }
        catch (Throwable e) {
        	log.error("Failed to update tag " + entityId, e);
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
            		"An unexpected error occurred while updating tag  " + entityId + ". "
                			+ "If this continues, please contact your tenant administrator.", e);
        }
	}
}
