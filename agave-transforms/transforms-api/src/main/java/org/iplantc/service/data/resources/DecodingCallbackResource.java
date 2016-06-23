/**
 * 
 */
package org.iplantc.service.data.resources;

import java.io.IOException;
import java.net.URI;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.common.representation.IplantErrorRepresentation;
import org.iplantc.service.common.representation.IplantSuccessRepresentation;
import org.iplantc.service.data.dao.DecodingTaskDao;
import org.iplantc.service.data.exceptions.TransformPersistenceException;
import org.iplantc.service.data.model.DecodingTask;
import org.iplantc.service.data.transform.FileTransform;
import org.iplantc.service.data.transform.FileTransformFilter;
import org.iplantc.service.data.transform.FileTransformProperties;
import org.iplantc.service.data.transform.FilterChain;
import org.iplantc.service.data.util.ServiceUtils;
import org.iplantc.service.io.dao.LogicalFileDao;
import org.iplantc.service.io.exceptions.InvalidFileTransformFilterException;
import org.iplantc.service.io.exceptions.TransformException;
import org.iplantc.service.io.model.FileEvent;
import org.iplantc.service.io.model.LogicalFile;
import org.iplantc.service.io.model.enumerations.FileEventType;
import org.iplantc.service.io.model.enumerations.TransformTaskStatus;
import org.iplantc.service.io.util.PathResolver;
import org.iplantc.service.systems.manager.SystemManager;
import org.iplantc.service.transfer.RemoteDataClient;
import org.iplantc.service.transfer.URLCopy;
import org.iplantc.service.transfer.dao.TransferTaskDao;
import org.iplantc.service.transfer.exceptions.RemoteDataException;
import org.iplantc.service.transfer.model.TransferTask;
import org.quartz.SchedulerException;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;

/**
 * Class to handle update triggers from transform routines
 * 
 * @author dooley
 * 
 */
public class DecodingCallbackResource extends AbstractTransformResource 
{
	private static final Logger log = Logger.getLogger(DecodingCallbackResource.class); 

	private String callbackKey;
	private String sStatus;
	private String internalUsername;
	private SystemManager manager;
	private RemoteDataClient remoteClient;
	
	/**
	 * @param context
	 * @param request
	 * @param response
	 */
	public DecodingCallbackResource(Context context, Request request, Response response) {
		super(context, request, response);

		this.callbackKey = (String) request.getAttributes().get("callbackKey");
		
		this.sStatus = (String) request.getAttributes().get("status");
		
        manager = new SystemManager();
        
		getVariants().add(new Variant(MediaType.APPLICATION_JSON));
	}

	/** 
	 * This method represents the HTTP GET action. Invoking this service will update the
	 * status of a TransferTask and it's associated LogicalFile. If an event is registered
	 * with the import, it will be updated as well. 
	 */
	@SuppressWarnings("unused")
	@Override
	public Representation represent(Variant variant) throws ResourceException 
	{
		LogicalFile file = null;
		try 
		{
			if (!ServiceUtils.isValid(callbackKey)) {
				getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
				return new IplantErrorRepresentation("Invalid callback key");
			}
			
			DecodingTask task;
			try {
				task = DecodingTaskDao.getTransformTaskByCallBackKey(callbackKey);
				
				if (task == null) {
					getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
					return new IplantErrorRepresentation("Invalid callback key");
				} else {
					file = task.getLogicalFile();
					
					TenancyHelper.setCurrentEndUser(file.getOwner());
					TenancyHelper.setCurrentEndUser(file.getTenantId());
				}
			} catch (TransformPersistenceException e) {
				getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
				return new IplantErrorRepresentation("Failed to fetch decoding task");
			}
			
			
			if (!isValidStatus(sStatus)) {
				getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
				return new IplantErrorRepresentation("Invalid status value");
			}
			
			log.debug("Received status update " + sStatus + " for file " + task.getSourcePath());
			TransformTaskStatus status = null;
			try {
				status = TransformTaskStatus.valueOf(sStatus.toUpperCase());
				task.setStatus(status);
			} catch (Exception e) {
				log.error("Unknown status, " + sStatus + ", given on callback");
				return new IplantErrorRepresentation("Invalid status value");
			}
			
			try {
				DecodingTaskDao.persist(task);
			} catch (TransformPersistenceException e) {
				log.error("Failed to update status of transform task " + 
						task.getId() + " to " + status.name(), e);
			}
			
			if (status.equals(TransformTaskStatus.TRANSFORMING_COMPLETED)) 
			{	
				FileTransformProperties transformProperties = new FileTransformProperties();
				FileTransform srcTransform = null;
				 
				try 
				{
					this.remoteClient = task.getLogicalFile().getSystem().getRemoteDataClient(internalUsername);
				       
					srcTransform = transformProperties.getTransform(task.getSrcTransform());
					
					if (srcTransform != null) {
						
						log.debug("Completed filter " + task.getCurrentFilter() + " for transform " + task.getDestTransform() + " on file " + task.getSourcePath());
						
						// start the next stage of the filtering process by pushing another transform task onto the queue
						FilterChain decoder = srcTransform.getDecoder(task.getDestTransform());
						if (decoder == null) {
							
						}
						FileTransformFilter nextFilter = srcTransform.getDecoder(task.getDestTransform()).getNextFilter(task.getCurrentFilter());
						
						if (nextFilter == null) 
						{
							// transform has completed all filters, stage the file out to the supplied address
							log.debug("Staging file " + task.getDestPath() + " to " + task.getDestinationUri());
							
							if (!task.getDestinationUri().contains("://")) 
							{
								// don't overwrite by default
								if (remoteClient.doesExist(task.getDestinationUri())) {
									throw new IOException("Target path already exists");
								} else {
									remoteClient.copy(task.getDestPath(), task.getDestinationUri());
								}
								
								// add a logical record for the new file
								LogicalFile logicalFile = LogicalFileDao.findBySystemAndPath(task.getStorageSystem(), task.getSourcePath());
								if (logicalFile == null) {
									logicalFile = new LogicalFile();
									logicalFile.setName(FilenameUtils.getName(task.getDestPath()));
									logicalFile.setPath(remoteClient.resolvePath(task.getDestPath()));
									logicalFile.setOwner(PathResolver.getOwner(task.getDestPath()));
								}
								logicalFile.setSourceUri(task.getSourcePath());
								logicalFile.setNativeFormat(task.getDestTransform());
								logicalFile.setStatus(TransformTaskStatus.TRANSFORMING_COMPLETED.name());
								LogicalFileDao.persist(logicalFile);
								
							} 
							else 
							{
								// not sure why we're verifying the destination uri here. it should have been verified prior to queueing.
								URI destUri = new URI(task.getDestinationUri());
								
								RemoteDataClient destClient = manager.getUserDefaultStorageSystem(PathResolver.getOwner(task.getDestPath())).getRemoteDataClient();
								URLCopy urlCopy = new URLCopy(remoteClient, destClient);
								try {
									TransferTask transferTask = new TransferTask(task.getSourcePath(), task.getDestPath(), task.getLogicalFile().getOwner(), null, null);
									TransferTaskDao.persist(transferTask);
									urlCopy.copy(task.getSourcePath(), task.getDestPath(), transferTask);
								} catch (IOException e) {
									throw new RemoteDataException("Failed to copy file to destination location");
								}
								finally {
									try { destClient.disconnect(); } catch (Exception e) {}
								}
							}	
							
							// delete the temp file now that the transform is complete
							try {
								remoteClient.delete(task.getDestPath());
							} catch (Exception e) {
								log.error("Failed to delete " + task.getDestPath() + " after failed encoding transform + " + srcTransform.getId() + ":" + decoder.getName());
							} 
							
							// trigger the event...this should have been done already in the worker
							LogicalFileDao.updateTransferStatus(task.getLogicalFile(), TransformTaskStatus.TRANSFORMING_COMPLETED, getAuthenticatedUsername());
						} 
						else 
						{
							// there are still filters yet to run, so queue up the next one using the
							String tmpFilePath = "/scratch/" + FilenameUtils.getName(task.getSourcePath()) + "-" + System.currentTimeMillis();
							
							if (nextFilter.isUseOriginalFile()) {
								// use the original source file
								DecodingTaskDao.enqueueDecodingTask(task.getLogicalFile(), task.getStorageSystem(), task.getSourcePath(), tmpFilePath, task.getSrcTransform(), task.getDestTransform(), nextFilter.getName(), task.getDestinationUri(), task.getOwner());
							} else {
								// destination file of the previous task as the input to this one.
								DecodingTaskDao.enqueueDecodingTask(task.getLogicalFile(), task.getStorageSystem(), task.getDestPath(), tmpFilePath, task.getSrcTransform(), task.getDestTransform(), nextFilter.getName(), task.getDestinationUri(), task.getOwner());
							}
						}
					} else {
						log.info("File transform " + task.getSrcTransform() + " no longer exists");
						file.setStatus(TransformTaskStatus.TRANSFORMING_FAILED);
						file.addContentEvent(new FileEvent(FileEventType.TRANSFORMING_FAILED,
								"File transform " + task.getSrcTransform() + " no longer exists.",
								task.getOwner() ));
						LogicalFileDao.persist(file);
					}
				} 
				catch (InvalidFileTransformFilterException e) 
				{
					log.info("File transform " + task.getDestTransform() + " no longer exists for transform " + task.getSrcTransform());
					file.setStatus(TransformTaskStatus.TRANSFORMING_FAILED);
					file.addContentEvent(new FileEvent(FileEventType.TRANSFORMING_FAILED,
							"File transform " + task.getDestTransform() + " no longer exists for transform " + task.getSrcTransform(),
							task.getOwner()));
					LogicalFileDao.persist(file);
				} 
				catch (SchedulerException e) 
				{
					// no handler for this file, so mark it as raw and move on
					log.info("Failed invoke next filter after " + task.getCurrentFilter() + " for transform " + task.getDestTransform());
					file.setStatus(TransformTaskStatus.TRANSFORMING_FAILED);
					file.addContentEvent(new FileEvent(FileEventType.TRANSFORMING_FAILED,
							"Failed to invoke next filter after " + task.getCurrentFilter() + " for transform " + task.getDestTransform(),
							task.getOwner()));
					LogicalFileDao.persist(file);
				} 
				catch (Exception e) {
					log.info(e.getMessage());
					file.setStatus(TransformTaskStatus.TRANSFORMING_FAILED);
					file.addContentEvent(new FileEvent(FileEventType.TRANSFORMING_FAILED,
							"Decoding failed due to internal error: " + e.getMessage(),
							task.getOwner()));
					LogicalFileDao.persist(file);
				}
				
			} else {
				// decoding filter failed
				file.setStatus(TransformTaskStatus.TRANSFORMING_FAILED);
				file.addContentEvent(new FileEvent(FileEventType.TRANSFORMING_FAILED,
						"Decoding failed unexpectedly. Consult transfer history for detailed explaination.",
						task.getOwner()));
				LogicalFileDao.persist(file);
			}
			
			return new IplantSuccessRepresentation();
		} 
		finally {
			try { remoteClient.disconnect(); } catch (Exception e1) {}
		}
		
	}

	private boolean isValidStatus(String sStatus) {
		
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
		return true;
	}

	/* (non-Javadoc)
	 * @see org.restlet.resource.Resource#allowPost()
	 */
	@Override
	public boolean allowPost() {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.restlet.resource.Resource#allowPut()
	 */
	@Override
	public boolean allowPut() {
		return false;
	}
	
}
