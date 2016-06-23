/**
 * 
 */
package org.iplantc.service.systems.util;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Matcher;

import org.apache.commons.lang.StringUtils;
import org.iplantc.service.common.exceptions.AgaveNamespaceException;
import org.iplantc.service.common.exceptions.PermissionException;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.common.uri.AgaveUriRegex;
import org.iplantc.service.common.uri.AgaveUriUtil;
import org.iplantc.service.systems.dao.SystemDao;
import org.iplantc.service.systems.exceptions.SystemException;
import org.iplantc.service.systems.exceptions.SystemUnknownException;
import org.iplantc.service.systems.manager.SystemManager;
import org.iplantc.service.systems.model.RemoteSystem;
import org.iplantc.service.transfer.RemoteDataClient;

/**
 * Utility class to parse URLs and transform API urls into their various
 * API references.
 *  
 * @author dooley
 *
 */
public class ApiUriUtil {

	/**
	 * Determines whether the URI references an API endpoint. This is true
	 * if the URI has the <code>agave</code> schema or the URL is an IO 
	 * service URL
	 * 
	 * @param inputUri
	 * @return
	 */
	public static boolean isInternalURI(URI inputUri)
	{
		try {
			return AgaveUriUtil.isInternalURI(inputUri);
		} catch (AgaveNamespaceException e) {
			return false;
		}
	}

	/**
	 * Returns the Agave-relevant path in an API URL. This is generally the remainder of the path
	 * after the system id or, if no system id is specified, the remainder of the 
	 * path after the content type. In the case of an internal URI, the entire path
	 * is returned.
	 *  
	 * @param inputUri
	 * @return
	 * @throws IOException 
	 * @throws URISyntaxException 
	 */
	public static String getPath(URI inputUri) throws AgaveNamespaceException
	{
	    Matcher matcher = AgaveUriRegex.getMatcher(inputUri);
	    
	    if (matcher == null) 
	    {
	        throw new AgaveNamespaceException("URI does conform to the API URL structure.");
	    }
	    // file urls with custom systems and job output uri match the path second
	    else if (AgaveUriRegex.FILES_URI_CUSTOM_SYSTEM.matches(inputUri) 
	            || AgaveUriRegex.JOBS_URI.matches(inputUri)
	            || AgaveUriRegex.AGAVE_URI.matches(inputUri))
	    {
	        return matcher.group(2);
	    }
	    // default files urls and paths do not have a host associated with them, so 
	    // the path is first.
	    else 
	    {
	        return matcher.group(1);
	    }
	}

	/**
	 * Returns the API system referenced in the URI. If the <code>agave</code>
	 * scheme is specified, the system id will be the hostname in the URI. If 
	 * an IO service URI is given, it will be system id in the path. If the 
	 * path does not have a system id in the expected place, the user's default
	 * storage system will be returned. Note that the owner must have a role on
	 * the given system to for this to work.
	 * 
	 * @param owner
	 * @param inputUri
	 * @return RemoteSystem 
	 * @throws AgaveNamespaceException if the URI is not a valid URI
	 * @throws SystemUnknownException if no available system can be found for the user and uri 
	 * @throws PermissionException if the user does not have access to the system represented by the uri
	 */
	public static RemoteSystem getRemoteSystem(String owner, URI inputUri)
	throws AgaveNamespaceException, SystemUnknownException, PermissionException
	{
		SystemDao dao = new SystemDao();
		RemoteSystem system = null;
		
		Matcher matcher = AgaveUriRegex.getMatcher(inputUri);
        
		// let's check for an internal URI
        if (matcher == null) {
            throw new AgaveNamespaceException("URI does conform to the API URL structure.");
        } 
        // Is it a job output uri?
        else if (AgaveUriRegex.JOBS_URI.matches(inputUri)) 
        {
            String uuid = matcher.group(1);
            
            // no permission check here. user permissions were vetted on the job
            // here we know that if there was a system returned, the user has access
            // to it due to their job permissions
            system = dao.getRemoteSystemForJobOutput(uuid, owner, TenancyHelper.getCurrentTenantId());
            if (system == null) {
                throw new SystemUnknownException("No output system found for job " + uuid);
            } else {
                return system;
            }
        } 
        // is it an agave uri?
        else if (AgaveUriRegex.AGAVE_URI.matches(inputUri)) 
        {
            String systemId = matcher.group(1);

            // use the public default storage system
            if (StringUtils.isEmpty(systemId)) 
            {
                system = new SystemManager().getUserDefaultStorageSystem(owner);
                if (system == null) {
                    throw new SystemUnknownException("No default storage system found for user " + owner);
                } else {
                    return system;
                }
            } 
            else {
                system = dao.findBySystemId(systemId);
                if (system != null) {
                    if (system.getUserRole(owner).canRead()) {
                        return system;
                    } else {
                        throw new PermissionException("User does not have permission to use system " + system.getSystemId());
                    }
                } 
                else 
                {
                    throw new SystemUnknownException("No output system found for job " + matcher.group(1));
                }
            }
        }
        // is it a file service uri?
        else if (AgaveUriRegex.FILES_URI_CUSTOM_SYSTEM.matches(inputUri))
        {
            system = dao.findBySystemId(matcher.group(1));
            if (system != null) {
                if (system.getUserRole(owner).canRead()) {
                    return system;
                } else {
                    throw new PermissionException("User does not have permission to use system " + system.getSystemId());
                }
            } 
            else 
            {
                throw new SystemUnknownException("No system found for the given URL");
            }
        }
        // fall back on user default storage system.
        else 
        {
            system = new SystemManager().getUserDefaultStorageSystem(owner);
            if (system == null) {
                throw new SystemUnknownException("The internal URI references a default storage system, "
                		+ "but no default storage system was found for user " + owner);
            } else {
                return system;
            }
        }
	}
	
	
	/**
	 * Returns the absolute path on a remote system for the given {@code inputUri}.
	 * This is done by calling the {@link RemoteDataClient#resolvePath(String)} method with 
	 * the value from {@link #getPath(URI)}.
	 * @param inputUri
	 * @return
	 * @throws AgaveNamespaceException if the URI is not a valid URI
	 * @throws SystemUnknownException if no available system can be found for the user and uri 
	 * @throws PermissionException if the user does not have access to the system represented by the uri
	 */
	public static String getAbsolutePath(String requestingUser, URI inputUri) 
	throws SystemUnknownException, PermissionException, AgaveNamespaceException
    {
	    RemoteSystem system = ApiUriUtil.getRemoteSystem(requestingUser, inputUri);
	    
	    String relativePath = ApiUriUtil.getPath(inputUri);
	    
	    // for job output URI we need to resolve the work directory first to obtain the actual URI
	    // this is probably not what you want to use to validate access, however, since the user
	    // may not even have access to this system outside of this work directory.
	    if (AgaveUriRegex.JOBS_URI.matches(inputUri)) {
	        String jobUuid = AgaveUriRegex.getMatcher(inputUri).group(1);
	        String workDir = new SystemDao().getRelativePathForJobOutput(jobUuid, requestingUser, TenancyHelper.getCurrentTenantId());
	        relativePath = StringUtils.removeEnd(workDir, "/") + "/" + relativePath;
	    }
	    
        RemoteDataClient client = null;
        try {
            client = system.getRemoteDataClient();
            return client.resolvePath(relativePath);
        } 
        catch (Exception e) {
            throw new SystemUnknownException("Unable to resolve the remote path of the given URI.", e);
        }
        finally {
            try { client.disconnect(); } catch(Exception e) {}
        }
    }
}
