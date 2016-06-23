package org.iplantc.service.monitor.model;

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
import javax.persistence.UniqueConstraint;

import org.apache.commons.lang.StringUtils;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.Filters;
import org.hibernate.annotations.ParamDef;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.common.uuid.AgaveUUID;
import org.iplantc.service.common.uuid.UUIDType;
import org.iplantc.service.monitor.Settings;
import org.iplantc.service.monitor.dao.MonitorCheckDao;
import org.iplantc.service.monitor.exceptions.MonitorException;
import org.iplantc.service.monitor.util.ServiceUtils;
import org.iplantc.service.profile.dao.InternalUserDao;
import org.iplantc.service.profile.model.InternalUser;
import org.iplantc.service.systems.dao.SystemDao;
import org.iplantc.service.systems.model.RemoteSystem;
import org.joda.time.DateTime;
import org.testng.log4testng.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Entity
@Table(name = "monitors", uniqueConstraints=
	@UniqueConstraint(columnNames={"system","owner","tenant_id"}))
@FilterDef(name="monitorTenantFilter", parameters=@ParamDef(name="tenantId", type="string"))
@Filters(@Filter(name="monitorTenantFilter", condition="tenant_id=:tenantId"))
public class Monitor
{
	private static final Logger log = Logger.getLogger(Monitor.class);

	private Long id;						// private db id
	private String uuid;					// uuid of this monitor
	private RemoteSystem system;			// system to monitor
	private int frequency = 720;				// how often this monitor will run
	private String owner;					// who created this monitor
	private boolean updateSystemStatus;		// should the system status be updated when this monitor detects a change?
	private String internalUsername;		// internal user account to use when connecting
	private boolean active = true;			// is this monitor active
	private String tenantId;				// tenant to which this monitor belongs
	private Date lastUpdated = new Date();	// when was the monitor last updated
	private Date nextUpdateTime = new Date();// when was the monitor last updated
	private Date lastSuccess = null;
	private Date created = new Date();		// when was the monitor created

	public Monitor() {
		this.uuid = new AgaveUUID(UUIDType.MONITOR).toString();
		this.tenantId = TenancyHelper.getCurrentTenantId();
	}

	public Monitor(RemoteSystem system, int frequency, String owner) throws MonitorException
	{
		this();
		setSystem(system);
		setFrequency(frequency);
		setOwner(owner);
	}

	/**
	 * @return the id
	 */
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
	public void setUuid(String uuid) throws MonitorException
	{
		if (StringUtils.length(uuid) > 64) {
			throw new MonitorException("Invalid monitor uuid. " +
					"Monitor associatedUuid must be less than 64 characters.");
		}
		this.uuid = uuid;
	}

	/**
	 * @return the owner
	 */
	@Column(name = "owner", nullable = false, length = 32)
	public String getOwner()
	{
		return owner;
	}

	/**
	 * @param owner the owner to set
	 */
	public void setOwner(String owner) throws MonitorException
	{
		if (StringUtils.length(owner) > 32) {
			throw new MonitorException("Invalid monitor owner. " +
					"Monitor owner must be less than 32 characters.");
		}
		this.owner = owner;
	}

	/**
	 * @return the system
	 */
	@ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "system", referencedColumnName = "id")
    public RemoteSystem getSystem() {
		return system;
	}

	/**
	 * @param system the system to set
	 */
	public void setSystem(RemoteSystem system) throws MonitorException
	{
		if (system == null) {
			throw new MonitorException("Invalid monitor target. " +
					"Monitor target must be a valid system id.");
		}
		this.system = system;
	}

	/**
	 * @return the frequency
	 */
	@Column(name = "frequency")
	public int getFrequency() {
		return frequency;
	}

	/**
	 * @param frequency the frequency to set
	 */
	public void setFrequency(int frequency) throws MonitorException
	{
		if (frequency < 5) {
			throw new MonitorException("Invalid monitor frequency. " +
					"Monitor frequency must be greater than 5 minutes.");
		}
		this.frequency = frequency;
	}

	/**
	 * @return the updateSystemStatus
	 */
	@Column(name = "update_system_status", columnDefinition = "TINYINT(1)")
	public boolean isUpdateSystemStatus() {
		return updateSystemStatus;
	}

	/**
	 * @param updateSystemStatus the updateSystemStatus to set
	 */
	public void setUpdateSystemStatus(boolean updateSystemStatus) {
		this.updateSystemStatus = updateSystemStatus;
	}

	/**
	 * @return the internalUsername
	 */
	@Column(name = "internal_username", nullable=true, length = 64)
	public String getInternalUsername() {
		return internalUsername;
	}

	/**
	 * @param internalUsername the internalUsername to set
	 */
	public void setInternalUsername(String internalUsername) throws MonitorException
	{
		if (StringUtils.length(internalUsername) > 64) {
			throw new MonitorException("Invalid monitor internalUsername. " +
					"Monitor internalUsername must be less than 64 characters.");
		}

		this.internalUsername = internalUsername;
	}

	/**
	 * @return is this monitor active
	 */
	@Column(name = "is_active", columnDefinition = "TINYINT(1)")
	public boolean isActive()
	{
		return active;
	}

	/**
	 * @param active the active to set
	 */
	public void setActive(boolean active)
	{
		this.active = active;
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
	 * @return nextCheckTime
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "next_update_time", nullable = false, length = 19)
	public Date getNextUpdateTime() {
		return nextUpdateTime;
	}

	/**
	 * @param nextCheckTime
	 */
	public void setNextUpdateTime(Date nextUpdateTime) {
		this.nextUpdateTime = nextUpdateTime;
	}

	/**
	 * @return the lastUpdated
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "last_updated", nullable = false, length = 19)
	public Date getLastUpdated()
	{
		return lastUpdated;
	}

	/**
	 * @param lastUpdated the lastUpdated to set
	 */
	public void setLastUpdated(Date lastUpdated)
	{
		this.lastUpdated = lastUpdated;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "last_success", nullable = true, length = 19)
	public Date getLastSuccess() {
		return lastSuccess;
	}

	public void setLastSuccess(Date lastSuccess) {
		this.lastSuccess = lastSuccess;
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

	public static Monitor fromJSON(JsonNode json, Monitor monitor, String username)
	throws MonitorException
	{
		if (monitor == null) {
			monitor = new Monitor();
			monitor.setOwner(username);
		}

		try
		{
			if (!json.has("target") || json.get("target").isNull()) {
				throw new MonitorException("Invalid 'target' value. "
						+ "Please specify the id of a valid storage or execution system.");
			}
			else
			{
				RemoteSystem system = new SystemDao().findBySystemId(json.get("target").asText());

				if (system == null) {
					throw new MonitorException("Invalid 'target' value. "
							+ "No system found matching the given target system id.");
				}
				else if (!system.getUserRole(username).canUse())
				{
					throw new MonitorException("Permission denied. You do not have permission to view this system.");
				}
				else
				{
					monitor.setSystem(system);
				}
			}

			if (json.has("updateSystemStatus"))
			{
				if (ServiceUtils.isBooleanOrNumericBoolean(json.get("updateSystemStatus")))
				{
					boolean doUpdate = json.get("updateSystemStatus").asBoolean();
					if (doUpdate && !monitor.getSystem().getUserRole(username).canAdmin()) {
						throw new MonitorException("Permission denied. "
								+ "You must have an ADMIN role on the system to update its status.");
					} else {
						monitor.setUpdateSystemStatus(doUpdate);
					}
				} else {
					throw new MonitorException("Invalid 'updateSystemStatus' value. "
							+ "If provided, updateSystemStatus must be a boolean value.");
				}
			}
			else
			{
				monitor.setUpdateSystemStatus(false);
			}

			if (json.has("internalUsername") && !json.get("internalUsername").isNull())
	    	{
				String internalUsername = null;
				if (json.get("internalUsername").isTextual())
				{
					internalUsername = json.get("internalUsername").asText();
				}
				else
				{
					throw new MonitorException("Invalid 'internalUsername' value. "
							+ "If provided, internalUsername must be the username of a valid internal user.");
				}

				InternalUser iternalUser = new InternalUserDao().getInternalUserByAPIUserAndUsername(username, internalUsername);
				if (iternalUser == null)
	        	{
	        		throw new MonitorException(
							"Invalid 'updateSystemStatus' value. No internal user found matching username '" + internalUsername + "'");
				}
	        	else if (!iternalUser.getCreatedBy().equals(username)) {
	        		throw new MonitorException("Permission denied. "
	        				+ "You do not have permission to interact with internal users that you did not create.");
	        	}
	        	else
	        	{
	        		monitor.setInternalUsername(internalUsername);
	        	}
	    	}
	    	else {
	    		monitor.setInternalUsername(null);
	    	}

			if (json.has("frequency"))
			{
				if (json.get("frequency").isNumber())
				{
					int frequency = json.get("frequency").asInt();
					if (frequency < (Settings.MINIMUM_MONITOR_REPEAT_INTERVAL/60))
					{
						throw new MonitorException("Invalid 'frequency' value. "
								+ "If provided, frequency must be an integer value represeting the "
								+ "time in minutes between invocations of the monitor. The minimum "
								+ "frequency for any monitor is " + (Settings.MINIMUM_MONITOR_REPEAT_INTERVAL/60) + " minutes.");
					}
					else
					{
						monitor.setFrequency(frequency);
					}
				}
				else
				{
					throw new MonitorException("Invalid 'frequency' value. "
							+ "If provided, frequency must be an integer value represeting the "
							+ "time in minutes between invocations of the monitor.");
				}
			}
			else
			{
				monitor.setFrequency(720);
			}
			
			// set the next update time to now + monitor.getFrequency() minutes
			// in the future so users can fire the check immediately to test without
			// violating the minimum check interval quota. This will only happen if
			// 1. this is a new monitor or 
			// 2. the monitor has not run yet or
			// 3. the new frequency would result in the monitor running before it 
			//    is currently scheduled to run
			// This prevents us from negatively delaying scheduled checks on updates.
			Date nextUpdateTime = new DateTime().plusMinutes(monitor.getFrequency()).toDate();
			if (monitor.getNextUpdateTime() == null 
					|| monitor.getNextUpdateTime().after(nextUpdateTime)) {
				monitor.setNextUpdateTime(nextUpdateTime);
			}
			
			if (json.has("active"))
			{
				if (json.get("active").isBoolean()) {
					monitor.setActive(json.get("active").asBoolean());
				} else {
					throw new MonitorException("Invalid 'active' value. "
							+ "If provided, active must be a boolean value.");
				}
			}
			else
			{
				monitor.setActive(true);
			}

			return monitor;
		}
		catch (MonitorException e)
		{
			throw e;
		}
		catch (Exception e) {
			throw new MonitorException("Failed to parse 'monitor' object: " + e.getMessage(), e);
		}
	}

	public String toJSON()
	{
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode json = mapper.createObjectNode();
		try
		{
			json.put("id", getUuid())
				.put("target", getSystem().getSystemId())
				.put("owner", getOwner())
				.put("frequency", frequency)
				.put("updateSystemStatus", isUpdateSystemStatus())
				.put("internalUsername", internalUsername)
				.put("lastSuccess", getLastSuccess() == null ? null : new DateTime(getLastSuccess()).toString())
				.put("lastUpdated", new DateTime(getLastUpdated()).toString())
				.put("nextUpdate", isActive() ? new DateTime(getNextUpdateTime()).toString() : null)
				.put("created", new DateTime(getCreated()).toString())
				.put("active", isActive());

				ObjectNode lastCheckObject = mapper.createObjectNode();
				MonitorCheck lastCheck = new MonitorCheckDao().getLastMonitorCheck(getId());
				if (lastCheck != null)
				{
					lastCheckObject
						.put("id", lastCheck.getUuid())
						.put("result", lastCheck.getResult().name())
						.put("message", lastCheck.getMessage())
						.put("type", lastCheck.getCheckType().name())
						.put("created", new DateTime(lastCheck.getCreated()).toString());
				}

				json.put("lastCheck", lastCheckObject);

				ObjectNode linksObject = mapper.createObjectNode();
				linksObject.put("self", (ObjectNode)mapper.createObjectNode()
		    		.put("href", TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_MONITOR_SERVICE) + getUuid()));
				linksObject.put("checks", (ObjectNode)mapper.createObjectNode()
			    		.put("href", TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_MONITOR_SERVICE) + getUuid() + "/checks"));
				if (!StringUtils.isEmpty(internalUsername)) {
		    		linksObject.put("internalUser", mapper.createObjectNode()
		    			.put("href", TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_PROFILE_SERVICE) + getOwner() + "/users/" + getInternalUsername()));
		    	}
				linksObject.put("notifications", mapper.createObjectNode()
						.put("href", TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_NOTIFICATION_SERVICE) + "?associatedUuid=" + getUuid()));
				linksObject.put("owner", mapper.createObjectNode()
						.put("href", TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_PROFILE_SERVICE) + getOwner()));
				linksObject.put("system", (ObjectNode)mapper.createObjectNode()
			    		.put("href", TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_SYSTEM_SERVICE) + getSystem().getSystemId()));

				json.put("_links", linksObject);
		}
		catch (Exception e)
		{
			log.error("Error producing JSON output.", e);
		}

		return json.toString();

	}
	public String toString() {
		return String.format("%s - %s - %s", owner, system.getSystemId(), frequency);
	}
}
