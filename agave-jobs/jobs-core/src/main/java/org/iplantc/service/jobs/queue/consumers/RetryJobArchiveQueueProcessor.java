package org.iplantc.service.jobs.queue.consumers;

import java.nio.channels.ClosedByInterruptException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.StaleObjectStateException;
import org.hibernate.UnresolvableObjectException;
import org.iplantc.service.apps.dao.SoftwareDao;
import org.iplantc.service.apps.model.Software;
import org.iplantc.service.common.dao.TenantDao;
import org.iplantc.service.common.exceptions.MessageProcessingException;
import org.iplantc.service.common.messaging.MessageClientFactory;
import org.iplantc.service.common.messaging.clients.MessageQueueClient;
import org.iplantc.service.common.messaging.clients.MessageQueueListener;
import org.iplantc.service.common.messaging.model.Message;
import org.iplantc.service.common.model.Tenant;
import org.iplantc.service.common.persistence.HibernateUtil;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.jobs.dao.JobDao;
import org.iplantc.service.jobs.exceptions.JobException;
import org.iplantc.service.jobs.managers.JobManager;
import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.jobs.model.enumerations.JobStatusType;
import org.iplantc.service.jobs.queue.actions.ArchiveAction;
import org.iplantc.service.jobs.queue.messaging.JobMessageBody;
import org.iplantc.service.notification.Settings;
import org.iplantc.service.notification.events.NotificationAttemptProcessor;
import org.iplantc.service.notification.model.NotificationAttempt;
import org.iplantc.service.notification.util.EmailMessage;
import org.iplantc.service.notification.util.ServiceUtils;
import org.iplantc.service.systems.dao.SystemDao;
import org.iplantc.service.systems.exceptions.SystemUnavailableException;
import org.iplantc.service.systems.exceptions.SystemUnknownException;
import org.iplantc.service.systems.model.ExecutionSystem;
import org.iplantc.service.systems.model.enumerations.StorageProtocolType;
import org.iplantc.service.systems.model.enumerations.SystemStatusType;
import org.joda.time.DateTime;
import org.quartz.InterruptableJob;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.UnableToInterruptJobException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.surftools.BeanstalkClient.BeanstalkException;

/**
 * Class to watch for notification messages coming across the message queue. Despite being 
 * invoked by Quartz, this Job is blocking and each instance starts an infinite loop, thus
 * Quarts is never really used as intended. Each thread will only ever have a single Job 
 * running. The benefit is that if the job dies or fails for any reason, Quarts will
 * restart it right away. 
 * 
 * @author dooley
 * 
 */
//@DisallowConcurrentExecution
public class RetryJobArchiveQueueProcessor implements InterruptableJob, MessageQueueListener 
{
	private static final Logger	log	= Logger.getLogger(RetryJobArchiveQueueProcessor.class);
	private MessageQueueClient messageClient = null;
	private JobExecutionContext context;
	protected AtomicBoolean stopped = new AtomicBoolean(false);
	
	public void execute(JobExecutionContext context)
	{
		setContext(context);
		
		if (org.iplantc.service.common.Settings.isDrainingQueuesEnabled()) {
            log.debug("Queue draining has been enabled. Skipping check for job archive task." );
            return;
        }
		
		String dedicatedTenantId = org.iplantc.service.common.Settings.getDedicatedTenantIdFromServiceProperties();
		String topicName = org.iplantc.service.common.Settings.JOBS_ARCHIVING_TOPIC;
		String queueName = org.iplantc.service.common.Settings.JOBS_ARCHIVING_QUEUE;
		
		if (!StringUtils.startsWith(dedicatedTenantId, "!")) {
			topicName = dedicatedTenantId + "." + topicName;
        } 

        try
		{
			messageClient = MessageClientFactory.getMessageClient();
			
			Message message = null;
			try
			{
				message = messageClient.pop(topicName,queueName);
				
				if (message != null) {
					
			        processMessage(message.getMessage());
					
					messageClient.delete(topicName, queueName, message.getId());
		        }
				else {
		            log.debug(getClass().getSimpleName() + " worker was given null job uuid. Going for coffee...");
		        }
			} 
			catch (MessageProcessingException e) {
				messageClient.reject(topicName, queueName, message.getId(), message.getMessage());
			}
		}
		catch (Throwable e) {
			
		    String message = "";
		    if (e instanceof BeanstalkException && e.getMessage().contains("Connection refused")) {
		    	
		        message = "A job archive  retry worker on " + ServiceUtils.getLocalIP() 
                        + " is unable to connect to the " + Settings.MESSAGING_SERVICE_PROVIDER + " message queue at " 
                        + Settings.MESSAGING_SERVICE_HOST + ":" + Settings.MESSAGING_SERVICE_PORT 
                        + ". Pending messages will remained queued until the queue is available again";
		        log.error(message);
            } 
		    else { 
		        message = "A job archive worker on " + ServiceUtils.getLocalIP() + " died unexpectedly. "
		                + "Pending messages will remained queued.";
    			log.error(message, e);
    		}
		    
		    try {
				Tenant tenant = new TenantDao().findByTenantId(TenancyHelper.getCurrentTenantId());
				if (tenant != null) {
					
					EmailMessage.send(tenant.getContactName(), 
						tenant.getContactEmail(), 
						"Job archive worker died unexpectedly", 
						message + "\n\n" +  ExceptionUtils.getStackTrace(e),
						"<p>" + message + "</p><pre>" + ExceptionUtils.getStackTrace(e) + "</pre></p>");
				}
			} catch (Throwable e1) {
				log.error("Failed to send worker failure message to admin.",e1);
			}
		}
		finally {
			try { messageClient.stop(); } catch (Exception e) {}
		}

	}
	
	
	@Override
    public void processMessage(String body) throws MessageProcessingException
    {
		Job job = null;
    	try
        {
            JobMessageBody messageBody =
                    new ObjectMapper().readValue(body, JobMessageBody.class );

            job = JobDao.getByUuid(messageBody.getUuid());
            int attempt = messageBody.getContext().getAttempt();
            
            if (job == null) {
            	context.setResult(JobStatusType.ARCHIVING_FAILED);
                throw new MessageProcessingException("No job with uuid " + messageBody.getUuid() + " found.");
            }
            // make sure the job has not been cancelled
            else if (job.getStatus() != JobStatusType.CLEANING_UP) {
            	context.setResult(JobStatusType.ARCHIVING_FAILED);
            	throw new MessageProcessingException( "Job " + messageBody.getUuid() +
                       " is already archiving through another worker. Ignoring.");
                
            }
            else if (!job.isArchiveOutput())
			{
				log.debug("Job " + job.getUuid() + " completed. Skipping archiving at user request.");
				job = JobManager.updateStatus(job, JobStatusType.FINISHED,
						"Job completed. Skipping archiving at user request.");
				context.setResult(JobStatusType.ARCHIVING_FINISHED);
			}
			else
			{
				// we will only retry for 7 days after the job should have stopped running
				DateTime jobExpirationDate = new DateTime(job.calculateExpirationDate());

				if (jobExpirationDate.plusDays(7).isBeforeNow())
				{
					log.debug("Terminating job " + job.getUuid() + " after 7 days trying to archive output.");
					job = JobManager.updateStatus(job, JobStatusType.KILLED,
						"Removing job from queue after 7 days attempting to archive output.");
					job = JobManager.updateStatus(job, JobStatusType.FAILED,
							"Unable to archive outputs for job after 7 days. Job cancelled.");
					
					context.setResult(JobStatusType.ARCHIVING_FAILED);
					return;
				}

            	Software software = SoftwareDao.getSoftwareByUniqueName(job.getSoftwareName());

            	// if the execution system for this job has a local storage config,
            	// all other transfer workers will pass on it.
                if (!StringUtils.equals(org.iplantc.service.jobs.Settings.LOCAL_SYSTEM_ID, job.getSystem()) &&
                		software.getExecutionSystem().getStorageConfig().getProtocol().equals(StorageProtocolType.LOCAL))
                {
                    return;
                }
                else
                {
					// check for resource availability before updating status
                	ExecutionSystem executionSystem = (ExecutionSystem) new SystemDao().findBySystemId(job.getSystem());

            		if (executionSystem != null && (!executionSystem.isAvailable() || !executionSystem.getStatus().equals(SystemStatusType.UP)))
            		{
            			if (!StringUtils.contains(job.getErrorMessage(), "paused waiting")) {
            				log.debug("Archiving skipped for job " + job.getUuid() + ". Execution system " +
            						executionSystem.getSystemId() + " is currently unavailable.");
            			}
            			job = JobManager.updateStatus(job, JobStatusType.CLEANING_UP,
							"Archiving is current paused waiting for the execution system " + executionSystem.getSystemId() +
							" to become available. If the system becomes available again within 7 days, this job " +
							"will resume archiving. After 7 days it will be killed.");
						return;
            		}
            		else if (job.getArchiveSystem() != null && (!job.getArchiveSystem().isAvailable() || !job.getArchiveSystem().getStatus().equals(SystemStatusType.UP)))
            		{
            			if (!StringUtils.contains(job.getErrorMessage(), "paused waiting")) {
            				log.debug("Archiving skipped for job " + job.getUuid() + ". Archive system " +
            						job.getArchiveSystem().getSystemId() + " is currently unavailable. ");
            			}
            			job = JobManager.updateStatus(job, JobStatusType.CLEANING_UP,
							"Archiving is current paused waiting for the archival system " + job.getArchiveSystem().getSystemId() +
							" to become available. If the system becomes available again within 7 days, this job " +
							"will resume archiving. After 7 days it will be killed.");
						return;
            		}

            		// mark the job as submitting so no other process claims it
                	try {
                		job = JobManager.updateStatus(job, JobStatusType.ARCHIVING, "Beginning to archive output.");
                	}
                	catch (Throwable e) {
                	    log.debug("Job " + job.getUuid() + " already being processed by another thread. Ignoring.");
                	    return;
                	}
                	
                	log.debug("Attempt " + attempt + " to archive job " + job.getUuid() + " output");

					// mark the job as submitting so no other process claims it
					job = JobManager.updateStatus(job, JobStatusType.ARCHIVING,
					        "Attempt " + attempt + " to archive job output");

					JobDao.persist(job);
    				
					try
					{
    				    ArchiveAction action = new ArchiveAction(job);

    				    try {
    				    	action.run();
    				    } catch (Throwable t) {
    				        job = action.getJob();
    				        throw t;
    				    }

        				if (job.getStatus() == JobStatusType.ARCHIVING_FINISHED
        				        || job.getStatus() == JobStatusType.ARCHIVING_FAILED)
        				{
        				    log.debug("Finished archiving job " + job.getUuid() + " output");
            				job = JobManager.updateStatus(job, JobStatusType.FINISHED);
            				context.setResult(JobStatusType.ARCHIVING_FINISHED);
        				}
					}
    				catch (ClosedByInterruptException e) {
    				    throw e;
    				}
    				catch (SystemUnknownException e)
    				{
    					try
						{
    					    job = JobDao.getById(job.getId());
							log.error("System for job " + job.getUuid() + " is currently unknown. ", e);
							job = JobManager.updateStatus(job, JobStatusType.ARCHIVING_FAILED, e.getMessage());

							log.error("Job " + job.getUuid() + " completed, but failed to archive output.");
							job = JobManager.updateStatus(job, JobStatusType.FINISHED,
									"Job completed, but failed to archive output.");
						}
						catch (Exception e1) {
							log.error("Failed to update job " + job.getUuid() + " status to FINISHED");
						}
    				}
    				catch (SystemUnavailableException e)
					{
						try
						{
						    job = JobDao.getById(job.getId());
							log.debug("System for job " + job.getUuid() + " is currently unavailable. " + e.getMessage());
							job = JobManager.updateStatus(job, JobStatusType.CLEANING_UP,
								"Job output archiving is current paused waiting for a system containing " +
								"input data to become available. If the system becomes available again within 7 days, this job " +
								"will resume staging. After 7 days it will be killed.");
						}
						catch (Throwable e1) {
							log.error("Failed to update job " + job.getUuid() + " status to CLEANING_UP");
						}
						context.setResult(JobStatusType.ARCHIVING_FAILED);
			            
					}
					catch (JobException e)
    				{
					    job = JobDao.getById(job.getId());

					    
						if (attempt >= org.iplantc.service.jobs.Settings.MAX_SUBMISSION_RETRIES )
						{
							log.error("Failed to archive job " + job.getUuid() +
									" output after " + attempt + " attempts.", e);
							job = JobManager.updateStatus(job, JobStatusType.CLEANING_UP, "Attempt "
									+ attempt + " failed to archive job output. " + e.getMessage());

							log.error("Unable to archive output for job " + job.getUuid() +
									" after " + attempt + " attempts.");
							job =JobManager.updateStatus(job, JobStatusType.ARCHIVING_FAILED,
									"Unable to archive outputs for job" +
									" after " + attempt + " attempts.");

							log.error("Job " + job.getUuid() + " completed, but failed to archive output.");
							job = JobManager.updateStatus(job, JobStatusType.FINISHED,
									"Job completed, but failed to archive output.");

							context.setResult(JobStatusType.ARCHIVING_FAILED);
						}
						else
						{
						    try {
						        job = JobManager.updateStatus(job, JobStatusType.CLEANING_UP, "Attempt "
						                + attempt + " failed to archive job output.");
						    } catch (Exception e1) {
						        log.error("Attempt " + attempt + " for job " + job.getUuid()
						                + " failed to archive output.", e);
						    }
						    
						    context.setResult(JobStatusType.ARCHIVING_FAILED);
				            
						}
					}
    			}
            }
		}
    	catch (StaleObjectStateException | UnresolvableObjectException e) {
		    log.debug("Job " + job.getUuid() + " already being processed by another archiving thread. Ignoring.");
		    context.setResult(JobStatusType.ARCHIVING_FAILED);            
		}
		catch (HibernateException e) {
			log.error("Failed to retrieve job information from db", e);
			context.setResult(JobStatusType.ARCHIVING_FAILED);
		}
    	catch (ClosedByInterruptException e) {
		    log.debug("Archive task for job " + job.getUuid() + " aborted due to interrupt by worker process.");
		    context.setResult(JobStatusType.ARCHIVING_FAILED);
            
		    try {
	            job = JobManager.updateStatus(job, JobStatusType.CLEANING_UP, 
	                "Job archiving reset due to worker shutdown. Archiving will resume in another worker automatically.");
	            JobDao.persist(job);
	        } catch (UnresolvableObjectException | JobException e1) {
	            log.error("Failed to roll back job status when archive task was interrupted.", e);
	        }
		    throw new MessageProcessingException("Staging task for job " + job.getUuid() + " aborted due to interrupt by worker process.");
		}
		catch (MessageProcessingException e) {
        	context.setResult(JobStatusType.ARCHIVING_FAILED);
            throw e;
        }
        catch (JobException e) {
        	context.setResult(JobStatusType.ARCHIVING_FAILED);
            throw new MessageProcessingException(true, e);
        }
    	catch (Throwable e)
		{
			if (e.getCause() instanceof StaleObjectStateException) {
			    log.debug("Job " + job.getUuid() + "already being processed by another thread. Ignoring.");
			}
			else {
				context.setResult(JobStatusType.ARCHIVING_FAILED);
	            
				String message = "Failed to archive job " + job.getUuid() + " " + e.getMessage();
				log.error("Failed to archive output for job " + job.getUuid(), e);
	
				try {
				    job = JobDao.getById(job.getId());
				    job =JobManager.updateStatus(job, JobStatusType.ARCHIVING_FAILED, message);
				    job =JobManager.updateStatus(job, JobStatusType.FINISHED, "Job completed, but failed to archive.");
				} catch (Exception e1) {}
			}
	//		throw new JobExecutionException(e);
		}
	    finally {
		    try { HibernateUtil.flush(); } catch (Exception e) {}//e.printStackTrace();};
	        try { HibernateUtil.commitTransaction(); } catch (Exception e) {}//e.printStackTrace();};
	        try { HibernateUtil.disconnectSession(); } catch (Exception e) {}//e.printStackTrace();};
		}   
    }
	
	@Override
	public synchronized void stop()
	{
		try {
			messageClient.stop();
		} catch (Exception e) {
			log.error("Failed to stop message client.",e);
		}
	}


    @Override
    public void interrupt() throws UnableToInterruptJobException {
        this.stop();
    }


	/**
	 * @return the context
	 */
	public JobExecutionContext getContext() {
		return context;
	}


	/**
	 * @param context the context to set
	 */
	public void setContext(JobExecutionContext context) {
		this.context = context;
	}
}