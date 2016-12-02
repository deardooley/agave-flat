package org.iplantc.service.jobs.exceptions;

/** A subtype of the general job exception.
 * 
 * @author rcardone
 */
@SuppressWarnings("serial")
public class JobFinishedException extends JobException {

	/**
	 * 
	 */
	public JobFinishedException() {}

	/**
	 * @param message
	 */
	public JobFinishedException(String message)
	{
		super(message);
	}

	/**
	 * @param cause
	 */
	public JobFinishedException(Throwable cause)
	{
		super(cause);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public JobFinishedException(String message, Throwable cause)
	{
		super(message, cause);
	}

}
