package org.iplantc.service.jobs.dao.utils;

import java.math.BigInteger;
import java.nio.channels.InterruptedByTimeoutException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.iplantc.service.jobs.dao.JobWorkerDao;
import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.jobs.model.JobClaim;
import org.iplantc.service.jobs.model.JobEvent;
import org.iplantc.service.jobs.model.JobUpdateParameters;
import org.iplantc.service.jobs.model.enumerations.JobStatusType;
import org.iplantc.service.jobs.phases.schedulers.dto.JobActiveCount;
import org.iplantc.service.jobs.phases.schedulers.dto.JobMonitorInfo;
import org.iplantc.service.jobs.phases.schedulers.dto.JobQuotaInfo;
import org.iplantc.service.transfer.dao.TransferTaskDao;
import org.iplantc.service.transfer.model.enumerations.TransferStatusType;

/** This file contains auxiliary methods used by JobDao.  The methods here
 * manipulate data structures and perform other tasks in support of SQL calls
 * to the jobs table.  These methods do not make database calls themselves.
 * 
 * @author rcardone
 */
public final class JobDaoUtils
{
    /* ********************************************************************** */
    /*                               Constants                                */
    /* ********************************************************************** */
    // Tracing.
    private static final Logger _log = Logger.getLogger(JobDaoUtils.class);
    
    // Worker claim polling constants.
    private static final int CLAIM_POLL_ITERATIONS = 15;
    private static final int CLAIM_POLL_SLEEP_MILLIS = 1000;

    /* ********************************************************************** */
    /*                                 Fields                                 */
    /* ********************************************************************** */
    // Initialize the dedicated configuration once to avoid synchronization contention.
    private static final DedicatedConfig _dedicatedConfig = DedicatedConfig.getInstance();
    
    /* ********************************************************************** */
    /*                             Public Methods                             */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* createSetClause:                                                       */
    /* ---------------------------------------------------------------------- */
    /** Create the set clause with no padding for an sql update command given
     * a parameter object.  If no values are set, null is returned. 
     * 
     * @param parms object that specified at least one update value
     * @return the sql set clause with no leading or trailing space characters
     *             or null
     */
    public static String createSetClause(JobUpdateParameters parms)
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
        
        // ----- epoch
        if (parms.isEpochFlag()) {
            if (!initialClause.equals(clause)) clause += ", ";
            clause += "epoch = :epoch";
        }
        
        // See if we found any fields to update.
        if (initialClause.equals(clause)) return null;
        return clause;
    }
    
    /* ---------------------------------------------------------------------- */
    /* setUpdatePlaceholders:                                                 */
    /* ---------------------------------------------------------------------- */
    /** Assign the placeholder variable in the update query with the values
     * in the parameter object.
     * 
     * @param qry the sql update statement
     * @param parms the parameters object containing the new values
     */
    public static void setUpdatePlaceholders(Query qry, JobUpdateParameters parms)
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
        
        // ----- epoch
        if (parms.isEpochFlag()) 
            qry.setInteger("epoch", parms.getEpoch());
    }  
    
    /* ---------------------------------------------------------------------- */
    /* getDedicatedTenantIdClause:                                            */
    /* ---------------------------------------------------------------------- */
    /** Create a properly formed where clause for the jobs table query using the 
     * table alias "j". If a tenant id restriction is configured, create a like 
     * clause that may be a negation if the id starts with an exclamation point.
     * 
     * If the returned string is not empty it should be padded with a space at the end.
     * 
     * Security Note: The result clause embeds values read directly from a 
     *                configuration file installed on the server, which is assumed 
     *                to be trustworthy.  
     * 
     * @return a non-null tenant id where clause that may be empty
     */
    public static String getDedicatedTenantIdClause()
    {
        // Read in the configuration string if it exists.
        IDedicatedProvider provider = _dedicatedConfig.getDedicatedProvider();
        String dedicatedTenantId = provider.getDedicatedTenantIdForThisService();
        if (StringUtils.isBlank(dedicatedTenantId)) return "";
        
        // The string can be an assertion or a negation.
        // Return a clause with a trailing space.
        if (dedicatedTenantId.startsWith("!"))
            return "and j.tenant_id not like '" + StringUtils.removeStart(dedicatedTenantId, "!") + "' ";
         else return "and j.tenant_id like '" + dedicatedTenantId + "' ";
    }
    
    /* ---------------------------------------------------------------------- */
    /* getDedicatedUsersClause:                                               */
    /* ---------------------------------------------------------------------- */
    /** Create a properly formed where clause for the jobs table query using the 
     * table alias "j". If owner (i.e., user) restrictions are configured, create 
     * a clause that may be a mix of assertions and negations.
     * 
     * If the returned string is not empty it should be padded with a space at the end.
     * 
     * Security Note: The result clause embeds values read directly from a 
     *                configuration file installed on the server, which is assumed 
     *                to be trustworthy.  
     * 
     * @return a non-null owners where clause that may be empty
     */
    public static String getDedicatedUsersClause()
    {
        // Read in the configuration string if it exists.
        IDedicatedProvider provider = _dedicatedConfig.getDedicatedProvider();
        String[] dedicatedUsers = provider.getDedicatedUsernamesFromServiceProperties();
        if (dedicatedUsers == null || dedicatedUsers.length == 0) return "";
        
        // Initialize the positive and negative clause info.
        String posClause = "and j.owner in (";
        String negClause = "and j.owner not in (";
        boolean pos = false;
        boolean neg = false;
        
        // Work through the list of owners.
        for (String user : dedicatedUsers)
        {
            // Skip bad input.
            if (StringUtils.isBlank(user)) continue;
            
            // Determine polarity and add to proper clause
            if (user.startsWith("!")) {
                if (!neg) neg = true;
                 else negClause += ", ";
                negClause += "'" + StringUtils.removeStart(user, "!") + "'";
            }
            else {
               if (!pos) pos = true;
                else posClause += ", ";
               posClause += "'" + user + "'";
            }
        }
        
        // Add closing parentheses and trailing spaces.
        if (pos) posClause += ") ";
        if (neg) negClause += ") ";
        
        // Determine what to return.
        if (!pos && !neg) return "";
        else if (pos && neg) return posClause + negClause;
        else if (pos) return posClause;
        else return negClause;
    }
    
    /* ---------------------------------------------------------------------- */
    /* getDedicatedSystemIdsClause:                                           */
    /* ---------------------------------------------------------------------- */
    /** Create a properly formed where clause for the jobs table query using the 
     * table alias "j". If system id restrictions are configured, create a clause 
     * that may be a mix of assertions and negations.  
     * 
     * If the returned string is not empty it should be padded with a space at the end.
     * 
     * Security Note: The result clause embeds values read directly from a 
     *                configuration file installed on the server, which is assumed 
     *                to be trustworthy.  
     * 
     * @return a non-null systems where clause that may be empty
     */
    public static String getDedicatedSystemIdsClause()
    {
        // Read in the configuration string if it exists.
        IDedicatedProvider provider = _dedicatedConfig.getDedicatedProvider();
        String[] dedicatedSystemIds = provider.getDedicatedSystemIdsFromServiceProperties();
        if (dedicatedSystemIds == null || dedicatedSystemIds.length == 0) return "";
        
        // Initialize the positive and negative clause info.
        String clause = "";
        
        // Work through the list of owners.
        for (String systemEntry : dedicatedSystemIds)
        {
            // Skip bad input.
            if (StringUtils.isBlank(systemEntry)) continue;
            
            // See if we have a queuename attached to the sysytem.
            String systemId = null;
            String queueName = null;
            if (StringUtils.contains(systemEntry, "#")) {
                String[] tokens = systemEntry.split("#");
                if (tokens.length > 0) systemId = tokens[0];
                if (tokens.length > 1) queueName = tokens[1];
            }
            else systemId = systemEntry;
            
            // We better have a system id.
            if (StringUtils.isBlank(systemId)) continue;
            
            // Use polarity to determine which operators to use inside each
            // parenthesized subclause and also strip out the leading   
            // exclamation point if it exists.
            String equalOp;
            String connectOp;
            if (systemId.startsWith("!")) {
                equalOp = "!=";
                connectOp = "or"; // inner disjunction
                systemId = StringUtils.removeStart(systemId, "!");
            }
            else {
                equalOp = "=";
                connectOp = "and"; // inner conjunction
            }
            
            // Only on the first time through do we define the outer conjunct.
            // Otherwise, we connect the different system specs with a disjunction.
            if (clause.isEmpty()) clause = "and (";
              else clause += " or ";
            
            // Build clause.  Note that when queue names are present this inner clause
            // becomes a disjunction which will evaluate to true if either the system
            // id or the queue name is not equal to the configured values.
            clause += "(j.execution_system " + equalOp + " '" + systemId + "'"; 
            if (!StringUtils.isBlank(queueName))
                clause += " " + connectOp + " j.queue_request " + equalOp + " '" + queueName + "'";
            clause += ") ";
        }
        
        // Terminate the outer conjunct if any systems were specified.
        if (!clause.isEmpty()) clause += ") ";
        return clause;
    }

    /* ---------------------------------------------------------------------- */
    /* populateActiveCountList:                                               */
    /* ---------------------------------------------------------------------- */
    /** Called by getSchedulerActiveJobCount() to populate a list of job active
     * count objects from rows returned from the database.
     * 
     * @param qryResults the raw list of row objects from the database
     * @return a non-null but possibly empty list
     */
    @SuppressWarnings("rawtypes")
    public static List<JobActiveCount> populateActiveCountList(List qryResults)
    {
        // Maybe there's nothing to do.
        if (qryResults == null || qryResults.isEmpty()) 
            return new LinkedList<JobActiveCount>();
       
        // Create output list of proper size.
        ArrayList<JobActiveCount> outList = new ArrayList<>(qryResults.size());
       
        // Marshal each row from the query results.
        for (Object rowobj : qryResults)
        {
            // Access row as an array and create a new active object.
            Object[] row = (Object[]) rowobj;
            JobActiveCount activeCount = new JobActiveCount();
            
            // Marshal fields in result order.  
            // Note that no value can be null. 
            activeCount.setTenantId((String) row[0]);
            activeCount.setOwner((String) row[1]);
            activeCount.setExecutionSystem((String) row[2]);
            activeCount.setQueueRequest((String) row[3]);
            activeCount.setCount(Integer.parseInt((String) row[4]));

            // Add the lease to the result list.
            outList.add(activeCount);
        }
        
        return outList;
    }

    /* ---------------------------------------------------------------------- */
    /* populateQuotaInfoList:                                                 */
    /* ---------------------------------------------------------------------- */
    /** Called by getSchedulerJobQuotaInfo() to populate a list of job quota
     * information objects from rows returned from the database.
     * 
     * @param qryResults the raw list of row objects from the database
     * @return a non-null but possibly empty list
     */
    @SuppressWarnings("rawtypes")
    public static List<JobQuotaInfo> populateQuotaInfoList(List qryResults)
    {
        // Maybe there's nothing to do.
        if (qryResults == null || qryResults.isEmpty()) 
           return new LinkedList<JobQuotaInfo>();
        
        // Create output list of proper size.
        ArrayList<JobQuotaInfo> outList = new ArrayList<>(qryResults.size());
       
        // Marshal each row from the query results.
        for (Object rowobj : qryResults)
        {
            // Access row as an array and create new lease.
            Object[] row = (Object[]) rowobj;
            JobQuotaInfo quotaInfo = new JobQuotaInfo();
            
            // Marshal fields in result order.
            quotaInfo.setUuid((String) row[0]);
            quotaInfo.setTenantId((String) row[1]);
            quotaInfo.setOwner((String) row[2]);
            quotaInfo.setExecutionSystem((String) row[3]);
            quotaInfo.setQueueRequest((String) row[4]);
            
            // These values should never be null, but since we are dealing
            // joined tables, we check anyway.
            if (row[5] == null) quotaInfo.setMaxQueueJobs(-1L);
              else quotaInfo.setMaxQueueJobs(((BigInteger)row[5]).longValue());
            if (row[6] == null) quotaInfo.setMaxQueueUserJobs(-1L);
              else quotaInfo.setMaxQueueUserJobs(((BigInteger)row[6]).longValue());
            
            // Unfortunately, these fields can be null in the database.
            if (row[7] == null) quotaInfo.setMaxSystemJobs(-1L);
              else quotaInfo.setMaxSystemJobs(((Integer)row[7]).longValue());
            if (row[8] == null) quotaInfo.setMaxSystemUserJobs(-1L);
              else quotaInfo.setMaxSystemUserJobs(((Integer)row[8]).longValue());

            // Add the lease to the result list.
            outList.add(quotaInfo);
        }
        
        return outList;
    }
    
    /* ---------------------------------------------------------------------- */
    /* populateMonitorInfoList:                                               */
    /* ---------------------------------------------------------------------- */
    /** Called by the monitoring scheduler to populate a list of job monitor
     * information objects from rows returned from the database.
     * 
     * @param qryResults the raw list of row objects from the database
     * @return a non-null but possibly empty list
     */
    @SuppressWarnings("rawtypes")
    public static List<JobMonitorInfo> populateMonitorInfoList(List qryResults)
    {
        // Maybe there's nothing to do.
        if (qryResults == null || qryResults.isEmpty()) 
            return new LinkedList<JobMonitorInfo>();;
       
        // Create output list of proper size.
        ArrayList<JobMonitorInfo> outList = new ArrayList<>(qryResults.size());
           
        // Marshal each row from the query results.
        for (Object rowobj : qryResults)
        {
            // Access row as an array and create new lease.
            Object[] row = (Object[]) rowobj;
            JobMonitorInfo monitorInfo = new JobMonitorInfo();
            
            // Marshal fields in result order.
            monitorInfo.setUuid((String) row[0]);
            monitorInfo.setStatusChecks((Integer) row[1]);
            Timestamp lastUpdatedTS = (Timestamp) row[2];
            monitorInfo.setLastUpdated(new Date(lastUpdatedTS.getTime()));

            // Add the lease to the result list.
            outList.add(monitorInfo);
        }
        
        return outList;
    }
    
    /* ---------------------------------------------------------------------- */
    /* cancelTransfers:                                                       */
    /* ---------------------------------------------------------------------- */
    /** Make a best effort attempt to cancel all transfers for the specified job.
     * This method does not throw exceptions.
     * 
     * @param job the job whose transfer will be canceled
     */
    public static void cancelTransfers(Job job)
    {
        // Try to get the job's events
        List<JobEvent> events = null;
        try {events = job.getEvents();}
        catch (Exception e) {
            String msg = "Unable to retrieve events for job " + job.getUuid() + ".";
            _log.error(msg, e);
            return;
        }
        
        // Try to cancel the transfers associated with each event.
        for (JobEvent event: events) {
            
            // Maybe there's nothing to do.
            if (event.getTransferTask() == null) continue;
            
            if (event.getTransferTask().getStatus() == TransferStatusType.PAUSED ||
                event.getTransferTask().getStatus() == TransferStatusType.QUEUED ||
                event.getTransferTask().getStatus() == TransferStatusType.RETRYING ||
                event.getTransferTask().getStatus() == TransferStatusType.TRANSFERRING) 
            {
               try {
                   TransferTaskDao.cancelAllRelatedTransfers(event.getTransferTask().getId());
               } catch (Exception e ) {
                   _log.error("Failed to cancel transfer task " +
                              event.getTransferTask().getUuid() + " while stopping job " +
                              job.getUuid(), e);
               }
            }
        }
    }
    
    /* ---------------------------------------------------------------------- */
    /* isJobClaimed:                                                          */
    /* ---------------------------------------------------------------------- */
    /** Poll the job_workers table for a configured period of time to determine
     * if any worker thread is claiming the job.  Return true if after polling
     * completes some worker thread still claims the job; return false if no
     * worker claims the thread.  It is assumed that the caller has configured
     * the job such that a new worker will not claim the job once it is unclaimed.  
     * 
     * Since this can be a long running method, we throw an exception if the 
     * thread is interrupted.
     * 
     * @param jobUuid the job uuid whose claim we are checking
     * @throws InterruptedException if the we receive an interrupt
     */
    public static boolean isJobClaimed(String jobUuid) 
     throws InterruptedException
    {
        // Assume some worker claims the job until proven otherwise.
        boolean jobStillClaimed = true;
        
        // See if no worker is claiming the job.
        boolean errorLogged = false; // Limit the amount of noise we log.
        for (int i = 0; i < CLAIM_POLL_ITERATIONS; i++)
        {
            // Poll the job_workers table for any worker claiming our job.
            try {
                // If we don't get back a claim, no worker has claimed it.
                JobClaim curClaim = JobWorkerDao.getJobClaimByJobUuid(jobUuid);
                if (curClaim == null) {
                    jobStillClaimed = false;
                    break;
                }
            }
            catch (Exception e) {
                if (!errorLogged) {
                    String msg = "Unable to query worker claims for job " + jobUuid + ".";
                    _log.error(msg, e);
                    errorLogged = true;
                }
            }
            
            // Sleep before trying again.  Stop everything on interrupt.
            if (Thread.interrupted()) 
                throw new InterruptedException("Interrupted while polling claim for job " + jobUuid + ".");
            Thread.sleep(CLAIM_POLL_SLEEP_MILLIS);
        }
        
        // If we broke out of the loop, the job is not claimed.
        return jobStillClaimed;
    }
}
