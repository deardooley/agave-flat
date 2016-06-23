package org.iplantc.service.io.queue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.nio.channels.ClosedByInterruptException;
import java.util.Date;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.plexus.util.FileUtils;
import org.hibernate.StaleObjectStateException;
import org.hibernate.UnresolvableObjectException;
import org.iplantc.service.common.exceptions.AgaveNamespaceException;
import org.iplantc.service.common.exceptions.PermissionException;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.common.uri.UrlPathEscaper;
import org.iplantc.service.data.transform.FileTransform;
import org.iplantc.service.data.transform.FileTransformFilter;
import org.iplantc.service.data.transform.FileTransformProperties;
import org.iplantc.service.io.Settings;
import org.iplantc.service.io.dao.LogicalFileDao;
import org.iplantc.service.io.dao.QueueTaskDao;
import org.iplantc.service.io.exceptions.FileEventProcessingException;
import org.iplantc.service.io.exceptions.LogicalFileException;
import org.iplantc.service.io.exceptions.TaskException;
import org.iplantc.service.io.manager.FileEventProcessor;
import org.iplantc.service.io.model.FileEvent;
import org.iplantc.service.io.model.LogicalFile;
import org.iplantc.service.io.model.StagingTask;
import org.iplantc.service.io.model.enumerations.FileEventType;
import org.iplantc.service.io.model.enumerations.StagingTaskStatus;
import org.iplantc.service.io.model.enumerations.TransformTaskStatus;
import org.iplantc.service.systems.exceptions.RemoteCredentialException;
import org.iplantc.service.systems.exceptions.SystemUnavailableException;
import org.iplantc.service.systems.exceptions.SystemUnknownException;
import org.iplantc.service.systems.model.RemoteSystem;
import org.iplantc.service.systems.model.enumerations.SystemStatusType;
import org.iplantc.service.systems.util.ApiUriUtil;
import org.iplantc.service.transfer.RemoteDataClient;
import org.iplantc.service.transfer.RemoteDataClientFactory;
import org.iplantc.service.transfer.URLCopy;
import org.iplantc.service.transfer.dao.TransferTaskDao;
import org.iplantc.service.transfer.exceptions.AuthenticationException;
import org.iplantc.service.transfer.exceptions.RemoteDataException;
import org.iplantc.service.transfer.model.TransferTask;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerException;
import org.quartz.UnableToInterruptJobException;

/**
 * Handles the staging of data into the user's StorageSystem. This differs
 * from a transfer task in that the flow is always inward to the StorageSystem
 * so only one RemoteSystem need be defined.
 * 
 * @author dooley
 *
 */
//@DisallowConcurrentExecution
public class StagingJob extends AbstractJobWatch<StagingTask> 
{
	private static final Logger log = Logger.getLogger(StagingJob.class);
	
	private TransferTask rootTask = null;
	private URLCopy urlCopy;
	private LogicalFile file = null;
	
	public StagingJob() {}
	
	/* (non-Javadoc)
	 * @see org.iplantc.service.io.queue.WorkerWatch#doExecute()
	 */
	@Override
	public void doExecute() throws JobExecutionException 
	{
		RemoteDataClient destClient = null;
		RemoteDataClient sourceClient = null;
		
		try 
		{	
		    this.queueTask.setStatus(StagingTaskStatus.STAGING);
			
			QueueTaskDao.persist(this.queueTask);
			
			file = this.queueTask.getLogicalFile();

			URI sourceUri = new URI(file.getSourceUri());
			
			// instantiate a client appropriate for the source uri
			destClient = getDestinationRemoteDataClient(file);
			
			// fetch a client for the source
			sourceClient = getSourceRemoteDataClient(this.queueTask.getOwner(), sourceUri);
			
			String srcUri = file.getSourceUri();
			String srcAbsolutePath = null;
			if (ApiUriUtil.isInternalURI(sourceUri)) { 
				srcAbsolutePath = UrlPathEscaper.decode(ApiUriUtil.getAbsolutePath(this.queueTask.getOwner(), sourceUri));
			}
			else {
				srcAbsolutePath = StringUtils.removeEnd(sourceUri.getPath(), "/");
			}
			sourceUri = new URI(file.getSourceUri());
			if (destClient.doesExist(file.getAgaveRelativePathFromAbsolutePath())) 
			{
				if (destClient.isDirectory(file.getAgaveRelativePathFromAbsolutePath())) 
				{
					if (StringUtils.isEmpty(file.getAgaveRelativePathFromAbsolutePath())) {
						file.setPath(srcAbsolutePath);
					} else if (StringUtils.endsWith(file.getAgaveRelativePathFromAbsolutePath(), "/")) {
						file.setPath(destClient.resolvePath(file.getAgaveRelativePathFromAbsolutePath() + FilenameUtils.getName(srcAbsolutePath)));
					} else {
						file.setPath(destClient.resolvePath(file.getAgaveRelativePathFromAbsolutePath() + File.separator + FilenameUtils.getName(srcAbsolutePath)));
					}
				}
			}
			else if (!destClient.doesExist(file.getAgaveRelativePathFromAbsolutePath() + (StringUtils.isEmpty(file.getAgaveRelativePathFromAbsolutePath()) ? ".." : "/..")))
			{
				throw new FileNotFoundException("Destination directory not found.");
			}
			
			
			String destUri = "agave://" + file.getSystem().getSystemId() + "/" + file.getAgaveRelativePathFromAbsolutePath();
			
//				rootTask = TransferTaskDao.findRootTransferTaskBySourceDestAndOwner(srcUri, destUri, file.getOwner());
			
//				if (rootTask == null)
//				{
				rootTask = new TransferTask(
						file.getSourceUri(), 
						destUri, 
						file.getOwner(), 
						null, 
						null);
				TransferTaskDao.persist(rootTask);
//				}
			
			file.addContentEvent(new FileEvent(
					FileEventType.STAGING, 
					"Transfer in progress",
					queueTask.getOwner(),
					rootTask));
			file.setStatus(StagingTaskStatus.STAGING);
			
			log.debug("Attempt " + this.queueTask.getRetryCount() + " to stage file " + file.getAgaveRelativePathFromAbsolutePath() + " for user " + file.getOwner());
			
			LogicalFileDao.persist(file);
			
			urlCopy = new URLCopy(sourceClient, destClient);
			// will close connections on its own
			
			try 
			{
				String src;
				if (ApiUriUtil.isInternalURI(sourceUri)) {
					src = ApiUriUtil.getPath(sourceUri);
					src = UrlPathEscaper.decode(StringUtils.removeStart(src, "/"));
				} else {
					src = sourceUri.getRawPath();
				}
				
				if (sourceUri.getRawQuery() != null) {
					src += "?" + sourceUri.getRawQuery();
				}
				
				rootTask = urlCopy.copy(src, file.getAgaveRelativePathFromAbsolutePath(), rootTask);
				
//				// set permissions on the parent folder. 
//				if (destClient.isPermissionMirroringRequired()) {
//					String parentPath = file.getAgaveRelativePathFromAbsolutePath();
//					if (StringUtils.endsWith(parentPath, "/")) {
//						parentPath = StringUtils.substringBeforeLast(parentPath, "/");
//					}
//					parentPath = FileUtils.getPath(parentPath);
//					try {
//						destClient.setOwnerPermission(destClient.getUsername(), parentPath, true);
//					} catch (Throwable e) {}
//					
//					try {
//						destClient.disconnect();
//						destClient.authenticate();
//						destClient.setOwnerPermission(file.getOwner(), parentPath, true);
//					} catch (Throwable e) {}
//				}
				
				log.info("Completed staging file " + this.queueTask.getLogicalFile().getAgaveRelativePathFromAbsolutePath() + " for user " + file.getOwner());
				
				// update the staging task as done
				this.queueTask.setStatus(StagingTaskStatus.STAGING_COMPLETED);
				QueueTaskDao.persist(this.queueTask);
				
				
				// file will be untouched after staging, so just mark as completed
				if (StringUtils.isEmpty(file.getNativeFormat()) || 
						file.getNativeFormat().equals("raw")) 
				{
					// update the file task
					file.setStatus(StagingTaskStatus.STAGING_COMPLETED.name());
					file.addContentEvent(FileEventType.STAGING_COMPLETED, queueTask.getOwner());
					
					file.setStatus(TransformTaskStatus.TRANSFORMING_COMPLETED.name());
					file.addContentEvent(new FileEvent(FileEventType.TRANSFORMING_COMPLETED,
							"Your scheduled transfer of " + this.queueTask.getLogicalFile().getSourceUri() +
							" completed staging. You can access the raw file on " + file.getSystem().getName() + " at " + 
							this.queueTask.getLogicalFile().getPath() + " or via the API at " + 
							file.getPublicLink() + ".", 
							queueTask.getOwner()));
					
					LogicalFileDao.persist(file);
				}
				else
				{
					file.setStatus(StagingTaskStatus.STAGING_COMPLETED.name());
					file.addContentEvent(FileEventType.STAGING_COMPLETED, queueTask.getOwner());
					file.addContentEvent(new FileEvent(FileEventType.PREPROCESSING, 
							"Calculating transformation pipeline for file type " + 
									file.getNativeFormat(),
							queueTask.getOwner()));
					LogicalFileDao.persist(file);
				
					// look up their transform by name
					FileTransformProperties props = new FileTransformProperties();
					FileTransform transform = props.getTransform(file.getNativeFormat());
					
					FileTransformFilter transformFilter = null;
					
					// no handler for this file, so mark it as raw and skip transform queue
					if (transform == null) 
					{
						String message = "No transform found. Staging complete for file " + this.queueTask.getLogicalFile().getAgaveRelativePathFromAbsolutePath() + " for user " + file.getOwner();
						log.info(message);
						file.setNativeFormat("raw");
						file.setStatus(TransformTaskStatus.TRANSFORMING_COMPLETED.name());
						file.addContentEvent(new FileEvent(FileEventType.TRANSFORMING_FAILED, 
								"Your scheduled transfer of " + this.queueTask.getLogicalFile().getSourceUri() + 
									" completed staging, but has failed encoding because the transform " +
									"you specified was not found. You can access the raw file on " + 
									file.getSystem().getName() + " at " + 
									this.queueTask.getLogicalFile().getPath() + " or via the API at " + 
									file.getPublicLink() + ".",
								queueTask.getOwner()));
						
						LogicalFileDao.persist(file);
					}
					// no handler for this file, so mark it as raw and skip transform queue
					else if (!transform.isEnabled()) 
					{
						log.debug("Transform " + transform.getId() + " is disabled. Staging complete for file " + this.queueTask.getLogicalFile().getAgaveRelativePathFromAbsolutePath() + " for user " + file.getOwner());
						
						file.setNativeFormat("raw");
						file.setStatus(TransformTaskStatus.TRANSFORMING_COMPLETED.name());
						
						file.addContentEvent(new FileEvent(FileEventType.TRANSFORMING_COMPLETED, 
								"Your scheduled transfer of " + this.queueTask.getLogicalFile().getSourceUri() + 
									" has completed successfully. You can access this file on " + 
									file.getSystem().getName() + " at " + 
									this.queueTask.getLogicalFile().getPath() + " or via the API at " + 
									file.getPublicLink() + ".",
								queueTask.getOwner()));
						
						LogicalFileDao.persist(file);
					}
					// kick off the transformation process
					else 
					{	
						// store the transform name with the logical file for later reference
						// filter can't be null since the transform would be disabled if there were
						// no enabled filters.
						transformFilter = transform.getEncodingChain().getFirstFilter();
//						file.addEvent(TransformTaskStatus.TRANSFORMING_QUEUED, queueTask.getOwner());
						file.setNativeFormat(transform.getId());
					
						log.debug("Submitting staged file " + this.queueTask.getLogicalFile().getAgaveRelativePathFromAbsolutePath() + " to the transform queue for user " + file.getOwner());
					
						// push the staged file into the transform queue
						try {
							String tmpFilePath = "scratch/" + file.getName() + "-" + System.currentTimeMillis();
							QueueTaskDao.enqueueEncodingTask(file, file.getSystem(), file.getAgaveRelativePathFromAbsolutePath(), tmpFilePath, transform.getId(), transformFilter.getName(), this.queueTask.getOwner());
						} catch (SchedulerException e) {
							// no handler for this file, so mark it as raw and move on
							String message = "Failed to submit file " + file.getAgaveRelativePathFromAbsolutePath() + " to the transform queue for owner " + file.getOwner();
							log.info(message);
							file.setStatus(TransformTaskStatus.TRANSFORMING_COMPLETED);
							file.addContentEvent(new FileEvent(FileEventType.TRANSFORMING_COMPLETED, 
									"Your scheduled transfer of " + this.queueTask.getLogicalFile().getSourceUri() + 
										" completed staging, but failed to encode because it could not " +
										"be submitted to the transform queue. If this problem persists, please " +
										"contact your api administrator for assistance. You may access the " +
										"raw file on " + file.getSystem().getSystemId() + " at " + 
										this.queueTask.getLogicalFile().getPath() + " or via the API at " + 
										file.getPublicLink() + ".",
									queueTask.getOwner()));
							LogicalFileDao.persist(file);
						}
					}
				}
			} 
			catch (Throwable e) 
			{
			    
				// if the transfer failed, retry as many times as defined in 
				// the service config file
				if (this.queueTask.getRetryCount() < Settings.MAX_STAGING_RETRIES) {
					log.info("Failed attempt " + this.queueTask.getRetryCount() + " to stage file " + this.queueTask.getLogicalFile().getPath() + " for user " + file.getOwner());
					this.queueTask.setStatus(StagingTaskStatus.STAGING_QUEUED);
					this.queueTask.setRetryCount(this.queueTask.getRetryCount() + 1);
				} 
				else 
				{
					String message = "Failed attempt " + this.queueTask.getRetryCount() + " to stage file " + this.queueTask.getLogicalFile().getPath() 
							+ " for user " + file.getOwner() + ". The maximum number of retries has been reached.";
					
					log.error(message, e);
					
					file.setStatus(StagingTaskStatus.STAGING_FAILED);
					file.addContentEvent(new FileEvent(FileEventType.STAGING_FAILED, 
							"Your scheduled transfer of " + this.queueTask.getLogicalFile().getSourceUri() + 
							" failed after " + Settings.MAX_STAGING_RETRIES + 
							" attempts with the following error message: " + e.getMessage() + 
							". Please check the source url, " + sourceUri + " and make sure it is a " + 
							" valid URI or path on your default system. If you feel there was an error and this problem persists, please " +
							"contact your api administrator for assistance.",
							queueTask.getOwner()));
					LogicalFileDao.persist(file);
					
					this.queueTask.setStatus(StagingTaskStatus.STAGING_FAILED);
				}
				QueueTaskDao.persist(this.queueTask);
			}
		} 
		catch (ClosedByInterruptException e) {
			try {
                file.setStatus(StagingTaskStatus.STAGING_FAILED.name());
                LogicalFileDao.persist(file);
            } catch (Throwable t) {
                log.error("Failed to update status of logical file " + file.getUuid() 
                        + " to STAGING_FAILED after staging worker was interrupted "
                        + "while processing staging task " + this.queueTask.getId(), t);
            }
            
            try {
                FileEvent event = new FileEvent(FileEventType.STAGING_FAILED, 
            		"Your scheduled transfer of " + file.getSourceUri() + 
					" failed to transfer on attempt " + (this.queueTask.getRetryCount() + 1) + 
					" after the staging worker was interrupted." + 
					" This transfer will be attempted " + (Settings.MAX_STAGING_RETRIES - this.queueTask.getRetryCount()) + 
					" more times before being being abandonded.",
					queueTask.getOwner());
                FileEventProcessor.processAndSaveContentEvent(file, event);
            } 
            catch (LogicalFileException | FileEventProcessingException e1) {
                log.error("Failed to send notification of failed staging task " + this.queueTask.getId(), e1);
            }
        }
		catch (SystemUnknownException e) {
			String message = "Unsupported protocol for queued file " + this.queueTask.getLogicalFile().getPath();
			
			log.error(message);
			file.setStatus(StagingTaskStatus.STAGING_FAILED);
			file.addContentEvent(new FileEvent(FileEventType.STAGING_FAILED, 
					"Your scheduled transfer of " + this.queueTask.getLogicalFile().getSourceUri() + 
					" failed to because an unrecognized protocol " +
					" was provided. Please check the source url, " + this.queueTask.getLogicalFile().getPath() + 
					" and make sure it is a valid URI or path on your default system. If you feel there was an error and this problem persists, please " +
					"contact your api administrator for assistance.",
					queueTask.getOwner()));
			LogicalFileDao.persist(file);
			
			this.queueTask.setStatus(StagingTaskStatus.STAGING_FAILED);
			QueueTaskDao.persist(this.queueTask);
		}
		catch (SystemUnavailableException e) {
			log.debug("Staging task paused while waiting for system availability. " + e.getMessage());
			file.setStatus(StagingTaskStatus.STAGING_QUEUED.name());
//				file.addEvent(new FileEvent(StagingTaskStatus.STAGING_QUEUED.name(), 
//						"Your scheduled transfer of " + this.queueTask.getLogicalFile().getSourceUri() + 
//						" is paused waiting on one or more systems to become available.",
//						queueTask.getOwner()));
			LogicalFileDao.persist(file);
			
			this.queueTask.setStatus(StagingTaskStatus.STAGING_QUEUED);
			this.queueTask.setLastUpdated(new Date());
			QueueTaskDao.persist(this.queueTask);
		}
		catch (StaleObjectStateException | UnresolvableObjectException e) {
			log.debug("Just avoided a file staging race condition from worker");
		}
		catch (Throwable e) 
		{
			try {
				if (this.queueTask != null) 
				{
					LogicalFile file = this.queueTask.getLogicalFile();
					String message = "Failed to submit file " + file.getPath() + " to the transform queue for owner " + file.getOwner();
					log.info(message);
					
					if (this.queueTask.getRetryCount() < Settings.MAX_STAGING_RETRIES) 
					{
					    try {
	                        file.setStatus(StagingTaskStatus.STAGING_FAILED.name());
	                        LogicalFileDao.persist(file);
	                    } catch (Throwable t) {
	                        log.error("Failed to update status of logical file " + file.getUuid() 
	                                + " to STAGING_FAILED after error with staging task " + this.queueTask.getId(), t);
	                    }
	                    
	                    try {
	                        FileEvent event = new FileEvent(FileEventType.STAGING_FAILED, 
	                    		"Your scheduled transfer of " + file.getSourceUri() + 
								" failed to transfer on attempt " + (this.queueTask.getRetryCount() + 1) + 
								" with the following error message: " + e.getMessage() + 
								". This transfer will be attempted " + (Settings.MAX_STAGING_RETRIES - this.queueTask.getRetryCount()) + 
								" more times before being being abandonded.",
								queueTask.getOwner());
	                        FileEventProcessor.processAndSaveContentEvent(file, event);
	//                        event.setLogicalFile(file);
	//                        FileEventDao.persist(event);
	                    } catch (LogicalFileException | FileEventProcessingException e1) {
	                        log.error("Failed to send notification of failed staging task " + this.queueTask.getId(), e1);
	                    }
						
						// increment the retry counter and throw it back into queue
						this.queueTask.setRetryCount(this.queueTask.getRetryCount() + 1);
						this.queueTask.setStatus(StagingTaskStatus.STAGING_QUEUED);
						QueueTaskDao.persist(this.queueTask);
					} 
					else 
					{	
					    try {
	    					file.setStatus(StagingTaskStatus.STAGING_FAILED.name());
	    					LogicalFileDao.persist(file);
					    } catch (Throwable t) {
					        log.error("Failed to update status of logical file " + file.getUuid() 
					                + " to STAGING_FAILED after error with staging task " + this.queueTask.getId(), t);
					    }
						
						try {
						    FileEvent event = new FileEvent(FileEventType.STAGING_FAILED, 
		                            "Your scheduled transfer of " + file.getSourceUri() + 
		                            " failed after " + Settings.MAX_STAGING_RETRIES + " attempts with the following message: " + 
		                            e.getMessage() + ". If you feel there was an error and this problem persists, please " +
		                            "contact your api administrator for assistance.",
		                            queueTask.getOwner());
						    FileEventProcessor.processAndSaveContentEvent(file, event);
	//	                    event.setLogicalFile(file);
	//	                    FileEventDao.persist(event);
	                    } catch (LogicalFileException | FileEventProcessingException e1) {
	                        log.error("Failed to send notification of failed staging task " + this.queueTask.getId(), e1);
	                    }
						
						this.queueTask.setStatus(StagingTaskStatus.STAGING_FAILED);
						QueueTaskDao.persist(this.queueTask);
					}
				} 
				else 
				{
					log.info("Failed to submit unknown file",e);
				}
			}
			catch (Throwable t) {
				log.info("Failed to roll back failed staging task", e);
			}
			
			try 
			{
				if (rootTask != null)
				{
					if (rootTask.getRootTask() != null) {
						TransferTaskDao.cancelAllRelatedTransfers(rootTask.getRootTask().getId()); 
					} else if (rootTask.getParentTask() != null) {
						TransferTaskDao.cancelAllRelatedTransfers(rootTask.getParentTask().getId());
					} else {
						TransferTaskDao.cancelAllRelatedTransfers(rootTask.getId());
					}
				}
			} catch (Throwable t) {}
		}
		finally
		{
		    try { sourceClient.disconnect(); } catch(Throwable e) {}
			try { destClient.disconnect(); } catch(Throwable e) {}
            setTaskComplete(true);
            releaseJob();
		}
	}
	
	/**
	 * Returns an authenticated {@link RemoteDataClient} for the given {@link LogicalFile}. 
	 * Exceptions will be thrown rather than null returned if the {@link RemoteSystem} on 
	 * which the data resides was not available.
	 * @param logicalFile
	 * @return
	 * @throws AuthenticationException
	 * @throws SystemUnavailableException
	 * @throws SystemUnknownException
	 * @throws RemoteDataException
	 */
	private RemoteDataClient getDestinationRemoteDataClient(LogicalFile logicalFile) 
	throws AuthenticationException, SystemUnavailableException, SystemUnknownException, 
	RemoteDataException
	{
		RemoteSystem system = null;
        RemoteDataClient remoteDataClient = null;
        
        system = file.getSystem();
        
        if (system == null) 
        {
            throw new SystemUnknownException("No destination system was found for user " 
            		+ logicalFile + " satisfying the URI.");
        } 
        else if (!system.isAvailable())
        {
            throw new SystemUnavailableException("The destination system is currently unavailable.");
        } 
        else if ( system.getStatus() != SystemStatusType.UP)
        {
            throw new SystemUnavailableException(system.getStatus().getExpression() );
        }
        else {
        	try {
				remoteDataClient = system.getRemoteDataClient(logicalFile.getInternalUsername());
				remoteDataClient.authenticate();
			} 
        	catch (IOException e) {
        		throw new RemoteDataException("Failed to connect to the remote destination system", e);
        	}
        	catch (RemoteCredentialException e) {
				throw new AuthenticationException("Failed to authenticate to remote destination system ", e);
			} 
        }
        
        return remoteDataClient;
	}

	/**
	 * Validates a URI and returns an authenticated {@link RemoteDataClient}. Exceptions
	 * will be thrown rather than null returned if a system was not available.
	 * @param requestingUser
	 * @param singleRawInputUri
	 * @return
	 * @throws AgaveNamespaceException
	 * @throws AuthenticationException
	 * @throws SystemUnavailableException
	 * @throws SystemUnknownException
	 * @throws RemoteDataException
	 */
	private RemoteDataClient getSourceRemoteDataClient(String requestingUser, URI singleRawInputUri) 
	throws AgaveNamespaceException, AuthenticationException, SystemUnavailableException, SystemUnknownException, 
	RemoteDataException
	{
		RemoteSystem system = null;
        RemoteDataClient remoteDataClient = null;
        
        if (ApiUriUtil.isInternalURI(singleRawInputUri))
        {
    		try {
        		system = ApiUriUtil.getRemoteSystem(requestingUser, singleRawInputUri);
    		} catch (PermissionException e) {
    			throw new AuthenticationException(e.getMessage(), e);
    		}
    		
            if (system == null) 
            {
                throw new SystemUnknownException("No system was found for user " 
                		+ requestingUser + " satisfying the source URI.");
            } 
            else if (!system.isAvailable())
            {
                throw new SystemUnavailableException("The source system is currently unavailable.");
            } 
            else if ( system.getStatus() != SystemStatusType.UP)
            {
                throw new SystemUnavailableException(system.getStatus().getExpression() );
            }
            else {
            	try {
					remoteDataClient = system.getRemoteDataClient(null);
					remoteDataClient.authenticate();
				} 
            	catch (IOException e) {
            		throw new RemoteDataException("Failed to connect to the remote source system", e);
            	}
            	catch (RemoteCredentialException e) {
					throw new AuthenticationException("Failed to authenticate to remote source system ", e);
				} 
            }
        }
    	else {
			try {
				remoteDataClient = new RemoteDataClientFactory().getInstance(
						requestingUser, null, singleRawInputUri);
				
				if (remoteDataClient == null) 
	            {
	                throw new SystemUnknownException("No system was found for user " 
	                		+ requestingUser + " satisfying the source URI.");
	            } 
				else {
					remoteDataClient.authenticate();
				}
			}
			catch (SystemUnknownException e) {
				throw e;
			}
			catch (FileNotFoundException e) {
				throw new SystemUnknownException("No source system was found for user " 
                		+ requestingUser + " satisfying the URI.");
			}
			catch (IOException e) {
        		throw new RemoteDataException("Failed to connect to the remote source system", e);
        	}
        	catch (PermissionException | RemoteCredentialException e) {
				throw new AuthenticationException("Failed to authenticate to remote source system. ", e);
			}
    	}
            
        return remoteDataClient;
	}

	/**
	 * @return the rootTask
	 */
	public synchronized TransferTask getRootTask() {
		return rootTask;
	}

	/**
	 * @param rootTask the rootTask to set
	 */
	public synchronized void setRootTask(TransferTask rootTask) {
		this.rootTask = rootTask;
	}
	
	/**
     * @param stopped the stopped to set
	 * @throws UnableToInterruptJobException 
     */
    @Override
    public synchronized void setStopped(boolean stopped) 
    throws UnableToInterruptJobException 
    {
        if (getUrlCopy() != null) {
            getUrlCopy().setKilled(true);
        }
        
        super.setStopped(true);
    }
    
    /**
     * @return the urlCopy
     */
    public synchronized URLCopy getUrlCopy() {
        return urlCopy;
    }

    /**
     * @param urlCopy the urlCopy to set
     */
    public synchronized void setUrlCopy(URLCopy urlCopy) {
        this.urlCopy = urlCopy;
    }
    

	@Override
	public void interrupt() throws UnableToInterruptJobException 
	{
		if (getQueueTask() != null) 
		{
			try 
			{
				this.queueTask = (StagingTask) QueueTaskDao.merge(getQueueTask());
				this.queueTask.setStatus(StagingTaskStatus.STAGING_QUEUED);
				QueueTaskDao.persist(this.queueTask);
			}
			catch (Throwable e) {
				log.error("Failed to roll back transfer task during interrupt.");
			}
			
			try 
			{
				LogicalFile file = getQueueTask().getLogicalFile();
				file.setStatus(StagingTaskStatus.STAGING_FAILED.name());
				file.addContentEvent(new FileEvent(FileEventType.STAGING_FAILED, 
						"Transfer was interrupted by the worker thread.",
						queueTask.getOwner()));
				LogicalFileDao.persist(file);
			}
			catch (Throwable e) {
				log.error("Failed to roll back transfer task during interrupt.");
			}
			
			if (getRootTask() != null)
			{
				try 
				{
					rootTask = TransferTaskDao.merge(getRootTask());
					
					if (rootTask.getRootTask() != null) {
						TransferTaskDao.cancelAllRelatedTransfers(rootTask.getRootTask().getId()); 
					} else if (rootTask.getParentTask() != null) {
						TransferTaskDao.cancelAllRelatedTransfers(rootTask.getParentTask().getId());
					} else {
						TransferTaskDao.cancelAllRelatedTransfers(rootTask.getId());
					}
				} catch (Exception e1) {}
			}
		}
		
		releaseJob();
	}

    @Override
    public synchronized Long selectNextAvailableQueueTask() throws TaskException {
        return QueueTaskDao.getNextStagingTask(TenancyHelper.getDedicatedTenantIdForThisService());
    }
    
    /* (non-Javadoc)
     * @see org.iplantc.service.jobs.queue.WorkerWatch#getJob()
     */
    @Override
    public synchronized StagingTask getQueueTask() {
        if (this.queueTask == null && queueTaskId != null) {
            this.queueTask = QueueTaskDao.getStagingTaskById(this.queueTaskId);
        }
        return this.queueTask;
    }

    @Override
    protected void rollbackStatus() {
        //
    }

    @Override
    public synchronized void setQueueTaskId(Long queueTaskId) {
        this.queueTaskId = queueTaskId;
    }

    @Override
    protected void releaseJob() {
        JobProducerFactory.releaseStagingJob(this.queueTaskId);
    }
}