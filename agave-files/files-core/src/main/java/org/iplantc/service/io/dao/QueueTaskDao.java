/**
 * 
 */
package org.iplantc.service.io.dao;

import java.math.BigInteger;
import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.hibernate.CacheMode;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Session;
import org.hibernate.StaleObjectStateException;
import org.iplantc.service.common.exceptions.PersistenceException;
import org.iplantc.service.common.persistence.HibernateUtil;
import org.iplantc.service.io.Settings;
import org.iplantc.service.io.exceptions.TaskException;
import org.iplantc.service.io.exceptions.TransformException;
import org.iplantc.service.io.model.EncodingTask;
import org.iplantc.service.io.model.LogicalFile;
import org.iplantc.service.io.model.QueueTask;
import org.iplantc.service.io.model.StagingTask;
import org.iplantc.service.io.model.enumerations.StagingTaskStatus;
import org.iplantc.service.io.model.enumerations.TransformTaskStatus;
import org.iplantc.service.io.util.ServiceUtils;
import org.iplantc.service.systems.model.RemoteSystem;
import org.iplantc.service.transfer.exceptions.TransferException;
import org.iplantc.service.transfer.model.TransferTask;
import org.quartz.SchedulerException;

/**
 * @author dooley
 *
 */
public class QueueTaskDao 
{	
	public static Long getNextStagingTask(String tenantId) 
	{
		boolean excludeTenant = false;
        if (StringUtils.isEmpty(tenantId)) {
            tenantId = "%";
        } else if (tenantId.contains("!")) {
            excludeTenant = true;
            tenantId = StringUtils.removeStart(tenantId, "!");
        }
        
		try 
		{
			Session session = HibernateUtil.getSession();
			session.clear();
			
			String hql = "select t.id "
			        + "from staging_tasks t left join logical_files f on t.logical_file_id = f.id "
					+ "where t.status = :status "
					+ "		and f.tenant_id " + (excludeTenant ? "not" : "") + " like :tenantid "
					+ "order by rand()";
			
			BigInteger taskId = (BigInteger)session.createSQLQuery(hql)
			        .setCacheable(false)
			        .setCacheMode(CacheMode.REFRESH)
    				.setString("status", StagingTaskStatus.STAGING_QUEUED.name())
    				.setString("tenantid", tenantId)
    				.setMaxResults(1)
    				.uniqueResult();
			
			session.flush();
			
			if (taskId == null) {
			    return null;
			} else {
			    return taskId.longValue();
			}
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
			throw new HibernateException("Failed to retrieve next staging task", ex);
		}
		finally
		{
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}
	
	public static StagingTask getStagingTaskById(Long id) 
	{	
		try 
		{
			HibernateUtil.beginTransaction();
			Session session = HibernateUtil.getSession();
			StagingTask task = (StagingTask)session.get(StagingTask.class, id);
			
			return task;
		} 
		catch (HibernateException e) 
		{
			try {
				if (HibernateUtil.getSession().isOpen()) {
					HibernateUtil.rollbackTransaction();
				}
			} catch (Exception e1) {}
			throw e;
		}
		finally
		{
//			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}
	
	public static EncodingTask getEncodingTaskById(Long id) 
    {   
        try 
        {
            HibernateUtil.beginTransaction();
            Session session = HibernateUtil.getSession();
            EncodingTask task = (EncodingTask)session.get(EncodingTask.class, id);
//            session.flush();
            return task;
        } 
        catch (HibernateException e) 
        {
            try {
                if (HibernateUtil.getSession().isOpen()) {
                    HibernateUtil.rollbackTransaction();
                }
            } catch (Exception e1) {}
            throw e;
        }
        finally
        {
//            try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
        }
    }
	
	public static Long getNextTransformTask(String tenantId) 
	{
		boolean excludeTenant = false;
        if (StringUtils.isEmpty(tenantId)) {
            tenantId = "%";
        } else if (tenantId.contains("!")) {
            excludeTenant = true;
            tenantId = StringUtils.removeStart(tenantId, "!");
        }
        
		try 
		{
			HibernateUtil.beginTransaction();
			Session session = HibernateUtil.getSession();
		
			String hql = "select t.id "
			        + "from encoding_tasks t left join logical_files l on l.id = t.logical_file_id "
					+ "where t.status = :status "
					+ "		and l.tenant_id " + (excludeTenant ? "not" : "") + " like :tenantid "
					+ "order by rand()";
			
			BigInteger taskId = (BigInteger)session.createSQLQuery(hql)
			        .setCacheable(false)
                    .setCacheMode(CacheMode.REFRESH)
                    .setString("status", TransformTaskStatus.TRANSFORMING_QUEUED.name())
	                .setString("tenantid", tenantId)
	                .setMaxResults(1)
	                .uniqueResult();
			
			session.flush();
			
			if (taskId == null) {
                return null;
            } else {
                return taskId.longValue();
            }
		} 
		catch (HibernateException e) 
		{
			try {
				if (HibernateUtil.getSession().isOpen()) {
					HibernateUtil.rollbackTransaction();
				}
			} catch (Exception e1) {}
			throw e;
		}
		finally
		{
//			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}
	
	public static void persist(QueueTask task) 
	throws TaskException, StaleObjectStateException 
	{
		try {
			HibernateUtil.beginTransaction();
			Session session = HibernateUtil.getSession();
			task.setLastUpdated(new Date());
			session.saveOrUpdate(task);
			session.flush();
		} 
		catch (StaleObjectStateException e) {
			throw e;
		}
		catch (HibernateException e) 
		{
			try {
				if (HibernateUtil.getSession().isOpen()) {
					HibernateUtil.rollbackTransaction();
				}
			} catch (Exception e1) {}
			throw new TaskException("Failed to save queue task", e);
		}
		finally
		{
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}
	
	public static void remove(QueueTask task) 
	throws TaskException, StaleObjectStateException 
	{	
		if (task == null) {
			throw new HibernateException("Null task cannot be deleted.");
		}
		
		try {
			HibernateUtil.beginTransaction();
			Session session = HibernateUtil.getSession();
			session.delete(task);
			session.flush();
		}
		catch (StaleObjectStateException e) {
			throw e;
		}
		catch (HibernateException e) 
		{
			try {
				if (HibernateUtil.getSession().isOpen()) {
					HibernateUtil.rollbackTransaction();
				}
			} catch (Exception e1) {}
			throw new TaskException("Failed to delete task", e);
		}
		finally
		{
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		} 
	}
	
	public static QueueTask merge(QueueTask task) throws TransferException
	{
		if (task == null)
			throw new TaskException("QueueTask cannot be null");

		try
		{
			HibernateUtil.beginTransaction();
			Session session = HibernateUtil.getSession();
			task.setLastUpdated(new Date());
			QueueTask mergedTask = (QueueTask)session.merge(task);
			session.flush();
			return mergedTask;
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
			
			throw new TransferException("Failed to save transfer task", ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}

	public static EncodingTask getTransformTaskByCallBackKey(String callbackKey) 
	throws TransformException 
	{
		if (!ServiceUtils.isValid(callbackKey)) {
			throw new TransformException("Callback key cannot be null");
		}
		
		try 
		{
			HibernateUtil.beginTransaction();
			Session session = HibernateUtil.getSession();
		
			String hql = "from EncodingTask where callbackKey = :key";
			EncodingTask task = (EncodingTask) session.createQuery(hql)
				.setString("key", callbackKey)
				.setMaxResults(1)
				.uniqueResult();
			
//			session.flush();
			
			return task;
		}
		catch (HibernateException e) 
		{
			try {
				if (HibernateUtil.getSession().isOpen()) {
					HibernateUtil.rollbackTransaction();
				}
			} catch (Exception e1) {}
			throw new TransformException("Failed to retrieve transform by callback key", e);
		}
		finally
		{
//			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}
	
	/**
	 * Pushes a job into the quartz queue by adding a StagingTask record to the db. 
	 * The task will be pulled by the existing triggers and run.
	 * 
	 * @param file
	 * @param system
	 * @param sourcePath
	 * @param destPath
	 * @param transformName
	 * @param handlerName
	 * @param createdBy
	 * @throws SchedulerException
	 */
	public static void enqueueEncodingTask(LogicalFile file, RemoteSystem system, String sourcePath, String destPath, String transformName, String handlerName, String createdBy) throws SchedulerException {
		
		if (file == null) {
			throw new SchedulerException("Logical file cannot be null");
		}
		
		if (!ServiceUtils.isValid(transformName)) {
			throw new SchedulerException("Invalid transform name");
		}
		
		if (!ServiceUtils.isValid(handlerName)) {
			throw new SchedulerException("Invalid transform handle");
		}
		
		try {
			EncodingTask task = new EncodingTask(file, system, sourcePath, destPath, transformName, handlerName, createdBy);
			
			QueueTaskDao.persist(task);
			
			//if (ServiceUtils.isValid(eventId)) EventClient.update(Settings.EVENT_API_KEY, eventId, EventClient.EVENT_STATUS_QUEUED);
			LogicalFileDao.updateTransferStatus(file, TransformTaskStatus.TRANSFORMING_QUEUED, createdBy);
		
		} catch (Exception e) {
			file.setStatus(StagingTaskStatus.STAGING_FAILED.name());
			try {
				LogicalFileDao.persist(file);
			} catch (Exception e1) {}
			
			throw new SchedulerException("Failed to add file to staging queue", e);
		}
	}
	
	/**
	 * Pushes a job into the quartz queue by adding a StagingTask record to the db. 
	 * The task will be pulled by the existing triggers and run.
	 * 
	 * @param file the file to transfer
	 * @param createdBy the user to whom the staging task belongs
	 * @throws SchedulerException 
	 */
	public static void enqueueStagingTask(LogicalFile file, String createdBy) 
	throws SchedulerException 
	{
		try 
		{
			StagingTask task = new StagingTask(file, createdBy);
			
			QueueTaskDao.persist(task);
			
			//if (ServiceUtils.isValid(eventId)) EventClient.update(Settings.EVENT_API_KEY, eventId, EventClient.EVENT_STATUS_QUEUED);
			LogicalFileDao.updateTransferStatus(file, StagingTaskStatus.STAGING_QUEUED, createdBy);
			
		} 
		catch (Exception e) 
		{
			file.setStatus(StagingTaskStatus.STAGING_FAILED.name());
			try {
				LogicalFileDao.persist(file);
			} catch (Exception e1) {}
			
			throw new SchedulerException("Failed to add file to staging queue", e);
		}
	}

}
