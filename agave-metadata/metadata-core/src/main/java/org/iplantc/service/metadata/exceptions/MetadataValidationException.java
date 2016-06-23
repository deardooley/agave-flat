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
public class MetadataValidationException extends Exception {

    private Set<ConstraintViolation<MetadataItem>> violations;
    
	/**
	 *
	 */
	public MetadataValidationException() {}
	
	/**
	 * Constructor adding the constraint violations.
	 * @param violations
	 */
	public MetadataValidationException(final Set<ConstraintViolation<MetadataItem>> violations) {
	    this.violations = violations;
	}

	/**
	 * @param message
	 */
	public MetadataValidationException(String message)
	{
		super(message);
	}

	/**
	 * @param cause
	 */
	public MetadataValidationException(Throwable cause)
	{
		super(cause);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public MetadataValidationException(String message, Throwable cause)
	{
		super(message, cause);
	}

}