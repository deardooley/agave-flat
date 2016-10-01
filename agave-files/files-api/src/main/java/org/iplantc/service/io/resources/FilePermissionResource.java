/**
 * 
 */
package org.iplantc.service.io.resources;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.common.clients.AgaveLogServiceClient;
import org.iplantc.service.common.exceptions.PermissionException;
import org.iplantc.service.common.representation.AgaveSuccessRepresentation;
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
import org.iplantc.service.transfer.exceptions.RemoteDataException;
import org.iplantc.service.transfer.model.RemoteFilePermission;
import org.iplantc.service.transfer.model.enumerations.PermissionType;
import org.restlet.Request;
import org.restlet.data.Form;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Class to handle get and post requests for jobs
 * 
 * @author dooley
 * 
 */
public class FilePermissionResource extends AbstractFileResource 
{
	private static final Logger log = Logger.getLogger(FilePermissionResource.class); 

	private String systemId;
	private int limit;
	private int offset;
	private String username;
    private String internalUsername;
	private String owner;
	private String path;
	private String searchUsername = null;
	private PermissionType searchPermission = null;

    private RemoteDataClient remoteDataClient;
    private RemoteSystem system;
	
    @Override
	public void doInit() {

		this.username = getAuthenticatedUsername();
		this.limit = getLimit();
		this.offset = getOffset();
		this.internalUsername = getInternalUsername();
		String sPermission = null;
		Form form = Request.getCurrent().getOriginalRef().getQueryAsForm();
        if (form != null) {
        	for (String key: form.getNames()) {
	        	if (this.searchUsername == null && 
						StringUtils.trimToEmpty(key).toLowerCase().startsWith("username")) {
					this.searchUsername = form.getFirstValue(key).toString();
				}
				else if (sPermission == null && 
						StringUtils.trimToEmpty(key).toLowerCase().startsWith("permission")) {
					sPermission = form.getFirstValue(key).toString();
				}
        	}
		}
		
		try {
			if (StringUtils.isNotEmpty(sPermission)) {
				this.searchPermission = PermissionType.valueOf(sPermission);
			}
		}
		catch (IllegalArgumentException e) {
        	throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
            		"Invalid permission value. Valid values are: " + 
            		ServiceUtils.explode(",", Arrays.asList(PermissionType.values())));
        }
		
        SystemManager sysManager = new SystemManager();
        SystemDao sysDao = new SystemDao();
        //get system ID
        systemId = (String)Request.getCurrent().getAttributes().get("systemId");

        // Instantiate remote data client to the correct system
		try 
        {
            if (systemId != null) 
            {
                this.system = sysDao.findUserSystemBySystemId(username, systemId);
            } 
            else 
            {
                // If no system specified, select the user default system
                this.system = sysManager.getUserDefaultStorageSystem(username);
            }
	    } 
        catch (SystemException e) {
        	throw new ResourceException(Status.SERVER_ERROR_INTERNAL, e);
		}
		catch (Throwable e) 
        {
        	log.error("Failed to connect to remote system");
            
        	try { remoteDataClient.disconnect(); } catch (Exception e1) {}
            
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL, e);
        }		
	}

	/** 
	 * Lists the permissions of the file or directory given in the path.
	 */
	@Get
	public Representation represent() throws ResourceException 
	{
		AgaveLogServiceClient.log(AgaveLogServiceClient.ServiceKeys.FILES02.name(), 
				AgaveLogServiceClient.ActivityKeys.IOPemsList.name(), 
				username, "", getRequest().getClientInfo().getUpstreamAddress());
		
		try 
		{
			// make sure the resource they are looking for is available.
			if (system == null) 
			{
	        	if (StringUtils.isEmpty(systemId)) {
	        		throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, 
	        				"No default storage system found. Please register a system and set it as your default. ");
	        	} else {
	        		throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, 
	        				"No resource found for user with system id '" + systemId + "'");
	        	}
	        }
			else if (!ServiceUtils.isAdmin(username) && system.isPubliclyAvailable() && 
					system.getType().equals(RemoteSystemType.EXECUTION)) 
			{
				throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN, 
						"User does not have access to view the requested resource. " + 
						"File access is restricted to administrators on public execution systems.");
			}
			
			try {
				this.remoteDataClient = system.getRemoteDataClient(internalUsername);
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
				String originalPath = getRequest().getOriginalRef().toUri().getPath();
				path = PathResolver.resolve(owner, originalPath);
				
				if (system.isPubliclyAvailable()) {
					owner = PathResolver.getOwner(originalPath);
				} 
			} 
			catch (Exception e) 
			{
				throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
						"Invalid file path");
			}

			LogicalFile logicalFile = null;
			
            try {
            	logicalFile=LogicalFileDao.findBySystemAndPath(system, remoteDataClient.resolvePath(path));
            } catch (Exception e) {}

            PermissionManager pm = new PermissionManager(system, remoteDataClient, logicalFile, username);

            try 
            {
                remoteDataClient.authenticate();
				
                if (!remoteDataClient.doesExist(path) && logicalFile == null)  
				{
					// if it doesn't exist, it was deleted outside of the api, 
					// so clean up the file entry in the db and its permissions
//                	if (logicalFile != null) {
//                		logicalFile.addEvent(new FileEvent("DELETED", "File or directory deleted outside of API"));
//						LogicalFileDao.removeSubtree(logicalFile);
//					}
					
					throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, 
					        "File/folder does not exist");
				} 
				else 
				{	
					try {
                        if (!pm.canRead(remoteDataClient.resolvePath(path))) {
                        	throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
    								"User does not have access to view the requested resource");
                        }
                    } catch (PermissionException e) {
                    	throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN, 
    							"Failed to retrieve permissions for " + path + ", " + e.getMessage());
                    }
					
					if (logicalFile == null) 
					{
						// we don't add logical file entries for directories unless share permissions
						// are explicitly added
//						if (!remoteDataClient.isDirectory(path)) 
//						{
							logicalFile = new LogicalFile();
							logicalFile.setUuid(new AgaveUUID(UUIDType.FILE).toString());
	                		logicalFile.setName(new File(path).getName());
                            logicalFile.setSystem(system);
							logicalFile.setNativeFormat(remoteDataClient.isDirectory(path) ? LogicalFile.DIRECTORY : LogicalFile.RAW);
							logicalFile.setOwner(owner == null ? remoteDataClient.getUsername() : owner);
							logicalFile.setSourceUri("");
							logicalFile.setPath(remoteDataClient.resolvePath(path));
							logicalFile.setStatus(StagingTaskStatus.STAGING_COMPLETED);
                            logicalFile.setInternalUsername(internalUsername);
							LogicalFileDao.persist(logicalFile);
//						}
					} 
					
					StringBuilder builder = new StringBuilder();
					RemoteFilePermission[] pems = null;
					if (StringUtils.isEmpty(this.searchUsername)) {
						pems = pm.getAllPermissions(path).toArray(new RemoteFilePermission[]{});
					}
					else {
						PermissionManager searchPermissionManager = new PermissionManager(system, remoteDataClient, logicalFile, this.searchUsername);
						pems = new RemoteFilePermission[]{searchPermissionManager.getUserPermission(remoteDataClient.resolvePath(path))};
					}
					
					// filter before pagination
					if (this.searchPermission != null) {
						List<RemoteFilePermission> filteredPems = new ArrayList<RemoteFilePermission>();
						for (RemoteFilePermission pem: pems) {
							if (this.searchPermission == pem.getPermission()) {
								filteredPems.add(pem);
							}
						}
						pems = filteredPems.toArray(new RemoteFilePermission[]{});
					}
					
					// serialize the filtered permsisions to json
					for (int i=offset; i< Math.min((limit+offset), pems.length); i++)
					{
						RemoteFilePermission pem = pems[i];
						builder.append( "," + pem.toJSON(path, system.getSystemId()));
                    }
					
					return new AgaveSuccessRepresentation("[" + StringUtils.removeStart(builder.toString(), ",") + "]");
				}
            } catch (FileNotFoundException e) {
				throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
						"File/folder does not exist");
			} 
            catch (ResourceException e) {
				throw e;
			} 
            catch (Throwable e) 
            {
            	throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
            			"Failed to retrieve permissions for " + path);
			}
		} 
        catch (ResourceException e) {
			throw e;
		} 
        catch (Throwable e) {
        	throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
        			"Failed to retrieve permissions for " + path);
		}
		finally {
			try {remoteDataClient.disconnect();} catch (Exception e) {}
		}
	}

	
	/* 
	 * Sets the share permissions operations on a particular file or folder. This will overwrite the existing
	 * permissions for that file or directory.
	 */
	@Post
	public Representation accept(Representation input) throws ResourceException 
	{
		AgaveLogServiceClient.log(AgaveLogServiceClient.ServiceKeys.FILES02.name(), 
				AgaveLogServiceClient.ActivityKeys.IOPemsUpdate.name(), 
				username, "", getRequest().getClientInfo().getUpstreamAddress());
		
		try 
		{	
			// make sure the resource they are looking for is available.
			if (system == null) 
			{
	        	if (StringUtils.isEmpty(systemId)) {
	        		throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, 
	        				"No default storage system found. Please register a system and set it as your default. ");
	        	} else {
	        		throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, 
	        				"No resource found for user with system id '" + systemId + "'");
	        	}
	        }
			else if (system.isPubliclyAvailable() && 
					system.getType().equals(RemoteSystemType.EXECUTION)) 
			{
				throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN, 
						"User does not have access to view the requested resource. " + 
						"File access is restricted to administrators on public execution systems.");
			}
			
			try {
				this.remoteDataClient = system.getRemoteDataClient(internalUsername);
			} 
			catch(RemoteDataException e)
			{
				throw new ResourceException(Status.CLIENT_ERROR_PRECONDITION_FAILED, e.getMessage(), e);
			} 
			catch (Exception e) {	
				throw new ResourceException(Status.CLIENT_ERROR_PRECONDITION_FAILED, 
						"Unable to establish a connection to the remote server. " + e.getMessage(), e);
			}
			
			// parse the form to get the job specs
			JsonNode inputJson = getPostedContentAsJsonNode(input);
			
			
			String shareUsername = null;
			if (inputJson.hasNonNull("username")) {
				shareUsername = inputJson.get("username").asText();
			}
				
			if (StringUtils.isEmpty(shareUsername) || StringUtils.equalsIgnoreCase(shareUsername, "null")) {
				throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
						"No username specified");
			} 
			
//			else if ((shareUsername.equalsIgnoreCase(owner) || shareUsername.equalsIgnoreCase(username))
//					&& !system.getStorageConfig().isMirrorPermissions()) {
//				// don't set permissions for the owner.
//				PermissionManager pm = new PermissionManager(system, remoteDataClient, logicalFile, shareUsername);
//                
//				RemoteFilePermission userPem = pm.getUserPermission(logicalFile.getPath());
//                return new AgaveSuccessRepresentation("[" + userPem.toJSON(path, logicalFile.getSystem().getSystemId()) + "]");
//                
//				return new AgaveSuccessRepresentation();
//			}
			
			try 
			{
				String originalPath = getRequest().getOriginalRef().toUri().getPath();
				path = PathResolver.resolve(owner, originalPath);
				if (system.isPubliclyAvailable()) {
					owner = PathResolver.getOwner(originalPath);
				}
			} 
			catch (Exception e) {
				throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
						"Invalid file path " + path);
			}
			
			LogicalFile logicalFile = null;
            try {
            	logicalFile = LogicalFileDao.findBySystemAndPath(system, remoteDataClient.resolvePath(path));
            } catch (Exception e) {}

            PermissionManager pm = null;
			
			// check for the file on disk
			try 
			{
                remoteDataClient.authenticate();
	            
                if (!remoteDataClient.doesExist(path) && logicalFile == null) {
				    throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
				    		"File/folder does not exist");
                } 
                else 
                {
                    if (logicalFile == null)  // if the logical file isn't there, create one. Even folders get one
                    {
                        logicalFile = new LogicalFile(StringUtils.isEmpty(owner) ? username : owner, system, remoteDataClient.resolvePath(path));
                        logicalFile.setSourceUri("");
                        logicalFile.setNativeFormat(remoteDataClient.isDirectory(path) ? LogicalFile.DIRECTORY : LogicalFile.RAW);
                        logicalFile.setStatus(StagingTaskStatus.STAGING_COMPLETED);
                        logicalFile.setInternalUsername(internalUsername);
                        LogicalFileDao.persist(logicalFile);
                    }
                    
                    pm = new PermissionManager(system, remoteDataClient, logicalFile, username);
                    
                    if (!pm.canWrite(remoteDataClient.resolvePath(path))) {
                        throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN, "User does not have permission to manage permissions on this file for directory.");
                    }
                    
                    try 
                    {	
                        Boolean grantRead = false;
                        Boolean grantWrite = false;
                        Boolean grantExecute = false;
                        Boolean grantAll = false;
                        
                        if (inputJson.hasNonNull("permission")) 
                        {
                        	String permission = inputJson.get("permission").asText();
                        
                        	if (StringUtils.isEmpty(permission)) {
                    			throw new IllegalArgumentException("No permission value specified.");
                    		} else {
                    			PermissionType pem = PermissionType.valueOf(permission.toUpperCase());
                    			if (pem.canRead()) {
                    				grantRead = true;
                    			}
                    			if (pem.canWrite()) {
                    				grantWrite = true;
                    			}
                    			if (pem.canExecute()) {
                    				grantExecute = true;
                    			}
                    			if (pem.equals(PermissionType.ALL)) {
                    				grantAll = true;
                    			}
                    		}
                        } 
                        else 
                        {
                            if (inputJson.has("canRead")) {
                                grantRead = inputJson.get("canRead").asBoolean();
                            } else if (inputJson.has("can_read")) {
                                grantRead = inputJson.get("can_read").asBoolean();
                            } else if (inputJson.has("read")) {
                                grantRead = inputJson.get("read").asBoolean();
                            } else {
                                grantRead = pm.canRead(remoteDataClient.resolvePath(path));
                            }

                            if (inputJson.has("canWrite")) {
                                grantWrite = inputJson.get("canWrite").asBoolean();
                            } else if (inputJson.has("can_write")) {
                                grantWrite = inputJson.get("can_write").asBoolean();
                            } else if (inputJson.has("write")) {
                                grantWrite = inputJson.get("write").asBoolean();
                            } else {
                                grantWrite = pm.canWrite(remoteDataClient.resolvePath(path));
                            }

                            if (inputJson.has("grantExecute")) {
                                grantExecute = inputJson.get("grantExecute").asBoolean();
                            } else if (inputJson.has("can_execute")) {
                                grantExecute = inputJson.get("can_execute").asBoolean();
                            } else if (inputJson.has("execute")) {
                                grantExecute = inputJson.get("execute").asBoolean();
                            } else {
                                grantExecute = false;
                            }

                            if (inputJson.has("grantAll")) {
                                grantAll = inputJson.get("grantAll").asBoolean();
                            } else if (inputJson.has("can_all")) {
                                grantAll = inputJson.get("can_all").asBoolean();
                            } else if (inputJson.has("all")) {
                                grantAll = inputJson.get("all").asBoolean();
                            } else {
                                grantAll = false;
                            }
                        }

                        boolean recursive = false;
                        if (StringUtils.equalsIgnoreCase(logicalFile.getNativeFormat(), LogicalFile.DIRECTORY)  
                        		&& inputJson.hasNonNull("recursive")) {
                        	recursive = BooleanUtils.toBoolean(inputJson.get("recursive").asText());
                        }
                        
                        // check for wildcard delete operation. This enables clear operation recursively
            			if (StringUtils.equals(shareUsername, "*") && grantRead == false && grantWrite == false && grantExecute == false) {
            				shareUsername = username;
            			}
            			
            			// now set the permission for the target user of this post
                        pm = new PermissionManager(system, remoteDataClient, logicalFile, shareUsername);
                        
            			if (grantAll)
                        {
                        	pm.clearPermissions(recursive);
                        	pm.addAllPermission(path, recursive);
                            setStatus(Status.SUCCESS_CREATED);
                        }
                        else if (grantRead == false && grantWrite == false && grantExecute == false)
                        {
                        	pm.clearPermissions(recursive);
                            setStatus(Status.SUCCESS_OK);
                        }
                        else
                        {
                        	pm.clearPermissions(recursive);
                        	
                        	if (grantExecute) {
                            	pm.addExecutePermission(path, recursive);
                            } 
                            
                            if (grantRead) {
                            	pm.addReadPermission(path, recursive);
                            } 
                            
                            if (grantWrite) {
                            	pm.addWritePermission(path, recursive);
                            } 
                            setStatus(Status.SUCCESS_CREATED);
                        }
                        
    					RemoteFilePermission userPem = pm.getUserPermission(logicalFile.getPath());

                        return new AgaveSuccessRepresentation("[" + userPem.toJSON(path, logicalFile.getSystem().getSystemId()) + "]");
                        
                    } 
                    catch (IllegalArgumentException e) {
                    	throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
                        		"Invalid permission value. Valid values are: " + 
                        		ServiceUtils.explode(",", Arrays.asList(PermissionType.values())));
                    } 
                    catch (Exception e) {
                    	log.error("Failed to update permissions for agave://" + system.getSystemId() + "/" + path, e);
                        throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
                        		"Failed to update permissions.");
                    }
                }
			} catch (ResourceException e) {
				 throw e;
			} catch (Exception e) {
				throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
                		"Failed to update permissions for " + path);
            }
		} 
		catch (ResourceException e) {
			throw e;
		}
		catch (Throwable e) {
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
					"Failed to update permissions for " + path, e);
		} 
		finally {
			try {remoteDataClient.disconnect();} catch (Exception e) {}
		}
	}

	/**
	 * Processes the delete operation on a particular file or folder. 
	 * This will clear all permissions for that file or directory, but 
	 * the file or directory itself will remain unchanged. Only admin 
	 * and above can grant or change permissions.
	 */
	@Delete
	public Representation remove() throws ResourceException 
	{
		AgaveLogServiceClient.log(AgaveLogServiceClient.ServiceKeys.FILES02.name(), 
				AgaveLogServiceClient.ActivityKeys.IOPemsDelete.name(), 
				username, "", getRequest().getClientInfo().getUpstreamAddress());
		
		try 
		{
			// make sure the resource they are looking for is available.
			if (system == null) 
			{
	        	if (StringUtils.isEmpty(systemId)) {
	        		throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, 
	        				"No default storage system found. Please register a system and set it as your default. ");
	        	} else {
	        		throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, 
	        				"No resource found for user with system id '" + systemId + "'");
	        	}
	        }
			else if (system.isPubliclyAvailable() && 
					system.getType().equals(RemoteSystemType.EXECUTION)) 
			{
				throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN, 
						"User does not have access to view the requested resource. " + 
						"File access is restricted to administrators on public execution systems.");
			}
			
			try {
				this.remoteDataClient = system.getRemoteDataClient(internalUsername);
			} 
			catch(RemoteDataException e)
			{
				throw new ResourceException(Status.CLIENT_ERROR_PRECONDITION_FAILED, 
						e.getMessage(), e);
			} 
			catch (Exception e) {	
				throw new ResourceException(Status.CLIENT_ERROR_PRECONDITION_FAILED, 
						"Unable to establish a connection to the remote server. " + 
								e.getMessage(), e);
			}
			
			try 
			{
				String originalPath = getRequest().getOriginalRef().toUri().getPath();
				path = PathResolver.resolve(owner, originalPath);
				if (system.isPubliclyAvailable()) {
					owner = PathResolver.getOwner(originalPath);
				}
			} 
			catch (Exception e) 
			{
				throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
						"Invalid file path " + path);
			}
			
			LogicalFile logicalFile = null;
            
			try { 
            	logicalFile=LogicalFileDao.findBySystemAndPath(system, remoteDataClient.resolvePath(path)); 
            } catch(Exception e) {}
            
            PermissionManager pm = new PermissionManager(system, remoteDataClient, logicalFile, username);
			
			// check for the file on disk
			try 
			{
				if (!pm.canWrite(remoteDataClient.resolvePath(path))) {
					throw new PermissionException();
				}
				
                remoteDataClient.authenticate();
			} 
			catch (PermissionException e) {
				throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN, 
						"User does not have access to change permissions on " + path);
			}
			catch (Exception e) {
				throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
						"Failed to authenticate to " + system.getId());
			}
			
			try 
			{
				if (!remoteDataClient.doesExist(path) && logicalFile == null)
				{
//					if (logicalFile != null) {
//						LogicalFileDao.deleteSubtreePath(logicalFile.getPath(), system.getId());
//					}
//            	   
					throw new FileNotFoundException("File/folder does not exist");
				}
				
				// check for forced recursive operation in url query...this is the counterpart to 
				// making a POST { username: "*", permission: "" }
				String sRecursive = Request.getCurrent().getOriginalRef().getQueryAsForm().getFirstValue("recursive");
				boolean recursive = BooleanUtils.toBoolean(sRecursive);
                
				pm.clearPermissions(recursive);
				
				return new AgaveSuccessRepresentation();
			} 
			catch (PermissionException e) {
				throw new ResourceException(Status.SERVER_ERROR_INTERNAL, 
						"Failed to remove permissions.");
			}
			catch (FileNotFoundException e) {
				throw new ResourceException(Status.SERVER_ERROR_INTERNAL, e);
			}
			catch (Exception e) {
				throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
						"Failed to retrieve information for " + path);
			}
        } 
		catch (ResourceException e) {
			throw e;
		}
		catch (Throwable e) {
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
					"Failed to retrieve information for " + path);
		}
		finally {
        	try {remoteDataClient.disconnect();} catch (Exception e) {}
        }
	}
}