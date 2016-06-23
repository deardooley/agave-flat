package org.iplantc.service.io.exceptions;

public class RemoteCopyException extends Exception {

	private static final long serialVersionUID = -7369674867770072948L;

	public RemoteCopyException() {}

	public RemoteCopyException(String message) {
		super(message);
	}

	public RemoteCopyException(Throwable cause) {
		super(cause);
	}

	public RemoteCopyException(String message, Throwable cause) {
		super(message, cause);
	}

}
