/**
 * 
 */
package org.iplantc.service.transfer.dao;

import org.iplantc.service.common.dao.AbstractEntityEventDao;
import org.iplantc.service.common.model.AgaveEntityEvent;
import org.iplantc.service.transfer.model.TransferTaskEvent;
import org.iplantc.service.transfer.model.enumerations.TransferTaskEventType;

/**
 * Concrete implementation of the with {@link AbstractEntityEventDao} abstract class
 * handling all historical entity bits. {@link AgaveEntityEvent}s are
 * persisted independently of the parent object for async management.
 * 
 * @author dooley
 * 
 */
public class TransferTaskEventDao extends AbstractEntityEventDao<TransferTaskEvent, TransferTaskEventType> {
	
	/**
	 * Default no-args constructor
	 */
	public TransferTaskEventDao() {}
	
	@Override
	protected Class<?> getEntityEventClass() {
		return TransferTaskEvent.class;
	}
	
	protected String getTenantFilterName() {
		return "transferTaskEventTenantFilter";
	}
}