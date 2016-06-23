package org.iplantc.service.common.discovery;

import java.util.List;

import org.iplantc.service.common.exceptions.ServiceDiscoveryException;

public interface ServiceDiscoveryClient<A extends PlatformService<?>, W extends PlatformService<?>>
{
	public abstract List<A> listApiServicesWithCapability(ServiceCapability capability) throws ServiceDiscoveryException;

	public abstract void addApiService(A discoverableApi) throws ServiceDiscoveryException;

	public abstract  void deleteApiService(A discoverableApi) throws ServiceDiscoveryException;

	public abstract List<A> listApiServices() throws ServiceDiscoveryException;

	public abstract List<W> listWorkerServiceWithCapability(ServiceCapability capability) throws ServiceDiscoveryException;

	public abstract void addDiscoverableWorker(W discoverableWorker) throws ServiceDiscoveryException;

	public abstract void deleteWorkerService(W discoverableWorker) throws ServiceDiscoveryException;

	public abstract List<W> listWorkerServices() throws ServiceDiscoveryException;

}