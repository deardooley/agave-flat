package org.iplantc.service.common.restlet;


import org.apache.commons.lang.StringUtils;
import org.iplantc.service.common.uuid.AgaveUUID;
import org.iplantc.service.common.uuid.UUIDType;
import org.slf4j.MDC;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;

import java.io.IOException;

/**
 * Created by wcs on 7/17/14.
 */
public class UniqueIdFilter implements Filter {


    public void destroy() {
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException, ServletException {
        if (req instanceof HttpServletRequest) {
            final HttpServletRequest httpRequest = (HttpServletRequest)req;
            final String requestId = httpRequest.getHeader("UNIQUE_ID");
            if (StringUtils.isNotEmpty(requestId)) {
                MDC.put("UNIQUE_ID", requestId);
            } else {
                MDC.put("UNIQUE_ID", "none");
            }
        } else {
            MDC.put("UNIQUE_ID", "none");
        }
        
//        final String hostname = System.getenv("HOSTNAME");
//        if (StringUtils.isNotEmpty(hostname)) {
//        	MDC.put("HOSTNAME", hostname);
//        } else {
//        	MDC.put("HOSTNAME", "docker.example.com");
//        }
//        
//        final String containerId = System.getenv("CONTAINER_ID");
//        if (StringUtils.isNotEmpty(containerId)) {
//        	MDC.put("CONTAINER_ID", containerId);
//        } else {
//        	// use the second token in the uuid. This is a unique identifier for a container per system.
//        	AgaveUUID uuid = new AgaveUUID(UUIDType.APP);
//        	MDC.put("CONTAINER_ID", StringUtils.split(uuid.toString())[1]);
//        }
        
        chain.doFilter(req, resp);

    }

}
