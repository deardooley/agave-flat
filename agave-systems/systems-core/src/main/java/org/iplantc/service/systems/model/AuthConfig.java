/**
 * 
 */
package org.iplantc.service.systems.model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.globus.common.CoGProperties;
import org.globus.gsi.CredentialException;
import org.globus.gsi.GlobusCredential;
import org.globus.gsi.X509Credential;
import org.globus.gsi.gssapi.GlobusGSSCredentialImpl;
import org.globus.myproxy.MyProxy;
import org.globus.myproxy.MyProxyException;
import org.hibernate.cfg.NotYetImplementedException;
import org.ietf.jgss.GSSCredential;
import org.iplantc.service.common.auth.AgaveX509Credential;
import org.iplantc.service.common.auth.MyProxyClient;
import org.iplantc.service.common.auth.TrustedCALocation;
import org.iplantc.service.common.clients.MyProxyGatewayClient;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.systems.Settings;
import org.iplantc.service.systems.crypt.Encryption;
import org.iplantc.service.systems.exceptions.AuthConfigException;
import org.iplantc.service.systems.exceptions.EncryptionException;
import org.iplantc.service.systems.exceptions.RemoteCredentialException;
import org.iplantc.service.systems.exceptions.SystemArgumentException;
import org.iplantc.service.systems.exceptions.SystemException;
import org.iplantc.service.systems.model.enumerations.AuthConfigType;
import org.iplantc.service.systems.model.enumerations.CredentialServerProtocolType;
import org.iplantc.service.systems.util.ServiceUtils;
import org.iplantc.service.transfer.exceptions.RemoteDataException;
import org.iplantc.service.transfer.ftp.FTP;
import org.joda.time.DateTime;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;


/**
 * Contains the information needed to connect to a remote system. This information
 * is protocol independent. If a third-party authentication service is needed, an  
 * AuthenticationSystem can be added to act as a credential server. For direct 
 * connections (ie. ssh), the credentialServer is not needed.
 *  
 * @author dooley
 *
 */
@Entity
@Table(name = "authconfigs")
public class AuthConfig
{
	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger(AuthConfig.class);
	
	public static final AuthConfig IPLANT_IRODS_AUTH_CONFIG = 
			new AuthConfig(Settings.IRODS_USERNAME, 
								Settings.IRODS_PASSWORD, 
								AuthConfigType.PASSWORD);
	
	public static final AuthConfig IPLANT_MYPROXY_AUTH_CONFIG = 
			new AuthConfig(Settings.COMMUNITY_USERNAME, 
					Settings.COMMUNITY_PASSWORD, 
					AuthConfigType.X509,
					CredentialServer.IPLANT_MYPROXY);
	
	private Long 					id;
	private String 					username = null;
	private String 					password = null;
	private String 					credential = null;
	private String					publicKey = null;
	private String					privateKey = null;
	private boolean					systemDefault = false;
	private String					internalUsername = null;
	private Date 					lastUpdated = new Date();
	private Date 					created = new Date();
	private AuthConfigType 			type = AuthConfigType.X509;
	private CredentialServer 		credentialServer = null;
	private String 					trustedCaLocation = null;
	private RemoteConfig			remoteConfig;
	
	public AuthConfig() {}
	
	public AuthConfig(String username, String password) 
	{
		this(username, password, AuthConfigType.PASSWORD);
	}
	
	public AuthConfig(String username, String password, AuthConfigType type)
	{ 
		this(username, password, type, null);
	}
	
	public AuthConfig(String username, String password,
			AuthConfigType type, CredentialServer credentialServer)
	{
		this(null, username, password, type, credentialServer);
	}
	
	public AuthConfig(String internalUsername, String username, String password,
			AuthConfigType type, CredentialServer credentialServer) 
	{
		setInternalUsername(internalUsername);
		setUsername(username);
		setPassword(password);
		setType(type);
		setCredentialServer(credentialServer);
	}
	
	public AuthConfig(String internalUsername, String username, String password,
			String credential, AuthConfigType type, 
			CredentialServer credentialServer)
	{
		setInternalUsername(internalUsername);
		setUsername(username);
		setPassword(password);
		setType(type);
		setCredential(credential);
		setCredentialServer(credentialServer);
	}
	
	/**
	 * @return the id
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", unique = true, nullable = false)
	public Long getId()
	{
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(Long id)
	{
		this.id = id;
	}
	
	/**
	 * @return the systemDefault
	 */
	@Column(name = "system_default", columnDefinition = "TINYINT(1)")
	public boolean isSystemDefault()
	{
		return systemDefault;
	}

	/**
	 * @param systemDefault the systemDefault to set
	 */
	public void setSystemDefault(boolean systemDefault)
	{
		this.systemDefault = systemDefault;
	}

	/**
	 * @return the internalUsername
	 */
	@Column(name = "internal_username", length=32)
	public String getInternalUsername()
	{
		return internalUsername;
	}

	/**
	 * @param internalUsername the internalUsername to set
	 */
	public void setInternalUsername(String internalUsername)
	{
		if (!StringUtils.isEmpty(internalUsername) && internalUsername.length() > 32) {
			throw new SystemException("'auth.internalUsername' must be less than 32 characters.");
		}
		
		this.internalUsername = internalUsername;
	}

	/**
	 * @return the username
	 */
	@Column(name = "username", nullable = true, length = 32)
	public String getUsername()
	{
		return username;
	}

	/**
	 * @param username the username to set
	 */
	public void setUsername(String username)
	{
		if (!StringUtils.isEmpty(username) && username.length() > 32) {
			throw new SystemException("'auth.username' must be less than 32 characters.");
		}
		
		this.username = username;
	}

	/**
	 * @return the remoteConfig
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "remote_config_id")
	public RemoteConfig getRemoteConfig()
	{
		return remoteConfig;
	}

	/**
	 * @param remoteConfig the remoteConfig to set
	 */
	public void setRemoteConfig(RemoteConfig remoteConfig)
	{
		this.remoteConfig = remoteConfig;
	}

	/**
	 * @return the password
	 */
	@Column(name = "`password`", nullable = true, length = 128)
	public String getPassword()
	{
		return password;
	}
	
	/**
	 * @return the password
	 * @throws EncryptionException 
	 */
	@Transient
	public String getClearTextPassword(String encryptionKey) throws EncryptionException
	{
		if (StringUtils.isEmpty(password)) {
			return null;
		} else {
			Encryption crypt = new Encryption();
			try
			{
				crypt.setPassword(encryptionKey.toCharArray());
				return crypt.decrypt(password);
			}
			catch (Exception e)
			{
				throw new EncryptionException("Unable to decrypt the password");
			}
		}
	}
	
	/**
	 * @return the password
	 * @throws EncryptionException 
	 */
	@Transient
	public void encryptCurrentPassword(String encryptionKey) throws EncryptionException
	{
		if (StringUtils.isEmpty(password)) {
			throw new EncryptionException("Cannot encrypt an empty password");
		} else if (StringUtils.isEmpty(encryptionKey)) {
			throw new EncryptionException("Cannot encrypt passwords using an empty key");
		} else {
			Encryption crypt = new Encryption();
			try
			{
//				String oldPassword = password;
				crypt.setPassword(encryptionKey.toCharArray());
				this.password = crypt.encrypt(this.password);
//				System.out.println("Username: " + username + " Key: " + encryptionKey + " Pass: " + oldPassword + " => " + password);
//				System.out.println("Username: " + username + " Key: " + encryptionKey + " Pass: " + password + " => " + crypt.decrypt(password));
			}
			catch (Exception e)
			{
				throw new EncryptionException("Unable to encrypt the password");
			}
		}
	}
	
	/**
	 * @return the publicKey
	 * @throws EncryptionException 
	 */
	@Transient
	public String getClearTextPublicKey(String encryptionKey) throws EncryptionException
	{
		if (StringUtils.isEmpty(publicKey)) {
			return null;
		} else {
			Encryption crypt = new Encryption();
			try
			{
				crypt.setPassword(encryptionKey.toCharArray());
				return crypt.decrypt(publicKey);
			}
			catch (Exception e)
			{
				throw new EncryptionException("Unable to decrypt the publicKey");
			}
		}
	}
	
	/**
	 * @return the privateKey
	 * @throws EncryptionException 
	 */
	@Transient
	public String getClearTextPrivateKey(String encryptionKey) throws EncryptionException
	{
		if (StringUtils.isEmpty(privateKey)) {
			return null;
		} else {
			Encryption crypt = new Encryption();
			try
			{
				crypt.setPassword(encryptionKey.toCharArray());
				return crypt.decrypt(privateKey);
			}
			catch (Exception e)
			{
				throw new EncryptionException("Unable to decrypt the privateKey");
			}
		}
	}
	
	/**
	 * @return the publicKey
	 * @throws EncryptionException 
	 */
	@Transient
	public void encryptCurrentPublicKey(String encryptionKey) throws EncryptionException
	{
		if (StringUtils.isEmpty(publicKey)) {
			throw new EncryptionException("Cannot encrypt an empty publicKey");
		} else if (StringUtils.isEmpty(encryptionKey)) {
			throw new EncryptionException("Cannot encrypt publicKey using an empty key");
		} else {
			Encryption crypt = new Encryption();
			try
			{
				crypt.setPassword(encryptionKey.toCharArray());
				this.publicKey = crypt.encrypt(this.publicKey);
			}
			catch (Exception e)
			{
				throw new EncryptionException("Unable to encrypt the publicKey");
			}
		}
	}
	
	/**
	 * @return the privateKey
	 * @throws EncryptionException 
	 */
	@Transient
	public void encryptCurrentPrivateKey(String encryptionKey) throws EncryptionException
	{
		if (StringUtils.isEmpty(privateKey)) {
			throw new EncryptionException("Cannot encrypt an empty privateKey");
		} else if (StringUtils.isEmpty(encryptionKey)) {
			throw new EncryptionException("Cannot encrypt privateKey using an empty key");
		} else {
			Encryption crypt = new Encryption();
			try
			{
				crypt.setPassword(encryptionKey.toCharArray());
				this.privateKey = crypt.encrypt(this.privateKey);
			}
			catch (Exception e)
			{
				throw new EncryptionException("Unable to encrypt the privateKey");
			}
		}
	}
	
	/**
	 * Decrypts the current credential using the encryptionKey provided.
	 * 
	 * @param encryptionKey Same key used to encrypt the credential
	 * @return String clear text credential 
	 * @throws EncryptionException
	 */
	@Transient
	public String getClearTextCredential(String encryptionKey) throws EncryptionException
	{
		if (StringUtils.isEmpty(credential)) {
			return null;
		} else {
			Encryption crypt = new Encryption();
			try
			{
				crypt.setPassword(encryptionKey.toCharArray());
				return crypt.decrypt(getCredential());
			}
			catch (Exception e)
			{
				throw new EncryptionException("Unable to decrypt the credential");
			}
		}
	}
	
	/**
	 * Encrypts the current credential in place.
	 * 
	 * @param encryptionKey
	 * @throws EncryptionException
	 */
	@Transient
	public void encryptCurrentCredential(String encryptionKey) throws EncryptionException
	{
		if (StringUtils.isEmpty(credential)) {
			throw new EncryptionException("Cannot encrypt an empty credential");
		} else if (StringUtils.isEmpty(encryptionKey)) {
			throw new EncryptionException("Cannot encrypt credential using an empty key");
		} else {
			Encryption crypt = new Encryption();
			try
			{
//				String oldCred = credential;
				crypt.setPassword(encryptionKey.toCharArray());
				this.credential = crypt.encrypt(this.credential);
//				System.out.println("Username: " + username + " Key: " + encryptionKey + " Credential: " + oldCred + " => " + credential);
//				System.out.println("Username: " + username + " Key: " + encryptionKey + " Pass: " + credential + " => " + crypt.decrypt(credential));
			}
			catch (Exception e)
			{
				throw new EncryptionException("Unable to encrypt the credential");
			}
		}
	}

	/**
	 * @param password the password to set
	 * @throws SystemException 
	 */
	public void setPassword(String password)
	{
		this.password = password;
	}	

	/**
	 * @return the credential
	 */
	@Column(name = "credential", columnDefinition="TEXT")
	public String getCredential()
	{
		return credential;
	}
	
	/**
	 * @param credential the credential to set
	 * @throws SystemException 
	 */
	public void setCredential(String credential)
	{
		if (!StringUtils.isEmpty(credential) && credential.length() > 8192) {
			throw new SystemException("'auth.credential' must be less than 8192 characters.");
		}
		
		this.credential = credential;
	}

	/**
	 * Returns the trusted CA location for interacting with target systems.
	 * The URL is expected to return a zip or tar archive with the trusted
	 * certs in it. 
	 * @return
	 */
	@Column(name = "trusted_ca_url", length = 256)
	public String getTrustedCaLocation() {
		return trustedCaLocation;
	}

	/**
	 * Sets a url to a zip or tar archive with the trusted
	 * certs in it.
	 * @param trustedCaLocation
	 */
	public void setTrustedCaLocation(String trustedCaLocation) {
		this.trustedCaLocation = trustedCaLocation;
	}

	/**
	 * @return publicKey
	 */
	@Column(name = "public_key", columnDefinition="TEXT")
	public String getPublicKey() {
		return publicKey;
	}

	/**
	 * @param publicKey
	 */
	public void setPublicKey(String publicKey) {
	    if (!StringUtils.isEmpty(publicKey) && publicKey.length() >= 8192) {
            throw new SystemException("'auth.publicKey' must be less than 8192 characters.");
        }
	    
	    this.publicKey = publicKey;
	}

	/**
	 * @return privateKey
	 */
	@Column(name = "private_key", columnDefinition="TEXT")
	public String getPrivateKey() {
		return privateKey;
	}

	/**
	 * @param privateKey
	 */
	public void setPrivateKey(String privateKey) {
	    if (!StringUtils.isEmpty(privateKey) && privateKey.length() >= 16384) {
            throw new SystemException("'auth.privateKey' must be less than 16384 characters.");
        }
        
        this.privateKey = privateKey;
	}

	/**
	 * @return the type
	 */
	@Enumerated(EnumType.STRING)
	@Column(name = "login_credential_type", nullable = false, length = 16)
	public AuthConfigType getType()
	{
		return type;
	}

	/**
	 * @param type the type to set
	 */
	public void setType(AuthConfigType type)
	{
		this.type = type;
	}

	/**
	 * @return the credentialServer
	 */
	@OneToOne(fetch = FetchType.EAGER, cascade={CascadeType.ALL,CascadeType.REMOVE}, targetEntity=CredentialServer.class )
	@JoinColumn(name = "authentication_system_id")
	public CredentialServer getCredentialServer()
	{
		return credentialServer;
	}

	/**
	 * @param credentialServer the credentialServer to set
	 */
	public void setCredentialServer(CredentialServer credentialServer)
	{
		this.credentialServer = credentialServer;
	}
	
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "last_updated", nullable = false, length = 19)
	public Date getLastUpdated()
	{
		return this.lastUpdated;
	}

	public void setLastUpdated(Date lastUpdated)
	{
		this.lastUpdated = lastUpdated;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "created", nullable = false, length = 19)
	public Date getCreated()
	{
		return this.created;
	}

	public void setCreated(Date created)
	{
		this.created = created;
	}

	public static AuthConfig fromJSON(JSONObject jsonAuthConfig) throws SystemArgumentException
	{
		return fromJSON(jsonAuthConfig, null);
	}
	
	public static AuthConfig fromJSON(JSONObject jsonAuthConfig, AuthConfig authConfig) throws SystemArgumentException
	{
		if (authConfig == null) {
			authConfig = new AuthConfig();
		}
		
		try {
			
			if (jsonAuthConfig.has("internalUsername"))
			{
				if (ServiceUtils.isNonEmptyString(jsonAuthConfig, "auth.internalUsername")) {
					authConfig.setInternalUsername(jsonAuthConfig.getString("auth.internalUsername"));
				}
				else {
					throw new SystemArgumentException("Invalid 'auth.internalUsername' value. " +
							"If provided, please specify a valid username for an internal user path.");
				}
			} else {
				authConfig.setInternalUsername(null);
			}
			
			if (ServiceUtils.isNonEmptyString(jsonAuthConfig, "type"))
			{
				try 
				{
					AuthConfigType credentialType = AuthConfigType.valueOf(jsonAuthConfig.getString("type").toUpperCase());
					authConfig.setType(credentialType);
					
				} catch (Exception e) {
					throw new SystemArgumentException("Invalid 'auth.type' value. " +
							"Please specify one of: '" + ServiceUtils.explode("', '", Arrays.asList(AuthConfigType.values())) + "'");
				}
			} 
			else
			{
				throw new SystemArgumentException("No 'auth.type' value specified. " +
						"Please specify one of: '" + ServiceUtils.explode("', '", Arrays.asList(AuthConfigType.values())) + "'");
			}
			
			if (jsonAuthConfig.has("username") && !jsonAuthConfig.isNull("username"))
			{
				if (ServiceUtils.isValidString(jsonAuthConfig, "username")) {
					authConfig.setUsername(jsonAuthConfig.getString("username"));
				} else if (authConfig.getType().equals(AuthConfigType.ANONYMOUS)) {
					authConfig.setUsername(FTP.ANONYMOUS_USER);
				} else {
					throw new SystemArgumentException("Invalid 'auth.username' value. " +
							"If provided, please specify a valid string value.");
				}
			} else {
				if (authConfig.getType().equals(AuthConfigType.ANONYMOUS)) {
					authConfig.setUsername(FTP.ANONYMOUS_USER);
				} else {
					authConfig.setUsername(null);
				}
			}
			
			if (jsonAuthConfig.has("password") && !jsonAuthConfig.isNull("password"))
			{
				if (ServiceUtils.isValidString(jsonAuthConfig, "password")) 
				{
					String password = jsonAuthConfig.getString("password");
					
					if (!StringUtils.isEmpty(password) && password.length() > 128) {
						throw new SystemArgumentException("Invalid 'auth.password' value. " +
							"If provided, please specify a string value less than 128 characters.");
					}
					authConfig.setPassword(password);
				} else {
					throw new SystemArgumentException("Invalid 'auth.password' value. " +
							"If provided, please specify a valid string value.");
				}
			} else {
				authConfig.setPassword(null);
			}
			
			if (jsonAuthConfig.has("publicKey") && !jsonAuthConfig.isNull("publicKey"))
			{
				if (ServiceUtils.isValidString(jsonAuthConfig, "publicKey")) 
				{
					String publicKey = jsonAuthConfig.getString("publicKey");
					
					if (!StringUtils.isEmpty(publicKey) && publicKey.length() > 4099) {
						throw new SystemArgumentException("Invalid 'auth.publicKey' value. " +
							"If provided, please specify a string value less than 4099 characters.");
					}
					authConfig.setPublicKey(publicKey);
				} else {
					throw new SystemArgumentException("Invalid 'auth.publicKey' value. " +
							"If provided, please specify a valid string value.");
				}
			} else {
				authConfig.setPublicKey(null);
			}
			
			if (jsonAuthConfig.has("privateKey") && !jsonAuthConfig.isNull("privateKey"))
			{
				if (ServiceUtils.isValidString(jsonAuthConfig, "privateKey")) 
				{
					String privateKey = jsonAuthConfig.getString("privateKey");
					
					if (!StringUtils.isEmpty(privateKey) && privateKey.length() > 4099) {
						throw new SystemArgumentException("Invalid 'auth.privateKey' value. " +
							"If provided, please specify a string value less than 4099 characters.");
					}
					authConfig.setPrivateKey(privateKey);
				} else {
					throw new SystemArgumentException("Invalid 'auth.privateKey' value. " +
							"If provided, please specify a valid string value.");
				}
			} else {
				authConfig.setPrivateKey(null);
			}
			
			if (jsonAuthConfig.has("credential") && !jsonAuthConfig.isNull("credential"))
			{
				if (ServiceUtils.isValidString(jsonAuthConfig, "credential") && 
						StringUtils.isNotEmpty(jsonAuthConfig.getString("credential"))) {
					String cred = jsonAuthConfig.getString("credential");
					cred = cred.replaceAll("\\\\n", "\n");
					cred = cred.replaceAll("\\r\\n", "\n");
					authConfig.setCredential(cred);
				} else {
					throw new SystemArgumentException("Invalid 'auth.credential' value. " +
							"If provided, please specify a valid string representation of your credential.");
				}
			} else {
				authConfig.setCredential(null);
			}
			
			if (jsonAuthConfig.has("caCerts") && !jsonAuthConfig.isNull("caCerts"))
			{
				if (ServiceUtils.isValidString(jsonAuthConfig, "caCerts")) 
				{
					String caCertUrl = jsonAuthConfig.getString("caCerts");
					if (!StringUtils.isEmpty(caCertUrl) ) {
						try {
							URL caUrl = new URL(caCertUrl);
							if (StringUtils.equals(caUrl.getProtocol(), "http") ||
									StringUtils.equals(caUrl.getProtocol(), "http")) {
								authConfig.setTrustedCaLocation(caCertUrl);
							}
							else {
								throw new SystemArgumentException("Invalid trusted CA location. "
										+ "Please provided a valid web-accessible URL.");
							}
						} catch (SystemArgumentException e) {
							throw e;
						} catch (Exception e) {
							throw new SystemArgumentException("Invalid trusted CA location. "
									+ "Please provided a valid web-accessible URL.");
						}
					}
				}
			} else {
				authConfig.setTrustedCaLocation(null);
			}
						
			if (jsonAuthConfig.has("server"))
			{
				JSONObject jsonAuth = jsonAuthConfig.getJSONObject("server");
				
				if (jsonAuth == null) {
					throw new SystemArgumentException("Invalid 'auth.server' value. Please specify a " +
							"JSON object representing a valid 'auth.server' configuration.");
				}
				else
				{
					CredentialServer credentialServer = 
							CredentialServer.fromJSON(jsonAuth, authConfig.getCredentialServer());
					authConfig.setCredentialServer(credentialServer);
				}
			} else {
				authConfig.setCredentialServer(null);
			}
			
			AuthConfig.validateConfiguration(authConfig);
		}
		catch (SystemArgumentException e) {
			throw e;
		}
		catch (JSONException e) {
			throw new SystemArgumentException("Failed to parse 'auth' object", e);
		}
		catch (Exception e) {
			throw new SystemArgumentException("Failed to parse 'auth' object: " + e.getMessage(), e);
		}
		
		return authConfig;
	}
	
	/**
	 * Verifies that an AuthConfig has a valid combination of values.
	 * 
	 * @param authConfig
	 * @throws SystemArgumentException
	 */
	public static void validateConfiguration(AuthConfig authConfig) 
	throws SystemArgumentException
	{
		if (authConfig.getType().equals(AuthConfigType.PASSWORD))
		{
			if (StringUtils.isEmpty(authConfig.getUsername())) {
				throw new SystemArgumentException("Invalid authentication combination. " +
						"PASSWORD 'auth.type' specified, but no 'auth.username' provided.");
			} 
			else if (StringUtils.isEmpty(authConfig.getPassword())) {
				throw new SystemArgumentException("Invalid authentication combination. " +
						"PASSWORD 'auth.type' specified, but no 'auth.password' provided.");
			}
			else if (!StringUtils.isEmpty(authConfig.getCredential())) 
			{
				throw new SystemArgumentException("Invalid authentication combination. " +
						"'auth.credential' is not supported when 'auth.type' is PASSWORD");
			} 
			else if (authConfig.getCredentialServer() != null) 
			{
				throw new SystemArgumentException("Invalid authentication combination. " +
						"'auth.server' is not supported when 'auth.type' is PASSWORD");
			}
			else if (!StringUtils.isEmpty(authConfig.getPublicKey())) {
				throw new SystemArgumentException("Invalid authentication combination. " +
						"'auth.publicKey' is not supported when 'auth.type' is PASSWORD");
			} 
			else if (!StringUtils.isEmpty(authConfig.getPrivateKey())) {
				throw new SystemArgumentException("Invalid authentication combination. " +
						"'auth.privateKey' is not supported when 'auth.type' is PASSWORD");
			}
		}
		else if (authConfig.getType().equals(AuthConfigType.ANONYMOUS))
		{
			if (!StringUtils.equals(authConfig.getUsername(), FTP.ANONYMOUS_USER)) 
			{
				throw new SystemArgumentException("Invalid authentication combination. " +
						"ANONYMOUS 'auth.username', if specified, should be " + 
						FTP.ANONYMOUS_USER + ".");
			}
			
			if (authConfig.getPassword() != null) 
			{
				if (!StringUtils.equals(authConfig.getPassword(), FTP.ANONYMOUS_PASSWORD) && 
						!ServiceUtils.isEmailAddress(authConfig.getPassword()))  
				{
					throw new SystemArgumentException("Invalid authentication combination. " +
						"ANONYMOUS 'auth.password', if specified, should be a valid email address or " + 
						FTP.ANONYMOUS_PASSWORD + ".");
				} 
			}
			
			if (!StringUtils.isEmpty(authConfig.getCredential())) 
			{
				throw new SystemArgumentException("Invalid authentication combination. " +
						"'auth.credential' is not supported when 'auth.type' is ANONYMOUS");
			} 
			
			if (authConfig.getCredentialServer() != null) 
			{
				throw new SystemArgumentException("Invalid authentication combination. " +
						"'auth.server' is not supported when 'auth.type' is ANONYMOUS");
			}
			
			if (!StringUtils.isEmpty(authConfig.getPublicKey())) {
				throw new SystemArgumentException("Invalid authentication combination. " +
						"'auth.publicKey' is not supported when 'auth.type' is ANONYMOUS");
			} 
			
			if (!StringUtils.isEmpty(authConfig.getPrivateKey())) {
				throw new SystemArgumentException("Invalid authentication combination. " +
						"'auth.privateKey' is not supported when 'auth.type' is ANONYMOUS");
			}
		}
		else if (authConfig.getType().equals(AuthConfigType.PAM))
		{
			if (StringUtils.isEmpty(authConfig.getUsername())) {
				throw new SystemArgumentException("Invalid authentication combination. " +
						"PAM 'auth.type' specified, but no 'auth.username' provided.");
			} 
			else if (StringUtils.isEmpty(authConfig.getPassword())) {
				throw new SystemArgumentException("Invalid authentication combination. " +
						"PAM 'auth.type' specified, but no 'auth.password' provided.");
			}
			else if (!StringUtils.isEmpty(authConfig.getCredential())) 
			{
				throw new SystemArgumentException("Invalid authentication combination. " +
						"'auth.credential' is not supported when 'auth.type' is PAM");
			} 
			else if (authConfig.getCredentialServer() != null) 
			{
				throw new SystemArgumentException("Invalid authentication combination. " +
						"'auth.server' is not supported when 'auth.type' is PAM");
			}
			else if (!StringUtils.isEmpty(authConfig.getPublicKey())) {
				throw new SystemArgumentException("Invalid authentication combination. " +
						"'auth.publicKey' is not supported when 'auth.type' is PAM");
			} 
			else if (!StringUtils.isEmpty(authConfig.getPrivateKey())) {
				throw new SystemArgumentException("Invalid authentication combination. " +
						"'auth.privateKey' is not supported when 'auth.type' is PAM");
			}
		}
		else if (authConfig.getType().equals(AuthConfigType.KERBEROSE))
		{
			if (StringUtils.isEmpty(authConfig.getUsername())) {
				throw new SystemArgumentException("Invalid authentication combination. " +
						"KERBEROSE 'auth.type' specified, but no 'auth.username' provided.");
			} 
			else if (StringUtils.isEmpty(authConfig.getPassword())) {
				throw new SystemArgumentException("Invalid authentication combination. " +
						"KERBEROSE 'auth.type' specified, but no 'auth.password' provided.");
			} 
			else if (!StringUtils.isEmpty(authConfig.getCredential())) 
			{
				throw new SystemArgumentException("Invalid authentication combination. " +
						"'auth.credential' is not supported when 'auth.type' is KERBEROSE");
			} 
			// remote kerberose auth is disabled at the moment. Instead we use it as ssh
			else if (authConfig.getCredentialServer() != null) 
			{
				throw new SystemArgumentException("Invalid authentication combination. " +
						"'auth.server' is not supported when 'auth.type' is KERBEROSE");
			}
			else if (!StringUtils.isEmpty(authConfig.getPublicKey())) {
				throw new SystemArgumentException("Invalid authentication combination. " +
						"'auth.publicKey' is not supported when 'auth.type' is KERBEROSE");
			} 
			else if (!StringUtils.isEmpty(authConfig.getPrivateKey())) {
				throw new SystemArgumentException("Invalid authentication combination. " +
						"'auth.privateKey' is not supported when 'auth.type' is KERBEROSE");
			}
		} 
		else if (authConfig.getType().equals(AuthConfigType.TOKEN))
		{
			if (authConfig.getCredentialServer() == null) 
			{
				if (StringUtils.isEmpty(authConfig.getCredential())) 
				{
					throw new SystemArgumentException("Invalid authentication combination. " +
							"TOKEN 'auth.type' specified, but no 'auth.credential' or " +
							"'auth.server' provided.");
				} 
				else if (StringUtils.isEmpty(authConfig.getPassword())) 
				{
					throw new SystemArgumentException("Invalid authentication combination. " +
							"TOKEN 'auth.type' specified, but no 'auth.credential' or " +
							"'auth.password' provided.");
				}
				else if (StringUtils.isEmpty(authConfig.getUsername())) 
				{
					throw new SystemArgumentException("Invalid authentication combination. " +
							"TOKEN 'auth.type' specified, but no 'auth.username' provided.");
				}
				else if (!StringUtils.isEmpty(authConfig.getPublicKey())) {
					throw new SystemArgumentException("Invalid authentication combination. " +
							"'auth.publicKey' is not supported when 'auth.type' is TOKEN");
				} 
				else if (!StringUtils.isEmpty(authConfig.getPrivateKey())) {
					throw new SystemArgumentException("Invalid authentication combination. " +
							"'auth.privateKey' is not supported when 'auth.type' is TOKEN");
				}
			}
			else 
			{
				if (!authConfig.getType().accepts(authConfig.getCredentialServer().getProtocol()))
				{
					throw new SystemArgumentException("Invalid authentication combination. " +
							"'auth.server.protocol' must be OAUTH2 when 'auth.type' is TOKEN");
				}
				else if (StringUtils.isEmpty(authConfig.getCredential()) && 
						StringUtils.isEmpty(authConfig.getPassword())) 
				{
					throw new SystemArgumentException("Invalid authentication combination. " +
							"TOKEN 'auth.type' specified, but no 'auth.password' or " +
							"'auth.credential' provided.");
				} 
				else if (StringUtils.isEmpty(authConfig.getUsername())) 
				{
					throw new SystemArgumentException("Invalid authentication combination. " +
							"TOKEN 'auth.type' specified, but no 'auth.username' provided.");
				}
				else if (!StringUtils.isEmpty(authConfig.getPublicKey())) {
					throw new SystemArgumentException("Invalid authentication combination. " +
							"'auth.publicKey' is not supported when 'auth.type' is TOKEN");
				} 
				else if (!StringUtils.isEmpty(authConfig.getPrivateKey())) {
					throw new SystemArgumentException("Invalid authentication combination. " +
							"'auth.privateKey' is not supported when 'auth.type' is TOKEN");
				}
			}
		}
		else if (authConfig.getType().equals(AuthConfigType.X509))
		{
			if (authConfig.getCredentialServer() == null) 
			{
				if (StringUtils.isEmpty(authConfig.getCredential())) 
				{
					throw new SystemArgumentException("Invalid authentication combination. " +
							"'auth.credential' is required when 'auth.type' is X509 and " +
							"'auth.server' is not provided.");
				}
				else if (!StringUtils.isEmpty(authConfig.getUsername())) 
				{
					throw new SystemArgumentException("Invalid authentication combination. " +
							"'auth.username' is not supported when 'auth.type' is X509");
				}
				else if (!StringUtils.isEmpty(authConfig.getPublicKey())) {
					throw new SystemArgumentException("Invalid authentication combination. " +
							"'auth.publicKey' is not supported when 'auth.type' is X509");
				} 
				else if (!StringUtils.isEmpty(authConfig.getPrivateKey())) {
					throw new SystemArgumentException("Invalid authentication combination. " +
							"'auth.privateKey' is not supported when 'auth.type' is X509");
				}
				
				try {
					AuthConfig.testCredential(authConfig);
				} catch (IOException e) {
					throw new SystemArgumentException("Unable to verify credential. " + e.getMessage());
				}
			}
			else 
			{
				if (!authConfig.getType().accepts(authConfig.getCredentialServer().getProtocol()))
				{
					throw new SystemArgumentException("Invalid authentication combination. " +
							"'auth.server.protocol' must be MYPROXY or MPG when 'auth.type' is X509");
				} 
				else if (StringUtils.isEmpty(authConfig.getUsername())) 
				{
					throw new SystemArgumentException("Invalid authentication combination. " +
							"'auth.username' is required when 'auth.type' is X509 and " +
							"'auth.server' is provided.");
				}
				else if (StringUtils.isEmpty(authConfig.getPassword()) && 
						!authConfig.getCredentialServer().getProtocol().equals(CredentialServerProtocolType.MPG)) 
				{
					throw new SystemArgumentException("Invalid authentication combination. " +
							"'auth.password' is required when 'auth.type' is X509 and " +
							"'auth.server' is provided.");
				}
				else if (!StringUtils.isEmpty(authConfig.getPublicKey())) {
					throw new SystemArgumentException("Invalid authentication combination. " +
							"'auth.publicKey' is not supported when 'auth.type' is X509");
				} 
				else if (!StringUtils.isEmpty(authConfig.getPrivateKey())) {
					throw new SystemArgumentException("Invalid authentication combination. " +
							"'auth.privateKey' is not supported when 'auth.type' is X509");
				}
			}
		}
		else if (authConfig.getType().equals(AuthConfigType.LOCAL))
		{
			if (!StringUtils.isEmpty(authConfig.getCredential()) || 
					!StringUtils.isEmpty(authConfig.getUsername()) || 
					!StringUtils.isEmpty(authConfig.getPassword()) || 
					!StringUtils.isEmpty(authConfig.getPublicKey()) || 
					!StringUtils.isEmpty(authConfig.getPrivateKey()) || 
					authConfig.getCredentialServer() != null ) 
			{
				throw new SystemArgumentException("Invalid authentication combination. " +
						"'auth.server', 'auth.username', 'auth.password', and 'auth.credential' " +
						"are not supported when 'auth.type' is LOCAL");
			} 
		}
		else if (authConfig.getType().equals(AuthConfigType.SSHKEYS))
		{
			if (StringUtils.isEmpty(authConfig.getPublicKey())) {
				throw new SystemArgumentException("Invalid authentication combination. " +
						"SSHKEYS 'auth.type' specified, but no 'auth.publicKey' provided.");
			} 
			else if (StringUtils.isEmpty(authConfig.getPrivateKey())) {
				throw new SystemArgumentException("Invalid authentication combination. " +
						"SSHKEYS 'auth.type' specified, but no 'auth.privateKey' provided.");
			}
			else if (StringUtils.isEmpty(authConfig.getUsername())) {
				throw new SystemArgumentException("Invalid authentication combination. " +
						"'auth.username' is required when 'auth.type' is SSHKEYS");
			} 
			else if (!StringUtils.isEmpty(authConfig.getCredential())) 
			{
				throw new SystemArgumentException("Invalid authentication combination. " +
						"'auth.credential' is not supported when 'auth.type' is SSHKEYS");
			} 
			else if (authConfig.getCredentialServer() != null) 
			{
				throw new SystemArgumentException("Invalid authentication combination. " +
						"'auth.server' is not supported when 'auth.type' is SSHKEYS");
			}
		}
		else if (authConfig.getType().equals(AuthConfigType.APIKEYS))
		{
			if (!StringUtils.isEmpty(authConfig.getUsername())) {
				throw new SystemArgumentException("Invalid authentication combination. " +
						"'auth.username' is not supported when 'auth.type' is APIKEYS");
			} 
			else if (!StringUtils.isEmpty(authConfig.getPassword())) 
			{
				throw new SystemArgumentException("Invalid authentication combination. " +
						"'auth.password' is not supported when 'auth.type' is APIKEYS");
			} 
			else if (StringUtils.isEmpty(authConfig.getPublicKey())) {
				throw new SystemArgumentException("Invalid authentication combination. " +
						"APIKEYS 'auth.type' specified, but no 'auth.publicKey' provided.");
			} 
			else if (StringUtils.isEmpty(authConfig.getPrivateKey())) {
				throw new SystemArgumentException("Invalid authentication combination. " +
						"APIKEYS 'auth.type' specified, but no 'auth.privateKey' provided.");
			}
//			if (StringUtils.isEmpty(authConfig.getCredential())) 
//			{
//				if (StringUtils.isEmpty(authConfig.getPublicKey())) {
//					throw new SystemArgumentException("Invalid authentication combination. " +
//							"APIKEYS 'auth.type' specified, but no 'auth.publicKey' provided.");
//				} 
//				else if (StringUtils.isEmpty(authConfig.getPrivateKey())) {
//					throw new SystemArgumentException("Invalid authentication combination. " +
//							"SSHKEYS 'auth.type' specified, but no 'auth.privateKey' provided.");
//				}
//				else if (authConfig.getCredentialServer() == null) {
//					throw new SystemArgumentException("Invalid authentication combination. " +
//							"'auth.server' must be specified when 'auth.type' "
//							+ "is APIKEYS and no 'auth.credential', 'auth.publicKey', and "
//							+ "'auth.privateKey' are supplied.");
//				}
//			else
//			{
//				throw new SystemArgumentException("Invalid authentication combination. " +
//						"'auth.server' must be specified when 'auth.type' "
//						+ "is APIKEYS and no 'auth.credential' is supplied.");
//			}
		}
	}
	
	public static void testCredential(AuthConfig authConfig) 
	throws SystemArgumentException, IOException
	{
		InputStream is = null;
		File tempDir = null;
		try 
		{
			tempDir = new File(System.getProperty("java.io.tmpdir")
					+ File.separator + DigestUtils.md5Hex(authConfig.getCredential()));
			
			is = new ByteArrayInputStream(authConfig.getCredential().getBytes());
			
			TrustedCALocation trustedCALocation = new TrustedCALocation(tempDir.getAbsolutePath());
			trustedCALocation.fetchRemoteCACertBundle(authConfig.getTrustedCaLocation());
			X509Credential globusCred = new AgaveX509Credential(is, trustedCALocation);
//			try {
//				globusCred.verify();
//			} catch (CredentialException e) {
//				throw new SystemArgumentException("Invalid credential. " + e.getMessage(), e);
//			}
			
			GSSCredential proxy = new GlobusGSSCredentialImpl(globusCred, GSSCredential.INITIATE_AND_ACCEPT);
			if (proxy.getRemainingLifetime() == 0) {
				throw new SystemArgumentException("Invalid credential. The provided credential has already expired.");
			}
			
//			return trustedCaPath;
		} catch (MalformedURLException e) {
			throw new SystemArgumentException("Invalid url for trusted ca location.", e);
		} catch (org.ietf.jgss.GSSException | CredentialException e) {
			throw new SystemArgumentException("Unable to read the provided credential.", e);
		} finally {
			FileUtils.deleteQuietly(tempDir);
		}
	}

	@Transient
	public boolean isCredentialRequired() {
		return this.type.equals(AuthConfigType.TOKEN) ||
				this.type.equals(AuthConfigType.X509);
	}
	
	/**
	 * Returns whether the credential assigned to this auth config is
	 * expired. It does not, however, check the validity of the credential.
	 * If no credential is required, it returns false.
	 * @param salt
	 * @return
	 */
	public boolean isCredentialExpired(String salt) {
		if (!isCredentialRequired()) {
			return false;
		} else {
			return (getCredentialRemainingTime(salt) == 0);
		}
	}
	
	/**
	 * Checks for a credential and returns the time left in seconds if present.
	 * 
	 * @return 0 if no time is left or an exception, -1 if no credential is present, 
	 * the time remaining in seconds for this credential otherwise.
	 */
	@Transient
	public long getCredentialRemainingTime(String salt) 
	{
		if (this.type.equals(AuthConfigType.X509))
		{
			if (!StringUtils.isEmpty(credential)) {
				InputStream is = null;
				try {
					String cred = getClearTextCredential(salt);
					is = new ByteArrayInputStream(cred.getBytes());
					X509Credential globusCred = new X509Credential(is);
					GSSCredential proxy = new GlobusGSSCredentialImpl(globusCred, GSSCredential.INITIATE_AND_ACCEPT);
					
					return proxy.getRemainingLifetime();
				}
				catch (Exception e) {
					return 0;
				} finally {
					try { is.close(); } catch (Exception e) {}
				}
			} 
			else
			{
				return -1;
			}
		}
		else
		{
			return -1;
		}
	}
	
	@Transient
	public Object retrieveCredential(String salt) 
	throws RemoteCredentialException 
	{
		TrustedCALocation trustedCALocation = null;
		
		try 
		{
			if (isCredentialRequired()) {
				if (this.type.equals(AuthConfigType.X509)) 
				{
					if (credentialServer != null)
					{
//						File tempDir = new File(System.getProperty("java.io.tmpdir")
//								+ File.separator + DigestUtils.md5Hex(salt));
//						
						trustedCALocation = new TrustedCALocation(MyProxy.getTrustRootPath());
						// We will pull the trusted ca from the credential server, but 
						// go ahead and use the extra ca cert location if provided. 
						trustedCALocation.fetchRemoteCACertBundle(getTrustedCaLocation());
						
						if (credentialServer.getProtocol().equals(CredentialServerProtocolType.MYPROXY))
						{
							String decryptedPass = getClearTextPassword(salt);
							
							try 
							{	
								GSSCredential proxy = null;
								// check for a cached credential
								if (!StringUtils.isEmpty(credential))
								{
									InputStream is = null;
									try {
										// decrypt the credential
										String cred = getClearTextCredential(salt);
										
										// deserialize it
										is = new ByteArrayInputStream(cred.getBytes());
										
//										X509Credential globusCred = new AgaveX509Credential(is, trustedCALocation);
										X509Credential globusCred = new X509Credential(is);
//										try {
//											globusCred.verify();
//										} catch (CredentialException e) {
//											throw new SystemArgumentException("Invalid credential. The CA used to sign the system credential is not recognized.", e);
//										}
//										proxy = new AgaveGSSCredentialImpl(globusCred, GSSCredential.INITIATE_AND_ACCEPT);
										proxy = new GlobusGSSCredentialImpl(globusCred, GSSCredential.INITIATE_AND_ACCEPT);
										if (proxy.getRemainingLifetime() < 60) {
											setCredential(null);
											proxy = null;
										}
									}
									catch (Exception e) {
										throw new AuthConfigException("Unable to read X.509 credential.", e);
									} finally {
										try { is.close(); } catch (Exception e) {}
									}
								}
								
								// if we didn't have a valid, live credential cached, pull one from myproxy 
								if (proxy == null)
								{
									proxy = MyProxyClient.getCredential(credentialServer.getEndpoint(), credentialServer.getPort(), username, decryptedPass, trustedCALocation.getCaPath());
									
									// serialize it and prepair for caching
									ByteArrayOutputStream out = new ByteArrayOutputStream();
									((GlobusGSSCredentialImpl)proxy).getX509Credential().save(out);
									String serializedCredential = new String(out.toByteArray());
									setCredential(serializedCredential);
									
									// encrypt it prior to saving
									encryptCurrentCredential(salt);
								}
								
								return proxy;
							}
							catch (Exception e) 
							{		
								// retry again since myproxy can be flaky under load
								try {
									return MyProxyClient.getCredential(credentialServer.getEndpoint(), credentialServer.getPort(), username, decryptedPass, trustedCALocation.getCaPath());
								} catch (MyProxyException e1) {
									throw new RemoteCredentialException("Failed to retrieve a credential from " + 
											credentialServer.getEndpoint() + ".", e1);
								} catch (Exception e1) {
									throw new RemoteDataException("Failed to retrieve a credential from " + 
										credentialServer.getEndpoint() + ". GSI data will be unavailable.", e1);
								}
							}
						}
						else if (credentialServer != null && credentialServer.getProtocol().equals(CredentialServerProtocolType.MPG))
						{
	//						throw new NotYetImplementedException("MPG support is not yet availabled as a form of remote authentication");
							MyProxyGatewayClient mpgClient = new MyProxyGatewayClient(credentialServer.getEndpoint());
							GSSCredential proxy = null;
							try 
							{
								proxy = mpgClient.getCredential(username, TenancyHelper.getCurrentTenantId(), trustedCALocation);
							
								// serialize it and prepair for caching
								ByteArrayOutputStream out = new ByteArrayOutputStream();
								((GlobusGSSCredentialImpl)proxy).getX509Credential().save(out);
								String serializedCredential = new String(out.toByteArray());
								setCredential(serializedCredential);
								
								// encrypt it prior to saving
								encryptCurrentCredential(salt);
								return proxy;
							}
							catch (Exception e) 
							{		
								throw new RemoteDataException("Failed to retrieve a credential from " + 
										credentialServer.getEndpoint() + ". GSI data will be unavailable.", e);
							}
						}
						else if (credentialServer != null && credentialServer.getProtocol().equals(CredentialServerProtocolType.OA4MP))
						{
							
							throw new NotYetImplementedException("OA4MP support is not yet availabled as a form of remote authentication");
	//						InputStream is = null;
	//						GlobusGSSCredentialImpl proxy = null;
	//						try {
	//							is = new ByteArrayInputStream(credential.getBytes());
	//							GlobusCredential cred = new GlobusCredential(is);
	//							cred.
	//							proxy = new GlobusGSSCredentialImpl(cred, GSSCredential.INITIATE_AND_ACCEPT);
	//						}
	//						catch (Exception e) {
	//							throw new AuthConfigException("Unable to read X.509 credential.", e);
	//						}
	//						
	//						X509CertChainValidatorExt validator = CertificateValidatorBuilder.buildCertificateValidator();
	//						VOMSACService service = new DefaultVOMSACService.Builder(validator).build();
	//						
	//						VOMSACRequest request = new GenericVOMSACRequest(12, this.voName);
	//						
	//						AttributeCertificate attributeCertificate = service.getVOMSAttributeCertificate((X509Credential) proxy, request);
	//						
	//						ProxyCertificateOptions proxyOptions = new ProxyCertificateOptions(proxy.getCertificateChain());
	//						proxyOptions.setAttributeCertificates(new AttributeCertificate[] {attributeCertificate});
	//						
	//						AttributeCertificate attributeCertificate = service.getVOMSAttributeCertificate(cred, request);
	
						}
						else if (credentialServer != null && credentialServer.getProtocol().equals(CredentialServerProtocolType.VOMS))
						{
							throw new NotYetImplementedException("OA4MP support is not yet availabled as a form of remote authentication");
						}
						else
						{
							throw new AuthConfigException("No credential server specified for this X509 configuration.");
						}
					}
					else 
					{
						if (!StringUtils.isEmpty(credential))
						{
							InputStream is = null;
							try {
//								File tempDir = new File(System.getProperty("java.io.tmpdir")
//										+ File.separator + DigestUtils.md5Hex(salt));
//								File tempDir = new File(MyProxy.getTrustRootPath());
								trustedCALocation = new TrustedCALocation(MyProxy.getTrustRootPath());
								trustedCALocation.fetchRemoteCACertBundle(getTrustedCaLocation());
								
								String cred = getClearTextCredential(salt);
								is = new ByteArrayInputStream(cred.getBytes());
								X509Credential proxy = new X509Credential(is);
								return new GlobusGSSCredentialImpl(proxy, GSSCredential.INITIATE_AND_ACCEPT);
							}
							catch (Exception e) {
								throw new AuthConfigException("Unable to read X.509 credential.", e);
							}
						}
						else
						{
							throw new AuthConfigException("No credential server or credential specified for this X509 configuration.");
						}
					}
				}
				else if (this.type.equals(AuthConfigType.KERBEROSE)) 
				{
					throw new NotYetImplementedException("Kerberose support is not yet availabled as a form of remote authentication");
				}
				else if (this.type.equals(AuthConfigType.TOKEN)) 
				{
					throw new NotYetImplementedException("Token support is not yet availabled as a form of remote authentication");
				}
				else if (this.type.equals(AuthConfigType.SSHKEYS)) 
				{
					throw new NotYetImplementedException("SSH key support is not yet availabled as a form of remote authentication");
				}
			}
			
			return null;
		} 
		catch (RemoteCredentialException e) {
			throw e;
		}
		catch (Exception e) {
			throw new RemoteCredentialException(e.getMessage(), e);
		}
	}

	/* 
	 * Returns a shallow clone of the AuthConfig. Username, password, and credential are not copied over. 
	 */
	public AuthConfig clone()
	{
		AuthConfig authConfig = new AuthConfig();
		//authConfig.username = username;
		//authConfig.password = password;
		//authConfig.credential = credential;
		authConfig.type = type;
		if (authConfig.credentialServer != null) {
			authConfig.credentialServer = credentialServer.clone();
		}
		authConfig.trustedCaLocation = getTrustedCaLocation();
		
		return authConfig;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime
				* result
				+ ( ( internalUsername == null ) ? 0 : internalUsername
						.hashCode() );
		result = prime * result
				+ ( ( remoteConfig == null ) ? 0 : remoteConfig.hashCode() );
		result = prime * result
				+ ( ( username == null ) ? 0 : username.hashCode() );
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AuthConfig other = (AuthConfig) obj;
		if (internalUsername == null)
		{
			if (other.internalUsername != null)
				return false;
		}
		else if (!internalUsername.equals(other.internalUsername))
			return false;
		if (getRemoteConfig().getHost() == null)
		{
			if (other.getRemoteConfig().getHost() != null)
				return false;
		}
		else if (!getRemoteConfig().getHost().equals(other.getRemoteConfig().getHost()))
			return false;
		if (username == null)
		{
			if (other.username != null)
				return false;
		}
		else if (!username.equals(other.username))
			return false;
		return true;
	}

	public String toJSON(RemoteConfig config, String salt, RemoteSystem system) throws SystemArgumentException
	{
		String output = null;
		JSONStringer js = new JSONStringer();
		try
		{	
			js.object()
				.key("parentType").value(((config instanceof StorageConfig) ? "storage" : "login"))
				.key("default").value(isSystemDefault())
				.key("internalUsername").value(getInternalUsername())
				.key("type").value(getType().name())
				.key("created").value(new DateTime(getCreated()).toString());
				
				long remainingTime = getCredentialRemainingTime(salt);
				if (remainingTime != 0) {
					js.key("valid").value(true);
					
					if (remainingTime == -1) {
						js.key("expirationDate").value(null);
					} 
					else
					{
						DateTime dt = new DateTime(remainingTime);
						js.key("expriationDate").value(dt.toString());
					}
				} else {
					js.key("valid").value(false);
					js.key("expirationDate").value(null);
				}
				
				if (credentialServer == null) {
					js.key("server").value(null);
				} 
				else {
					js.key("server").object()
						.key("name").value(getCredentialServer().getName())
						.key("endpoint").value(getCredentialServer().getEndpoint())
						.key("port").value(getCredentialServer().getPort())
						.key("protocol").value(getCredentialServer().getProtocol().name())
					.endObject();
				}
				js.key("_links").object()
	            	.key("self").object()
		        		.key("href").value(TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_SYSTEM_SERVICE) + 
		        		        system.getSystemId() + "/credentials/" + 
		        		        (StringUtils.isEmpty(getInternalUsername()) ? "default" : getInternalUsername()) + 
		        		        "/" + ((config instanceof StorageConfig) ? "storage" : "login"))
		        	.endObject()
		        	.key("parent").object()
		        		.key("href").value(TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_SYSTEM_SERVICE) + system.getSystemId() + "/credentials")
		        	.endObject();
		        	if (!StringUtils.isEmpty(getInternalUsername())) {
		        	js.key("internalUser").object()
		        		.key("href").value(TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_PROFILE_SERVICE) + system.getOwner() + "/users/" + internalUsername)
		        	.endObject();
		        	}
		        js.endObject()
			.endObject();
			
			output = js.toString();
		}
		catch (Exception e) {
			log.error("Error producing JSON output for auth config of system " + system.getSystemId());
		}

		return output;
	}
	
//	class GenericVOMSACRequest implements VOMSACRequest {
//
//		private int lifetime = (int) TimeUnit.HOURS.toSeconds(12);
//
//		private List<String> requestedFQANs;
//
//		private List<String> targets;
//
//		private String voName;
//
//		public int getLifetime() {
//
//			return lifetime;
//		}
//
//
//		public List<String> getRequestedFQANs() {
//			
//			return requestedFQANs;
//		}
//
//		
//		public List<String> getTargets() {
//
//			return targets;
//		}
//
//		public String getVoName() {
//
//			return voName;
//		}
//		
//		public GenericVOMSACRequest(String voName, 
//				List<String> targets, List<String> requestedFQANs) 
//		{
//			this.voName = voName;
//			this.targets = targets;
//			this.requestedFQANs = requestedFQANs;
//		}
//		
//		public GenericVOMSACRequest(int lifetime, String voName, 
//				List<String> targets, List<String> requestedFQANs) 
//		{
//			this.lifetime = lifetime;
//			this.voName = voName;
//			this.targets = targets;
//			this.requestedFQANs = requestedFQANs;
//		}
//	}
}
