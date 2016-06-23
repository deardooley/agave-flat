/**
 * 
 */
package org.iplantc.service.tags.exceptions;

/**
 * @author dooley
 *
 */
public class UnknownIdentityException extends Exception {

	private static final long serialVersionUID = -6376343460696066808L;

	/**
	 * 
	 */
	public UnknownIdentityException() {
	}

	/**
	 * @param arg0
	 */
	public UnknownIdentityException(String arg0) {
		super(arg0);
	}

	/**
	 * @param arg0
	 */
	public UnknownIdentityException(Throwable arg0) {
		super(arg0);
	}

	/**
	 * @param arg0
	 * @param arg1
	 */
	public UnknownIdentityException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

}
