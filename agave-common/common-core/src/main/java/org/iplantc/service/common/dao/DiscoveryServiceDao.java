/**
 * 
 */
package org.iplantc.service.common.dao;

import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.iplantc.service.common.discovery.ServiceCapability;
import org.iplantc.service.common.discovery.providers.sql.DiscoverableApi;
import org.iplantc.service.common.discovery.providers.sql.DiscoverableService;
import org.iplantc.service.common.discovery.providers.sql.DiscoverableWorker;
import org.iplantc.service.common.exceptions.ServiceDiscoveryException;
import org.iplantc.service.common.persistence.HibernateUtil;

/**
 * @author dooley
 *
 */
public class DiscoveryServiceDao extends AbstractDao
{
	protected Session getSession() {
		HibernateUtil.beginTransaction();
		Session session = HibernateUtil.getSession();
		return session;
	}
	
	/**
	 * Save or update a discoverableService
	 * 
	 * @param discoverableService
	 * @throws ServiceDiscoveryException 
	 */
	public <T extends DiscoverableService> void persist(T discoverableService) throws ServiceDiscoveryException
	{
		if (discoverableService == null)
			throw new ServiceDiscoveryException("DatabaseDiscoverableService cannot be null");

		try
		{
			Session session = getSession();
			
			session.saveOrUpdate(discoverableService);
			session.flush();
		}
		catch (HibernateException ex)
		{
			try
			{
				if (HibernateUtil.getSession().isOpen())
				{
					HibernateUtil.rollbackTransaction();
					HibernateUtil.getSession().close();
				}
			}
			catch (Exception e)
			{
			}
			throw new ServiceDiscoveryException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction();} catch (Exception e) {}
		}
	}

	/**
	 * Deletes a discoverableService from the db.
	 * @param discoverableService
	 * @throws ServiceDiscoveryException
	 */
	public <T extends DiscoverableService> void delete(T discoverableService) throws ServiceDiscoveryException
	{
		if (discoverableService == null)
			throw new ServiceDiscoveryException("DiscoveryService cannot be null");

		try
		{
			Session session = getSession();
			session.clear();
			session.delete(discoverableService);
			session.flush();
		}
		catch (HibernateException ex)
		{
			try
			{
				if (HibernateUtil.getSession().isOpen()) {
					HibernateUtil.rollbackTransaction();
				}
			}
			catch (Throwable e) {}
				
			throw new ServiceDiscoveryException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction();} catch (Exception e) {}
		}
	}
	
	/**
	 * Retrieves all {@link DiscoverableWorkerImpl} records
	 * 
	 * @return {@link List} of all {@link DiscoverableWorkerImpl}
	 */
	@SuppressWarnings("unchecked")
	public List<DiscoverableWorker> getAllWorkerDiscoveryServices() throws ServiceDiscoveryException
	{
		try
		{
			Session session = getSession();
			session.clear();
			List<DiscoverableWorker> services = (List<DiscoverableWorker>) session.createQuery("from DiscoverableWorker")
					.list();
			
			session.flush();
			
			return services;
		}
		catch (HibernateException ex)
		{
			throw new ServiceDiscoveryException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction();} catch (Exception e) {}
		}
	}
	
	/**
	 * Retrieves all {@link DiscoverableApiImpl} records
	 * 
	 * @return {@link List} of all {@link DiscoverableApiImpl}
	 */
	@SuppressWarnings("unchecked")
	public List<DiscoverableApi> getAllApiDiscoveryServices() throws ServiceDiscoveryException
	{
		try
		{
			Session session = getSession();
			session.clear();
			List<DiscoverableApi> services = (List<DiscoverableApi>) session.createQuery("from DiscoverableApi")
					.list();
			
			session.flush();
			
			return services;
		}
		catch (HibernateException ex)
		{
			throw new ServiceDiscoveryException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction();} catch (Exception e) {}
		}
	}

	@SuppressWarnings("unchecked")
	public List<DiscoverableApi> findApiDiscoveryServicesByCapability(ServiceCapability capability)
	throws ServiceDiscoveryException
	{
		try
		{
			Session session = getSession();
			session.clear();
			String hql = "from DiscoverableApi as service "
							+ "left join service.capabilities as cap "
						+ "where (cap.tenantCode = :tenantCode or cap.tenantCode = :tenantCodeAlt)" 
							+ " and (cap.apiName = :apiName or cap.apiName = :apiNameAlt) "
							+ " and (cap.activityType = :activityType or cap.apiName = :activityTypeAlt) "
							+ " and (cap.username = :username or cap.apiName = :usernameAlt) "
							+ " and (cap.groupName = :groupName or cap.apiName = :groupNameAlt) ";
			
			List<DiscoverableApi> services = (List<DiscoverableApi>)session.createQuery(hql)
					.setString("tenantCode", capability.getTenantCode())
					.setString("tenantCodeAlt", capability.getTenantCode().equals("*") ? capability.getTenantCode() : "_" + capability.getTenantCode())
					.setString("apiName", capability.getApiName())
					.setString("apiNameAlt", capability.getApiName().equals("*") ? capability.getApiName() : "_" + capability.getApiName())
					.setString("activityType", capability.getActivityType())
					.setString("activityTypeAlt", capability.getActivityType().equals("*") ? capability.getActivityType() : "_" + capability.getActivityType())
					.setString("username", capability.getUsername())
					.setString("usernameAlt", capability.getUsername().equals("*") ? capability.getUsername() : "_" + capability.getUsername())
					.setString("groupName", capability.getGroupName())
					.setString("groupNameAlt", capability.getGroupName().equals("*") ? capability.getGroupName() : "_" + capability.getGroupName())
					.list();
			
			session.flush();
			
			return services;
		}
		catch (HibernateException ex)
		{
			throw new ServiceDiscoveryException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction();} catch (Exception e) {}
		}
	}
	
	@SuppressWarnings("unchecked")
	public List<DiscoverableWorker> findWorkerDiscoveryServicesByCapability(ServiceCapability capability)
	throws ServiceDiscoveryException
	{
		try
		{
			Session session = getSession();
			session.clear();
			String hql = "from DiscoverableWorker as service "
							+ "left join service.capabilities as cap "
						+ "where (cap.tenantCode = :tenantCode or cap.tenantCode = :tenantCodeAlt)" 
							+ " and (cap.apiName = :apiName or cap.apiName = :apiNameAlt) "
							+ " and (cap.activityType = :activityType or cap.apiName = :activityTypeAlt) "
							+ " and (cap.username = :username or cap.apiName = :usernameAlt) "
							+ " and (cap.groupName = :groupName or cap.apiName = :groupNameAlt) ";
			
			List<DiscoverableWorker> services = (List<DiscoverableWorker>)session.createQuery(hql)
					.setString("tenantCode", capability.getTenantCode())
					.setString("tenantCodeAlt", capability.getTenantCode().equals("*") ? capability.getTenantCode() : "_" + capability.getTenantCode())
					.setString("apiName", capability.getApiName())
					.setString("apiNameAlt", capability.getApiName().equals("*") ? capability.getApiName() : "_" + capability.getApiName())
					.setString("activityType", capability.getActivityType())
					.setString("activityTypeAlt", capability.getActivityType().equals("*") ? capability.getActivityType() : "_" + capability.getActivityType())
					.setString("username", capability.getUsername())
					.setString("usernameAlt", capability.getUsername().equals("*") ? capability.getUsername() : "_" + capability.getUsername())
					.setString("groupName", capability.getGroupName())
					.setString("groupNameAlt", capability.getGroupName().equals("*") ? capability.getGroupName() : "_" + capability.getGroupName())
					.list();
			
			session.flush();
			
			return services;
		}
		catch (HibernateException ex)
		{
			throw new ServiceDiscoveryException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction();} catch (Exception e) {}
		}
	}
}
