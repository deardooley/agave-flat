/**
 * 
 */
package org.iplantc.service.notification.providers.email.enumeration;

/**
 * Types of email providers supported
 * @author dooley
 *
 */
/**
 * @author dooley
 *
 */
public enum EmailProviderType {

	/**
	 * Sends email using SendGrid API 
	 */
	SENDGRID,
	
	/**
	 * Sends email via smtp 
	 */
	SMTP,
	
	/**
	 * Sends mail via unauthenticated localhost email server
	 */
	LOCAL,
	
	
	/**
	 * Writes email to log file 
	 */
	LOG,
	
	
	/**
	 * Ignores emails entirely 
	 */
	NONE
}
