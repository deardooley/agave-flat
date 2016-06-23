/**
 * 
 */
package org.iplantc.service.metadata.model.enumerations;

/**
 * @author dooley
 *
 */
public enum MetadataEventType {
    CREATED("A new metadata item was created"), 
    UPDATED("A metadata was updated"), 
    DELETED("A metadata item was deleted from active use"),
    RESTORED("A metadata item was restored from deleted status"),
    
    PERMISSION_REVOKE("One or more user permissions were revoked on this metadata item"),
    PERMISSION_GRANT("One or more user permissions were granted on this app"),
    PERMISSION_UPDATE("One or more user permissions were changed on this app"),
    
    METADATA_CREATED("A metadata item associated with this resource was created"), 
    METADATA_UPDATED("A metadata item associated with this resource was updated"), 
    METADATA_DELETED("A metadata item associated with this resource was deleted from active use"),
    METADATA_RESTORED("A metadata item associated with this resource was restored from deleted status");
    
    private String description;
    
    private MetadataEventType(String description) {
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
