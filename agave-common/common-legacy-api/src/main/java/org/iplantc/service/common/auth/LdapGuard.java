package org.iplantc.service.common.auth;

import java.util.List;

import org.apache.log4j.Logger;
import org.iplantc.service.common.exceptions.PermissionException;
import org.restlet.Context;
import org.restlet.data.ChallengeScheme;
import org.restlet.data.Method;
import org.restlet.data.Request;

public class LdapGuard extends AbstractGuard {
	
	public LdapGuard(Context context, ChallengeScheme scheme, String realm,
			List<Method> unprotectedMethods) throws IllegalArgumentException
	{
		super(context, scheme, realm, unprotectedMethods);
	}

	private Logger log = Logger.getLogger(LdapGuard.class);
	
	/* (non-Javadoc)
	 * @see org.restlet.Guard#authenticate(org.restlet.data.Request)
	 */
	@Override
	public boolean checkSecret(Request request, String identifier, char[] secret) {
		
		// start running the auth chain starting with iplant community users,
		// on down to teragrid users.
		try {
			log.debug("Authenticating user: " + identifier);

			if (new ProxyUserGuard().checkToken(identifier, secret)) {
			    return true;
			}
			else if (isValidLdapUser(identifier, secret)) {
			    return true;
			}
			
			return false;
		
		} catch (Exception e) {
			log.error("Failed to retrieve proxy: " + e.getMessage());
			return false;
		}
    }

    private boolean isValidLdapUser(String identifier, char[] secret) throws PermissionException {
        LDAPClient client = new LDAPClient(identifier, new String(secret));
        if (!client.login()) {
        	throw new PermissionException("Incorrect username/password combination.");
        } else {
        	return true;
        }
    }
}
