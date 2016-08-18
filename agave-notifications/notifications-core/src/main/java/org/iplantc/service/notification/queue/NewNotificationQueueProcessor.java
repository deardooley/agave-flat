package org.iplantc.service.notification.queue;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.common.dao.TenantDao;
import org.iplantc.service.common.exceptions.MessageProcessingException;
import org.iplantc.service.common.messaging.MessageClientFactory;
import org.iplantc.service.common.messaging.clients.MessageQueueClient;
import org.iplantc.service.common.messaging.clients.MessageQueueListener;
import org.iplantc.service.common.messaging.model.Message;
import org.iplantc.service.common.model.Tenant;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.notification.Settings;
import org.iplantc.service.notification.dao.NotificationDao;
import org.iplantc.service.notification.events.NotificationMessageProcessor;
import org.iplantc.service.notification.exceptions.NotificationException;
import org.iplantc.service.notification.model.Notification;
import org.iplantc.service.notification.model.NotificationAttempt;
import org.iplantc.service.notification.model.enumerations.NotificationStatusType;
import org.iplantc.service.notification.queue.messaging.NotificationMessageBody;
import org.iplantc.service.notification.queue.messaging.NotificationMessageContext;
import org.iplantc.service.notification.util.EmailMessage;
import org.iplantc.service.notification.util.ServiceUtils;
import org.quartz.InterruptableJob;
import org.quartz.JobExecutionContext;
import org.quartz.UnableToInterruptJobException;
import org.quartz.Job;

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
public class NewNotificationQueueProcessor implements InterruptableJob, MessageQueueListener 
{
	private static final Logger	log	= Logger.getLogger(NewNotificationQueueProcessor.class);
	private NotificationDao dao = new NotificationDao();
	private MessageQueueClient messageClient = null;
	private JobExecutionContext context = null;
	
	/* (non-Javadoc)
	 * @see org.quartz.Job#execute(org.quartz.JobExecutionContext)
	 */
	@Override
	public void execute(JobExecutionContext context)
	{
		setContext(context);
		
	    if (org.iplantc.service.common.Settings.isDrainingQueuesEnabled()) {
            log.debug("Queue draining has been enabled. Skipping notification message processing." );
            return;
        }
	    
		try
		{
			do {
				messageClient = MessageClientFactory.getMessageClient();
				
				Message message = null;
				try
				{
					message = messageClient.pop(Settings.NOTIFICATION_TOPIC, Settings.NOTIFICATION_QUEUE);
					processMessage(message.getMessage());
					messageClient.delete(Settings.NOTIFICATION_TOPIC, Settings.NOTIFICATION_QUEUE, message.getId());
				} 
				catch (MessageProcessingException e) 
				{	
					// if the message has been retired sufficent times or failed due to a 
					// systemic failure, then we will not return it to the queue for further processing.
					if (message != null) {
						if (e.isExpired()) 
						{
							messageClient.delete(Settings.NOTIFICATION_TOPIC, Settings.NOTIFICATION_QUEUE, message.getId());
						}
						// something happend, just return it to the queue.
						else 
						{
							messageClient.reject(Settings.NOTIFICATION_TOPIC, 
												Settings.NOTIFICATION_QUEUE, 
												message.getId(), 
												message.getMessage());
						}
					}
				}
			} while(true);
		}
		catch (Throwable e) 
		{
		    String message = "";
		    if (e instanceof BeanstalkException && e.getMessage().contains("Connection refused")) 
		    {
		        message = "A notification worker on " + ServiceUtils.getLocalIP() 
                        + " is unable to connect to the " + Settings.MESSAGING_SERVICE_PROVIDER + " message queue at " 
                        + Settings.MESSAGING_SERVICE_HOST + ":" + Settings.MESSAGING_SERVICE_PORT 
                        + ". Pending messages will remained queued until the queue is available again";
		        log.error(message);
            } 
		    else 
		    { 
		        message = "A notification worker on " + ServiceUtils.getLocalIP() + " died unexpectedly. "
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
            JsonNode jsonBody = mapper.readTree(body);
            
            NotificationMessageBody messageBody = new NotificationMessageBody();
            messageBody.setOwner(jsonBody.get("owner").textValue());
            messageBody.setTenant(jsonBody.get("tenant").textValue());
            messageBody.setUuid(jsonBody.get("uuid").textValue());
            
            JsonNode jsonContext = null;
            if (jsonBody.hasNonNull("wrapper") && jsonBody.get("wrapper").isObject()) {
                jsonContext = jsonBody.get("wrapper");
            } else if (jsonBody.hasNonNull("context") && jsonBody.get("context").isObject()) {
                jsonContext = jsonBody.get("context");
            }
            
            if (jsonContext != null && !jsonContext.isNull()) 
            {
                NotificationMessageContext context = new NotificationMessageContext();
                
                if (jsonContext.has("associatedUuid")) {
                    context.setAssociatedUuid(jsonContext.get("associatedUuid").asText());
                }
                
                if (jsonContext.hasNonNull("customData")) {
                    if (jsonContext.get("customData").isValueNode()) {
                        context.setCustomData(jsonContext.get("customData").asText()); 
                    } else {
                        context.setCustomData(jsonContext.get("customData").toString());
                    }
                } 
                context.setEvent(jsonContext.get("event").asText());
                messageBody.setContext(context);
            } else {
                messageBody.setContext(new NotificationMessageContext());
            }
            
            Notification notification = dao.findByUuidAcrossTenants(messageBody.getUuid());
            
            if (notification == null) {
                log.error("No notification with uuid " + messageBody.getUuid() + " found.");
            } 
            else if (notification.getStatus() == NotificationStatusType.COMPLETE) {
                throw new NotificationException( "The notification " + notification.getUuid() + 
                        ", which would have handled this " + messageBody.getContext().getEvent() + 
                        " event notification message, has already processed. Skipping delivery.");
            }
            else if (notification.getStatus() == NotificationStatusType.INACTIVE) {
                throw new NotificationException( "The notification " + notification.getUuid() + 
                        ", which would have handled this " + messageBody.getContext().getEvent() + 
                        " event notification message, is currently inactive. Skipping delivery.");
            }
            else if (notification.getStatus() == NotificationStatusType.FAILED) {
                throw new NotificationException( "The notification " + notification.getUuid() + 
                        ", which would have handled this " + messageBody.getContext().getEvent() + 
                        " event notification message, has already failed and is no longer active.  "
                        + "Skipping delivery.");
            }
            else 
            {
            	// now that we have a valid message, we send it off to be converted into a valid
            	// NotificationAttempt and processed. Exceptions will be swallowed here and it is
            	// the responsibility of the NotificationAttemptProcessor to fail attempts according
            	// to the policy defined in the original Notification
            	NotificationAttempt attempt = NotificationMessageProcessor.process(notification, 
						                           messageBody.getContext().getEvent(), 
						                           messageBody.getOwner(),
						                           messageBody.getContext().getAssociatedUuid(),
						                           messageBody.getContext().getCustomData()); 
//                {
//                    throw new NotificationException("Failed to process notification " + notification.getUuid() + 
//                            " for " + messageBody.getContext().getEvent() + " event  after " + 
//                            Settings.MAX_NOTIFICATION_RETRIES + " attempts.");
//                } 
                
                getContext().setResult(attempt);
            }
        }
        catch (NotificationException e) {
            throw new MessageProcessingException(true, e);
        }
        catch (Throwable e) {
            log.error(e);
            throw new MessageProcessingException("Message processing failed.", e);
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