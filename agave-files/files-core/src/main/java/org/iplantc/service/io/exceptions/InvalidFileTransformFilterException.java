/**
 * 
 */
package org.iplantc.service.io.exceptions;

/**
 * @author dooley
 *
 */
public class InvalidFileTransformFilterException extends Exception {

	private static final long serialVersionUID = 5529057238805752022L;

	/**
	 * 
	 */
	public InvalidFileTransformFilterException() {}

	/**
	 * @param message
	 */
	public InvalidFileTransformFilterException(String message) {
		super(message);
	}

	/**
	 * @param cause
	 */
	public InvalidFileTransformFilterException(Throwable cause) {
		super(cause);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public InvalidFileTransformFilterException(String message, Throwable cause) {
		super(message, cause);
	}

}
