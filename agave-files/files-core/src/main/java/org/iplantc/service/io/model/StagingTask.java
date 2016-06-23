/**
 * 
 */
package org.iplantc.service.io.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.persistence.Version;

import org.apache.commons.lang3.StringUtils;
import org.iplantc.service.io.model.enumerations.StagingTaskStatus;

/**
 * Represents a persistent record of a file transfer. For recursive transfers,
 * the source should be recursively listed and a task created for each file/folder.
 * 
 * @author dooley
 *
 */
@Entity
@Table(name = "staging_tasks")
public class StagingTask implements QueueTask {
	
	private Long id;
	private LogicalFile logicalFile;
	private StagingTaskStatus status = StagingTaskStatus.STAGING_QUEUED;
	private int retryCount = 0;
	private long totalBytes = 0;
	private long bytesTransferred = 0;
	private String owner;
	private Date created = new Date();	
	private Date lastUpdated = new Date();
	private Integer version = 0;
	

	public StagingTask() {}
	
	public StagingTask(LogicalFile file, String owner) {
		setLogicalFile(file);
		this.setStatus(StagingTaskStatus.STAGING_QUEUED);
		this.setOwner(owner);
	}

	/**
	 * @param id the id to set
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * @return the id
	 */
	@Id
	@GeneratedValue
	@Column(name = "id", unique = true, nullable = false)
	public Long getId() {
		return id;
	}

	@Override
	@ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "logicalFileId", referencedColumnName = "id")
    public LogicalFile getLogicalFile()
	{
		return logicalFile;
	}

	@Override
	public void setLogicalFile(LogicalFile logicalFile)
	{
		this.logicalFile = logicalFile;
	}

	public void setStatus(StagingTaskStatus status)
	{
		this.status = status;
	}

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 32)
	public StagingTaskStatus getStatus()
	{
		return status;
	}
	
	@Override
	@Transient
	public String getStatusAsString()
	{
		return status.name();
	}

	/**
	 * @return the created
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "created", nullable = false, length = 19)
	public Date getCreated()
	{
		return created;
	}

	/**
	 * @param created
	 *            the created to set
	 */
	public void setCreated(Date created)
	{
		this.created = created;
	}

	/**
	 * @return the lastUpdated
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "lastUpdated", nullable = false, length = 19)
	public Date getLastUpdated()
	{
		return lastUpdated;
	}

	/**
	 * @param lastUpdated
	 *            the lastUpdated to set
	 */
	public void setLastUpdated(Date lastUpdated)
	{
		this.lastUpdated = lastUpdated;
	}
	
	/**
	 * @param retryCount the retryCount to set
	 */
	@Column(name = "retryCount", nullable = true, length = 3)
	public void setRetryCount(int retryCount) {
		this.retryCount = retryCount;
	}

	/**
	 * @return the retryCount
	 */
	
	public int getRetryCount() {
		return retryCount;
	}

	/**
	 * @return the totalBytes
	 */
	@Column(name = "totalBytes", nullable = true)
	public long getTotalBytes()
	{
		return totalBytes;
	}

	/**
	 * @param totalBytes the totalBytes to set
	 */
	public void setTotalBytes(long totalBytes)
	{
		this.totalBytes = totalBytes;
	}

	/**
	 * @return the bytesTransferred
	 */
	@Column(name = "bytesTransferred", nullable = true)
	public long getBytesTransferred()
	{
		return bytesTransferred;
	}

	/**
	 * @param bytesTransferred the bytesTransferred to set
	 */
	public void setBytesTransferred(long bytesTransferred)
	{
		this.bytesTransferred = bytesTransferred;
	}
	
	/**
	 * @return the version
	 */
	@Version
    @Column(name="OPTLOCK")
    public Integer getVersion() {
		return version;
	}
	
	/**
	 * @param version the current version
	 */
	public void setVersion(Integer version) {
		this.version = version;
	}

    /* (non-Javadoc)
     * @see org.iplantc.service.io.model.QueueTask#getOwner()
     */
    @Override
    @Column(name = "owner", nullable = false, length = 32)
    public String getOwner() {
        return owner;
    }

    /* (non-Javadoc)
     * @see org.iplantc.service.io.model.QueueTask#setOwner(java.lang.String)
     */
    @Override
    public void setOwner(String owner) {
        this.owner = owner;
    }

	/* (non-Javadoc)
	 * @see org.iplantc.service.io.model.QueueTask#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object o) {
		if (o instanceof StagingTask) {
			StagingTask task = (StagingTask)o;
			return (task.id.equals(id) && 
					task.logicalFile.equals(logicalFile) && 
					StringUtils.equals(task.owner,owner)); 
		}
		return false;
	}
	
	
	
}
