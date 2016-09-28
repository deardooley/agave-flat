package org.iplantc.service.notification;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import org.iplantc.service.common.auth.VerifierFactory;
import org.iplantc.service.common.persistence.HibernateUtil;
import org.iplantc.service.common.persistence.JndiSetup;
import org.iplantc.service.common.representation.AgaveErrorRepresentation;
import org.iplantc.service.common.representation.AgaveRepresentation;
import org.iplantc.service.common.representation.AgaveSuccessRepresentation;
import org.iplantc.service.common.restlet.AgaveServerApplication;
import org.iplantc.service.common.restlet.AgaveStatusService;
import org.iplantc.service.common.restlet.resource.QuartzUtilityResource;
import org.iplantc.service.notification.resources.impl.FireNotificationResourceImpl;
import org.iplantc.service.notification.resources.impl.NotificationAttemptCollectionImpl;
import org.iplantc.service.notification.resources.impl.NotificationAttemptResourceImpl;
import org.iplantc.service.notification.resources.impl.NotificationCollectionImpl;
import org.iplantc.service.notification.resources.impl.NotificationResourceImpl;
import org.iplantc.service.notification.resources.impl.QuartzResourceImpl;
import org.restlet.Component;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.ChallengeScheme;
import org.restlet.data.Status;
import org.restlet.ext.jaxrs.JaxRsApplication;
import org.restlet.representation.Representation;
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
                rrcs.add(NotificationResourceImpl.class);
                rrcs.add(NotificationCollectionImpl.class);
                rrcs.add(NotificationAttemptResourceImpl.class);
                rrcs.add(NotificationAttemptCollectionImpl.class);
//                rrcs.add(FireNotificationResourceImpl.class);
                rrcs.add(QuartzResourceImpl.class);
                rrcs.add(QuartzUtilityResource.class);
                return rrcs;
            }
        });
        
        application.setStatusService(new AgaveStatusService() {
        	/* (non-Javadoc)
        	 * @see org.restlet.service.StatusService#getStatus(java.lang.Throwable, org.restlet.data.Request, org.restlet.data.Response)
        	 */
        	@Override
        	public Status getStatus(Throwable throwable, Request request,
        			Response response)
        	{
        		return response.getStatus();
        	}
        	
        	/* (non-Javadoc)
        	 * @see org.restlet.service.StatusService#getRepresentation(org.restlet.data.Status, org.restlet.data.Request, org.restlet.data.Response)
        	 */
        	@Override
        	public Representation getRepresentation(Status status,
        			Request request, Response response)
        	{
        		try {
        			Representation currentRepresentation = response.getEntity();
        			
        			if (currentRepresentation instanceof AgaveRepresentation) {
        				return currentRepresentation;
        			} else if (status.isSuccess()) {
        				return new AgaveSuccessRepresentation();
        			} else {
        				String message = null;
        				if (status.getCode() == 401) {
        					if (request.getChallengeResponse() == null) {
        						message = "Permission denied. This resource requires authentication.";
        					} else {
        						message = "Invalid username/password combination";
        					}
        				} else {
        					message = status.getDescription();
        				}
        				return new AgaveErrorRepresentation(message);
        			}
        		} finally {
        			try { HibernateUtil.closeSession(); } catch(Exception e) {}
        		}
        	}
        });
        
        // add basic auth
  		Verifier verifier = new VerifierFactory().createVerifier(Settings.AUTH_SOURCE);
  		ChallengeAuthenticator guard = new ChallengeAuthenticator(application.getContext().createChildContext(),
  				 ChallengeScheme.HTTP_BASIC, "The Agave Platform");
  		guard.setVerifier(verifier);
  		guard.setNext(application);
  		
  		component.getDefaultHost().attach(guard);

  		launchServer(component);
  		
    }
}