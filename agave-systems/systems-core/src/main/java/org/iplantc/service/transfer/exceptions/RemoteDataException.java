/**
 * 
 */
package org.iplantc.service.transfer.exceptions;

/**
 * @author dooley
 * 
 */
public class RemoteDataException extends Exception {

	private static final long serialVersionUID = 1869381745825081111L;

	/**
	 * 
	 */
	public RemoteDataException() {}

	/**
	 * @param arg0
	 */
	public RemoteDataException(String arg0)
	{
		super(arg0);
	}

	/**
	 * @param arg0
	 */
	public RemoteDataException(Throwable arg0)
	{
		super(arg0);
	}

	/**
	 * @param arg0
	 * @param arg1
	 */
	public RemoteDataException(String arg0, Throwable arg1)
	{
		super(arg0, arg1);
	}

}
