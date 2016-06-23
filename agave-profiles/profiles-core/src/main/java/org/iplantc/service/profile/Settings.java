package org.iplantc.service.profile;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.iplantc.service.profile.model.enumeration.ProfileType;
import org.iplantc.service.profile.util.ServiceUtils;

public class Settings 
{
	private static final String PROPERTY_FILE = "service.properties";
	
	private static Properties props = new Properties();
	
	/* Debug settings */
	public static boolean DEBUG;
	public static String DEBUG_USERNAME;
	
	public static String 		AUTH_SOURCE;
	
	public static String		API_VERSION;
	public static String		SERVICE_VERSION;
	
	/* Trusted user settings */
	public static List<String> 	TRUSTED_USERS = new ArrayList<String>();
	
	/* Community user credentials */
	public static String 		COMMUNITY_PROXY_USERNAME;
	public static String 		COMMUNITY_PROXY_PASSWORD;
	
	/* Authentication service settings */
	public static String 		IPLANT_LDAP_URL;
	public static String 		IPLANT_LDAP_BASE_DN;
	public static String		IPLANT_LDAP_PASSWORD;
	public static String		IPLANT_LDAP_USERNAME;
	public static ProfileType 	IPLANT_DATA_SOURCE;
	public static String 		KEYSTORE_PATH;
	public static String 		TRUSTSTORE_PATH;
	public static String 		TACC_MYPROXY_SERVER;
	public static int 	 		TACC_MYPROXY_PORT;
	public static String 		IPLANT_PROFILE_SERVICE;
	public static String 		IPLANT_AUTH_SERVICE;
	public static String 		IPLANT_METADATA_SERVICE;
	public static String		IPLANT_LOG_SERVICE;
	
	/* iPlant dependent services */
	public static String 		QUERY_URL;
	public static String 		QUERY_URL_USERNAME;
	public static String 		QUERY_URL_PASSWORD;
	
	/* Database Settings */
	public static String 		SSH_TUNNEL_USERNAME;
	public static String 		SSH_TUNNEL_PASSWORD;
	public static String		SSH_TUNNEL_HOST;
	public static int			SSH_TUNNEL_PORT;
	public static String 		DB_USERNAME;
	public static String 		DB_PASSWORD;
	public static String 		DB_NAME;
	public static boolean		USE_SSH_TUNNEL;
	public static Integer 		DEFAULT_PAGE_SIZE;
	public static String 		IPLANT_DOCS;
	
	static
	{
		props = org.iplantc.service.common.Settings.loadRuntimeProperties();
		
		DEBUG = Boolean.valueOf((String)props.get("iplant.debug.mode"));
		
		DEBUG_USERNAME = (String)props.get("iplant.debug.username");
		
		AUTH_SOURCE = (String) props.get("iplant.auth.source");
		
		API_VERSION = (String)props.getProperty("iplant.api.version");
		
		SERVICE_VERSION = (String)props.getProperty("iplant.service.version");
		
		IPLANT_LDAP_URL = props.getProperty("iplant.ldap.url");

		IPLANT_LDAP_BASE_DN = props.getProperty("iplant.ldap.base.dn");
		
		IPLANT_LDAP_PASSWORD = props.getProperty("iplant.ldap.password");
		
		IPLANT_LDAP_USERNAME = props.getProperty("iplant.ldap.username");

		String profileDataSourceType = props.getProperty("iplant.data.source");
		if (!ServiceUtils.isValid(profileDataSourceType)) {
			IPLANT_DATA_SOURCE = ProfileType.LDAP;
		} else {
			try {
				IPLANT_DATA_SOURCE = ProfileType.valueOf(profileDataSourceType.toUpperCase());
			} catch (Exception e) {
				IPLANT_DATA_SOURCE = ProfileType.LDAP;
			}
		}
		COMMUNITY_PROXY_USERNAME = (String)props.get("iplant.community.username");
		
		COMMUNITY_PROXY_PASSWORD = (String)props.get("iplant.community.password");
		
		String trustedUsers = (String)props.get("iplant.trusted.users");
		if (trustedUsers != null && !trustedUsers.equals("")) {
			for (String user: trustedUsers.split(",")) {
				TRUSTED_USERS.add(user);
			}
		}
		
		KEYSTORE_PATH = props.getProperty("system.keystore.path");

		TRUSTSTORE_PATH = props.getProperty("system.truststore.path");
		
		TACC_MYPROXY_SERVER = (String)props.get("iplant.myproxy.server");
		
		TACC_MYPROXY_PORT = Integer.valueOf((String)props.get("iplant.myproxy.port"));
		
		IPLANT_AUTH_SERVICE = (String)props.get("iplant.auth.service");
		if (!IPLANT_AUTH_SERVICE.endsWith("/")) IPLANT_AUTH_SERVICE += "/";
		
		IPLANT_PROFILE_SERVICE = (String)props.get("iplant.profile.service");
		if (!IPLANT_PROFILE_SERVICE.endsWith("/")) IPLANT_PROFILE_SERVICE += "/";
		
		IPLANT_METADATA_SERVICE = (String)props.get("iplant.metadata.service");
		if (!IPLANT_METADATA_SERVICE.endsWith("/")) IPLANT_METADATA_SERVICE += "/";
		
		IPLANT_LOG_SERVICE = (String) props.get("iplant.log.service");
		if (!IPLANT_LOG_SERVICE.endsWith("/")) IPLANT_LOG_SERVICE += "/";
		
		IPLANT_DOCS = (String) props.get("iplant.service.documentation");
		if (!IPLANT_DOCS.endsWith("/")) IPLANT_DOCS += "/";
		
		QUERY_URL = (String)props.getProperty("iplant.internal.account.service");
		
		QUERY_URL_USERNAME = (String)props.getProperty("iplant.internal.account.service.key");
		
		QUERY_URL_PASSWORD = (String)props.getProperty("iplant.internal.account.service.secret");
		
		USE_SSH_TUNNEL = Boolean.valueOf((String) props.get("db.use.tunnel"));
		
		SSH_TUNNEL_USERNAME = (String)props.getProperty("db.ssh.tunnel.username");
		
		SSH_TUNNEL_PASSWORD = (String)props.getProperty("db.ssh.tunnel.password");
		
		SSH_TUNNEL_HOST = (String)props.getProperty("db.ssh.tunnel.host");
		
		SSH_TUNNEL_PORT = Integer.valueOf((String)props.get("db.ssh.tunnel.port"));
		
		DB_USERNAME = (String)props.getProperty("db.username");
		
		DB_PASSWORD = (String)props.getProperty("db.password");
		
		DB_NAME = (String)props.getProperty("db.name");
		
		DEFAULT_PAGE_SIZE = Integer.parseInt((String) props.getProperty("iplant.default.page.size", "25"));
	}
}
