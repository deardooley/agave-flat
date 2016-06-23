package org.iplantc.service.realtime.model.enumerations;

/**
 * All possible events occuring on a {@link RealtimeChannel} object within
 * the platform. These correspond to notification events thrown throughout
 * the domain logic.
 * 
 * @author dooley
 *
 */
public enum RealtimeChannelEventType {
    CREATED("Channel was created"), 
    UPDATED("Channel was updated"),
    DISABLED("Channel was disabled"), 
    ENABLED("Channel was enabled"),
    JOINED("Channel was joined by a new listener"),
    DROPPED("Channel was dropped by an existing listener"),
    DELETED("Channel was deleted"),
    EVENT_ADDED("Channel was restored from deleted status"),
    EVENT_REMOVED("Channel was disabled"),
    PERMISSION_REVOKE("One or more user permissions were revoked on this channel"),
    PERMISSION_GRANT("One or more user permissions were granted on this channel");

    private String description;
    
    private RealtimeChannelEventType(String description) {
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
