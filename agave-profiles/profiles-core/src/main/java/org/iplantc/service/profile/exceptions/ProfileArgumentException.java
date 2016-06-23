/**
 * 
 */
package org.iplantc.service.profile.exceptions;

/**
 * @author dooley
 *
 */
public class ProfileArgumentException extends Exception {

	private static final long serialVersionUID = -4400771337818936770L;

	/**
	 * 
	 */
	public ProfileArgumentException() {}

	/**
	 * @param arg0
	 */
	public ProfileArgumentException(String arg0)
	{
		super(arg0);
	}

	/**
	 * @param arg0
	 */
	public ProfileArgumentException(Throwable arg0)
	{
		super(arg0);
	}

	/**
	 * @param arg0
	 * @param arg1
	 */
	public ProfileArgumentException(String arg0, Throwable arg1)
	{
		super(arg0, arg1);
	}

}
