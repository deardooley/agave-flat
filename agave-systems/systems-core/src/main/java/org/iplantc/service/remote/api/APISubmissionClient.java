package org.iplantc.service.remote.api;

import org.apache.commons.lang.NotImplementedException;
import org.iplantc.service.remote.RemoteSubmissionClient;

/**
 * Handles submitting jobs to an api.
 * 
 * @author dooley
 *
 */
public class APISubmissionClient implements RemoteSubmissionClient {

	public APISubmissionClient(String endpoint, int port, String username, String password) {
		
	}
	
	@Override
	public String runCommand(String command) throws Exception
	{
		throw new NotImplementedException();
	}

	@Override
	public void close()
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean canAuthentication()
	{
		return false;
	}

}
