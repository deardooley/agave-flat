package org.iplantc.service.jobs.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
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
    
    // Default tenant queue configuration directory.
    public static final String DEFAULT_CONFIG_DIR = "queueConfigurations/";
    
    // Configuration files have the form <tenantId>.json.
    public static final String CONFIG_FILE_SUFFIX = ".json";
    
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
     throws JobException, FileNotFoundException
    {
        // Parse parms.
        String fileName = null;
        if (args.length > 0) fileName = args[0];
        
        // Update the queue configuration.
        TenantQueues tenantQueues = new TenantQueues();
        UpdateResult result;
        if (fileName != null) result = tenantQueues.update(fileName);
         else result = tenantQueues.updateAll();

        // Print results.
        if (_log.isInfoEnabled()) {
            StringBuilder buf = new StringBuilder(512);
            buf.append("\n-------------- Queue Update Results -------------\n");
            buf.append(result.toString());
            buf.append("-------------------------------------------------\n");
            _log.info(buf.toString());
        }
    }
    
    /* ---------------------------------------------------------------------- */
    /* updateAll:                                                              */
    /* ---------------------------------------------------------------------- */
    /** Convenience method that reads the default queue configuration file.
     * 
     * @return a result object
     * @throws JobException on error
     */
    public UpdateResult updateAll()
     throws JobException
    {
        // Get all active tenant ids.
        TenantDao tenantDao = new TenantDao();
        List<String> tenantIds;
        try {tenantIds = tenantDao.getTenantIds(true);}
            catch (Exception e) {
                String msg = "Unable to query active tenants.";
                _log.error(msg, e);
                throw new JobException(msg,e);
            }
        
        // Accumulate results across all tenants that are found.
        // We ignore tenant without configuration files.
        UpdateResult result = new UpdateResult();
        for (String tenantId : tenantIds) 
            try {update(tenantId, result);} catch (FileNotFoundException e) {} 
        return result;
    }
    
    /* ---------------------------------------------------------------------- */
    /* update:                                                                */
    /* ---------------------------------------------------------------------- */
    /** This method reads a queue configuration file that contains queue
     * definitions in a json array.
     * 
     * @param tenantId non-null configuration file path
     * @return a result object
     * @throws JobException on error
     * @throws FileNotFoundException 
     */
    public UpdateResult update(String tenantId, UpdateResult... cumulativeResult)
     throws JobException, FileNotFoundException
    {
        // -------------------- Validation/Initialization -------------------
        // Check input.
        if (StringUtils.isBlank(tenantId)) {
            String msg = "Invalid tenant ID.";
            _log.error(msg);
            throw new JobException(msg);
        }
        
        // Validate the tenant.
        TenantDao tenantDao = new TenantDao();
        boolean exists;
        try {exists = tenantDao.exists(tenantId);}
        catch (Exception e) {
            String msg = "Unable to check existance of tenant " + tenantId + 
                         ". Skipping queue definition refresh for this tenant.";
            _log.error(msg, e);
            throw new JobException(msg, e);
        }
        if (!exists) {
            String msg = "Tenant " + tenantId + " does not exist.";
            _log.error(msg);
            throw new JobException(msg);
        }
        
        // Assign result varable.
        UpdateResult result;
        if (cumulativeResult.length > 0) result = cumulativeResult[0];
            else result = new UpdateResult();
        
        // Construct resource file name.
        String fileName = DEFAULT_CONFIG_DIR + tenantId + CONFIG_FILE_SUFFIX;
        
        // -------------------- Read Configuration File ---------------------
        // Get the class loader.
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) classLoader = ClassLoader.getSystemClassLoader();
        
        // Read the configuration file.
        InputStream ins = null;
        try {ins = classLoader.getResourceAsStream(fileName);}
            catch (Exception e) {
                String msg = "Unable to read tenant queue configuration file: " + fileName + ".";
                _log.error(msg, e);
                throw new JobException(msg, e);
            }
        // A tenant may legitimately not have a configuration file.
        if (ins == null) {
            String msg = "Tenant queue configuration file: " + fileName + " not found.";
            _log.info(msg);
            throw new FileNotFoundException(msg);
        }
        
        // Get an object mapper.
        ObjectMapper mapper = new ObjectMapper();
        JobQueue[] queueDefArray = null;
        try {queueDefArray = mapper.readValue(ins, JobQueue[].class);}
            catch (Throwable e) {
                String msg = "Unable to read tenant queue configuration file: " + fileName + ".";
                _log.error(msg, e);
                throw new JobException(msg, e);
            }
            finally {
                // Always attempt to close the input stream.
                try {ins.close();} catch (IOException e){}
            }
        
        // Record that we read the tenant configuration file.
        result.fileNames.add(fileName);
        
        // Is there any work to do?
        if ((queueDefArray == null) || queueDefArray.length == 0) return result;
        
        // Accumulate the total read total.
        result.queueDefinitionsRead += queueDefArray.length;
        if (_log.isDebugEnabled())
            _log.debug(queueDefArray.length + " queue definitions read from " + fileName + ".");
        
        // -------------------- Get Tenant Info -----------------------------
        // Put the currently defined queues for the tenant in a map 
        // with key queue name and value queue object.
        _jobQueueDao = new JobQueueDao();
        List<JobQueue> existingQueueList = null;
        try {existingQueueList = _jobQueueDao.getQueues(tenantId);}
            catch (Exception e) {
                // Log error and skip this tenant.
                String msg = "Unable to retreive queue definitions for tenant " + tenantId + 
                             ". Skipping queue definition refresh for this tenant.";
                 _log.error(msg, e);
                 throw new JobException(msg, e);
            }
            
        // Populate the nested map structure.
        HashMap<String,JobQueue> existingQueueMap = new HashMap<>(2 * existingQueueList.size() + 1);
        for (JobQueue queue : existingQueueList) existingQueueMap.put(queue.getName(), queue);
        
        // -------------------- Write Database ------------------------------
        // Process each queue defintion.
        for (JobQueue queueDef : queueDefArray) {
            
            // Skip garbage.
            if (queueDef.getName() == null) {
                result.queuesRejected.add("Unnamed Queue");
                _log.warn("Encountered unnamed queue definition in file " + fileName + ".");
                continue;
            }
            if (queueDef.getTenantId() == null) {
                result.queuesRejected.add(queueDef.getName());
                _log.warn("No tenant ID configured in queue definition " + 
                          queueDef.getName() + ".");
                continue;
            }
            
            // Check for the expected tenant id.
            if (!tenantId.equals(queueDef.getTenantId())) {
                result.queuesRejected.add(queueDef.getName());
                _log.warn("Queue " + queueDef.getName() + " is defined in file " + fileName + 
                          " with tenant id " + queueDef.getTenantId() + " instead of " +
                          tenantId + ".");
                continue;
            }
                
            // We either insert a new queue definition or update an existing one.
            JobQueue existingQueue = existingQueueMap.get(queueDef.getName());
            if (existingQueue == null) insertQueueDefintion(queueDef, result);
             else updateQueueDefinition(queueDef, existingQueue, result);
        }
        
        return result;
    }
    
    /* ********************************************************************** */
    /*                            Private Methods                             */
    /* ********************************************************************** */
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
        public int queueDefinitionsRead;
        public ArrayList<String> fileNames = new ArrayList<>();
        public ArrayList<String> queuesCreated = new ArrayList<>();
        public ArrayList<String> queuesUpdated = new ArrayList<>();
        public ArrayList<String> queuesNotChanged = new ArrayList<>();
        public ArrayList<String> queuesRejected = new ArrayList<>();
        public ArrayList<String> queuesCreateFailed = new ArrayList<>();
        public ArrayList<String> queuesUpdateFailed = new ArrayList<>();
        
        /** Provide a formatted string of queue results that looks like:
         * 
         *  Queues created: 2, [q1, q2]
         *  Queues updated: 0
         *  ...
         * 
         * The skipped queues are not listed to avoid drownding in noise.
         * */
        @Override
        public String toString()
        {
            StringBuilder buf = new StringBuilder(200);
            buf.append("Queue definitions read: ");
            buf.append(queueDefinitionsRead);
            buf.append("\n");
            
            buf.append("Files read: ");
            buf.append(fileNames.size());
            if (!fileNames.isEmpty()){
                buf.append(", ");
                buf.append(list(fileNames));
            }
            buf.append("\n");
            
            buf.append("Queues created: ");
            buf.append(queuesCreated.size());
            if (!queuesCreated.isEmpty()){
                buf.append(", ");
                buf.append(list(queuesCreated));
            }
            buf.append("\n");
            
            buf.append("Queues updated: ");
            buf.append(queuesUpdated.size());
            if (!queuesUpdated.isEmpty()){
                buf.append(", ");
                buf.append(list(queuesUpdated));
            }
            buf.append("\n");
            
            buf.append("Queues skipped: ");
            buf.append(queuesNotChanged.size());
            buf.append("  <Not Shown>");
            buf.append("\n");
            
            buf.append("Queues rejected: ");
            buf.append(queuesRejected.size());
            if (!queuesRejected.isEmpty()){
                buf.append(", ");
                buf.append(list(queuesRejected));
            }
            buf.append("\n");
            
            buf.append("Queue create failures: ");
            buf.append(queuesCreateFailed.size());
            if (!queuesCreateFailed.isEmpty()){
                buf.append(", ");
                buf.append(list(queuesCreateFailed));
            }
            buf.append("\n");
            
            buf.append("Queue update failures: ");
            buf.append(queuesUpdateFailed.size());
            if (!queuesUpdateFailed.isEmpty()){
                buf.append(", ");
                buf.append(list(queuesUpdateFailed));
            }
            buf.append("\n");
            
            return buf.toString();
        }
        
        /** Concatenate queue names into a string. */
        private String list(List<String> qlist)
        {
            StringBuilder buf = new StringBuilder(200);
            buf.append("[");
            for (int i = 0; i < qlist.size(); i++) {
                if (i != 0) buf.append(", ");
                buf.append(qlist.get(i));
            }
            buf.append("]");
            return buf.toString();
        }
    }
}
