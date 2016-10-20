package org.iplantc.service.jobs.exceptions;

public class RemoteJobMonitorResponseParsingException extends Exception {

	private static final long serialVersionUID = 5847230922629188093L;

	public RemoteJobMonitorResponseParsingException()
	{
	}

	public RemoteJobMonitorResponseParsingException(String message)
	{
		super(message);
	}

	public RemoteJobMonitorResponseParsingException(Throwable cause)
	{
		super(cause);
	}

	public RemoteJobMonitorResponseParsingException(String message, Throwable cause)
	{
		super(message, cause);
	}

}
