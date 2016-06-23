/**
 * 
 */
package org.iplantc.service.apps.resources.impl;

import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.commons.lang.StringUtils;
import org.iplantc.service.apps.dao.SoftwarePermissionDao;
import org.iplantc.service.apps.managers.ApplicationManager;
import org.iplantc.service.apps.managers.SoftwarePermissionManager;
import org.iplantc.service.apps.model.Software;
import org.iplantc.service.apps.model.SoftwarePermission;
import org.iplantc.service.apps.resources.SoftwarePermissionCollection;
import org.iplantc.service.apps.util.ServiceUtils;
import org.iplantc.service.common.clients.AgaveLogServiceClient;
import org.iplantc.service.common.representation.AgaveSuccessRepresentation;
import org.iplantc.service.transfer.model.enumerations.PermissionType;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.resource.ResourceException;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * @author dooley
 *
 */
@Path("{softwareId}/pems")
public class SoftwarePermissionCollectionImpl extends AbstractSoftwareCollection implements SoftwarePermissionCollection {
    
    public SoftwarePermissionCollectionImpl() {}
    
    @GET
	public Response getSoftwarePermissions(@PathParam("softwareId") String softwareId) {
        
        logUsage(AgaveLogServiceClient.ActivityKeys.AppsListPermissions);
        
        try
        {
            Software software = getSoftwareFromPathValue(softwareId);
        
            if (software.isPubliclyAvailable()) 
            {
                throw new ResourceException(Status.SERVER_ERROR_NOT_IMPLEMENTED,
                        "Share permissions are not suported on public applications.");
            }
            
            if (new SoftwarePermissionManager(software).canRead(getAuthenticatedUsername()))
            {
                String jsonPermissions = 
                        new SoftwarePermission(software, software.getOwner(), PermissionType.ALL).toJSON();
                    
                List<SoftwarePermission> pems = SoftwarePermissionDao.getSoftwarePermissions(software.getId());
                if (ServiceUtils.isAdmin(getAuthenticatedUsername())) {
                    boolean foundCurrentUser = false;
                    for (SoftwarePermission pem: pems) {
                        if (StringUtils.equals(getAuthenticatedUsername(), pem.getUsername())) {
                            pem.setPermission(PermissionType.ALL);
                            foundCurrentUser = true;
                        }
                    }
                    if (!foundCurrentUser) {
                        pems.add(new SoftwarePermission(software, getAuthenticatedUsername(), PermissionType.ALL));
                    }
                }
                
                for (SoftwarePermission pem: pems)
                {
                    jsonPermissions += "," + pem.toJSON();
                }
                
                jsonPermissions = "[" + jsonPermissions + "]";
                
                return Response.ok(new AgaveSuccessRepresentation(jsonPermissions)).build();
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
	
	@POST
	public Response addSoftwarePermission(@PathParam("softwareId") String softwareId,
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
                    String sharedUsername = null;
                    if (!contentJson.hasNonNull("username")) {
                        throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
                                "Missing username. Please provide a username for whom the permission will be granted.");         
                    } else {
                        sharedUsername = contentJson.get("username").textValue();
                    }
                    
//                    // validate the user they are giving permissions to exists
//                    AgaveProfileServiceClient authClient = new AgaveProfileServiceClient(Settings.IPLANT_PROFILE_SERVICE, Settings.IRODS_USERNAME, Settings.IRODS_PASSWORD);
//                    
//                    if (authClient.getUser(sharedUsername) == null) {
//                        throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
//                            "No user found matching " + sharedUsername);
//                    }
    
                    if (!ApplicationManager.userCanPublish(getAuthenticatedUsername(), software)) {
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
                    if (contentJson.hasNonNull("permission")) 
                    {
                        sPermission = contentJson.get("permission").textValue();
                        
                        // if the permission is null or empty, the permission
                        // will be removed
                        try 
                        {
                            pm.setPermission(sharedUsername, sPermission);
                            ResponseBuilder builder = null;
                            if (StringUtils.isEmpty(sPermission)) {
                                builder = Response.ok();
                            } else {
                                builder = Response.status(javax.ws.rs.core.Response.Status.CREATED);
                            }
                            
                            SoftwarePermission pem = null;
                            // Removing 304 response as the spec only allows for 304 on GET requests.

                            pem = SoftwarePermissionDao.getUserSoftwarePermissions(sharedUsername, software.getId());
                            
                            if (pem == null) {
                                pem = new SoftwarePermission(software, sharedUsername,
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
	
	@DELETE
    public Response clearAllSoftwarePermissions(@PathParam("softwareId") String softwareId) {
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
                pm.clearAllPermissions(software, getAuthenticatedUsername());
                    
                return Response.ok(new AgaveSuccessRepresentation()).build();
            } 
            else 
            {
                throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
                        "User does not have permission to clear this app permissions");
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
	
	/**
     * Fetches the {@link Software} object for the id in the URL or throws 
     * an exception that can be re-thrown from the route method.
     * @param softwareId
     * @return Software object referenced in the path
     * @throws ResourceException
     */
	@Override
    protected Software getSoftwareFromPathValue(String softwareId)
    throws ResourceException
    {
        Software existingSoftware = super.getSoftwareFromPathValue(softwareId);
        
        // public apps cannot have permissions udpated
        if (existingSoftware.isPubliclyAvailable()) 
        {
            throw new ResourceException(Status.SERVER_ERROR_NOT_IMPLEMENTED,
                    "App permissions are not suported on public apps.");
        }
        
        return existingSoftware;
    }

}
