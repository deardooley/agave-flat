/**
 * 
 */
package org.iplantc.service.notification.resources.impl;

import static org.iplantc.service.common.clients.AgaveLogServiceClient.ActivityKeys.NotifAdd;
import static org.iplantc.service.common.clients.AgaveLogServiceClient.ActivityKeys.NotifList;
import static org.iplantc.service.common.clients.AgaveLogServiceClient.ActivityKeys.NotifSearch;
import static org.iplantc.service.common.clients.AgaveLogServiceClient.ServiceKeys.NOTIFICATIONS02;

import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.hibernate.HibernateException;
import org.iplantc.service.common.clients.AgaveLogServiceClient;
import org.iplantc.service.common.exceptions.UUIDException;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.common.representation.AgaveSuccessRepresentation;
import org.iplantc.service.common.search.SearchTerm;
import org.iplantc.service.common.uuid.AgaveUUID;
import org.iplantc.service.notification.Settings;
import org.iplantc.service.notification.dao.NotificationDao;
import org.iplantc.service.notification.exceptions.NotificationException;
import org.iplantc.service.notification.managers.NotificationPermissionManager;
import org.iplantc.service.notification.model.Notification;
import org.iplantc.service.notification.model.NotificationPolicy;
import org.iplantc.service.notification.resources.NotificationCollection;
import org.iplantc.service.notification.util.ServiceUtils;
import org.restlet.Request;
import org.restlet.data.Status;
import org.restlet.ext.fileupload.RestletFileUpload;
import org.restlet.representation.Representation;
import org.restlet.resource.ResourceException;
import org.testng.log4testng.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author dooley
 *
 */
@Produces(MediaType.APPLICATION_JSON)
@Path("")
public class NotificationCollectionImpl extends AbstractNotificationCollection implements NotificationCollection
{
	private static final Logger log = Logger.getLogger(NotificationCollectionImpl.class);
	protected NotificationDao dao = new NotificationDao();
	protected NotificationPermissionManager pm = null;
	
	/* (non-Javadoc)
	 * @see org.iplantc.service.notification.resources.NotificationResource#getNotifications()
	 */
	@Override
	@GET
	public Response getNotifications(@PathParam("associatedUuid") String associatedUuid)
	{	
		try
		{	
			Map<SearchTerm, Object>  searchCriteria = getQueryParameters();
			
			List<Notification> notifications = null;
			if (searchCriteria.isEmpty()) {
				AgaveLogServiceClient.log(NOTIFICATIONS02.name(), 
						NotifList.name(), 
						getAuthenticatedUsername(), "", 
						Request.getCurrent().getClientInfo().getUpstreamAddress());
				notifications = dao.getActiveUserNotifications(getAuthenticatedUsername(), getOffset(), getLimit());
			} else {
				AgaveLogServiceClient.log(NOTIFICATIONS02.name(), 
						NotifSearch.name(), 
						getAuthenticatedUsername(), "", 
						Request.getCurrent().getClientInfo().getUpstreamAddress());
				notifications = dao.findMatching(getAuthenticatedUsername(), searchCriteria, getOffset(), getLimit());
			}
			
			ObjectMapper mapper = new ObjectMapper();
	        ArrayNode jsonApps = mapper.createArrayNode();
	        
            for (Notification notification: notifications) 
            {   
                
                ObjectNode node = mapper.createObjectNode()
                    .put("id", notification.getUuid())
                    .put("url", notification.getCallbackUrl())
                    .put("associatedUuid", notification.getAssociatedUuid())
                    .put("event", notification.getEvent());
                
                ObjectNode links = node.putObject("_links");
                links.putObject("self")
                     .put("href", TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_NOTIFICATION_SERVICE) + notification.getUuid());
                links.putObject("profile")
                	.put("href", TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_PROFILE_SERVICE) + notification.getOwner());
                
                try 
                {
                	if (!StringUtils.contains("*", notification.getAssociatedUuid()) 
                			&& !StringUtils.isEmpty(notification.getAssociatedUuid())) {
                        AgaveUUID agaveUUID = new AgaveUUID(notification.getAssociatedUuid());
                        links.putObject(agaveUUID.getResourceType().name().toLowerCase())
                             .put("href", TenancyHelper.resolveURLToCurrentTenant(agaveUUID.getObjectReference()));
                    }
                } 
                catch (UUIDException e) {}
                
                jsonApps.add(node);
                
            }
            
            return Response.ok(new AgaveSuccessRepresentation(jsonApps.toString())).build();
		}
		catch (NotificationException e)
		{
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
					"Failed to retrieve notifications.", e);
		}
		catch (ResourceException e) {
			throw e;
		} 
		
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.notification.resources.NotificationResource#addNotificationFromForm(org.iplantc.service.notification.model.Notification)
	 */
	@Override
	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public Response addNotificationFromForm(
			@FormParam("url") String callbackUrl,
			@FormParam("event") String notificationEvent,
			@FormParam("associatedUuid") String associatedUuid,
			@FormParam("persistent") Boolean persistent,
			@FormParam("status") String status,
			@FormParam("policy.retryStrategyType") String retryStrategyType,
			@FormParam("policy.retryLimit") Integer retryLimit,
			@FormParam("policy.retryRate") Integer retryRate,
			@FormParam("policy.retryDelay") Integer retryDelay,
			@FormParam("policy.saveOnFailure") Boolean saveOnFailure)
	{
		AgaveLogServiceClient.log(NOTIFICATIONS02.name(), 
				NotifAdd.name(), 
				getAuthenticatedUsername(), "", 
				Request.getCurrent().getClientInfo().getUpstreamAddress());
		
		try
		{		
			ObjectMapper mapper = new ObjectMapper();
    		ObjectNode jsonNotification = new ObjectMapper().createObjectNode();
    		jsonNotification.put("url", callbackUrl);
    		jsonNotification.put("event", notificationEvent);
    		jsonNotification.put("associatedUuid", associatedUuid);
    		jsonNotification.put("persistent", BooleanUtils.toBoolean(persistent));
    		jsonNotification.put("status", status);
    		
    		NotificationPolicy policy = new NotificationPolicy();
    		jsonNotification.put("policy", mapper.createObjectNode()
    				.put("retryStrategy", StringUtils.isEmpty(retryStrategyType) ? policy.getRetryStrategyType().name() : retryStrategyType)
    				.put("retryLimit", retryLimit == null ? policy.getRetryLimit() : retryLimit)
    				.put("retryRate", retryRate == null ? policy.getRetryRate() : retryRate)
    				.put("retryDelay", retryDelay == null ? policy.getRetryDelay() : retryDelay)
    				.put("saveOnFailure", BooleanUtils.toBoolean(saveOnFailure)));
    		
    		Notification notification = Notification.fromJSON(jsonNotification);
    		notification.setOwner(getAuthenticatedUsername());
			

			if (StringUtils.contains(notification.getAssociatedUuid(), "*") && 
	    			!ServiceUtils.isAdmin(getAuthenticatedUsername())) 
	    	{
	    		throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
						"Administrator permissions are required to set wildcard notifications.");
	    	} 
	    	else 
	    	{	
				dao.persist(notification);
				
				return Response.status(javax.ws.rs.core.Response.Status.CREATED)
						.entity(new AgaveSuccessRepresentation(notification.toJSON()))
						.build();
	    	} 
		}
		catch (NotificationException e) {
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
					e.getLocalizedMessage(), e);
		}
		catch (IllegalArgumentException e) {
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
					"Unable to save notification: " + e.getMessage(), e);
	    } 
		catch (HibernateException e) {
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
					"Unable to save notification: " + e.getMessage(), e);
	    }
		catch (ResourceException e) {
			throw e;
		} 
		catch (Exception e) {
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
					"Failed to save notification: " + e.getMessage(), e);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.iplantc.service.notification.resources.NotificationResource#addNotification(Representation)
	 */
	@Override
	@POST
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response addNotification(Representation input)
	{
		AgaveLogServiceClient.log(NOTIFICATIONS02.name(), 
				NotifAdd.name(), 
				getAuthenticatedUsername(), "", 
				Request.getCurrent().getClientInfo().getUpstreamAddress());
		
		try {
			if (input == null) {
				throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
						"No notification object provided.");
			} 
			else 
			{
				
				RestletFileUpload fileUpload = new RestletFileUpload(new DiskFileItemFactory());

		        // this list is always empty !!
		        List<FileItem> fileItems = fileUpload.parseRepresentation(input);
		        JsonNode jsonNotification = null;
		        for (FileItem fileItem : fileItems) {
		            if (!fileItem.isFormField()) {
		            	ObjectMapper mapper = new ObjectMapper();
		        		jsonNotification = mapper.readTree(fileItem.getInputStream());
		            	
		            	break;
		            }
		        }
		        
				if (jsonNotification == null) {
					throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
							"No notification found in upload form.");
				} 
		    	else 
		    	{	
		    		Notification notification = Notification.fromJSON(jsonNotification);
		    		
			    	notification.setOwner(getAuthenticatedUsername());
					
			    	if (StringUtils.contains(notification.getAssociatedUuid(), "*") && 
			    			!ServiceUtils.isAdmin(getAuthenticatedUsername())) 
			    	{
			    		throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
								"Administrator permissions are required to set wildcard notifications.");
			    	} 
			    	else 
			    	{
						dao.persist(notification);
						
						return Response.status(javax.ws.rs.core.Response.Status.CREATED)
								.entity(new AgaveSuccessRepresentation(notification.toJSON()))
								.build();
			    	}
		    	}
			}
		} 
		catch (NotificationException e) {
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
					e.getMessage(), e);
		}
		catch (IllegalArgumentException e) {
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
					"Unable to save notification: " + e.getMessage(), e);
	    } 
		catch (HibernateException e) {
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
					"Unable to save notification: " + e.getMessage(), e);
	    }
		catch (JsonProcessingException e)
		{
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
					"Unable to process the notification json description.", e);
		}
		catch (ResourceException e) {
			throw e;
		} 
		catch (Throwable e) {
			e.printStackTrace();
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
					"Failed to save notification: " + e.getMessage(), e);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.iplantc.service.notification.resources.NotificationResource#addNotification(byte[])
	 */
	@Override
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	public Response addNotification(byte[] bytes)
	{
		AgaveLogServiceClient.log(NOTIFICATIONS02.name(), 
				NotifAdd.name(), 
				getAuthenticatedUsername(), "", 
				Request.getCurrent().getClientInfo().getUpstreamAddress());
		
		try {
			if (ArrayUtils.isEmpty(bytes)) {
				throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
						"No notification object provided.");
			} 
			else 
			{
				ObjectMapper mapper = new ObjectMapper();
        		JsonNode jsonNotification = mapper.readTree(bytes);
            	
        		Notification notification = null;
				if (jsonNotification.isObject()) {
					notification = Notification.fromJSON((ObjectNode)jsonNotification);
				} else {
					throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
							"Notification should be a valid JSON object.");
				}
				
		    	notification.setOwner(getAuthenticatedUsername());
		    	
		    	if (StringUtils.contains(notification.getAssociatedUuid(), "*") && 
		    			!ServiceUtils.isAdmin(getAuthenticatedUsername())) 
		    	{
		    		throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
							"Administrator permissions are required to set wildcard notifications.");
		    	} 
		    	else 
		    	{
					dao.persist(notification);
					
					return Response.status(javax.ws.rs.core.Response.Status.CREATED)
							.entity(new AgaveSuccessRepresentation(notification.toJSON()))
							.build();
		    	}
			}
		} 
		catch (NotificationException e) {
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
					e.getMessage(), e);
		}
		catch (IllegalArgumentException e) {
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
					"Unable to save notification: " + e.getMessage(), e);
	    } 
		catch (HibernateException e) {
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
					"Unable to save notification: " + e.getMessage(), e);
	    }
		catch (JsonProcessingException e)
		{
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
					"Unable to process the notification json description.", e);
		}
		catch (ResourceException e) {
			throw e;
		} 
		catch (Throwable e) {
			e.printStackTrace();
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
					"Failed to save notification: " + e.getMessage(), e);
		}
	}
}
