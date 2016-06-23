/**
 *
 */
package org.iplantc.service.transfer.operations;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.iplantc.service.transfer.RemoteDataClient;
import org.iplantc.service.transfer.exceptions.RemoteDataException;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Coverage tests for {@link RemoteDataClient#mkdir} and
 * {@link RemoteDataClient#mkdirs}
 * @author dooley
 *
 */
@Test(singleThreaded=true, groups= {"mkdirs"})
public class MkdirsOperationTest extends BaseRemoteDataClientOperationTest {

    public MkdirsOperationTest(String systemJsonFilePath,
            TransferOperationAfterMethod afterMethodTeardown,
            TransferOperationBeforeMethod beforeMethodSetup,
            ForbiddenPathProvider forbiddenPathProvider)
    {
        super(systemJsonFilePath, afterMethodTeardown, beforeMethodSetup, forbiddenPathProvider);
    }

    private static final Logger log = Logger.getLogger(MkdirsOperationTest.class);

    @DataProvider(name="mkdirsProvider", parallel=false)
    protected Object[][] mkdirsProvider()
    {
        return new Object[][] {
            { null, false, "mkdirs on null name should resolve to user home and not throw an exception." },
            { "", false, "mkdirs on empty name should resolve to user home and not throw an exception." },
            { "deleteme-"+System.currentTimeMillis(), false, "mkdirs new directory in current folder should not fail." },
            { MISSING_DIRECTORY, false, "mkdirs when parent does not exist should not throw an exception." },
        };
    }



    @Test(dataProvider="mkdirsProvider")
	public void mkdirs(String remotedir, boolean shouldThrowException, String message)
	{
        _mkdirs(remotedir, shouldThrowException, message);
	}

    protected void _mkdirs(String remotedir, boolean shouldThrowException, String message)
    {
    	try
    	{
    		getClient().mkdirs(remotedir);
    		Assert.assertFalse(shouldThrowException, message);
    		Assert.assertTrue(getClient().doesExist(remotedir), "Failed to create remote directory");
        }
    	catch (Throwable e) {
        	if (!shouldThrowException) e.printStackTrace();
        	Assert.assertTrue(shouldThrowException, message);
        }
	}

    @Test(dependsOnMethods={"mkdirs"})
    public void mkdirsWithoutRemotePermissionThrowsRemoteDataException()
	{
        _mkdirsWithoutRemotePermissionThrowsRemoteDataException();
	}

    protected void _mkdirsWithoutRemotePermissionThrowsRemoteDataException()
    {
    	try
    	{
    		getClient().mkdirs(getForbiddenDirectoryPath(false) + "/bar");
    		Assert.fail("Mkdirs on file or folder without permission "
    				+ "on the remote system should throw RemoteDataException");
        }
    	catch (IOException e) {
    		Assert.fail("Mkdirs on file or folder without permission "
    				+ "on the remote system should throw RemoteDataException", e);
    	}
    	catch (RemoteDataException e)
    	{
    		Assert.assertTrue(true);
        }
	}


}
