/**
 * 
 */
package org.iplantc.service.jobs.resources;

import org.iplantc.service.common.resource.AgaveResource;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;

/**
 * @author dooley
 *
 */
public class AbstractJobResource extends AgaveResource {

	protected String username = null;
	
	/**
	 * @param context
	 * @param request
	 * @param response
	 */
	public AbstractJobResource(Context context, Request request,
			Response response)
	{
		super(context, request, response);
	}
	
}
