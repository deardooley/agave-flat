package org.iplantc.service.jobs.queue.managers;

import org.apache.log4j.Logger;
import org.iplantc.service.common.Settings;
import org.iplantc.service.common.exceptions.MessagingException;
import org.iplantc.service.common.exceptions.NotificationException;
import org.iplantc.service.common.messaging.MessageClientFactory;
import org.iplantc.service.common.messaging.clients.MessageQueueClient;
import org.iplantc.service.common.messaging.model.RetryCalculator;
import org.iplantc.service.common.messaging.model.RetryPolicy;
import org.iplantc.service.common.messaging.model.RetryPolicyHelper;
import org.iplantc.service.common.messaging.model.enumerations.RetryStrategyType;
import org.iplantc.service.jobs.queue.messaging.JobMessageBody;
import org.iplantc.service.jobs.queue.messaging.JobMessageContext;

/**
 * Manager class for handling tasks on the archive queue
 * @author dooley
 *
 */
public class ArchiveQueueManager {
	
	private static final Logger log = Logger
			.getLogger(ArchiveQueueManager.class);
	
	public ArchiveQueueManager() {
	}
	
	/**
	 * Writes a {@link JobMessageBody} to a message queue and topic appropriate 
	 * for the given {@code jobUuid}. 
	 * @param jobUuid
	 * @param jobOwner
	 * @param jobTenantId
	 * @param attempt
	 * @return
	 */
	public boolean push(String jobUuid, String jobOwner, String jobTenantId, int attempt) {
		MessageQueueClient queue = null;
		try {
			queue = MessageClientFactory.getMessageClient();
			
			try {
				RetryPolicy policy = 
						RetryPolicyHelper.getDefaultRetryPolicyForRetryStrategyType(
								RetryStrategyType.EXPONENTIAL);
				RetryCalculator retryCalculator = new RetryCalculator(policy, attempt);
				int secondsUntilNextScheduledAttempt = retryCalculator.getNextScheduledTime();
				
				JobMessageContext messageBodyContext = new JobMessageContext(policy, attempt);

				JobMessageBody messageBody = new JobMessageBody(jobUuid, jobOwner, jobTenantId,
						messageBodyContext);

				queue.push(jobTenantId + "." + Settings.JOBS_ARCHIVING_TOPIC,
						Settings.JOBS_ARCHIVING_QUEUE, 
						messageBody.toJSON(),
						secondsUntilNextScheduledAttempt);
				
				return true;
			} 
			catch (Exception e) {
				log.error("Failed to push archiving message for job " + jobUuid);
			}			
		} 
		catch (MessagingException e) {
			log.error(
					"Failed to connect to the messaging queue. No archiving message "
					+ "will be written for job " + jobUuid, e);
//		} catch (NotificationException e) {
//			log.error("Failed to process notifications for " + jobId
//					+ " on event " + notificationEvent, e);
		} catch (Throwable e) {
			log.error(
					"Unknown messaging exception occurred. Failed to push archiving "
					+ "message for job " + jobUuid, e);
		} finally {
			if (queue != null) {
				queue.stop();
			}
		}
		
		return false;
	}

}
