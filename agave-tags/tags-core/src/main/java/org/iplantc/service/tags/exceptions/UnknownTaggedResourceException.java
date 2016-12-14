package org.iplantc.service.tags.exceptions;

public class UnknownTaggedResourceException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7470941640786948222L;

	public UnknownTaggedResourceException() {
	}

	public UnknownTaggedResourceException(String message) {
		super(message);
	}

	public UnknownTaggedResourceException(Throwable cause) {
		super(cause);
	}

	public UnknownTaggedResourceException(String message, Throwable cause) {
		super(message, cause);
	}

	public UnknownTaggedResourceException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
