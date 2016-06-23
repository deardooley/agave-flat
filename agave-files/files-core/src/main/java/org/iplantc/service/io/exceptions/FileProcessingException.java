/**
 * 
 */
package org.iplantc.service.io.exceptions;

/**
 * @author dooley
 *
 */
public class FileProcessingException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3930877072924526797L;

	/**
	 * 
	 */
	public FileProcessingException() {
	}

	/**
	 * @param message
	 */
	public FileProcessingException(String message) {
		super(message);
	}

	/**
	 * @param cause
	 */
	public FileProcessingException(Throwable cause) {
		super(cause);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public FileProcessingException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * @param message
	 * @param cause
	 * @param enableSuppression
	 * @param writableStackTrace
	 */
	public FileProcessingException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
