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
public enum ErrorStrategyType {
	
	/**
	 * Die and disregard notification 
	 */
	NONE,
	
	/**
	 * Cache the failed notifications into a queue 
	 */
	QUEUE;
}
