/**
 * 
 */
package org.iplantc.service.common.exceptions;

/**
 * @author dooley
 *
 */
public class ServiceDiscoveryException extends Exception
{

	/**
	 * 
	 */
	private static final long	serialVersionUID	= 7030718256798843788L;

	/**
	 * 
	 */
	public ServiceDiscoveryException()
	{
	}

	/**
	 * @param message
	 */
	public ServiceDiscoveryException(String message)
	{
		super(message);
	}

	/**
	 * @param cause
	 */
	public ServiceDiscoveryException(Throwable cause)
	{
		super(cause);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public ServiceDiscoveryException(String message, Throwable cause)
	{
		super(message, cause);
	}

	/**
	 * @param message
	 * @param cause
	 * @param enableSuppression
	 * @param writableStackTrace
	 */
	public ServiceDiscoveryException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace)
	{
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
