package org.iplantc.service.common.auth;


import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.RSAPublicKey;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.ReadOnlyJWSHeader;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.util.Base64URL;

/**
 * Custom verifier to check for SHA256withRSA signing of a JWT
 * 
 * @author dooley
 *
 */
public class SHA256withRSAVerifier extends RSASSAVerifier {

	public SHA256withRSAVerifier(RSAPublicKey publicKey) {
		super(publicKey);
	}

	@Override
	public boolean verify(final ReadOnlyJWSHeader header,
			final byte[] signedContent, final Base64URL signature)
			throws JOSEException {

		try {
			Signature verifier = Signature.getInstance("SHA256withRSA");// getRSASignerAndVerifier(JWSAlgorithm.RS256);

			verifier.initVerify(getPublicKey());
			verifier.update(signedContent);
			return verifier.verify(signature.decode());

		} catch (InvalidKeyException e) {

			throw new JOSEException(
					"Invalid public RSA key: " + e.getMessage(), e);

		} catch (SignatureException e) {

			throw new JOSEException("RSA signature exception: "
					+ e.getMessage(), e);
		} catch (NoSuchAlgorithmException e) {
			throw new JOSEException("RSA signature exception: "
					+ e.getMessage(), e);
		}
	}
}