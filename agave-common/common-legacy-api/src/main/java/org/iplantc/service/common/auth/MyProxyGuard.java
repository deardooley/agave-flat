package org.iplantc.service.common.auth;

import java.util.Hashtable;
import java.util.List;

import org.apache.log4j.Logger;
import org.globus.myproxy.MyProxy;
import org.ietf.jgss.GSSCredential;
import org.iplantc.service.common.exceptions.PermissionException;
import org.iplantc.service.common.Settings;
import org.restlet.Context;
import org.restlet.data.ChallengeScheme;
import org.restlet.data.Method;
import org.restlet.data.Request;

public class MyProxyGuard extends AbstractGuard 
{
	private static Logger log = Logger.getLogger(MyProxyGuard.class);
	
	private static Hashtable<String, GSSCredential> proxyCache = new Hashtable<String, GSSCredential>();
	
	public MyProxyGuard(Context context, ChallengeScheme scheme, String realm, List<Method> unprotectedMethods)
			throws IllegalArgumentException {
		
		super(context, scheme, realm, unprotectedMethods);
		
	}

	/* (non-Javadoc)
	 * @see org.restlet.Guard#authenticate(org.restlet.data.Request)
	 */
	@Override
	public boolean checkSecret(Request request, String identifier, char[] secret) 
	{
		try {
			
			// are they a valid ldap entry?
			boolean isProxyUser = new ProxyUserGuard().checkToken(identifier, secret);
			
			if (!isProxyUser) {
				LDAPClient client = new LDAPClient(identifier, new String(secret));
				if (!client.login()) {
					throw new PermissionException("Incorrect username/password combination.");
				}
			} else {
				return false;
			}
			
			// pull a community proxy from the tacc myproxy server
			GSSCredential cred = getCommunityCredential();
			
			// now markup the community proxy with the user attributes
			log.debug("Issued attribute-added proxy");
			
			// add the user's dn to their session context
			getContext().getAttributes().put("username", identifier);
			getContext().getAttributes().put("proxy", cred);
			
			//log.debug("User " + identifier + " successfully logged in.");
			return true;
			
		} catch (PermissionException e) {
			log.error(e.getMessage());
			return false;
		} catch (Exception e) {
			log.error("Failed to retrieve proxy",e);
			return false;
		}
		
    }
	
	public static GSSCredential getCommunityCredential() throws Exception 
	{
		return getCredential(Settings.TACC_MYPROXY_SERVER,
					Settings.TACC_MYPROXY_PORT,
					Settings.COMMUNITY_PROXY_USERNAME,
					Settings.COMMUNITY_PROXY_PASSWORD);
	}
	
	public static GSSCredential getUserCredential(String username, String password) throws Exception 
	{
		return getCredential(Settings.TACC_MYPROXY_SERVER,
				Settings.TACC_MYPROXY_PORT,
				username, 
				password);
	}
	
	public static GSSCredential getCredential(String host, int port, String username, String password) throws Exception 
	{
		if (!proxyCache.containsKey(host+username) || proxyCache.get(host+username).getRemainingLifetime() < 3600) 
		{
			MyProxy myproxy = new MyProxy(host, port);
			GSSCredential gssCred = myproxy.get(username, password, 7200);
			log.debug("Retrieved user credential from myproxy " + host);
			
			proxyCache.put(username, gssCred);
		}
		
		return proxyCache.get(username);
	}
}
