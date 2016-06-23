package org.iplantc.service.io.model;

import java.util.Date;

public interface QueueTask {

    /**
     * @return
     */
    public abstract Long getId();
    
    /**
     * @param id
     */
    public abstract void setId(Long id);
    
	/**
	 * @return the logicalFile
	 */
	public abstract LogicalFile getLogicalFile();

	/**
	 * @param logicalFile the logicalFile to set
	 */
	public abstract void setLogicalFile(LogicalFile logicalFile);

	/**
	 * Get username of the user who creatd the task
	 * @return
	 */
	public abstract String getOwner();
	
	/**
	 * Set username of the user who created this task
	 * @param owner
	 */
	public abstract void setOwner(String owner);
	
//	/**
//	 * @return the eventId
//	 */
//	public abstract String getEventId();
//
//	/**
//	 * @param eventId the eventId to set
//	 */
//	public abstract void setEventId(String eventId);

	/**
	 * @return the created
	 */
	public abstract Date getCreated();

	/**
	 * @param created the created to set
	 */
	public abstract void setCreated(Date created);
	
	/**
	 * @return the lastUpdated
	 */
	public abstract Date getLastUpdated();

	/**
	 * @param lastUpdated the lastUpdated to set
	 */
	public abstract void setLastUpdated(Date lastUpdated);
	
	
	/**
	 * Returns string value of enumerated status object.
	 * @return
	 */
	public abstract String getStatusAsString();
}