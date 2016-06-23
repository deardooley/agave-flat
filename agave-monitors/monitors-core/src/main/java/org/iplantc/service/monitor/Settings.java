package org.iplantc.service.monitor;
/**
 * 
 */


import org.apache.commons.lang.StringUtils;

import javax.net.ssl.*;

import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * @author dooley
 *
 */
public class Settings 
{
	
	private static Properties props = new Properties();
	
	/* Debug settings */
	public static boolean 						DEBUG;
	public static String 						DEBUG_USERNAME;
	
	/* Community user credentials */
	public static String						API_VERSION;
	public static String						SERVICE_VERSION;
	
	/* Trusted user settings */
	public static List<String> 					TRUSTED_USERS = new ArrayList<String>();
	
	/* Community storage settings */
	public static String 						PUBLIC_USER_USERNAME;
	public static String 						WORLD_USER_USERNAME;
	public static String						IRODS_USERNAME;
	public static String						IRODS_PASSWORD;

	/* Authentication service settings */
	public static String 						AUTH_SOURCE;
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
	public static String						IPLANT_FILE_SERVICE;
	public static String 						IPLANT_METADATA_SERVICE;
	public static String						IPLANT_MONITOR_SERVICE;
	public static String 						IPLANT_JOB_SERVICE;
	public static String 						IPLANT_APP_SERVICE;
	public static String						IPLANT_SYSTEM_SERVICE;
	public static String 						IPLANT_TRANSFER_SERVICE;
	public static String 						IPLANT_TRANSFORM_SERVICE;
	public static String 						IPLANT_NOTIFICATION_SERVICE;
	public static String						IPLANT_POSTIT_SERVICE;
	
	public static String 						MAIL_SERVER;
	public static String 						MAILSMTPSPROTOCOL;
	public static String 						MAILLOGIN;    
    public static String 						MAILPASSWORD;
	
	/* General policy settings */
	public static int 							MAX_MONITOR_RETRIES;
	public static int 							MAX_MONITOR_TASKS;
	public static int							MAX_MONITOR_QUEUE_FAILURES;
	public static boolean 						SLAVE_MODE;
	public static String	 					IPLANT_DOCS;

	public static String 						NOTIFICATION_QUEUE;
	public static String 						NOTIFICATION_TOPIC;
	public static String 						MONITOR_QUEUE;
	public static String 						MONITOR_TOPIC;
	
	public static String						MESSAGING_SERVICE_PROVIDER;
	public static String 						MESSAGING_SERVICE_HOST;
	public static int	 						MESSAGING_SERVICE_PORT;
	public static String 						MESSAGING_SERVICE_USERNAME;
	public static String 						MESSAGING_SERVICE_PASSWORD;

	public static Integer 						MINIMUM_MONITOR_REPEAT_INTERVAL;

	public static Integer 						DEFAULT_PAGE_SIZE;
	
	static {
//		System.setProperty("java.protocol.handler.pkgs", "org.iplantc.service.io.remote.url.gsiftp");
		
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
		
		PUBLIC_USER_USERNAME = (String)props.get("iplant.public.user");
		
		WORLD_USER_USERNAME = (String)props.get("iplant.world.user");
		
		MAIL_SERVER = (String) props.getProperty("mail.smtps.host");
		
		MAILSMTPSPROTOCOL = (String) props.getProperty("mail.smtps.auth");
		
		MAILLOGIN = (String) props.getProperty("mail.smtps.user");
		
		MAILPASSWORD = (String) props.getProperty("mail.smtps.passwd");
		
		IPLANT_MYPROXY_SERVER = (String)props.get("iplant.myproxy.server");
		
		IPLANT_MYPROXY_PORT = Integer.valueOf((String)props.get("iplant.myproxy.port"));
		
		IPLANT_LDAP_URL = (String)props.get("iplant.ldap.url");
		
		IPLANT_LDAP_BASE_DN = (String)props.get("iplant.ldap.base.dn");
		
		KEYSTORE_PATH = (String)props.get("system.keystore.path");
		
		TRUSTSTORE_PATH = (String)props.get("system.truststore.path");
		
		TRUSTED_CA_CERTS_DIRECTORY = (String) props.get("system.ca.certs.path");
		
		IRODS_USERNAME = (String)props.get("iplant.irods.username");
		
		IRODS_PASSWORD = (String)props.get("iplant.irods.password");
		
		IPLANT_AUTH_SERVICE = (String)props.get("iplant.auth.service");
		if (!IPLANT_AUTH_SERVICE.endsWith("/")) IPLANT_AUTH_SERVICE += "/";
		
		IPLANT_APP_SERVICE = (String) props.get("iplant.app.service");
		if (!IPLANT_APP_SERVICE.endsWith("/")) IPLANT_APP_SERVICE += "/";
		
		IPLANT_JOB_SERVICE = (String) props.get("iplant.job.service");
		if (!IPLANT_JOB_SERVICE.endsWith("/")) IPLANT_JOB_SERVICE += "/";

		IPLANT_PROFILE_SERVICE = (String) props.get("iplant.profile.service");
		if (!IPLANT_PROFILE_SERVICE.endsWith("/")) IPLANT_PROFILE_SERVICE += "/";
		
		IPLANT_FILE_SERVICE = (String) props.get("iplant.io.service");
		if (!IPLANT_FILE_SERVICE.endsWith("/")) IPLANT_FILE_SERVICE += "/";
		
		IPLANT_LOG_SERVICE = (String) props.get("iplant.log.service");
		if (!IPLANT_LOG_SERVICE.endsWith("/")) IPLANT_LOG_SERVICE += "/";
		
		IPLANT_METADATA_SERVICE = (String) props.get("iplant.metadata.service");
		if (!IPLANT_METADATA_SERVICE.endsWith("/")) IPLANT_METADATA_SERVICE += "/";
		
		IPLANT_MONITOR_SERVICE = (String) props.get("iplant.monitor.service");
		if (!IPLANT_MONITOR_SERVICE.endsWith("/")) IPLANT_MONITOR_SERVICE += "/";
		
		IPLANT_SYSTEM_SERVICE = (String) props.get("iplant.system.service");
		if (!IPLANT_SYSTEM_SERVICE.endsWith("/")) IPLANT_SYSTEM_SERVICE += "/";
		
		IPLANT_TRANSFER_SERVICE = (String) props.get("iplant.transfer.service");
		if (!IPLANT_TRANSFER_SERVICE.endsWith("/")) IPLANT_METADATA_SERVICE += "/";
		
		IPLANT_NOTIFICATION_SERVICE = (String) props.get("iplant.notification.service");
		if (!IPLANT_NOTIFICATION_SERVICE.endsWith("/")) IPLANT_NOTIFICATION_SERVICE += "/";
		
		IPLANT_POSTIT_SERVICE = (String) props.get("iplant.postit.service");
		if (!IPLANT_POSTIT_SERVICE.endsWith("/")) IPLANT_POSTIT_SERVICE += "/";
		
		IPLANT_DOCS = (String) props.get("iplant.service.documentation");
		if (!IPLANT_DOCS.endsWith("/")) IPLANT_DOCS += "/";
		
		MAX_MONITOR_RETRIES = Integer.valueOf((String)props.getProperty("iplant.max.monitor.retries", "1"));
		MAX_MONITOR_TASKS = Integer.valueOf((String)props.getProperty("iplant.max.monitor.tasks", "1"));
		MAX_MONITOR_QUEUE_FAILURES = Integer.valueOf((String)props.getProperty("iplant.max.monitor.queue.failures", "5"));
		
		SLAVE_MODE = Boolean.valueOf((String)props.get("iplant.slave.mode"));
		
		NOTIFICATION_QUEUE = (String) props.getProperty("iplant.notification.service.queue");
		NOTIFICATION_TOPIC = (String) props.getProperty("iplant.notification.service.topic");
		MONITOR_QUEUE = (String) props.getProperty("iplant.monitor.service.queue");
		MONITOR_TOPIC = (String) props.getProperty("iplant.monitor.service.topic");
		
		MESSAGING_SERVICE_PROVIDER = (String) props.getProperty("plant.messaging.provider");
		MESSAGING_SERVICE_USERNAME = (String)props.get("iplant.messaging.username");
		MESSAGING_SERVICE_PASSWORD = (String)props.get("iplant.messaging.password");
		MESSAGING_SERVICE_HOST = (String)props.get("iplant.messaging.host");
		MESSAGING_SERVICE_PORT = Integer.valueOf((String)props.get("iplant.messaging.port"));

		DEFAULT_PAGE_SIZE = Integer.parseInt((String) props.getProperty("iplant.default.page.size", "25"));
		
		MINIMUM_MONITOR_REPEAT_INTERVAL = Integer.parseInt((String) props.getProperty("iplant.min.monitor.repeat.interval", "300"));
	}
}
