package org.iplantc.service.common.exceptions;


public class PermissionException extends Exception {

	private static final long serialVersionUID = 4919073684528860067L;

	public PermissionException() {
		super();
	}

	public PermissionException(String message, Throwable cause) {
		super(message, cause);
	}

	public PermissionException(String message) {
		super(message);
	}

	public PermissionException(Throwable cause) {
		super(cause);
	}

}
