/**
 * 
 */
package org.iplantc.service.realtime.exceptions;

import org.iplantc.service.common.exceptions.PermissionException;

/**
 * @author dooley
 *
 */
public class RealtimeChannelPermissionException extends PermissionException {

    /**
     * 
     */
    private static final long serialVersionUID = 2163363450498546108L;

    /**
     * 
     */
    public RealtimeChannelPermissionException() {
    }

    /**
     * @param message
     */
    public RealtimeChannelPermissionException(String message) {
        super(message);
    }

    /**
     * @param cause
     */
    public RealtimeChannelPermissionException(Throwable cause) {
        super(cause);
    }

    /**
     * @param message
     * @param cause
     */
    public RealtimeChannelPermissionException(String message, Throwable cause) {
        super(message, cause);
    }

}
