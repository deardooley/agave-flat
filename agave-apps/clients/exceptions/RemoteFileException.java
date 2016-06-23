/**
 * 
 */
package org.iplantc.service.clients.exceptions;

/**
 * @author dooley
 *
 */
public class RemoteFileException extends Exception {

	private static final long serialVersionUID = 7476160720918353052L;

	/**
	 * 
	 */
	public RemoteFileException() {}

	/**
	 * @param arg0
	 */
	public RemoteFileException(String arg0)
	{
		super(arg0);
	}

	/**
	 * @param arg0
	 */
	public RemoteFileException(Throwable arg0)
	{
		super(arg0);
	}

	/**
	 * @param arg0
	 * @param arg1
	 */
	public RemoteFileException(String arg0, Throwable arg1)
	{
		super(arg0, arg1);
	}

}
