/**
 * 
 */
package org.iplantc.service.common.uri;

import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.iplantc.service.common.Settings;
import org.iplantc.service.common.persistence.TenancyHelper;

/**
 * Holds regular expression stubs for Agave URI. These values should be
 * used to resolve general URI paths into tenant-specific URI paths.
 * 
 * @author dooley
 *
 */
public enum AgaveUriRegex {

    /**
     * Represents all standard paths which imply relation to the user
     * default {@link StorageSystem}. 
     */
    RELATIVE_PATH("([^\\?]*)([\\?]+.*)?"),
    
    /**
     * Represents all custom Agave data URI. Hostname can be a 
     * {@link RemoteSystem#systeId}
     */
    AGAVE_URI("(?:(?i)agave(?-i)\\://)([0-9a-zA-Z\\.\\-]{0,64})/([^\\?]*)([\\?]+.*)?"),
    
    /**
     * Represents all urls to a {@link Job} output folder. This can change over 
     * time, so using these convenience URL is helpful to prevent breaking links
     * and to enable late-binding of values.
     */
    JOBS_URI("([0-9a-f]+-[0-9a-f]+-[0-9]+-007)/outputs/(?:media|listings|pems|index|history)/([^\\?]*)([\\?]+.*)?"),
    
    /**
     * Represents a url to the Files API with the {@link RemoteSystem#systeId} 
     * explicitly included in the path.
     */
    FILES_URI_CUSTOM_SYSTEM("(?:media|listings|pems|index|history)/system/([0-9a-zA-Z\\.\\-]{3,64})/([^\\?]*)([\\?]+.*)?"),
    
    /**
     * Represents a url to the Files API without the {@link RemoteSystem#systemId} 
     * included in the path. These links imply use of the user's default {@link StorageSystem}.
     */
    FILES_URI_DEFAULT_SYSTEM("(?:media|listings|pems|index|history)/([^\\?]*)([\\?]+.*)?"),
    
    /**
     * Represents a url to the Metadata API
     */
    METADATA_URI("(?:data)/([0-9a-f]+-[0-9a-f]+-[0-9]+-0122)"),
    
    /**
     * Represents a url to the Metadata Schema API
     */
    METADATA_SCHEMA_URI("(?:schemas)/([0-9a-f]+-[0-9a-f]+-[0-9]+-013)(?:#.*)?");
    
    
    private String regexStub = null;
    
    private AgaveUriRegex(String regexStub) {
        this.setRegexStub(regexStub);
    }
    
    /**
     * Checks to see if the given URI matches this {@link AgaveUriRegex} value
     * in the context of the current tenant.
     * 
     * @see {@link #matches(String, String)} to match against a named tenant
     * @param uri the url to check 
     * @return true if the {@code uri} matches any known regex, false otherwise
     */
    public boolean matches(URI uri) {
        return matches(uri, null);
    }
    
    /**
     * Checks to see if the given URI matches this {@link AgaveUriRegex} value
     * in the context of the current tenant.
     * 
     * @param suri the url to check
     * @param tenantId the tenant to check against. Defaults to current tenant if null 
     * @return true if the {@code uri} matches any known regex, false otherwise
     */
    public boolean matches(URI uri, String tenantId) {
    
        if (uri == null) return false;
        
        if (StringUtils.isEmpty(tenantId)) {
            tenantId = TenancyHelper.getCurrentTenantId();
        }
        
        // nothing to resolve here
        String suri = uri.toString();
        if (this == AGAVE_URI) 
        {
            return suri.matches(this.getRegexStub());
        }
        // resolve job api
        else if (this == JOBS_URI) 
        {
            String baseUrl = TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_JOB_SERVICE, tenantId);
            return (StringUtils.isNotEmpty(baseUrl) && suri.matches("(?:" + baseUrl.replaceAll("\\:", "\\\\:") + ")" + this.getRegexStub()));
        }
        // resolve files api and check for system info
        else if (this == FILES_URI_CUSTOM_SYSTEM) {
            String baseUrl = TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_FILE_SERVICE, tenantId);
            // should be a custom system url. check for valid system pattern
            return (StringUtils.isNotEmpty(baseUrl) && suri.matches("(?:" + baseUrl.replaceAll("\\:", "\\\\:") + ")media/system(.*)")
                    && suri.matches(baseUrl + this.getRegexStub()));
        }
        // resolve files api against default system 
        else if (this == FILES_URI_DEFAULT_SYSTEM) {
            String baseUrl = TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_FILE_SERVICE, tenantId);
            return (StringUtils.isNotEmpty(baseUrl) && suri.matches("(?:" + baseUrl.replaceAll("\\:", "\\\\:") + ")" + this.getRegexStub()));
        }
        // perhaps it is a file system path
        else if (StringUtils.isEmpty(uri.getScheme()) && StringUtils.isEmpty(uri.getHost())) 
        {
            return suri.matches(RELATIVE_PATH.getRegexStub());
        }
        
        return false;
    }
    
    /**
     * Checks to see if the given URI matches any {@link AgaveUriRegex} value
     * in the context of the current tenant.
     * 
     * This falls back to the individual {@link #matches(String, String)} methods.
     * 
     * @param uri the uri to check 
     * @return true if the {@code uri} matches any known regex, false otherwise
     */
    public static boolean matchesAny(URI uri) {
        return AgaveUriRegex.getMatcher(uri, null) != null;
    }
    
    /**
     * Checks to see if the given URI matches any {@link AgaveUriRegex} value
     * in the context of the current tenant.
     * 
     * This falls back to the individual {@link #matches(String, String)} methods.
     * 
     * @param uri the uri to check 
     * @param tenantId the tenant to check against. Defaults to current tenant if null
     * @return true if the {@code uri} matches any known regex, false otherwise
     */
    public static boolean matchesAny(URI uri, String tenantId) {
        return AgaveUriRegex.getMatcher(uri, tenantId) != null;
    }
    
    /**
     * Returns a {@link java.util.regex.Matcher} for the given
     * {@code uri}. This will contain the cleaned up uuid/systemId 
     * and path represented by the {@code uri}.
     * <code>Matcher matcher = AgaveUriRegex.getMatcher(uri);
     * matcher.get(0); // returns original uri
     * matcher.get(1); // uuid or systemid
     * matcher.get(2); // path minus url query 
     * matcher.get(3); // url query path or null if none provided
     * </code>
     * @param uri the uri to check 
     * @return Matcher containing the parsed uri. 
     */
    public static Matcher getMatcher(URI uri) {
        return AgaveUriRegex.getMatcher(uri, null);
    }
    
    /**
     * Returns a {@link java.util.regex.Matcher} for the given
     * {@code uri}. This will contain the cleaned up uuid/systemId 
     * and path represented by the {@code uri}. Job and custom file
     * URI have the following structure in their {@link Matcher}.
     * <pre><code>
     * Matcher matcher = AgaveUriRegex.getMatcher(uri);
     * matcher.get(0); // returns original uri
     * matcher.get(1); // uuid or systemid
     * matcher.get(2); // path minus url query 
     * matcher.get(3); // url query path or null if none provided
     * </code></pre>
     * Default {@link StorageSystem} and standard path URI have the
     * following, reduced structure in their {@link Matcher}.
     * <pre><code>
     * Matcher matcher = AgaveUriRegex.getMatcher(uri);
     * matcher.get(0); // path minus url query 
     * matcher.get(1); // url query path or null if none provided
     * </code></pre>
     * @param uri the uri to check 
     * @param tenantId the tenant to check against. Defaults to current tenant if null
     * @return Matcher containing the parsed uri. 
     */
    public static Matcher getMatcher(URI uri, String tenantId) {
        if (uri == null) return null;
        
        if (StringUtils.isEmpty(tenantId)) {
            tenantId = TenancyHelper.getCurrentTenantId();
        }
        
        String baseJobUrl = TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_JOB_SERVICE, tenantId);
        if (StringUtils.isNotEmpty(baseJobUrl)) baseJobUrl = "(?:" + baseJobUrl.replaceAll("\\:", "\\\\:") + ")";
        String baseFileUrl = TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_FILE_SERVICE, tenantId);
        if (StringUtils.isNotEmpty(baseFileUrl)) baseFileUrl = "(?:" + baseFileUrl.replaceAll("\\:", "\\\\:") + ")";
        
        Matcher matcher = null;
        
        // nothing to resolve here
        String suri = uri.toString();
        if (suri.matches(AGAVE_URI.getRegexStub())) {
            matcher = Pattern.compile(AGAVE_URI.getRegexStub(), Pattern.CASE_INSENSITIVE).matcher(suri);
        } 
        // resolve job api
        else if (StringUtils.isNotEmpty(baseJobUrl) && suri.matches(baseJobUrl + JOBS_URI.getRegexStub())) 
        {
            matcher = Pattern.compile(baseJobUrl + JOBS_URI.getRegexStub(), Pattern.CASE_INSENSITIVE).matcher(suri);
        }
        // resolve files api and check for system info
        else if (StringUtils.isNotEmpty(baseFileUrl) && suri.matches(baseFileUrl + "media/system/(.*)")) {
            
            // now run against this regex stub to validate teh lenght of the sytem id and trailing slash
            if (suri.matches(baseFileUrl + FILES_URI_CUSTOM_SYSTEM.getRegexStub())) {
                matcher = Pattern.compile(baseFileUrl + FILES_URI_CUSTOM_SYSTEM.getRegexStub(), Pattern.CASE_INSENSITIVE).matcher(suri);
            }
            
        // resolve files api against default system 
        } 
        else if (StringUtils.isNotEmpty(baseFileUrl) && suri.matches(baseFileUrl + FILES_URI_DEFAULT_SYSTEM.getRegexStub())) 
        {
            matcher = Pattern.compile(baseFileUrl + FILES_URI_DEFAULT_SYSTEM.getRegexStub(), Pattern.CASE_INSENSITIVE).matcher(suri);
        }
        // perhaps it is a file system path
        else if (StringUtils.isEmpty(uri.getScheme()) && StringUtils.isEmpty(uri.getHost())) 
        {
            matcher = Pattern.compile(RELATIVE_PATH.getRegexStub(), Pattern.CASE_INSENSITIVE).matcher(suri);
        }
        
        // initialize the matcher if it was found
        if (matcher != null) {
            matcher.matches();
            return matcher;
        } 
        // no valid matches, return null
        else {
            return null;
        }
    }

	/**
	 * @return the regexStub
	 */
	public String getRegexStub() {
		return regexStub;
	}

	/**
	 * @param regexStub the regexStub to set
	 */
	private void setRegexStub(String regexStub) {
		this.regexStub = regexStub;
	}
    
}
