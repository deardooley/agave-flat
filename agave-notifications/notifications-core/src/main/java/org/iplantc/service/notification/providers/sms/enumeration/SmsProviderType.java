/**
 * 
 */
package org.iplantc.service.notification.providers.sms.enumeration;

/**
 * @author dooley
 *
 */
public enum SmsProviderType {
    
	/**
	 * Uses Twilio API to send SMS
	 */
	TWILIO, 
	
    /**
     * Writes messages to log. The message format is a JSON representation
     * of the message sent by the {@link TwilioSmsClient}  
     */
    LOG,
    
    /**
     * Ignores notification delivery to all SMS destinations. 
     */
    NONE;
}
