/**
 * 
 */
package org.iplantc.service.apps;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.ietf.jgss.GSSCredential;

/**
 * @author dooley
 * 
 */
public class Settings {

	private static Properties					props			= new Properties();

	private static Map<String, GSSCredential>	userProxies		= Collections
																		.synchronizedMap(new HashMap<String, GSSCredential>());

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
	public static String 						IPLANT_APPS_SERVICE;
	public static String						IPLANT_JOB_SERVICE;
	public static String						IPLANT_PROFILE_SERVICE;
	public static String						IPLANT_ATMOSPHERE_SERVICE;
	public static String						IPLANT_METADATA_SERVICE;
	public static String						IPLANT_LOG_SERVICE;
	public static String						IPLANT_SYSTEM_SERVICE;
	
	/* Job service settings */
	public static boolean						DEBUG;
	public static String						DEBUG_USERNAME;

//	public static int							REFRESH_RATE	= 0;
//	public static int							MAX_SUBMISSION_TASKS;
//	public static int							MAX_STAGING_TASKS;
//	public static int							MAX_ARCHIVE_TASKS;
	public static int 							MAX_USER_CONCURRENT_TRANSFERS;

	public static boolean						CONDOR_GATEWAY;
	
	public static boolean						SLAVE_MODE;
//
//	public static int 							MAX_SUBMISSION_RETRIES;
//
	public static String						IRODS_USERNAME;
	public static String						IRODS_PASSWORD;
	public static String						PUBLIC_USER_USERNAME;
	public static String						WORLD_USER_USERNAME;
//    public static String						IRODS_STAGING_DIRECTORY;
//    public static String 						IRODS_DEFAULT_RESOURCE;
//    public static String 						IRODS_ZONE;
//
//    public static String[]					BLACKLIST_COMMANDS;
//    public static String						DEFAULT_PARALLEL_SYSTEM;
//    public static String						DEFAULT_SERIAL_SYSTEM;
    public static Integer 						DEFAULT_PAGE_SIZE;
    public static String 						IPLANT_DOCS;

	public static String 						PUBLIC_APPS_DEFAULT_DIRECTORY;
    
	static 
	{
		props = org.iplantc.service.common.Settings.loadRuntimeProperties();
		
		HOSTNAME = org.iplantc.service.common.Settings.getLocalHostname();

		AUTH_SOURCE = props.getProperty("iplant.auth.source", "none").trim();

		COMMUNITY_USERNAME = (String) props.get("iplant.community.username");

		COMMUNITY_PASSWORD = (String) props.get("iplant.community.password");

		String trustedUsers = (String)props.get("iplant.trusted.users");
		if (trustedUsers != null && !trustedUsers.equals("")) {
			for (String user: trustedUsers.split(",")) {
				TRUSTED_USERS.add(user);
			}
		}
		
		API_VERSION = (String)props.getProperty("iplant.api.version");
		
		SERVICE_VERSION = (String)props.getProperty("iplant.service.version");
		
//        IRODS_ZONE = (String)props.get("iplant.irods.zone");
//
//        IRODS_STAGING_DIRECTORY = (String)props.get("iplant.irods.staging.directory");
//
//        IRODS_DEFAULT_RESOURCE = (String)props.get("iplant.irods.default.resource");
		IPLANT_AUTH_SERVICE = (String)props.get("iplant.auth.service");
		
		IPLANT_MYPROXY_SERVER = (String) props.get("iplant.myproxy.server");

		IPLANT_MYPROXY_PORT = Integer.valueOf((String) props
				.get("iplant.myproxy.port"));

		IPLANT_LDAP_URL = (String) props.get("iplant.ldap.url");

		IPLANT_LDAP_BASE_DN = (String) props.get("iplant.ldap.base.dn");

//		ARCHIVE_DIRECTORY = (String) props.get("iplant.archive.path");
//
//		WORK_DIRECTORY = (String) props.get("iplant.work.path");

		TEMP_DIRECTORY = (String) props.get("iplant.server.temp.dir");
		
//		IPLANT_CHARGE_NUMBER = (String) props.get("iplant.charge.number");
		
		IPLANT_IO_SERVICE = (String) props.get("iplant.io.service");
		if (!IPLANT_IO_SERVICE.endsWith("/")) IPLANT_IO_SERVICE += "/";
		
		IPLANT_APPS_SERVICE = (String) props.get("iplant.app.service");
		if (!IPLANT_APPS_SERVICE.endsWith("/")) IPLANT_APPS_SERVICE += "/";
		
		IPLANT_JOB_SERVICE = (String) props.get("iplant.job.service");
		if (!IPLANT_JOB_SERVICE.endsWith("/")) IPLANT_JOB_SERVICE += "/";

		IPLANT_PROFILE_SERVICE = (String) props.get("iplant.profile.service");
		if (!IPLANT_PROFILE_SERVICE.endsWith("/")) IPLANT_PROFILE_SERVICE += "/";

		IPLANT_ATMOSPHERE_SERVICE = (String) props.get("iplant.atmosphere.service");
		if (!IPLANT_ATMOSPHERE_SERVICE.endsWith("/")) IPLANT_ATMOSPHERE_SERVICE += "/";
		
		IPLANT_LOG_SERVICE = (String) props.get("iplant.log.service");
		if (!IPLANT_LOG_SERVICE.endsWith("/")) IPLANT_LOG_SERVICE += "/";
		
		IPLANT_SYSTEM_SERVICE = (String) props.get("iplant.system.service");
		if (!IPLANT_SYSTEM_SERVICE.endsWith("/")) IPLANT_SYSTEM_SERVICE += "/";
		
		IPLANT_METADATA_SERVICE = (String) props.get("iplant.metadata.service");
		if (!IPLANT_METADATA_SERVICE.endsWith("/")) IPLANT_METADATA_SERVICE += "/";
		
		IPLANT_DOCS = (String) props.get("iplant.service.documentation");
		if (!IPLANT_DOCS.endsWith("/")) IPLANT_DOCS += "/";
		
		CONDOR_GATEWAY = Boolean.valueOf((String) props.get("iplant.condor.gateway.node"));
		
		DEBUG = Boolean.valueOf((String) props.get("iplant.debug.mode"));

		DEBUG_USERNAME = (String) props.get("iplant.debug.username");

		KEYSTORE_PATH = (String) props.get("system.keystore.path");

		TRUSTSTORE_PATH = (String) props.get("system.truststore.path");
		
		TRUSTED_CA_CERTS_DIRECTORY = (String) props.get("system.ca.certs.path");
		
		MAIL_SERVER = (String) props.getProperty("mail.smtps.host");
		
		MAILSMTPSPROTOCOL = (String) props.getProperty("mail.smtps.auth");
		
		MAILLOGIN = (String) props.getProperty("mail.smtps.user");
		
		MAILPASSWORD = (String) props.getProperty("mail.smtps.passwd");

		SLAVE_MODE = Boolean.valueOf((String) props.get("iplant.slave.mode"));
//		
//		MAX_SUBMISSION_TASKS = Integer.valueOf((String) props
//				.get("iplant.max.job.tasks"));
//
//		MAX_STAGING_TASKS = Integer.valueOf((String) props
//				.get("iplant.max.staging.tasks"));
//
//		MAX_ARCHIVE_TASKS = Integer.valueOf((String) props
//				.get("iplant.max.archive.tasks"));
//		
//		MAX_SUBMISSION_RETRIES = Integer.valueOf((String) props
//				.get("iplant.max.submission.retries"));
//		
		String maxUserTransfers = (String) props.get("iplant.max.user.concurrent.transfers");
		try {
			MAX_USER_CONCURRENT_TRANSFERS = Integer.parseInt(maxUserTransfers);
		} catch (Exception e) {
			MAX_USER_CONCURRENT_TRANSFERS = Integer.MAX_VALUE;
		}
		
//		REFRESH_RATE = Integer.valueOf((String) props
//				.get("iplant.refresh.interval"));
		
		IRODS_USERNAME = (String) props.get("iplant.irods.username");

		IRODS_PASSWORD = (String) props.get("iplant.irods.password");

		PUBLIC_USER_USERNAME = (String) props.get("iplant.public.user");

		WORLD_USER_USERNAME = (String) props.get("iplant.world.user");
		
		PUBLIC_APPS_DEFAULT_DIRECTORY = (String) props.getProperty("iplant.default.apps.dir", "/api/" + API_VERSION + "/apps/");
		if (!PUBLIC_APPS_DEFAULT_DIRECTORY.endsWith("/")) PUBLIC_APPS_DEFAULT_DIRECTORY += "/";
		

//        String blacklistCommands = (String) props.get("iplant.blacklist.commands");
//        if (blacklistCommands != null && blacklistCommands.contains(",")) {
//            BLACKLIST_COMMANDS = StringUtils.split(blacklistCommands, ',');
//        } else {
//            BLACKLIST_COMMANDS = new String[1];
//            BLACKLIST_COMMANDS[0] = blacklistCommands;
//        }
//        
//        DEFAULT_PARALLEL_SYSTEM = (String) props.get("iplant.default.parallel.system");
//        DEFAULT_SERIAL_SYSTEM = (String) props.get("iplant.default.serial.system");
        DEFAULT_PAGE_SIZE = Integer.parseInt((String) props.getProperty("iplant.default.page.size", "25"));
	}

	public static GSSCredential getProxyForUser(String username)
	{
		return userProxies.get(username);
	}

	public static void setProxyForUser(String username, GSSCredential proxy)
	{
		userProxies.put(username, proxy);
	}

}
