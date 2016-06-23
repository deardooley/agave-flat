/**
 * 
 */
package org.iplantc.service.io.dao;

import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.iplantc.service.common.persistence.HibernateUtil;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.common.uuid.AgaveUUID;
import org.iplantc.service.common.uuid.UUIDType;
import org.iplantc.service.io.Settings;
import org.iplantc.service.io.exceptions.LogicalFileException;
import org.iplantc.service.io.model.FileEvent;
import org.iplantc.service.io.util.ServiceUtils;

/**
 * Model class for interacting with job events. FileEvents are
 * not persisted as mapped entities in the LogicalFile class due to the
 * potentially large number.
 * 
 * @author dooley
 * 
 */
public class FileEventDao {

	protected static Session getSession() {
		Session session = HibernateUtil.getSession();
		HibernateUtil.beginTransaction();
//        session.enableFilter("fileEventTenantFilter").setParameter("tenantId", TenancyHelper.getCurrentTenantId());
		session.clear();
		return session;
	}
	
	/**
	 * Returns the job event with the given id.
	 * 
	 * @param eventId
	 * @return
	 * @throws LogicalFileException
	 */
	public static FileEvent getById(Long eventId)
	throws LogicalFileException
	{

		if (!ServiceUtils.isValid(eventId))
			throw new LogicalFileException("Event id cannot be null");

		try
		{
			Session session = getSession();
			
			FileEvent event = (FileEvent)session.get(FileEvent.class, eventId);
			
			session.flush();
			
			return event;
		}
		catch (HibernateException ex)
		{
			throw new LogicalFileException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}
	
	/**
	 * Returns all job job events for the job with the given id.
	 * 
	 * @param logicalFileId
	 * @return
	 * @throws LogicalFileException
	 */
	public static List<FileEvent> getByLogicalFileId(Long logicalFileId)
	throws LogicalFileException
	{
		return FileEventDao.getByLogicalFileId(logicalFileId, Settings.DEFAULT_PAGE_SIZE, 0);
	}
	
	/**
	 * @param logicalFileId
	 * @param limit
	 * @param offset
	 * @return
	 * @throws LogicalFileException
	 */
	@SuppressWarnings("unchecked")
	public static List<FileEvent> getByLogicalFileId(Long logicalFileId, int limit, int offset)
	throws LogicalFileException
	{

		if (!ServiceUtils.isValid(logicalFileId))
			throw new LogicalFileException("LogicalFile id cannot be null");

		try
		{
			Session session = getSession();
			
			String hql = "from FileEvent where logicalFile.id = :fileid order by id asc";
			List<FileEvent> events = session.createQuery(hql)
					.setLong("fileid", logicalFileId)
					.setFirstResult(offset)
					.setMaxResults(limit)
					.list();
			
			session.flush();
			
			return events;
		}
		catch (HibernateException ex)
		{
			throw new LogicalFileException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}

	/**
	 * Gets the job events for the specified job id and job status
	 * 
	 * @param logicalFileId
	 * @param status
	 * @return
	 * @throws LogicalFileException
	 */
	@SuppressWarnings("unchecked")
	public static List<FileEvent> getByLogicalFileIdAndStatus(Long logicalFileId, String status) 
	throws LogicalFileException
	{
		if (StringUtils.isEmpty(status))
			throw new LogicalFileException("status cannot be empty");
		
		if (!ServiceUtils.isValid(logicalFileId))
			throw new LogicalFileException("job id cannot be null");

		try
		{
			Session session = getSession();
			
			String hql = "select * from jobevents where job_id = :fileid and status = :status order by created asc";
			List<FileEvent> events = session.createSQLQuery(hql)
					.addEntity(FileEvent.class)
					.setString("status", status)
					.setLong("fileid", logicalFileId)
					.list();
			
			session.flush();
			
			return events;
		}
		catch (HibernateException ex)
		{
			throw new LogicalFileException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}

	/**
	 * Saves a new job permission. Upates existing ones.
	 * @param pem
	 * @throws LogicalFileException
	 */
	public static void persist(FileEvent event) throws LogicalFileException
	{
		if (event == null)
			throw new LogicalFileException("FileEvent cannot be null");

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
			
			throw new LogicalFileException("Failed to save job event.", ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}
	
	/**
	 * Saves multiple job permissions. Updates existing ones.
	 * @param pem
	 * @throws LogicalFileException
	 */
	public static void persistAllRaw(List<Object[]> events) throws LogicalFileException
	{
		if (events == null) {
			throw new LogicalFileException("FileEvents cannot be null");
		} else if (events.isEmpty()) {
			return;
		}
		
		try
		{
			String ipAddress = org.iplantc.service.common.Settings.getIpLocalAddress();
			String tenantId = TenancyHelper.getCurrentTenantId();	
			Session session = getSession();
			int i=1;
			
			for (Object[] event: events) {
				
				session.createSQLQuery("INSERT INTO `fileevents` "
						+ "(`id`, `created`, `description`, `ip_address`, `status`, `logicalfile_id`, `transfertask`, `uuid`, `tenant_id`, `created_by`) "
						+ "VALUES "
						+ "(NULL, :created, :description, :ipaddress, :status, :logicalfileid, NULL, :eventuuid, :tenantid, :createdby)")
						.setDate("created", new Date())
						.setString("description", (String)event[2])
						.setString("ipaddress", ipAddress)
						.setString("status", (String)event[1])
						.setLong("logicalfileid", (Long)event[0])
						.setString("eventuuid", new AgaveUUID(UUIDType.FILE_EVENT).toString())
						.setString("tenantid", tenantId)
						.setString("createdby", (String)event[3])
						.executeUpdate();
						
//				session.saveOrUpdate(event);
				
				if (++i%50 == 0) {
					session.flush();
					session.clear();
				}
			}
			
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
			
			throw new LogicalFileException("Failed to save job event.", ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}
	
	/**
	 * Saves multiple job permissions. Updates existing ones.
	 * @param pem
	 * @throws LogicalFileException
	 */
	public static void persistAll(List<FileEvent> events) throws LogicalFileException
	{
		if (events == null) {
			throw new LogicalFileException("FileEvents cannot be null");
		} else if (events.isEmpty()) {
			return;
		}
		
		try
		{
			String ipAddress = org.iplantc.service.common.Settings.getIpLocalAddress();
			String tenantId = TenancyHelper.getCurrentTenantId();	
			Session session = getSession();
			int i=1;
			
			for (FileEvent event: events) {
				
				session.saveOrUpdate(event);
				
				if (++i%50 == 0) {
					session.flush();
					session.clear();
				}
			}
			
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
			
			throw new LogicalFileException("Failed to save job event.", ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}

	/**
	 * Deletes the give job permission.
	 * 
	 * @param event
	 * @throws LogicalFileException
	 */
	public static void delete(FileEvent event) throws LogicalFileException
	{
		if (event == null)
			throw new LogicalFileException("FileEvent cannot be null");

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
			
			throw new LogicalFileException("Failed to delete job event.", ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}

	/**
	 * Deletes all job events for the job with given id
	 * 
	 * @param logicalFileId
	 * @throws LogicalFileException
	 */
	public static void deleteByLogicalFileId(Long logicalFileId) throws LogicalFileException
	{
		if (logicalFileId == null) {
			return;
		}

		try
		{
			Session session = getSession();

			String hql = "delete from FileEvent where logicalFile.id = :fileid";
			session.createQuery(hql)
					.setLong("fileid", logicalFileId)
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
			
			throw new LogicalFileException("Failed to delete events for job " + logicalFileId, ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}

}
