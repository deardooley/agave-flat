/**
 * 
 */
package org.iplantc.service.common.exceptions;

/**
 * @author dooley
 *
 */
public class DomainException extends Exception {

    /**
     * 
     */
    private static final long serialVersionUID = 7097492621531359517L;

    /**
     * 
     */
    public DomainException() {

    }

    /**
     * @param message
     */
    public DomainException(String message) {
        super(message);

    }

    /**
     * @param cause
     */
    public DomainException(Throwable cause) {
        super(cause);

    }

    /**
     * @param message
     * @param cause
     */
    public DomainException(String message, Throwable cause) {
        super(message, cause);

    }

    /**
     * @param message
     * @param cause
     * @param enableSuppression
     * @param writableStackTrace
     */
    public DomainException(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);

    }

}
