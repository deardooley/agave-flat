/**
 * 
 */
package org.iplantc.service.apps.exceptions;

/**
 * @author dooley
 * 
 */
@SuppressWarnings("serial")
public class UnknownSoftwareException extends Exception {

	/**
	 * 
	 */
	public UnknownSoftwareException()
	{}

	/**
	 * @param arg0
	 */
	public UnknownSoftwareException(String arg0)
	{
		super(arg0);
	}

	/**
	 * @param arg0
	 */
	public UnknownSoftwareException(Throwable arg0)
	{
		super(arg0);
	}

	/**
	 * @param arg0
	 * @param arg1
	 */
	public UnknownSoftwareException(String arg0, Throwable arg1)
	{
		super(arg0, arg1);
	}

}
