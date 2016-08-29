/**
 * 
 */
package org.iplantc.service.common.restlet;

import java.util.List;

import org.iplantc.service.common.Settings;
import org.iplantc.service.common.auth.AuthServiceGuardFactory;
import org.iplantc.service.common.auth.GuardFactory;
import org.iplantc.service.common.auth.JWTGuardFactory;
import org.iplantc.service.common.auth.LdapGuardFactory;
import org.iplantc.service.common.auth.MyProxyGuardFactory;
import org.iplantc.service.common.auth.NullGuardFactory;
import org.iplantc.service.common.resource.RuntimeConfigurationResource;
import org.restlet.Application;
import org.restlet.Component;
import org.restlet.Guard;
import org.restlet.Restlet;
import org.restlet.Router;
import org.restlet.Server;
import org.restlet.data.ChallengeScheme;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Protocol;
import org.restlet.ext.jetty.AjpServerHelper;
import org.restlet.ext.jetty.HttpServerHelper;
import org.restlet.ext.jetty.JettyServerHelper;
import org.restlet.resource.Resource;

/**
 * Parent class for all Agave legacy services
 * 
 * @author dooley
 *
 */
public abstract class AgaveApplication extends Application
{
	protected GuardFactory guardFactory;
	
	private boolean standaloneMode = false;
	
	public AgaveApplication() 
    {
        super();
        setStatusService(new IPlantStatusService());
        getMetadataService().setDefaultMediaType(MediaType.APPLICATION_JSON);
    }
	
	/**
	 * Prefix to be prepended to all routes when running in standalone mode.
	 * Since the application will run as the root resource in standalone, this
	 * is needed to make the routes match the production services routes.
	 *  
	 * @return route prefix
	 */
	abstract protected String getStandalonePrefix();

	/**
	 * Map all the routes in this class by attaching them to via the secureEndpoint methods.
	 * 
	 * @param router
	 */
	abstract protected void mapServiceEndpoints(Router router);

	protected void setStandaloneMode(boolean standaloneMode)
	{
		this.standaloneMode = standaloneMode;
	}
		
	protected boolean isStandaloneMode() {
		return standaloneMode;
	}
	    
	/** 
     * Creates a root Restlet that will receive all incoming calls. 
     */  
    @Override  
    public synchronized Restlet createRoot() 
    {
    	
    	if ("none".equals(Settings.AUTH_SOURCE))
		{
			guardFactory = new NullGuardFactory();
		}
		else if ("ldap".equals(Settings.AUTH_SOURCE))
		{
			guardFactory = new LdapGuardFactory();
		}
		else if ("ldap+tacc".equals(Settings.AUTH_SOURCE))
		{
			guardFactory = new MyProxyGuardFactory();
		} 
		else if ("api".equals(Settings.AUTH_SOURCE)) 
		{
			guardFactory = new AuthServiceGuardFactory();
		} 
		else if ("wso2".equals(Settings.AUTH_SOURCE)) 
		{
			guardFactory = new JWTGuardFactory();
		}
    	
    	// Create a router Restlet that routes each call to a  
 		// new instance of HelloWorldResource.  
    	Router router = new Router(getContext());
    	
    	secureEndpoint(router, "/runtimes", RuntimeConfigurationResource.class);
        secureEndpoint(router, "/runtimes/environment", RuntimeConfigurationResource.class);
        secureEndpoint(router, "/runtimes/configuration", RuntimeConfigurationResource.class);
        secureEndpoint(router, "/runtimes/container", RuntimeConfigurationResource.class);
        
        mapServiceEndpoints(router);
        
        
        return router;
    }
    
    protected void secureEndpoint(Router router, String path,
			Class<? extends Resource> targetResource)
	{
		secureEndpoint(router, path, targetResource, null);
	}
	
	protected void secureEndpoint(Router router, String path,
			Class<? extends Resource> targetResource, List<Method> unprotectedMethods)
	{
		Guard guard = guardFactory.createGuard(getContext(),
				ChallengeScheme.HTTP_BASIC, "The Agave Platform", unprotectedMethods);

		if (guard != null)
		{
			router.attach(getStandalonePrefix() + path, guard);
			guard.setNext(targetResource);
		}
		else
		{
			router.attach(getStandalonePrefix() + path, targetResource);
		}
	}
	
	protected static void launchServer(Component component) throws Exception 
	{	
		 // create embedding jetty server
        Server embedingJettyServer = new Server(
	        component.getContext().createChildContext(),
	        Protocol.HTTP,
	        Settings.JETTY_PORT,
	        component
        );
        
        //construct and start JettyServerHelper
        JettyServerHelper jettyServerHelper = new HttpServerHelper(embedingJettyServer);
        jettyServerHelper.start();

        //create embedding AJP Server
        Server embedingJettyAJPServer=new Server(
            component.getContext(),
            Protocol.HTTP,
            Settings.JETTY_AJP_PORT,
            component
        );

        //construct and start AjpServerHelper
        AjpServerHelper ajpServerHelper = new AjpServerHelper(embedingJettyAJPServer);
        ajpServerHelper.start();
	}
	
	
}
