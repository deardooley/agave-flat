package org.iplantc.service.clients.exceptions;

public class APIClientException extends Exception {

	private static final long serialVersionUID = -592576201522437386L;

	public APIClientException()
	{
		super();
	}

	public APIClientException(String arg0, Throwable arg1)
	{
		super(arg0, arg1);
	}

	public APIClientException(String arg0)
	{
		super(arg0);
	}

	public APIClientException(Throwable arg0)
	{
		super(arg0);
	}

}
