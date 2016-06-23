package org.iplantc.service.data.resources;  

import org.iplantc.service.common.resource.DocumentationResource;
import org.iplantc.service.data.Settings;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
  
/** 
 * Resource which has only one representation. 
 * 
 */  
public class TransformsDocumentationResource extends DocumentationResource 
{  
	public TransformsDocumentationResource(Context context, Request request, Response response) 
	{   
		super(context, request, response);  
    }  
  
    public String getServiceDocumentationUrl() {
    	return Settings.IPLANT_DOCS + "/#!/transforms.json";
    }
}  