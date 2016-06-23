/**
 * 
 */
package org.iplantc.service.monitor.exceptions;

/**
 * @author dooley
 *
 */
public class MonitorEventProcessingException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3249365405469625839L;

	/**
	 * 
	 */
	public MonitorEventProcessingException() {
	}

	/**
	 * @param paramString
	 */
	public MonitorEventProcessingException(String paramString) {
		super(paramString);
	}

	/**
	 * @param paramThrowable
	 */
	public MonitorEventProcessingException(Throwable paramThrowable) {
		super(paramThrowable);
	}

	/**
	 * @param paramString
	 * @param paramThrowable
	 */
	public MonitorEventProcessingException(String paramString,
			Throwable paramThrowable) {
		super(paramString, paramThrowable);
	}

	/**
	 * @param paramString
	 * @param paramThrowable
	 * @param paramBoolean1
	 * @param paramBoolean2
	 */
	public MonitorEventProcessingException(String paramString,
			Throwable paramThrowable, boolean paramBoolean1,
			boolean paramBoolean2) {
		super(paramString, paramThrowable, paramBoolean1, paramBoolean2);
	}

}
