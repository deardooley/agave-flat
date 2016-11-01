/**
 * 
 */
package org.iplantc.service.jobs.exceptions;

/**
 * @author dooley
 *
 */
public class RemoteJobFailureDetectedException extends
		RemoteJobUnrecoverableStateException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4670514417643984817L;

	/**
	 * 
	 */
	public RemoteJobFailureDetectedException() {
	}

	/**
	 * @param message
	 */
	public RemoteJobFailureDetectedException(String message) {
		super(message);
	}

	/**
	 * @param cause
	 */
	public RemoteJobFailureDetectedException(Throwable cause) {
		super(cause);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public RemoteJobFailureDetectedException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * @param message
	 * @param cause
	 * @param enableSuppression
	 * @param writableStackTrace
	 */
	public RemoteJobFailureDetectedException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
