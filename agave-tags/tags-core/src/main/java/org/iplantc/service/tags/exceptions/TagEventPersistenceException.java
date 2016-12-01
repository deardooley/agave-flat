/**
 * 
 */
package org.iplantc.service.tags.exceptions;

/**
 * @author dooley
 *
 */
public class TagEventPersistenceException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3680340103138343268L;

	/**
	 * 
	 */
	public TagEventPersistenceException() {
	}

	/**
	 * @param message
	 */
	public TagEventPersistenceException(String message) {
		super(message);
	}

	/**
	 * @param cause
	 */
	public TagEventPersistenceException(Throwable cause) {
		super(cause);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public TagEventPersistenceException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * @param message
	 * @param cause
	 * @param enableSuppression
	 * @param writableStackTrace
	 */
	public TagEventPersistenceException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
