/**
 * 
 */
package org.iplantc.service.jobs;

import java.io.InputStream;
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
	
	private static final String PROPERTY_FILE = "service.properties";
	
	private static Properties					props			= new Properties();

	/* Trusted user settings */
	public static List<String> 					TRUSTED_USERS = new ArrayList<String>();
	
	public static String						HOSTNAME;
	public static String						AUTH_SOURCE;
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
//	public static String						ARCHIVE_DIRECTORY;
//	public static String						WORK_DIRECTORY;

	/* Iplant API service endpoints */
	public static String						IPLANT_APPS_SERVICE;
	public static String						IPLANT_IO_SERVICE;
	public static String						IPLANT_JOB_SERVICE;
	public static String						IPLANT_PROFILE_SERVICE;
	public static String						IPLANT_ATMOSPHERE_SERVICE;
	public static String						IPLANT_LOG_SERVICE;
	public static String						IPLANT_SYSTEM_SERVICE;
	public static String						IPLANT_METADATA_SERVICE;
	public static String						IPLANT_NOTIFICATION_SERVICE;
	
	/* Job service settings */
	public static boolean						DEBUG;
	public static String						DEBUG_USERNAME;
//	public static String						DEFAULT_SERIAL_SYSTEM;
//	public static String						DEFAULT_PARALLEL_SYSTEM;
	public static String[]						BLACKLIST_COMMANDS;
	public static String[]						BLACKLIST_FILES;
	//public static String						BLACKLIST_REPLACEMENT_TEXT;

//	public static int							REFRESH_RATE	= 0;
	public static int							MAX_SUBMISSION_TASKS;
	public static int							MAX_STAGING_TASKS;
	public static int							MAX_ARCHIVE_TASKS;
	public static int							MAX_MONITORING_TASKS;
	
	public static boolean						SLAVE_MODE;
	public static boolean 						CONDOR_GATEWAY;

	public static String						IRODS_USERNAME;
	public static String						IRODS_PASSWORD;
//	public static String						IRODS_HOST;
//	public static int							IRODS_PORT;
//	public static String						IRODS_ZONE;
//	public static String						IRODS_STAGING_DIRECTORY;
//	public static String						IRODS_DEFAULT_RESOURCE;
//	public static int							IRODS_REFRESH_RATE;	// how often to check the irods connection info
	public static String						PUBLIC_USER_USERNAME;
	public static String						WORLD_USER_USERNAME;
//	public static String						IRODS_HEARTBEAT;

//	public static String						XSEDE_IIS_PROFILE_SERVICE_URL;
//	public static String						TERAGRID_IIS_CTSS_SERVICE_URL;
//	public static String						TERAGRID_IIS_RDR_SERVICE_URL;
//	public static String						TERAGRID_IIS_OUTAGE_SERVICE_URL;
//	public static String						TERAGRID_OUTAGE_SERVICE_URL;
//	public static String						TERAGRID_IIS_KIT_SERVICE_URL;
//	public static int							TERAGRID_REFRESH_RATE;

	public static int 							MAX_SUBMISSION_RETRIES;

//	public static String 						IPLANT_CHARGE_NUMBER;
    public static Integer 						DEFAULT_PAGE_SIZE;
    public static String 						IPLANT_DOCS;

	public static String 						LOCAL_SYSTEM_ID;

	public static boolean 						ENABLE_ZOMBIE_CLEANUP;
	
	static
	{
		props = org.iplantc.service.common.Settings.loadRuntimeProperties();
		
		HOSTNAME = org.iplantc.service.common.Settings.getLocalHostname();

		AUTH_SOURCE = (String) props.get("iplant.auth.source");

		COMMUNITY_USERNAME = (String) props.get("iplant.community.username");

		COMMUNITY_PASSWORD = (String) props.get("iplant.community.password");

		String trustedUsers = (String)props.get("iplant.trusted.users");
		if (trustedUsers != null && !trustedUsers.equals("")) {
			for (String user: trustedUsers.split(",")) {
				TRUSTED_USERS.add(user);
			}
		}
		
		SERVICE_VERSION = (String)props.getProperty("iplant.service.version");
		
		IPLANT_AUTH_SERVICE = (String)props.get("iplant.auth.service");
		
		IPLANT_MYPROXY_SERVER = (String) props.get("iplant.myproxy.server");

		IPLANT_MYPROXY_PORT = Integer.valueOf((String) props
				.get("iplant.myproxy.port"));

		IPLANT_LDAP_URL = (String) props.get("iplant.ldap.url");

		IPLANT_LDAP_BASE_DN = (String) props.get("iplant.ldap.base.dn");
//
//		ARCHIVE_DIRECTORY = (String) props.get("iplant.archive.path");
//
//		WORK_DIRECTORY = (String) props.get("iplant.work.path");
		
//		IPLANT_CHARGE_NUMBER = (String) props.get("iplant.charge.number");
//		
		IPLANT_APPS_SERVICE = (String) props.get("iplant.app.service");
		if (!IPLANT_APPS_SERVICE.endsWith("/")) IPLANT_APPS_SERVICE += "/";
		
		IPLANT_IO_SERVICE = (String) props.get("iplant.io.service");
		if (!IPLANT_IO_SERVICE.endsWith("/")) IPLANT_IO_SERVICE += "/";
		
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
		
		IPLANT_NOTIFICATION_SERVICE = (String) props.get("iplant.notification.service");
		if (!IPLANT_NOTIFICATION_SERVICE.endsWith("/")) IPLANT_NOTIFICATION_SERVICE += "/";
		
		IPLANT_METADATA_SERVICE = (String) props.get("iplant.metadata.service");
		if (!IPLANT_METADATA_SERVICE.endsWith("/")) IPLANT_METADATA_SERVICE += "/";
		
		IPLANT_DOCS = (String) props.get("iplant.service.documentation");
		if (!IPLANT_DOCS.endsWith("/")) IPLANT_DOCS += "/";
		
		DEBUG = Boolean.valueOf((String) props.get("iplant.debug.mode"));

		DEBUG_USERNAME = (String) props.get("iplant.debug.username");

		String blacklistCommands = (String) props.get("iplant.blacklist.commands");
		if (blacklistCommands != null && blacklistCommands.contains(",")) {
//		if (StringUtils.contains(blacklistCommands, ",")) {
			BLACKLIST_COMMANDS = StringUtils.split(blacklistCommands, ',');
		} else {
			BLACKLIST_COMMANDS = new String[1];
			BLACKLIST_COMMANDS[0] = blacklistCommands;
		}
		
		String blacklistFiles = (String) props.get("iplant.blacklist.files");
		if (blacklistFiles != null &&blacklistFiles.contains(",")) {
			BLACKLIST_FILES = StringUtils.split(blacklistFiles, ',');
		} else {
			BLACKLIST_FILES = new String[1];
			BLACKLIST_FILES[0] = blacklistFiles;
		}

		KEYSTORE_PATH = (String) props.get("system.keystore.path");

		TRUSTSTORE_PATH = (String) props.get("system.truststore.path");
		
		TRUSTED_CA_CERTS_DIRECTORY = (String) props.get("system.ca.certs.path");
		
		MAIL_SERVER = (String) props.getProperty("mail.smtps.host");
		
		MAILSMTPSPROTOCOL = (String) props.getProperty("mail.smtps.auth");
		
		MAILLOGIN = (String) props.getProperty("mail.smtps.user");
		
		MAILPASSWORD = (String) props.getProperty("mail.smtps.passwd");

		SLAVE_MODE = Boolean.valueOf((String) props.get("iplant.slave.mode"));
		
		CONDOR_GATEWAY = Boolean.valueOf((String) props.get("iplant.condor.gateway.node"));
		
		LOCAL_SYSTEM_ID = (String) props.getProperty("iplant.local.system.id");
		
		MAX_SUBMISSION_TASKS = Integer.valueOf((String) props
				.getProperty("iplant.max.submission.tasks", "0"));

		MAX_STAGING_TASKS = Integer.valueOf((String) props
				.getProperty("iplant.max.staging.tasks", "0"));

		MAX_ARCHIVE_TASKS = Integer.valueOf((String) props
				.getProperty("iplant.max.archive.tasks", "0"));
		
		MAX_SUBMISSION_RETRIES = Integer.valueOf((String) props
				.getProperty("iplant.max.submission.retries", "0"));
		
		MAX_MONITORING_TASKS = Integer.valueOf((String) props
				.getProperty("iplant.max.monitoring.tasks", "0"));
		
		ENABLE_ZOMBIE_CLEANUP = Boolean.valueOf((String) props
				.getProperty("iplant.enable.zombie.cleanup", "false"));
		
//		String maxUserJobs = (String) props.get("iplant.max.user.jobs.per.system");
//		try {
//			MAX_USER_JOBS_PER_SYSTEM = Integer.parseInt(maxUserJobs);
//		} catch (Exception e) {
//			MAX_USER_JOBS_PER_SYSTEM = Integer.MAX_VALUE;
//		}
		
//		String maxUserTransfers = (String) props.get("iplant.max.user.concurrent.transfers");
//		try {
//			MAX_USER_CONCURRENT_TRANSFERS = Integer.parseInt(maxUserTransfers);
//		} catch (Exception e) {
//			MAX_USER_CONCURRENT_TRANSFERS = Integer.MAX_VALUE;
//		}
		
		if (blacklistFiles != null && blacklistFiles.contains(",")) {
			BLACKLIST_FILES = StringUtils.split(blacklistFiles, ',');
		} else {
			BLACKLIST_FILES = new String[1];
			BLACKLIST_FILES[0] = blacklistFiles;
		}
		
//		REFRESH_RATE = Integer.valueOf((String) props
//				.get("iplant.refresh.interval"));
		
		IRODS_USERNAME = (String) props.get("iplant.irods.username");

		IRODS_PASSWORD = (String) props.get("iplant.irods.password");

		PUBLIC_USER_USERNAME = (String) props.get("iplant.public.user");

		WORLD_USER_USERNAME = (String) props.get("iplant.world.user");
		
		DEFAULT_PAGE_SIZE = Integer.parseInt((String) props.getProperty("iplant.default.page.size", "25"));
	}

	
	/**
	 * Reads in the tenant id every time from the service properties file.
	 * This can be updated in real time to re-read the value at run time.
	 * @return the Tenant.tenantCode
	 */
	public static String getDedicatedTenantIdFromServiceProperties()
	{
		Properties props = new Properties();
		InputStream stream = null;
		try
		{
			stream = Settings.class.getClassLoader().getResourceAsStream(PROPERTY_FILE);
			props.load(stream);
			
			return (String)props.get("iplant.dedicated.tenant.id");
		}
		catch (Exception e)
		{
			log.warn("Unable to load " + PROPERTY_FILE + ".");
			return null;
		}
		finally {
			if (stream != null) try {stream.close();} catch (Exception e){}
		}
	}
	
	/**
	 * Reads in the iplant.dedicated.tenant.id property every time from 
	 * the service properties file. This can be updated in real time to 
	 * prevent workers from accepting any more work at run time.
	 * 
	 * @return true if iplant.dedicated.tenant.id is true, false otherwise
	 */
	public static boolean isDrainingQueuesEnabled()
	{
		Properties props = new Properties();
		InputStream stream = null;
		try
		{
			stream = Settings.class.getClassLoader().getResourceAsStream(PROPERTY_FILE);
			props.load(stream);
			
			return Boolean.parseBoolean(props.getProperty("iplant.drain.all.queues", "false"));
		}
		catch (Exception e)
		{
			log.warn("Unable to load " + PROPERTY_FILE + ".");
			return false;
		}
		finally {
			if (stream != null) try {stream.close();} catch (Exception e) {}
		}
	}

}
