/**
 * 
 */
package org.iplantc.service.data.resources;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.common.clients.AgaveLogServiceClient;
import org.iplantc.service.common.exceptions.PermissionException;
import org.iplantc.service.common.representation.IplantErrorRepresentation;
import org.iplantc.service.common.representation.IplantSuccessRepresentation;
import org.iplantc.service.common.uuid.AgaveUUID;
import org.iplantc.service.common.uuid.UUIDType;
import org.iplantc.service.data.dao.DecodingTaskDao;
import org.iplantc.service.data.transform.FileTransform;
import org.iplantc.service.data.transform.FileTransformProperties;
import org.iplantc.service.data.transform.FilterChain;
import org.iplantc.service.io.dao.LogicalFileDao;
import org.iplantc.service.io.exceptions.TransformException;
import org.iplantc.service.io.manager.LogicalFileManager;
import org.iplantc.service.io.model.FileEvent;
import org.iplantc.service.io.model.LogicalFile;
import org.iplantc.service.io.model.enumerations.FileEventType;
import org.iplantc.service.io.model.enumerations.StagingTaskStatus;
import org.iplantc.service.io.permissions.PermissionManager;
import org.iplantc.service.notification.model.Notification;
import org.iplantc.service.systems.model.RemoteSystem;
import org.iplantc.service.systems.util.ApiUriUtil;
import org.iplantc.service.transfer.RemoteDataClient;
import org.quartz.SchedulerException;
import org.restlet.Context;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Class to handle get and post requests for jobs
 * 
 * @author dooley
 * 
 */
public class AsyncFileTransformResource extends AbstractTransformResource {
	private static final Logger log = Logger.getLogger(AsyncFileTransformResource.class); 

	private String username;
	private String internalUsername;
	private RemoteDataClient remoteClient;
	
	/**
	 * @param context
	 * @param request
	 * @param response
	 */
	public AsyncFileTransformResource(Context context, Request request, Response response) {
		super(context, request, response);

		this.username = getAuthenticatedUsername();
		
		this.internalUsername = (String)request.getAttributes().get("internal.username");
        
        getVariants().add(new Variant(MediaType.APPLICATION_JSON));
	}

	/* 
	 * Handles requests for file transforms
	 */
	@Override
    public void acceptRepresentation(Representation entity) 
	{
		AgaveLogServiceClient.log(AgaveLogServiceClient.ServiceKeys.JOBS02.name(), 
				AgaveLogServiceClient.ActivityKeys.DataImpliedExport.name(), 
				username, "", getRequest().getClientInfo().getUpstreamAddress());
		
		try 
		{
			JsonNode json = super.getPostedEntityAsObjectNode(true);
			
			Representation representation = processTransformRequest(json);
    		
        	getResponse().setStatus(Status.SUCCESS_CREATED);
			getResponse().setEntity(representation);
		}
    	catch (ResourceException e) 
		{
			getResponse().setEntity(
					new IplantErrorRepresentation(e.getMessage()));
			getResponse().setStatus(e.getStatus());
			log.error("Synchronous file transform failed for user " + username, e);
		}
		catch (Throwable e) {
			getResponse().setEntity(
					new IplantErrorRepresentation("Failed to synchronously transform file: " + e.getMessage()));
			getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
			log.error("Failed to synchronously transform file for user " + username, e);
		}
		
	}
	
	public Representation processTransformRequest(Form form)
	throws ResourceException
	{
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode json = mapper.createObjectNode();
		json.put("sourceUrl", form.getFirstValue("sourceUrl"));
		json.put("destUrl", form.getFirstValue("destUrl"));
		json.put("destFormat", form.getFirstValue("destFormat"));
		json.put("sourceFormat", form.getFirstValue("sourceFormat"));
		if (!StringUtils.isEmpty(form.getFirstValue("notifications"))) {
			json.put("notification", form.getFirstValue("path"));
		}
		
		return processTransformRequest(json);
	}
	
	/**
	 * Takes the 
	 * @param path
	 * @param systemId
	 * @throws ResourceException
	 */
	public Representation processTransformRequest(JsonNode json)
	throws ResourceException
	{
		URI sourceUrl = null;
		URI destUrl = null;
		String sourceFormat = null;
		String destFormat = null;
		
		try {
			FileTransformProperties transformProps = new FileTransformProperties();
			FileTransform nativeTransform = null;
			
			if (json.hasNonNull("sourceUrl"))
			{
				if (json.get("sourceUrl").isTextual()) {
					sourceUrl = new URI(json.get("sourceUrl").asText());
				}
				else 
				{
					throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
							"Invalid sourceUrl value. Please specify a valid URI to a file.");
				}
			}
			else
			{
				throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
						"No source url specified.");
			}
			
			if (json.hasNonNull("sourceFormat"))
			{
				if (json.get("sourceFormat").isTextual()) {
					sourceFormat = json.get("sourceFormat").asText();
				}
				else 
				{
					throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
							"Invalid sourceFormat value. If specified, sourceFormat must be either raw or one of the " +
							"known transform types registered with the API's transforms service.");
				}
			}
			
			if (json.hasNonNull("destFormat"))
			{
				if (json.get("destFormat").isTextual()) {
					destFormat = json.get("destFormat").asText();
				}
				else
				{
					throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
							"Invalid destFormat value. If specified, sourceFormat must be either raw or one of the " +
							"known transform types registered with the API's transforms service.");
				}
			}
			else
			{
				// if not specified, this becomes a pure IO operation.
				destFormat = sourceFormat;
			}
			
			if (json.hasNonNull("destUrl"))
			{
				if (json.get("destUrl").isTextual()) {
					destUrl = new URI(json.get("destUrl").asText());
				}
				else 
				{
					throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
							"Invalid destination url. Please specify a valid URI or agave URL to a file.");
				}				
			}
			else
			{
				// stream back to the client
				throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
						"No destUrl url specified.");
			}
			
			RemoteSystem system = ApiUriUtil.getRemoteSystem(username, sourceUrl);
			String path = ApiUriUtil.getPath(sourceUrl);
			LogicalFile logicalFile = LogicalFileDao.findBySystemAndPath(system, path);
			
			remoteClient = system.getRemoteDataClient(internalUsername);
			
            remoteClient.authenticate();
			
            if (!remoteClient.doesExist(path)) 
            {
				// if it doesn't exist, it was deleted outside of the api, 
				// so clean up the file entry in the db and its permissions
				if (logicalFile != null) {
					logicalFile.addContentEvent(new FileEvent(FileEventType.DELETED, "File or directory deleted outside of API", username));
					LogicalFileDao.removeSubtree(logicalFile);
				}
				
				throw new ResourceException(Status.SERVER_ERROR_INTERNAL, "File does not exist");
			} 
            else 
            {
				if (!remoteClient.isFile(path)) 
				{
					throw new ResourceException(Status.SERVER_ERROR_NOT_IMPLEMENTED, "Directory analysis not supported");
				} 
				else 
				{
					// make sure they have permission to view the file
					PermissionManager pm = new PermissionManager(system, remoteClient, logicalFile, username);
					
					try {
						if (!pm.canRead(remoteClient.resolvePath(path))) {
							throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
									"User does not have access to view the requested resource");
						}
					} catch (PermissionException e) {
						throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
								"Failed to retrieve permissions for " + path + ", " + e.getMessage());
					}
				}
			
				// verify the file exists
	            log.debug("Requested format for " + path + " is " + sourceFormat);
				if (!StringUtils.isEmpty(sourceFormat)) {
	    			// lookup the original import record of the file
					// if it's not there, we assume it's the destination format. ie, it's essentially an IO operation
					if (logicalFile != null) {
						sourceFormat = logicalFile.getNativeFormat();
						log.debug("looking up source transform from original file " + sourceFormat);
					} else {
						sourceFormat = destFormat;
						
						logicalFile = new LogicalFile();
		                logicalFile.setUuid(new AgaveUUID(UUIDType.FILE).toString());
		        		logicalFile.setName(FilenameUtils.getName(path));
		                logicalFile.setNativeFormat(sourceFormat);
		                logicalFile.setOwner(username);
		                logicalFile.setSourceUri(sourceUrl.toString());
		                logicalFile.setPath(remoteClient.resolvePath(path));
		                logicalFile.setSystem(system);
		                logicalFile.setStatus(StagingTaskStatus.STAGING_COMPLETED);
		                logicalFile.setInternalUsername(internalUsername);
		                LogicalFileDao.persist(logicalFile);
						
						log.debug("assuming " + path + " was originally in the destination format " + sourceFormat);
					}
				} 
				
				log.debug("Source format for requested file " + path + " is " + sourceFormat);
	    		// get the original file type. if it's not available, FAIL. There's no way to know how to convert
	        	// it without the original file type
				try {
	    			nativeTransform = transformProps.getTransform(sourceFormat);
	    			if (nativeTransform == null) {
	    				throw new ResourceException(Status.SERVER_ERROR_NOT_IMPLEMENTED,
	    						"Original transform is no longer supported");
	    			}
	    		} catch (TransformException e) {
	    			throw new ResourceException(Status.SERVER_ERROR_INTERNAL, "Unable to retrieve transform list");
	    		}
            }
	        	
            FilterChain decoder = null;
			if (!StringUtils.isEmpty(destFormat)) 
			{
				throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, "Invalid transform name");
			} 
			else 
			{
				decoder = nativeTransform.getDecoder(destFormat);
				
				// With one way transforms like HEAD, TAIL etc, some transforms now just have encoders. If there is no decoder, it is now
                // assumed that one is not needed
                if (decoder == null) {
                    decoder = nativeTransform.getEncodingChain();
                }

				if (decoder == null) {
					throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, 
							"No transform found to convert " + sourceFormat + " to " + destFormat);
				}
			}
	    	
			if (json.hasNonNull("destUrl"))
			{
				if (json.get("destUrl").isTextual()) {
					destUrl = new URI(json.get("destUrl").asText());
				}
				else 
				{
					throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
							"Invalid destination url. Please specify a valid URI or agave URL to a file.");
				}				
			}
			
			List<Notification> notifications = new ArrayList<Notification>();
			if (json.has("notifications") && json.get("notifications").isArray()) 
			{	
				notifications = LogicalFileManager.addUploadNotifications(logicalFile, this.username, json.get("notifications").asText());
				
//				ArrayNode jsonNotifications = (ArrayNode)json.get("notifications");
//				for (int i=0; i<jsonNotifications.size(); i++) 
//				{
//					JsonNode jsonNotif = jsonNotifications.get(i);
//					if (jsonNotif != null) 
//					{	
//						String url = jsonNotif.get("url").asText();
//						String event = jsonNotif.get("event").asText();
//						try {
//							if (StringUtils.equals("*", event)) {
//								// this is ok 
//							} else {
//								try {
//									StagingTaskStatus.valueOf(event.toUpperCase());
//								} catch (IllegalArgumentException e) {
//									TransformTaskStatus.valueOf(event.toUpperCase());
//								}
//							}
//						} catch (Throwable e) {
//							throw new NotificationException("Valid values are: *, " + 
//									ServiceUtils.explode(", ", Arrays.asList(StagingTaskStatus.values())) + "," + 
//									ServiceUtils.explode(", ", Arrays.asList(TransformTaskStatus.values())));
//						}
//						
//						boolean persistent = false;
//						if (jsonNotif.has("persistent") && !jsonNotif.get("persistent").isNull()) 
//						{
//							persistent = jsonNotif.get("persistent").asBoolean(false);
//						}
//						logicalFile.addNotification(event, url, persistent);
//					}
//				}
			}
        	
			// generate a temp file for the operation
			File tmpFilePath = new File(org.iplantc.service.common.Settings.TEMP_DIRECTORY, FilenameUtils.getName(path) + "-" + System.currentTimeMillis());
			if (!tmpFilePath.exists()) {
				tmpFilePath.mkdirs();
			}
			
			DecodingTaskDao.enqueueDecodingTask(logicalFile, system, path, tmpFilePath.getAbsolutePath(), sourceFormat,
					sourceFormat, decoder.getFirstFilter().getName(), destUrl.toString(), username);
            
			// we need to assign a transfer id here
			getResponse().setStatus(Status.SUCCESS_ACCEPTED);
			return new IplantSuccessRepresentation(logicalFile.toJsonWithNotifications(notifications));
		}
		catch (SchedulerException e) {
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL, e.getMessage());
		}
		catch (Exception e) 
		{
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, e.getMessage());
		}
		finally {
			try { remoteClient.disconnect(); } catch (Exception e1) {}
		}
    }
	
	/* (non-Javadoc)
	 * @see org.restlet.resource.Resource#allowDelete()
	 */
	@Override
	public boolean allowDelete() {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.restlet.resource.Resource#allowGet()
	 */
	@Override
	public boolean allowGet() {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.restlet.resource.Resource#allowPost()
	 */
	@Override
	public boolean allowPost() {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.restlet.resource.Resource#allowPut()
	 */
	@Override
	public boolean allowPut() {
		return false;
	}
}
