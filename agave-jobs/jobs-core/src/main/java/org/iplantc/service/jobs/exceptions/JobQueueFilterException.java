package org.iplantc.service.jobs.exceptions;

/** A subtype of the general job exception.
 * 
 * @author rcardone
 */
@SuppressWarnings("serial")
public class JobQueueFilterException extends JobQueueException {

	/**
	 * 
	 */
	public JobQueueFilterException() {}

	/**
	 * @param message
	 */
	public JobQueueFilterException(String message)
	{
		super(message);
	}

	/**
	 * @param cause
	 */
	public JobQueueFilterException(Throwable cause)
	{
		super(cause);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public JobQueueFilterException(String message, Throwable cause)
	{
		super(message, cause);
	}

}
