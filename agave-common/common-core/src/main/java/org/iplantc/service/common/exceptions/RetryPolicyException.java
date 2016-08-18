package org.iplantc.service.common.exceptions;

public class RetryPolicyException extends Exception {

	private static final long serialVersionUID = -4674167421857392526L;

	public RetryPolicyException() {}

	public RetryPolicyException(String arg0)
	{
		super(arg0);
	}

	public RetryPolicyException(Throwable arg0)
	{
		super(arg0);
	}

	public RetryPolicyException(String arg0, Throwable arg1)
	{
		super(arg0, arg1);
	}

}
