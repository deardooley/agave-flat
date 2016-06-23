package org.iplantc.service.systems.model.events;

import java.util.Date;

import org.hibernate.event.SaveOrUpdateEvent;
import org.hibernate.event.def.DefaultSaveOrUpdateEventListener;
import org.iplantc.service.systems.model.LastUpdatable;

public class SaveOrUpdateDateListener extends DefaultSaveOrUpdateEventListener {

	private static final long serialVersionUID = -6045152190953427308L;

	@Override
	public void onSaveOrUpdate(SaveOrUpdateEvent event)
	{
		if (event.getObject() instanceof LastUpdatable)
		{
			LastUpdatable record = (LastUpdatable) event.getObject();
			record.setLastUpdated(new Date());
		}
		super.onSaveOrUpdate(event);
	}
}