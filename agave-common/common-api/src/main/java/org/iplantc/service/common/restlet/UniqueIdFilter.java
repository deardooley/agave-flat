package org.iplantc.service.common.restlet;


import org.apache.commons.lang.StringUtils;
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

    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) 
    throws IOException, ServletException 
    {
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
        chain.doFilter(req, resp);

    }

}
