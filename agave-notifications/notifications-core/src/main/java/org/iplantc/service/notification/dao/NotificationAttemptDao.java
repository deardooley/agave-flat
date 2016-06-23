package org.iplantc.service.notification.dao;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.transform.Transformers;
import org.iplantc.service.common.auth.AuthorizationHelper;
import org.iplantc.service.common.dao.AbstractDao;
import org.iplantc.service.common.persistence.HibernateUtil;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.common.search.SearchTerm;
import org.iplantc.service.notification.Settings;
import org.iplantc.service.notification.exceptions.NotificationException;
import org.iplantc.service.notification.model.Notification;
import org.iplantc.service.notification.model.NotificationAttempt;

public class NotificationAttemptDao extends AbstractDao {

	private static final Logger log = Logger.getLogger(NotificationAttemptDao.class);
	
	/* (non-Javadoc)
	 * @see org.iplantc.service.common.persistence.AbstractDao#getSession()
	 */
	@Override
	protected Session getSession() {
		HibernateUtil.beginTransaction();
		Session session = HibernateUtil.getSession();
		session.clear();
		session.enableFilter("notificationAttemptTenantFilter").setParameter("tenantId", TenancyHelper.getCurrentTenantId());
		return session;
	}
	
	/**
	 * Fetches a {@link NotificationAttempt} by its uuid.
	 * 
	 * @param uuid {@link NotificationAttempt#getUuid()} to search by
	 * @return
	 * @throws NotificationException
	 */
	public NotificationAttempt findByUuid(String uuid) throws NotificationException
	{
		try
		{
			Session session = getSession();
			
			String hql = "from NotificationAttempt where uuid = :uuid";
			
			NotificationAttempt attempt = (NotificationAttempt)session.createQuery(hql)
					.setString("uuid",uuid)
					.uniqueResult();
			
			session.flush();
			
			return attempt;
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
			
			throw new NotificationException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}
	
	/**
	 * Gets a {@link NotificationAttempt} by its uuid regardless of tenant.
	 * 
	 * @param uuid the {@link NotificationAttempt#getUuid()} to search by.
	 * @return
	 * @throws NotificationException
	 */
	public NotificationAttempt findByUuidAcrossTenants(String uuid) throws NotificationException
	{
		try
		{
			HibernateUtil.beginTransaction();
			Session session = HibernateUtil.getSession();
			session.clear();
			session.disableFilter("notificationTenantFilter");
			
			String sql = "from NotificationAttempt n where n.uuid = :uuid";
			
			NotificationAttempt notification = (NotificationAttempt)session.createQuery(sql)
					.setString("uuid",uuid)
					.uniqueResult();
			
			session.flush();
			
			return notification;
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
			
			throw new NotificationException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}
	
	/**
	 * Returns {@link NotificationAttempt} for a given {@link Notification} uuid.
	 * Pagination is honored through the {@code limit} and {@code offset} parameters.
	 * 
	 * @param notificationUuid uuid of the {@link Notification} for whom the attempts are made.
	 * @param limit
	 * @param offset
	 * @return
	 * @throws NotificationException
	 */
	@SuppressWarnings("unchecked")
	public List<NotificationAttempt> getByNotificationUuid(String notificationUuid, int limit, int offset) 
	throws NotificationException
	{
		if (StringUtils.isEmpty(notificationUuid)) {
			throw new NotificationException("No notification uuid provided.");
		}
		
		try
		{
			Session session = getSession();
			
			String hql = "FROM NotificationAttempt where a.notificationId = :uuid " +
					"ORDER BY created DESC";
			
			List<NotificationAttempt> attempts = (List<NotificationAttempt>)session.createQuery(hql)
					.setString("uuid",notificationUuid)
					.setMaxResults(limit)
					.setFirstResult(offset)
					.list();
			
			session.flush();
			
			return attempts;
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
			
			throw new NotificationException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}
	
	
	/**
	 * Saves or updates the {@link NotificationAttempt}
	 * @param attempt
	 * @throws NotificationException
	 */
	public void persist(NotificationAttempt attempt) throws NotificationException
	{
		if (attempt == null)
			throw new NotificationException("Notification cannot be null");

		try
		{	
			Session session = getSession();
			
			session.saveOrUpdate(attempt);
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
			
			throw new NotificationException("Failed to save notification", ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {e.printStackTrace();}
		}
	}

	/**
	 * Deletes a notification
	 * 
	 * @param profile
	 * @throws NotificationException
	 */
	public void delete(NotificationAttempt attempt) throws NotificationException
	{
		if (attempt == null)
			throw new NotificationException("NotificationAttempt cannot be null");

		try
		{
			Session session = getSession();
			session.disableFilter("notificationAttemptTenantFilter");
            
			session.delete(attempt);
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
			
			throw new NotificationException("Failed to delete notification", ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}

	@SuppressWarnings("unchecked")
	public List<NotificationAttempt> getAll() throws NotificationException
	{
		try
		{
			Session session = getSession();
			
			List<NotificationAttempt> attempts = (List<NotificationAttempt>) session.createQuery("FROM NotificationAttempt").list();
			
			session.flush();
			
			return attempts;
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
			
			throw new NotificationException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}
	
	public NotificationAttempt getLastAttemptForNotificationId(String notificationUuid) 
	throws NotificationException
	{
		if (StringUtils.isEmpty(notificationUuid)) {
			throw new NotificationException("No notification uuid provided.");
		}
		
		try
		{
			Session session = getSession();
			
			String hql = "FROM NotificationAttempt where a.notificationId = :uuid " +
					"ORDER BY created DESC";
			
			NotificationAttempt attempt = (NotificationAttempt)session.createQuery(hql)
					.setString("uuid",notificationUuid)
					.setMaxResults(1)
					.uniqueResult();
			
			session.flush();
			
			return attempt;
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
			
			throw new NotificationException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}
	
	/**
     * Searches for software by the given user who matches the given set of 
     * parameters. Permissions are honored in this query as in pagination
     *
     * @param username
     * @param searchCriteria
     * @param offset
     * @param limit
     * @return
     * @throws SoftwareException
     */
    @SuppressWarnings("unchecked")
    public List<NotificationAttempt> findMatching(String notificationId, String username, Map<SearchTerm, Object> searchCriteria, int offset, int limit) 
    throws NotificationException
    {
        try
        {
            Session session = getSession();
            session.clear();
//            String hql = "FROM NotificationAttempt c left join c.observableEvents observableEvents \n";
//            
//            if (!AuthorizationHelper.isTenantAdmin(username)) {
//                // no public search term specified. return public and private
//                hql +=  " WHERE ( \n" +
//                        "       c.owner = :owner OR \n" +
//                        "       c.id in ( \n" + 
//                        "               SELECT cp.resourceId FROM RealtimeChannelPermission cp \n" +
//                        "               WHERE cp.username = :owner AND cp.permission <> :none \n" +
//                        "              ) \n" +
//                        "      ) AND \n";
//            } else {
//                hql += " WHERE ";
//            }
            
            String hql = "FROM NotificationAttempt c "; 
        		 
            hql += "\n WHERE c.tenantId = :tenantid ";
            hql += "\n       AND       c.notificationId = :notificationId ";
            
            if (!AuthorizationHelper.isTenantAdmin(username)) {
				hql +=   "    AND c.owner = :username \n";
			}
            
            for (SearchTerm searchTerm: searchCriteria.keySet()) 
            {
                if (searchCriteria.get(searchTerm) == null 
                        || StringUtils.equalsIgnoreCase(searchCriteria.get(searchTerm).toString(), "null")) 
                {
                    if (searchTerm.getOperator() == SearchTerm.Operator.NEQ ) {
                        hql += "\n       AND       " + String.format(searchTerm.getMappedField(), searchTerm.getPrefix()) + " is not null ";
                    } else if (searchTerm.getOperator() == SearchTerm.Operator.EQ ) {
                        hql += "\n       AND       " + String.format(searchTerm.getMappedField(), searchTerm.getPrefix()) + " is null ";
                    } else {
                        hql += "\n       AND       " + searchTerm.getExpression();
                    }
                } else {
                    hql += "\n       AND       " + searchTerm.getExpression();
                }
            }
            
            hql +=  " ORDER BY c.name ASC\n";
            
            String q = hql;
            
            Query query = session.createQuery(hql)
                                 .setResultTransformer(Transformers.aliasToBean(NotificationAttempt.class))
                                 .setString("tenantid", TenancyHelper.getCurrentTenantId())
                                 .setString("notificationId", notificationId);
            
            q = q.replaceAll(":tenantid", "'" + TenancyHelper.getCurrentTenantId() + "'");
            q = q.replaceAll(":notificationId", "'" + notificationId + "'");
            
            if (!AuthorizationHelper.isTenantAdmin(username)) {
                query.setString("owner",username);
                q = q.replaceAll(":owner", "'" + username + "'");
            }
            
            for (SearchTerm searchTerm: searchCriteria.keySet()) 
            {
                if (searchTerm.getOperator() == SearchTerm.Operator.BETWEEN) {
                    List<String> formattedDates = (List<String>)searchTerm.getOperator().applyWildcards(searchCriteria.get(searchTerm));
                    for(int i=0;i<formattedDates.size(); i++) {
                        query.setString(searchTerm.getSafeSearchField()+i, formattedDates.get(i));
                        q = q.replaceAll(":" + searchTerm.getSafeSearchField(), "'" + formattedDates.get(i) + "'");
                    }
                }
                else if (searchTerm.getOperator().isSetOperator()) 
                {
                    query.setParameterList(searchTerm.getSafeSearchField(), (List<Object>)searchCriteria.get(searchTerm));
                    q = q.replaceAll(":" + searchTerm.getSafeSearchField(), "('" + StringUtils.join((List<String>)searchTerm.getOperator().applyWildcards(searchCriteria.get(searchTerm)), "','") + "')" );
                }
                else if (searchCriteria.get(searchTerm) == null 
                        || StringUtils.equalsIgnoreCase(searchCriteria.get(searchTerm).toString(), "null")
                        && (searchTerm.getOperator() == SearchTerm.Operator.NEQ || searchTerm.getOperator() == SearchTerm.Operator.EQ )) {
                    // this was explicitly set to 'is null' or 'is not null'
                }
                else 
                {
                    query.setParameter(searchTerm.getSafeSearchField(), 
                            searchTerm.getOperator().applyWildcards(searchCriteria.get(searchTerm)));
                    q = q.replaceAll(":" + searchTerm.getSafeSearchField(), "'" + String.valueOf(searchTerm.getOperator().applyWildcards(searchCriteria.get(searchTerm))) + "'");
                }
                
            }
            
            log.debug(q);
            
            List<NotificationAttempt> attempts = query
                    .setFirstResult(offset)
                    .setMaxResults(limit)
                    .list();
            
            session.flush();
            
            return attempts;

        }
        catch (Throwable ex)
        {
            throw new NotificationException(ex);
        }
        finally {
            try { HibernateUtil.commitTransaction();} catch (Exception e) {}
        }
    }

	/**
	 * Clears all {@link NotificationAttempt} for a given {@code notificationUuid}.
	 * 
	 * @param notificationUuid
	 * @throws NotificationException
	 */
	public void clearNotificationAttemptsforUuid(String notificationUuid) 
	throws NotificationException
	{
		if (StringUtils.isEmpty(notificationUuid))
			throw new NotificationException("Notification uuid cannot be null");

		try
		{
			Session session = getSession();
			session.createSQLQuery("DELETE FROM NotificationAttempt where notificationId = :nid")
				.setString("nid", notificationUuid)
				.executeUpdate();
			session.disableFilter("notificationAttemptTenantFilter");
            
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
			
			throw new NotificationException("Failed to delete notification attempts for notificaiton " 
					+ notificationUuid, ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}

}
