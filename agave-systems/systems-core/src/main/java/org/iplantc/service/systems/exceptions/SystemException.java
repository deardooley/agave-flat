/**
 * 
 */
package org.iplantc.service.systems.exceptions;

/**
 * @author dooley
 * 
 */
@SuppressWarnings("serial")
public class SystemException extends RuntimeException {

	/**
	 * 
	 */
	public SystemException()
	{
	}

	/**
	 * @param message
	 */
	public SystemException(String message)
	{
		super(message);
	}

	/**
	 * @param cause
	 */
	public SystemException(Throwable cause)
	{
		super(cause);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public SystemException(String message, Throwable cause)
	{
		super(message, cause);
	}

}
