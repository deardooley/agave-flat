package org.iplantc.service.systems.resources;  

import org.iplantc.service.common.resource.DocumentationResource;
import org.iplantc.service.systems.Settings;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
  
/** 
 * Resource which has only one representation. 
 * 
 */  
public class SystemsDocumentationResource extends DocumentationResource 
{  
	public SystemsDocumentationResource(Context context, Request request, Response response) 
	{   
		super(context, request, response);  
    }  
  
    public String getServiceDocumentationUrl() {
    	return Settings.IPLANT_DOCS + "/#!/systems.json";
    }
}  