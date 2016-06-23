package org.iplantc.service.notification.exceptions;

public class BadCallbackException extends Exception {

	private static final long	serialVersionUID	= -2557764167667782200L;

	public BadCallbackException() {}

	public BadCallbackException(String arg0)
	{
		super(arg0);
	}

	public BadCallbackException(Throwable arg0)
	{
		super(arg0);
	}

	public BadCallbackException(String arg0, Throwable arg1)
	{
		super(arg0, arg1);
	}

}
