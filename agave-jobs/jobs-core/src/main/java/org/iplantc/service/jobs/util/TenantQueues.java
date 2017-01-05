package org.iplantc.service.jobs.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.common.dao.TenantDao;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.jobs.dao.JobQueueDao;
import org.iplantc.service.jobs.exceptions.JobException;
import org.iplantc.service.jobs.model.JobQueue;

import com.fasterxml.jackson.databind.ObjectMapper;

/** Class that reads the tenant queue configuration resource file
 * and updates the job_queues table with the latest information.
 * 
 * @author rcardone
 */
public final class TenantQueues
{
    /* ********************************************************************** */
    /*                                Constants                               */
    /* ********************************************************************** */
    // Tracing.
    private static final Logger _log = Logger.getLogger(TenantQueues.class);
    
    // Default tenant queue configuration file.
    public static final String DEFAULT_CONFIG_FILE = "TenantQueueConfiguration.json";
    
    /* ********************************************************************** */
    /*                                 Fields                                 */
    /* ********************************************************************** */
    // Reusable dao instance.    
    private JobQueueDao _jobQueueDao;
    
    /* ********************************************************************** */
    /*                             Public Methods                             */
    /* ********************************************************************** */    
    /* ---------------------------------------------------------------------- */
    /* main:                                                                  */
    /* ---------------------------------------------------------------------- */
    public static void main(String[] args)
     throws JobException
    {
        // Parse parms.
        String fileName;
        if (args.length > 0) fileName = args[0];
         else fileName = DEFAULT_CONFIG_FILE;
        
        // Update the queue configuration.
        TenantQueues tenantQueues = new TenantQueues();
        tenantQueues.update(fileName);
    }
    
    /* ---------------------------------------------------------------------- */
    /* update:                                                                */
    /* ---------------------------------------------------------------------- */
    /** This method reads a queue configuration file that contains queue
     * definitions in a json array.
     * 
     * @param fileName non-null configuration file path
     * @return a result object
     * @throws JobException on error
     */
    public UpdateResult update(String fileName)
     throws JobException
    {
        // Check input.
        if (StringUtils.isBlank(fileName)) {
            String msg = "Invalid queue configuration file name.";
            _log.error(msg);
            throw new JobException(msg);
        }
        
        // Result accumulator.
        UpdateResult result = new UpdateResult();
        
        // -------------------- Read Configuration File ---------------------
        BufferedReader rdr = null;
        try {rdr = new BufferedReader(new FileReader(fileName));}
            catch (Exception e) {
                String msg = "Unable to read tenant queue configuration file: " + fileName + ".";
                _log.error(msg, e);
                throw new JobException(msg, e);
            }
        
        // Get an object mapper.
        ObjectMapper mapper = new ObjectMapper();
        JobQueue[] queueDefArray = null;
        try {queueDefArray = mapper.readValue(rdr, JobQueue[].class);}
            catch (Throwable e) {
                String msg = "Unable to read tenant queue configuration file: " + fileName + ".";
                _log.error(msg, e);
                throw new JobException(msg, e);
            }
        
        // Is there any work to do?
        if ((queueDefArray == null) || queueDefArray.length == 0) return result;
        
        // Tracing.
        if (_log.isDebugEnabled())
            _log.debug(queueDefArray.length + " queue definitions read from " + fileName + ".");
        
        // -------------------- Get Tenant Info -----------------------------
        // Create the set of all tenantId's configured with a queue.
        HashSet<String> tenantSet = new HashSet<>();
        for (JobQueue queue : queueDefArray) {
            if (queue.getName() == null) continue;
            if (queue.getTenantId() != null) tenantSet.add(queue.getTenantId());
             else {
                 String msg = "No tenant ID configured in queue definition " + 
                              queue.getName() + ".";
                 _log.warn(msg);
             }
        }
        
        // Remove any tenant that isn't defined in the tenants table.
        TenantDao tenantDao = new TenantDao();
        Iterator<String> it = tenantSet.iterator();
        while (it.hasNext()) {
            // Check that the tenant has been defined.
            String tenantId = it.next();
            boolean exists = false; // 
            try {exists = tenantDao.exists(tenantId);}
                catch (Exception e) {
                    String msg = "Unable to check existance of tenant " + tenantId + 
                                 ". Skipping queue definition refresh for this tenant.";
                    _log.error(msg, e);
                }
            
            // Remove the undefined or inaccessible tenant from 
            // the set of tenant with configured queues.
            if (!exists) it.remove(); 
        }
        
        // Don't bother if there's no work to do.
        if (tenantSet.isEmpty()) return result;
        _jobQueueDao = new JobQueueDao();

        // Get all the currently defined queues for all the vetted tenants and
        // save them in a map.  The map organization is by tenant by queue name:
        //
        //      tenantId -> Map<queueName, queue>
        //
        HashMap<String,HashMap<String,JobQueue>> tenantQueueMap = new HashMap<>();
        it = tenantSet.iterator();
        while (it.hasNext()) {
            
            // Get all the queues defined for a tenant.
            String tenantId = it.next();
            List<JobQueue> existingQueueList = null;
            try {existingQueueList = _jobQueueDao.getQueues(tenantId);}
                catch (Exception e) {
                    // Log error and skip this tenant.
                    String msg = "Unable to retreive queue definitions for tenant " + tenantId + 
                                 ". Skipping queue definition refresh for this tenant.";
                    _log.error(msg, e);
                    continue;
                }
            
            // Populate the nested map structure.
            HashMap<String,JobQueue> queueMap = new HashMap<>(2 * existingQueueList.size() + 1);
            for (JobQueue queue : existingQueueList) queueMap.put(queue.getName(), queue);
            tenantQueueMap.put(tenantId, queueMap);
        }
        
        // -------------------- Write Database ------------------------------
        // Process each queue defintion.
        for (JobQueue queueDef : queueDefArray) {
            
            // Skip garbage; missing tenantId already logged.
            if (queueDef.getName() == null || queueDef.getTenantId() == null) continue;
            
            // Process the definition as long as its tenant is defined.
            // Missing tenants have already been logged.
            HashMap<String,JobQueue> existingQueueMap = tenantQueueMap.get(queueDef.getTenantId());
            if (existingQueueMap == null) result.queuesRejected.add(queueDef.getName());
             else processQueueDefinition(queueDef, existingQueueMap, result);
        }
        
        return result;
    }
    
    /* ********************************************************************** */
    /*                            Private Methods                             */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* processQueueDefinition:                                                */
    /* ---------------------------------------------------------------------- */
    /** Determine whether a queue insert or update should take place. 
     * 
     * @param queueDef the queue definition from the configuration file
     * @param existingQueueMap the (non-null) queues currently defined for the tenant
     * @param result the result reporting object that will be modified
     */
    private void processQueueDefinition(JobQueue queueDef,  
                                        HashMap<String,JobQueue> existingQueueMap, 
                                        UpdateResult result)
    {
        // We either insert a new queue definition or update an existing one.
        JobQueue existingQueue = existingQueueMap.get(queueDef.getName());
        if (existingQueue == null) insertQueueDefintion(queueDef, result);
         else updateQueueDefinition(queueDef, existingQueue, result);
    }
    
    /* ---------------------------------------------------------------------- */
    /* insertQueueDefintion:                                                  */
    /* ---------------------------------------------------------------------- */
    /** Insert a new queue definition into the queues table.
     * 
     * @param queueDef the queue definition from the configuration file
     * @param result the result reporting object that will be modified
     */
    private void insertQueueDefintion(JobQueue queueDef, UpdateResult result)
    {
        // Save the current tenant id.
        String savedTenantId = TenancyHelper.getCurrentTenantId();
        
        // Swallow all exceptions.
        try {
            // Temporarily reassign the tenant to pass permission checking.
            TenancyHelper.setCurrentTenantId(queueDef.getTenantId());
            
            // Attempt to add a new queue.
            int rows = _jobQueueDao.createQueue(queueDef);
            if (rows > 0) result.queuesCreated.add(queueDef.getName());
             else result.queuesRejected.add(queueDef.getName());
        }
        catch (Exception e) {
            String msg = "Unable to add queue definition to database for queue " +
                         queueDef.getName() + ".";
            _log.error(msg, e);
            result.queuesCreateFailed.add(queueDef.getName());
        }
        finally {
            // Always restore the tenant id.
            TenancyHelper.setCurrentTenantId(savedTenantId);
        }
    }
    
    /* ---------------------------------------------------------------------- */
    /* updateQueueDefinition:                                                 */
    /* ---------------------------------------------------------------------- */
    /** Update an existing queue definition in the database if it differs from
     * the configuration file definition.  
     * 
     * @param queueDef the queue definition from the configuration file
     * @param existingQueueMap the queues currently defined for the tenant
     * @param result the result reporting object that will be modified
     */
    private void updateQueueDefinition(JobQueue queueDef, 
                                       JobQueue existingQueue,
                                       UpdateResult result)
    {
        // Only the paranoid survive.
        if (!queueDef.getTenantId().equals(existingQueue.getTenantId())) {
            String msg = "Unexpected tenant mismatch for queue " + queueDef.getName() + ".";
            _log.error(msg);
            result.queuesRejected.add(queueDef.getName());
            return;
        }
        
        // Tolerate null or empty filters since the configuration file may not have
        // specified a filter when the queue was first defined.  In that case, the 
        // queue would have been created with the default filter and the original 
        // definition could still be in the configuration file.
        if (StringUtils.isBlank(queueDef.getFilter())) 
            queueDef.setFilter(existingQueue.getFilter());
        if (StringUtils.isBlank(queueDef.getFilter())) {
            String msg = "Null or empty filter configured for queue " + queueDef.getName() + ".";
            _log.error(msg);
            result.queuesRejected.add(queueDef.getName());
            return;
        }
        
        // Save the current tenant id.
        String savedTenantId = TenancyHelper.getCurrentTenantId();
        
        // Swallow all exceptions.
        try {
            // Temporarily reassign the tenant to pass permission checking.
            TenancyHelper.setCurrentTenantId(queueDef.getTenantId());
            
            // Determine if the queue definition in the configuration file
            // differs from the current database definition.
            boolean changed = false;
            if (queueDef.getPriority() != existingQueue.getPriority()) changed = true;
            if (queueDef.getNumWorkers() != existingQueue.getNumWorkers()) changed = true;
            if (queueDef.getMaxMessages() != existingQueue.getMaxMessages()) changed = true;
            if (!queueDef.getFilter().equals(existingQueue.getFilter()))  changed = true;
            
            // Is an update necessary?
            if (!changed) {
                result.queuesNotChanged.add(queueDef.getName());
                return;
            }
            
            // Update the queue definition.
            int rows = _jobQueueDao.updateFields(queueDef);
            if (rows > 0) result.queuesUpdated.add(queueDef.getName());
             else result.queuesRejected.add(queueDef.getName());
        }
        catch (Exception e) {
            String msg = "Unable to update queue definition in database for queue " +
                         queueDef.getName() + ".";
            _log.error(msg, e);
            result.queuesUpdateFailed.add(queueDef.getName());
        }
        finally {
            // Always restore the tenant id.
            TenancyHelper.setCurrentTenantId(savedTenantId);
        }
    }
    
    /* ********************************************************************** */
    /*                            UpdateResult Class                          */
    /* ********************************************************************** */ 
    public static final class UpdateResult
    {
        public LinkedList<String> queuesCreated = new LinkedList<>();
        public LinkedList<String> queuesUpdated = new LinkedList<>();
        public LinkedList<String> queuesNotChanged = new LinkedList<>();
        public LinkedList<String> queuesRejected = new LinkedList<>();
        public LinkedList<String> queuesCreateFailed = new LinkedList<>();
        public LinkedList<String> queuesUpdateFailed = new LinkedList<>();
    }
}
