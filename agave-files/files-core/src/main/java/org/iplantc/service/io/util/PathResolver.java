/**
 * 
 */
package org.iplantc.service.io.util;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.tools.ant.util.FileUtils;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.systems.manager.SystemManager;
import org.iplantc.service.systems.model.RemoteSystem;
import org.iplantc.service.systems.model.StorageSystem;
import org.iplantc.service.transfer.RemoteDataClient;

/**
 * Utility class to determine permissions on remote files.
 * 
 * @author dooley
 *
 */
public class PathResolver {
    
    public enum FilePathRegex {
        
        /**
         * Represents a url to the Files API with the {@link RemoteSystem#systeId} 
         * explicitly included in the path.
         */
        FILES_PUBLIC_URI("(?:files/)?(?:download)/([0-9a-zA-Z_\\-]{3,64})/system/([0-9a-zA-Z\\.\\-]{3,64})/(.*)"),
        
        /**
         * Represents a url to the Files API with the {@link RemoteSystem#systeId} 
         * explicitly included in the path.
         */
        FILES_URI_CUSTOM_SYSTEM("(?:files/)?(?:listings|media|pems|meta|history)/system/([0-9a-zA-Z\\.\\-]{3,64})/(.*)"),
        
        /**
         * Represents a url to the Files API without the {@link RemoteSystem#systeId} 
         * included in the path. These links imply use of the user's default {@link StorageSystem}.
         */
        FILES_URI_DEFAULT_SYSTEM("(?:files/)?(?:listings|media|pems|meta|history)/(.*)");
        
        private String regexStub;
        
        private FilePathRegex(String regexStub) {
            this.regexStub = regexStub;
        }
        
        /**
         * Checks to see if the given URI matches this {@link FilePathRegex} value
         * in the context of the current tenant.
         * 
         * @see {@link #matches(String, String)} to match against a named tenant
         * @param path the current request path to check 
         * @return true if the {@code uri} matches any known regex, false otherwise
         */
        public boolean matches(String path) {
            return matches(path, null);
        }
        
        /**
         * Checks to see if the given URI matches this {@link FilePathRegex} value
         * in the context of the current tenant.
         * 
         * @param uri the url to check
         * @param tenantId the tenant to check against. Defaults to current tenant if null 
         * @return true if the {@code uri} matches any known regex, false otherwise
         */
        public boolean matches(String path, String tenantId) {
        
            if (StringUtils.isEmpty(path)) {
                return true;
            } else if (this == FILES_URI_CUSTOM_SYSTEM) {
                return (path.matches("(?:listings|media|pems|meta|history|index)/system/(.*)") && path.matches(this.regexStub));
            } else {
                return path.matches(this.regexStub);
            }
        }
        
        /**
         * Returns a {@link java.util.regex.Matcher} for the given
         * {@code uri}. This will contain the cleaned up systemId 
         * and path represented by the {@code path}.
         * <code>Matcher matcher = PathResolver.FilePathRegex.getMatcher(path);
         * matcher.get(0); // returns original path
         * matcher.get(1); // uuid or systemid
         * matcher.get(2); // path minus url query 
         * matcher.get(3); // url query path or null if none provided
         * </code>
         * @param path the path to check 
         * @return Matcher containing the parsed path. 
         */
        public static Matcher getMatcher(String path, String username) {
            return getMatcher(path, username, null);
        }
        
        /**
         * Returns a {@link java.util.regex.Matcher} for the given
         * {@code uri}. This will contain the cleaned up uuid/systemId 
         * and path represented by the {@code uri}. Job and custom file
         * URI have the following structure in their {@link Matcher}.
         * <pre><code>
         * Matcher matcher = PathResolver.FilePathRegex.getMatcher(uri);
         * matcher.get(0); // returns original uri
         * matcher.get(1); // uuid or systemid
         * matcher.get(2); // path minus url query 
         * matcher.get(3); // url query path or null if none provided
         * </code></pre>
         * Default {@link StorageSystem} and standard path URI have the
         * following, reduced structure in their {@link Matcher}.
         * <pre><code>
         * Matcher matcher = PathResolver.FilePathRegex.getMatcher(path);
         * matcher.get(0); // path minus url query 
         * matcher.get(1); // url query path or null if none provided
         * </code></pre>
         * @param uri the uri to check 
         * @param tenantId the tenant to check against. Defaults to current tenant if null
         * @return Matcher containing the parsed uri. 
         */
        public static Matcher getMatcher(String path, String username, String tenantId) {
            
            if (StringUtils.isEmpty(path)) {
                return null;
            }
            
            if (StringUtils.isEmpty(tenantId)) {
                tenantId = TenancyHelper.getCurrentTenantId();
            }
            
            Matcher matcher = null;
            
            if (path.matches(FILES_PUBLIC_URI.regexStub)) 
            {
                matcher = Pattern.compile(FILES_PUBLIC_URI.regexStub, Pattern.CASE_INSENSITIVE).matcher(path);
                matcher.matches();
            }
            // check for system info
            else if (path.matches("(?:listings|media|pems|meta|history)/system/(.*)")) {
                
                // now run against this regex stub to validate teh lenght of the sytem id and trailing slash
                if (path.matches(FILES_URI_CUSTOM_SYSTEM.regexStub)) {
                    matcher = Pattern.compile(FILES_URI_CUSTOM_SYSTEM.regexStub, Pattern.CASE_INSENSITIVE).matcher(path);
                }
            }
            // resolve files api against default system
            else if (path.matches(FILES_URI_DEFAULT_SYSTEM.regexStub)) 
            {
                matcher = Pattern.compile(FILES_URI_DEFAULT_SYSTEM.regexStub, Pattern.CASE_INSENSITIVE).matcher(path);
                matcher.matches();
                
                // rebuild the path with the user default storage system in it so matchers all resolve with equal parts
                String canonicalPath = "/files/media/system/";
                SystemManager manager = new SystemManager();
                RemoteSystem defaultStorageSystem = manager.getUserDefaultStorageSystem(username);
                if (defaultStorageSystem != null) {
                    canonicalPath += defaultStorageSystem.getSystemId();
                }
                
                // add the path and optional url query
                canonicalPath += "/" + matcher.group(1) + (matcher.group(2) == null ? "" : matcher.group(2));
                
                // now parse the resolved path and return it
                matcher = Pattern.compile(FILES_URI_DEFAULT_SYSTEM.regexStub, Pattern.CASE_INSENSITIVE).matcher(canonicalPath);
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
    }
    
	/**
	 * Extracts the virtual path on a remote file system from the original
	 * URL request path made to the Files API. 
	 * 
	 * @param owner
	 * @param path
	 * @return resolved path
	 * @throws IOException 
	 * @throws URISyntaxException 
	 */
	public static String resolve(String owner, String originalPath) throws IOException 
	{	
//	    Matcher matcher = FilePathRegex.getMatcher(originalPath, owner);
//	    
//	    if (matcher == null) {
//	        throw new IOException("Unrecognized URL structure for path " + originalPath);
//	    }
//	    
//	    // get the path component of the original request URL path. 
//	    String path = null;
//	    // if public path,the path is in group 3
//	    if (matcher.groupCount() == 5) {
//	        path = matcher.group(3);
//	    } 
//	    // in all other paths, the path is in group 2
//	    else {
//	        path = matcher.group(2);
//	    }
//	    
//	    // Convert Windows-format paths to sensible paths
//        Pattern regexDriveLetter = Pattern.compile("^[a-zA-Z]:");
//        Pattern regexSlash = Pattern.compile("\\\\");
//
//        path = regexDriveLetter.matcher(path).replaceAll("");
//        path = regexSlash.matcher(path).replaceAll("/");
//
//        return path; 
		
	    
		// this will drop the webapp name which probably won't match the IO service route
		String path = originalPath;
		//path = path.substring(path.indexOf("files", 1));
		String[] endpoints = { "listings", "media", "pems", "meta", "history", "index" };
		
		String[] segments = path.substring(1).split("/", -1);//FileUtils.getPathStack(path);
		
		if (segments.length > 3 && segments[0].equals("files") && 
				ArrayUtils.contains(endpoints, segments[1]) &&
				segments[2].equals("system")) 
		{	
			// they specified the system id as the next token. drop it.
			if (segments.length == 4) 
			{
				path = "";
			} 
			else 
			{
				List<String> segmentList = new ArrayList<String>();
				for(int i=4;i<segments.length; i++) {
					segmentList.add(segments[i]);
				}
				path = ServiceUtils.explode("/", segmentList);
			}
		} 
		else if (segments.length > 1 && segments[0].equals("files") && 
				ArrayUtils.contains(endpoints, segments[1])) 
		{
			// their default system is implied. the structure is /files/$media[$path]
			if (segments.length == 2) 
			{
				path = "";
			} 
			else 
			{
				List<String> segmentList = new ArrayList<String>();
				for(int i=2;i<segments.length; i++) {
					segmentList.add(segments[i]);
				}
				path = ServiceUtils.explode("/", segmentList);
			}
		} else {
			throw new IOException("Unrecognized URL structure for path " + path);
		}
		
		if (path.equals(" ")) {
			path = "/";
		} else {
			path = StringUtils.replace(path, "/ /",  "//");
			// escape spaces in file names. the path is already decoded at this point
//			path = StringUtils.replace(path, " ", "\\ ");
		}
		
        // Convert Windows-format paths to sensible paths
        Pattern regexDriveLetter = Pattern.compile("^[a-zA-Z]:");
        Pattern regexSlash = Pattern.compile("\\\\");

        path = regexDriveLetter.matcher(path).replaceAll("");
        path = regexSlash.matcher(path).replaceAll("/");

		return path; 
		
	}
	
	/**
	 * Returns the {@link RemoteSystem#systemId} id or {@link RemoteSystem#uuid} from a valid File API request path 
	 * given by {@code originalPath}. If {@code originalPath} does not contain a 
	 * {@link RemoteSystem#systemId} id or {@link RemoteSystem#uuid}, it returns the user's default {@link StorageSystem#systemId}.
	 * 
	 * Example:
	 * <code>"/files/media/system/storage.example.com/path/to/file.dat"     =>  "storage.example.com"
	 * "/files/media/system/storage.example.com//path/to/file.dat"     =>  "storage.example.com"
	 * "/files/media/path/to/file.dat"                                 =>  "default.storage.example.com"
	 * "/files/media///path/to/file.dat"                               =>  "storage.example.com"
	 * </code>
	 * 
	 * @param originalPath full URL request path to the Files API
	 * @param username of user making the request. This is needed when looking up the default storage system. 
	 * @return the {@link RemoteSystem#systemId} id or {@link RemoteSystem#uuid} represented by the {@code originalPath}
	 * @throws IOException if the {@code originalPath} is not a valid Files API path 
	 */
	public static String getSystemIdFromPath(String originalPath, String owner) throws IOException 
	{	
//	    Matcher matcher = FilePathRegex.getMatcher(originalPath, owner);
//        
//        if (matcher == null) {
//            throw new IOException("Unrecognized URL structure for path " + originalPath);
//        } else { 
//            return matcher.group(1);
//        }
        
        if (StringUtils.isEmpty(originalPath)) {
			throw new IOException("Unrecognized URL structure for path " + originalPath);
		}
		
		// this will drop the webapp name which probably won't match the IO service route
		String path = originalPath;
		//path = path.substring(path.indexOf("files", 1));
		String[] endpoints = { "listings", "media", "pems", "meta", "history", "index" };
		
		String[] segments = path.substring(1).split("/", -1);//FileUtils.getPathStack(path);
		
		if (segments.length > 3 && segments[0].equals("files") && 
				ArrayUtils.contains(endpoints, segments[1]) &&
				segments[2].equals("system")) 
		{	
			// they specified the system id as the next token. drop it.
			return segments[3];
		} 
		else if (segments.length > 1 && segments[0].equals("files") && 
				ArrayUtils.contains(endpoints, segments[1])) 
		{
			return null;
		} else {
			throw new IOException("Unrecognized URL structure for path " + path);
		}
	}

	/**
	 * Resolve the file system path represented by the public unauthenticated
	 * file path represented by the request to the Files API.
	 * 
	 * @param owner
	 * @param path
	 * @return true if the path is in the user's home subtree, false otherwise
	 * @throws IOException 
	 * @throws URISyntaxException 
	 */
	public static String resolvePublic(String owner, String originalPath) throws IOException 
	{	
//	    if (!FilePathRegex.FILES_PUBLIC_URI.matches(originalPath, owner)) 
//	    {
//	        throw new IOException("Unrecognized publci URL structure for path " + originalPath);
//	    } 
//	    else 
//	    {
//            return resolve(owner, originalPath);
//	    }
        
		// this will drop the webapp name which probably won't match the IO service route
		String path = originalPath;
		//path = path.replaceAll("//", "/ /");
		path = path.substring(path.indexOf("files", 1));
		String[] segments = FileUtils.getPathStack(path);
		
		if (segments[0].equals("files") && segments[1].equals("download") &&
				segments[3].equals("system")) {
			// they specified the system id as the next token. drop it.
			if (segments.length == 5) {
				path = "";
			} else {
				List<String> segmentList = new ArrayList<String>();
				for(int i=5;i<segments.length; i++) {
					segmentList.add(segments[i]);
				}
				path = ServiceUtils.explode("/", segmentList);
			}
		} else {
			throw new IOException("Unrecognized URL structure for path " + path);
		}
		
		if (path.equals(" ")) {
			path = "/";
		} else {
			path = path.replaceAll("/ /",  "//");
		}
		
        // Convert Windows-format paths to sensible paths
        Pattern regexDriveLetter = Pattern.compile("^[a-zA-Z]:");
        Pattern regexSlash = Pattern.compile("\\\\");

        path = regexDriveLetter.matcher(path).replaceAll("");
        path = regexSlash.matcher(path).replaceAll("/");

		return path; 
		
	}
	
	/**
	 * Extracts the Agave username from the full URL {@code originalPath} of the current
	 * request to the Files API. This is only relevant when the given system is a public
	 * storage system. Otherwise ownership is not in any way implied by a file path.
	 * 
	 * @param originalPath full URL request path
	 * @return username or null if the path is absolute
	 * @throws IOException
	 */
	public static String getOwner(String originalPath) 
	throws IOException 
	{
	    String path = PathResolver.resolve(null, originalPath);
		if (StringUtils.isEmpty(path) || !path.startsWith("/") || path.equals("/")) {
			return null;
		} 
		else 
		{
			return FileUtils.getPathStack(path)[0];
		}
	}
	
	/**
	 * Checks to see if a username can be inferred from a system path. This only
	 * works if the system is public. Otherwise the determination needs to be 
	 * made from system roles and ACL.
	 * @param systemPath
	 * @param system
	 * @param remoteDataClient
	 * @return username if one exists, otherwise null
	 * 
	 * @throws IOException
	 */
	public static String getImpliedOwnerFromSystemPath(String systemPath, RemoteSystem system, RemoteDataClient remoteDataClient) 
	throws IOException 
	{
		// only public systems have implied ownership. Everything else can be determined from
		// system roles and ACLs.
		if (system.isPubliclyAvailable()) 
		{
			String resolvedPath = remoteDataClient.resolvePath(systemPath);
			String homeDir = remoteDataClient.resolvePath("");
			
			if (StringUtils.isEmpty(resolvedPath)) {
				return null;
			} 
			else if (StringUtils.startsWith(resolvedPath, homeDir)) 
			{
				resolvedPath = StringUtils.replaceOnce(resolvedPath, homeDir, "");
				return resolvedPath.split("/")[0];
			}
		} 
		
		
		return null;
	}
	
}
