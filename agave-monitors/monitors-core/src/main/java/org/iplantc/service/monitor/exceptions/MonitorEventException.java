/**
 * 
 */
package org.iplantc.service.monitor.exceptions;

/**
 * @author dooley
 *
 */
public class MonitorEventException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3597807604303028543L;

	/**
	 * 
	 */
	public MonitorEventException() {}

	/**
	 * @param paramString
	 */
	public MonitorEventException(String paramString) {
		super(paramString);
	}

	/**
	 * @param paramThrowable
	 */
	public MonitorEventException(Throwable paramThrowable) {
		super(paramThrowable);
	}

	/**
	 * @param paramString
	 * @param paramThrowable
	 */
	public MonitorEventException(String paramString, Throwable paramThrowable) {
		super(paramString, paramThrowable);
	}

	/**
	 * @param paramString
	 * @param paramThrowable
	 * @param paramBoolean1
	 * @param paramBoolean2
	 */
	public MonitorEventException(String paramString, Throwable paramThrowable,
			boolean paramBoolean1, boolean paramBoolean2) {
		super(paramString, paramThrowable, paramBoolean1, paramBoolean2);
	}

}
