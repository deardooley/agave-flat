/**
 * 
 */
package org.iplantc.service.auth.exceptions;

/**
 * @author dooley
 * 
 */
@SuppressWarnings("serial")
public class AuthenticationTokenException extends RuntimeException {

	/**
	 * 
	 */
	public AuthenticationTokenException()
	{
	}

	/**
	 * @param message
	 */
	public AuthenticationTokenException(String message)
	{
		super(message);
	}

	/**
	 * @param cause
	 */
	public AuthenticationTokenException(Throwable cause)
	{
		super(cause);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public AuthenticationTokenException(String message, Throwable cause)
	{
		super(message, cause);
	}

}
