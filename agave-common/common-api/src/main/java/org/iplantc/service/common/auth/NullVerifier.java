package org.iplantc.service.common.auth;

import org.restlet.Request;
import org.restlet.Response;
import org.restlet.security.Verifier;

public class NullVerifier implements Verifier {

	public int verify(Request request, Response response)
	{
		return Verifier.RESULT_VALID;
	}

	

}
