/**
 * 
 */
package org.iplantc.service.apps.exceptions;

/**
 * @author dooley
 *
 */
public class SoftwareEventPersistenceException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7587475762588923423L;

	/**
	 * 
	 */
	public SoftwareEventPersistenceException() {
	}

	/**
	 * @param message
	 */
	public SoftwareEventPersistenceException(String message) {
		super(message);
	}

	/**
	 * @param cause
	 */
	public SoftwareEventPersistenceException(Throwable cause) {
		super(cause);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public SoftwareEventPersistenceException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * @param message
	 * @param cause
	 * @param enableSuppression
	 * @param writableStackTrace
	 */
	public SoftwareEventPersistenceException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
