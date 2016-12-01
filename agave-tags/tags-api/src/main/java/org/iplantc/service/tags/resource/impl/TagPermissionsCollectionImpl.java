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
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.log4j.Logger;
import org.iplantc.service.common.clients.AgaveLogServiceClient;
import org.iplantc.service.common.representation.AgaveSuccessRepresentation;
import org.iplantc.service.tags.exceptions.PermissionValidationException;
import org.iplantc.service.tags.exceptions.TagPermissionException;
import org.iplantc.service.tags.managers.TagPermissionManager;
import org.iplantc.service.tags.model.Tag;
import org.iplantc.service.tags.model.TagPermission;
import org.iplantc.service.tags.model.enumerations.PermissionType;
import org.iplantc.service.tags.resource.TagPermissionsCollection;
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
@Path("{entityId}/pems")
public class TagPermissionsCollectionImpl extends AbstractTagCollection implements TagPermissionsCollection {
    
	private static final Logger log = Logger.getLogger(TagPermissionsCollectionImpl.class);
	
    public TagPermissionsCollectionImpl() {}
    
    @Override
    public Response getEntityPermissions(@PathParam("entityId") String entityId) {
        
        logUsage(AgaveLogServiceClient.ActivityKeys.TagPermissionsList);
        
        try
        {
            Tag tag = getResourceFromPathValue(entityId);
            TagPermissionManager pm = new TagPermissionManager(tag);
            
            if (pm.canRead(getAuthenticatedUsername()))
            {
            	// get all permissions
                List<TagPermission> pems = pm.getAllPermissions(getAuthenticatedUsername());
                
                // calculate limit and offset relative to the permission collection
                int offset = Math.min(pems.size()-1, getOffset());
                int limit = Math.min(pems.size()-1-offset, getLimit());
                
                // serialize to json array
                ObjectMapper mapper = new ObjectMapper();
                String json = mapper.writerWithType(new TypeReference<List<TagPermission>>() {})
                					.writeValueAsString(pems.subList(offset, limit));
                
                return Response.ok(new AgaveSuccessRepresentation(json)).build();
            }
            else
            {
                throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
                        "User does not have permission to view permissions on this resource");
            }
        }
        catch (ResourceException e) 
        {
            throw e;
        }
        catch (Throwable e)
        {
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
            		"An unexpected error occurred while fetching permissions for tag  " + entityId + ". "
            				+ "If this continues, please contact your tenant administrator.", e);
        }
    }
	
	@Override
	public Response addEntityPermission(@PathParam("entityId") String entityId, Representation input) 
	{   
		logUsage(AgaveLogServiceClient.ActivityKeys.TagPermissionUpdate);
        
        try
        {
            Tag tag = getResourceFromPathValue(entityId);
            
            TagPermissionManager pm = new TagPermissionManager(tag);
            
            if (!pm.canWrite(getAuthenticatedUsername())) {
                throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
                        "User does not have permission to set permissions for tag " +
                        tag.getUuid() + "");
            }
            else {
                JsonNode contentJson = getPostedContentAsJsonNode(input);
                
                try
                {
                	TagPermission requestedPermission = TagPermission.fromJSON(contentJson, tag.getUuid());
                	
                	TagPermission existingPermission = pm.getUserPermission(requestedPermission.getUsername());
                	
                	ResponseBuilder builder = null;
                    if (existingPermission.getPermission() == PermissionType.NONE) {
                    	builder = Response.status(javax.ws.rs.core.Response.Status.CREATED);
                    } else {
                    	builder = Response.ok();
                    }
                    
                    TagPermission newPermission = pm.setPermission(requestedPermission);
                    
                    return builder.entity(new AgaveSuccessRepresentation(newPermission.toJSON())).build();
                }
                catch (IllegalArgumentException iae) {
                    throw new ResourceException(
                            Status.CLIENT_ERROR_BAD_REQUEST,
                            "Invalid permission value. Valid values are: " + PermissionType.supportedValuesAsString());
                }
                catch (TagPermissionException e) {
                	log.error(e);
                	throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
                            "Failed to update permission. If this problem persists, please contact your administrator.");
                }
                catch (PermissionValidationException e) {
                	throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
                            "Invalid permission value. " +
                            e.getMessage());
                }
                
            }
        }
        catch (ResourceException e) {
            throw e;
        }
        catch (Exception e) {
        	log.error("Failed to updated permission", e);
        	throw new ResourceException(Status.SERVER_ERROR_INTERNAL, 
        			"An unexpected error occurred while updating permissions for tag  " + entityId + ". "
            				+ "If this continues, please contact your tenant administrator.", e);
        }
	}
	
	@Override
    public Response clearAllEntityPermissions(@PathParam("entityId") String entityId) {
	    
		logUsage(AgaveLogServiceClient.ActivityKeys.TagPermissionDelete);
        
        try
        {
            Tag tag = getResourceFromPathValue(entityId);
            
            TagPermissionManager pm = new TagPermissionManager(tag);
            
            if (pm.canWrite(getAuthenticatedUsername()))
            {
                pm.clearPermissions();
                    
                return Response.ok(new AgaveSuccessRepresentation()).build();
            } 
            else 
            {
                throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
                        "User does not have permission to clear permissions");
            }
        }
        catch (ResourceException e) {
            throw e;
        }
        catch (Exception e)
        {
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL, 
            		"An unexpected error occurred while clearing permissions for tag  " + entityId + ". "
            				+ "If this continues, please contact your tenant administrator.", e);
        }
	}
	
}
