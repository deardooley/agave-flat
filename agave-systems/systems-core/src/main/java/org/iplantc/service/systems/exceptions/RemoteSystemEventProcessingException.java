package org.iplantc.service.systems.exceptions;

public class RemoteSystemEventProcessingException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8179958799992940235L;

	public RemoteSystemEventProcessingException() {}

	public RemoteSystemEventProcessingException(String arg0)
	{
		super(arg0);
	}

	public RemoteSystemEventProcessingException(Throwable arg0)
	{
		super(arg0);
	}

	public RemoteSystemEventProcessingException(String arg0, Throwable arg1)
	{
		super(arg0, arg1);
	}

}
