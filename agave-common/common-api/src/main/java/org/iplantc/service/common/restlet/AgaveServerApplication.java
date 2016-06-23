package org.iplantc.service.common.restlet;

import org.iplantc.service.common.Settings;
import org.restlet.Component;
import org.restlet.Server;
import org.restlet.data.Protocol;
import org.restlet.ext.jetty.HttpServerHelper;
import org.restlet.ext.jetty.JettyServerHelper;

public class AgaveServerApplication
{
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

    }
}