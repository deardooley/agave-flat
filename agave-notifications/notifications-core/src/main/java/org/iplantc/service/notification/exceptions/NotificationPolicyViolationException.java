package org.iplantc.service.notification.exceptions;

public class NotificationPolicyViolationException extends Exception {

	private static final long	serialVersionUID	= -2557764167667782200L;

	public NotificationPolicyViolationException() {}

	public NotificationPolicyViolationException(String arg0)
	{
		super(arg0);
	}

	public NotificationPolicyViolationException(Throwable arg0)
	{
		super(arg0);
	}

	public NotificationPolicyViolationException(String arg0, Throwable arg1)
	{
		super(arg0, arg1);
	}

}
