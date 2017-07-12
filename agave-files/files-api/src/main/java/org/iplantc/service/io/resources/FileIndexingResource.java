/**
 *
 */
package org.iplantc.service.io.resources;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.common.clients.AgaveLogServiceClient;
import org.iplantc.service.common.exceptions.PermissionException;
import org.iplantc.service.common.representation.AgaveRepresentation;
import org.iplantc.service.common.representation.AgaveSuccessRepresentation;
import org.iplantc.service.io.dao.LogicalFileDao;
import org.iplantc.service.io.model.FileEvent;
import org.iplantc.service.io.model.LogicalFile;
import org.iplantc.service.io.model.enumerations.FileEventType;
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
import org.iplantc.service.transfer.model.RemoteFilePermission;
import org.iplantc.service.transfer.model.enumerations.PermissionType;
import org.joda.time.DateTime;
import org.json.JSONStringer;
import org.json.JSONWriter;
import org.restlet.Request;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;

/**
 * Class to handle async indexing of files for bulk discovery and assignment of
 * uuid.
 *
 * @author dooley
 *
 */
public class FileIndexingResource extends AbstractFileResource {
	private static final Logger log = Logger
			.getLogger(FileIndexingResource.class);

	private String username;
	private String internalUsername;
	private String owner;
	private String path;
	private String systemId;
	private RemoteSystem remoteSystem;
	private RemoteDataClient remoteDataClient;

	@Override
	public void doInit() {

		this.username = getAuthenticatedUsername();
		this.systemId = (String) Request.getCurrent().getAttributes()
				.get("systemId");
		this.internalUsername = getInternalUsername();
		SystemManager sysManager = new SystemManager();
		SystemDao sysDao = new SystemDao();

		// Instantiate remote data client to the correct system

		try {
			if (systemId != null) {
				remoteSystem = sysDao.findUserSystemBySystemId(username,
						systemId);
			} else {
				// If no system specified, select the user default system
				remoteSystem = sysManager.getUserDefaultStorageSystem(username);
			}
		} catch (SystemException e) {
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL, e);
		} catch (Throwable e) {
			log.error("Failed to connect to remote system", e);
			// is this needed?
			try {
				remoteDataClient.disconnect();
			} catch (Exception e1) {
			}

		}

		AgaveLogServiceClient.log(
				AgaveLogServiceClient.ServiceKeys.FILES02.name(),
				AgaveLogServiceClient.ActivityKeys.IOIndex.name(), username,
				"", getRequest().getClientInfo().getUpstreamAddress());
	}

	/**
	 * Same as the {@link #represent} method for webhook support.
	 *
	 * @return {@link AgaveRepresentation} of the indexed directory
	 */
	@Post
	public Representation accept() throws ResourceException {
		return represent();
	}

	/**
	 * This method represents the HTTP GET action. The input files for the
	 * authenticated user are retrieved from the database and sent to the user
	 * as a {@link org.json.JSONArray JSONArray} of {@link org.json.JSONObject
	 * JSONObject}.
	 *
	 * @return {@link AgaveRepresentation} of the indexed directory
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
			catch(RemoteDataException e) {
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
			try {
				logicalFile=LogicalFileDao.findBySystemAndPath(remoteSystem, remoteDataClient.resolvePath(path));
			} catch(Exception e) {}

			PermissionManager pm = new PermissionManager(remoteSystem, remoteDataClient, logicalFile, username);

			// check for the file on disk
			boolean exists = false;
			RemoteFileInfo remoteFileInfo = null;
			long startTime = 0;
			try {
		        remoteDataClient.authenticate();
		        exists = remoteDataClient.doesExist(path);
	
		        if (!exists && logicalFile == null) {
		            throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
						"File/folder does not exist");
		        } else {
	
		        	startTime = System.currentTimeMillis();
	
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
		                logicalFile = new LogicalFile();
		        		logicalFile.setNativeFormat(remoteFileInfo.isFile() ? LogicalFile.RAW : LogicalFile.DIRECTORY);
		                logicalFile.setOwner(StringUtils.isEmpty(owner) ? username : owner);
		                logicalFile.setSystem(remoteSystem);
		                logicalFile.setPath(remoteDataClient.resolvePath(path));
		                logicalFile.setName(FilenameUtils.getName(logicalFile.getPath()));
		                logicalFile.setSourceUri("agave://" + remoteSystem.getSystemId() + "/" + logicalFile.getAgaveRelativePathFromAbsolutePath());
	
		                logicalFile.setStatus(StagingTaskStatus.STAGING_COMPLETED);
		                logicalFile.setInternalUsername(internalUsername);
		                LogicalFileDao.persist(logicalFile);
		            }
		        }
			} 
			catch (ResourceException e) {
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
	//          String relativePath = path + (fileName.equals(".") ? "" : File.separator + fileName);
	
				String absPath = StringUtils.removeStart(logicalFile.getPath(), remoteSystem.getStorageConfig().getRootDir());
				absPath = StringUtils.removeEnd(absPath, "/");
				
				// Avoid multiple leading slashes.
				if ((absPath != null) && !absPath.startsWith("/")) {  
					absPath = "/" + absPath;
				}
	          
				// adjust the offset and print the path element only if offset is zero
				int theLimit = getLimit();
				int theOffset = getOffset();
				if (theOffset == 0)
				{
					theLimit--;
	
					writer.array().object()
						.key("name").value(fileName)
						.key("path").value(path)
						.key("lastModified").value(new DateTime(remoteFileInfo.getLastModified()).toString())
						.key("length").value(remoteFileInfo.getSize())
						.key("permissions").value(sPermission)
						.key("format").value(format);
					if (isDirectory) {
						writer.key("mimeType").value("text/directory")
							.key("type").value(LogicalFile.DIRECTORY);
					} 
					else {
						writer.key("mimeType").value(resolveMimeTime(fileName))
							.key("type").value(LogicalFile.FILE);
					}
					writer.key("system").value(remoteSystem.getSystemId())
	              	  	.key("uuid").value(logicalFile.getUuid())
	                	.key("_links").object()
            			.key("self").object()
        					.key("href").value(logicalFile.getPublicLink())
        				.endObject()
	                	.key("system").object()
                			.key("href").value(remoteSystem.getPublicLink())
	                	.endObject()
	                	.key("profile").object()
	                		.key("href").value(logicalFile.getOwnerLink())
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
				else {
					theOffset--;
				}
	
				if (remoteFileInfo.isDirectory())
				{
					absPath = StringUtils.equals(absPath, "/") ? absPath : absPath + "/";
	        	
					listing = remoteDataClient.ls(path);
	            
					for (int i=theOffset; i< Math.min((theLimit + theOffset), listing.size()); i++)
					{
						RemoteFileInfo childFileInfo = listing.get(i); 
		                
		            	if (childFileInfo.getName().equals("..") || childFileInfo.getName().equals(".")) {
		            		continue;
		            	}
		            	else
		            	{
		            		RemoteFilePermission childPem = null;
		            		LogicalFile child = null;
		            		String childRemoteAbsolutePath = remoteDataClient.resolvePath(path + "/" + childFileInfo.getName());
		              		try {
		              			child=LogicalFileDao.findBySystemAndPath(remoteSystem, childRemoteAbsolutePath);
		            		} catch(Exception e) {}
	
			              	String childFileName = FilenameUtils.getName(childFileInfo.getName());
			                
		                	if (child == null) {
		              			PermissionManager childPm = new PermissionManager(remoteSystem, remoteDataClient, null, username);
		
			              		child = new LogicalFile();
			              		child.setNativeFormat(remoteFileInfo.isFile() ? LogicalFile.RAW : LogicalFile.DIRECTORY);
			              		child.setPath(childRemoteAbsolutePath);
		                		child.setName(childFileName);
		                		child.setSystem(remoteSystem);
		                		child.setStatus(StagingTaskStatus.STAGING_COMPLETED);
		                		child.setInternalUsername(internalUsername);
		
		                		childPem = childPm.getUserPermission(child.getPath());
			                	if (childPem.getPermission() == PermissionType.ALL || childPem.getPermission() == PermissionType.OWNER) {
			                        	child.setOwner(username);
		                        } 
			                	else if (remoteSystem.isPubliclyAvailable()) {
		                        	if (childPm.isUnderUserHomeDirOnPublicSystem(child.getPath())) {
		                        		child.setOwner(PathResolver.getImpliedOwnerFromSystemPath(child.getPath(), remoteSystem, remoteDataClient));
		                        	}
		                        	else {
		                        		child.setOwner(remoteSystem.getOwner());
		                        	}
			              		} 
		                        else {
			              			child.setOwner(remoteSystem.getOwner());
			              		}
		                        child.setSourceUri("agave://" + remoteSystem.getSystemId() + "/" + child.getAgaveRelativePathFromAbsolutePath());
		
		                        LogicalFileDao.persist(child);
		                	}
		                	else {
			              		PermissionManager childPm = new PermissionManager(remoteSystem, remoteDataClient, child, username);
			                    childPem = childPm.getUserPermission(child.getPath());
			              	}
	
		                	writer.object()
								.key("name").value(child.getName())
								.key("path").value(child.getAgaveRelativePathFromAbsolutePath())
								.key("lastModified").value(new DateTime(childFileInfo.getLastModified()).toString())
								.key("length").value(childFileInfo.getSize())
								.key("permissions").value(childPem.getPermission().name())
								.key("format").value(child.isDirectory() ? "folder" : child.getNativeFormat());
	
		                		if (childFileInfo.isDirectory()) {
									writer.key("mimeType").value("text/directory")
									.key("type").value(LogicalFile.DIRECTORY);
									
								} else {
									writer.key("mimeType").value(resolveMimeTime(childFileName))
									  .key("type").value(LogicalFile.FILE);
								}
	
		                		writer.key("uuid").value(child.getUuid())
			              			.key("system").value(remoteSystem.getSystemId())
									.key("_links").object()
			                        	.key("self").object()
			                        		.key("href").value(child.getPublicLink())
			                        	.endObject()
			                        	.key("system").object()
			                        		.key("href").value(remoteSystem.getPublicLink())
			                        	.endObject()
					                	.key("profile").object()
		                						.key("href").value(logicalFile.getOwnerLink())
		                				.endObject()
		                			.endObject()
								.endObject();
		            	}              
					}
				}
	          
				writer.endArray();
	
				if (logicalFile != null) {
					logicalFile.addContentEvent(new FileEvent(FileEventType.INDEX_COMPLETE,
							"Indexing completed in " + (System.currentTimeMillis() - startTime) + "ms",
							username));
				}
	
				return new AgaveSuccessRepresentation(writer.toString());
			}
			catch (Throwable e) {
				if (logicalFile != null) {
					logicalFile.addContentEvent(new FileEvent(FileEventType.INDEX_FAILED,
	      				"Indexing failed after " + (System.currentTimeMillis() - startTime) + "ms",
	      				username));
				}
	
				throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
							"Failed to perform index on " + path, e);
			}
		}
        finally {
    	    try { remoteDataClient.disconnect(); } catch (Exception e) {}
        }
	}
}