package org.iplantc.service.uuid.exceptions;

public class UUIDResolutionException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4742663831129464555L;

	/**
	 * 
	 */
	public UUIDResolutionException() {
		super();
	}

	/**
	 * @param message
	 * @param cause
	 * @param enableSuppression
	 * @param writableStackTrace
	 */
	public UUIDResolutionException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public UUIDResolutionException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * @param message
	 */
	public UUIDResolutionException(String message) {
		super(message);
	}

	/**
	 * @param cause
	 */
	public UUIDResolutionException(Throwable cause) {
		super(cause);
	}

}
