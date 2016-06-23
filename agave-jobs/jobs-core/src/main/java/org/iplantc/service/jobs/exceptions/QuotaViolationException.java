package org.iplantc.service.jobs.exceptions;

public class QuotaViolationException extends Exception {

	private static final long serialVersionUID = 6743021909388152813L;

	public QuotaViolationException()
	{
	}

	public QuotaViolationException(String message)
	{
		super(message);
	}

	public QuotaViolationException(Throwable cause)
	{
		super(cause);
	}

	public QuotaViolationException(String message, Throwable cause)
	{
		super(message, cause);
	}

}
