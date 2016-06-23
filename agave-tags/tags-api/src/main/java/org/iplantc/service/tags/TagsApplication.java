package org.iplantc.service.tags;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import org.iplantc.service.tags.resource.impl.TagPermissionResourceImpl;
import org.iplantc.service.tags.resource.impl.TagPermissionsCollectionImpl;
import org.iplantc.service.tags.resource.impl.TagResourceImpl;
import org.iplantc.service.tags.resource.impl.TagResourceResourceImpl;
import org.iplantc.service.tags.resource.impl.TagResourcesCollectionImpl;
import org.iplantc.service.tags.resource.impl.TagsCollectionImpl;


public class TagsApplication extends Application {

	/**
	 * @see javax.ws.rs.core.ApplicationConfig#getResourceClasses()
	 */
	@Override
    public Set<Class<?>> getClasses() {
        final Set<Class<?>> rrcs = new  HashSet<Class<?>>();
        // tag entities
        rrcs.add(TagResourceImpl.class);
        rrcs.add(TagsCollectionImpl.class);
        
        // tag associatedIds
        rrcs.add(TagResourceResourceImpl.class);
        rrcs.add(TagResourcesCollectionImpl.class);
        
        // tag permissions
        rrcs.add(TagPermissionResourceImpl.class);
        rrcs.add(TagPermissionsCollectionImpl.class);
        
        // tag history
//        rrcs.add(TagEventResourceImpl.class);
//        rrcs.add(TagEventsCollectionImpl.class);
        
        
        
        return rrcs;
    }
}
