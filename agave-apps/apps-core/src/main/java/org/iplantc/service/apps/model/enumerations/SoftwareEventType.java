/**
 * 
 */
package org.iplantc.service.apps.model.enumerations;

/**
 * All possible events occuring on a {@link Software} object withing
 * the platform. These correspond to notification events thrown throughout
 * the domain logic.
 * 
 * @author dooley
 *
 */
public enum SoftwareEventType {
	CREATED("App was created"), 
    UPDATED("App was updated"), 
    CLONED("App was cloned for use as another app"),
    DELETED("App was deleted from active use"),
    RESTORED("App was restored from deleted status"),
    DISABLED("App was disabled"),
    PUBLISHED("App was published for public use"),
    REPUBLISHED("Previously published app was published again with a new revision number"),
    UNPUBLISHED("App was unpublished. It will no longer be available for public use"),
    PERMISSION_REVOKE("One or more user permissions were revoked on this app"),
    PERMISSION_GRANT("One or more user permissions were granted on this app"),
    PUBLISHING_FAILED("A publishing operation failed for this app. This app will not be publicly available until publishing completes successfully."),
    CLONING_FAILED("A cloning operation failed for this app.");

    private String description;
    
    private SoftwareEventType(String description) {
        this.description = description;
    }
    
    /**
     * Gets the description of this event.
     * @return
     */
    public String getDescription() {
        return this.description;
    }

	/**
	 * Determines whether the event is a status related event.
	 * If true, then events will be propagated to execution systems
	 * tied to the app.
	 * @return
	 */
	public boolean isStatusEvent() {
		return (this == CREATED ||
//				this == CLONED || 
				this == DELETED || 
				this == RESTORED ||
				this == DISABLED || 
//				this == PUBLISHED ||
				this == DISABLED);
	}
       
}
