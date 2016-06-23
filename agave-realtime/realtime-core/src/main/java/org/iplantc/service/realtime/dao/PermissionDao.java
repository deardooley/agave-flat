/**
 * 
 */
package org.iplantc.service.realtime.dao;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.hibernate.HibernateException;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.Session;
import org.iplantc.service.common.persistence.HibernateUtil;
import org.iplantc.service.realtime.exceptions.RealtimeChannelPermissionException;
import org.iplantc.service.realtime.model.RealtimeChannelPermission;

/**
 * @author dooley
 * 
 */
public class PermissionDao 
{
	@SuppressWarnings("unchecked")
	public static List<RealtimeChannelPermission> getEntityPermissions(Long realtimeChannelId)
	throws RealtimeChannelPermissionException
	{
		if (realtimeChannelId != null && realtimeChannelId < 1)
			throw new RealtimeChannelPermissionException("RealtimeChannel id cannot be null");

		try
		{
			HibernateUtil.beginTransaction();
			Session session = HibernateUtil.getSession();
			session.clear();
			String hql = "from RealtimeChannelPermission where resourceId = :id order by username asc";
			List<RealtimeChannelPermission> permissions = session.createQuery(hql)
					.setLong("id", realtimeChannelId)
					.list();

			session.flush();
			
			return permissions;

		}
		catch (HibernateException ex) 
		{
			try
			{
				if (HibernateUtil.getSession().isOpen())
				{
					HibernateUtil.rollbackTransaction();
				}
			}
			catch (Exception e)
			{
			}
			throw new RealtimeChannelPermissionException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction();} catch (Exception e) {}
		}
	}

	public static RealtimeChannelPermission getUserTagPermissions(String username,
			long softwareId) throws RealtimeChannelPermissionException
	{

		if (StringUtils.isEmpty(username))
			throw new RealtimeChannelPermissionException("Username cannot be null");

		try
		{
			HibernateUtil.beginTransaction();
			Session session = HibernateUtil.getSession();
			
			String hql = "from RealtimeChannelPermission where realtimeChannel.id = :id and username = :username";
			RealtimeChannelPermission permission = (RealtimeChannelPermission)session.createQuery(hql)
					.setString("username", username)
					.setLong("id", softwareId)
					.setMaxResults(1)
					.uniqueResult();

			session.flush();
			
			return permission;
		}
		catch (ObjectNotFoundException ex)
		{
			return null;
		}
		catch (HibernateException ex) 
		{
			try
			{
				if (HibernateUtil.getSession().isOpen())
				{
					HibernateUtil.rollbackTransaction();
				}
			}
			catch (Exception e)
			{
			}
			throw new RealtimeChannelPermissionException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction();} catch (Exception e) {}
		}
	}

	public static void persist(RealtimeChannelPermission pem) throws RealtimeChannelPermissionException
	{
		if (pem == null)
			throw new RealtimeChannelPermissionException("Permission cannot be null");

		try
		{
			HibernateUtil.beginTransaction();
			Session session = HibernateUtil.getSession();
			session.saveOrUpdate(pem);
			session.flush();
		}
		catch (HibernateException ex) 
		{
			try
			{
				if (HibernateUtil.getSession().isOpen())
				{
					HibernateUtil.rollbackTransaction();
				}
			}
			catch (Exception e)
			{
			}
			throw new RealtimeChannelPermissionException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction();} catch (Exception e) {}
		}
	}

	public static void delete(RealtimeChannelPermission pem) throws RealtimeChannelPermissionException
	{
		if (pem == null)
			throw new RealtimeChannelPermissionException("Permission cannot be null");

		try
		{
			HibernateUtil.beginTransaction();
			Session session = HibernateUtil.getSession();
			session.delete(pem);
			session.flush();
		}
		catch (HibernateException ex) 
		{
			try
			{
				if (HibernateUtil.getSession().isOpen())
				{
					HibernateUtil.rollbackTransaction();
				}
			}
			catch (Exception e)
			{
			}
			throw new RealtimeChannelPermissionException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction();} catch (Exception e) {}
		}
	}

}
