package org.iplantc.service.monitor.queue;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.log4j.Logger;
import org.hibernate.StaleObjectStateException;
import org.iplantc.service.common.dao.TenantDao;
import org.iplantc.service.common.exceptions.MessageProcessingException;
import org.iplantc.service.common.messaging.Message;
import org.iplantc.service.common.messaging.MessageClientFactory;
import org.iplantc.service.common.messaging.MessageQueueClient;
import org.iplantc.service.common.model.Tenant;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.monitor.Settings;
import org.iplantc.service.monitor.dao.MonitorDao;
import org.iplantc.service.monitor.managers.MonitorManager;
import org.iplantc.service.monitor.model.Monitor;
import org.iplantc.service.monitor.util.ServiceUtils;
import org.iplantc.service.notification.util.EmailMessage;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.surftools.BeanstalkClient.BeanstalkException;

/**
 * Class to watch for monitor messages coming across the message queue. Despite being 
 * invoked by Quartz, this Job is blocking and each instance starts an infinite loop, thus
 * Quarts is never really used as intended. Each thread will only ever have a single Job 
 * running. The benefit is that if the job dies or fails for any reason, Quarts will
 * restart it right away. 
 * 
 * @author dooley
 * 
 */
@DisallowConcurrentExecution
public class MonitorQueueListener implements org.quartz.Job
{
	private static final Logger	log	= Logger.getLogger(MonitorQueueListener.class);
	
	private MonitorDao dao = new MonitorDao();
	
	private MessageQueueClient messageClient = null;
	
	public void execute(JobExecutionContext context)
	{
//		log.debug("TRIGGER: " + context.getTrigger().getKey());
		
		Message message = null;
		try
		{
			messageClient = MessageClientFactory.getMessageClient();
			
			message = messageClient.pop(Settings.MONITOR_TOPIC, Settings.MONITOR_QUEUE);
			
			if (message != null) 
			{
				messageClient.delete(Settings.MONITOR_TOPIC, Settings.MONITOR_QUEUE, message.getId());
			
				processMessage(message.getMessage());
			}
		} 
//		catch (MessageProcessingException e) {
//			log.error(e);
//			if (message != null) {
//				try
//				{
//					messageClient.reject(Settings.MONITOR_TOPIC, Settings.MONITOR_QUEUE, message.getId(), message.getMessage());
//				}
//				catch (MessagingException e1) {
//					log.error("Failed to return monitor message to queue: " + message.getMessage(), e1);
//				}
//			}
//		}
		catch (Throwable e) {
			log.error("Monitor messaging client failed unexpectedly." + (message == null ? "" : message.getMessage()));
			String msg;
		    if (e instanceof BeanstalkException && e.getMessage().contains("Connection refused")) 
		    {
		    	msg = "A Monitor worker on " + ServiceUtils.getLocalIP() 
                        + " is unable to connect to the " + Settings.MESSAGING_SERVICE_PROVIDER + " message queue at " 
                        + Settings.MESSAGING_SERVICE_HOST + ":" + Settings.MESSAGING_SERVICE_PORT 
                        + ". Pending messages will remained queued until the queue is available again";
		        log.error(msg, e);
            } 
		    else 
		    { 
		    	msg = "A monitoring worker on " + ServiceUtils.getLocalIP() + " died unexpectedly. "
		                + (message == null ? "" : "Pending message was \n" + message.getMessage());
    			log.error(msg, e);
    		}
//			try {
//				String body = "Monitor worker on " + ServiceUtils.getLocalIP() + " died unexpectedly.";
//				Tenant tenant = new TenantDao().findByTenantId(TenancyHelper.getCurrentTenantId());
//				EmailMessage.send(tenant.getContactName(), 
//					tenant.getContactEmail(), 
//					"Monitor worker died unexpectedly", 
//					body + "\n\n" +  ExceptionUtils.getStackTrace(e),
//                    "<p>" + body + "</p><pre>" + ExceptionUtils.getStackTrace(e) + "</pre></p>");
//				
//			} catch (Throwable e1) {
//				log.error("Failed to send worker failure message to admin.",e1);
//			}
		}
		finally {
			try { messageClient.stop(); } catch (Exception e) {}
		}
	}
			
	public void processMessage(String body) throws MessageProcessingException
	{
		try 
		{
			ObjectMapper mapper = new ObjectMapper();
			JsonNode json = mapper.readTree(body);
			String uuid = json.get("uuid").asText();
//			String target = json.has("target") ? json.get("target").asText() : null;
//			String owner = json.has("owner") ? json.get("owner").asText() : null;
			
			Monitor monitor = dao.findByUuid(uuid);
			
			if (monitor == null) {
				log.error("No monitor with uuid " + uuid + " found.");
			} 
			else 
			{
				MonitorManager manager = new MonitorManager();
				manager.resetNextUpdateTime(monitor);
				manager.check(monitor, monitor.getOwner());
			}
		}
		catch (StaleObjectStateException e) {
			log.debug("Just avoided a monitor check race condition.");
		}
		catch (Throwable e) {
			throw new MessageProcessingException("Failed to process monitor message: " + body, e);
			
		}
		
	}

	public void stop()
	{
		try {
			messageClient.stop();
		} catch (Exception e) {
			log.error("Failed to stop message client.",e);
		}
	}
}