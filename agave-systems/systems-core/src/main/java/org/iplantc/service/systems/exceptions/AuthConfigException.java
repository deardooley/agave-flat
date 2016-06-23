/**
 * 
 */
package org.iplantc.service.systems.exceptions;

/**
 * @author dooley
 * 
 */
@SuppressWarnings("serial")
public class AuthConfigException extends Exception {

	/**
	 * 
	 */
	public AuthConfigException()
	{
	}

	/**
	 * @param message
	 */
	public AuthConfigException(String message)
	{
		super(message);
	}

	/**
	 * @param cause
	 */
	public AuthConfigException(Throwable cause)
	{
		super(cause);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public AuthConfigException(String message, Throwable cause)
	{
		super(message, cause);
	}

}
