package org.iplantc.service.io.queue;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

import java.io.File;
import java.nio.channels.ClosedByInterruptException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.StaleObjectStateException;
import org.iplantc.service.common.exceptions.PermissionException;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.data.transform.FileTransform;
import org.iplantc.service.data.transform.FileTransformFilter;
import org.iplantc.service.data.transform.FileTransformProperties;
import org.iplantc.service.data.transform.FilterChain;
import org.iplantc.service.io.dao.LogicalFileDao;
import org.iplantc.service.io.dao.QueueTaskDao;
import org.iplantc.service.io.exceptions.TransformException;
import org.iplantc.service.io.manager.FileEventProcessor;
import org.iplantc.service.io.model.FileEvent;
import org.iplantc.service.io.model.LogicalFile;
import org.iplantc.service.io.model.enumerations.FileEventType;
import org.iplantc.service.io.model.enumerations.StagingTaskStatus;
import org.iplantc.service.io.model.enumerations.TransformTaskStatus;
import org.iplantc.service.notification.managers.NotificationManager;
import org.iplantc.service.transfer.RemoteDataClient;
import org.iplantc.service.transfer.RemoteTransferListener;
import org.iplantc.service.transfer.URLCopy;
import org.iplantc.service.transfer.dao.TransferTaskDao;
import org.iplantc.service.transfer.exceptions.RemoteDataException;
import org.iplantc.service.transfer.local.Local;
import org.iplantc.service.transfer.model.Range;
import org.iplantc.service.transfer.model.TransferTask;
import org.quartz.InterruptableJob;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.quartz.UnableToInterruptJobException;
import org.quartz.impl.StdSchedulerFactory;

/**
 * Class to encapsulate staging of files directly uploaded to the api to the remote system.
 * Files are first cached on the server, then transfered in a single trigger UploadJob 
 * execution. This allows the service to respond immediately without blocking the client
 * for the transfer to the remote system.
 *  
 * @author dooley
 *
 */
public class UploadJob implements InterruptableJob 
{
	private static final Logger log = Logger.getLogger(StagingJob.class);
	
	protected RemoteDataClient remoteDataClient = null;
	protected LogicalFile logicalFile = null;
	protected String cachedFile = null;
    protected URLCopy urlCopy = null;
    
	public UploadJob() {}
	
	/* (non-Javadoc)
	 * @see org.quartz.Job#execute(org.quartz.JobExecutionContext)
	 */
	@Override
	public void execute(JobExecutionContext context) 
	throws JobExecutionException 
	{
		try 
		{
			JobDataMap dataMap = context.getJobDetail().getJobDataMap();  // Note the difference from the previous example
			doUpload(dataMap);
		}
		catch(Throwable e) {
			log.error("Failed to process cached file " + context.get("cachedFile") + " while transferring to remote destination.", e);
		}
	}
	
	/**
	 * Perform the actual upload.
	 * 
	 * @param dataMap
	 * @throws JobExecutionException
	 */
	public void doUpload(JobDataMap dataMap) throws JobExecutionException
	{
		RemoteDataClient remoteDataClient = null;
		String owner = null;
		String createdBy = null;
		String cachedFile = null;
		try {
			try {
				Long logicalFileId = dataMap.getLong("logicalFileId");
				cachedFile = dataMap.getString("cachedFile");
				owner = dataMap.getString("owner");
				createdBy = dataMap.getString("createdBy");
				String tenantCode = dataMap.getString("tenantId");
				
				String uploadSource = dataMap.getString("sourceUrl");
				String uploadDest = dataMap.getString("destUrl");
				boolean isRangeCopyOperation = dataMap.getBooleanFromString("isRangeCopyOperation");
				long rangeIndex = dataMap.getLong("rangeIndex");
				long rangeSize = dataMap.getLong("rangeSize");
				
				TenancyHelper.setCurrentTenantId(tenantCode);
				TenancyHelper.setCurrentEndUser(createdBy);
				
				logicalFile = LogicalFileDao.findById(logicalFileId);
				
				if (logicalFile == null) {
					log.info("Aborting upload task from " + uploadSource + " to " + uploadDest + " because logicalFile has been deleted.");
					return;
				}
				
				remoteDataClient = logicalFile.getSystem().getRemoteDataClient(logicalFile.getInternalUsername());
				remoteDataClient.authenticate();
				
				String agavePath = logicalFile.getAgaveRelativePathFromAbsolutePath();
				
	//			if (StringUtils.startsWith(agavePath, "/")) {
	//				agavePath = FilenameUtils.getPathNoEndSeparator(agavePath);
	//				if (!StringUtils.startsWith(agavePath, "/")) {
	//					agavePath = "/" + agavePath;
	//				}
	//			}
				
				TransferTask transferTask = new TransferTask(
				        uploadSource, 
				        uploadDest, 
				        createdBy, 
	                    null, 
	                    null);
	            
	            TransferTaskDao.persist(transferTask);
				
	            urlCopy = new URLCopy(new Local(null, "/", "/"), remoteDataClient);
	            
				if (isRangeCopyOperation) {
				    transferTask = urlCopy.copyRange(cachedFile, 0, new File(cachedFile).length(), agavePath, rangeIndex, transferTask);
				} else {
				    transferTask = urlCopy.copy(cachedFile, agavePath, transferTask);
				}
	    		
	    		if (logicalFile.getOwner() != null && remoteDataClient.isPermissionMirroringRequired()) {
		            try {
		            	remoteDataClient.setOwnerPermission(remoteDataClient.getUsername(), logicalFile.getAgaveRelativePathFromAbsolutePath(), true);
		            	remoteDataClient.setOwnerPermission(owner, logicalFile.getAgaveRelativePathFromAbsolutePath(), true);
		            } catch (RemoteDataException e) {
		            	if (! (e.getCause() instanceof org.irods.jargon.core.exception.InvalidUserException)) {
		            		throw new PermissionException(e);
		            	}
		            }
	            }
	    		
	//    		logicalFile.addContentEvent(new FileEvent("UPLOAD",
	//                    "Your upload of " + logicalFile.getName() +
	//                    " completed successfully. You can access the raw file on " + logicalFile.getSystem().getSystemId() + " at " + 
	//                    logicalFile.getPath() + " or via the API at " + 
	//                    logicalFile.getPublicLink() + "."));
	    		
	    		FileEventProcessor eventProcessor = new FileEventProcessor(); 
	    		eventProcessor.processContentEvent(logicalFile, new FileEvent(FileEventType.UPLOAD,
	                        FileEventType.UPLOAD.getDescription(),
	                        createdBy));
	//    		NotificationManager.process(logicalFile.getUuid(), "UPLOAD", createdBy, logicalFile.toJSON());
	    		
	    		log.info("Completed staging file " + logicalFile.getAgaveRelativePathFromAbsolutePath() + " for user " + owner);
	            
	            // file will be untouched after staging, so just mark as completed
	            if (StringUtils.isEmpty(logicalFile.getNativeFormat()) || 
	                    logicalFile.getNativeFormat().equals("raw")) 
	            {
	                // update the file task
	                logicalFile.setStatus(StagingTaskStatus.STAGING_COMPLETED.name());
	                logicalFile.addContentEvent(FileEventType.STAGING_COMPLETED, createdBy);
	                
	                logicalFile.addContentEvent(new FileEvent(FileEventType.TRANSFORMING_COMPLETED,
	                        "Your scheduled transfer of " + uploadSource +
	                        " completed staging. You can access the raw file on " + logicalFile.getSystem().getName() + " at " + 
	                        logicalFile.getPath() + " or via the API at " + 
	                        logicalFile.getPublicLink() + ".",
	                        createdBy));
	                
	                LogicalFileDao.persist(logicalFile);
	            }
	            else 
	            {
	                logicalFile.setStatus(StagingTaskStatus.STAGING_COMPLETED.name());
	                logicalFile.addContentEvent(FileEventType.STAGING_COMPLETED, createdBy);
	                logicalFile.addContentEvent(new FileEvent(FileEventType.PREPROCESSING, 
	                        "Calculating transformation pipeline for file type " 
	                        		+ logicalFile.getNativeFormat(),
	                		createdBy));
	                LogicalFileDao.persist(logicalFile);
	            
	                // enable the user to specify the input formatting. Check against our internally
	                // supported formatting 
	                // ignore user-defined formatting for now. Let the service auto-discover it
	                FileTransformProperties transformProperties = new FileTransformProperties();
	                FileTransform transform = transformProperties.getTransform(logicalFile.getNativeFormat());
	                
	                FileTransformFilter transformFilter = null;
	                
	                // no handler for this file, so mark it as raw and skip transform queue
	                if (transform == null) 
	                {
	                    String message = "No transform found. Staging complete for file " + logicalFile.getAgaveRelativePathFromAbsolutePath() + " for user " + createdBy;
	                    log.info(message);
	                    logicalFile.setNativeFormat("raw");
	                    logicalFile.setStatus(TransformTaskStatus.TRANSFORMING_COMPLETED.name());
	                    logicalFile.addContentEvent(new FileEvent(FileEventType.TRANSFORMING_FAILED, 
	                            "Your scheduled transfer of " + logicalFile.getSourceUri() + 
		                            " completed staging, but has failed encoding because the transform " +
		                            "you specified was not found. You can access the raw file on " + 
		                            logicalFile.getSystem().getName() + " at " + 
		                            logicalFile.getPath() + " or via the API at " + 
		                            logicalFile.getPublicLink() + ".",
	                            createdBy));
	                    
	                    LogicalFileDao.persist(logicalFile);
	                }
	                // no handler for this file, so mark it as raw and skip transform queue
	                else if (!transform.isEnabled()) 
	                {
	                    log.debug("Transform " + transform.getId() + " is disabled. Staging complete for file " + logicalFile.getAgaveRelativePathFromAbsolutePath() + " for user " + createdBy);
	                    
	                    logicalFile.setNativeFormat("raw");
	                    logicalFile.setStatus(TransformTaskStatus.TRANSFORMING_COMPLETED.name());
	                    
	                    logicalFile.addContentEvent(new FileEvent(FileEventType.TRANSFORMING_COMPLETED, 
	                            "Your scheduled transfer of " + logicalFile.getSourceUri() + 
		                            " has completed successfully. You can access this file on " + 
		                            logicalFile.getSystem().getName() + " at " + 
		                            logicalFile.getPath() + " or via the API at " + 
		                            logicalFile.getPublicLink() + ".",
	                            createdBy));
	                    
	                    LogicalFileDao.persist(logicalFile);
	                }
	                // otherwise send it to the trnasform queue
	                else 
	                {
	                    // push the staged file into the transform queue
	                    String tmpFilePath = "/scratch/" + logicalFile.getName() + "-" + System.currentTimeMillis();
					
						FilterChain chain = transform.getEncodingChain();
						if (chain == null) throw new SchedulerException("No encoding chain found for " + transform.getId());
						
						FileTransformFilter filter = chain.getFirstFilter();
						if (filter == null) throw new SchedulerException("No initial file transform filter found for " + chain.getId());
						
						String filterName = filter.getName();
						if (filterName == null) throw new SchedulerException("First file transform filter for " + chain.getId() + " has no name");
						
						QueueTaskDao.enqueueEncodingTask(logicalFile, 
														logicalFile.getSystem(), 
														logicalFile.getAgaveRelativePathFromAbsolutePath(), 
														tmpFilePath, 
														logicalFile.getNativeFormat(), 
														transform.getEncodingChain().getFirstFilter().getName(),
														createdBy);
					}
					
	            }
			}
			catch (ClosedByInterruptException e) {
				if (logicalFile != null) {
					LogicalFileDao.updateTransferStatus(logicalFile, 
							StagingTaskStatus.STAGING_FAILED.name(), 
							"Failed to transfer uploaded file to the remote system. "
							+ "The server was stopped prior to the transfer completing "
							+ "and the file was no longer available. No further action "
							+ "can be taken to recover. Please attempt the upload again.", 
							createdBy);
				}
				
				log.error("Failed to transfer uploaded file " 
	                    + dataMap.getString("cachedFile") + " to remote destination for "
	            		+ " when the worker was interrupted.");
				
				Thread.currentThread().interrupt();
				
	        }
			catch (SchedulerException e) {
				if (logicalFile != null) {
					// no handler for this file, so mark it as raw and move on
					log.info("Failed to submit file " + logicalFile.getAgaveRelativePathFromAbsolutePath() + 
		                    " to the transfer queue for " + createdBy);
		            LogicalFileDao.updateTransferStatus(logicalFile, TransformTaskStatus.TRANSFORMING_COMPLETED, createdBy);
				}
				
	            log.error("Failed to transfer cached file " + 
	                    dataMap.getString("cachedFile") + " to remote destination.", e);
	        }
			catch (StaleObjectStateException e) {
				log.debug("Just avoided a file upload race condition from worker");
			}
			catch (RemoteDataException e) {
				if (logicalFile != null) {
					LogicalFileDao.updateTransferStatus(logicalFile, StagingTaskStatus.STAGING_FAILED, createdBy);
					try {
						if (logicalFile.getOwner() != null && remoteDataClient.isPermissionMirroringRequired()) {
			            	remoteDataClient.setOwnerPermission(remoteDataClient.getUsername(), logicalFile.getAgaveRelativePathFromAbsolutePath(), true);
			            	remoteDataClient.setOwnerPermission(owner, logicalFile.getAgaveRelativePathFromAbsolutePath(), true);
			            }
					} catch (Exception e1) {}
				}
				log.error("File upload worker failed to transfer the cached file " + 
						dataMap.getString("cachedFile") + " to remote destination.", e); 
			}
			catch (Throwable e) {
				if (logicalFile != null) {
					LogicalFileDao.updateTransferStatus(logicalFile, StagingTaskStatus.STAGING_FAILED, createdBy);
					
					try {
						if (logicalFile.getOwner() != null && remoteDataClient.isPermissionMirroringRequired()) {
			            	remoteDataClient.setOwnerPermission(remoteDataClient.getUsername(), logicalFile.getAgaveRelativePathFromAbsolutePath(), true);
			            	remoteDataClient.setOwnerPermission(owner, logicalFile.getAgaveRelativePathFromAbsolutePath(), true);
			            }
					} catch (Exception e1) {}
				}
				log.error("File upload worker failed unexpectedly while attempting to transfer cached file " + 
						dataMap.getString("cachedFile") + " to remote destination.", e);
			}
		} catch (Throwable e) {
			log.error("File upload worker failed unexpectedly while attempting to transfer cached "
				+ "file to remote destination.", e);
		}
		finally {
			try { remoteDataClient.disconnect(); } catch (Exception e) {}
			if (StringUtils.isNotEmpty(cachedFile)) {
				FileUtils.deleteQuietly(new File(cachedFile).getParentFile());
			}
		}
	}
	
    @Override
    public void interrupt() throws UnableToInterruptJobException {
    	try {
	    	if (logicalFile != null) {
	    		LogicalFileDao.updateTransferStatus(logicalFile, StagingTaskStatus.STAGING_FAILED, logicalFile.getOwner());
	    	}
    	}
    	catch (Exception e) {
    		log.error("Failed to updated logical file status to STAGING_FAILED due to "
    				+ "worker interrupt during upload file staging.", e);
    	}
    	finally {
    		Thread.currentThread().interrupt();
    	}
		
    }
}