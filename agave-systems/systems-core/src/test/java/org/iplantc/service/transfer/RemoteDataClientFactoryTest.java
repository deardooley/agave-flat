package org.iplantc.service.transfer;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.iplantc.service.systems.model.enumerations.StorageProtocolType;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.google.common.io.Files;

public class RemoteDataClientFactoryTest {
    
    private RemoteDataClientFactory factory = new RemoteDataClientFactory();

     @BeforeMethod
    public void beforeMethod() {
    }

    @DataProvider
    public Object[][] dp() {
        return new Object[][] { new Object[] { 1, "a" }, new Object[] { 2, "b" }, };
    }

    @Test
    public void getInstanceRemoteSystemString() {
        
    }

    @DataProvider(name = "getInstanceFromURIProvider")
    protected Object[][] getInstanceFromURIProvider() {
        
        ArrayList<Object[]> testCases = new ArrayList<Object[]>();
        
//        for (iterable_type iterable_element : iterable) {
//            testCases.add(new Object[] { iterable_element, null, true, "message" });
//        }
        return testCases.toArray(new Object[][] {});
    }
    
    @Test(dataProvider="getInstanceFromURIProvider")
    public void getInstanceFromURI() {
        
    }

    @DataProvider(name = "isSchemaSupportedProvider")
    protected Object[][] isSchemaSupportedProvider() {
        ArrayList<Object[]> testCases = new ArrayList<Object[]>();

        for (StorageProtocolType protocol: StorageProtocolType.values()) {
            testCases.add(new Object[] { protocol.name(), protocol.allowsURLAuth() });
            testCases.add(new Object[] { protocol.name().toLowerCase(), protocol.allowsURLAuth() });
        }
        testCases.add(new Object[] { "agave", true });
        testCases.add(new Object[] { "AGAVE", true });
        testCases.add(new Object[] { "fooftp", false });
        testCases.add(new Object[] { "FOOFTP", false });
        testCases.add(new Object[] { "file", false });
        testCases.add(new Object[] { "", true });
        testCases.add(new Object[] { null, true });
        
        return testCases.toArray(new Object[][] {});
    }   
    
    @Test(dataProvider="isSchemaSupportedProvider")
    public void isSchemeSupported(String schema, boolean shouldSucceed) 
    throws Exception 
    {
        URI uri = null;
        File tempDir = null;
        try
        {
            tempDir = File.createTempFile("foo", "bar");
            if (StringUtils.isEmpty(schema)) {
                uri = URI.create(tempDir.getAbsolutePath());
            } else {
                uri = URI.create(schema + "://storage.example.com/" + tempDir.getAbsolutePath());
            }
            Assert.assertEquals(RemoteDataClientFactory.isSchemeSupported(uri), shouldSucceed, 
                    "Factory should " + (shouldSucceed ? "" : "not ") + " support URI with schema " + schema);
        } 
        finally {
            FileUtils.deleteQuietly(tempDir);
        }
    }
}
