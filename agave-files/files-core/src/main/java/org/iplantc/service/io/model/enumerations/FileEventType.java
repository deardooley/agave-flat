/**
 * 
 */
package org.iplantc.service.io.model.enumerations;

import org.apache.commons.lang.StringUtils;


/**
 * @author dooley
 *
 */
public enum FileEventType {

	CREATED("New file/folder was created"),
	OVERWRITTEN("Indexing of file/folder has overwritten with new content"),
	MOVED("Indexing of file/folder has moved"),
	RENAME("File/folder has been renamed"),
	DELETED("File/folder has been deleted"),
	
	STAGING_QUEUED("File/folder queued for staging"), 
	STAGING("Staging file/folder"), 
	STAGING_FAILED("Staging failed"), 
	STAGING_COMPLETED("Staging completed successfully"), 
	PREPROCESSING("Prepairing file for next processing step"),
	
	TRANSFORMING_QUEUED("File/folder queued for transform"), 
	TRANSFORMING("Transforming file/folder"), 
	TRANSFORMING_FAILED("Transform failed"), 
	TRANSFORMING_COMPLETED("Transform completed successfully"), 
	
	INDEX_START("Indexing of file/folder has begun"),
	INDEX_COMPLETE("Indexing of file/folder has completed"),
	INDEX_FAILED("Indexing of file/folder has completed"),
	UPLOAD("File upload has completed"),
	CONTENT_CHANGE("Content of file/folder has changed"),
	
	PERMISSION_GRANT("Permission was added or updated"),
	PERMISSION_REVOKE("Permission was removed"),
	UNKNOWN("Unknown event status"),
	DOWNLOAD("File was downloaded");
	
	private String description;
	
	private FileEventType(String description) {
		this.description = description;
	}

	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * @param description the description to set
	 */
	private void setDescription(String description) {
		this.description = description;
	}
	
	
	/**
	 * Static method to flag events related to content changes
	 * @return
	 */
	public boolean isContentChangeEvent() {
		return (this == OVERWRITTEN || 
				this == STAGING_COMPLETED || 
				this == MOVED || 
				this == RENAME || 
				this == DELETED || 
				this == UPLOAD ||
				this == CREATED);
	}
	
	/**
	 * null-safe check to determine whether an event is related to file/folder content changes
	 * @return
	 */
	public static boolean isContentChangeEvent(String sEvent) {
		try {
			return FileEventType.valueOf(StringUtils.upperCase(sEvent)).isContentChangeEvent();
		} catch (Exception e) {
			return false;
		}
	}
	
	/**
	 * Static method to flag events related to permission changes
	 * @return
	 */
	public boolean isPermissionChangeEvent() {
		return (this == PERMISSION_GRANT || 
				this == PERMISSION_REVOKE);
	}
	
	/**
	 * null-safe check to determine whether an event is related to file/folder permission changes
	 * @return
	 */
	public static boolean isPermissionChangeEvent(String sEvent) {
		try {
			return FileEventType.valueOf(StringUtils.upperCase(sEvent)).isPermissionChangeEvent();
		} catch (Exception e) {
			return false;
		}
	}
	
	public static FileEventType[] getFileTransformEvents() {
		return new FileEventType[] { 
				TRANSFORMING_QUEUED, 
				TRANSFORMING, 
				TRANSFORMING_FAILED, 
				TRANSFORMING_COMPLETED, 
			};
	}
	
	public static FileEventType[] getFileStagingEvents() {
		return new FileEventType[] { 
				STAGING_QUEUED, 
				STAGING, 
				STAGING_FAILED, 
				STAGING_COMPLETED, 
			};
	}
}
