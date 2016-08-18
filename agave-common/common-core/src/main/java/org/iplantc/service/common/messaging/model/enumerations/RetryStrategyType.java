/**
 * 
 */
package org.iplantc.service.common.messaging.model.enumerations;

/**
 * Valid behaviors by which notification delivery is retried.
 * 
 * @author dooley
 *
 */
public enum RetryStrategyType {
	
	/**
	 * No subsequent retries after the first failed attempt 
	 */
	NONE,
	
	/**
	 * Retries are processed immediately 
	 */
	IMMEDIATE,
	
	/**
	 * Pauses for a user-defined amount of time between retries. 
	 */
	DELAYED,
	
	/**
	 * Retries are performed according to an exponential backoff schedule. 
	 */
	EXPONENTIAL;
}
