/**
 * 
 */
package org.iplantc.service.notification.model.enumerations;

/**
 * Valid failed notification delivery strategies.
 * 
 * @author dooley
 *
 */
public enum NotificationStatusType {
	
	/**
	 * Subscription is active 
	 */
	ACTIVE,
	
	/**
	 * Subscription has been disabled and notifications will not send 
	 */
	INACTIVE,
	
	/**
	 * Subscription has failed to send and is out of service
	 */
	FAILED,
	
	/**
	 * Subscription has successfully sent and is out of service
	 */
	COMPLETE;
}
