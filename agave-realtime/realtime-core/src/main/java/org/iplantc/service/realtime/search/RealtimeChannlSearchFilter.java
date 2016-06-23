package org.iplantc.service.realtime.search;

import java.util.Date;
import java.util.Map;
import java.util.Set;

import org.iplantc.service.common.search.AgaveResourceSearchFilter;

/**
 * This class is the basis for search support across the API.
 * Each service should implement this class to filter query parameters
 * from the url into valid hibernate query.
 * 
 * @author dooley
 *
 */
public class RealtimeChannlSearchFilter extends AgaveResourceSearchFilter
{
    public RealtimeChannlSearchFilter() {}

	/* (non-Javadoc)
	 * @see org.iplantc.service.common.search.AgaveResourceSearchFilter#getSearchTermPrefix()
	 */
	@Override
	protected String getSearchTermPrefix() {
	    return "c.";
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
            searchTermMappings.put("events", "observableEvents.name");
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
            searchTypeMappings.put("events", Set.class);
		}
		
		return searchTypeMappings;

	}
}

