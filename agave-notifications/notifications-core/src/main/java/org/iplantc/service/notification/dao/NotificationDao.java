/**
 * 
 */
package org.iplantc.service.notification.dao;

import java.util.ArrayList;
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
import org.iplantc.service.notification.model.enumerations.NotificationEventType;
import org.iplantc.service.notification.model.enumerations.NotificationStatusType;

/**
 * Data access class for internal users.
 * 
 * @author dooley
 */
public class NotificationDao extends AbstractDao
{
	private static final Logger log = Logger.getLogger(NotificationDao.class);
	
	/* (non-Javadoc)
	 * @see org.iplantc.service.common.persistence.AbstractDao#getSession()
	 */
	@Override
	protected Session getSession() {
		HibernateUtil.beginTransaction();
		Session session = HibernateUtil.getSession();
//		session.clear();
		session.enableFilter("notificationTenantFilter")
			.setParameter("tenantId", TenancyHelper.getCurrentTenantId());
		return session;
	}
	
	/**
     * Gets all active notifications for a given user.
     * 
     * @param apiUsername
     * @return
     * @throws NotificationException
     */
    public List<Notification> getActiveUserNotifications(String username) 
    throws NotificationException
    {
        return getActiveUserNotifications(username, 0, org.iplantc.service.common.Settings.DEFAULT_PAGE_SIZE);
    }
    
	/**
	 * Gets all active notifications for a given user.
	 * 
	 * @param apiUsername owner of notification
	 * @param offset number of results to skip 
	 * @param limit max results
	 * @return
	 * @throws NotificationException
	 */
	@SuppressWarnings("unchecked")
	public List<Notification> getActiveUserNotifications(String username, int offset, int limit) 
	throws NotificationException
	{
		try
		{
			Session session = getSession();
			
			String hql = "FROM Notification " +
						 "WHERE status = :activeStatus ";
			if (!AuthorizationHelper.isTenantAdmin(username)) {
				hql +=   "    AND owner = :username ";
			}
			hql += 		 "ORDER BY created DESC";
			
			Query query = session.createQuery(hql)
					.setString("activeStatus", NotificationStatusType.ACTIVE.name());
			
			if (!AuthorizationHelper.isTenantAdmin(username)) {
				query.setString("username",username);
			}
	
			List<Notification> notifications = 
					(List<Notification>)query.setMaxResults(limit)
											.setFirstResult(offset)
											.list();
			
			session.flush();
			return notifications;
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
     * Gets all active notifications for a given user and associated uuid.
     * 
     * @param username
     * @param uuid
     * @return
     * @throws NotificationException
     */
    public List<Notification> getActiveUserNotificationsForUuid(String username, String associatedUuid) 
    throws NotificationException
    {
        return getActiveUserNotificationsForUuid(username, associatedUuid, 0, org.iplantc.service.common.Settings.DEFAULT_PAGE_SIZE);
    }
	
	/**
	 * Gets all active notifications for a given user and associated uuid.
	 * 
	 * @param username
	 * @param uuid
	 * @param offset number of results to skip 
     * @param limit max results
     * @return
	 * @throws NotificationException
	 */
	@SuppressWarnings("unchecked")
	public List<Notification> getActiveUserNotificationsForUuid(String username, String associatedUuid, int offset, int limit) 
	throws NotificationException
	{
		try
		{
			Session session = getSession();
			
			String hql = "FROM Notification \n" +
					 	 "WHERE status = :activeStatus \n" + 
					 	 "    AND (associatedUuid = :associatedUuid OR associatedUuid = :wildcarduuid) \n";
			if (!AuthorizationHelper.isTenantAdmin(username)) {
				hql +=   "    AND owner = :username \n";
			}
			
			hql += 		 "ORDER BY created DESC";
			
			
			Query query = session.createQuery(hql)
					.setString("activeStatus", NotificationStatusType.ACTIVE.name())
					.setString("wildcarduuid", "*")
					.setString("associatedUuid",associatedUuid);
					
			if (!AuthorizationHelper.isTenantAdmin(username)) {
				query.setString("username",username);
			}
	
			List<Notification> notifications = 
					(List<Notification>)query.setMaxResults(limit)
											.setFirstResult(offset)
											.list();
			
			session.flush();
			
			return notifications;
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
	 * Gets all active notifications for all users on a given uuid.
	 * 
	 * @param uuid
	 * @return
	 * @throws NotificationException
	 */
	@SuppressWarnings("unchecked")
	public List<Notification> getActiveNotificationsForUuid(String associatedUuid) 
	throws NotificationException
	{
		try
		{
			Session session = getSession();
			
			String hql = "FROM Notification \n" +
				 	 	 "WHERE status = :activeStatus \n" + 
				 	 	 "    AND (associatedUuid = :associatedUuid OR associatedUuid = :wildcarduuid) \n";
			
			hql += 		 "ORDER BY created DESC";
			
			List<Notification> notifications = (List<Notification>)session.createQuery(hql)
					.setString("wildcarduuid", "*")
					.setString("associatedUuid",associatedUuid)
					.setString("activeStatus", NotificationStatusType.ACTIVE.name())
					.list();
			
			session.flush();
			
			return notifications;
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
	 * Get for a given uuid.
	 * 
	 * @param uuid
	 * @return
	 * @throws NotificationException
	 */
	public Notification findByUuid(String uuid) throws NotificationException
	{
		try
		{
			Session session = getSession();
			
			String hql = "from Notification t where t.uuid = :uuid";
			
			Notification notification = (Notification)session.createQuery(hql)
					.setString("uuid",uuid)
					.uniqueResult();
			
//			session.flush();
			
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
	 * Gets a notification by uuid regardless of tenant.
	 * 
	 * @param apiUsername
	 * @return
	 * @throws NotificationException
	 */
	public Notification findByUuidAcrossTenants(String uuid) throws NotificationException
	{
		try
		{
			HibernateUtil.beginTransaction();
			Session session = HibernateUtil.getSession();
			session.clear();
			session.disableFilter("notificationTenantFilter");
			
			String sql = "from Notification n where n.uuid = :uuid";
			
			Notification notification = (Notification)session.createQuery(sql)
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
	 * Returns all notifications for the given associated uuid registered for the
	 * given event or the wildcard event. 
	 * 
	 * @param associatedUuid
	 * @param notificationEvent
	 * @return
	 * @throws NotificationException
	 */
	@SuppressWarnings("unchecked")
	public List<Notification> getActiveForAssociatedUuidAndEvent(String associatedUuid, String notificationEvent) 
	throws NotificationException
	{
		if (StringUtils.isEmpty(associatedUuid)) {
			throw new NotificationException("No UUID provided.");
		}
		
		if (StringUtils.isEmpty(notificationEvent)) {
			throw new NotificationException("No notification event provided.");
		}
		
		try
		{
			Session session = getSession();
			
			String hql = "FROM Notification \n" +
				 	 	 "WHERE status = :activeStatus \n" + 
				 	 	 "    AND (associatedUuid = :associatedUuid OR associatedUuid = :wildcarduuid) \n" +
						 "    AND (event = :event OR event = :wildcardevent) \n";
			hql += 		 "ORDER BY created DESC ";
			
			List<Notification> notifications = (List<Notification>)session.createQuery(hql)
					.setString("activeStatus", NotificationStatusType.ACTIVE.name())
					.setString("wildcarduuid", "*")
					.setString("associatedUuid",associatedUuid)
					.setString("wildcardevent", "*")
					.setString("event", notificationEvent)
					.list();
			
//			session.flush();
			
			return notifications;
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
	 * Merges the current instance with the one in the db.
	 * @param notification
	 * @return the merged Notification
	 * @throws NotificationException
	 */
	public boolean update(Notification notification) throws NotificationException
	{
		if (notification == null)
			throw new NotificationException("Notification cannot be null");

		try
		{	
			HibernateUtil.beginTransaction();
			Session session = HibernateUtil.getSession();
			session.clear();
			session.disableFilter("notificationTenantFilter");
			
			String sql = "UPDATE notifications n "
					+ "SET  n.status = :status, "
					+ "	    n.last_updated = CURRENT_TIMESTAMP, "
					+ "	    n.associated_uuid = :associateduuid, "
					+ "	    n.notification_event = :event, "
					+ "	    n.callback_url = :callbackurl, "
					+ "	    n.is_persistent = :ispersistent, "
					+ "	    n.is_visible = :isvisible, "
					+ "	    n.retry_strategy = :retrystrategy, "
					+ "	    n.retry_limit = :retrylimit, "
					+ "	    n.retry_rate = :retryrate, "
					+ "	    n.retry_delay = :retrydelay, "
					+ "	    n.save_on_failure = :saveonfailure "
					+ "WHERE n.uuid = :uuid ";
			
			int rowsAffected = session.createSQLQuery(sql)
					.setString("uuid",notification.getUuid())
					.setString("status", notification.getStatus().name())
					.setString("associateduuid",notification.getAssociatedUuid())
					.setString("event",notification.getEvent())
					.setString("callbackurl",notification.getCallbackUrl())
					.setInteger("ispersistent", notification.isPersistent() ? 1: 0)
					.setInteger("isvisible",notification.isVisible() ? 1: 0)
					.setString("retrystrategy",notification.getPolicy().getRetryStrategyType().name())
					.setInteger("retrylimit",notification.getPolicy().getRetryLimit())
					.setInteger("retryrate",notification.getPolicy().getRetryRate())
					.setInteger("retrydelay",notification.getPolicy().getRetryDelay())
					.setInteger("saveonfailure",notification.getPolicy().isSaveOnFailure() ? 1 : 0)
					.executeUpdate();
			
			session.flush();
			
			return rowsAffected > 0;
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
			
			throw new NotificationException("Failed to merge notification", ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {e.printStackTrace();}
		}
	}

	/**
	 * Saves or updates the InternalUser
	 * @param notification
	 * @throws NotificationException
	 */
	public void persist(Notification notification) throws NotificationException
	{
		if (notification == null)
			throw new NotificationException("Notification cannot be null");

		try
		{	
			Session session = getSession();
			
			session.saveOrUpdate(notification);
//			session.clear();
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
	public void delete(Notification notification) throws NotificationException
	{

		if (notification == null)
			throw new NotificationException("Notification cannot be null");

		try
		{
			Session session = getSession();
//			session.disableFilter("notificationTenantFilter");
            
			session.delete(notification);
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
	public List<Notification> getAll() throws NotificationException
	{
		try
		{
			Session session = getSession();
			
			List<Notification> users = (List<Notification>) session.createQuery("FROM Notification").list();
			
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
			
			throw new NotificationException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}	
	
	/**
     * Searches for an entity by search criteria. Permissions are honored in this query as is pagination.
     *
     * @param username
     * @param searchCriteria
     * @param offset
     * @param limit
     * @return
     * @throws NotificationException
     */
    @SuppressWarnings("unchecked")
    public List<Notification> findMatching(String username, Map<SearchTerm, Object> searchCriteria, int offset, int limit) 
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
            
            String hql = "FROM Notification c "; 
        		 
            hql += "\n WHERE c.tenantId = :tenantid ";
            
            if (!AuthorizationHelper.isTenantAdmin(username)) {
				hql +=   "    AND c.owner = :owner \n";
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
            
            hql +=  " ORDER BY c.created DESC\n";
            
            String q = hql;
            
            Query query = session.createQuery(hql)
                                 .setString("tenantid", TenancyHelper.getCurrentTenantId());
            
            q = q.replaceAll(":tenantid", "'" + TenancyHelper.getCurrentTenantId() + "'");
            
            if (!AuthorizationHelper.isTenantAdmin(username)) {
                query.setString("owner", username);
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
            
//            log.debug(q);
            List<Notification> attempts = query
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
	 * Performs a sql update of the a notification row setting the 
	 * {@link Notification#getLastUpdated()} timestamp at the same time.
	 * 
	 * @param notificationUuid uuid of the {@link Notification}
	 * @param status the new status of the {@link Notification}
	 * @return
	 * @throws NotificationException
	 */
	public boolean updateStatus(String notificationUuid, NotificationStatusType status) 
	throws NotificationException
	{
		try
		{
			HibernateUtil.beginTransaction();
			Session session = HibernateUtil.getSession();
			session.clear();
			session.disableFilter("notificationTenantFilter");
			
			String sql = "UPDATE notifications n "
					+ "SET n.status = :status, n.last_updated = CURRENT_TIMESTAMP "
					+ "WHERE n.uuid = :uuid ";
			
			int rowsAffected = session.createSQLQuery(sql)
					.setString("uuid",notificationUuid)
					.setString("status", status.name())
					.executeUpdate();
			
			session.flush();
			
			return rowsAffected > 0;
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
	 * Adds new {@link Notification} records for all the provided <code>events</code> that
	 * do not already have existing subscriptions. Tenancy and ownership are preserved.
	 * 
	 * @param associatedUuid
	 * @param createdBy
	 * @param events
	 * @param callbackUrl
	 * @throws NotificationException
	 */
	@SuppressWarnings("unchecked")
	public List<Notification> addUniqueNotificationForEvents(
			String associatedUuid, String createdBy, List<String> events, String callbackUrl, boolean isPersistent) 
	throws NotificationException
	{
		List<Notification> newNotifications = new ArrayList<Notification>();
		
		if (events == null || events.isEmpty()) {
			return newNotifications;
		}
		
		try
		{
			HibernateUtil.beginTransaction();
			Session session = HibernateUtil.getSession();
			session.clear();
			session.disableFilter("notificationTenantFilter");
			
			
			String sql = "SELECT distinct n.event "
					+ "FROM Notification n \n"
					+ "WHERE n.status = :status and \n "
					+ "		 n.visible = :visible and \n "
					+ "		 n.owner = :owner and \n"
					+ "		 ( n.event in (:events) or n.event = :wildcard ) and \n"
					+ "		 ( associatedUuid = :uuid or n.associatedUuid = :wildcard ) \n";
			
			List<String> existingSubscriptions = (List<String>)session.createQuery(sql)
					.setString("status",NotificationStatusType.ACTIVE.name())
					.setString("uuid", associatedUuid)
					.setBoolean("visible",true)
					.setString("owner", createdBy)
					.setString("wildcard", "*")
					.setString("events", "'" + StringUtils.join(events,"','") + "'")
					.list();
			
			if (existingSubscriptions == null) {
				existingSubscriptions = new ArrayList<String>();
			}
			
			// add all missing notifications. wildcard excludes everything
			if (!existingSubscriptions.contains("*")) {
				for (String event: events) {
					if (!existingSubscriptions.contains(event)) {
						Notification notification = new Notification(associatedUuid, createdBy, event, callbackUrl, isPersistent);
						try {
							persist(notification);
							newNotifications.add(notification);
						}
						catch (Exception e) {}
					}
				}
			}
			
			// save the flush until here so we can do bulk insert the previous transactions
			session.flush();
			
			return newNotifications;
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
	 * Soft deletes a notification
	 * @param notificationUuid
	 */
	public void softDeleteNotification(String notificationUuid) 
	throws NotificationException
	{
		try
		{
			HibernateUtil.beginTransaction();
			Session session = HibernateUtil.getSession();
			session.clear();
			session.disableFilter("notificationTenantFilter");
			
			String sql = "UPDATE notifications n "
					+ "SET n.status = :status, n.is_visible = :visible, n.last_updated = CURRENT_TIMESTAMP "
					+ "WHERE n.uuid = :uuid ";
			
			int rowsAffected = session.createSQLQuery(sql)
					.setString("uuid",notificationUuid)
					.setString("status", NotificationStatusType.INACTIVE.name())
					.setBoolean("visible", false)
					.executeUpdate();
			
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
			
			throw new NotificationException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}
}
