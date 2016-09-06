/**
 * 
 */
package org.iplantc.service.common.exceptions;

/**
 * @author dooley
 *
 */
public class AgaveCacheException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4605650235456386025L;

	/**
	 * 
	 */
	public AgaveCacheException() {
	}

	/**
	 * @param message
	 */
	public AgaveCacheException(String message) {
		super(message);
	}

	/**
	 * @param cause
	 */
	public AgaveCacheException(Throwable cause) {
		super(cause);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public AgaveCacheException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * @param message
	 * @param cause
	 * @param enableSuppression
	 * @param writableStackTrace
	 */
	public AgaveCacheException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
