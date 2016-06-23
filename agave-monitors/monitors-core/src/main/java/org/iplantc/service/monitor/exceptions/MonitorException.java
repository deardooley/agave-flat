package org.iplantc.service.monitor.exceptions;

public class MonitorException extends Exception {

	private static final long serialVersionUID = -4674167421857392526L;

	public MonitorException() {}

	public MonitorException(String arg0)
	{
		super(arg0);
	}

	public MonitorException(Throwable arg0)
	{
		super(arg0);
	}

	public MonitorException(String arg0, Throwable arg1)
	{
		super(arg0, arg1);
	}

}
