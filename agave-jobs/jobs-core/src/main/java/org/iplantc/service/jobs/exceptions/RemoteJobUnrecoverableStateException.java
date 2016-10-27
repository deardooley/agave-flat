/**
 * 
 */
package org.iplantc.service.jobs.exceptions;

/**
 * @author dooley
 *
 */
public class RemoteJobUnrecoverableStateException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -9045807483816053893L;

	/**
	 * 
	 */
	public RemoteJobUnrecoverableStateException() {
	}

	/**
	 * @param message
	 */
	public RemoteJobUnrecoverableStateException(String message) {
		super(message);
	}

	/**
	 * @param cause
	 */
	public RemoteJobUnrecoverableStateException(Throwable cause) {
		super(cause);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public RemoteJobUnrecoverableStateException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * @param message
	 * @param cause
	 * @param enableSuppression
	 * @param writableStackTrace
	 */
	public RemoteJobUnrecoverableStateException(String message,
			Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
