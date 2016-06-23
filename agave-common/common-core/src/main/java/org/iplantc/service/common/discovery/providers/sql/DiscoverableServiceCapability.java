package org.iplantc.service.common.discovery.providers.sql;

import java.util.Arrays;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import org.codehaus.plexus.util.StringUtils;
import org.iplantc.service.common.discovery.ServiceCapability;

/**
 * When a container starts, it will contain zero or
 * more {@link DiscoverableService}s. Each {@link DiscoverableService} 
 * registers itself with the appropriate list of one or more
 * {@link ServiceCapabilityImpl}. In this way, the existence of all
 * {@link DiscoverableService} and their {@link ServiceCapabilityImpl} 
 * is known at all times and can be used to make proper load 
 * balancing and routing decisions within the work queues. 
 * 
 * A {@link ServiceCapabilityImpl} is simply a statment of the context 
 * are conceptually mapped to specific worker tasks within each
 * API. 
 * 
 * @author dooley
 *
 */
@Entity
@Table(name="servicecapabilities")
public class DiscoverableServiceCapability implements ServiceCapability
{	
	@Id
	@GeneratedValue
	@Column(name = "id", unique = true, nullable = false)
	private Long id;
	
	@Column(name = "tenant_id", nullable = true, unique = false, length = 64)
	private String tenantCode;
	
	@Column(name = "api_name", nullable = true, unique = false, length = 32)
	private String apiName;
	
	@Column(name = "activity_type", nullable = true, unique = false, length = 32)
	private String activityType;
	
	@Column(name = "username", nullable = true, unique = false, length = 64)
	private String username;
	
	@Column(name = "group_name", nullable = true, unique = false, length = 64)
	private String groupName;
	
	@Column(name = "execution_system", nullable = true, unique = false, length = 64)
    private String executionSystemId;
	
	@Column(name = "source_system", nullable = true, unique = false, length = 64)
    private String sourceSystemId;
	
	@Column(name = "dest_system", nullable = true, unique = false, length = 64)
    private String destSystemId;
	
	@Column(name = "batch_queue", nullable = true, unique = false, length = 64)
    private String batchQueueName;
	
	@Column(name = "definition", nullable = true, unique = false, columnDefinition = "text")
	private String definition;
	
	public DiscoverableServiceCapability(){}
	
	public DiscoverableServiceCapability(String capabilityString) 
	{	
		this();
		
		String[] tokens = StringUtils.split(capabilityString, "/");
		
		if (tokens.length > 0) {
			setTenantCode(tokens[0]);
		}
		
		if (tokens.length > 1) {
			setApiName(tokens[1]);
		}
		
		if (tokens.length > 2) {
			setActivityType(tokens[2]);
		}
		
		if (tokens.length > 3) {
			setGroupName(tokens[3]);
		}
		
		if (tokens.length > 4) {
			setUsername(tokens[4]);
		}
		
		if (tokens.length > 5) {
            setExecutionSystemId(tokens[5]);
        }
		
		if (tokens.length > 6) {
		    setBatchQueueName(tokens[6]);
        }
		
		if (tokens.length > 7) {
		    setSourceSystemId(tokens[7]);
        }
		
		if (tokens.length > 8) {
            setDestSystemId(tokens[8]);
        }
		
		if (tokens.length > 5) {
			setDefinition(StringUtils.join(Arrays.copyOfRange(tokens, 5, tokens.length - 1), "/"));
		}
	}

	
	/* (non-Javadoc)
	 * @see org.iplantc.service.common.discovery.ServiceCapabilityInterface#getId()
	 */
	@Override
	public Long getId()
	{
		return id;
	}


	/* (non-Javadoc)
	 * @see org.iplantc.service.common.discovery.ServiceCapabilityInterface#setId(java.lang.Long)
	 */
	@Override
	public void setId(Long id)
	{
		this.id = id;
	}


	/* (non-Javadoc)
	 * @see org.iplantc.service.common.discovery.ServiceCapabilityInterface#getTenantCode()
	 */
	@Override
	public String getTenantCode()
	{
		return StringUtils.isEmpty(tenantCode) ? "*" : tenantCode;
	}


	/* (non-Javadoc)
	 * @see org.iplantc.service.common.discovery.ServiceCapabilityInterface#setTenantCode(java.lang.String)
	 */
	@Override
	public void setTenantCode(String tenantCode)
	{
		this.tenantCode = StringUtils.isEmpty(tenantCode) ? "*" : tenantCode;
	}


	/* (non-Javadoc)
	 * @see org.iplantc.service.common.discovery.ServiceCapabilityInterface#getApiName()
	 */
	@Override
	public String getApiName()
	{
		return StringUtils.isEmpty(apiName) ? "*" : apiName;
	}


	/* (non-Javadoc)
	 * @see org.iplantc.service.common.discovery.ServiceCapabilityInterface#setApiName(java.lang.String)
	 */
	@Override
	public void setApiName(String apiName)
	{
		this.apiName = StringUtils.isEmpty(apiName) ? "*" : apiName;
	}


	/* (non-Javadoc)
	 * @see org.iplantc.service.common.discovery.ServiceCapabilityInterface#getActivityType()
	 */
	@Override
	public String getActivityType()
	{
		return StringUtils.isEmpty(activityType) ? "*" : activityType;
	}


	/* (non-Javadoc)
	 * @see org.iplantc.service.common.discovery.ServiceCapabilityInterface#setActivityType(java.lang.String)
	 */
	@Override
	public void setActivityType(String activityType)
	{
		this.activityType = StringUtils.isEmpty(activityType) ? "*" : activityType;
	}


	/* (non-Javadoc)
	 * @see org.iplantc.service.common.discovery.ServiceCapabilityInterface#getUsername()
	 */
	@Override
	public String getUsername()
	{
		return StringUtils.isEmpty(username) ? "*" : username;
	}


	/* (non-Javadoc)
	 * @see org.iplantc.service.common.discovery.ServiceCapabilityInterface#setUsername(java.lang.String)
	 */
	@Override
	public void setUsername(String username)
	{
		this.username = StringUtils.isEmpty(username) ? "*" : username;
	}


	/* (non-Javadoc)
	 * @see org.iplantc.service.common.discovery.ServiceCapabilityInterface#getGroupName()
	 */
	@Override
	public String getGroupName()
	{
		return StringUtils.isEmpty(groupName) ? "*" : groupName;
	}


	/* (non-Javadoc)
	 * @see org.iplantc.service.common.discovery.ServiceCapabilityInterface#setGroupName(java.lang.String)
	 */
	@Override
	public void setGroupName(String groupName)
	{
		this.groupName = StringUtils.isEmpty(groupName) ? "*" : groupName;
	}


	/* (non-Javadoc)
	 * @see org.iplantc.service.common.discovery.ServiceCapabilityInterface#getDefinition()
	 */
	@Override
	public String getDefinition()
	{
		return StringUtils.isEmpty(definition) ? "*" : definition;
	}


	/* (non-Javadoc)
	 * @see org.iplantc.service.common.discovery.ServiceCapabilityInterface#setDefinition(java.lang.String)
	 */
	@Override
	public void setDefinition(String definition)
	{
		
		this.definition = StringUtils.isEmpty(definition) ? "*" : definition;
	}

	/**
     * @return the executionSystemId
     */
	@Override
    public synchronized String getExecutionSystemId() {
        return executionSystemId;
    }

    /**
     * @param executionSystemId the executionSystemId to set
     */
	@Override
	public synchronized void setExecutionSystemId(String executionSystemId) {
        this.executionSystemId = StringUtils.isEmpty(executionSystemId) ? "*" : executionSystemId;
    }
	
	/**
     * @return the sourceSystemId
     */
    @Override
    public synchronized String getSourceSystemId() {
        return sourceSystemId;
    }

    /**
     * @param sourceSystemId the sourceSystemId to set
     */
    @Override
    public synchronized void setSourceSystemId(String sourceSystemId) {
        this.sourceSystemId = StringUtils.isEmpty(sourceSystemId) ? "*" : sourceSystemId;
    }
    
    /**
     * @return the destSystemId
     */
    @Override
    public synchronized String getDestSystemId() {
        return destSystemId;
    }

    /**
     * @param sourceSystemId the destSystemId to set
     */
    @Override
    public synchronized void setDestSystemId(String sourceSystemId) {
        this.destSystemId = StringUtils.isEmpty(destSystemId) ? "*" : destSystemId;
    }

    /**
     * @return the batchQueueName
     */
	@Override
	public synchronized String getBatchQueueName() {
        return batchQueueName;
    }

    /**
     * @param batchQueueName the batchQueueName to set
     */
	@Override
	public synchronized void setBatchQueueName(String batchQueueName) {
        this.batchQueueName = StringUtils.isEmpty(batchQueueName) ? "*" : batchQueueName;
    }

    
	/* (non-Javadoc)
	 * @see org.iplantc.service.common.discovery.ServiceCapabilityInterface#allows(org.iplantc.service.common.discovery.ServiceCapabilityInterface)
	 */
	@Override
	public boolean allows(ServiceCapability o)
	{
		if (supports(getTenantCode(), o.getTenantCode()) && 
				supports(getApiName(), o.getApiName()) &&
				supports(getActivityType(), o.getActivityType()) &&
				supports(getGroupName(), o.getGroupName()) &&
				supports(getUsername(), o.getUsername()) && 
				supports(getExecutionSystemId(), o.getExecutionSystemId()) && 
				supports(getBatchQueueName(), o.getBatchQueueName()) && 
				supports(getSourceSystemId(), o.getSourceSystemId()) && 
				supports(getDestSystemId(), o.getDestSystemId())) 
		{
			String[] tokens = getDefinition().split("/");
			String[] oTokens = o.getDefinition().split("/");
			if (tokens.length != oTokens.length) {
				return false;
			} else {
				for(int i=0; i<tokens.length; i++) {
					if (!supports(tokens[i], oTokens[i])) {
						return false;
					}
				}
				return true;
			}
		}
		
		return false;
	}
	
	/* (non-Javadoc)
	 * @see org.iplantc.service.common.discovery.ServiceCapabilityInterface#invalidates(org.iplantc.service.common.discovery.ServiceCapabilityInterface)
	 */
	@Override
	public boolean invalidates(ServiceCapability o)
	{
		if (negates(getTenantCode(), o.getTenantCode()) || 
		        negates(getApiName(), o.getApiName()) ||
		        negates(getActivityType(), o.getActivityType()) ||
		        negates(getGroupName(), o.getGroupName()) ||
		        negates(getUsername(), o.getUsername()) ||
                negates(getExecutionSystemId(), o.getExecutionSystemId()) || 
                negates(getBatchQueueName(), o.getBatchQueueName()) ||
                negates(getSourceSystemId(), o.getSourceSystemId()) || 
                negates(getDestSystemId(), o.getDestSystemId())) 
		{
			return true;
		} 
		else 
		{
			String[] tokens = getDefinition().split("/");
			String[] oTokens = o.getDefinition().split("/");
			if (tokens.length == oTokens.length) 
			{
				for(int i=0; i<tokens.length; i++) {
					if (negates(tokens[i], oTokens[i])) {
						return true;
					}
				}
			}
		}
		
		return false;
	}
	
	/**
	 * Values starting with an underscore are invalidators.
	 * If an invalidator is found, only values matching
	 * the string after the underscore are value.
	 * 
	 * @param val1
	 * @param val2
	 * @return true if the first value begins with an underscore 
	 * and the remaining substring does not match the second value. 
	 * false otherwise
	 */
	private boolean negates(String val1, String val2) 
	{	
		if (val1.startsWith("_")) {
			return (val1.substring(1).equals(val2));
		} else {
			return false;
		}
	}
		
	/**
	 * Compares two {@link ServiceCapabilityImpl} fields to determine whether
	 * the first value covers the same scope the second value 
	 * supports. Every field contains something different, however
	 * the determination uses a common vocabulary.
	 * 
	 * *: matches any value
	 * ^token: invalidates * as a match for this kind of token.
	 * token: only values with exact match are covered
	 * 
	 * The implication of this grammar is that all {@link ServiceCapabilityImpl} 
	 * on all matching {@link DiscoveryService}s must be checked before a 
	 * determination can be made about whether a particular {@link ServiceCapabilityImpl}
	 * is supported for a given {@link DiscoveryService}.
	 * 
	 * @param val1
	 * @param val2
	 * @return
	 */
	private boolean supports(String val1, String val2) 
	{
		// identical values match
		if (val1.equals(val2)) {
			return true;
		// values starting with _ are invalidators. if
		// the values, minus the preceding underscore
		// are the same, a match is present otherwise
		// no match
		} else if (val1.startsWith("^")) {
			
			return (val1.substring(1).equals(val2));
			
		// values starting with _ are invalidators. if
		// the values, minus the preceding underscore
		// are the same, a match is present otherwise
		// no match
		} else if (val2.startsWith("^")) {
			
			return (val2.substring(1).equals(val1));
		
		// neither is an invalidator. if the first 
		// value is a wildcard, it matches any other value
		} else if (val1.equals("*")) {
			return true; 
		
		// otherwise, this value is a specific
		// named value and only matches the same value.
		} else {
			return false;
		}
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return String.format("%s/%s/%s/%s/%s/%s/%/%/%/%", 
				getTenantCode(), 
				getApiName(),
				getActivityType(),
				getGroupName(),
				getUsername(),
				getExecutionSystemId(),
				getBatchQueueName(),
				getSourceSystemId(),
				getDestSystemId(),
				getDefinition());
	}
}