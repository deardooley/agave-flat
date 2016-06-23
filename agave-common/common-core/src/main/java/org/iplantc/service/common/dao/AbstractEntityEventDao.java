package org.iplantc.service.common.dao;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.iplantc.service.common.Settings;
import org.iplantc.service.common.exceptions.EntityEventPersistenceException;
import org.iplantc.service.common.model.AgaveEntityEvent;
import org.iplantc.service.common.persistence.HibernateUtil;
import org.iplantc.service.common.persistence.TenancyHelper;

/**
 * Model class for interacting with {@link AgaveEntityEvent}. {@link AgaveEntityEvent}s are
 * persisted independently of the parent object for async management.
 * 
 * @author dooley
 * 
 */
public abstract class AbstractEntityEventDao<T extends AgaveEntityEvent, V extends Enum> implements EntityEventDao<T, V> {
	
	/**
	 * Default no-args constructor
	 */
	public AbstractEntityEventDao() {}
	
	protected abstract Class<?> getEntityEventClass();
	
	/**
	 * Returns name of the tenant filter to apply when looking things up.
	 * Tenant filters should be the name of the {@link AgaveEntityEvent} 
	 * concrete class and "TenantFilter". First letter lowercase.
	 * 
	 * @return
	 */
	protected String getTenantFilterName() {
		return Character.toLowerCase(getEntityEventClass().getSimpleName().charAt(0)) + 
				getEntityEventClass().getSimpleName().substring(1) + "TenantFilter";
	}
	
	/**
	 * Creates a hibernate session and resolves the tenancy filter for the domain
	 * object by default.
	 * 
	 * @return
	 */
	protected Session getSession() {
		Session session = HibernateUtil.getSession();
		HibernateUtil.beginTransaction();
        session.enableFilter(getTenantFilterName())
        	.setParameter("tenantId", TenancyHelper.getCurrentTenantId());
		session.clear();
		return session;
	}
	
	/* (non-Javadoc)
	 * @see org.iplantc.service.common.dao.EntityEventDao#getEntityEventByUuid(java.lang.String)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public T getEntityEventByUuid(String uuid)
	throws EntityEventPersistenceException
	{

		if (StringUtils.isEmpty(uuid))
			throw new EntityEventPersistenceException("Event id cannot be null");

		try
		{
			Session session = getSession();
			
			T event = (T)session.createQuery("FROM " + getEntityEventClass().getSimpleName() + " WHERE uuid = :uuid")
							.setString("uuid", uuid)
							.setMaxResults(1)
							.uniqueResult();
			
			session.flush();
			
			return event;
		}
		catch (HibernateException ex)
		{
			throw new EntityEventPersistenceException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}
	
	/* (non-Javadoc)
	 * @see org.iplantc.service.common.dao.EntityEventDao#getEntityEventByEntityUuid(java.lang.String)
	 */
	@Override
	public List<T> getEntityEventByEntityUuid(String uuid)
	throws EntityEventPersistenceException
	{
		return getEntityEventByEntityUuid(uuid, Settings.DEFAULT_PAGE_SIZE, 0);
	}
	
	/* (non-Javadoc)
	 * @see org.iplantc.service.common.dao.EntityEventDao#getEntityEventByEntityUuid(java.lang.String, int, int)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public List<T> getEntityEventByEntityUuid(String uuid, int limit, int offset)
	throws EntityEventPersistenceException
	{

		if (StringUtils.isEmpty(uuid))
			throw new EntityEventPersistenceException("Entity id cannot be null");

		try
		{
			Session session = getSession();
			
			String sql = "FROM " + getEntityEventClass().getSimpleName() + 
					"  where entity = :uuid order by id asc";
			
			List<T> events = session.createQuery(sql)
			        .setString("uuid", uuid)
					.setFirstResult(offset)
					.setMaxResults(limit)
					.list();
			
			session.flush();
			
			return events;
		}
		catch (HibernateException ex)
		{
			throw new EntityEventPersistenceException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}
	
	/* (non-Javadoc)
	 * @see org.iplantc.service.common.dao.EntityEventDao#getByEntityUuidAndEntityEventUuid(java.lang.String, java.lang.String)
	 */
    @Override
	@SuppressWarnings("unchecked")
	public T getByEntityUuidAndEntityEventUuid(String entityUuid, String entityEventUuid)
    throws EntityEventPersistenceException
    {

    	 if (StringUtils.isEmpty(entityUuid)) 
            throw new EntityEventPersistenceException("Entity uuid cannot be null");
        
        if (StringUtils.isEmpty(entityEventUuid)) 
            throw new EntityEventPersistenceException("Event uuid cannot be null");

        try
        {
            Session session = getSession();
            
            String sql = "FROM " + getEntityEventClass().getSimpleName() 
            		+ " WHERE entity = :entityUuid and uuid = :entityEventUuid";
            
            T event = (T)session.createQuery(sql)
                    .setString("entityUuid", entityUuid)
                    .setString("entityEventUuid", entityEventUuid)
                    .setMaxResults(1)
                    .uniqueResult();
            
            session.flush();
            
            return event;
        }
        catch (HibernateException ex)
        {
            throw new EntityEventPersistenceException(ex);
        }
        finally {
            try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
        }
    }

	/* (non-Javadoc)
	 * @see org.iplantc.service.common.dao.EntityEventDao#getAllEntityEventWithStatusForEntityUuid(java.lang.String, V)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public List<T> getAllEntityEventWithStatusForEntityUuid(String eventUuid, V eventType) 
	throws EntityEventPersistenceException
	{
		if (eventType == null)
			throw new EntityEventPersistenceException("Entity event status cannot be null");
		
		if (StringUtils.isEmpty(eventUuid))
			throw new EntityEventPersistenceException("Event uuid cannot be null");

		try
		{
			Session session = getSession();
			
			String hql = "FROM " + getEntityEventClass().getSimpleName() 
            		+ " WHERE entity = :uuid "
            		+ "		  AND status = :status "
            		+ " ORDER BY id ASC";
			List<T> events = session.createQuery(hql)
					.setString("status", eventType.name())
					.setString("uuid", eventUuid)
					.list();
			
			session.flush();
			
			return events;
		}
		catch (HibernateException ex)
		{
			throw new EntityEventPersistenceException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.common.dao.EntityEventDao#persist(T)
	 */
	@Override
	public void persist(T event) throws EntityEventPersistenceException
	{
		if (event == null)
			throw new EntityEventPersistenceException("Event cannot be null");

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
			
			throw new EntityEventPersistenceException("Failed to save entity event.", ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.common.dao.EntityEventDao#delete(T)
	 */
	@Override
	public void delete(T event) throws EntityEventPersistenceException
	{
		if (event == null)
			throw new EntityEventPersistenceException("Event cannot be null");

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
			
			throw new EntityEventPersistenceException("Failed to delete entity event.", ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.common.dao.EntityEventDao#deleteBySoftwareId(java.lang.String)
	 */
	@Override
	public void deleteByEntityId(String entityUuid) throws EntityEventPersistenceException
	{
		if (StringUtils.isEmpty(entityUuid)) {
			throw new EntityEventPersistenceException("Event uuid cannot be null");
		}

		try
		{
			Session session = getSession();

			String hql = "DELETE FROM " + getEntityEventClass().getSimpleName() + 
					" WHERE entity = :uuid";
			session.createQuery(hql)
					.setString("uuid", entityUuid)
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
			
			throw new EntityEventPersistenceException("Failed to delete events for uuid " + entityUuid, ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}

}
