/**
 * 
 */
package org.iplantc.service.data.dao;

import java.math.BigInteger;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.hibernate.CacheMode;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.StaleObjectStateException;
import org.iplantc.service.common.exceptions.PersistenceException;
import org.iplantc.service.common.persistence.HibernateUtil;
import org.iplantc.service.data.exceptions.TransformPersistenceException;
import org.iplantc.service.data.model.DecodingTask;
import org.iplantc.service.data.util.ServiceUtils;
import org.iplantc.service.io.dao.LogicalFileDao;
import org.iplantc.service.io.exceptions.TransformException;
import org.iplantc.service.io.model.LogicalFile;
import org.iplantc.service.io.model.enumerations.StagingTaskStatus;
import org.iplantc.service.io.model.enumerations.TransformTaskStatus;
import org.iplantc.service.systems.model.RemoteSystem;
import org.quartz.SchedulerException;

/**
 * @author dooley
 *
 */
public class DecodingTaskDao {
	
	/**
	 * Fetches next available task to decode. no scheduling is done here
	 * 
	 * @param tenantId
	 * @return
	 */
	public static Long getNextDecodingTask(String tenantId) 
	throws TransformPersistenceException
	{	
		try 
		{
			boolean excludeTenant = false;
			if (StringUtils.isEmpty(tenantId)) {
	            tenantId = "%";
	        } else if (tenantId.contains("!")) {
	            excludeTenant = true;
	            tenantId = StringUtils.removeStart(tenantId, "!");
	        }
	        
	        Session session = HibernateUtil.getSession();
			session.clear();
			
			String hql = "select t.id "
			        + "from decoding_tasks t left join logical_files f on t.logical_file_id = f.id "
					+ "where t.status = :status "
					+ "		and f.tenant_id " + (excludeTenant ? "not" : "") + " like :tenantid "
					+ "order by rand()";
			
			BigInteger taskId = (BigInteger)session.createSQLQuery(hql)
					.addEntity(DecodingTask.class)
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
	        
//			HibernateUtil.beginTransaction();
//			Session session = HibernateUtil.getSession();
//		
//			String hql = "from DecodingTask t where t.status = :status order by t.created desc";
//			DecodingTask task = (DecodingTask) session.createQuery(hql)
//				.setString("status", TransformTaskStatus.TRANSFORMING_QUEUED.name())
//				.setMaxResults(1)
//				.uniqueResult();
//			
//			session.flush();
//			
//			return task;
		}  
		catch (HibernateException e) 
		{
			try {
				HibernateUtil.rollbackTransaction();
			} catch (Exception e1) {}
			throw e;
		}
		finally {  
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		} 
	}
	
	public static void persist(DecodingTask task) throws TransformPersistenceException 
	{	
		if (task == null) {
			throw new HibernateException("Null task cannot be committed.");
		}
		
		try 
		{
			HibernateUtil.beginTransaction();
			Session session = HibernateUtil.getSession();		
			session.saveOrUpdate(task);
			session.flush();
		} 
		catch (StaleObjectStateException e) {
			throw e;
		}
		catch (HibernateException e) 
		{
			try {
				HibernateUtil.rollbackTransaction();
			} catch (Exception e1) {}
			throw new TransformPersistenceException("Failed to save decoding task",e);
		}
		finally {  
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		} 
	}
	
	public static void remove(DecodingTask task) 
	throws TransformPersistenceException 
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
		catch (HibernateException e) 
		{
			try {
				HibernateUtil.rollbackTransaction();
			} catch (Exception e1) {}
			throw new TransformPersistenceException("Failed to delete decoding task",e);
		}
		finally {  
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}   
	}

	public static DecodingTask getTransformTaskByCallBackKey(String callbackKey) 
	throws TransformPersistenceException 
	{	
		if (!ServiceUtils.isValid(callbackKey)) {
			throw new TransformPersistenceException("Callback key cannot be null");
		}
		
		try 
		{
			HibernateUtil.beginTransaction();
			Session session = HibernateUtil.getSession();
				
			String hql = "from DecodingTask t where t.callbackKey = :key";
			DecodingTask task = (DecodingTask) session.createQuery(hql)
				.setString("key", callbackKey)
				.setMaxResults(1)
				.uniqueResult();
			
//			session.flush();
			
			return task;
		} 
		catch (HibernateException e) 
		{
			try {
				HibernateUtil.rollbackTransaction();
			} catch (Exception e1) {}
			throw new TransformPersistenceException("Failed to retrieve transform by callback key", e);
		}
		finally {  
//			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		} 
	}
	
	/**
	 * Pushes a job into the quartz queue by adding a StagingTask record to the db. 
	 * The task will be pulled by the existing triggers and run.
	 * 
	 * @param file
	 * @param eventId
	 * @throws SchedulerException 
	 */
	public static void enqueueDecodingTask(LogicalFile logicalFile, 
			RemoteSystem system, 
			String sourcePath, 
			String destPath, 
			String srcTransformName, 
			String destTransformName, 
			String currentFilter, 
			String destinationUri,
			String owner) throws SchedulerException {
		
		if (StringUtils.isEmpty(sourcePath)) {
			throw new SchedulerException("Invalid source path");
		}
		
		if (StringUtils.isEmpty(destPath)) {
			throw new SchedulerException("Invalid destination path");
		}
		
		if (StringUtils.isEmpty(srcTransformName)) {
			throw new SchedulerException("Invalid source transform name");
		}
		
		if (StringUtils.isEmpty(destTransformName)) {
			throw new SchedulerException("Invalid destination transform name");
		}
		
		if (StringUtils.isEmpty(currentFilter)) {
			throw new SchedulerException("Invalid transform filter name");
		}
		
		if (StringUtils.isEmpty(destinationUri)) {
			throw new SchedulerException("Invalid destination URI");
		}
		
		if (StringUtils.isEmpty(owner)) {
			throw new SchedulerException("Invalid transform filter name");
		}
		
		try 
		{	
			DecodingTask task = new DecodingTask(logicalFile, system, sourcePath, destPath, srcTransformName, destTransformName, currentFilter, destinationUri, owner);
			DecodingTaskDao.persist(task);
			
			LogicalFileDao.updateTransferStatus(task.getLogicalFile(), TransformTaskStatus.TRANSFORMING_QUEUED, owner);
		} 
		catch (Exception e) {
			throw new SchedulerException("Failed to add file to staging queue", e);
		}
	}

	/**
	 * Returns all zombie decoding tasks  that have not been 
	 * updated in the last hour.
	 * 
	 * @return {@link List<Long>} of zombie {@link DecodingTask} ids
	 * @throws TransformPersistenceException
	 */
	@SuppressWarnings("unchecked")
	public static List<Long> findZombieTransforms(String tenantId) 
	throws TransformPersistenceException
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
				
			String sql =  "select t.id"
						+ "from decoding_tasks t left join logical_files f on t.logical_file_id = f.id "
						+ "where t.status in ('TRANSFORMING', 'PREPROCESSING') "
						+ "		and NOW() > DATE_ADD(t.last_updated, INTERVAL 1 hour) "
						+ "		and f.tenant_id like :tenantId";
			
			return session.createSQLQuery(sql)
				.setString("tenantId", tenantId)
				.list();
		} 
		catch (HibernateException e) 
		{
			try {
				HibernateUtil.rollbackTransaction();
			} catch (Exception e1) {}
			throw new TransformPersistenceException("Failed to retrieve transform by callback key", e);
		}
	}

	/**
	 * Returns the {@link DecodingTask} with the given id
	 * @param id
	 * @return decoding task with the given id
	 */
	public static DecodingTask getById(Long id) 
	throws TransformPersistenceException
	{
		if (id == null) {
			throw new TransformPersistenceException("DecodingTask id cannot be null");
		}
		
		try 
		{
			HibernateUtil.beginTransaction();
			Session session = HibernateUtil.getSession();
				
			return (DecodingTask) session.get(DecodingTask.class, id);
		} 
		catch (HibernateException e) 
		{
			try {
				HibernateUtil.rollbackTransaction();
			} catch (Exception e1) {}
			
			throw new TransformPersistenceException("Failed to retrieve transform by callback key", e);
		}
	}
}
