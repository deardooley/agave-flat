package org.iplantc.service.jobs.phases.queuemessages;

import org.iplantc.service.jobs.exceptions.JobException;
import org.iplantc.service.jobs.model.enumerations.JobPhaseType;
import org.iplantc.service.jobs.phases.utils.TopicMessageSender;
import org.testng.annotations.Test;

/** This test suite does not use any Agave services and should be run when 
 * the Agave jobs service is not running.  The set-up and tear-down methods
 * release any existing leases.
 * 
 * @author rcardone
 */
public class ResetNumWorkersMsgTest {
    /* ********************************************************************** */
    /*                              Test Methods                              */
    /* ********************************************************************** */    
    /* ---------------------------------------------------------------------- */
    /* getAndReleaseLease:                                                    */
    /* ---------------------------------------------------------------------- */
    @Test(enabled=true)
    public void shutdownAllPhases() throws JobException
    {   
        // This message adds new workers.
        ResetNumWorkersMessage resetMsg = new ResetNumWorkersMessage();
        resetMsg.phase = JobPhaseType.STAGING;
        resetMsg.tenantId = "iplantc.org";
        resetMsg.queueName = "STAGING.iplantc.org";
        resetMsg.numWorkers = 3;
        TopicMessageSender.sendResetNumWorkersMessage(resetMsg);
        
        // This message removes those workers.
        resetMsg = new ResetNumWorkersMessage();
        resetMsg.phase = JobPhaseType.STAGING;
        resetMsg.tenantId = "iplantc.org";
        resetMsg.queueName = "STAGING.iplantc.org";
        resetMsg.numWorkers = -3;
        TopicMessageSender.sendResetNumWorkersMessage(resetMsg);
    }
}
