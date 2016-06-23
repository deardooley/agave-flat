/**
 * 
 */
package org.iplantc.service.transfer.exceptions;

/**
 * @author dooley
 * 
 */
public class AuthenticationException extends Exception 
{
	private static final long serialVersionUID = -6027612700138570638L;

	/**
	 * 
	 */
	public AuthenticationException() {}

	/**
	 * @param arg0
	 */
	public AuthenticationException(String arg0)
	{
		super(arg0);
	}

	/**
	 * @param arg0
	 */
	public AuthenticationException(Throwable arg0)
	{
		super(arg0);
	}

	/**
	 * @param arg0
	 * @param arg1
	 */
	public AuthenticationException(String arg0, Throwable arg1)
	{
		super(arg0, arg1);
	}

}
