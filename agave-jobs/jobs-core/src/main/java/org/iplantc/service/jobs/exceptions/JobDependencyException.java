/**
 * 
 */
package org.iplantc.service.jobs.exceptions;

/**
 * @author dooley
 *
 */
public class JobDependencyException extends Exception {

	private static final long serialVersionUID = -6732369267361539581L;

	public JobDependencyException() {}

	/**
	 * @param arg0
	 */
	public JobDependencyException(String arg0)
	{
		super(arg0);
	}

	/**
	 * @param arg0
	 */
	public JobDependencyException(Throwable arg0)
	{
		super(arg0);
	}

	/**
	 * @param arg0
	 * @param arg1
	 */
	public JobDependencyException(String arg0, Throwable arg1)
	{
		super(arg0, arg1);
	}

}
