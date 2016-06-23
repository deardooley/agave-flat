package org.iplantc.service.realtime;

import org.iplantc.service.common.Settings;
import org.iplantc.service.realtime.WebApiApplication;
import org.restlet.Component;
import org.restlet.data.Protocol;

public class WebApiHost {

    public static void main(String[] args) throws Exception {

        // Attach application to http://localhost:9001/v1
        Component c = new Component();
        c.getServers().add(Protocol.HTTP, Settings.JETTY_PORT);
        c.getDefaultHost().attach("/", new WebApiApplication());
        c.start();
    }
}