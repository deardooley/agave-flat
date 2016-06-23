/**
 * 
 */
package org.iplantc.service.common.exceptions;

/**
 * Catchall for scheduling related exceptions.
 * 
 * @author dooley
 *
 */
public class TaskSchedulerException extends Exception {

    /**
     * 
     */
    private static final long serialVersionUID = 5372279019505989712L;

    /**
     * 
     */
    public TaskSchedulerException() {
        super();

    }

    /**
     * @param message
     * @param cause
     * @param enableSuppression
     * @param writableStackTrace
     */
    public TaskSchedulerException(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);

    }

    /**
     * @param message
     * @param cause
     */
    public TaskSchedulerException(String message, Throwable cause) {
        super(message, cause);

    }

    /**
     * @param message
     */
    public TaskSchedulerException(String message) {
        super(message);

    }

    /**
     * @param cause
     */
    public TaskSchedulerException(Throwable cause) {
        super(cause);

    }

}
