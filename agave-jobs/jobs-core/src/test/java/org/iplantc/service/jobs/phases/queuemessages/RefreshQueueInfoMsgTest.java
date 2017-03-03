package org.iplantc.service.jobs.phases.queuemessages;

import org.iplantc.service.jobs.exceptions.JobException;
import org.iplantc.service.jobs.phases.utils.TopicMessageSender;
import org.testng.annotations.Test;

/** This test suite requires the jobs service to be running. 
 * 
 * @author rcardone
 */
public class RefreshQueueInfoMsgTest {
    /* ********************************************************************** */
    /*                              Test Methods                              */
    /* ********************************************************************** */    
    /* ---------------------------------------------------------------------- */
    /* refresh:                                                               */
    /* ---------------------------------------------------------------------- */
    @Test(enabled=true)
    public void refresh() throws JobException
    {   
        // This message adds new workers.
        RefreshQueueInfoMessage refreshMsg = new RefreshQueueInfoMessage();
        TopicMessageSender.sendRefreshQueueInfoMessage(refreshMsg);
    }
}
