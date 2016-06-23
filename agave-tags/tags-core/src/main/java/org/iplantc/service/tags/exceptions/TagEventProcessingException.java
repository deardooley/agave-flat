package org.iplantc.service.tags.exceptions;

public class TagEventProcessingException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8611970170259762629L;

	public TagEventProcessingException() {}

	public TagEventProcessingException(String arg0)
	{
		super(arg0);
	}

	public TagEventProcessingException(Throwable arg0)
	{
		super(arg0);
	}

	public TagEventProcessingException(String arg0, Throwable arg1)
	{
		super(arg0, arg1);
	}

}
