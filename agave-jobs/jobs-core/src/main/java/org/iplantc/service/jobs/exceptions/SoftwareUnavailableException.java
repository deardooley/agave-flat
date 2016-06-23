/**
 * 
 */
package org.iplantc.service.jobs.exceptions;

/**
 * @author dooley
 *
 */
public class SoftwareUnavailableException extends Exception {

    /**
     * 
     */
    private static final long serialVersionUID = 2619858642194995284L;

    /**
     * 
     */
    public SoftwareUnavailableException() {
        super();
        
    }

    /**
     * @param message
     * @param cause
     * @param enableSuppression
     * @param writableStackTrace
     */
    public SoftwareUnavailableException(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
        
    }

    /**
     * @param message
     * @param cause
     */
    public SoftwareUnavailableException(String message, Throwable cause) {
        super(message, cause);
        
    }

    /**
     * @param message
     */
    public SoftwareUnavailableException(String message) {
        super(message);
        
    }

    /**
     * @param cause
     */
    public SoftwareUnavailableException(Throwable cause) {
        super(cause);
        
    }

}
