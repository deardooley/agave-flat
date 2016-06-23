/**
 * 
 */
package org.iplantc.service.io.resources;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import javax.activation.MimetypesFileTypeMap;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.common.clients.AgaveLogServiceClient;
import org.iplantc.service.common.exceptions.PermissionException;
import org.iplantc.service.common.representation.AgaveSuccessRepresentation;
import org.iplantc.service.common.uri.UrlPathEscaper;
import org.iplantc.service.common.uuid.AgaveUUID;
import org.iplantc.service.common.uuid.UUIDType;
import org.iplantc.service.io.dao.LogicalFileDao;
import org.iplantc.service.io.model.LogicalFile;
import org.iplantc.service.io.model.enumerations.StagingTaskStatus;
import org.iplantc.service.io.permissions.PermissionManager;
import org.iplantc.service.io.util.PathResolver;
import org.iplantc.service.io.util.ServiceUtils;
import org.iplantc.service.systems.dao.SystemDao;
import org.iplantc.service.systems.exceptions.SystemException;
import org.iplantc.service.systems.manager.SystemManager;
import org.iplantc.service.systems.model.RemoteSystem;
import org.iplantc.service.systems.model.enumerations.RemoteSystemType;
import org.iplantc.service.transfer.RemoteDataClient;
import org.iplantc.service.transfer.RemoteFileInfo;
import org.iplantc.service.transfer.exceptions.RemoteDataException;
import org.iplantc.service.transfer.model.enumerations.PermissionType;
import org.joda.time.DateTime;
import org.json.JSONStringer;
import org.json.JSONWriter;
import org.restlet.Request;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.representation.Variant;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;

import com.google.gson.JsonArray;

/**
 * Class to handle get and post requests for jobs
 * 
 * @author dooley
 * 
 */
public class FileListingResource extends AbstractFileResource {
	private static final Logger log = Logger.getLogger(FileListingResource.class); 

	private String username;
    private String internalUsername;
	private String owner;
	private String path;
	private String systemId;
	private RemoteSystem remoteSystem;
	private RemoteDataClient remoteDataClient;
	
	@Override
	public void doInit() 
	{
		this.username = getAuthenticatedUsername();
		
		systemId = (String)Request.getCurrent().getAttributes().get("systemId");
        internalUsername = getInternalUsername();
        SystemManager sysManager = new SystemManager();
        SystemDao sysDao = new SystemDao();
        
        // Instantiate remote data client to the correct system

        try {
            if (systemId != null) {
            	remoteSystem = sysDao.findUserSystemBySystemId(username, systemId);
            } else {
                // If no system specified, select the user default system
            	remoteSystem = sysManager.getUserDefaultStorageSystem(username);
            }
        } 
        catch (SystemException e) {
        	throw new ResourceException(Status.SERVER_ERROR_INTERNAL, e);
        } 
        catch (Throwable e) {
            log.error("Failed to connect to remote system", e);
            // is this needed?
            try { remoteDataClient.disconnect(); } catch (Exception e1) {}
            
        }
        
		AgaveLogServiceClient.log(AgaveLogServiceClient.ServiceKeys.FILES02.name(), 
				AgaveLogServiceClient.ActivityKeys.IOList.name(), 
				username, "", getRequest().getClientInfo().getUpstreamAddress());
	}

	/** 
	 * This method represents the HTTP GET action. A directory listing.
	 *  
	 * @return {@link JsonArray} of {@link LogicalFile}
	 */
	@Get
	public Representation represent() throws ResourceException 
	{
		try
		{
			// make sure the resource they are looking for is available.
			if (remoteSystem == null) 
			{
	        	if (StringUtils.isEmpty(systemId)) {
	        		throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, 
	        				"No default storage system found. Please register a system and set it as your default. ");
	        	} else {
	        		throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, 
	        				"No resource found for user with system id '" + systemId + "'");
	        	}
	        }
			else if (!ServiceUtils.isAdmin(username) && remoteSystem.isPubliclyAvailable() && 
	        		remoteSystem.getType().equals(RemoteSystemType.EXECUTION)) 
			{
				throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN, 
						"User does not have access to view the requested resource. " + 
						"File access is restricted to administrators on public execution systems.");
			}
			
			try {
				this.remoteDataClient = remoteSystem.getRemoteDataClient(internalUsername);
			} 
			catch(RemoteDataException e)
			{
				throw new ResourceException(Status.CLIENT_ERROR_PRECONDITION_FAILED, e.getMessage(), e);
			} 
			catch (Exception e) {	
				throw new ResourceException(Status.CLIENT_ERROR_PRECONDITION_FAILED, 
						"Unable to establish a connection to the remote server. " + e.getMessage(), e);
			}
			
			try {
				String originalPath = getRequest().getOriginalRef().toUri().getPath();
				path = PathResolver.resolve(owner, originalPath);
				
				if (remoteSystem.isPubliclyAvailable()) {
					owner = PathResolver.getOwner(originalPath);
				}
			} catch (Exception e) {
				throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Invalid file path", e);
			}
	        
			LogicalFile logicalFile = null;
            try {logicalFile=LogicalFileDao.findBySystemAndPath(remoteSystem, remoteDataClient.resolvePath(path));} catch(Exception e) {}
            
            PermissionManager pm = new PermissionManager(remoteSystem, remoteDataClient, logicalFile, username);
            
			// check for the file on disk
			boolean exists = false;
			RemoteFileInfo remoteFileInfo = null;
			
			try {
                remoteDataClient.authenticate();
                exists = remoteDataClient.doesExist(path);
 
                if (!exists) {
                    throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
    						"File/folder does not exist");
                } 
                else {
                    // file exists on the file system, so make sure we have
                    // a logical file for it if not, add one
                    try {
                        if (!pm.canRead(remoteDataClient.resolvePath(path))) {
                        	throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
    								"User does not have access to view the requested resource");
                        }
                    } catch (PermissionException e) {
                    	throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN, 
    							"Failed to retrieve permissions for '" + path + "', " + e.getMessage());
                    }
                    
                    remoteFileInfo = remoteDataClient.getFileInfo(path);
                    
                    if (logicalFile == null) 
                    {
                        // if not a directory add the logical file
//                        if (!remoteFileInfo.isDirectory()) {
                            logicalFile = new LogicalFile();
                            logicalFile.setUuid(new AgaveUUID(UUIDType.FILE).toString());
                    		logicalFile.setNativeFormat(remoteFileInfo.isFile() ? LogicalFile.RAW : LogicalFile.DIRECTORY);
                            logicalFile.setOwner(StringUtils.isEmpty(owner) ? username : owner);
                            logicalFile.setSourceUri("");
                            logicalFile.setPath(remoteDataClient.resolvePath(path));
                            logicalFile.setName(FilenameUtils.getName(logicalFile.getPath()));
                            logicalFile.setSystem(remoteSystem);
                            logicalFile.setStatus(StagingTaskStatus.STAGING_COMPLETED);
                            logicalFile.setInternalUsername(internalUsername);
                            LogicalFileDao.persist(logicalFile);
//                        }
                    }
                }
			} catch (FileNotFoundException e) {
				throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
						"File/folder does not exist");
			} catch (ResourceException e) {
				throw e;
			} catch (RemoteDataException e) {
				throw new ResourceException(Status.SERVER_ERROR_BAD_GATEWAY,
						e.getMessage(), e);
            } catch (Exception e) {
            	throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
						"Failed to retrieve information for " + path, e);
            }

            List<RemoteFileInfo> listing = new ArrayList<RemoteFileInfo>();
			
            try 
            {
                JSONWriter writer = new JSONStringer();
                
                String format = "raw";
                boolean isDirectory = remoteFileInfo.isDirectory();
                if (isDirectory) {
                	format = "folder";
                } else if (logicalFile != null) {
                	format = logicalFile.getNativeFormat();
                }
                String sPermission = PermissionType.NONE.name();
                if (StringUtils.equals(username, owner)) {
                	sPermission = PermissionType.ALL.name();
                } else {
                    sPermission = remoteFileInfo.getPermissionType().name();
                }
                
                String fileName = remoteFileInfo.isFile() ? FilenameUtils.getName(remoteFileInfo.getName()) : ".";
                
                String absPath = StringUtils.removeStart(logicalFile.getPath(), remoteSystem.getStorageConfig().getRootDir());
                absPath = "/" + StringUtils.removeEnd(absPath, "/"); 
        		
                
//                String absPath = logicalFile.getPath()remoteDataClient.resolvePath(path) + (fileName.equals(".") ? "" : File.separator + fileName);
                
                // adjust the offset and print the path element only if offset is zero
                int theLimit = getLimit();
                int theOffset = getOffset();
                if (theOffset == 0) 
                {
                	theLimit--;
                	
	                writer.array().object()
		                .key("name").value(fileName)
		                .key("path").value(absPath)
		                .key("lastModified").value(new DateTime(remoteFileInfo.getLastModified()).toString())
		                .key("length").value(remoteFileInfo.getSize())
		//                            .key("owner").value(owner)
		                .key("permissions").value(sPermission)
		                .key("format").value(format);
		                if (isDirectory) {
							writer.key("mimeType").value("text/directory")
								  .key("type").value(LogicalFile.DIRECTORY);
						} else {
							File f = new File(remoteFileInfo.getName());
							String mimetype = new MimetypesFileTypeMap().getContentType(f);
							writer.key("mimeType").value(mimetype)
							  	  .key("type").value(LogicalFile.FILE);
						}
		                writer.key("system").value(remoteSystem.getSystemId())
 		                .key("_links").object()
		                	.key("self").object()
		                		.key("href").value(logicalFile.getPublicLink())
		                	.endObject()
		                	.key("system").object()
		                		.key("href").value(remoteSystem.getPublicLink())
		                	.endObject();
	                	if (logicalFile != null) {
		                	writer.key("metadata").object()
		            			.key("href").value(logicalFile.getMetadataLink())
		            		.endObject()
	                		.key("history").object()
		            			.key("href").value(logicalFile.getEventLink())
		            		.endObject();
	                	}
		            	writer.endObject()
		            .endObject();
                }
                else
                {
                	theOffset--;
                }
                
                if (remoteFileInfo.isDirectory()) 
                {
                	absPath = StringUtils.equals(absPath, "/") ? absPath : absPath + "/";
                	
	                listing = remoteDataClient.ls(path);
	                
	                for (int i=theOffset; i< Math.min((theLimit + theOffset), listing.size()); i++)
	                {
	                	RemoteFileInfo file = listing.get(i); 
	                
	                	if (file.getName().equals("..") || file.getName().equals(".")) {
	                		continue;
	                	} 
	                	else
	                	{
	                        format = "raw";
	                        
	                        if (file.isDirectory()) {
	                            format = "folder";
	                        }
	
	                        if (owner != null && username.equals(owner)) {
	                        	sPermission = PermissionType.ALL.name();
	                        } else {
	                            sPermission = file.getPermissionType().name();
	                        }
	                                    
	                        String absChildPath = absPath + file.getName();
	                        writer.object()
								.key("name").value(file.getName())
								.key("path").value(absChildPath)
								.key("lastModified").value(new DateTime(file.getLastModified()).toString())
								.key("length").value(file.getSize())
	//							.key("owner").value(owner)
								.key("permissions").value(sPermission)
								.key("format").value(format)
								.key("system").value(remoteSystem.getSystemId());
			                
	                        if (file.isDirectory()) {
								writer.key("mimeType").value("text/directory");
							} else {
								File f = new File(new File(absChildPath).getName());
								String mimetype = new MimetypesFileTypeMap().getContentType(f);
								writer.key("mimeType").value(mimetype);
							}
							
	                        writer.key("type").value((file.isDirectory()?LogicalFile.DIRECTORY:LogicalFile.FILE))
								.key("_links").object()
	                            	.key("self").object()
	                            		.key("href").value(getPublicLink(remoteSystem, absChildPath))
	                            	.endObject()
	                            	.key("system").object()
	                            		.key("href").value(remoteSystem.getPublicLink())
	                            	.endObject()
//	        	                	.key("metadata").object()
//		    	            			.key("href").value(getMetadataLink(remoteSystem, relativePath))
//		    	            		.endObject()
	                            .endObject()
							.endObject();
	                	}
					}
                }
	                
				return new AgaveSuccessRepresentation(writer.endArray().toString());
			} 
            catch (ResourceException e) {
            	throw e;
            }
            catch (Exception e) 
            {
				throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
						"Failed to retrieve file listing for " + path, e);
			}
		}
		finally 
		{
			try { remoteDataClient.disconnect(); } catch (Exception e) {}
			this.remoteDataClient = null;
        }
	}
	
	public long getLength(String len) {
		long length = 0;
		try {
			length = Long.parseLong(len);
		} catch (Exception e) {}
		return length;
	}
	
}