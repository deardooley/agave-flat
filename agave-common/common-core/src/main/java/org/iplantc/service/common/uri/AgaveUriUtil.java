/**
 *
 */
package org.iplantc.service.common.uri;

import java.net.URI;

import org.iplantc.service.common.exceptions.AgaveNamespaceException;

/**
 * Utility class to parse URLs and transform API urls into their various
 * API references.
 *
 * @author dooley
 *
 */
public class AgaveUriUtil {
    
	/**
	 * Determines whether the URI references an API resource within a given
	 * tenant. Since this may be called from worker processes, we need to
	 * explicitly provide the tenant info in the call. API resources are
	 * defined as those which have the <code>agave</code> schema or the URL
	 * is an service URL
	 *
	 * @param inputUri
	 * @return
	 * @throws AgaveNamespaceException
	 */
	public static boolean isInternalURI(URI inputUri) throws AgaveNamespaceException
	{
	    return AgaveUriRegex.matchesAny(inputUri);
		
          
//        else if (sUir.matches())
//		// if this is an files service API
//		else if (inputUri.toString().startsWith(filesServiceUri.toString()))
//		{
//			// parse out api path prefix
//			String path = StringUtils.substring(inputUri.getPath(), filesServiceUri.getPath().length());
//
//			if (StringUtils.isNotEmpty(path) && path.toLowerCase().startsWith("media/"))
//			{
//				// remove content type keyword
//				path = StringUtils.replaceOnce(path, "media/", "");
//
//				// check for system definition
//				if (path.startsWith("system/"))
//				{
//					// remove system keyword
//					path = StringUtils.replaceOnce(path, "system/", "");
//
//					// if the systme id is empty, it's a bad url
//					if (StringUtils.startsWith(path, "/"))
//					{
//						throw new AgaveNamespaceException("The given URL, " + inputUri + ", is an invalid internal URL.");
//					}
//					else if (!StringUtils.contains(path, "/")) {
//						throw new AgaveNamespaceException("The given URL, " + inputUri + ", is an invalid internal URL.");
//					} else {
//						return true;
//					}
//				}
////				else if (path.startsWith("system")) {
////					throw new AgaveNamespaceException("The given URL, " + inputUri + ", is an invalid internal URL.");
////				}
//				else
//				{
//					return true;
//				}
//			}
//			else
//			{
//				return false;
//			}
//		}
//		// if the scheme and host are the same, it's internal
//		else if (StringUtils.equals(inputUri.getScheme(), filesServiceUri.getScheme()) &&
//				StringUtils.equals(inputUri.getHost(), filesServiceUri.getHost())) {
//			return true;
//		// if the scheme is agave:// it's internal
//		} else if (StringUtils.equalsIgnoreCase(inputUri.getScheme(), "agave")) {
//			return true;
//		// if it's a file path, it's internal...resolved to default storage system path
//		} else if (StringUtils.isEmpty(inputUri.getScheme()) &&
//				StringUtils.isEmpty(inputUri.getHost()) &&
//				!StringUtils.isEmpty(inputUri.getPath())) {
//			return true;
//		// otherwise it's false
//		} else {
//			return false;
//		}
	}
}
