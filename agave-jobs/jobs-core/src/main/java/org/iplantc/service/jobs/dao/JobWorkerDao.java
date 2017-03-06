package org.iplantc.service.jobs.dao;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
/** Accessor class for job_workers table.
 */
import org.apache.log4j.Logger;
import org.hibernate.CacheMode;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.exception.ConstraintViolationException;
import org.iplantc.service.common.persistence.HibernateUtil;
import org.iplantc.service.jobs.exceptions.JobException;
import org.iplantc.service.jobs.exceptions.JobWorkerException;
import org.iplantc.service.jobs.model.JobClaim;

public final class JobWorkerDao
{
    /* ********************************************************************** */
    /*                               Constants                                */
    /* ********************************************************************** */
    // Tracing.
    private static final Logger _log = Logger.getLogger(JobWorkerDao.class);

    /* ********************************************************************** */
    /*                                 Enums                                  */
    /* ********************************************************************** */
    // Used to consolidate code that only varies by lookup key.
    private enum Selector {
        JOB_UUID("job_uuid"), WORKER_UUID("worker_uuid");
        
        private String label;
        private Selector(String s){label = s;}
        @Override
        public String toString(){return label;}
    };
    
    // Used to consolidate code that only varies by non-unique lookup key.
    private enum NonUniqueSelector {
        SCHEDULER_NAME("scheduler_name"), HOST("host"), CONTAINER_ID("container_id"), ALL("all");
        
        private String label;
        private NonUniqueSelector(String s){label = s;}
        @Override
        public String toString(){return label;}
    };
    
    /* ********************************************************************** */
    /*                             Public Methods                             */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* claimJob:                                                              */
    /* ---------------------------------------------------------------------- */
    /** A convenience method that uncurries the claim argument and calls the 
     * real method.
     * 
     * @param claim the curried parameters for claim creation
     * @throws JobException any error other than constraint violations
     * @throws JobWorkerException an SQL constraint violation on job or worker uuid
     */
    public static void claimJob(JobClaim claim)
     throws JobException, JobWorkerException
    {
        claimJob(claim.getJobUuid(), claim.getWorkerUuid(), claim.getSchedulerName(),
                 claim.getHost(), claim.getContainerId());
    }
    
    /* ---------------------------------------------------------------------- */
    /* claimJob:                                                              */
    /* ---------------------------------------------------------------------- */
    /** Worker threads call this method to claim a job.  Workers can only start
     * processing a job if they can insert a registration record into the 
     * job_workers table.  Records have unique job UUIDs and unique worker UUIDs.
     * If either of these constraints is violated a JobWorkerException is thrown.
     * All other errors cause a JobException to be thrown.
     * 
     * The host and containerId information advertise where the worker is running
     * so that external processes can shutdown the container if the worker becomes
     * unresponsive to interrupts.
     * 
     * @param jobUuid a job uuid not currently registered by any worker thread
     * @param workerUuid a worker uuid that is does not currently claim any job
     * @param host the host on which the worker thread runs
     * @param containerId the container in which the worker thread runs
     * @throws JobException any error other than constraint violations
     * @throws JobWorkerException an SQL constraint violation on job or worker uuid
     */
    public static void claimJob(String jobUuid, String workerUuid, String schedulerName,
                                String host, String containerId)
     throws JobException, JobWorkerException
    {
        // ------------------------- Check Input -------------------------
        // No parameters can be null or empty.
        if (StringUtils.isBlank(jobUuid)) {
            String msg = "Null or empty job UUID received.";
            _log.error(msg);
            throw new JobException(msg);
        }
        if (StringUtils.isBlank(workerUuid)) {
            String msg = "Null or empty worker UUID received.";
            _log.error(msg);
            throw new JobException(msg);
        }
        if (StringUtils.isBlank(schedulerName)) {
            String msg = "Null or empty worker schedulerName received.";
            _log.error(msg);
            throw new JobException(msg);
        }
        if (StringUtils.isBlank(host)) {
            String msg = "Null or empty host received.";
            _log.error(msg);
            throw new JobException(msg);
        }
        if (StringUtils.isBlank(containerId)) {
            String msg = "Null container ID received.";
            _log.error(msg);
            throw new JobException(msg);
        }
        
        // ------------------------- Call SQL ----------------------------
        // Claim the job.
        try {
            // Get a hibernate session.
            Session session = HibernateUtil.getSession();
            session.clear();
            HibernateUtil.beginTransaction();

            // Create the insert command.
            String sql = "insert into job_workers " +
                         "(job_uuid, worker_uuid, scheduler_name, host, container_id) " +
                         "values " +
                         "(:job_uuid, :worker_uuid, :scheduler_name, :host, :container_id) ";
            
            // Fill in the placeholders.           
            Query qry = session.createSQLQuery(sql);
            qry.setString("job_uuid", jobUuid);
            qry.setString("worker_uuid", workerUuid);
            qry.setString("scheduler_name", schedulerName);
            qry.setString("host", host);
            qry.setString("container_id", containerId);
            qry.setCacheable(false);
            qry.setCacheMode(CacheMode.IGNORE);
            
            // Issue the call.
            qry.executeUpdate();
            
            // Commit the transaction.
            HibernateUtil.commitTransaction();
        }
        catch (ConstraintViolationException e) {
            String msg = "Insertion of duplicate job or worker id failure.";
            _log.error(msg, e);
            throw new JobWorkerException(msg, e);
        }        
        catch (Exception e) {
            // Rollback transaction.
            try {HibernateUtil.rollbackTransaction();}
             catch (Exception e1){_log.error("Rollback failed.", e1);}
            
            String msg = "Unable to claim job " + jobUuid + " for worker " + workerUuid + ".";
            _log.error(msg, e);
            throw new JobException(msg, e);
        }
    }

    /* ---------------------------------------------------------------------- */
    /* unclaimJobByJobUuid:                                                   */
    /* ---------------------------------------------------------------------- */
    /** Remove the claim for the specified job if one exists.  
     * 
     * @param jobUuid the job uuid whose claim will be removed
     * @return the number of claims removed (0 or 1)
     * @throws JobException on error
     */
    public static int unclaimJobByJobUuid(String jobUuid)
     throws JobException
    {
        return unclaimJob(Selector.JOB_UUID, jobUuid);
    }
    
    /* ---------------------------------------------------------------------- */
    /* unclaimJobByWorkerUuid:                                                */
    /* ---------------------------------------------------------------------- */
    /** Remove a job claim for the specified worker if one exists.  
     * 
     * @param workerUuid the worker uuid whose claim will be removed
     * @return the number of claims removed (0 or 1)
     * @throws JobException on error
     */
    public static int unclaimJobByWorkerUuid(String workerUuid)
     throws JobException
    {
        return unclaimJob(Selector.WORKER_UUID, workerUuid);    
    }
    
    /* ---------------------------------------------------------------------- */
    /* unclaimJobsForScheduler:                                               */
    /* ---------------------------------------------------------------------- */
    /** Remove the job claims for the specified scheduler.  
     * 
     * @param schedulerName the name of the scheduler whose claims will be removed
     * @return the number of claims removed
     * @throws JobException on error
     */
    public static int unclaimJobsForScheduler(String schedulerName)
     throws JobException
    {
        return unclaimJobs(NonUniqueSelector.SCHEDULER_NAME, schedulerName);    
    }
    
    /* ---------------------------------------------------------------------- */
    /* unclaimJobsForHost:                                                    */
    /* ---------------------------------------------------------------------- */
    /** Remove the job claims for the specified host.  
     * 
     * @param host the host whose claims will be removed
     * @return the number of claims removed
     * @throws JobException on error
     */
    public static int unclaimJobsForHost(String host)
     throws JobException
    {
        return unclaimJobs(NonUniqueSelector.HOST, host);    
    }
    
    /* ---------------------------------------------------------------------- */
    /* unclaimJobsForContainer:                                               */
    /* ---------------------------------------------------------------------- */
    /** Remove the job claims for the specified container.  
     * 
     * @param containerId the id of the container whose claims will be removed
     * @return the number of claims removed
     * @throws JobException on error
     */
    public static int unclaimJobsForContainer(String containerId)
     throws JobException
    {
        return unclaimJobs(NonUniqueSelector.CONTAINER_ID, containerId);    
    }
    
    /* ---------------------------------------------------------------------- */
    /* getJobClaimByJobUuid:                                                  */
    /* ---------------------------------------------------------------------- */
    /** Retrieve a job claim if it exists.
     * 
     * @param jobUuid the job uuid of the claim to be returned
     * @return the claim or null if no claim is found
     * @throws JobException on error
     */
    public static JobClaim getJobClaimByJobUuid(String jobUuid)
     throws JobException
    {
        return getJobClaim(Selector.JOB_UUID, jobUuid);    
    }
    
    /* ---------------------------------------------------------------------- */
    /* getJobClaimByWorkerUuid:                                               */
    /* ---------------------------------------------------------------------- */
    /** Retrieve a job claim if it exists.
     * 
     * @param workerUuid the worker uuid of the claim to be returned
     * @return the claim or null if no claim is found
     * @throws JobException on error
     */
    public static JobClaim getJobClaimByWorkerUuid(String workerUuid)
     throws JobException
    {
        return getJobClaim(Selector.WORKER_UUID, workerUuid);    
    }
    
    /* ---------------------------------------------------------------------- */
    /* getJobClaims:                                                          */
    /* ---------------------------------------------------------------------- */
    /** Dump the current state of all claims.  This is an administrative command 
     * that is useful in testing but will probably never be used in production.
     *  
     * @return the list of all job claims, which might be empty
     * @throws JobException on error
     */
    public static List<JobClaim> getJobClaims() 
     throws JobException
    {
        return getJobClaims(NonUniqueSelector.ALL, "all");
    }
        
    /* ---------------------------------------------------------------------- */
    /* getJobClaimsForScheduler:                                              */
    /* ---------------------------------------------------------------------- */
    /** Get all job claims for specific scheduler.
     *  
     * @param schedulerName the name of the scheduler of interest
     * @return the list of all job claims, which might be empty
     * @throws JobException on error
     */
    public static List<JobClaim> getJobClaimsForScheduler(String schedulerName) 
     throws JobException
    {
        return getJobClaims(NonUniqueSelector.SCHEDULER_NAME, schedulerName);
    }
        
    /* ---------------------------------------------------------------------- */
    /* getJobClaimsForHost:                                                   */
    /* ---------------------------------------------------------------------- */
    /** Get all job claims for specific host.
     *  
     * @param host the name of the host of interest
     * @return the list of all job claims, which might be empty
     * @throws JobException on error
     */
    public static List<JobClaim> getJobClaimsForHost(String host) 
     throws JobException
    {
        return getJobClaims(NonUniqueSelector.HOST, host);
    }
        
    /* ---------------------------------------------------------------------- */
    /* getJobClaimsForContainer:                                              */
    /* ---------------------------------------------------------------------- */
    /** Get all job claims for a specific containerr.
     *  
     * @param container the id of the container of interest
     * @return the list of all job claims, which might be empty
     * @throws JobException on error
     */
    public static List<JobClaim> getJobClaimsForContainer(String containerId) 
     throws JobException
    {
        return getJobClaims(NonUniqueSelector.CONTAINER_ID, containerId);
    }
        
    /* ---------------------------------------------------------------------- */
    /* clearClaims:                                                           */
    /* ---------------------------------------------------------------------- */
    /** This is an administrative command useful in testing but not likely to
     * be used in production.  This command deletes all rows from the job_workers 
     * table.
     * 
     * @return number of rows affected.
     */
    public static int clearClaims()
     throws JobException
    {
        // Return value.
        int rows = 0;
        
        // Dump the complete table..
        try {
            // Get a hibernate session.
            Session session = HibernateUtil.getSession();
            session.clear();
            HibernateUtil.beginTransaction();

            // Release the lease.
            String sql = "delete from job_workers";
            Query qry = session.createSQLQuery(sql);
            qry.setCacheable(false);
            qry.setCacheMode(CacheMode.IGNORE);
            rows = qry.executeUpdate();
            
            // Commit the transaction.
            HibernateUtil.commitTransaction();
        }
        catch (Exception e)
        {
            // Rollback transaction.
            try {HibernateUtil.rollbackTransaction();}
             catch (Exception e1){_log.error("Rollback failed.", e1);}
            
            String msg = "Unable to clear all worker claims.";
            _log.error(msg, e);
            throw new JobException(msg, e);
        }
        
        return rows;
    }
    
    /* ---------------------------------------------------------------------- */
    /* lockJobClaim:                                                          */
    /* ---------------------------------------------------------------------- */
    /** This method should only be called during rollback processing where locks
     * have already been acquired on a job record in the jobs table and we need
     * to lock the job's record in the job_workers table.  
     * 
     * The caller is expected to have already begun a transaction on the session
     * assigned in thread local storage.  This method uses that session without
     * committing or rolling back its transaction--it just issues a SELECT FOR 
     * UPDATE call to retrieve information and lock the record.
     * 
     * Retrieve the claim for the specified job if one exists.   
     * 
     * @param jobUuid the UUID of the job being queried
     * @return the job claim or null if it doesn't exist
     * @throws JobException on error
     */
    public static JobClaim lockJobClaim(String jobUuid)
     throws JobException
    {
        // ------------------------- Check Input -------------------------
        // Null or empty check.
        if (StringUtils.isBlank(jobUuid)) {
            String msg = "Null or empty job UUID received.";
            _log.error(msg);
            throw new JobException(msg);
        }
        
        // ------------------------- Call SQL ----------------------------
        JobClaim jobClaim = null;
        try
        {
            // Continue using existing transaction in thread local session.
            Session session = HibernateUtil.getSession();

            // Issue the SELECT FOR UPDATE call.
            // NOTE: Any changes to the job_workers table requires maintenance
            //       here and in the populate routine below.
            String sql = "select job_uuid, worker_uuid, scheduler_name, host, container_id " +
                         "from job_workers " +
                         "where job_uuid = :jobUuid FOR UPDATE";
            
            // Fill in the placeholders.           
            Query qry = session.createSQLQuery(sql);
            qry.setString("jobUuid", jobUuid);
            qry.setCacheable(false);
            qry.setCacheMode(CacheMode.IGNORE);
            
            // Issue the call to get the claim.
            Object obj = qry.uniqueResult();
            
            // Populate the output object.
            jobClaim = populateJobClaim(obj);
        }
        catch (Exception e)
        {
            // Log error, throw exception, but let caller clean up transaction.
            String msg = "Unable to query job claim for job " + jobUuid + "."; 
            _log.error(msg);
            throw new JobException(msg, e);
        }
        
        return jobClaim;
    }
    
    /* ********************************************************************** */
    /*                            Private Methods                             */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* unclaimJob:                                                            */
    /* ---------------------------------------------------------------------- */
    /** Remove the claim for the specified job if one exists.  
     * 
     * @param selector the search criterion
     * @param selectorValue the value of the search criterion
     * @return the number of claims removed (0 or 1)
     * @throws JobException on error
     */
    private static int unclaimJob(Selector selector, String selectorValue)
     throws JobException
    {
        // ------------------------- Check Input -------------------------
        // No parameters can be null or empty.
        if (StringUtils.isBlank(selectorValue)) {
            String msg = "Null or empty value received.";
            _log.error(msg);
            throw new JobException(msg);
        }
        
        // ------------------------- Call SQL ----------------------------
        // Remove the job claim.
        int rows = 0;
        try {
            // Get a hibernate session.
            Session session = HibernateUtil.getSession();
            session.clear();
            HibernateUtil.beginTransaction();

            // Create the insert command.
            String sql = "delete from job_workers " +
                         "where " + selector.toString() + " = :selectorValue";
            
            // Fill in the placeholders.           
            Query qry = session.createSQLQuery(sql);
            qry.setString("selectorValue", selectorValue);
            qry.setCacheable(false);
            qry.setCacheMode(CacheMode.IGNORE);
            
            // Issue the call.
            rows = qry.executeUpdate();
            
            // Commit the transaction.
            HibernateUtil.commitTransaction();
        }
        catch (Exception e) {
            // Rollback transaction.
            try {HibernateUtil.rollbackTransaction();}
             catch (Exception e1){_log.error("Rollback failed.", e1);}
            
            String msg = "Unable to unclaim job using " + 
                         ((selector == Selector.JOB_UUID) ? "job " : " worker ") + 
                         "uuid " + selectorValue + ".";
            _log.error(msg, e);
            throw new JobException(msg, e);
        }
        
        return rows;
    }
    
    /* ---------------------------------------------------------------------- */
    /* unclaimJobs:                                                           */
    /* ---------------------------------------------------------------------- */
    /** Remove the claim for the selected jobs.  
     * 
     * @param selector the search criterion
     * @param selectorValue the value of the search criterion
     * @return the number of claims removed (0 or 1)
     * @throws JobException on error
     */
    private static int unclaimJobs(NonUniqueSelector selector, String selectorValue)
     throws JobException
    {
        // ------------------------- Check Input -------------------------
        // Not currently used, but included for completeness.
        if (selector == NonUniqueSelector.ALL) return clearClaims();
        
        // No parameters can be null or empty.
        if (StringUtils.isBlank(selectorValue)) {
            String msg = "Null or empty value received.";
            _log.error(msg);
            throw new JobException(msg);
        }
        
        // ------------------------- Call SQL ----------------------------
        // Remove the job claim.
        int rows = 0;
        try {
            // Get a hibernate session.
            Session session = HibernateUtil.getSession();
            session.clear();
            HibernateUtil.beginTransaction();

            // Create the insert command.
            String sql = "delete from job_workers " +
                         "where " + selector.toString() + " = :selectorValue";
            
            // Fill in the placeholders.           
            Query qry = session.createSQLQuery(sql);
            qry.setString("selectorValue", selectorValue);
            qry.setCacheable(false);
            qry.setCacheMode(CacheMode.IGNORE);
            
            // Issue the call.
            rows = qry.executeUpdate();
            
            // Commit the transaction.
            HibernateUtil.commitTransaction();
        }
        catch (Exception e) {
            // Rollback transaction.
            try {HibernateUtil.rollbackTransaction();}
             catch (Exception e1){_log.error("Rollback failed.", e1);}
            
            String msg = "Unable to unclaim jobs using " + selector.name() + 
                         " value " + selectorValue + ".";
            _log.error(msg, e);
            throw new JobException(msg, e);
        }
        
        return rows;
    }
    
    /* ---------------------------------------------------------------------- */
    /* getJobClaim:                                                           */
    /* ---------------------------------------------------------------------- */
    /** Retrieve the claim for the specified job or worker if one exists.  The
     * selector allows us to use the same code to search by job or worker UUID.  
     * 
     * @param selector the search criterion
     * @param selectorValue the value of the search criterion
     * @return the job claim or null if it doesn't exist
     * @throws JobException on error
     */
    private static JobClaim getJobClaim(Selector selector, String selectorValue)
     throws JobException
    {
        // ------------------------- Check Input -------------------------
        // Null or empty check.
        if (StringUtils.isBlank(selectorValue)) {
            String msg = "Null or empty value received.";
            _log.error(msg);
            throw new JobException(msg);
        }
        
        // ------------------------- Call SQL ----------------------------
        JobClaim jobClaim = null;
        try
        {
            // Begin new transaction.
            Session session = HibernateUtil.getSession();
            session.clear();
            HibernateUtil.beginTransaction();

            // Retrieve the claim record using the selector information.
            // NOTE: Any changes to the job_queues table requires maintenance
            //       here and in the populate routine below.
            String sql = "select job_uuid, worker_uuid, scheduler_name, host, container_id " +
                         "from job_workers " +
                         "where " + selector.toString() + " = :selectorValue";
            
            // Fill in the placeholders.           
            Query qry = session.createSQLQuery(sql);
            qry.setString("selectorValue", selectorValue);
            qry.setCacheable(false);
            qry.setCacheMode(CacheMode.IGNORE);
            
            // Issue the call to get the claim.
            Object obj = qry.uniqueResult();
            
            // Commit the transaction.
            HibernateUtil.commitTransaction();
            
            // Populate the output object.
            jobClaim = populateJobClaim(obj);
        }
        catch (Exception e)
        {
            // Rollback transaction.
            try {HibernateUtil.rollbackTransaction();}
             catch (Exception e1){_log.error("Rollback failed.", e1);}
            
            String msg = "Unable to query job claim using " + 
                    ((selector == Selector.JOB_UUID) ? "job " : " worker ") + 
                    "uuid " + selectorValue + ".";
            _log.error(msg);
            throw new JobException(msg, e);
        }
        
        return jobClaim;
    }
    
    /* ---------------------------------------------------------------------- */
    /* getJobClaims:                                                          */
    /* ---------------------------------------------------------------------- */
    /** This is an administrative command that is useful in testing and in
     * recovery.  The non-unique selector filters the results unless the ALL
     * selector is used, which returns the all table records.
     *  
     * @param selector the non-unique filter
     * @param selectorValue the non-null value by which selection is filtered
     * @return the list of all job claims, which might be empty
     * @throws JobException on error
     */
    private static List<JobClaim> getJobClaims(NonUniqueSelector selector, String selectorValue) 
     throws JobException
    {
        // ------------------------- Check Input -------------------------
        // Null or empty check.
        if (StringUtils.isBlank(selectorValue)) {
            String msg = "Null or empty value received.";
            _log.error(msg);
            throw new JobException(msg);
        }
        
        // Assign where clause when we are not dumping the whole table.
        String whereClause;
        if (selector != NonUniqueSelector.ALL)
            whereClause = " where " + selector.toString() + " = :selectorValue";
         else whereClause = "";
        
        // Initialize result list to fixed number of schedulers.
        List<JobClaim> list = null;
        
        // ------------------------- Call SQL ----------------------------
        // Dump the complete table or some part of it.
        try {
            // Get a hibernate session.
            Session session = HibernateUtil.getSession();
            session.clear();
            HibernateUtil.beginTransaction();

            // Dump all rows in the table.
            String sql = "select job_uuid, worker_uuid, scheduler_name, host, container_id from job_workers" +
                         whereClause;
            
            // Issue the call and populate the lease object.
            Query qry = session.createSQLQuery(sql);
            if (!whereClause.isEmpty()) qry.setString("selectorValue", selectorValue);
            qry.setCacheable(false);
            qry.setCacheMode(CacheMode.IGNORE);
            @SuppressWarnings("rawtypes")
            List qryResuts = qry.list();
            
            // Commit the transaction.
            HibernateUtil.commitTransaction();
            
            // Populate the list.
            list = populateList(qryResuts);
        }
        catch (Exception e)
        {
            // Rollback transaction.
            try {HibernateUtil.rollbackTransaction();}
             catch (Exception e1){_log.error("Rollback failed.", e1);}
            
            String msg = "Unable to retrieve leases.";
            _log.error(msg, e);
            throw new JobException(msg, e);
        }
        
        return list;
    }
    
    /* ---------------------------------------------------------------------- */
    /* populateList:                                                          */
    /* ---------------------------------------------------------------------- */
    /** Return a list claims given the raw sql output.
     * 
     * @param qryResults sql rows from the job_workers table
     * @return a list job claim objects
     */
    @SuppressWarnings("rawtypes")
    private static List<JobClaim> populateList(List qryResults)
    {
       // There should always be the initialized rows for each phase.
        // We should always get a record back.
        if (qryResults == null || qryResults.isEmpty())
            return new LinkedList<JobClaim>();
       
        // Size the array list to the input.
        ArrayList<JobClaim> outList = new ArrayList<>(qryResults.size());
        
        // Marshal each row from the query results.
        for (Object rowobj : qryResults)
        {
            // Populate a new claim object.
            JobClaim claim = populateJobClaim(rowobj);
            
            // Save the claim in the output list.
            outList.add(claim);
        }
        
        return outList;
    }
    
    /* ---------------------------------------------------------------------- */
    /* populateJobClaim:                                                      */
    /* ---------------------------------------------------------------------- */
    /** Populate a new JobClaim object with a record retrieved from the 
     * job_interrupts table.
     * 
     * NOTE: This method must be manually maintained whenever the job_interrupts
     *       table schema changes.  
     * 
     * @param obj a record from the job_workers table
     * @return a new model object
     */
    private static JobClaim populateJobClaim(Object obj)
    {
        // Don't blow up.
        if (obj == null) return null;
        Object[] array = (Object[]) obj;

        // Populate the queue object using table definition field order,
        // which is the order specified in all calling methods.
        JobClaim jobClaim = new JobClaim();
        jobClaim.setJobUuid((String) array[0]);
        jobClaim.setWorkerUuid((String) array[1]);
        jobClaim.setSchedulerName((String) array[2]);
        jobClaim.setHost((String) array[3]);
        jobClaim.setContainerId((String) array[4]);
        
        return jobClaim;
    }
}
