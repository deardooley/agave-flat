/**
 * 
 */
package org.iplantc.service.tags.dao;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
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
import org.iplantc.service.common.uuid.AgaveUUID;
import org.iplantc.service.common.uuid.UUIDType;
import org.iplantc.service.tags.exceptions.TagException;
import org.iplantc.service.tags.model.Tag;
import org.iplantc.service.tags.model.enumerations.PermissionType;

/**
 * Data access class for internal users.
 * 
 * @author dooley
 */
public class TagDao extends AbstractDao
{
	/* (non-Javadoc)
	 * @see org.iplantc.service.common.persistence.AbstractDao#getSession()
	 */
	@Override
	protected Session getSession() {
		HibernateUtil.beginTransaction();
		Session session = HibernateUtil.getSession();
		session.clear();
		session.enableFilter("tagTenantFilter").setParameter("tenantId", TenancyHelper.getCurrentTenantId());
		return session;
	}
	
	
	/**
	 *  Gets all tags for a given user filtering by active status and system
	 *  
	 * @param username
	 * @param includeActive
	 * @param includeInactive
	 * @param tagId
	 * @return
	 * @throws TagException
	 */
	@SuppressWarnings("unchecked")
	public List<Tag> getUserTags(String username, int limit, int offset)
	throws TagException
	{
		try
		{
			Session session = getSession();
			
			Query query = session.createQuery("from Tag where owner = :owner order by created desc")
								 .setString("owner", username)
								 .setMaxResults(limit)
								 .setFirstResult(offset);
			
			List<Tag> tags = (List<Tag>)query.list();
			
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
			
			throw new TagException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}
	
	/**
	 * Find {@link Tag}s with the given uuid regardless of tenant.
	 * 
	 * @param uuid
	 * @return
	 * @throws TagException
	 */
	public Tag findByUuid(String uuid) throws TagException
	{
		try
		{
			HibernateUtil.beginTransaction();
			Session session = HibernateUtil.getSession();
			session.clear();
			
			String hql = "from Tag t where t.uuid = :uuid";
			
			Tag tag = (Tag)session.createQuery(hql)
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
			
			throw new TagException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}
	
	/**
	 * Find a {@link Tag} by either name or uuid.
	 * 
	 * @param uuid
	 * @return
	 * @throws TagException
	 */
	public Tag findByNameOrUuid(String uuidOrName, String username) throws TagException
	{
		if (StringUtils.isEmpty(uuidOrName)) {
			return null;
		}
		else {
			try {
				AgaveUUID uuid = new AgaveUUID(uuidOrName);
				if (UUIDType.TAG == uuid.getResourceType()) {
					return findByUuid(uuidOrName);
				}
				// it's still a valid name, go for it
				else {
					return findByNameAndOwner(uuidOrName, username);
				}
			}
			catch (Exception e) {
				return findByNameAndOwner(uuidOrName, username);
			}
		}
	}
		
	@SuppressWarnings("unchecked")
	public Tag findByNameAndOwner(String name, String owner) 
	throws TagException 
	{
		try
		{	
			HibernateUtil.beginTransaction();
			Session session = HibernateUtil.getSession();
			session.clear();
			
			String hql = "from Tag t where t.name = :name and " +
					"		( " +
					"			t.owner = :owner OR \n" +
                    "	        t.id in ( \n" + 
                    "               SELECT tp.entityId FROM TagPermission tp \n" +
                    "               WHERE tp.username = :owner AND tp.permission <> :none \n" +
                    "           ) \n" +
                    "       ) \n" +
                    "      )";
			
			List<Tag> tags = (List<Tag>)session.createQuery(hql)
					.setString("name",name)
					.setString("owner", owner)
					.setString("none", PermissionType.NONE.name())
					.list();
			
			session.flush();
			
			if (tags.size() > 0) {
				for (Tag tag: tags) {
					if (StringUtils.equals(tag.getOwner(), owner)) {
						return tag;
					}
				}
			}
			
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
			
			throw new TagException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}
	
	/**
	 * Returns the {@link Tag} matching the given uuid within the current tenant id
	 * 
	 * @param uuid
	 * @return {@link Tag} with the matching uuid
	 * @throws TagException
	 */
	public Tag findByUuidWithinSessionTenant(String uuid) throws TagException
	{
		try
		{
			Session session = getSession();
			
			String hql = "from Tag t where t.uuid = :uuid";
			
			Tag tag = (Tag)session.createQuery(hql)
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
			
			throw new TagException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}
	
	/**
	 * Saves or updates the {@link Tag}
	 * @param tag
	 * @throws TagException
	 */
	public void persist(Tag tag) throws TagException
	{
		if (tag == null)
			throw new TagException("Tag cannot be null");

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
			
			throw new TagException("Failed to save tag", ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {e.printStackTrace();}
		}
	}

	/**
	 * Deletes a {@link Tag}
	 * 
	 * @param tag
	 * @throws TagException
	 */
	public void delete(Tag tag) throws TagException
	{

		if (tag == null)
			throw new TagException("Tag cannot be null");

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
			
			throw new TagException("Failed to delete tag", ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}

	/**
	 * Merges the active {@link Tag} back with the saved instance. Associations are
	 * not updated.
	 * @param tag
	 * @return
	 * @throws TagException
	 */
	public Tag merge(Tag tag) throws TagException
	{
		try
		{
			HibernateUtil.beginTransaction();
			Session session = HibernateUtil.getSession();
			session.clear();
			
			Tag mergedTag = (Tag)session.merge(tag);
			
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
			throw new TagException("Failed to merge tag", ex);
		}
		finally
		{
			try { HibernateUtil.commitTransaction(); } catch (Throwable e) {}
		}
	}
	
	/**
	 * Resyncs the {@link Tag} with the stored version replacing
	 * any local changes.
	 * 
	 * @param tag
	 * @throws TagException
	 */
	public void refresh(Tag tag) throws TagException
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
			throw new TagException("Failed to merge tag", ex);
		}
		finally
		{
			try { HibernateUtil.commitTransaction(); } catch (Throwable e) {}
		}
	}
	
	/**
	 * Fetch all {@link Tag}s.
	 * 
	 * @return
	 * @throws TagException
	 */
	@SuppressWarnings("unchecked")
	public List<Tag> getAll() throws TagException
	{
		try
		{
			Session session = getSession();
			
			List<Tag> users = (List<Tag>) session.createQuery("FROM Tag").list();
			
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
			
			throw new TagException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}

	/**
	 * Returns true if a {@link Tag} by the same name already exists for the given user.
	 * @param name name of tag
	 * @param username owner of tag
	 * @return true of a match exists within the current tenant. false otherwise
	 * @throws TagException
	 */
	public boolean doesTagNameExistForUser(String name, String username) throws TagException 
	{
		try
		{
			Session session = getSession();
			
			String sql = "select uuid from tags where name = :name and owner = :owner and tenant_id = :tenantId";
			
			String match = (String)session.createSQLQuery(sql)
					.setString("name", name)
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
			
			throw new TagException(ex);
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
    public List<Tag> findMatching(String username, Map<SearchTerm, Object> searchCriteria, int offset, int limit) 
    throws TagException
    {
        try
        {
            Session session = getSession();
            session.clear();
            String hql = "FROM Tag t left join t.taggedResources taggedResource \n";
            
            SearchTerm publicSearchTerm = null;
            for (SearchTerm searchTerm: searchCriteria.keySet()) {
                if (searchTerm.getMappedField().startsWith("public")) {
                    publicSearchTerm = searchTerm;
                }
            }
            if (!AuthorizationHelper.isTenantAdmin(username)) {
                // no public search term specified. return public and private
                if (publicSearchTerm == null) {
                    hql +=  " WHERE ( \n" +
                            "       t.owner = :owner OR \n" +
                            "       t.id in ( \n" + 
                            "               SELECT tp.tag.id FROM TagPermission tp \n" +
                            "               WHERE tp.username = :owner AND tp.permission <> :none \n" +
                            "              ) \n" +
                            "      ) AND \n";
                } 
                // public = false || public.eq = false || public.neq = true, return only private
                else if ((publicSearchTerm.getOperator() == SearchTerm.Operator.EQ && 
                            !(Boolean)searchCriteria.get(publicSearchTerm)) || 
                        (publicSearchTerm.getOperator() == SearchTerm.Operator.NEQ && 
                            (Boolean)searchCriteria.get(publicSearchTerm))) {
                    hql +=  " WHERE ( \n" +
                            "       t.owner = :owner OR \n" +
                            "       t.id in ( \n" + 
                            "               SELECT tp.tag.id FROM TagPermission tp \n" +
                            "               WHERE tp.username = :owner AND tp.permission <> :none \n" +
                            "              ) \n" +
                            "      ) AND \n";
                } 
                // else return public apps. they will be included in the general where clause
                else {
                    
                }
            } else {
                hql += " WHERE ";
            }
            
            hql +=  "        t.tenantId = :tenantid "; 
            
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
            
            hql +=  " ORDER BY t.name ASC\n";
            
            String q = hql;
            
            Query query = session.createQuery(hql)
                                 .setResultTransformer(Transformers.aliasToBean(Tag.class))
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
            
            List<Tag> tags = query
                    .setFirstResult(offset)
                    .setMaxResults(limit)
                    .list();
            
            session.flush();
            
            return tags;

        }
        catch (Throwable ex)
        {
            throw new TagException(ex);
        }
        finally {
            try { HibernateUtil.commitTransaction();} catch (Exception e) {}
        }
    }


	public boolean doesUserTagExistWithName(String name) {
		// TODO Auto-generated method stub
		return false;
	}
}
