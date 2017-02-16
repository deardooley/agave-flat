/**
 * 
 */
package org.iplantc.service.systems.exceptions;

/**
 * @author dooley
 *
 */
public class RolePersistenceException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5790694970498468264L;

	/**
	 * 
	 */
	public RolePersistenceException() {
	}

	/**
	 * @param message
	 */
	public RolePersistenceException(String message) {
		super(message);
	}

	/**
	 * @param cause
	 */
	public RolePersistenceException(Throwable cause) {
		super(cause);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public RolePersistenceException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * @param message
	 * @param cause
	 * @param enableSuppression
	 * @param writableStackTrace
	 */
	public RolePersistenceException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
