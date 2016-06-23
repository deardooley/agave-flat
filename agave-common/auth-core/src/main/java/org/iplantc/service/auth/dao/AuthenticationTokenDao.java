package org.iplantc.service.auth.dao;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.LockOptions;
import org.hibernate.Session;
import org.hibernate.criterion.Example;
import org.iplantc.service.auth.exceptions.AuthenticationTokenException;
import org.iplantc.service.auth.model.AuthenticationToken;
import org.iplantc.service.common.persistence.HibernateUtil;

public class AuthenticationTokenDao 
{
	private static final Logger log = Logger.getLogger(AuthenticationTokenDao.class);
	
	public void delete(AuthenticationToken token)
	{
    	if (token == null)
			throw new HibernateException("Cannot delete null token");

    	try
		{
    		HibernateUtil.beginTransaction();
			Session session = HibernateUtil.getSession();
			session.delete(token);
			session.flush();
		}
		catch (Exception ex)
		{
			try
			{
				if (HibernateUtil.getSession().isOpen())
				{
					HibernateUtil.rollbackTransaction();
				}
			}
			catch (Exception e) {}
			
			throw new AuthenticationTokenException("Failed to delete auth token", ex);
		}
		finally {
			try { HibernateUtil.commitTransaction();} catch (Exception e) {}
		}
	}

	public void update(AuthenticationToken token)
	{
		if (token == null)
			throw new HibernateException("Cannot update null token");

		try
		{
			HibernateUtil.beginTransaction();
			Session session = HibernateUtil.getSession();
			session.update(token);
			session.flush();
		}
		catch (Exception ex)
		{
			try
			{
				if (HibernateUtil.getSession().isOpen())
				{
					HibernateUtil.rollbackTransaction();
				}
			}
			catch (Exception e) {}
			
			throw new AuthenticationTokenException("Failed to update auth token", ex);
		}
		finally {
			try { HibernateUtil.commitTransaction();} catch (Exception e) {}
		}
	}

	
	public void persist(AuthenticationToken token)
	{
		if (token == null)
			throw new HibernateException("Cannot save null object");

		try
		{
			HibernateUtil.beginTransaction();
			Session session = HibernateUtil.getSession();
			session.saveOrUpdate(token);
			session.flush();
		}
		catch (Exception ex)
		{
			try
			{
				if (HibernateUtil.getSession().isOpen())
				{
					HibernateUtil.rollbackTransaction();
				}
			}
			catch (Exception e) {}
			
			throw new AuthenticationTokenException("Failed to save auth token", ex);
		}
		finally {
			try { HibernateUtil.commitTransaction();} catch (Exception e) {}
		}
	}

	
	public AuthenticationToken merge(AuthenticationToken token)
	{
		if (token == null)
			throw new HibernateException("Cannot merge null token");

		try
		{
			HibernateUtil.beginTransaction();
			Session session = HibernateUtil.getSession();
			session.merge(token);
			session.flush();
			return token;
		}
		catch (Exception ex)
		{
			log.error("Failed to merge auth token", ex);
			try
			{
				if (HibernateUtil.getSession().isOpen())
				{
					HibernateUtil.rollbackTransaction();
				}
			}
			catch (Exception e) {}
			
			throw new AuthenticationTokenException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction();} catch (Exception e) {}
		}
	}

	
	@SuppressWarnings("unchecked")
	public List<AuthenticationToken> findByExample(AuthenticationToken token)
	{
		if (token == null)
			throw new HibernateException("Cannot match null token");

		try
		{
			HibernateUtil.beginTransaction();
			Session session = HibernateUtil.getSession();
			
			List<AuthenticationToken> tokens = (List<AuthenticationToken>)HibernateUtil.getSession()
					.createCriteria(AuthenticationToken.class)
					.add(Example.create(token))
					.list();
			
			session.flush();
			
			return tokens;
		}
		catch (Exception ex)
		{
			try
			{
				if (HibernateUtil.getSession().isOpen())
				{
					HibernateUtil.rollbackTransaction();
				}
			}
			catch (Exception e) {}
			
			throw new AuthenticationTokenException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction();} catch (Exception e) {}
		}
	}

	
	public void attachClean(AuthenticationToken token)
	{
		if (token == null)
			throw new HibernateException("Cannot attach clean null token");
		
		try
		{
			HibernateUtil.beginTransaction();
			Session session = HibernateUtil.getSession();
			session.buildLockRequest(LockOptions.NONE).lock(token);
			session.flush();
		}
		catch (Exception ex)
		{
			throw new AuthenticationTokenException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction();} catch (Exception e) {}
		}
    }

	
	public void attachDirty(AuthenticationToken token)
	{
		if (token == null)
			throw new HibernateException("Cannot attach dirty null token");

		persist(token);
	}

	public AuthenticationToken findById(Long id)
	{
		if (id == null)
			throw new HibernateException("Cannot match null identifier");
		
		try
		{
			HibernateUtil.beginTransaction();
			Session session = HibernateUtil.getSession();
			AuthenticationToken token = (AuthenticationToken)session.get(AuthenticationToken.class, id);
			session.flush();
			return token;
		}
		catch (Exception ex)
		{
			throw new AuthenticationTokenException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction();} catch (Exception e) {}
		}
	}
	
	public AuthenticationToken findByToken(String token)
	{
		if (StringUtils.isEmpty(token))
			throw new HibernateException("Cannot match emtpy token");

		try
		{
			HibernateUtil.beginTransaction();
			Session session = HibernateUtil.getSession();
			
			String hql = "from AuthenticationToken where " +
					"token = :token and " + 
					"(remaining_uses = -1 or remaining_uses > 0) and " + 
					"expires_at > current_timestamp()";
			
			AuthenticationToken authToken = (AuthenticationToken) session.createQuery(hql)
					.setString("token", token)
					//.setTimestamp("expirationdate", new Date())
					.uniqueResult();
			
			session.flush();
			
			return authToken;
		}
		catch (Exception ex)
		{
			throw new AuthenticationTokenException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction();} catch (Exception e) {}
		}		
	}
	 
	public List<AuthenticationToken> getAll() 
	{
		HibernateUtil.beginTransaction();
		Session session = null;
		try
		{
			session = HibernateUtil.getSession();
			session.clear();
			
			String hql = "from AuthenticationToken";
			
			return (List<AuthenticationToken>) session.createQuery(hql).list();
		}
		catch (Exception ex)
		{
			throw new AuthenticationTokenException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction();} catch (Exception e) {}
		}
	}

	/**
	 * Returns all tokens assigned to or created by a user
	 * @param username
	 * @return List of AuthenticationTokens
	 */
	@SuppressWarnings("unchecked")
	public List<AuthenticationToken> findAllByUsername(String username)
	{
		if (StringUtils.isEmpty(username))
			throw new HibernateException("Cannot find tokens for empty username");

		HibernateUtil.beginTransaction();
		Session session = null;
		try
		{
			session = HibernateUtil.getSession();
			session.clear();
			String hql = "from AuthenticationToken where username = :username or creator = :username";
			return (List<AuthenticationToken>) session.createQuery(hql)
					.setString("username", username)
					.list();
		}
		catch (Exception ex)
		{
			throw new AuthenticationTokenException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction();} catch (Exception e) {}
		}
	}

	/**
	 * Returns all active tokens assigned to or created by a user
	 * @param username
	 * @return List of AuthenticationTokens
	 */
	@SuppressWarnings("unchecked")
	public List<AuthenticationToken> findActiveByUsername(String username)
	{
		if (StringUtils.isEmpty(username))
			throw new HibernateException("Cannot find tokens for empty username");
		
		try
		{
			HibernateUtil.beginTransaction();
			Session session = HibernateUtil.getSession();
			
			String hql = "from AuthenticationToken where " +
					"expires_at > current_timestamp() and " +
					"(username = :username or creator = :username)";
			List<AuthenticationToken> tokens = (List<AuthenticationToken>) session.createQuery(hql)
					.setString("username", username)
					//.setTimestamp("expirationdate", new Date())
					.list();
			
			session.flush();
			
			return tokens;
		}
		catch (Exception ex)
		{
			throw new AuthenticationTokenException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction();} catch (Exception e) {}
		}
	}
	 
}
