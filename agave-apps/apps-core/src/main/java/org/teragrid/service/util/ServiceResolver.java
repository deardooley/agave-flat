package org.teragrid.service.util;

import java.util.Hashtable;
import java.util.Set;

import org.teragrid.service.tgcdb.dto.ComputeDTO;
import org.teragrid.service.tgcdb.dto.Service;

@SuppressWarnings("unused")
public class ServiceResolver {

	// table of services for each resource keyed off the info service resource
	// id
	private static Hashtable<String, Set<Service>>	serviceCache	= new Hashtable<String, Set<Service>>();

	public static void resolve(ComputeDTO system)
	{

	// query info service to find services for the resource
	// query inca for status of services
	// cache services and status
	// add services to system
	// system.setServices(serviceCache.get(system.getId()));

	}

	public void updateServiceCache()
	{
	// call general info service web service query
	// parse results into table
	}

	public void updateStatusCache()
	{
	// call inca stats query for service
	// parse results into table
	}

}
