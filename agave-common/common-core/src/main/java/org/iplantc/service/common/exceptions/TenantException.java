/**
 * 
 */
package org.iplantc.service.common.exceptions;

/**
 * @author dooley
 *
 */
public class TenantException extends Exception {

	private static final long serialVersionUID = 7421111896376555662L;

	public TenantException() {
		super();
	}

	public TenantException(String message, Throwable cause) {
		super(message, cause);
	}

	public TenantException(String message) {
		super(message);
	}

	public TenantException(Throwable cause) {
		super(cause);
	}

}
