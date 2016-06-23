/**
 * 
 */
package org.iplantc.service.apps.exceptions;

/**
 * @author dooley
 * 
 */
public class SoftwareResourceException extends RuntimeException {

	private static final long	serialVersionUID	= -7845904825208032472L;
	
	public static final int CLIENT_ERROR_FORBIDDEN = 403;
	public static final int CLIENT_ERROR_BAD_REQUEST = 401;
	public static final int CLIENT_ERROR_NOT_FOUND = 404;
	public static final int SERVER_ERROR_INTERNAL = 500;
	
	public static final int SUCCESS_OK = 200;
	
	private int status = 200;
	
	/**
	 * 
	 */
	public SoftwareResourceException()
	{}

	/**
	 * @param arg0
	 */
	public SoftwareResourceException(int status, String arg0)
	{
		super(arg0);
		this.status = status;
	}

	/**
	 * @param arg0
	 */
	public SoftwareResourceException(int status, Throwable arg0)
	{
		super(arg0);
		this.status = status;
	}

	/**
	 * @param arg0
	 * @param arg1
	 */
	public SoftwareResourceException(int status, String arg0, Throwable arg1)
	{
		super(arg0, arg1);
		this.status = status;
	}
	
	/**
	 * @return the status
	 */
	public int getStatus()
	{
		return status;
	}

	/**
	 * @param status the status to set
	 */
	public void setStatus(int status)
	{
		this.status = status;
	}

}
