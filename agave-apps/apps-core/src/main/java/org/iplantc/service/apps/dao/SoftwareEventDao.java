/**
 * 
 */
package org.iplantc.service.apps.dao;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.iplantc.service.apps.exceptions.SoftwareEventPersistenceException;
import org.iplantc.service.apps.model.SoftwareEvent;
import org.iplantc.service.apps.model.enumerations.SoftwareEventType;
import org.iplantc.service.apps.util.ServiceUtils;
import org.iplantc.service.common.Settings;
import org.iplantc.service.common.persistence.HibernateUtil;
import org.iplantc.service.common.persistence.TenancyHelper;

/**
 * Model class for interacting with software events. {@link SoftwareEvent}s are
 * not persisted as mapped entities in the Software class due to the
 * potentially large number.
 * 
 * @author dooley
 * 
 */
public class SoftwareEventDao {

	protected Session getSession() {
		Session session = HibernateUtil.getSession();
		HibernateUtil.beginTransaction();
        session.enableFilter("softwareEventTenantFilter").setParameter("tenantId", TenancyHelper.getCurrentTenantId());
		session.clear();
		return session;
	}
	
	/**
	 * Returns the software event with the given id.
	 * 
	 * @param eventId
	 * @return
	 * @throws SoftwareEventPersistenceException
	 */
	public SoftwareEvent getById(Long eventId)
	throws SoftwareEventPersistenceException
	{

		if (!ServiceUtils.isValid(eventId))
			throw new SoftwareEventPersistenceException("Event id cannot be null");

		try
		{
			Session session = getSession();
			
			SoftwareEvent event = (SoftwareEvent)session.get(SoftwareEvent.class, eventId);
			
			session.flush();
			
			return event;
		}
		catch (HibernateException ex)
		{
			throw new SoftwareEventPersistenceException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}
	
	/**
	 * Returns all software software events for the software with the given id.
	 * 
	 * @param softwareUuid
	 * @return
	 * @throws SoftwareEventPersistenceException
	 */
	public List<SoftwareEvent> getBySoftwareUuid(String softwareUuid)
	throws SoftwareEventPersistenceException
	{
		return getBySoftwareUuid(softwareUuid, Settings.DEFAULT_PAGE_SIZE, 0);
	}
	
	/**
	 * Returns all software software events for the software with the given id. Pagination
	 * is supported.
	 * 
	 * @param softwareUuid
	 * @param limit
	 * @param offset
	 * @return
	 * @throws SoftwareEventPersistenceException
	 */
	@SuppressWarnings("unchecked")
	public List<SoftwareEvent> getBySoftwareUuid(String softwareUuid, int limit, int offset)
	throws SoftwareEventPersistenceException
	{

		if (StringUtils.isEmpty(softwareUuid))
			throw new SoftwareEventPersistenceException("Software uuid cannot be null");

		try
		{
			Session session = getSession();
			
			String sql = "select * from softwareevents where software_uuid = :uuid order by id asc";
			List<SoftwareEvent> events = session.createSQLQuery(sql)
			        .addEntity(SoftwareEvent.class)
                    .setString("uuid", softwareUuid)
					.setFirstResult(offset)
					.setMaxResults(limit)
					.list();
			
			session.flush();
			
			return events;
		}
		catch (HibernateException ex)
		{
			throw new SoftwareEventPersistenceException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}
	
	/**
     * @param softwareUuid
     * @param limit
     * @param offset
     * @return
     * @throws SoftwareEventPersistenceException
     */
    public SoftwareEvent getBySoftwareUuidAndUuid(String softwareUuid, String uuid)
    throws SoftwareEventPersistenceException
    {

        if (softwareUuid == null)
            throw new SoftwareEventPersistenceException("Software id cannot be null");
        
        if (StringUtils.isEmpty(uuid)) 
            throw new SoftwareEventPersistenceException("Software event uuid cannot be null");

        try
        {
            Session session = getSession();
            
            String sql = "select * from softwareevents where software_uuid = :softwareUuid and uuid = :uuid";
            SoftwareEvent event = (SoftwareEvent)session.createSQLQuery(sql)
                    .addEntity(SoftwareEvent.class)
                    .setString("softwareUuid", softwareUuid)
                    .setString("uuid", uuid)
                    .uniqueResult();
            
            session.flush();
            
            return event;
        }
        catch (HibernateException ex)
        {
            throw new SoftwareEventPersistenceException(ex);
        }
        finally {
            try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
        }
    }

	/**
	 * Gets the software events for the specified software id and software status
	 * 
	 * @param softwareId
	 * @param status
	 * @return
	 * @throws SoftwareEventPersistenceException
	 */
	@SuppressWarnings("unchecked")
	public List<SoftwareEvent> getBySoftwareUuidAndStatus(String softwareUuid, SoftwareEventType eventType) 
	throws SoftwareEventPersistenceException
	{
		if (eventType == null)
			throw new SoftwareEventPersistenceException("eventType cannot be null");
		
		if (StringUtils.isEmpty(softwareUuid))
			throw new SoftwareEventPersistenceException("software uuid cannot be null");

		try
		{
			Session session = getSession();
			
			String hql = "select * from softwareevents where software_uuid = :softwareUuid and status = :status order by id asc";
			List<SoftwareEvent> events = session.createSQLQuery(hql)
					.addEntity(SoftwareEvent.class)
					.setString("status", eventType.name())
					.setString("software_uuid", softwareUuid)
					.list();
			
			session.flush();
			
			return events;
		}
		catch (HibernateException ex)
		{
			throw new SoftwareEventPersistenceException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}

	/**
	 * Saves a new software permission. Upates existing ones.
	 * @param pem
	 * @throws SoftwareEventPersistenceException
	 */
	public void persist(SoftwareEvent event) throws SoftwareEventPersistenceException
	{
		if (event == null)
			throw new SoftwareEventPersistenceException("SoftwareEvent cannot be null");

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
			
			throw new SoftwareEventPersistenceException("Failed to save software event.", ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}

	/**
	 * Deletes the give software permission.
	 * 
	 * @param event
	 * @throws SoftwareEventPersistenceException
	 */
	public void delete(SoftwareEvent event) throws SoftwareEventPersistenceException
	{
		if (event == null)
			throw new SoftwareEventPersistenceException("SoftwareEvent cannot be null");

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
			
			throw new SoftwareEventPersistenceException("Failed to delete software event.", ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}

	/**
	 * Deletes all software events for the software with given id
	 * 
	 * @param softwareId
	 * @throws SoftwareEventPersistenceException
	 */
	public void deleteBySoftwareId(String softwareUuid) throws SoftwareEventPersistenceException
	{
		if (StringUtils.isEmpty(softwareUuid)) {
			return;
		}

		try
		{
			Session session = getSession();

			String hql = "delete from softwareevents where software_uuid = :uuid";
			session.createQuery(hql)
					.setString("uuid", softwareUuid)
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
			
			throw new SoftwareEventPersistenceException("Failed to delete events for software " + softwareUuid, ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}

}
