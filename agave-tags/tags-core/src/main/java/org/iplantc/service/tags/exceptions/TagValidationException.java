/**
 * 
 */
package org.iplantc.service.tags.exceptions;

/**
 * @author dooley
 *
 */
public class TagValidationException extends RuntimeException {

    /**
     * 
     */
    private static final long serialVersionUID = -3601590174187150310L;

    /**
     * 
     */
    public TagValidationException() {}

    /**
     * @param arg0
     */
    public TagValidationException(String arg0) {
        super(arg0);
    }

    /**
     * @param arg0
     */
    public TagValidationException(Throwable arg0) {
        super(arg0);
    }

    /**
     * @param arg0
     * @param arg1
     */
    public TagValidationException(String arg0, Throwable arg1) {
        super(arg0, arg1);
    }

}
