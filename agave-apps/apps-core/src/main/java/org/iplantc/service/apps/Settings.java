package org.iplantc.service.apps;

import java.util.Properties;

/**
 * @author dooley
 * 
 */
public class Settings {

	private static Properties					props = new Properties();

	public static String						AUTH_SOURCE;
	
	public static String						API_VERSION;
	public static String						SERVICE_VERSION;
	
	/* Data service settings */
	public static String						TEMP_DIRECTORY;

	/* Iplant API service endpoints */
	public static String 						IPLANT_APPS_SERVICE;
	public static String						IPLANT_JOB_SERVICE;
	public static String						IPLANT_PROFILE_SERVICE;
	public static String						IPLANT_METADATA_SERVICE;
	public static String						IPLANT_SYSTEM_SERVICE;
	
	public static String						IRODS_USERNAME;
	public static String						IRODS_PASSWORD;
	public static String						WORLD_USER_USERNAME;
	
    public static Integer 						DEFAULT_PAGE_SIZE;

	public static String 						PUBLIC_APPS_DEFAULT_DIRECTORY;
    
	static 
	{
		props = org.iplantc.service.common.Settings.loadRuntimeProperties();
		
		AUTH_SOURCE = props.getProperty("iplant.auth.source", "none").trim();

		API_VERSION = (String)props.getProperty("iplant.api.version");
		
		SERVICE_VERSION = (String)props.getProperty("iplant.service.version");
		
		TEMP_DIRECTORY = (String) props.get("iplant.server.temp.dir");
		
		IPLANT_APPS_SERVICE = (String) props.get("iplant.app.service");
		if (!IPLANT_APPS_SERVICE.endsWith("/")) IPLANT_APPS_SERVICE += "/";
		
		IPLANT_JOB_SERVICE = (String) props.get("iplant.job.service");
		if (!IPLANT_JOB_SERVICE.endsWith("/")) IPLANT_JOB_SERVICE += "/";

		IPLANT_PROFILE_SERVICE = (String) props.get("iplant.profile.service");
		if (!IPLANT_PROFILE_SERVICE.endsWith("/")) IPLANT_PROFILE_SERVICE += "/";

		IPLANT_SYSTEM_SERVICE = (String) props.get("iplant.system.service");
		if (!IPLANT_SYSTEM_SERVICE.endsWith("/")) IPLANT_SYSTEM_SERVICE += "/";
		
		IPLANT_METADATA_SERVICE = (String) props.get("iplant.metadata.service");
		if (!IPLANT_METADATA_SERVICE.endsWith("/")) IPLANT_METADATA_SERVICE += "/";
		
		IRODS_USERNAME = (String) props.get("iplant.irods.username");

		IRODS_PASSWORD = (String) props.get("iplant.irods.password");

		WORLD_USER_USERNAME = (String) props.get("iplant.world.user");
		
		PUBLIC_APPS_DEFAULT_DIRECTORY = (String) props.getProperty("iplant.default.apps.dir", "/api/" + API_VERSION + "/apps/");
		if (!PUBLIC_APPS_DEFAULT_DIRECTORY.endsWith("/")) PUBLIC_APPS_DEFAULT_DIRECTORY += "/";

        DEFAULT_PAGE_SIZE = Integer.parseInt((String) props.getProperty("iplant.default.page.size", "25"));
	}
}
