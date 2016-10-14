/**
 * Entity class for persisting transfer task events. This creates a history log 
 * per transfer that can be queried and/or mined 
 * 
 * @author dooley
 *
 */
package org.iplantc.service.transfer.model;

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
import org.iplantc.service.transfer.model.enumerations.TransferTaskEventType;

/**
 * Concrete {@link AgaveEntityEvent} class for {@link Transfer}
 * 
 * @author dooley
 *
 */
@Entity
@Table(name = "transfertaskevents")
@FilterDef(name = "transferTaskEventTenantFilter", parameters = @ParamDef(name = "tenantId", type = "string"))
@Filters(@Filter(name = "transferTaskEventTenantFilter", condition = "tenant_id=:tenantId"))
public class TransferTaskEvent extends AbstractEntityEvent<TransferTaskEventType> {

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.iplantc.service.common.model.AbstractEntityEvent#getEntityEventUUIDType
	 * ()
	 */
	@Override
	protected UUIDType getEntityEventUUIDType() {
		return UUIDType.TRANSFER_EVENT;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.iplantc.service.common.model.AbstractEntityEvent#getEntityUUIDType()
	 */
	@Override
	protected UUIDType getEntityUUIDType() {
		return UUIDType.TRANSFER;
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
    	return TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_TRANSFER_SERVICE) + getEntity();
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

	public TransferTaskEvent() {
		setTenantId(TenancyHelper.getCurrentTenantId());
		setUuid(new AgaveUUID(getEntityEventUUIDType()).toString());
		setCreated(new Timestamp(System.currentTimeMillis()));
		setIpAddress(org.iplantc.service.common.Settings.getIpLocalAddress());
	}

	public TransferTaskEvent(TransferTaskEventType eventType, String description, String createdBy) {
		this();
		setStatus(eventType.name());
		setDescription(description);
		setCreatedBy(createdBy);
	}
	
	public TransferTaskEvent(String entityUuid, TransferTaskEventType eventType, String createdBy) {
		this(eventType, eventType.getDescription(), createdBy);
		setEntity(entityUuid);setStatus(eventType.name());
	}


	public TransferTaskEvent(String entityUuid, TransferTaskEventType eventType,
			String description, String createdBy) {
		this(eventType, description, createdBy);
		setEntity(entityUuid);
	}
}
