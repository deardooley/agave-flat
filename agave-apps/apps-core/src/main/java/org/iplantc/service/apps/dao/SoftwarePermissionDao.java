/**
 * 
 */
package org.iplantc.service.apps.dao;

import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.Session;
import org.iplantc.service.apps.exceptions.SoftwareException;
import org.iplantc.service.apps.model.SoftwarePermission;
import org.iplantc.service.apps.util.ServiceUtils;
import org.iplantc.service.common.persistence.HibernateUtil;

/**
 * @author dooley
 * 
 */
public class SoftwarePermissionDao 
{
	@SuppressWarnings("unchecked")
	public static List<SoftwarePermission> getSoftwarePermissions(Long softwareId)
			throws SoftwareException
	{

		if (!ServiceUtils.isValid(softwareId))
			throw new SoftwareException("Software id cannot be null");

		try
		{
			HibernateUtil.beginTransaction();
			Session session = HibernateUtil.getSession();
			session.clear();
			String hql = "from SoftwarePermission where software.id = :softwareid order by username asc";
			List<SoftwarePermission> permissions = session.createQuery(hql)
					.setLong("softwareid", softwareId)
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
			throw new SoftwareException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction();} catch (Exception e) {}
		}
	}

	public static SoftwarePermission getUserSoftwarePermissions(String username,
			long softwareId) throws SoftwareException
	{

		if (!ServiceUtils.isValid(username))
			throw new SoftwareException("Username cannot be null");

		try
		{
			HibernateUtil.beginTransaction();
			Session session = HibernateUtil.getSession();
			
			String hql = "from SoftwarePermission where software.id = :softwareid and username = :username";
			SoftwarePermission permission = (SoftwarePermission)session.createQuery(hql)
					.setString("username", username)
					.setLong("softwareid", softwareId)
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
			throw new SoftwareException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction();} catch (Exception e) {}
		}
	}

	public static void persist(SoftwarePermission pem) throws SoftwareException
	{
		if (pem == null)
			throw new SoftwareException("Permission cannot be null");

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
			throw new SoftwareException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction();} catch (Exception e) {}
		}
	}

	public static void delete(SoftwarePermission pem) throws SoftwareException
	{
		if (pem == null)
			throw new SoftwareException("Permission cannot be null");

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
			throw new SoftwareException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction();} catch (Exception e) {}
		}
	}

}
