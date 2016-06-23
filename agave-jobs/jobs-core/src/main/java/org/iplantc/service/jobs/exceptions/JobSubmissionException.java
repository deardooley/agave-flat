package org.iplantc.service.jobs.exceptions;

public class JobSubmissionException extends Exception {

	private static final long serialVersionUID = 1L;

	public JobSubmissionException()
	{
	}

	public JobSubmissionException(String message)
	{
		super(message);
	}

	public JobSubmissionException(Throwable cause)
	{
		super(cause);
	}

	public JobSubmissionException(String message, Throwable cause)
	{
		super(message, cause);
	}

}
