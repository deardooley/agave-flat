package org.iplantc.service.data.resources;

import org.iplantc.service.common.resource.AgaveResource;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;

/**
 * @author dooley
 *
 */
public class AbstractTransformResource extends AgaveResource {

	/**
	 * @param context
	 * @param request
	 * @param response
	 */
	public AbstractTransformResource(Context context, Request request,
			Response response)
	{
		super(context, request, response);
	}
}
