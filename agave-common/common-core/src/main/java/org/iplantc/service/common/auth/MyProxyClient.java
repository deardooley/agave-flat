package org.iplantc.service.common.auth;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.globus.myproxy.GetParams;
import org.globus.myproxy.MyProxyException;
import org.ietf.jgss.GSSCredential;
import org.iplantc.service.common.Settings;

public class MyProxyClient 
{	
	private static final Logger log = Logger.getLogger(MyProxyClient.class);
	
	public static GSSCredential getCommunityCredential() throws MyProxyException {
		return getCredential(Settings.TACC_MYPROXY_SERVER,
							Settings.TACC_MYPROXY_PORT, 
							Settings.COMMUNITY_PROXY_USERNAME, 
							Settings.COMMUNITY_PROXY_PASSWORD,
							null);
	}
	
	public static GSSCredential getUserCredential(String username, String password) throws MyProxyException {
		return getCredential(Settings.TACC_MYPROXY_SERVER,
							Settings.TACC_MYPROXY_PORT, 
							username, 
							password,
							null);
	}
	
	/**
	 * This is the preferred method for fetching an X509 credential from
	 * a MyProxy server. This method will bootstrap trust if not already 
	 * established, fetch the trusted CA certs, and persist them to disk
	 * for use in later gridftp actions.
	 * 
	 * @param host myproxy host
	 * @param port myproxy port
	 * @param username credential username
	 * @param passphrase credential passphrase
	 * @param trustedCALocation location to save the trustroots from the myproxy server.
	 * If not specified, the default globus location will be used.
	 * @return credential valid for 12 hours.
	 * @throws MyProxyException
	 */
	public static GSSCredential getCredential(String host, int port, String username, String passphrase, String trustedCALocation) 
	throws MyProxyException 
	{
		MyProxy myproxy = new MyProxy(host, port, trustedCALocation);
		
		GetParams request = new GetParams();
		request.setWantTrustroots(true);
		request.setUserName(username);
        request.setPassphrase(passphrase);
        request.setLifetime(43200);
        GSSCredential gssCred = null;
        
        File trustrootDir = new File(myproxy.getInstanceTrustRootPath());
        try 
        {	
        	myproxy.bootstrapTrust();
        } 
        catch (MyProxyException e) 
        {
        	log.error(e);
        }
        
        try 
        {	
        	gssCred = myproxy.get(null, request);
        } 
        catch (MyProxyException e) 
        {
        	throw e;
        }
        
		try {
			log.debug("Writing trustroots for myproxy server " + host + 
					" to " + trustrootDir.getAbsolutePath());
			myproxy.writeTrustRoots();
		} catch (Exception e) {
			log.error("Failed to write trustroots to " + trustrootDir.getAbsolutePath(), e);
		}
	
		return gssCred;
	}
}