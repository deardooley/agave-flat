/**
 * 
 */
package org.iplantc.service.common.resource;

import static org.iplantc.service.common.search.AgaveResourceResultOrdering.*;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.iplantc.service.common.search.AgaveResourceResultOrdering;
import org.iplantc.service.common.search.AgaveResourceSearchFilter;
import org.iplantc.service.common.search.SearchTerm;
import org.restlet.Context;
import org.restlet.data.Form;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;

/**
 * @author dooley
 *
 */
public abstract class SortableAgaveResource<T extends AgaveResourceSearchFilter> extends AgaveResource implements Sortable<T>
{
	/**
	 * Default {@link Restlet} constructor.
	 */
	public SortableAgaveResource(Context context, Request request, Response response) {
		super(context,request,response);
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.common.resource.Sortable#getSortOrder()
	 */
    @Override
	public AgaveResourceResultOrdering getSortOrder() 
	throws ResourceException 
    {	
    	return getSortOrder(ASCENDING);
    }
    
    /* (non-Javadoc)
	 * @see org.iplantc.service.common.resource.Sortable#getSortOrder(org.iplantc.service.common.search.AgaveResourceResultOrdering)
	 */
    @Override
	public AgaveResourceResultOrdering getSortOrder(AgaveResourceResultOrdering defaultOrder) 
    throws ResourceException 
    {	
    	AgaveResourceResultOrdering sortOrder = ASCENDING;
    	if (defaultOrder != null) {
    		sortOrder = defaultOrder;
    	}
    	
    	Form form = Request.getCurrent().getOriginalRef().getQueryAsForm();
        if (form != null) {
        	String sorder = form.getFirstValue("order");
	        if (!StringUtils.isBlank(sorder)) {
	        	try {
	        		sortOrder = AgaveResourceResultOrdering.getCaseInsensitiveValue(sorder);
	        	}
	        	catch (IllegalArgumentException e) {
	        		throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
                            "Invalid value for order. If specified, please provide one of: ASC, DESC");
	        	}
	        }
        }
        
        return sortOrder;
    }
    
    /* (non-Javadoc)
	 * @see org.iplantc.service.common.resource.SortableResource#getSortOrderSearchTerm()
	 */
    @Override
	public SearchTerm getSortOrderSearchTerm() 
    throws ResourceException 
    {	
    	AgaveResourceSearchFilter searchFilter = getAgaveResourceSearchFilter();
		SearchTerm searchTerm = null;
		
    	Form form = Request.getCurrent().getOriginalRef().getQueryAsForm();
        if (form != null) {
        	String orderByQueryTerm = form.getFirstValue("orderBy");
	        if (!StringUtils.isBlank(orderByQueryTerm)) {
	        	try {
	        		orderByQueryTerm = orderByQueryTerm.toLowerCase();
	        		if (searchFilter.getSearchTermMappings().containsKey(orderByQueryTerm)) {
	        			searchTerm = searchFilter.filterAttributeName(orderByQueryTerm);
	        		}
	        		else {
	        			throw new IllegalArgumentException();
	        		}
	        	}
	        	catch (Exception e) {
	        		throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
                            "Invalid value for orderBy. If specified, please provide a valid search field by which to sort.", e);
	        	}
	        }
        }
        
        return searchTerm;
    }
}
