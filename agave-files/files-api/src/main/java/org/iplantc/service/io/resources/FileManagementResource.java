/**
 *
 */
package org.iplantc.service.io.resources;

import static org.iplantc.service.io.model.enumerations.FileOperationType.COPY;
import static org.iplantc.service.io.model.enumerations.FileOperationType.MKDIR;
import static org.iplantc.service.io.model.enumerations.FileOperationType.MOVE;
import static org.iplantc.service.io.model.enumerations.FileOperationType.RENAME;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.net.URI;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.iplantc.service.common.clients.AgaveLogServiceClient;
import org.iplantc.service.common.exceptions.PermissionException;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.common.representation.AgaveErrorRepresentation;
import org.iplantc.service.common.representation.AgaveSuccessRepresentation;
import org.iplantc.service.data.transform.FileTransform;
import org.iplantc.service.data.transform.FileTransformProperties;
import org.iplantc.service.io.Settings;
import org.iplantc.service.io.dao.LogicalFileDao;
import org.iplantc.service.io.dao.QueueTaskDao;
import org.iplantc.service.io.exceptions.FileProcessingException;
import org.iplantc.service.io.exceptions.TaskException;
import org.iplantc.service.io.exceptions.TransformException;
import org.iplantc.service.io.manager.FileEventProcessor;
import org.iplantc.service.io.manager.LogicalFileManager;
import org.iplantc.service.io.model.FileEvent;
import org.iplantc.service.io.model.LogicalFile;
import org.iplantc.service.io.model.enumerations.FileEventType;
import org.iplantc.service.io.model.enumerations.FileOperationType;
import org.iplantc.service.io.model.enumerations.StagingTaskStatus;
import org.iplantc.service.io.permissions.PermissionManager;
import org.iplantc.service.io.queue.UploadJob;
import org.iplantc.service.io.util.PathResolver;
import org.iplantc.service.io.util.ServiceUtils;
import org.iplantc.service.notification.model.Notification;
import org.iplantc.service.notification.util.EntityWithNotificationReferenceSerializer;
import org.iplantc.service.systems.dao.SystemDao;
import org.iplantc.service.systems.exceptions.RemoteCredentialException;
import org.iplantc.service.systems.exceptions.SystemException;
import org.iplantc.service.systems.exceptions.SystemUnavailableException;
import org.iplantc.service.systems.exceptions.SystemUnknownException;
import org.iplantc.service.systems.manager.SystemManager;
import org.iplantc.service.systems.model.RemoteSystem;
import org.iplantc.service.systems.model.enumerations.SystemStatusType;
import org.iplantc.service.transfer.RemoteDataClient;
import org.iplantc.service.transfer.RemoteDataClientFactory;
import org.iplantc.service.transfer.RemoteFileInfo;
import org.iplantc.service.transfer.exceptions.RemoteDataException;
import org.json.JSONException;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.quartz.impl.StdSchedulerFactory;
import org.restlet.Request;
import org.restlet.data.CharacterSet;
import org.restlet.data.ClientInfo;
import org.restlet.data.Disposition;
import org.restlet.data.MediaType;
import org.restlet.data.Range;
import org.restlet.data.Status;
import org.restlet.ext.fileupload.RestletFileUpload;
import org.restlet.representation.EmptyRepresentation;
import org.restlet.representation.Representation;
import org.restlet.representation.WriterRepresentation;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.Put;
import org.restlet.resource.ResourceException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.io.Files;

/**
 * Class to handle get and post requests for jobs
 *
 * @author dooley
 *
 */
public class FileManagementResource extends AbstractFileResource
{
	private static final Logger log = Logger.getLogger(FileManagementResource.class);

//	private static final String RENAME = "rename";
//	private static final String MKDIR = "mkdir";
//	private static final String COPY = "copy";
//	private static final String MOVE = "move";

	private String owner;  		// username listed at the root of the url path
	private String internalUsername;  		// username listed at the root of the url path
	private String systemId;
	private RemoteSystem system;

	private String username;	// authenticated user
	private String path;		// path of the file
	private List<Range> ranges = null;	// ranges of the file to return, given by byte index for start and a size.
	private SystemManager sysManager = null;

    private RemoteDataClient remoteDataClient;
    private SystemDao systemDao;
    
    @Override
	public void doInit() {
    	
    	try {
    		this.sysManager = new SystemManager();
	    	this.systemDao = new SystemDao();
	        
	        // the authenticated user
			this.username = getAuthenticatedUsername();
			
			// the internal user attached to the request
			this.internalUsername = getInternalUsername();
	
			// Get the requested file ranges
			this.ranges = getRanges();
	
	        //get system ID
	        this.systemId = getAttribute("systemId");
	
	        // Validate system being requested
	        this.system = resolveRemoteSystemFromURL(this.systemId, this.username);
	        
	        // Instantiate remote data client to the correct system
	        this.remoteDataClient = initRemoteDataClientForSystem(this.system, this.internalUsername);
	        
	        this.path = resolveRequestedSystemPathFromUrl();
	        
	        this.owner = resolveResourceOwnerFromPathIfPublicSystem();
	        		
	        // Specify that range requests are accepted
	        getResponse().getServerInfo().setAcceptingRanges(true);
    	
		} 
    	catch (ResourceException e) {
    		try {remoteDataClient.disconnect();} catch (Exception e1) {}
    		setStatus(e.getStatus());
    		getResponse().setEntity(new AgaveErrorRepresentation(e.getMessage()));
    		throw e;
    	}
    	catch (Throwable e) {
    		try {remoteDataClient.disconnect();} catch (Exception e1) {}
		   	setStatus(Status.SERVER_ERROR_INTERNAL);
	   		getResponse().setEntity(new AgaveErrorRepresentation("Unexpected error while processing this request"));
	   		throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
				   "Unexpected error while processing this request",e);
    	}
    }
    
    /**
     * On public storage systems, the first token of any relative path will
     * indicate an implied ownership permission for the resource. 
     * @return
     */
    private String resolveResourceOwnerFromPathIfPublicSystem() {
    	try {
    		String owner = null;
    		if (system.isPubliclyAvailable()) {
    			String originalPath = getRequest().getOriginalRef().toUri().getPath();
    			owner = PathResolver.getOwner(originalPath);
    		}
    		return owner;
		} catch (IOException e) {
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
					"Invalid file path " + path, e);
		}
	}

	/**
     * Parses the request URL and returns the relative or absolute path reference 
     * by this request. The leading slash is stripped on relative paths. 
     * 
     * @return
     */
    private String resolveRequestedSystemPathFromUrl() {
    	try {
    		String originalPath = getRequest().getOriginalRef().toUri().getPath();
			return PathResolver.resolve(owner, originalPath);
		} catch (Exception e) {
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
					"Invalid file path " + path, e);
		}
	}
    
    

	/**
     * Fetches a {@link RemoteDataClient} for the requested system and authenticates
     * for use in the remainder of this session.
     * @param remoteSystem
     * @param internalUsername
     * @return
     * @throws ResourceException
     */
    private RemoteDataClient initRemoteDataClientForSystem(RemoteSystem remoteSystem, String internalUsername) 
    throws ResourceException 
    {
    	try
        {
    		RemoteDataClient rdc = null;
    		if (remoteSystem != null) {
    			
    			// get a valid client
    			rdc = new RemoteDataClientFactory().getInstance(
                    remoteSystem, internalUsername);
    			
    			// authenticate for the remaining request.
    			rdc.authenticate();
    			
    			return rdc;
	        }
    		else {
    			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
    					"No system found to satisfy the request.", 
    					new SystemUnknownException());
    		}
		} 
		catch (RemoteDataException e) {
        	throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
					"Failed to connect to the remote system. "+ e.getMessage(), e);
		} catch (RemoteCredentialException | IOException e) {
			throw new ResourceException(Status.SERVER_ERROR_BAD_GATEWAY,
					"Failed to authenticate to the remote system. " + e.getMessage(), e);
		}
    }

	/**
	 * 
	 * @param systemId
	 * @param username
	 * @return
	 */
	private RemoteSystem resolveRemoteSystemFromURL(String systemId, String username) {
		try
        {
			RemoteSystem system = null;
            if (StringUtils.isNotEmpty(systemId)) {
            	system = systemDao.findUserSystemBySystemId(username, systemId);
            	
            	if (system == null) {
            		throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
							"No system found for user with id " + systemId,
							new SystemUnknownException());
            	}
            } 
            else { 
                // If no system specified, select the user default system
            	system = sysManager.getUserDefaultStorageSystem(username);
            	
            	if (system == null) {
            		throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
							"No default storage system found. Please register a system " +
							"and set it as your default.",
							new SystemUnknownException());
            	}
            }
            
            if (!system.isAvailable()) {
            	throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
						"System " + systemId + " is not currently available.",
						new SystemUnavailableException());
            }
            else if (system.getStatus() != SystemStatusType.UP) {
            	throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
            			"System " + systemId + " is " + system.getStatus().name(),
            			new SystemUnavailableException());
            }
            
            return system;
        }
		catch (ResourceException e) {
			throw e;
		}
        catch (Exception e) {
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL, 
            		"Failed to fetch user system", 
            		new SystemException());
        }
	}

	/* (non-Javadoc)
	 * @see org.restlet.Handler#handleHead()
	 */
	@Override
	public Representation head() {
		AgaveLogServiceClient.log(AgaveLogServiceClient.ServiceKeys.FILES02.name(),
				AgaveLogServiceClient.ActivityKeys.IOList.name(),
				username, "", getRequest().getClientInfo().getUpstreamAddress());
		
		try {
			
			// make sure the resource they are looking for is available.
	        if (remoteDataClient == null) {
	        	if (StringUtils.isEmpty(systemId)) {
	        		throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
	        				"No default storage system found. Please register a system and set it as your default. ");
	        	} else {
	        		throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
	        				"No system found for user with system id " + systemId);
	        	}
	        }
	
	        try {
	        	String originalPath = getRequest().getOriginalRef().toUri().getPath();
				path = PathResolver.resolve(owner, originalPath);
				if (system.isPubliclyAvailable()) {
					owner = PathResolver.getOwner(originalPath);
				}
			} catch (Exception e) {
				throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Invalid file path", e);
			}

		
	        LogicalFile logicalFile = null;
			RemoteFileInfo remoteFileInfo = null;
			try
			{
				remoteDataClient.authenticate();
				
				logicalFile = LogicalFileDao.findBySystemAndPath(system, remoteDataClient.resolvePath(path));

				PermissionManager pm = new PermissionManager(system, remoteDataClient, logicalFile, username);
				
                // check for file on disk
                if (!remoteDataClient.doesExist(path)) {
                    // if it doesn't exist, it was deleted outside of the api,
                    // so clean up the file entry in the db and its permissions
                    if (logicalFile != null) {
                        pm.clearPermissions(false);
                        //SharePermissionDao.removeAllPermissionsForPathSubtree(path);
                        logicalFile.addContentEvent(new FileEvent(FileEventType.DELETED, 
                        		"File or directory deleted outside of API", 
								getAuthenticatedUsername()));
						LogicalFileDao.removeSubtree(logicalFile);
                    }

                    throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
    						"File/folder does not exist");

                } else if (logicalFile.isDirectory()) { // return file listing

                	throw new ResourceException(Status.SERVER_ERROR_NOT_IMPLEMENTED,
                			"Directory downloads not supported");
                } else {
                    // file exists on the file system, so make sure we have
                    // a logical file for it if not, add one
                	remoteFileInfo = remoteDataClient.getFileInfo(path);
    				
                    try {
                        if (!pm.canRead(remoteDataClient.resolvePath(path))) {
                        	throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
                        			"User does not have access to view the requested resource");
                        }
                    } catch (PermissionException e) {
                    	throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
                    			"Failed to retrieve permissions for " + path + ", " + e.getMessage());
                    }

                    if (ranges.size() > 0) {
                        Range range = ranges.get(0);

                        if (range.getSize() < 0 && range.getSize() != -1) {
                        	throw new ResourceException(
                        			new Status(416, "Requested Range Not Satisfiable",
                        					"Upper bound less than lower bound",
                        					"http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html"),
                        			"Specified Range upper bound less than lower bound");
                        }

                        if (range.getIndex() > remoteFileInfo.getSize()) {
                        	throw new ResourceException(
                            		new Status(416, "Requested Range Not Satisfiable",
                            				"Lower bound out of range of file",
                            				"http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html"),
                        			"Specified Range lower bound outside bounds of file");
                        }

                        if ((range.getIndex() + Math.abs(range.getSize())) > remoteFileInfo.getSize()) {
                            throw new ResourceException(
                            		new Status(416, "Requested Range Not Satisfiable",
		                            		"Upper bound out of range of file",
		                            		"http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html"),
                            		"Specified Range upper bound outside bounds of file");
                        }
                    }
                }
            } catch (ResourceException e) {
				throw e;
            } catch (Exception e) {
            	log.error("Failed to fetch info for HEAD response of " + path, e);
            	throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
            			"Failed to retrieve information for " + path);
            }

			Disposition disposition = new Disposition();
			disposition.setModificationDate(remoteFileInfo.getLastModified());
			disposition.setFilename(logicalFile.getName());
			disposition.setType(Disposition.TYPE_INLINE);
			if (isForceDownload()) { 
				disposition.setType(Disposition.TYPE_ATTACHMENT);
			}
			
			final Representation wrep = new EmptyRepresentation();
			wrep.setDisposition(disposition);
			wrep.setSize(remoteFileInfo.getSize());
			wrep.setMediaType(MediaType.valueOf(resolveMimeTime(remoteFileInfo.getName())));
			wrep.setModificationDate(remoteFileInfo.getLastModified());
			
			// Only supporting the first range specified for each GET request
			if (!ranges.isEmpty())
				wrep.setRange(ranges.get(0));
			
			return wrep;
		}
		catch (ResourceException e) {
			setStatus(e.getStatus());
			return new AgaveErrorRepresentation(e.getMessage());
		}
		catch (Exception e) {
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL, e.getMessage(), e);
        } finally {
        	try {remoteDataClient.disconnect();} catch (Exception e) {}
        }
    }

	/**
	 * This method represents the HTTP GET action. Using the file id from the URL, the
	 * input file is streamed to the user from the local cache. If the file id is invalid
	 * for any reason, a HTTP {@link org.restlet.data.Status#CLIENT_ERROR_BAD_REQUEST 400} code is sent.
	 */
	@Get
	public Representation get() throws ResourceException
	{
		AgaveLogServiceClient.log(AgaveLogServiceClient.ServiceKeys.FILES02.name(),
				AgaveLogServiceClient.ActivityKeys.IODownload.name(),
				username, "", getRequest().getClientInfo().getUpstreamAddress());

		// make sure the resource they are looking for is available.
        if (remoteDataClient == null) {
        	if (StringUtils.isEmpty(systemId)) {
        		throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
        				"No default storage system found. Please register a system and set it as your default. ");
        	} else {
        		throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
        				"No system found for user with system id " + systemId);
        	}
        }

        try {
        	String originalPath = getRequest().getOriginalRef().toUri().getPath();
			path = PathResolver.resolve(owner, originalPath);
			if (system.isPubliclyAvailable()) {
				owner = PathResolver.getOwner(originalPath);
			}
		} catch (Exception e) {
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Invalid file path", e);
		}

		try
		{
			LogicalFile logicalFile = null;
			RemoteFileInfo remoteFileInfo = null;
			try
			{
				remoteDataClient.authenticate();

				logicalFile = LogicalFileDao.findBySystemAndPath(system, remoteDataClient.resolvePath(path));

				PermissionManager pm = new PermissionManager(system, remoteDataClient, logicalFile, username);
				
                // check for file on disk
                if (!remoteDataClient.doesExist(path)) {
                    // if it doesn't exist, it was deleted outside of the api,
                    // so clean up the file entry in the db and its permissions
                    if (logicalFile != null) {
                        pm.clearPermissions(false);
                        //SharePermissionDao.removeAllPermissionsForPathSubtree(path);
                        logicalFile.addContentEvent(new FileEvent(FileEventType.DELETED, 
                        		"File or directory deleted outside of API", 
								getAuthenticatedUsername()));
						LogicalFileDao.removeSubtree(logicalFile);
                    }

                    throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
    						"File/folder does not exist",
    						new FileNotFoundException());

                } else if (remoteDataClient.isDirectory(path)) { // return file listing

                	throw new ResourceException(Status.SERVER_ERROR_NOT_IMPLEMENTED,
                			"Directory downloads not supported", 
                			new NotImplementedException());
                } else {
                	
                	remoteFileInfo = remoteDataClient.getFileInfo(path);
                    // file exists on the file system, so make sure we have
                    // a logical file for it if not, add one
                    try {
                        if (!pm.canRead(remoteDataClient.resolvePath(path))) {
                        	throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
                        			"User does not have access to view the requested resource",
									new PermissionException());
                        }
                    } catch (PermissionException e) {
                    	throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
                    			"Failed to retrieve permissions for " + path + ", " + e.getMessage(), e);
                    }

                    if (ranges.size() > 0) {
                        Range range = ranges.get(0);

                        if (range.getSize() < 0 && range.getSize() != -1) {
                        	throw new ResourceException(
                        			new Status(416, "Requested Range Not Satisfiable",
                        					"Upper bound less than lower bound",
                        					"http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html"),
                        			"Specified Range upper bound less than lower bound");
                        }

                        if (range.getIndex() > remoteDataClient.length(path)) {
                        	throw new ResourceException(
                            		new Status(416, "Requested Range Not Satisfiable",
                            				"Lower bound out of range of file",
                            				"http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html"),
                        			"Specified Range lower bound outside bounds of file");
                        }

                        if ((range.getIndex() + Math.abs(range.getSize())) > remoteDataClient.length(path)) {
                            throw new ResourceException(
                            		new Status(416, "Requested Range Not Satisfiable",
		                            		"Upper bound out of range of file",
		                            		"http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html"),
                            		"Specified Range upper bound outside bounds of file");
                        }
                    }

                    // validate
                    if (logicalFile == null)
                    {
                    	if (owner != null && !owner.equals(username) && !ServiceUtils.isAdmin(username)) {
                        	throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
                        			"User does not have access to view the requested resource",
									new PermissionException());
                        }

                        logicalFile = new LogicalFile();
                        logicalFile.setSystem(system);
                        logicalFile.setName(new File(path).getName());
                        logicalFile.setNativeFormat(LogicalFile.RAW);
                        logicalFile.setOwner(StringUtils.isEmpty(owner) ? username : owner);
                        logicalFile.setSourceUri("http://" + getRequest().getClientInfo().getUpstreamAddress() + "/");
                        logicalFile.setPath(remoteDataClient.resolvePath(path));
                        logicalFile.setStatus(StagingTaskStatus.STAGING_COMPLETED);
                        logicalFile.setInternalUsername(internalUsername);
                        LogicalFileDao.persist(logicalFile);
                    }
                }
            } catch (ResourceException e) {
				throw e;
            } catch (Exception e) {
            	e.printStackTrace();
            	throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
            			"Failed to retrieve information for " + path,
    		    		new RemoteDataException());
            }
			
			FileEventProcessor.processAndSaveContentEvent(logicalFile, new FileEvent(
					FileEventType.DOWNLOAD,
					FileEventType.DOWNLOAD.getDescription(),
					Settings.PUBLIC_USER_USERNAME,
					TenancyHelper.getCurrentTenantId()));
			

            // stream the file to them
			MediaType mediaType = MediaType.valueOf(resolveMimeTime(remoteFileInfo.getName()));
			
			final String remotePath = path;
			final long fileSize = remoteDataClient.length(path);
			final WriterRepresentation wrep = new WriterRepresentation(mediaType, fileSize)
			{
				private InputStream in;
				private RemoteDataClient client = null;

				@Override
				public long getSize()
				{
					if (getRange() != null) {
						if (getRange().getSize() == -1) {
							return fileSize - getRange().getIndex();
						} else {
							return getRange().getSize();
						}
					} else {
						return fileSize;
					}
				}

				@Override
				public void write(OutputStream out) throws IOException
				{
                    try
                    {
                    	client = system.getRemoteDataClient(internalUsername);
                    	client.authenticate();
						in = client.getInputStream(remotePath, false);

						BufferedOutputStream bout = new BufferedOutputStream(out);

						// bufferSize defined as a variable for robust use with Range Requests
						int bufferSize = remoteDataClient.getMaxBufferSize();
						byte[] b = new byte[bufferSize];
						int len = 0;

						// If no range specified
						if (this.getRange() == null)
						{
							while ((len = in.read(b, 0, bufferSize)) > -1) {
								bout.write(b, 0, len);
							}
						} else {
							// Skip all input data prior to the index point.
							long skipped = in.skip(this.getRange().getIndex());

                            // This should never happen due to earlier bounds check
                            if (skipped < this.getRange().getIndex()) {
                                throw new IOException("Requested Range out of bounds");
                            }

							// Define a remaining number of bytes
							long bytesRemaining = getSize();

							// write a buffered number of bytes until all of the requested range of data is sent
							while (((len = in.read(b, 0, bufferSize)) > -1) && (bytesRemaining > 0)) {

								if (len > bytesRemaining)
									len = (int)bytesRemaining;

								bout.write(b, 0, len);

								// Reduce the remaining number of bytes by len
								bytesRemaining -= len;
							}

                            // This should never happen due to earlier bounds check
                            if (bytesRemaining > 0) {
                                throw new IOException("Requested Range out of bounds");
                            }

						}
						bout.flush();
                    }
                    catch (RemoteDataException e) {
						throw new IOException(e);
					}
                    catch (IOException e) {
                    	throw e;
                    }
					catch (Exception e)
					{
						throw new IOException(e);
					}
                    finally
                    {
						try { remoteDataClient.disconnect(); } catch (Exception e) {}
						try { in.close(); } catch (Exception e) {}
						try { client.disconnect(); } catch (Exception e) {}
					}
				}

				@SuppressWarnings("unused")
				@Override
				public void write(Writer writer) throws IOException {

					if (true) throw new IOException("Too slow to use");

					try
                    {
                    	// Not sure whether to set input stream to passive or not
                    	client = system.getRemoteDataClient(internalUsername);
                    	client.authenticate();
						in = client.getInputStream(remotePath, false);

						int bufferSize = 65536;
						byte[] b = new byte[bufferSize];
						int len = 0;
						// If no range specified
						if (this.getRange() == null) {
							while ((len = in.read(b)) >= 0) {
								writer.write((new String(b)).toCharArray());
							}
						} else {
							// Skip all input data prior to the index point.
							long skipped = in.skip(this.getRange().getIndex());

                            // This should never happen due to earlier bounds check
                            if (skipped < this.getRange().getIndex()) {
                                throw new IOException("Requested Range out of bounds");
                            }

							int bytesRemaining = (int)this.getRange().getSize();

							// write a buffered number of bytes until all of the requested range of data is sent
							while (((len = in.read(b, 0, bufferSize)) >= 0) && (bytesRemaining > 0)) {

								// Do not send more than the requested data
								if (len > bytesRemaining)
									len = bytesRemaining;

								writer.write(new String(b), 0, len);

								// Reduce the remaining number of bytes by len
								bytesRemaining -= len;
							}

                            // This should never happen due to earlier bounds check
                            if (bytesRemaining > 0) {
                                throw new IOException("Requested Range out of bounds");
                            }
						}
						writer.flush();
						in.close();
                    }
                    catch (RemoteDataException e) {
						throw new IOException(e);
					}
                    catch (IOException e) {
                    	throw e;
                    }
					catch (Exception e)
					{
						throw new IOException(e);
					}
					finally {
						try { in.close(); } catch (Exception e) {}
						try { client.disconnect(); } catch (Exception e) {}
					}
				}
			};
			
			Disposition disposition = new Disposition();
			disposition.setModificationDate(remoteFileInfo.getLastModified());
			disposition.setFilename(logicalFile.getName());
			disposition.setType(Disposition.TYPE_INLINE);
			if (isForceDownload()) { 
				disposition.setType(Disposition.TYPE_ATTACHMENT);
			}
			
			wrep.setDisposition(disposition);
			wrep.setSize(remoteFileInfo.getSize());
			wrep.setMediaType(mediaType);
			wrep.setModificationDate(remoteFileInfo.getLastModified());
			
			// Only supporting the first range specified for each GET request
			if (!ranges.isEmpty())
				wrep.setRange(ranges.get(0));
			
			return wrep;
		}
		catch (ResourceException e) {
			setStatus(e.getStatus());
			return new AgaveErrorRepresentation(e.getMessage());
		}
		catch (Exception e) {
            throw new ResourceException(e);
        } 
		finally {
        	try {remoteDataClient.disconnect();} catch (Exception e) {}
        }

    }

	/**
	 * This method represents the HTTP POST action. Posting a file upload form
	 * to this service will cache a file on behalf of the authenticated
	 * user and submit it to the i/o processing queue. While this method does
	 * not return a value internally, a {@link org.json.JSONObject JSONObject} representation
	 * of the successfully uploaded file is written to the output stream. If the
	 * upload fails due to transport issue, "no file uploaded" is returned in the body
	 * of the response. If any other error occurs a HTTP {@link org.restlet.data.Status#CLIENT_ERROR_BAD_REQUEST 400} code will be
	 * sent.
	 * <p>
	 * For the file upload form, only multipart form data is accepted. The form
	 * should contain the following field:
	 * <p>
	 * <ul><li>format: the input file type. If not type is given, it's treated as raw data</li></ul>
	 * <ul><li>url: the url of the file.</li></ul>
	 * <ul><li>fileToUpload: the path on the user's system to the file (if uploading).</li></ul>
	 * </p>
	 */
	@Post
    public Representation add(Representation input)
	{
		AgaveLogServiceClient.log(AgaveLogServiceClient.ServiceKeys.FILES02.name(),
				AgaveLogServiceClient.ActivityKeys.IOUpload.name(),
				username, "", getRequest().getClientInfo().getUpstreamAddress());
		try {	
			try
			{
                if (!remoteDataClient.doesExist(path))
                {
                	throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
                    		"Destination path " + path + " does not exist.", 
                    		new FileNotFoundException("No such file or folder"));
//                    // user must be owner to create new folders
//                    if (!StringUtils.equals(owner, username) && !ServiceUtils.isAdmin(username)) {
//                        throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
//                        		"User does not have access to modify the requested resource");
//                    }
                }
                else if (!remoteDataClient.isDirectory(path)) {
                    throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
                    		"Destination path " + path + " is not a directory",
                    		new RemoteDataException("No such directory exists."));
                }
			} catch (RemoteDataException e) {
				throw e;
			} catch (ResourceException e) {
				throw e;
            } catch (Exception e) {
                throw new ResourceException(Status.SERVER_ERROR_INTERNAL, 
                		"Failed to retrieve information for " + path);
            }
			
			List<Notification> notifications = null;
			
			// if the request contains a body
            if (input != null)
            {	
                // process multipart form data
	            if (MediaType.MULTIPART_FORM_DATA.equals(input.getMediaType(), true))
	            {
	            	// always use utf-8
	            	input.setCharacterSet(CharacterSet.UTF_8);
	            	
	            	DiskFileItemFactory factory = new DiskFileItemFactory();
	            	factory.setSizeThreshold((int)(Math.pow(2, 20) * 5));
	            	RestletFileUpload upload = new RestletFileUpload(factory);

	                List<FileItem> items;
					try {
						items = upload.parseRequest(getRequest());
					} catch (Exception e) {
						log.error("Failed to parse request form",e);
						throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
								"Failed to parse request form",
								new FileProcessingException());
					}

	                String format = null;
	                String sNotifications = null;
	                String name = null;
	                String sUri = null;
	                for (FileItem item: items) {
	                	if (item.getFieldName().equalsIgnoreCase("fileType")) {
	                		format = item.getString();
	                	} 
	                	else if (item.getFieldName().equalsIgnoreCase("callbackURL")) {
	                		sNotifications = item.getString();
	                	}
	                	else if (item.getFieldName().equalsIgnoreCase("callbackUrl")) {
	                		sNotifications = item.getString();
	                	}
	                	else if (item.getFieldName().equalsIgnoreCase("notifications")) {
	                		sNotifications = item.getString();
	                	} 
	                	else if (item.getFieldName().equalsIgnoreCase("fileName")) {
	                		name = item.getString();
	                	} 
	                	else if (item.getFieldName().equalsIgnoreCase("urlToIngest")) {
	                		sUri = item.getString();
	                	}
	                }

					// enable the user to specify the input formatting. Check against our internally
					// supported formatting
					// ignore user-defined formatting for now. Let the service auto-discover it
					FileTransformProperties transformProperties = new FileTransformProperties();
					FileTransform transform = null;
					if (!ServiceUtils.isValid(format)) {
						format = "raw";
					} else {
						// check against xml formats
						format = format.toLowerCase();
						try {
							transform = transformProperties.getTransform(format);

							if (transform == null) {
								if (format.equalsIgnoreCase("raw"))
									format = "raw";
								else
									throw new TransformException("Format " + format + " is not recognized");
							} else {
								format = transform.getId();
							}
						} catch (TransformException e) {
							format = "raw";
						}
					}


					// If a URI has been specified in the request, there will be no file upload
					if (StringUtils.isNotEmpty(sUri))
					{
						try {
							// resolve the uri
							URI uri = new URI(sUri);
							if (StringUtils.isEmpty(name)) {
								// pull the file name from the url
								name = FilenameUtils.getName(uri.getPath());

								// make sure the name is valid
								if (StringUtils.isEmpty(name)) {
									// TODO: we should either check the remote system to 
									// apply a name conflict policy or use the uuid of the
									// resulting logical file
									name = "unknown";
								}
							}

							name = name.trim();

							// files should overwrite. We can add an option to allow them to guarantee uniqueness,
							// but they should overwrite by default.

							String remotePath = null;
							if (StringUtils.isEmpty(path)) {
								remotePath = name;
				            } else {
				            	remotePath = path + File.separator + name;
				            }
				
				            remotePath = StringUtils.replace(remotePath, "//", "/");
				
							LogicalFile logicalFile = null;
				            try {
				            	logicalFile = LogicalFileDao.findBySystemAndPath(system, remoteDataClient.resolvePath(remotePath));;
				            } catch(Exception e) {}
				
				            PermissionManager pm = new PermissionManager(system, remoteDataClient, logicalFile, username);

							if (logicalFile == null) {

								if (!pm.canWrite(remoteDataClient.resolvePath(remotePath))) {
									throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
											"User does not have access to modify the requested resource",
											new PermissionException());
								}

								logicalFile = new LogicalFile();
								logicalFile.setSystem(system);
								logicalFile.setInternalUsername(internalUsername);
								logicalFile.setName(new File(remotePath).getName());
								logicalFile.setSourceUri(uri.toString());
								logicalFile.setNativeFormat(format);
								logicalFile.setPath(remoteDataClient.resolvePath(remotePath));
								logicalFile.setOwner(StringUtils.isEmpty(owner) ? username : owner);
								logicalFile.setStatus(StagingTaskStatus.STAGING_QUEUED);
								LogicalFileDao.persist(logicalFile);
							} 
							else {
								if (!pm.canWrite(logicalFile.getPath())) {
									throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
											"User does not have access to modify the requested resource", 
											new PermissionException());
								}
								logicalFile.setSystem(system);
								logicalFile.setInternalUsername(internalUsername);
								logicalFile.setNativeFormat(format);
								logicalFile.setSourceUri(uri.toString());
								logicalFile.setLastUpdated(new Date());
								logicalFile.setStatus(StagingTaskStatus.STAGING_QUEUED);
								LogicalFileDao.persist(logicalFile);
							}

							// set valid notifications for the upload if a single notification URL was provided. 
							if (StringUtils.isNotEmpty(sNotifications)) {
								notifications = LogicalFileManager.addUploadNotifications(logicalFile, username, sNotifications);
							}
							
							logicalFile.addContentEvent(FileEventType.STAGING_QUEUED, username);
							LogicalFileDao.persist(logicalFile);

							// add the logical file to the staging queue
		                    QueueTaskDao.enqueueStagingTask(logicalFile, username);

		                    setStatus(Status.SUCCESS_ACCEPTED);
		                    return new AgaveSuccessRepresentation(logicalFile.toJsonWithNotifications(notifications));
						} 
						catch (ResourceException e) {
							throw e;
						} 
						catch (Exception e) {
							throw new ResourceException(Status.SERVER_ERROR_INTERNAL, "Invalid name " + name);
						}
					}
					// without a URL to import, look for the file to upload
					else
					{
						LogicalFile logicalFile = null;
		                String remotePath = null;
		                try
		                {
		                	// Process only the uploaded item called "fileToUpload" and
		                    // save it on disk
		                    boolean found = false;
		                    for (final Iterator<FileItem> it = items.iterator(); it.hasNext() && !found;)
		                    {
		                        FileItem fi = it.next();
		                        if (fi.getFieldName().equals("fileToUpload"))
		                        {
		                            found = true;
		                            String fileName = null;
		                            if (ServiceUtils.isValid(name)) {
		                            	fileName = name;
		                            } else {
			                            fileName = fi.getName();
			                            if (fileName != null) {
			                            	fileName = FilenameUtils.getName(fileName);
			                            }
		                            }

		                            // I'm making a policy call here that POST will overwrite existing files of the same name
		    						// if they want to avoid it, use the name parameter in the post form.
		    						//fileName = IrodsUtils.resolveFileName(fileName);

		                            // this would be a great place to enforce user disk quota.
		                            if (StringUtils.isEmpty(path)) {
		                            	remotePath = fileName;
		                            } else {
		                            	remotePath = path + File.separator + fileName;
		                            }

		                            remotePath = StringUtils.replace(remotePath, "//", "/");


		                            Request request = getRequest();
		                            ClientInfo info = request.getClientInfo();
		                            String host = (info.getUpstreamAddress().contains(":")? "127.0.0.1":info.getUpstreamAddress());
		                            String tmpUrl = "http://" + host + "/" + fi.getName();

		                            logicalFile = null;

                                    try {
                                        logicalFile = LogicalFileDao.findBySystemAndPath(system, remoteDataClient.resolvePath(remotePath));
                                    } catch(Exception e) {
                                        // Just catches an exception when the LogicalFile does not exist.
                                        // If there is no LogicalFile, one will be created below.
                                    }

                                    PermissionManager pm = new PermissionManager(system, remoteDataClient, logicalFile, username);

		    						if (logicalFile == null) {
		    							if (!pm.canWrite(remoteDataClient.resolvePath(remotePath))) {
		    								throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
		    										"User does not have access to modify " + path,
		    										new PermissionException());
		    							}

		    							logicalFile = new LogicalFile();
		    							logicalFile.setSystem(system);
										logicalFile.setInternalUsername(internalUsername);
		    							logicalFile.setName(fileName);
		    							logicalFile.setSourceUri(tmpUrl);
		    							logicalFile.setPath(remoteDataClient.resolvePath(remotePath));
		    							logicalFile.setNativeFormat(format);
		    							logicalFile.setOwner(StringUtils.isEmpty(owner) ? username : owner);
		    							logicalFile.setStatus(StagingTaskStatus.STAGING_QUEUED);
		    							LogicalFileDao.persist(logicalFile);
		    						} 
		    						else {
		    							if (!pm.canWrite(logicalFile.getPath())) {
		    								throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
		    										"User does not have access to modify " + path,
		    										new PermissionException());
		    							}
		    							logicalFile.setSystem(system);
										logicalFile.setInternalUsername(internalUsername);
		    							logicalFile.setNativeFormat(format);
		    							logicalFile.setSourceUri(tmpUrl);
		    							logicalFile.setLastUpdated(new Date());
		    							logicalFile.setStatus(StagingTaskStatus.STAGING_QUEUED);
		    							LogicalFileDao.persist(logicalFile);
		    						}
		    						
		    						// set valid notifications for the upload if a single notification URL was provided. 
		    						if (StringUtils.isNotEmpty(sNotifications)) {
		    							notifications = LogicalFileManager.addUploadNotifications(logicalFile, username, sNotifications);
									}
									
									logicalFile.addContentEvent(FileEventType.STAGING_QUEUED, username);
									LogicalFileDao.persist(logicalFile);

                                    // Copy the contents of the FileItem to the remoteDataClient location.
		    						File cachedDir = Files.createTempDir();
		    						File cachedFile = new File(cachedDir, fileName);
		    						cachedFile.createNewFile();
		    						fi.write(cachedFile);
		    						log.debug("File upload of " + cachedFile.length() + " bytes received from " + getAuthenticatedUsername());
		    						
		    						// kick off a background task to stage the file so we can return the connection immediately
		    						JobDetail jobDetail = newJob(UploadJob.class)
                    					.withIdentity("cache-upload-" + logicalFile.getId() + "-" + System.currentTimeMillis(), "Staging upload")
                    					.usingJobData("logicalFileId", logicalFile.getId())
                    					.usingJobData("cachedFile", cachedFile.getAbsolutePath())
                    					.usingJobData("owner", StringUtils.isEmpty(owner) ? username : owner)
                    					.usingJobData("tenantId", TenancyHelper.getCurrentTenantId())
                    					.usingJobData("createdBy", username)
                    					.usingJobData("sourceUrl", tmpUrl)
                    					.usingJobData("destUrl", "agave://" + system.getSystemId() + "/" + logicalFile.getAgaveRelativePathFromAbsolutePath())
                    				    .usingJobData("isRangeCopyOperation", Boolean.toString(!ranges.isEmpty()))
            					        .usingJobData("rangeIndex", ranges.isEmpty() ? Long.valueOf(0) : ranges.get(0).getIndex())
            					        .usingJobData("rangeSize", ranges.isEmpty() ? Long.valueOf(-1) : ranges.get(0).getSize())
                    					.build();

									SimpleTrigger trigger = (SimpleTrigger)newTrigger()
										.withIdentity("trigger-" + logicalFile.getId() + "-" + System.currentTimeMillis(), "Upload")
										.startNow()
										.build();

									Scheduler sched = getUploadScheduler();
									if (!sched.isStarted()) {
                                	   sched.start();
									}
									sched.scheduleJob(jobDetail, trigger);

									break;
		                        }
		                    }

		                    // Once handled, the metadata of the uploaded file is sent back to the client.
		                    if (found) {
		                    	setStatus(Status.SUCCESS_ACCEPTED);
		                    	return new AgaveSuccessRepresentation(logicalFile.toJsonWithNotifications(notifications));
		                    }
		                    else
		                    {
		                        // Some problem occurs, sent back a simple line of text.
		                    	throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
		                        		"No file uploaded",
		            		    		new FileNotFoundException());
		                    }
		                }
		                catch (Exception e)
		                {
		                    // The message of all thrown exception is sent back to
		                    // client as simple plain text
		                	log.error("File upload failed",e);
		                    throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, e);
		                }
					}
	            }
	            else
	            {
	            	// parse the form to get the job specs
	            	JsonNode inputJson = getPostedContentAsJsonNode(input);
//					Map<String,String> pTable = form.getValuesMap();

					// enable the user to specify the input formatting. Check against our internally
					// supported formatting
					// ignore user-defined formatting for now. Let the service auto-discover it
	            	String format = null;
	            	if (inputJson.hasNonNull("fileType")) {
	            		format = inputJson.get("fileType").asText();
	            	}
					
					FileTransformProperties transformProperties = new FileTransformProperties();
					FileTransform transform = null;

					if (StringUtils.isEmpty(format)) {
						format = "raw";
					} 
					else {
						// check against xml formats
						format = format.toLowerCase();
						try {
							transform = transformProperties.getTransform(format);

							if (transform == null) {
								if (format.equalsIgnoreCase("raw"))
									format = "raw";
								else
									throw new TransformException("Format " + format + " is not recognized");
							} else {
								format = transform.getId();
							}
						} catch (TransformException e) {
							format = "raw";
						}
					}
					
					String sNotifications = null;
	            	if (inputJson.hasNonNull("notifications")) {
	            		if (inputJson.get("notifications").isValueNode()) {
	            			sNotifications = inputJson.get("notifications").asText();
	            		} else {
	            			sNotifications = inputJson.get("notifications").toString();
	            		}
	            	} else if (inputJson.has("callbackUrl")) {
	            		sNotifications = inputJson.get("callbackUrl").asText();
	            	} else if (inputJson.has("callbackURL")) {
	            		sNotifications = inputJson.get("callbackURL").asText();
	            	}
	            	

					String name = null;
					if (inputJson.hasNonNull("fileName")) {
						name = inputJson.get("fileName").asText();
					}
					
					String sUri = null;
					if (inputJson.hasNonNull("urlToIngest")) {
						sUri = inputJson.get("urlToIngest").asText();
					} else {
						throw new ResourceException(Status.SERVER_ERROR_INTERNAL, "No valid URL specified");
					}
					
					try
					{
						// resolve the uri
						URI uri = new URI(StringUtils.trim(sUri));
						if (StringUtils.isBlank(name)) {
							// pull the file name from the url
							name = FilenameUtils.getName(uri.getPath());

							// make sure the name is valid
							if (StringUtils.isEmpty(name)) {
								name = "unknown";
							}
						}

						name = StringUtils.trim(name);

						// create a reference to the uri
						String remotePath = null;
						if (StringUtils.isEmpty(path)) {
							remotePath = name;
						} else {
							remotePath = path + File.separator + name;
						}
					
						remotePath = StringUtils.replace(remotePath, "//", "/");
					
						LogicalFile logicalFile = null;
					
						try {
							logicalFile = LogicalFileDao.findBySystemAndPath(system, remoteDataClient.resolvePath(remotePath));
						} catch(Exception e) {}
					  
						PermissionManager pm = new PermissionManager(system, remoteDataClient, logicalFile, username);

						if (logicalFile == null) {

							if (!StringUtils.isEmpty(owner) && !owner.equals(username) && !ServiceUtils.isAdmin(username)) {
								throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
										"User does not have access to modify " + remotePath);
							}

							logicalFile = new LogicalFile();
							logicalFile.setSystem(system);
							logicalFile.setInternalUsername(internalUsername);
							logicalFile.setName(name);
							logicalFile.setSourceUri(uri.toString());
							logicalFile.setNativeFormat(format);
							logicalFile.setPath(remoteDataClient.resolvePath(remotePath));
							logicalFile.setOwner(StringUtils.isEmpty(owner) ? username : owner);
							logicalFile.setStatus(StagingTaskStatus.STAGING_QUEUED);
							LogicalFileDao.persist(logicalFile);
							
						} else {
							if (!pm.canRead(logicalFile.getPath())) {
								throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
										"User does not have access to modify " + path);
							}
							logicalFile.setSystem(system);
							logicalFile.setInternalUsername(internalUsername);
							logicalFile.setNativeFormat(format);
							logicalFile.setSourceUri(uri.toString());
							logicalFile.setLastUpdated(new Date());
							logicalFile.setStatus(StagingTaskStatus.STAGING_QUEUED);
							LogicalFileDao.persist(logicalFile);
						}

						// set valid notifications for the upload if a single notification URL was provided.
						if (StringUtils.isNotEmpty(sNotifications)) {
							notifications = LogicalFileManager.addUploadNotifications(logicalFile, username, sNotifications);
						}
						
						logicalFile.addContentEvent(FileEventType.STAGING_QUEUED, username);
						LogicalFileDao.persist(logicalFile);

						// add the logical file to the staging queue
						QueueTaskDao.enqueueStagingTask(logicalFile, username);

						setStatus(Status.SUCCESS_ACCEPTED);
						
						// append notifications to the hypermedia response 
						return new AgaveSuccessRepresentation(logicalFile.toJsonWithNotifications(notifications));
	                    
					} 
					catch (ResourceException e) {
						throw e;
					} 
					catch (Exception e) {
						throw new ResourceException(Status.SERVER_ERROR_INTERNAL, e.getMessage(), e);
					} 
					finally {
						try {remoteDataClient.disconnect();} catch (Exception e) {}
					}
	            }
	        } else {
	            // POST request with no entity.
	            throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "No post entity");
	        }
		}
		catch (ResourceException e) {
			setStatus(e.getStatus());
			return new AgaveErrorRepresentation(e.getMessage());
		}
		catch (Throwable e) {
			 throw new ResourceException(Status.SERVER_ERROR_INTERNAL, 
					 "Error occurred while importing file.", e);

		} finally {
			try {remoteDataClient.disconnect();} catch (Exception e) {}
        }
    }

	

	/**
	 * Fetches the custom scheduler needed to manage file upload tasks.
	 * @return
	 * @throws SchedulerException
	 */
	private Scheduler getUploadScheduler() throws SchedulerException {
	    StdSchedulerFactory schedulerFactory = new StdSchedulerFactory();
	    InputStream in = null;
	    Scheduler scheduler = null;
	    try {
	        in = getClass().getClassLoader().getResourceAsStream("quartz-httpupload.properties");
	        schedulerFactory.initialize(in);
	    }
	    catch (SchedulerException e) {
	        Properties props = new Properties();
	        props.put("org.quartz.scheduler.instanceName", "AgaveFileUploadScheduler");
	        props.put("org.quartz.threadPool.threadCount", "5");
	        props.put("org.quartz.jobStore.class", "org.quartz.simpl.RAMJobStore");
	        props.put("org.quartz.plugin.shutdownhook.class", "org.quartz.plugins.management.ShutdownHookPlugin");
	        props.put("org.quartz.plugin.shutdownhook.cleanShutdown", "true");
	        props.put("org.quartz.scheduler.skipUpdateCheck","true");
	        schedulerFactory.initialize(props);
	    }
	    finally {
	        try { in.close(); } catch (Exception e) {}
	    }
	    scheduler = schedulerFactory.getScheduler();
	    return scheduler;
    }

    /**
	 * Deletes a file or folder with provenance and alerts
	 *
	 */
	@Delete
	@Override
	public Representation delete()
	{
		AgaveLogServiceClient.log(AgaveLogServiceClient.ServiceKeys.FILES02.name(),
				AgaveLogServiceClient.ActivityKeys.IODelete.name(),
				username, "", getRequest().getClientInfo().getUpstreamAddress());

		try {
			LogicalFile logicalFile = null;
            try {
            	logicalFile = LogicalFileDao.findBySystemAndPath(system, remoteDataClient.resolvePath(path));
            }
            catch(Exception e){}

            PermissionManager pm = new PermissionManager(system, remoteDataClient, logicalFile, username);
            AgaveSuccessRepresentation representation = new AgaveSuccessRepresentation();
            if (logicalFile == null) {
                if (!pm.canWrite(remoteDataClient.resolvePath(path))) {
                    throw new ResourceException(Status.CLIENT_ERROR_UNAUTHORIZED, 
                    		"User does not have permission to delete " + path,
                    		new PermissionException());
                } else {
                    try {
                        remoteDataClient.delete(path);
                        if (representation.isNaked()) {
                        	setStatus(Status.SUCCESS_NO_CONTENT);
                        } else {
                        	setStatus(Status.SUCCESS_OK);
                        }
                        
                        return representation;
                    } 
                    catch (FileNotFoundException e) {
                    	setStatus(Status.CLIENT_ERROR_NOT_FOUND, "Remote path " + path + " does not exists");
                    	return new AgaveErrorRepresentation("Remote path " + path + " does not exists");
                    }
                    catch (Exception e) {
                    	log.error("Failed to delete file agave://" + systemId + "/" + path);
                    	throw new ResourceException(Status.SERVER_ERROR_INTERNAL, 
                        		"Failed to delete " + path + " on the remote system.", e);
                        
                    }
                }
            } else {

                try {
                    try {
                        if (!pm.canWrite(remoteDataClient.resolvePath(path))) {
                            throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN, 
                            		"User does not have permission to delete " + path,
                		    		new PermissionException());
                        }
                    } 
                    catch (PermissionException e) {
                        throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN, 
                        		"Failed to retrieve permissions for " + path + ", " + e.getMessage(), e);
                    }

                    remoteDataClient.delete(path);
                    
                    // this should cascade delete the permissions when the logical file deletes
                    logicalFile.addContentEvent(new FileEvent(FileEventType.DELETED, "Deleted via API", 
							getAuthenticatedUsername()));
                    
                    LogicalFileDao.removeSubtree(logicalFile);
                    
                    //SharePermissionDao.removeAllPermissionsForPathSubtree(path);
                    
                    if (representation.isNaked()) {
                    	setStatus(Status.SUCCESS_NO_CONTENT);
                    } else {
                    	setStatus(Status.SUCCESS_OK);
                    }
                    
                    return representation;
                    
                } 
                catch (ResourceException e) {
                	throw e;
                }
                catch (Throwable e) {
                	log.error("File deletion failed " + e.getMessage());
                	throw new ResourceException(Status.SERVER_ERROR_INTERNAL, 
                			"File deletion failed", e);
                    
                }
            }
        } 
		catch(ResourceException e) {
			throw e;
//			setStatus(new Status(e.getStatus(), e.getMessage()));
//			return new AgaveErrorRepresentation(e.getMessage());
		}
		catch (Exception e) {
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
            		"Failed to retrieve information for " + path, e);
//            }

        } finally {
        	try {remoteDataClient.disconnect();} catch (Exception e) {}
		}
	}
		
	/**
	 * Accepts mkdir, copy, move, touch, index, and rename functionality.
	 */
	@Put
	public Representation put(Representation input)
	throws ResourceException
	{
		try
		{
			// parse the form to get the job specs
			JsonNode jsonInput = getPostedContentAsJsonNode(input);
			
//			String agaveUrl = String.format("agave://%s/%s", system.getSystemId(), path);
			
			
			String absolutePath = null;
			try {
				absolutePath = remoteDataClient.resolvePath(path);
			} catch (FileNotFoundException e) {
				throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, 
						e.getMessage(), e);
			}

			LogicalFile logicalFile = LogicalFileDao.findBySystemAndPath(system, absolutePath);

            PermissionManager pm = new PermissionManager(system, remoteDataClient, logicalFile, username);

			try {
				if (!pm.canWrite(absolutePath)) {
					throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
							"User does not have access to modify the requested path " + path,
				    		new PermissionException());
				}
			} catch (PermissionException e) {
				throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
						"Failed to retrieve permissions for " + path + ", " + e.getMessage(), e);
			}

			try
			{
				if (logicalFile == null)
				{	RemoteFileInfo fileInfo = remoteDataClient.getFileInfo(path);
					logicalFile = new LogicalFile();
					logicalFile.setName(fileInfo.getName());
					logicalFile.setSourceUri("");
					logicalFile.setPath(absolutePath);
					logicalFile.setNativeFormat(fileInfo.isDirectory() ? LogicalFile.DIRECTORY : LogicalFile.RAW);
					logicalFile.setOwner(StringUtils.isEmpty(owner) ? username : owner);
					logicalFile.setSystem(system);
					logicalFile.setInternalUsername(internalUsername);
					logicalFile.setStatus(StagingTaskStatus.STAGING_COMPLETED);
				}
				
				
				// this is the action they're requesting be performed
				FileOperationType operation = null; 
				try {
					if (jsonInput.hasNonNull("action")) {
						operation = FileOperationType.valueOfIgnoreCase(jsonInput.get("action").asText());
					}
					else {
						throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
								"Please provide a file action to perform",
								new TaskException());
					}
				} 
				catch(ResourceException e) {
					throw e;
				}
				catch (Exception e) {
					throw new ResourceException(Status.SERVER_ERROR_NOT_IMPLEMENTED, 
							"Action " + operation + " not supported", e);
				}
				
				String message = "";

				if (operation == MKDIR)
				{
					return doMkdirOperation(jsonInput, logicalFile, pm);
				}
				else if (operation == RENAME)
				{
					return doRenameOperation(jsonInput, absolutePath, logicalFile, pm);
				}
				else if (operation == COPY)
				{
					return doCopyOperation(jsonInput, absolutePath, logicalFile, pm);
				}
				else if (operation == MOVE)
				{
					return doMoveOperation(jsonInput, logicalFile, pm);
				}
				else
				{
					throw new ResourceException(Status.SERVER_ERROR_NOT_IMPLEMENTED, "Action " + operation + " not supported");
				}				
			} 
			catch (FileNotFoundException e) {
				throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
						e.getMessage(), e);
			} 
            catch (IllegalArgumentException e) {
                throw new ResourceException(Status.CLIENT_ERROR_CONFLICT,
                        e.getMessage(), e);
            } 
			catch (ResourceException e) {
				throw e;
			} 
			catch (RemoteDataException e) {
				throw new ResourceException(Status.SERVER_ERROR_INTERNAL, e.getMessage(), e);
			} 
			catch (Exception e) {
				log.error("Error performing file operation",e);
				throw new ResourceException(Status.SERVER_ERROR_INTERNAL, 
						"File operation failed " + e.getMessage(), e);
			}
		}
		catch (ResourceException e) {
			setStatus(e.getStatus());
			return new AgaveErrorRepresentation(e.getMessage());
		}
		finally {
			try {remoteDataClient.disconnect();} catch (Exception e) {}
		}
	}

	/**
	 * @param jsonInput
	 * @param absolutePath
	 * @param logicalFile
	 * @param pm
	 * @return the new file item representation
	 * @throws ResourceException
	 * @throws PermissionException
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws RemoteDataException
	 * @throws HibernateException
	 * @throws JSONException 
	 */
	protected Representation doRenameOperation(JsonNode jsonInput, String absolutePath,
			LogicalFile logicalFile, PermissionManager pm)
			throws ResourceException, PermissionException,
			FileNotFoundException, IOException, RemoteDataException,
			HibernateException, JSONException {
		String message;
		AgaveLogServiceClient.log(AgaveLogServiceClient.ServiceKeys.FILES02.name(),
				AgaveLogServiceClient.ActivityKeys.IORename.name(),
				username, "", getRequest().getClientInfo().getUpstreamAddress());

		String newName = null;
		if (jsonInput.has("path")) {
			newName = jsonInput.get("path").textValue();
		}
		
		if (StringUtils.isEmpty(newName)) {
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
		    		"No 'path' value provided. Please provide the new name of "
							+ "the file or folder in the 'path' attribute.",
				    		new RemoteDataException());
		}

		String newPath = path;
		if (newPath.endsWith("/")) {
			newPath = StringUtils.removeEnd(newPath, "/");
		}

		if (StringUtils.isEmpty(newPath) || newPath.equals("/")) {
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "No path specified.");
		} 
		else {
			String currentTargetDirectory = org.codehaus.plexus.util.FileUtils.getPath(newPath);
			if (StringUtils.isEmpty(currentTargetDirectory)) {
				newPath = newName;
			} else {
				newPath = currentTargetDirectory + "/" + newName;
			}
		}

		// do they have permission to write to this new folder
		if (!pm.canWrite(remoteDataClient.resolvePath(newPath))) {
			throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
		    		"User does not have access to rename the requested resource",
		    		new PermissionException());
		}
		
		if (remoteDataClient.doesExist(newPath)) {
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
					"A File or folder at " + newPath + " already exists",
		    		new RemoteDataException());
		}

		// move is essentially just a rename on the same system
		remoteDataClient.doRename(path, newPath); 
		message = "Rename success";
		if (remoteDataClient.isPermissionMirroringRequired())
		{
			remoteDataClient.setOwnerPermission(remoteDataClient.getUsername(), newPath, true);

			String pemUser = StringUtils.isEmpty(owner) ? username : owner;
			try {
				remoteDataClient.setOwnerPermission(pemUser, newPath, true);
			} catch (Exception e) {
				log.error(e.getMessage(), e);
				message = "Rename was successful, but unable to mirror permissions for user " +
						pemUser + " on new directory " + path;
			}
		}

		// now keep the logical file up to date
		logicalFile.setPath(remoteDataClient.resolvePath(newPath));
		logicalFile.setName(FilenameUtils.getName(newPath));
		logicalFile.addContentEvent(new FileEvent(FileEventType.RENAME, 
				"Renamed by " + getAuthenticatedUsername() + " from " + path + " to " + newPath, 
				getAuthenticatedUsername()));
		LogicalFileDao.persist(logicalFile);
//					LogicalFileDao.moveSubtreePath(absolutePath, logicalFile.getSystem().getId(), remoteDataClient.resolvePath(newPath), logicalFile.getSystem().getId());

		if (logicalFile.isDirectory())
		{
			// we can delete any children under the destination root since they could
			// not actually exist if the rename operation worked.
			for (LogicalFile child: LogicalFileDao.findChildren(logicalFile.getPath(), system.getId())) {
				child.addContentEvent(new FileEvent(FileEventType.DELETED, 
						"Detected that file item was deleted by an outside source when"
						+ " as part of a rename operation on " + getPublicLink(system, path), 
						getAuthenticatedUsername()));
				LogicalFileDao.remove(child);
			}

			// we also need to replicate any children that were not copied over before
			List<LogicalFile> renamedChildren =
					LogicalFileDao.findChildren(logicalFile.getPath(), system.getId());

			for (LogicalFile child: renamedChildren) {
				child.setPath(StringUtils.replaceOnce(child.getPath(), absolutePath, logicalFile.getPath()));
				child.setLastUpdated(new Date());
				LogicalFileDao.persist(child);
				child.addContentEvent(new FileEvent(FileEventType.MOVED, 
						"File item moved from " + child.getPublicLink() +
						" as part of a rename operation on " + logicalFile.getPublicLink(), 
						getAuthenticatedUsername()));
				LogicalFileDao.persist(child);
			}
		}
		
		return new AgaveSuccessRepresentation(message, logicalFile.toJSON());
	}

	/**
	 * @param jsonInput
	 * @param logicalFile
	 * @param pm
	 * @return 
	 * @throws PermissionException
	 * @throws FileNotFoundException
	 * @throws ResourceException
	 * @throws IOException
	 * @throws RemoteDataException
	 * @throws HibernateException
	 * @throws JSONException 
	 */
	protected AgaveSuccessRepresentation doMoveOperation(JsonNode jsonInput,
			LogicalFile logicalFile, PermissionManager pm)
			throws PermissionException, FileNotFoundException,
			ResourceException, IOException, RemoteDataException,
			HibernateException, JSONException {
		String message;
		AgaveLogServiceClient.log(AgaveLogServiceClient.ServiceKeys.FILES02.name(),
				AgaveLogServiceClient.ActivityKeys.IOMove.name(),
				username, "", getRequest().getClientInfo().getUpstreamAddress());

		String destPath = null;
		if (jsonInput.has("path")) {
			destPath = jsonInput.get("path").textValue();
		}
		
		// do they have permission to write to this new folder
		if (!pm.canWrite(remoteDataClient.resolvePath(destPath))) {
			throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
		    		"User does not have access to move data to " + destPath,
		    		new PermissionException());
		}

		// support overwriting, they need to be aware of what is and is not there
		if (remoteDataClient.doesExist(destPath))
		{
			if (remoteDataClient.isDirectory(path) && remoteDataClient.isFile(destPath)) {
				throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
		        		"File at " + destPath + " already exists",
			    		new RemoteDataException());
			} else if (remoteDataClient.isFile(path) && remoteDataClient.isDirectory(destPath)) {
				throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
		        		"Folder at " + destPath + " already exists",
			    		new RemoteDataException());
			}
		}

		// move is essentially just a rename on the same system
		remoteDataClient.doRename(path, destPath);
		message = "Move success";
		if (remoteDataClient.isPermissionMirroringRequired())
		{
			remoteDataClient.setOwnerPermission(remoteDataClient.getUsername(), destPath, true);

			String pemUser = StringUtils.isEmpty(owner) ? username : owner;
			try {
				remoteDataClient.setOwnerPermission(pemUser, destPath, true);
			} catch (Exception e) {
				log.error(e.getMessage(), e);
				message = "Rename was successful, but unable to mirror permissions for user " +
						pemUser + " on new directory " + path;
//							getResponse().setEntity(new IplantErrorRepresentation(message));
//				            throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST);
//				            return;
			}
		}

		// now keep the logical file up to date
		LogicalFile destLogicalFile = null;
		try {
			destLogicalFile = LogicalFileDao.findBySystemAndPath(system, remoteDataClient.resolvePath(destPath));
		} catch (Exception e) {}

		String sourceUrl = logicalFile.getPublicLink();
		String destUrl = null;
		
		// no logical file for destination, this is an add or update
		if (destLogicalFile == null)
		{
			destLogicalFile = logicalFile.clone();
			destLogicalFile.setSourceUri(logicalFile.getPublicLink());
			destLogicalFile.setPath(remoteDataClient.resolvePath(destPath));
			destLogicalFile.setName(FilenameUtils.getName(destLogicalFile.getPath()));
			destLogicalFile.setSystem(logicalFile.getSystem());
			destLogicalFile.setOwner(username);
			destLogicalFile.setInternalUsername(internalUsername);
			destLogicalFile.setLastUpdated(new Date());
			
			// set the resulting url of the destination for use in events
			destUrl = destLogicalFile.getPublicLink();
			
			// fire event before record update so the notification event references the source record
			logicalFile.addContentEvent(new FileEvent(FileEventType.MOVED, 
					"Moved from " + sourceUrl + " to " + destUrl, 
					getAuthenticatedUsername()));
			
			// now update source path and name to reference the new location. This will
			// carry the history with it.
			logicalFile.setPath(remoteDataClient.resolvePath(destPath));
			logicalFile.setName(FilenameUtils.getName(logicalFile.getPath()));
			logicalFile.setLastUpdated(new Date());
			
			LogicalFileDao.persist(logicalFile);
		}
		else
		{
			destLogicalFile.setName(FilenameUtils.getName(destLogicalFile.getPath()));
			
			// set the resulting url of the destination for use in events
			destUrl = destLogicalFile.getPublicLink();
			
			destLogicalFile.addContentEvent(new FileEvent(FileEventType.OVERWRITTEN, 
					"Overwritten by a move from " + logicalFile.getPublicLink() + 
					" to " + destLogicalFile.getPublicLink(), 
					getAuthenticatedUsername()));
			LogicalFileDao.persist(destLogicalFile);
		}

		if (logicalFile.isDirectory())
		{
			// we also need to replicate any children that were not copied over before
			List<LogicalFile> nonOverlappingChildren =
					LogicalFileDao.findNonOverlappingChildren(logicalFile.getPath(),
															system.getId(),
															destLogicalFile.getPath(),
															system.getId());

			for (LogicalFile child: nonOverlappingChildren) {
				// capture the original url to the child
				String sourceChildUrl = child.getPublicLink();
				
				// update the path and timestamp
				child.setPath(StringUtils.replaceOnce(child.getPath(), logicalFile.getPath(), destLogicalFile.getPath()));
				child.setLastUpdated(new Date());
				
				// add event
				child.addContentEvent(new FileEvent(FileEventType.MOVED, 
						"File item moved from " + sourceChildUrl + " to " + child.getPublicLink(), 
						getAuthenticatedUsername()));
				
				// update afterwards so the event has the original child path 
				LogicalFileDao.persist(child);
			}

			// now that the file item is moved over, we need to alert the children that the file has been copied
			for (LogicalFile child: LogicalFileDao.findChildren(destLogicalFile.getPath(), system.getId())) {
				if (!nonOverlappingChildren.contains(child))
				{
					child.addContentEvent(new FileEvent(FileEventType.OVERWRITTEN, "Possibly overwritten as "
							+ "part of file item move from " + StringUtils.replace(child.getPublicLink(), destUrl, sourceUrl) + " to " + child.getPublicLink(), 
							getAuthenticatedUsername()));
					LogicalFileDao.persist(child);
				}
			}
		}
		return new AgaveSuccessRepresentation(message, logicalFile.toJSON());
	}
	
	/**
	 * @param jsonInput
	 * @param logicalFile
	 * @param pm
	 * @return
	 * @throws PermissionException
	 * @throws FileNotFoundException
	 * @throws ResourceException
	 * @throws IOException
	 * @throws RemoteDataException
	 * @throws HibernateException
	 */
	protected Representation doMkdirOperation(JsonNode jsonInput, LogicalFile logicalFile, PermissionManager pm)
	throws ResourceException 
	{
		AgaveLogServiceClient.log(AgaveLogServiceClient.ServiceKeys.FILES02.name(),
				AgaveLogServiceClient.ActivityKeys.IOMakeDir.name(),
				username, "", getRequest().getClientInfo().getUpstreamAddress());
		
		String message = "";
		String newdir = path;
		try {
			String dirPath = null;
			if (jsonInput.hasNonNull("path")) {
				dirPath = jsonInput.get("path").textValue();
			}
			
			if (StringUtils.isEmpty(dirPath)) {
				throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
			    		"No path value provided. Please provide the path of "
								+ "the new directory to create. Paths may be absolute or "
								+ "relative to the path given in the url.",
					    		new RemoteDataException());
			}

			if (StringUtils.isEmpty(path)) {
				newdir = dirPath;
			} else {
				if (path.endsWith("/")) {
					newdir += dirPath;
				} else {
					newdir += File.separator + dirPath;
				}
			}

			if (!pm.canWrite(remoteDataClient.resolvePath(newdir))) {
				throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
			    		"User does not have access to create the directory" + newdir,
			    		new PermissionException());
			}
			
			String pemUser = StringUtils.isEmpty(owner) ? username : owner;
			boolean dirCreated = false;
			if (remoteDataClient.doesExist(newdir)) {
				if (remoteDataClient.isDirectory(newdir)) {
					message = "Directory " + newdir + " already exists";
				} else {
					message = "A file already exists at " + newdir;
					throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
							message,
				    		new RemoteDataException());
				}
			} 
			else {
			    if (remoteDataClient.mkdirs(newdir, pemUser)) {
					message = "Mkdir success";
					dirCreated = true;
				} 
				else {
			        throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
			        		"Failed to create directory " + newdir,
				    		new RemoteDataException());
			    }
			}

			try {
				logicalFile = LogicalFileDao.findBySystemAndPath(system, remoteDataClient.resolvePath(newdir));
			} catch(Exception e) {}

			if (logicalFile == null) {
				logicalFile = new LogicalFile();
				logicalFile.setNativeFormat(LogicalFile.DIRECTORY);
				logicalFile.setSourceUri(null);
				logicalFile.setPath(remoteDataClient.resolvePath(newdir));
				logicalFile.setName(FilenameUtils.getName(StringUtils.removeEnd(logicalFile.getPath(), "/")));
				logicalFile.setSystem(system);
				logicalFile.setOwner(pemUser);
				logicalFile.setInternalUsername(internalUsername);
				logicalFile.setLastUpdated(new Date());
				LogicalFileDao.persist(logicalFile);
				
				logicalFile.addContentEvent(new FileEvent(FileEventType.CREATED, 
						"New directory created at " + logicalFile.getPublicLink(), 
						getAuthenticatedUsername()));
				LogicalFileDao.persist(logicalFile);
			}
			else
			{
				logicalFile.setLastUpdated(new Date());
				logicalFile.addContentEvent(new FileEvent(FileEventType.CREATED, 
						"Directory recreated at " + logicalFile.getPublicLink(), 
						getAuthenticatedUsername()));
				LogicalFileDao.persist(logicalFile);
			}
			
			if (dirCreated) {
			    setStatus(Status.SUCCESS_CREATED);
			    return new AgaveSuccessRepresentation(logicalFile.toJSON());
			} else {
				setStatus(Status.SUCCESS_OK);
				return new AgaveSuccessRepresentation(message, logicalFile.toJSON());
			}
		}
		catch (ResourceException e) {
			throw e;
		}
		catch (FileNotFoundException e) {
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, e);
		} 
		catch (HibernateException e) {
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL, 
					"An unexpected internal error occurred while processing the request. " + 
					"If this persists, please contact your tenant administrator", e);
		} 
		catch (PermissionException e) {
			throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN, 
					"User does not have access to create the directory" + newdir, e);
		} 
		catch (RemoteDataException | IOException e) {
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL, e.getMessage(), e);
		} 
		catch (JSONException e) {
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL, 
					"An unexpected internal error occurred while formatting the response message. " + 
					"If this persists, please contact your tenant administrator", e);
		}
	}

	/**
	 * @param jsonInput
	 * @param logicalFile
	 * @param pm
	 * @return
	 * @throws PermissionException
	 * @throws FileNotFoundException
	 * @throws ResourceException
	 * @throws IOException
	 * @throws RemoteDataException
	 * @throws JSONException 
	 * @throws HibernateException
	 */
	protected AgaveSuccessRepresentation doCopyOperation(JsonNode jsonInput, String absolutePath, LogicalFile logicalFile, PermissionManager pm)
	throws PermissionException, FileNotFoundException, ResourceException, IOException, RemoteDataException, JSONException 
	{
		String message;
		AgaveLogServiceClient.log(AgaveLogServiceClient.ServiceKeys.FILES02.name(),
				AgaveLogServiceClient.ActivityKeys.IOCopy.name(),
				username, "", getRequest().getClientInfo().getUpstreamAddress());

		String destPath = null;
		if (jsonInput.has("path")) {
			destPath = jsonInput.get("path").asText();
		} else {
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
		    		"Please specify a destination location",
		    		new RemoteDataException());
		}

		if (!pm.canWrite(remoteDataClient.resolvePath(destPath))) {
			throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
		    		"User does not have access to copy data to " + destPath, 
		    		new FileNotFoundException());
		}
		else if (destPath.equalsIgnoreCase(path)) {
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
		    		"Source and destination locations cannot be the same.",
		    		new RemoteDataException());
		}

		boolean append = false;
		if (jsonInput.has("append")) {
			append = jsonInput.get("append").asBoolean();
		}

		if (append) {
		    remoteDataClient.append(path, destPath);
		} else {
		    remoteDataClient.copy(path, destPath);
		}

		message = "Copy success";
		if (remoteDataClient.isPermissionMirroringRequired())
		{
			remoteDataClient.setOwnerPermission(remoteDataClient.getUsername(), destPath, true);

			String pemUser = StringUtils.isEmpty(owner) ? username : owner;
			try {
				remoteDataClient.setOwnerPermission(pemUser, destPath, true);
			} catch (Exception e) {
				log.error(e.getMessage(), e);
				message = "Rename was successful, but unable to mirror permissions for user " +
						pemUser + " on new directory " + path;
			}
		}

		LogicalFile copiedLogicalFile = null;

		if (logicalFile != null)
		{
			copiedLogicalFile = null;

			try {
				copiedLogicalFile = LogicalFileDao.findBySystemAndPath(system, remoteDataClient.resolvePath(destPath));
			} catch(Exception e) {}

			if (copiedLogicalFile == null) {
				copiedLogicalFile = logicalFile.clone();
				copiedLogicalFile.setSourceUri(logicalFile.getPublicLink());
				copiedLogicalFile.setPath(remoteDataClient.resolvePath(destPath));
				copiedLogicalFile.setSystem(system);
				copiedLogicalFile.setName(FilenameUtils.getName(copiedLogicalFile.getPath()));
				copiedLogicalFile.setOwner(StringUtils.isEmpty(owner) ? username : owner);
		        copiedLogicalFile.setInternalUsername(internalUsername);
				copiedLogicalFile.setLastUpdated(new Date());
				LogicalFileDao.persist(copiedLogicalFile);
				copiedLogicalFile.addContentEvent(new FileEvent(FileEventType.CREATED, 
						"File item copied from " + copiedLogicalFile.getPublicLink(), 
						getAuthenticatedUsername()));
				LogicalFileDao.persist(copiedLogicalFile);
			}
		}

		// note that we do not send notifications for every subfile that may have been updated in a copy
		// operation. That could flood the notification queue and spam people.
		if (logicalFile.isDirectory())
		{
			// we also need to replicate any children that were not copied over before
			List<LogicalFile> nonOverlappingChildren =
					LogicalFileDao.findNonOverlappingChildren(logicalFile.getPath(),
															system.getId(),
															copiedLogicalFile.getPath(),
															system.getId());

			for (LogicalFile child: nonOverlappingChildren)
			{
				LogicalFile copiedChild = child.clone();
				copiedChild.setSourceUri(child.getPublicLink());
				copiedChild.setPath(StringUtils.replaceOnce(child.getPath(), logicalFile.getPath(), copiedLogicalFile.getPath()));
				copiedChild.setSystem(child.getSystem());
				copiedChild.setOwner(child.getOwner());
				copiedChild.setInternalUsername(internalUsername);
				copiedChild.setLastUpdated(new Date());
				LogicalFileDao.persist(copiedChild);
				copiedChild.addContentEvent(new FileEvent(FileEventType.CREATED, 
						"File item copied from " + child.getPublicLink(), 
						getAuthenticatedUsername()));
				LogicalFileDao.persist(copiedChild);
			}

			// now that the file is copied over, we need to alert the children that the file has been copied
//			for (LogicalFile child: LogicalFileDao.findChildren(copiedLogicalFile.getPath(), system.getId())) {
//				child.addContentEvent(new FileEvent(FileEventType.OVERWRITTEN, 
//						"Possibly overwritten as "
//						+ "part of file item copied from " + copiedLogicalFile.getPublicLink(), 
//						getAuthenticatedUsername()));
//				LogicalFileDao.persist(child);
//			}
		}
		return new AgaveSuccessRepresentation(message, copiedLogicalFile.toJSON());
	}
}
