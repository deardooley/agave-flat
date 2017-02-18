/**
 * 
 */
package org.iplantc.service.monitor.dao;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.hibernate.CacheMode;
import org.hibernate.HibernateException;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.iplantc.service.common.dao.AbstractDao;
import org.iplantc.service.common.persistence.HibernateUtil;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.monitor.exceptions.MonitorException;
import org.iplantc.service.monitor.model.MonitorCheck;
import org.iplantc.service.monitor.model.enumeration.MonitorCheckType;
import org.iplantc.service.monitor.model.enumeration.MonitorStatusType;
import org.joda.time.DateTime;

/**
 * Data access class for internal users.
 * 
 * @author dooley
 */
public class MonitorCheckDao extends AbstractDao
{
	/* (non-Javadoc)
	 * @see org.iplantc.service.common.persistence.AbstractDao#getSession()
	 */
	@Override
	protected Session getSession() {
		HibernateUtil.beginTransaction();
		Session session = HibernateUtil.getSession();
		session.clear();
		session.enableFilter("monitorcheckTenantFilter").setParameter("tenantId", TenancyHelper.getCurrentTenantId());
		return session;
	}
	
	
	/**
	 * Gets all checks for a monitor.
	 * 
	 * @param username
	 * @param uuid
	 * @return
	 * @throws MonitorException
	 */
	@SuppressWarnings("unchecked")
	public List<MonitorCheck> getAllChecksByMonitorId(Long monitorId) 
	throws MonitorException
	{
		try
		{
			Session session = getSession();
			
			String hql = "from MonitorCheck mc " +
					"where mc.monitor.id = :monitorId " +
					"order by mc.created DESC";
			
			List<MonitorCheck> checks = (List<MonitorCheck>)session.createQuery(hql)
					.setLong("monitorId", monitorId)
					.list();
			
			session.flush();
			
			return checks;
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
	 * Gets all checks for a monitor.
	 * 
	 * @param username
	 * @param uuid
	 * @return
	 * @throws MonitorException
	 */
	@SuppressWarnings("unchecked")
	public MonitorCheck getLastMonitorCheck(Long monitorId) 
	throws MonitorException
	{
		try
		{
			Session session = getSession();
			
			String hql = "from MonitorCheck mc " +
					"where mc.monitor.id = :monitorId " +
					"order by mc.id DESC";
			
			List<MonitorCheck> checks = (List<MonitorCheck>)session.createQuery(hql)
					.setLong("monitorId", monitorId)
					.setMaxResults(1)
					.list();
			
			session.flush();
			
			if (checks.isEmpty()) {
				return null;
			} else {
				return checks.get(0);
			}
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
	 * Finds check by uuid within a tenant
	 * 
	 * @param apiUsername
	 * @return
	 * @throws MonitorException
	 */
	public MonitorCheck findByUuid(String uuid) throws MonitorException
	{
		try
		{
			Session session = getSession();
			
			String hql = "from MonitorCheck t where t.uuid = :uuid";
			
			MonitorCheck check = (MonitorCheck)session.createQuery(hql)
					.setString("uuid",uuid)
					.uniqueResult();
			
			session.flush();
			
			return check;
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
	 * Returns all checks for the given monitor id during the start and end date paginated
	 * using the limit and offset parameters
	 * 
	 * @param monitorId
	 * @param type
	 * @param startDate
	 * @param endDate
	 * @param limit
	 * @param offset
	 * @return
	 * @throws MonitorException
	 */
	@SuppressWarnings("unchecked")
	public List<MonitorCheck> getPaginatedByIdAndRange( Long monitorId, 
	                                                    MonitorCheckType type,
														Date startDate, 
														Date endDate, 
														MonitorStatusType result, 
														int limit, 
														int offset) 
	throws MonitorException
	{
		if (monitorId == null) {
			throw new MonitorException("No monitor id provided.");
		}
		
		if (startDate == null) {
			startDate = new DateTime().minusSeconds(1).toDate();
		}
		
		if (endDate == null) {
			endDate = new DateTime().plusSeconds(1).toDate();
		}
		
		if (endDate.before(startDate))
		{
			Date tmp = startDate;
			startDate = endDate;
			endDate = tmp;
		}
		
		try
		{
			Session session = getSession();
			
			String hql = "from MonitorCheck mc " +
					"where mc.monitor.id = :monitorId " +
					"and mc.created >= :startDate " +
					"and mc.created <= :endDate ";
			
			if (result != null) {
				hql += "and mc.result = :checkResult ";
			}
			
			if (type != null) {
			    hql += " and mc.checkType = :checkType ";
			}
			
			hql += "order by mc.created DESC";
			
			Query query = session.createQuery(hql)
					.setLong("monitorId", monitorId)
					.setParameter("startDate", startDate)
					.setParameter("endDate", endDate);
			
			if (result != null) {
				query.setString("checkResult", result.name());
			}
			
			if (type != null) {
                query.setString("checkType", type.name());
            }
			
			List<MonitorCheck> checks = (List<MonitorCheck>)query
//					.setResultTransformer(Transformers.aliasToBean(MonitorCheck.class))
					.setCacheable(false)
					.setCacheMode(CacheMode.IGNORE)
					.setFirstResult(offset)
					.setMaxResults(limit)
					.list();
			
			session.flush();
			
			return checks;
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
	public void persist(MonitorCheck monitor) throws MonitorException
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
		catch (HibernateException ex)
		{
			try
			{
				if (HibernateUtil.getSession().isOpen()) {
					HibernateUtil.rollbackTransaction();
				}
			}
			catch (Exception e) {}
			
			throw new MonitorException("Failed to save monitor check", ex);
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
	public void delete(MonitorCheck monitor) throws MonitorException
	{

		if (monitor == null)
			throw new MonitorException("MonitorCheck cannot be null");

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
			
			throw new MonitorException("Failed to delete monitor check", ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}

	@SuppressWarnings("unchecked")
	public List<MonitorCheck> getAll() throws MonitorException
	{
		try
		{
			Session session = getSession();
			
			List<MonitorCheck> users = (List<MonitorCheck>) session.createQuery("FROM MonitorCheck").list();
			
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
}
