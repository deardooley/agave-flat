package org.iplantc.service.jobs.phases.utils;

import org.testng.Assert;
import org.testng.annotations.Test;

/** This test suite requires the jobs service to be running. 
 * 
 * @author rcardone
 */
public class ThrottleTest 
{
    /* ********************************************************************** */
    /*                              Test Methods                              */
    /* ********************************************************************** */    
    /* ---------------------------------------------------------------------- */
    /* exceedTest:                                                            */
    /* ---------------------------------------------------------------------- */
    @Test(enabled=true)
    public void exceedTest() 
    {   
        int seconds = 1000;
        int limit = 3;
        Throttle throttle = new Throttle(seconds, limit);
        
        // We should only be able to call record 3 times in the time period.
        boolean result = throttle.record();
        Assert.assertEquals(result, true);
        result = throttle.record();
        Assert.assertEquals(result, true);
        result = throttle.record();
        Assert.assertEquals(result, true);
        result = throttle.record();
        Assert.assertEquals(result, false);
    }
    
    /* ---------------------------------------------------------------------- */
    /* slidingWindowTest:                                                     */
    /* ---------------------------------------------------------------------- */
    @Test(enabled=true)
    public void slidingWindowTest() throws InterruptedException 
    {   
        int seconds = 1;
        int limit = 1;
        Throttle throttle = new Throttle(seconds, limit);
        
        // We should never exceed the limit as long as we sleep
        // for a sufficiently long period.
        boolean result = throttle.record();
        Assert.assertEquals(result, true);
        Thread.sleep(1001);
        result = throttle.record();
        Assert.assertEquals(result, true);
        Thread.sleep(1001);
        result = throttle.record();
        Assert.assertEquals(result, true);
        Thread.sleep(1001);
        result = throttle.record();
        Assert.assertEquals(result, true);
        Thread.sleep(1001);
        result = throttle.record();
        Assert.assertEquals(result, true);
        Thread.sleep(1001);
    }
}
