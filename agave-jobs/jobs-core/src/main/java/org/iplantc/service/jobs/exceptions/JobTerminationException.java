/**
 * 
 */
package org.iplantc.service.jobs.exceptions;

/**
 * @author dooley
 * 
 */
@SuppressWarnings("serial")
public class JobTerminationException extends Exception {

	/**
	 * 
	 */
	public JobTerminationException() {}

	/**
	 * @param message
	 */
	public JobTerminationException(String message)
	{
		super(message);
	}

	/**
	 * @param cause
	 */
	public JobTerminationException(Throwable cause)
	{
		super(cause);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public JobTerminationException(String message, Throwable cause)
	{
		super(message, cause);
	}

}
