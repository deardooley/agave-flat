/**
 * 
 */
package org.iplantc.service.tags.dao;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.iplantc.service.tags.exceptions.TagEventPersistenceException;
import org.iplantc.service.tags.model.TagEvent;
import org.iplantc.service.tags.model.enumerations.TagEventType;
import org.iplantc.service.common.Settings;
import org.iplantc.service.common.persistence.HibernateUtil;
import org.iplantc.service.common.persistence.TenancyHelper;

/**
 * Model class for interacting with a {@link Tag}'s history. {@link TagEvent}s are
 * not persisted as mapped entities in the Tag class due to the
 * potentially large number.
 * 
 * @author dooley
 * 
 */
public class TagEventDao {

	protected Session getSession() {
		Session session = HibernateUtil.getSession();
		HibernateUtil.beginTransaction();
        session.enableFilter("tagEventTenantFilter").setParameter("tenantId", TenancyHelper.getCurrentTenantId());
		session.clear();
		return session;
	}
	
	/**
	 * Returns the tag event with the given id.
	 * 
	 * @param eventId
	 * @return
	 * @throws TagEventPersistenceException
	 */
	public TagEvent getById(Long eventId)
	throws TagEventPersistenceException
	{

		if (eventId == null)
			throw new TagEventPersistenceException("Event id cannot be null");

		try
		{
			Session session = getSession();
			
			TagEvent event = (TagEvent)session.get(TagEvent.class, eventId);
			
			session.flush();
			
			return event;
		}
		catch (HibernateException ex)
		{
			throw new TagEventPersistenceException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}
	
	/**
	 * Returns all tag tag events for the tag with the given id.
	 * 
	 * @param tagUuid
	 * @return
	 * @throws TagEventPersistenceException
	 */
	public List<TagEvent> getByTagUuid(String tagUuid)
	throws TagEventPersistenceException
	{
		return getByTagUuid(tagUuid, Settings.DEFAULT_PAGE_SIZE, 0);
	}
	
	/**
	 * Returns all tag tag events for the tag with the given id. Pagination
	 * is supported.
	 * 
	 * @param tagUuid
	 * @param limit
	 * @param offset
	 * @return
	 * @throws TagEventPersistenceException
	 */
	@SuppressWarnings("unchecked")
	public List<TagEvent> getByTagUuid(String tagUuid, int limit, int offset)
	throws TagEventPersistenceException
	{

		if (StringUtils.isEmpty(tagUuid))
			throw new TagEventPersistenceException("Tag uuid cannot be null");

		try
		{
			Session session = getSession();
			
			String sql = "select * from tagevents where entity_uuid = :uuid order by id asc";
			List<TagEvent> events = session.createSQLQuery(sql)
			        .addEntity(TagEvent.class)
                    .setString("uuid", tagUuid)
					.setFirstResult(offset)
					.setMaxResults(limit)
					.list();
			
			session.flush();
			
			return events;
		}
		catch (HibernateException ex)
		{
			throw new TagEventPersistenceException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}
	
	/**
     * @param tagUuid
     * @param limit
     * @param offset
     * @return
     * @throws TagEventPersistenceException
     */
    public TagEvent getByTagUuidAndUuid(String tagUuid, String uuid)
    throws TagEventPersistenceException
    {

        if (tagUuid == null)
            throw new TagEventPersistenceException("Tag id cannot be null");
        
        if (StringUtils.isEmpty(uuid)) 
            throw new TagEventPersistenceException("Tag event uuid cannot be null");

        try
        {
            Session session = getSession();
            
            String sql = "select * from tagevents where entity_uuid = :tagUuid and uuid = :uuid";
            TagEvent event = (TagEvent)session.createSQLQuery(sql)
                    .addEntity(TagEvent.class)
                    .setString("tagUuid", tagUuid)
                    .setString("uuid", uuid)
                    .uniqueResult();
            
            session.flush();
            
            return event;
        }
        catch (HibernateException ex)
        {
            throw new TagEventPersistenceException(ex);
        }
        finally {
            try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
        }
    }

	/**
	 * Gets the tag events for the specified tag id and tag status
	 * 
	 * @param tagId
	 * @param status
	 * @return
	 * @throws TagEventPersistenceException
	 */
	@SuppressWarnings("unchecked")
	public List<TagEvent> getByTagUuidAndStatus(String tagUuid, TagEventType eventType) 
	throws TagEventPersistenceException
	{
		if (eventType == null)
			throw new TagEventPersistenceException("eventType cannot be null");
		
		if (StringUtils.isEmpty(tagUuid))
			throw new TagEventPersistenceException("tag uuid cannot be null");

		try
		{
			Session session = getSession();
			
			String hql = "select * from tagevents where entity_uuid = :tagUuid and status = :status order by id asc";
			List<TagEvent> events = session.createSQLQuery(hql)
					.addEntity(TagEvent.class)
					.setString("status", eventType.name())
					.setString("entity_uuid", tagUuid)
					.list();
			
			session.flush();
			
			return events;
		}
		catch (HibernateException ex)
		{
			throw new TagEventPersistenceException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}

	/**
	 * Saves a new tag permission. Upates existing ones.
	 * @param pem
	 * @throws TagEventPersistenceException
	 */
	public void persist(TagEvent event) throws TagEventPersistenceException
	{
		if (event == null)
			throw new TagEventPersistenceException("TagEvent cannot be null");

		try
		{
			Session session = getSession();
			session.saveOrUpdate(event);
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
			
			throw new TagEventPersistenceException("Failed to save tag event.", ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}

	/**
	 * Deletes the give tag permission.
	 * 
	 * @param event
	 * @throws TagEventPersistenceException
	 */
	public void delete(TagEvent event) throws TagEventPersistenceException
	{
		if (event == null)
			throw new TagEventPersistenceException("TagEvent cannot be null");

		try
		{
			Session session = getSession();
			session.delete(event);
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
			
			throw new TagEventPersistenceException("Failed to delete tag event.", ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}

	/**
	 * Deletes all tag events for the tag with given id
	 * 
	 * @param tagId
	 * @throws TagEventPersistenceException
	 */
	public void deleteByTagId(String tagUuid) throws TagEventPersistenceException
	{
		if (StringUtils.isEmpty(tagUuid)) {
			return;
		}

		try
		{
			Session session = getSession();

			String hql = "delete from tagevents where entity_uuid = :uuid";
			session.createQuery(hql)
					.setString("uuid", tagUuid)
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
			
			throw new TagEventPersistenceException("Failed to delete events for tag " + tagUuid, ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}

}
