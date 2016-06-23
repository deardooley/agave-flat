package org.iplantc.service.notification.exceptions;

public class NotificationPolicyException extends Exception {

	private static final long serialVersionUID = -4674167421857392526L;

	public NotificationPolicyException() {}

	public NotificationPolicyException(String arg0)
	{
		super(arg0);
	}

	public NotificationPolicyException(Throwable arg0)
	{
		super(arg0);
	}

	public NotificationPolicyException(String arg0, Throwable arg1)
	{
		super(arg0, arg1);
	}

}
