package org.iplantc.service.metadata.exceptions;

public class MetadataAssociationException extends Exception {

    /**
     * 
     */
    private static final long serialVersionUID = -2716485130024492231L;

    /**
     * 
     */
    public MetadataAssociationException() {
        super();
    }

    /**
     * @param message
     * @param cause
     * @param enableSuppression
     * @param writableStackTrace
     */
    public MetadataAssociationException(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    /**
     * @param message
     * @param cause
     */
    public MetadataAssociationException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * @param message
     */
    public MetadataAssociationException(String message) {
        super(message);
    }

    /**
     * @param cause
     */
    public MetadataAssociationException(Throwable cause) {
        super(cause);
    }

}