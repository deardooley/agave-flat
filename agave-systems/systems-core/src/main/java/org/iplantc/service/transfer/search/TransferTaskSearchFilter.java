package org.iplantc.service.transfer.search;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.iplantc.service.common.search.AgaveResourceSearchFilter;
import org.iplantc.service.common.util.StringToTime;
import org.iplantc.service.transfer.model.enumerations.TransferStatusType;
import org.joda.time.DateTime;

/**
 * This class is the basis for search support across the API.
 * Each service should implement this class to filter query parameters
 * from the url into valid hibernate query.
 * 
 * @author dooley
 *
 */
public class TransferTaskSearchFilter extends AgaveResourceSearchFilter
{
	private static HashMap<String, String> searchTermMappings = new HashMap<String,String>();
	
	@SuppressWarnings("rawtypes")
	private static HashMap<String, Class> searchTypeMappings = new HashMap<String,Class>();
	
	public TransferTaskSearchFilter() {}

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
//          searchTermMappings.put("protocol", String.class);
            searchTermMappings.put("parenttask", "%sparentTask.uuid");
            searchTermMappings.put("roottask", "%srootTask.uuid");
            searchTermMappings.put("created",  "%screated");
            searchTermMappings.put("starttime", "%sstartTime");
            searchTermMappings.put("endtime", "%sendTime");
            searchTermMappings.put("id", "%suuid");
            searchTermMappings.put("source", "%ssource");
            searchTermMappings.put("dest", "%sdest");
            searchTermMappings.put("bytestransferred", "%sbytesTransferred");
            searchTermMappings.put("totalsize", "%stotalSize");
            searchTermMappings.put("owner", "%sowner");
            searchTermMappings.put("retries", "%sretries");
            searchTermMappings.put("transfertime", "%sstartTime is not null and (time_to_sec(%sendTime) - time_to_sec(%sstartTime))");
            searchTermMappings.put("status", "%sstatus");
            searchTermMappings.put("visible", "%svisible");
            searchTermMappings.put("walltime", "abs(time_to_sec(%sendTime) - time_to_sec(%screated))");
        }
		
		return searchTermMappings;
	}
	
	/* (non-Javadoc)
	 * @see org.iplantc.service.common.search.AgaveResourceSearchFilter#getSearchTypeMappings()
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public Map<String, Class> getSearchTypeMappings()
	{
		if (searchTypeMappings.isEmpty()) {
//			searchTypeMappings.put("protocol", String.class);
			searchTypeMappings.put("parenttask", String.class);
			searchTypeMappings.put("roottask", String.class);
			searchTypeMappings.put("created", Date.class);
			searchTypeMappings.put("starttime", Date.class);
            searchTypeMappings.put("endtime", Date.class);
			searchTypeMappings.put("id", String.class);
			searchTypeMappings.put("source", String.class);
			searchTypeMappings.put("dest", String.class);
			searchTypeMappings.put("bytestransferred", Long.class);
			searchTypeMappings.put("totalSize", Long.class);
			searchTypeMappings.put("owner", String.class);
			searchTypeMappings.put("retries", Integer.class);
			searchTypeMappings.put("transfertime", Long.class);
			searchTypeMappings.put("status", TransferStatusType.class);
			searchTypeMappings.put("visible", Boolean.class);
			searchTypeMappings.put("walltime", Integer.class);
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
        } else if (searchTermType == TransferStatusType.class) {
            try {
                return TransferStatusType.valueOf(StringUtils.upperCase(searchValue));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Unknown job status " + searchValue);
            }
        } else if (StringUtils.startsWithIgnoreCase(searchField, "maxruntime")) {
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

    /* (non-Javadoc)
     * @see org.iplantc.service.common.search.AgaveResourceSearchFilter#getSearchTermPrefix()
     */
    @Override
    protected String getSearchTermPrefix() {
        return "t.";
    }
}

