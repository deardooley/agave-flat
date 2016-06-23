package org.iplantc.service.notification.exceptions;

public class NotificationException extends Exception {

	private static final long serialVersionUID = -4674167421857392526L;

	public NotificationException() {}

	public NotificationException(String arg0)
	{
		super(arg0);
	}

	public NotificationException(Throwable arg0)
	{
		super(arg0);
	}

	public NotificationException(String arg0, Throwable arg1)
	{
		super(arg0, arg1);
	}

}
