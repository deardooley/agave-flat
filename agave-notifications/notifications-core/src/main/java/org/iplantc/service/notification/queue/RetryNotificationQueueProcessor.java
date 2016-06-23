package org.iplantc.service.notification.queue;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.common.dao.TenantDao;
import org.iplantc.service.common.exceptions.MessageProcessingException;
import org.iplantc.service.common.messaging.Message;
import org.iplantc.service.common.messaging.MessageClientFactory;
import org.iplantc.service.common.messaging.MessageQueueClient;
import org.iplantc.service.common.messaging.MessageQueueListener;
import org.iplantc.service.common.model.Tenant;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.notification.Settings;
import org.iplantc.service.notification.dao.NotificationDao;
import org.iplantc.service.notification.events.NotificationAttemptProcessor;
import org.iplantc.service.notification.exceptions.NotificationException;
import org.iplantc.service.notification.model.Notification;
import org.iplantc.service.notification.model.NotificationAttempt;
import org.iplantc.service.notification.model.enumerations.NotificationStatusType;
import org.iplantc.service.notification.queue.messaging.NotificationMessageContext;
import org.iplantc.service.notification.util.EmailMessage;
import org.iplantc.service.notification.util.ServiceUtils;
import org.quartz.InterruptableJob;
import org.quartz.JobExecutionContext;
import org.quartz.UnableToInterruptJobException;

import com.fasterxml.jackson.databind.JsonNode;
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
public class RetryNotificationQueueProcessor implements InterruptableJob, MessageQueueListener 
{
	private static final Logger	log	= Logger.getLogger(RetryNotificationQueueProcessor.class);
	private MessageQueueClient messageClient = null;
	private JobExecutionContext context;
	
	public void execute(JobExecutionContext context)
	{
		setContext(context);
		
	    if (org.iplantc.service.common.Settings.isDrainingQueuesEnabled()) {
            log.debug("Queue draining has been enabled. Skipping retry message processing." );
            return;
        }
	    
		try
		{
			messageClient = MessageClientFactory.getMessageClient();
			
			Message message = null;
			try
			{
				message = messageClient.pop(Settings.NOTIFICATION_RETRY_TOPIC, Settings.NOTIFICATION_RETRY_QUEUE);
				processMessage(message.getMessage());
				messageClient.delete(Settings.NOTIFICATION_RETRY_TOPIC, Settings.NOTIFICATION_RETRY_QUEUE, message.getId());
			} 
			catch (MessageProcessingException e) 
			{
				messageClient.reject(Settings.NOTIFICATION_RETRY_TOPIC, 
										Settings.NOTIFICATION_RETRY_QUEUE, 
										message.getId(), 
										message.getMessage());
			}
		}
		catch (Throwable e) 
		{
		    String message = "";
		    if (e instanceof BeanstalkException && e.getMessage().contains("Connection refused")) 
		    {
		        message = "A notification retry worker on " + ServiceUtils.getLocalIP() 
                        + " is unable to connect to the " + Settings.MESSAGING_SERVICE_PROVIDER + " message queue at " 
                        + Settings.MESSAGING_SERVICE_HOST + ":" + Settings.MESSAGING_SERVICE_PORT 
                        + ". Pending messages will remained queued until the queue is available again";
		        log.error(message);
            } 
		    else 
		    { 
		        message = "A notification retry worker on " + ServiceUtils.getLocalIP() + " died unexpectedly. "
		                + "Pending messages will remained queued.";
    			log.error(message, e);
    		}
		    
		    try {
				Tenant tenant = new TenantDao().findByTenantId(TenancyHelper.getCurrentTenantId());
				if (tenant != null)
				{   
					EmailMessage.send(tenant.getContactName(), 
						tenant.getContactEmail(), 
						"Notification worker died unexpectedly", 
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
        try 
        {
            ObjectMapper mapper = new ObjectMapper();
            NotificationAttempt attempt = mapper.readValue(body, NotificationAttempt.class);
            
            NotificationAttemptProcessor processor = new NotificationAttemptProcessor(attempt);
            
            processor.fire();
            
            // update job context for reporting purposes
            context.setResult(processor.getAttempt());
        }
        catch (Exception e) {
            throw new MessageProcessingException("Failed to parse notification attempt from retry message queue", e);
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