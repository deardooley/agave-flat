/**
 * 
 */
package org.iplantc.service.jobs.exceptions;

/**
 * @author dooley
 *
 */
public class MissingSoftwareDependencyException extends Exception {

    /**
     * 
     */
    private static final long serialVersionUID = 8484109887237732683L;

    /**
     * 
     */
    public MissingSoftwareDependencyException() {
        
    }

    /**
     * @param message
     */
    public MissingSoftwareDependencyException(String message) {
        super(message);
        
    }

    /**
     * @param cause
     */
    public MissingSoftwareDependencyException(Throwable cause) {
        super(cause);
        
    }

    /**
     * @param message
     * @param cause
     */
    public MissingSoftwareDependencyException(String message, Throwable cause) {
        super(message, cause);
        
    }

    /**
     * @param message
     * @param cause
     * @param enableSuppression
     * @param writableStackTrace
     */
    public MissingSoftwareDependencyException(String message, Throwable cause,
            boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
        
    }

}
