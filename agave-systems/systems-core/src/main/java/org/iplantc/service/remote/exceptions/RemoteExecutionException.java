/**
 * 
 */
package org.iplantc.service.remote.exceptions;

/**
 * @author dooley
 * 
 */
public class RemoteExecutionException extends Exception {

	private static final long serialVersionUID = -4302182103043924535L;

	/**
	 * 
	 */
	public RemoteExecutionException() {}

	/**
	 * @param arg0
	 */
	public RemoteExecutionException(String arg0)
	{
		super(arg0);
	}

	/**
	 * @param arg0
	 */
	public RemoteExecutionException(Throwable arg0)
	{
		super(arg0);
	}

	/**
	 * @param arg0
	 * @param arg1
	 */
	public RemoteExecutionException(String arg0, Throwable arg1)
	{
		super(arg0, arg1);
	}

}
