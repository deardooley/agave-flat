/**
 * 
 */
package org.iplantc.service.systems.exceptions;

/**
 * @author dooley
 * 
 */
@SuppressWarnings("serial")
public class EncryptionException extends Exception {

	/**
	 * 
	 */
	public EncryptionException()
	{
	}

	/**
	 * @param message
	 */
	public EncryptionException(String message)
	{
		super(message);
	}

	/**
	 * @param cause
	 */
	public EncryptionException(Throwable cause)
	{
		super(cause);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public EncryptionException(String message, Throwable cause)
	{
		super(message, cause);
	}

}
