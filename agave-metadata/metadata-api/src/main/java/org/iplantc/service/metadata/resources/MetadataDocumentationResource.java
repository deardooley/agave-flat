package org.iplantc.service.metadata.resources;  

import org.iplantc.service.common.resource.DocumentationResource;
import org.iplantc.service.metadata.Settings;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
  
/** 
 * Resource which has only one representation. 
 * 
 */  
public class MetadataDocumentationResource extends DocumentationResource 
{  
	public MetadataDocumentationResource(Context context, Request request, Response response) 
	{   
		super(context, request, response);  
    }  
  
    public String getServiceDocumentationUrl() {
    	return Settings.IPLANT_DOCS + "/#!/metadata.json";
    }
}  