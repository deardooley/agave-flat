/**
 * 
 */
package org.iplantc.service.tags.dao;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.hibernate.HibernateException;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.Session;
import org.iplantc.service.tags.exceptions.TagPermissionException;
import org.iplantc.service.tags.model.TagPermission;
import org.iplantc.service.common.persistence.HibernateUtil;

/**
 * @author dooley
 * 
 */
public class PermissionDao 
{
	@SuppressWarnings("unchecked")
	public static List<TagPermission> getEntityPermissions(String entityId)
	throws TagPermissionException
	{
		if (StringUtils.isEmpty(entityId))
			throw new TagPermissionException("Tag id cannot be null");

		try
		{
			HibernateUtil.beginTransaction();
			Session session = HibernateUtil.getSession();
			session.clear();
			String hql = "from TagPermission where entityId = :uuid order by username asc";
			List<TagPermission> permissions = session.createQuery(hql)
					.setString("uuid", entityId)
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
			throw new TagPermissionException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction();} catch (Exception e) {}
		}
	}

	public static TagPermission getUserTagPermissions(String username,
			String entityId) throws TagPermissionException
	{

		if (StringUtils.isEmpty(username))
			throw new TagPermissionException("Username cannot be null");

		try
		{
			HibernateUtil.beginTransaction();
			Session session = HibernateUtil.getSession();
			
			String hql = "from TagPermission where entityId = :uuid and username = :username";
			TagPermission permission = (TagPermission)session.createQuery(hql)
					.setString("username", username)
					.setString("uuid", entityId)
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
			throw new TagPermissionException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction();} catch (Exception e) {}
		}
	}

	public static void persist(TagPermission pem) throws TagPermissionException
	{
		if (pem == null)
			throw new TagPermissionException("Permission cannot be null");

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
			throw new TagPermissionException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction();} catch (Exception e) {}
		}
	}

	public static void delete(TagPermission pem) throws TagPermissionException
	{
		if (pem == null)
			throw new TagPermissionException("Permission cannot be null");

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
			throw new TagPermissionException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction();} catch (Exception e) {}
		}
	}

}
