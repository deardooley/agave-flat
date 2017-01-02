package org.iplantc.service.common.search;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.iplantc.service.common.exceptions.SearchSyntaxException;
import org.iplantc.service.common.util.StringToTime;
import org.joda.time.DateTime;

public abstract class AgaveResourceSearchFilter {

    protected static HashMap<String, String> searchTermMappings = new HashMap<String,String>();
    @SuppressWarnings("rawtypes")
    protected static HashMap<String, Class> searchTypeMappings = new HashMap<String,Class>();

    public AgaveResourceSearchFilter() {
        super();
    }

    public Set<String> getSearchParameters() {
    	return getSearchTermMappings().keySet();
    }

    /**
     * Returns the mapping of query terms to entity fields for
     * use in the generated query.
     * 
     * @return mapped field name
     */
    public abstract Map<String, String> getSearchTermMappings();
    
    /**
     * Returns the mapping of query terms to entity fields types for
     * use in the generated query.
     * 
     * @return mapped field class
     */
    public abstract Map<String, Class> getSearchTypeMappings();
    
    /**
     * Returns the query prefix for the entity for use in the generated query.
     * @return query term prefix, ex. "j."
     */
    protected abstract String getSearchTermPrefix();
    
    /**
     * Transforms a specific, user-supplied search term into the appropriate
     * {@link SearchTerm} object.
     * @param name
     * @return
     */
    public SearchTerm filterAttributeName(String attributeName) 
    { 
        SearchTerm searchTerm = null;
        
        try {
            searchTerm = new SearchTerm(attributeName.toLowerCase(), getSearchTermPrefix());
            searchTerm.setMappedField(getSearchTermMappings().get(searchTerm.getSearchField()));
            
            // adjust data queries to make LT and GT equivalent to BEFORE and AFTER
            if (getSearchTypeMappings().get(searchTerm.getSearchField()) == Date.class) {
            	if (searchTerm.getOperator() == SearchTerm.Operator.EQ) {
                	searchTerm.setOperator(SearchTerm.Operator.BETWEEN);
                }
                else if (searchTerm.getOperator() == SearchTerm.Operator.LT) {
                    searchTerm.setOperator(SearchTerm.Operator.BEFORE);
                }
                else if (searchTerm.getOperator() == SearchTerm.Operator.GT) {
                    searchTerm.setOperator(SearchTerm.Operator.AFTER);
                }
            }
        } catch (SearchSyntaxException e) {
            return null;
        }
        
        return searchTerm;
    }
    
    /**
     * Filters the provided search criteria terms by adjusting for case
     * sensitivity and removing those that do not apply to the search object.
     * 
     * @param searchCriteria
     * @return Filtered map with search criteria replaced by SearchTerm.
     */
    public Map<SearchTerm, Object> filterCriteria(Map<String, String> searchCriteria) throws IllegalArgumentException {
    	Map<SearchTerm, Object> filteredCriteria = new HashMap<SearchTerm, Object>();
    	
    	for (String criteria: searchCriteria.keySet()) {
    		SearchTerm term = filterAttributeName(criteria);
    		if (term != null) {
    			Object value = filterAttributValue(term, searchCriteria.get(criteria));
    			filteredCriteria.put(term, value);
    		}
    	}
    	
    	return filteredCriteria;
    }

    /**
     * Converts the search value given in a url query to the proper
     * object type for the hibernate query.
     * 
     * @param attributeName
     * @param searchValue
     * @return
     * @throws IllegalArgumentException
     */
    public Object filterAttributValue(SearchTerm searchTerm, String searchValue) throws IllegalArgumentException {
    	if (searchTerm == null) return null;
    	
    	@SuppressWarnings("rawtypes")
    	Class searchTermType = getSearchTypeMappings().get(searchTerm.getSearchField());
    	String[] splitValues = StringUtils.split(searchValue, ",");
    	
    	 
    	if (searchTerm.getOperator().isSetOperator() && searchTermType == Date.class) 
    	{
    	    if (searchTerm.getOperator() != SearchTerm.Operator.BETWEEN) {
    	        throw new IllegalArgumentException("Illegal use of IN operation for " + searchTerm.getSearchField() + 
                        ". To query multiple dates, plese specify a comma separated date range consisting of a start and end date/time expression.");
    	    } else if (splitValues.length == 0) {
    	        throw new IllegalArgumentException("Illegal date range format for " + searchTerm.getSearchField() + 
    	                ". Please specify a comma separated date range consisting of a start and end date/time expression.");
    	    }
    	    // if a single date value is given to a BETWEEN operation, we treat like an equality operation 
    	    // since precision is only guaranteed to the second. This is essentially the same as an EQ operation 
    	    // as the result will be anything within the the given time rounded down to 0 milliseconds and rounded 
    	    // up to .999 milliseconds. This is the only safe way to hedge against db precision issues going forward
    	    // without changing all DateTime fields to timestamps.
    	    else if (splitValues.length == 1) {
//    	        throw new IllegalArgumentException("Illegal date range format for " + searchTerm.getSearchField() + 
//    	                ". Please specify an end date/time expression in the date range.");
    	    	List<Object> searchValues = new ArrayList<Object>();
    	    	Object strongTypeSearchValue = strongTypeSearchValue(searchTermType, searchTerm.getSearchField(), splitValues[0]);
    	    	searchValues.add(strongTypeSearchValue);
    	    	searchValues.add(strongTypeSearchValue);
    	    	
    	    	return searchValues;
    	    } else if (splitValues.length > 2) {
    	        throw new IllegalArgumentException("Illegal date range format for " + searchTerm.getSearchField() + 
    	                ". Please specify a comma separated date range consisting of a start and end date/time expression.");
    	    } else {
    	        List<Object> searchValues = new ArrayList<Object>();
                for (String splitValue: splitValues)
                {
                    searchValues.add(strongTypeSearchValue(searchTermType, searchTerm.getSearchField(), splitValue));
                }
                return searchValues;
    	    }
    	}
    	else if (searchTerm.getOperator().isSetOperator() && splitValues.length > 1) 
    	{
    	    List<Object> searchValues = new ArrayList<Object>();
    		for (String splitValue: splitValues)
    		{
    			searchValues.add(strongTypeSearchValue(searchTermType, searchTerm.getSearchField(), splitValue));
    		}
    		return searchValues;
    	}
    	else if (searchTerm.getOperator().isSetOperator() && splitValues.length == 1) 
        {
    	    List<Object> searchValues = new ArrayList<Object>();
            searchValues.add(strongTypeSearchValue(searchTermType, searchTerm.getSearchField(), splitValues[0]));
            return searchValues; 
        }
    	else if (searchTermType == Boolean.class && !searchTerm.getOperator().isEqualityOperator())
    	{
    	    throw new IllegalArgumentException(String.format("Illegal search term operator for %s. " 
                    + "Boolean search terms may only be compared by equality. "
                    + "Please specify one of %s or %s.",
                    searchTerm.getSearchField(),
                    searchTerm.getSearchField() + ".EQ",
                    searchTerm.getSearchField() + ".NEQ"));
    	}
    	else
    	{
    	    return strongTypeSearchValue(searchTermType, searchTerm.getSearchField(), searchValue);
    	}
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
    throws IllegalArgumentException {
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
        } else {
            return searchValue;
        }
    }

}