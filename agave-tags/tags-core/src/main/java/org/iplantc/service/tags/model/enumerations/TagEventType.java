package org.iplantc.service.tags.model.enumerations;

/**
 * All possible events occuring on a {@link Tag} object within
 * the platform. These correspond to notification events thrown throughout
 * the domain logic.
 * 
 * @author dooley
 *
 */
public enum TagEventType {
    CREATED("Tag was registered"), 
    UPDATED("Tag was updated"), 
    DELETED("Tag was deleted from active use"),
    RESOURCE_ADDED("Tag was restored from deleted status"),
    RESOURCE_REMOVED("Tag was disabled"),
    PUBLISHED("Tag was published for public use"),
    UNPUBLISHED("Tag was unpublished. It will no longer be available for public use"),
    PERMISSION_REVOKE("One or more user permissions were revoked on this tag"),
    PERMISSION_GRANT("One or more user permissions were granted on this tag");

    private String description;
    
    private TagEventType(String description) {
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
