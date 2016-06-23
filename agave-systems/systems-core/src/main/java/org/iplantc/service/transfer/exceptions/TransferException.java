/**
 * 
 */
package org.iplantc.service.transfer.exceptions;

/**
 * @author dooley
 * 
 */
@SuppressWarnings("serial")
public class TransferException extends Exception {

	/**
	 * 
	 */
	public TransferException()
	{
	
	}

	/**
	 * @param message
	 */
	public TransferException(String message)
	{
		super(message);
	}

	/**
	 * @param cause
	 */
	public TransferException(Throwable cause)
	{
		super(cause);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public TransferException(String message, Throwable cause)
	{
		super(message, cause);
	}

}
