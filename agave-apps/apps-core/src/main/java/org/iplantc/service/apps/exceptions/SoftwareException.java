/**
 * 
 */
package org.iplantc.service.apps.exceptions;

/**
 * @author dooley
 * 
 */
@SuppressWarnings("serial")
public class SoftwareException extends RuntimeException {

	/**
	 * 
	 */
	public SoftwareException()
	{}

	/**
	 * @param arg0
	 */
	public SoftwareException(String arg0)
	{
		super(arg0);
	}

	/**
	 * @param arg0
	 */
	public SoftwareException(Throwable arg0)
	{
		super(arg0);
	}

	/**
	 * @param arg0
	 * @param arg1
	 */
	public SoftwareException(String arg0, Throwable arg1)
	{
		super(arg0, arg1);
	}

}
