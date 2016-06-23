/**
 * 
 */
package org.iplantc.service.io;

import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.lang.StringUtils;

/**
 * @author dooley
 *
 */
public class Settings {
	private static Properties props = new Properties();
	
	/* Debug settings */
	public static boolean 						DEBUG;
	public static String 						DEBUG_USERNAME;
	
	/* Community user credentials */
	public static String 						COMMUNITY_USERNAME;
	public static String 						COMMUNITY_PASSWORD;
	
	public static String						API_VERSION;
	public static String						SERVICE_VERSION;
	
	/* Trusted user settings */
	public static List<String> 					TRUSTED_USERS = new ArrayList<String>();
	
	/* Community storage settings */
	public static String 						IRODS_USERNAME;
	public static String 						IRODS_PASSWORD;
	public static String 						IRODS_HOST;
	public static int 							IRODS_PORT;
	public static String 						IRODS_ZONE;
	public static String 						IRODS_STAGING_DIRECTORY;
	public static String 						IRODS_DEFAULT_RESOURCE;
//	public static int 							IRODS_REFRESH_RATE; // how often to check the irods connection info
	public static String 						PUBLIC_USER_USERNAME;
	public static String 						WORLD_USER_USERNAME;

	/* Authentication service settings */
	public static String 						AUTH_SOURCE;
//	public static String 						TACC_MYPROXY_SERVER;
//	public static int 							TACC_MYPROXY_PORT;
	public static String 						IPLANT_MYPROXY_SERVER;
	public static int 							IPLANT_MYPROXY_PORT;
	public static String 						IPLANT_LDAP_URL;
	public static String 						IPLANT_LDAP_BASE_DN;
	public static String 						KEYSTORE_PATH;
	public static String 						TRUSTSTORE_PATH;
	public static String 						TRUSTED_CA_CERTS_DIRECTORY;
	public static String 						IPLANT_AUTH_SERVICE;
	public static String 						IPLANT_PROFILE_SERVICE;
	public static String 						IPLANT_LOG_SERVICE;
	public static String						IPLANT_IO_SERVICE;
	public static String 						IPLANT_METADATA_SERVICE;
	public static String 						IPLANT_NOTIFICATION_SERVICE;
	public static String 						IPLANT_SYSTEM_SERVICE;
	public static String 						MAIL_SERVER;
	public static String 						MAILSMTPSPROTOCOL;
	public static String 						MAILLOGIN;    
    public static String 						MAILPASSWORD;
	
	/* General policy settings */
//	public static long 							INDIVIDUAL_DISK_QUOTA = 0;
	public static String 						TRANSFORMS_FOLDER_PATH;
	public static int 							MAX_TRANSFORM_TASKS;
	public static int 							MAX_STAGING_TASKS;
	public static int 							MAX_STAGING_RETRIES;
	public static int 							MAX_USER_CONCURRENT_TRANSFERS;
	
	public static String 						TRANSFORM_DEFINITION_FILE_PATH;
	public static String 						TRANSFORM_CONVERSION_MAP_FILE_PATH;
	public static boolean 						SLAVE_MODE;
	public static boolean						ENABLE_ZOMBIE_CLEANUP;

	public static String SERVICE_URL; // base url of this service

	public static String	 					IPLANT_DOCS;
    public static Integer                       DEFAULT_PAGE_SIZE;

	static {
		//System.setProperty("java.protocol.handler.pkgs", "org.iplantc.service.io.remote.url.gsiftp");
		
		// trust everyone. we need this due to the unknown nature of the callback urls
		try { 
            HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier(){ 
                    public boolean verify(String hostname, SSLSession session) { 
                            return true; 
                    }}); 
            SSLContext context = SSLContext.getInstance("TLS"); 
            context.init(null, new X509TrustManager[]{new X509TrustManager(){ 
                    public void checkClientTrusted(X509Certificate[] chain, 
                                    String authType) throws CertificateException {} 
                    public void checkServerTrusted(X509Certificate[] chain, 
                                    String authType) throws CertificateException {} 
                    public X509Certificate[] getAcceptedIssuers() { 
                            return new X509Certificate[0]; 
                    }}}, new SecureRandom()); 
            HttpsURLConnection.setDefaultSSLSocketFactory( 
                            context.getSocketFactory());
    		
	    } catch (Exception e) { // should never happen 
	            e.printStackTrace(); 
	    }
		
		props = org.iplantc.service.common.Settings.loadRuntimeProperties();
		
		try {
		
			DEBUG = Boolean.valueOf((String)props.get("iplant.debug.mode"));
		
			DEBUG_USERNAME = (String)props.get("iplant.debug.username");
			
			AUTH_SOURCE = (String)props.get("iplant.auth.source");
			
			String trustedUsers = (String)props.get("iplant.trusted.users");
			if (!StringUtils.isEmpty(trustedUsers)) {
				for (String user: trustedUsers.split(",")) {
					TRUSTED_USERS.add(user);
				}
			}
			
			API_VERSION = (String)props.getProperty("iplant.api.version");
			
			SERVICE_VERSION = (String)props.getProperty("iplant.service.version");
			
			COMMUNITY_USERNAME = (String)props.get("iplant.community.username");
			
			COMMUNITY_PASSWORD = (String)props.get("iplant.community.password");
			
			IRODS_USERNAME = (String)props.get("iplant.irods.username");
			
			IRODS_PASSWORD = (String)props.get("iplant.irods.password");
			
			IRODS_HOST = (String)props.get("iplant.irods.host");
			
			IRODS_PORT = Integer.valueOf((String)props.get("iplant.irods.port"));
			
			IRODS_ZONE = (String)props.get("iplant.irods.zone");
			
			IRODS_STAGING_DIRECTORY = (String)props.get("iplant.irods.staging.directory");
			
			IRODS_DEFAULT_RESOURCE = (String)props.get("iplant.irods.default.resource");
			
	//		IRODS_REFRESH_RATE = Integer.valueOf((String)props.get("iplant.irods.refresh.interval"));
			
			PUBLIC_USER_USERNAME = (String)props.get("iplant.public.user");
			
			WORLD_USER_USERNAME = (String)props.get("iplant.world.user");
			
			MAIL_SERVER = (String) props.getProperty("mail.smtps.host");
			
			MAILSMTPSPROTOCOL = (String) props.getProperty("mail.smtps.auth");
			
			MAILLOGIN = (String) props.getProperty("mail.smtps.user");
			
			MAILPASSWORD = (String) props.getProperty("mail.smtps.passwd");
			
	//		TACC_MYPROXY_SERVER = (String)props.get("iplant.myproxy.server");
	//		
	//		TACC_MYPROXY_PORT = Integer.valueOf((String)props.get("iplant.myproxy.port"));
			
			IPLANT_MYPROXY_SERVER = (String)props.get("iplant.myproxy.server");
			
			IPLANT_MYPROXY_PORT = Integer.valueOf((String)props.get("iplant.myproxy.port"));
			
			IPLANT_LDAP_URL = (String)props.get("iplant.ldap.url");
			
			IPLANT_LDAP_BASE_DN = (String)props.get("iplant.ldap.base.dn");
			
	//		INDIVIDUAL_DISK_QUOTA = Long.valueOf((String)props.get("iplant.user.disk.quota"));
			
			KEYSTORE_PATH = (String)props.get("system.keystore.path");
			
			TRUSTSTORE_PATH = (String)props.get("system.truststore.path");
			
			TRUSTED_CA_CERTS_DIRECTORY = (String) props.get("system.ca.certs.path");
			
			IPLANT_AUTH_SERVICE = (String)props.get("iplant.auth.service");
			if (!IPLANT_AUTH_SERVICE.endsWith("/")) IPLANT_AUTH_SERVICE += "/";
			
			IPLANT_PROFILE_SERVICE = (String) props.get("iplant.profile.service");
			if (!IPLANT_PROFILE_SERVICE.endsWith("/")) IPLANT_PROFILE_SERVICE += "/";
			
			IPLANT_IO_SERVICE = (String) props.get("iplant.io.service");
			if (!IPLANT_IO_SERVICE.endsWith("/")) IPLANT_IO_SERVICE += "/";
			
			IPLANT_LOG_SERVICE = (String) props.get("iplant.log.service");
			if (!IPLANT_LOG_SERVICE.endsWith("/")) IPLANT_LOG_SERVICE += "/";
			
			IPLANT_METADATA_SERVICE = (String) props.get("iplant.metadata.service");
			if (!IPLANT_METADATA_SERVICE.endsWith("/")) IPLANT_METADATA_SERVICE += "/";
			
			IPLANT_NOTIFICATION_SERVICE = (String) props.get("iplant.notification.service");
			if (!IPLANT_NOTIFICATION_SERVICE.endsWith("/")) IPLANT_NOTIFICATION_SERVICE += "/";
			
			IPLANT_SYSTEM_SERVICE = (String) props.get("iplant.system.service");
			if (!IPLANT_SYSTEM_SERVICE.endsWith("/")) IPLANT_SYSTEM_SERVICE += "/";
			
			IPLANT_DOCS = (String) props.get("iplant.service.documentation");
			if (!IPLANT_DOCS.endsWith("/")) IPLANT_DOCS += "/";
			
			TRANSFORMS_FOLDER_PATH = (String)props.get("iplant.transforms.dir.path");
			
			MAX_TRANSFORM_TASKS = Integer.valueOf((String)props.get("iplant.max.transform.tasks"));
			
			MAX_STAGING_TASKS = Integer.valueOf((String)props.get("iplant.max.staging.tasks"));
			
			MAX_STAGING_RETRIES = Integer.valueOf((String)props.get("iplant.max.staging.retries"));
			
			String maxUserTransfers = (String) props.get("iplant.max.user.concurrent.transfers");
			try {
				MAX_USER_CONCURRENT_TRANSFERS = Integer.parseInt(maxUserTransfers);
			} catch (Exception e) {
				MAX_USER_CONCURRENT_TRANSFERS = Integer.MAX_VALUE;
			}
			
			ENABLE_ZOMBIE_CLEANUP = Boolean.valueOf((String) props
					.getProperty("iplant.enable.zombie.cleanup", "false"));
			
			TRANSFORM_DEFINITION_FILE_PATH = (String)props.get("iplant.transforms.file.path");
			
			TRANSFORM_CONVERSION_MAP_FILE_PATH = (String)props.get("iplant.transforms.conversion.map.path");
			
			SLAVE_MODE = Boolean.valueOf((String)props.get("iplant.slave.mode"));
			
			SERVICE_URL = (String)props.get("iplant.io.service.url");
			
			DEFAULT_PAGE_SIZE = Integer.parseInt((String) props.getProperty("iplant.default.page.size", "250"));
		} 
		catch (Throwable e) {
			e.printStackTrace();
		}
	}
}
