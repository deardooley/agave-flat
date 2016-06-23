package org.iplantc.service.jobs.exceptions;

public class MissingDataException extends Exception {

	private static final long serialVersionUID = -2112183333631414096L;

	public MissingDataException() {}

	public MissingDataException(String message)
	{
		super(message);
	}

	public MissingDataException(Throwable cause)
	{
		super(cause);
	}

	public MissingDataException(String message, Throwable cause)
	{
		super(message, cause);
	}

}
