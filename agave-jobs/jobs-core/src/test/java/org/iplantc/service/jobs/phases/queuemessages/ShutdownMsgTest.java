package org.iplantc.service.jobs.phases.queuemessages;

import org.iplantc.service.jobs.exceptions.JobException;
import org.iplantc.service.jobs.phases.utils.TopicMessageSender;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

/** This test suite does not use any Agave services and should be run when 
 * the Agave jobs service is not running.  The set-up and tear-down methods
 * release any existing leases.
 * 
 * @author rcardone
 */
public class ShutdownMsgTest {
    /* ********************************************************************** */
    /*                             Static Fields                              */
    /* ********************************************************************** */ 
    
    /* ********************************************************************** */
    /*                                 Fields                                 */
    /* ********************************************************************** */
    
    /* ********************************************************************** */
    /*                            Set Up / Tear Down                          */
    /* ********************************************************************** */    
    /* ---------------------------------------------------------------------- */
    /* setup:                                                                 */
    /* ---------------------------------------------------------------------- */
    @BeforeSuite
    private void setup()
    {
    }
    
    /* ---------------------------------------------------------------------- */
    /* teardown:                                                              */
    /* ---------------------------------------------------------------------- */
    @AfterSuite
    private void teardown()
    {
    }
    
    /* ********************************************************************** */
    /*                              Test Methods                              */
    /* ********************************************************************** */    
    /* ---------------------------------------------------------------------- */
    /* getAndReleaseLease:                                                    */
    /* ---------------------------------------------------------------------- */
    @Test(enabled=true)
    public void shutdownAllPhases() throws JobException
    {   
        // This message targets all phased since we don't specifically name any.
        ShutdownMessage shutdownMsg = new ShutdownMessage();
        TopicMessageSender.sendShutdownMessage(shutdownMsg);
    }
}
