package org.iplantc.service.notification;
/**
 * 
 */


import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.notification.providers.email.enumeration.EmailProviderType;
import org.iplantc.service.notification.providers.realtime.enumeration.RealtimeProviderType;
import org.iplantc.service.notification.providers.sms.enumeration.SmsProviderType;

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
	private static final Logger log = Logger.getLogger(Settings.class);
	
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
	public static String 						IPLANT_JOB_SERVICE;
	public static String 						IPLANT_APP_SERVICE;
	public static String 						IPLANT_TRANSFER_SERVICE;
	public static String 						IPLANT_TRANSFORM_SERVICE;
	public static String 						IPLANT_NOTIFICATION_SERVICE;
	public static String						IPLANT_POSTIT_SERVICE;
	
//	public static String 						MAIL_SERVER;
//	public static String 						MAILSMTPSPROTOCOL;
//	public static String 						MAILLOGIN;    
//    public static String 						MAILPASSWORD;
    
	public static EmailProviderType             EMAIL_PROVIDER;
    public static String 						SMTP_HOST_NAME;
    public static int 							SMTP_HOST_PORT;
    public static boolean 						SMTP_AUTH_REQUIRED;
    public static String 						SMTP_AUTH_USER;
    public static String 						SMTP_AUTH_PWD;
    public static String 						SMTP_FROM_NAME;
    public static String 						SMTP_FROM_ADDRESS;
	
	/* General policy settings */
	public static int 							MAX_NOTIFICATION_RETRIES;
	public static int 							MAX_NOTIFICATION_TASKS;
	public static boolean 						SLAVE_MODE;
	public static String	 					IPLANT_DOCS;

	public static String 						NOTIFICATION_QUEUE;
	public static String 						NOTIFICATION_TOPIC;
	public static String 						NOTIFICATION_RETRY_QUEUE;
	public static String 						NOTIFICATION_RETRY_TOPIC;
	
	public static String 						FAILED_NOTIFICATION_DB_HOST;
	public static String 						FAILED_NOTIFICATION_DB_SCHEME;
    public static int                           FAILED_NOTIFICATION_DB_PORT;
    public static String                        FAILED_NOTIFICATION_DB_USER;
    public static String                        FAILED_NOTIFICATION_DB_PWD;
    public static int                           FAILED_NOTIFICATION_COLLECTION_LIMIT;
    public static int                           FAILED_NOTIFICATION_COLLECTION_SIZE;
    
	public static String						MESSAGING_SERVICE_PROVIDER;
	public static String 						MESSAGING_SERVICE_HOST;
	public static int	 						MESSAGING_SERVICE_PORT;
	public static String 						MESSAGING_SERVICE_USERNAME;
	public static String 						MESSAGING_SERVICE_PASSWORD;

	public static String 						TWILIO_ACCOUNT_SID;
	public static String 						TWILIO_AUTH_TOKEN;
	public static String						TWILIO_PHONE_NUMBER;

	public static RealtimeProviderType			REALTIME_PROVIDER;
	public static String                        REALTIME_BASE_URL;
	public static String                        REALTIME_REALM_KEY;
    public static String                        REALTIME_REALM_ID;

	public static SmsProviderType 				SMS_PROVIDER;
	
	public static String						SLACK_WEBHOOK_USERNAME;
	public static String						SLACK_WEBHOOK_ICON_URL;
	public static String						SLACK_WEBHOOK_ICON_EMOJI;
    
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
		
		/** Email server settings **/

//	    public static String 						SMTP_HOST_NAME;
//	    public static String 						SMTP_AUTH_USER;
//	    public static String 						SMTP_AUTH_PWD;
//	    public static String 						SMTP_FROM_NAME;
//	    public static String 						SMTP_FROM_ADDRESS;
	    
		String emailProvider = (String) props.getProperty("mail.smtps.provider", "localhost");
		try {
		    EMAIL_PROVIDER = EmailProviderType.valueOf(StringUtils.upperCase(emailProvider));
		} catch (Exception e) {
		    log.error("Invalid email provider specified. Defaulting to localhost.");
		    EMAIL_PROVIDER = EmailProviderType.LOCAL;
		}
		finally {}
		
		SMTP_AUTH_REQUIRED = Boolean.parseBoolean((String) props.getProperty("mail.smtps.auth", "false"));
		
		if (SMTP_AUTH_REQUIRED) {
			SMTP_HOST_NAME = (String) props.getProperty("mail.smtps.host", "smtp");
			SMTP_HOST_PORT = NumberUtils.toInt((String) props.getProperty("mail.smtps.port"), 587);
			SMTP_AUTH_USER = (String) props.getProperty("mail.smtps.user", System.getProperty("user.name"));
			SMTP_AUTH_PWD = (String) props.getProperty("mail.smtps.passwd", "");
		} else {
			SMTP_HOST_NAME = (String) props.getProperty("mail.smtps.host", "localhost");
			SMTP_HOST_PORT = NumberUtils.toInt((String) props.getProperty("mail.smtps.port"), 25);
		}
		
		SMTP_FROM_NAME = (String) props.getProperty("mail.smtps.from.name", "Agave Notification Service");
		SMTP_FROM_ADDRESS = (String) props.getProperty("mail.smtps.from.address", "no-reply@agaveapi.co");
		
//	    MAIL_SERVER = (String) props.getProperty("mail.smtps.host");
//		
//		MAILSMTPSPROTOCOL = (String) props.getProperty("mail.smtps.auth");
//		
//		MAILLOGIN = (String) props.getProperty("mail.smtps.user");
//		
//		MAILPASSWORD = (String) props.getProperty("mail.smtps.passwd");
		
		String smsProvider = (String) props.getProperty("sms.provider", "localhost");
		try {
		    SMS_PROVIDER = SmsProviderType.valueOf(StringUtils.upperCase(smsProvider));
		} catch (Exception e) {
		    SMS_PROVIDER = SmsProviderType.LOG;
		}
		finally {}
		
		TWILIO_AUTH_TOKEN = (String) props.getProperty("twilio.token");
		
		TWILIO_ACCOUNT_SID = (String) props.getProperty("twilio.sid");
		
		TWILIO_PHONE_NUMBER = (String) props.getProperty("twilio.phone.number");
		
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
		
		IPLANT_TRANSFER_SERVICE = (String) props.get("iplant.transfer.service");
		if (!IPLANT_TRANSFER_SERVICE.endsWith("/")) IPLANT_METADATA_SERVICE += "/";
		
		IPLANT_NOTIFICATION_SERVICE = (String) props.get("iplant.notification.service");
		if (!IPLANT_NOTIFICATION_SERVICE.endsWith("/")) IPLANT_NOTIFICATION_SERVICE += "/";
		
		IPLANT_POSTIT_SERVICE = (String) props.get("iplant.postit.service");
		if (!IPLANT_POSTIT_SERVICE.endsWith("/")) IPLANT_POSTIT_SERVICE += "/";
		
		IPLANT_DOCS = (String) props.get("iplant.service.documentation");
		if (!IPLANT_DOCS.endsWith("/")) IPLANT_DOCS += "/";
		
		String realtimeProvider = (String) props.getProperty("realtime.provider", "LOG");
		try {
			REALTIME_PROVIDER = RealtimeProviderType.valueOf(StringUtils.upperCase(realtimeProvider));
		} catch (Exception e) {
		    log.error("Invalid realtime provider specified. Defaulting to log provider.");
		    REALTIME_PROVIDER = RealtimeProviderType.LOG;
		}
		
		REALTIME_BASE_URL = (String) props.getProperty("realtime.url", "http://realtime.example.com:7999");
        if (!REALTIME_BASE_URL.endsWith("/")) REALTIME_BASE_URL += "/";
		
        REALTIME_REALM_ID = (String) props.getProperty("realtime.realm.id", "");
        REALTIME_REALM_KEY = (String) props.getProperty("realtime.realm.key", "");
        
		MAX_NOTIFICATION_RETRIES = Integer.valueOf((String)props.getProperty("iplant.max.notification.retries", "5"));
		MAX_NOTIFICATION_TASKS = Integer.valueOf((String)props.getProperty("iplant.max.notification.tasks", "1"));
		
		SLAVE_MODE = Boolean.valueOf((String)props.get("iplant.slave.mode"));
		
		NOTIFICATION_QUEUE = (String) props.getProperty("iplant.notification.service.queue", "prod.notifications.queue");
		NOTIFICATION_TOPIC = (String) props.getProperty("iplant.notification.service.topic", "prod.notifications.queue");
		
		NOTIFICATION_RETRY_QUEUE = (String) props.getProperty("iplant.notification.service.retry.queue", "retry." + NOTIFICATION_QUEUE);
		NOTIFICATION_RETRY_TOPIC = (String) props.getProperty("iplant.notification.service.retry.topic", "retry." + NOTIFICATION_TOPIC);
		
		
		FAILED_NOTIFICATION_DB_SCHEME = (String) props.getProperty("iplant.notification.failed.db.scheme", "api");
		FAILED_NOTIFICATION_DB_HOST = (String) props.getProperty("iplant.notification.failed.db.host", "mongodb");
		FAILED_NOTIFICATION_DB_PORT = NumberUtils.toInt((String)props.getProperty("iplant.notification.failed.db.port"), 27017);
		FAILED_NOTIFICATION_DB_USER = (String) props.getProperty("iplant.notification.failed.db.user", "agaveuser");
		FAILED_NOTIFICATION_DB_PWD = (String) props.getProperty("iplant.notification.failed.db.pwd", "password");
		FAILED_NOTIFICATION_COLLECTION_SIZE = NumberUtils.toInt((String)props.getProperty("iplant.notification.failed.db.max.queue.size"), 1048576);
		FAILED_NOTIFICATION_COLLECTION_LIMIT = NumberUtils.toInt((String)props.getProperty("iplant.notification.failed.db.max.queue.limit"), 1000);
		
		
		SLACK_WEBHOOK_ICON_EMOJI = (String) props.getProperty("iplant.notification.service.slack.icon.emoji");
		SLACK_WEBHOOK_ICON_URL = (String) props.getProperty("iplant.notification.service.slack.icon.url", "http://www.gravatar.com/avatar/4a01bd7c64dbb313035b23fe521f75e6");
		SLACK_WEBHOOK_USERNAME = (String) props.getProperty("iplant.notification.service.slack.username", "AgaveBot");
		
		MESSAGING_SERVICE_PROVIDER = (String) props.getProperty("iplant.messaging.provider", "beanstalk");
		MESSAGING_SERVICE_USERNAME = (String)props.get("iplant.messaging.username");
		MESSAGING_SERVICE_PASSWORD = (String)props.get("iplant.messaging.password");
		MESSAGING_SERVICE_HOST = (String)props.getProperty("iplant.messaging.host", "beanstalkd");
		MESSAGING_SERVICE_PORT = Integer.valueOf((String)props.getProperty("iplant.messaging.port", "11300"));
	}
}
