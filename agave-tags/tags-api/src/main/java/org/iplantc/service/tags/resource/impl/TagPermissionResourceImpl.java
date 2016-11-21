/**
 * 
 */
package org.iplantc.service.tags.resource.impl;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.common.clients.AgaveLogServiceClient;
import org.iplantc.service.common.representation.AgaveSuccessRepresentation;
import org.iplantc.service.tags.exceptions.PermissionValidationException;
import org.iplantc.service.tags.exceptions.TagPermissionException;
import org.iplantc.service.tags.managers.TagPermissionManager;
import org.iplantc.service.tags.model.Tag;
import org.iplantc.service.tags.model.TagPermission;
import org.iplantc.service.tags.model.enumerations.PermissionType;
import org.iplantc.service.tags.resource.TagPermissionResource;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.resource.ResourceException;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Handles {@link SoftwarePermission} for a single user on a single
 * {@link Software} object.
 * @author dooley
 *
 */
@Path("{entityId}/pems/{sharedUsername}")
public class TagPermissionResourceImpl extends AbstractTagResource implements TagPermissionResource {
    
	private static final Logger log = Logger.getLogger(TagPermissionResourceImpl.class);
	
    public TagPermissionResourceImpl() {}
    
    /* (non-Javadoc)
     * @see org.iplantc.service.apps.resources.impl.SoftwarePermissionResource#getNotification(java.lang.String, java.lang.String)
     */
    @GET
    @Override
	public Response getEntityPermissionForUser(@PathParam("entityId") String entityId,
                                    		   @PathParam("sharedUsername") String sharedUsername) {
        logUsage(AgaveLogServiceClient.ActivityKeys.TagPermissionGetByUsername);
        
        try
        {
            Tag tag = getResourceFromPathValue(entityId);
            
            TagPermissionManager pm = new TagPermissionManager(tag);
            
            if (pm.canRead(getAuthenticatedUsername())) {
                return Response.ok(new AgaveSuccessRepresentation(pm.getUserPermission(sharedUsername).toJSON())).build();
            }
            else {
                throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
                        "User does not have permission to view permissions on this resource");
            }
        }
        catch (ResourceException e) {
            throw e;
        }
        catch (Throwable e) {
        	log.error("Failed to retrieve user permissions", e);
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
            		"An unexpected error occurred while fetching user permissions for tag  " + entityId + ". "
            				+ "If this continues, please contact your tenant administrator.", e);
        }
    }
	
    /* (non-Javadoc)
     * @see org.iplantc.service.apps.resources.impl.SoftwarePermissionResource#updateUserSoftwarePermission(java.lang.String, java.lang.String, org.restlet.representation.Representation)
     */
    @Override
    @POST
    public Response updateEntityPermissionForUser(@PathParam("entityId") String entityId,
            									  @PathParam("sharedUsername") String sharedUsername,
            									  Representation input) {
        
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
                	
                	if (StringUtils.equals(requestedPermission.getUsername(), sharedUsername)) {
                		throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
                                "Username provided in request body, " + 
                                contentJson.get("username").textValue() + 
                                ", does not match the username in the URL, " + sharedUsername);     
                	}
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
        			"An unexpected error occurred while updating user permissions for tag  " + entityId + ". "
            				+ "If this continues, please contact your tenant administrator.", e);
        }
    }
    
	/* (non-Javadoc)
	 * @see org.iplantc.service.apps.resources.impl.SoftwarePermissionResource#deleteUserSoftwarePermission(java.lang.String, java.lang.String)
	 */
	@DELETE
	@Override
	public Response removeEntityPermissionForUser(@PathParam("entityId") String entityId,
			  									  @PathParam("sharedUsername") String sharedUsername) {
	    logUsage(AgaveLogServiceClient.ActivityKeys.TagPermissionDelete);
        
        try
        {
        	Tag tag = getResourceFromPathValue(entityId);
            
            TagPermissionManager pm = new TagPermissionManager(tag);
            
            if (!pm.canWrite(getAuthenticatedUsername())) {
                throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
                        "User does not have permission to remove permissions for tag " +
                        tag.getUuid() + "");
            }
            else {
                pm.removeAllPermissionForUser(sharedUsername);
            }
            
            return Response.ok(new AgaveSuccessRepresentation()).build();
                
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
                    e.getMessage());
        }
        catch (Exception e) {
        	log.error("Failed to remove user permissions", e);
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
            		"An unexpected error occurred while removing permissions for tag  " + entityId + ". "
            				+ "If this continues, please contact your tenant administrator.", e);
        }
	}
}
