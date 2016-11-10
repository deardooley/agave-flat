/**
 * 
 */
package org.iplantc.service.metadata.exceptions;

import java.util.Set;

import javax.validation.ConstraintViolation;

import org.iplantc.service.metadata.model.MetadataItem;

/**
 * @author dooley
 * 
 */
@SuppressWarnings("serial")
public class MetadataSchemaValidationException extends Exception {

    private Set<ConstraintViolation<MetadataItem>> violations;
    
	/**
	 *
	 */
	public MetadataSchemaValidationException() {}
	
	/**
	 * Constructor adding the constraint violations.
	 * @param violations
	 */
	public MetadataSchemaValidationException(final Set<ConstraintViolation<MetadataItem>> violations) {
	    this.violations = violations;
	}

	/**
	 * @param message
	 */
	public MetadataSchemaValidationException(String message)
	{
		super(message);
	}

	/**
	 * @param cause
	 */
	public MetadataSchemaValidationException(Throwable cause)
	{
		super(cause);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public MetadataSchemaValidationException(String message, Throwable cause)
	{
		super(message, cause);
	}

}