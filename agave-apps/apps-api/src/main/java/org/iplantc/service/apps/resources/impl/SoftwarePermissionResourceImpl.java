/**
 * 
 */
package org.iplantc.service.apps.resources.impl;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.commons.lang.StringUtils;
import org.iplantc.service.apps.Settings;
import org.iplantc.service.apps.dao.SoftwarePermissionDao;
import org.iplantc.service.apps.managers.ApplicationManager;
import org.iplantc.service.apps.managers.SoftwarePermissionManager;
import org.iplantc.service.apps.model.Software;
import org.iplantc.service.apps.model.SoftwarePermission;
import org.iplantc.service.apps.resources.SoftwarePermissionResource;
import org.iplantc.service.apps.util.ServiceUtils;
import org.iplantc.service.common.clients.AgaveLogServiceClient;
import org.iplantc.service.common.clients.AgaveProfileServiceClient;
import org.iplantc.service.common.representation.AgaveSuccessRepresentation;
import org.iplantc.service.transfer.model.enumerations.PermissionType;
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
@Path("{softwareId}/pems/{sharedUsername}")
public class SoftwarePermissionResourceImpl extends AbstractSoftwareResource implements SoftwarePermissionResource {
    
    public SoftwarePermissionResourceImpl() {}
    
    /* (non-Javadoc)
     * @see org.iplantc.service.apps.resources.impl.SoftwarePermissionResource#getNotification(java.lang.String, java.lang.String)
     */
    @GET
    @Override
	public Response getUserSoftwarePermission(@PathParam("softwareId") String softwareId,
                                    @PathParam("sharedUsername") String sharedUsername) {
        logUsage(AgaveLogServiceClient.ActivityKeys.AppsListPermissions);
        
        try
        {
            Software software = getSoftwareFromPathValue(softwareId);
        
            if (software == null)
            {
                throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
                        "No shared software found matching " + softwareId);
            }
            else if (software.isPubliclyAvailable()) 
            {
                throw new ResourceException(Status.SERVER_ERROR_NOT_IMPLEMENTED,
                        "Share permissions are not suported on public applications.");
            }
            
            if (new SoftwarePermissionManager(software).canRead(getAuthenticatedUsername()))
            {
                SoftwarePermission pem = SoftwarePermissionDao.getUserSoftwarePermissions(sharedUsername, software.getId());
                try 
                {
                    // check validate username
                    // we back off this for now to prevent people using this as a lookup service
                    AgaveProfileServiceClient authClient = new AgaveProfileServiceClient(
                            Settings.IPLANT_PROFILE_SERVICE, 
                            Settings.IRODS_USERNAME, 
                            Settings.IRODS_PASSWORD);
                    
                    if (authClient.getUser(sharedUsername) == null) {
                        throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
                                "No permissions found for user " + sharedUsername);
                    } else {
                        if (pem == null) {
                            if (ServiceUtils.isAdmin(sharedUsername)) {
                                pem = new SoftwarePermission(software, sharedUsername, PermissionType.ALL);
                            } else if (StringUtils.equals(sharedUsername, software.getOwner())) {
                                pem = new SoftwarePermission(software, sharedUsername, PermissionType.ALL);
                            } else {
                                throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
                                        "No permissions found for user " + sharedUsername);
                            }
                            
                        }
                        return Response.ok(new AgaveSuccessRepresentation(pem.toJSON())).build();
                    }
                } catch (Exception e) {
                    throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
                            "No permissions found for user " + sharedUsername);
                }
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
                    "Failed to retrieve app permissions: " + e.getMessage(), e);
        }
    }
	
    /* (non-Javadoc)
     * @see org.iplantc.service.apps.resources.impl.SoftwarePermissionResource#updateUserSoftwarePermission(java.lang.String, java.lang.String, org.restlet.representation.Representation)
     */
    @Override
    @POST
    public Response updateUserSoftwarePermission(@PathParam("softwareId") String softwareId,
                                    @PathParam("sharedUsername") String sharedUsername,
                                    Representation input) {
        logUsage(AgaveLogServiceClient.ActivityKeys.AppsUpdatePermissions);
        
        try
        {
            Software software = getSoftwareFromPathValue(softwareId);
            
            if (software.isPubliclyAvailable()) 
            {
                throw new ResourceException(Status.SERVER_ERROR_NOT_IMPLEMENTED,
                        "Share permissions are not suported on public applications.");
            }
            
            SoftwarePermissionManager pm = new SoftwarePermissionManager(software);
            
            if (pm.canWrite(getAuthenticatedUsername()))
            {
                JsonNode contentJson = getPostedContentAsJsonNode(input);
                
                try
                {
                    String name = contentJson.has("username") ? contentJson.get("username").textValue() : null;
                    if (StringUtils.isEmpty(name)) {
                        if (StringUtils.isEmpty(sharedUsername)) {
                            throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
                                    "Please provide a valid username to apply the permission");
                        } 
                        else {
                            name = sharedUsername;
                        }
                    } else if (StringUtils.isEmpty(sharedUsername)) {
                        sharedUsername = name;
                    }
                    else if (!StringUtils.equals(name, sharedUsername)) {
                        throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
                            "Username provided in request body, " + 
                            contentJson.get("username").textValue() + 
                            ", does not match the username in the URL, " + sharedUsername);     
                    }
                    
                    if (StringUtils.isEmpty(name) || StringUtils.equals(name, "null")) { 
                        throw new ResourceException(
                            Status.CLIENT_ERROR_BAD_REQUEST, "No user found matching " + name); 
                    } 
//                    else 
//                    {
//                        // validate the user they are giving permissions to exists
//                        AgaveProfileServiceClient authClient = new AgaveProfileServiceClient(Settings.IPLANT_PROFILE_SERVICE, Settings.IRODS_USERNAME, Settings.IRODS_PASSWORD);
//                        
//                        if (authClient.getUser(name) == null) {
//                            throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
//                                "No user found matching " + name);
//                        }
//                    }
    
                    if (contentJson.hasNonNull("permission") && 
                            !ApplicationManager.userCanPublish(getAuthenticatedUsername(), software)) {
                        throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
                                "User does not have permission to publish apps to " +
                                software.getExecutionSystem().getSystemId() + "");
                    }
                    
                    if (!pm.canWrite(getAuthenticatedUsername())) {
                        throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
                                "User does not have permission to set permissions for app " +
                                software.getUniqueName() + "");
                    }
                    
                    String sPermission = null;
                    if (contentJson.has("permission")) 
                    {
                        sPermission = contentJson.get("permission").textValue();
                        
                        // if the permission is null or empty, the permission
                        // will be removed
                        try 
                        {
                            pm.setPermission(name, sPermission);
                            ResponseBuilder builder = null;
                            if (StringUtils.isEmpty(sPermission)) {
                                builder = Response.ok();
                            } else {
                                builder = Response.status(Response.Status.CREATED);
                            }
                            
                            SoftwarePermission pem = SoftwarePermissionDao.getUserSoftwarePermissions(name, software.getId());
                            
                            if (pem == null) {
                                pem = new SoftwarePermission(software, name,
                                        PermissionType.NONE);
                            }
                            
                            return builder.entity(new AgaveSuccessRepresentation(pem.toJSON())).build();
                        } 
                        catch (IllegalArgumentException iae) {
                            throw new ResourceException(
                                    Status.CLIENT_ERROR_BAD_REQUEST,
                                    "Invalid permission value. Valid values are: " + PermissionType.supportedValuesAsString());
                        }
                    } 
                    else {
                        throw new ResourceException(
                                Status.CLIENT_ERROR_BAD_REQUEST,
                                "Missing permission field.");
                    }
                }
                catch (ResourceException e) {
                    throw e;
                }
                catch (Exception e) {
                    throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
                            "User does not have permission to set permissions for app " +
                            software.getUniqueName() + "");
                }
            } else {
                throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
                        "User does not have permission to set permissions for app " +
                        software.getUniqueName() + "");
            }
        }
        catch (ResourceException e) {
            throw e;
        }
        catch (Exception e)
        {
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL, 
                    "Failed to update app permissions: " + e.getMessage(), e);
        }
    }
    
	/* (non-Javadoc)
	 * @see org.iplantc.service.apps.resources.impl.SoftwarePermissionResource#deleteUserSoftwarePermission(java.lang.String, java.lang.String)
	 */
	@DELETE
	@Override
	public Response deleteUserSoftwarePermission(@PathParam("softwareId") String softwareId,
	                                       @PathParam("sharedUsername") String sharedUsername) {
	    logUsage(AgaveLogServiceClient.ActivityKeys.AppsRemovePermissions);
        
        try
        {
            Software software = getSoftwareFromPathValue(softwareId);
            
            if (software.isPubliclyAvailable()) 
            {
                throw new ResourceException(Status.SERVER_ERROR_NOT_IMPLEMENTED,
                        "Share permissions are not suported on public applications.");
            }
            
            SoftwarePermissionManager pm = new SoftwarePermissionManager(software);
            
            if (pm.canWrite(getAuthenticatedUsername()))
            {
                if (!StringUtils.isEmpty(sharedUsername)) 
                {
                    // validate the user they are giving permissions to exists
//                    AgaveProfileServiceClient authClient = new AgaveProfileServiceClient(
//                            Settings.IPLANT_PROFILE_SERVICE, Settings.IRODS_USERNAME, Settings.IRODS_PASSWORD);
//                        
//                    if (authClient.getUser(sharedUsername) == null) {
//                        throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
//                            "No permissions found for user " + sharedUsername);
//                    } else {
                        pm.setPermission(sharedUsername, null);
//                    }
                } 
                else
                {
                    throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
                            "No username provided. Please provide the username for whom permissions should be revoked.");    
                }   
                    
                return Response.ok(new AgaveSuccessRepresentation()).build();
            } 
            else 
            {
                throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
                        "User does not have permission to update this app permissions");
            }
        }
        catch (ResourceException e) {
            throw e;
        }
        catch (Exception e)
        {
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL, 
                    "Failed to remove app permissions: " + e.getMessage(), e);
        }
	}

//  @POST
//@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
//public Response addNotificationFromForm(@PathParam("softwareId") String softwareId,
//                                          @PathParam("sharedUsername") String sharedUsername,
//                                        @FormParam("permission") String permission);
//
//
//@POST
//@Consumes(MediaType.APPLICATION_JSON)
//public Response addNotification(@PathParam("softwareId") String softwareId,
//                                @PathParam("sharedUsername") String sharedUsername,
//                                byte[] bytes);
  


}
