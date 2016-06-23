package org.iplantc.service.monitor;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import org.iplantc.service.common.auth.VerifierFactory;
import org.iplantc.service.common.persistence.JndiSetup;
import org.iplantc.service.common.restlet.AgaveServerApplication;
import org.iplantc.service.common.restlet.AgaveStatusService;
import org.iplantc.service.monitor.resources.impl.EntityEventCollectionImpl;
import org.iplantc.service.monitor.resources.impl.EntityEventResourceImpl;
import org.iplantc.service.monitor.resources.impl.MonitorCheckCollectionImpl;
import org.iplantc.service.monitor.resources.impl.MonitorCheckResourceImpl;
import org.iplantc.service.monitor.resources.impl.MonitorCollectionImpl;
import org.iplantc.service.monitor.resources.impl.MonitorResourceImpl;
import org.iplantc.service.monitor.resources.impl.QuartzResourceImpl;
import org.restlet.Component;
import org.restlet.data.ChallengeScheme;
import org.restlet.ext.jaxrs.JaxRsApplication;
import org.restlet.security.ChallengeAuthenticator;
import org.restlet.security.Verifier;

public class RestletServerApplication extends AgaveServerApplication
{
	public static void main(String[] args) throws Exception 
	{
		JndiSetup.init();
		
        // create Component (as ever for Restlet)
        Component component = new Component();
        
        // create JAX-RS runtime environment
        JaxRsApplication application = new JaxRsApplication(component.getContext().createChildContext());
        application.add(new Application() {
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
        });
        
        application.setStatusService(new AgaveStatusService());
        
        // add basic auth
  		Verifier verifier = new VerifierFactory().createVerifier(Settings.AUTH_SOURCE);
  		ChallengeAuthenticator guard = new ChallengeAuthenticator(application.getContext().createChildContext(),
  				 ChallengeScheme.HTTP_BASIC, "The Agave Platform");
  		guard.setVerifier(verifier);
  		guard.setNext(application);
  		
//  		application.setAuthenticator(guard);
  		
  		component.getDefaultHost().attach(guard);

  		launchServer(component);
    }
}