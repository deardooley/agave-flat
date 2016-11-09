package org.iplantc.service.common.exceptions;

/**
 * Wrapper for all exceptions coming from notification tasks.
 * 
 * @author Rion Dooley < dooley [at] tacc [dot] utexas [dot] edu >
 *
 */
@SuppressWarnings("serial")
public class NotificationException extends Exception {

    /**
     * 
     */
    public NotificationException() {
        super();
        // TODO Auto-generated constructor stub
    }

    /**
     * @param arg0
     */
    public NotificationException(String arg0) {
        super(arg0);
        // TODO Auto-generated constructor stub
    }

    /**
     * @param arg0
     * @param arg1
     */
    public NotificationException(String arg0, Throwable arg1) {
        super(arg0, arg1);
        // TODO Auto-generated constructor stub
    }

    /**
     * @param arg0
     */
    public NotificationException(Throwable arg0) {
        super(arg0);
        // TODO Auto-generated constructor stub
    }

}
