/**
 * 
 */
package org.iplantc.service.transfer.dao;

import java.math.BigInteger;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.CacheMode;
import org.hibernate.HibernateException;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.StaleObjectStateException;
import org.hibernate.criterion.Restrictions;
import org.hibernate.transform.AliasToEntityMapResultTransformer;
import org.hibernate.type.StandardBasicTypes;
import org.iplantc.service.common.persistence.HibernateUtil;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.common.search.SearchTerm;
import org.iplantc.service.transfer.Settings;
import org.iplantc.service.transfer.exceptions.TransferException;
import org.iplantc.service.transfer.model.TransferSummary;
import org.iplantc.service.transfer.model.TransferTask;
import org.iplantc.service.transfer.model.enumerations.PermissionType;
import org.iplantc.service.transfer.model.enumerations.TransferStatusType;
import org.iplantc.service.transfer.util.ServiceUtils;

/**
 * @author dooley
 * 
 */
public class TransferTaskDao
{	
    private static final Logger log = Logger.getLogger(TransferTaskDao.class);
    
	protected static Session getSession() {
		HibernateUtil.beginTransaction();
		Session session = HibernateUtil.getSession();
		session.clear();
		session.enableFilter("transferTaskTenantFilter").setParameter("tenantId", TenancyHelper.getCurrentTenantId());
		return session;
	}
	
	/**
     * Gets the transfer task by id regardless of tenant.
     * 
     * @param transferTaskId
     * @return
     * @throws TransferException
     */
    public static TransferTask getById(long transferTaskId) throws TransferException
    {
        try
        {
            Session session = getSession();
            session.disableFilter("transferTaskTenantFilter");
            
            TransferTask task = (TransferTask) session.get(TransferTask.class, transferTaskId);
            session.flush();
            return task;
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
            
            throw new TransferException(ex);
        }
        finally {
            try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
        }
    }
    
    
    /**
     * Gets the transfer task by uuid regardless of tenant.
     * 
     * @param uuid
     * @return
     */
    public static TransferTask findByUuid(String uuid)
    {
        try
        {
            Session session = getSession();
            session.disableFilter("transferTaskTenantFilter");
            
            TransferTask system = (TransferTask)session
                    .createCriteria(TransferTask.class)
                    .add(Restrictions.eq("uuid", uuid))
                    .uniqueResult();
            
            session.flush();
            
            return system;
        }
        catch (HibernateException ex)
        {
            throw ex;
        }
        finally
        {
            try { HibernateUtil.commitTransaction(); } catch (Throwable e) {}
        }
    }
    
    /**
     * Gets the transfer task by uuid regardless of tenant.
     * 
     * @param uuid
     * @return
     */
    public static TransferTask findChildTasksByUuid(String uuid)
    {
        try
        {
            Session session = getSession();
            session.disableFilter("transferTaskTenantFilter");
            
            TransferTask system = (TransferTask)session
                    .createCriteria(TransferTask.class)
                    .add(Restrictions.eq("uuid", uuid))
                    .uniqueResult();
            
            session.flush();
            
            return system;
        }
        catch (HibernateException ex)
        {
            throw ex;
        }
        finally
        {
            try { HibernateUtil.commitTransaction(); } catch (Throwable e) {}
        }
    }

    
	/**
	 * Gets all transfers for the given user in the current session tenant context.
	 * 
	 * @param username
	 * @param limit max number of results
	 * @param offset number of results to skip
	 * @return list of transfers for the given {@code username}
	 * @throws TransferException
	 */
	@SuppressWarnings("unchecked")
	public static List<TransferTask> getUserTransfers(String username, int limit, int offset) throws TransferException
	{
		try
		{
			Session session = getSession();
			
			String hql =  "FROM TransferTask t left join TransferTaskPermission p on t.id = p.transferTaskId "
    			        + "WHERE t.owner = :owner "
    			        + "          or (p.username = :owner and p.permission like 'READ%') "
    			        + "      and status in ('" + StringUtils.join(TransferStatusType.getActiveStatusValues(), "','") + "')";
			
			List<TransferTask> tasks = (List<TransferTask>)session.createQuery(hql)
					.setString("owner",username)
					.setMaxResults(limit)
					.setFirstResult(offset)
                    .list();
			
			session.flush();
			
			return tasks;
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
			
			throw new TransferException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}

	/**
     * Returns number of active transfers by the given user in the session tenant context.
     * 
     * @param username
     * @return 
     * @throws TransferException
     */
    public static long getUserActiveTransferCount(String username) throws TransferException
	{
		try
		{
			Session session = getSession();
			
			String hql = "SELECT count(t.id) "
			        + "FROM TransferTask t left join TransferTaskPermission p on t.id = p.transferTaskId "
                    + "WHERE t.owner = :owner "
                    + "          or (p.username = :owner and p.permission like 'READ%') "
                    + "      and status in ('" + StringUtils.join(TransferStatusType.getActiveStatusValues(), "','") + "')";
			
			long currentUserJobCount = ((Long)session.createQuery(hql)
					.setString("owner",username)
					.uniqueResult()).longValue();
			
			session.flush();
			
			return currentUserJobCount;

		}
		catch (ObjectNotFoundException ex)
		{
			throw new TransferException(ex);
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
			
			throw new TransferException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}
	
	

	/**
	 * Saves a {@link TransferTask} or updates if already saved.
	 * @param task
	 * @throws TransferException
	 */
	public static void persist(TransferTask task) throws TransferException
	{
		if (task == null)
			throw new TransferException("TransferTask cannot be null");

		try
		{
			Session session = getSession();
			task.setLastUpdated(new Date());
			session.saveOrUpdate(task);
			session.flush();
		}
		catch (ObjectNotFoundException ex)
		{
			throw ex;
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
	
	/**
	 * Copy the state of the given object onto the persistent object with the same identifier. 
	 * If there is no persistent instance currently associated with the session, it will be loaded. 
	 * Return the persistent instance. If the given instance is unsaved, save a copy of and return 
	 * it as a newly persistent instance. The given instance does not become associated with the 
	 * session. This operation cascades to associated instances if the association is mapped with cascade="merge".
     *
	 * @param task
	 * @return
	 * @throws TransferException
	 */
	public static TransferTask merge(TransferTask task) throws TransferException
	{
		if (task == null)
			throw new TransferException("TransferTask cannot be null");

		try
		{
			Session session = getSession();
			task.setLastUpdated(new Date());
			TransferTask mergedTask = (TransferTask)session.merge(task);
			session.flush();
			return mergedTask;
		}
		catch (StaleObjectStateException ex)
		{
			throw ex;
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


	/**
	 * Deletes teh object from the db.
	 * @param task
	 * @throws TransferException
	 */
	public static void delete(TransferTask task) throws TransferException
	{
		if (task == null)
			throw new TransferException("TransferTask cannot be null");

		try
		{
			Session session = getSession();
			session.delete(task);
			session.flush();
		}
		catch (ObjectNotFoundException ex)
		{
			throw ex;
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
			
			throw new TransferException("Failed to delete transfer task", ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}

	
	/**
     * Selects a job at random from the population of jobs of the given {@code status}. This method
     * calls out to {@link #getRandomUserForNextQueuedJobOfStatus(JobStatusType, String, String[], String[])} 
     * first to select a user in a fair manner, then selects one of the user's jobs which honors
     * all system quotas and capacity filtering parameters which enable restriction of job 
     * selection by tenant, owner, system, and batch queue.
     * 
     * @param status by which to filter the list of pending {@link TransferTask}s.
     * @param tenantid tenant to include or exclude from selection.
     * @param owners array of owners to include or exclude from the selection process 
     * @param systemIds array of systems and queues to include or exclude from the selectino process. 
     * @return username of user with pending job
     * @see {@link #getRandomUserForNextQueuedJobOfStatus(JobStatusType, String, String[], String[])}
     * @throws JobException
     */
    public static String getNextQueuedTask(TransferStatusType status, String tenantId, String[] systemIds, String[] owners)
	throws TransferException
	{
        String transferTaskUuid = null;
        
	    boolean excludeTenant = false;
        if (StringUtils.isEmpty(tenantId)) {
            tenantId = "%";
        } else if (tenantId.contains("!")) {
            excludeTenant = true;
            tenantId = StringUtils.removeStart(tenantId, "!");
        }
        
        boolean excludeOwners = false;
        if (ArrayUtils.isNotEmpty(owners) && StringUtils.join(owners, ",").contains("!")) {
            excludeOwners = true;
            for (int i = 0; i < owners.length; i++)
                owners[i] = StringUtils.removeStart(owners[i], "!");
        }
        
        boolean excludeSystems = false;
        if (ArrayUtils.isNotEmpty(systemIds) && StringUtils.join(systemIds, ",").contains("!")) {
            excludeSystems = true;
            for (int i = 0; i < systemIds.length; i++)
                systemIds[i] = StringUtils.removeStart(systemIds[i], "!");
        }
        
		try
		{
		    Session session = getSession();
            
			session.disableFilter("transferTaskTenantFilter");
            
			String sql =  "select distinct t.owner, t.tenant_id, tt.active_transfer_count \n"
			            + "from transfertasks t \n"
			            + "      left join ( \n"
			            + "           select tc.owner, count(tc.id) as active_transfer_count \n" 
						+ "           from transfertasks tc \n"  
						+ "           where tc.status in ('" + ServiceUtils.explode("','", TransferStatusType.getActiveStatusValues())  + "') \n"
						+ "               and tc.tenant_id :excludetenant like :tenantid \n"
						+ "               and tc.root_task is null \n";
			
			if (!ArrayUtils.isEmpty(systemIds)) 
            {   
				sql	+=    "               and :excludesystems (";
			
    			for (int i=0;i<systemIds.length;i++) {
    			    sql += "                 tc.dest like 'agave://:systemid" + i + "%' ";
    			    
    			    if (systemIds.length > (i+1)) {
                        sql += " or \n ";
                    }
                }
    			
    			sql +=    "\n                 ) \n";
            }
			
			if (!ArrayUtils.isEmpty(owners)) {
                sql +=    "               and tc.owner :excludeowners in :owners \n";
            }
            
			sql		+=    "           group by tc.owner \n"
						+ "      ) as tt on t.owner = tt.owner \n" 
						+ "where j.status = :taskstatus \n"
						+ "   and ( \n"
						+ "       tt.active_transfer_count is NULL \n"
						+ "       or tt.active_transfer_count < :maxUserConcurrentTransfers \n" 
						+ "   ) \n"  
						+ "order rand()";
			
			sql = sql.replaceAll(":excludeowners", excludeOwners ? "not" : "")
                    .replaceAll(":excludetenant", excludeTenant ? "not" : "")
                    .replaceAll(":excludesystems", excludeSystems ? "not" : "");
           
			String q = sql.replaceAll(":excludesystems", excludeSystems ? "not" : "")
                    .replaceAll(":tenantid", String.format("'%s'", tenantId));
			
			Query query = session.createSQLQuery(sql)
                    .addScalar("owner", StandardBasicTypes.STRING)
                    .addScalar("active_transfer_count", StandardBasicTypes.INTEGER)
                    .addScalar("tenant_id", StandardBasicTypes.STRING)
                    .setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE)
                    .setString("tenantid", tenantId)
                    .setInteger("maxUserConcurrentTransfers", Settings.MAX_USER_CONCURRENT_TRANSFERS);
                    
			if (!ArrayUtils.isEmpty(owners)) 
            {
                query.setParameterList("owners", owners);
                q = q.replaceAll(":owners", "('" + StringUtils.join(owners, "','")+"')");
            }
            
            if (!ArrayUtils.isEmpty(systemIds)) 
            {
                for (int i=0;i<systemIds.length;i++) {
                    if (StringUtils.contains(systemIds[i], "#")) {
                        String[] tokens = StringUtils.split(systemIds[i], "#");
                        query.setString("systemid" + i, tokens[0]);
                        q = q.replaceAll(":systemid"+i, "'" + tokens[0] +"'");
                    }
                    else
                    {
                        query.setString("systemid" + i, systemIds[i]);
                        q = q.replaceAll(":systemid"+i, "'" + systemIds[i] +"'");
                    }
                }
            }
            
            log.debug(q);
            List<Map<String,Object>> aliasToValueMapList = (List<Map<String,Object>>)query.setCacheable(false)
                    .setCacheMode(CacheMode.REFRESH)
                    .setMaxResults(1).uniqueResult();
            
            if (aliasToValueMapList.isEmpty()) {
                return null;
            } else {
                String owner = (String) aliasToValueMapList.get(0).get("owner");
                String tid = (String) aliasToValueMapList.get(0).get("tenant_id");
                
                String taskSql =  "select t.uuid \n"
                        + "from transfertasks t \n"
                        + "where j.status = :taskstatus \n"
                        + "   and t.root_task is null \n"
                        + "   and t.tenant_id = :tid \n"
                        + "   and t.owner = :owner \n";
                if (!ArrayUtils.isEmpty(systemIds)) 
                {   
                    sql +=    "   and :excludesystems (";
                
                    for (int i=0;i<systemIds.length;i++) {
                        sql += "        tc.dest like 'agave://:systemid" + i + "%' ";
                        
                        if (systemIds.length > (i+1)) {
                            sql += " or \n ";
                        }
                    }
                    
                    sql += "\n       ) \n";
                }
                
                sql     += "order by t.last_updated desc";
                
                taskSql = taskSql.replaceAll(":excludesystems", excludeSystems ? "not" : "");
               
                String tq = taskSql.replaceAll(":tenantid", String.format("'%s'", tid));
                
                query = session.createSQLQuery(taskSql)
                        .setString("taskstatus", status.name())
                        .setString("tid", tid)
                        .setString("owner", owner);
                        
                if (!ArrayUtils.isEmpty(systemIds)) 
                {
                    for (int i=0;i<systemIds.length;i++) {
                        if (StringUtils.contains(systemIds[i], "#")) {
                            String[] tokens = StringUtils.split(systemIds[i], "#");
                            query.setString("systemid" + i, tokens[0]);
                            tq = tq.replaceAll(":systemid"+i, "'" + tokens[0] +"'");
                        }
                        else
                        {
                            query.setString("systemid" + i, systemIds[i]);
                            tq = tq.replaceAll(":systemid"+i, "'" + systemIds[i] +"'");
                        }
                    }
                }
                
                log.debug(tq);
                transferTaskUuid = (String)query
                        .setCacheable(false)
                        .setCacheMode(CacheMode.REFRESH)
                        .setMaxResults(1)
                        .uniqueResult();
            }
                
			session.flush();
		
			return transferTaskUuid;
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
			
			throw new TransferException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}

	@SuppressWarnings("unchecked")
	public static TransferSummary getTransferSummary(TransferTask task) throws TransferException
	{
		TransferSummary transferSummary = new TransferSummary();
		
		try
		{
			Session session = getSession();
			session.disableFilter("transferTaskTenantFilter");
			
			String hql = "from TransferTask as task where task.id = :rootid or task.rootTask.id = :rootid";

			List<TransferTask> tasks = (List<TransferTask>) session.createQuery(hql)
					.setLong("rootid", task.getId())
					.setCacheMode(CacheMode.IGNORE)
					.setCacheable(false)
					.list();
			
			session.flush();
			
			double transferRate = 0;
			double totalTransferredBytes = 0;
			double totalBytes = 0;
			long totalActiveTransfers = 0;
			long transfers = 0;
			if (tasks.size() > 1) {
				for (TransferTask childTask: tasks) 
				{
					totalBytes += childTask.getTotalSize();
					totalTransferredBytes += childTask.getBytesTransferred();
					if (!childTask.getStatus().equals(TransferStatusType.COMPLETED)) {
						totalActiveTransfers++;
					}
					double tRate = childTask.calculateTransferRate();
					if (tRate > 0) {
						transferRate += tRate;
						transfers++;
					}
				}
			} else {
				TransferTask childTask = tasks.get(0);
				totalBytes = childTask.getTotalSize();
				totalTransferredBytes += childTask.getBytesTransferred();
				if (!childTask.getStatus().equals(TransferStatusType.COMPLETED)) {
					totalActiveTransfers++;
				}
				transferRate = childTask.getTransferRate() > 0 ? childTask.getTransferRate() : childTask.calculateTransferRate();
				transfers = childTask.getTotalFiles() == 0 ? 1 : childTask.getTotalFiles();
			}
			
			transferSummary.setTotalTransfers(tasks.size());
			transferSummary.setTotalActiveTransfers(totalActiveTransfers);
			if (transfers > 0) {
				transferSummary.setAverageTransferRate( Math.floor((double)(transferRate / transfers)));
			} 
			transferSummary.setTotalTransferredBytes(BigInteger.valueOf((long)totalTransferredBytes));
			transferSummary.setTotalBytes(BigInteger.valueOf((long)totalBytes));
			
			return transferSummary;
			
		}
		catch (ObjectNotFoundException ex)
		{
			return transferSummary;
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
			
			throw new TransferException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}

	/**
	 * Sets a TransferTask and all its sub tasks to a status of TransferStatusType.CANCELLED
	 * 
	 * @param rootTask
	 * @throws TransferException
	 */
	public static void cancelAllRelatedTransfers(Long transferTask) throws TransferException 
	{
		try
		{
			Session session = getSession();
			
			String hql = "update TransferTask t "
					+ "set t.status = :status, t.lastUpdated = :instant "
					+ "where t.status in :statuses " 
					+ "		and ( "
					+ "			t.id = :taskid "
					+ "			or (t.rootTask is not null and t.rootTask.id = :taskid) "
					+ "			or (t.parentTask is not null and t.parentTask.id = :taskid) ) ";

			session.createQuery(hql)
					.setLong("taskid", transferTask)
					.setParameter("status", TransferStatusType.CANCELLED)
					.setParameterList("statuses", TransferStatusType.getActiveStatusValues())
					.setDate("instant", new Date())
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
			
			throw new TransferException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}

	public static TransferTask getChildTransferTask(Long rootTaskId, String srcUri, String destUri, String owner) 
	throws TransferException
	{
		try
		{
			Session session = getSession();
			
			String hql = "from TransferTask t where "
					+ "t.parentTask.id = :parentid and "
					+ "t.source = :srcuri and "
					+ "t.dest = :desturi and "
					+ "t.owner = :owner";

			TransferTask task = (TransferTask)session.createQuery(hql)
					.setLong("parentid", rootTaskId)
					.setString("srcuri", srcUri)
					.setString("desturi", destUri)
					.setString("owner", owner)
					.uniqueResult();
			
			session.flush();
			
			return task;
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
			
			throw new TransferException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}

	@SuppressWarnings("unchecked")
	public static TransferTask findRootTransferTaskBySourceDestAndOwner(String srcUri, String destUri, String owner) 
	throws TransferException
	{
		try
		{
			Session session = getSession();
			
			String hql = "from TransferTask t where "
					+ "t.endTime is null and "
					+ "t.parentTask is null and "
					+ "t.status = :status and "
					+ "t.source = :srcuri and "
					+ "t.dest = :desturi and "
					+ "t.owner = :owner "
					+ "order by t.id desc limit 1";

			List<TransferTask> tasks = (List<TransferTask>)session.createQuery(hql)
					.setString("srcuri", srcUri)
					.setString("desturi", destUri)
					.setString("owner", owner)
					.setString("status", TransferStatusType.TRANSFERRING.name())
					.list();
			
			session.flush();
			
			if (tasks.isEmpty()) {
				return null;
			} else {
				return tasks.get(0);
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
			
			throw new TransferException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}

	/**
     * Searches for {@link TransferTask}s by the given user who matches the given set of 
     * parameters. Permissions are honored in this query.
     *
     * @param username
     * @param searchCriteria
     * @param offset
     * @param limit
     * @return
     * @throws JobException
     */
    @SuppressWarnings("unchecked")
    public static List<TransferTask> findMatching(String username, Map<SearchTerm, Object> searchCriteria, int limit, int offset) 
    throws TransferException
    {
        try
        {
            Session session = getSession();
            session.clear();
            String sql = " SELECT j FROM TransferTask as t \n";
                    
            if (!ServiceUtils.isAdmin(username)) {
                
                sql += " WHERE ( \n" +
                    "       t.owner = :owner OR \n" +
                    "       t.id in ( \n" + 
                    "               SELECT pm.transferTaskId FROM TransferTaskPermission as pm \n" +
                    "               WHERE pm.username = :owner AND pm.permission <> :none \n" +
                    "              ) \n" +
                    "      ) AND \n";
            } else {
                sql += " WHERE ";
            }
            
            sql +=  "        j.tenantId = :tenantid "; 
            
            for (SearchTerm searchTerm: searchCriteria.keySet()) 
            {
                sql += "\n       AND       " + searchTerm.getExpression();
            }
            
            if (!sql.contains("j.visible")) {
                sql +=  "\n       AND j.visible = :visiblebydefault \n";
            }
            
            sql +=  " ORDER BY j.lastUpdated DESC\n";
            
            String q = sql;
            
            Query query = session.createQuery(sql)
                                 .setString("tenantid", TenancyHelper.getCurrentTenantId());
            
            q = q.replaceAll(":tenantid", "'" + TenancyHelper.getCurrentTenantId() + "'");
            
            if (sql.contains(":visiblebydefault") ) {
                query.setBoolean("visiblebydefault", Boolean.TRUE);
                
                q = q.replaceAll(":visiblebydefault", "1");
            }
            
            if (!ServiceUtils.isAdmin(username)) {
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
                        query.setString(searchTerm.getSearchField()+i, formattedDates.get(i));
                        q = q.replaceAll(":" + searchTerm.getSearchField(), "'" + formattedDates.get(i) + "'");
                    }
                }
                else if (searchTerm.getOperator().isSetOperator()) 
                {
                    query.setParameterList(searchTerm.getSearchField(), (List<Object>)searchCriteria.get(searchTerm));
                    q = q.replaceAll(":" + searchTerm.getSearchField(), "('" + StringUtils.join((List<String>)searchTerm.getOperator().applyWildcards(searchCriteria.get(searchTerm)), "','") + "')" );
                }
                else 
                {
                    query.setParameter(searchTerm.getSearchField(), 
                            searchTerm.getOperator().applyWildcards(searchCriteria.get(searchTerm)));
                    q = q.replaceAll(":" + searchTerm.getSearchField(), "'" + String.valueOf(searchTerm.getOperator().applyWildcards(searchCriteria.get(searchTerm))) + "'");
                }
                
            }
            
            // log.debug(q);
            
            List<TransferTask> transferTasks = query
                    .setFirstResult(offset)
                    .setMaxResults(limit)
                    .list();

            session.flush();
            
            return transferTasks;

        }
        catch (Throwable ex)
        {
            throw new TransferException(ex);
        }
        finally {
            try { HibernateUtil.commitTransaction();} catch (Exception e) {}
        }
    }
}
