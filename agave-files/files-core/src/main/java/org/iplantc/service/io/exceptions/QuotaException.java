package org.iplantc.service.io.exceptions;

public class QuotaException extends Exception {

	private static final long serialVersionUID = 2518108761896670982L;

	public QuotaException() {}

	public QuotaException(String message) {
		super(message);
	}

	public QuotaException(Throwable cause) {
		super(cause);
	}

	public QuotaException(String message, Throwable cause) {
		super(message, cause);
	}

}
