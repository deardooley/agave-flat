/**
 * 
 */
package org.iplantc.service.tags.exceptions;

/**
 * @author dooley
 *
 */
public class DuplicateTaggedResourceException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3666635067314951993L;

	/**
	 * 
	 */
	public DuplicateTaggedResourceException() {
	}

	/**
	 * @param message
	 */
	public DuplicateTaggedResourceException(String message) {
		super(message);
	}

	/**
	 * @param cause
	 */
	public DuplicateTaggedResourceException(Throwable cause) {
		super(cause);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public DuplicateTaggedResourceException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * @param message
	 * @param cause
	 * @param enableSuppression
	 * @param writableStackTrace
	 */
	public DuplicateTaggedResourceException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
