/**
 * 
 */
package org.iplantc.service.jobs.dao;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.CacheMode;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.StaleObjectStateException;
import org.hibernate.StaleStateException;
import org.hibernate.UnresolvableObjectException;
import org.hibernate.transform.AliasToEntityMapResultTransformer;
import org.hibernate.type.StandardBasicTypes;
import org.iplantc.service.apps.util.ServiceUtils;
import org.iplantc.service.common.persistence.HibernateUtil;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.common.search.SearchTerm;
import org.iplantc.service.jobs.Settings;
import org.iplantc.service.jobs.exceptions.JobException;
import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.jobs.model.JobUpdateParameters;
import org.iplantc.service.jobs.model.enumerations.JobStatusType;
import org.iplantc.service.jobs.model.enumerations.PermissionType;
import org.iplantc.service.jobs.statemachine.JobFSMUtils;
import org.joda.time.DateTime;

/**
 * @author dooley
 * 
 */
public class JobDao
{
    private static final Logger log = Logger.getLogger(JobDao.class);
	
	protected static Session getSession() {
		Session session = HibernateUtil.getSession();
		HibernateUtil.beginTransaction();
        
		//session.clear();
		session.enableFilter("jobTenantFilter").setParameter("tenantId", TenancyHelper.getCurrentTenantId());
		return session;
	}
	
	protected static Session getSession(String tenantId) {
		Session session = HibernateUtil.getSession();
		HibernateUtil.beginTransaction();
        //session.clear();
		session.enableFilter("jobTenantActivityFilter").setParameter("tenantId", tenantId);
		return session;
	}
	
	/**
	 * Returns all jobs for a given username.
	 * 
	 * @param username
	 * @return
	 * @throws JobException
	 */
	public static List<Job> getByUsername(String username) throws JobException
	{
		return JobDao.getByUsername(username, 0, Settings.DEFAULT_PAGE_SIZE);
	}
	
	/**
	 * Returns all jobs for a given username.
	 * @param username
	 * @param offset
	 * @param limit
	 * @return
	 * @throws JobException
	 */
	@SuppressWarnings("unchecked")
	public static List<Job> getByUsername(String username, int offset, int limit) throws JobException
	{
		try
		{
			Session session = getSession();
			session.clear();
			String sql = "select distinct j.* "
					+ "from jobs j "
					+ "		left join job_permissions p on j.id = p.job_id "
					+ "where ("
					+ "			j.owner = :owner "
					+ "			or ("
					+ "				p.username = :owner " 
					+ "				and p.permission <> :none "  
					+ "			) " 
					+ "		) "
					+ "		and j.visible = :visible "
					+ "		and j.tenant_id = :tenantid "
					+ "order by j.id desc";
			
			List<Job> jobs = session.createSQLQuery(sql).addEntity(Job.class)
					.setString("owner",username)
					.setString("none",PermissionType.NONE.name())
					.setBoolean("visible", Boolean.TRUE)
					.setString("tenantid", TenancyHelper.getCurrentTenantId())
					.setFirstResult(offset)
					.setMaxResults(limit)
					.list();
			
			session.flush();
			
			return jobs;

		}
		catch (ObjectNotFoundException e) {
			return new ArrayList<Job>();
		}
		catch (HibernateException ex)
		{
			throw new JobException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction();} catch (Exception e) {}
		}
	}

	/**
	 * Gets a job by its unique id.
	 * @param jobId
	 * @return
	 * @throws JobException
	 */
	public static Job getById(long jobId) throws JobException
	{
		try
		{
			Session session = getSession();
//			session.clear();
			session.disableFilter("jobTenantFilter");
			Job job = (Job) session.get(Job.class, jobId);
			
//			session.flush();
			
			return job;

		}
		catch (ObjectNotFoundException ex)
		{
			return null;
		}
		catch (HibernateException ex)
		{
			throw new JobException(ex);
		}
		finally {
//			try { HibernateUtil.commitTransaction();} catch (Exception e) {}
		}
	}
	
	/**
     * Gets a {@link Job} by its uuid.
     * 
     * @param uuid identifier for the job
     * @return {@link Job} object
     * @throws JobException
     */
	public static Job getByUuid(String uuid) throws JobException
    {
	    return getByUuid(uuid, true);
    }
	
	/**
	 * Gets a {@link Job} by its uuid, optionally forcing cache eviction
	 * 
	 * @param uuid identifier for the job
	 * @param forceFlush should the cache be flushed on this request?
	 * @return {@link Job} object
	 * @throws JobException
	 */
	public static Job getByUuid(String uuid, boolean forceFlush) throws JobException
	{
		if (StringUtils.isEmpty(uuid)) return null;
		
		try
		{
		    HibernateUtil.beginTransaction();
			Session session = HibernateUtil.getSession();
			
			session.clear();
			
			Job job = (Job) session.createSQLQuery("select * from jobs where uuid = :uuid")
			        .addEntity(Job.class)
					.setString("uuid",  uuid)
					.setCacheable(false)
                    .setCacheMode(CacheMode.REFRESH)
                    .uniqueResult();
			
			session.flush();
			
			return job;

		}
		catch (ObjectNotFoundException ex)
		{
			return null;
		}
		catch (HibernateException ex)
		{
			throw new JobException(ex);
		}
		finally {
//		    if (forceFlush)
		        try { HibernateUtil.commitTransaction();} catch (Exception e) {}
		}
	}
	
	/**
	 * Refreshes a stale job
	 * @param job
	 * @return
	 * @throws JobException
	 */
	public static void refresh(Job job) throws JobException
	{
		try
		{
			Session session = getSession();
			session.clear();
			session.refresh(job);
			session.flush();
		}
		catch (HibernateException ex)
		{
		    log.error("Concurrency issue with job " + job.getUuid());
			throw new JobException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction();} catch (Exception e) {}
		}
	}
	
	/**
	 * Merges the current cached job object with the persisted value. The 
	 * current instance is not updated. The merged instance is returned.
	 * 
	 * @param job
	 * @return merged job.
	 * @throws JobException
	 */
	public static Job merge(Job job) throws JobException
	{
		try
		{
		    HibernateUtil.beginTransaction();
			Session session = HibernateUtil.getSession();
			return (Job)session.merge(job);
		}
		catch (HibernateException ex)
		{
		    log.error("Concurrency issue with job " + job.getUuid());
			throw new JobException(ex);
		}
	}
	
	/**
	 * Returns a job owned by the user with teh given job id. This is essentially
	 * just a security check to make sure the owner matches the job id. ACL are 
	 * not taken into consideration here, just ownership.
	 * 
	 * @param username
	 * @param jobId
	 * @return
	 * @throws JobException
	 */
	public static Job getByUsernameAndId(String username, long jobId) throws JobException
	{
		try
		{
			Session session = getSession();
			session.clear();
			String hql = "from Job j "
					+ "where j.owner = :owner and "
					+ "		j.id = :jid and "
					+ "		j.visible = :visible ";
			
			Job job = (Job)session.createQuery(hql)
					.setString("owner",username)
					.setLong("jid", jobId)
					.setBoolean("visible", Boolean.TRUE)
					.setMaxResults(1)
					.uniqueResult();
			
			session.flush();
			
			if (job == null) {
				throw new JobException("No job found matching the given owner and id.");
			} else {
				return job;
			}

		}
		catch (ObjectNotFoundException ex)
		{
			return null;
		}
		catch (HibernateException ex)
		{
			throw new JobException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction();} catch (Exception e) {}
		}
	}


    /**
     * Gets the least recently updated job for a given system id with the given
     * status. 
     * @param system
     * @param jobStatus
     * @return
     * @throws JobException
     * @deprecated
     */
    public static Job getNextJobOnSystemByStatus(String system, JobStatusType jobStatus) throws JobException
    {
        try
        {
        	Session session = getSession();
        	session.clear();
        	session.disableFilter("jobTenantFilter");
			
			String sql = "select distinct j.* "
					+ "from jobs j "
					+ "where ("
					+ "			j.execution_system = :system"
                    + " 		and j.status = :status"
                    + "		) "
                    + "order by j.last_updated desc";

            Job job = (Job) session.createSQLQuery(sql).addEntity(Job.class)
                    .setParameter("system",system)
                    .setParameter("status", jobStatus.toString())
                    .setLockMode("j", LockMode.PESSIMISTIC_FORCE_INCREMENT)
					.setMaxResults(1)
                    .uniqueResult();

//            session.flush();
            
            return job;
        } 
        catch (HibernateException ex) {
            throw new JobException(ex);
        }
        finally {
//			try { HibernateUtil.commitTransaction();} catch (Exception e) {}
		}
	}

    /** 
     * Convenience method that returns a {@link List} of {@link Job}s belonging 
     * with the given status.
     * 
     * @param status a job status
     * @return the job list
     * @throws JobException
     */
    public static List<Job> getByStatus(JobStatusType status) 
    throws JobException
    {
        // Package parameter and call real method.
        ArrayList<JobStatusType> list = new ArrayList<>(1);
        list.add(status);
        return getByStatus(list);
    }
    
    /**
     * Returns a {@link List} of {@link Job}s belonging with the given statuses.
     * 
     * @param statuses the non-empty list of status filters
     * @return the job list
     * @throws JobException
     */
    @SuppressWarnings("unchecked")
    public static List<Job> getByStatus(List<JobStatusType> statuses) 
    throws JobException
    {
        // At least one status must be specified.
        if (statuses == null || statuses.isEmpty())
        {
            String msg = "At least one status must be specified for job search.";
            log.error(msg);
            throw new JobException(msg);
        }
        
        // Create the where clause.
        String statusClause = " status in (";
        for (int i = 0; i < statuses.size(); i++)
        {
            // Single quote each status and add comma where needed.
            statusClause += "'" + statuses.get(i).name() + "'";
            if (i < statuses.size() - 1)
                statusClause += ", ";
        }
        statusClause += ") ";
        
        // Make the database call for all visible 
        // jobs with the required statuses.
        try
        {
            Session session = getSession();
            session.clear();
            String sql = "select * from jobs where " + statusClause +
                         " and visible = :visible";
            List<Job> jobs = session.createSQLQuery(sql).addEntity(Job.class)
                    .setBoolean("visible", true)
                    .list();

            session.flush();
            
            return jobs;

        }
        catch (ObjectNotFoundException e) {
            return new ArrayList<Job>();
        }
        catch (HibernateException ex)
        {
            throw new JobException(ex);
        }
        finally {
            try { HibernateUtil.commitTransaction();} catch (Exception e) {}
        }
    }

    /**
     * Returns a {@link List} of {@link Job}s with the given status that have
     * an outstanding interrupt.
     * 
     * @param status the status filter
     * @return the interrupted jobs with the specified status
     * @throws JobException
     */
    @SuppressWarnings("unchecked")
    public static List<Job> getInterruptedJobsByStatus(JobStatusType status) 
    throws JobException
    {
        // At least one status must be specified.
        if (status == null)
        {
            String msg = "A job status must be specified for job search.";
            log.error(msg);
            throw new JobException(msg);
        }
        
        // Make the database call for all visible 
        // jobs with the required statuses.
        try
        {
            Session session = getSession();
            session.clear();
            String sql = "select distinct jobs.* from jobs, job_interrupts " + 
                         "where jobs.uuid = job_interrupts.job_uuid " +
                         "and jobs.status = :status " +
                         "and jobs.visible = :visible";
            List<Job> jobs = session.createSQLQuery(sql).addEntity(Job.class)
                    .setString("status", status.name())
                    .setBoolean("visible", true)
                    .list();

            session.flush();
            
            return jobs;

        }
        catch (ObjectNotFoundException e) {
            return new ArrayList<Job>();
        }
        catch (HibernateException ex)
        {
            throw new JobException(ex);
        }
        finally {
            try { HibernateUtil.commitTransaction();} catch (Exception e) {}
        }
    }

	/**
	 * Returns a {@link List} of {@link Job}s belonging to the given user with the given status.
	 * Permissions are observed in this query.
	 * 
	 * @param username
	 * @param jobStatus
	 * @return
	 * @throws JobException
	 */
	@SuppressWarnings("unchecked")
	public static List<Job> getByUsernameAndStatus(String username, JobStatusType jobStatus) 
	throws JobException
	{
		try
		{
			Session session = getSession();
			session.clear();
			String sql = "select distinct j.* from jobs j " +
					"left join job_permissions p on j.id = p.job_id " +
					"where ( " + 
					"        j.owner = :owner " +
					"        or 1 = :isadmin " +
					"		 or ( " + 
					"             p.username = :owner " +
					"			  and p.permission <> :none " +
					"		    ) " + 
					"		) and " +
					"		j.visible = :visible and " +
					"		j.status = :status " + 
					"order by j.last_updated desc ";
			
			List<Job> jobs = session.createSQLQuery(sql).addEntity(Job.class)
					.setString("owner",username)
					.setString("none",PermissionType.NONE.name())
					.setInteger("isadmin", new Integer(BooleanUtils.toInteger(ServiceUtils.isAdmin(username))) )
					.setBoolean("visible", true)
					.setString("status", jobStatus.name())
					.list();

			session.flush();
			
			return jobs;

		}
		catch (ObjectNotFoundException e) {
			return new ArrayList<Job>();
		}
		catch (HibernateException ex)
		{
			throw new JobException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction();} catch (Exception e) {}
		}
	}
	
	
	/**
	 * Returns the number of jobs in an active state for a given user 
	 * on a given system. Active states are defined by 
	 * JobStatusType.getActiveStatuses().
	 * 
	 * @param username
	 * @param system
	 * @return
	 * @throws JobException
	 */
	public static long countActiveUserJobsOnSystem(String username, String system) throws JobException
	{
		try
		{
			Session session = getSession();
			session.clear();
			String hql = "select count(j.id) "
					+ "from Job j "
					+ "where j.owner = :jobowner "
					+ " 	and j.visible = :visible "
					+ "		and j.system = :jobsystem "
					+ "		and j.status in "
					+ "(" + JobStatusType.getActiveStatusValues() + ") ";
			
			long currentUserJobCount = ((Long)session.createQuery(hql)
					.setBoolean("visible", true)
					.setString("jobowner",username)
					.setString("jobsystem", system)
					.uniqueResult()).longValue();
			
			session.flush();
			
			return currentUserJobCount;

		}
		catch (ObjectNotFoundException ex)
		{
			throw new JobException(ex);
		}
		catch (HibernateException ex)
		{
			throw new JobException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction();} catch (Exception e) {}
		}
	}
	
//	/**
//	 * Returns the least recently checked active job for monitoring.
//	 * 
//	 * @return
//	 * @throws JobException
//	 */
//	public static Job getNextActiveJob() throws JobException
//	{
//		try
//		{
//			Session session = getSession();
//			session.clear();
//			session.disableFilter("jobTenantFilter");
//			
//			String hql = "from Job j where "
//					+ "j.lastUpdated > :refreshRate and "
//					+ "j.status in (" + JobStatusType.getActiveStatusValues() + ") " +
//					"order by rand()";
//			
//			Job job = (Job)session.createQuery(hql)
//					.setTimestamp("refreshRate", new Date(System.currentTimeMillis() - 300000))
//					.setLockMode("j", LockMode.PESSIMISTIC_FORCE_INCREMENT)
//					.setMaxResults(1)
//					.uniqueResult();
//			
////            if (job != null) {
////            	session.buildLockRequest(LockOptions.UPGRADE).setLockMode(LockMode.PESSIMISTIC_WRITE).setTimeOut(0).lock(job);           
////            }
////			session.flush();
//			
//			return job;
//		}
//		catch (StaleObjectStateException ex) {
//			return null;
//		}
//		catch (ObjectNotFoundException ex)
//		{
//			return null;
//		}
//		catch (HibernateException ex)
//		{
//			throw new JobException(ex);
//		}
//		finally {
////			try { HibernateUtil.commitTransaction();} catch (Exception e) {}
//		}
//	}
	
	/**
	 * Returns the least recently checked active job for monitoring.
	 * 
	 * @return
	 * @throws JobException
	 */
	public static String getNextExecutingJobUuid(String tenantId, String[] owners, String[] systemIds) 
	throws JobException
	{
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
			session.disableFilter("jobTenantFilter");
//			session.clear();
			/* we need a step function here to scale back slower...
			 * every 30 seconds for the first 5 minutes
			 * every minute for the next 30 minutes
			 * every 5 minutes for the next hour
			 * every 15 minutes for the next 12 hours
			 * every 30 minutes for the next 24 hours
			 * every hour for the next 14 days
			 */
			String sql =  "select j.uuid \n";
    			        
			if (!ArrayUtils.isEmpty(systemIds)) 
            {   
                sql += "from ( \n"
                     + "    select s.system_id, q.name as queue_name, s.tenant_id \n"
                     + "    from systems s left join batchqueues q on s.id = q.execution_system_id \n"
                     + "    where s.tenant_id :excludetenant like :tenantid and :excludesystems (\n";
                
                for (int i=0;i<systemIds.length;i++) {
                    
                    if (StringUtils.contains(systemIds[i], "#")) {
                        sql += "        (s.system_id = :systemid" + i + " and q.name = :queuename" + i + ") ";
                    } else {
                        sql += "        (s.system_id = :systemid" + i + ") ";
                    }
                    
                    if (systemIds.length > (i+1)) {
                        sql += " or \n ";
                    }
                }
                
                sql    += "\n        ) \n"
                        + "    ) e left join jobs j on j.execution_system = e.system_id \n"
                        + "        and j.queue_request = e.queue_name \n"
                        + "        and j.tenant_id = e.tenant_id ";
            } 
			else {
			    sql    += "from jobs j ";
			}
			
    			sql    += "\nwhere j.visible = 1 and j.created < j.last_updated + INTERVAL 60 DAY \n"
    					+ "    and (\n"
    					+ "        (j.status_checks < 4 and CURRENT_TIMESTAMP >= j.last_updated + INTERVAL 15 SECOND) or  \n"
    					+ "        (j.status_checks < 14 and CURRENT_TIMESTAMP >= j.last_updated + INTERVAL 30 SECOND) or  \n"
    					+ "        (j.status_checks < 44 and CURRENT_TIMESTAMP >= j.last_updated + INTERVAL 60 SECOND) or  \n"
    					+ "        (j.status_checks < 56 and CURRENT_TIMESTAMP >= j.last_updated + INTERVAL 5 MINUTE) or  \n"
    					+ "        (j.status_checks < 104 and CURRENT_TIMESTAMP >= j.last_updated + INTERVAL 15 MINUTE) or  \n"
    					+ "        (j.status_checks < 152 and CURRENT_TIMESTAMP >= j.last_updated + INTERVAL 30 MINUTE) or  \n"
    					+ "        (CURRENT_TIMESTAMP >= j.last_updated + INTERVAL 1 HOUR) \n"
    					+ "    )  \n"
    					+ "    and (\n"
    					+ "      j.status = :queuedstatus \n"
    					+ "      or j.status = :runningstatus \n"
    					+ "      or j.status = :pausedstatus \n"
    					+ "    ) \n"
    					+ "    and j.local_job_id is not null \n"
			            + "    and j.tenant_id :excludetenant like :tenantid \n";
    		
    		if (!ArrayUtils.isEmpty(owners)) {
    		    sql     += "    and j.owner :excludeowners in :owners  \n"; 
    		}
			    sql     += "order by rand()\n";
			
			sql = StringUtils.replace(sql, ":excludeowners", excludeOwners ? "not" : "");
			sql = StringUtils.replace(sql, ":excludetenant", excludeTenant ? "not" : "");
			sql = StringUtils.replace(sql, ":excludesystems", excludeSystems ? "not" : "");
    			
            String q = StringUtils.replace(sql, ":queuedstatus", "'QUEUED'");
            q = StringUtils.replace(q, ":runningstatus", "'RUNNING'");
            q = StringUtils.replace(q, ":pausedstatus", "'PAUSED'");
            q = StringUtils.replace(q, ":tenantid", "'" + tenantId + "'");        
            
			Query query = session.createSQLQuery(sql)
					.setString("queuedstatus", JobStatusType.QUEUED.name())
					.setString("runningstatus", JobStatusType.RUNNING.name())
					.setString("pausedstatus", JobStatusType.PAUSED.name())
			        .setString("tenantid", tenantId);
			
			if (!ArrayUtils.isEmpty(owners))
			{
			    // remove leading negation if present
                for (int i = 0; i < owners.length; i++)
                    owners[i] = StringUtils.removeStart(owners[i], "!");
                
                query.setParameterList("owners", owners);
                
                q = StringUtils.replace(q, ":owners", "('" + StringUtils.join(owners, "','")+"')");
            }
            
            if (!ArrayUtils.isEmpty(systemIds)) 
            {
                // remove leading negation if present
                for (int i = 0; i < systemIds.length; i++)
                    systemIds[i] = StringUtils.removeStart(systemIds[i], "!");
                
                for (int i=0;i<systemIds.length;i++) {
                    if (StringUtils.contains(systemIds[i], "#")) {
                        String[] tokens = StringUtils.split(systemIds[i], "#");
                        query.setString("systemid" + i, tokens[0]);
                        q = StringUtils.replace(q, ":systemid"+i, "'" + tokens[0] +"'");
                        if (tokens.length > 1) {
                            query.setString("queuename" + i, tokens[1]); 
                            q = StringUtils.replace(q, ":queuename"+i, "'" + tokens[1] +"'");
                        }
                    }
                    else
                    {
                        query.setString("systemid" + i, systemIds[i]);
                        q = StringUtils.replace(q, ":systemid"+i, "'" + systemIds[i] +"'");
                    }
                }
            }
			
            // log.debug(q);
			   
            String uuid = (String)query
			        .setCacheable(false)
                    .setCacheMode(CacheMode.REFRESH)
                    .setMaxResults(1)
                    .uniqueResult();
            
            return uuid;
            
//            if (!results.isEmpty() && results.get(0).length > 0) {
//                return (String) results.get(0)[0];
//            } else {
//                return null;
//            }
		}
		catch (StaleObjectStateException ex) {
			return null;
		}
		catch (ObjectNotFoundException ex)
		{
			return null;
		}
		catch (HibernateException ex)
		{
			throw new JobException(ex);
		}
		finally {
//			try { HibernateUtil.commitTransaction();} catch (Exception e) {}
		}
	}
	
	/**
	 * Returns the total number of jobs in an active state 
	 * on a given system. Active states are defined by 
	 * JobStatusType.getActiveStatuses().
	 * 
	 * @param username
	 * @param system
	 * @return jobCount 
	 * @throws JobException
	 */
	public static long countActiveJobsOnSystem(String system) throws JobException
	{
		try
		{
			Session session = getSession();
			session.clear();
			String hql = "select count(*) from Job " +
					"where system = :jobsystem and " +
					"status in (" + JobStatusType.getActiveStatusValues() + ")";
			
			long currentUserJobCount = ((Long)session.createQuery(hql)
					.setString("jobsystem", system)
					.uniqueResult()).longValue();
			
			session.flush();
			
			return currentUserJobCount;

		}
		catch (ObjectNotFoundException ex)
		{
			throw new JobException(ex);
		}
		catch (HibernateException ex)
		{
			throw new JobException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction();} catch (Exception e) {}
		}
	}
	
	/** Get the current status value of a job record and maintain an exclusive 
	 * lock on that record.  We begin a transaction, query a job by its primary
	 * key, and keep that transaction open when the method completes.  
	 * 
	 * The caller is responsible for committing or rolling back the transaction
	 * and if this requirement is not observed, bad things will happen.  To
	 * emphasize:
	 * 
	 *     THE CALLER MUST COMPLETE THE TRANSACTION BEGUN HERE
	 *     
	 * The session on which the transaction can be rolled back or committed is
	 * the threadlocal session managed by HibernateUtil.  Therefore, the 
	 * transaction can be completed by calling rollbackTransaction() or
	 * commitTransaction() on HibernateUtil on the SAME THREAD.  The usual case,
	 * however, is to call persistLockedJob(job) to update the job record to
	 * complete the transaction. 
	 * 
	 * NOTE: If the query fails, the transaction is rolled back and a null
	 *       status is returned.  The caller does not need to complete the 
	 *       transaction in this case.
	 * 
	 * @param job the job to lock
	 * @return the current version of a job (open transaction) or 
	 *         null (rolled back transaction) 
	 */
    public static JobStatusType lockJobForStatus(Job job)
    {
        // Get the current version of a job record 
        // and obtain an exclusive lock on that record.
        JobStatusType status = null;
        try {
            // Get a hibernate session.
            Session session = HibernateUtil.getSession();
            session.clear();
            HibernateUtil.beginTransaction();
            
            // Retrieve the current version of the job.
            String sql = "select status from jobs " +
                         "where id = :id FOR UPDATE";
       
            // Make the query and leave transaction open.          
            String result = (String) session.createSQLQuery(sql)
                            .setLong("id", job.getId())
                            .uniqueResult();
            
            // Convert the result string.
            if (result != null) {
                try {status = JobStatusType.valueOf(result);}
                catch (Exception e) {
                    String msg = "Unable to convert " + result + " into a JobStatusType";
                    log.error(msg, e);
                    throw e;
                }
            }
        }
        catch (Exception e)
        {
            // Rollback transaction.
            try {HibernateUtil.rollbackTransaction();}
             catch (Exception e1){log.error("Rollback failed.", e1);}
            
            String msg = "Unable to lock running job " + job.getUuid() + 
                         " (" + job.getName() + ").";
            log.error(msg, e);
        }
        
        // Return session with uncommitted transaction.
        return status;
    }
	
    /** Get the current status value of a job record and maintain an exclusive 
     * lock on that record.  We begin a transaction, query a job by its primary
     * key, and keep that transaction open when the method completes.  
     * 
     * The caller is responsible for committing or rolling back the transaction
     * and if this requirement is not observed, bad things will happen.  To
     * emphasize:
     * 
     *     THE CALLER MUST COMPLETE THE TRANSACTION BEGUN HERE
     *     
     * The session on which the transaction can be rolled back or committed is
     * the threadlocal session managed by HibernateUtil.  Therefore, the 
     * transaction can be completed by calling rollbackTransaction() or
     * commitTransaction() on HibernateUtil on the SAME THREAD.  The usual case,
     * however, is to call persistLockedJob(job) to update the job record to
     * complete the transaction. 
     * 
     * NOTE: If the query fails, the transaction is rolled back and a null
     *       status is returned.  The caller does not need to complete the 
     *       transaction in this case.
     * 
     * @param jobUuid the unique identifier of the job to lock
     * @return the current version of a job (open transaction) or 
     *         null (rolled back transaction) 
     */
    public static JobStatusType lockJobForStatus(String jobUuid)
    {
        // Get the current version of a job record 
        // and obtain an exclusive lock on that record.
        JobStatusType status = null;
        try {
            // Get a hibernate session.
            Session session = HibernateUtil.getSession();
            session.clear();
            HibernateUtil.beginTransaction();
            
            // Retrieve the current version of the job.
            String sql = "select status from jobs " +
                         "where uuid = :uuid FOR UPDATE";
       
            // Make the query and leave transaction open.          
            String result = (String) session.createSQLQuery(sql)
                            .setString("uuid", jobUuid)
                            .uniqueResult();
            
            // Convert the result string.
            if (result != null) {
                try {status = JobStatusType.valueOf(result);}
                catch (Exception e) {
                    String msg = "Unable to convert " + result + " into a JobStatusType";
                    log.error(msg, e);
                    throw e;
                }
            }
        }
        catch (Exception e)
        {
            // Rollback transaction.
            try {HibernateUtil.rollbackTransaction();}
             catch (Exception e1){log.error("Rollback failed.", e1);}
            
            String msg = "Unable to lock running job with UUID " + jobUuid + ").";
            log.error(msg, e);
        }
        
        // Return session with uncommitted transaction.
        return status;
    }
    
    /** Use this method insert new job records into the database.
     * 
     * @param job the new job
     * @throws JobException
     * @throws UnresolvableObjectException
     */
	public static void create(Job job) 
	throws JobException, UnresolvableObjectException 
	{
		create(job, true);
	}
	
    /** Use this method insert new job records into the database.
     * 
     * @param job the new job
     * @param forceTimestamp true to assign current time to lastUpated field
     * @throws JobException
     * @throws UnresolvableObjectException
     */
	public static void create(Job job, boolean forceTimestamp) 
	throws JobException, UnresolvableObjectException
	{
		if (job == null)
			throw new JobException("Job cannot be null");
		
		Session session = null;
		
		try
		{
			session = getSession();
//			SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
			
			if (forceTimestamp) {
//				log.debug(String.format("Job.created(pre force timestamp)[%s] %s vs %s vs %s", job.getUuid(), f.format(job.getCreated()), new DateTime().toString(), f.format(new Date())));
//				log.debug(String.format("Job.lastUpdated(pre force timestamp)[%s] %s vs %s vs %s", job.getUuid(), f.format(job.getLastUpdated()), new DateTime().toString(), f.format(new Date())));
				job.setLastUpdated(new DateTime().toDate());
			}
			
//			log.debug(String.format("Job.created[%s] %s vs %s vs %s", job.getUuid(), f.format(job.getCreated()), new DateTime().toString(), f.format(new Date())));
//			log.debug(String.format("Job.lastUpdated(pre force timestamp)[%s] %s vs %s vs %s", job.getUuid(), f.format(job.getLastUpdated()), new DateTime().toString(), f.format(new Date())));
			
			// Use the update() method in this class for updates.
			session.save(job);
		}
		catch (UnresolvableObjectException ex) {
//		    throw ex;
		}
		catch (StaleStateException ex) {
//			throw new UnresolvableObjectException(job.getId(), Job.class.getName());
//		    log.error("Concurrency issue with job " + job.getUuid());
		    throw ex;
		}
		catch (HibernateException ex)
		{
			try
			{
				if (session != null && session.isOpen())
				{
					HibernateUtil.rollbackTransaction();
					session.close();
				}
			}
			catch (Exception e) {}
			log.error("Concurrency issue with job " + job.getUuid());
			
			throw new JobException(ex);
		}
		finally {
		    try {HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}

    /** Perform an atomic update of all fields set in the parameter object and
     * refresh the job object to keep hibernate happy.  See the 3 parameter version
     * of this method for details.
     *  
     * @param job a hibernate-managed job object
     * @param parms the update fields and values
     * @return the number of rows in the database affected (0 or 1)
     */ 
    public static int update(Job job, JobUpdateParameters parms)
     throws JobException
    {
        // Make sure we have a job.
        if (job == null) {
            String msg = "Null job received.";
            log.error(msg);
            throw new JobException(msg);
        }
        
        // Call the real update method and then refresh the job object.
        int rows = update(job.getUuid(), job.getTenantId(), parms);
        refresh(job);
        return rows;
    }
            
	/** Perform an atomic update of all fields set in the parameter object.
	 * All updates are performed in a single transaction.
	 * 
	 * This method and JobManager.updateStatus() comprise the two ways that status
	 * updates can be safely performed.  This method is called by JobManager.updateStatus() 
	 * and is ultimately responsible for validating status changes and locking
	 * the job record during status transitions.  Use this method if you only need
	 * to update a job record, use JobManager.updateStatus() if you also require 
	 * visibility and event processing.  
	 * 
	 * NOTE: This method does not update an in-memory job object, so existing
	 *       job objects outside of this method will not contain the latest
	 *       values after the job in the database is updated.  Use JobDao.refresh() 
	 *       or the overloaded version of this method if you need the latest 
	 *       database updates reflected in job objects. 
	 * 
	 * If the status field is set, the update will continue only if the
	 * transition from the current status as recorded in the database to
	 * the new status in the parameter object is legal.  This method locks
	 * the job record to read the current status, preventing concurrent
	 * status updates from stepping on each other.
	 * 
	 * If the lastUpdateTime has not been set, this method will set it in the 
	 * parameter object before processing the object.  This modification of an
	 * input variable is a side-effect of calling this method.
	 * 
	 * See the JobUpdateParameters class for guidance on how to add update 
	 * support for new fields.  All fields in the parameter object are processed 
	 * by the createSetClause() and setUpdatePlaceholders() methods.
	 * 
	 * @param jobUuid the unique identifier of the job to be updated
	 * @param jobTenantId the tenant id of the job
	 * @param parms the update fields and values
	 * @return the number of rows in the database affected (0 or 1)
	 */
	public static int update(String jobUuid, String jobTenantId, 
	                         JobUpdateParameters parms)
	 throws JobException
	{
	    // ------------------- Check Input -------------------
	    // Make sure we have a job uuid.
        if (jobUuid == null) {
            String msg = "Null job UUID received.";
            log.error(msg);
            throw new JobException(msg);
        }
	    // Make sure we have a tenant.
        if (jobTenantId == null) {
            String msg = "Null tenant ID received.";
            log.error(msg);
            throw new JobException(msg);
        }
	    // Make sure field updates have been specified.
	    if (parms == null) {
	        String msg = "Null update parameter received.";
	        log.error(msg);
	        throw new JobException(msg);
	    }
	    
        // -------------------- Permission Checks ------------
        // We only allow users to create queues under their own tenant. 
        String currentTenantId = TenancyHelper.getCurrentTenantId();
        if (StringUtils.isBlank(currentTenantId)) {
            String msg = "Unable to retrieve current tenant id.";
            log.error(msg);
            throw new JobException(msg);
        }
        if (!jobTenantId.equals(currentTenantId)) {
            String msg = "Unable to update job because " +
                         "the current tenant id (" + currentTenantId + ") does not match " +
                         "the job tenant id (" + jobTenantId + ").";
            log.error(msg);
            throw new JobException(msg);
        }
	    
	    // ------------------- Create Set Clause -------------
        // Assign last update time if it's not already set.
        if (!parms.isLastUpdatedFlag()) parms.setLastUpdated(new Date());
        
        // Construct the set clause for the sql update statement.
	    String setClause = createSetClause(parms);
	    if (setClause == null) {
            String msg = "No field values to update.";
            log.error(msg);
            throw new JobException(msg);
	    }

	    // ------------------- Get Session -------------------
	    // Status changes require validation using the current status in the database
	    // and require us to lock the job record until this transaction completes.
	    // The lock method acquires the session for us and starts a transaction.
	    Session session = null;
	    
	    // Both branches of this conditional statement set the session variable.
	    if (parms.isStatusFlag()) {
	        
	        // A transaction is begun only if the lock method returns a non-null status.
	        JobStatusType curStatus = lockJobForStatus(jobUuid);
	        if (curStatus == null) {
	            String msg = "Unable to query job record for current status.";
	            log.error(msg);
	            throw new JobException(msg);
	        }
	        
	        // Check if the status change is legal.
	        if (!JobFSMUtils.hasTransition(curStatus, parms.getStatus())) {
	            
	            // Abort the update since the attempted status 
	            // change is illegal, but first log the problem.
	            String msg = "Invalid transition from " + curStatus.name() +
	                         " to " + parms.getStatus() + " attempted.";
	            log.error(msg);
	            
	            // Release the lock on the job record.
	            try {HibernateUtil.rollbackTransaction();}
	            catch (Exception e) {
	                String msg2 = "Failure rolling back transaction on locked job record.";
	                log.error(msg2, e);
	            }
	            
	            // Throw our exception.
	            throw new JobException(msg);
	        }
	        
	        // Get the thread-local session in which the lock routine began our transaction.
	        try {session = HibernateUtil.getSession();}
	            catch (Exception e) {
	                String msg = "Unable to retrieve thread-local session.";
	                log.error(msg, e);
	                throw new JobException(msg);
	            }
	    }
	    else {
	        try {
	            // Get a hibernate session.
	            session = HibernateUtil.getSession();
	            session.clear();
	            HibernateUtil.beginTransaction();
	        }
            catch (Exception e) {
                String msg = "Unable to start transaction on thread-local session.";
                log.error(msg, e);
                throw new JobException(msg);
            }
	    }
	    
	    // ------------------- Issue Update ------------------
	    // Note from this point on we must close the transaction before returning.
	    int rows = 0;
	    try {
            // Construct the update statement.
            String sql = "Update jobs " + setClause +
                         " where uuid = :uuid and tenant_id = :tenantId";
            Query qry = session.createSQLQuery(sql);
            qry.setString("uuid", jobUuid);
            qry.setString("tenantId", jobTenantId);
            setUpdatePlaceholders(qry, parms);
            qry.setCacheable(false);
            qry.setCacheMode(CacheMode.IGNORE);
            rows = qry.executeUpdate();
            
            // Sanity check.
            if (rows == 0)
            {
                // We expected something to happen...
                String msg = "Nothing updated for job " + jobUuid + " under tenant " + jobTenantId + ".";
                log.warn(msg);
            }
	        
            // End the transaction.
            HibernateUtil.commitTransaction();
	    }
	    catch (Exception e) {
            // Rollback transaction.
            try {HibernateUtil.rollbackTransaction();}
             catch (Exception e1){log.error("Rollback failed.", e1);}
            
            // Log original problem.
	        String msg = "Error updating job " + jobUuid + " for tenant " + jobTenantId + ".";
	        log.error(msg, e);
	        throw new JobException(msg, e);
	    }
	    
	    // 0 or 1 row.
	    return rows;
	}
	
	/** Create the set clause with no padding for an sql update command given
	 * a parameter object.  If no values are set, null is returned. 
	 * 
	 * @param parms object that specified at least one update value
	 * @return the sql set clause with no leading or trailing space characters
	 *             or null
	 */
	private static String createSetClause(JobUpdateParameters parms)
	{
	    // Initialize the set clause.
	    final String initialClause = "set ";
	    String clause = initialClause;
	    
	    // Fields are processed in alphabetic order for our benefit.
	    //
	    // The calling method and this method must agree and placeholder
	    // names used inside of the constructed string.
	    // ----- archivePath
	    if (parms.isArchivePathFlag()) {
	        if (!initialClause.equals(clause)) clause += ", ";
	        clause += "archive_path = :archivePath";
	    }
	    
	    // ----- created
        if (parms.isCreatedFlag()) {
            if (!initialClause.equals(clause)) clause += ", ";
            clause += "created = :created";
        }
        
	    // ----- endTime
        if (parms.isEndTimeFlag()) {
            if (!initialClause.equals(clause)) clause += ", ";
            clause += "end_time = :endTime";
        }
        
        // ----- errorMessage
        if (parms.isErrorMessageFlag()) {
            if (!initialClause.equals(clause)) clause += ", ";
            clause += "error_message = :errorMessage";
        }
        
	    // ----- lastUpdated
        if (parms.isLastUpdatedFlag()) {
            if (!initialClause.equals(clause)) clause += ", ";
            clause += "last_updated = :lastUpdated";
        }
        
	    // ----- localJobId
        if (parms.isLocalJobIdFlag()) {
            if (!initialClause.equals(clause)) clause += ", ";
            clause += "local_job_id = :localJobId";
        }
        
        // ----- owner
        if (parms.isOwnerFlag()) {
            if (!initialClause.equals(clause)) clause += ", ";
            clause += "owner = :owner";
        }
        
	    // ----- retries
        if (parms.isRetriesFlag()) {
            if (!initialClause.equals(clause)) clause += ", ";
            clause += "retries = :retries";
        }
        
	    // ----- startTime
        if (parms.isStartTimeFlag()) {
            if (!initialClause.equals(clause)) clause += ", ";
            clause += "start_time = :startTime";
        }
        
	    // ----- status
        if (parms.isStatusFlag()) {
            if (!initialClause.equals(clause)) clause += ", ";
            clause += "status = :status";
        }
        
        // ----- statusChecks
        if (parms.isStatusChecksFlag()) {
            if (!initialClause.equals(clause)) clause += ", ";
            clause += "status_checks = :statusChecks";
        }
        
	    // ----- submitTime
        if (parms.isSubmitTimeFlag()) {
            if (!initialClause.equals(clause)) clause += ", ";
            clause += "submit_time = :submitTime";
        }
        
	    // ----- visible
        if (parms.isVisible()) {
            if (!initialClause.equals(clause)) clause += ", ";
            clause += "visible = :visible";
        }
        
	    // ----- workPath
        if (parms.isWorkPathFlag()) {
            if (!initialClause.equals(clause)) clause += ", ";
            clause += "work_path = :workPath";
        }
        
	    // See if we found any fields to update.
	    if (initialClause.equals(clause)) return null;
	    return clause;
	}
	
	/** Assign the placeholder variable in the update query with the values
	 * in the parameter object.
	 * 
	 * @param qry the sql update statement
	 * @param parms the parameters object containing the new values
	 */
	private static void setUpdatePlaceholders(Query qry, JobUpdateParameters parms)
	{
	    // ----- archivePath
        if (parms.isArchivePathFlag()) 
            qry.setString("archivePath", parms.getArchivePath());
        
        // ----- created
        if (parms.isCreatedFlag()) 
            qry.setTimestamp("created", parms.getCreated());
        
        // ----- endTime
        if (parms.isEndTimeFlag()) 
            qry.setTimestamp("endTime", parms.getEndTime());
        
        // ----- errorMessage
        if (parms.isErrorMessageFlag()) 
            qry.setString("errorMessage", parms.getErrorMessage());
        
        // ----- lastUpdated
        if (parms.isLastUpdatedFlag()) 
            qry.setTimestamp("lastUpdated", parms.getLastUpdated());
        
        // ----- localJobId
        if (parms.isLocalJobIdFlag()) 
            qry.setString("localJobId", parms.getLocalJobId());
        
        // ----- owner
        if (parms.isOwnerFlag()) 
            qry.setString("owner", parms.getOwner());
        
        // ----- retries
        if (parms.isRetriesFlag()) 
            qry.setInteger("retries", parms.getRetries());
        
        // ----- startTime
        if (parms.isStartTimeFlag()) 
            qry.setTimestamp("startTime", parms.getStartTime());
        
        // ----- status
        if (parms.isStatusFlag()) 
            qry.setString("status", parms.getStatus().name());
        
        // ----- statusChecks
        if (parms.isStatusChecksFlag()) 
            qry.setInteger("statusChecks", parms.getStatusChecks());
        
        // ----- submitTime
        if (parms.isSubmitTimeFlag()) 
            qry.setTimestamp("submitTime", parms.getSubmitTime());
        
        // ----- visible
        if (parms.isVisible()) 
            qry.setBoolean("visible", parms.isVisible());
        
        // ----- workPath
        if (parms.isWorkPathFlag()) 
            qry.setString("workPath", parms.getWorkPath());
	}
	
	/**
	 * Deletes a job from the db.
	 * @param job
	 * @throws JobException
	 */
	public static void delete(Job job) throws JobException
	{
		if (job == null)
			throw new JobException("Job cannot be null");

		try
		{
			Session session = getSession();
			session.disableFilter("jobTenantFilter");
			session.disableFilter("jobEventTenantFilter");
			session.disableFilter("jobPermissionTenantFilter");
			session.clear();
			session.delete(job);
//			session.evict(job);
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
			catch (Throwable e) {}
				
			throw new JobException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction();} catch (Exception e) {}
		}
	}
	
	
	/**
	 * Selects a job at random from the population of jobs waiting to be archived. The selection 
	 * is made by first getting a list of all users with pending archiving jobs, then selecting 
	 * one user and picking one of their jobs at random. All capacity filtering parameters which 
     * enable restriction of job selection by tenant, owner, system, and batch queue are taken
     * into consideration.
	 * 
	 * @param tenantid tenant to include or exclude from selection.
     * @param owners array of owners to include or exclude from the selection process 
	 * @param systemIds array of systems and queues to include or exclude from the selectino process. 
	 * @return uuid of next job to archive.
	 * @throws JobException
	 */
	@SuppressWarnings("unchecked")
	public static String getFairButRandomJobUuidForNextArchivingTask(String tenantId, String[] owners, String[] systemIds)
	throws JobException
	{
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
			session.clear();
			session.disableFilter("jobTenantFilter");
			
			
			String sql = "select j.owner, usg.pending_archiving_tasks, usg.active_archiving_tasks, j.tenant_id  \n";
			
			if (!ArrayUtils.isEmpty(systemIds)) 
            {   
                sql += "from ( \n"
                     + "    select s.system_id, q.name as queue_name, s.tenant_id \n"
                     + "    from systems s left join batchqueues q on s.id = q.execution_system_id \n"
                     + "    where s.tenant_id :excludetenant like :tenantid and :excludesystems (\n";
                
                for (int i=0;i<systemIds.length;i++) {
                    
                    if (StringUtils.contains(systemIds[i], "#")) {
                        sql += "        (s.system_id = :systemid" + i + " and q.name = :queuename" + i + ") ";
                    } else {
                        sql += "        (s.system_id = :systemid" + i + ") ";
                    }
                    
                    if (systemIds.length > (i+1)) {
                        sql += " or \n ";
                    }
                }
                
                sql    += "\n        ) \n"
                        + "    ) e left join jobs j on j.execution_system = e.system_id \n"
                        + "        and j.queue_request = e.queue_name \n"
                        + "        and j.tenant_id = e.tenant_id ";
            } 
            else {
                sql    += "from jobs j ";
            }
			
			sql  += "\n    left join (  \n" + 
					"		select jj.owner,   \n" + 
					"			sum(CASE WHEN jj.status = 'CLEANING_UP' then 1 else 0 end) as pending_archiving_tasks,  \n" + 
					"			sum(CASE WHEN jj.status = 'ARCHIVING' then 1 else 0 end) as active_archiving_tasks,  \n" + 
					"			jj.tenant_id  \n" + 
					"		from jobs jj  where jj.visible = 1 \n" + 
					"		group by jj.owner, jj.tenant_id  \n" + 
					"    ) as usg on j.owner = usg.owner and j.tenant_id = usg.tenant_id  \n" + 
					"where j.status in ('ARCHIVING', 'CLEANING_UP')   \n" + 
					"	and j.visible = 1   \n" +
					"	and usg.pending_archiving_tasks > 0   \n" +
					"	and usg.tenant_id :excludetenant like :tenantid  \n";
			
			if (!ArrayUtils.isEmpty(owners)) {
			    sql += " and usg.owner :excludeowners in :owners  \n";
			}
			
			if (!ArrayUtils.isEmpty(systemIds)) {
			    
			    sql += "    and :excludesystems ( ";
			    
			    for (int i=0;i<systemIds.length;i++) {
			        
			        if (StringUtils.contains(systemIds[i], "#")) {
			            sql += "         (j.execution_system = :systemid" + i + " and j.queue_request = :queuename" + i + ")  \n";
			        } else {
			            sql += "         (j.execution_system = :systemid" + i + "  \n"
                             + "             or j.archive_system in ( \n"
                             + "                                    select s.id  \n"
                             + "                                    from systems s  \n"
                             + "                                    where e.system_id in (:systemid" + i + ")  \n" 
                             + "                                            and s.tenant_id = j.tenant_id  \n"
                             + "                                     ) \n"
                             + "         )  \n";
			        }
			        
			        if (systemIds.length > (i+1)) {
			            sql += " or  \n";
			        }
			    }
			    
			    sql += "        ) \n";
			}
			
			sql +=  "group by j.owner, j.tenant_id, usg.pending_archiving_tasks, usg.active_archiving_tasks  \n" + 
					"order by RAND() \n";
			
			sql = StringUtils.replace(sql, ":excludeowners", excludeOwners ? "not" : "");
			sql = StringUtils.replace(sql, ":excludetenant", excludeTenant ? "not" : "");
			sql = StringUtils.replace(sql, ":excludesystems", excludeSystems ? "not" : "");
           
			String q = StringUtils.replace(sql, ":queuedstatus", "'QUEUED'");
			q = StringUtils.replace(q, ":runningstatus", "'RUNNING'");
			q = StringUtils.replace(q, ":pausedstatus", "'PAUSED'");
			q = StringUtils.replace(q, ":tenantid", "'" + tenantId + "'");    
			
//			log.debug(sql);
			Query query = session.createSQLQuery(sql)
			        .addScalar("owner", StandardBasicTypes.STRING)
					.addScalar("pending_archiving_tasks", StandardBasicTypes.INTEGER)
					.addScalar("active_archiving_tasks", StandardBasicTypes.INTEGER)
					.addScalar("tenant_id", StandardBasicTypes.STRING)
					.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE)
					.setString("tenantid", tenantId);
			
			if (!ArrayUtils.isEmpty(owners)) 
			{
			    query.setParameterList("owners", owners);
			    q = StringUtils.replace(q, ":owners", "('" + StringUtils.join(owners, "','")+"')");
            }
            
            if (!ArrayUtils.isEmpty(systemIds)) 
            {
                for (int i=0;i<systemIds.length;i++) {
                    if (StringUtils.contains(systemIds[i], "#")) {
                        String[] tokens = StringUtils.split(systemIds[i], "#");
                        query.setString("systemid" + i, tokens[0]);
                        q = StringUtils.replace(q, ":systemid"+i, "'" + tokens[0] +"'");
                        if (tokens.length > 1) {
                            query.setString("queuename" + i, tokens[1]); 
                            q = StringUtils.replace(q, ":queuename"+i, "'" + tokens[1] +"'");
                        }
                    }
                    else
                    {
                        query.setString("systemid" + i, systemIds[i]);
                        q = StringUtils.replace(q, ":systemid"+i, "'" + systemIds[i] +"'");
                    }
                }
            }
            
//            log.debug(q);
            
            List<Map<String,Object>> aliasToValueMapList = query.setCacheable(false)
                                                                .setCacheMode(CacheMode.REFRESH)
                                                                .setMaxResults(1).list();
			
			if (aliasToValueMapList.isEmpty()) {
				return null;
			} else {
				String username = (String) aliasToValueMapList.get(0).get("owner");
				String tid = (String) aliasToValueMapList.get(0).get("tenant_id");
				sql = "select j.uuid \n";
				if (!ArrayUtils.isEmpty(systemIds)) 
	            {   
	                sql += "from ( \n"
	                     + "    select s.system_id, q.name as queue_name, s.tenant_id \n"
	                     + "    from systems s left join batchqueues q on s.id = q.execution_system_id \n"
	                     + "    where s.tenant_id :excludetenant like :tenantid and :excludesystems (\n";
	                
	                for (int i=0;i<systemIds.length;i++) {
	                    
	                    if (StringUtils.contains(systemIds[i], "#")) {
	                        sql += "        (s.system_id = :systemid" + i + " and q.name = :queuename" + i + ") ";
	                    } else {
	                        sql += "        (s.system_id = :systemid" + i + ") ";
	                    }
	                    
	                    if (systemIds.length > (i+1)) {
	                        sql += " or \n ";
	                    }
	                }
	                
	                sql    += "\n        ) \n"
	                        + "    ) e left join jobs j on j.execution_system = e.system_id \n"
	                        + "        and j.queue_request = e.queue_name \n"
	                        + "        and j.tenant_id = e.tenant_id ";
	            } 
	            else {
	                sql    += "from jobs j \n";
	            }
				
				sql	+= "where j.owner = :owner \n"
					+ "		and j.visible = 1  \n"
					+ "		and j.tenant_id = :tenantid \n"
					+ "		and j.status = 'CLEANING_UP' \n"
					+ "order by rand() ";
				
				
				
				String jq = StringUtils.replace(sql, ":excludesystems", excludeSystems ? "not" : "");
				jq = StringUtils.replace(jq, ":tenantid", tid);
				jq = StringUtils.replace(jq, ":owner", username);
				
				sql = StringUtils.replace(sql, ":excludetenant", excludeTenant ? "not" : "");
				sql = StringUtils.replace(sql, ":excludesystems", excludeSystems ? "not" : "");
	           
				Query jobQuery = session.createSQLQuery(sql)
						.setString("owner", username)
						.setString("tenantid", tid);
				
				if (!ArrayUtils.isEmpty(systemIds)) 
	            {
	                for (int i=0;i<systemIds.length;i++) {
	                    if (StringUtils.contains(systemIds[i], "#")) {
	                        String[] tokens = StringUtils.split(systemIds[i], "#");
	                        jobQuery.setString("systemid" + i, tokens[0]);
	                        jq = StringUtils.replace(jq, ":systemid"+i, "'" + tokens[0] +"'");
	                        if (tokens.length > 1) {
	                            jobQuery.setString("queuename" + i, tokens[1]); 
	                            jq = StringUtils.replace(jq, ":queuename"+i, "'" + tokens[1] +"'");
	                        }
	                    }
	                    else
	                    {
	                        jobQuery.setString("systemid" + i, systemIds[i]);
	                        jq = StringUtils.replace(jq, ":systemid"+i, "'" + systemIds[i] +"'");
	                    }
	                }
	            }
				
				// log.debug(jq);
				
				List<String> uuids = (List<String>)jobQuery.setCacheable(false)
						.setCacheMode(CacheMode.REFRESH)
                    	.setMaxResults(1)
						.list();
				
				if (uuids.isEmpty()) {
					// should never happen
					return null;
				} else {
					return uuids.get(0);
				}
			}
		}
		catch (StaleObjectStateException ex) {
			return null;
		}
		catch (ObjectNotFoundException ex)
		{
			return null;
		}
		catch (HibernateException ex)
		{
			throw new JobException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction();} catch (Exception e) {}
		}
		
	}
	
	/**
	 * Selects a user at random from the population of users with jobs of the given {@code status}.
	 * System quotas are taken into consideration as are the capacity filtering parameters which 
	 * enable restriction of job selection by tenant, owner, system, and batch queue.
	 * 
	 * @param status by which to filter the list of active jobs.
	 * @param tenantid tenant to include or exclude from selection.
     * @param owners array of owners to include or exclude from the selection process 
     * @param systemIds array of systems and queues to include or exclude from the selectino process. 
     * @return username of user with pending job
	 * @throws JobException
	 */
	@SuppressWarnings("unchecked")
	public static String getRandomUserForNextQueuedJobOfStatus(JobStatusType status, String tenantId, String[] systemIds, String[] owners)
	throws JobException
	{
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
		
		Session session = null;
		try
		{ 
		    HibernateUtil.beginTransaction();
			session = HibernateUtil.getSession();
			session.clear();
			
//			String sql = "select jq.owner, jq.execution_system, jq.queue_request, jq.status, jq.jq.tenant_id  " + 
			String sql = "select jq.owner, jq.tenant_id   \n" +
    			        "from (    \n" + 
    			        "       select sq.system_id, sq.name, sq.max_jobs, sq.max_user_jobs, t.system_backlogged_queue_jobs, t.system_queue_jobs, sq.tenant_id    \n" + 
    			        "       from (    \n";
			if (JobStatusType.PENDING == status) {
                sql +=  "               select bqj.execution_system, bqj.queue_request, sum( if(bqj.status not in ('PENDING','PROCESSING_INPUTS'), 1, 0)) as system_queue_jobs, sum( if(bqj.status in ('PENDING','PROCESSING_INPUTS'), 1, 0)) as system_backlogged_queue_jobs, bqj.tenant_id     \n";
			} else {
			    sql +=  "               select bqj.execution_system, bqj.queue_request, sum( if(bqj.status not in ('PENDING','PROCESSING_INPUTS','STAGED','STAGING_INPUTS'), 1, 0)) as system_queue_jobs, sum( if(bqj.status in ('PENDING','PROCESSING_INPUTS','STAGED','STAGING_INPUTS'), 1, 0)) as system_backlogged_queue_jobs, bqj.tenant_id     \n";
			}
    		sql +=      "               from jobs bqj     \n" + 
    			        "               where bqj.visible = 1 and bqj.status in ('PENDING','PROCESSING_INPUTS', 'RUNNING', 'PAUSED', 'QUEUED', 'CLEANING_UP', 'SUBMITTING', 'STAGING_INPUTS', 'STAGING_JOB', 'STAGED') \n" +
    			        "                     and bqj.tenant_id :excludetenant like :tenantid \n";
	        
    	    if (!ArrayUtils.isEmpty(owners)) {
    	        sql += "    and bqj.owner :excludeowners in :owners \n";
            }
            
            if (!ArrayUtils.isEmpty(systemIds)) {
                
                sql += "    and :excludesystems ( \n";
                
                for (int i=0;i<systemIds.length;i++) {
                    
                    if (StringUtils.contains(systemIds[i], "#")) {
                        sql += "         (bqj.execution_system = :systemid" + i + " and bqj.queue_request = :queuename" + i + ") ";
                    } else {
                        sql += "         (bqj.execution_system = :systemid" + i + ") ";
                    }
                    
                    if (systemIds.length > (i+1)) {
                        sql += " or \n";
                    }
                }
                
                sql += "        ) \n";
            }
                    
    		sql +=      "               group by bqj.execution_system, bqj.queue_request, bqj.tenant_id    \n" + 
    			        "           ) as t  \n" + 
    			        "           left join (  \n" + 
    			        "               select ss.system_id, q.name, q.max_jobs, q.max_user_jobs, ss.tenant_id   \n" + 
    			        "               from batchqueues q  \n" + 
    			        "                   left join systems ss on ss.id = q.execution_system_id  \n" + 
    			        "                   where ss.type = 'EXECUTION'  \n" + 
    			        "           ) as sq on sq.system_id = t.execution_system and sq.name = t.queue_request and t.tenant_id = sq.tenant_id    \n" + 
    			        "       where t.system_queue_jobs < sq.max_jobs     \n" + 
    			        "           or sq.max_jobs = -1    \n" + 
    			        "           or sq.max_jobs is NULL  \n" + 
    			        "   ) as sysq   \n" + 
    			        "   left join (    \n" + 
    			        "       select buqj.owner, buqj.status, buqj.execution_system, buqj.queue_request, auj.user_system_queue_jobs, auj.total_backlogged_user_jobs, buqj.tenant_id     \n" + 
    			        "       from jobs buqj  \n" + 
    			        "           left join ( \n";
    		if (JobStatusType.PENDING == status) {
                sql +=  "               select aj.owner, aj.execution_system, aj.queue_request, sum( if(aj.status in ('PENDING','PROCESSING_INPUTS'), 0, 1)) as user_system_queue_jobs, sum( if(aj.status in ('PENDING','PROCESSING_INPUTS'), 1, 0)) as total_backlogged_user_jobs, aj.tenant_id \n";
    		} else {
    		    sql +=  "               select aj.owner, aj.execution_system, aj.queue_request, sum( if(aj.status in ('PENDING','PROCESSING_INPUTS','STAGED','STAGING_INPUTS'), 0, 1)) as user_system_queue_jobs, sum( if(aj.status in ('PENDING','PROCESSING_INPUTS','STAGED','STAGING_INPUTS'), 1, 0)) as total_backlogged_user_jobs, aj.tenant_id \n";
    		}
    		sql +=      "               from jobs aj  \n" + 
    			        "               where aj.visible = 1 and aj.status in ( 'PENDING','PROCESSING_INPUTS', 'RUNNING', 'PAUSED', 'QUEUED', 'CLEANING_UP', 'SUBMITTING', 'STAGING_INPUTS', 'STAGING_JOB', 'STAGED')   \n" + 
    			        "               group by aj.owner, aj.execution_system, aj.queue_request, aj.tenant_id \n" + 
    			        "           ) as auj on buqj.owner = auj.owner and buqj.execution_system = auj.execution_system and buqj.queue_request = auj.queue_request and buqj.tenant_id = auj.tenant_id \n" + 
    			        "       group by buqj.tenant_id, buqj.owner, buqj.status, buqj.execution_system, buqj.queue_request, auj.user_system_queue_jobs, auj.total_backlogged_user_jobs \n" + 
    			        "   ) as jq on jq.execution_system = sysq.system_id and jq.queue_request = sysq.name and jq.tenant_id = sysq.tenant_id    \n" + 
    			        "where     \n" + 
    			        "   jq.status like :status      \n" + 
    			        "   and (    \n" + 
    			        "       jq.user_system_queue_jobs < sysq.max_user_jobs    \n" + 
    			        "       or sysq.max_user_jobs = -1    \n" + 
    			        "       or sysq.max_user_jobs is NULL   \n" + 
    			        "   ) \n"; 
    			        
			if (JobStatusType.PENDING == status) {
				sql +=	"	and jq.total_backlogged_user_jobs > 0 \n"; 	
			}
			if (!ArrayUtils.isEmpty(owners)) {
                sql += "    and jq.owner :excludeowners in :owners \n";
            }
			sql +=		"	and jq.queue_request is not NULL    \n" + 
						"	and jq.queue_request <> ''    \n" +
						
						"	and jq.tenant_id :excludetenant like :tenantid \n" +
//						"group by jq.owner, jq.execution_system, jq.queue_request, jq.status, jq.jq.tenant_id    \n" +
						"group by jq.owner, jq.tenant_id   \n" +
						"order by rand() \n";
	//					"order by jq.execution_system, jq.queue_request, jq.owner ";
			
			sql = StringUtils.replace(sql, ":excludeowners", excludeOwners ? "not" : "");
			sql = StringUtils.replace(sql, ":excludetenant", excludeTenant ? "not" : "");
			sql = StringUtils.replace(sql, ":excludesystems", excludeSystems ? "not" : "");
           
            
			String q = StringUtils.replace(sql, ":status", "'" + status + "'");
            q = StringUtils.replace(q, ":tenantid", "'" + tenantId + "'");
			
			Query query = session.createSQLQuery(sql)
			        .addScalar("owner", StandardBasicTypes.STRING)
//					.addScalar("execution_system", StandardBasicTypes.STRING)
//					.addScalar("batch_queue", StandardBasicTypes.STRING)
//					.addScalar("user_system_queue_jobs", StandardBasicTypes.INTEGER)
//					.addScalar("total_backlogged_user_jobs", StandardBasicTypes.INTEGER)
					.addScalar("tenant_id", StandardBasicTypes.STRING) 
					.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE)
					.setString("status", status.name())
					.setString("tenantid", tenantId);
			
			if (!ArrayUtils.isEmpty(owners)) {
			    query.setParameterList("owners", owners);
                q = StringUtils.replace(q, ":owners", "('" + StringUtils.join(owners, "','")+"')");
            }
			
			if (!ArrayUtils.isEmpty(systemIds)) {
			    for (int i=0;i<systemIds.length;i++) 
                {
                    if (StringUtils.contains(systemIds[i], "#")) {
                        String[] tokens = StringUtils.split(systemIds[i], "#");
                        query.setString("systemid" + i, tokens[0]);
                        q = StringUtils.replace(q, ":systemid"+i, "'" + tokens[0] +"'");
                        if (tokens.length > 1) {
                            query.setString("queuename" + i, tokens[1]); 
                            q = StringUtils.replace(q, ":queuename"+i, "'" + tokens[1] +"'");
                        }
                    }
                    else
                    {
                        query.setString("systemid" + i, systemIds[i]);
                        q = StringUtils.replace(q, ":systemid"+i, "'" + systemIds[i] +"'");
                    }
                }
			}
			
//			log.debug(q);
//			log.debug(query.getQueryString());
			List<Map<String,Object>> aliasToValueMapList = query
			        .setCacheable(false)
                    .setCacheMode(CacheMode.REFRESH)
                    .setMaxResults(1)
					.list();
			
			if (aliasToValueMapList.isEmpty()) {
				return null;
			} else {
//				long randomIndex = RandomUtils.nextInt(aliasToValueMapList.size());
			    String username = (String) aliasToValueMapList.get(0).get("owner");
//			     log.debug("Selected " + username + " for the next job of status " + status.name());
				return username;
			}
		}
		catch (StaleObjectStateException ex) {
			return null;
		}
		catch (ObjectNotFoundException ex)
		{
			return null;
		}
		catch (HibernateException ex)
		{
			throw new JobException(ex);
		}
		finally {
            try { HibernateUtil.commitTransaction();} catch (Exception e) {}
        }
	}

	/**
     * Selects a job at random from the population of jobs of the given {@code status}. This method
     * calls out to {@link #getRandomUserForNextQueuedJobOfStatus(JobStatusType, String, String[], String[])} 
     * first to select a user in a fair manner, then selects one of the user's jobs which honors
     * all system quotas and capacity filtering parameters which enable restriction of job 
     * selection by tenant, owner, system, and batch queue.
     * 
     * @param status by which to filter the list of active jobs.
     * @param tenantid tenant to include or exclude from selection.
     * @param owners array of owners to include or exclude from the selection process 
     * @param systemIds array of systems and queues to include or exclude from the selectino process. 
     * @return username of user with pending job
     * @see {@link #getRandomUserForNextQueuedJobOfStatus(JobStatusType, String, String[], String[])}
     * @throws JobException
     */
    @SuppressWarnings("unchecked")
    public static String getNextQueuedJobUuid(JobStatusType status, String tenantId, String[] owners, String[] systemIds)
	throws JobException
	{
    	// we don't need to clone these arrays, they should be safe, but I'm tired and this is safe, so we'll do this
		// until i come back and add formal whitelist/blacklist support.
		String nextUser = getRandomUserForNextQueuedJobOfStatus(status, tenantId, (String[])ArrayUtils.clone(systemIds), (String[])ArrayUtils.clone(owners));
        
		boolean excludeTenant = false;
	    if (StringUtils.isEmpty(tenantId)) {
            tenantId = "%";
        } else if (tenantId.contains("!")) {
            excludeTenant = true;
            tenantId = StringUtils.removeStart(tenantId, "!");
        }
	    
		try
		{
		    
			if (StringUtils.isNotEmpty(nextUser))
			{
			    HibernateUtil.beginTransaction();
			    
    			Session session = HibernateUtil.getSession();
    			
                String sql = "select jq.owner, jq.tenant_id, jq.uuid   \n" +
                            "from (    \n" + 
                            "       select sq.system_id, sq.name, sq.max_jobs, sq.max_user_jobs, t.system_backlogged_queue_jobs, t.system_queue_jobs, sq.tenant_id    \n" + 
                            "       from (    \n";
                if (JobStatusType.PENDING == status) {
                    sql +=  "               select bqj.execution_system, bqj.queue_request, sum( if(bqj.status not in ('PENDING','PROCESSING_INPUTS'), 1, 0)) as system_queue_jobs, sum( if(bqj.status in ('PENDING','PROCESSING_INPUTS'), 1, 0)) as system_backlogged_queue_jobs, bqj.tenant_id     \n";
                } else {
                    sql +=  "               select bqj.execution_system, bqj.queue_request, sum( if(bqj.status not in ('PENDING','PROCESSING_INPUTS','STAGED','STAGING_INPUTS'), 1, 0)) as system_queue_jobs, sum( if(bqj.status in ('PENDING','PROCESSING_INPUTS','STAGED','STAGING_INPUTS'), 1, 0)) as system_backlogged_queue_jobs, bqj.tenant_id     \n";
                }
                sql +=      "               from jobs bqj     \n" + 
                            "               where bqj.visible = 1 and bqj.status in ('PENDING','PROCESSING_INPUTS', 'RUNNING', 'PAUSED', 'QUEUED', 'CLEANING_UP', 'SUBMITTING', 'STAGING_INPUTS', 'STAGING_JOB', 'STAGED') \n" +
                            "                     and bqj.tenant_id :excludetenant like :tenantid \n" + 
                            "                     and bqj.owner = :owner \n";
                
                if (!ArrayUtils.isEmpty(systemIds)) 
                {
                    boolean exclusive = false;
                    if (StringUtils.join(systemIds, ",").contains("!")) {
                        exclusive = true;
                    }
                    
                    sql += "    and ( \n";
                    
                    for (int i=0;i<systemIds.length;i++) {
                        
                        if (StringUtils.contains(systemIds[i], "#")) {
                            sql += "         (bqj.execution_system " + (exclusive ? " <> " : " = ") + " :systemid" + i + " and bqj.queue_request " + (exclusive ? " <> " : " = ") + " :queuename" + i + ") ";
                        } else {
                            sql += "         (bqj.execution_system " + (exclusive ? " <> " : " = ") + " :systemid" + i + ") ";
                        }
                        
                        if (systemIds.length > (i+1)) {
                            sql += " or \n";
                        }
                    }
                    
                    sql += "        ) \n";
                }
                        
                sql +=      "               group by bqj.execution_system, bqj.queue_request, bqj.tenant_id    \n" + 
                            "           ) as t  \n" + 
                            "           left join (  \n" + 
                            "               select ss.system_id, q.name, q.max_jobs, q.max_user_jobs, ss.tenant_id   \n" + 
                            "               from batchqueues q  \n" + 
                            "                   left join systems ss on ss.id = q.execution_system_id  \n" + 
                            "                   where ss.type = 'EXECUTION'  \n" + 
                            "           ) as sq on sq.system_id = t.execution_system and sq.name = t.queue_request and t.tenant_id = sq.tenant_id    \n" + 
                            "       where t.system_queue_jobs < sq.max_jobs     \n" + 
                            "           or sq.max_jobs = -1    \n" + 
                            "           or sq.max_jobs is NULL  \n" + 
                            "   ) as sysq   \n" + 
                            "   left join (    \n" + 
                            "       select buqj.owner, buqj.status, buqj.execution_system, buqj.queue_request, auj.user_system_queue_jobs, auj.total_backlogged_user_jobs, buqj.tenant_id, buqj.uuid     \n" + 
                            "       from jobs buqj  \n" + 
                            "           left join ( \n";
                if (JobStatusType.PENDING == status) {
                    sql +=  "               select aj.owner, aj.execution_system, aj.queue_request, sum( if(aj.status in ('PENDING','PROCESSING_INPUTS'), 0, 1)) as user_system_queue_jobs, sum( if(aj.status in ('PENDING','PROCESSING_INPUTS'), 1, 0)) as total_backlogged_user_jobs, aj.tenant_id \n";
                } else {
                    sql +=  "               select aj.owner, aj.execution_system, aj.queue_request, sum( if(aj.status in ('PENDING','PROCESSING_INPUTS','STAGED','STAGING_INPUTS'), 0, 1)) as user_system_queue_jobs, sum( if(aj.status in ('PENDING','PROCESSING_INPUTS','STAGED','STAGING_INPUTS'), 1, 0)) as total_backlogged_user_jobs, aj.tenant_id \n";
                }
                sql +=      "               from jobs aj  \n" + 
                            "               where aj.visible = 1 and aj.status in ( 'PENDING','PROCESSING_INPUTS', 'RUNNING', 'PAUSED', 'QUEUED', 'CLEANING_UP', 'SUBMITTING', 'STAGING_INPUTS', 'STAGING_JOB', 'STAGED')   \n" + 
                            "               group by aj.owner, aj.execution_system, aj.queue_request, aj.tenant_id \n" + 
                            "           ) as auj on buqj.owner = auj.owner and buqj.execution_system = auj.execution_system and buqj.queue_request = auj.queue_request and buqj.tenant_id = auj.tenant_id \n" + 
                            "       group by buqj.tenant_id, buqj.owner, buqj.status, buqj.execution_system, buqj.queue_request, auj.user_system_queue_jobs, auj.total_backlogged_user_jobs, buqj.uuid \n" + 
                            "   ) as jq on jq.execution_system = sysq.system_id and jq.queue_request = sysq.name and jq.tenant_id = sysq.tenant_id    \n" + 
                            "where     \n" + 
                            "   jq.status like :status      \n" + 
                            "   and (    \n" + 
                            "       jq.user_system_queue_jobs < sysq.max_user_jobs    \n" + 
                            "       or sysq.max_user_jobs = -1    \n" + 
                            "       or sysq.max_user_jobs is NULL   \n" + 
                            "   ) \n"; 
                            
                if (JobStatusType.PENDING == status) {
                    sql +=  "   and jq.total_backlogged_user_jobs > 0 \n";    
                }
                
                sql +=      "   and jq.queue_request is not NULL    \n" + 
                            "   and jq.queue_request <> ''    \n" + 
                            "   and jq.tenant_id :excludetenant like :tenantid \n" +
                            "group by jq.owner, jq.execution_system, jq.queue_request, jq.status, jq.tenant_id, jq.uuid    \n" +
                            "order by rand() \n";
                
                sql = StringUtils.replace(sql, ":excludetenant", excludeTenant ? "not" : "");
    			
                Query query = session.createSQLQuery(sql)
                        .addScalar("owner", StandardBasicTypes.STRING)
                        .addScalar("tenant_id", StandardBasicTypes.STRING)
                        .addScalar("uuid", StandardBasicTypes.STRING)
                        .setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE)
                        .setString("status", status.name())
                        .setString("tenantid", tenantId)
                        .setString("owner", nextUser);
                
                String q = StringUtils.replace(sql, ":status", "'" + status + "'");
                q = StringUtils.replace(q, ":tenantid", "'" + tenantId + "'");
                q = StringUtils.replace(q, ":owner", nextUser);
                
                if (!ArrayUtils.isEmpty(systemIds)) 
                {
                    // remove leading negation if present
                    for (int i = 0; i < systemIds.length; i++)
                        systemIds[i] = StringUtils.removeStart(systemIds[i], "!");
                    
                    
                    for (int i=0;i<systemIds.length;i++) 
                    {
                        if (StringUtils.contains(systemIds[i], "#")) {
                            String[] tokens = StringUtils.split(systemIds[i], "#");
                            query.setString("systemid" + i, tokens[0]);
                            q = StringUtils.replace(q, ":systemid"+i, "'" + tokens[0] +"'");
                            if (tokens.length > 1) {
                                query.setString("queuename" + i, tokens[1]); 
                                q = StringUtils.replace(q, ":queuename"+i, "'" + tokens[1] +"'");
                            }
                        }
                        else
                        {
                            query.setString("systemid" + i, systemIds[i]);
                            q = StringUtils.replace(q, ":systemid"+i, "'" + systemIds[i] +"'");
                        }
                    }
                }
                
//                 log.debug(q);              
                List<Map<String,Object>> aliasToValueMapList = query
                        .setCacheable(false)
                        .setCacheMode(CacheMode.REFRESH)
                        .setMaxResults(1)
                        .list();
                
                if (aliasToValueMapList.isEmpty()) {
                    return null;
                } else {
                    String uuid = (String) aliasToValueMapList.get(0).get("uuid");
                    // log.debug("Selected " + uuid + " for the next job of status " + status.name());
//                    return JobDao.getByUuid(uuid);
                    return uuid;
                }
    		}
			return null;
		}
		catch (StaleObjectStateException ex) {
			return null;
		}
		catch (ObjectNotFoundException ex)
		{
			return null;
		}
		catch (HibernateException ex)
		{
			throw new JobException(ex);
		}
		finally {
            try { HibernateUtil.commitTransaction();} catch (Exception e) {}
        }
	}
	
	/**
	 * Performs a string replacement and update on the job with the 
	 * given id. This is only needed because job inputs are not a 
	 * separate table.
	 * 
	 * @param jobId
	 * @param source
	 * @param dest
	 * @throws JobException
	 */
	public static void updateInputs(long jobId, String source, String dest)
	throws JobException
	{
		try
		{
			Session session = getSession();
			session.clear();
			String sql = "update jobs set inputs = replace(inputs, :source, :dest)";
			session.createQuery(sql).setString("source", source).setString(
					"dest", dest).executeUpdate();
			
			session.flush();
		}
		catch (HibernateException ex)
		{
			throw new JobException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction();} catch (Exception e) {}
		}

	}

	/**
	 * Searches for jobs by the given user who matches the given set of 
	 * parameters. Permissions are honored in this query.
	 * 
	 * @param username
	 * @param searchCriteria Map of key value pairs by which to query.
	 * @return
	 * @throws JobException
	 */
	public static List<Job> findMatching(String username,
			Map<SearchTerm, Object> searchCriteria) throws JobException
	{
		return JobDao.findMatching(username, searchCriteria, 0, Settings.DEFAULT_PAGE_SIZE);
	}
	
	/**
	 * Searches for jobs by the given user who matches the given set of 
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
	public static List<Job> findMatching(String username,
			Map<SearchTerm, Object> searchCriteria,
			int offset, int limit) throws JobException
	{
		try
		{
			Session session = getSession();
			session.clear();
			String sql = " SELECT j FROM Job as j \n";
					//"LEFT OUTER JOIN JobPermission as p ON j.id = p.jobId ";
			if (!ServiceUtils.isAdmin(username)) {
				
				sql += " WHERE ( \n" +
				    "       j.owner = :jobowner OR \n" +
					"       j.id in ( \n" + 
				    "               SELECT pm.jobId FROM JobPermission as pm \n" +
					"               WHERE pm.username = :jobowner AND pm.permission <> :none \n" +
					"              ) \n" +
					"      ) AND \n";
			} else {
				sql += " WHERE ";
			}
			
			sql +=  "        j.tenantId = :tenantid "; 
			
			for (SearchTerm searchTerm: searchCriteria.keySet()) 
			{
//			    if (searchTerm.getOperator() == SearchTerm.Operator.EQ && searchCriteria.get(searchTerm) instanceof Date) {
//			        searchTerm.setOperator(SearchTerm.Operator.ON);
//			    } 
//			    
			    sql += "\n       AND       " + searchTerm.getExpression();
			}
			
			if (!sql.contains("j.visible")) {
				sql +=  "\n       AND j.visible = :visiblebydefault \n";
			}
			
			sql +=	" ORDER BY j.lastUpdated DESC\n";
			
			String q = sql;
			//log.debug(q);
			Query query = session.createQuery(sql)
								 .setString("tenantid", TenancyHelper.getCurrentTenantId());
			
			q = StringUtils.replace(q, ":tenantid", "'" + TenancyHelper.getCurrentTenantId() + "'");
			
			if (sql.contains(":visiblebydefault") ) {
				query.setBoolean("visiblebydefault", Boolean.TRUE);
				
				q = StringUtils.replace(q, ":visiblebydefault", "1");
			}
			
		 	if (!ServiceUtils.isAdmin(username)) {
		 		query.setString("jobowner",username)
		 			.setString("none",PermissionType.NONE.name());
		 		q = StringUtils.replace(q, ":jobowner", "'" + username + "'");
		 		q = StringUtils.replace(q, ":none", "'NONE'");
		 	}
		 	
			for (SearchTerm searchTerm: searchCriteria.keySet()) 
			{
			    if (searchTerm.getOperator() == SearchTerm.Operator.BETWEEN) {
			        List<String> formattedDates = (List<String>)searchTerm.getOperator().applyWildcards(searchCriteria.get(searchTerm));
			        for(int i=0;i<formattedDates.size(); i++) {
			            query.setString(searchTerm.getSearchField()+i, formattedDates.get(i));
			            q = StringUtils.replace(q, ":" + searchTerm.getSearchField(), "'" + formattedDates.get(i) + "'");
			        }
			    }
			    else if (searchTerm.getOperator().isSetOperator()) 
				{
					query.setParameterList(searchTerm.getSearchField(), (List<Object>)searchCriteria.get(searchTerm));
					q = StringUtils.replace(q, ":" + searchTerm.getSearchField(), "('" + StringUtils.join((List<String>)searchTerm.getOperator().applyWildcards(searchCriteria.get(searchTerm)), "','") + "')" );
				}
				else 
				{
					query.setParameter(searchTerm.getSearchField(), 
							searchTerm.getOperator().applyWildcards(searchCriteria.get(searchTerm)));
					q = StringUtils.replace(q, ":" + searchTerm.getSearchField(), "'" + String.valueOf(searchTerm.getOperator().applyWildcards(searchCriteria.get(searchTerm))) + "'");
				}
			    
			}
			
			// log.debug(q);
			
			List<Job> jobs = query
					.setFirstResult(offset)
					.setMaxResults(limit)
					.list();

			session.flush();
			
			return jobs;

		}
		catch (Throwable ex)
		{
			throw new JobException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction();} catch (Exception e) {}
		}
	}

	@SuppressWarnings("unchecked")
	public static List<Job> getAll() throws JobException
	{
		try
		{
			Session session = getSession();
			session.clear();
			List<Job> jobs = (List<Job>)session.createQuery("FROM Job").list();
			
			session.flush();
			
			return jobs;
		}
		catch (HibernateException ex)
		{
			throw new JobException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction();} catch (Exception e) {}
		}
	}

	public static Integer countTotalActiveJobs() throws JobException
	{
		try
		{
			Session session = getSession();
			session.clear();
			String hql = "select count(*) "
					+ "from Job j"
					+ "where j.status in (" + JobStatusType.getActiveStatusValues() + ") "
					+ "		and j.visible = :visible";
			
			int currentJobCount = ((Long)session.createQuery(hql)
					.setBoolean("visible", true)
					.uniqueResult()).intValue();
			
			session.flush();
			
			return currentJobCount;

		}
		catch (Throwable ex)
		{
			throw new JobException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction();} catch (Exception e) {}
		}
	}
	
//	/**
//	 * Collects current active job activity stats for a single tenant using 
//	 * formula driven attributes.
//	 *  
//	 * @param tenantCode The unique code of the tenant
//	 * @return {@link TenantJobActivity} containing stats on active jobs in the tenant
//	 * @throws JobException
//	 */
//	public static SummaryTenantJobActivity getSummaryTenantJobActivity(String tenantCode)
//	throws JobException
//	{
//		try
//		{
//			HibernateUtil.beginTransaction();
//			Session session = HibernateUtil.getSession();
//			String hql = "from SummaryTenantJobActivity where id.tenantId = :tenantCode";
//			SummaryTenantJobActivity tenantJobActivity = (SummaryTenantJobActivity)(session.createQuery(hql)
//					.setString("tenantCode", tenantCode)
//					.uniqueResult());
//			
//			session.flush();
//			
//			return tenantJobActivity;
//
//		}
//		catch (Throwable ex)
//		{
//			throw new JobException(ex);
//		}
//		finally {
//			try { HibernateUtil.commitTransaction();} catch (Exception e) {}
//		}
//	}
//	
//	/**
//	 * Collects current active job activity stats for all tenants using 
//	 * formula driven attributes.
//	 *  
//	 * @return {@link List} of {@link TenantJobActivity} containing stats on active jobs for each tenant
//	 * @throws JobException
//	 */
//	@SuppressWarnings("unchecked")
//	public static List<SummaryTenantJobActivity> getSummaryTenantJobActivityForAllTenants()
//	throws JobException
//	{
//		try
//		{
//			HibernateUtil.beginTransaction();
//			Session session = HibernateUtil.getSession();
//			String hql = "from SummaryTenantJobActivity";
//			List<SummaryTenantJobActivity> tenantJobActivities = (List<SummaryTenantJobActivity>)session
//					.createQuery(hql)
//					.list();
//			
//			session.flush();
//			
//			return tenantJobActivities;
//
//		}
//		catch (Throwable ex)
//		{
//			throw new JobException(ex);
//		}
//		finally {
//			try { HibernateUtil.commitTransaction();} catch (Exception e) {}
//		}
//	}

	public static Long countActiveJobsOnSystemQueue(String system, String queueName) 
	throws JobException
	{
		try
		{
			Session session = getSession();
			session.clear();
			String hql = "select count(*) from Job where "
					+ "		queue_request = :queuerequest and "
					+ "		system = :jobsystem and "
					+ "		status in " + "(" + JobStatusType.getActiveStatusValues() + ") and "
					+ "		visible = :visible ";
			
			long currentUserJobCount = ((Long)session.createQuery(hql)
					.setBoolean("visible", true)
					.setString("queuerequest", queueName)
					.setString("jobsystem", system)
					.uniqueResult()).longValue();
			
			session.flush();
			
			return currentUserJobCount;

		}
		catch (ObjectNotFoundException ex)
		{
			throw new JobException(ex);
		}
		catch (HibernateException ex)
		{
			throw new JobException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction();} catch (Exception e) {}
		}
	}

	public static Long countActiveUserJobsOnSystemQueue(String owner, String system, String queueName) 
	throws JobException
	{
		try
		{
			Session session = getSession();
			session.clear();
			String hql = "select count(*) "
					+ "from Job where "
					+ "		queue_request = :queuerequest and "
					+ "		owner = :jobowner and "
					+ "		system = :jobsystem and "
					+ "		status in " + "(" + JobStatusType.getActiveStatusValues() + ") and "
					+ "		visible = :visible ";
			
			long jobCount = ((Long)session.createQuery(hql)
					.setBoolean("visible", true)
					.setString("jobowner",owner)
					.setString("queuerequest", queueName)
					.setString("jobsystem", system)
					.uniqueResult()).longValue();
			
			session.flush();
			
			return jobCount;

		}
		catch (ObjectNotFoundException ex)
		{
			throw new JobException(ex);
		}
		catch (HibernateException ex)
		{
			throw new JobException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction();} catch (Exception e) {}
		}
	}

	public static String getNextActiveJobUuidForUser(String username, String tenantCode)
	throws JobException
	{
		Session session = null;
		try
		{
			session = HibernateUtil.getSession();
			session.clear();
			
			String hql = "select j.uuid "
					+ "from Job j "
					+ "where j.owner = :owner "
					+ "		and tenantId = :tenantid "
					+ "		and j.status in (" + JobStatusType.getActiveStatusValues() + ") "
					+ "		and visible = :visible "
					+ "order by j.created asc ";
			
			String uuid = (String)session.createQuery(hql)
					.setBoolean("visible", true)
					.setString("owner", username)
					.setString("tenantid", tenantCode)
					.setMaxResults(1)
					.uniqueResult();
			
			return uuid;
		}
		catch (StaleObjectStateException ex) {
			return null;
		}
		catch (ObjectNotFoundException ex)
		{
			return null;
		}
		catch (Throwable ex)
		{
			throw new JobException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction();} catch (Exception e) {}
		}
	}

	/**
	 * Returns all zombie jobs which have active transfers that have not been 
	 * updated in the last 15 minutes or which have been in intermediate 
	 * statuses without transfers for over an hour.
	 * 
	 * @return {@link List<Long>} of zombie {@link Job} ids
	 */
	@SuppressWarnings("unchecked")
	public static List<BigInteger> findZombieJobs(String tenantId) 
	throws JobException
	{
		boolean excludeTenant = false;
	    if (StringUtils.isEmpty(tenantId)) {
            tenantId = "%";
        } else if (tenantId.contains("!")) {
            excludeTenant = true;
            tenantId = StringUtils.removeStart(tenantId, "!");
        }
	    
		Session session = null;
		try
		{
			session = getSession();
			session.clear();
			session.disableFilter("jobTenantFilter");
			
			String sql ="select j.id " + 
						"from  " + 
						"	( " + 
						"		select jj.id, jj.status, jj.last_updated, jj.uuid, jj.tenant_id, jj.owner, e.transfertask as task_id " + 
						"		from jobs jj left join jobevents e on jj.id = e.job_id " + 
						"		where ( " + 
						"				jj.status in ('PROCESSING_INPUTS', 'STAGING_INPUTS', 'STAGING_JOB', 'SUBMITTING_JOB', 'SUBMITTING', 'ARCHIVING')  " + 
						"				and NOW() > DATE_ADD(jj.last_updated, INTERVAL 15 minute) " + 
						"				and e.transfertask is not null " + 
						"			  ) or  " + 
						"			  ( " + 
						"			  	jj.status in ('PROCESSING_INPUTS', 'STAGING_JOB', 'SUBMITTING_JOB', 'SUBMITTING')  " + 
						"				and NOW() > DATE_ADD(jj.last_updated, INTERVAL 1 hour) " + 
						"			  ) " + 
						"	) as j " + 
						"	left join transfertasks t on j.task_id = t.id " + 
						"where (t.status = 'TRANSFERRING' or t.status = 'QUEUED') " + 
						"	and NOW() > DATE_ADD(t.last_updated, INTERVAL 15 minute) " + 
						"	and t.tenant_id :excludetenant like :tenantId " + 
						"group by j.id " + 
						"order by t.last_updated ASC ";
			
			sql = StringUtils.replace(sql, ":excludetenant", excludeTenant ? "not" : "");
			
			List<BigInteger> jobIds = session.createSQLQuery(sql)
					.setString("tenantId", tenantId)
					.list();
			
			return jobIds;
		}
		catch (StaleObjectStateException e) {
			return null;
		}
		catch (Throwable e)
		{
			throw new JobException("Failed to retrieve zombie archiving jobs", e);
		}
	}

}
