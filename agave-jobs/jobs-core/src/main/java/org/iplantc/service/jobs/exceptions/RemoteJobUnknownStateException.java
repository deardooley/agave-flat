/**
 * 
 */
package org.iplantc.service.jobs.exceptions;

/**
 * @author dooley
 *
 */
public class RemoteJobUnknownStateException extends RemoteJobUnrecoverableStateException {

	private String jobState;
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -9045807483816053893L;

	/**
	 * 
	 */
	public RemoteJobUnknownStateException() {
	}

	/**
	 * @param message
	 */
	public RemoteJobUnknownStateException(String jobState, String message) {
		super(message);
		setJobState(jobState);
	}

	/**
	 * @param cause
	 */
	public RemoteJobUnknownStateException(String jobState, Throwable cause) {
		super(cause);
		setJobState(jobState);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public RemoteJobUnknownStateException(String jobState, String message, Throwable cause) {
		super(message, cause);
		setJobState(jobState);
	}

	/**
	 * @param message
	 * @param cause
	 * @param enableSuppression
	 * @param writableStackTrace
	 */
	public RemoteJobUnknownStateException(String jobState, String message,
			Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
		setJobState(jobState);
	}

	/**
	 * @return the jobState
	 */
	public String getJobState() {
		return jobState;
	}

	/**
	 * @param jobState the jobState to set
	 */
	public void setJobState(String jobState) {
		this.jobState = jobState;
	}

}
