package org.iplantc.service.systems.exceptions;

/**
 * Exception to handle situations where no system could be found
 * with the given identifier for a user. This usually happens when
 * a user has had a role removed, the system was retired, or the 
 * system is simply not known.
 * 
 * @author dooley
 *
 */
public class SystemUnknownException extends Exception 
{
	private static final long serialVersionUID = -562960579998632420L;

	public SystemUnknownException() {}

	public SystemUnknownException(String arg0)
	{
		super(arg0);
	}

	public SystemUnknownException(Throwable arg0)
	{
		super(arg0);
	}

	public SystemUnknownException(String arg0, Throwable arg1)
	{
		super(arg0, arg1);
	}

}
