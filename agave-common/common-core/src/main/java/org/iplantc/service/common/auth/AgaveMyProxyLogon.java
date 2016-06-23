package org.iplantc.service.common.auth;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.ProtocolException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.security.auth.login.FailedLoginException;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DEROutputStream;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.X509Name;
import org.bouncycastle.jce.PKCS10CertificationRequest;
import org.bouncycastle.util.encoders.Base64;
import org.globus.gsi.X509Credential;
import org.globus.gsi.gssapi.GlobusGSSCredentialImpl;
import org.globus.myproxy.MyProxyException;
import org.gridforum.jgss.ExtendedGSSCredential;
import org.gridforum.jgss.ExtendedGSSManager;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.iplantc.service.common.util.Slug;
import org.testng.Assert;

/**
 * The MyProxyLogon class provides an interface for retrieving credentials from
 * a MyProxy server.
 * <p>
 * First, use <code>setHost</code>, <code>setPort</code>,
 * <code>setUsername</code>, <code>setPassphrase</code>,
 * <code>setCredentialName</code>, <code>setLifetime</code> and
 * <code>requestTrustRoots</code> to configure. Then call <code>connect</code>,
 * <code>logon</code>, <code>getCredentials</code>, then <code>disconnect</code>
 * . Use <code>getCertificates</code> and <code>getPrivateKey</code> to access
 * the retrieved credentials, or <code>writeProxyFile</code> or
 * <code>saveCredentialsToFile</code> to write them to a file. Use
 * <code>writeTrustRoots</code>, <code>getTrustedCAs</code>,
 * <code>getCRLs</code>, <code>getTrustRootData</code>, and
 * <code>getTrustRootFilenames</code> for trust root information.
 * 
 * @version 1.0
 * @see <a href="http://myproxy.ncsa.uiuc.edu/">MyProxy Project Home Page</a>
 */
public class AgaveMyProxyLogon {
	static Logger logger = Logger.getLogger(AgaveMyProxyLogon.class);
	public final static String version = "1.0";
	public final static String BouncyCastleLicense = org.bouncycastle.LICENSE.licenseText;

	protected enum State {
		READY, CONNECTED, LOGGEDON, DONE
	}

	private class MyTrustManager implements X509TrustManager {
		private String trustrootPath = null;
		
		public MyTrustManager(String trustrootPath) {
			this.trustrootPath = trustrootPath;
		}
		
		public X509Certificate[] getAcceptedIssuers() {
			X509Certificate[] issuers = null;
			String certDirPath = trustrootPath;
			if (certDirPath == null) {
				return null;
			}
			File dir = new File(certDirPath);
			if (!dir.isDirectory()) {
				return null;
			}
			String[] certFilenames = dir.list();
			String[] certData = new String[certFilenames.length];
			for (int i = 0; i < certFilenames.length; i++) {
				try {
					FileInputStream fileStream = new FileInputStream(
							certDirPath + File.separator + certFilenames[i]);
					byte[] buffer = new byte[fileStream.available()];
					fileStream.read(buffer);
					certData[i] = new String(buffer);
				} catch (Exception e) {
					// ignore
				}
			}
			try {
				issuers = getX509CertsFromStringList(certData, certFilenames);
			} catch (Exception e) {
				// ignore
			}
			return issuers;
		}

		public void checkClientTrusted(X509Certificate[] certs, String authType)
				throws CertificateException {
			throw new CertificateException(
					"checkClientTrusted not implemented by trust manager");
		}

		public void checkServerTrusted(X509Certificate[] certs, String authType)
				throws CertificateException {
			checkServerCertPath(certs);
			checkServerDN(certs[0]);
		}

		private void checkServerCertPath(X509Certificate[] certs)
		throws CertificateException 
		{
			checkServerCertPath(certs, false);
		}
		
		private void checkServerCertPath(X509Certificate[] certs, boolean forceBootstrap)
		throws CertificateException 
		{
			CertPathValidator validator = null;
			CertificateFactory certFactory = null;
			CertPath certPath = null;
			X509Certificate[] acceptedIssuers = null;
			try 
			{
				validator = CertPathValidator
						.getInstance(CertPathValidator.getDefaultType());
				certFactory = CertificateFactory
						.getInstance("X.509");
				certPath = certFactory.generateCertPath(Arrays
						.asList(certs));
				acceptedIssuers = getAcceptedIssuers();
				if (acceptedIssuers == null || forceBootstrap) 
				{
					String certDir = trustrootPath;
					if (certDir != null && !requestTrustRoots) {
						throw new CertificateException(
								"no CA certificates found in " + certDir);
					} else if (!requestTrustRoots) {
						throw new CertificateException(
								"no CA certificates directory found");
					}
					logger.info("No trusted CAs configured -- bootstrapping trust from MyProxy server at " + host);
					acceptedIssuers = new X509Certificate[1];
					acceptedIssuers[0] = certs[certs.length - 1];
				}
				Set<TrustAnchor> trustAnchors = new HashSet<TrustAnchor>(
						acceptedIssuers.length);
				for (int i = 0; i < acceptedIssuers.length; i++) {
					TrustAnchor ta = new TrustAnchor(acceptedIssuers[i], null);
					trustAnchors.add(ta);
				}
				PKIXParameters pkixParameters = new PKIXParameters(trustAnchors);
				pkixParameters.setRevocationEnabled(false);
				validator.validate(certPath, pkixParameters);
			} catch (CertificateException e) {
				throw e;
			} 
			catch (CertPathValidatorException e) 
			{
				// try again after bootstrapping
				if (!forceBootstrap) {
					checkServerCertPath(certs, true);
				}
			} catch (GeneralSecurityException e) {
				throw new CertificateException(e);
			}
		}

		private void checkServerDN(X509Certificate cert)
		throws CertificateException 
		{
			String subject = cert.getSubjectX500Principal().getName();
			int index = subject.indexOf("CN=");
			if (index == -1) {
				throw new CertificateException("Server certificate subject ("
						+ subject + "does not contain a CN component.");
			}
			String CN = subject.substring(index + 3);
			index = CN.indexOf(',');
			if (index >= 0) {
				CN = CN.substring(0, index);
			}
			if ((index = CN.indexOf('/')) >= 0) {
				String service = CN.substring(0, index);
				CN = CN.substring(index + 1);
				if (!service.equals("host") && !service.equals("myproxy")) {
					throw new CertificateException(
							"Server certificate subject CN contains unknown service element: "
									+ subject);
				}
			}
			String myHostname = host;
			if (myHostname.equals("localhost")) {
				try {
					myHostname = InetAddress.getLocalHost().getHostName();
				} catch (Exception e) {
					// ignore
				}
			}
			if (!CN.equals(myHostname)) {
				throw new CertificateException(
						"Server certificate subject CN (" + CN
								+ ") does not match server hostname (" + host
								+ ").");
			}
		}
	}

	private final static int b64linelen = 64;
	private final static String X509_USER_PROXY_FILE = "x509up_u";
	private final static String VERSION = "VERSION=MYPROXYv2";
	private final static String GETCOMMAND = "COMMAND=0";
	private final static String TRUSTROOTS = "TRUSTED_CERTS=";
	private final static String USERNAME = "USERNAME=";
	private final static String PASSPHRASE = "PASSPHRASE=";
	private final static String LIFETIME = "LIFETIME=";
	private final static String CREDNAME = "CRED_NAME=";
	private final static String RESPONSE = "RESPONSE=";
	private final static String ERROR = "ERROR=";
	private final static String DN = "CN=ignore";
	private final static String TRUSTED_CERT_PATH = "/.globus/certificates";
	public static final int DEFAULT_PORT = 7512;
	
	protected final static int keySize = 1024;
	protected final int MIN_PASS_PHRASE_LEN = 6;
	protected final static String keyAlg = "RSA";
	protected final static String pkcs10SigAlgName = "SHA1withRSA";
	protected final static String pkcs10Provider = "SunRsaSign";
	protected State state = State.READY;
	protected String host = "docker.example.com";
	protected String username;
	protected String credname;
	protected String passphrase;
	protected int port = 7512;
	protected int lifetime = 43200;
	protected boolean requestTrustRoots = false;
	protected SSLSocket socket;
	protected BufferedInputStream socketIn;
	protected BufferedOutputStream socketOut;
	protected KeyPair keypair;
	protected Collection certificateChain;
	protected String[] trustrootFilenames;
	protected String[] trustrootData;
	protected TrustedCALocation trustedCALocation;
//	protected String trustrootPath;

	{
		synchronized(this){
//				System.setProperty("org.globus.gsi.gssapi.provider", 
//					"org.iplantc.service.common.auth.AgaveGSSManagerImpl");
		}
	}
	
	/**
	 * Constructs a MyProxyLogon object. Order of preference lookup for server
	 * and port are TGFM config, System env, default values.
	 */
	public AgaveMyProxyLogon(String host, int port) {
		super();
		this.host = host;
		this.port = port;
	}

	/**
	 * Gets the hostname of the MyProxy server.
	 * 
	 * @return MyProxy server hostname
	 */
	public String getHost() {
		return this.host;
	}

	/**
	 * Sets the hostname of the MyProxy server. Defaults to localhost.
	 * 
	 * @param host
	 *            MyProxy server hostname
	 */
	public void setHost(String host) {
		this.host = host;
	}

	/**
	 * Gets the port of the MyProxy server.
	 * 
	 * @return MyProxy server port
	 */
	public int getPort() {
		return this.port;
	}

	/**
	 * Sets the port of the MyProxy server. Defaults to 7512.
	 * 
	 * @param port
	 *            MyProxy server port
	 */
	public void setPort(int port) {
		this.port = port;
	}

	/**
	 * Gets the MyProxy username.
	 * 
	 * @return MyProxy server port
	 */
	public String getUsername() {
		return this.username;
	}

	/**
	 * Sets the MyProxy username. Defaults to user.name.
	 * 
	 * @param username
	 *            MyProxy username
	 */
	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * Gets the optional MyProxy credential name.
	 * 
	 * @return credential name
	 */
	public String getCredentialName() {
		return this.credname;
	}

	/**
	 * Sets the optional MyProxy credential name.
	 * 
	 * @param credname
	 *            credential name
	 */
	public void setCredentialName(String credname) {
		this.credname = credname;
	}

	/**
	 * Sets the MyProxy passphrase.
	 * 
	 * @param passphrase
	 *            MyProxy passphrase
	 */
	public void setPassphrase(String passphrase) {
		this.passphrase = passphrase;
	}

	/**
	 * Gets the requested credential lifetime.
	 * 
	 * @return Credential lifetime
	 */
	public int getLifetime() {
		return this.lifetime;
	}

	/**
	 * Sets the requested credential lifetime. Defaults to 43200 seconds (12
	 * hours).
	 * 
	 * @param seconds
	 *            Credential lifetime
	 */
	public void setLifetime(int seconds) {
		this.lifetime = seconds;
	}
	
	public void setTrustedCALocation(TrustedCALocation trustedCALocation) {
		this.trustedCALocation = trustedCALocation;
	}

	/**
	 * Gets the certificates returned from the MyProxy server by
	 * getCredentials().
	 * 
	 * @return Collection of java.security.cert.Certificate objects
	 */
	public Collection getCertificates() {
		return this.certificateChain;
	}

	/**
	 * Gets the private key generated by getCredentials().
	 * 
	 * @return PrivateKey
	 */
	public PrivateKey getPrivateKey() {
		return this.keypair.getPrivate();
	}

	/**
	 * Sets whether to request trust roots (CA certificates, CRLs, signing
	 * policy files) from the MyProxy server. Defaults to false (i.e., not to
	 * request trust roots).
	 * 
	 * @param flag
	 *            If true, request trust roots. If false, don't request trust
	 *            roots.
	 */
	public void requestTrustRoots(boolean flag) {
		this.requestTrustRoots = flag;
	}

	/**
	 * Gets trust root filenames.
	 * 
	 * @return trust root filenames
	 */
	public String[] getTrustRootFilenames() {
		return this.trustrootFilenames;
	}

	/**
	 * Gets trust root data corresponding to the trust root filenames.
	 * 
	 * @return trust root data
	 */
	public String[] getTrustRootData() {
		return this.trustrootData;
	}

	/**
	 * Connects to the MyProxy server at the desired host and port. Requires
	 * host authentication via SSL. The host's certificate subject must match
	 * the requested hostname. If CA certificates are found in the standard GSI
	 * locations, they will be used to verify the server's certificate. If trust
	 * roots are requested and no CA certificates are found, the server's
	 * certificate will still be accepted.
	 */
	public void connect() throws IOException, GeneralSecurityException {
		SSLContext sc = SSLContext.getInstance("SSL");
		TrustManager[] trustAllCerts = new TrustManager[] { new MyTrustManager(getTrustRootPath()) };
		sc.init(null, trustAllCerts, new java.security.SecureRandom());
		SSLSocketFactory sf = sc.getSocketFactory();
		this.socket = (SSLSocket) sf.createSocket(this.host, this.port);
		this.socket.setEnabledProtocols(new String[] { "SSLv3" });
		this.socket.startHandshake();
		this.socketIn = new BufferedInputStream(this.socket.getInputStream());
		this.socketOut = new BufferedOutputStream(this.socket.getOutputStream());
		this.state = State.CONNECTED;
	}

	/**
	 * Disconnects from the MyProxy server.
	 */
	public void disconnect() throws IOException {
		this.socket.close();
		this.socket = null;
		this.socketIn = null;
		this.socketOut = null;
		this.state = State.READY;
	}

	/**
	 * Logs on to the MyProxy server by issuing the MyProxy GET command.
	 */
	public void logon() throws IOException, GeneralSecurityException {
		String line;
		char response;

		if (this.state != State.CONNECTED) {
				this.connect();
		}

		this.socketOut.write('0');
		this.socketOut.flush();
		this.socketOut.write(VERSION.getBytes());
		this.socketOut.write('\n');
		this.socketOut.write(GETCOMMAND.getBytes());
		this.socketOut.write('\n');
		this.socketOut.write(USERNAME.getBytes());
		this.socketOut.write(this.username.getBytes());
		this.socketOut.write('\n');
		this.socketOut.write(PASSPHRASE.getBytes());
		this.socketOut.write(this.passphrase.getBytes());
		this.socketOut.write('\n');
		this.socketOut.write(LIFETIME.getBytes());
		this.socketOut.write(Integer.toString(this.lifetime).getBytes());
		this.socketOut.write('\n');
		if (this.credname != null) {
			this.socketOut.write(CREDNAME.getBytes());
			this.socketOut.write(this.credname.getBytes());
			this.socketOut.write('\n');
		}
		if (this.requestTrustRoots) {
			this.socketOut.write(TRUSTROOTS.getBytes());
			this.socketOut.write("1\n".getBytes());
		}
		this.socketOut.flush();

		line = readLine(this.socketIn);
		if (line == null) {
			throw new EOFException();
		}
		if (!line.equals(VERSION)) {
			throw new ProtocolException("bad MyProxy protocol VERSION string: "
					+ line);
		}
		line = readLine(this.socketIn);
		if (line == null) {
			throw new EOFException();
		}
		if (!line.startsWith(RESPONSE)
				|| line.length() != RESPONSE.length() + 1) {
			throw new ProtocolException(
					"bad MyProxy protocol RESPONSE string: " + line);
		}
		response = line.charAt(RESPONSE.length());
		if (response == '1') {
			StringBuffer errString;

			errString = new StringBuffer("MyProxy logon failed");
			while ((line = readLine(this.socketIn)) != null) {
				if (line.startsWith(ERROR)) {
					errString.append('\n');
					errString.append(line.substring(ERROR.length()));
				}
			}
			throw new FailedLoginException(errString.toString());
		} else if (response == '2') {
			throw new ProtocolException(
					"MyProxy authorization RESPONSE not implemented");
		} else if (response != '0') {
			throw new ProtocolException(
					"unknown MyProxy protocol RESPONSE string: " + line);
		}
		while ((line = readLine(this.socketIn)) != null) {
			if (line.startsWith(TRUSTROOTS)) {
				String filenameList = line.substring(TRUSTROOTS.length());
				this.trustrootFilenames = filenameList.split(",");
				this.trustrootData = new String[this.trustrootFilenames.length];
				for (int i = 0; i < this.trustrootFilenames.length; i++) {
					String lineStart = "FILEDATA_" + this.trustrootFilenames[i]
							+ "=";
					line = readLine(this.socketIn);
					if (line == null) {
						throw new EOFException();
					}
					if (!line.startsWith(lineStart)) {
						throw new ProtocolException(
								"bad MyProxy protocol RESPONSE: expecting "
										+ lineStart + " but received " + line);
					}
					this.trustrootData[i] = new String(Base64.decode(line
							.substring(lineStart.length())));
				}
			}
		}
		this.state = State.LOGGEDON;
	}

	/**
	 * Retrieves credentials from the MyProxy server.
	 */
	@SuppressWarnings("rawtypes")
	public GSSCredential getCredentials() throws MyProxyException 
	{
		try 
		{
			int numCertificates;
			KeyPairGenerator keyGenerator;
			PKCS10CertificationRequest pkcs10;
			CertificateFactory certFactory;
	
			if (this.state != State.LOGGEDON) {
				this.logon();
			}
	
			keyGenerator = KeyPairGenerator.getInstance(keyAlg);
			keyGenerator.initialize(keySize);
			this.keypair = keyGenerator.genKeyPair();
	
			pkcs10 = new PKCS10CertificationRequest(pkcs10SigAlgName, new X509Name(
					DN), this.keypair.getPublic(), null, this.keypair.getPrivate(),
					pkcs10Provider);
	
			this.socketOut.write(pkcs10.getEncoded());
			this.socketOut.flush();
			numCertificates = this.socketIn.read();
			if (numCertificates == -1) {
				throw new MyProxyException("Invalid number of certifications. connection aborted");
			} else if (numCertificates == 0 || numCertificates < 0) {
				throw new MyProxyException("Bad number of certificates sent by server: " + 
						Integer.toString(numCertificates));
			}
			certFactory = CertificateFactory.getInstance("X.509");
			this.certificateChain = certFactory.generateCertificates(this.socketIn);
			this.state = State.DONE;
			
			X509Certificate certificate;
			PrintStream printStream;
			
			Iterator iter = this.certificateChain.iterator();
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
	
			certificate = (X509Certificate) iter.next();
			printStream = new PrintStream(bos);
			printCert(certificate, printStream);
			printKey(keypair.getPrivate(), printStream);
			while (iter.hasNext()) {
				certificate = (X509Certificate) iter.next();
				printCert(certificate, printStream);
			}
	
			ExtendedGSSManager manager = (ExtendedGSSManager) ExtendedGSSManager
					.getInstance();
			AgaveGSSCredentialImpl proxy = (AgaveGSSCredentialImpl)manager.createCredential(bos.toByteArray(),
					ExtendedGSSCredential.IMPEXP_OPAQUE,
					GSSCredential.DEFAULT_LIFETIME, null, // use default mechanism - GSI
					GSSCredential.INITIATE_AND_ACCEPT);
			((AgaveX509Credential)proxy.getX509Credential()).setTrustedCALocation(new TrustedCALocation(getTrustRootPath()));
			return proxy;
		} 
		catch (NoSuchElementException e) {
			throw new MyProxyException("Failed to retrieve certificate from the server.", e);
		}
		catch (Exception e) 
		{
			throw new MyProxyException(e.getMessage(), e);
		}
		
	}

	/**
	 * Writes the retrieved credentials to the Globus proxy file location.
	 */
	public void writeProxyFile() throws IOException, GeneralSecurityException {
		saveCredentialsToFile(getProxyLocation());
	}

	public Collection getCertificateChain() {
		return certificateChain;
	}

	/**
	 * Writes the retrieved credentials to the specified filename.
	 */
	public void saveCredentialsToFile(String filename) throws IOException,
	GeneralSecurityException {
		Iterator iter;
		X509Certificate certificate;
		PrintStream printStream;

		iter = this.certificateChain.iterator();
		certificate = (X509Certificate) iter.next();
		File outFile = new File(filename);
		outFile.delete();
		outFile.createNewFile();
		setFilePermissions(filename, "0600");
		printStream = new PrintStream(new FileOutputStream(outFile));
		printCert(certificate, printStream);
		printKey(keypair.getPrivate(), printStream);
		while (iter.hasNext()) {
			certificate = (X509Certificate) iter.next();
			printCert(certificate, printStream);
		}
	}

	public X509Credential convertCredentialsToGlobusCredential()
			throws CertificateEncodingException, IOException, GSSException {
		Iterator iter;
		X509Certificate certificate;
		PrintStream printStream;

		iter = this.certificateChain.iterator();
		ByteArrayOutputStream bos = new ByteArrayOutputStream();

		certificate = (X509Certificate) iter.next();
		printStream = new PrintStream(bos);
		printCert(certificate, printStream);
		printKey(keypair.getPrivate(), printStream);
		while (iter.hasNext()) {
			certificate = (X509Certificate) iter.next();
			printCert(certificate, printStream);
		}

		ExtendedGSSManager manager = (ExtendedGSSManager) ExtendedGSSManager
				.getInstance();
		GSSCredential proxy = manager.createCredential(bos.toByteArray(),
				ExtendedGSSCredential.IMPEXP_OPAQUE,
				GSSCredential.DEFAULT_LIFETIME, null, // use default mechanism -
														// GSI
				GSSCredential.ACCEPT_ONLY);

		X509Credential globusCred = null;

		if (proxy instanceof GlobusGSSCredentialImpl) {
			globusCred = ((GlobusGSSCredentialImpl) proxy).getX509Credential();
		}

		return globusCred;

	}

	/**
	 * Writes the retrieved trust roots to the Globus trusted certificates
	 * directory.
	 * 
	 * @return true if trust roots are written successfully, false if no trust
	 *         roots are available to be written
	 */
	public boolean writeTrustRoots() throws IOException {
		return writeTrustRoots(getTrustRootPath());
	}

	/**
	 * Writes the retrieved trust roots to a trusted certificates directory.
	 * 
	 * @param directory
	 *            path where the trust roots should be written
	 * @return true if trust roots are written successfully, false if no trust
	 *         roots are available to be written
	 */
	public boolean writeTrustRoots(String directory) throws IOException {
		if (this.trustrootFilenames == null || this.trustrootData == null) {
			return false;
		}
		File rootDir = new File(directory);
		if (!rootDir.exists()) {
			rootDir.mkdirs();
		}
		for (int i = 0; i < trustrootFilenames.length; i++) {
//			logger.debug("Wrote trust file " + directory + File.separator
//					+ this.trustrootFilenames[i]);
			FileOutputStream out = new FileOutputStream(directory
					+ File.separator + this.trustrootFilenames[i]);
			out.write(this.trustrootData[i].getBytes());
			out.close();
		}
		logger.info("Wrote trust files for " + host + " to " + directory);
		
		return true;
	}

	/**
	 * Gets the trusted CA certificates returned by the MyProxy server.
	 * 
	 * @return trusted CA certificates, or null if none available
	 */
	public X509Certificate[] getTrustedCAs() throws CertificateException {
		if (trustrootData == null)
			return null;
		return getX509CertsFromStringList(trustrootData, trustrootFilenames);
	}

	private static X509Certificate[] getX509CertsFromStringList(
			String[] certList, String[] nameList) throws CertificateException {
		CertificateFactory certFactory = CertificateFactory
				.getInstance("X.509");
		Collection<X509Certificate> c = new ArrayList<X509Certificate>(
				certList.length);
		for (int i = 0; i < certList.length; i++) {
			int index = -1;
			String certData = certList[i];
			if (certData != null) {
				index = certData.indexOf("-----BEGIN CERTIFICATE-----");
			}
			if (index >= 0) {
				certData = certData.substring(index);
				ByteArrayInputStream inputStream = new ByteArrayInputStream(
						certData.getBytes());
				try {
					X509Certificate cert = (X509Certificate) certFactory
							.generateCertificate(inputStream);
					c.add(cert);
				} catch (Exception e) {
					if (nameList != null) {
						logger.debug(nameList[i]
								+ " can not be parsed as an X509Certificate.");
					} else {
						logger.error("failed to parse an X509Certificate");
					}
				}
			}
		}
		if (c.isEmpty())
			return null;
		return c.toArray(new X509Certificate[0]);
	}

	/**
	 * Gets the CRLs returned by the MyProxy server.
	 * 
	 * @return CRLs or null if none available
	 */
	public X509CRL[] getCRLs() throws CertificateException {
		if (trustrootData == null)
			return null;
		CertificateFactory certFactory = CertificateFactory
				.getInstance("X.509");
		Collection<X509CRL> c = new ArrayList<X509CRL>(trustrootData.length);
		for (int i = 0; i < trustrootData.length; i++) {
			String crlData = trustrootData[i];
			int index = crlData.indexOf("-----BEGIN X509 CRL-----");
			if (index >= 0) {
				crlData = crlData.substring(index);
				ByteArrayInputStream inputStream = new ByteArrayInputStream(
						crlData.getBytes());
				try {
					X509CRL crl = (X509CRL) certFactory
							.generateCRL(inputStream);
					c.add(crl);
				} catch (Exception e) {
					logger.error(this.trustrootFilenames[i]
							+ " can not be parsed as an X509CRL.");
				}
			}
		}
		if (c.isEmpty())
			return null;
		return c.toArray(new X509CRL[0]);
	}

	/**
     * Returns the trusted certificates directory location where
     * writeTrustRoots() will store certificates. If the path
     * has not explicitly been set, it follows standard Globus
     * path checking. It first checks the X509_CERT_DIR system
     * property. If that property is not set, it uses
     * ${user.home}/.globus/certificates.
     * Note that, unlike CoGProperties.getCaCertLocations(),
     * it does not return /etc/grid-security/certificates or
     * ${GLOBUS_LOCATION}/share/certificates.
     */
    public String getTrustRootPath() 
    {
    	String path = null;
    	
    	if (this.trustedCALocation == null || 
    			StringUtils.isEmpty(this.trustedCALocation.getCaPath())) 
    	{
//    		path = System.getProperty("X509_CERT_DIR");
//	        if (path == null) {
//	        	path = System.getProperty("user.home") + TRUSTED_CERT_PATH;
//	        }
    		path = System.getProperty("java.io.tmpdir") + File.separator + 
    				Slug.toSlug(host) + File.separator + "certificates";
    	}
    	else
    	{
    		path = trustedCALocation.getCaPath();
    	}
    	
    	new File(path).mkdirs();
    	
        return path;
    }

	/**
	 * Gets the existing trusted CA certificates directory.
	 * 
	 * @return directory path string or null if none found
	 */
	public String getExistingTrustRootPath() {
		String path, GL;

		path = getTrustRootPath();
		
//		if (path == null) {
//			path = getDir("/etc/grid-security/certificates");
//		}
//		if (path == null) {
//			GL = System.getenv("GLOBUS_LOCATION");
//			if (GL == null) {
//				GL = System.getProperty("GLOBUS_LOCATION");
//			}
//			path = getDir(GL + File.separator + "share" + File.separator
//					+ "certificates");
//		}

		return path;
	}

	/**
	 * Returns the default Globus proxy file location.
	 */
	public String getProxyLocation() throws IOException {
		
		String suffix = null;

		File proxyDir = new File(System.getProperty("java.io.tmpdir") + File.separator + 
				Slug.toSlug(host));
		
		proxyDir.mkdirs();
    	
		if (username == null) {
			suffix = "nousername-" + System.currentTimeMillis();
		} else {
			suffix = username;
		}
		
		return proxyDir.getAbsolutePath() + File.separator
				+ X509_USER_PROXY_FILE + suffix;
	}

	/**
	 * Provides a simple command-line interface.
	 */
	public static void main(String[] args) {
		try {
			AgaveMyProxyLogon m = new AgaveMyProxyLogon("myproxy.teragrid.org", DEFAULT_PORT);
			// Console cons = System.console();
			String passphrase = null;
			X509Certificate[] CAcerts;
			X509CRL[] CRLs;
			
			m.setUsername("dooley");
			System.out.println("Warning: terminal will echo passphrase as you type.");
			System.out.print("MyProxy Passphrase: ");
			passphrase = readLine(System.in);
			
			if (passphrase == null) {
				System.err.println("Error reading passphrase.");
				System.exit(1);
			}
			m.setPassphrase(passphrase);
			m.requestTrustRoots(true);
			GSSCredential proxy = m.getCredentials();
			m.writeProxyFile();
			
			Assert.assertNotNull(proxy, "No proxy retrieved");
			Assert.assertTrue(new File(m.getTrustRootPath()).exists(), "Trusted CA folder was not created");
			Assert.assertTrue(new File(m.getTrustRootPath()).list().length > 0, "Trusted CA folder is empty");
			
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
	}

	private static void printB64(byte[] data, PrintStream out) {
		byte[] b64data;

		b64data = Base64.encode(data);
		for (int i = 0; i < b64data.length; i += b64linelen) {
			if ((b64data.length - i) > b64linelen) {
				out.write(b64data, i, b64linelen);
			} else {
				out.write(b64data, i, b64data.length - i);
			}
			out.println();
		}
	}

	private static void printCert(X509Certificate certificate, PrintStream out)
			throws CertificateEncodingException {
		out.println("-----BEGIN CERTIFICATE-----");
		printB64(certificate.getEncoded(), out);
		out.println("-----END CERTIFICATE-----");
	}

	private static void printKey(PrivateKey key, PrintStream out)
			throws IOException {
		out.println("-----BEGIN RSA PRIVATE KEY-----");
		ByteArrayInputStream inStream = new ByteArrayInputStream(
				key.getEncoded());
		ASN1InputStream derInputStream = new ASN1InputStream(inStream);
		ASN1Primitive keyInfo = derInputStream.readObject();
//		DERObject keyInfo = derInputStream.readObject();
		PrivateKeyInfo pkey = new PrivateKeyInfo((ASN1Sequence) keyInfo);
		ASN1Primitive derKey = pkey.getPrivateKey();
//		DERObject derKey = pkey.getPrivateKey();
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		DEROutputStream der = new DEROutputStream(bout);
		der.writeObject(derKey);
		printB64(bout.toByteArray(), out);
		out.println("-----END RSA PRIVATE KEY-----");
	}

	private static void setFilePermissions(String file, String mode) {
		String command = "chmod " + mode + " " + file;
		try {
			Runtime.getRuntime().exec(command);
		} catch (IOException e) {
			logger.error("Failed to run: " + command); // windows
		}
	}

	private static String readLine(InputStream is) throws IOException {
		StringBuffer sb = new StringBuffer();
		for (int c = is.read(); c > 0 && c != '\n'; c = is.read()) {
			sb.append((char) c);
		}
		if (sb.length() > 0) {
			return new String(sb);
		}
		return null;
	}

	private static String getDir(String path) {
		if (path == null)
			return null;
		File f = new File(path);
		if (f.isDirectory() && f.canRead()) {
			return f.getAbsolutePath();
		}
		return null;
	}
}
