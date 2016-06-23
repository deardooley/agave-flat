package org.iplantc.service.notification.search;

import java.util.Date;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.iplantc.service.common.search.AgaveResourceSearchFilter;
import org.iplantc.service.notification.model.enumerations.NotificationCallbackProviderType;

/**
 * This class is the basis for search support across the API.
 * Each service should implement this class to filter query parameters
 * from the url into valid hibernate query.
 * 
 * @author dooley
 *
 */
public class NotificationAttemptSearchFilter extends AgaveResourceSearchFilter
{
    public NotificationAttemptSearchFilter() {}

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
	    	searchTermMappings.put("attemptNumber", "%sattemptNumber");
	    	searchTermMappings.put("content", "%scontent");
            searchTermMappings.put("created", "%screated");
            searchTermMappings.put("endtime", "%sendTime");
            searchTermMappings.put("eventname", "%seventName");
            searchTermMappings.put("id", "%suuid");
            searchTermMappings.put("notificationid", "%snotificationId");
            searchTermMappings.put("owner", "%sowner");
            searchTermMappings.put("provider", "%sprovider");
            searchTermMappings.put("response.code", "%sresponse.code");
            searchTermMappings.put("response.message", "%sresponse.message");
            searchTermMappings.put("scheduledtime", "%sscheduledTime");
            searchTermMappings.put("starttime", "%sstartTime");
            searchTermMappings.put("url", "%scallbackUrl");
		}
		
		return searchTermMappings;

	}
	
	@SuppressWarnings("rawtypes")
	@Override
	public Map<String, Class> getSearchTypeMappings()
	{
	    if (searchTypeMappings.isEmpty()) {
	    	searchTypeMappings.put("attemptNumber", Integer.class);
	    	searchTypeMappings.put("content", String.class);
	    	searchTypeMappings.put("created", Date.class);
	    	searchTypeMappings.put("endtime", Date.class);
	    	searchTypeMappings.put("eventname", String.class);
	    	searchTypeMappings.put("id", String.class);
	    	searchTypeMappings.put("owner", String.class);
	    	searchTypeMappings.put("notificationid", String.class);
	    	searchTypeMappings.put("provider", NotificationCallbackProviderType.class);
            searchTypeMappings.put("response.code", Integer.class);
            searchTypeMappings.put("response.message", String.class);
            searchTypeMappings.put("scheduledtime", Date.class);
            searchTypeMappings.put("starttime", Date.class);
            searchTypeMappings.put("url", String.class);
            
		}
		
		return searchTypeMappings;
	}
	
	/* (non-Javadoc)
	 * @see org.iplantc.service.common.search.AgaveResourceSearchFilter#strongTypeSearchValue(java.lang.Class, java.lang.String, java.lang.String)
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public Object strongTypeSearchValue(Class searchTermType, String searchField, String searchValue) 
	throws IllegalArgumentException
	{
	    if (searchTermType == NotificationCallbackProviderType.class) {
            try {
                return NotificationCallbackProviderType.valueOf(StringUtils.upperCase(searchValue));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Unknown notification status " + searchValue);
            }
        } else {
            return super.strongTypeSearchValue(searchTermType, searchField, searchValue);
        }
	}
}

