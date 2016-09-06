package org.iplantc.service.common.exceptions;

public class TaskQueueException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2363333419901860112L;

	/**
	 * 
	 */
	public TaskQueueException() {
		super();
	}

	/**
	 * @param message
	 * @param cause
	 * @param enableSuppression
	 * @param writableStackTrace
	 */
	public TaskQueueException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public TaskQueueException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * @param message
	 */
	public TaskQueueException(String message) {
		super(message);
	}

	/**
	 * @param cause
	 */
	public TaskQueueException(Throwable cause) {
		super(cause);
	}

}
