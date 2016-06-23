/**
 * 
 */
package org.iplantc.service.profile.exceptions;

/**
 * @author dooley
 *
 */
public class InternalUsernameConflictException extends Exception {

	private static final long serialVersionUID = 2227843654191540854L;

	/**
	 * 
	 */
	public InternalUsernameConflictException() {}

	/**
	 * @param arg0
	 */
	public InternalUsernameConflictException(String arg0)
	{
		super(arg0);
	}

	/**
	 * @param arg0
	 */
	public InternalUsernameConflictException(Throwable arg0)
	{
		super(arg0);
	}

	/**
	 * @param arg0
	 * @param arg1
	 */
	public InternalUsernameConflictException(String arg0, Throwable arg1)
	{
		super(arg0, arg1);
	}

}
