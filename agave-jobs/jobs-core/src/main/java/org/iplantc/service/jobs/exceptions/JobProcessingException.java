/**
 * 
 */
package org.iplantc.service.jobs.exceptions;

/**
 * @author dooley
 *
 */
public class JobProcessingException extends Exception {

	private static final long serialVersionUID = 184077193335396939L;

	private int status = 500;
	
	/**
	 * @param arg0
	 */
	public JobProcessingException(int statusCode, String arg0)
	{
		super(arg0);
		this.status = statusCode;
	}

	/**
	 * @param arg0
	 */
	public JobProcessingException(int statusCode, Throwable arg0)
	{
		super(arg0);
		this.status = statusCode;
	}

	/**
	 * @param arg0
	 * @param arg1
	 */
	public JobProcessingException(int statusCode, String arg0, Throwable arg1)
	{
		super(arg0, arg1);
		this.status = statusCode;
	}

	public int getStatus()
	{
		return status;
	}

}
