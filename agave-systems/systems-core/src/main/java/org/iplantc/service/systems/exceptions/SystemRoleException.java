/**
 * 
 */
package org.iplantc.service.systems.exceptions;

/**
 * @author dooley
 *
 */
public class SystemRoleException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 762604473983292579L;

	/**
	 * 
	 */
	public SystemRoleException() {
	}

	/**
	 * @param message
	 */
	public SystemRoleException(String message) {
		super(message);
	}

	/**
	 * @param cause
	 */
	public SystemRoleException(Throwable cause) {
		super(cause);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public SystemRoleException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * @param message
	 * @param cause
	 * @param enableSuppression
	 * @param writableStackTrace
	 */
	public SystemRoleException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
