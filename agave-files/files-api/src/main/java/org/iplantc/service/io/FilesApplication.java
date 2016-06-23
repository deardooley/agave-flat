/**
 * 
 */
package org.iplantc.service.io;

import java.io.FileReader;

import org.apache.log4j.Logger;
import org.iplantc.service.common.auth.VerifierFactory;
import org.iplantc.service.common.persistence.HibernateUtil;
import org.iplantc.service.common.persistence.JndiSetup;
import org.iplantc.service.common.representation.AgaveErrorRepresentation;
import org.iplantc.service.common.representation.AgaveRepresentation;
import org.iplantc.service.common.representation.AgaveSuccessRepresentation;
import org.iplantc.service.common.restlet.resource.QuartzUtilityResource;
import org.iplantc.service.io.resources.EncodingCallbackResource;
import org.iplantc.service.io.resources.FileHistoryResource;
import org.iplantc.service.io.resources.FileIndexingResource;
import org.iplantc.service.io.resources.FileListingResource;
import org.iplantc.service.io.resources.FileManagementResource;
import org.iplantc.service.io.resources.FilePermissionResource;
import org.iplantc.service.io.resources.FilesDocumentationResource;
import org.iplantc.service.io.resources.PublicFileDownloadResource;
import org.iplantc.service.io.resources.QuartzResource;
import org.restlet.Application;
import org.restlet.Component;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.Server;
import org.restlet.data.ChallengeScheme;
import org.restlet.data.CharacterSet;
import org.restlet.data.MediaType;
import org.restlet.data.Protocol;
import org.restlet.data.Status;
import org.restlet.ext.jetty.HttpServerHelper;
import org.restlet.ext.jetty.JettyServerHelper;
import org.restlet.representation.Representation;
import org.restlet.resource.Resource;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;
import org.restlet.routing.Router;
import org.restlet.routing.Template;
import org.restlet.security.ChallengeAuthenticator;
import org.restlet.security.Verifier;
import org.restlet.service.CorsService;
import org.restlet.service.MetadataService;
import org.restlet.service.StatusService;

/**
 * @author dooley
 *
 */
public class FilesApplication extends Application
{   
	
	private static final Logger log = Logger.getLogger(FilesApplication.class);
	
	public FilesApplication() {
		super();
		setName("agaveFilesApi");
		
		try {
			log.debug("JVM character encoding: " + System.getProperty("file.encoding"));
			log.debug("IO Readers character encoding: " + new FileReader("/etc/hosts").getEncoding());
		} catch (Exception e) {
			log.error("Unable to determine IO Reader character encoding");
		}
    	
    	setStatusService(new StatusService() {

    		/* (non-Javadoc)
    		 * @see org.restlet.service.StatusService#toStatus(java.lang.Throwable, org.restlet.resource.Resource)
    		 */
    		@Override
    		public Status toStatus(Throwable throwable, Resource resource)
    		{
    			if ( throwable instanceof ResourceException ) {
    				ResourceException re = ((ResourceException)throwable);
    				return new Status(re.getStatus(), 
    						(throwable.getCause() != null && throwable.getCause() != throwable) ? re.getCause() : throwable, 
    						re.getMessage());
    			} else {
    				return resource.getResponse().getStatus();
    			}
    		}
    		
    		@Override
    		public Representation toRepresentation(Status status, Request request,
    	            Response response) {
    			try {
    				Representation currentRepresentation = response.getEntity();
    				if (currentRepresentation instanceof AgaveRepresentation) {
    					return currentRepresentation;
    				} else if (status.isSuccess()) {
    					return new AgaveSuccessRepresentation();
    				} else {
    					String message = null;
    					if (status.getCode() == 401) {
    						if (request.getChallengeResponse() == null) {
    							message = "Permission denied. No authentication credentials found.";
    						} else {
    							message = "Permission denied. Invalid authentication credentials";
    						}
    					} else {
    						message = status.getDescription();
    					}
    					return new AgaveErrorRepresentation(message);
    				}
    			} finally {
    				try { HibernateUtil.closeSession(); } catch(Exception e) {}
    			}
    		}
    		
//    			if (throwable == null) {
//    		        return resource.getStatus();
//    		    }
//    		    else if (throwable instanceof ResourceException) {
//    		        return ((ResourceException)throwable).getStatus();
//    		    } else {
//    		    	return getStatus(throwable, resource);
//    		    }
    		

//			/* (non-Javadoc)
//			 * @see org.restlet.service.StatusService#toStatus(java.lang.Throwable, org.restlet.resource.Resource)
//			 */
//			@Override
//			public Status toStatus(Throwable throwable, Resource resource) {
//				// TODO Auto-generated method stub
//				return super.toStatus(throwable, resource);
//			}
    		
    	});
    	
    	MetadataService metadataService = new MetadataService();
    	metadataService.setDefaultCharacterSet(CharacterSet.UTF_8);
    	metadataService.setDefaultMediaType(MediaType.APPLICATION_JSON);
    	setMetadataService(metadataService);
    	
        //CorsService corsService = new CorsService();
        //corsService.setAllowedCredentials(true);
        //corsService.setAllowingAllRequestedHeaders(true);
        //corsService.setSkippingResourceForCorsOptions(true);
        //getServices().add(corsService);
    }

	@Override
	public Restlet createInboundRoot() {
        
    	Router router = new Router(getContext());
    	router.setDefaultMatchingMode(Template.MODE_STARTS_WITH);
    	
    	// Define the router for the static usage page
//    	router.attach("/", FilesDocumentationResource.class);
    	router.attach("/usage", FilesDocumentationResource.class);
    	router.attach("/workers/{quartzaction}", QuartzUtilityResource.class);
    	
    	
        secureEndpoint(router, "/quartz", QuartzResource.class);
        secureEndpoint(router, "/quartz/", QuartzResource.class);
        
    		// Define authenticated I/O routes
        if (!Settings.SLAVE_MODE) {
	        router.attach("/download/{username}/system/{systemId}/", PublicFileDownloadResource.class);
        
    		secureEndpoint(router,"/media/system/{systemId}", FileManagementResource.class); // DONE upload(POST) a file, get upload form(GET)
            secureEndpoint(router,"/media/system/{systemId}/", FileManagementResource.class); // DONE upload(POST) a file, get upload form(GET)
            secureEndpoint(router,"/listings/system/{systemId}", FileListingResource.class); // DONE get(GET) to list upload files for user
            secureEndpoint(router,"/listings/system/{systemId}/", FileListingResource.class); // DONE get(GET) to list upload files for user
            secureEndpoint(router,"/history/system/{systemId}", FileHistoryResource.class); // DONE get(GET) to list upload files for user
            secureEndpoint(router,"/history/system/{systemId}/", FileHistoryResource.class); // DONE get(GET) to list upload files for user
            secureEndpoint(router,"/pems/system/{systemId}", FilePermissionResource.class); // DONE get(GET) to list upload files for user
            secureEndpoint(router,"/pems/system/{systemId}/", FilePermissionResource.class); // DONE get(GET) to list upload files for user
          
            secureEndpoint(router,"/index", FileIndexingResource.class); // DONE index(POST) a file, get index
    		secureEndpoint(router,"/index/", FileIndexingResource.class); // DONE index(POST) a file, get index
    		secureEndpoint(router,"/media", FileManagementResource.class); // DONE upload(POST) a file, get upload form(GET)
    		secureEndpoint(router,"/media/", FileManagementResource.class); // DONE upload(POST) a file, get upload form(GET)
    		secureEndpoint(router,"/listings", FileListingResource.class); // DONE get(GET) to list upload files for user
	        secureEndpoint(router,"/listings/", FileListingResource.class); // DONE get(GET) to list upload files for user
	        secureEndpoint(router,"/history", FileHistoryResource.class); // DONE get(GET) to list upload files for user
            secureEndpoint(router,"/history/", FileHistoryResource.class); // DONE get(GET) to list upload files for user
            secureEndpoint(router,"/pems", FilePermissionResource.class); // DONE get(GET) to list upload files for user
	        secureEndpoint(router,"/pems/", FilePermissionResource.class); // DONE get(GET) to list upload files for user
        
        }   
    
	    // Define callback route for the transform scripts
	    router.attach("/trigger/encoding/{callbackKey}/{status}", EncodingCallbackResource.class);
	    router.attach("/system/{systemId}/trigger/encoding/{callbackKey}/{status}", EncodingCallbackResource.class);
	    
	    return router;
    }

	protected void secureEndpoint(Router router, String path,
			Class<? extends ServerResource> targetResource)
	{
		Verifier verifier = new VerifierFactory().createVerifier(Settings.AUTH_SOURCE);
  		
		ChallengeAuthenticator apiGuard = new ChallengeAuthenticator(getContext(),
  				 ChallengeScheme.HTTP_BASIC, "The Agave Platform");
  		
  		
		
        apiGuard.setVerifier(verifier);
        
		apiGuard.setNext(targetResource);
		
		router.attach(path, apiGuard);
	}
	
	public static void main(String[] args) throws Exception 
	{	
		JndiSetup.init();
		
		// Create a new Component.
        Component component = new Component();
        component.getServers().add(Protocol.HTTP, 8080);
        component.getDefaultHost().attach("/files", new FilesApplication());
        component.start();
        
//       launchServer(component);
    }
	
	protected static void launchServer(Component component) throws Exception 
	{	
		 // create embedding jetty server
        Server embedingJettyServer = new Server(
	        component.getContext().createChildContext(),
	        Protocol.HTTP,
//	        org.iplantc.service.common.Settings.JETTY_PORT,
	        8080,
	        component
        );
        
        //construct and start JettyServerHelper
        JettyServerHelper jettyServerHelper = new HttpServerHelper(embedingJettyServer);
        jettyServerHelper.start();
	}
}
