package org.iplantc.service.systems.search;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.common.search.AgaveResourceSearchFilter;
import org.iplantc.service.common.util.StringToTime;
import org.iplantc.service.systems.manager.SystemManager;
import org.iplantc.service.systems.model.RemoteSystem;
import org.iplantc.service.systems.model.enumerations.AuthConfigType;
import org.iplantc.service.systems.model.enumerations.ExecutionType;
import org.iplantc.service.systems.model.enumerations.LoginProtocolType;
import org.iplantc.service.systems.model.enumerations.RemoteSystemType;
import org.iplantc.service.systems.model.enumerations.SchedulerType;
import org.iplantc.service.systems.model.enumerations.StorageProtocolType;
import org.iplantc.service.systems.model.enumerations.SystemStatusType;
import org.joda.time.DateTime;

/**
 * This class is the basis for search support across the API.
 * Each service should implement this class to filter query parameters
 * from the url into valid hibernate query.
 * 
 * @author dooley
 *
 */
public class SystemSearchFilter extends AgaveResourceSearchFilter
{
	private static HashMap<String, String> searchTermMappings = new HashMap<String,String>();
	
	@SuppressWarnings("rawtypes")
	private static HashMap<String, Class> searchTypeMappings = new HashMap<String,Class>();
	
	public SystemSearchFilter() {}

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
            searchTermMappings.put("globaldefault", "%sglobalDefault");
            searchTermMappings.put("default", "%ssystemId");
            searchTermMappings.put("available", "%savailable");
            searchTermMappings.put("created", "%screated");
            searchTermMappings.put("description", "%sdescription");
            searchTermMappings.put("id", "%ssystemId");
            searchTermMappings.put("lastupdated", "%slastUpdated");
            searchTermMappings.put("lastmodified", "%slastUpdated");
            searchTermMappings.put("name", "%sname");
            searchTermMappings.put("owner", "%sowner");
            searchTermMappings.put("public", "%spubliclyAvailable");
            searchTermMappings.put("site", "%ssite");
            searchTermMappings.put("status", "%sstatus");
            searchTermMappings.put("storage.zone", "%sstorageConfig.zone");
            searchTermMappings.put("storage.resource", "%sstorageConfig.resource");
            searchTermMappings.put("storage.bucket", "%sstorageConfig.bucket");
            searchTermMappings.put("storage.host", "%sstorageConfig.host");
            searchTermMappings.put("storage.port", "%sstorageConfig.port");
            searchTermMappings.put("storage.homedir", "%sstorageConfig.homeDir");
            searchTermMappings.put("storage.rootdir", "%sstorageConfig.rootDir");
            searchTermMappings.put("storage.protocol", "%sstorageConfig.protocol");
            searchTermMappings.put("storage.proxy.name", "%sstorageConfig.proxyServer.name");
            searchTermMappings.put("storage.proxy.host", "%sstorageConfig.proxyServer.host");
            searchTermMappings.put("storage.proxy.port", "%sstorageConfig.proxyServer.port");
            searchTermMappings.put("type", "%stype");
            searchTermMappings.put("login.host", "loginConfig.host");
            searchTermMappings.put("login.port", "loginConfig.port");
            searchTermMappings.put("login.protocol", "loginConfig.protocol");
            searchTermMappings.put("login.proxy.name", "loginConfig.proxyServer.name");
            searchTermMappings.put("login.proxy.host", "loginConfig.proxyServer.host");
            searchTermMappings.put("login.proxy.port", "loginConfig.proxyServer.port");
            searchTermMappings.put("workdir", "%sworkDir");
            searchTermMappings.put("scratchdir", "%sscratchDir");
            searchTermMappings.put("maxsystemjobs", "%smaxSystemJobs");
            searchTermMappings.put("maxsystemjobsperuser", "%smaxSystemJobsPerUser");
            searchTermMappings.put("startupscript", "%sstartupScript");
            searchTermMappings.put("executiontype", "%sexecutionType");
            searchTermMappings.put("environment", "%senvironment");
            searchTermMappings.put("scheduler", "%sscheduler");
            searchTermMappings.put("queues.default", "queue.systemDefault");
            searchTermMappings.put("queues.customdirectives", "queue.customDirectives");
            searchTermMappings.put("queues.maxjobs", "queue.maxJobs");
            searchTermMappings.put("queues.maxmemoryperjob", "queue.maxMemoryPerNode");
            searchTermMappings.put("queues.maxnodes", "queue.maxNodes");
            searchTermMappings.put("queues.maxprocessorspernode", "queue.maxProcessorsPerNode");
            searchTermMappings.put("queues.maxrequestedtime", "time_to_sec(queue.maxRequestedTime)");
            searchTermMappings.put("queues.maxuserjobs", "queue.maxUserJobs");
            searchTermMappings.put("queues.name", "queue.name");
            searchTermMappings.put("uuid", "%suuid");
		}
		
		return searchTermMappings;
	}
	
	@SuppressWarnings("rawtypes")
	@Override
    public Map<String, Class> getSearchTypeMappings()
	{
	    if (searchTypeMappings.isEmpty()) {
            searchTypeMappings.put("globaldefault", Boolean.class);
            searchTypeMappings.put("default", Boolean.class);
            searchTypeMappings.put("available", Boolean.class);
            searchTypeMappings.put("description", String.class);
            searchTypeMappings.put("id", String.class);
            searchTypeMappings.put("name", String.class);
            searchTypeMappings.put("owner", String.class);
            searchTypeMappings.put("public", Boolean.class);
            searchTypeMappings.put("site", String.class);
            searchTypeMappings.put("status", SystemStatusType.class);
            searchTypeMappings.put("storage.zone", String.class);
            searchTypeMappings.put("storage.resource", String.class);
            searchTypeMappings.put("storage.bucket", String.class);
            searchTypeMappings.put("storage.host", String.class);
            searchTypeMappings.put("storage.port", Integer.class);
            searchTypeMappings.put("storage.homedir", String.class);
            searchTypeMappings.put("storage.rootdir", String.class);
            searchTypeMappings.put("storage.protocol", StorageProtocolType.class);
            searchTypeMappings.put("storage.proxy.name", String.class);
            searchTypeMappings.put("storage.proxy.host", String.class);
            searchTypeMappings.put("storage.proxy.port", Integer.class);
            searchTypeMappings.put("type", RemoteSystemType.class);
            searchTypeMappings.put("login.host", String.class);
            searchTypeMappings.put("login.port", Integer.class);
            searchTypeMappings.put("login.protocol", LoginProtocolType.class);
            searchTypeMappings.put("login.proxy.name", String.class);
            searchTypeMappings.put("login.proxy.host", String.class);
            searchTypeMappings.put("login.proxy.port", Integer.class);
            searchTypeMappings.put("workdir", String.class);
            searchTypeMappings.put("scratchdir", String.class);
            searchTypeMappings.put("maxsystemjobs", Integer.class);
            searchTypeMappings.put("maxsystemjobsperuser", Integer.class);
            searchTypeMappings.put("startupscript", String.class);
            searchTypeMappings.put("environment", String.class);
            searchTypeMappings.put("scheduler", SchedulerType.class);
            searchTypeMappings.put("executiontype", ExecutionType.class);
            searchTypeMappings.put("queues.default", Boolean.class);
            searchTypeMappings.put("queues.customdirectives", String.class);
            searchTypeMappings.put("queues.maxjobs", Long.class);
            searchTypeMappings.put("queues.maxmemoryperjob", Long.class);
            searchTypeMappings.put("queues.maxnodes", Long.class);
            searchTypeMappings.put("queues.maxprocessorspernode", Long.class);
            searchTypeMappings.put("queues.maxrequestedtime", String.class);
            searchTypeMappings.put("queues.maxuserjobs", Long.class);
            searchTypeMappings.put("queues.name", String.class);
            searchTypeMappings.put("created", Date.class);
            searchTypeMappings.put("lastupdated", Date.class);
            searchTypeMappings.put("lastmodified", Date.class);
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
        } else if (searchTermType == SystemStatusType.class) {
            try {
                return SystemStatusType.valueOf(StringUtils.upperCase(searchValue));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Unknown system status " + searchValue);
            }
        } else if (searchTermType == RemoteSystemType.class) {
            try {
                return RemoteSystemType.valueOf(StringUtils.upperCase(searchValue));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Unknown system type " + searchValue);
            }
        } else if (searchTermType == LoginProtocolType.class) {
            try {
                return LoginProtocolType.valueOf(StringUtils.upperCase(searchValue));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Unknown system login protocol " + searchValue);
            }
        } else if (searchTermType == StorageProtocolType.class) {
            try {
                return StorageProtocolType.valueOf(StringUtils.upperCase(searchValue));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Unknown system storage protocol " + searchValue);
            }
        } else if (searchTermType == ExecutionType.class) {
            try {
                return ExecutionType.valueOf(StringUtils.upperCase(searchValue));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Unknown system execution type " + searchValue);
            }
        } else if (searchTermType == SchedulerType.class) {
            try {
                return SchedulerType.valueOf(StringUtils.upperCase(searchValue));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Unknown system scheduler type " + searchValue);
            }
        } else if (searchTermType == AuthConfigType.class) {
            try {
                return AuthConfigType.valueOf(StringUtils.upperCase(searchValue));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Unknown system authentication type " + searchValue);
            }
        } else if (StringUtils.startsWithIgnoreCase(searchField, "queue.maxrequestedtime")) {
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
        return "s.";
    }
}

