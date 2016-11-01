package org.iplantc.service.jobs.exceptions;

/** A subtype of the general job exception.
 * 
 * @author rcardone
 */
@SuppressWarnings("serial")
public class JobSchedulerException extends JobException {

	/**
	 * 
	 */
	public JobSchedulerException() {}

	/**
	 * @param message
	 */
	public JobSchedulerException(String message)
	{
		super(message);
	}

	/**
	 * @param cause
	 */
	public JobSchedulerException(Throwable cause)
	{
		super(cause);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public JobSchedulerException(String message, Throwable cause)
	{
		super(message, cause);
	}

}
