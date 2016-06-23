package org.iplantc.service.profile;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import org.iplantc.service.profile.resource.impl.InternalUserResourceImpl;
import org.iplantc.service.profile.resource.impl.ProfileResourceImpl;

public class ProfileApplication extends Application 
{	
	/**
	 * @see javax.ws.rs.core.ApplicationConfig#getResourceClasses()
	 */
	@Override
    public Set<Class<?>> getClasses() {
        final Set<Class<?>> rrcs = new  HashSet<Class<?>>();
        // add all the resource beans
        rrcs.add(ProfileResourceImpl.class);
        rrcs.add(InternalUserResourceImpl.class);
        return rrcs;
    }
}

