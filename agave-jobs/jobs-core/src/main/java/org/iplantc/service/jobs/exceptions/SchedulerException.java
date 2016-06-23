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
	// TODO Auto-generated constructor stub
	}

	/**
	 * @param message
	 */
	public SchedulerException(String message)
	{
		super(message);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param cause
	 */
	public SchedulerException(Throwable cause)
	{
		super(cause);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param message
	 * @param cause
	 */
	public SchedulerException(String message, Throwable cause)
	{
		super(message, cause);
		// TODO Auto-generated constructor stub
	}

}
