/**
 * 
 */
package org.iplantc.service.common.exceptions;

/**
 * Catchall for dependency exceptions during action processing
 * 
 * @author dooley
 *
 */
public class DependencyException extends Exception {

    /**
     * 
     */
    private static final long serialVersionUID = 1023722142554133111L;

    /**
     * 
     */
    public DependencyException() {

    }

    /**
     * @param message
     */
    public DependencyException(String message) {
        super(message);

    }

    /**
     * @param cause
     */
    public DependencyException(Throwable cause) {
        super(cause);

    }

    /**
     * @param message
     * @param cause
     */
    public DependencyException(String message, Throwable cause) {
        super(message, cause);

    }

    /**
     * @param message
     * @param cause
     * @param enableSuppression
     * @param writableStackTrace
     */
    public DependencyException(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);

    }

}
