package org.iplantc.service.common.auth;

import org.apache.log4j.Logger;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.security.Verifier;

public class AgaveVerifier implements Verifier {
	private static final Logger log = Logger.getLogger(AgaveVerifier.class);
	
	public int verify(Request request, Response response)
	{
		try 
		{	
			AuthServiceClient client = new AuthServiceClient(
					request.getChallengeResponse().getIdentifier(),
					new String(request.getChallengeResponse().getSecret()));
			
			return client.login();
			
		} catch (Exception e) {
			log.error("Error authenticating user", e);
			return Verifier.RESULT_INVALID;
		}	
	}
}
