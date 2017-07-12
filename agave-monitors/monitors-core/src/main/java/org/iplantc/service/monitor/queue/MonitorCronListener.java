package org.iplantc.service.monitor.queue;

import java.util.Date;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.log4j.Logger;
import org.hibernate.StaleObjectStateException;
import org.iplantc.service.common.dao.TenantDao;
import org.iplantc.service.common.exceptions.MessagingException;
import org.iplantc.service.common.messaging.MessageClientFactory;
import org.iplantc.service.common.messaging.MessageQueueClient;
import org.iplantc.service.common.model.Tenant;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.monitor.Settings;
import org.iplantc.service.monitor.dao.MonitorDao;
import org.iplantc.service.monitor.exceptions.MonitorException;
import org.iplantc.service.monitor.managers.MonitorManager;
import org.iplantc.service.monitor.model.Monitor;
import org.iplantc.service.monitor.util.ServiceUtils;
import org.iplantc.service.notification.util.EmailMessage;
import org.joda.time.DateTime;
import org.quartz.JobExecutionContext;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.surftools.BeanstalkClient.BeanstalkException;

/**
 * Class to check for monitors who are past their next run time and need to run.
 * All pending monitor tasks are placed on the monitor work queue to run.
 *
 * @author dooley
 *
 */
public class MonitorCronListener implements org.quartz.Job
{
	private static final Logger	log	= Logger.getLogger(MonitorCronListener.class);

	private MonitorDao dao = new MonitorDao();
	private MessageQueueClient messageClient;

	/* (non-Javadoc)
	 * @see org.quartz.Job#execute(org.quartz.JobExecutionContext)
	 */
	public void execute(JobExecutionContext context)
	{
//		log.debug("TRIGGER: " + context.getTrigger().getKey());
		try
		{
			messageClient = MessageClientFactory.getMessageClient();

			Monitor monitor = dao.getNextPendingActiveMonitor();
			while (monitor != null)
			{
				try
				{
					MonitorManager manager = new MonitorManager();
					manager.resetNextUpdateTime(monitor);

					JsonNode json = new ObjectMapper().createObjectNode()
							.put("uuid", monitor.getUuid())
							.put("target", monitor.getSystem().getSystemId())
							.put("owner", monitor.getOwner());

					messageClient.push(Settings.MONITOR_TOPIC, Settings.MONITOR_QUEUE, json.toString());

					// we could just process them here, but by pushing to the work queue
					// we can cleanly separte the workers from the service and run
					// workers anywhere
					//MonitorManager.check(monitor);
				}
				catch (Throwable e)
				{
					String message;
					if (e instanceof BeanstalkException && e.getMessage().contains("Connection refused")) 
				    {
				        message = "A monitor producer on " + ServiceUtils.getLocalIP() 
		                        + " is unable to connect to the " + Settings.MESSAGING_SERVICE_PROVIDER + " message queue at " 
		                        + Settings.MESSAGING_SERVICE_HOST + ":" + Settings.MESSAGING_SERVICE_PORT 
		                        + ".  Monitor will be queued again during next cron check.";
				        log.error(message, e);
		            } 
				    else { 
				    	DateTime maxFailureDate = new DateTime(monitor.getLastUpdated()).plusMinutes(Settings.MAX_MONITOR_QUEUE_FAILURES * monitor.getFrequency());
				    	if (new Date().after(maxFailureDate.toDate()))
					    {
					    	message = "Monitor " + monitor.getUuid() + " for target " + monitor.getSystem().getSystemId() +
	                                " failed to process after " + Settings.MAX_MONITOR_QUEUE_FAILURES + " attemptes. " +
	                                "This monitor has been disabled ";
					    	
			    			log.error(message, e);
			    			try {
				    			// disable the monitor since it's failed for too long
				    			monitor.setActive(false);
								monitor.setLastUpdated(new Date());
								dao.persist(monitor);
			    			}
			    			catch (Throwable t) {
			    				log.error("Failed to disable monitor after excessive failed attempts.", e);
			    			}
//							
//								// send an email to whoever mans the  tenant.
//								Tenant tenant = null;
//								String tenantId = monitor.getTenantId();
//								if (StringUtils.isEmpty(tenantId)) {
//									tenantId = TenancyHelper.getCurrentTenantId();
//								}
//								
//								tenant = new TenantDao().findByTenantId(tenantId);
//								
//								if (tenant != null) {
//									EmailMessage.send(tenant.getContactName(),
//										tenant.getContactEmail(),
//										"Monitor worker died unexpectedly",
//										message + "\n" + ExceptionUtils.getStackTrace(e),
//										"<p>" + message + "</p><pre>" + ExceptionUtils.getStackTrace(e) + "</pre></p>");
//								}
//							} catch (Throwable e1) {
//								log.error("Failed to send monitor worker failure message to admin.",e1);
//							}
					    }
					    else {
					    	log.error("Failed to put monitor on work queue. Monitor will be queued again during next cron check.", e);
					    }
				    }
				}
				monitor = dao.getNextPendingActiveMonitor();
			}

		}
		catch (StaleObjectStateException e) {
			log.debug("Just avoided a monitor check race condition.");
		}
		catch (MonitorException e)
		{
			log.error("Failed to retrieve list of active monitors. No monitors will be added to the message queue.", e);
//			try {
//				// send an email to whoever mans the default tenant.
//				String message = "Monitor worker on " + ServiceUtils.getLocalIP() + " failed to retrieve a " +
//						" list of monitors from the database. No monitors will be processed "
//						+ "until the connection returns.";
//				Tenant tenant = new TenantDao().findByTenantId(TenancyHelper.getCurrentTenantId());
//				EmailMessage.send(tenant.getContactName(),
//					tenant.getContactEmail(),
//					"Monitor worker unable to retrieve list of monitors",
//					message + "\n\n" +  ExceptionUtils.getStackTrace(e),
//					"<p>" + message + "</p><pre>" + ExceptionUtils.getStackTrace(e) + "</pre></p>");
//			} catch (Throwable e1) {
//				log.error("Failed to send monitor worker failure message to admin.",e1);
//			}
		}
		catch (MessagingException e)
		{
//			log.error("Failed to connect to message queue. No monitors will be added to the message queue.", e);
			try {
				// send an email to whoever mans the default tenant.
				String message = "Monitor worker on " + ServiceUtils.getLocalIP() + " failed to connect to the " +
						Settings.MESSAGING_SERVICE_PROVIDER + " messaging service on " +
						Settings.MESSAGING_SERVICE_HOST +
						". No monitors will be processed until the connection returns.";
				log.error(message, e);
				
//				Tenant tenant = new TenantDao().findByTenantId(TenancyHelper.getCurrentTenantId());
//				EmailMessage.send(tenant.getContactName(),
//					tenant.getContactEmail(),
//					"Monitor worker unable to connect to messaging queue",
//					message + "\n\n" +  ExceptionUtils.getStackTrace(e),
//					"<p>" + message + "</p><pre>" + ExceptionUtils.getStackTrace(e) + "</pre></p>");
			} catch (Throwable e1) {
				log.error("Failed to send monitor worker failure message to admin.",e1);
			}
		}
		finally {
			try { messageClient.stop(); } catch (Exception e) {}
		}

	}
}
