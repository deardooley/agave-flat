package org.iplantc.service.systems.model.events;

import java.util.Date;

import org.hibernate.event.MergeEvent;
import org.hibernate.event.def.DefaultMergeEventListener;
import org.iplantc.service.systems.model.LastUpdatable;

public class MergeListener extends DefaultMergeEventListener {

	private static final long serialVersionUID = 3360666288040532875L;

	@Override
	public void onMerge(MergeEvent event)
	{
		if (event.getOriginal() instanceof LastUpdatable)
		{
			LastUpdatable record = (LastUpdatable) event.getOriginal();
			record.setLastUpdated(new Date());
		}
		super.onMerge(event);
	}
}