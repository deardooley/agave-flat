/**
 * 
 */
package org.iplantc.service.realtime.exceptions;

/**
 * @author dooley
 *
 */
public class RealtimeChannelValidationException extends RuntimeException {

    /**
     * 
     */
    private static final long serialVersionUID = -3601590174187150310L;

    /**
     * 
     */
    public RealtimeChannelValidationException() {}

    /**
     * @param arg0
     */
    public RealtimeChannelValidationException(String arg0) {
        super(arg0);
    }

    /**
     * @param arg0
     */
    public RealtimeChannelValidationException(Throwable arg0) {
        super(arg0);
    }

    /**
     * @param arg0
     * @param arg1
     */
    public RealtimeChannelValidationException(String arg0, Throwable arg1) {
        super(arg0, arg1);
    }

}
