package org.iplantc.service.apps.resources.impl;

import static org.iplantc.service.apps.model.enumerations.SoftwareActionType.CLONE;
import static org.iplantc.service.apps.model.enumerations.SoftwareActionType.DISABLE;
import static org.iplantc.service.apps.model.enumerations.SoftwareActionType.ENABLE;
import static org.iplantc.service.apps.model.enumerations.SoftwareActionType.ERASE;
import static org.iplantc.service.apps.model.enumerations.SoftwareActionType.PUBLISH;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.StatusType;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.apps.dao.SoftwareDao;
import org.iplantc.service.apps.exceptions.SoftwareException;
import org.iplantc.service.apps.exceptions.SoftwareResourceException;
import org.iplantc.service.apps.managers.ApplicationManager;
import org.iplantc.service.apps.managers.SoftwareEventProcessor;
import org.iplantc.service.apps.model.Software;
import org.iplantc.service.apps.model.enumerations.SoftwareActionType;
import org.iplantc.service.apps.model.enumerations.SoftwareEventType;
import org.iplantc.service.apps.queue.actions.CloneAction;
import org.iplantc.service.apps.queue.actions.PublishAction;
import org.iplantc.service.apps.resources.SoftwareResource;
import org.iplantc.service.apps.util.ServiceUtils;
import org.iplantc.service.common.clients.AgaveLogServiceClient;
import org.iplantc.service.common.exceptions.DependencyException;
import org.iplantc.service.common.exceptions.DomainException;
import org.iplantc.service.common.exceptions.PermissionException;
import org.iplantc.service.common.representation.AgaveSuccessRepresentation;
import org.iplantc.service.systems.exceptions.SystemUnavailableException;
import org.iplantc.service.systems.exceptions.SystemUnknownException;
import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.Request;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.resource.ResourceException;

import com.fasterxml.jackson.databind.JsonNode;

@Path("/{softwareId}")
@Produces("application/json")
public class SoftwareResourceImpl extends AbstractSoftwareResource implements SoftwareResource {
    
    private static final Logger log = Logger.getLogger(SoftwareResourceImpl.class);
    
    private SoftwareEventProcessor eventProcessor = new SoftwareEventProcessor();
	
    /* (non-Javadoc)
     * @see org.iplantc.service.apps.resources.SoftwareResource#getSoftware(java.lang.String)
     */
    @GET
    @Override
    public Response getSoftware(@PathParam("softwareId") String softwareId) 
    {
    	logUsage(AgaveLogServiceClient.ActivityKeys.AppsGetByID);
    	
        try 
        {
            Software software = getSoftwareFromPathValue(softwareId);
            
            if (ApplicationManager.isVisibleByUser(software, getAuthenticatedUsername()))
            {
                return Response.ok(new AgaveSuccessRepresentation(software.toJSON())).build();
            } 
            else if (software.isPubliclyAvailable() && !software.isAvailable()) 
            {
                throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
                    "This application has been removed by the administrator.");
            }
            else 
            {
                throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
                        "Permission denied. You do not have permission to view this application");
            }
        }
        catch (SoftwareException e)
        {
            throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Failed to locate software entry: " + e.getMessage(), e);
        }
        catch (ResourceException e) {
            throw e;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL, "Failed to retrieve application information", e);
        }
    }
    
    /* (non-Javadoc)
     * @see org.iplantc.service.apps.resources.SoftwareResource#update(java.lang.String, org.restlet.representation.Representation)
     */
    @POST
    @Override
    @Consumes({MediaType.MULTIPART_FORM_DATA, MediaType.APPLICATION_JSON, MediaType.APPLICATION_FORM_URLENCODED})
    public Response update(@PathParam("softwareId") String softwareId,
                                    Representation input) 
    {
    	logUsage(AgaveLogServiceClient.ActivityKeys.AppsAdd);
        
        Software existingSoftware = null;
        try 
        {
            existingSoftware = getSoftwareFromPathValue(softwareId);
            
            if (ApplicationManager.isManageableByUser(existingSoftware, getAuthenticatedUsername()))
            {
                if (existingSoftware.isPubliclyAvailable()) {
                    throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
                            "Permission denied. Public applications cannot be updated.");
                }
            } 
            else 
            {
                throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
                        "Permission denied. An application with this unique id already " +
                        "exists and you do not have permission to update this application. " +
                        "Please either change your application name or update the version " +
                        "number past " + SoftwareDao.getMaxVersionForSoftwareName(existingSoftware.getName()));
            }
            
            JSONObject json = super.getPostedContentAsJsonObject(input);
            
            Software newSoftware = new ApplicationManager().processSoftware(existingSoftware, json, getAuthenticatedUsername());
            
            SoftwareDao.replace(existingSoftware, newSoftware);
            
            // maintain the uuid of the previous app for history tracking
//            newSoftware.setUuid(existingSoftware.getUuid());
            
//            SoftwareDao.persist(newSoftware);
            
            eventProcessor.processSoftwareContentEvent(existingSoftware, 
                                        SoftwareEventType.UPDATED, 
                                        "App was updated by " + getAuthenticatedUsername(), 
                                        getAuthenticatedUsername());
            
            return Response.status(javax.ws.rs.core.Response.Status.CREATED)
                    .entity(new AgaveSuccessRepresentation(newSoftware.toJSON()))
                    .build();
            
        } 
        catch (JSONException e) {
            log.error(e);
            throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Invalid json description: " + e.getMessage(), e);
        } 
        catch (SoftwareResourceException e) { 
            log.error(e);
            throw new ResourceException(Status.valueOf(e.getStatus()), e.getMessage(), e);
        }
        catch (SoftwareException e) {
            log.error(e);
            throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, e.getMessage(), e);
        } 
        catch (ResourceException e) {
            log.error(e);
            throw e;
        } 
        catch (Throwable e) {
            log.error(e);
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
                    "Failed to update application: " + e.getMessage(), e);
        } 
    }
    
//    /**
//     * Fetches the {@link Software} object for the id in the URL or throws 
//     * an exception that can be re-thrown from the route method.
//     * @param softwareId
//     * @return Software object referenced in the path
//     * @throws ResourceException
//     */
//    protected Software getSoftwareFromPathValue(String softwareId)
//    throws ResourceException
//    {
//    	String decodedSoftwareId = URLDecoder.decode(softwareId);
//        
//    	Software existingSoftware = SoftwareDao.getSoftwareByUniqueName(decodedSoftwareId);
//        
//        // update if the existing software belongs to the user, otherwise throw an exception
//        if (existingSoftware == null) {
//            throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
//                    "No software found matching " + softwareId);
//        } 
//        
//        return existingSoftware;
//    }

    /* (non-Javadoc)
     * @see org.iplantc.service.apps.resources.SoftwareResource#manage(java.lang.String)
     */
    @PUT
    @Override
    public Response manage(@PathParam("softwareId") String softwareId) 
    {
        Software software = null;
        try 
        {
            software = getSoftwareFromPathValue(softwareId);
            
            JsonNode postContentData = getPostedContentAsJsonNode(Request.getCurrent().getEntity());
            
            SoftwareActionType action = null;
            String sAction = null;
            try {
            	if (postContentData.hasNonNull("action")) {
            		sAction = postContentData.get("action").textValue();
            	}
            	else {
            		throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
            				"Please provide a valid action to perform on the app. " +
            				"Valid actions are: " + StringUtils.join(SoftwareActionType.values(),","), 
            				new IllegalArgumentException()); 
            	}
            	
            	action = SoftwareActionType.valueOf(StringUtils.upperCase(sAction));
            }
            catch (IllegalArgumentException e) {
                throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
                        "Invalid action, " + sAction + ", on software entry. " +
                        "Valid actions are: " + StringUtils.join(SoftwareActionType.values(),","), e); 
            }
                    
            if (action == PUBLISH) 
            {
            	logUsage(AgaveLogServiceClient.ActivityKeys.AppsPublish);
                
                String publicName = postContentData.has("name") ? postContentData.get("name").textValue() : null;
                String publicVersion = postContentData.has("version") ? postContentData.get("version").textValue() : null;
                String publicExecutionSystemId = postContentData.has("executionSystem") ? postContentData.get("executionSystem").textValue() : null;
                
                PublishAction publishAction = new PublishAction(software, 
                                                                getAuthenticatedUsername(),
                                                                publicName,
                                                                publicVersion,
                                                                publicExecutionSystemId);
                                                                
                publishAction.run();
                return Response.status(202).entity(new AgaveSuccessRepresentation(publishAction.getPublishedSoftware().toJSON())).build();
            } 
            else if (action == CLONE) 
            {
            	logUsage(AgaveLogServiceClient.ActivityKeys.AppsClone);
                
                String clonedName = postContentData.has("name") ? postContentData.get("name").textValue() : null;
                String clonedVersion = postContentData.has("version") ? postContentData.get("version").textValue() : null;
                String clonedExecutionSystemId = postContentData.has("executionSystem") ? postContentData.get("executionSystem").textValue() : null;
                String clonedDeploymentSystemId = postContentData.has("deploymentSystem") ? postContentData.get("deploymentSystem").textValue() : null;
                String clonedDeploymentPath = postContentData.has("deploymentPath") ? postContentData.get("deploymentPath").textValue() : null;
                
                CloneAction cloneAction = new CloneAction(software,
                                                          getAuthenticatedUsername(),
                                                          clonedName,
                                                          clonedVersion,
                                                          clonedExecutionSystemId,
                                                          clonedDeploymentSystemId,
                                                          clonedDeploymentPath);
                cloneAction.run();
                
                return Response.status(202).entity(new AgaveSuccessRepresentation(cloneAction.getClonedSoftware().toJSON())).build();
            } 
            else if (action == ERASE) 
            {
            	logUsage(AgaveLogServiceClient.ActivityKeys.AppsErase);
                
                if (ServiceUtils.isAdmin(getAuthenticatedUsername())) 
                {
                    ApplicationManager.eraseSoftware(software, getAuthenticatedUsername());
                    
                    return Response.ok(new AgaveSuccessRepresentation()).build();
                }
                else 
                {
                    throw new PermissionException("Permission denied. Only tenant administrators may erase apps.");
                }
            }
            else if (action == ENABLE) 
            {
            	logUsage(AgaveLogServiceClient.ActivityKeys.AppsEnable);
                
                if (ApplicationManager.isManageableByUser(software, getAuthenticatedUsername())) 
                {
                    software = ApplicationManager.enableSoftware(software, getAuthenticatedUsername());
                    
                    return Response.ok(new AgaveSuccessRepresentation(software.toJSON())).build();
                }
                else 
                {
                    throw new PermissionException("Permission denied. You do not have permission to enable this app.");
                }
            }
            else if (action == DISABLE) 
            {
            	logUsage(AgaveLogServiceClient.ActivityKeys.AppsDisable);
                
                if (ApplicationManager.isManageableByUser(software, getAuthenticatedUsername())) 
                {
                    software = ApplicationManager.disableSoftware(software, getAuthenticatedUsername());
                    
                    return Response.ok(new AgaveSuccessRepresentation(software.toJSON())).build();
                }
                else 
                {
                    throw new PermissionException("Permission denied. You do not have permission to disable this app.");
                }
            }
//            else if (action == CANCEL) 
//            {
//            	logUsage(AgaveLogServiceClient.ActivityKeys.AppsCancel);
//                
//                if (ApplicationManager.isManageableByUser(software, getAuthenticatedUsername())) 
//                {
//                    software = ApplicationManager.interruptOperation(software, getAuthenticatedUsername());
//                    
//                    return Response.ok(new AgaveSuccessRepresentation(software.toJSON())).build();
//                }
//                else 
//                {
//                    throw new PermissionException("Permission denied. You do not have permission to cancel operations on this app.");
//                }
//            }
            else 
            {
                throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
                        "Invalid action, " + action + ", on software entry. " +
                        "Valid actions are: publish, clone, erase, disable, and enable");
            }
            
        } catch (SystemUnavailableException e) {
            log.error(e);
            throw new ResourceException(Status.SERVER_ERROR_SERVICE_UNAVAILABLE, e.getMessage(), e);
        } catch (DomainException | SystemUnknownException | DependencyException e) {
            log.error(e);
            throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, e.getMessage(), e);
        } catch (PermissionException e) {
            log.error(e);
            throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
                    "Permission denied. You do not have permission to perform this action.", e);
        } catch (SoftwareException e) {
            log.error(e);
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL, e.getMessage(), e);
        } catch (ResourceException e) {
            log.error(e);
            throw e;
        } catch (Throwable e) {
            log.error(e);
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL, e.getMessage(), e);
        }
    }
    
    /* (non-Javadoc)
     * @see org.iplantc.service.apps.resources.SoftwareResource#remove(java.lang.String)
     */
    @DELETE
    @Override
    public Response remove(@PathParam("softwareId") String softwareId) 
    {
        AgaveLogServiceClient.log(AgaveLogServiceClient.ServiceKeys.APPS02.name(), 
                AgaveLogServiceClient.ActivityKeys.AppsDelete.name(), 
                getAuthenticatedUsername(), "", Request.getCurrent().getClientInfo().getUpstreamAddress());
        
        Software software = null;
        try 
        {
            software = getSoftwareFromPathValue(softwareId);
            
            
            if (ApplicationManager.isManageableByUser(software, getAuthenticatedUsername())) {
                if (software.isPubliclyAvailable()) {
                	ApplicationManager.deleteApplication(software, getAuthenticatedUsername());
//                    software.setAvailable(false);
//                    SoftwareDao.persist(software);
                } else {
                    ApplicationManager.eraseSoftware(software, getAuthenticatedUsername());
//                      ApplicationManager.deleteApplication(software);
                }
                
//                eventProcessor.processSoftwareContentEvent(software, 
//                        SoftwareEventType.DELETED, 
//                        "App was deleted by " + getAuthenticatedUsername(), 
//                        getAuthenticatedUsername());
                
                return Response.ok(new AgaveSuccessRepresentation()).build();
            } else {
                throw new ResourceException(Status.CLIENT_ERROR_UNAUTHORIZED,
                        "User does not have permission to delete this app",
                        new PermissionException("User does not have permission to delete this app"));
            }
        } catch (ResourceException e) {
            log.error(e);
            throw e;
        } catch (Throwable e) {
            log.error(e);
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL, "Failed to delete app", e);
        }
    }    
}