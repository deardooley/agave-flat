package org.iplantc.service.monitor.events;

import java.sql.Timestamp;

import javax.persistence.Entity;
import javax.persistence.Table;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.Filters;
import org.hibernate.annotations.ParamDef;
import org.iplantc.service.common.Settings;
import org.iplantc.service.common.model.AbstractEntityEvent;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.common.uuid.AgaveUUID;
import org.iplantc.service.common.uuid.UUIDType;
import org.iplantc.service.monitor.model.Monitor;
import org.iplantc.service.monitor.model.enumeration.MonitorEventType;
import org.iplantc.service.systems.model.enumerations.SystemEventType;

/**
 * Concrete {@link AgaveEntityEvent} class for {@link Monitor}
 * 
 * @author dooley
 *
 */
@Entity
@Table(name = "monitorevents")
@FilterDef(name = "monitorEventTenantFilter", parameters = @ParamDef(name = "tenantId", type = "string"))
@Filters(@Filter(name = "monitorEventTenantFilter", condition = "tenant_id=:tenantId"))
public class DomainEntityEvent extends AbstractEntityEvent<MonitorEventType> {

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.iplantc.service.common.model.AbstractEntityEvent#getEntityEventUUIDType
	 * ()
	 */
	@Override
	protected UUIDType getEntityEventUUIDType() {
		return UUIDType.MONITOR_EVENT;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.iplantc.service.common.model.AbstractEntityEvent#getEntityUUIDType()
	 */
	@Override
	protected UUIDType getEntityUUIDType() {
		return UUIDType.MONITOR;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.iplantc.service.common.model.AbstractEntityEvent#getResolvedEntityUrl
	 * ()
	 */
	@Override
    protected String getResolvedEntityUrl() {
    	return TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_MONITOR_SERVICE) + getEntity();
    }

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.iplantc.service.common.model.AbstractEntityEvent#
	 * getResolvedEntityEventUrl()
	 */
	@Override
	protected String getResolvedEntityEventUrl() {
		return super.getResolvedEntityEventUrl();
	}

	public DomainEntityEvent() {
		setTenantId(TenancyHelper.getCurrentTenantId());
		setUuid(new AgaveUUID(getEntityEventUUIDType()).toString());
		setCreated(new Timestamp(System.currentTimeMillis()));
		setIpAddress(org.iplantc.service.common.Settings.getIpLocalAddress());
	}

	public DomainEntityEvent(MonitorEventType eventType, String description, String createdBy) {
		this();
		setStatus(eventType.name());
		setDescription(description);
		setCreatedBy(createdBy);
	}
	
	public DomainEntityEvent(String entityUuid, MonitorEventType eventType, String createdBy) {
		this(eventType, eventType.getDescription(), createdBy);
		setEntity(entityUuid);setStatus(eventType.name());
	}


	public DomainEntityEvent(String entityUuid, MonitorEventType eventType,
			String description, String createdBy) {
		this(eventType, description, createdBy);
		setEntity(entityUuid);
	}
}
