package org.iplantc.service.io.exceptions;

public class LogicalFileException extends Exception {

	private static final long serialVersionUID = 321181863976056616L;

	public LogicalFileException() {}

	public LogicalFileException(String arg0)
	{
		super(arg0);
	}

	public LogicalFileException(Throwable arg0)
	{
		super(arg0);
	}

	public LogicalFileException(String arg0, Throwable arg1)
	{
		super(arg0, arg1);
	}

}
