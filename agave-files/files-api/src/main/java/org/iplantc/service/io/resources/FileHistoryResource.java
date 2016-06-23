/**
 * 
 */
package org.iplantc.service.io.resources;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.common.auth.AuthorizationHelper;
import org.iplantc.service.common.clients.AgaveLogServiceClient;
import org.iplantc.service.common.exceptions.PermissionException;
import org.iplantc.service.common.representation.AgaveSuccessRepresentation;
import org.iplantc.service.io.dao.FileEventDao;
import org.iplantc.service.io.dao.LogicalFileDao;
import org.iplantc.service.io.model.FileEvent;
import org.iplantc.service.io.model.LogicalFile;
import org.iplantc.service.io.permissions.PermissionManager;
import org.iplantc.service.io.util.PathResolver;
import org.iplantc.service.systems.dao.SystemDao;
import org.iplantc.service.systems.exceptions.SystemException;
import org.iplantc.service.systems.manager.SystemManager;
import org.iplantc.service.systems.model.RemoteSystem;
import org.iplantc.service.systems.model.enumerations.RemoteSystemType;
import org.iplantc.service.transfer.RemoteDataClient;
import org.iplantc.service.transfer.dao.TransferTaskDao;
import org.iplantc.service.transfer.exceptions.RemoteDataException;
import org.iplantc.service.transfer.exceptions.TransferException;
import org.iplantc.service.transfer.model.TransferSummary;
import org.joda.time.DateTime;
import org.restlet.Request;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * The JobManageResource is the job management interface for users. Through the
 * actions bound to this class, users can obtain individual job
 * description(GET) and kill jobs (DELETE).
 * 
 * @author dooley
 * 
 */
public class FileHistoryResource extends AbstractFileResource {
	private static final Logger	log	= Logger.getLogger(FileHistoryResource.class);

	private String username;
	private String owner;
	private String internalUsername;
	private String path;
	private int limit;
	private int offset;
	private String systemId;
	private RemoteSystem remoteSystem;
	private RemoteDataClient remoteDataClient;
	
	@Override
	public void doInit()
	{
		this.username = getAuthenticatedUsername();
		this.limit = getLimit();
		this.offset = getOffset();
		this.systemId = (String)Request.getCurrent().getAttributes().get("systemId");

		this.internalUsername = getInternalUsername();
		
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
        	getResponse().setStatus(Status.SERVER_ERROR_INTERNAL, e);
        } 
		catch (Throwable e) {
            log.error("Failed to connect to remote system", e);
            // is this needed?
            try { remoteDataClient.disconnect(); } catch (Exception e1) {}
            
        }
	      
	    AgaveLogServiceClient.log(AgaveLogServiceClient.ServiceKeys.FILES02.name(), 
				AgaveLogServiceClient.ActivityKeys.FilesGetHistory.name(), 
				username, "", getRequest().getClientInfo().getUpstreamAddress());
	}

	/**
	 * Returns a collection of FileEvent objects representing the history
	 * of events for this job.
	 */
	@Get
	public Representation represent() throws ResourceException
	{
		try {
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
			else if (!AuthorizationHelper.isTenantAdmin(username) && remoteSystem.isPubliclyAvailable() && 
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
	        
			try 
			{
				try {
					String originalPath = getRequest().getOriginalRef().toUri().getPath();
					path = PathResolver.resolve(owner, originalPath);
					if (remoteSystem.isPubliclyAvailable()) {
						owner = PathResolver.getOwner(originalPath);
					}
				} catch (Exception e) {
					throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Invalid file path");
				}
				
	            LogicalFile logicalFile = null;
	            try {logicalFile=LogicalFileDao.findBySystemAndPath(remoteSystem, remoteDataClient.resolvePath(path));} catch(Exception e) {}
	
	            PermissionManager pm = new PermissionManager(remoteSystem, remoteDataClient, logicalFile, username);
	            
				// check for the file on disk
				boolean exists = false;
	
				try 
				{
	                remoteDataClient.authenticate();
	                exists = remoteDataClient.doesExist(path);
	 
	                if (!exists && logicalFile == null) {
	                    // if it doesn't exist, it was deleted outside of the api,
	                    // so clean up the file entry in the db and its permissions
	//                    if (logicalFile != null) {
	////                    	logicalFile.addEvent(new FileEvent("DELETED", "File or directory deleted outside of API"));
	////                    	LogicalFileDao.removeSubtree(logicalFile);
	//                    } else {
	                        throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
	    						"File/folder does not exist");
	//                    }
	                } else {
	                    // file exists on the file system, so make sure we have
	                    // a logical file for it if not, add one
	                    try {
	                        if (!pm.canRead(remoteDataClient.resolvePath(path))) {
	                        	throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
	                        			"User does not have permission to view this job history");
	                        }
	                    } catch (PermissionException e) {
	                    	throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN, 
	    							"Failed to retrieve history permissions for " + path + ", " + e.getMessage());
	                    }
	
	                    if (logicalFile == null) {
	                    	throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN, 
	    							"No history found on system " + remoteSystem.getSystemId() + " for " + (StringUtils.isEmpty(path)? "user home" : path));
	                    }
	                    else
	                    {
	                        ObjectMapper mapper = new ObjectMapper();
	                        ArrayNode history = mapper.createArrayNode();
	                        
	                    	for (FileEvent event: FileEventDao.getByLogicalFileId(logicalFile.getId(), limit, offset))
	                    	{
	                    	    ObjectNode jsonEvent = mapper.createObjectNode();
	
	                    		if (event.getTransferTask() != null) 
	        					{
	                                ObjectNode jsonTransferTask = mapper.createObjectNode();
	        						
	        						try {
	        							TransferSummary summary = TransferTaskDao.getTransferSummary(event.getTransferTask());
	        							
	        							jsonTransferTask
	        							    .put("uuid", event.getTransferTask().getUuid())
	                                    	.put("source", event.getTransferTask().getSource())
	        								.put("totalActiveTransfers",  summary.getTotalActiveTransfers())
	        								.put("totalFiles", summary.getTotalTransfers())
	        								.put("totalBytesTransferred", summary.getTotalTransferredBytes().longValue())
	        								.put("totalBytes", summary.getTotalBytes().longValue())
	        								.put("averageRate", summary.getAverageTransferRate());
	        							
	        							jsonEvent.put("progress", jsonTransferTask);
	        						} catch (TransferException e) {
	        							jsonEvent.put("progress", (String) null);
	        						}
	        					}
	        					jsonEvent
	        						.put("status", event.getStatus())
	        						.put("created", new DateTime(event.getCreated()).toString())
	        						.put("createdBy", event.getCreatedBy())
	        						.put("description", event.getDescription());
	//        					    .put("_links", jsonLinks);
	                            
	        					history.add(jsonEvent);
	        				}
	        				return new AgaveSuccessRepresentation(history.toString());
	                    }
	                }
				} catch (ResourceException e) {
					throw e;
	            } catch (Exception e) {
	            	throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
							"Failed to retrieve information for " + path);
	            }
			}
			catch (ResourceException e) {
				throw e;
			}
			catch (Exception e) {
				throw new ResourceException(Status.SERVER_ERROR_INTERNAL, e);
			}
		}
		finally {
			try {remoteDataClient.disconnect();} catch (Exception e) {}
		}

	}
}
