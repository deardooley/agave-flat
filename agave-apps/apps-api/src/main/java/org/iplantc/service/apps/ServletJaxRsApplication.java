package org.iplantc.service.apps;

import org.iplantc.service.common.auth.VerifierFactory;
import org.iplantc.service.common.restlet.AgaveStatusService;
import org.restlet.Context;
import org.restlet.data.ChallengeScheme;
import org.restlet.ext.jaxrs.JaxRsApplication;
import org.restlet.security.ChallengeAuthenticator;
import org.restlet.security.Verifier;

public class ServletJaxRsApplication extends JaxRsApplication {

    public ServletJaxRsApplication(Context context) 
    {
        super(context);
        
        // add basic auth
  		Verifier verifier = new VerifierFactory().createVerifier(Settings.AUTH_SOURCE);
  		ChallengeAuthenticator authenticator = new ChallengeAuthenticator(context,
  				 ChallengeScheme.HTTP_BASIC, "The Agave Platform");
  		authenticator.setVerifier(verifier);
  		
  		this.add(new SoftwareApplication());
        this.setStatusService(new AgaveStatusService());

        
  		authenticator.setNext(this);
  		this.setAuthenticator(authenticator);
        
    }
}