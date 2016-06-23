package org.iplantc.service.io.resources;  

import org.iplantc.service.io.Settings;
import org.restlet.Response;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;
  
/** 
 * Resource which has only one representation. 
 * 
 */  
public class FilesDocumentationResource extends ServerResource 
{  
	public FilesDocumentationResource() {}
  
	@Get
    public void redirectToDocumentation() {
    	Response.getCurrent().redirectPermanent(Settings.IPLANT_DOCS + "/#!/files.json");
    }
}  