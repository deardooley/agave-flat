package org.iplantc.service.monitor;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import org.iplantc.service.monitor.resources.impl.EntityEventCollectionImpl;
import org.iplantc.service.monitor.resources.impl.EntityEventResourceImpl;
import org.iplantc.service.monitor.resources.impl.MonitorCheckCollectionImpl;
import org.iplantc.service.monitor.resources.impl.MonitorCheckResourceImpl;
import org.iplantc.service.monitor.resources.impl.MonitorCollectionImpl;
import org.iplantc.service.monitor.resources.impl.MonitorResourceImpl;
import org.iplantc.service.monitor.resources.impl.QuartzResourceImpl;


public class MonitorApplication extends Application {

	/**
	 * @see javax.ws.rs.core.ApplicationConfig#getResourceClasses()
	 */
	@Override
    public Set<Class<?>> getClasses() {
        final Set<Class<?>> rrcs = new  HashSet<Class<?>>();
        // add all the resource beans
        rrcs.add(MonitorResourceImpl.class);
        rrcs.add(MonitorCheckResourceImpl.class);
        rrcs.add(MonitorCollectionImpl.class);
        rrcs.add(MonitorCheckCollectionImpl.class);
        rrcs.add(EntityEventCollectionImpl.class);
        rrcs.add(EntityEventResourceImpl.class);
        rrcs.add(QuartzResourceImpl.class);
        return rrcs;
    }
}
