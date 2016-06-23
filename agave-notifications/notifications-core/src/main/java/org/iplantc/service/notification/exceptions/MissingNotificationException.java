/**
 * 
 */
package org.iplantc.service.notification.exceptions;

/**
 * @author dooley
 *
 */
public class MissingNotificationException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4643089779986625624L;

	/**
	 * 
	 */
	public MissingNotificationException() {
	}

	/**
	 * @param message
	 */
	public MissingNotificationException(String message) {
		super(message);
	}

	/**
	 * @param cause
	 */
	public MissingNotificationException(Throwable cause) {
		super(cause);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public MissingNotificationException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * @param message
	 * @param cause
	 * @param enableSuppression
	 * @param writableStackTrace
	 */
	public MissingNotificationException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
