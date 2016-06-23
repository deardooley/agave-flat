package org.iplantc.service.systems.exceptions;

public class SystemUnavailableException extends Exception {

	private static final long serialVersionUID = 2306884163757216904L;

	public SystemUnavailableException() {}

	public SystemUnavailableException(String arg0)
	{
		super(arg0);
	}

	public SystemUnavailableException(Throwable arg0)
	{
		super(arg0);
	}

	public SystemUnavailableException(String arg0, Throwable arg1)
	{
		super(arg0, arg1);
	}

}
