package org.iplantc.service.common.discovery.providers.sql;

import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Table;

import org.iplantc.service.common.discovery.PlatformApi;

/**
 * POJO for an API {@link DiscoveryService}. These represent the 
 * core APIs and do not technically need to be discovered.
 * 
 * @author dooley
 *
 */
@Entity
@Table(name = "discoverableservices")
@DiscriminatorValue("API")
@DiscriminatorColumn(name="service_type")
public class DiscoverableApi extends DiscoverableService
{	
	
}
