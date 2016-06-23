/**
 * 
 */
package org.iplantc.service.monitor.dao;

import java.util.Date;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.exception.ConstraintViolationException;
import org.iplantc.service.common.dao.AbstractDao;
import org.iplantc.service.common.persistence.HibernateUtil;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.monitor.exceptions.MonitorException;
import org.iplantc.service.monitor.model.Monitor;
import org.iplantc.service.systems.model.RemoteSystem;

/**
 * Data access class for internal users.
 * 
 * @author dooley
 */
public class MonitorDao extends AbstractDao
{
	/* (non-Javadoc)
	 * @see org.iplantc.service.common.persistence.AbstractDao#getSession()
	 */
	@Override
	protected Session getSession() {
		HibernateUtil.beginTransaction();
		Session session = HibernateUtil.getSession();
		session.clear();
		session.enableFilter("monitorTenantFilter").setParameter("tenantId", TenancyHelper.getCurrentTenantId());
		return session;
	}
	
	/**
	 * Gets all active monitors for a given user.
	 * 
	 * @param apiUsername
	 * @return
	 * @throws MonitorException
	 */
	@SuppressWarnings("unchecked")
	public List<Monitor> getActiveUserMonitors(String username) 
	throws MonitorException
	{
		try
		{
			Session session = getSession();
			
			String hql = "from Monitor m " +
					"where m.owner = :owner " +
					"and m.active = :active " +
					"order by m.created DESC";
			
			List<Monitor> monitors = (List<Monitor>)session.createQuery(hql)
					.setBoolean("active", Boolean.TRUE)
					.setString("owner",username)
					.list();
			
			session.flush();
			return monitors;
		}
		catch (HibernateException ex)
		{
			try
			{
				if (HibernateUtil.getSession().isOpen()) {
					HibernateUtil.rollbackTransaction();
				}
			}
			catch (Exception e) {}
			
			throw new MonitorException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}
	
	/**
	 *  Gets all monitors for a given user filtering by active status and system
	 *  
	 * @param username
	 * @param includeActive
	 * @param includeInactive
	 * @param systemId
	 * @return
	 * @throws MonitorException
	 */
	@SuppressWarnings("unchecked")
	public List<Monitor> getUserMonitors(String username, boolean includeActive, boolean includeInactive, String systemId) 
	throws MonitorException
	{
		try
		{
			Session session = getSession();
			
			String hql = "from Monitor m " +
					"where m.owner = :owner ";
			
			if (!includeActive || !includeInactive)
			{
				if (includeActive || includeInactive) {
					hql += "and m.active = :active ";
				}
			}
			
			if (systemId != null) {
				hql += "and m.system.systemId = :systemId ";
			}
			
			hql += "order by m.created DESC";
			
			Query query = session.createQuery(hql)
					.setString("owner",username);
			
			if (!includeActive || !includeInactive)
			{
				if (includeActive) {
					query.setBoolean("active", Boolean.TRUE);
				} else if (includeInactive) {
					query.setBoolean("active", Boolean.FALSE);
				}
			}
			
			if (systemId != null) {
				query.setString("systemId", systemId);
			}
					
			List<Monitor> monitors = (List<Monitor>)query.list();
			
			session.flush();
			return monitors;
		}
		catch (HibernateException ex)
		{
			try
			{
				if (HibernateUtil.getSession().isOpen()) {
					HibernateUtil.rollbackTransaction();
				}
			}
			catch (Exception e) {}
			
			throw new MonitorException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}
	
	/**
	 * Gets all internal users created by the given apiUsername. This will return both
	 * active and inactive users.
	 * 
	 * @param apiUsername
	 * @return
	 * @throws MonitorException
	 */
	public Monitor findByUuid(String uuid) throws MonitorException
	{
		try
		{
			HibernateUtil.beginTransaction();
			Session session = HibernateUtil.getSession();
			session.clear();
			
			String hql = "from Monitor t where t.uuid = :uuid";
			
			Monitor monitor = (Monitor)session.createQuery(hql)
					.setString("uuid",uuid)
					.uniqueResult();
			
			session.flush();
			
			return monitor;
		}
		catch (HibernateException ex)
		{
			try
			{
				if (HibernateUtil.getSession().isOpen()) {
					HibernateUtil.rollbackTransaction();
				}
			}
			catch (Exception e) {}
			
			throw new MonitorException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}
	
	/**
	 * Returns the monitor matching the given uuid withing the current tenant id
	 * 
	 * @param apiUsername
	 * @return
	 * @throws MonitorException
	 */
	public Monitor findByUuidWithinSessionTenant(String uuid) throws MonitorException
	{
		try
		{
			Session session = getSession();
			
			String hql = "from Monitor t where t.uuid = :uuid";
			
			Monitor monitor = (Monitor)session.createQuery(hql)
					.setString("uuid",uuid)
					.uniqueResult();
			
			session.flush();
			
			return monitor;
		}
		catch (HibernateException ex)
		{
			try
			{
				if (HibernateUtil.getSession().isOpen()) {
					HibernateUtil.rollbackTransaction();
				}
			}
			catch (Exception e) {}
			
			throw new MonitorException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}
	
	/**
	 * Saves or updates the InternalUser
	 * @param monitor
	 * @throws MonitorException
	 */
	public void persist(Monitor monitor) throws MonitorException
	{
		if (monitor == null)
			throw new MonitorException("Monitor cannot be null");

		try
		{	
			HibernateUtil.beginTransaction();
			Session session = HibernateUtil.getSession();
			session.clear();
			
			session.saveOrUpdate(monitor);
			session.flush();
		}
		catch (ConstraintViolationException ex) {
			throw new MonitorException("A monitor already exists for this user on system " + 
					monitor.getSystem().getSystemId(), ex);
		}
		catch (HibernateException ex)
		{
			try
			{
				if (HibernateUtil.getSession().isOpen()) {
					HibernateUtil.rollbackTransaction();
				}
			}
			catch (Exception e) {}
			
			throw new MonitorException("Failed to save monitor", ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {e.printStackTrace();}
		}
	}

	/**
	 * Deletes a monitor
	 * 
	 * @param profile
	 * @throws MonitorException
	 */
	public void delete(Monitor monitor) throws MonitorException
	{

		if (monitor == null)
			throw new MonitorException("Monitor cannot be null");

		try
		{
			HibernateUtil.beginTransaction();
			Session session = HibernateUtil.getSession();
			session.clear();
			session.delete(monitor);
			session.getTransaction().commit();
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
			catch (Exception e) {}
			
			throw new MonitorException("Failed to delete monitor", ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}

	public Monitor merge(Monitor monitor) throws MonitorException
	{
		try
		{
			HibernateUtil.beginTransaction();
			Session session = HibernateUtil.getSession();
			session.clear();
			
			Monitor mergedMonitor = (Monitor)session.merge(monitor);
			
			//session.flush();
			
			return mergedMonitor;
		}
		catch (HibernateException ex)
		{
			try
			{
				if (HibernateUtil.getSession().isOpen()) {
					HibernateUtil.rollbackTransaction();
				}
			}
			catch (Exception e) {}
			throw new MonitorException("Failed to merge monitor", ex);
		}
		finally
		{
			try { HibernateUtil.commitTransaction(); } catch (Throwable e) {}
		}
	}
	
	public void refresh(Monitor monitor) throws MonitorException
	{
		try
		{
			HibernateUtil.beginTransaction();
			Session session = HibernateUtil.getSession();
			session.clear();
			
			session.refresh(monitor);
		}
		catch (HibernateException ex)
		{
			try
			{
				if (HibernateUtil.getSession().isOpen()) {
					HibernateUtil.rollbackTransaction();
				}
			}
			catch (Exception e) {}
			throw new MonitorException("Failed to merge monitor", ex);
		}
		finally
		{
			try { HibernateUtil.commitTransaction(); } catch (Throwable e) {}
		}
	}
	
	@SuppressWarnings("unchecked")
	public List<Monitor> getAll() throws MonitorException
	{
		try
		{
			Session session = getSession();
			
			List<Monitor> users = (List<Monitor>) session.createQuery("FROM Monitor").list();
			
			session.flush();
			
			return users;
		}
		catch (ObjectNotFoundException ex)
		{
			return null;
		}
		catch (HibernateException ex)
		{
			try
			{
				if (HibernateUtil.getSession().isOpen()) {
					HibernateUtil.rollbackTransaction();
				}
			}
			catch (Exception e) {}
			
			throw new MonitorException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}

	@SuppressWarnings("unchecked")
	public Monitor getNextPendingActiveMonitor() throws MonitorException
	{
		try
		{
			HibernateUtil.beginTransaction();
			Session session = HibernateUtil.getSession();
			session.clear();
			
			String hql = "from Monitor m " +
					"where m.nextUpdateTime < :rightnow " +
					"and m.active = :active " +
					"order by m.nextUpdateTime ASC";
			
			List<Monitor> monitors = (List<Monitor>)session.createQuery(hql)
					.setBoolean("active", Boolean.TRUE)
					.setParameter("rightnow", new Date())
					.setMaxResults(1)
					.list();
			
			session.flush();
			
			return monitors.isEmpty() ? null : monitors.get(0);
		}
		catch (HibernateException ex)
		{
			try
			{
				if (HibernateUtil.getSession().isOpen()) {
					HibernateUtil.rollbackTransaction();
				}
			}
			catch (Exception e) {}
			
			throw new MonitorException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
		
	}
	
	@SuppressWarnings("unchecked")
	public List<Monitor> getAllPendingActiveMonitor() throws MonitorException
	{
		try
		{
			HibernateUtil.beginTransaction();
			Session session = HibernateUtil.getSession();
			session.clear();
			
			String hql = "from Monitor m " +
					"where m.nextUpdateTime < :rightnow " +
					"and m.active = :active " +
					"order by m.nextUpdateTime ASC";
			
			List<Monitor> monitors = (List<Monitor>)session.createQuery(hql)
					.setBoolean("active", Boolean.TRUE)
					.setDate("rightnow", new Date())
					.list();
			
			session.flush();
			
			return monitors;
		}
		catch (HibernateException ex)
		{
			try
			{
				if (HibernateUtil.getSession().isOpen()) {
					HibernateUtil.rollbackTransaction();
				}
			}
			catch (Exception e) {}
			
			throw new MonitorException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
		
	}

	/**
	 * Returns all user monitors on a given system
	 * 
	 * @param username
	 * @param system
	 * @return
	 * @throws MonitorException
	 */
	@SuppressWarnings("unchecked")
	public List<Monitor> getActiveUserMonitorsOnSystem(String username, RemoteSystem system) throws MonitorException 
	{
		try
		{
			Session session = getSession();
			
			String hql = "from Monitor m " +
					"where m.owner = :owner " +
					"and m.active = :active " +
					"and m.system.id = :systemId " +
					"order by m.created DESC";
			
			List<Monitor> monitors = (List<Monitor>)session.createQuery(hql)
					.setBoolean("active", Boolean.TRUE)
					.setString("owner",username)
					.setLong("systemId", system.getId())
					.list();
			
			session.flush();
			return monitors;
		}
		catch (HibernateException ex)
		{
			try
			{
				if (HibernateUtil.getSession().isOpen()) {
					HibernateUtil.rollbackTransaction();
				}
			}
			catch (Exception e) {}
			
			throw new MonitorException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}

	public boolean doesExist(String username, RemoteSystem system) throws MonitorException 
	{
		try
		{
			Session session = getSession();
			
			String hql = "from Monitor m " +
					"where m.owner = :owner " +
					"and m.system.id = :systemId " +
					"order by m.created DESC";
			
			Monitor monitor = (Monitor)session.createQuery(hql)
					.setString("owner",username)
					.setLong("systemId", system.getId())
					.setMaxResults(1)
					.uniqueResult();
			
			session.flush();
			return monitor != null;
		}
		catch (HibernateException ex)
		{
			try
			{
				if (HibernateUtil.getSession().isOpen()) {
					HibernateUtil.rollbackTransaction();
				}
			}
			catch (Exception e) {}
			
			throw new MonitorException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}	
}
