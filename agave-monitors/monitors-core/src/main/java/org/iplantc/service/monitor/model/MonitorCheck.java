package org.iplantc.service.monitor.model;

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

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.Filters;
import org.hibernate.annotations.ParamDef;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.common.uuid.AgaveUUID;
import org.iplantc.service.common.uuid.UUIDType;
import org.iplantc.service.monitor.Settings;
import org.iplantc.service.monitor.model.enumeration.MonitorCheckType;
import org.iplantc.service.monitor.model.enumeration.MonitorStatusType;
import org.joda.time.DateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Represents a single invocation of a Monitoring check on a system.
 * 
 * @author dooley
 *
 */
@Entity
@Table(name = "monitor_checks")
@FilterDef(name="monitorcheckTenantFilter", parameters=@ParamDef(name="tenantId", type="string"))
@Filters(@Filter(name="monitorcheckTenantFilter", condition="tenant_id=:tenantId"))
public class MonitorCheck 
{
	private Long id;
	private MonitorStatusType result;
	private MonitorCheckType checkType;
	private String message;
	private Monitor monitor;
	private String uuid;
	private String tenantId;
	private Date created = new Date();
	
	public MonitorCheck() {
		this.uuid = new AgaveUUID(UUIDType.MONITORCHECK).toString();
		this.tenantId = TenancyHelper.getCurrentTenantId();
	}
	
	public MonitorCheck(Monitor monitor, MonitorStatusType result, String message, MonitorCheckType checkType) {
		this();
		setMonitor(monitor);
		setResult(result);
		setMessage(message);
		setCheckType(checkType);
	}

	@Id
	@GeneratedValue
	@Column(name = "id", unique = true, nullable = false)
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
	@Enumerated(EnumType.STRING)
	@Column(name = "result", nullable = false, length = 32)
	public MonitorStatusType getResult() {
		return result;
	}

	/**
	 * @param status the status to set
	 */
	public void setResult(MonitorStatusType result) {
		this.result = result;
	}

	/**
	 * Returns type of this check
	 * @return
	 */
	@Enumerated(EnumType.STRING)
	@Column(name = "type", nullable = true, length = 16)
	public MonitorCheckType getCheckType()
	{
		return checkType;
	}

	/** 
	 * Sets the type of check performed
	 * @param checkType
	 */
	public void setCheckType(MonitorCheckType checkType)
	{
		this.checkType = checkType;
	}

	/**
	 * @return the message
	 */
	@Column(name = "message", nullable = true, length = 2048)
	public String getMessage() {
		return message;
	}

	/**
	 * @param message the message to set
	 */
	public void setMessage(String message) {
		this.message = message;
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
	 * @param nonce the uuid to set
	 */
	public void setUuid(String uuid)
	{
		this.uuid = uuid;
	}

	/**
	 * @return the monitor
	 */
	@ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "monitor", referencedColumnName = "id")
    public Monitor getMonitor() {
		return monitor;
	}

	/**
	 * @param monitorId the monitor to set
	 */
	public void setMonitor(Monitor monitor) {
		this.monitor = monitor;
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
	
	@JsonFormat
	public String toJSON()
	{
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode json = mapper.createObjectNode();
		try
		{	
			json.put("id", getUuid())
				.put("type", getCheckType() == null ? "" : getCheckType().name())
				.put("result", getResult().name())
				.put("message", getMessage())
				.put("created", new DateTime(getCreated()).toString());
				
				ObjectNode linksObject = mapper.createObjectNode();
				linksObject.put("self", (ObjectNode)mapper.createObjectNode()
		    		.put("href", TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_MONITOR_SERVICE) + getMonitor().getUuid() + "/checks/" + getUuid()));
				linksObject.put("monitor", (ObjectNode)mapper.createObjectNode()
			    		.put("href", TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_MONITOR_SERVICE) + getMonitor().getUuid()));
				linksObject.put("system", (ObjectNode)mapper.createObjectNode()
			    		.put("href", TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_SYSTEM_SERVICE) + getMonitor().getSystem().getSystemId()));
			
				json.set("_links", linksObject);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.out.println("Error producing JSON output.");
		}

		return json.toString();
	}
	
}
