/**
 * 
 */
package org.iplantc.service.io.exceptions;

/**
 * @author dooley
 *
 */
public class TransformException extends Exception {

	private static final long serialVersionUID = 4536513385259959708L;

	/**
	 * 
	 */
	public TransformException() {}

	/**
	 * @param message
	 */
	public TransformException(String message) {
		super(message);
	}

	/**
	 * @param cause
	 */
	public TransformException(Throwable cause) {
		super(cause);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public TransformException(String message, Throwable cause) {
		super(message, cause);
	}

}
