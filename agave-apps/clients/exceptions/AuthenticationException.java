package org.iplantc.service.clients.exceptions;

public class AuthenticationException extends Exception {

	private static final long serialVersionUID = 5209949948949682574L;

	public AuthenticationException() {}

	public AuthenticationException(String arg0)
	{
		super(arg0);
	}

	public AuthenticationException(Throwable arg0)
	{
		super(arg0);
	}

	public AuthenticationException(String arg0, Throwable arg1)
	{
		super(arg0, arg1);
	}

}
