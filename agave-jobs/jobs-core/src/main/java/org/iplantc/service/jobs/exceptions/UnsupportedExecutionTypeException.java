package org.iplantc.service.jobs.exceptions;

public class UnsupportedExecutionTypeException extends Exception {

	private static final long serialVersionUID = 3837807407341345328L;

	public UnsupportedExecutionTypeException() {}

	public UnsupportedExecutionTypeException(String arg0)
	{
		super(arg0);
	}

	public UnsupportedExecutionTypeException(Throwable arg0)
	{
		super(arg0);
	}

	public UnsupportedExecutionTypeException(String arg0, Throwable arg1)
	{
		super(arg0, arg1);
	}

}
