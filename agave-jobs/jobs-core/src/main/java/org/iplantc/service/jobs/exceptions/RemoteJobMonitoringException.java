package org.iplantc.service.jobs.exceptions;

public class RemoteJobMonitoringException extends Exception {

	private static final long serialVersionUID = 1L;

	public RemoteJobMonitoringException() {}

	public RemoteJobMonitoringException(String arg0)
	{
		super(arg0);
	}

	public RemoteJobMonitoringException(Throwable arg0)
	{
		super(arg0);
	}

	public RemoteJobMonitoringException(String arg0, Throwable arg1)
	{
		super(arg0, arg1);
	}

}
