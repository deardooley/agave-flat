/**
 *
 */
package org.iplantc.service.transfer.operations;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.transfer.RemoteDataClient;
import org.iplantc.service.transfer.exceptions.RemoteDataException;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Coverage tests for {@link RemoteDataClient#getInputStream()}
 *
 * @author dooley
 *
 */
@Test(singleThreaded=true, groups= {"instream", "download", "integration"})
public class GetInputStreamOperationTest extends BaseRemoteDataClientOperationTest {

    public GetInputStreamOperationTest(String systemJsonFilePath,
            TransferOperationAfterMethod afterMethodTeardown,
            TransferOperationBeforeMethod beforeMethodSetup,
            ForbiddenPathProvider forbiddenPathProvider)
    {
        super(systemJsonFilePath, afterMethodTeardown, beforeMethodSetup, forbiddenPathProvider);
    }

    private static final Logger log = Logger.getLogger(GetInputStreamOperationTest.class);

    @DataProvider(name="getInputStreamProvider", parallel=false)
    protected Object[][] getInputStreamProvider()
    {
        return new Object[][] {
                { "", "", true, "empty localfile to get should throw exception." },
                { null, "", true, "null localfile to get should throw exception." },
                { "", null, true, "null remotedir to get should throw exception." },
                { LOCAL_TXT_FILE, MISSING_DIRECTORY, true, "get on missing remote file should throw exception." },
                { LOCAL_TXT_FILE, "", false, "get local file from remote home directory should succeed." },
        };
    }

    @Test(dataProvider="getInputStreamProvider")
    public void getInputStream(String localFile, String remotedir, boolean shouldThrowException, String message)
    {
        _getInputStream(localFile, remotedir, shouldThrowException, message);
    }

    protected void _getInputStream(String localFile, String remotedir, boolean shouldThrowException, String message)
    {
        boolean actuallyThrewException = false;
        InputStream in = null;
        BufferedOutputStream bout = null;
        String remotePutPath = null;
        try
        {
            getClient().put(localFile, remotedir);
            if (StringUtils.isEmpty(remotedir)) {
                remotePutPath = FilenameUtils.getName(localFile);
            } else {
                remotePutPath = remotedir + "/" + FilenameUtils.getName(localFile);
            }
            String localGetPath = getLocalDownloadDir() + "/" + FilenameUtils.getName(remotePutPath);
            Assert.assertTrue(getClient().doesExist(remotePutPath), "Data not found on remote system after put.");

            in = getClient().getInputStream(remotePutPath, true);
            File downloadfile = new File(localGetPath);
            if (!org.codehaus.plexus.util.FileUtils.fileExists(downloadfile.getParent())) {
                FileUtils.getFile(downloadfile.getParent()).mkdirs();
            }
            bout = new BufferedOutputStream(new FileOutputStream(downloadfile));

            int bufferSize = getClient().getMaxBufferSize();
            byte[] b = new byte[bufferSize];
            int len = 0;

            while ((len = in.read(b)) > -1) {
                bout.write(b, 0, len);
            }

            bout.flush();

            Assert.assertTrue(org.codehaus.plexus.util.FileUtils.fileExists(localGetPath), "Data not found on local system after get.");

        }
        catch (Exception e) {
            actuallyThrewException = true;
            if (!shouldThrowException) e.printStackTrace();
        }
        finally {
            try { in.close(); } catch (Exception e) {}
            try { bout.close(); } catch (Exception e) {}
        }

        Assert.assertEquals(actuallyThrewException, shouldThrowException, message);
    }

    @Test(dependsOnMethods= {"getInputStream"})
    public void getInputStreamThrowsExceptionWhenNoPermission()
    {
        _getInputStreamThrowsExceptionWhenNoPermission();
    }

    protected void _getInputStreamThrowsExceptionWhenNoPermission()
    {
        try
        {
            getClient().getInputStream(getForbiddenDirectoryPath(true), true);
            Assert.fail("getInputStream should throw RemoteDataException on no permissions");
        }
        catch (RemoteDataException e) {
            Assert.assertTrue(true);
        }
        catch (Exception e) {
            Assert.fail("getInputStream should throw RemoteDataException on no permissions", e);
        }
    }

}
