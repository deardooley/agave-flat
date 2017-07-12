/**
 * 
 */
package org.iplantc.service.jobs.exceptions;

/**
 * @author dooley
 *
 */
public class RemoteJobMonitorCommandSyntaxException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8839216462361641974L;

	/**
	 * 
	 */
	public RemoteJobMonitorCommandSyntaxException() {
	}

	/**
	 * @param message
	 */
	public RemoteJobMonitorCommandSyntaxException(String message) {
		super(message);
	}

	/**
	 * @param cause
	 */
	public RemoteJobMonitorCommandSyntaxException(Throwable cause) {
		super(cause);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public RemoteJobMonitorCommandSyntaxException(String message,
			Throwable cause) {
		super(message, cause);
	}

	/**
	 * @param message
	 * @param cause
	 * @param enableSuppression
	 * @param writableStackTrace
	 */
	public RemoteJobMonitorCommandSyntaxException(String message,
			Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
