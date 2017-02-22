package org.iplantc.service.jobs.statemachine;

import org.iplantc.service.jobs.model.enumerations.JobStatusType;
import org.testng.Assert;
import org.testng.annotations.Test;

/** This test program issues a small number of legal and illegal state transitions
 * validation calls.  The goal is to test the utility code and leave the 
 * comprehensive state machine validation testing to JobFSMTest. 
 * 
 * @author rcardone
 *
 */
public class JobFSMUtilsTest
{
    /* ********************************************************************** */
    /*                              Test Methods                              */
    /* ********************************************************************** */    
    /* ---------------------------------------------------------------------- */
    /* sampleTest:                                                            */
    /* ---------------------------------------------------------------------- */
    @Test(enabled=true)
    public void sampleTest()
    {   
        // Result of transition test.
        boolean result;
        
        // ----- Legal transitions.
        result = JobFSMUtils.hasTransition(JobStatusType.PAUSED, JobStatusType.ARCHIVING_FINISHED);
        Assert.assertTrue(result, "Failed on a legal transaction!");

        result = JobFSMUtils.hasTransition(JobStatusType.ARCHIVING, JobStatusType.ARCHIVING_FINISHED);
        Assert.assertTrue(result, "Failed on a legal transaction!");

        result = JobFSMUtils.hasTransition(JobStatusType.RUNNING, JobStatusType.CLEANING_UP);
        Assert.assertTrue(result, "Failed on a legal transaction!");

        result = JobFSMUtils.hasTransition(JobStatusType.ARCHIVING, JobStatusType.CLEANING_UP);
        Assert.assertTrue(result, "Failed on a legal transaction!");

        result = JobFSMUtils.hasTransition(JobStatusType.STAGED, JobStatusType.PENDING);
        Assert.assertTrue(result, "Failed on a legal transaction!");

        result = JobFSMUtils.hasTransition(JobStatusType.STAGING_JOB, JobStatusType.STAGED);
        Assert.assertTrue(result, "Failed on a legal transaction!");

        // ----- Illegal transitions.
        result = JobFSMUtils.hasTransition(JobStatusType.PROCESSING_INPUTS, JobStatusType.KILLED);
        Assert.assertFalse(result, "Failed to identify an illegal transaction!");

        result = JobFSMUtils.hasTransition(JobStatusType.FINISHED, JobStatusType.RUNNING);
        Assert.assertFalse(result, "Failed to identify an illegal transaction!");

        result = JobFSMUtils.hasTransition(JobStatusType.STAGED, JobStatusType.STAGING_INPUTS);
        Assert.assertFalse(result, "Failed to identify an illegal transaction!");
        
        result = JobFSMUtils.hasTransition(JobStatusType.PAUSED, null);
        Assert.assertFalse(result, "Failed to identify an illegal transaction!");
        
        result = JobFSMUtils.hasTransition(null, JobStatusType.PENDING);
        Assert.assertFalse(result, "Failed to identify an illegal transaction!");
        
        result = JobFSMUtils.hasTransition(JobStatusType.FINISHED, JobStatusType.STAGING_INPUTS);
        Assert.assertFalse(result, "Failed to identify an illegal transaction!");
        
        result = JobFSMUtils.hasTransition(JobStatusType.FAILED, JobStatusType.FINISHED);
        Assert.assertFalse(result, "Failed to identify an illegal transaction!");
        
        // ----- Junk.
        result = JobFSMUtils.hasTransition(null, JobStatusType.HEARTBEAT);
        Assert.assertFalse(result, "Failed to identify an illegal transaction!");
        
        result = JobFSMUtils.hasTransition(JobStatusType.HEARTBEAT, null);
        Assert.assertFalse(result, "Failed to identify an illegal transaction!");
        
        result = JobFSMUtils.hasTransition(JobStatusType.HEARTBEAT, JobStatusType.STAGING_INPUTS);
        Assert.assertFalse(result, "Failed to identify an illegal HEARTBEAT state!");

        result = JobFSMUtils.hasTransition(JobStatusType.STAGED, JobStatusType.HEARTBEAT);
        Assert.assertFalse(result, "Failed to identify an illegal HEARTBEAT state!");
    }
}
