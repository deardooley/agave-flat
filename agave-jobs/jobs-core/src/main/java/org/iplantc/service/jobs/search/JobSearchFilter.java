package org.iplantc.service.jobs.search;

import java.util.Date;
import java.util.Map;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.iplantc.service.common.search.AgaveResourceSearchFilter;
import org.iplantc.service.common.util.StringToTime;
import org.iplantc.service.jobs.model.enumerations.JobStatusType;
import org.joda.time.DateTime;

/**
 * This class is the basis for search support across the API.
 * Each service shold implement this class to filter query parameters
 * from the url into valid hibernate query.
 * 
 * @author dooley
 *
 */
public class JobSearchFilter extends AgaveResourceSearchFilter
{
	public JobSearchFilter() {}

	/* (non-Javadoc)
	 * @see org.iplantc.service.common.search.AgaveResourceSearchFilter#getSearchTermPrefix()
	 */
	@Override
	protected String getSearchTermPrefix() {
	    return "j.";
	}

    /* (non-Javadoc)
	 * @see org.iplantc.service.common.search.AgaveResourceSearchFilter#getSearchTermMappings()
	 */
	@Override
	public Map<String, String> getSearchTermMappings()
	{
		if (searchTermMappings.isEmpty()) {
			searchTermMappings.put("appid", "%ssoftwareName");
			searchTermMappings.put("archive", "%sarchiveOutput");
			searchTermMappings.put("archivepath", "%sarchivePath");
			searchTermMappings.put("archivesystem", "%sarchiveSystem.systemId");
			searchTermMappings.put("batchqueue", "%sbatchQueue");
			searchTermMappings.put("created", "%screated");
			searchTermMappings.put("endtime", "%sendTime");
			searchTermMappings.put("executionsystem", "%ssystem");
			searchTermMappings.put("id", "%suuid");
			searchTermMappings.put("inputs", "%sinputs");
			searchTermMappings.put("localid", "%slocalJobId");
			searchTermMappings.put("maxruntime", "time_to_sec(%smaxRunTime)");
			searchTermMappings.put("memorypernode", "%smemoryPerNode");
			searchTermMappings.put("name", "%sname");
			searchTermMappings.put("nodecount", "%snodeCount");
			searchTermMappings.put("outputpath", "%soutputPath");
			searchTermMappings.put("owner", "%sowner");
			searchTermMappings.put("parameters", "%sparameters");
			searchTermMappings.put("processorspernode", "%sprocessorsPerNode");
			searchTermMappings.put("retries", "%sretries");
			searchTermMappings.put("runtime", "%sstartTime is not null and (time_to_sec(%sendTime) - time_to_sec(%sstartTime))");
			searchTermMappings.put("starttime", "%sstartTime");
			searchTermMappings.put("status", "%sstatus");
			searchTermMappings.put("submittime", "%ssubmitTime");
			searchTermMappings.put("visible", "%svisible");
			searchTermMappings.put("walltime", "abs(time_to_sec(%sendTime) - time_to_sec(%screated))");
		}
		
		return searchTermMappings;

	}
	
	@SuppressWarnings("rawtypes")
	@Override
	public Map<String, Class> getSearchTypeMappings()
	{
		if (searchTypeMappings.isEmpty()) {
			searchTypeMappings.put("appid", String.class);
			searchTypeMappings.put("archive", Boolean.class);
			searchTypeMappings.put("archivepath", String.class);
			searchTypeMappings.put("archivesystem", String.class);
			searchTypeMappings.put("batchqueue", String.class);
			searchTypeMappings.put("created", Date.class);
			searchTypeMappings.put("endtime", Date.class);
			searchTypeMappings.put("executionsystem", String.class);
			searchTypeMappings.put("id", String.class);
			searchTypeMappings.put("inputs", String.class);
			searchTypeMappings.put("localid", String.class);
			searchTypeMappings.put("maxruntime", String.class);
			searchTypeMappings.put("memorypernode", Double.class);
			searchTypeMappings.put("name", String.class);
			searchTypeMappings.put("nodecount", Long.class);
			searchTypeMappings.put("outputpath", String.class);
			searchTypeMappings.put("owner", String.class);
			searchTypeMappings.put("parameters", String.class);
			searchTypeMappings.put("processorspernode", Long.class);
			searchTypeMappings.put("retries", Integer.class);
			searchTypeMappings.put("runtime", Long.class);
			searchTypeMappings.put("starttime", Date.class);
			searchTypeMappings.put("status", JobStatusType.class);
			searchTypeMappings.put("submittime", Date.class);
			searchTypeMappings.put("visible", Boolean.class);
			searchTypeMappings.put("walltime", Integer.class);
		}
		
		return searchTypeMappings;

	}
	
	/* (non-Javadoc)
	 * @see org.iplantc.service.common.search.AgaveResourceSearchFilter#strongTypeSearchValue(java.lang.Class, java.lang.String, java.lang.String)
	 */
	@Override
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
            return BooleanUtils.toBoolean(searchValue);
        } else if (searchTermType == JobStatusType.class) {
            try {
                return JobStatusType.valueOf(StringUtils.upperCase(searchValue));
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
}

