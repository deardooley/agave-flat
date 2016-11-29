package org.iplantc.service.remote.unicore;

import org.apache.commons.lang.NotImplementedException;
import org.iplantc.service.remote.RemoteSubmissionClient;

/**
 * Submits a job to a UNICORE job execution server.
 * 
 * 
 * @author dooley
 *
 */
public class UNICORESubmissionClient implements RemoteSubmissionClient {

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

	@Override
	public String getHost() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getPort() {
		// TODO Auto-generated method stub
		return 0;
	}

}
