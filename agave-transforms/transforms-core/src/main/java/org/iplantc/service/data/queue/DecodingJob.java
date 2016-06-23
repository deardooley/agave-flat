package org.iplantc.service.data.queue;

import java.io.File;

import org.apache.log4j.Logger;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.data.Settings;
import org.iplantc.service.data.dao.DecodingTaskDao;
import org.iplantc.service.data.exceptions.TransformPersistenceException;
import org.iplantc.service.data.model.DecodingTask;
import org.iplantc.service.data.transform.FileTransform;
import org.iplantc.service.data.transform.FileTransformFilter;
import org.iplantc.service.data.transform.FileTransformProperties;
import org.iplantc.service.data.transform.FilterChain;
import org.iplantc.service.data.transform.TransformLauncher;
import org.iplantc.service.io.dao.LogicalFileDao;
import org.iplantc.service.io.dao.QueueTaskDao;
import org.iplantc.service.io.exceptions.TaskException;
import org.iplantc.service.io.exceptions.TransformException;
import org.iplantc.service.io.model.FileEvent;
import org.iplantc.service.io.model.LogicalFile;
import org.iplantc.service.io.model.StagingTask;
import org.iplantc.service.io.model.enumerations.FileEventType;
import org.iplantc.service.io.model.enumerations.TransformTaskStatus;
import org.iplantc.service.transfer.RemoteDataClient;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class DecodingJob implements org.quartz.Job {
	private static final Logger log = Logger.getLogger(DecodingJob.class);
	private Long queueTaskId;
	private DecodingTask queueTask;
	private JobExecutionContext context;
	private boolean allowFailure = false;
	
	public DecodingJob() {
        super();
    }
    
    public DecodingJob(boolean allowFailure) {
        this();
        this.allowFailure = allowFailure;
    }

	public void execute(JobExecutionContext context) throws JobExecutionException {
		try 
    	{
			if (org.iplantc.service.common.Settings.isDrainingQueuesEnabled()) {
	    		log.debug("Queue draining has been enabled. Skipping decoding task." );
				return;
			}
			
			this.context = context;
            if (context.getMergedJobDataMap().containsKey("queueTaskId")) {
	            setQueueTaskId(context.getMergedJobDataMap().getLong("queueTaskId"));
	        }
            else {
            	setQueueTaskId(selectNextAvailableQueueTask());
            }
			
			if (getQueueTask() != null 
			        && (getQueueTask().getStatusAsString().equalsIgnoreCase("STAGING_QUEUED") 
			            || getQueueTask().getStatusAsString().equalsIgnoreCase("TRANSFORMING_QUEUED"))) 
	        {
			    // this is a new thread and thus has no tenant info loaded. we set it up
	            // here so things like app and system lookups will stay local to the 
	            // tenant
	            TenancyHelper.setCurrentTenantId(getQueueTask().getLogicalFile().getTenantId());
	            TenancyHelper.setCurrentEndUser(getQueueTask().getLogicalFile().getOwner());
	            
	            doExecute();
	            
	            log.debug("Worker found " + getClass().getSimpleName() + " " + getQueueTask().getId() 
	                    + " for user " + getQueueTask().getLogicalFile().getOwner() + " to process");
	            
	        }
		}
		catch(JobExecutionException e) {
	//	    log.error(e);
		    if (allowFailure) throw e;
		}
		catch (Throwable e) 
		{
			log.error("Unexpected error during decoding task work execution", e);
			if (allowFailure) 
			    throw new JobExecutionException("Unexpected error during decoding task worker execution",e);
		}
		finally {
	        if (this.queueTaskId != null) {
//		        log.debug("Releasing " + getClass().getSimpleName() + " " + this.queueTaskId 
//		                + " after getQueueTask() completion");
//		        releaseJob();
	        }
		}
	}
	
	public void doExecute() 
	throws JobExecutionException 
	{
		LogicalFile file = null;
		RemoteDataClient remoteDataClient = null;
		
		try 
		{
			// if there are no transforms, then return without doing anything.
			if (getQueueTask() == null) { 
				return;
			}
			
			// this is a new thread and thus has no tenant info loaded. we set it up
			// here so things like app and system lookups will stay local to the 
			// tenant
			TenancyHelper.setCurrentTenantId(this.queueTask.getLogicalFile().getTenantId());
			TenancyHelper.setCurrentEndUser(this.queueTask.getLogicalFile().getOwner());
			
			this.queueTask.setStatus(TransformTaskStatus.TRANSFORMING);
			DecodingTaskDao.persist(this.queueTask);
			 
			file = this.queueTask.getLogicalFile();
			file.addContentEvent(new FileEvent(FileEventType.TRANSFORMING, 
					"Attempting to apply transform " + this.queueTask.getDestTransform() + " on file " + this.queueTask.getSourcePath(),
					this.queueTask.getOwner()));
			file.setStatus(TransformTaskStatus.TRANSFORMING);
			LogicalFileDao.persist(file);
			
			log.info("Attempting to apply transform " + this.queueTask.getDestTransform() + " on file " + this.queueTask.getSourcePath());		
			
			if (this.queueTask.getDestTransform().equalsIgnoreCase("raw")) {
				// nothing to be done, just leave the file raw. Usually this will be caught
				// in the service or staging layers, but this leaves the opportunity open
				// for someone to include transformations with "raw" actions. ie, if the contents
				// match a certain regex, then leave them be. Multiple regexes can be supported
				// in this manner.
				log.info("Completed applying transform raw to file " + this.queueTask.getSourcePath());
				
				this.queueTask.setStatus(TransformTaskStatus.TRANSFORMING_COMPLETED);
				DecodingTaskDao.persist(this.queueTask);
				
				file.setStatus(TransformTaskStatus.TRANSFORMING_COMPLETED);
				file.addContentEvent(new FileEvent(FileEventType.TRANSFORMING_COMPLETED,
						"Your scheduled transform of " + this.queueTask.getLogicalFile().getSourceUri() + 
						" has completed successfully. You can access this file on " + file.getSystem().getSystemId() + " at /" + 
						this.queueTask.getLogicalFile().getPath() + " or through the API at " + 
						Settings.IPLANT_IO_SERVICE + "/media/system/" + file.getSystem() + "/" + this.queueTask.getSourcePath() + ".",
						this.queueTask.getOwner()));
				LogicalFileDao.persist(file);
				
			} else {

                // pull the source file to the server (may need a new SETTINGS variable)
                File tempDir = new File(org.iplantc.service.common.Settings.TEMP_DIRECTORY, this.queueTask.getId() + "-" + this.queueTask.getCreated().getTime());
                tempDir.mkdirs();

                remoteDataClient = this.queueTask.getStorageSystem().getRemoteDataClient();
                remoteDataClient.authenticate();
                String localSourcePath = tempDir.getAbsolutePath() + new File(this.queueTask.getSourcePath()).getName();
                String localDestPath = tempDir.getAbsolutePath() + new File(this.queueTask.getDestPath()).getName();
                remoteDataClient.get(this.queueTask.getSourcePath(), localSourcePath);

				// the final status update for the LogicalFile and TransformTask will have
				// to come in the form of a callback from the transform routine.
				
				FileTransformProperties transformProperties = new FileTransformProperties();
				FileTransform transform = transformProperties.getTransform(this.queueTask.getSrcTransform());
				FilterChain decoder = transform.getDecoder(this.queueTask.getDestTransform());
				FileTransformFilter filter = decoder.getFilter(this.queueTask.getCurrentFilter());
				
				if (filter == null) {
					throw new TransformException("Unknown filter " + this.queueTask.getCurrentFilter());
				} else {
					String triggerUrl = Settings.IPLANT_TRANSFORMS_SERVICE + "/trigger/decoding/" + this.queueTask.getCallbackKey(); 
					
//					File file = new File(this.queueTask.getSourcePath());
//					String tmpFilePath = Settings.IRODS_STAGING_DIRECTORY + "/scratch/" + file.getName() + "-" + System.currentTimeMillis();
					
					TransformLauncher.invokeBlocking(transform.getScriptFolder() + "/" + filter.getHandle(), this.queueTask.getSourcePath(), this.queueTask.getDestPath()); //, triggerUrl);

                    // put the transformed file to the remoteDataClient and delete the tempDir
                    remoteDataClient.put(localDestPath, this.queueTask.getDestPath());
                    tempDir.delete();

                    // callback url (previously handled in script when invoked without blocking)
                    ProcessBuilder trigger = new ProcessBuilder("curl -o /dev/null -O \"" + triggerUrl + "/TRANSFORMING_COMPLETED\" > /dev/null");
                    trigger.start();
				}
			}
			
		} 
		catch (TransformException e) {
			
		}
		catch (Exception e) 
		{
			if (this.queueTask != null) 
			{	
				this.queueTask.setStatus(TransformTaskStatus.TRANSFORMING_FAILED);
				try {
					DecodingTaskDao.persist(this.queueTask);
				}
				catch (Throwable t) {
					log.error("Failed to update decoding task " + queueTask.getId() + " on failed decoding task.", e);
				}
				
				String message = "Failed to apply transform " + this.queueTask.getDestTransform() + ":" + this.queueTask.getCurrentFilter() + " to file " + this.queueTask.getSourcePath();
				log.error(message, e);
				
				throw new JobExecutionException(message, e);
			} 
			else 
			{
				log.error("Failed to retrieve unknown decoding task record", e);
				throw new JobExecutionException("Failed to retrieve decoding task record", e);
			}
		} 
		finally {
			try { remoteDataClient.disconnect(); } catch (Exception e) {}
		}

	}
	
	public synchronized Long selectNextAvailableQueueTask() throws TaskException {
        return QueueTaskDao.getNextStagingTask(TenancyHelper.getDedicatedTenantIdForThisService());
    }
	
	public synchronized void setQueueTaskId(Long queueTaskId) {
        this.queueTaskId = queueTaskId;
    }
	
	public synchronized Long getQueueTaskId() {
        return this.queueTaskId;
    }
    
    public synchronized DecodingTask getQueueTask() {
        if (this.queueTask == null && queueTaskId != null) {
            try {
				this.queueTask = DecodingTaskDao.getById(this.queueTaskId);
			} catch (TransformPersistenceException e) {
				log.error("Failed to fetch decoding task from db", e);
			}
        }
        return this.queueTask;
    }
}
