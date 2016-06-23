/**
 * 
 */
package org.iplantc.service.data.resources;

import java.io.File;
import java.io.IOException;
import java.net.URI;

import javax.activation.MimetypesFileTypeMap;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.common.clients.AgaveLogServiceClient;
import org.iplantc.service.common.exceptions.PermissionException;
import org.iplantc.service.common.representation.IplantErrorRepresentation;
import org.iplantc.service.common.representation.RemoteDataWriterRepresentation;
import org.iplantc.service.common.util.Slug;
import org.iplantc.service.common.uuid.UniqueId;
import org.iplantc.service.data.transform.FileTransform;
import org.iplantc.service.data.transform.FileTransformFilter;
import org.iplantc.service.data.transform.FileTransformProperties;
import org.iplantc.service.data.transform.FilterChain;
import org.iplantc.service.data.transform.TransformLauncher;
import org.iplantc.service.io.dao.LogicalFileDao;
import org.iplantc.service.io.exceptions.TransformException;
import org.iplantc.service.io.model.LogicalFile;
import org.iplantc.service.io.permissions.PermissionManager;
import org.iplantc.service.systems.model.RemoteSystem;
import org.iplantc.service.systems.util.ApiUriUtil;
import org.iplantc.service.transfer.RemoteDataClient;
import org.restlet.Context;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.FileRepresentation;
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
public class SyncFileTransformResource extends AbstractTransformResource 
{
	private static final Logger log = Logger.getLogger(SyncFileTransformResource.class); 

	private String internalUsername;
	private String username;
	private RemoteDataClient remoteClient;
	
	/**
	 * @param context
	 * @param request
	 * @param response
	 */
	public SyncFileTransformResource(Context context, Request request, Response response) {
		super(context, request, response);

		this.username = getAuthenticatedUsername();
		
		this.internalUsername = (String)request.getAttributes().get("internalUsername");
        
		getVariants().add(new Variant(MediaType.TEXT_ALL));
	}
	
	/* (non-Javadoc)
	 * @see org.restlet.resource.Resource#acceptRepresentation(org.restlet.resource.Representation)
	 */
	@Override
	public void acceptRepresentation(Representation entity)
	{
		AgaveLogServiceClient.log(AgaveLogServiceClient.ServiceKeys.JOBS02.name(), 
				AgaveLogServiceClient.ActivityKeys.DataSpecifiedExport.name(), 
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
		json.put("name", form.getFirstValue("name"));
		json.put("destFormat", form.getFirstValue("destFormat"));
		json.put("sourceFormat", form.getFirstValue("sourceFormat"));
		
//		json.put("destUrl", form.getFirstValue("destUrl"));
//		if (StringUtils.isEmpty(form.getFirstValue("notifications"))) {
//			json.put("notification", form.getFirstValue("path"));
//		}
		
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
//		URI destUrl = null;
		String sourceFormat = null;
		String destFormat = null;
		String downloadFilname = null;
//		List<Notification> notifications = new ArrayList<Notification>();
		
		String tempRoot = org.iplantc.service.common.Settings.TEMP_DIRECTORY;
        File tempDir = new File(org.iplantc.service.common.Settings.TEMP_DIRECTORY + "/" + new UniqueId().getStringId());
        
		try {
			
			if (!tempDir.mkdirs()) {
				throw new ResourceException(Status.SERVER_ERROR_BAD_GATEWAY, 
						"Unable to create temp directory to transform file", new IOException());
			}

			
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
			
			if (json.hasNonNull("name"))
			{
				if (json.get("name").isTextual()) {
					downloadFilname = json.get("name").asText();
				}
				else
				{
					throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
							"Invalid name value. If specified, name must be a "
							+ "valid filename for the downloaded item.");
				}
			}
			else
			{
				// if not specified, this becomes a pure IO operation.
				downloadFilname = null;
			}
			
//			if (json.hasNonNull("destUrl"))
//			{
//				if (json.get("destUrl").isTextual()) {
//					destUrl = new URI(json.get("destUrl").asText());
//				}
//				else 
//				{
//					throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
//							"Invalid destination url. Please specify a valid URI or agave URL to a file.");
//				}				
//			}
//			else
//			{
//				// stream back to the client
//				throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
//						"No destUrl url specified.");
//			}
			
			RemoteSystem system = ApiUriUtil.getRemoteSystem(username, sourceUrl);
			String path = ApiUriUtil.getPath(sourceUrl);
			LogicalFile logicalFile = LogicalFileDao.findBySystemAndPath(system, path);
			
			remoteClient = system.getRemoteDataClient(internalUsername);
			
            remoteClient.authenticate();
			
            if (!remoteClient.doesExist(path)) 
			{ 
				// if it doesn't exist, it was deleted outside of the api, 
				// so clean up the file entry in the db and its permissions
//				if (logicalFile != null) {
//					// permissions should be deleted on cascade
//					logicalFile.addEvent(new FileEvent("DELETED", "File or directory deleted outside of API", username));
//					LogicalFileDao.removeSubtree(logicalFile);
//				}
				
				throw new ResourceException(Status.SERVER_ERROR_INTERNAL, "File does not exist");
			} 
			else if (remoteClient.isDirectory(path)) 
			{ 
				// return file listing
				throw new ResourceException(Status.SERVER_ERROR_NOT_IMPLEMENTED, "Directory analysis not supported");
			} 
			else 
			{
//				if (logicalFile == null) {
//					logicalFile = new LogicalFile();
//	                logicalFile.setUuid(new AgaveUUID(UUIDType.FILE).toString());
//	        		logicalFile.setName(FilenameUtils.getName(path));
//	                logicalFile.setNativeFormat(StringUtils.isEmpty(sourceFormat) ? "raw" : sourceFormat);
//	                logicalFile.setOwner(username);
//	                logicalFile.setSource(sourceUrl.toString());
//	                logicalFile.setPath(path);
//	                logicalFile.setSystem(system);
//	                logicalFile.setStatus(StagingTaskStatus.STAGING_COMPLETED);
//	                logicalFile.setInternalUsername(internalUsername);
//	                LogicalFileDao.persist(logicalFile);
//				}
				PermissionManager pm = new PermissionManager(system, remoteClient, logicalFile, username);
				
				// make sure they have permission to view the file
				try {
					if (!pm.canRead(remoteClient.resolvePath(path))) {
						throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
								"User does not have access to view the requested resource");
					}
				} catch (PermissionException e) {
					throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
							"Failed to retrieve permissions for " + path + ", " + e.getMessage());
				}
				
				log.debug("Requested format for " + path + " is " + sourceFormat);
				if (StringUtils.isEmpty(sourceFormat)) {
	    			// lookup the original import record of the file
					// if it's not there, we assume it's the destination format. ie, it's essentially an IO operation
					if (logicalFile != null) {
						sourceFormat = logicalFile.getNativeFormat();
						log.debug("looking up source transform from original file " + sourceFormat);
					} else {
						sourceFormat = destFormat;
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
	    			throw new ResourceException(Status.SERVER_ERROR_INTERNAL, 
	    					"Unable to retrieve transform list", e);
	    		}
			}
	    	
	    	// check that there is a decoding available from the original format to the specified one
			FilterChain decoder = null;
			if (StringUtils.isEmpty(destFormat)) 
			{
				throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, 
						"Invalid transform name", new NullPointerException());
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
							"No transform found to convert " + sourceFormat + " to " + destFormat
							, new TransformException());
				}
			}

            // GET the source file to the server
            
            // Get remote file to local transform node
            String localPath = tempDir.getAbsolutePath() + FilenameUtils.getName(path);
            remoteClient.get(path, localPath);

			// Now run the transforms in place.
			String previousIterationPath = localPath;

			for (FileTransformFilter filter: decoder.getFilters()) {
				File decodedOutputFile = File.createTempFile(FilenameUtils.getName(path) + "-" + System.currentTimeMillis(), Slug.toSlug(filter.getName()).toLowerCase(), tempDir);
				String sourcePathForIteration = null;
				
				try {
					// filters have a flag enabling them to always operate on the original file. Check for this
					sourcePathForIteration = (filter.isUseOriginalFile() ? localPath : previousIterationPath);

					TransformLauncher.invokeBlocking(nativeTransform.getScriptFolder() + "/" + filter.getHandle(), sourcePathForIteration, decodedOutputFile.getAbsolutePath());
                    //remoteClient.put(tmpFilePath, tmpFilePath);
					
				} catch (TransformException e) {
					String message = "Failed to apply filter " + decoder.getName() + ":" + filter.getName() + " to file " + sourcePathForIteration;
					
					// cleanup the file
					FileUtils.deleteQuietly(decodedOutputFile);
					
                    throw new ResourceException(Status.SERVER_ERROR_INTERNAL, message);
				} 
				
				previousIterationPath = decodedOutputFile.getAbsolutePath();
			}
			
			// rename the file so download and/or staging is clean
			if (StringUtils.isNotEmpty(downloadFilname)) {
        		File dowloadFile = new File(previousIterationPath, downloadFilname);
        		new File(previousIterationPath).renameTo(dowloadFile);
        		previousIterationPath = dowloadFile.getAbsolutePath();
        	}
        	
			// serve it up directly if it's a file. Saves download time staging back to source
			final File decodedFinalOutput = new File(previousIterationPath);
            if (decodedFinalOutput.exists() && decodedFinalOutput.isFile()) {
            	return new FileRepresentation(decodedFinalOutput, MediaType.valueOf("APPLICATION_OCTET_STREAM")) {
            		
            		 /* (non-Javadoc)
					 * @see org.restlet.resource.Representation#getDownloadName()
					 */
					@Override
					public String getDownloadName() {
						return super.getDownloadName();
					}

					/**
            	     * Releases the file handle.
            	     */
            	    @Override
            	    public void release() {
            	        super.release();
            	        FileUtils.deleteQuietly(decodedFinalOutput);
            	    }
            	};
            }
            // otherwise put file from final iteration to remoteDataClient
            else {
            	remoteClient.put(decodedFinalOutput.getAbsolutePath(), FilenameUtils.getPath(path));
				return new RemoteDataWriterRepresentation(
						remoteClient, path, previousIterationPath, MediaType.valueOf("APPLICATION_OCTET_STREAM"));
            }
		}
		catch (ResourceException e) {
			throw e;
		}
		catch (Throwable e)
		{
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL, e.getMessage());
		} 
		finally {
			try { remoteClient.disconnect(); } catch (Exception e) {}
			FileUtils.deleteQuietly(tempDir);
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
