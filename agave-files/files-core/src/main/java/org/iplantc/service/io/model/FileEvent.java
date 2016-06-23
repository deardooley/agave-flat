/**
 * 
 */
package org.iplantc.service.io.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.apache.commons.lang.SerializationException;
import org.iplantc.service.common.Settings;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.common.uuid.AgaveUUID;
import org.iplantc.service.common.uuid.UUIDType;
import org.iplantc.service.io.model.enumerations.FileEventType;
import org.iplantc.service.io.util.ServiceUtils;
import org.iplantc.service.transfer.model.TransferTask;
import org.joda.time.DateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Entity class for persisting job events. This creates a history log 
 * per job that can be queried and/or mined 
 * 
 * @author dooley
 *
 */
@Entity
@Table(name = "fileevents")
//@FilterDef(name="fileEventTenantFilter", parameters=@ParamDef(name="tenantId", type="string"))
//@Filters(@Filter(name="fileEventTenantFilter", condition="tenant_id=:tenantId"))
public class FileEvent {

	private Long id;
	private LogicalFile logicalFile;
	private String status;
	private String description;
	private String ipAddress;
    private String tenantId;
	private String createdBy;
	private String uuid;
	private Date created;
	private TransferTask transferTask;
	
	public FileEvent() {
		this.uuid = new AgaveUUID(UUIDType.FILE_EVENT).toString();
		this.tenantId = TenancyHelper.getCurrentTenantId();
		this.created = new Date();
	}

	public FileEvent(FileEventType status, String description, String createdBy)
	{
		this();
		this.status = status.name();
		this.description = description;
		this.createdBy = createdBy;
		this.ipAddress = org.iplantc.service.common.Settings.getIpLocalAddress();
	}
	
	public FileEvent(FileEventType status, String description, String createdBy, String tenantId)
	{
		this();
		this.status = status.name();
		this.description = description;
		this.createdBy = createdBy;
		this.ipAddress = org.iplantc.service.common.Settings.getIpLocalAddress();
		this.tenantId = tenantId;
	}
	
	public FileEvent(LogicalFile logicalFile, FileEventType status, String description, String createdBy)
	{
		this(status, description, createdBy);
		this.logicalFile = logicalFile;
	}
	
	public FileEvent(FileEventType status, String description, String createdBy, TransferTask transferTask)
	{
		this(status, description, createdBy);
		this.transferTask = transferTask;
	}

	/**
	 * @return the id
	 */
	@Id
	@GeneratedValue
	@Column(name = "`id`", unique = true, nullable = false)
	public Long getId()
	{
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(Long id)
	{
		this.id = id;
	}

	/**
	 * @return the job
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "logicalfile_id")
	public LogicalFile getLogicalFile()
	{
		return logicalFile;
	}

	/**
	 * @param logicalFile the LogicalFile to set
	 */
	public void setLogicalFile(LogicalFile logicalFile)
	{
		this.logicalFile = logicalFile;
	}

	/**
	 * @return the status
	 */
	@Column(name = "`status`", nullable = false, length = 32)
	public String getStatus()
	{
		return status;
	}

	/**
	 * @param status the status to set
	 */
	public void setStatus(String status)
	{
		this.status = status;
	}

	/**
	 * @return the message
	 */
	@Column(name = "description", length = 1024)
	public String getDescription()
	{
		return description;
	}

	/**
	 * @param description the description to set
	 */
	public void setDescription(String description)
	{
		this.description = description;
	}

	/**
	 * @return the ipAddress
	 */
	@Column(name = "ip_address", nullable = false, length = 15)
	public String getIpAddress()
	{
		return ipAddress;
	}

	/**
	 * @param ipAddress the ipAddress to set
	 */
	public void setIpAddress(String ipAddress)
	{
		this.ipAddress = ipAddress;
	}

	/**
	 * @return the transferTask
	 */
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "transfertask")
	public TransferTask getTransferTask()
	{
		return transferTask;
	}

	/**
	 * @param transferTask the transferTask to set
	 */
	public void setTransferTask(TransferTask transferTask)
	{
		this.transferTask = transferTask;
	}

	/**
     * @return the username
     */
    @Column(name = "created_by", nullable = false, length = 32)
    public String getCreatedBy() {
        return createdBy;
    }

    /**
     * @param username
     *            the creator to set
     */
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
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
	 * @param created the created to set
	 */
	public void setCreated(Date created)
	{
		this.created = created;
	}

	/**
     * @return the tenantId
     */
    @Column(name = "tenant_id", nullable = false, length = 64)
    @JsonIgnore
    public String getTenantId() {
        return tenantId;
    }

    /**
     * @param tenantId
     *            the tenantId to set
     */
    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    /**
     * @return the uuid
     */
    @Column(name = "uuid", length = 64, nullable = false, unique = true)
    public String getUuid() {
        return uuid;
    }

    /**
     * @param uuid
     *            the uuid to set
     */
    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
	
	public String toString() {
		return logicalFile + " " + status + " " + new DateTime(created).toString();
	}
	

    @JsonValue
    public String toJSON() throws SerializationException {
        ObjectMapper mapper = new ObjectMapper();
        
        ObjectNode json = mapper.createObjectNode()
        		.put("status", getStatus())
                .put("createdBy", getCreatedBy())
                .put("description", getDescription())
                .put("created", new DateTime(getCreated()).toString())
                .put("id", getUuid());

        ObjectNode linksObject = json.putObject("_links");
        linksObject.put(
                "self",
                (ObjectNode) mapper.createObjectNode().put(
                        "href",
                        logicalFile.getEventLink()));
        
        linksObject.put("file", (ObjectNode)mapper.createObjectNode()
                .put("href", 
                		logicalFile.getPublicLink()));
        
        linksObject.put(
                "profile",
                (ObjectNode) mapper.createObjectNode().put(
                        "href",
                        TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_PROFILE_SERVICE)
                                + getCreatedBy()));
        json.put("_links", linksObject);

        return json.toString();
    }

}
