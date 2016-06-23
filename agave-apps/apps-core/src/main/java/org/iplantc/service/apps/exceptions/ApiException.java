package org.iplantc.service.apps.exceptions;

public class ApiException extends Exception {

	private static final long serialVersionUID = -3526649859568618569L;

	public ApiException() {}
	
	public ApiException(String arg0)
	{
		super(arg0);
	}

	public ApiException(Throwable arg0)
	{
		super(arg0);
	}

	public ApiException(String arg0, Throwable arg1)
	{
		super(arg0, arg1);
	}

}
