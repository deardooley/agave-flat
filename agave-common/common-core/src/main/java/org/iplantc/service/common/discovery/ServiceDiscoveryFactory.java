/**
 * 
 */
package org.iplantc.service.common.discovery;

import org.iplantc.service.common.discovery.providers.sql.DatabaseServiceDiscoveryClient;
import org.testng.log4testng.Logger;

/**
 * Service discovery factory to obtain a service discovery client
 * based on the configuration definition of preferred service.
 * 
 * @author dooley
 *
 */
public class ServiceDiscoveryFactory
{
	private Logger log = Logger.getLogger(ServiceDiscoveryFactory.class);
	
	public static ServiceDiscoveryClient<?, ?> getInstance() {
		
		return new DatabaseServiceDiscoveryClient();
		
	}
	
}
