package org.iplantc.service.systems.search;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.iplantc.service.common.search.AgaveResourceSearchFilter;
import org.iplantc.service.common.util.StringToTime;
import org.joda.time.DateTime;

/**
 * This class is the basis for search support across the API.
 * Each service should implement this class to filter query parameters
 * from the url into valid hibernate query.
 * 
 * @author dooley
 *
 */
public class BatchQueueSearchFilter extends AgaveResourceSearchFilter
{
	private static HashMap<String, String> searchTermMappings = new HashMap<String,String>();
	
	@SuppressWarnings("rawtypes")
	private static HashMap<String, Class> searchTypeMappings = new HashMap<String,Class>();
	
	public BatchQueueSearchFilter() {}

	public Set<String> getSearchParameters()
	{
		return getSearchTermMappings().keySet();
	}
	
	/* (non-Javadoc)
	 * @see org.iplantc.service.common.search.AgaveResourceSearchFilter#getSearchTermMappings()
	 */
	@Override
	public Map<String, String> getSearchTermMappings()
	{
	    if (searchTermMappings.isEmpty()) {
	        
            searchTermMappings.put("created", "%screated");
            searchTermMappings.put("customdirectives", "%scustomDirectives");
            searchTermMappings.put("default", "%ssystemDefault");
            searchTermMappings.put("description", "%sdescription");
            searchTermMappings.put("lastupdated", "%slastUpdated");
            searchTermMappings.put("mappedname", "%smappedName");
            searchTermMappings.put("maxjobs", "%smaxJobs");
            searchTermMappings.put("maxmemoryperjob", "%smaxMemoryPerNode");
            searchTermMappings.put("maxnodes", "%smaxNodes");
            searchTermMappings.put("maxprocessorspernode", "%smaxProcessorsPerNode");
            searchTermMappings.put("maxuserjobs", "%smaxUserJobs");
            searchTermMappings.put("maxrequestedtime", "time_to_sec(%smaxRequestedTime)");
            searchTermMappings.put("name", "%sname");
            searchTermMappings.put("uuid", "%suuid");
		}
		
		return searchTermMappings;
	}
	
	@SuppressWarnings("rawtypes")
	@Override
    public Map<String, Class> getSearchTypeMappings()
	{
	    if (searchTypeMappings.isEmpty()) {
	        searchTypeMappings.put("created", Date.class);
	        searchTypeMappings.put("default", Boolean.class);
	        searchTypeMappings.put("description", String.class);
	        searchTypeMappings.put("customdirectives", String.class);
            searchTypeMappings.put("lastupdated", Date.class);
            searchTypeMappings.put("mappedname", String.class);
            searchTypeMappings.put("maxjobs", Long.class);
            searchTypeMappings.put("maxmemoryperjob", Long.class);
            searchTypeMappings.put("maxnodes", Long.class);
            searchTypeMappings.put("maxprocessorspernode", Long.class);
            searchTypeMappings.put("maxrequestedtime", String.class);
            searchTypeMappings.put("maxuserjobs", Long.class);
            searchTypeMappings.put("name", String.class);
            searchTypeMappings.put("uuid", String.class);
		}
		
		return searchTypeMappings;

	}
	
	/**
	 * Validates an individual search value against the type defined by the field mapping.
	 *  
	 * @param searchTermType
	 * @param searchField
	 * @param searchValue
	 * @return 
	 * @throws IllegalArgumentException
	 */
	public Object strongTypeSearchValue(Class searchTermType, String searchField, String searchValue) 
	throws IllegalArgumentException
	{
	    if (searchTermType == Date.class) {
            Object time = StringToTime.date(searchValue);
            if (Boolean.FALSE.equals(time)) {
                if (NumberUtils.isDigits(searchValue)) {
                    try {
                        DateTime dateTime = new DateTime(Long.valueOf(searchValue));
                        return dateTime.toDate();
                    } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException("Illegal date format for " + searchField);
                    }
                } else {
                    try {
                        DateTime dateTime = new DateTime(searchValue);
                        return dateTime.toDate();
                    } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException("Illegal date format for " + searchField);
                    }
                }
            } else {
                return time;
            }
        } else if (searchTermType == Long.class) {
            if (NumberUtils.isNumber(searchValue)) {
                return NumberUtils.toLong(searchValue);
            } else {
                throw new IllegalArgumentException("Illegal integer value for " + searchField);
            }
        } else if (searchTermType == Integer.class) {
            if (NumberUtils.isNumber(searchValue)) {
                return NumberUtils.toInt(searchValue);
            } else {
                throw new IllegalArgumentException("Illegal integer value for " + searchField);
            }
        } else if (searchTermType == Double.class) {
            if (NumberUtils.isNumber(searchValue)) {
                return NumberUtils.toDouble(searchValue);
            } else {
                throw new IllegalArgumentException("Illegal decimal value for " + searchField);
            }
        } else if (searchTermType == Boolean.class) {
        	if (NumberUtils.isNumber(searchValue)) {
            	return NumberUtils.toInt(searchValue, 0) == 1;
            } 
            else {
            	return BooleanUtils.toBoolean(searchValue);
            }
        } else if (StringUtils.startsWithIgnoreCase(searchField, "maxrequestedtime")) {
            if (NumberUtils.isNumber(searchValue)) {
                return NumberUtils.toInt(searchValue, 0);
            } else {
                String[] hms = StringUtils.split(searchValue, ":");
                int secs = 0;
                if (hms != null) 
                {
                    if (hms.length > 2) {
                        secs += NumberUtils.toInt(hms[2], 0) * 3600;
                    }
                    
                    if (hms.length > 1) {
                        secs += NumberUtils.toInt(hms[1], 0) * 60;
                    }
                
                    secs += NumberUtils.toInt(hms[0], 0);
                }
                return secs;
            }
        } else {
            return searchValue;
        }
	}

    @Override
    protected String getSearchTermPrefix() {
        return "q.";
    }
}

