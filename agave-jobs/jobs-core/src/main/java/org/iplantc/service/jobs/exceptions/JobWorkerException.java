package org.iplantc.service.jobs.exceptions;

/** A subtype of the general job exception.
 * 
 * @author rcardone
 */
@SuppressWarnings("serial")
public class JobWorkerException extends JobException {

	/**
	 * 
	 */
	public JobWorkerException() {}

	/**
	 * @param message
	 */
	public JobWorkerException(String message)
	{
		super(message);
	}

	/**
	 * @param cause
	 */
	public JobWorkerException(Throwable cause)
	{
		super(cause);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public JobWorkerException(String message, Throwable cause)
	{
		super(message, cause);
	}

}
