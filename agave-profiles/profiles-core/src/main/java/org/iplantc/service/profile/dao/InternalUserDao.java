/**
 * 
 */
package org.iplantc.service.profile.dao;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.iplantc.service.common.dao.AbstractDao;
import org.iplantc.service.common.persistence.HibernateUtil;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.profile.exceptions.ProfileException;
import org.iplantc.service.profile.model.InternalUser;

/**
 * Data access class for internal users.
 * 
 * @author dooley
 */
public class InternalUserDao extends AbstractDao
{	
	/* (non-Javadoc)
	 * @see org.iplantc.service.common.persistence.AbstractDao#getSession()
	 */
	@Override
	protected Session getSession() {
		HibernateUtil.beginTransaction();
		Session session = HibernateUtil.getSession();
		session.clear();
		session.enableFilter("internalUserTenantFilter").setParameter("tenantId", TenancyHelper.getCurrentTenantId());
		return session;
	}
	
	/**
	 * Gets all internal users created by the given apiUsername. This will return only
	 * active users.
	 * 
	 * @param apiUsername
	 * @return
	 * @throws ProfileException
	 */
	@SuppressWarnings("unchecked")
	public List<InternalUser> getActiveInternalUsersCreatedByAPIUser(String apiUsername) 
	throws ProfileException
	{
		try
		{
			Session session = getSession();
			
			String hql = "from InternalUser u " +
					"where u.createdBy = :createdby " +
					"and u.active = :active " +
					"order by username ASC";
			
			List<InternalUser> users = (List<InternalUser>)session.createQuery(hql)
					.setBoolean("active", Boolean.TRUE)
					.setString("createdby",apiUsername)
					.list();
			
			session.flush();
			
			return users;
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
			
			throw new ProfileException(ex);
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
	 * @throws ProfileException
	 */
	@SuppressWarnings("unchecked")
	public List<InternalUser> getAllInternalUsersCreatedByAPIUser(String apiUsername) 
	throws ProfileException
	{
		try
		{
			Session session = getSession();
			
			String hql = "from InternalUser u where u.createdBy = :createdby order by username ASC";
			
			List<InternalUser> users = (List<InternalUser>)session.createQuery(hql)
					.setString("createdby",apiUsername)
					.list();
			
			session.flush();
			
			return users;
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
			
			throw new ProfileException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}

	/**
	 * Gets internal user by database id.
	 * 
	 * @param profileId
	 * @return
	 * @throws ProfileException
	 */
	public InternalUser getInternalUserById(long profileId) throws ProfileException
	{
		try
		{
			Session session = getSession();
			
			InternalUser user = (InternalUser) session.get(InternalUser.class, profileId);
			
			session.flush();
			
			return user;
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
			
			throw new ProfileException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}

	/**
	 * Saves or updates the InternalUser
	 * @param task
	 * @throws ProfileException
	 */
	public void persist(InternalUser task) throws ProfileException
	{
		if (task == null)
			throw new ProfileException("Profile cannot be null");

		try
		{	
			Session session = getSession();
			session.saveOrUpdate(task);
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
			
			throw new ProfileException("Failed to save internal user", ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {e.printStackTrace();}
		}
	}

	/**
	 * Deletes an InternalUser
	 * 
	 * @param profile
	 * @throws ProfileException
	 */
	public void delete(InternalUser profile) throws ProfileException
	{

		if (profile == null)
			throw new ProfileException("InternalUser cannot be null");

		try
		{
			Session session = getSession();
			session.delete(profile);
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
			
			throw new ProfileException("Failed to delete user", ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}

	/**
	 * Looks up an active internal user by their username within the context of the given
	 * API user. This will ignore inactive users.
	 *  
	 * @param apiUsername
	 * @param username
	 * @return
	 * @throws ProfileException
	 */
	public InternalUser getInternalUserByAPIUserAndUsername(String apiUsername, String username) 
	throws ProfileException
	{
		try
		{
			Session session = getSession();
			
			String hql = "from InternalUser u " +
					"where u.createdBy = :createdby " +
					//"and u.active = :active " + 
					"and u.username = :username ";
			
			InternalUser user = (InternalUser)session.createQuery(hql)
					//.setBoolean("active", Boolean.TRUE)
					.setString("createdby",apiUsername)
					.setString("username",username)
					.setMaxResults(1)
					.uniqueResult();
			
			session.flush();
			
			return user;
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
			
			throw new ProfileException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}	

	/**
	 * Search for internal users with a value containing <code>value</code> within the
	 * column defined by <code>field</code> for the given API user.
	 *  
	 * @param apiUsername
	 * @param field
	 * @param value
	 * @return
	 * @throws ProfileException
	 */
	@SuppressWarnings("unchecked")
	public List<InternalUser> findByExample(String apiUsername, String field, Object value)
	throws ProfileException
	{
		try
		{
			Session session = getSession();
			
//			String hql = "from InternalUser u " +
//					"where u.createdBy = :createdby " +
//					//"and u.active = :active " + 
//					"and u." + field + " like '%" + value + "%'";
			
			
			Criteria criteria = session.createCriteria(InternalUser.class);
			if (value instanceof Boolean) {
				criteria.add(Restrictions.eq(field, value));
			} else {
				if (field.equalsIgnoreCase("name")) {
					String where = "CONCAT(lower({alias}.first_name), ' ', lower({alias}.last_name)) like ?";
					criteria.add(Restrictions.sqlRestriction(where, "%" + ((String)value).toLowerCase() + "%", org.hibernate.type.StandardBasicTypes.STRING ));
				} else {
					criteria.add(Restrictions.ilike(field, "%" + value + "%"));
				}
			}
					
			criteria.add(Restrictions.eq("createdBy", apiUsername));
			
			List<InternalUser> users = criteria.list();
			
			session.flush();
			
			return users;
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
			
			throw new ProfileException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}

	/**
	 * Checks for the existence of the given username registered to the given API
	 * user.
	 * 
	 * @param apiUsername
	 * @param internalUsername
	 * @return false if the username is already registered to the API user. True 
	 * otherwise.
	 * @throws ProfileException
	 */
	public boolean isUsernameAvailable(String apiUsername, String internalUsername)
	throws ProfileException
	{
		try
		{
			Session session = getSession();
			
			String hql = "from InternalUser u " +
					"where u.createdBy = :createdby " +
					"and u.username = :username";
			
			boolean empty = session.createQuery(hql)
					.setString("createdby", apiUsername)
					.setString("username", internalUsername)
					.list().isEmpty();
			
			session.flush();
			
			return empty;
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
			
			throw new ProfileException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}

	@SuppressWarnings("unchecked")
	public List<InternalUser> getAll() throws ProfileException
	{
		try
		{
			Session session = getSession();
			
			List<InternalUser> users = (List<InternalUser>) session.createQuery("FROM InternalUser").list();
			
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
			
			throw new ProfileException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}	
}
