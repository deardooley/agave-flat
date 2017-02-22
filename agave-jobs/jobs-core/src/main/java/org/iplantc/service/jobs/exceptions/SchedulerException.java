/**
 * 
 */
package org.iplantc.service.jobs.exceptions;

/**
 * @author dooley
 * 
 */
@SuppressWarnings("serial")
public class SchedulerException extends Exception {

	/**
	 * 
	 */
	public SchedulerException()
	{
	}

	/**
	 * @param message
	 */
	public SchedulerException(String message)
	{
		super(message);
	}

	/**
	 * @param cause
	 */
	public SchedulerException(Throwable cause)
	{
		super(cause);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public SchedulerException(String message, Throwable cause)
	{
		super(message, cause);
	}

}
