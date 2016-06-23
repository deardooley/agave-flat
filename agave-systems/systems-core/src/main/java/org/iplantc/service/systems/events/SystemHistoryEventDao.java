package org.iplantc.service.systems.events;

import org.iplantc.service.common.dao.AbstractEntityEventDao;
import org.iplantc.service.common.model.AgaveEntityEvent;
import org.iplantc.service.systems.model.enumerations.SystemEventType;

/**
 * Concrete impelmentation of the with {@link AbstractEntityEventDao} abstract class
 * handling all historical entity bits. {@link AgaveEntityEvent}s are
 * persisted independently of the parent object for async management.
 * 
 * @author dooley
 * 
 */
public class SystemHistoryEventDao extends AbstractEntityEventDao<SystemHistoryEvent, SystemEventType> {
	
	/**
	 * Default no-args constructor
	 */
	public SystemHistoryEventDao() {}
	
	@Override
	protected Class<?> getEntityEventClass() {
		return SystemHistoryEvent.class;
	}
	
//	@Override
//	protected String getTenantFilterName() {
//		return "HistoryTenantFilter";
//	}
}
