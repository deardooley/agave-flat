package org.iplantc.service.remote.gram;

import org.apache.commons.lang.NotImplementedException;
import org.ietf.jgss.GSSCredential;
import org.iplantc.service.remote.RemoteSubmissionClient;

/**
 * Submits jobs to gram servers.
 * 
 * @author dooley
 *
 */
public class GRAMSubmissionClient implements RemoteSubmissionClient {

	@SuppressWarnings("unused")
	private String				endpoint;
	@SuppressWarnings("unused")
	private GSSCredential		cred;
	@SuppressWarnings("unused")
	private int					port = 2119;
	
	public GRAMSubmissionClient(String endpoint, GSSCredential cred) {
		this.endpoint = endpoint;
		this.cred = cred;
	}
	
	public GRAMSubmissionClient(String endpoint, int port, GSSCredential cred) {
		this.endpoint = endpoint;
		this.cred = cred;
		this.port = port;
	}
	
	@Override
	public String runCommand(String command) throws Exception
	{
		throw new NotImplementedException();
	}

	@Override
	public void close()
	{
		
	}

	@Override
	public boolean canAuthentication()
	{
		return false;
	}

}
