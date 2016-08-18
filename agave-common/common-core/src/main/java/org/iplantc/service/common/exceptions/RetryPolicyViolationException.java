package org.iplantc.service.common.exceptions;

public class RetryPolicyViolationException extends Exception {

	private static final long	serialVersionUID	= -2557764167667782200L;

	public RetryPolicyViolationException() {}

	public RetryPolicyViolationException(String arg0)
	{
		super(arg0);
	}

	public RetryPolicyViolationException(Throwable arg0)
	{
		super(arg0);
	}

	public RetryPolicyViolationException(String arg0, Throwable arg1)
	{
		super(arg0, arg1);
	}

}
