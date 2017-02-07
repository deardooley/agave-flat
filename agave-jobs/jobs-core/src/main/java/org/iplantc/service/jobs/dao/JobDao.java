/**
 * 
 */
package org.iplantc.service.jobs.dao;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.CacheMode;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.StaleObjectStateException;
import org.hibernate.StaleStateException;
import org.hibernate.UnresolvableObjectException;
import org.hibernate.transform.Transformers;
import org.hibernate.type.StandardBasicTypes;
import org.iplantc.service.apps.util.ServiceUtils;
import org.iplantc.service.common.persistence.HibernateUtil;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.common.search.AgaveResourceResultOrdering;
import org.iplantc.service.common.search.SearchTerm;
import org.iplantc.service.jobs.Settings;
import org.iplantc.service.jobs.dao.utils.JobDaoUtils;
import org.iplantc.service.jobs.exceptions.JobException;
import org.iplantc.service.jobs.exceptions.JobFinishedException;
import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.jobs.model.JobUpdateParameters;
import org.iplantc.service.jobs.model.dto.JobDTO;
import org.iplantc.service.jobs.model.enumerations.JobPhaseType;
import org.iplantc.service.jobs.model.enumerations.JobStatusType;
import org.iplantc.service.jobs.model.enumerations.PermissionType;
import org.iplantc.service.jobs.phases.schedulers.dto.JobActiveCount;
import org.iplantc.service.jobs.phases.schedulers.dto.JobArchiveInfo;
import org.iplantc.service.jobs.phases.schedulers.dto.JobMonitorInfo;
import org.iplantc.service.jobs.phases.schedulers.dto.JobQuotaInfo;
import org.iplantc.service.jobs.search.JobSearchFilter;
import org.iplantc.service.jobs.statemachine.JobFSMUtils;
import org.joda.time.DateTime;

/**
 * @author dooley
 * 
 */
public class JobDao
{
    private static final Logger log = Logger.getLogger(JobDao.class);
    
    // Limit the number of uuids allowed in a query request to avoid exceeding
    // the packet request bound configured in the database.  Issue this
    // command to discover the database's current configuration:
    //
    //     SHOW VARIABLES LIKE 'max_allowed_packet';
    // 
    private static final int UUID_QUERY_LIMIT = 1000;
	
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
	 * @deprecated 
	 * @see {@link #getByUsername(String, int, int, AgaveResourceSearchResultOrdering, SearchTerm)
	 */
	public static List<Job> getByUsername(String username, int offset, int limit) 
	throws JobException
	{
		return getByUsername(username, offset, limit, null, null);
	}
	
	/**
	 * Returns all jobs for a given username with optional search result ordering
	 * @param username
	 * @param offset
	 * @param limit
	 * @return
	 * @throws JobException
	 */
	@SuppressWarnings("unchecked")
	public static List<Job> getByUsername(String username, int offset, int limit, AgaveResourceResultOrdering order, SearchTerm orderBy) 
	throws JobException
	{
		if (order == null) {
			order = AgaveResourceResultOrdering.DESC;
		}
		
		if (orderBy == null) {
			orderBy = new JobSearchFilter().filterAttributeName("lastupdated");
		}
		
		try
		{
			Session session = getSession();
			session.clear();
			String sql = "select distinct j.* \n"
					+ "from jobs j \n"
					+ "		left join job_permissions p on j.id = p.job_id \n"
					+ "where ( \n"
					+ "			j.owner = :owner \n"
					+ "			or ( \n"
					+ "				p.username = :owner \n" 
					+ "				and p.permission <> :none \n"  
					+ "			) \n" 
					+ "		) \n"
					+ "		and j.visible = :visible \n"
					+ "		and j.tenant_id = :tenantid \n"
					+ "order by " + String.format(orderBy.getMappedField(), orderBy.getPrefix()) + " " +  order.toString() + " \n";
			
			
			String q = sql;
			q = StringUtils.replace(q, ":owner", String.format("'%s'", username));
			q = StringUtils.replace(q, ":none", String.format("'%s'", PermissionType.NONE.name()));
			q = StringUtils.replace(q, ":visible", String.format("'%d'", 1));
			q = StringUtils.replace(q, ":tenantid", String.format("'%s'", TenancyHelper.getCurrentTenantId()));
			
//			log.debug(q);
			List<Job> jobs = session.createSQLQuery(sql).addEntity(Job.class)
					.setString("owner",username)
					.setString("none",PermissionType.NONE.name())
					.setInteger("visible", new Integer(1))
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
			
			if (forceTimestamp) {
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
     throws JobException, JobFinishedException
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
	 * to update a job record; use JobManager.updateStatus() if you also require 
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
	 throws JobException, JobFinishedException
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
	    String setClause = JobDaoUtils.createSetClause(parms);
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
	                         " to " + parms.getStatus().name() + " attempted for job " +
	                         jobUuid + ".";
	            log.error(msg);
	            
	            // Release the lock on the job record.
	            try {HibernateUtil.rollbackTransaction();}
	            catch (Exception e) {
	                String msg2 = "Failure rolling back transaction on locked job record.";
	                log.error(msg2, e);
	            }
	            
	            // Throw our exception.
	            if (JobStatusType.isFinished(curStatus)) throw new JobFinishedException(msg);
	              else throw new JobException(msg);
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
            JobDaoUtils.setUpdatePlaceholders(qry, parms);
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
	
    /** Multi-tenant retrieval of jobs with the specified UUIDs.  This method
     * should only be called by schedulers that need to service job across tenants.
     * 
     * This method limits the number of UUIDs that it includes in the query to 
     * no more than the first UUID_QUERY_LIMIT entries in the input list.  This
     * is to avoid exceeding the allowed request buffer size on the server.
     * 
     * Returns a {@link List} of visible {@link Job}s with the specified uuids.
     * 
     * @param uuids the non-empty list of job uuids
     * @return the job list
     * @throws JobException on error
     */
    @SuppressWarnings("unchecked")
    public static List<Job> getSchedulerJobsByUuids(List<String> uuids) 
    throws JobException
    {
        // At least one status must be specified.
        if (uuids == null || uuids.isEmpty())
        {
            String msg = "At least one uuid must be specified for job search.";
            log.error(msg);
            throw new JobException(msg);
        }
        
        // Bound the number of uuids that we put in the sql statement so
        // that we don't exceed max_allowed_packet size.
        int limit = Math.min(uuids.size(), UUID_QUERY_LIMIT);
        
        // Create the where clause.
        String uuidClause = "uuid in (";
        for (int i = 0; i < limit; i++)
        {
            // Single quote each uuid and add comma where needed.
            uuidClause += "'" + uuids.get(i) + "'";
            if (i < limit - 1)
                uuidClause += ", ";
        }
        uuidClause += ") ";
        
        // Make the database call for visible jobs with requested uuids.
        List<Job> jobs = null;
        try
        {
            // We intentionally bypass tenant filtering by going calling the utility
            // methods directly.  This approach allows queries across tenants.  
            Session session = HibernateUtil.getSession();
            HibernateUtil.beginTransaction();
            session.clear();
            
            String sql = "select * from jobs where " + uuidClause +
                         "and visible = :visible";
            jobs = session.createSQLQuery(sql).addEntity(Job.class)
                    .setBoolean("visible", true)
                    .setCacheable(false)
                    .setCacheMode(CacheMode.REFRESH)
                    .list();

            session.flush();
            
            HibernateUtil.commitTransaction();
        }
        catch (ObjectNotFoundException e) {
            return new LinkedList<Job>();
        }
        catch (Exception e)
        {
            try {HibernateUtil.rollbackTransaction();}
            catch (Exception e1){log.error("Rollback failed.", e1);}
            
            String msg = "Unable to retrieve jobs by UUID.";
            throw new JobException(msg, e);
        }
        
        return jobs;
    }

    
    /** This method is intended to only be called from a phase scheduler.  It retrieves 
     * information pertinent to quota checking on active jobs.   
     * 
     * @return a non-null list of active job records
     * @throws JobException on error
     */
    public static List<JobActiveCount> getSchedulerActiveJobCount()
     throws JobException
    {
        // Build the query without creating so many temporary strings.
        // Since none of the returned can be null, every row is completely
        // filled in with concrete values.
        StringBuilder buf = new StringBuilder(512);
        buf.append("select ");
        buf.append("tenant_id, owner, execution_system, queue_request, sum(1) ");
        buf.append("from jobs ");
        buf.append("where status in (");
        buf.append(JobStatusType.getActiveStatusValues());
        buf.append(") ");
        buf.append("and visible = 1 ");
        buf.append("group by tenant_id, execution_system, queue_request, owner");
       
        // Result list.
        List<JobActiveCount> result = null;
        
        // Dump the complete table..
        try {
            // Get a hibernate session.
            Session session = HibernateUtil.getSession();
            session.clear();
            HibernateUtil.beginTransaction();

            // Issue the call and populate the lease object.
            Query qry = session.createSQLQuery(buf.toString());
            qry.setCacheable(false);
            qry.setCacheMode(CacheMode.IGNORE);
            @SuppressWarnings("rawtypes")
            List qryResuts = qry.list();
            
            // Commit the transaction.
            HibernateUtil.commitTransaction();
            
            // Populate the list.
            result = JobDaoUtils.populateActiveCountList(qryResuts);
        }
        catch (Exception e)
        {
            // Rollback transaction.
            try {HibernateUtil.rollbackTransaction();}
             catch (Exception e1){log.error("Rollback failed.", e1);}
            
            String msg = "Unable to retrieve active job information.";
            log.error(msg, e);
            throw new JobException(msg, e);
        }
        
        return result;
    }

	/** This method is intended to only be called from a phase scheduler.  It retrieves the uuid's 
	 * of jobs that may be ready to be scheduled along with quota information for those jobs.  
	 * The statuses represent the triggers for the phase.  The query issued by this method skips 
	 * jobs that have already been published in the job_published table for the phase.
	 * 
	 * The job quota records returned identify jobs via their UUIDs. These jobs are in one of the trigger 
	 * statuses and do not have a publish record for the specified phase.  The quota records contain 
	 * all the information needed to determine if any system, tenant, batchqueue or user quota has 
	 * been exceeded.
	 * 
	 * @param phase the phase of the calling scheduler
	 * @param statuses the trigger statuses for the phase scheduler
	 * @return a non-null list of job quota records
	 * @throws JobException on error
	 */
	public static List<JobQuotaInfo> getSchedulerJobQuotaInfo(JobPhaseType phase,
	                                                          List<JobStatusType> statuses)
	 throws JobException
	{
	    // ------------------- Check Input -------------------
	    // We need a phase.
        if (phase == null)
        {
            String msg = "Missing phase parameter in scheduler job quota retrieval.";
            log.error(msg);
            throw new JobException(msg);
        }
	    
        // At least one status must be specified.
        if (statuses == null || statuses.isEmpty())
        {
            String msg = "Missing statuses parameter in scheduler job quota retrieval.";
            log.error(msg);
            throw new JobException(msg);
        }
        
        // Create the status where clause.
        String statusClause = " j.status in (";
        for (int i = 0; i < statuses.size(); i++)
        {
            // Single quote each status and add comma where needed.
            statusClause += "'" + statuses.get(i).name() + "'";
            if (i < statuses.size() - 1)
                statusClause += ", ";
        }
        statusClause += ") ";
        
        // ------------------- Get Configured Input ----------
        // Retrieve information that may have been set in configuration files and
        // incorporate them into SQL clauses.  None of these calls throw exceptions.
        String dedicatedTenantIdClause = JobDaoUtils.getDedicatedTenantIdClause();
        String dedicatedUsersClause = JobDaoUtils.getDedicatedUsersClause();
        String dedicatedSystemIdsClause = JobDaoUtils.getDedicatedSystemIdsClause();
        
        // ------------------- Get Quota Info ----------------
        // Put together the sql statement.  The basic idea is to retrieve all job UUIDs along
        // with the quota information from the jobs table that have quotas defined.  Specifically,
        // the execution system and the batchqueue have limits defined on a per tenant basis.
        // These limits include the maximum number of jobs overall and the maximum number per user.
        //
        // To get the required four maximum bounds we join the jobs table to the systems table, 
        // which allows us to then join the executionsystems and batchqueue tables that contain the 
        // bounds.  All join fields are indexed.  The only values that can be null are the 
        // batchqueues.execution_system_id fields and jobs.visible (though never is). 
        //
        // The distinct keyword is used to remove duplicates that can be returned for the chosen
        // field selection.  We use the systems.available field to skip over jobs targeting 
        // unavailable systems.  We also avoid returning uuids for jobs that have been published
        // in this phase or that are not visible.
        // 
        // Build the query without creating so many temporary strings.
        StringBuilder buf = new StringBuilder(512);
        buf.append("select distinct ");
        buf.append("j.uuid, j.tenant_id, j.owner, j.execution_system, j.queue_request, ");
        buf.append("b.max_jobs max_queue_jobs, b.max_user_jobs max_queue_user_jobs, ");
        buf.append("e.max_system_jobs, e.max_system_jobs_per_user ");
        buf.append("from jobs j ");
        
        buf.append("left join (systems s, executionsystems e, batchqueues b) ");
        buf.append(  "on (j.execution_system = s.system_id ");
        buf.append(      "and s.id = b.execution_system_id ");
        buf.append(      "and s.id = e.id ");
        buf.append(      "and j.queue_request = b.name ");
        buf.append(      "and j.tenant_id = s.tenant_id) ");
        
        buf.append("where ");
        buf.append(statusClause);
        buf.append("and j.visible = 1 ");
        buf.append("and s.available = 1 ");
        buf.append("and s.type = 'EXECUTION' ");
        buf.append(dedicatedTenantIdClause);
        buf.append(dedicatedUsersClause);
        buf.append(dedicatedSystemIdsClause);
        buf.append("and not exists ");
        buf.append(  "(select jp.job_uuid from job_published jp ");
        buf.append(     "where jp.phase = :phase and jp.job_uuid = j.uuid)");
        
        // Result list.
        List<JobQuotaInfo> result = null;
        
        // Dump the complete table..
        try {
            // Get a hibernate session.
            Session session = HibernateUtil.getSession();
            session.clear();
            HibernateUtil.beginTransaction();

            // Issue the call and populate the lease object.
            Query qry = session.createSQLQuery(buf.toString());
            qry.setString("phase", phase.name());
            qry.setCacheable(false);
            qry.setCacheMode(CacheMode.IGNORE);
            @SuppressWarnings("rawtypes")
            List qryResuts = qry.list();
            
            // Commit the transaction.
            HibernateUtil.commitTransaction();
            
            // Populate the list.
            result = JobDaoUtils.populateQuotaInfoList(qryResuts);
        }
        catch (Exception e)
        {
            // Rollback transaction.
            try {HibernateUtil.rollbackTransaction();}
             catch (Exception e1){log.error("Rollback failed.", e1);}
            
            String msg = "Unable to retrieve job quota information.";
            log.error(msg, e);
            throw new JobException(msg, e);
        }
	    
	    return result;
	}
	
    /** This method is intended to only be called from a phase scheduler.  It retrieves the uuid's 
     * of jobs that may be ready to be scheduled along with monitoring information for those jobs.  
     * The statuses represent the triggers for the phase.  The query issued by this method skips 
     * jobs that have already been published in the job_published table for the phase.
     * 
     * The job monitor records returned identify jobs via their UUIDs. These jobs are in one of the 
     * trigger statuses and do not have a publish record for the specified phase.  The quota records 
     * contain all the information needed to determine if a job is ready to be monitored.
     * 
     * @param phase the phase of the calling scheduler
     * @param statuses the trigger statuses for the phase scheduler
     * @return a non-null list of job monitor records
     * @throws JobException on error
     */
    public static List<JobMonitorInfo> getSchedulerJobMonitorInfo(JobPhaseType phase,
                                                                  List<JobStatusType> statuses)
     throws JobException
    {
        // ------------------- Check Input -------------------
        // We need a phase.
        if (phase == null)
        {
            String msg = "Missing phase parameter in scheduler job monitor information retrieval.";
            log.error(msg);
            throw new JobException(msg);
        }
        
        // At least one status must be specified.
        if (statuses == null || statuses.isEmpty())
        {
            String msg = "Missing statuses parameter in scheduler job monitor information retrieval.";
            log.error(msg);
            throw new JobException(msg);
        }
        
        // Create the status where clause.
        String statusClause = " j.status in (";
        for (int i = 0; i < statuses.size(); i++)
        {
            // Single quote each status and add comma where needed.
            statusClause += "'" + statuses.get(i).name() + "'";
            if (i < statuses.size() - 1)
                statusClause += ", ";
        }
        statusClause += ") ";
        
        // ------------------- Get Configured Input ----------
        // Retrieve information that may have been set in configuration files and
        // incorporate them into SQL clauses.  None of these calls throw exceptions.
        String dedicatedTenantIdClause = JobDaoUtils.getDedicatedTenantIdClause();
        String dedicatedUsersClause = JobDaoUtils.getDedicatedUsersClause();
        String dedicatedSystemIdsClause = JobDaoUtils.getDedicatedSystemIdsClause();
        
        // ------------------- Get Monitor Info --------------
        // Put together the sql statement.  The basic idea is to retrieve all job UUIDs along
        // with the information from the jobs table needed to determine if the job is ready
        // to be monitored.  We avoid returning uuids for jobs that have been published
        // in this phase or that are not visible.
        // 
        // Build the query without creating so many temporary strings.
        StringBuilder buf = new StringBuilder(512);
        buf.append("select j.uuid, j.status_checks, j.last_updated ");
        buf.append("from jobs j ");
        
        buf.append("where ");
        buf.append(statusClause);
        buf.append("and j.visible = 1 ");
        buf.append("and j.local_job_id is not null ");
        buf.append(dedicatedTenantIdClause);
        buf.append(dedicatedUsersClause);
        buf.append(dedicatedSystemIdsClause);
        buf.append("and not exists ");
        buf.append(  "(select jp.job_uuid from job_published jp ");
        buf.append(     "where jp.phase = :phase and jp.job_uuid = j.uuid)");
        
        // Result list.
        List<JobMonitorInfo> result = null;
        
        // Dump the complete table..
        try {
            // Get a hibernate session.
            Session session = HibernateUtil.getSession();
            session.clear();
            HibernateUtil.beginTransaction();

            // Issue the call and populate the lease object.
            Query qry = session.createSQLQuery(buf.toString());
            qry.setString("phase", phase.name());
            qry.setCacheable(false);
            qry.setCacheMode(CacheMode.IGNORE);
            @SuppressWarnings("rawtypes")
            List qryResuts = qry.list();
            
            // Commit the transaction.
            HibernateUtil.commitTransaction();
            
            // Populate the list.
            result = JobDaoUtils.populateMonitorInfoList(qryResuts);
        }
        catch (Exception e)
        {
            // Rollback transaction.
            try {HibernateUtil.rollbackTransaction();}
             catch (Exception e1){log.error("Rollback failed.", e1);}
            
            String msg = "Unable to retrieve job quota information.";
            log.error(msg, e);
            throw new JobException(msg, e);
        }
        
        return result;
    }

    /** This method is intended to only be called from a phase scheduler.  It retrieves the uuid's 
     * of jobs that may be ready to be scheduled along with monitoring information for those jobs.  
     * The statuses represent the triggers for the phase.  The query issued by this method skips 
     * jobs that have already been published in the job_published table for the phase.
     * 
     * The job monitor records returned identify jobs via their UUIDs. These jobs are in one of the 
     * trigger statuses and do not have a publish record for the specified phase.  The quota records 
     * contain all the information needed to determine if a job is ready to be monitored.
     * 
     * @param phase the phase of the calling scheduler
     * @param statuses the trigger statuses for the phase scheduler
     * @return a non-null list of job monitor records
     * @throws JobException on error
     */
    public static List<JobArchiveInfo> getSchedulerJobArchiveInfo(JobPhaseType phase,
                                                                  List<JobStatusType> statuses)
     throws JobException
    {
        // ------------------- Check Input -------------------
        // We need a phase.
        if (phase == null)
        {
            String msg = "Missing phase parameter in scheduler job archive information retrieval.";
            log.error(msg);
            throw new JobException(msg);
        }
        
        // At least one status must be specified.
        if (statuses == null || statuses.isEmpty())
        {
            String msg = "Missing statuses parameter in scheduler job archive information retrieval.";
            log.error(msg);
            throw new JobException(msg);
        }
        
        // Create the status where clause.
        String statusClause = " j.status in (";
        for (int i = 0; i < statuses.size(); i++)
        {
            // Single quote each status and add comma where needed.
            statusClause += "'" + statuses.get(i).name() + "'";
            if (i < statuses.size() - 1)
                statusClause += ", ";
        }
        statusClause += ") ";
        
        // ------------------- Get Configured Input ----------
        // Retrieve information that may have been set in configuration files and
        // incorporate them into SQL clauses.  None of these calls throw exceptions.
        String dedicatedTenantIdClause = JobDaoUtils.getDedicatedTenantIdClause();
        String dedicatedUsersClause = JobDaoUtils.getDedicatedUsersClause();
        String dedicatedSystemIdsClause = JobDaoUtils.getDedicatedSystemIdsClause();
        
        // ------------------- Get Monitor Info --------------
        // Put together the sql statement.  The basic idea is to retrieve all job UUIDs
        // that represent jobs in an archiving trigger state.
        // 
        // Build the query without creating so many temporary strings.
        StringBuilder buf = new StringBuilder(512);
        buf.append("select j.uuid ");
        buf.append("from jobs j ");
        
        buf.append("where ");
        buf.append(statusClause);
        buf.append("and j.visible = 1 ");
        buf.append(dedicatedTenantIdClause);
        buf.append(dedicatedUsersClause);
        buf.append(dedicatedSystemIdsClause);
        buf.append("and not exists ");
        buf.append(  "(select jp.job_uuid from job_published jp ");
        buf.append(     "where jp.phase = :phase and jp.job_uuid = j.uuid)");
        
        // Result list.
        List<JobArchiveInfo> result = null;
        
        // Dump the complete table..
        try {
            // Get a hibernate session.
            Session session = HibernateUtil.getSession();
            session.clear();
            HibernateUtil.beginTransaction();

            // Issue the call and populate the lease object.
            Query qry = session.createSQLQuery(buf.toString());
            qry.setString("phase", phase.name());
            qry.setCacheable(false);
            qry.setCacheMode(CacheMode.IGNORE);
            @SuppressWarnings("rawtypes")
            List qryResuts = qry.list();
            
            // Commit the transaction.
            HibernateUtil.commitTransaction();
            
            // Populate the list.
            result = JobDaoUtils.populateArchiveInfoList(qryResuts);
        }
        catch (Exception e)
        {
            // Rollback transaction.
            try {HibernateUtil.rollbackTransaction();}
             catch (Exception e1){log.error("Rollback failed.", e1);}
            
            String msg = "Unable to retrieve job quota information.";
            log.error(msg, e);
            throw new JobException(msg, e);
        }
        
        return result;
    }

    /** Query the database for jobs that have not progressed recently.
     * 
     * @return the list of zombie jobs.
     * @throws JobException on error
     */
    @SuppressWarnings("unchecked")
    public static List<Job> getSchedulerZombieJobs()
     throws JobException
    {
        // Retrieve information that may have been set in configuration files and
        // incorporate them into SQL clauses.  This call does not throw exceptions.
        String dedicatedTenantIdClause = JobDaoUtils.getDedicatedTenantIdClause();
        
        // Build the query without creating so many temporary strings.
        StringBuilder buf = new StringBuilder(512);
        buf.append("select j.* ");
        buf.append("from jobs j ");
        buf.append("left join jobevents e on j.id = e.job_id ");
        buf.append("where ");
        buf.append("((");
        buf.append("j.status in ('PROCESSING_INPUTS', 'STAGING_INPUTS', 'STAGING_JOB', 'SUBMITTING', 'ARCHIVING') ");
        buf.append("and NOW() > DATE_ADD(j.last_updated, INTERVAL 15 minute) ");
        buf.append("and e.transfertask is not null ");
        buf.append(") or (");
        buf.append("j.status in ('PROCESSING_INPUTS', 'STAGING_JOB', 'SUBMITTING') ");
        buf.append("and NOW() > DATE_ADD(j.last_updated, INTERVAL 1 hour) ");
        buf.append(")) ");
        buf.append(dedicatedTenantIdClause);

        // Result list.
        List<Job> jobList = null;
        
        // Dump the complete table..
        try {
            // Get a hibernate session.
            Session session = HibernateUtil.getSession();
            session.clear();
            HibernateUtil.beginTransaction();

            // Issue the call and populate the lease object.
            // Implicit tenant filtering should be DISABLED
            // since we got our session from HibernateUtil.
            Query qry = session.createSQLQuery(buf.toString()).addEntity(Job.class);
            jobList = (List<Job>) qry.list();
            
            // Commit the transaction.
            HibernateUtil.commitTransaction();
        }
        catch (Exception e)
        {
            // Rollback transaction.
            try {HibernateUtil.rollbackTransaction();}
             catch (Exception e1){log.error("Rollback failed.", e1);}
            
            String msg = "Unable to retrieve job quota information.";
            log.error(msg, e);
            throw new JobException(msg, e);
        }
        
        return jobList;
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
	public static List<JobDTO> findMatching(String username,
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
	public static List<JobDTO> findMatching(String username,
			Map<SearchTerm, Object> searchCriteria,
			int offset, int limit) throws JobException
	{
		return findMatching(username, searchCriteria, offset, limit, null, null);
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
	public static List<JobDTO> findMatching(String username,
			Map<SearchTerm, Object> searchCriteria,
			int offset, int limit, AgaveResourceResultOrdering order, SearchTerm orderBy) throws JobException
	{
		if (order == null) {
			order = AgaveResourceResultOrdering.ASCENDING;
		}
		
		if (orderBy == null) {
			orderBy = new JobSearchFilter().filterAttributeName("lastupdated");
		}
		
		try
		{
			Session session = getSession();
			session.clear();
			String sql = "SELECT j.archive_output, \n" + 
					"       j.archive_output, \n" + 
					"       j.archive_path, \n" + 
					"       a.system_id as archive_system, \n" + 
					"       j.charge, \n" + 
					"       j.created, \n" + 
					"       j.end_time, \n" + 
					"       j.error_message, \n" + 
					"       j.execution_system, \n" + 
					"       j.id, \n" + 
					"       j.inputs, \n" + 
					"       j.internal_username, \n" + 
					"       j.last_updated, \n" + 
					"       j.local_job_id, \n" + 
					"       j.memory_request, \n" + 
					"       j.name, \n" + 
					"       j.node_count, \n" + 
					"       j.owner, \n" + 
					"       j.parameters, \n" + 
					"       j.processor_count, \n" + 
					"       j.queue_request, \n" + 
					"       j.requested_time, \n" + 
					"       j.retries, \n" + 
					"       j.scheduler_job_id, \n" + 
					"       j.software_name, \n" + 
					"       j.start_time, \n" + 
					"       j.status, \n" + 
					"       j.status_checks, \n" + 
					"       j.submit_time, \n" + 
					"       j.tenant_id, \n" + 
					"       j.update_token, \n" + 
					"       j.uuid, \n" + 
					"       j.visible, \n" + 
					"       j.work_path \n" + 
					" FROM jobs j \n" +
					" LEFT OUTER JOIN systems a ON j.archive_system = a.id \n";
			if (!ServiceUtils.isAdmin(username)) {
				
				sql += " WHERE ( \n" +
				    "       j.owner = :jobowner OR \n" +
					"       j.id in ( \n" + 
				    "               SELECT pm.job_id FROM job_permissions as pm \n" +
					"               WHERE pm.username = :jobowner AND pm.permission <> :none \n" +
					"              ) \n" +
					"      ) AND \n";
			} else {
				sql += " WHERE ";
			}
			
			sql +=  "        j.tenant_id = :tenantid "; 
			
			for (SearchTerm searchTerm: searchCriteria.keySet()) 
			{
				// we have to format
//				if (searchTerm.getSafeSearchField().equalsIgnoreCase("runtime") || 
//						searchTerm.getSafeSearchField().equalsIgnoreCase("walltime")) {
//					sql += "\n       AND       " + 
//						StringUtils.replace(searchTerm.getExpression(), "__MYSQL_DATE_FORMAT__", "%Y-%m-%d %H:%i:%s.0");
//				}
//				else {
					sql += "\n       AND       " + searchTerm.getExpression();
//				}
			}
			
			if (!sql.contains("j.visible")) {
				sql +=  "\n       AND j.visible = :visiblebydefault ";
			}
			
			sql +=	"\n ORDER BY " + String.format(orderBy.getMappedField(), orderBy.getPrefix()) + " " + order.toString() + " \n";
			
			String q = sql;
			//log.debug(q);
			SQLQuery query = session.createSQLQuery(sql);
			query.addScalar("id", StandardBasicTypes.LONG)
				.addScalar("charge", StandardBasicTypes.DOUBLE)
				.addScalar("memory_request", StandardBasicTypes.DOUBLE)
				.addScalar("node_count", StandardBasicTypes.INTEGER)
				.addScalar("processor_count", StandardBasicTypes.INTEGER)
				.addScalar("retries", StandardBasicTypes.INTEGER)
				.addScalar("status_checks", StandardBasicTypes.INTEGER)
				.addScalar("archive_output", StandardBasicTypes.BOOLEAN)
				.addScalar("visible", StandardBasicTypes.BOOLEAN)
				.addScalar("archive_path", StandardBasicTypes.STRING)
				.addScalar("archive_system", StandardBasicTypes.STRING)
				.addScalar("created", StandardBasicTypes.TIMESTAMP)
				.addScalar("end_time", StandardBasicTypes.TIMESTAMP)
				.addScalar("error_message", StandardBasicTypes.STRING)
				.addScalar("execution_system", StandardBasicTypes.STRING)
				.addScalar("inputs", StandardBasicTypes.STRING)
				.addScalar("internal_username", StandardBasicTypes.STRING)
				.addScalar("last_updated", StandardBasicTypes.TIMESTAMP)
				.addScalar("local_job_id", StandardBasicTypes.STRING)
				.addScalar("name", StandardBasicTypes.STRING)
				.addScalar("owner", StandardBasicTypes.STRING)
				.addScalar("parameters", StandardBasicTypes.STRING)
				.addScalar("queue_request", StandardBasicTypes.STRING)
				.addScalar("requested_time", StandardBasicTypes.STRING)
				.addScalar("scheduler_job_id", StandardBasicTypes.STRING)
				.addScalar("software_name", StandardBasicTypes.STRING)
				.addScalar("start_time", StandardBasicTypes.TIMESTAMP)
				.addScalar("status", StandardBasicTypes.STRING)
				.addScalar("submit_time", StandardBasicTypes.TIMESTAMP)
				.addScalar("tenant_id", StandardBasicTypes.STRING)
				.addScalar("update_token", StandardBasicTypes.STRING)
				.addScalar("uuid", StandardBasicTypes.STRING)
				.addScalar("work_path", StandardBasicTypes.STRING)
				.setResultTransformer(Transformers.aliasToBean(JobDTO.class));
			
            query.setString("tenantid", TenancyHelper.getCurrentTenantId());
			
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
			    if (searchTerm.getOperator() == SearchTerm.Operator.BETWEEN || searchTerm.getOperator() == SearchTerm.Operator.ON) {
			        List<String> formattedDates = (List<String>)searchTerm.getOperator().applyWildcards(searchCriteria.get(searchTerm));
			        for(int i=0;i<formattedDates.size(); i++) {
			            query.setString(searchTerm.getSearchField()+i, formattedDates.get(i));
			            q = StringUtils.replace(q, ":" + searchTerm.getSearchField() + i, "'" + formattedDates.get(i) + "'");
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
			
			log.debug(q);
			
			List<JobDTO> jobs = query
					.setFirstResult(offset)
					.setMaxResults(limit)
					.setCacheable(false)
					.setCacheMode(CacheMode.IGNORE)
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
	

	public static int getJobWallTime(String uuid) throws JobException {
		try
		{
			Session session = getSession();
			session.clear();
			String sql = "SELECT abs(unix_timestamp(j.end_time) - unix_timestamp(j.created)) as walltime from jobs j where j.uuid = :uuid";
			
			return ((BigInteger)session.createSQLQuery(sql)
					.addScalar("walltime")
					.setString("uuid", uuid)
					.uniqueResult()).intValue();
			
		}
		catch (Throwable ex)
		{
			throw new JobException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction();} catch (Exception e) {}
		}
	}
	
	public static int getJobRunTime(String uuid) throws JobException {
		try
		{
			Session session = getSession();
			session.clear();
			String sql = "SELECT abs(unix_timestamp(j.end_time) - unix_timestamp(j.start_time)) as runtime from jobs j where j.uuid = :uuid";
			
			return ((BigInteger)session.createSQLQuery(sql)
					.addScalar("runtime")
					.setString("uuid", uuid)
					.uniqueResult()).intValue();
			
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

}
