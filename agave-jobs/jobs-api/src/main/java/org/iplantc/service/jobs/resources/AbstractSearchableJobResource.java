/**
 * 
 */
package org.iplantc.service.jobs.resources;

import java.util.HashMap;
import java.util.Map;

import org.iplantc.service.common.resource.SearchableAgaveResource;
import org.iplantc.service.common.search.AgaveResourceSearchFilter;
import org.iplantc.service.common.search.SearchTerm;
import org.restlet.Context;
import org.restlet.data.Form;
import org.restlet.data.Request;
import org.restlet.data.Response;

/**
 * @author dooley
 *
 */
public abstract class AbstractSearchableJobResource extends SearchableAgaveResource<AgaveResourceSearchFilter> {

	protected String username = null;
	
	/**
	 * @param context
	 * @param request
	 * @param response
	 */
	public AbstractSearchableJobResource(Context context, Request request,
			Response response)
	{
		super(context, request, response);
	}
	
}
