package org.iplantc.service.monitor.model.enumeration;

/**
 * Defines all events that can occur on a {@link Monitor} or 
 * {@link MonitorCheck}.
 * 
 * @author dooley
 *
 */
public enum MonitorEventType {
	CREATED("This monitor was created"),
    UPDATED("This monitor was updated"),
    DELETED("This monitor was deleted"),
    
    ENABLED("The monitor was enabled"),
    DISABLED("The monitor was disabled"),
    
    PERMISSION_GRANT("A new user permission was granted on this monitor"),
    PERMISSION_REVOKE("A user permission was revoked on this sytem"),
    
    FORCED_CHECK_REQUESTED("A status check was requested by the user outside of the existing monitor schedule."),
    
    CHECK_PASSED("The status check passed"),
	CHECK_FAILED("The status check failed"),
	CHECK_UNKNOWN("The status check finished in an unknown state"),
	
    STATUS_CHANGE("The status condition of the monitored resource changed since the last check"),
    RESULT_CHANGE("The cumulative result of all checks performed on the monitored resource changed since the last suite of checks");
    
	
	
    private String description;
    
    private MonitorEventType(String description) {
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
     * Translates {@link MonitorStatusType} into a comparable {@link MonitorEventType}
     * @param monitorStatusType
     * @return
     */
    public static MonitorEventType valueOfCheckStatus(MonitorStatusType monitorStatusType) {
    	if (monitorStatusType == MonitorStatusType.FAILED) {
    		return CHECK_FAILED;
    	} else if (monitorStatusType == MonitorStatusType.PASSED) {
    		return CHECK_PASSED;
    	} else {
    		return CHECK_UNKNOWN;
    	}
    }
	
}
