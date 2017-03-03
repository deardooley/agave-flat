package org.iplantc.service.jobs.phases.queuemessages;

import org.iplantc.service.jobs.exceptions.JobException;
import org.iplantc.service.jobs.model.enumerations.JobPhaseType;
import org.iplantc.service.jobs.phases.utils.TopicMessageSender;
import org.testng.annotations.Test;

/** This test suite requires the jobs service to be running. 
 * 
 * @author rcardone
 */
public class ResetNumWorkersMsgTest {
    /* ********************************************************************** */
    /*                              Test Methods                              */
    /* ********************************************************************** */    
    /* ---------------------------------------------------------------------- */
    /* resetNumWorkers:                                                       */
    /* ---------------------------------------------------------------------- */
    @Test(enabled=true)
    public void resetNumWorkers() throws JobException
    {   
        // This message adds new workers.
        ResetNumWorkersMessage resetMsg = new ResetNumWorkersMessage();
        resetMsg.phase = JobPhaseType.STAGING;
        resetMsg.tenantId = "iplantc.org";
        resetMsg.queueName = "STAGING.iplantc.org";
        resetMsg.numWorkersDelta = 3;
        TopicMessageSender.sendResetNumWorkersMessage(resetMsg);
        
        // This message removes those workers.
        resetMsg = new ResetNumWorkersMessage();
        resetMsg.phase = JobPhaseType.STAGING;
        resetMsg.tenantId = "iplantc.org";
        resetMsg.queueName = "STAGING.iplantc.org";
        resetMsg.numWorkersDelta = -3;
        TopicMessageSender.sendResetNumWorkersMessage(resetMsg);
    }
}
