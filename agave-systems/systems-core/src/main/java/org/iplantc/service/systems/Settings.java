/**
 * 
 */
package org.iplantc.service.systems;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

/**
 * @author dooley
 * 
 */
public class Settings {

	private static final Logger log = Logger.getLogger(Settings.class);
	
	private static Properties					props			= new Properties();

	/* Trusted user settings */
	public static List<String> 					TRUSTED_USERS = new ArrayList<String>();
	
	public static String						HOSTNAME;
	public static String						AUTH_SOURCE;
	
	public static String						API_VERSION;
	public static String						SERVICE_VERSION;
	
	/* Community user credentials */
	public static String						COMMUNITY_USERNAME;
	public static String						COMMUNITY_PASSWORD;

	/* Authentication service settings */
	public static String 						IPLANT_AUTH_SERVICE;
	public static String						IPLANT_MYPROXY_SERVER;
	public static int							IPLANT_MYPROXY_PORT;
	public static String						IPLANT_LDAP_URL;
	public static String						IPLANT_LDAP_BASE_DN;
	public static String						KEYSTORE_PATH;
	public static String						TRUSTSTORE_PATH;
	public static String						TRUSTED_CA_CERTS_DIRECTORY;
	public static String						MAIL_SERVER;
	public static String 						MAILSMTPSPROTOCOL;
	public static String 						MAILLOGIN;    
    public static String 						MAILPASSWORD;
    
	/* Data service settings */
	public static String						TEMP_DIRECTORY;

	/* Iplant API service endpoints */
	public static String						IPLANT_IO_SERVICE;
	public static String						IPLANT_JOB_SERVICE;
	public static String						IPLANT_PROFILE_SERVICE;
	public static String						IPLANT_LOG_SERVICE;
	public static String 						IPLANT_SYSTEM_SERVICE;
	public static String 						IPLANT_METADATA_SERVICE;
	
	/* Job service settings */
	public static boolean						DEBUG;
	public static String						DEBUG_USERNAME;


	public static boolean						SLAVE_MODE;
	
	public static String						IRODS_USERNAME;
	public static String						IRODS_PASSWORD;
	public static String						IRODS_HOST;
	public static int							IRODS_PORT;
	public static String						IRODS_ZONE;
	public static String						IRODS_STAGING_DIRECTORY;
	public static String						IRODS_DEFAULT_RESOURCE;
	public static String						PUBLIC_USER_USERNAME;
	public static String						WORLD_USER_USERNAME;

    public static Integer 						DEFAULT_PAGE_SIZE;

	public static String 						IPLANT_DOCS;

	public static long							MAX_REMOTE_OPERATION_TIME;
	
	static
	{
		props = org.iplantc.service.common.Settings.loadRuntimeProperties();
		
		HOSTNAME = org.iplantc.service.common.Settings.getLocalHostname();

		API_VERSION = props.getProperty("iplant.api.version");
		
		SERVICE_VERSION = props.getProperty("iplant.service.version");
		
		COMMUNITY_USERNAME = props.getProperty("iplant.community.username");

		COMMUNITY_PASSWORD = props.getProperty("iplant.community.password");

		String trustedUsers = props.getProperty("iplant.trusted.users");
		if (!StringUtils.isEmpty(trustedUsers)) {
			for (String user: trustedUsers.split(",")) {
				TRUSTED_USERS.add(user);
			}
		}
		
		SERVICE_VERSION = props.getProperty("iplant.service.version");
		
		IPLANT_AUTH_SERVICE = props.getProperty("iplant.auth.service");
		
		IPLANT_MYPROXY_SERVER = props.getProperty("iplant.myproxy.server");

		try {IPLANT_MYPROXY_PORT = Integer.valueOf(props.getProperty("iplant.myproxy.port"));}
			catch (Exception e) {
				log.error("Failure loading setting iplant.myproxy.port", e);
			}

		IPLANT_LDAP_URL = props.getProperty("iplant.ldap.url");

		IPLANT_LDAP_BASE_DN = props.getProperty("iplant.ldap.base.dn");

		TEMP_DIRECTORY = props.getProperty("iplant.server.temp.dir");
		
		IPLANT_SYSTEM_SERVICE = props.getProperty("iplant.system.service");
		if (IPLANT_SYSTEM_SERVICE != null)
			if (!IPLANT_SYSTEM_SERVICE.endsWith("/")) IPLANT_SYSTEM_SERVICE += "/";
		
		IPLANT_IO_SERVICE = props.getProperty("iplant.io.service");
		if (IPLANT_IO_SERVICE != null)
			if (!IPLANT_IO_SERVICE.endsWith("/")) IPLANT_IO_SERVICE += "/";
		
		IPLANT_JOB_SERVICE = props.getProperty("iplant.job.service");
		if (IPLANT_JOB_SERVICE != null)
			if (!IPLANT_JOB_SERVICE.endsWith("/")) IPLANT_JOB_SERVICE += "/";

		IPLANT_PROFILE_SERVICE = props.getProperty("iplant.profile.service");
		if (IPLANT_PROFILE_SERVICE != null)
			if (!IPLANT_PROFILE_SERVICE.endsWith("/")) IPLANT_PROFILE_SERVICE += "/";
		
		IPLANT_LOG_SERVICE = props.getProperty("iplant.log.service");
		if (IPLANT_LOG_SERVICE != null)
			if (!IPLANT_LOG_SERVICE.endsWith("/")) IPLANT_LOG_SERVICE += "/";
		
		IPLANT_METADATA_SERVICE = props.getProperty("iplant.metadata.service");
		if (IPLANT_METADATA_SERVICE != null)
			if (!IPLANT_METADATA_SERVICE.endsWith("/")) IPLANT_METADATA_SERVICE += "/";
		
		IPLANT_DOCS = props.getProperty("iplant.service.documentation");
		if (IPLANT_DOCS != null)
			if (!IPLANT_DOCS.endsWith("/")) IPLANT_DOCS += "/";
		
		try {DEBUG = Boolean.valueOf(props.getProperty("iplant.debug.mode", "false"));}
			catch (Exception e) {
        		log.error("Failure loading setting iplant.debug.mode.", e);
        		DEBUG = false;
			}

		DEBUG_USERNAME = props.getProperty("iplant.debug.username");
		
		KEYSTORE_PATH = props.getProperty("system.keystore.path");

		TRUSTSTORE_PATH = props.getProperty("system.truststore.path");
		
		TRUSTED_CA_CERTS_DIRECTORY = props.getProperty("system.ca.certs.path");
		
		MAIL_SERVER = props.getProperty("mail.smtps.host");
		
		MAILSMTPSPROTOCOL = props.getProperty("mail.smtps.auth");
		
		MAILLOGIN =  props.getProperty("mail.smtps.user");
		
		MAILPASSWORD = props.getProperty("mail.smtps.passwd");

		try {SLAVE_MODE = Boolean.valueOf(props.getProperty("iplant.slave.mode", "false"));}
			catch (Exception e) {
				log.error("Failure loading setting iplant.slave.mode.", e);
				SLAVE_MODE = false;
			}
		
		IRODS_USERNAME = props.getProperty("iplant.irods.username");

		IRODS_PASSWORD =  props.getProperty("iplant.irods.password");

		IRODS_HOST = props.getProperty("iplant.irods.host");

		try {IRODS_PORT = Integer.valueOf((String) props.getProperty("iplant.irods.port"));}
			catch (Exception e) {
				log.error("Failure loading setting iplant.irods.port.", e);
			}

		IRODS_ZONE =  props.getProperty("iplant.irods.zone");

		IRODS_STAGING_DIRECTORY = props.getProperty("iplant.irods.staging.directory");

		IRODS_DEFAULT_RESOURCE = props.getProperty("iplant.irods.default.resource");

		PUBLIC_USER_USERNAME = props.getProperty("iplant.public.user");

		WORLD_USER_USERNAME = props.getProperty("iplant.world.user");

		try {DEFAULT_PAGE_SIZE = Integer.parseInt((String) props.getProperty("iplant.default.page.size", "25"));}
			catch (Exception e) {
				log.error("Failure loading setting iplant.default.page.size.", e);
				DEFAULT_PAGE_SIZE = 25;
			}
		
		try {MAX_REMOTE_OPERATION_TIME = Integer.parseInt(props.getProperty("iplant.max.remote.connection.time", "90"));}
		catch (Exception e) {
			log.error("Failure loading setting iplant.max.remote.connection.time.", e);
			MAX_REMOTE_OPERATION_TIME = 90;
		}
	}

}
