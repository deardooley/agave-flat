/**
 * 
 */
package org.iplantc.service.clients.exceptions;

/**
 * @author dooley
 *
 */
public class IOServiceException extends Exception {

	private static final long serialVersionUID = 8062334002283162390L;

	/**
	 * 
	 */
	public IOServiceException() {}

	/**
	 * @param arg0
	 */
	public IOServiceException(String arg0)
	{
		super(arg0);
	}

	/**
	 * @param arg0
	 */
	public IOServiceException(Throwable arg0)
	{
		super(arg0);
	}

	/**
	 * @param arg0
	 * @param arg1
	 */
	public IOServiceException(String arg0, Throwable arg1)
	{
		super(arg0, arg1);
	}

}
