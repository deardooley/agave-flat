/**
 * 
 */
package org.iplantc.service.notification.providers.sms.clients;

import org.apache.log4j.Logger;
import org.iplantc.service.notification.exceptions.NotificationException;
import org.iplantc.service.notification.model.NotificationAttempt;
import org.iplantc.service.notification.model.NotificationAttemptResponse;
import org.iplantc.service.notification.model.enumerations.NotificationCallbackProviderType;
import org.iplantc.service.notification.providers.sms.enumeration.SmsProviderType;
import org.iplantc.service.notification.util.ServiceUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author dooley
 *
 */
public class LoggingSmsClient extends AbstractSmsClient {
	private static final Logger	log	= Logger.getLogger(LoggingSmsClient.class);
	
	public LoggingSmsClient(NotificationAttempt attempt) {
		super(attempt);
	}
	
	/* (non-Javadoc)
	 * @see org.iplantc.service.notification.sms.clients.SmsClient#publish(org.iplantc.service.notification.model.Notification, java.lang.String)
	 */
	@Override
	public NotificationAttemptResponse publish() throws NotificationException 
	{
		NotificationAttemptResponse attemptResponse = new NotificationAttemptResponse();
		
		long callstart = System.currentTimeMillis();
        
		try 
		{
			// write the message to the log file
			log.info(new ObjectMapper().createObjectNode()
				.put("Body", attempt.getContent())
				.put("To", ServiceUtils.formatPhoneNumberForSMS(attempt.getCallbackUrl()))
				.put("From", getClass().getSimpleName()).toString());
			
			log.debug("[" + attempt.getUuid() + "] Successfully sent " + attempt.getEventName() + 
				getSupportedCallbackProviderType() + " notification to " + attempt.getCallbackUrl());
			attemptResponse.setCode(200);
			attemptResponse.setMessage("200 ok");
		}
		catch (Exception e) 
		{
			attemptResponse.setCode(500);
			attemptResponse.setMessage("Failed to send " + attempt.getEventName() + 
					getSupportedCallbackProviderType() + " notification to " + attempt.getCallbackUrl() +
					" due to internal server error. Write operation failed after " + 
						(System.currentTimeMillis() - callstart) + " milliseconds.");
			log.error("[" + attempt.getUuid() + "] " + attemptResponse.getMessage(), e);
		}
		
		return attemptResponse;
		
	}

	@Override
	public String getSupportedCallbackProviderType() {
		return SmsProviderType.LOG.name().toLowerCase() + NotificationCallbackProviderType.SMS.name().toLowerCase();
	}

}
