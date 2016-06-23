/**
 * 
 */
package org.iplantc.service.apps.exceptions;

/**
 * @author dooley
 * 
 */
public class RuntimeSecurityManagementException extends RuntimeException {

	private static final long serialVersionUID = -6285529079611811561L;

	public RuntimeSecurityManagementException() {}

	/**
	 * @param message
	 */
	public RuntimeSecurityManagementException(String message)
	{
		super(message);
	}

	/**
	 * @param cause
	 */
	public RuntimeSecurityManagementException(Throwable cause)
	{
		super(cause);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public RuntimeSecurityManagementException(String message, Throwable cause)
	{
		super(message, cause);
	}

}
