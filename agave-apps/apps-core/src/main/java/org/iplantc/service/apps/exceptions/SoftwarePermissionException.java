/**
 * 
 */
package org.iplantc.service.apps.exceptions;

/**
 * @author dooley
 * 
 */
public class SoftwarePermissionException extends RuntimeException {

	/**
	 * 
	 */
	private static final long	serialVersionUID	= -3176182359408566107L;
	
	private int status = 403;
	/**
	 * 
	 */
	public SoftwarePermissionException()
	{}

	/**
	 * @param arg0
	 */
	public SoftwarePermissionException(int status, String arg0)
	{
		super(arg0);
		this.status = status;
	}

	/**
	 * @param arg0
	 */
	public SoftwarePermissionException(int status, Throwable arg0)
	{
		super(arg0);
		this.status = status;
	}

	/**
	 * @param arg0
	 * @param arg1
	 */
	public SoftwarePermissionException(int status, String arg0, Throwable arg1)
	{
		super(arg0, arg1);
		this.status = status;
	}

}
