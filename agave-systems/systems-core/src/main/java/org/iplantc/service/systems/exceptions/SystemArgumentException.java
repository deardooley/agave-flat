/**
 * 
 */
package org.iplantc.service.systems.exceptions;

/**
 * @author dooley
 * 
 */
@SuppressWarnings("serial")
public class SystemArgumentException extends Exception {

	/**
	 * 
	 */
	public SystemArgumentException()
	{
	}

	/**
	 * @param message
	 */
	public SystemArgumentException(String message)
	{
		super(message);
	}

	/**
	 * @param cause
	 */
	public SystemArgumentException(Throwable cause)
	{
		super(cause);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public SystemArgumentException(String message, Throwable cause)
	{
		super(message, cause);
	}

}
