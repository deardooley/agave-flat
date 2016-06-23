/**
 * 
 */
package org.iplantc.service.metadata.exceptions;

/**
 * @author dooley
 * 
 */
@SuppressWarnings("serial")
public class MetadataStoreException extends Exception {

	/**
	 *
	 */
	public MetadataStoreException() {}

	/**
	 * @param message
	 */
	public MetadataStoreException(String message)
	{
		super(message);
	}

	/**
	 * @param cause
	 */
	public MetadataStoreException(Throwable cause)
	{
		super(cause);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public MetadataStoreException(String message, Throwable cause)
	{
		super(message, cause);
	}

}
