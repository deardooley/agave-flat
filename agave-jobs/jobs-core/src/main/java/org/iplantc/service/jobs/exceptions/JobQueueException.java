package org.iplantc.service.jobs.exceptions;

/** A subtype of the general job exception.
 * 
 * @author rcardone
 */
@SuppressWarnings("serial")
public class JobQueueException extends JobException {

	/**
	 * 
	 */
	public JobQueueException() {}

	/**
	 * @param message
	 */
	public JobQueueException(String message)
	{
		super(message);
	}

	/**
	 * @param cause
	 */
	public JobQueueException(Throwable cause)
	{
		super(cause);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public JobQueueException(String message, Throwable cause)
	{
		super(message, cause);
	}

}
