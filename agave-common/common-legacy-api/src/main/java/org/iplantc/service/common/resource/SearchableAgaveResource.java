/**
 * 
 */
package org.iplantc.service.common.resource;

import java.util.HashMap;
import java.util.Map;

import org.iplantc.service.common.search.AgaveResourceSearchFilter;
import org.iplantc.service.common.search.SearchTerm;
import org.restlet.Context;
import org.restlet.data.Form;
import org.restlet.data.Request;
import org.restlet.data.Response;

/**
 * Abstract parent class for all searchable {@link AgaveResource} implementing classes.
 * This should be extended by all collections.
 * @author dooley
 *
 */
public abstract class SearchableAgaveResource<T extends AgaveResourceSearchFilter> extends SortableAgaveResource<T> implements Searchable<T> {

	/**
	 * Default {@link Restlet} constructor
	 * @param context
	 * @param request
	 * @param response
	 */
	public SearchableAgaveResource(Context context, Request request,
			Response response) {
		super(context, request, response);
	}
	
	/* (non-Javadoc)
	 * @see org.iplantc.service.common.resource.SortableAgaveResource#getQueryParameters()
	 */
	@Override
	public Map<SearchTerm, Object> getQueryParameters()
	{
		Form form = getRequest().getOriginalRef().getQueryAsForm();
		if (form != null && !form.isEmpty()) {
			return getAgaveResourceSearchFilter().filterCriteria(form.getValuesMap());
		} else {
			return new HashMap<SearchTerm, Object>();
		}
	}

}
