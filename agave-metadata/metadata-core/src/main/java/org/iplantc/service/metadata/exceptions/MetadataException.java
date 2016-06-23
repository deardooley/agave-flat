/**
 * 
 */
package org.iplantc.service.metadata.exceptions;

/**
 * @author dooley
 * 
 */
@SuppressWarnings("serial")
public class MetadataException extends Exception {

	/**
	 *
	 */
	public MetadataException() {}

	/**
	 * @param message
	 */
	public MetadataException(String message)
	{
		super(message);
	}

	/**
	 * @param cause
	 */
	public MetadataException(Throwable cause)
	{
		super(cause);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public MetadataException(String message, Throwable cause)
	{
		super(message, cause);
	}

}
