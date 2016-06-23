/**
 * 
 */
package org.iplantc.service.transfer.model.enumerations;

/**
 * @author dooley
 *
 */
public enum TransferTaskEventType {
	
	CREATED("This transfer task was created"),
    UPDATED("This transfer task was updated"),
    DELETED("This transfer task was deleted"),
    
    STARTED("This transfer task started transferring data"),
    RESTARTED("This transfer task was restarted"),
    CANCELLED("This transfer task was cancelled"),
    
    PAUSED("This transfer task was paused"),
    RESUME("This transfer task was resumed"),
    RESET("This transfer task was reset to the beginning"),
	
    CHECKSUM_STARTED("This transfer task began calculating checksum(s) for the transferred data"),
    CHECKSUM_COMPLETED("This transfer finished calculating checksum(s) for the transferred data"),
    CHECKSUM_FAILED("This transfer finished calculating checksum(s) for the transferred data"),
    
    COMPRESSION_STARTED("This transfer task began compressing transferred data"),
    COMPRESSION_COMPLETED("This transfer finished compressing transferred data"),
    COMPRESSION_FAILED("This transfer finished compressing transferred data"),
    
    BUILDING_MANIFEST("This transfer task is building a manifest of all files and folders to be transferred");
	
	private String description;

	private TransferTaskEventType(String description) {
		setDescription(description);
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
	public void setDescription(String description) {
		this.description = description;
	}

}
