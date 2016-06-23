package org.iplantc.service.common.auth;

import static org.iplantc.service.common.auth.roles.AgaveRole.ROLE_NONE;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.Form;
import org.restlet.data.Method;
import org.restlet.security.Role;
import org.restlet.security.Verifier;
import org.restlet.util.Series;

public class JWTVerifier implements Verifier {
    private static final Logger log = Logger.getLogger(JWTVerifier.class);
    private static final String JWT_HEADER_NAME = "x-jwt-assertion";
    private static final String INTERNALUSER_HEADER_NAME = "x-agave-internaluser";
    
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public int verify(Request request, Response response)
    {
        if (request.getMethod().equals(Method.OPTIONS)) return Verifier.RESULT_VALID;
        
        Series series = (Series)request.getAttributes().get("org.restlet.http.headers");
        for (String key: (Set<String>)series.getNames()) 
        {
            if (key.toLowerCase().startsWith(JWT_HEADER_NAME.toLowerCase())) 
            {
                String jwtHeader = (String)series.getFirstValue(key);
                
                Form queryForm = (Form)request.getOriginalRef().getQueryAsForm();
                if (queryForm != null && !queryForm.isEmpty()) {
                    String debugJWT = queryForm.getFirstValue("debugjwt");
                    if (!StringUtils.isEmpty(debugJWT)) {
                        log.debug(key + " : " + jwtHeader);
                    }
                }
                
                String tenantId = key.toLowerCase().substring(JWT_HEADER_NAME.length() + 1);
                List<Role> roles = new CopyOnWriteArrayList<Role>();
                
                if (JWTClient.parse(jwtHeader, tenantId)) 
                {
                    String bearerToken = (String)series.getFirstValue("Authorization");
                    if (StringUtils.isNotEmpty(bearerToken)) {
                        bearerToken = StringUtils.removeStart(bearerToken, "Bearer");
                        bearerToken = StringUtils.strip(bearerToken);
                        JWTClient.setCurrentBearerToken(bearerToken);
                    }
                    
//                  log.debug(JWTClient.getCurrentJWSObject().toString());
                    return Verifier.RESULT_VALID;
                } 
                else 
                {
                    roles.add(ROLE_NONE);
                    request.getClientInfo().setRoles(roles);
                    
                    return Verifier.RESULT_INVALID;
                }
                
            }
            else if (key.toLowerCase().startsWith(INTERNALUSER_HEADER_NAME.toLowerCase())) 
            {
                String internalUsername = (String)series.getFirstValue(key);
                Request.getCurrent().getAttributes().put("internal.user", internalUsername);
            }
        }
        
        log.error("No " + JWT_HEADER_NAME + " header found in request. Authentication failed.");
        return Verifier.RESULT_MISSING;
    }
}
