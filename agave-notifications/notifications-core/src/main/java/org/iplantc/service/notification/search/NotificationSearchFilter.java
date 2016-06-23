package org.iplantc.service.notification.search;

import java.util.Date;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.iplantc.service.common.search.AgaveResourceSearchFilter;
import org.iplantc.service.notification.model.enumerations.NotificationCallbackProviderType;
import org.iplantc.service.notification.model.enumerations.NotificationStatusType;
import org.iplantc.service.notification.model.enumerations.RetryStrategyType;

/**
 * This class is the basis for search support across the API.
 * Each service should implement this class to filter query parameters
 * from the url into valid hibernate query.
 * 
 * @author dooley
 *
 */
public class NotificationSearchFilter extends AgaveResourceSearchFilter
{
    public NotificationSearchFilter() {}

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
	    	searchTermMappings.put("associateduuid", "%sassociatedUuid");
	    	searchTermMappings.put("url", "%scallbackUrl");
            searchTermMappings.put("created", "%screated");
            searchTermMappings.put("event", "%sevent");
            searchTermMappings.put("id", "%suuid");
            searchTermMappings.put("lastupdated", "%slastUpdated");
            searchTermMappings.put("owner", "%sowner");
            searchTermMappings.put("persistent", "%spersistent");
            searchTermMappings.put("policy.retrystrategy", "%spolicy.retryStrategy");
            searchTermMappings.put("policy.retryrate", "%spolicy.retryRate");
            searchTermMappings.put("policy.retrylimit", "%spolicy.retryLimit");
            searchTermMappings.put("policy.retrydelay", "%spolicy.retryDelay");
            searchTermMappings.put("policy.saveonfailure", "%spolicy.saveOnFailure");
            searchTermMappings.put("status", "%sstatus");
            searchTermMappings.put("visible", "%svisible");
		}
		
		return searchTermMappings;

	}
	
	@SuppressWarnings("rawtypes")
	@Override
	public Map<String, Class> getSearchTypeMappings()
	{
	    if (searchTypeMappings.isEmpty()) {
	    	searchTypeMappings.put("associateduuid", String.class);
	    	searchTypeMappings.put("url", String.class);
            searchTypeMappings.put("created", Date.class);
            searchTypeMappings.put("event", String.class);
            searchTypeMappings.put("id", String.class);
            searchTypeMappings.put("lastupdated", Date.class);
            searchTypeMappings.put("owner", String.class);
            searchTypeMappings.put("persistent", Boolean.class);
            searchTypeMappings.put("provider", NotificationCallbackProviderType.class);
            searchTypeMappings.put("policy.retrystrategy", RetryStrategyType.class);
            searchTypeMappings.put("policy.retryrate", Integer.class);
            searchTypeMappings.put("policy.retrylimit", Integer.class);
            searchTypeMappings.put("policy.retrydelay", Integer.class);
            searchTypeMappings.put("policy.saveonfailure", Boolean.class);
            searchTypeMappings.put("status", NotificationStatusType.class);
            searchTypeMappings.put("visible", Boolean.class);
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
	    if (searchTermType == NotificationStatusType.class) {
            try {
                return NotificationStatusType.valueOf(StringUtils.upperCase(searchValue));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Unknown notification status " + searchValue);
            }
        } else if (searchTermType == RetryStrategyType.class) {
            try {
                return RetryStrategyType.valueOf(StringUtils.upperCase(searchValue));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Unknown notification retry strategy " + searchValue);
            }
        } else {
            return super.strongTypeSearchValue(searchTermType, searchField, searchValue);
        }
	}
}

