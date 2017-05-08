/**
 *
 */
package org.iplantc.service.transfer.operations;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.OutputStream;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.transfer.RemoteDataClient;
import org.iplantc.service.transfer.exceptions.RemoteDataException;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Coverage tests for {@link RemoteDataClient#getOutputStream()}
 * @author dooley
 *
 */
@Test(groups= {"outstream", "upload", "integration"})
public class GetOutputStreamOperationTest extends BaseRemoteDataClientOperationTest {

    private static final Logger log = Logger.getLogger(GetOutputStreamOperationTest.class);

    public GetOutputStreamOperationTest(String systemJsonFilePath,
            TransferOperationAfterMethod afterMethodTeardown,
            TransferOperationBeforeMethod beforeMethodSetup,
            ForbiddenPathProvider forbiddenPathProvider)
    {
        super(systemJsonFilePath, afterMethodTeardown, beforeMethodSetup, forbiddenPathProvider);
    }

    @Test(groups={"stream","put"})
    public void getOutputStream()
    {
        _getOutputStream();
    }

    protected void _getOutputStream()
    {
        OutputStream out = null;
        BufferedInputStream in = null;
        String remotePath = null;
        try
        {
            remotePath = LOCAL_BINARY_FILE_NAME;
            out = getClient().getOutputStream(remotePath, true, false);
            in = new BufferedInputStream(new FileInputStream(LOCAL_BINARY_FILE));
            Assert.assertTrue(IOUtils.copy(in, out) > 0, "Zero bytes were copied to remote output stream.");
            out.flush();
            in.close();
            out.close();

            Assert.assertTrue(getClient().doesExist(remotePath),
                    "Data not found on remote system after writing via output stream.");

            Assert.assertTrue(getClient().isFile(remotePath),
                    "Data found to be a directory on remote system after writing via output stream.");
        }
        catch (Throwable e) {
            Assert.fail("Writing to output stream threw unexpected exception", e);
        }
        finally {
            try { in.close(); } catch (Exception e) {}
            try { out.close(); } catch (Exception e) {}
        }
    }

    @Test(groups={"stream","put"}, dependsOnMethods={"getOutputStream"})
    public void getOutputStreamFailsWhenRemotePathIsNullOrEmpty()
    {
        _getOutputStreamFailsWhenRemotePathIsNullOrEmpty();
    }

    protected void _getOutputStreamFailsWhenRemotePathIsNullOrEmpty()
    {
        OutputStream out = null;
        try
        {
            out = getClient().getOutputStream(null, true, false);
            Assert.fail("null remotedir to getOutputStream should throw exception.");
        }
        catch (Exception e) {
            Assert.assertTrue(true);
        }
        finally {
            try { out.close(); } catch (Exception e) {}
        }

        try
        {
            out = getClient().getOutputStream("", true, false);
            Assert.fail("empty remotedir to getOutputStream should throw exception.");
        }
        catch (Exception e) {
            Assert.assertTrue(true);
        }
        finally {
            try { out.close(); } catch (Exception e) {}
        }
    }

    @Test(groups={"stream","put"}, dependsOnMethods={"getOutputStreamFailsWhenRemotePathIsNullOrEmpty"})
    public void getOutputStreamFailsWhenRemotePathIsDirectory()
    {
        _getOutputStreamFailsWhenRemotePathIsDirectory();
    }

    protected void _getOutputStreamFailsWhenRemotePathIsDirectory()
    {
        OutputStream out = null;
        try
        {
            getClient().mkdir(LOCAL_DIR_NAME);
            out = getClient().getOutputStream(LOCAL_DIR_NAME, true, false);
            Assert.fail("Passing valid directory to getOutputStream should throw exception.");
        }
        catch (Exception e) {
            Assert.assertTrue(true);
        }
        finally {
            try { out.close(); } catch (Exception e) {}
        }

        try
        {
            out = getClient().getOutputStream("", true, false);
            Assert.fail("empty remotedir to getOutputStream should throw exception.");
        }
        catch (Exception e) {
            Assert.assertTrue(true);
        }
        finally {
            try { out.close(); } catch (Exception e) {}
        }
    }

    @Test(groups={"stream","put"}, dependsOnMethods={"getOutputStreamFailsWhenRemotePathIsDirectory"})
    public void getOutputStreamFailsOnMissingPath()
    {
        _getOutputStreamFailsOnMissingPath();
    }

    protected void _getOutputStreamFailsOnMissingPath()
    {
        try
        {
            getClient().getOutputStream(MISSING_DIRECTORY, true, false);
            Assert.fail("getOutputStream should throw FileNotFoundException on missing directory");
        }
        catch (FileNotFoundException e) {
            Assert.assertTrue(true);
        }
        catch (Exception e) {
            Assert.fail("getOutputStream should throw FileNotFoundException on missing directory", e);
        }
    }

    @Test(groups={"stream","put"}, dependsOnMethods={"getOutputStreamFailsOnMissingPath"})
    public void getOutputStreamThrowsExceptionWhenNoPermission()
    {
        _getOutputStreamThrowsExceptionWhenNoPermission();
    }

    protected void _getOutputStreamThrowsExceptionWhenNoPermission()
    {
        try
        {
            getClient().getOutputStream(getForbiddenDirectoryPath(true), true, false);
            Assert.fail("getOutputStream should throw RemoteDataException on no permissions");
        }
        catch (RemoteDataException e) {
            Assert.assertTrue(true);
        }
        catch (Exception e) {
            Assert.fail("getOutputStream should throw RemoteDataException on no permissions",e);
        }
    }

}
