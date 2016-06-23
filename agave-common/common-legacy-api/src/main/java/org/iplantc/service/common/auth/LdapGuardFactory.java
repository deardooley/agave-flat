package org.iplantc.service.common.auth;

import java.util.List;

import org.restlet.Context;
import org.restlet.Guard;
import org.restlet.data.ChallengeScheme;
import org.restlet.data.Method;

public class LdapGuardFactory implements GuardFactory 
{
	/* (non-Javadoc)
	 * @see org.iplantc.service.apps.auth.GuardFactory#createGuard(org.restlet.Context, org.restlet.data.ChallengeScheme, java.lang.String, java.util.List)
	 */
	public Guard createGuard(Context context, ChallengeScheme scheme,
			String realm, List<Method> unprotectedMethods)
	{
		return new LdapGuard(context, scheme, realm, unprotectedMethods);
	}
}
