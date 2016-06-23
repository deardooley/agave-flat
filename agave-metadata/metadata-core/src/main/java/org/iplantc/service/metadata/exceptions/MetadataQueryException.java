package org.iplantc.service.metadata.exceptions;

public class MetadataQueryException extends Exception {

    /**
     * 
     */
    private static final long serialVersionUID = 7704513928164134362L;

    /**
     * 
     */
    public MetadataQueryException() {
        super();
    }

    /**
     * @param message
     * @param cause
     * @param enableSuppression
     * @param writableStackTrace
     */
    public MetadataQueryException(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    /**
     * @param message
     * @param cause
     */
    public MetadataQueryException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * @param message
     */
    public MetadataQueryException(String message) {
        super(message);
    }

    /**
     * @param cause
     */
    public MetadataQueryException(Throwable cause) {
        super(cause);
    }

}
