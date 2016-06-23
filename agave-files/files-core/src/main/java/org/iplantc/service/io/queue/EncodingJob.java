package org.iplantc.service.io.queue;

import java.io.File;
import java.nio.channels.ClosedByInterruptException;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.hibernate.StaleObjectStateException;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.data.transform.FileTransform;
import org.iplantc.service.data.transform.FileTransformFilter;
import org.iplantc.service.data.transform.FileTransformProperties;
import org.iplantc.service.data.transform.TransformLauncher;
import org.iplantc.service.io.Settings;
import org.iplantc.service.io.dao.FileEventDao;
import org.iplantc.service.io.dao.LogicalFileDao;
import org.iplantc.service.io.dao.QueueTaskDao;
import org.iplantc.service.io.exceptions.FileEventProcessingException;
import org.iplantc.service.io.exceptions.LogicalFileException;
import org.iplantc.service.io.exceptions.TaskException;
import org.iplantc.service.io.exceptions.TransformException;
import org.iplantc.service.io.manager.FileEventProcessor;
import org.iplantc.service.io.model.EncodingTask;
import org.iplantc.service.io.model.FileEvent;
import org.iplantc.service.io.model.enumerations.FileEventType;
import org.iplantc.service.io.model.enumerations.StagingTaskStatus;
import org.iplantc.service.io.model.enumerations.TransformTaskStatus;
import org.iplantc.service.notification.managers.NotificationManager;
import org.iplantc.service.transfer.RemoteDataClient;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionException;
import org.quartz.UnableToInterruptJobException;

/**
 * Handles the processing of file encoding tasks.
 * @author dooley
 *
 */
@DisallowConcurrentExecution
public class EncodingJob extends AbstractJobWatch<EncodingTask>
{
	private static final Logger log = Logger.getLogger(EncodingJob.class);
	
	public EncodingJob() {}

	public void doExecute() throws JobExecutionException
	{
	    RemoteDataClient remoteDataClient = null;
	    File tempDir = null;
		try
		{
			this.queueTask.setStatus(TransformTaskStatus.TRANSFORMING);
			QueueTaskDao.persist(this.queueTask);

			file = this.queueTask.getLogicalFile();
			if (file == null) {
//				LogicalFileDao.updateTransferStatus(file, TransformTaskStatus.TRANSFORMING_FAILED);
				log.info("Aborting transform task " + this.queueTask.getId() + " because logicalFile has been deleted.");
				return;
			}
			if (this.queueTask.getTransformName().equalsIgnoreCase("raw"))
            {
				// nothing to be done, just leave the file raw. Usually this will be caught
				// in the service or staging layers, but this leaves the opportunity open
				// for someone to include transformations with "raw" actions. ie, if the contents
				// match a certain regex, then leave them be. Multiple regexes can be supported
				// in this manner.
				log.info("Completed applying transform raw to file " + file.getAgaveRelativePathFromAbsolutePath() + " for user " + file.getOwner());

				FileEvent event = new FileEvent(FileEventType.TRANSFORMING_COMPLETED,
						"Your scheduled transfer of " + this.queueTask.getLogicalFile().getSourceUri() +
						" has completed successfully. You can access this file on " + file.getSystem().getSystemId() + " at /" +
						this.queueTask.getLogicalFile().getPath() + " or through the API at " +
						Settings.IPLANT_IO_SERVICE + "/media/system/" + file.getSystem().getSystemId() + "/" + 
						this.queueTask.getLogicalFile().getAgaveRelativePathFromAbsolutePath() + ".",
            			this.queueTask.getOwner());
				
				FileEventProcessor.processAndSaveContentEvent(file, event);
//				event.setLogicalFile(file);
//                FileEventDao.persist(event);
//                NotificationManager.process(file.getUuid(), event.getStatus(), file.getOwner());
				
				this.queueTask.setStatus(TransformTaskStatus.TRANSFORMING_COMPLETED);
				QueueTaskDao.persist(this.queueTask);
			}
			else
			{
			    FileEvent event = new FileEvent(FileEventType.TRANSFORMING,
	                    "Attempting to apply transform " + this.queueTask.getTransformFilterName(),
	                    queueTask.getOwner());
			    
			    FileEventProcessor.processAndSaveContentEvent(file, event);
			    
//	            event.setLogicalFile(file);
//	            FileEventDao.persist(event);
//	            NotificationManager.process(file.getUuid(), event.getStatus(), file.getOwner());
	            
	            
	            log.info("Attempting to apply transform " + this.queueTask.getTransformFilterName() + " on file " + file.getAgaveRelativePathFromAbsolutePath() + " for user " + file.getOwner());
	            
//	            file.setStatus(TransformTaskStatus.TRANSFORMING);
//	            LogicalFileDao.persist(file);


                // pull the source file to the server (may need a new SETTINGS variable)
                tempDir = new File("scratch/", this.queueTask.getTransformName()
                        + "-" + this.queueTask.getId() + "-" + this.queueTask.getCreated().getTime());
                tempDir.mkdirs();

                remoteDataClient = this.queueTask.getStorageSystem().getRemoteDataClient();
                remoteDataClient.authenticate();
                String localSourcePath = tempDir.getAbsolutePath() + new File(this.queueTask.getSourcePath()).getName();
                String localDestPath = tempDir.getAbsolutePath() + new File(this.queueTask.getDestPath()).getName();
                remoteDataClient.get(this.queueTask.getSourcePath(), localSourcePath);

				// the final status update for the LogicalFile and TransformTask will have
				// to come in the form of a callback from the transform routine.

				FileTransformProperties transformProperties = new FileTransformProperties();
				FileTransform transform = transformProperties.getTransform(this.queueTask.getTransformName());
				FileTransformFilter filter = transform.getEncodingChain().getFilter(this.queueTask.getTransformFilterName());

				if (filter == null) {
					throw new TransformException("Unknown filter " + this.queueTask.getTransformFilterName());
				} else {
					// assign an intermediate file for this transform
					String triggerUrl = Settings.IPLANT_IO_SERVICE + "/data/trigger/encoding/" + this.queueTask.getCallbackKey();

					TransformLauncher.invokeBlocking(transform.getScriptFolder() + "/" + filter.getHandle(), localSourcePath, localDestPath); //, triggerUrl);
					
                    // put the transformed file to the remoteDataClient and delete the tempDir
                    remoteDataClient.put(localDestPath, this.queueTask.getDestPath());
                    tempDir.delete();

                    // callback url (previously handled in script when invoked without blocking)
                    ProcessBuilder trigger = new ProcessBuilder("curl -o /dev/null -O \"" + triggerUrl + "/TRANSFORMING_COMPLETED\" > /dev/null");
                    trigger.start();
				}
			}
		}
		catch (ClosedByInterruptException e) {
			try {
                FileEvent event = new FileEvent(FileEventType.TRANSFORMING_FAILED, 
            		"Your scheduled transfer of " + this.queueTask.getLogicalFile().getSourceUri() +
					" failed while applying the " + this.queueTask.getTransformName() + " transform " +
					" when the worker was interrupted.", 
					queueTask.getOwner());
                FileEventProcessor.processAndSaveContentEvent(file, event);
            } 
            catch (LogicalFileException | FileEventProcessingException e1) {
                log.error("Failed to send notification of failed transform task " + this.queueTask.getId(), e);
            }
			
			String message = null;
			try {
				message = "Failed to apply transform " + this.queueTask.getTransformName() + 
						":" + this.queueTask.getTransformFilterName() + " to file " + 
						file.getAgaveRelativePathFromAbsolutePath();
			}
			catch (Throwable t) {
				message = "Failed to apply transform for task " + queueTask.getId();
			}
			
			log.error(message, e);
			throw new JobExecutionException(message, e);
        }
		catch (StaleObjectStateException e) {
			log.debug("Just avoided a file encoding race condition from worker");
		}
		catch (TransformException e)
		{
			if (this.queueTask != null)
			{
				try {
					
//				    file.setStatus(TransformTaskStatus.TRANSFORMING_FAILED);
//					LogicalFileDao.persist(file);
                    
					FileEvent event = new FileEvent(FileEventType.TRANSFORMING_FAILED,
							"Your scheduled transfer of " + this.queueTask.getLogicalFile().getSourceUri() +
							" failed while applying the " + this.queueTask.getTransformName() + " transform " +
							" Please verify the desired file format and and make sure the necessary transforms " +
							" are available. If you feel there was an error and this problem persists, please " +
							"contact your api administrator for assistance.",
							queueTask.getOwner());
					FileEventProcessor.processAndSaveContentEvent(file, event);
//					event.setLogicalFile(file);
//					FileEventDao.persist(event);
//	                NotificationManager.process(file.getUuid(), event.getStatus(), file.getOwner());
//	                
					
	                
				} catch (Exception e1){}
				
				String message = null;
				try {
					message = "Failed to apply transform " + this.queueTask.getTransformName() + 
							":" + this.queueTask.getTransformFilterName() + " to file " + 
							file.getAgaveRelativePathFromAbsolutePath();
				}
				catch (Throwable t) {
					message = "Failed to apply transform for task " + queueTask.getId();
				}
				
				log.error(message, e);
				throw new JobExecutionException(message, e);
			}
			else {
				throw new JobExecutionException(e.getMessage(), e);
			}
		} catch (Throwable e) {
            log.error("Encoding task failed unexpectedly", e);
            throw new JobExecutionException(e);
        }
		finally {
		    try { remoteDataClient.disconnect(); } catch (Exception e) {}
		    FileUtils.deleteQuietly(tempDir);
		    setTaskComplete(true);
		    releaseJob();
		}
	}
	
	/* (non-Javadoc)
	 * @see org.quartz.InterruptableJob#interrupt()
	 */
	@Override
	public void interrupt() throws UnableToInterruptJobException {
		
		if (getQueueTask() != null)
		{
			try 
			{
				FileEvent event = new FileEvent(FileEventType.TRANSFORMING_FAILED,
						"Transfer of " + this.queueTask.getSourcePath() + " was cancelled during transformation. "
								+ "The transfer will be returned to the queue and attempted again momentarily.",
								getQueueTask().getOwner());
				FileEventProcessor.processAndSaveContentEvent(file, event);
				
//				event.setLogicalFile(getQueueTask().getLogicalFile());
//				FileEventDao.persist(event);
				
//				file.setStatus(TransformTaskStatus.TRANSFORMING_FAILED);
//				LogicalFileDao.persist(file);
			} catch (Throwable t) {
				log.error("Failed to reset logical file status during worker interrupt.", t);
			}
			
//			try 
//			{
//				LogicalFile file = getQueueTask().getLogicalFile();
//				
//				file.setStatus(TransformTaskStatus.TRANSFORMING_QUEUED);
//				file.addEvent(TransformTaskStatus.TRANSFORMING_QUEUED);
//				LogicalFileDao.persist(file);
//			} catch (Throwable t) {
//				log.error("Failed to reset logical file status during worker interrupt.", t);
//			}
		
			try {
			    getQueueTask().setStatus(TransformTaskStatus.TRANSFORMING_QUEUED);
				QueueTaskDao.persist(this.queueTask);
			} catch (Exception e) {
				log.error("Failed to reset encoding task to the queue during worker interrupt.", e);
			}
		}
		
		releaseJob();
		
	}
	
    @Override
    public synchronized Long selectNextAvailableQueueTask() throws TaskException {
        return QueueTaskDao.getNextTransformTask(TenancyHelper.getDedicatedTenantIdForThisService());
    }
    

    /* (non-Javadoc)
     * @see org.iplantc.service.jobs.queue.WorkerWatch#getJob()
     */
    @Override
    public synchronized EncodingTask getQueueTask() {
        if (this.queueTask == null && this.queueTaskId != null) {
            this.queueTask = QueueTaskDao.getEncodingTaskById(this.queueTaskId);
        }
        return this.queueTask;
    }

    @Override
    protected void rollbackStatus() {
        //
    }
    
    @Override
    public void setQueueTaskId(Long queueTaskId) {
        this.queueTaskId = queueTaskId;
    }

    @Override
    protected void releaseJob() {
        JobProducerFactory.releaseEncodingJob(this.queueTaskId);
    }
}
