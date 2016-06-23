package org.iplantc.service.notification;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import org.iplantc.service.common.restlet.resource.QuartzUtilityResource;
import org.iplantc.service.notification.resources.impl.FireNotificationResourceImpl;
import org.iplantc.service.notification.resources.impl.NotificationAttemptCollectionImpl;
import org.iplantc.service.notification.resources.impl.NotificationAttemptResourceImpl;
import org.iplantc.service.notification.resources.impl.NotificationCollectionImpl;
import org.iplantc.service.notification.resources.impl.NotificationResourceImpl;
import org.iplantc.service.notification.resources.impl.QuartzResourceImpl;


public class NotificationApplication extends Application {

	/**
	 * @see javax.ws.rs.core.ApplicationConfig#getResourceClasses()
	 */
	@Override
    public Set<Class<?>> getClasses() {
        final Set<Class<?>> rrcs = new  HashSet<Class<?>>();
        // add all the resource beans
        rrcs.add(NotificationResourceImpl.class);
        rrcs.add(NotificationCollectionImpl.class);
        rrcs.add(NotificationAttemptResourceImpl.class);
        rrcs.add(NotificationAttemptCollectionImpl.class);
        rrcs.add(FireNotificationResourceImpl.class);
        rrcs.add(QuartzResourceImpl.class);
        rrcs.add(QuartzUtilityResource.class);
        return rrcs;
    }
}
