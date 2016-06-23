/**
 * 
 */
package org.iplantc.service.io.model.enumerations;

/**
 * @author dooley
 *
 */
public enum TransformTaskStatus
{
	TRANSFORMING_QUEUED("File/folder queued for transform"), 
	TRANSFORMING("Transforming file/folder"), 
	TRANSFORMING_FAILED("Transform failed"), 
	TRANSFORMING_COMPLETED("Transform completed successfully"), 
	PREPROCESSING("Prepairing file for transform");
	
	private final String description;
	
	TransformTaskStatus(String description) {
		this.description = description;
	}
	
	public String getDescription() {
		return description;
	}
}
