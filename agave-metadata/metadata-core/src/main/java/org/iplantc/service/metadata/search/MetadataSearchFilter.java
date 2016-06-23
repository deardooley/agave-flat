package org.iplantc.service.metadata.search;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.iplantc.service.common.exceptions.SearchSyntaxException;
import org.iplantc.service.common.search.AgaveResourceSearchFilter;
import org.iplantc.service.common.search.SearchTerm;
import org.iplantc.service.common.util.StringToTime;
import org.joda.time.DateTime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.DBObject;
import com.mongodb.util.JSON;
import com.mongodb.util.JSONParseException;

/**
 * This class is the basis for search support across the API.
 * Each service shold implement this class to filter query parameters
 * from the url into valid hibernate query.
 * 
 * @author dooley
 *
 */
public class MetadataSearchFilter extends AgaveResourceSearchFilter
{
    /* (non-Javadoc)
     * @see org.iplantc.service.common.search.AgaveResourceSearchFilter#filterCriteria(java.util.Map)
     */
    @Override
    public Map<SearchTerm, Object> filterCriteria(Map<String, String> searchCriteria)
    throws IllegalArgumentException 
    {
        Map<SearchTerm, Object> filteredCriteria = new HashMap<SearchTerm, Object>();
        
        // if the user specified a freeform query, exclude all others.
        if (searchCriteria.containsKey("q")) {
            SearchTerm term = filterAttributeName("q.eq");
            Object value = filterAttributValue(term, searchCriteria.get("q"));
            filteredCriteria.put(term, value);
        }
        else if (searchCriteria.containsKey("Q")) {
            SearchTerm term = filterAttributeName("q.eq");
            Object value = filterAttributValue(term, searchCriteria.get("Q"));
            filteredCriteria.put(term, value);
        }
        // otherwise, filter for known fields as well as value.* queries against contents of
        // the metadata content.
        else {
            for (String criteria: searchCriteria.keySet()) {
                SearchTerm term = filterAttributeName(criteria);
                if (term != null) {
                    Object value = filterAttributValue(term, searchCriteria.get(criteria));
                    filteredCriteria.put(term, value);
                }
            }
        }
        
        return filteredCriteria;
    }
    
    public MetadataSearchFilter() {}

	/* (non-Javadoc)
	 * @see org.iplantc.service.common.search.AgaveResourceSearchFilter#getSearchTermPrefix()
	 */
	@Override
	protected String getSearchTermPrefix() {
	    return "";
	}

    /* (non-Javadoc)
	 * @see org.iplantc.service.common.search.AgaveResourceSearchFilter#getSearchTermMappings()
	 */
	@Override
	public Map<String, String> getSearchTermMappings()
	{
	    if (searchTermMappings.isEmpty()) {
            searchTermMappings.put("q", "value");
            searchTermMappings.put("value", "value");
            searchTermMappings.put("name", "name");
            searchTermMappings.put("created", "created");
            searchTermMappings.put("internalUsername", "internalUsername");
            searchTermMappings.put("lastupdated", "lastUpdated");
            searchTermMappings.put("owner", "owner");
            searchTermMappings.put("uuid", "uuid");
            searchTermMappings.put("associatedIds", "associatedIds");
            searchTermMappings.put("available", "available");
            searchTermMappings.put("versioned", "versioned");
            
		}
		
		return searchTermMappings;

	}
	
	@SuppressWarnings("rawtypes")
	@Override
	public Map<String, Class> getSearchTypeMappings()
	{
	    if (searchTypeMappings.isEmpty()) {
	        searchTypeMappings.put("q", DBObject.class);
	        searchTypeMappings.put("value", JsonNode.class);
            searchTypeMappings.put("name", String.class);
            searchTypeMappings.put("created", Date.class);
            searchTypeMappings.put("internalUsername", String.class);
            searchTypeMappings.put("lastupdated", Date.class);
            searchTypeMappings.put("owner", String.class);
            searchTypeMappings.put("uuid", String.class);
            searchTypeMappings.put("associatedIds", String.class);
            searchTypeMappings.put("available", Boolean.class);
            searchTypeMappings.put("versioned", Boolean.class);
		}
		
		return searchTypeMappings;

	}
	
	/* (non-Javadoc)
	 * @see org.iplantc.service.common.search.AgaveResourceSearchFilter#strongTypeSearchValue(java.lang.Class, java.lang.String, java.lang.String)
	 */
	@Override
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
            return BooleanUtils.toBoolean(searchValue);
        } else if (searchTermType == DBObject.class) {
            try {
                return (DBObject)JSON.parse(searchValue);
            } catch (JSONParseException e) {
                throw new IllegalArgumentException("Malformed search value. "
                        + "Please specify a valid json object representing your "
                        + "search criteria " + searchValue, e);
            }
        } else if (searchTermType == JsonNode.class) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                return mapper.readTree(searchValue);
            } catch (IOException e) {
                throw new IllegalArgumentException("Unknown search value " + searchValue, e);
            }
        } else {
            return searchValue;
        }
    }
}

