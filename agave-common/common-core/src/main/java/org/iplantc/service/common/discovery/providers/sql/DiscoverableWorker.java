package org.iplantc.service.common.discovery.providers.sql;

import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Table;

import org.iplantc.service.common.discovery.PlatformWorker;

/**
 * POJO for an Worker {@link DiscoveryService}s. WorkerDiscoveryService 
 * can be any elastically scalable process used across the platform. 
 * Workers are self-reporting and report their existence with a regular heartbeat.
 * 
 * @author dooley
 *
 */
@Entity
@Table(name = "discoverableservices")
@DiscriminatorValue("WORKER")
@DiscriminatorColumn(name="service_type")
public class DiscoverableWorker extends DiscoverableService
{	
	
}
