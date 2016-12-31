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
			searchTermMappings.put("appid", "%ssoftware_name");
			searchTermMappings.put("archive", "%sarchive_output");
			searchTermMappings.put("archivepath", "%sarchive_path");
			searchTermMappings.put("archivesystem", "a.system_id ");
			searchTermMappings.put("batchqueue", "%squeue_request");
			searchTermMappings.put("charge", "%scharge");
			searchTermMappings.put("created", "%screated");
			searchTermMappings.put("endtime", "%send_time");
			searchTermMappings.put("errormessage", "%serror_message");
			searchTermMappings.put("executionsystem", "%sexecution_system");
			searchTermMappings.put("id", "%suuid");
			searchTermMappings.put("inputs", "%sinputs");
			searchTermMappings.put("lastmodified", "%slast_updated");
			searchTermMappings.put("lastupdated", "%slast_updated");
			searchTermMappings.put("localid", "%sscheduler_job_id");
			searchTermMappings.put("maxruntime", "time_to_sec(%srequested_time)");
			searchTermMappings.put("memorypernode", "%smemory_request");
			searchTermMappings.put("name", "%sname");
			searchTermMappings.put("nodecount", "%snode_count");
			searchTermMappings.put("outputpath", "%soutput_path");
			searchTermMappings.put("owner", "%sowner");
			searchTermMappings.put("parameters", "%sparameters");
			searchTermMappings.put("processorspernode", "%sprocessor_count");
			searchTermMappings.put("retries", "%sretries");
			searchTermMappings.put("runtime", "%sstart_time is not null and (unix_timestamp(DATE(DATE_FORMAT(%send_time,'%Y-%m-%d %H:%i:%s'))) - unix_timestamp(DATE(DATE_FORMAT(%sstart_time,'%Y-%m-%d %H:%i:%s'))))");
			searchTermMappings.put("starttime", "%sstart_time");
			searchTermMappings.put("status", "%sstatus");
			searchTermMappings.put("submittime", "%ssubmit_time");
			searchTermMappings.put("visible", "%svisible");
			searchTermMappings.put("walltime", "abs(unix_timestamp(DATE(DATE_FORMAT(%send_time,'%Y-%m-%d %H:%i:%s'))) - unix_timestamp(DATE(DATE_FORMAT(%screated,'%Y-%m-%d %H:%i:%s'))))");
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
			searchTypeMappings.put("charge", Double.class);
			searchTypeMappings.put("endtime", Date.class);
			searchTypeMappings.put("errormessage", String.class);
			searchTypeMappings.put("executionsystem", String.class);
			searchTypeMappings.put("id", String.class);
			searchTypeMappings.put("inputs", String.class);
			searchTypeMappings.put("lastmodified", Date.class);
			searchTypeMappings.put("lastupdated", Date.class);
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

