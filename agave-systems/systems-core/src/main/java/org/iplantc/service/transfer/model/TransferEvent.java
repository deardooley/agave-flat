/**
 * 
 */
package org.iplantc.service.transfer.model;

import java.util.Date;

import javax.persistence.CascadeType;
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

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.Filters;
import org.hibernate.annotations.ParamDef;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.common.uuid.AgaveUUID;
import org.iplantc.service.common.uuid.UUIDType;
import org.iplantc.service.transfer.model.enumerations.TransferStatusType;
import org.joda.time.DateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Entity class for persisting transfer task events. This creates a history log 
 * per transfer that can be queried and/or mined 
 * 
 * @author dooley
 *
 */
@Entity
@Table(name = "transferevents")
@FilterDef(name="transferEventTenantFilter", parameters=@ParamDef(name="tenantId", type="string"))
@Filters(@Filter(name="transferEventTenantFilter", condition="tenant_id=:tenantId"))
public class TransferEvent {

    @JsonIgnore private Long id;
    @JsonIgnore private TransferTask transferTask;
    @JsonIgnore private String tenantId;
    @JsonProperty("id") private String uuid;
	
	// Searchable fields for this object
	@JsonProperty("status") private String status;
	@JsonProperty("description") private String description;
	@JsonProperty("ipAddress") private String ipAddress;
	@JsonProperty("owner") private String owner;
	@JsonProperty("created") private Date created;
	
	private TransferEvent() {
		this.created = new Date();
		this.tenantId = TenancyHelper.getCurrentTenantId();
		this.setUuid(new AgaveUUID(UUIDType.TRANSFER_EVENT).toString());
	}

	public TransferEvent(String status, String description, String createdBy) {
		this();
		this.status = status;
		this.description = description;
		this.ipAddress = org.iplantc.service.common.Settings.getIpLocalAddress();
		this.owner = createdBy;
	}
	
	public TransferEvent(TransferStatusType status, String description, String createdBy)
	{
		this(status.name(), description, createdBy);
	}
	
	public TransferEvent(TransferTask job, TransferStatusType status, String description, String createdBy)
	{
		this(status.name(), description, createdBy);
		this.transferTask = job;
	}
	
	public TransferEvent(TransferStatusType status, String description, TransferTask transferTask, String createdBy)
	{
		this(status.name(), description, createdBy);
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
	 * @return the owner
	 */
	@Column(name = "owner", nullable = false, length = 128)
	public String getOwner()
	{
		return owner;
	}

	/**
	 * @param owner the owner to set
	 */
	public void setOwner(String createdBy)
	{
		this.owner = createdBy;
	}

	/**
	 * @return the message
	 */
	@Column(name = "description", length = 4096)
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
	 * @return the uuid
	 */
	@Column(name = "uuid", nullable = false, length = 64, unique = true)
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
	@OneToOne(fetch = FetchType.EAGER, cascade={CascadeType.ALL,CascadeType.REMOVE})
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
	 * @param created the created to set
	 */
	public void setCreated(Date created)
	{
		this.created = created;
	}
	
	public String toString() {
		return transferTask + " " + status + " " + new DateTime(created).toString();
	}
}
