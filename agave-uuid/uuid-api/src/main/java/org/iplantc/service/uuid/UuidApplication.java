package org.iplantc.service.uuid;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import org.iplantc.service.uuid.resource.impl.UuidResourceImpl;
import org.iplantc.service.uuid.resource.impl.UuidCollectionImpl;


public class UuidApplication extends Application {

	/**
	 * @see javax.ws.rs.core.ApplicationConfig#getResourceClasses()
	 */
	@Override
    public Set<Class<?>> getClasses() {
        final Set<Class<?>> rrcs = new  HashSet<Class<?>>();
        // tag entities
        rrcs.add(UuidResourceImpl.class);
        rrcs.add(UuidCollectionImpl.class);

        return rrcs;
    }
}
