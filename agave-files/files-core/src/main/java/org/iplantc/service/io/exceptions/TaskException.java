package org.iplantc.service.io.exceptions;

public class TaskException extends RuntimeException {

	private static final long serialVersionUID = -7137156848996590796L;

	public TaskException() {
		super();
	}

	public TaskException(String message, Throwable cause) {
		super(message, cause);
	}

	public TaskException(String message) {
		super(message);
	}

	public TaskException(Throwable cause) {
		super(cause);
	}

}
