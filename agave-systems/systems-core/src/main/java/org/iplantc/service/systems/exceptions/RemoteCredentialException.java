/**
 * 
 */
package org.iplantc.service.systems.exceptions;

/**
 * @author dooley
 * 
 */
public class RemoteCredentialException extends Exception {

	private static final long	serialVersionUID	= -4037349129182589687L;

	/**
	 * 
	 */
	public RemoteCredentialException()
	{
	}

	/**
	 * @param message
	 */
	public RemoteCredentialException(String message)
	{
		super(message);
	}

	/**
	 * @param cause
	 */
	public RemoteCredentialException(Throwable cause)
	{
		super(cause);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public RemoteCredentialException(String message, Throwable cause)
	{
		super(message, cause);
	}

}
