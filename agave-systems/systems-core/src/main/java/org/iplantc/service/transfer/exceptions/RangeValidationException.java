/**
 * 
 */
package org.iplantc.service.transfer.exceptions;

/**
 * @author dooley
 *
 */
public class RangeValidationException extends Exception {
    
    private static final long serialVersionUID = 6673552706214894826L;

    /**
     * 
     */
    public RangeValidationException() {
        
    }

    /**
     * @param message
     */
    public RangeValidationException(String message) {
        super(message);
        
    }

    /**
     * @param cause
     */
    public RangeValidationException(Throwable cause) {
        super(cause);
        
    }

    /**
     * @param message
     * @param cause
     */
    public RangeValidationException(String message, Throwable cause) {
        super(message, cause);
     
    }

    /**
     * @param message
     * @param cause
     * @param enableSuppression
     * @param writableStackTrace
     */
    public RangeValidationException(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
     
    }

}
