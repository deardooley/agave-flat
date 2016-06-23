package org.iplantc.service.realtime;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import org.iplantc.service.realtime.resource.impl.RealtimeCollectionImpl;
import org.iplantc.service.realtime.resource.impl.RealtimeResourceImpl;


public class RealtimeApplication extends Application {

	/**
	 * @see javax.ws.rs.core.ApplicationConfig#getResourceClasses()
	 */
	@Override
    public Set<Class<?>> getClasses() {
        final Set<Class<?>> rrcs = new  HashSet<Class<?>>();
        // add all the resource beans
        rrcs.add(RealtimeResourceImpl.class);
        rrcs.add(RealtimeCollectionImpl.class);
        return rrcs;
    }
}
