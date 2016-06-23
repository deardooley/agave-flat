package org.iplantc.service.common.exceptions;

public class MessagingException extends Exception {

	private static final long serialVersionUID = -6616835300249564998L;

	public MessagingException()
	{
		super();
	}

	public MessagingException(String arg0, Throwable arg1)
	{
		super(arg0, arg1);
	}

	public MessagingException(String arg0)
	{
		super(arg0);
	}

	public MessagingException(Throwable arg0)
	{
		super(arg0);
	}

}
