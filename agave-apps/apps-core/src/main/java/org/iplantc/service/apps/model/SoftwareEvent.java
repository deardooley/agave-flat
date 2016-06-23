/**
 * 
 */
package org.iplantc.service.apps.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.Filters;
import org.hibernate.annotations.Index;
import org.hibernate.annotations.ParamDef;
import org.iplantc.service.apps.Settings;
import org.iplantc.service.apps.model.enumerations.SoftwareEventType;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.common.uuid.AgaveUUID;
import org.iplantc.service.common.uuid.UUIDType;
import org.iplantc.service.notification.managers.NotificationManager;
import org.iplantc.service.transfer.model.TransferTask;
import org.joda.time.DateTime;

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
@Table(name = "softwareevents")
@FilterDef(name="softwareEventTenantFilter", parameters=@ParamDef(name="tenantId", type="string"))
@Filters(@Filter(name="softwareEventTenantFilter", condition="tenant_id=:tenantId"))
public class SoftwareEvent {

	private Long id;
	private String softwareUuid;
	private String status;
	private String description;
	private String ipAddress;
	private String createdBy;
	private Date created;
	private String tenantId;
	private String uuid;
	
	private TransferTask transferTask;
	
	private SoftwareEvent() {
		this.tenantId = TenancyHelper.getCurrentTenantId();
		this.uuid = new AgaveUUID(UUIDType.APP_EVENT).toString();
		this.created = new Date();
	}

	public SoftwareEvent(String status, String description, String createdBy) {
		this();
		this.status = status;
		this.description = description;
		this.ipAddress = org.iplantc.service.common.Settings.getIpLocalAddress();
		this.createdBy = createdBy;
		this.created = new Date();
	}
	
	public SoftwareEvent(Software software, SoftwareEventType eventType, String description, String createdBy)
	{
		this(eventType.name(), description, createdBy);
		setSoftwareUuid(software.getUuid());
	}
	
	public SoftwareEvent(Software software, SoftwareEventType eventType, String description, TransferTask transferTask, String createdBy)
	{
		this(eventType.name(), description, createdBy);
		setTransferTask(transferTask);
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
	 * @return the softwareUuid
	 */
	@Index(name="software_uuid")
	@Column(name = "software_uuid", length = 128, nullable = false)
	public String getSoftwareUuid()
	{
		return softwareUuid;
	}

	/**
	 * @param softwareUuid the softwareUuid to set
	 */
	public void setSoftwareUuid(String softwareUuid)
	{
		this.softwareUuid = softwareUuid;
	}

	/**
	 * Assigns the uuid of the given software object to 
	 * the event
	 * @param software
	 */
	@Transient
	public void setSoftware(Software software)
    {
	    if (software != null) {
	        this.softwareUuid = software.getUuid();
	    }
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
	 * @return the username
	 */
	@Column(name = "created_by", nullable = false, length = 128)
	public String getCreatedBy()
	{
		return createdBy;
	}

	/**
	 * @param username the creator to set
	 */
	public void setCreatedBy(String createdBy)
	{
		this.createdBy = createdBy;
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
	 * @return the tenantId
	 */
	@Column(name = "tenant_id", nullable=false, length = 128)
	public String getTenantId()
	{
		return tenantId;
	}

	/**
	 * @param tenantId the tenantId to set
	 */
	public void setTenantId(String tenantId)
	{
		this.tenantId = tenantId;
	}

	/**
	 * @return the transferTask
	 */
	@OneToOne(fetch = FetchType.EAGER)
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
	 * @return the created
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "created", nullable = false, length = 19)
	public Date getCreated()
	{
		return created;
	}

	/**
     * @return the uuid
     */
    @Column(name = "uuid", length = 128, nullable = false, unique = true)
    public String getUuid()
    {
        return uuid;
    }

    /**
     * @param uuid the uuid to set
     */
    public void setUuid(String uuid)
    {
        this.uuid = uuid;
    }
    
	/**
	 * @param created the created to set
	 */
	public void setCreated(Date created)
	{
		this.created = created;
	}
	
	public void fire() {
	    NotificationManager.process(getSoftwareUuid(), getStatus(), getCreatedBy());
	}
	
	public String toJSON(Software software) {
	    ObjectMapper mapper = new ObjectMapper();
        ObjectNode json = mapper.createObjectNode()
                .put("status", getStatus())
                .put("createdBy", getCreatedBy())
                .put("description", getDescription())
                .put("created", new DateTime(getCreated()).toString())
                .put("id", getUuid());
        ObjectNode links = json.putObject("_links");
        links.put("self", mapper.createObjectNode()
                .put("href", TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_APPS_SERVICE) + software.getUniqueName() + "/history/" + getUuid()));
        links.put("app", mapper.createObjectNode()
                .put("href", TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_APPS_SERVICE) + software.getUniqueName()));
        links.put("profile", mapper.createObjectNode()
                .put("href", TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_PROFILE_SERVICE) + getCreatedBy()));
        
        return json.toString();
	}
	
	public String toString() {
		return getSoftwareUuid() + " " + getStatus() + " " + new DateTime(getCreated()).toString();
	}
}
