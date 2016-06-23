/**
 * 
 */
package org.iplantc.service.io.exceptions;

/**
 * @author dooley
 *
 */
public class UnsupportedProtocolException extends Exception {

	private static final long serialVersionUID = 5244243480178656773L;

	/**
	 * 
	 */
	public UnsupportedProtocolException() {}

	/**
	 * @param message
	 */
	public UnsupportedProtocolException(String message) {
		super(message);
	}

	/**
	 * @param cause
	 */
	public UnsupportedProtocolException(Throwable cause) {
		super(cause);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public UnsupportedProtocolException(String message, Throwable cause) {
		super(message, cause);	
	}

}
