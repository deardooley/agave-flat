package org.iplantc.service.tags.exceptions;

public class TagException extends Exception {

	private static final long serialVersionUID = -4674167421857392526L;

	public TagException() {}

	public TagException(String arg0)
	{
		super(arg0);
	}

	public TagException(Throwable arg0)
	{
		super(arg0);
	}

	public TagException(String arg0, Throwable arg1)
	{
		super(arg0, arg1);
	}

}
