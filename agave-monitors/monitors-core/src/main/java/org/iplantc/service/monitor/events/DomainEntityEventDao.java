package org.iplantc.service.monitor.events;

import org.iplantc.service.common.dao.AbstractEntityEventDao;
import org.iplantc.service.common.model.AgaveEntityEvent;
import org.iplantc.service.monitor.model.enumeration.MonitorEventType;

/**
 * Concrete impelmentation of the with {@link AbstractEntityEventDao} abstract class
 * handling all historical entity bits. {@link AgaveEntityEvent}s are
 * persisted independently of the parent object for async management.
 * 
 * @author dooley
 * 
 */
public class DomainEntityEventDao extends AbstractEntityEventDao<DomainEntityEvent, MonitorEventType> {
	
	/**
	 * Default no-args constructor
	 */
	public DomainEntityEventDao() {}
	
	@Override
	protected Class<?> getEntityEventClass() {
		return DomainEntityEvent.class;
	}
	
	protected String getTenantFilterName() {
		return "monitorEventTenantFilter";
	}
}
