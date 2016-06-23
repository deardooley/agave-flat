/**
 * 
 */
package org.iplantc.service.tags.exceptions;

/**
 * @author dooley
 *
 */
public class PermissionValidationException extends RuntimeException {

    private static final long serialVersionUID = 302611659048064543L;

    /**
     * 
     */
    public PermissionValidationException() {
    }

    /**
     * @param message
     */
    public PermissionValidationException(String message) {
        super(message);
    }

    /**
     * @param cause
     */
    public PermissionValidationException(Throwable cause) {
        super(cause);
    }

    /**
     * @param message
     * @param cause
     */
    public PermissionValidationException(String message, Throwable cause) {
        super(message, cause);
    }

}
