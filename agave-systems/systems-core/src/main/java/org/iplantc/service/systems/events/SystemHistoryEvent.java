package org.iplantc.service.systems.events;

import java.sql.Timestamp;

import javax.persistence.Entity;
import javax.persistence.Table;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.Filters;
import org.hibernate.annotations.ParamDef;
import org.iplantc.service.common.Settings;
import org.iplantc.service.common.model.AbstractEntityEvent;
import org.iplantc.service.common.model.AgaveEntityEvent;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.common.uuid.AgaveUUID;
import org.iplantc.service.common.uuid.UUIDType;
import org.iplantc.service.systems.model.RemoteSystem;
import org.iplantc.service.systems.model.enumerations.SystemEventType;

/**
 * Concrete {@link AgaveEntityEvent} class for {@link RemoteSystem}
 * 
 * @author dooley
 *
 */
@Entity
@Table(name = "systemevents")
@FilterDef(name = "systemHistoryEventTenantFilter", parameters = @ParamDef(name = "tenantId", type = "string"))
@Filters(@Filter(name = "systemHistoryEventTenantFilter", condition = "tenant_id=:tenantId"))
public class SystemHistoryEvent extends AbstractEntityEvent<SystemEventType> {

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.iplantc.service.common.model.AbstractEntityEvent#getEntityEventUUIDType
	 * ()
	 */
	@Override
	protected UUIDType getEntityEventUUIDType() {
		return UUIDType.SYSTEM_EVENT;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.iplantc.service.common.model.AbstractEntityEvent#getEntityUUIDType()
	 */
	@Override
	protected UUIDType getEntityUUIDType() {
		return UUIDType.SYSTEM;
	}

//	/*
//	 * (non-Javadoc)
//	 * 
//	 * @see
//	 * org.iplantc.service.common.model.AbstractEntityEvent#getResolvedEntityUrl
//	 * ()
//	 */
//	@Override
//    protected String getResolvedEntityUrl() {
//    	return TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_SYSTEM_SERVICE) + getEntity();
//    }

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

	public SystemHistoryEvent() {
		setTenantId(TenancyHelper.getCurrentTenantId());
		setUuid(new AgaveUUID(getEntityEventUUIDType()).toString());
		setCreated(new Timestamp(System.currentTimeMillis()));
		setIpAddress(org.iplantc.service.common.Settings.getIpLocalAddress());
	}

	public SystemHistoryEvent(SystemEventType eventType, String description, String createdBy) {
		this();
		setStatus(eventType.name());
		setDescription(description);
		setCreatedBy(createdBy);
	}
	
	public SystemHistoryEvent(String entityUuid, SystemEventType eventType, String createdBy) {
		this(eventType, eventType.getDescription(), createdBy);
		setEntity(entityUuid);setStatus(eventType.name());
	}

	public SystemHistoryEvent(String entityUuid, SystemEventType eventType,
			String description, String createdBy) {
		this(eventType, description, createdBy);
		setEntity(entityUuid);
	}
}
