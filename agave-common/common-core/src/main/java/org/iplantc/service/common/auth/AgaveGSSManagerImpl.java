/*
 * Copyright 1999-2010 University of Chicago
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS,WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied.
 *
 * See the License for the specific language governing permissions and limitations under the License.
 */
package org.iplantc.service.common.auth;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.Provider;
import java.util.Iterator;
import java.util.Set;

import javax.security.auth.Subject;

import org.apache.log4j.Logger;
import org.globus.gsi.CredentialException;
import org.globus.gsi.gssapi.GSSConstants;
import org.globus.gsi.gssapi.GlobusGSSException;
import org.globus.gsi.gssapi.GlobusGSSName;
import org.globus.gsi.gssapi.jaas.JaasSubject;
import org.gridforum.jgss.ExtendedGSSCredential;
import org.gridforum.jgss.ExtendedGSSManager;
import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSName;
import org.ietf.jgss.Oid;

/**
 * An implementation of <code>GlobusGSSManager</code>.
 */
public class AgaveGSSManagerImpl extends ExtendedGSSManager {

	private static Logger logger = Logger.getLogger(AgaveGSSManagerImpl.class);

	static final Oid[] MECHS;

	static {
		try {
			MECHS = new Oid[] { GSSConstants.MECH_OID };
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage());
		}
	}

	private AgaveGSSCredentialImpl defaultCred;

	/**
	 * Acquires GSI GSS credentials.
	 *
	 * @see #createCredential(GSSName, int, Oid, int)
	 */
	public GSSCredential createCredential(int usage) throws GSSException {
		return createCredential(null, GSSCredential.DEFAULT_LIFETIME,
				(Oid) null, usage);
	}

	/**
	 * Acquires GSI GSS credentials. First, it tries to find the credentials in
	 * the private credential set of the current JAAS Subject. If the Subject is
	 * not set or credentials are not found in the Subject, it tries to get a
	 * default user credential (usually an user proxy file)
	 *
	 * @param lifetime
	 *            Only lifetime set to {@link GSSCredential#DEFAULT_LIFETIME
	 *            GSSCredential.DEFAULT_LIFETIME} is allowed.
	 * @see org.globus.gsi.X509Credential#getDefaultCredential()
	 */
	public GSSCredential createCredential(GSSName name, int lifetime, Oid mech,
			int usage) throws GSSException {
		checkMechanism(mech);

		if (name != null) {
			if (name.isAnonymous()) {
				return new AgaveGSSCredentialImpl();
			} else {
				throw new GSSException(GSSException.UNAVAILABLE);
			}
		}

		AgaveX509Credential cred = null;

		Subject subject = JaasSubject.getCurrentSubject();
		if (subject != null) {
			logger.debug("Getting credential from context");
			Set gssCreds = subject
					.getPrivateCredentials(AgaveGSSCredentialImpl.class);
			if (gssCreds != null) {
				Iterator iter = gssCreds.iterator();
				if (iter.hasNext()) {
					AgaveGSSCredentialImpl credImpl = (AgaveGSSCredentialImpl) iter
							.next();
					cred = (AgaveX509Credential) credImpl.getX509Credential();
				}
			}
		}

		if (lifetime == GSSCredential.INDEFINITE_LIFETIME || lifetime > 0) {
			// lifetime not supported
			throw new GlobusGSSException(GSSException.FAILURE,
					GlobusGSSException.BAD_ARGUMENT, "badLifetime01");
		}

		if (cred == null) {
			logger.debug("Getting default credential");
			try {
				cred = AgaveX509Credential.getDefaultCredential();
			} catch (CredentialException e) {
				throw new GlobusGSSException(GSSException.DEFECTIVE_CREDENTIAL,
						e);
			} catch (Exception e) {
				throw new GlobusGSSException(GSSException.DEFECTIVE_CREDENTIAL,
						e);
			}

			return getDefaultCredential(cred, usage);
		} else {
			return new AgaveGSSCredentialImpl(cred, usage);
		}
	}

	private synchronized GSSCredential getDefaultCredential(
			AgaveX509Credential cred, int usage) throws GSSException {
		if (this.defaultCred != null && this.defaultCred.getUsage() == usage
				&& this.defaultCred.getX509Credential() == cred) {
			return this.defaultCred;
		} else {
			this.defaultCred = new AgaveGSSCredentialImpl(cred, usage);
			return this.defaultCred;
		}
	}

	/**
	 * Acquires GSI GSS credentials.
	 *
	 * @see #createCredential(GSSName, int, Oid, int)
	 */
	public GSSCredential createCredential(GSSName name, int lifetime,
			Oid mechs[], int usage) throws GSSException {
		if (mechs == null || mechs.length == 0) {
			return createCredential(name, lifetime, (Oid) null, usage);
		} else {
			// XXX: not sure this is correct
			GSSCredential cred = createCredential(name, lifetime, mechs[0],
					usage);
			for (int i = 1; i < mechs.length; i++) {
				cred.add(name, lifetime, lifetime, mechs[i], usage);
			}
			return cred;
		}
	}

	/**
	 * Imports a credential.
	 *
	 * @param lifetime
	 *            Only lifetime set to {@link GSSCredential#DEFAULT_LIFETIME
	 *            GSSCredential.DEFAULT_LIFETIME} is allowed.
	 */
	public GSSCredential createCredential(byte[] buff, int option,
			int lifetime, Oid mech, int usage) throws GSSException {
		checkMechanism(mech);

		if (buff == null || buff.length < 1) {
			throw new GlobusGSSException(GSSException.FAILURE,
					GlobusGSSException.BAD_ARGUMENT, "invalidBuf");
		}

		if (lifetime == GSSCredential.INDEFINITE_LIFETIME || lifetime > 0) {
			// lifetime not supported
			throw new GlobusGSSException(GSSException.FAILURE,
					GlobusGSSException.BAD_ARGUMENT, "badLifetime01");
		}

		InputStream input = null;

		switch (option) {
		case ExtendedGSSCredential.IMPEXP_OPAQUE:
			input = new ByteArrayInputStream(buff);
			break;
		case ExtendedGSSCredential.IMPEXP_MECH_SPECIFIC:
			String s = new String(buff);
			int pos = s.indexOf('=');
			if (pos == -1) {
				throw new GSSException(GSSException.FAILURE);
			}
			String filename = s.substring(pos + 1).trim();
			try {
				input = new FileInputStream(filename);
			} catch (IOException e) {
				throw new GlobusGSSException(GSSException.FAILURE, e);
			}
			break;
		default:
			throw new GlobusGSSException(GSSException.FAILURE,
					GlobusGSSException.BAD_ARGUMENT, "unknownOption",
					new Object[] { new Integer(option) });
		}

		AgaveX509Credential cred = null;
		try {
			cred = new AgaveX509Credential(input, new TrustedCALocation(null));
		} catch (CredentialException e) {
			throw new GlobusGSSException(GSSException.DEFECTIVE_CREDENTIAL, e);
		} catch (Exception e) {
			throw new GlobusGSSException(GSSException.DEFECTIVE_CREDENTIAL, e);
		}

		return new AgaveGSSCredentialImpl(cred, usage);
	}

	// for initiators
	public GSSContext createContext(GSSName peer, Oid mech, GSSCredential cred,
			int lifetime) throws GSSException {
		checkMechanism(mech);

		AgaveGSSCredentialImpl globusCred = null;
		if (cred == null) {
			globusCred = (AgaveGSSCredentialImpl) createCredential(GSSCredential.INITIATE_ONLY);
		} else if (cred instanceof AgaveGSSCredentialImpl) {
			globusCred = (AgaveGSSCredentialImpl) cred;
		} else {
			throw new GSSException(GSSException.NO_CRED);
		}

		AgaveGSSContextImpl ctx = new AgaveGSSContextImpl(peer, globusCred);
		ctx.requestLifetime(lifetime);
		
		AgaveX509Credential x509 = (AgaveX509Credential)globusCred.getX509Credential();
		try {
			ctx.sslConfigurator.setCredentialStore(x509.getTrustStore());			
			ctx.sslConfigurator.setCrlStore(x509.getCRLStore());
			ctx.sslConfigurator.setPolicyStore(x509.getSigPolStore());
		} catch (Exception e) {
			logger.error("Failed to set ssl configuration for credential " + x509.getSubject() + 
					". Using default trustroots.");
		}
		return ctx;
	}

	// for acceptors
	public GSSContext createContext(GSSCredential cred) throws GSSException {

		AgaveGSSCredentialImpl globusCred = null;
		if (cred == null) {
			globusCred = (AgaveGSSCredentialImpl) createCredential(GSSCredential.ACCEPT_ONLY);
		} else if (cred instanceof AgaveGSSCredentialImpl) {
			globusCred = (AgaveGSSCredentialImpl) cred;
		} else {
			throw new GSSException(GSSException.NO_CRED);
		}
		// XXX: don't know about the first argument
		GSSContext ctx = new AgaveGSSContextImpl(null, globusCred);
		return ctx;
	}

	public Oid[] getMechs() {
		return MECHS;
	}

	public GSSName createName(String nameStr, Oid nameType) throws GSSException {
		return new GlobusGSSName(nameStr, nameType);
	}

	/**
	 * Checks if the specified mechanism matches the mechanism supported by this
	 * implementation.
	 *
	 * @param mech
	 *            mechanism to check
	 * @exception GSSException
	 *                if mechanism not supported.
	 */
	public static void checkMechanism(Oid mech) throws GSSException {
		if (mech != null && !mech.equals(GSSConstants.MECH_OID)) {
			throw new GSSException(GSSException.BAD_MECH);
		}
	}

	// ==================================================================
	// Not implemented below
	// ==================================================================

	/**
	 * Currently not implemented.
	 */
	public GSSContext createContext(byte[] interProcessToken)
			throws GSSException {
		throw new GSSException(GSSException.UNAVAILABLE);
	}

	/**
	 * Currently not implemented.
	 */
	public Oid[] getNamesForMech(Oid mech) throws GSSException {
		throw new GSSException(GSSException.UNAVAILABLE);
	}

	/**
	 * Currently not implemented.
	 */
	public Oid[] getMechsForName(Oid nameType) {
		// not implemented, not needed by Globus
		return null;
	}

	/**
	 * Currently not implemented.
	 */
	public GSSName createName(String nameStr, Oid nameType, Oid mech)
			throws GSSException {
		throw new GSSException(GSSException.UNAVAILABLE);
	}

	/**
	 * Currently not implemented.
	 */
	public GSSName createName(byte name[], Oid nameType) throws GSSException {
		throw new GSSException(GSSException.UNAVAILABLE);
	}

	/**
	 * Currently not implemented.
	 */
	public GSSName createName(byte name[], Oid nameType, Oid mech)
			throws GSSException {
		throw new GSSException(GSSException.UNAVAILABLE);
	}

	/**
	 * Currently not implemented.
	 */
	public void addProviderAtFront(Provider p, Oid mech) throws GSSException {
		// this GSSManager implementation does not support an SPI
		// with a pluggable provider architecture
		throw new GSSException(GSSException.UNAVAILABLE);
	}

	/**
	 * Currently not implemented.
	 */
	public void addProviderAtEnd(Provider p, Oid mech) throws GSSException {
		// this GSSManager implementation does not support an SPI
		// with a pluggable provider architecture
		throw new GSSException(GSSException.UNAVAILABLE);
	}

}
