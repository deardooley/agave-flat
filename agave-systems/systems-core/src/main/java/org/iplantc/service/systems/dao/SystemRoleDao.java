/**
 * 
 */
package org.iplantc.service.systems.dao;

import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.Session;
import org.iplantc.service.common.Settings;
import org.iplantc.service.common.persistence.HibernateUtil;
import org.iplantc.service.systems.exceptions.RolePersistenceException;
import org.iplantc.service.systems.model.SystemRole;
import org.iplantc.service.systems.util.ServiceUtils;

/**
 * @author dooley
 * 
 */
public class SystemRoleDao 
{
	/**
	 * Returns all roles for the given system, {@link Settings.DEFAULT_PAGE_SIZE} 
	 * results at a time, starting at result 0.
	 * @param remoteSystemId
	 * @return
	 * @throws RolePersistenceException
	 */
	public static List<SystemRole> getSystemRoles(Long remoteSystemId)
	throws RolePersistenceException
	{
		return getSystemRoles(remoteSystemId, Settings.DEFAULT_PAGE_SIZE, 0);
	}
	
	/**
	 * Returns {@code limit} {@link SystemRoles} for the given {@code remoteSystemId}. 
	 * At most {@link Settings.MAX_PAGE_SIZE} results will be returned regardless of 
	 * {@code limit}. 
	 * 
	 * @param remoteSystemId
	 * @param limit
	 * @param offset
	 * @return
	 * @throws RolePersistenceException
	 */
	@SuppressWarnings("unchecked")
	public static List<SystemRole> getSystemRoles(Long remoteSystemId, int limit, int offset)
			throws RolePersistenceException
	{

		if (limit < 0) {
			limit = 0;
		} 
		else if (limit > Settings.MAX_PAGE_SIZE) { 
			limit = Settings.MAX_PAGE_SIZE;
		}
		
		if (offset < 0) offset = 0;
		
		if (!ServiceUtils.isValid(remoteSystemId))
			throw new RolePersistenceException("RemoteSystem id cannot be null");

		try
		{
			HibernateUtil.beginTransaction();
			Session session = HibernateUtil.getSession();
			session.clear();
			String hql = "from SystemRole where remoteSystem.id = :systemid order by username asc";
			List<SystemRole> permissions = session.createQuery(hql)
					.setLong("systemid", remoteSystemId)
					.setFirstResult(offset)
					.setMaxResults(limit)
					.list();

			session.flush();
			
			return permissions;

		}
		catch (HibernateException ex) {
			try {
				if (HibernateUtil.getSession().isOpen()) {
					HibernateUtil.rollbackTransaction();
				}
			}
			catch (Exception e) {}
			throw new RolePersistenceException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction();} catch (Exception e) {}
		}
	}
	
	/**
	 * Returns {@link SystemRoles} for the given {@code systemId}, {@link Settings.DEFAULT_PAGE_SIZE} 
	 * results at a time, starting at result 0.
	 * @param systemId
	 * @return
	 * @throws RolePersistenceException
	 */
	public static List<SystemRole> getSystemRoles(String systemId)
	throws RolePersistenceException
	{
		return getSystemRoles(systemId, Settings.DEFAULT_PAGE_SIZE, 0);
	}

	/**
	 * Returns {@code limit} {@link SystemRoles} for the given {@code systemId}. 
	 * At most {@link Settings.MAX_PAGE_SIZE} results will be returned regardless of 
	 * {@code limit}. 
	 * 
	 * @param systemId
	 * @param limit
	 * @param offset
	 * @return
	 * @throws RolePersistenceException
	 */
	@SuppressWarnings("unchecked")
	public static List<SystemRole> getSystemRoles(String systemId, int limit, int offset)
	throws RolePersistenceException
	{
		if (limit < 0) {
			limit = 0;
		} 
		else if (limit > Settings.MAX_PAGE_SIZE) { 
			limit = Settings.MAX_PAGE_SIZE;
		}
		
		if (offset < 0) offset = 0;
		
		if (!ServiceUtils.isValid(systemId))
			throw new RolePersistenceException("RemoteSystem id cannot be null");

		try
		{
			HibernateUtil.beginTransaction();
			Session session = HibernateUtil.getSession();
			session.clear();
			String hql = "from SystemRole where remoteSystem.systemId = :systemid order by username asc";
			List<SystemRole> permissions = session.createQuery(hql)
					.setString("systemid", systemId)
					.setFirstResult(offset)
					.setMaxResults(limit)
					.list();

			session.flush();
			
			return permissions;

		}
		catch (HibernateException ex) {
			try {
				if (HibernateUtil.getSession().isOpen()) {
					HibernateUtil.rollbackTransaction();
				}
			}
			catch (Exception e) {}
			throw new RolePersistenceException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction();} catch (Exception e) {}
		}
	}

	/**
	 * Returns any {@link SystemRole} assigned to a particular user on the
	 * given {@link RemoteSystem#getSystemId}
	 * @param username the name of the user for whom to retrieve the permissions
	 * @param systemId the {@link RemoteSystem#getSystemId()} of a system
	 * @return
	 * @throws RolePersistenceException
	 */
	public static SystemRole getSystemRoleForUser(String username, String systemId) 
	throws RolePersistenceException
	{

		if (!ServiceUtils.isValid(username))
			throw new RolePersistenceException("Username cannot be null");

		try
		{
			HibernateUtil.beginTransaction();
			Session session = HibernateUtil.getSession();
			
			String hql = "from SystemRole where remoteSystem.systemId = :systemid and username = :username";
			SystemRole permission = (SystemRole)session.createQuery(hql)
					.setString("username", username)
					.setString("systemid", systemId)
					.setMaxResults(1)
					.uniqueResult();

			session.flush();
			
			return permission;
		}
		catch (ObjectNotFoundException ex)
		{
			return null;
		}
		catch (HibernateException ex) {
			try {
				if (HibernateUtil.getSession().isOpen()) {
					HibernateUtil.rollbackTransaction();
				}
			}
			catch (Exception e) {}
			throw new RolePersistenceException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction();} catch (Exception e) {}
		}
	}

	/**
	 * Saves or updates a {@link SystemRole}. 
	 * @param pem
	 * @throws RolePersistenceException
	 */
	public static void persist(SystemRole pem) throws RolePersistenceException
	{
		if (pem == null)
			throw new RolePersistenceException("Permission cannot be null");

		try
		{
			HibernateUtil.beginTransaction();
			Session session = HibernateUtil.getSession();
			session.saveOrUpdate(pem);
			session.flush();
		}
		catch (HibernateException ex) {
			try {
				if (HibernateUtil.getSession().isOpen()) {
					HibernateUtil.rollbackTransaction();
				}
			}
			catch (Exception e) {}
			throw new RolePersistenceException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction();} catch (Exception e) {}
		}
	}

	/**
	 * Deletes an existing {@link SystemRole}.
	 * @param pem
	 * @throws RolePersistenceException
	 */
	public static void delete(SystemRole pem) throws RolePersistenceException
	{
		if (pem == null)
			throw new RolePersistenceException("Permission cannot be null");

		try
		{
			HibernateUtil.beginTransaction();
			Session session = HibernateUtil.getSession();
			session.delete(pem);
			session.flush();
		}
		catch (HibernateException ex) {
			try {
				if (HibernateUtil.getSession().isOpen()) {
					HibernateUtil.rollbackTransaction();
				}
			}
			catch (Exception e) {}
			throw new RolePersistenceException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction();} catch (Exception e) {}
		}
	}

	
	/**
	 * Removes all {@link SystemRole} for a given {@link RemoteSytem} as a single sql update.
	 * @param remoteSystemId
	 * @throws RolePersistenceException
	 */
	public static void clearSystemRoles(Long remoteSystemId) throws RolePersistenceException {
		if (!ServiceUtils.isValid(remoteSystemId))
			throw new RolePersistenceException("RemoteSystem id cannot be null");

		try
		{
			HibernateUtil.beginTransaction();
			Session session = HibernateUtil.getSession();
			session.clear();
			String sql = "DELETE systemroles WHERE remote_system_id = :systemid";
			session.createSQLQuery(sql)
					.setLong("systemid", remoteSystemId)
					.executeUpdate();
		            
			session.flush();
		}
		catch (HibernateException ex) {
			try {
				if (HibernateUtil.getSession().isOpen()) {
					HibernateUtil.rollbackTransaction();
				}
			}
			catch (Exception e) {}
			throw new RolePersistenceException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction();} catch (Exception e) {}
		}
	}
}
