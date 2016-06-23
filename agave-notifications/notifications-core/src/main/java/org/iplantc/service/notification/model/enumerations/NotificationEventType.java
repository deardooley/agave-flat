/**
 * 
 */
package org.iplantc.service.notification.model.enumerations;

/**
 * Events occurring on a {@link Notification}
 * @author dooley
 *
 */
public enum NotificationEventType {
	
	CREATED("Notification was created"),
	UPDATED("Notification was updated"),
	DELETED("Notification was deleted"),
	
	DISABLED("Notification was disabled"), 
    ENABLED("Notification was enabled"),
	
	FAILURE("Notification of an event notification failed"),
	SUCCESS("Notification was successfully delivered"),
	
	SEND_ERROR("Notification attempt was unsuccessful"),
	RETRY_ERROR("Notification retry attempt was unsuccessful"),
	
	PERMISSION_REVOKE("One or more user permissions were revoked on this notification"),
    PERMISSION_GRANT("One or more user permissions were granted on this notification"),
    FORCED_ATTEMPT("Notification attempt was forced by user");

    private String description;
    
    private NotificationEventType(String description) {
        this.description = description;
    }
    
    /**
     * Gets the description of this event.
     * @return
     */
    public String getDescription() {
        return this.description;
    }
}
