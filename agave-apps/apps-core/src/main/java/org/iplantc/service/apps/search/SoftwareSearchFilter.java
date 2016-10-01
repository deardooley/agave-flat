package org.iplantc.service.apps.search;

import java.util.Date;
import java.util.Map;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.iplantc.service.apps.model.enumerations.ParallelismType;
import org.iplantc.service.apps.model.enumerations.SoftwareParameterType;
import org.iplantc.service.common.search.AgaveResourceSearchFilter;
import org.iplantc.service.common.util.StringToTime;
import org.iplantc.service.systems.model.enumerations.ExecutionType;
import org.joda.time.DateTime;

/**
 * This class is the basis for search support across the API.
 * Each service shold implement this class to filter query parameters
 * from the url into valid hibernate query.
 * 
 * @author dooley
 *
 */
public class SoftwareSearchFilter extends AgaveResourceSearchFilter
{
    public SoftwareSearchFilter() {}

	/* (non-Javadoc)
	 * @see org.iplantc.service.common.search.AgaveResourceSearchFilter#getSearchTermPrefix()
	 */
	@Override
	protected String getSearchTermPrefix() {
	    return "s.";
	}

    /* (non-Javadoc)
	 * @see org.iplantc.service.common.search.AgaveResourceSearchFilter#getSearchTermMappings()
	 */
	@Override
	public Map<String, String> getSearchTermMappings()
	{
	    if (searchTermMappings.isEmpty()) {
            searchTermMappings.put("available", "%savailable");
            searchTermMappings.put("checkpointable", "%scheckpointable");
            searchTermMappings.put("checksum", "%schecksum");
            searchTermMappings.put("created", "%screated");
            searchTermMappings.put("defaultmaxruntime", "%sdefaultMaxRunTime");
            searchTermMappings.put("defaultmemorypernode", "%sdefaultMemoryPerNode");
            searchTermMappings.put("defaultnodes", "%sdefaultNodes");
            searchTermMappings.put("defaultprocessorspernode", "%sdefaultProcessorsPerNode");
            searchTermMappings.put("defaultqueue", "%sdefaultQueue");
            searchTermMappings.put("deploymentpath", "%sdeploymentPath");
            searchTermMappings.put("templatepath", "%sexecutablePath");
            searchTermMappings.put("executionsystem", "%sexecutionSystem.systemId");
            searchTermMappings.put("executiontype", "%sexecutionType");
            searchTermMappings.put("helpuri", "%shelpURI");
            searchTermMappings.put("icon", "%sicon");
            searchTermMappings.put("id",
                    "(case %spubliclyAvailable \n"
                    + "  when 1 then CONCAT(%sname,'-',%sversion, 'u', %srevisionCount)\n"
                    + "  when 0 then CONCAT(%sname,'-',%sversion) end) \n ");
            searchTermMappings.put("inputs.id", "input.key");
            searchTermMappings.put("label", "%slabel");
            searchTermMappings.put("lastupdated", "%slastUpdated");
            searchTermMappings.put("lastmodified", "%slastUpdated");
            searchTermMappings.put("longdescription", "%slongDescription");
            searchTermMappings.put("name", "%sname");
            searchTermMappings.put("outputs.id", "output.key");
            searchTermMappings.put("ontology", "%sontology");
            searchTermMappings.put("owner", "%sowner");
            searchTermMappings.put("parallelism", "%sparallelism");
            searchTermMappings.put("parameters.id", "parameter.key");
            searchTermMappings.put("parameters.type", "parameter.type");
            searchTermMappings.put("public", "%spubliclyAvailable");
            searchTermMappings.put("publiconly", "%spubliclyAvailable");
            searchTermMappings.put("privateonly", "%spubliclyAvailable");
            searchTermMappings.put("revision", "%srevisionCount");
            searchTermMappings.put("shortdescription", "%sshortDescription");
            searchTermMappings.put("storagesystem", "%sstorageSystem.systemId");
            searchTermMappings.put("tags", "%stags");
            searchTermMappings.put("testpath", "%stestPath");
            searchTermMappings.put("uuid", "%suuid");
            searchTermMappings.put("version", "%sversion");
		}
		
		return searchTermMappings;

	}
	
	@SuppressWarnings("rawtypes")
	@Override
	public Map<String, Class> getSearchTypeMappings()
	{
	    if (searchTypeMappings.isEmpty()) {
            searchTypeMappings.put("available", Boolean.class);
            searchTypeMappings.put("checkpointable", Boolean.class);
            searchTypeMappings.put("checksum", String.class);
            searchTypeMappings.put("created", Date.class);
            searchTypeMappings.put("defaultmaxruntime", String.class);
            searchTypeMappings.put("defaultmemorypernode", Double.class);
            searchTypeMappings.put("defaultnodes", Long.class);
            searchTypeMappings.put("defaultprocessorspernode", Long.class);
            searchTypeMappings.put("defaultqueue", String.class);
            searchTypeMappings.put("deploymentpath", String.class);
            searchTypeMappings.put("templatepath", String.class);
            searchTypeMappings.put("executionsystem", String.class);
            searchTypeMappings.put("executiontype", ExecutionType.class);
            searchTypeMappings.put("helpuri", String.class);
            searchTypeMappings.put("icon", String.class);
            searchTypeMappings.put("id", String.class);
            searchTypeMappings.put("inputs.id", String.class);
            searchTypeMappings.put("label", String.class);
            searchTypeMappings.put("lastupdated", Date.class);
            searchTypeMappings.put("lastmodified", Date.class);
            searchTypeMappings.put("longdescription", String.class);
            searchTypeMappings.put("modules", String.class);
            searchTypeMappings.put("name", String.class);
            searchTypeMappings.put("ontology", String.class);
            searchTypeMappings.put("outputs.id", String.class);
            searchTypeMappings.put("owner", String.class);
            searchTypeMappings.put("parallelism", ParallelismType.class);
            searchTypeMappings.put("parameters.id", String.class);
            searchTypeMappings.put("parameters.type", SoftwareParameterType.class);
            searchTypeMappings.put("public", Boolean.class);
            searchTypeMappings.put("publiconly", Boolean.class);
            searchTypeMappings.put("privateonly", Boolean.class);
            searchTypeMappings.put("revision", Integer.class);
            searchTypeMappings.put("shortdescription", String.class);
            searchTypeMappings.put("storagesystem", String.class);
            searchTypeMappings.put("tags", String.class);
            searchTypeMappings.put("tenantid", String.class);
            searchTypeMappings.put("testpath", String.class);
            searchTypeMappings.put("uuid", String.class);
            searchTypeMappings.put("version", String.class);
		}
		
		return searchTypeMappings;

	}
	
	/* (non-Javadoc)
	 * @see org.iplantc.service.common.search.AgaveResourceSearchFilter#strongTypeSearchValue(java.lang.Class, java.lang.String, java.lang.String)
	 */
	@Override
	public Object strongTypeSearchValue(Class searchTermType, String searchField, String searchValue)
    throws IllegalArgumentException {
	    if (searchField.startsWith("publiconly")) {
	        return Boolean.TRUE;
	    } else if (searchField.startsWith("privateonly")) {
	        return Boolean.FALSE;
	    }
	    
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
        } else if (searchTermType == ExecutionType.class) {
            try {
                return ExecutionType.valueOf(StringUtils.upperCase(searchValue));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Unknown execution type " + searchValue);
            }
        } else if (searchTermType == ParallelismType.class) {
            try {
                return ParallelismType.valueOf(StringUtils.upperCase(searchValue));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Unknown parallelism type " + searchValue);
            }
        } else if (searchTermType == SoftwareParameterType.class) {
            try {
                return SoftwareParameterType.valueOf(StringUtils.lowerCase(searchValue));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Unknown software parameter type " + searchValue);
            }
        } else {
            return searchValue;
        }
    }
}

