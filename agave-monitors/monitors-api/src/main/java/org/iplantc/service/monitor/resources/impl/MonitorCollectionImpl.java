/**
 * 
 */
package org.iplantc.service.monitor.resources.impl;

import static org.iplantc.service.common.clients.AgaveLogServiceClient.ActivityKeys.MonitorAdd;
import static org.iplantc.service.common.clients.AgaveLogServiceClient.ActivityKeys.MonitorsList;
import static org.iplantc.service.common.clients.AgaveLogServiceClient.ServiceKeys.MONITORS02;

import java.util.Arrays;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.hibernate.HibernateException;
import org.iplantc.service.common.clients.AgaveLogServiceClient;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.common.representation.AgaveSuccessRepresentation;
import org.iplantc.service.common.restlet.resource.AbstractAgaveResource;
import org.iplantc.service.monitor.Settings;
import org.iplantc.service.monitor.dao.MonitorDao;
import org.iplantc.service.monitor.events.MonitorEventProcessor;
import org.iplantc.service.monitor.exceptions.MonitorException;
import org.iplantc.service.monitor.managers.MonitorPermissionManager;
import org.iplantc.service.monitor.model.Monitor;
import org.iplantc.service.monitor.model.enumeration.MonitorEventType;
import org.iplantc.service.monitor.resources.MonitorCollection;
import org.iplantc.service.monitor.util.ServiceUtils;
import org.iplantc.service.notification.dao.NotificationDao;
import org.iplantc.service.notification.exceptions.NotificationException;
import org.iplantc.service.notification.model.Notification;
import org.iplantc.service.systems.dao.SystemDao;
import org.iplantc.service.systems.model.RemoteSystem;
import org.joda.time.DateTime;
import org.restlet.Request;
import org.restlet.data.Status;
import org.restlet.ext.fileupload.RestletFileUpload;
import org.restlet.representation.Representation;
import org.restlet.resource.ResourceException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author dooley
 *
 */
@Path("")
@Produces(MediaType.APPLICATION_JSON)
public class MonitorCollectionImpl extends AbstractAgaveResource implements MonitorCollection
{
	protected MonitorDao dao = new MonitorDao();
	protected MonitorPermissionManager pm = null;
	protected MonitorEventProcessor eventProcessor = new MonitorEventProcessor();
	
	
	/* (non-Javadoc)
	 * @see org.iplantc.service.monitor.resources.MonitorResource#getMonitors()
	 */
	@Override
	@GET
	public Response getMonitors(@QueryParam("target") String systemId, @QueryParam("active") String active)
	{
		AgaveLogServiceClient.log(MONITORS02.name(), 
				MonitorsList.name(), 
				getAuthenticatedUsername(), "", 
				Request.getCurrent().getClientInfo().getAddress());
		
		try
		{
			List<Monitor> monitors = null;
			boolean includeActive = false;
			boolean includeInactive = false;
			if (!StringUtils.isEmpty(active)) {
				includeActive = Boolean.parseBoolean(active);
				includeInactive = !Boolean.parseBoolean(active);
			}
			if (!StringUtils.isEmpty(systemId)) 
			{
				SystemDao systemDao = new SystemDao();
				RemoteSystem system = systemDao.findBySystemId(systemId);
				
				if (system == null) 
				{
					throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
							"No system found matching " + systemId);
				}
				else
				{
					if (!system.getUserRole(getAuthenticatedUsername()).canUse())
					{
						throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
								"Permission denied. You do not have permission to view this system");
					}
				}
			}
			
			monitors = dao.getUserMonitors(getAuthenticatedUsername(), includeActive, includeInactive, systemId);
			
			ObjectMapper mapper = new ObjectMapper();
			ArrayNode arrayNode = mapper.createArrayNode();
			
			for (int i=getOffset(); i< Math.min((getLimit()+getOffset()), monitors.size()); i++)
			{
				Monitor monitor = monitors.get(i);
				ObjectNode json = mapper.createObjectNode();
				
				json.put("id", monitor.getUuid())
					.put("target", monitor.getSystem().getSystemId())
					.put("updateSystemStatus", monitor.isUpdateSystemStatus())
					.put("active", monitor.isActive())
					.put("frequency", monitor.getFrequency())
					.put("lastSuccess", monitor.getLastSuccess() == null ? null : new DateTime(monitor.getLastSuccess()).toString())
					.put("lastUpdated", new DateTime(monitor.getLastUpdated()).toString());
					
					ObjectNode linksObject = mapper.createObjectNode();
					linksObject.put("self", (ObjectNode)mapper.createObjectNode()
			    		.put("href", TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_MONITOR_SERVICE) + monitor.getUuid()));
					linksObject.put("history", (ObjectNode)mapper.createObjectNode()
				    		.put("href", TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_MONITOR_SERVICE) + monitor.getUuid() + "/history"));
					linksObject.put("checks", (ObjectNode)mapper.createObjectNode()
				    		.put("href", TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_MONITOR_SERVICE) + monitor.getUuid() + "/checks"));
					linksObject.put("target", (ObjectNode)mapper.createObjectNode()
				    		.put("href", TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_SYSTEM_SERVICE) + monitor.getSystem().getSystemId()));
					
					json.set("_links", linksObject);
				
				arrayNode.add(json);
			}
			
			return Response.ok(new AgaveSuccessRepresentation(arrayNode.toString())).build();
		}
		catch (MonitorException e)
		{
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
					"Failed to retrieve monitors.", e);
		}
		catch (ResourceException e) {
			throw e;
		}
		catch (Throwable e)
		{
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
					"Failed to retrieve monitors.", e);
		}
		
		
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.monitor.resources.MonitorResource#addMonitorFromForm(org.iplantc.service.monitor.model.Monitor)
	 */
	@Override
	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public Response addMonitorFromForm(@FormParam("target") String systemId,
										@FormParam("internalUsername") String internalUsername,	
										@FormParam("frequency") String frequency,
										@FormParam("updateSystemStatus") String updateSystemStatus,
										@FormParam("active") String active)
	{
		AgaveLogServiceClient.log(MONITORS02.name(), 
				MonitorAdd.name(), 
				getAuthenticatedUsername(), "", 
				Request.getCurrent().getClientInfo().getAddress());
		
		if (StringUtils.isEmpty(systemId)) {
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
					"No system found matching " + systemId);
		} else {
			RemoteSystem system = new SystemDao().findBySystemId(systemId);
			if (!system.getUserRole(getAuthenticatedUsername()).canUse()) {
				throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
						"Permission denied. You do not have permission to view this system");
			}
		}
		
		try
		{	
			ObjectMapper mapper = new ObjectMapper();
			ObjectNode jsonMonitor = mapper.createObjectNode()
					.put("target", systemId)
					.put("frequency", Integer.parseInt(frequency))
					.put("updateSystemStatus", StringUtils.equalsIgnoreCase(updateSystemStatus, "true") || 
											   StringUtils.equalsIgnoreCase(updateSystemStatus, "1"))
					.put("internalUsername", internalUsername)
					.put("active", StringUtils.equalsIgnoreCase(active, "true") || 
                                   StringUtils.equalsIgnoreCase(active, "1"));
			
    		Monitor monitor = Monitor.fromJSON(jsonMonitor, null, getAuthenticatedUsername());
    		monitor.setOwner(getAuthenticatedUsername());
			
			dao.persist(monitor);
			
			if (jsonMonitor.has("notifications")) 
			{	
				processNotifications(jsonMonitor, monitor);
			}
			
			eventProcessor.processContentEvent(monitor, MonitorEventType.CREATED, getAuthenticatedUsername());
			// NotificationManager.process(monitor.getUuid(), MonitorEventType.CREATED.name(), monitor.getOwner());
			
			return Response.status(javax.ws.rs.core.Response.Status.CREATED)
					.entity(new AgaveSuccessRepresentation(monitor.toJSON()))
					.build();
    	} 
		catch (MonitorException e) {
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
					e.getLocalizedMessage(), e);
		}
		catch (IllegalArgumentException e) {
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
					"Unable to save monitor: " + e.getMessage(), e);
	    } 
		catch (HibernateException e) {
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
					"Unable to save monitor: " + e.getMessage(), e);
	    }
//		catch (JsonException e)
//		{
//			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
//					"Unable to process the monitor json description.", e);
//		}
		catch (ResourceException e) {
			throw e;
		} 
		catch (Exception e) {
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
					"Failed to save monitor: " + e.getMessage(), e);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.iplantc.service.monitor.resources.MonitorResource#addMonitor(Representation)
	 */
	@Override
	@POST
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response addMonitor(Representation input)
	{
		AgaveLogServiceClient.log(MONITORS02.name(), 
				MonitorAdd.name(), 
				getAuthenticatedUsername(), "", 
				Request.getCurrent().getClientInfo().getAddress());
		
		try {
			if (input == null) {
				throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
						"No monitor object provided.");
			} 
			else 
			{
				
				RestletFileUpload fileUpload = new RestletFileUpload(new DiskFileItemFactory());

		        // this list is always empty !!
		        List<FileItem> fileItems = fileUpload.parseRepresentation(input);
		        ObjectMapper mapper = new ObjectMapper();
		        JsonNode jsonMonitor = null;
		        for (FileItem fileItem : fileItems) {
		            if (!fileItem.isFormField()) {
		            	jsonMonitor = mapper.readTree(fileItem.getInputStream());
		            	break;
		            }
		        }
		        
				if (jsonMonitor == null || jsonMonitor.size() == 0) {
					throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
							"No monitor provided.");
				} 
		    	else 
		    	{	
		    		Monitor monitor = Monitor.fromJSON(jsonMonitor, null, getAuthenticatedUsername());
		    		
			    	monitor.setOwner(getAuthenticatedUsername());
					
					dao.persist(monitor);
					
					if (jsonMonitor.has("notifications")) 
					{	
						processNotifications(jsonMonitor, monitor);
					}
					
					eventProcessor.processContentEvent(monitor, MonitorEventType.CREATED, getAuthenticatedUsername());
					// NotificationManager.process(monitor.getUuid(), MonitorEventType.CREATED.name(), monitor.getOwner());
					
					return Response.status(javax.ws.rs.core.Response.Status.CREATED)
							.entity(new AgaveSuccessRepresentation(monitor.toJSON()))
							.build();
		    	}
			}
		} 
		catch (MonitorException e) {
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
					e.getMessage(), e);
		}
		catch (IllegalArgumentException e) {
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
					"Unable to save monitor: " + e.getMessage(), e);
	    } 
		catch (HibernateException e) {
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
					"Unable to save monitor: " + e.getMessage(), e);
	    }
		catch (ResourceException e) {
			throw e;
		} 
		catch (Throwable e) {
			e.printStackTrace();
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
					"Failed to save monitor: " + e.getMessage(), e);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.iplantc.service.monitor.resources.MonitorResource#addMonitor(byte[])
	 */
	@Override
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	public Response addMonitor(byte[] bytes)
	{
		AgaveLogServiceClient.log(MONITORS02.name(), 
				MonitorAdd.name(), 
				getAuthenticatedUsername(), "", 
				Request.getCurrent().getClientInfo().getAddress());
		
		try 
		{
			if (ArrayUtils.isEmpty(bytes)) 
			{
				throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
						"No monitor object provided.");
			} 
			else 
			{
				
				JsonNode jsonMonitor = new ObjectMapper().readTree(bytes);
		        
				Monitor monitor = Monitor.fromJSON(jsonMonitor, null, getAuthenticatedUsername());
	    		
		    	if (dao.doesExist(getAuthenticatedUsername(), monitor.getSystem())) {
		    		throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
							"Monitor already exists for " + monitor.getSystem().getSystemId());
		    	} else {
		    		dao.persist(monitor);
		    	}
				
				if (jsonMonitor.has("notifications")) 
				{	
					processNotifications(jsonMonitor, monitor);
				}
				
				eventProcessor.processContentEvent(monitor, MonitorEventType.CREATED, getAuthenticatedUsername());
				// NotificationManager.process(monitor.getUuid(), MonitorEventType.CREATED.name(), monitor.getOwner());
				
				return Response.status(javax.ws.rs.core.Response.Status.CREATED)
						.entity(new AgaveSuccessRepresentation(monitor.toJSON()))
						.build();
			}
		} 
		catch (MonitorException e) {
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
					e.getMessage(), e);
		}
		catch (IllegalArgumentException e) {
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
					"Unable to save monitor. " + e.getMessage(), e);
	    } 
		catch (HibernateException e) {
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
					"Unable to save monitor." + e.getMessage(), e);
	    }
		catch (ResourceException e) {
			throw e;
		} 
		catch (Throwable e) {
			e.printStackTrace();
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
					"Failed to save monitor: " + e.getMessage(), e);
		}
	}
	
	private void processNotifications(JsonNode json, Monitor monitor) throws NotificationException
	{
		NotificationDao notificationDao = new NotificationDao();
		
		String currentKey = "notifications";
		
		if (!json.get("notifications").isArray())
		{
			throw new NotificationException("Invalid " + currentKey + " value given. "
					+ "notifications must be an array of notification objects specifying a "
					+ "valid url, event, and an optional boolean persistence attribute.");
		}
		else
		{
			currentKey = "notifications";
			
			ArrayNode jsonNotifications = (ArrayNode)json.get("notifications");
			for (int i=0; i<jsonNotifications.size(); i++) 
			{
				currentKey = "notifications["+i+"]";
				JsonNode jsonNotif = jsonNotifications.get(i);
				if (!jsonNotif.isObject())
				{
					throw new NotificationException("Invalid " + currentKey + " value given. "
						+ "Each notification objects should specify a "
						+ "valid url, event, and an optional boolean persistence attribute.");
				}
				else
				{
					Notification notification = new Notification();
					notification.setAssociatedUuid(monitor.getUuid());
					notification.setOwner(getAuthenticatedUsername());
					
					currentKey = "notifications["+i+"].url";
					if (!jsonNotif.has("url")) {
						throw new NotificationException("No " + currentKey + " attribute given. "
								+ "Notifications must have valid url and event attributes.");
					}
					else 
					{
						notification.setCallbackUrl(jsonNotif.get("url").textValue());
					}
					
					currentKey = "notifications["+i+"].event";
					if (!jsonNotif.has("event")) {
						throw new NotificationException("No " + currentKey + " attribute given. "
								+ "Notifications must have valid url and event attributes.");
					}
					else
					{
						String event = jsonNotif.get("event").textValue();
						try {
							if (!StringUtils.equals("*", event)) {
								MonitorEventType.valueOf(event.toUpperCase());
								notification.setEvent(StringUtils.upperCase(event));
							}
							else {
								notification.setEvent("*");
							}
						} catch (Throwable e) {
							throw new NotificationException("Valid values are: *, " + 
									ServiceUtils.explode(", ", Arrays.asList(MonitorEventType.values())));
						}
					}
					
					if (jsonNotif.has("persistent")) 
					{
						currentKey = "notifications["+i+"].persistent";
						if (jsonNotif.get("persistent").isNull()) {
							throw new NotificationException(currentKey + " cannot be null");
						}
						else if (!jsonNotif.get("persistent").isBoolean()) 
						{
							throw new NotificationException("Invalid value for " + currentKey + ". "
									+ "If provided, " + currentKey + " must be a boolean value.");
						} else {
							notification.setPersistent(jsonNotif.get("persistent").asBoolean());
						}
					} else {
						notification.setPersistent(true);
					}
					
					notificationDao.persist(notification);
					try { Thread.sleep(5); } catch (Exception e){}
				}
			}
		}
	}
}
