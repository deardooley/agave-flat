/**
 * 
 */
package org.iplantc.service.common.auth;

import java.util.List;

import org.apache.log4j.Logger;
import org.iplantc.service.common.exceptions.PermissionException;
import org.restlet.Context;
import org.restlet.data.ChallengeScheme;
import org.restlet.data.Method;
import org.restlet.data.Request;

/**
 * @author dooley
 *
 */
public class AuthServiceGuard extends AbstractGuard {
	private static Logger log = Logger.getLogger(AuthServiceGuard.class);
	
	/**
	 * @param context
	 * @param scheme
	 * @param realm
	 * @throws IllegalArgumentException
	 */
	public AuthServiceGuard(Context context, ChallengeScheme scheme,
			String realm, List<Method> unprotectedMethods) throws IllegalArgumentException
	{
		super(context, scheme, realm, unprotectedMethods);
	}
	
	
	/* 
	 * Queries the auth service database for a valid token and places the token, if present
	 * into the Context as auth.token. If an internal user is attached to the token, the
	 * username is included in the context as internal.user for quick access.
	 *
	 * @see org.restlet.Guard#checkSecret(org.restlet.data.Request, java.lang.String, char[])
	 */
	@Override
	public boolean checkSecret(Request request, String identifier, char[] secret) {
		
		try {
			
			AuthServiceClient client = new AuthServiceClient(identifier, new String(secret));
			if (!client.manualLogin()) {
				throw new PermissionException("Incorrect username/password combination.");
			} else {
				request.getAttributes().put("auth.token", client.getAuthToken());
				
				if (client.getAuthToken().getInternalUsername() != null) {
					request.getAttributes().put("internal.user", client.getAuthToken().getInternalUsername());
				}
			}
			
			return true;
			
		} catch (PermissionException e) {
			log.error(e.getMessage());
			return false;
		} catch (Exception e) {
			log.error("Failed to retrieve proxy",e);
			return false;
		}
		
    }
}
