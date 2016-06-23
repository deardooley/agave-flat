package org.iplantc.service.remote;

import org.ietf.jgss.GSSCredential;
import org.iplantc.service.remote.api.APISubmissionClient;
import org.iplantc.service.remote.gsissh.GSISSHClient;
import org.iplantc.service.remote.local.LocalSubmissionClient;
import org.iplantc.service.remote.ssh.MaverickSSHSubmissionClient;
import org.iplantc.service.systems.model.AuthConfig;
import org.iplantc.service.systems.model.ExecutionSystem;
import org.iplantc.service.systems.model.enumerations.AuthConfigType;

/**
 * Given a login 
 * @author dooley
 *
 */
public class RemoteSubmissionClientFactory {

	public RemoteSubmissionClient getInstance(ExecutionSystem system, String internalUsername) 
	throws Exception 
	{
		AuthConfig userAuthConfig = system.getLoginConfig().getAuthConfigForInternalUsername(internalUsername);
		String encryptionKey = system.getEncryptionKeyForAuthConfig(userAuthConfig);
		String username = userAuthConfig.getUsername();
		String password = userAuthConfig.getClearTextPassword(encryptionKey);
		String host = system.getLoginConfig().getHost();
		int port = system.getLoginConfig().getPort();
		
		switch (system.getLoginConfig().getProtocol()) 
		{
			case GSISSH: 
				GSSCredential credential = (GSSCredential)userAuthConfig.retrieveCredential(encryptionKey);
				return new GSISSHClient(host, port, credential);
			case SSH: 
				String proxyHost = null;
				int proxyPort = 22;
				
				if (system.getLoginConfig().getProxyServer() != null) {
					proxyHost = system.getLoginConfig().getProxyServer().getHost();
					proxyPort = system.getLoginConfig().getProxyServer().getPort();
				}
				
				if (userAuthConfig.getType().equals(AuthConfigType.SSHKEYS)) 
				{	
					return new MaverickSSHSubmissionClient(host, 
							port, username, password, proxyHost, proxyPort,
							userAuthConfig.getClearTextPublicKey(encryptionKey),
							userAuthConfig.getClearTextPrivateKey(encryptionKey));
				}
				else
				{
					return new MaverickSSHSubmissionClient(host, 
							port, username, password, proxyHost, proxyPort);
				}
			case API: 
				return new APISubmissionClient(host, port, username, password);
//			case GRAM:
//				//GSSCredential credential = (GSSCredential)userAuthConfig.retrieveCredential();
//				//return new GRAMSubmissionClient(host, port, credential);
//				throw new NotImplementedException("GRAM submission not yet implemented.");
//			case UNICORE: 
//				//GSSCredential credential = (GSSCredential)userAuthConfig.retrieveCredential();
//				//return new UNICORESubmissionClient(host, port, username, password);
//				throw new NotImplementedException("UNICORE submission not yet implemented.");
			case LOCAL:
			default: 
				return new LocalSubmissionClient(host);
		}
	}
}
