/**
 * 
 */
package org.iplantc.service.jobs;

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
    // Tracing.
    private static final Logger _log = Logger.getLogger(Settings.class);

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
	public static String[]						BLACKLIST_COMMANDS;
	public static String[]						BLACKLIST_FILES;

	public static boolean						SLAVE_MODE;
	public static boolean 						CONDOR_GATEWAY;

	public static String						IRODS_USERNAME;
	public static String						IRODS_PASSWORD;
	public static String						PUBLIC_USER_USERNAME;
	public static String						WORLD_USER_USERNAME;

    public static Integer 						DEFAULT_PAGE_SIZE;
    public static String 						IPLANT_DOCS;

	public static String 						LOCAL_SYSTEM_ID;

	// Configuration value used in queue-based implementation (RabbitMQ). 
	public static boolean                       JOB_SCHEDULER_MODE;
	public static boolean                       JOB_WORKER_MODE;
	public static boolean                       JOB_ADMIN_MODE;
	public static boolean                       JOB_ENABLE_ZOMBIE_CLEANUP;
	public static int                           JOB_CLAIM_POLL_ITERATIONS;
	public static long                          JOB_CLAIM_POLL_SLEEP_MS;
	public static int                           JOB_UUID_QUERY_LIMIT;
	public static int                           JOB_SCHEDULER_LEASE_SECONDS;
	public static long                          JOB_SCHEDULER_NORMAL_POLL_MS;
	public static int                           JOB_INTERRUPT_TTL_SECONDS;
	public static long                          JOB_INTERRUPT_DELETE_MS;
	public static long                          JOB_ZOMBIE_MONITOR_MS;
	public static long                          JOB_THREAD_DEATH_MS;
	public static long                          JOB_THREAD_DEATH_POLL_MS;
	public static long                          JOB_CONNECTION_CLOSE_MS;
	public static long                          JOB_WORKER_INIT_RETRY_MS;
	public static int                           JOB_MAX_SUBMISSION_RETRIES;
	public static String                        JOB_QUEUE_CONFIG_FOLDER;
	public static int                           JOB_MAX_THREADS_PER_QUEUE;
	public static int                           JOB_THREAD_RESTART_SECONDS;
	public static int                           JOB_THREAD_RESTART_LIMIT;
	
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
		
		// ----------------- Queue-Based Implementation -----------------
		// See the agave-api's service.properties file for parameter descriptions.
		// Note that the following parameters are set to their default values if
		// any runtime error is encountered during value assignment.
		
		try {JOB_SCHEDULER_MODE = Boolean.valueOf(props
		        .getProperty("iplant.service.jobs.scheduler.mode", "true"));}
		    catch (Exception e) {
		        _log.error("Error initializing setting JOB_SCHEDULER_MODE, using default.", e);
		        JOB_SCHEDULER_MODE = true;
		    }
		
		try {JOB_WORKER_MODE = Boolean.valueOf(props
		        .getProperty("iplant.service.jobs.worker.mode", "true"));}
            catch (Exception e) {
                _log.error("Error initializing setting JOB_WORKER_MODE, using default.", e);
                JOB_WORKER_MODE = true;
            }
		
        try {JOB_ADMIN_MODE = Boolean.valueOf(props
                .getProperty("iplant.service.jobs.admin.mode", "false"));}
            catch (Exception e) {
                _log.error("Error initializing setting JOB_ADMIN_MODE, using default.", e);
                JOB_ADMIN_MODE = false;
            }
        
        try {JOB_ENABLE_ZOMBIE_CLEANUP = Boolean.valueOf((String) props
                .getProperty("iplant.service.jobs.enable.zombie.cleanup", "false"));}
            catch (Exception e) {
                _log.error("Error initializing setting ENABLE_ZOMBIE_CLEANUP, using default.", e);
                JOB_ENABLE_ZOMBIE_CLEANUP = false;
            }
        
		try {JOB_CLAIM_POLL_ITERATIONS = Integer.valueOf(props
		        .getProperty("iplant.service.jobs.claim.poll.iterations", "15"));}
            catch (Exception e) {
                _log.error("Error initializing setting JOB_CLAIM_POLL_ITERATIONS, using default.", e);
                JOB_CLAIM_POLL_ITERATIONS = 15;
            }
		
		try {JOB_CLAIM_POLL_SLEEP_MS = Long.valueOf(props
		        .getProperty("iplant.service.jobs.claim.poll.sleep.ms", "1000"));}
            catch (Exception e) {
                _log.error("Error initializing setting JOB_CLAIM_POLL_SLEEP_MS, using default.", e);
                JOB_CLAIM_POLL_SLEEP_MS = 1000;
            }
		
		try {JOB_UUID_QUERY_LIMIT = Integer.valueOf(props
		        .getProperty("iplant.service.jobs.uuid.query.limit", "1000"));}
            catch (Exception e) {
                _log.error("Error initializing setting JOB_UUID_QUERY_LIMIT, using default.", e);
                JOB_UUID_QUERY_LIMIT = 1000;
            }
		
		try {JOB_SCHEDULER_LEASE_SECONDS = Integer.valueOf(props
		        .getProperty("iplant.service.jobs.scheduler.lease.seconds", "250"));}
            catch (Exception e) {
                _log.error("Error initializing setting JOB_SCHEDULER_LEASE_SECONDS, using default.", e);
                JOB_SCHEDULER_LEASE_SECONDS = 250;
            }
		
		try {JOB_SCHEDULER_NORMAL_POLL_MS = Long.valueOf(props
		        .getProperty("iplant.service.jobs.scheduler.normal.poll.ms", "10000"));}
            catch (Exception e) {
                _log.error("Error initializing setting JOB_SCHEDULER_NORMAL_POLL_MS, using default.", e);
                JOB_SCHEDULER_NORMAL_POLL_MS = 10000;
            }
		
		try {JOB_INTERRUPT_TTL_SECONDS = Integer.valueOf(props
		        .getProperty("iplant.service.jobs.interrupt.ttl.seconds", "3600"));}
            catch (Exception e) {
                _log.error("Error initializing setting JOB_INTERRUPT_TTL_SECONDS, using default.", e);
                JOB_INTERRUPT_TTL_SECONDS = 3600;
            }
		
		try {JOB_INTERRUPT_DELETE_MS = Long.valueOf(props
		        .getProperty("iplant.service.jobs.interrupt.delete.ms", "240000"));}
            catch (Exception e) {
                _log.error("Error initializing setting JOB_INTERRUPT_DELETE_MS", e);
                JOB_INTERRUPT_DELETE_MS = 240000;
            }
		
		try {JOB_ZOMBIE_MONITOR_MS = Long.valueOf(props
		        .getProperty("iplant.service.jobs.zombie.monitor.ms", "600000"));}
            catch (Exception e) {
                _log.error("Error initializing setting JOB_ZOMBIE_MONITOR_MS, using default.", e);
                JOB_ZOMBIE_MONITOR_MS = 600000;
            }
		
		try {JOB_THREAD_DEATH_MS = Long.valueOf(props
		        .getProperty("iplant.service.jobs.thread.death.ms", "10000"));}
            catch (Exception e) {
                _log.error("Error initializing setting JOB_THREAD_DEATH_MS, using default.", e);
                JOB_THREAD_DEATH_MS = 10000;
            }
		
		try {JOB_THREAD_DEATH_POLL_MS = Long.valueOf(props
		        .getProperty("iplant.service.jobs.thread.death.poll.ms", "100"));}
            catch (Exception e) {
                _log.error("Error initializing setting JOB_THREAD_DEATH_POLL_MS, using default.", e);
                JOB_THREAD_DEATH_POLL_MS = 100;
            }
		
		try {JOB_CONNECTION_CLOSE_MS = Long.valueOf(props
		        .getProperty("iplant.service.jobs.connection.close.ms", "5000"));}
		    catch (Exception e) {
		        _log.error("Error initializing setting JOB_CONNECTION_CLOSE_MS, using default.", e);
		        JOB_CONNECTION_CLOSE_MS = 5000;
		    }
		
		try {JOB_WORKER_INIT_RETRY_MS = Long.valueOf(props
		        .getProperty("iplant.service.jobs.worker.init.retry.ms", "10000"));}
            catch (Exception e) {
                _log.error("Error initializing setting JOB_WORKER_INIT_RETRY_MS, using default.", e);
                JOB_WORKER_INIT_RETRY_MS = 10000;
            }
		
		try {JOB_MAX_SUBMISSION_RETRIES = Integer.valueOf((String) props
                .getProperty("iplant.service.jobs.max.submission.retries", "0"));}
            catch (Exception e) {
                _log.error("Error initializing setting MAX_SUBMISSION_RETRIES, using default.", e);
                JOB_MAX_SUBMISSION_RETRIES = 0;
            }
        
        try {JOB_QUEUE_CONFIG_FOLDER = props
                .getProperty("iplant.service.jobs.queue.config.folder", "basic");}
            catch (Exception e) {
                _log.error("Error initializing setting JOB_QUEUE_CONFIG_FOLDER, using default.", e);
                JOB_QUEUE_CONFIG_FOLDER = "basic";
            }
        
        try {JOB_MAX_THREADS_PER_QUEUE = Integer.valueOf(props
                .getProperty("iplant.service.jobs.max.threads.per.queue", "50"));}
            catch (Exception e) {
                _log.error("Error initializing setting JOB_MAX_THREADS_PER_QUEUE, using default.", e);
                JOB_MAX_THREADS_PER_QUEUE = 50;
            }
        
        try {JOB_THREAD_RESTART_SECONDS = Integer.valueOf(props
                .getProperty("iplant.service.jobs.thread.restart.seconds", "300"));}
            catch (Exception e) {
                _log.error("Error initializing setting JOB_THREAD_RESTART_SECONDS, using default.", e);
                JOB_THREAD_RESTART_SECONDS = 300;
            }
        
        try {JOB_THREAD_RESTART_LIMIT = Integer.valueOf(props
                .getProperty("iplant.service.jobs.thread.restart.limit", "50"));}
            catch (Exception e) {
                _log.error("Error initializing setting JOB_THREAD_RESTART_LIMIT, using default.", e);
                JOB_THREAD_RESTART_LIMIT = 50;
            }
        
		// --------------- End Queue-Based Implementation ---------------
		
		if (blacklistFiles != null && blacklistFiles.contains(",")) {
			BLACKLIST_FILES = StringUtils.split(blacklistFiles, ',');
		} else {
			BLACKLIST_FILES = new String[1];
			BLACKLIST_FILES[0] = blacklistFiles;
		}
		
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
		try
		{
			props.load(Settings.class.getClassLoader().getResourceAsStream(PROPERTY_FILE));
			
			return (String)props.get("iplant.dedicated.tenant.id");
		}
		catch (Exception e)
		{
			return null;
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
		try
		{
			props.load(Settings.class.getClassLoader().getResourceAsStream(PROPERTY_FILE));
			
			return Boolean.parseBoolean(props.getProperty("iplant.drain.all.queues", "false"));
		}
		catch (Exception e)
		{
			return false;
		}
	}

}
