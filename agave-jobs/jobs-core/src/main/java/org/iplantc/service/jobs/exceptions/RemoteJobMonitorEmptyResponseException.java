/**
 * 
 */
package org.iplantc.service.jobs.exceptions;

/**
 * @author dooley
 *
 */
public class RemoteJobMonitorEmptyResponseException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1895985005061789587L;

	/**
	 * 
	 */
	public RemoteJobMonitorEmptyResponseException() {
	}

	/**
	 * @param message
	 */
	public RemoteJobMonitorEmptyResponseException(String message) {
		super(message);
	}

	/**
	 * @param cause
	 */
	public RemoteJobMonitorEmptyResponseException(Throwable cause) {
		super(cause);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public RemoteJobMonitorEmptyResponseException(String message,
			Throwable cause) {
		super(message, cause);
	}

	/**
	 * @param message
	 * @param cause
	 * @param enableSuppression
	 * @param writableStackTrace
	 */
	public RemoteJobMonitorEmptyResponseException(String message,
			Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
