package org.iplantc.service.common.auth;

import org.apache.commons.lang.StringUtils;
import org.restlet.security.Verifier;

public class VerifierFactory {

	public Verifier createVerifier(String authSource) {
		if (StringUtils.equals("none", authSource))
		{
			return new NullVerifier();
		}
		else if (StringUtils.equals("agave", authSource))
		{
			return new AgaveVerifier();
		}
		else
		{
			return new JWTVerifier();
		}
	}

}