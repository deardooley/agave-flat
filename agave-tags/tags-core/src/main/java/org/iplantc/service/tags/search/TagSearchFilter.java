package org.iplantc.service.tags.search;

import java.util.Date;
import java.util.Map;

import org.iplantc.service.common.search.AgaveResourceSearchFilter;

/**
 * This class is the basis for search support across the API.
 * Each service shold implement this class to filter query parameters
 * from the url into valid hibernate query.
 * 
 * @author dooley
 *
 */
public class TagSearchFilter extends AgaveResourceSearchFilter
{
    public TagSearchFilter() {}

	/* (non-Javadoc)
	 * @see org.iplantc.service.common.search.AgaveResourceSearchFilter#getSearchTermPrefix()
	 */
	@Override
	protected String getSearchTermPrefix() {
	    return "t.";
	}

    /* (non-Javadoc)
	 * @see org.iplantc.service.common.search.AgaveResourceSearchFilter#getSearchTermMappings()
	 */
	@Override
	public Map<String, String> getSearchTermMappings()
	{
	    if (searchTermMappings.isEmpty()) {
            searchTermMappings.put("created", "%screated");
            searchTermMappings.put("id", "%suuid");
            searchTermMappings.put("lastupdated", "%slastUpdated");
            searchTermMappings.put("name", "%sname");
            searchTermMappings.put("resources", "taggedResource.uuid");
		}
		
		return searchTermMappings;

	}
	
	@SuppressWarnings("rawtypes")
	@Override
	public Map<String, Class> getSearchTypeMappings()
	{
	    if (searchTypeMappings.isEmpty()) {
            searchTypeMappings.put("created", Date.class);
            searchTypeMappings.put("id", String.class);
            searchTypeMappings.put("lastupdated", Date.class);
            searchTypeMappings.put("name", String.class);
            searchTypeMappings.put("public", Boolean.class);
            searchTypeMappings.put("resources", Integer.class);
		}
		
		return searchTypeMappings;

	}
}

