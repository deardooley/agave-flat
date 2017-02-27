package org.iplantc.service.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.common.util.IPAddressValidator;
import org.iplantc.service.common.util.OSValidator;
import org.joda.time.DateTimeZone;


public class Settings 
{
    private static final Logger log = Logger.getLogger(Settings.class);
    
    protected static final String PROPERTY_FILE = "service.properties";

	/* Debug settings */
    public static boolean       DEBUG;
    public static String        DEBUG_USERNAME;
    
    public static String        AUTH_SOURCE;
    public static boolean		VERIFY_JWT_SIGNATURE;
    
    public static String        API_VERSION;
    public static String        SERVICE_VERSION;
    public static int           JETTY_PORT;
    public static int           JETTY_AJP_PORT;
    
    public static String        PUBLIC_USER_USERNAME;
    public static String        WORLD_USER_USERNAME;
    
    
    /* Trusted user settings */
    public static List<String>  TRUSTED_USERS = new ArrayList<String>();
    
    /* Community user credentials */
    public static String        COMMUNITY_PROXY_USERNAME;
    public static String        COMMUNITY_PROXY_PASSWORD;
    
    /* Authentication service settings */
    public static String        IPLANT_LDAP_URL;
    public static String        IPLANT_LDAP_BASE_DN;
    public static String        IPLANT_LDAP_PASSWORD;
    public static String        IPLANT_LDAP_USERNAME;
    public static String        KEYSTORE_PATH;
    public static String        TRUSTSTORE_PATH;
    public static String        TACC_MYPROXY_SERVER;
    public static int           TACC_MYPROXY_PORT;
    public static String        IPLANT_AUTH_SERVICE;
    public static String        IPLANT_APP_SERVICE;
    public static String        IPLANT_JOB_SERVICE;
    public static String        IPLANT_FILE_SERVICE;
    public static String        IPLANT_LOG_SERVICE;
    public static String        IPLANT_METADATA_SERVICE;
    public static String        IPLANT_MONITOR_SERVICE;
    public static String        IPLANT_PROFILE_SERVICE;
    public static String        IPLANT_SYSTEM_SERVICE;
    public static String        IPLANT_TRANSFORM_SERVICE;
    public static String        IPLANT_TRANSFER_SERVICE;
    public static String        IPLANT_NOTIFICATION_SERVICE;
    public static String        IPLANT_POSTIT_SERVICE;
    public static String        IPLANT_TENANTS_SERVICE;
    public static String        IPLANT_ROLES_SERVICE;
    public static String        IPLANT_PERMISSIONS_SERVICE;
    public static String        IPLANT_TAGS_SERVICE;
    public static String        IPLANT_GROUPS_SERVICE;
    public static String		IPLANT_REALTIME_SERVICE;
    public static String		IPLANT_CLIENTS_SERVICE;
    public static String 		IPLANT_REACTOR_SERVICE;
    public static String 		IPLANT_ABACO_SERVICE;
    public static String 		IPLANT_REPOSITORY_SERVICE;
    
    /* iPlant dependent services */
    public static String        QUERY_URL;
    public static String        QUERY_URL_USERNAME;
    public static String        QUERY_URL_PASSWORD;
    
    /* Database Settings */
    public static String        SSH_TUNNEL_USERNAME;
    public static String        SSH_TUNNEL_PASSWORD;
    public static String        SSH_TUNNEL_HOST;
    public static int           SSH_TUNNEL_PORT;
    public static String        DB_USERNAME;
    public static String        DB_PASSWORD;
    public static String        DB_NAME;
    public static boolean       USE_SSH_TUNNEL;
    public static Integer       DEFAULT_PAGE_SIZE;
    public static Integer		MAX_PAGE_SIZE;
    public static String        IPLANT_DOCS;
    
    /* Global notifications */
    public static String                        NOTIFICATION_QUEUE;
    public static String                        NOTIFICATION_TOPIC;
    public static String                        NOTIFICATION_RETRY_QUEUE;
    public static String                        NOTIFICATION_RETRY_TOPIC;
    
    /* Messaging support */
    public static String                        MESSAGING_SERVICE_PROVIDER;
    public static String                        MESSAGING_SERVICE_HOST;
    public static int                           MESSAGING_SERVICE_PORT;
    public static String                        MESSAGING_SERVICE_USERNAME;
    public static String                        MESSAGING_SERVICE_PASSWORD;
    
    /* API specific queues */
    public static String                        FILES_ENCODING_QUEUE;
    public static String                        FILES_STAGING_QUEUE;
    public static String                        FILES_STAGING_TOPIC;
    public static String                        FILES_ENCODING_TOPIC;
    
    public static String                        TRANSFERS_ENCODING_QUEUE;
    public static String                        TRANSFERS_ENCODING_TOPIC;
    public static String                        TRANSFERS_DECODING_QUEUE;
    public static String                        TRANSFERS_DECODING_TOPIC;
    public static String                        TRANSFERS_STAGING_TOPIC;
    public static String                        TRANSFERS_STAGING_QUEUE;
    
    public static String                        TRANSFORMS_ENCODING_QUEUE;
    public static String                        TRANSFORMS_ENCODING_TOPIC;
    public static String                        TRANSFORMS_DECODING_QUEUE;
    public static String                        TRANSFORMS_DECODING_TOPIC;
    public static String                        TRANSFORMS_STAGING_TOPIC;
    public static String                        TRANSFORMS_STAGING_QUEUE;
    
    public static String                        MONITORS_CHECK_QUEUE;
    public static String                        MONITORS_CHECK_TOPIC;
    
    public static String                        APPS_PUBLISHING_QUEUE;
    public static String                        APPS_PUBLISHING_TOPIC;

    public static String                        USAGETRIGGERS_CHECK_QUEUE;
    public static String                        USAGETRIGGERS_CHECK_TOPIC;
    
    public static String                        PLATFORM_STORAGE_SYSTEM_ID;

    private static String                       DEDICATED_TENANT_ID;

    private static String[]                     DEDICATED_USER_IDS;

    private static String[]                     DEDICATED_USER_GROUPS;

    private static String[]                     DEDICATED_SYSTEM_IDS;

    private static String                       DRAIN_QUEUES;
        
    public static String						TEMP_DIRECTORY;
    
    static
    {
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
        
        Properties props = loadRuntimeProperties();
        
        DateTimeZone.setDefault(DateTimeZone.forID("America/Chicago"));
        TimeZone.setDefault(TimeZone.getTimeZone("America/Chicago"));
        
        DEBUG = Boolean.valueOf((String)props.get("iplant.debug.mode"));
        
        DEBUG_USERNAME = (String)props.get("iplant.debug.username");
        
        AUTH_SOURCE = (String) props.get("iplant.auth.source");
        
        VERIFY_JWT_SIGNATURE = Boolean.valueOf((String) props.getProperty("iplant.verify.jwt.signature", "no"));
        
        API_VERSION = (String)props.getProperty("iplant.api.version");
        
        if (!StringUtils.isEmpty(System.getProperty("jetty.port"))) {
            JETTY_PORT = Integer.valueOf(System.getProperty("jetty.port"));
        }
        if (JETTY_PORT == 0) {
            JETTY_PORT = Integer.valueOf((String)props.getProperty("jetty.port", "8182"));
        }
        
        if (!StringUtils.isEmpty(System.getProperty("jetty.ajp.port"))) {
            JETTY_AJP_PORT = Integer.valueOf(System.getProperty("jetty.ajp.port"));
        }
        if (JETTY_AJP_PORT == 0) {
            JETTY_AJP_PORT = Integer.valueOf((String)props.getProperty("jetty.ajp.port", "8183"));
        }
        
        TEMP_DIRECTORY = (String) props.getProperty("iplant.server.temp.dir", "/scratch");
        
        SERVICE_VERSION = (String)props.getProperty("iplant.service.version");
        
        IPLANT_LDAP_URL = props.getProperty("iplant.ldap.url");

        IPLANT_LDAP_BASE_DN = props.getProperty("iplant.ldap.base.dn");
        
        IPLANT_LDAP_PASSWORD = props.getProperty("iplant.ldap.password");
        
        IPLANT_LDAP_USERNAME = props.getProperty("iplant.ldap.username");
        
        PUBLIC_USER_USERNAME = (String)props.getProperty("iplant.public.user", "public");
        
        WORLD_USER_USERNAME = (String)props.getProperty("iplant.world.user", "world");
        
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
        
        TACC_MYPROXY_PORT = Integer.valueOf((String)props.getProperty("iplant.myproxy.port", "7512"));
        
        IPLANT_APP_SERVICE          = Settings.getSantizedServiceUrl(props, "iplant.app.service", "https://public.agaveapi.co/apps/v2");        
        IPLANT_AUTH_SERVICE         = Settings.getSantizedServiceUrl(props, "iplant.auth.service", "https://public.agaveapi.co/auth/v2");
        IPLANT_CLIENTS_SERVICE      = Settings.getSantizedServiceUrl(props, "iplant.clients.service", "http://public.agaveapi.co/clients/v2");
        IPLANT_SYSTEM_SERVICE       = Settings.getSantizedServiceUrl(props, "iplant.system.service", "https://public.agaveapi.co/systems/v2");
        IPLANT_DOCS                 = Settings.getSantizedServiceUrl(props, "iplant.service.documentation", "https://public.agaveapi.co/docs/v2");
        IPLANT_FILE_SERVICE         = Settings.getSantizedServiceUrl(props, "iplant.io.service", "https://public.agaveapi.co/files/v2");
        IPLANT_GROUPS_SERVICE       = Settings.getSantizedServiceUrl(props, "iplant.groups.service", "https://public.agaveapi.co/groups/v2");
        IPLANT_JOB_SERVICE          = Settings.getSantizedServiceUrl(props, "iplant.job.service", "https://public.agaveapi.co/jobs/v2");
        IPLANT_LOG_SERVICE          = Settings.getSantizedServiceUrl(props, "iplant.log.service", "https://public.agaveapi.co/logging/v2");
        IPLANT_METADATA_SERVICE     = Settings.getSantizedServiceUrl(props, "iplant.metadata.service", "https://public.agaveapi.co/meta/v2");
        IPLANT_MONITOR_SERVICE      = Settings.getSantizedServiceUrl(props, "iplant.monitor.service", "https://public.agaveapi.co/monitors/v2");
        IPLANT_NOTIFICATION_SERVICE = Settings.getSantizedServiceUrl(props, "iplant.notification.service", "https://public.agaveapi.co/notifications/v2");
        IPLANT_PERMISSIONS_SERVICE  = Settings.getSantizedServiceUrl(props, "iplant.permissions.service", "https://public.agaveapi.co/permissions/v2");
        IPLANT_POSTIT_SERVICE       = Settings.getSantizedServiceUrl(props, "iplant.postit.service", "https://public.agaveapi.co/postits/v2");
        IPLANT_PROFILE_SERVICE      = Settings.getSantizedServiceUrl(props, "iplant.profile.service", "https://public.agaveapi.co/profiles/v2");
        IPLANT_REALTIME_SERVICE     = Settings.getSantizedServiceUrl(props, "iplant.realtime.service", "http://public.realtime.prod.agaveapi.co/channels/v2");
        IPLANT_ROLES_SERVICE        = Settings.getSantizedServiceUrl(props, "iplant.roles.service", "https://public.agaveapi.co/roles/v2");
        IPLANT_TAGS_SERVICE         = Settings.getSantizedServiceUrl(props, "iplant.tags.service", "https://public.agaveapi.co/tags/v2");
        IPLANT_TENANTS_SERVICE      = Settings.getSantizedServiceUrl(props, "iplant.tenants.service", "http://agaveapi.co/tenants/");
        IPLANT_TRANSFER_SERVICE     = Settings.getSantizedServiceUrl(props, "iplant.transfer.service", "https://public.agaveapi.co/transfers/v2");
        IPLANT_REACTOR_SERVICE     	= Settings.getSantizedServiceUrl(props, "iplant.reactors.service", "https://public.agaveapi.co/reactors/v2");
        IPLANT_REPOSITORY_SERVICE   = Settings.getSantizedServiceUrl(props, "iplant.repositories.service", "https://public.agaveapi.co/repositories/v2");
        IPLANT_ABACO_SERVICE     	= Settings.getSantizedServiceUrl(props, "iplant.abaco.service", "https://public.agaveapi.co/abaco/v2");
        
        NOTIFICATION_QUEUE = (String) props.getProperty("iplant.notification.service.queue", "prod.notifications.queue");
		NOTIFICATION_TOPIC = (String) props.getProperty("iplant.notification.service.topic", "prod.notifications.queue");
		
		NOTIFICATION_RETRY_QUEUE = (String) props.getProperty("iplant.notification.service.retry.queue", "retry." + NOTIFICATION_QUEUE);
		NOTIFICATION_RETRY_TOPIC = (String) props.getProperty("iplant.notification.service.retry.topic", "retry." + NOTIFICATION_TOPIC);
		
        MESSAGING_SERVICE_PROVIDER = (String)props.get("iplant.messaging.provider");
        MESSAGING_SERVICE_USERNAME = (String)props.get("iplant.messaging.username");
        MESSAGING_SERVICE_PASSWORD = (String)props.get("iplant.messaging.password");
        MESSAGING_SERVICE_HOST = (String)props.get("iplant.messaging.host");
        MESSAGING_SERVICE_PORT = Integer.valueOf((String)props.get("iplant.messaging.port"));
        
        FILES_ENCODING_QUEUE = (String) props.getProperty("iplant.files.service.encoding.queue", "encoding.prod.files.queue");
        FILES_ENCODING_TOPIC = (String) props.getProperty("iplant.files.service.encoding.topic", "encoding.prod.files.topic");
        FILES_STAGING_QUEUE = (String) props.getProperty("iplant.files.service.staging.queue", "staging.prod.files.queue");
        FILES_STAGING_TOPIC = (String) props.getProperty("iplant.files.service.staging.topic", "staging.prod.files.topic");
        
        TRANSFERS_DECODING_QUEUE = (String) props.getProperty("iplant.transfers.service.decoding.queue", "decoding.prod.transfers.queue");
        TRANSFERS_DECODING_TOPIC = (String) props.getProperty("iplant.transfers.service.decoding.topic", "decoding.prod.transfers.topic");
        TRANSFERS_ENCODING_QUEUE = (String) props.getProperty("iplant.transfers.service.encoding.queue", "encoding.prod.transfers.queue");
        TRANSFERS_ENCODING_TOPIC = (String) props.getProperty("iplant.transfers.service.encoding.topic", "encoding.prod.transfers.topic");
        TRANSFERS_STAGING_QUEUE = (String) props.getProperty("iplant.transfers.service.staging.queue", "staging.prod.transfers.queue");
        TRANSFERS_STAGING_TOPIC = (String) props.getProperty("iplant.transfers.service.staging.topic", "staging.prod.transfers.topic");
        
        TRANSFORMS_ENCODING_QUEUE = (String) props.getProperty("iplant.transforms.service.encoding.queue", "encoding.prod.transforms.queue");
        TRANSFORMS_ENCODING_TOPIC = (String) props.getProperty("iplant.transforms.service.encoding.topic", "decoding.prod.transforms.topic");
        TRANSFORMS_DECODING_QUEUE = (String) props.getProperty("iplant.transforms.service.decoding.queue", "decoding.prod.transforms.queue");
        TRANSFORMS_DECODING_TOPIC = (String) props.getProperty("iplant.transforms.service.decoding.topic", "decoding.prod.transforms.topic");
        TRANSFORMS_STAGING_QUEUE = (String) props.getProperty("iplant.transforms.service.staging.queue", "staging.prod.transforms.queue");
        TRANSFORMS_STAGING_TOPIC = (String) props.getProperty("iplant.transforms.service.staging.topic", "staging.prod.transforms.topic");
        
        MONITORS_CHECK_QUEUE = (String) props.getProperty("iplant.monitors.service.checks.queue", "checks.prod.monitors.queue");
        MONITORS_CHECK_TOPIC = (String) props.getProperty("iplant.monitors.service.checks.topic", "checks.prod.monitors.topic");
        
        APPS_PUBLISHING_QUEUE = (String) props.getProperty("iplant.apps.service.publishing.queue", "publish.prod.apps.queue");
        APPS_PUBLISHING_TOPIC = (String) props.getProperty("iplant.apps.service.publishing.topic", "publish.prod.apps.topic");
        
        USAGETRIGGERS_CHECK_QUEUE = (String) props.getProperty("iplant.usagetriggers.service.queue", "check.prod.usagetriggers.queue");
        USAGETRIGGERS_CHECK_TOPIC = (String) props.getProperty("iplant.usagetriggers.service.topic", "check.prod.usagetriggers.topic");

        
        DEFAULT_PAGE_SIZE = NumberUtils.toInt((String) props.getProperty("iplant.default.page.size", "100"), 100);
        
        MAX_PAGE_SIZE = NumberUtils.toInt((String) props.getProperty("iplant.max.page.size", "100"), 100);
        
        PLATFORM_STORAGE_SYSTEM_ID = (String)props.getProperty("iplant.platform.storage.system", "agave-s3-prod");
        
        DRAIN_QUEUES = (String)props.getProperty("iplant.drain.all.queues");
        
        DEDICATED_TENANT_ID = (String)props.getProperty("iplant.dedicated.tenant.id");
        
        DEDICATED_USER_IDS = StringUtils.split((String)props.get("iplant.dedicated.user.id"), ",");
        
        DEDICATED_USER_GROUPS = StringUtils.split((String)props.get("iplant.dedicated.user.group.id"), ",");
        
        DEDICATED_SYSTEM_IDS = StringUtils.split((String)props.getProperty("iplant.dedicated.system.id", ""), ",");
    }
    
    /**
     * Reads in the service.properties file from disk and merges it with
     * any environment variables injected at runtime. 
     * 
     * @return {@link Properties} object containing the runtime properties
     * to be used by this app.
     */
    public static Properties loadRuntimeProperties()
    {
        Properties props = new Properties();
        try {
            props.load(Settings.class.getClassLoader().getResourceAsStream(PROPERTY_FILE));
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        
        Map<String, String> environment = System.getenv();
        for (String varName : environment.keySet())
        {
            if (StringUtils.isBlank(varName)) continue;
            
            String propName = varName.toLowerCase().replaceAll("_", ".");
            
            props.remove(propName);
            props.setProperty(propName, environment.get(varName));
        }
        
        return props;
    }
    
    /**
     * Reads in a service url from the properties file and ensures it ends in a 
     * single trailing slash.
     * @param props the properes to fetch the service url from.
     * @param propertyKey the key to lookup
     * @param defaultValue value to use if no value is present in the properties object
     * @return
     */
    public static String getSantizedServiceUrl(Properties props, String propertyKey, String defaultValue) {
    	return StringUtils.trimToEmpty(
    			(String)props.getProperty(propertyKey, defaultValue))
    				.replaceAll("/$", "") + "/";
	}

	/**
     * Reads in the tenant id every time from the service properties file.
     * This can be updated in real time to re-read the value at run time.
     * @return the Tenant.tenantCode
     */
    public static synchronized String getDedicatedTenantIdFromServiceProperties()
    {
        if (StringUtils.isEmpty(DEDICATED_TENANT_ID)) 
        {
            Properties props = new Properties();
            try
            {
                props.load(Settings.class.getClassLoader().getResourceAsStream(PROPERTY_FILE));
                
                DEDICATED_TENANT_ID = (String)props.get("iplant.dedicated.tenant.id");
            }
            catch (Exception e)
            {
                DEDICATED_TENANT_ID = null;
            }
        }
        
        return StringUtils.stripToNull(new String(DEDICATED_TENANT_ID));
    }
    
    /**
     * Reads in the dedicated username every time from the service properties file.
     * This can be updated in real time to re-read the value at run time.
     * @return an array of usernames
     */
    public static String[] getDedicatedUsernamesFromServiceProperties()
    {
        if (ArrayUtils.isEmpty(DEDICATED_USER_IDS)) 
        {
            Properties props = new Properties();
            try
            {
                props.load(Settings.class.getClassLoader().getResourceAsStream(PROPERTY_FILE));
                
                List<String> userIds = new ArrayList<String>();
                for (String userId: StringUtils.split((String)props.getProperty("iplant.dedicated.user.id", ""), ",")) {
                    userId = StringUtils.trimToNull(userId);
                    if (userId != null) {
                        userIds.add(userId);
                    }
                }
                
                DEDICATED_USER_IDS = userIds.toArray(new String[] {});
                
            }
            catch (Exception e)
            {
                DEDICATED_USER_IDS = new String[] {};
            }
        }
        
        return (String[])ArrayUtils.clone(DEDICATED_USER_IDS);
    }
    
    /**
     * Reads in the dedicated username every time from the service properties file.
     * This can be updated in real time to re-read the value at run time.
     * @return an array of user group names
     */
    public static String[] getDedicatedUserGroupsFromServiceProperties()
    {
        if (ArrayUtils.isEmpty(DEDICATED_USER_GROUPS)) 
        {
            Properties props = new Properties();
            try
            {
                props.load(Settings.class.getClassLoader().getResourceAsStream(PROPERTY_FILE));
                
                List<String> groupIds = new ArrayList<String>();
                for (String groupId: StringUtils.split((String)props.getProperty("iplant.dedicated.user.group", ""), ",")) {
                    groupId = StringUtils.trimToNull(groupId);
                    if (groupId != null) {
                        groupIds.add(groupId);
                    }
                }
                
                DEDICATED_USER_GROUPS = groupIds.toArray(new String[] {});
            }
            catch (Exception e)
            {
                DEDICATED_USER_GROUPS = new String[] {};
            }
        }
        
        return (String[])ArrayUtils.clone(DEDICATED_USER_GROUPS);
    }
    
    /**
     * Reads in the system ids every time from the service properties file.
     * This can be updated in real time to re-read the value at run time.
     * Multiple systems may be specified as a comma-delimited list.
     * If further isolation at the BatchQueue level may be obtained by 
     * providing the queue name in {@code systemId#queueName} format.
     *  
     * @return an array of system ids
     */
    public static String[] getDedicatedSystemIdsFromServiceProperties()
    {
        if (ArrayUtils.isEmpty(DEDICATED_SYSTEM_IDS)) 
        {
            Properties props = new Properties();
            try
            {
                props.load(Settings.class.getClassLoader().getResourceAsStream(PROPERTY_FILE));
                
                List<String> systemIds = new ArrayList<String>();
                for (String systemId: StringUtils.split((String)props.getProperty("iplant.dedicated.system.id", ""), ",")) {
                    systemId = StringUtils.trimToNull(systemId);
                    if (systemId != null) {
                        systemIds.add(systemId);
                    }
                }
                
                DEDICATED_SYSTEM_IDS = systemIds.toArray(new String[] {});
            }
            catch (Exception e)
            {
                DEDICATED_SYSTEM_IDS = new String[] {};
            }
        }
    
        return (String[])ArrayUtils.clone(DEDICATED_SYSTEM_IDS);
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
        if (StringUtils.isEmpty(DRAIN_QUEUES)) 
        {
            Properties props = new Properties();
            try
            {
                props.load(Settings.class.getClassLoader().getResourceAsStream(PROPERTY_FILE));
                
                DRAIN_QUEUES = (String)props.getProperty("iplant.drain.all.queues");
            }
            catch (Exception e)
            {
                DRAIN_QUEUES = null;
            }
        }
        
        return Boolean.parseBoolean(DRAIN_QUEUES);
    }
    
    /**
     * Returns the local hostname by resolving the HOSTNAME environment variable.
     * If that variable is not available, the {@link InetAddress} class is used
     * to resolve it from the system.
     *  
     * @return hostname of current running process
     */
    public static String getLocalHostname()
    {
        String hostname = System.getenv("HOSTNAME");
        
        if (StringUtils.isBlank(hostname)) 
        {
            try {
                hostname = InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException e1) {
                log.error("Unable to resolve local hostname");
                hostname = "localhost";
            }
        }
        
        return hostname;
    }

    /**
     * Returns the ip address of the container/host. 127.0.0.1 if the ip cannot
     * be determined.
     * 
     * @return host ip or 127.0.0.1
     */
    public static String getIpLocalAddress() 
    {
        IPAddressValidator ipAddressValidator = null;
        try {
            ipAddressValidator = new IPAddressValidator();
            
            // try to resolve via service discovery first
            String hostname = System.getenv("TUTUM_CONTAINER_FQDN");
            if (StringUtils.isNotEmpty(hostname)) 
            {
                InetAddress ip = null;
                try {
                    ip = InetAddress.getByName(hostname);
                    if (!ip.isLoopbackAddress() && !ip.isAnyLocalAddress()) {
                        return ip.getHostAddress();
                    }
                } catch (Exception e) {}
            } 
            
            if (OSValidator.isMac() || OSValidator.isUnix()) 
            {
                // check for the hostname variable and try to resolve that
                hostname = System.getenv("HOSTNAME");
                if (StringUtils.isNotEmpty(hostname)) {
                    InetAddress ip = null;
                    try {
                        ip = InetAddress.getByName(hostname);
                        if (!ip.isLoopbackAddress() && !ip.isAnyLocalAddress()) {
                            return ip.getHostAddress();
                        }
                    } catch (Exception e) {}
                }
                
                // ip could not be found from the environment, check ifconfig
                List<String> ipAddresses = Settings.getIpAddressesFromNetInterface();
                if (!ipAddresses.isEmpty()) {
                    if (ipAddresses.contains("192.168.59.3")) {
                        return "192.168.59.3";
                    }
                    return ipAddresses.get(0);
                } 
                
                // ifconfig failed. try netstat
                ipAddresses = Settings.fork("netstat -rn | grep 'default' | awk '{print $2}' | head -n 1");
                if (!ipAddresses.isEmpty()) {
                    for(String ip: ipAddresses) {
                        if (ipAddressValidator.validate(StringUtils.trimToEmpty(ip))) {
                            return ip;
                        }
                    }
                }
            }
            
            return "127.0.0.1";
        }
        catch (Exception e) {
            log.error("Failed to retrieve local ip address. Some processes may not operate correctly.", e);
            return "127.0.0.1";
        }
    }
    
    /**
     * Helper to fork commands and return response as a newline
     * delimited list.
     * 
     * @param command
     * @return response broken up by newline characters.
     */
    public static List<String> fork(String command) 
    {
        List<String> lines = new ArrayList<String>();
        BufferedReader reader = null;
        String[] cmd = { "sh", "-c", command };
        
        try {
            Process p = Runtime.getRuntime().exec(cmd);
            p.waitFor();
             
            reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
         
            String line = "";
            while ((line = reader.readLine())!= null) {
                lines.add(line);
            }
        }
        catch (IOException | InterruptedException e) {
            log.error("Failed to fork command " + command);
        }
        finally {
            try {reader.close();} catch (Exception e) {}
        }
        
        return lines;
    }
    
    /**
     * Parses local network interface and returns a list of hostnames
     * for 
     * @return list of ip addresses for the host machine
     */
    public static List<String> getIpAddressesFromNetInterface() 
    {
        List<String> ipAddresses = new ArrayList<String>();
        IPAddressValidator ipAddressValidator = new IPAddressValidator();
        try 
        {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) 
            {
                NetworkInterface ni = networkInterfaces.nextElement();
                if (ni.isUp() && !ni.isLoopback() && !ni.isPointToPoint() && !ni.isVirtual()) 
                {
                    Enumeration<InetAddress> inetAddresses = ni.getInetAddresses();
                    while (inetAddresses.hasMoreElements()) 
                    {
                        InetAddress add = inetAddresses.nextElement();
                        String ip = add.getHostAddress();
                        if (ipAddressValidator.validate(ip) 
                                && !StringUtils.startsWithAny(ip, new String[]{"127", ":", "0"})) 
                        {
                            ipAddresses.add(ip);
                        }
                    }
                }
            }
        } catch (SocketException e) {
            log.error("Unable to resolve local ip address from network interface. Passive connections may fail.");
        }
        
        return ipAddresses;
    }
    
    /**
     * Returns the container id from the local environment. If this cannot
     * be found (eg. running standard docker) then null is returned.
     * @return the Docker container id or null if it cannot be found.
     */
    public static String getContainerId() {
        
        String tutumNodeApiUri = System.getenv("TUTUM_CONTAINER_API_URI");
        // look for container id in tutum environment
        if (StringUtils.isNotEmpty(tutumNodeApiUri)) {
            // container id is last token in uri
            // ex. /api/v1/container/f17015ce-d907-48d7-87e8-0521aa2c67e6/
            tutumNodeApiUri = StringUtils.removeEnd(tutumNodeApiUri, "/");
            return StringUtils.substringAfterLast(tutumNodeApiUri, "/");
        }
        // look for container id in docker environment...probably not there
        else 
        {
            return null;
        }
    }
    
    public static List<String> getEditableRuntimeConfigurationFields() {
        return Arrays.asList("DRAIN_QUEUES",
                "DEDICATED_TENANT_ID",
                "DEDICATED_SYSTEM_IDS",
                "DEDICATED_USER_IDS",
                "DEDICATED_USER_GROUPS");
    }
}
