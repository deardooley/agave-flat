/**
 * 
 */
package org.iplantc.service.apps.resources.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.apps.Settings;
import org.iplantc.service.apps.dao.SoftwareDao;
import org.iplantc.service.apps.exceptions.SoftwareException;
import org.iplantc.service.apps.exceptions.SoftwareResourceException;
import org.iplantc.service.apps.managers.ApplicationManager;
import org.iplantc.service.apps.managers.SoftwareEventProcessor;
import org.iplantc.service.apps.model.Software;
import org.iplantc.service.apps.model.enumerations.SoftwareEventType;
import org.iplantc.service.apps.resources.SoftwareCollection;
import org.iplantc.service.apps.search.SoftwareSearchFilter;
import org.iplantc.service.common.clients.AgaveLogServiceClient;
import org.iplantc.service.common.persistence.HibernateUtil;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.common.representation.AgaveSuccessRepresentation;
import org.iplantc.service.common.search.SearchTerm;
import org.iplantc.service.transfer.RemoteDataClient;
import org.joda.time.DateTime;
import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.resource.ResourceException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author dooley
 *
 */
public class SoftwareCollectionImpl extends AbstractSoftwareCollection implements SoftwareCollection {
    
    private static final Logger log = Logger.getLogger(SoftwareCollectionImpl.class);
    
    private SoftwareEventProcessor eventProcessor = new SoftwareEventProcessor();
	
    public SoftwareCollectionImpl() {}

    /* (non-Javadoc)
     * @see org.iplantc.service.apps.resources.impl.SoftwareCollection#getSoftwareCollection(java.lang.String)
     */
    @GET
    public Response getSoftwareCollection() {
        
        logUsage(AgaveLogServiceClient.ActivityKeys.AppsList);
        
        List<Software> apps = null;
        try {
            Map<SearchTerm, Object> queryParameters = getQueryParameters();
            
            apps = SoftwareDao.findMatching(getAuthenticatedUsername(), queryParameters, getOffset(), getLimit(), hasJsonPathFilters());

            return Response.ok(new AgaveSuccessRepresentation(serializeSoftwareCollection(apps).toString())).build();
        }
        catch (ResourceException e) {
            throw e;
        }
        catch (IllegalArgumentException e) {
            throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, e.getMessage(), e);
        } 
        catch (Throwable e) {
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL, 
                    "Failed to query applications catalog.", e);
        }
    }
    
    @GET
    @Path("name/{softwareName}")
    public Response findSoftwareByName(@PathParam("softwareName") String softwareName) {
        logUsage(AgaveLogServiceClient.ActivityKeys.AppsSearchPublicByName);
       
        try 
        {
            Map<String, String> queryParameters = new HashMap<String, String>();
            queryParameters.put("id.like", softwareName);
            SoftwareSearchFilter filter = new SoftwareSearchFilter();
            Map<SearchTerm, Object> searchCriteria = filter.filterCriteria(queryParameters);
            
            List<Software> apps = null;
            apps = SoftwareDao.findMatching(getAuthenticatedUsername(), searchCriteria, getOffset(), getLimit(), hasJsonPathFilters());
            
            return Response.ok(new AgaveSuccessRepresentation(serializeSoftwareCollection(apps).toString())).build();
        }
        catch (ResourceException e) {
            throw e;
        }
        catch (IllegalArgumentException e) {
            throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, e.getMessage(), e);
        } 
        catch (Throwable e) {
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL, 
                    "Failed to query applications catalog.", e);
        }
    }
    
    @GET
    @Path("ontology/{ontology}")
    public Response findSoftwareByOntology(@PathParam("ontology") String ontology) {
        logUsage(AgaveLogServiceClient.ActivityKeys.AppsSearchPublicByTerm);
       
        try {
            Map<String, String> queryParameters = new HashMap<String, String>();
            queryParameters.put("ontology.like", ontology);
            SoftwareSearchFilter filter = new SoftwareSearchFilter();
            Map<SearchTerm, Object> searchCriteria = filter.filterCriteria(queryParameters);
            
            List<Software> apps = null;
            apps = SoftwareDao.findMatching(getAuthenticatedUsername(), searchCriteria, getOffset(), getLimit(), hasJsonPathFilters());
            
            return Response.ok(new AgaveSuccessRepresentation(serializeSoftwareCollection(apps).toString())).build();
        }
        catch (ResourceException e) {
            throw e;
        }
        catch (IllegalArgumentException e) {
            throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, e.getMessage(), e);
        } 
        catch (Throwable e) {
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL, 
                    "Failed to query applications catalog.", e);
        }
    }
    
    @GET
    @Path("tag/{tag}")
    public Response findSoftwareByTag(@PathParam("tag") String tag) {
        logUsage(AgaveLogServiceClient.ActivityKeys.AppsSearchPublicByTag);
       
        try {
            Map<String, String> queryParameters = new HashMap<String, String>();
            queryParameters.put("tag.like", tag);
            SoftwareSearchFilter filter = new SoftwareSearchFilter();
            Map<SearchTerm, Object> searchCriteria = filter.filterCriteria(queryParameters);
            
            List<Software> apps = null;
            apps = SoftwareDao.findMatching(getAuthenticatedUsername(), searchCriteria, getOffset(), getLimit(), hasJsonPathFilters());
            
            return Response.ok(new AgaveSuccessRepresentation(serializeSoftwareCollection(apps).toString())).build();
        }
        catch (ResourceException e) {
            throw e;
        }
        catch (IllegalArgumentException e) {
            throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, e.getMessage(), e);
        } 
        catch (Throwable e) {
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL, 
                    "Failed to query applications catalog.", e);
        }
    }
    
    @GET
    @Path("system/{systemId}")
    public Response findSoftwareBySystemId(@PathParam("systemId") String systemId) {
        logUsage(AgaveLogServiceClient.ActivityKeys.AppsSearchPublicByTag);
       
        try {
            Map<String, String> queryParameters = new HashMap<String, String>();
            queryParameters.put("executionSystem.like", systemId);
            SoftwareSearchFilter filter = new SoftwareSearchFilter();
            Map<SearchTerm, Object> searchCriteria = filter.filterCriteria(queryParameters);
            
            List<Software> apps = null;
            apps = SoftwareDao.findMatching(getAuthenticatedUsername(), searchCriteria, getOffset(), getLimit(), hasJsonPathFilters());
            
            return Response.ok(new AgaveSuccessRepresentation(serializeSoftwareCollection(apps).toString())).build();
        }
        catch (ResourceException e) {
            throw e;
        }
        catch (IllegalArgumentException e) {
            throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, e.getMessage(), e);
        } 
        catch (Throwable e) {
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL, 
                    "Failed to query applications catalog.", e);
        }
    }

    /* (non-Javadoc)
     * @see org.iplantc.service.apps.resources.impl.SoftwareCollection#addSoftware(org.restlet.representation.Representation)
     */
    @POST
    public Response addSoftware(Representation input) {
        logUsage(AgaveLogServiceClient.ActivityKeys.AppsAdd);
        
        if (StringUtils.isEmpty(getAuthenticatedUsername())) {
            throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
                    "Permission denied. You must authenticate to register an application.");
        }
        
        Software existingSoftware = null;
        RemoteDataClient remoteDataClient = null;
        
        try 
        {
            JSONObject json = getPostedContentAsJsonObject(input);
            
            // parse and validate the json
            String newSoftwareName = ApplicationManager.getSoftwareNameFromJSON(json);
            
            existingSoftware = SoftwareDao.getSoftwareByUniqueName(newSoftwareName);
            
            // verify the user has permission on an existing app
            if (existingSoftware != null) 
            {   
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
            }
            
            Software newSoftware = ApplicationManager.processSoftware(existingSoftware, json, getAuthenticatedUsername());
            
            if (existingSoftware == null) 
            {
                SoftwareDao.persist(newSoftware);
                
                eventProcessor.processSoftwareContentEvent(newSoftware, 
                        SoftwareEventType.CREATED, 
                        "App was registered by " + getAuthenticatedUsername(), 
                        getAuthenticatedUsername());
            }
            else 
            {
                // Delete the old software record and add the new one.
                SoftwareDao.replace(existingSoftware, newSoftware);
                
                eventProcessor.processSoftwareContentEvent(existingSoftware, 
                        SoftwareEventType.UPDATED, 
                        "App was updated by " + getAuthenticatedUsername(), 
                        getAuthenticatedUsername());
            }
            
            return Response.ok(new AgaveSuccessRepresentation(newSoftware.toJSON())).build();   
        } 
        catch (JSONException e) {
            log.error("Error parsing json for app." , e);
            throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
                    "Invalid json description: " + e.getMessage(), e);
        } 
        catch (SoftwareException | SoftwareResourceException e) {
            log.error(e);
            throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, e.getMessage(), e);
        } 
        catch (ResourceException e) {
            log.error("Error registering app." , e); 
            throw e;
        } 
        catch (Throwable e) {
            log.error("Error registering app." , e); 
            throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
                    "Failed to update application: " + e.getMessage(), e);
        } 
        finally {
            try { remoteDataClient.disconnect(); } catch (Exception e) {}
            try { HibernateUtil.closeSession(); } catch (Exception e) {}
        }   
    }

    /**
     * Serializes the given {@code apps} into a minimal JSON array of 
     * summary software objects.
     * @param apps
     * @return
     * @throws ResourceException
     */
    public ArrayNode serializeSoftwareCollection(List<Software> apps)
    throws ResourceException
    {
        log.debug("App manager found " + apps.size() + " applications.");

        ObjectMapper mapper = new ObjectMapper();
        ArrayNode jsonApps = mapper.createArrayNode();
        try
        { 	
        	// if we have full path filters, we should include the entire Sofware
        	// object so all the fields will be present for filtering.
        	if (hasJsonPathFilters()) {
        		for (Software app: apps) {
        			jsonApps.add(mapper.readTree(app.toJSON()));
        		}
        	} 
        	// return summary response
        	else {
	            for (Software app: apps) 
	            {   
	                ObjectNode node = mapper.createObjectNode()
	                    .put("id", app.getUniqueName())
	                    .put("name", app.getName())
	                    .put("version", app.getVersion())
	                    .put("revision", app.getRevisionCount())
	                    .put("executionSystem", app.getExecutionSystem().getSystemId())
	                    .put("shortDescription", app.getShortDescription())
	                    .put("isPublic", app.isPubliclyAvailable())
	                    .put("label", app.getLabel())
	                    .put("lastModified", new DateTime(app.getLastUpdated()).toString());
	                    node.putObject("_links")
	                        .putObject("self")
	                            .put("href", TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_APPS_SERVICE) + app.getUniqueName());
	                jsonApps.add(node);
	            }
        	}
            
            return jsonApps;
        }
        catch (Exception e)
        {
            log.error("Failed to list resources", e);
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL, e.getMessage(), e);
        }
    }
}
