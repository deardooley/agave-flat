/**
 *
 */
package org.iplantc.service.transfer.operations;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;
import org.iplantc.service.transfer.RemoteDataClient;
import org.iplantc.service.transfer.RemoteFileInfo;
import org.iplantc.service.transfer.exceptions.RemoteDataException;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Coverage tests for {@link RemoteDataClient#ls()}
 * @author dooley
 *
 */
@Test(singleThreaded=true, groups= {"ls","integration"})
public class ListingOperationTest extends BaseRemoteDataClientOperationTest {

    private static final Logger log = Logger.getLogger(ListingOperationTest.class);
    
    public ListingOperationTest(String systemJsonFilePath,
            TransferOperationAfterMethod afterMethodTeardown,
            TransferOperationBeforeMethod beforeMethodSetup,
            ForbiddenPathProvider forbiddenPathProvider)
    {
        super(systemJsonFilePath, afterMethodTeardown, beforeMethodSetup, forbiddenPathProvider);
    }

    public void ls()
    {
        _ls();
    }

    protected void _ls()
    {
        String remoteDir = null;
        try
        {
            getClient().put(LOCAL_DIR, "");
            remoteDir = LOCAL_DIR_NAME;
            Assert.assertTrue(getClient().doesExist(remoteDir),
                    "Directory " + remoteDir + " not found on remote system after put.");

            List<RemoteFileInfo> files = getClient().ls(remoteDir);
            List<String> localFiles = Arrays.asList(new File(LOCAL_DIR).list());

            for (RemoteFileInfo file: files)
            {
                if (file.getName().equals(".")) {
                    Assert.fail("Listing should not return current directory in response list.");
                }
                else if (file.getName().equals("..")) {
                    Assert.fail("Listing should not return parent directory in response list.");
                }
                else
                {
                    Assert.assertTrue(localFiles.contains(file.getName()),
                            "Remote file is not present on local file system.");
                }
            }
        }
        catch (Exception e) {
            Assert.fail("Failed to list contents of " + remoteDir, e);
        }
    }

    @Test(dependsOnMethods={"ls"})
    public void lsFailsOnMissingDirectory()
    {
        _lsFailsOnMissingDirectory();
    }

    protected void _lsFailsOnMissingDirectory()
    {
        try
        {
            getClient().ls(MISSING_DIRECTORY);
            Assert.fail("ls should throw exception on missing directory");
        }
        catch (FileNotFoundException e) {
            Assert.assertTrue(true);
        }
        catch (Exception e) {
            Assert.fail("Incorrect error thorwn when listing missing directory.", e);
        }
    }

    @Test(dependsOnMethods={"lsFailsOnMissingDirectory"})
    public void lsThrowsExceptionWhenNoPermission()
    {
        _lsThrowsExceptionWhenNoPermission();
    }

    protected void _lsThrowsExceptionWhenNoPermission()
    {
        try
        {
            getClient().ls(getForbiddenDirectoryPath(true));
            Assert.fail("ls should throw exception on no permissions");
        }
        catch (RemoteDataException e) {
            Assert.assertTrue(true);
        }
        catch (Exception e) {
            Assert.fail("ls should throw exception on no permissions");
        }
    }

}
