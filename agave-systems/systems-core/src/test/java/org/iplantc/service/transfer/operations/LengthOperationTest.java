/**
 *
 */
package org.iplantc.service.transfer.operations;

import java.io.File;
import java.io.FileNotFoundException;

import org.apache.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Coverage tests for getting the length of a {@link RemoteFileItem}.
 * @author dooley
 *
 */
@Test(singleThreaded=true, groups= {"length"}) //, dependsOnGroups= {"exists"}
public class LengthOperationTest extends BaseRemoteDataClientOperationTest {

    private static final Logger log = Logger.getLogger(LengthOperationTest.class);

    public LengthOperationTest(String systemJsonFilePath,
            TransferOperationAfterMethod afterMethodTeardown,
            TransferOperationBeforeMethod beforeMethodSetup,
            ForbiddenPathProvider forbiddenPathProvider)
    {
        super(systemJsonFilePath, afterMethodTeardown, beforeMethodSetup, forbiddenPathProvider);
    }

    public void length()
    {
        _length();
    }

    protected void _length()
    {
        try
        {
            getClient().put(LOCAL_BINARY_FILE, "");
            
            Assert.assertEquals(getClient().length(LOCAL_BINARY_FILE_NAME), new File(LOCAL_BINARY_FILE).length(),
                    "remote length does not match local length.");
        }
        catch (FileNotFoundException e) {
            Assert.fail("Failed to stage local file for length check prior to test.", e);
        }
        catch (Exception e) {
            Assert.fail("Failed to retrieve length of remote file", e);
        }
    }

}
