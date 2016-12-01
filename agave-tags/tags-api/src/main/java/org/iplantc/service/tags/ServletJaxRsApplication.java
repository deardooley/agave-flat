package org.iplantc.service.tags;

import org.iplantc.service.common.Settings;
import org.iplantc.service.common.auth.VerifierFactory;
import org.iplantc.service.common.restlet.AgaveStatusService;
import org.restlet.Context;
import org.restlet.data.ChallengeScheme;
import org.restlet.ext.jaxrs.JaxRsApplication;
import org.restlet.security.ChallengeAuthenticator;
import org.restlet.security.Verifier;

public class ServletJaxRsApplication extends JaxRsApplication 
{
	//private static final Logger log = Logger.getLogger(ServletJaxRsApplication.class);
	
    public ServletJaxRsApplication(Context context) 
    {
        super(context);
        
        add(new TagsApplication());
        setStatusService(new AgaveStatusService());
        
        // add basic auth
  		Verifier verifier = new VerifierFactory().createVerifier(Settings.AUTH_SOURCE);
  		ChallengeAuthenticator guard = new ChallengeAuthenticator(context,
  				 ChallengeScheme.HTTP_BASIC, "The Agave Platform");
  		guard.setVerifier(verifier);
  		
  		guard.setNext(this);
//  		setGuard(guard);
  		setAuthenticator(guard);
    }
}