/**
 * 
 */
package org.iplantc.service.common.exceptions;

import org.iplantc.service.common.exceptions.TaskQueueException;

/**
 * @author dooley
 *
 */
public class TaskQueueLockException extends TaskQueueException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -33967447527346638L;

	/**
	 * 
	 */
	public TaskQueueLockException() {
		super();
	}

	/**
	 * @param message
	 * @param cause
	 * @param enableSuppression
	 * @param writableStackTrace
	 */
	public TaskQueueLockException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public TaskQueueLockException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * @param message
	 */
	public TaskQueueLockException(String message) {
		super(message);
	}

	/**
	 * @param cause
	 */
	public TaskQueueLockException(Throwable cause) {
		super(cause);
	}

}
