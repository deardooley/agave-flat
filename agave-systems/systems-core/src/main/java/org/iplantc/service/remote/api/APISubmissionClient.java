package org.iplantc.service.remote.api;

import org.apache.commons.lang.NotImplementedException;
import org.ietf.jgss.GSSCredential;
import org.iplantc.service.remote.RemoteSubmissionClient;

/**
 * Handles submitting jobs to an api.
 * 
 * @author dooley
 *
 */
public class APISubmissionClient implements RemoteSubmissionClient {

	@SuppressWarnings("unused")
	private String				endpoint;
	@SuppressWarnings("unused")
	private int					port;
	@SuppressWarnings("unused")
	private String				apiKey;
	@SuppressWarnings("unused")
	private String				apiSecret;
	
	
	/**
	 * @param endpoint
	 * @param port
	 * @param apiKey
	 * @param apiSecret
	 */
	public APISubmissionClient(String endpoint, int port, String apiKey,
			String apiSecret) {
		this.endpoint = endpoint;
		this.port = port;
		this.apiKey = apiKey;
		this.apiSecret = apiSecret;
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

	@Override
	public String getHost() {
		return endpoint;
	}

	@Override
	public int getPort() {
		return port;
	}

	/**
	 * @return the endpoint
	 */
	public String getEndpoint() {
		return endpoint;
	}

	/**
	 * @param endpoint the endpoint to set
	 */
	public void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
	}

	/**
	 * @return the apiKey
	 */
	public String getApiKey() {
		return apiKey;
	}

	/**
	 * @param apiKey the apiKey to set
	 */
	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	/**
	 * @return the apiSecret
	 */
	public String getApiSecret() {
		return apiSecret;
	}

	/**
	 * @param apiSecret the apiSecret to set
	 */
	public void setApiSecret(String apiSecret) {
		this.apiSecret = apiSecret;
	}

	/**
	 * @param port the port to set
	 */
	public void setPort(int port) {
		this.port = port;
	}

}
