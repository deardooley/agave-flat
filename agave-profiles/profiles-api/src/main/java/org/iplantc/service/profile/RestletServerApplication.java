package org.iplantc.service.profile;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import org.iplantc.service.common.auth.VerifierFactory;
import org.iplantc.service.common.persistence.JndiSetup;
import org.iplantc.service.common.restlet.AgaveServerApplication;
import org.iplantc.service.common.restlet.AgaveStatusService;
import org.iplantc.service.profile.resource.impl.StandaloneInternalUserResourceImpl;
import org.iplantc.service.profile.resource.impl.StandaloneProfileResourceImpl;
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
                rrcs.add(StandaloneProfileResourceImpl.class);
                rrcs.add(StandaloneInternalUserResourceImpl.class);
                return rrcs;
            }
        });
        
        application.setStatusService(new AgaveStatusService());
        
        // add basic auth
  		Verifier verifier = new VerifierFactory().createVerifier(Settings.AUTH_SOURCE);
  		ChallengeAuthenticator guard = new ChallengeAuthenticator(application.getContext(),
  				 ChallengeScheme.HTTP_BASIC, "iPlant Agave API");
  		guard.setVerifier(verifier);
  		
  		guard.setNext(application);
  		
  		component.getDefaultHost().attach(guard);

  		launchServer(component);
    }
}