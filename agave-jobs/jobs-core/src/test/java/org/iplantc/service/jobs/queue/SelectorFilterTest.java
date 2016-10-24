package org.iplantc.service.jobs.queue;

import java.util.HashMap;
import java.util.Map;

import org.iplantc.service.jobs.exceptions.JobQueueFilterException;
import org.iplantc.service.jobs.model.enumerations.JobPhaseType;
import org.testng.Assert;
import org.testng.annotations.Test;

public class SelectorFilterTest {
    /* ********************************************************************** */
    /*                             Static Fields                              */
    /* ********************************************************************** */ 
    // Filter and replacement values used by all tests.
    private static String _filter;
    private static final Map<String, Object> _properties = new HashMap<>();
    
    /* ********************************************************************** */
    /*                              Test Methods                              */
    /* ********************************************************************** */    
    /* ---------------------------------------------------------------------- */
    /* defaultFilter:                                                         */
    /* ---------------------------------------------------------------------- */
    @Test(enabled=true)
    public void defaultFilter()
    {
        // Always clear the properties mapping.
        _properties.clear();
        
        // Define the filter expression.
        _filter = "phase = '" + JobPhaseType.STAGING + 
                  "' AND tenant_id = '" + "iplantc.org" + "'";
        
        // Positive test.
        _properties.put("phase", JobPhaseType.STAGING.name());
        _properties.put("tenant_id", "iplantc.org");
        boolean matched = match();
        Assert.assertTrue(matched, "Filter matching failed: " + _filter);
        
        // Negative test.
        _properties.put("phase", JobPhaseType.ARCHIVING.name());
        matched = match();
        Assert.assertFalse(matched, "1 - Filter matching should have failed: " + _filter);
        
        // Negative test.
        _properties.put("phase", JobPhaseType.STAGING.name());
        _properties.put("tenant_id", "BAD TENANT");
        matched = match();
        Assert.assertFalse(matched, "2 - Filter matching should have failed: " + _filter);
        
        // Negative test.
        _properties.clear();
        matched = match();
        Assert.assertFalse(matched, "3 - Filter matching should have failed: " + _filter);
    }

    /* ---------------------------------------------------------------------- */
    /* booleanFilter:                                                         */
    /* ---------------------------------------------------------------------- */
    @Test(enabled=true)
    public void booleanFilter()
    {  
        // Always clear the properties mapping.
        _properties.clear();
        
        // Define the filter expression.
        _filter = "int1 > 66 AND int2 <> 5 AND (name LIKE 'Jo%n' OR range BETWEEN 200 AND 300)";
        
        // Positive test.
        _properties.put("int1", 100);
        _properties.put("int2", 0);
        _properties.put("name", "John");
        _properties.put("range", 250);
        boolean matched = match();
        Assert.assertTrue(matched, "1 - Filter matching failed: " + _filter);
        
        // Positive test.
        matched = match();
        _properties.put("junk", 888);
        Assert.assertTrue(matched, "2 - Filter matching failed: " + _filter);
        
        // Negative test.
        _properties.put("int1", 9);
        matched = match();
        Assert.assertFalse(matched, "1 - Filter matching should have failed: " + _filter);
        
        // Negative test.
        _properties.put("int1", 100);
        _properties.put("int2", 5);
        matched = match();
        Assert.assertFalse(matched, "2 - Filter matching should have failed: " + _filter);
        
        // Negative test.
        _properties.put("int2", 8);
        _properties.put("name", "Johnny");
        _properties.put("range", Integer.MAX_VALUE);
        matched = match();
        Assert.assertFalse(matched, "3 - Filter matching should have failed: " + _filter);
    }

    /* ---------------------------------------------------------------------- */
    /* dateFilter:                                                            */
    /* ---------------------------------------------------------------------- */
    @Test(enabled=true)
    public void dateFilter()
    {  
        // Always clear the properties mapping.
        _properties.clear();
        
        // Dates can only be handled using epoch time.
        long millis = System.currentTimeMillis();
        
        // Define the filter expression.
        _filter = "date BETWEEN " + (millis - 10000) + " AND " + (millis - 1000);
        
        // Positive test.
        _properties.put("date", (millis - 5000));
        boolean matched = match();
        Assert.assertTrue(matched, "Filter matching failed: " + _filter);
        
        // Negative test.
        _properties.put("date", millis);
        matched = match();
        Assert.assertFalse(matched, "1 - Filter matching should have failed: " + _filter);
    }

    /* ---------------------------------------------------------------------- */
    /* notLikeFilter:                                                         */
    /* ---------------------------------------------------------------------- */
    @Test(enabled=true)
    public void notLikeFilter()
    {  
        // Always clear the properties mapping.
        _properties.clear();
        
        // Define the filter expression.  
        // Note that NOT can also be used with IN and BETWEEN.
        _filter = "name NOT LIKE 'Bi__y'";
        
        // Positive test.
        _properties.put("name", "Bill");
        boolean matched = match();
        Assert.assertTrue(matched, "1 - Filter matching failed: " + _filter);
        
        // Negative test.
        _properties.put("name", "Billy");
        matched = match();
        Assert.assertFalse(matched, "1 - Filter matching should have failed: " + _filter);
    }

    /* ---------------------------------------------------------------------- */
    /* escapeLikeFilter:                                                      */
    /* ---------------------------------------------------------------------- */
    @Test(enabled=true)
    public void escapeLikeFilter()
    {  
        // Always clear the properties mapping.
        _properties.clear();
        
        // Define the filter expression.
        // Note that any character can be used to escape _ and % in LIKE clauses.
        _filter = "name LIKE 'George\\_%' ESCAPE '\\'";
        
        // Positive test.
        _properties.put("name", "George_Washington");
        boolean matched = match();
        Assert.assertTrue(matched, "1 - Filter matching failed: " + _filter);
        
        // Negative test.
        _properties.put("name", "George Washington");
        matched = match();
        Assert.assertFalse(matched, "1 - Filter matching should have failed: " + _filter);
    }

    /* ---------------------------------------------------------------------- */
    /* inFilter:                                                              */
    /* ---------------------------------------------------------------------- */
    @Test(enabled=true)
    public void inFilter()
    {  
        // Always clear the properties mapping.
        _properties.clear();
        
        // Define the filter expression.
        _filter = "country IN ('UK', 'US')";
        
        // Positive test.
        _properties.put("country", "US");
        boolean matched = match();
        Assert.assertTrue(matched, "1 - Filter matching failed: " + _filter);
        
        // Positive test.
        _properties.put("country", "UK");
        matched = match();
        Assert.assertTrue(matched, "2 - Filter matching failed: " + _filter);
        
        // Negative test.
        _properties.put("country", "Uk");
        matched = match();
        Assert.assertFalse(matched, "1 - Filter matching should have failed: " + _filter);
        
        // Negative test.
        _properties.put("country", null);
        matched = match();
        Assert.assertFalse(matched, "2 - Filter matching should have failed: " + _filter);
    }

    /* ---------------------------------------------------------------------- */
    /* nullPropertyFilter:                                                    */
    /* ---------------------------------------------------------------------- */
    @Test(enabled=true)
    public void nullPropertyFilter()
    {  
        // Always clear the properties mapping.
        _properties.clear();
        
        // Define the filter expression.
        _filter = "missing is NULL";
        
        // Positive test.
        boolean matched = match();
        Assert.assertTrue(matched, "1 - Filter matching failed: " + _filter);
        
        // Positive test.
        _properties.put("missing", null);
        matched = match();
        Assert.assertTrue(matched, "2 - Filter matching failed: " + _filter);
        
        // Negative test.
        _properties.put("missing", "something");
        matched = match();
        Assert.assertFalse(matched, "1 - Filter matching should have failed: " + _filter);
    }

    /* ---------------------------------------------------------------------- */
    /* regexFilter:                                                           */
    /* ---------------------------------------------------------------------- */
    @Test(enabled=true)
    public void regexFilter()
    {  
        // Always clear the properties mapping.
        _properties.clear();
        
        // --- Define the filter expression.
        _filter = "REGEX('^a.c', 'abc')"; // hardcoded regex and value example
        
        // Positive test.
        boolean matched = match();
        Assert.assertTrue(matched, "1 - Filter matching failed: " + _filter);
        
        // --- Define the filter expression.
        _filter = "REGEX('^a.c', value)"; // variable value
        
        // Positive test.
        _properties.put("value", "abc");
         matched = match();
        Assert.assertTrue(matched, "2 - Filter matching failed: " + _filter);
        
        // Negative test.
        _properties.put("value", "ac");
        matched = match();
        Assert.assertFalse(matched, "1 - Filter matching should have failed: " + _filter);
    }

    /* ********************************************************************** */
    /*                            Private Methods                             */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* match:                                                                 */
    /* ---------------------------------------------------------------------- */
    private boolean match()
    {
        // Evaluate the filter field using the properties field values.
        boolean matched = false;
        try {matched = SelectorFilter.match(_filter, _properties);}
          catch (JobQueueFilterException e) {
            Assert.fail("Unable to process filter: " + _filter, e);        
        }
        return matched;
    }
}
