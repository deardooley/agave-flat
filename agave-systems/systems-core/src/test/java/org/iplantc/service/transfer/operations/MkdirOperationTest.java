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
@Test(singleThreaded=true, groups= {"mkdir"})
public class MkdirOperationTest extends BaseRemoteDataClientOperationTest {

    public MkdirOperationTest(String systemJsonFilePath,
            TransferOperationAfterMethod afterMethodTeardown,
            TransferOperationBeforeMethod beforeMethodSetup,
            ForbiddenPathProvider forbiddenPathProvider)
    {
        super(systemJsonFilePath, afterMethodTeardown, beforeMethodSetup, forbiddenPathProvider);
    }

    private static final Logger log = Logger.getLogger(MkdirOperationTest.class);

    @DataProvider(name="mkdirProvider", parallel=false)
    protected Object[][] mkdirProvider()
    {
        return new Object[][] {
            { null, true, false, "mkdir on null name should resolve to user home and not throw an exception." },
            { "", true, false, "mkdir on empty name should resolve to user home and not throw an exception." },
            { "deleteme-"+System.currentTimeMillis(), false, false, "mkdir new directory in current folder should not fail." },
            { MISSING_DIRECTORY, false, true, "mkdir when parent does not exist should throw exception." },
        };
    }

    @Test(dataProvider="mkdirProvider")
    public void mkdir(String remotedir, boolean shouldReturnFalse, boolean shouldThrowException, String message)
	{
	    _mkdir(remotedir, shouldReturnFalse, shouldThrowException, message);
	}

	protected void _mkdir(String remotedir, boolean shouldReturnFalse, boolean shouldThrowException, String message)
    {
    	boolean actuallyThrewException = false;

    	try
    	{
    		Assert.assertEquals(getClient().mkdir(remotedir), !shouldReturnFalse, message);

    		if (!shouldReturnFalse) {
    			Assert.assertTrue(getClient().doesExist(remotedir), "Failed to create remote directory");
    		}
        }
    	catch (Exception e) {
        	if (!shouldThrowException) e.printStackTrace();
            actuallyThrewException = true;
        }

    	Assert.assertEquals(actuallyThrewException, shouldThrowException, message);
	}

	@Test(dependsOnMethods={"mkdir"})
    public void mkdirWithoutRemotePermissionThrowsRemoteDataException()
	{
	    _mkdirWithoutRemotePermissionThrowsRemoteDataException();
	}

	protected void _mkdirWithoutRemotePermissionThrowsRemoteDataException()
    {
    	try
    	{
    		getClient().mkdir(getForbiddenDirectoryPath(false));
    		Assert.fail("Mkdir on file or folder without permission "
    				+ "on the remote system should throw RemoteDataException");
        }
    	catch (IOException e) {
    		Assert.fail("Mkdir on file or folder without permission "
    				+ "on the remote system should throw RemoteDataException", e);
    	}
    	catch (RemoteDataException e)
    	{
    		Assert.assertTrue(true);
        }
	}

	@Test(dependsOnMethods={"mkdirWithoutRemotePermissionThrowsRemoteDataException"})
    public void mkdirThrowsRemoteDataExceptionIfDirectoryAlreadyExists()
    {
	    _mkdirThrowsRemoteDataExceptionIfDirectoryAlreadyExists();
    }

	protected void _mkdirThrowsRemoteDataExceptionIfDirectoryAlreadyExists()
	{
        try
        {
            getClient().mkdir("someincrediblylongdirectoryname");
            getClient().mkdir("someincrediblylongdirectoryname");
        }
        catch (Exception e) {
            Assert.fail("Mkdir on existing path should not throw exception.", e);
        }
    }
}
