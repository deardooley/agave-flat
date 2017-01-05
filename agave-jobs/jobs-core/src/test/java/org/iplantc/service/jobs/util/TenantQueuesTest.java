package org.iplantc.service.jobs.util;

import org.apache.log4j.Logger;
import org.iplantc.service.jobs.exceptions.JobException;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Class that reads the tenant queue configuration resource file
 * and updates the job_queues table with the latest information.
 * 
 * @author rcardone
 */
public final class TenantQueuesTest
{
    /* ********************************************************************** */
    /*                                Constants                               */
    /* ********************************************************************** */
    // Tracing.
    private static final Logger _log = Logger.getLogger(TenantQueuesTest.class);
    
    // Default tenant queue configuration file.
    private static final String TEST_CONFIG_FILE = "src/main/resources/TenantQueueConfiguration.json";
    
    /* ********************************************************************** */
    /*                             Public Methods                             */
    /* ********************************************************************** */    
    /* ---------------------------------------------------------------------- */
    /* queueTest:                                                             */
    /* ---------------------------------------------------------------------- */
    @Test(enabled=true)
    public void queueTest()
     throws JobException
    {
        // Update the queue configuration.
        TenantQueues tenantQueues = new TenantQueues();
        TenantQueues.UpdateResult result = tenantQueues.update(TEST_CONFIG_FILE);
        
        Assert.assertEquals(result.queuesNotChanged.size(), 7, "Unexpected number of skipped queues.");
        Assert.assertEquals(result.queuesCreated.size(), 1, "Unexpected number of created queues.");
    }
    
}
