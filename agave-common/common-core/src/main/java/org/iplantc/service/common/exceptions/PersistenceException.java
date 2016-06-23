package org.iplantc.service.common.exceptions;

public class PersistenceException extends RuntimeException {

	private static final long serialVersionUID = -3104555115206622876L;

	public PersistenceException() {
		super();
	}

	public PersistenceException(String message, Throwable cause) {
		super(message, cause);
	}

	public PersistenceException(String message) {
		super(message);
	}

	public PersistenceException(Throwable cause) {
		super(cause);
	}

}
