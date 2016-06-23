package org.iplantc.service.profile.exceptions;

public class RemoteDataException extends Exception {

	private static final long serialVersionUID = -502884827723683499L;

	public RemoteDataException()
	{
		super();
	}

	public RemoteDataException(String arg0, Throwable arg1)
	{
		super(arg0, arg1);
	}

	public RemoteDataException(String arg0)
	{
		super(arg0);
	}

	public RemoteDataException(Throwable arg0)
	{
		super(arg0);
	}

}
