/**
 * 
 */
package org.iplantc.service.data;

import org.iplantc.service.common.representation.QuartzUtilityResource;
import org.iplantc.service.common.restlet.AgaveApplication;
import org.iplantc.service.data.resources.AsyncFileTransformResource;
import org.iplantc.service.data.resources.DecodingCallbackResource;
import org.iplantc.service.data.resources.QuartzResource;
import org.iplantc.service.data.resources.SyncFileTransformResource;
import org.iplantc.service.data.resources.TransformListingResource;
import org.iplantc.service.data.resources.TransformsDocumentationResource;
import org.restlet.Router;

/**
 * @author dooley
 *
 */
public class TransformApplication extends AgaveApplication
{
    @Override
    protected void mapServiceEndpoints(Router router) 
    {   
    	// Define the router for the static usage page
    	router.attach(getStandalonePrefix() + "/usage",TransformsDocumentationResource.class);
    	router.attach(getStandalonePrefix() + "/usage/",TransformsDocumentationResource.class);
    	router.attach(getStandalonePrefix() + "/workers/{quartzaction}", QuartzUtilityResource.class);
        secureEndpoint(router, "/quartz", QuartzResource.class);
    	secureEndpoint(router, "/quartz/", QuartzResource.class);
        
    	if (!Settings.SLAVE_MODE) 
    	{	
    		// Define authenticated Data routes
    		secureEndpoint(router, "", TransformListingResource.class); 
    		secureEndpoint(router, "/", TransformListingResource.class);
    		
    		secureEndpoint(router, "/{transformId}",TransformListingResource.class); 
    		secureEndpoint(router, "/{transformId}/",TransformListingResource.class);
 
	        secureEndpoint(router, "/{transformId}/async",AsyncFileTransformResource.class); 
	        secureEndpoint(router, "/{transformId}/async/",AsyncFileTransformResource.class);
	        secureEndpoint(router, "/{transformId}/sync",SyncFileTransformResource.class); 
	        secureEndpoint(router, "/{transformId}/sync/",SyncFileTransformResource.class);
    	}
     
    	// Define callback route for the transform scripts
    	router.attach(getStandalonePrefix() + "/trigger/decoding/{callbackKey}/{status}", DecodingCallbackResource.class);
    }
    
    @Override
	protected String getStandalonePrefix() {
		return !isStandaloneMode() ? "" : "/transforms";
	}
}