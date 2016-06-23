/**
 * 
 */
package org.iplantc.service.jobs.exceptions;

/**
 * @author dooley
 * 
 */
@SuppressWarnings("serial")
public class JobException extends Exception {

	/**
	 * 
	 */
	public JobException() {}

	/**
	 * @param message
	 */
	public JobException(String message)
	{
		super(message);
	}

	/**
	 * @param cause
	 */
	public JobException(Throwable cause)
	{
		super(cause);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public JobException(String message, Throwable cause)
	{
		super(message, cause);
	}

}
