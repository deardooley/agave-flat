/**
 * 
 */
package org.iplantc.service.tags.exceptions;

/**
 * @author dooley
 *
 */
public class TagPermissionException extends Exception {

    /**
     * 
     */
    private static final long serialVersionUID = 2163363450498546108L;

    /**
     * 
     */
    public TagPermissionException() {
    }

    /**
     * @param message
     */
    public TagPermissionException(String message) {
        super(message);
    }

    /**
     * @param cause
     */
    public TagPermissionException(Throwable cause) {
        super(cause);
    }

    /**
     * @param message
     * @param cause
     */
    public TagPermissionException(String message, Throwable cause) {
        super(message, cause);
    }

}
