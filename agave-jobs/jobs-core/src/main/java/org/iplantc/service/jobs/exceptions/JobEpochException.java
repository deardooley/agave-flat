package org.iplantc.service.jobs.exceptions;

/** A subtype of the general job exception.
 * 
 * @author rcardone
 */
@SuppressWarnings("serial")
public class JobEpochException extends JobException {

	/**
	 * 
	 */
	public JobEpochException() {}

	/**
	 * @param message
	 */
	public JobEpochException(String message)
	{
		super(message);
	}

	/**
	 * @param cause
	 */
	public JobEpochException(Throwable cause)
	{
		super(cause);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public JobEpochException(String message, Throwable cause)
	{
		super(message, cause);
	}

}
