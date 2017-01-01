/**
 * 
 */
package org.iplantc.service.realtime.dao;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.transform.Transformers;
import org.iplantc.service.apps.exceptions.SoftwareException;
import org.iplantc.service.common.Settings;
import org.iplantc.service.common.auth.AuthorizationHelper;
import org.iplantc.service.common.dao.AbstractDao;
import org.iplantc.service.common.persistence.HibernateUtil;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.common.search.SearchTerm;
import org.iplantc.service.realtime.exceptions.RealtimeChannelException;
import org.iplantc.service.realtime.model.RealtimeChannel;
import org.iplantc.service.realtime.model.enumerations.PermissionType;

/**
 * Data access class for internal users.
 * 
 * @author dooley
 */
public class RealtimeChannelDao extends AbstractDao
{
	private static final Logger log = Logger.getLogger(RealtimeChannelDao.class);
	
	/* (non-Javadoc)
	 * @see org.iplantc.service.common.persistence.AbstractDao#getSession()
	 */
	@Override
	protected Session getSession() {
		HibernateUtil.beginTransaction();
		Session session = HibernateUtil.getSession();
		session.clear();
		session.enableFilter("realtimeChanelTenantFilter").setParameter("tenantId", TenancyHelper.getCurrentTenantId());
		return session;
	}
	
	
	/**
	 *  Gets all tags for a given user filtering by active status and system
	 *  
	 * @param username
	 * @param includeActive
	 * @param tagId
	 * @return
	 * @throws RealtimeChannelException
	 */
	@SuppressWarnings("unchecked")
	public List<RealtimeChannel> getUserChannels(String username, boolean includePublic) 
	throws RealtimeChannelException
	{
		try
		{
			Session session = getSession();
			
			String hql = "from RealtimeChannel c left outer join RealtimeChannelPermission p on c.id = p.id " +
					"where c.owner = :owner ";
			
			if (includePublic) {
	            hql +=  "c.owner = :pemUser " +
	                    "OR p.username = :pemUser " +
	                    "OR p.username = :publicUser " +
	                    "OR p.username = :worldUser " +
	                "ORDER BY c.name ASC ";
	        } else {
	            hql +=  "c.owner = :pemUser " +
	                    "OR pems.username = :pemUser) " +
	                "ORDER BY c.name ASC ";
	        }
			
			Query query = session.createQuery(hql)
					.setString("owner",username)
					.setString("pemUser",username);
			
			if (includePublic) {
				query.setString("publicUser", Settings.PUBLIC_USER_USERNAME);
				query.setString("worldUser", Settings.WORLD_USER_USERNAME);
			}
			
			List<RealtimeChannel> tags = (List<RealtimeChannel>)query.list();
			
			session.flush();
			return tags;
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
			
			throw new RealtimeChannelException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}
	
	/**
	 * Find {@link RealtimeChannel}s with the given uuid regardless of tenant.
	 * 
	 * @param uuid
	 * @return
	 * @throws RealtimeChannelException
	 */
	public RealtimeChannel findByUuid(String uuid) throws RealtimeChannelException
	{
		try
		{
			HibernateUtil.beginTransaction();
			Session session = HibernateUtil.getSession();
			session.clear();
			
			String hql = "from RealtimeChannel c where c.uuid = :uuid";
			
			RealtimeChannel tag = (RealtimeChannel)session.createQuery(hql)
					.setString("uuid",uuid)
					.uniqueResult();
			
			session.flush();
			
			return tag;
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
			
			throw new RealtimeChannelException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}
	
	/**
	 * Returns the {@link RealtimeChannel} matching the given uuid within the current tenant id
	 * 
	 * @param uuid
	 * @return {@link RealtimeChannel} with the matching uuid
	 * @throws RealtimeChannelException
	 */
	public RealtimeChannel findByUuidWithinSessionTenant(String uuid) throws RealtimeChannelException
	{
		try
		{
			Session session = getSession();
			
			String hql = "from RealtimeChannel c where c.uuid = :uuid";
			
			RealtimeChannel tag = (RealtimeChannel)session.createQuery(hql)
					.setString("uuid",uuid)
					.uniqueResult();
			
			session.flush();
			
			return tag;
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
			
			throw new RealtimeChannelException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}
	
	/**
	 * Saves or updates the {@link RealtimeChannel}
	 * @param tag
	 * @throws RealtimeChannelException
	 */
	public void persist(RealtimeChannel tag) throws RealtimeChannelException
	{
		if (tag == null)
			throw new RealtimeChannelException("RealtimeChannel cannot be null");

		try
		{	
			HibernateUtil.beginTransaction();
			Session session = HibernateUtil.getSession();
			session.clear();
			
			session.saveOrUpdate(tag);
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
			
			throw new RealtimeChannelException("Failed to save tag", ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {e.printStackTrace();}
		}
	}

	/**
	 * Deletes a {@link RealtimeChannel}
	 * 
	 RealtimeChannelparam tag
	 * @throws RealtimeChannelException
	 */
	public void delete(RealtimeChannel tag) throws RealtimeChannelException
	{

		if (tag == null)
			throw new RealtimeChannelException("RealtimeChannel cannot be null");

		try
		{
			HibernateUtil.beginTransaction();
			Session session = HibernateUtil.getSession();
			session.clear();
			session.delete(tag);
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
			
			throw new RealtimeChannelException("Failed to delete tag", ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}

	/**
	 * Merges the active {@link RealtimeChannel} back with the saved instance. Associations are
	 * not updated.
	 * @param tag
	 * @return
	 * @throws RealtimeChannelException
	 */
	public RealtimeChannel merge(RealtimeChannel tag) throws RealtimeChannelException
	{
		try
		{
			HibernateUtil.beginTransaction();
			Session session = HibernateUtil.getSession();
			session.clear();
			
			RealtimeChannel mergedTag = (RealtimeChannel)session.merge(tag);
			
			//session.flush();
			
			return mergedTag;
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
			throw new RealtimeChannelException("Failed to merge tag", ex);
		}
		finally
		{
			try { HibernateUtil.commitTransaction(); } catch (Throwable e) {}
		}
	}
	
	/**
	 * Resyncs the {@link RealtimeChannel} with the stored version replacing
	 * any local changes.
	 * 
	 * @param tag
	 * @throws RealtimeChannelException
	 */
	public void refresh(RealtimeChannel tag) throws RealtimeChannelException
	{
		try
		{
			HibernateUtil.beginTransaction();
			Session session = HibernateUtil.getSession();
			session.clear();
			
			session.refresh(tag);
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
			throw new RealtimeChannelException("Failed to merge tag", ex);
		}
		finally
		{
			try { HibernateUtil.commitTransaction(); } catch (Throwable e) {}
		}
	}
	
	/**
	 * Fetch all {@link RealtimeChannel}s.
	 * 
	 * @return
	 * @throws RealtimeChannelException
	 */
	@SuppressWarnings("unchecked")
	public List<RealtimeChannel> getAll() throws RealtimeChannelException
	{
		try
		{
			Session session = getSession();
			
			List<RealtimeChannel> users = (List<RealtimeChannel>) session.createQuery("FROM RealtimeChannel").list();
			
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
			
			throw new RealtimeChannelException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}

	/**
	 * Returns true if a {@link RealtimeChannel} by the same name already exists for the given user.
	 * TODO: Should we match on subscribed events as well?
	 * @param username
	 * @param channelName
	 * @return
	 * @throws RealtimeChannelException
	 */
	public boolean doesExist(String username, String channelName) throws RealtimeChannelException 
	{
		try
		{
			Session session = getSession();
			
			String sql = "select uuid as from realtimechannels where name = :name and owner = :owner and tenant_id = :tenantId";
			
			String match = (String)session.createSQLQuery(sql)
					.setString("name", channelName)
					.setString("owner", username)
					.setString("tenantId", TenancyHelper.getCurrentTenantId())
					.setMaxResults(1)
					.uniqueResult();
			
			session.flush();
			return match != null;
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
			
			throw new RealtimeChannelException(ex);
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
    public List<RealtimeChannel> findMatching(String username, Map<SearchTerm, Object> searchCriteria, int offset, int limit) 
    throws RealtimeChannelException
    {
        try
        {
            Session session = getSession();
            session.clear();
            String hql = "FROM RealtimeChannel c left join c.observableEvents observableEvents \n";
            
            if (!AuthorizationHelper.isTenantAdmin(username)) {
                // no public search term specified. return public and private
                hql +=  " WHERE ( \n" +
                        "       c.owner = :owner OR \n" +
                        "       c.id in ( \n" + 
                        "               SELECT cp.resourceId FROM RealtimeChannelPermission cp \n" +
                        "               WHERE cp.username = :owner AND cp.permission <> :none \n" +
                        "              ) \n" +
                        "      ) AND \n";
            } else {
                hql += " WHERE ";
            }
            
            hql +=  "        c.tenantId = :tenantid "; 
            
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
                                 .setResultTransformer(Transformers.aliasToBean(RealtimeChannel.class))
                                 .setString("tenantid", TenancyHelper.getCurrentTenantId());
            
            q = q.replaceAll(":tenantid", "'" + TenancyHelper.getCurrentTenantId() + "'");
            
            if (!AuthorizationHelper.isTenantAdmin(username)) {
                query.setString("owner",username)
                    .setString("none",PermissionType.NONE.name());
                q = q.replaceAll(":owner", "'" + username + "'")
                    .replaceAll(":none", "'NONE'");
            }
            
            for (SearchTerm searchTerm: searchCriteria.keySet()) 
            {
                if (searchTerm.getOperator() == SearchTerm.Operator.BETWEEN || searchTerm.getOperator() == SearchTerm.Operator.ON) {
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
            
            List<RealtimeChannel> realtimeChannels = query
                    .setFirstResult(offset)
                    .setMaxResults(limit)
                    .list();
            
            session.flush();
            
            return realtimeChannels;

        }
        catch (Throwable ex)
        {
            throw new RealtimeChannelException(ex);
        }
        finally {
            try { HibernateUtil.commitTransaction();} catch (Exception e) {}
        }
    }
}
