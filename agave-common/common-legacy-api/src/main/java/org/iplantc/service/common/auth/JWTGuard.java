/**
 * 
 */
package org.iplantc.service.common.auth;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.restlet.Context;
import org.restlet.Guard;
import org.restlet.data.ChallengeScheme;
import org.restlet.data.Form;
import org.restlet.data.Method;
import org.restlet.data.Request;

/**
 * @author rion1
 *
 */
public class JWTGuard extends AbstractGuard {

	private static final Logger log = Logger.getLogger(JWTGuard.class);
	private static final String JWT_HEADER_NAME = "x-jwt-assertion";
	private static final String INTERNALUSER_HEADER_NAME = "x-agave-internaluser";
	
	/**
	 * @param context
	 * @param scheme
	 * @param realm
	 * @throws IllegalArgumentException
	 */
	public JWTGuard(Context context, ChallengeScheme scheme, String realm,
			List<Method> unprotectedMethods) throws IllegalArgumentException
	{
		super(context, scheme, realm, unprotectedMethods);
	}
	
	public boolean checkSecret(Request request, String identifier, char[] secret) 
	{
		Form headers = (Form)request.getAttributes().get("org.restlet.http.headers");
		for (String key: headers.getNames()) 
		{
			if (key.toLowerCase().startsWith(JWT_HEADER_NAME.toLowerCase())) 
			{
				String jwtHeader = (String)headers.getFirstValue(key);
				
				String tenantId = key.toLowerCase().substring(JWT_HEADER_NAME.length() + 1);
				
				return JWTClient.parse(jwtHeader, tenantId);
			}
			else if (key.toLowerCase().startsWith(INTERNALUSER_HEADER_NAME.toLowerCase())) 
			{
				String internalUsername = (String)headers.getFirstValue(key);
				Request.getCurrent().getAttributes().put("internal.user", internalUsername);
			}
		}
		
		return false;
	}
	
	@Override
	public int authenticate(Request request)
	{
		if (request.getMethod().equals(Method.OPTIONS)) {
			return Guard.AUTHENTICATION_VALID;
		} 
		else 
		{
			Form headers = (Form)request.getAttributes().get("org.restlet.http.headers");
			for (String key: headers.getNames()) 
			{
				if (key.toLowerCase().startsWith(JWT_HEADER_NAME.toLowerCase())) 
				{
					
					String jwtHeader = (String)headers.getFirstValue(key);
					
					Form queryForm = (Form)request.getOriginalRef().getQueryAsForm();
					if (queryForm != null && !queryForm.isEmpty()) {
						String debugJWT = queryForm.getFirstValue("debugjwt");
						if (!StringUtils.isEmpty(debugJWT)) {
							log.debug(key + " : " + jwtHeader);
						}
					}
					
					String tenantId = null;
					if (StringUtils.equalsIgnoreCase(key, JWT_HEADER_NAME)) {
						tenantId = null;
					} else {
						tenantId = key.toLowerCase().substring(JWT_HEADER_NAME.length() + 1);
					}
					try {
						if (JWTClient.parse(jwtHeader, tenantId)) {
	                        
	                        String bearerToken = headers.getFirstValue("Authorization");
	                        if (StringUtils.isNotEmpty(bearerToken)) {
	                            bearerToken = StringUtils.removeStart(bearerToken, "Bearer");
	                            bearerToken = StringUtils.strip(bearerToken);
	                            JWTClient.setCurrentBearerToken(bearerToken);
	                        }
	                        
	                        return Guard.AUTHENTICATION_VALID;
	                    } else {
	                        return Guard.AUTHENTICATION_INVALID;
	                    }
					} catch (Exception e) {
						log.error("Invalid JWT header presented for tenant " + tenantId);
					}
					
				}
				else if (key.toLowerCase().startsWith(INTERNALUSER_HEADER_NAME.toLowerCase())) 
				{
					String internalUsername = (String)headers.getFirstValue(key);
					Request.getCurrent().getAttributes().put("internal.user", internalUsername);
				}
			}
			
			log.error("No " + JWT_HEADER_NAME + " header found in request. Authentication failed.");
			return Guard.AUTHENTICATION_MISSING;
		}
	}
	
}
