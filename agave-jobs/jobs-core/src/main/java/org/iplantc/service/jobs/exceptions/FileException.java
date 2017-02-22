/**
 * 
 */
package org.iplantc.service.jobs.exceptions;

/**
 * @author dooley
 * 
 */
@SuppressWarnings("serial")
public class FileException extends Exception {

	/**
	 * 
	 */
	public FileException()
	{
	}

	/**
	 * @param message
	 */
	public FileException(String message)
	{
		super(message);
	}

	/**
	 * @param cause
	 */
	public FileException(Throwable cause)
	{
		super(cause);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public FileException(String message, Throwable cause)
	{
		super(message, cause);
	}

}
