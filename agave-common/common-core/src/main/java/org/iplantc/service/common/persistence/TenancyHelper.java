package org.iplantc.service.common.persistence;

import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.common.Settings;
import org.iplantc.service.common.auth.JWTClient;
import org.iplantc.service.common.dao.TenantDao;
import org.iplantc.service.common.exceptions.TenantException;
import org.iplantc.service.common.model.Tenant;

/**
 * Class to handle retriving tenant info statically. This currently relies
 * on a ThreadLocal within JWTClient that is populated during authentication.
 * 
 * @author dooley
 *
 */
public class TenancyHelper 
{
	private static final Logger log = Logger.getLogger(TenancyHelper.class);
	public static String getCurrentTenantId() {
		
		String tenantId = JWTClient.getCurrentTenant();
		if (StringUtils.isEmpty(tenantId)) {
			tenantId = "iplantc.org";
		}
		
		return tenantId;
	}
	
	public static void setCurrentTenantId(String tenantId) {
		JWTClient.setCurrentTenant(tenantId);
	}
	
	public static String getCurrentApplicationId() {
		return JWTClient.getCurrentApplicationId();
	}
	
	public static String getCurrentEndUser() {
		return JWTClient.getCurrentEndUser();
	}
	public static void setCurrentEndUser(String username) {
		JWTClient.setCurrentEndUser(username);
	}
	
	public static String getCurrentSubscriber() {
		return JWTClient.getCurrentSubscriber();
	}
	
	/**
	 * Checks to see that the current authenticated user is a tenant admin. This 
     * does <b>NOT</b> take into consideration the local admin configuration file.
	 * 
	 * @return true if the current user has the required roles, false otherwise.
	 */
	public static boolean isTenantAdmin() {
		return JWTClient.isTenantAdmin();
	}
	
	/**
     * Checks to see that the current authenticated user is a tenant admin. This 
     * does <b>NOT</b> take into consideration the local admin configuration file.
     * @param username the username to check
     * @return true if the {@code username} has the required roles, false otherwise.
     */
    public static boolean isTenantAdmin(String username) {
        return StringUtils.equals(JWTClient.getCurrentEndUser(), username) 
                && JWTClient.isTenantAdmin();
    }
	
    /**
     * Checks to see that the current authenticated user is a super admin. This 
     * does <b>NOT</b> take into consideration the local admin configuration file.
     * 
     * @return true if the current user has the required roles, false otherwise.
     */
	public static boolean isSuperAdmin() {
		return JWTClient.isSuperAdmin();
	}
	
	/**
     * Checks to see that the current authenticated user is a super admin. This 
     * does <b>NOT</b> take into consideration the local admin configuration file.
     * 
     * @param username the username to check
     * @return true if the {@code username} has the required roles, false otherwise.
     */
    public static boolean isSuperAdmin(String username) {
        return StringUtils.equals(JWTClient.getCurrentEndUser(), username) 
                && JWTClient.isSuperAdmin();
    }
	
	public static String getSuperTenantId() {
		String tenantId = "carbon.super";
		return tenantId;
	}
	
	/**
	 * Returns the bearer token used to authenticate the call. This could
	 * be empty if the current thread was not created through an API call.
	 * 
	 * @return a valid bearer token or null if this is an offline thread.
	 */
	public static String getCurrentBearerToken()
    {
        return JWTClient.getCurrentBearerToken();
    }
	
	/**
	 * Reads the dedicated tenant id setting from the local 
	 * service config file and verifies it is valid. 
	 * 
	 * @return the tenant id if valid, null otherwise.
	 */
	public static String getDedicatedTenantIdForThisService()
	{
		TenantDao dao = new TenantDao();
		try {
			String dedicatedTenantId = Settings.getDedicatedTenantIdFromServiceProperties();
			if (StringUtils.isNotEmpty(dedicatedTenantId) && 
			        dao.findByTenantId(StringUtils.stripStart(dedicatedTenantId, "!")) != null) {
				return dedicatedTenantId;
			} else {
				return null;
			}
		} catch (TenantException e) {
			log.error("Invalid tenant id specified in service.properties. It will be ignored");
			return null;
		} catch (Exception e) {
			return null;
		}
	}
	
	/**
	 * Adjusts the default API resource URLs to tenant-specific URLs given
	 * in the tenants table in the db. This should be called for every
	 * HAL url returned from the API. Say 
	 * Settings.IPLANT_APPS_SERVICE = https://agave.iplantc.org/apps/v2
	 * was the default url from the service settings. If there was a tenant 
	 * called xsede.org in the db with tenantId = xsede.org,
	 * then after calling this method, the returned url would be 
	 * https://api.xsede.org/apps/2.0
	 * 
	 * @param url default url to resolve
	 * @return String with the current hostname replaced by the tenant hostname or null of it did not resolve.
	 */
	public static String resolveURLToCurrentTenant(String url)
	{
		TenantDao dao = new TenantDao();
		Tenant tenant;
		try 
		{
			tenant = dao.findByTenantId(TenancyHelper.getCurrentTenantId());
			
			if (tenant == null) 
			{
				// returning the url if no tenant is found is probably a 
				// tenancy violation if this is called in situations where
				// the tenant is not known.
				return url;
			}
			else
			{
				String resolvedUrl = StringUtils.substringAfter(url, "://");
				boolean realtimeUrl = StringUtils.startsWith(resolvedUrl, "realtime.");
				resolvedUrl = StringUtils.substringAfter(resolvedUrl, "/");
				if (realtimeUrl) {
					resolvedUrl = StringUtils.replace(tenant.getBaseUrl(), "://", "://realtime."); 
				} else {
					resolvedUrl = tenant.getBaseUrl() + resolvedUrl;
				}
				//resolvedUrl = StringUtils.substringBefore(url, "://") + "://" + resolvedUrl;
				return resolvedUrl;
			}
		} catch (TenantException e) {
			log.error("Failed to resolve current tenant " + TenancyHelper.getCurrentTenantId() + " from db", e);
			return url;
		}
		
			
	}
	
	/**
	 * Adjusts the default API resource URLs to tenant-specific URLs given
	 * in the tenants table in the db. This should be called when there
	 * is no tenant associated with a session. ie. by worker processes.
	 * 
	 * @param url default url to resolve
	 * @param tenantId the id of the tenant to resolve.
	 * @return string tenant-specific url for the given resource
	 */
	public static String resolveURLToCurrentTenant(String url, String tenantId)
	{
		if (StringUtils.isEmpty(tenantId)) {
			return TenancyHelper.resolveURLToCurrentTenant(url);
		}
		
		TenantDao dao = new TenantDao();
		Tenant tenant;
		try 
		{
			tenant = dao.findByTenantId(tenantId);
			
			if (tenant == null) 
			{
				return url;
			}
			else
			{
				String resolvedUrl = StringUtils.substringAfter(url, "://");
				resolvedUrl = StringUtils.substringAfter(resolvedUrl, "/");
				resolvedUrl = tenant.getBaseUrl() + resolvedUrl;
				
				return resolvedUrl;
			}
		} catch (TenantException e) {
			log.error("Failed to resolve current tenant " + tenantId + " from db", e);
			return url;
		}
		
			
	}
}
