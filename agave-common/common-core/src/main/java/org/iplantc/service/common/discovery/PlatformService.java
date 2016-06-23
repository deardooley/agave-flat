package org.iplantc.service.common.discovery;

import java.util.Date;
import java.util.List;

public interface PlatformService<T extends ServiceCapability>
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
	 * Descriptive name of the tenant
	 * @return
	 */
	public abstract String getName();

	/**
	 * @param name
	 */
	public abstract void setName(String name);

	/**
	 * @return the capabilities
	 */
	public abstract List<T> getCapabilities();

	/**
	 * @param type the capabilities to set
	 */
	public abstract void setCapabilities(List<T> capabilities);
	
	/**
	 * Returns true if this {@link DiscoverableService} has invalidates the
	 * passed in {@link ServiceCapabilityImpl}. This occurs when a {@link ServiceCapabilityImpl}
	 * has a {@link ServiceCapabilityImpl} where one of the tokens is prefixed by an
	 * underscore, thereby indicating that only {@link DiscoverableService}s with
	 * the same value or value after the underscore can process tasks matching the
	 * that {@link ServiceCapabilityImpl}.
	 * 
	 * @param {@link ServiceCapabilityImpl} the capability to check
	 * @return true if any this {@link DiscoverableService}'s {@link ServiceCapabilityImpl} 
	 * invalidate the provided {@link ServiceCapabilityImpl}. 
	 */
	public abstract boolean hasExclusivity(T capability);
	
	/**
	 * @return the host
	 */
	public abstract String getHost();

	/**
	 * @param host the host to set
	 */
	public abstract void setHost(String host);

	/**
	 * @return the port
	 */
	public abstract Integer getPort();

	/**
	 * @param port the port to set
	 */
	public abstract void setPort(Integer port);

	/**
	 * @return the uuid
	 */
	public abstract String getUuid();

	/**
	 * @param uuid the uuid to set
	 */
	public abstract void setUuid(String uuid);

	/**
	 * @return the lastUpdated
	 */
	public abstract Date getLastUpdated();

	/**
	 * @param lastUpdated the lastUpdated to set
	 */
	public abstract void setLastUpdated(Date lastUpdated);

	/**
	 * @return the created
	 */
	public abstract Date getCreated();

	/**
	 * @param created the created to set
	 */
	public abstract void setCreated(Date created);

	public abstract String toString();

	public abstract String toJSON();

}