package org.iplantc.service.jobs.exceptions;

public class RemoteJobIDParsingException extends Exception {

	private static final long serialVersionUID = 5847230922629188093L;

	public RemoteJobIDParsingException()
	{
	}

	public RemoteJobIDParsingException(String message)
	{
		super(message);
	}

	public RemoteJobIDParsingException(Throwable cause)
	{
		super(cause);
	}

	public RemoteJobIDParsingException(String message, Throwable cause)
	{
		super(message, cause);
	}

}
