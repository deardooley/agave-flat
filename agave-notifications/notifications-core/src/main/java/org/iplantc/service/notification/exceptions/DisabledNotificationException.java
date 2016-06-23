package org.iplantc.service.notification.exceptions;

public class DisabledNotificationException extends Exception {
	private static final long serialVersionUID = 2783796963049998315L;

	/**
	 * 
	 */
	public DisabledNotificationException() {
		super();
	}

	/**
	 * @param message
	 * @param cause
	 * @param enableSuppression
	 * @param writableStackTrace
	 */
	public DisabledNotificationException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public DisabledNotificationException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * @param message
	 */
	public DisabledNotificationException(String message) {
		super(message);
	}

	/**
	 * @param cause
	 */
	public DisabledNotificationException(Throwable cause) {
		super(cause);
	}
}
