/**
 * 
 */
package org.iplantc.service.data.exceptions;

/**
 * @author dooley
 *
 */
public class TransformPersistenceException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8016393539702975951L;

	/**
	 * 
	 */
	public TransformPersistenceException() {
	}

	/**
	 * @param message
	 */
	public TransformPersistenceException(String message) {
		super(message);
	}

	/**
	 * @param cause
	 */
	public TransformPersistenceException(Throwable cause) {
		super(cause);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public TransformPersistenceException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * @param message
	 * @param cause
	 * @param enableSuppression
	 * @param writableStackTrace
	 */
	public TransformPersistenceException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
