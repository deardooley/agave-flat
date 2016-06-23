package org.iplantc.service.apps;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import org.iplantc.service.apps.resources.impl.QuartzResourceImpl;
import org.iplantc.service.apps.resources.impl.SoftwareCollectionImpl;
import org.iplantc.service.apps.resources.impl.SoftwareFormResourceImpl;
import org.iplantc.service.apps.resources.impl.SoftwareHistoryCollectionImpl;
import org.iplantc.service.apps.resources.impl.SoftwareHistoryResourceImpl;
import org.iplantc.service.apps.resources.impl.SoftwarePermissionCollectionImpl;
import org.iplantc.service.apps.resources.impl.SoftwarePermissionResourceImpl;
import org.iplantc.service.apps.resources.impl.SoftwareResourceImpl;


public class SoftwareApplication extends Application {

	/**
	 * @see javax.ws.rs.core.ApplicationConfig#getResourceClasses()
	 */
	@Override
    public Set<Class<?>> getClasses() {
        final Set<Class<?>> rrcs = new HashSet<Class<?>>();
        // add all the resource beans
        rrcs.add(SoftwareResourceImpl.class);
        rrcs.add(SoftwareCollectionImpl.class);
        rrcs.add(SoftwarePermissionResourceImpl.class);
        rrcs.add(SoftwarePermissionCollectionImpl.class);
        rrcs.add(SoftwareHistoryResourceImpl.class);
        rrcs.add(SoftwareHistoryCollectionImpl.class);
        rrcs.add(SoftwareFormResourceImpl.class);
        rrcs.add(QuartzResourceImpl.class);
        return rrcs;
    }
}
