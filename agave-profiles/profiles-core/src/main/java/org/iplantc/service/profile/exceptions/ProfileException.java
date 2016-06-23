/**
 * 
 */
package org.iplantc.service.profile.exceptions;

/**
 * @author dooley
 *
 */
public class ProfileException extends Exception {

	private static final long serialVersionUID = 7867455125903191068L;

	/**
	 * 
	 */
	public ProfileException() {}

	/**
	 * @param arg0
	 */
	public ProfileException(String arg0)
	{
		super(arg0);
	}

	/**
	 * @param arg0
	 */
	public ProfileException(Throwable arg0)
	{
		super(arg0);
	}

	/**
	 * @param arg0
	 * @param arg1
	 */
	public ProfileException(String arg0, Throwable arg1)
	{
		super(arg0, arg1);
	}

}
