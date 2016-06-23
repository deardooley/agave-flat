package org.iplantc.service.common.discovery;

import org.codehaus.plexus.util.StringUtils;

public interface ServiceCapability
{
	/**
	 * @return the id
	 */
	public abstract Long getId();

	/**
	 * @param id the id to set
	 */
	public abstract void setId(Long id);

	/**
	 * @return the tenantCode
	 */
	public abstract String getTenantCode();

	/**
	 * @param tenantCode the tenantCode to set
	 */
	public abstract void setTenantCode(String tenantCode);

	/**
	 * @return the apiName
	 */
	public abstract String getApiName();

	/**
	 * @param apiName the apiName to set
	 */
	public abstract void setApiName(String apiName);

	/**
	 * @return the activityType
	 */
	public abstract String getActivityType();

	/**
	 * @param activityType the activityType to set
	 */
	public abstract void setActivityType(String activityType);

	/**
	 * @return the username
	 */
	public abstract String getUsername();

	/**
	 * @param username the username to set
	 */
	public abstract void setUsername(String username);

	/**
	 * @return the groupName
	 */
	public abstract String getGroupName();

	/**
	 * @param groupName the groupName to set
	 */
	public abstract void setGroupName(String groupName);

	/**
	 * @return the definition
	 */
	public abstract String getDefinition();

	/**
	 * @param definition the definition to set
	 */
	public abstract void setDefinition(String definition);

	/**
	 * Whether the given {@link ServiceCapabilityImpl} allows the
	 * given behavior either through wildcard or exact matching.
	 * 
	 * @param {@link ServiceCapabilityImpl} 
	 * @return true if the this {@link ServiceCapabilityImpl} in any 
	 * way matches the provided {@link ServiceCapabilityImpl}
	 */
	public abstract boolean allows(ServiceCapability o);

	/**
	 * Whether the given {@link ServiceCapabilityImpl} is invalidated
	 * by this {@link ServiceCapabilityImpl}. This differs from the 
	 * {@link ServiceCapabilityImpl#allows(ServiceCapabilityImpl)} method in
	 * that this only returns true if an explicit invalidation 
	 * field is found. 
	 * 
	 * a.invalidates(b) does not imply a.allows(b)
	 * 
	 * @param {@link ServiceCapabilityImpl} 
	 * @return true if the this {@link ServiceCapabilityImpl} in any 
	 * way matches the provided {@link ServiceCapabilityImpl}
	 */
	public abstract boolean invalidates(ServiceCapability o);

	/**
     * @return the executionSystemId
     */
    public String getExecutionSystemId();

    /**
     * @param executionSystemId the executionSystemId to set
     */
    public void setExecutionSystemId(String executionSystemId);
    
    /**
     * @return the sourceSystemId
     */
    public String getSourceSystemId();

    /**
     * @param sourceSystemId the sourceSystemId to set
     */
    public void setSourceSystemId(String sourceSystemId);
    
    /**
     * @return the destSystemId
     */
    public String getDestSystemId();

    /**
     * @param sourceSystemId the destSystemId to set
     */
    public void setDestSystemId(String sourceSystemId);

    /**
     * @return the batchQueueName
     */
    public String getBatchQueueName();

    /**
     * @param batchQueueName the batchQueueName to set
     */
    public void setBatchQueueName(String batchQueueName);

}