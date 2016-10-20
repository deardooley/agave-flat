package org.iplantc.service.jobs.exceptions;

/** A subtype of the general job exception.
 * 
 * @author rcardone
 */
@SuppressWarnings("serial")
public class JobQueuePriorityException extends JobQueueException {

	/**
	 * 
	 */
	public JobQueuePriorityException() {}

	/**
	 * @param message
	 */
	public JobQueuePriorityException(String message)
	{
		super(message);
	}

	/**
	 * @param cause
	 */
	public JobQueuePriorityException(Throwable cause)
	{
		super(cause);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public JobQueuePriorityException(String message, Throwable cause)
	{
		super(message, cause);
	}

}
