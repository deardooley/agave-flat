package org.iplantc.service.apps;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import org.iplantc.service.apps.resources.impl.StandaloneQuartzResourceImpl;
import org.iplantc.service.apps.resources.impl.StandaloneSoftwareCollectionImpl;
import org.iplantc.service.apps.resources.impl.StandaloneSoftwareFormResourceImpl;
import org.iplantc.service.apps.resources.impl.StandaloneSoftwareHistoryCollectionImpl;
import org.iplantc.service.apps.resources.impl.StandaloneSoftwareHistoryResourceImpl;
import org.iplantc.service.apps.resources.impl.StandaloneSoftwarePermissionCollectionImpl;
import org.iplantc.service.apps.resources.impl.StandaloneSoftwarePermissionResourceImpl;
import org.iplantc.service.apps.resources.impl.StandaloneSoftwareResourceImpl;
import org.iplantc.service.common.auth.VerifierFactory;
import org.iplantc.service.common.persistence.JndiSetup;
import org.iplantc.service.common.restlet.AgaveServerApplication;
import org.iplantc.service.common.restlet.AgaveStatusService;
import org.restlet.Component;
import org.restlet.data.ChallengeScheme;
import org.restlet.ext.jaxrs.JaxRsApplication;
import org.restlet.security.ChallengeAuthenticator;
import org.restlet.security.MethodAuthorizer;
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
                rrcs.add(StandaloneSoftwareResourceImpl.class);
                rrcs.add(StandaloneSoftwareCollectionImpl.class);
                rrcs.add(StandaloneSoftwarePermissionResourceImpl.class);
                rrcs.add(StandaloneSoftwarePermissionCollectionImpl.class);
                rrcs.add(StandaloneSoftwareHistoryResourceImpl.class);
                rrcs.add(StandaloneSoftwareHistoryCollectionImpl.class);
                rrcs.add(StandaloneSoftwareFormResourceImpl.class);
                rrcs.add(StandaloneQuartzResourceImpl.class);
                return rrcs;
            }
        });
        
        application.setStatusService(new AgaveStatusService());
        
        // add basic auth
  		Verifier verifier = new VerifierFactory().createVerifier(Settings.AUTH_SOURCE);
  		ChallengeAuthenticator guard = new ChallengeAuthenticator(application.getContext().createChildContext(),
  				 ChallengeScheme.CUSTOM, "The Agave Platform");
  		
  		guard.setVerifier(verifier);
  		
  		MethodAuthorizer ma = createMethodAuthorizer();
        guard.setNext(ma);
        
  		guard.setNext(application);
  		
//  		application.setAuthenticator(guard);
  		
  		component.getDefaultHost().attach(guard);

  		launchServer(component);
  		
    }

    private static MethodAuthorizer createMethodAuthorizer() {
        // TODO Auto-generated method stub
        return null;
    }
}