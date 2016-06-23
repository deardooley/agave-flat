/**
 * 
 */
package org.iplantc.service.common.discovery;

import org.iplantc.service.common.discovery.providers.sql.DiscoverableServiceCapability;
import org.testng.log4testng.Logger;

/**
 * Factorty to create {@link ServiceCapability} objects. This is needed
 * to align with the generic use within the {@link ServiceDiscoveryFactory}.
 * 
 * @author dooley
 *
 */
public class ServiceCapabilityFactory
{
	private Logger log = Logger.getLogger(ServiceCapabilityFactory.class);
	
	/**
	 * Returns an instance of a {@link ServiceCapability} that
	 * aligns with the {@link ServiceDiscoveryClient} returned 
	 * from the {@link ServiceDiscoveryFactory}.
	 * 
	 * @return
	 */
	public static ServiceCapability getInstance(String capabilityString) {	
		return new DiscoverableServiceCapability(capabilityString);
	}
	
}
