/**
 * 
 */
package org.iplantc.service.io.resources;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.common.representation.AgaveSuccessRepresentation;
import org.iplantc.service.data.transform.FileTransform;
import org.iplantc.service.data.transform.FileTransformFilter;
import org.iplantc.service.data.transform.FileTransformProperties;
import org.iplantc.service.io.Settings;
import org.iplantc.service.io.dao.LogicalFileDao;
import org.iplantc.service.io.dao.QueueTaskDao;
import org.iplantc.service.io.exceptions.InvalidFileTransformFilterException;
import org.iplantc.service.io.exceptions.TransformException;
import org.iplantc.service.io.model.EncodingTask;
import org.iplantc.service.io.model.FileEvent;
import org.iplantc.service.io.model.LogicalFile;
import org.iplantc.service.io.model.enumerations.FileEventType;
import org.iplantc.service.io.model.enumerations.TransformTaskStatus;
import org.iplantc.service.io.util.ServiceUtils;
import org.iplantc.service.systems.dao.SystemDao;
import org.iplantc.service.systems.manager.SystemManager;
import org.iplantc.service.systems.model.RemoteSystem;
import org.iplantc.service.transfer.RemoteDataClient;
import org.restlet.Request;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;

/**
 * Class to handle update triggers from transform routines
 * 
 * @author dooley
 * 
 */
public class EncodingCallbackResource extends AbstractFileResource {
	private static final Logger log = Logger.getLogger(EncodingCallbackResource.class); 

	private String callbackKey;
	private String sStatus;
	
    private RemoteSystem system;
    private RemoteDataClient remoteDataClient;

	@Override
	public void doInit() {
		this.callbackKey = (String)Request.getCurrent().getAttributes().get("callbackKey");
		this.sStatus = (String)Request.getCurrent().getAttributes().get("status");
	}

	/** 
	 * This method represents the HTTP GET action. Invoking this service will update the
	 * status of a TransferTask and it's associated LogicalFile. If an event is registered
	 * with the import, it will be updated as well. 
	 */
	@Get
	public Representation represent() 
	throws ResourceException 
	{
		LogicalFile file = null;
		try 
		{
			if (!ServiceUtils.isValid(callbackKey)) {
				getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, 
						"Invalid callback key");
			}
			
			EncodingTask task = null;
			try 
			{
				task = QueueTaskDao.getTransformTaskByCallBackKey(callbackKey);
				
				if (task == null) {
					throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
							"Invalid callback key");
				} 
				else
				{
					String systemId = (String)getRequest().getAttributes().get("systemId");
		            if (!StringUtils.isEmpty(systemId)) {
		                this.system = new SystemDao().findBySystemId(systemId);
		            } else {
		                // If no system specified, select the user default system
		                this.system = (RemoteSystem)new SystemManager().getUserDefaultStorageSystem(task.getLogicalFile().getOwner());
		            }
		            
		            if (this.system == null) {
		            	getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, 
		            			"Unknown status, " + sStatus + " given on callback");
		            }
		            
		            remoteDataClient = this.system.getRemoteDataClient(task.getLogicalFile().getInternalUsername());
		            remoteDataClient.authenticate();
				}
			} 
			catch (TransformException e) {
				throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, e);
			} 
			catch (Exception e) {
				throw new ResourceException(Status.SERVER_ERROR_INTERNAL, e);
			}
			
			if (!isValidStatus(sStatus)) {
				throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
						"Unknown status, " + sStatus + " given on callback");
			}
			
			log.debug("Received status update " + sStatus + " for file " + task.getSourcePath());
			TransformTaskStatus status = null;
			try 
			{
				status = TransformTaskStatus.valueOf(sStatus.toUpperCase());
				task.setStatus(status);
				file = task.getLogicalFile();
				file.addContentEvent(FileEventType.valueOf(status.name()), task.getOwner());
            	LogicalFileDao.persist(file);
			} 
			catch (Exception e) 
			{
				file.addContentEvent(new FileEvent(FileEventType.UNKNOWN,
							"Unknown status, " + sStatus + ", given on encoding callback",
							getAuthenticatedUsername()));
				throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
						"Unknown status, " + sStatus + ", given on callback", e);
			}
			
			log.debug("Received status update " + status + " for file " + file.getAgaveRelativePathFromAbsolutePath());
			task.setStatus(status);
			QueueTaskDao.persist(task);
			
			if (status.equals(TransformTaskStatus.TRANSFORMING_COMPLETED)) {
				
				FileTransformProperties transformProperties = new FileTransformProperties();
				FileTransform transform = null;
				
				try {
					transform = transformProperties.getTransform(task.getTransformName());
					
					if (transform != null) {
						
						log.debug("Completed filter " + task.getTransformFilterName() + " for transform " + task.getTransformName() + " on file " + file.getAgaveRelativePathFromAbsolutePath());
						
						// start the next stage of the filtering process by pushing another transform task onto the queue
						FileTransformFilter nextFilter = transform.getEncodingChain().getNextFilter(task.getTransformFilterName());
						
						if (nextFilter == null) 
						{
							// transform has completed all filters
							file.setStatus(TransformTaskStatus.TRANSFORMING_COMPLETED);
							file.addContentEvent(new FileEvent(FileEventType.TRANSFORMING_COMPLETED, 
									"Your scheduled transfer of " + file.getSourceUri() + 
									" has completed successfully. You can access this file " + file.getSystem().getSystemId() + " at /" + 
									task.getLogicalFile().getPath() + " or through the API at " + 
									Settings.IPLANT_IO_SERVICE + "/media/system/" + file.getSystem() + "/" + task.getLogicalFile().getAgaveRelativePathFromAbsolutePath() + ".",
									getAuthenticatedUsername()));
							LogicalFileDao.persist(file);
						} else {
							// there are still filters yet to run, so queue up the next one using the
							String tmpFilePath = "scratch/" + file.getName() + "-" + System.currentTimeMillis();
							
							if (nextFilter.isUseOriginalFile()) {
								// use the original source file
								QueueTaskDao.enqueueEncodingTask(task.getLogicalFile(), task.getLogicalFile().getSystem(), file.getAgaveRelativePathFromAbsolutePath(), tmpFilePath, transform.getName(), nextFilter.getName(), task.getOwner());
							} else {
								// destination file of the previous task as the input to this one.
								QueueTaskDao.enqueueEncodingTask(task.getLogicalFile(), task.getLogicalFile().getSystem(), task.getDestPath(), tmpFilePath, transform.getName(), nextFilter.getName(), task.getOwner());
							}
						}
					} else {
						log.info("File transform " + task.getTransformName() + " no longer exists");
						
						file.setStatus(TransformTaskStatus.TRANSFORMING_FAILED);
						file.addContentEvent(new FileEvent(FileEventType.TRANSFORMING_FAILED, 
								"Your scheduled transfer of " + file.getSourceUri() + 
								" has failed because File transform " + task.getTransformName() + " no longer exists",
								getAuthenticatedUsername()));
						LogicalFileDao.persist(file);
					}
				
				} 
				catch (TransformException e) {
					log.info(e.getMessage());
					LogicalFileDao.updateTransferStatus(file, TransformTaskStatus.TRANSFORMING_FAILED, task.getOwner());
				} 
				catch (InvalidFileTransformFilterException e) {
					log.info("File transform " + task.getTransformName() + " no longer exists for transform " + transform.getName());
					//if (ServiceUtils.isValid(eventId)) EventClient.update(Settings.EVENT_API_KEY, eventId, EventClient.EVENT_STATUS_FAILED);
					file.setStatus(TransformTaskStatus.TRANSFORMING_FAILED);
					file.addContentEvent(new FileEvent(FileEventType.TRANSFORMING_FAILED,
								"File transform " + task.getTransformName() 
									+ " no longer exists for transform " + transform.getName(),
								getAuthenticatedUsername()));
					LogicalFileDao.persist(file);
				} catch (Exception e) {
					// no handler for this file, so mark it as raw and move on
					log.info("Failed invoke next filter after " + task.getTransformFilterName() + " for transform " + task.getTransformName());
					file.setStatus(TransformTaskStatus.TRANSFORMING_FAILED);
					file.addContentEvent(new FileEvent(FileEventType.TRANSFORMING_FAILED,
								"Failed invoke next filter after " + task.getTransformFilterName() + " for transform " + task.getTransformName(),
								getAuthenticatedUsername()));
					LogicalFileDao.persist(file);
				}
			} 
			else if (status.equals(TransformTaskStatus.TRANSFORMING)) 
			{
				log.debug("Transform " +  task.getTransformName() + ":" + task.getTransformFilterName() + " has begun for " + task.getLogicalFile().getPublicLink());
				task.getLogicalFile().setStatus(TransformTaskStatus.TRANSFORMING);
			} 
			else 
			{
				LogicalFileDao.updateTransferStatus(file, TransformTaskStatus.TRANSFORMING_FAILED, task.getOwner());
					
				// delete the staged file since it wasn't transformed properly
				try 
				{	
					remoteDataClient.delete(task.getSourcePath());
				} catch (Exception e) {
					log.error("Failed to delete " + task.getSourcePath() + " after failed encoding transform + " + task.getTransformName() + ":" + task.getTransformFilterName());
				}
				
				try 
				{
					remoteDataClient.delete(task.getDestPath());
				} catch (Exception e) {
					log.error("Failed to delete " + task.getSourcePath() + " after failed encoding transform + " + task.getTransformName() + ":" + task.getTransformFilterName());
				}
			}
			
			return new AgaveSuccessRepresentation();
		}
		catch (ResourceException e) {
			throw e;
		}
		catch (Throwable e) {
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL, e);
        } 
		finally {
        	try {remoteDataClient.disconnect();} catch (Exception e) {}
        	remoteDataClient = null;
        }
	}

	private boolean isValidStatus(String sStatus) 
	{
		TransformTaskStatus status = null;
		try {
			status = TransformTaskStatus.valueOf(sStatus.toUpperCase());
		} catch (Exception e) {
			return false;
		}
		// there are only two possible statuses that the transform routines
		// can return. FAILED or COMPLETED.
		if (status.equals(TransformTaskStatus.TRANSFORMING_FAILED) || 
				status.equals(TransformTaskStatus.TRANSFORMING_COMPLETED) ||
				status.equals(TransformTaskStatus.TRANSFORMING)) 
		{
			return true;
		} else {
			return false;
		}
	}
}
