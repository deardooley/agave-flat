/**
 * 
 */
package org.iplantc.service.transfer;

import java.net.InetAddress;
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
	public static String						IPLANT_JOB_SERVICE;
	public static String						IPLANT_PROFILE_SERVICE;
	public static String						IPLANT_ATMOSPHERE_SERVICE;
	public static String						IPLANT_LOG_SERVICE;
	public static String 						IPLANT_TRANSFER_SERVICE;
	public static String                        IPLANT_NOTIFICATION_SERVICE;
	
	/* Job service settings */
	public static boolean						DEBUG;
	public static String						DEBUG_USERNAME;
	
//	public static int							REFRESH_RATE	= 0;
    public static int                           MAX_STAGING_TASKS;
    public static int                           MAX_STAGING_RETRIES;
	public static int							MAX_ARCHIVE_TASKS;
	public static int 							MAX_USER_JOBS_PER_SYSTEM;
	public static int 							MAX_USER_CONCURRENT_TRANSFERS;
	
	public static boolean						SLAVE_MODE;
	public static boolean 						CONDOR_GATEWAY;

	public static String						PUBLIC_USER_USERNAME;
	public static String						WORLD_USER_USERNAME;
	
//	public static int 							MAX_SUBMISSION_RETRIES;	
    public static Integer 						DEFAULT_PAGE_SIZE;

	public static String 						IPLANT_DOCS;

	public static boolean						ALLOW_RELAY_TRANSFERS;
	public static int 							MAX_RELAY_TRANSFER_SIZE;
	
	static
	{
		com.maverick.ssh.LicenseManager.addLicense("----BEGIN 3SP LICENSE----\r\n"
                + "Product : J2SSH Maverick\r\n"
                + "Licensee: dooley@tacc.utexas.edu\r\n"
                + "Comments: 129.114.13.56\r\n"
                + "Type    : Evaluation License\r\n"
                + "Created : 23-Jun-2013\r\n"
                + "Expires : 07-Aug-2013\r\n"
                + "\r\n"
                + "378720362A1D8FDBD9FF0D703523BB376B56F9B4DC983584\r\n"
                + "E66A3681B76553C88E3C7B7975F2BC04EBA5D768A71ABD82\r\n"
                + "C40FA60D59227606E1F367FC932B9F890E2D4C238D82D05C\r\n"
                + "628471A63B600B3DDE686AC232199FBC049A3E914F984534\r\n"
                + "845890B484213484B726ECD0F1E0794C8CA8DA56EFF87DDE\r\n"
                + "18EEFE85FFFE20BA2C54B7AEB087904C04BD70F812699587\r\n"
                + "----END 3SP LICENSE----\r\n");
		
		try
		{
			props.load(Settings.class.getClassLoader().getResourceAsStream(
					"service.properties"));

			InetAddress addr = InetAddress.getLocalHost();
			HOSTNAME = addr.getHostName();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		AUTH_SOURCE = (String) props.get("iplant.auth.source");

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
		
		IPLANT_MYPROXY_SERVER = (String) props.get("iplant.myproxy.server");

		IPLANT_MYPROXY_PORT = Integer.valueOf((String) props
				.get("iplant.myproxy.port"));

		IPLANT_LDAP_URL = (String) props.get("iplant.ldap.url");

		IPLANT_LDAP_BASE_DN = (String) props.get("iplant.ldap.base.dn");
//
//		ARCHIVE_DIRECTORY = (String) props.get("iplant.archive.path");
//
//		WORK_DIRECTORY = (String) props.get("iplant.work.path");

		TEMP_DIRECTORY = (String) props.get("iplant.server.temp.dir");
		
		IPLANT_AUTH_SERVICE = (String)props.get("iplant.auth.service");
		if (!IPLANT_AUTH_SERVICE.endsWith("/")) IPLANT_AUTH_SERVICE += "/";
		
		IPLANT_IO_SERVICE = (String) props.get("iplant.io.service");
		if (!IPLANT_IO_SERVICE.endsWith("/")) IPLANT_IO_SERVICE += "/";
		
		IPLANT_JOB_SERVICE = (String) props.get("iplant.job.service");
		if (!IPLANT_JOB_SERVICE.endsWith("/")) IPLANT_JOB_SERVICE += "/";

		IPLANT_PROFILE_SERVICE = (String) props.get("iplant.profile.service");
		if (!IPLANT_PROFILE_SERVICE.endsWith("/")) IPLANT_PROFILE_SERVICE += "/";

		IPLANT_LOG_SERVICE = (String) props.get("iplant.log.service");
		if (!IPLANT_LOG_SERVICE.endsWith("/")) IPLANT_LOG_SERVICE += "/";
		
		IPLANT_TRANSFER_SERVICE = (String) props.get("iplant.transfer.service");
		if (!IPLANT_TRANSFER_SERVICE.endsWith("/")) IPLANT_TRANSFER_SERVICE += "/";
		
		IPLANT_DOCS = (String) props.get("iplant.service.documentation");
		if (!IPLANT_DOCS.endsWith("/")) IPLANT_DOCS += "/";
		
		IPLANT_NOTIFICATION_SERVICE = (String) props.get("iplant.notification.service");
        if (!IPLANT_NOTIFICATION_SERVICE.endsWith("/")) IPLANT_NOTIFICATION_SERVICE += "/";
		
		DEBUG = Boolean.valueOf((String) props.get("iplant.debug.mode"));

		DEBUG_USERNAME = (String) props.get("iplant.debug.username");

		//BLACKLIST_REPLACEMENT_TEXT = (String) props
		//		.get("iplant.blacklist.replacement.text");
		
		KEYSTORE_PATH = (String) props.get("system.keystore.path");

		TRUSTSTORE_PATH = (String) props.get("system.truststore.path");
		
		TRUSTED_CA_CERTS_DIRECTORY = (String) props.get("system.ca.certs.path");
		
		MAIL_SERVER = (String) props.getProperty("mail.smtps.host");
		
		MAILSMTPSPROTOCOL = (String) props.getProperty("mail.smtps.auth");
		
		MAILLOGIN = (String) props.getProperty("mail.smtps.user");
		
		MAILPASSWORD = (String) props.getProperty("mail.smtps.passwd");

		SLAVE_MODE = Boolean.valueOf((String) props.get("iplant.slave.mode"));
		
		CONDOR_GATEWAY = Boolean.valueOf((String) props.get("iplant.condor.gateway.node"));
		
//		MAX_SUBMISSION_TASKS = Integer.valueOf((String) props
//				.get("iplant.max.job.tasks"));

		MAX_STAGING_TASKS = Integer.valueOf((String) props
				.get("iplant.max.staging.tasks"));

		MAX_STAGING_RETRIES = Integer.valueOf((String)props.get("iplant.max.staging.retries"));
        
        MAX_ARCHIVE_TASKS = Integer.valueOf((String) props
				.get("iplant.max.archive.tasks"));
		
		MAX_RELAY_TRANSFER_SIZE = Integer.valueOf((String) props.getProperty("iplant.max.relay.transfer.size", "2"));
		
		ALLOW_RELAY_TRANSFERS = Boolean.valueOf((String) props.getProperty("iplant.allow.relay.transfers", "false"));
		
//		MAX_SUBMISSION_RETRIES = Integer.valueOf((String) props
//				.get("iplant.max.submission.retries"));
		
		String maxUserJobs = (String) props.get("iplant.max.user.jobs.per.system");
		try {
			MAX_USER_JOBS_PER_SYSTEM = Integer.parseInt(maxUserJobs);
		} catch (Exception e) {
			MAX_USER_JOBS_PER_SYSTEM = Integer.MAX_VALUE;
		}
		
		String maxUserTransfers = (String) props.get("iplant.max.user.concurrent.transfers");
		try {
			MAX_USER_CONCURRENT_TRANSFERS = Integer.parseInt(maxUserTransfers);
		} catch (Exception e) {
			MAX_USER_CONCURRENT_TRANSFERS = Integer.MAX_VALUE;
		}
		
//		REFRESH_RATE = Integer.valueOf((String) props
//				.get("iplant.refresh.interval"));

//		IRODS_USERNAME = (String) props.get("iplant.irods.username");
//
//		IRODS_PASSWORD = (String) props.get("iplant.irods.password");
//
//		IRODS_HOST = (String) props.get("iplant.irods.host");
//
//		IRODS_PORT = Integer.valueOf((String) props.get("iplant.irods.port"));
//
//		IRODS_ZONE = (String) props.get("iplant.irods.zone");
//
//		IRODS_STAGING_DIRECTORY = (String) props
//				.get("iplant.irods.staging.directory");
//
//		IRODS_DEFAULT_RESOURCE = (String) props
//				.get("iplant.irods.default.resource");
//
//		IRODS_REFRESH_RATE = Integer.valueOf((String) props
//				.get("iplant.irods.refresh.interval"));

		PUBLIC_USER_USERNAME = (String) props.get("iplant.public.user");

		WORLD_USER_USERNAME = (String) props.get("iplant.world.user");

//		IRODS_HEARTBEAT = (String) props.get("irods.heartbeat");
		
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
