/**
 * 
 */
package org.iplantc.service.io.resources;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.util.List;

import javax.activation.MimetypesFileTypeMap;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.common.clients.AgaveLogServiceClient;
import org.iplantc.service.common.dao.TenantDao;
import org.iplantc.service.common.exceptions.PermissionException;
import org.iplantc.service.common.model.Tenant;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.io.Settings;
import org.iplantc.service.io.dao.LogicalFileDao;
import org.iplantc.service.io.exceptions.FileEventProcessingException;
import org.iplantc.service.io.exceptions.LogicalFileException;
import org.iplantc.service.io.manager.FileEventProcessor;
import org.iplantc.service.io.model.FileEvent;
import org.iplantc.service.io.model.LogicalFile;
import org.iplantc.service.io.model.enumerations.FileEventType;
import org.iplantc.service.io.model.enumerations.StagingTaskStatus;
import org.iplantc.service.io.permissions.PermissionManager;
import org.iplantc.service.io.util.PathResolver;
import org.iplantc.service.io.util.ServiceUtils;
import org.iplantc.service.systems.dao.SystemDao;
import org.iplantc.service.systems.exceptions.RemoteCredentialException;
import org.iplantc.service.systems.exceptions.SystemUnavailableException;
import org.iplantc.service.systems.exceptions.SystemUnknownException;
import org.iplantc.service.systems.model.RemoteSystem;
import org.iplantc.service.systems.model.enumerations.SystemStatusType;
import org.iplantc.service.transfer.RemoteDataClient;
import org.iplantc.service.transfer.RemoteFileInfo;
import org.iplantc.service.transfer.exceptions.RemoteDataException;
import org.restlet.Request;
import org.restlet.data.Disposition;
import org.restlet.data.Header;
import org.restlet.data.MediaType;
import org.restlet.data.Range;
import org.restlet.data.Status;
import org.restlet.representation.EmptyRepresentation;
import org.restlet.representation.Representation;
import org.restlet.representation.WriterRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;
import org.restlet.util.Series;

/**
 * Class to handle get and post requests for jobs
 * 
 * @author dooley
 * 
 */
public class PublicFileDownloadResource extends AbstractFileResource {
	private static final Logger log = Logger.getLogger(PublicFileDownloadResource.class); 
	
	private String 			path;			// path of the file
	private List<Range> 	ranges = null;	// ranges of the file to return, given by byte index for start and a size.
	private String systemId;
	private RemoteSystem system;
	private RemoteDataClient remoteDataClient;
	private String internalUsername;
	
	
	public PublicFileDownloadResource() {}
	
	/* (non-Javadoc)
	 * @see org.restlet.resource.Resource#doInit()
	 */
	@Override
	public void doInit() {
		
        // Get the requested file ranges
		this.ranges = (List<Range>)Request.getCurrent().getRanges();

        SystemDao sysDao = new SystemDao();
        
        //get system ID
        systemId = (String)Request.getCurrent().getAttributes().get("systemId");

        try {
	        // Instantiate remote data client to the correct system
        	Series<Header> headers = Request.getCurrent().getHeaders();
        	Header xForwardHostHeader = headers.getFirst("x-forwarded-host", true);
        	Tenant tenant = null;
        	String xForwardHost = xForwardHostHeader.getValue();
    		if (StringUtils.isEmpty(xForwardHost)) {
    			log.error("No x-forward-host header found in the request for " + Request.getCurrent().getOriginalRef().toString());
    		}
    		else {
    			for (String hostname: StringUtils.split(xForwardHost, ",")) {
    				tenant = new TenantDao().findByBaseUrl(hostname);
    				if (tenant != null) break;
    			}
    		}
        	
//	        String requestHostname = Request.getCurrent().getOriginalRef().getHostDomain();
        		
	        
	        if (tenant == null) {
	        	log.error("No tenant found matching the x-forward-host url of " + xForwardHost);
	        	throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, "No system found with the given id.", 
	        			new SystemUnknownException("No tenant found matching the x-forward-host url of " + xForwardHost));
	        } else {
	        	TenancyHelper.setCurrentTenantId(tenant.getTenantCode());
	        	TenancyHelper.setCurrentEndUser(Settings.PUBLIC_USER_USERNAME);
	        }
        
            if (StringUtils.isNotEmpty(systemId)) {
            	this.system = sysDao.findBySystemId(systemId);
            } else {
                throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, 
                		"Not publicly shared resource found at the given path.", 
                		new SystemUnknownException("No system found with id " + systemId));
            }
            
            if (system == null) {
            	throw new SystemUnknownException("No system found for the published data matching " + systemId);
            }
            else if (!system.isAvailable()) {
            	throw new SystemUnavailableException("The system containing the published data is currently unavailable.");
            }
            else if (system.getStatus() != SystemStatusType.UP) {
            	throw new SystemUnavailableException("The system containing the published data is currently " + system.getStatus().name().toLowerCase());
            }
            else {
            	this.remoteDataClient = system.getRemoteDataClient(internalUsername);
            }
        }
        catch (SystemUnavailableException e) {
        	try { remoteDataClient.disconnect(); } catch (Exception e1) {}
			log.error("Failed to download published data.", e);
			throw new ResourceException(Status.SERVER_ERROR_SERVICE_UNAVAILABLE, e.getMessage(), e);
		}
        catch (SystemUnknownException e) {
        	try { remoteDataClient.disconnect(); } catch (Exception e1) {}
        	log.error("Failed to download published data.", e);
        	throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, e.getMessage(), e);
        } 
        catch (RemoteDataException e) {
        	try { remoteDataClient.disconnect(); } catch (Exception e1) {}
			String message = "Unable to connect to the system containing the published data, " + systemId;
        	log.error(message, e);
        	throw new ResourceException(Status.SERVER_ERROR_INTERNAL, message, e);
        }
        catch (RemoteCredentialException e) {
        	try { remoteDataClient.disconnect(); } catch (Exception e1) {}
        	String message = "Failed to authenticate to the system containing the published data, " + systemId;
        	log.error(message, e);
        	throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN, message, e);
        }
        catch (Throwable e) {
        	try { remoteDataClient.disconnect(); } catch (Exception e1) {}
        	String message = "An unexpected error occurred while fetching the published data from " + systemId 
            		+ ". If this error continues, please contact your tenant admin.";
            log.error(message, e);
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL, message, e);
        }
		
		AgaveLogServiceClient.log(AgaveLogServiceClient.ServiceKeys.FILES02.name(), 
				AgaveLogServiceClient.ActivityKeys.IOPublicDownload.name(), 
				"guest", "", getRequest().getClientInfo().getUpstreamAddress());
		
		// Specify that range requests are accepted
        getResponse().getServerInfo().setAcceptingRanges(true);
	}

	/** 
	 * This method represents the HTTP GET action. Using the file id from the URL, the
	 * input file is streamed to the user from the local cache. If the file id is invalid 
	 * for any reason, a HTTP {@link org.restlet.data.Status#CLIENT_ERROR_BAD_REQUEST 400} code is sent. 
	 */
	@Get
	public Representation represent() throws ResourceException 
	{	
		AgaveLogServiceClient.log(AgaveLogServiceClient.ServiceKeys.FILES02.name(), 
				AgaveLogServiceClient.ActivityKeys.IODownload.name(), 
				getAuthenticatedUsername(), "", getRequest().getClientInfo().getUpstreamAddress());
		
		// make sure the resource they are looking for is available.
        if (remoteDataClient == null) {
        	if (StringUtils.isEmpty(systemId)) {
        		throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, 
        				"No default storage system found. Please register a system and set it as your default. ");
        	} else {
        		throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, 
        				"No resource found for user with system id " + systemId);
        	}
        }
        
        try {
			try {
				String originalPath = getRequest().getOriginalRef().toUri().getPath();
				path = PathResolver.resolvePublic(getAuthenticatedUsername(), originalPath);
			} catch (Exception e) {
				throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Invalid file path");
			}
			
			LogicalFile logicalFile = null;
			RemoteFileInfo remoteFileInfo = null;
			PermissionManager pm = null;
			String absolutePath = null;
			try 
			{
				remoteDataClient.authenticate();
                
				remoteFileInfo = remoteDataClient.getFileInfo(path);
				absolutePath = remoteDataClient.resolvePath(path);
				
				logicalFile = LogicalFileDao.findBySystemAndPath(system, absolutePath);
                
				pm = new PermissionManager(
						system, remoteDataClient, logicalFile, Settings.PUBLIC_USER_USERNAME);
				
                // check for file on disk
//                if (logicalFile == null) {
                    // if it doesn't exist, it was deleted outside of the api,
                    // so clean up the file entry in the db and its permissions
//                    if (logicalFile != null) {
//                        pm.clearPermissions(false);
//                        //SharePermissionDao.removeAllPermissionsForPathSubtree(path);
//                        LogicalFileDao.removeSubtree(logicalFile);
//                    }

//                    throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
//    						"File/folder does not exist");

//                } else 
            	if (remoteDataClient.isDirectory(path)) { // return file listing
            		
            		if (remoteDataClient.doesExist(path + "/index.html")) {
            			path = path + "/index.html";
            			
            			remoteFileInfo = remoteDataClient.getFileInfo(path);
            			absolutePath = remoteDataClient.resolvePath(path);
        				
        				logicalFile = LogicalFileDao.findBySystemAndPath(system, absolutePath);
                        
        				pm = new PermissionManager(
        						system, remoteDataClient, logicalFile, Settings.PUBLIC_USER_USERNAME);
            		}
            		else if (remoteDataClient.doesExist(path + "/index.htm")) {
            			path = path + "/index.htm";
            			
            			remoteFileInfo = remoteDataClient.getFileInfo(path);
            			absolutePath = remoteDataClient.resolvePath(path);
        				
        				logicalFile = LogicalFileDao.findBySystemAndPath(system, absolutePath);
                        
        				pm = new PermissionManager(
        						system, remoteDataClient, logicalFile, Settings.PUBLIC_USER_USERNAME);
            		}
            		else {
            			throw new ResourceException(Status.SERVER_ERROR_NOT_IMPLEMENTED,
                    			"Directory downloads not supported");
            		}
                } 
            	
            	// file exists on the file system, so make sure we have
                // a logical file for it if not, add one
                try {
                    if (!pm.canRead(absolutePath)) {
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

                    if ((range.getIndex() + range.getSize()) > remoteFileInfo.getSize()) {
                        getResponse().setStatus(
                        		new Status(416, "Requested Range Not Satisfiable", 
	                            		"Upper bound out of range of file", 
	                            		"http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html"),
                        		"Specified Range upper bound outside bounds of file");
                    }
                } 
			} catch (FileNotFoundException e) {
				throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
						"File/folder does not exist");
			} catch (ResourceException e) {
				throw e;
            } catch (Exception e) {
            	e.printStackTrace();
            	throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
            			"Failed to retrieve information for " + path, e);
            }
			
			// we need to create a logical file here if it doesn't already exist so we can throw the
			// download event on the file item.
			
			if (logicalFile == null) {
				try {
					String fileOwner = pm.getImpliedOwnerForFileItemPath(system, absolutePath);
					
					logicalFile = new LogicalFile();
	                logicalFile.setSystem(system);
	                logicalFile.setName(new File(path).getName());
	                logicalFile.setNativeFormat(LogicalFile.RAW);
	                logicalFile.setOwner(fileOwner);
	                logicalFile.setPath(absolutePath);
	                logicalFile.setSourceUri("agave://" + system.getSystemId() + "/" + logicalFile.getAgaveRelativePathFromAbsolutePath());
	                logicalFile.setStatus(StagingTaskStatus.STAGING_COMPLETED);
	                LogicalFileDao.persist(logicalFile);	
				}
				catch (PermissionException e) {
					throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
	            			"Unable to fetch files outside of the system root.", e); 
				}
				catch (Throwable e) {
					log.error("Unable to resolve ownership of unknown file agave://" 
							+ system.getSystemId() + "/" + path 
							+ " during public file download. No event will be thrown.", e ); 
				}
			}
			
			if (logicalFile != null) {
				try {
					FileEventProcessor.processAndSaveContentEvent(logicalFile, new FileEvent(
							FileEventType.DOWNLOAD,
							FileEventType.DOWNLOAD.getDescription(),
							Settings.PUBLIC_USER_USERNAME,
							TenancyHelper.getCurrentTenantId()));
				}
				catch (LogicalFileException| FileEventProcessingException e) {
					log.error("Failed to send public DOWNLOAD event for agave://" 
							+ system.getSystemId() + "/" + path, e ); 
				}
			}
			
            // stream the file to them
			File f = new File(new File(path).getName());
			String mimetype = resolveMimeTime(f.getName());
			final String remotePath = path;
			final long fileSize = remoteFileInfo.getSize();
			final WriterRepresentation wrep = new WriterRepresentation(MediaType.valueOf(mimetype)) {
				
				private InputStream in;
				RemoteDataClient client = null;
				
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
                        try 
                        {
                        	client = system.getRemoteDataClient(internalUsername);
                        	client.authenticate();
							in = client.getInputStream(remotePath, false);
						} catch (Exception e) {
							throw new IOException(e);
						}
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
//						out.close();
					} finally {
						try { client.disconnect(); } catch (Exception e) {}
						try { remoteDataClient.disconnect(); } catch (Exception e) {}
						try { in.close(); } catch (Exception e) {}
//						try { out.close(); } catch (Exception e) {}
					}
				}
	
				@SuppressWarnings("unused")
				@Override
				public void write(Writer writer) throws IOException {
				
					if (true) throw new IOException("Too slow to use");

					try {
						try 
                        {
                        	// Not sure whether to set input stream to passive or not
                        	client = system.getRemoteDataClient(internalUsername);
                        	client.authenticate();
							in = client.getInputStream(remotePath, false);
						} catch (Exception e) {
							throw new IOException(e);
						}
						
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
					} finally {
						try { in.close(); } catch (Exception e) {}
						try { client.disconnect(); } catch (Exception e) {}
						try { remoteDataClient.disconnect(); } catch (Exception e) {}
					}
				}
			};
			
			// Only supporting the first range specified for each GET request

			if (!ranges.isEmpty())
				wrep.setRange(ranges.get(0));
			else
				wrep.setRange(null);
			
			wrep.setModificationDate(remoteFileInfo.getLastModified());
			
			Disposition disposition = new Disposition();
			disposition.setModificationDate(remoteFileInfo.getLastModified());
			disposition.setFilename(logicalFile.getName());
			disposition.setType(Disposition.TYPE_INLINE);
			if (isForceDownload()) { 
				disposition.setType(Disposition.TYPE_ATTACHMENT);
			}
			
			wrep.setDisposition(disposition);
			
			return wrep;
	    } 
		catch (ResourceException e) {
			throw e;
		}
		catch (Throwable e) {
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL, e);
        } 
		finally {
        	try {remoteDataClient.disconnect();} catch (Exception e) {}
        }
		
    }	
	
	@Override
	public Representation head() {
		
		AgaveLogServiceClient.log(AgaveLogServiceClient.ServiceKeys.FILES02.name(), 
				AgaveLogServiceClient.ActivityKeys.IODownload.name(), 
				getAuthenticatedUsername(), "", getRequest().getClientInfo().getUpstreamAddress());
		
		try {
			// make sure the resource they are looking for is available.
	        if (remoteDataClient == null) {
	        	if (StringUtils.isEmpty(systemId)) {
	        		throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, 
	        				"No default storage system found. Please register a system and set it as your default. ");
	        	} else {
	        		throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, 
	        				"No resource found for user with system id " + systemId);
	        	}
	        }
	        
			try {
				String originalPath = getRequest().getOriginalRef().toUri().getPath();
				path = PathResolver.resolvePublic(getAuthenticatedUsername(), originalPath);
			} catch (Exception e) {
				throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Invalid file path");
			}
			
			LogicalFile logicalFile = null;
			RemoteFileInfo remoteFileInfo = null;
			try 
			{
				remoteDataClient.authenticate();
                
				remoteFileInfo = remoteDataClient.getFileInfo(path);
				
				logicalFile = LogicalFileDao.findBySystemAndPath(system, remoteDataClient.resolvePath(path));
                
				PermissionManager pm = new PermissionManager(
						system, remoteDataClient, logicalFile, Settings.PUBLIC_USER_USERNAME);
				
                // check for file on disk
                if (logicalFile == null) {
                	throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
    						"File/folder does not exist");
                } 
                else if (remoteDataClient.isDirectory(path)) {
                	throw new ResourceException(Status.SERVER_ERROR_NOT_IMPLEMENTED,
                			"Directory downloads not supported");
                } 
                else {
                	try {
                        if (!pm.canRead(logicalFile.getPath())) {
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

                        if ((range.getIndex() + range.getSize()) > remoteFileInfo.getSize()) {
                            getResponse().setStatus(
                            		new Status(416, "Requested Range Not Satisfiable", 
		                            		"Upper bound out of range of file", 
		                            		"http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html"),
                            		"Specified Range upper bound outside bounds of file");
                        }
                    }                    
                }
            } 
			catch (ResourceException e) {
				throw e;
            }
			catch (IOException e) {
				throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, e.getMessage());
            } 
			catch (Exception e) {
            	log.error("Failed to fetch info for head response of " + path, e);
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
			throw e;
		}
		catch (Exception e) {
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL, e.getMessage());
        } 
		finally {
        	try {remoteDataClient.disconnect();} catch (Exception e) {}
        }
    }
}

