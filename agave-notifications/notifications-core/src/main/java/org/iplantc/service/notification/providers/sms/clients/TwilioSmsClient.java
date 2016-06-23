/**
 * 
 */
package org.iplantc.service.notification.providers.sms.clients;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.apache.log4j.Logger;
import org.iplantc.service.notification.exceptions.NotificationException;
import org.iplantc.service.notification.model.NotificationAttempt;
import org.iplantc.service.notification.model.NotificationAttemptResponse;
import org.iplantc.service.notification.model.enumerations.NotificationCallbackProviderType;
import org.iplantc.service.notification.providers.sms.enumeration.SmsProviderType;
import org.iplantc.service.notification.util.ServiceUtils;

import com.twilio.sdk.TwilioRestClient;
import com.twilio.sdk.TwilioRestException;
import com.twilio.sdk.resource.factory.MessageFactory;
import com.twilio.sdk.resource.instance.Message;

/**
 * @author dooley
 *
 */
public class TwilioSmsClient extends AbstractSmsClient {
	private static final Logger	log	= Logger.getLogger(TwilioSmsClient.class);

	private String twilioAccountSid;
	private String twilioAuthToken;
	private String fromPhoneNumber;
	
	public TwilioSmsClient(NotificationAttempt attempt, String twilioAccountSid, String twilioAuthToken, String fromPhoneNumber) {
		super(attempt);
		this.twilioAccountSid = twilioAccountSid;
		this.twilioAuthToken = twilioAuthToken;
		this.fromPhoneNumber = fromPhoneNumber;
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
			TwilioRestClient client = new TwilioRestClient(twilioAccountSid, twilioAuthToken);
			 
		    // Build a filter for the MessageList
		    List<NameValuePair> params = new ArrayList<NameValuePair>();
		    params.add(new BasicNameValuePair("Body", attempt.getContent()));
		    params.add(new BasicNameValuePair("To", ServiceUtils.formatPhoneNumberForSMS(attempt.getCallbackUrl())));
		    params.add(new BasicNameValuePair("From", fromPhoneNumber));
		     
		    MessageFactory messageFactory = client.getAccount().getMessageFactory();
		    Message message = messageFactory.create(params);
		    
		    // if we get any id back, it was good
		    if (message.getAccountSid() != null) {
				log.debug("[" + attempt.getUuid() + "] Successfully sent " + attempt.getEventName() + 
					getSupportedCallbackProviderType() + " notification to " + attempt.getCallbackUrl());
				attemptResponse.setCode(200);
		    } 
		    // otherwise it failed and we'll retry
		    else {
		    	log.debug("[" + attempt.getUuid() + "] Failed to send " + attempt.getEventName() + 
					getSupportedCallbackProviderType() + " notification to " + attempt.getCallbackUrl());
		    	attemptResponse.setCode(500);
		    	attemptResponse.setMessage(message.getBody());
		    }
		} 
		catch (TwilioRestException e) {
			attemptResponse.setCode(500);
			// TODO: check for account-specific issues and email admin to fix their account
			switch (e.getErrorCode()) {
				case 10001: //Account is not active
				case 10002: //Trial account does not support this feature
				case 10003: //Incoming call rejected due to inactive account
				case 14102: //Message "From" Attribute is Invalid
				case 14107: //Message rate limit exceeded
				case 14109: //Message Reply message limit exceeded
				case 14111: //Invalid To phone number for Trial mode
				case 20002: //Invalid FriendlyName
				case 20003: //Permission Denied
				case 20004: //Method not allowed
				case 20005: //Account not active
				case 20006: //Access Denied
				case 20008: //Cannot access this resource with Test Credentials
				case 20403: //403 Forbidden
				case 20429: //429 Too Many Requests
				case 21201: //No 'To' number specified
				case 21202: //'To' number is a premium number
				case 21203: //International calling not enabled
				case 14108: //Message "From" Phone Number not SMS capable
				case 21215: //Account not authorized to call phone number
				case 21216: //Account not allowed to call phone number
				case 21212: //Invalid 'From' Phone Number
				case 21213: //'From' phone number is required
				case 21238: //Address Validation Error
				case 21243: //Credential Validation Error
				case 21471: // Account does not exist
				case 21472: // Account is not active
				case 21603: // 'From' phone number is required to send a Message
				case 21606: //: //The 'From' phone number provided is not a valid, message-capable Twilio phone number.
				case 21607: //The 'From' number is not a valid, SMS-capable Twilio number
				case 21608: //This number can send messages only to verified numbers
				case 21609: //Invalid StatusCallback url
				case 21613: //PhoneNumber Requires Certification
				case 21616: //The 'From' number matches multiple numbers for your account
				case 21621: //The 'From' number has not been enabled for picture messaging
				case 21622: //MMS has not been enabled for your account
				case 30001: //Message Delivery - Queue overflow
				case 30002: //Message Delivery - Account suspended
				case 30003: //Message Delivery - Unreachable destination handset
				case 30004: //Message Delivery - Message blocked
				case 30005: //Message Delivery - Unknown destination handset
				case 30006: //Message Delivery - Landline or unreachable carrier
				case 30007: //Message Delivery - Carrier violation
					// account is jacked, quit now.
					attemptResponse.setCode(401);
					attemptResponse.setMessage(e.getErrorMessage());
					log.error("[" + attempt.getUuid() + "] Failed to send " + attempt.getEventName() + 
					getSupportedCallbackProviderType() + " notification to " + attempt.getCallbackUrl() + 
							". Twilio replied: " + e.getErrorMessage());
				
				case 21211: //Invalid 'To' Phone Number
				case 21401: //Invalid Phone Number
				case 21214: //'To' phone number cannot be reached
				case 21219: //'To' phone number not verified
				case 21421: //PhoneNumber is invalid
				case 21614: //'To' number is not a valid mobile number
				case 21618: //The message body cannot be sent
				case 21610: //Message cannot be sent to the 'To' number because the customer has replied with STOP
					// callback number is bad, quit now
					
					attemptResponse.setCode(400);
					attemptResponse.setMessage(e.getErrorMessage());
					log.error("[" + attempt.getUuid() + "] Failed to send " + attempt.getEventName() + 
					getSupportedCallbackProviderType() + " notification to " + attempt.getCallbackUrl() + 
							". Twilio replied: " + e.getErrorMessage());
					
				default:
					// service probably flickered, try again
					attemptResponse.setCode(500);
					break;
			}
			attemptResponse.setMessage(e.getErrorMessage());
			log.error("[" + attempt.getUuid() + "] Failed to send " + attempt.getEventName() + 
					getSupportedCallbackProviderType() + " notification to " + attempt.getCallbackUrl() + 
					". Twilio replied: " + e.getErrorMessage());
		}
		
		catch (Exception e) 
		{
			attemptResponse.setCode(500);
			attemptResponse.setMessage("Failed to send " + attempt.getEventName() + 
					getSupportedCallbackProviderType() + " notification to " + attempt.getCallbackUrl() +
					" due to internal server error. Remote call failed after " + 
						(System.currentTimeMillis() - callstart) + " milliseconds.");
			log.error("[" + attempt.getUuid() + "] " + attemptResponse.getMessage(), e);
		}
		
		return attemptResponse;
		
	}

	@Override
	public String getSupportedCallbackProviderType() {
		return SmsProviderType.TWILIO.name().toLowerCase() + NotificationCallbackProviderType.SMS.name().toLowerCase();
	}

}
