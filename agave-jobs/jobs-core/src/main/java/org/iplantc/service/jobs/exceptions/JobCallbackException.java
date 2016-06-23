package org.iplantc.service.jobs.exceptions;

public class JobCallbackException extends Exception {

    private static final long serialVersionUID = 7051317291338381944L;

    public JobCallbackException()
	{
	}

	public JobCallbackException(String message)
	{
		super(message);
	}

	public JobCallbackException(Throwable cause)
	{
		super(cause);
	}

	public JobCallbackException(String message, Throwable cause)
	{
		super(message, cause);
	}

}
