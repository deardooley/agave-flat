package org.iplantc.service.common.resource;  

import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.Resource;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;
  
/** 
 * Placeholder url to send the user to the official documentation for this service.
 * 
 */  
public abstract class DocumentationResource extends Resource 
{  
	public DocumentationResource(Context context, Request request, Response response) 
	{   
		super(context, request, response);  
		response.redirectSeeOther(getServiceDocumentationUrl());
    }  
  
    @Override  
    public Representation represent(Variant variant) throws ResourceException {
    	throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, "Please visit " + 
    			getServiceDocumentationUrl() + " for full api documentation.");  
    } 
   
    public abstract String getServiceDocumentationUrl();
}  