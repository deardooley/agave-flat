/**
 * 
 */
package org.iplantc.service.common.discovery.providers.sql;

import java.util.List;

import org.iplantc.service.common.dao.DiscoveryServiceDao;
import org.iplantc.service.common.discovery.ServiceCapability;
import org.iplantc.service.common.discovery.ServiceDiscoveryClient;
import org.iplantc.service.common.exceptions.ServiceDiscoveryException;

/**
 * Class to implement service discovery through the existing application
 * database. Given that we know this has to exist and every worker needs
 * connectivity, this makes sense as the default {@link ServiceDiscoveryClient} 
 * to use.
 *  
 * @author dooley
 *
 */
public class DatabaseServiceDiscoveryClient implements ServiceDiscoveryClient<DiscoverableApi, DiscoverableWorker>
{
	private DiscoveryServiceDao dao;
	
	public DatabaseServiceDiscoveryClient() {
		dao = new DiscoveryServiceDao();
	}
	
	/* (non-Javadoc)
	 * @see org.iplantc.service.common.discovery.impl.ServiceDiscoveryClient#listApiServicesWithCapability(org.iplantc.service.common.discovery.ServiceCapability)
	 */
	@Override
	public List<DiscoverableApi> listApiServicesWithCapability(ServiceCapability capability)
	throws ServiceDiscoveryException
	{
		return dao.findApiDiscoveryServicesByCapability(capability);
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.common.discovery.impl.ServiceDiscoveryClient#addApiService(org.iplantc.service.common.discovery.)
	 */
	@Override
	public void addApiService(DiscoverableApi discoverableApi)
	throws ServiceDiscoveryException
	{
		dao.persist(discoverableApi);
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.common.discovery.impl.ServiceDiscoveryClient#deleteApiService(org.iplantc.service.common.discovery.)
	 */
	@Override
	public void deleteApiService(DiscoverableApi discoverableApi)
	throws ServiceDiscoveryException
	{
		dao.delete(discoverableApi);
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.common.discovery.impl.ServiceDiscoveryClient#listApiServices()
	 */
	@Override
	public List<DiscoverableApi> listApiServices()
	throws ServiceDiscoveryException
	{
		return dao.getAllApiDiscoveryServices();
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.common.discovery.impl.ServiceDiscoveryClient#listWorkerServiceWithCapability(org.iplantc.service.common.discovery.ServiceCapability)
	 */
	@Override
	public List<DiscoverableWorker> listWorkerServiceWithCapability(ServiceCapability capability)
	throws ServiceDiscoveryException
	{
		return dao.findWorkerDiscoveryServicesByCapability(capability);
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.common.discovery.impl.ServiceDiscoveryClient#addDiscoverableWorker(org.iplantc.service.common.discovery.DiscoverableWorker)
	 */
	@Override
	public void addDiscoverableWorker(DiscoverableWorker discoverableWorker)
	throws ServiceDiscoveryException
	{
		dao.persist(discoverableWorker);
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.common.discovery.impl.ServiceDiscoveryClient#deleteWorkerService(org.iplantc.service.common.discovery.DiscoverableService)
	 */
	@Override
	public void deleteWorkerService(DiscoverableWorker discoverableWorker)
	throws ServiceDiscoveryException
	{
		dao.delete(discoverableWorker);
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.common.discovery.impl.ServiceDiscoveryClient#listWorkerServices()
	 */
	@Override
	public List<DiscoverableWorker> listWorkerServices()
	throws ServiceDiscoveryException
	{
		return dao.getAllWorkerDiscoveryServices();
	}
	
}
