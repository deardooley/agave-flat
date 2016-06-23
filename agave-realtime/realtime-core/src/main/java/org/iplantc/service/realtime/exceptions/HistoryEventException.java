/**
 * 
 */
package org.iplantc.service.realtime.exceptions;

/**
 * @author dooley
 *
 */
public class HistoryEventException extends Exception {

    /**
     * 
     */
    private static final long serialVersionUID = -3263256692345468659L;

    /**
     * 
     */
    public HistoryEventException() {
    }

    /**
     * @param arg0
     */
    public HistoryEventException(String arg0) {
        super(arg0);
    }

    /**
     * @param arg0
     */
    public HistoryEventException(Throwable arg0) {
        super(arg0);
    }

    /**
     * @param arg0
     * @param arg1
     */
    public HistoryEventException(String arg0, Throwable arg1) {
        super(arg0, arg1);
    }

}
