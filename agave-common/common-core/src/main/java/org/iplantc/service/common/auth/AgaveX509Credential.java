package org.iplantc.service.common.auth;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.CertStore;
import java.security.cert.X509Certificate;

import org.apache.commons.lang.StringUtils;
import org.globus.common.CoGProperties;
import org.globus.gsi.CredentialException;
import org.globus.gsi.X509Credential;
import org.globus.gsi.X509ProxyCertPathParameters;
import org.globus.gsi.bc.BouncyCastleOpenSSLKey;
import org.globus.gsi.provider.GlobusProvider;
import org.globus.gsi.provider.KeyStoreParametersFactory;
import org.globus.gsi.stores.ResourceCertStoreParameters;
import org.globus.gsi.stores.ResourceSigningPolicyStore;
import org.globus.gsi.stores.ResourceSigningPolicyStoreParameters;
import org.globus.gsi.trustmanager.X509ProxyCertPathValidator;
import org.globus.gsi.util.CertificateUtil;

/**
 * Extension of {@link X509Credential} class that disables caching for
 * multi-user environments.
 * 
 * @author dooley
 *
 */
public class AgaveX509Credential extends X509Credential {

	private static final long serialVersionUID = 3768397199199147079L;

	private String caCertsLocation = null;
	private TrustedCALocation trustedCALocation = null;

	private X509Certificate[] certChain;

	public AgaveX509Credential(InputStream input,
			TrustedCALocation trustedCALocation) throws CredentialException {
		super(input);

		if (trustedCALocation == null) {
			this.setTrustedCALocation(new TrustedCALocation(null));
		} else {
			this.setTrustedCALocation(trustedCALocation);
		}
		this.caCertsLocation = "file:" + trustedCALocation.getCaPath();

	}

	public AgaveX509Credential(PrivateKey private1, X509Certificate[] newChain) {
		super(private1, newChain);
		this.setTrustedCALocation(new TrustedCALocation(null));
	}

	/**
	 * Verifies the validity of the credentials. All certificate path validation
	 * is performed using trusted certificates in default locations.
	 * 
	 * @exception CredentialException
	 *                if one of the certificates in the chain expired or if path
	 *                validiation fails.
	 */
	public void verify() throws CredentialException {
		try {
			KeyStore keyStore = getTrustStore();
			CertStore crlStore = getCRLStore();
			ResourceSigningPolicyStore sigPolStore = getSigPolStore();

			X509ProxyCertPathParameters parameters = new X509ProxyCertPathParameters(
					keyStore, crlStore, sigPolStore, false);
			X509ProxyCertPathValidator validator = new X509ProxyCertPathValidator();
			validator.engineValidate(CertificateUtil.getCertPath(certChain),
					parameters);
		} catch (Exception e) {
			throw new CredentialException(e);
		}
	}
	
	protected KeyStore getTrustStore()
			throws GeneralSecurityException, IOException {
		String caCertsPattern = caCertsLocation + "/*.0";
		KeyStore keyStore = KeyStore.getInstance(GlobusProvider.KEYSTORE_TYPE,
				GlobusProvider.PROVIDER_NAME);
		keyStore.load(KeyStoreParametersFactory
				.createTrustStoreParameters(caCertsPattern));

		return keyStore;
	}

	protected CertStore getCRLStore()
			throws GeneralSecurityException, NoSuchAlgorithmException {
		String crlPattern = caCertsLocation + "/*.r*";
		CertStore crlStore = CertStore.getInstance(
				GlobusProvider.CERTSTORE_TYPE, new ResourceCertStoreParameters(
						null, crlPattern));

		return crlStore;
	}

	protected ResourceSigningPolicyStore getSigPolStore() throws GeneralSecurityException {
		String sigPolPattern = caCertsLocation + "/*.signing_policy";
		ResourceSigningPolicyStore sigPolStore = new ResourceSigningPolicyStore(
				new ResourceSigningPolicyStoreParameters(sigPolPattern));

		return sigPolStore;
	}

	/**
	 * Returns the default credential. The default credential is usually the
	 * user proxy certificate. <BR>
	 * The credential will be loaded on the initial call. It must not be
	 * expired. All subsequent calls to this function return cached credential
	 * object. Once the credential is cached, and the underlying file changes,
	 * the credential will be reloaded.
	 *
	 * @return the default credential.
	 * @exception CredentialException
	 *                if the credential expired or some other error with the
	 *                credential.
	 */
	public synchronized static AgaveX509Credential getDefaultCredential()
			throws CredentialException {
		return null;
	}

	public TrustedCALocation getTrustedCALocation() {
		return trustedCALocation;
	}

	public void setTrustedCALocation(TrustedCALocation trustedCALocation) {
		this.trustedCALocation = trustedCALocation;
	}
}
