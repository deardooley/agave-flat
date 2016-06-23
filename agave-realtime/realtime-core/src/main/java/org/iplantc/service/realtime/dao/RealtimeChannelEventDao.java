/**
 * 
 */
package org.iplantc.service.realtime.dao;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.iplantc.service.apps.util.ServiceUtils;
import org.iplantc.service.common.Settings;
import org.iplantc.service.common.persistence.HibernateUtil;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.realtime.exceptions.RealtimeChannelEventException;
import org.iplantc.service.realtime.model.RealtimeChannel;
import org.iplantc.service.realtime.model.RealtimeChannelEvent;
import org.iplantc.service.realtime.model.enumerations.RealtimeChannelEventType;

/**
 * Model class for interacting with {@link RealtimeChannelEvent}s. These represent
 * the actions taken on the channel, not the external events written to it.
 * 
 * @author dooley
 * 
 */
public class RealtimeChannelEventDao {

	protected static Session getSession() {
		Session session = HibernateUtil.getSession();
		HibernateUtil.beginTransaction();
        session.enableFilter("realtimeChannelEventTenantFilter").setParameter("tenantId", TenancyHelper.getCurrentTenantId());
		session.clear();
		return session;
	}
	
	/**
	 * Returns the software event with the given id.
	 * 
	 * @param eventId
	 * @return
	 * @throws RealtimeChannelEventException
	 */
	public static RealtimeChannelEvent getById(Long eventId)
	throws RealtimeChannelEventException
	{

		if (!ServiceUtils.isValid(eventId))
			throw new RealtimeChannelEventException("Event id cannot be null");

		try
		{
			Session session = getSession();
			
			RealtimeChannelEvent event = (RealtimeChannelEvent)session.get(RealtimeChannelEvent.class, eventId);
			
			session.flush();
			
			return event;
		}
		catch (HibernateException ex)
		{
			throw new RealtimeChannelEventException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}
	
	/**
	 * Returns all {@link RealtimeChannelEvent} for the {@link RealtimeChannel}
	 * with the provided uuid.
	 * 
	 * @param uuid the {@link RealtimeChannel#getUuid()}
	 * @return list of {@link RealtimeChannelEvent} for the uuid. At most, {@link Settings#DEFAULT_PAGE_SIZE} will be returned.
	 * @throws RealtimeChannelEventException
	 */
	public static List<RealtimeChannelEvent> getBySoftwareUuid(String uuid)
	throws RealtimeChannelEventException
	{
		return getBySoftwareUuid(uuid, Settings.DEFAULT_PAGE_SIZE, 0);
	}
	
	/**
	 * Gets the {@link RealtimeChannelEvent} for the specified {@link RealtimeChannel} uuid.
	 * Pagination is supported. Defaults to the value of {@link Settings.DEFAULT_PAGE_SIZE}.
	 * 
	 * @param uuid the {@link RealtimeChannel#getUuid()}
	 * @param limit maximum number of results to return.
	 * @param offset number of results to skip in the raw response.
	 * @return list of {@link RealtimeChannelEvent} matching the search constraints.
	 * @throws RealtimeChannelEventException
	 */
	@SuppressWarnings("unchecked")
	public static List<RealtimeChannelEvent> getBySoftwareUuid(String uuid, int limit, int offset)
	throws RealtimeChannelEventException
	{

		if (StringUtils.isEmpty(uuid))
			throw new RealtimeChannelEventException("Realtime channel uuid cannot be null");
		
		try
		{
			Session session = getSession();
			
			String sql = "from RealtimeChannelEvent where entity = :uuid order by id asc";
			List<RealtimeChannelEvent> events = session.createSQLQuery(sql)
			        .addEntity(RealtimeChannelEvent.class)
                    .setString("uuid", uuid)
					.setFirstResult(offset)
					.setMaxResults(limit)
					.list();
			
			session.flush();
			
			return events;
		}
		catch (HibernateException ex)
		{
			throw new RealtimeChannelEventException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}
	
	/**
	 * Gets the {@link RealtimeChannelEvent} for the specified {@link RealtimeChannel} uuid and 
	 * event type.
	 * 
	 * @param uuid the {@link RealtimeChannel#getUuid()}
	 * @param eventType string representation of a {@link RealtimeChannelEventType}
	 * @return list of {@link RealtimeChannelEvent} matching the search constraints.
	 * @throws RealtimeChannelEventException
	 */
	@SuppressWarnings("unchecked")
	public static List<RealtimeChannelEvent> getByRealtimeChannleUuidAndStatus(String uuid, String eventType)
    throws RealtimeChannelEventException
    {

    	if (StringUtils.isEmpty(eventType)) {
			throw new RealtimeChannelEventException("Event type cannot be null");
    	}
    	
		if (StringUtils.isEmpty(uuid)) {
			throw new RealtimeChannelEventException("Realtime channel uuid cannot be null");
		}
		
		try
		{
			Session session = getSession();
			
			String hql = "from RealtimeChannelEvent where entity = :uuid and status = :status order by id asc";
			List<RealtimeChannelEvent> events = session.createSQLQuery(hql)
					.setString("status", eventType)
					.setString("uuid", uuid)
					.list();
			
			session.flush();
			
			return events;
		}
		catch (HibernateException ex)
		{
			throw new RealtimeChannelEventException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
    }

	/**
	 * Gets the {@link RealtimeChannelEvent} for the specified {@link RealtimeChannel} uuid and 
	 * event type.
	 * 
	 * @param uuid the {@link RealtimeChannel#getUuid()}
	 * @param eventType the {@link RealtimeChannel#getSourceEvent()}
	 * @return list of {@link RealtimeChannelEvent} matching the search constraints.
	 * @throws RealtimeChannelEventException
	 */
	public static List<RealtimeChannelEvent> getByRealtimeChannleUuidAndStatus(String uuid, RealtimeChannelEventType eventType) 
	throws RealtimeChannelEventException
	{
		if (eventType == null) {
			throw new RealtimeChannelEventException("Event type cannot be null");
    	}
		
		return getByRealtimeChannleUuidAndStatus(uuid, eventType.name());
	}

	/**
	 * Saves or updates the {@link RealtimeChannelEvent}.
	 * @param event the event to persist.
	 * @throws RealtimeChannelEventException
	 */
	public static void persist(RealtimeChannelEvent event) throws RealtimeChannelEventException
	{
		if (event == null)
			throw new RealtimeChannelEventException("Event cannot be null");

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
			
			throw new RealtimeChannelEventException("Failed to save event.", ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}

	/**
	 * Deletes the given {@link RealtimeChannelEvent}.
	 * 
	 * @param event
	 * @throws RealtimeChannelEventException
	 */
	public static void delete(RealtimeChannelEvent event) throws RealtimeChannelEventException
	{
		if (event == null)
			throw new RealtimeChannelEventException("RealtimeChannelEvent cannot be null");

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
			
			throw new RealtimeChannelEventException("Failed to delete event.", ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}

	/**
	 * Deletes all {@link RealtimeChannelEvent} for the given {@link realtimeChannelUuid}.
	 * 
	 * @param realtimeChannelUuid {@link RealtimeChannel#getUuid()}
	 * @throws RealtimeChannelEventException
	 */
	public static void deleteByRealtimeChannelId(String realtimeChannelUuid) throws RealtimeChannelEventException
	{
		if (StringUtils.isEmpty(realtimeChannelUuid)) {
			return;
		}

		try
		{
			Session session = getSession();

			String hql = "delete from realtimechannelevents where entity_uuid = :uuid";
			session.createQuery(hql)
					.setString("uuid", realtimeChannelUuid)
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
			
			throw new RealtimeChannelEventException("Failed to delete events for realtimeChannel " + realtimeChannelUuid, ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}

}
