/**
 *
 */
package org.iplantc.service.transfer.operations;

import java.io.FileNotFoundException;

import org.apache.log4j.Logger;
import org.iplantc.service.transfer.RemoteDataClient;
import org.iplantc.service.transfer.exceptions.RemoteDataException;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Coverage tests for {@link RemoteDataClient#doRename}
 * @author dooley
 *
 */
@Test(singleThreaded=true, groups= {"rename","integration"})
public class DoRenameOperationTest extends BaseRemoteDataClientOperationTest {

    private static final Logger log = Logger.getLogger(DoRenameOperationTest.class);

    public DoRenameOperationTest(String systemJsonFilePath,
            TransferOperationAfterMethod afterMethodTeardown,
            TransferOperationBeforeMethod beforeMethodSetup,
            ForbiddenPathProvider forbiddenPathProvider)
    {
        super(systemJsonFilePath, afterMethodTeardown, beforeMethodSetup, forbiddenPathProvider);
    }
    
    @DataProvider(name="doRenameProvider", parallel=false)
    protected Object[][] doRenameProvider()
    {
        return new Object[][] {
            { null, LOCAL_DIR_NAME, true, "null oldpath should resolve to home and throw an exception while trying to rename into its own subtree." },
            { LOCAL_DIR_NAME, null, true, "null newpath should resolve to home and throw an exception wile trying to rename into its own parent." },
            { LOCAL_DIR_NAME, "foo", false, "rename should work for valid file names" },
            { LOCAL_DIR_NAME, LOCAL_DIR_NAME, true, "Renaming file or directory to the same name should throw an exception" },
        };
    }

    @Test(dataProvider="doRenameProvider")
    public void doRename(String oldpath, String newpath, boolean shouldThrowException, String message)
    {
        _doRename(oldpath, newpath, shouldThrowException, message);
    }

    protected void _doRename(String oldpath, String newpath, boolean shouldThrowException, String message)
    {
        boolean actuallyThrewException = false;

        try
        {
            getClient().mkdirs(oldpath);
            Assert.assertTrue(getClient().doesExist(oldpath), "Source folder not created for rename test.");

            getClient().doRename(oldpath, newpath);
            Assert.assertTrue(getClient().doesExist(newpath), "Rename operation did not rename the file or directory to " + newpath);

        }
        catch (Exception e) {
            actuallyThrewException = true;
            if (!shouldThrowException)
                Assert.fail(message, e);
        }

        Assert.assertEquals(actuallyThrewException, shouldThrowException, message);
    }

    @Test(dependsOnMethods= {"doRename"})
    public void doRenameThrowsRemotePermissionExceptionToRestrictedSource()
    {
        _doRenameThrowsRemotePermissionExceptionToRestrictedSource();
    }

    protected void _doRenameThrowsRemotePermissionExceptionToRestrictedSource()
    {
        try
        {
            getClient().doRename(getForbiddenDirectoryPath(true), "foo");

            Assert.fail("Rename a restricted remote path to a dest path should throw RemoteDataException.");
        }
        catch (RemoteDataException e) {
            Assert.assertTrue(true);
        }
        catch (Exception e) {
            Assert.fail("Rename a restricted remote path to a dest path should throw RemoteDataException.", e);
        }
    }

    @Test(dependsOnMethods= {"doRenameThrowsRemotePermissionExceptionToRestrictedSource"})
    public void doRenameThrowsRemotePermissionExceptionToRestrictedDest()
    {
        _doRenameThrowsRemotePermissionExceptionToRestrictedDest();
    }

    protected void _doRenameThrowsRemotePermissionExceptionToRestrictedDest()
    {
        try
        {
            getClient().mkdir("foo");
            getClient().doRename("foo", getForbiddenDirectoryPath(true));

            Assert.fail("Rename a remote path to a dest path the user does not have "
                    + "permission to access should throw RemoteDataException.");
        }
        catch (RemoteDataException e) {
            Assert.assertTrue(true);
        }
        catch (Exception e) {
            Assert.fail("Rename a remote path to a dest path the user does not have "
                    + "permission to access should throw RemoteDataException.", e);
        }
    }

    @Test(dependsOnMethods= {"doRenameThrowsRemotePermissionExceptionToRestrictedDest"})
    public void doRenameThrowsFileNotFoundExceptionOnMissingDestPath()
    {
        _doRenameThrowsFileNotFoundExceptionOnMissingDestPath();
    }

    protected void _doRenameThrowsFileNotFoundExceptionOnMissingDestPath()
    {
        try
        {
            getClient().mkdir("foo");
            getClient().doRename("foo", MISSING_DIRECTORY);

            Assert.fail("Rename a remote source path to a dest path that does not exist should throw FileNotFoundException.");
        }
        catch (FileNotFoundException e) {
            Assert.assertTrue(true);
        }
        catch (Exception e)
        {
            Assert.fail("Rename a remote source path to a dest path that does not exist should throw FileNotFoundException.", e);
        }
    }

    @Test(dependsOnMethods= {"doRenameThrowsFileNotFoundExceptionOnMissingDestPath"})
    public void doRenameThrowsFileNotFoundExceptionOnMissingSourcePath()
    {
        _doRenameThrowsFileNotFoundExceptionOnMissingSourcePath();
    }

    protected void _doRenameThrowsFileNotFoundExceptionOnMissingSourcePath()
    {
        try
        {
            getClient().doRename(MISSING_DIRECTORY, "foo");

            Assert.fail("Rename a remote source path that does not exist should throw FileNotFoundException.");
        }
        catch (FileNotFoundException e) {
            Assert.assertTrue(true);
        }
        catch (Exception e)
        {
            Assert.fail("Rename a remote source path that does not exist should throw FileNotFoundException.", e);
        }
    }

}
