package org.iplantc.service.jobs.resources;  

import org.iplantc.service.common.resource.DocumentationResource;
import org.iplantc.service.jobs.Settings;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
  
/** 
 * Resource which has only one representation. 
 * 
 */  
public class JobDocumentationResource extends DocumentationResource 
{  
	public JobDocumentationResource(Context context, Request request, Response response) 
	{   
		super(context, request, response);
    }  
  
    public String getServiceDocumentationUrl() {
    	return Settings.IPLANT_DOCS + "/#!/jobs.json";
    }
}  