package org.iplantc.service.monitor;

import org.iplantc.service.common.auth.VerifierFactory;
import org.iplantc.service.common.restlet.AgaveStatusService;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.impl.StdSchedulerFactory;
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
        
        SchedulerFactory sf;
		try {
			sf = new StdSchedulerFactory("quartz.properties");
			Scheduler sched = sf.getScheduler();
			sched.start();
		} catch (SchedulerException e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		
        add(new MonitorApplication());
        setStatusService(new AgaveStatusService());
        
        // add basic auth
  		Verifier verifier = new VerifierFactory().createVerifier(Settings.AUTH_SOURCE);
  		ChallengeAuthenticator guard = new ChallengeAuthenticator(context,
  				 ChallengeScheme.HTTP_BASIC, "iPlant Agave API");
  		guard.setVerifier(verifier);
  		
  		guard.setNext(this);
//  		setGuard(guard);
  		setAuthenticator(guard);
    }
}