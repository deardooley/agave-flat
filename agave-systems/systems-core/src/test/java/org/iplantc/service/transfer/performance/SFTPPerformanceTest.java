/**
 * 
 */
package org.iplantc.service.transfer.performance;

import static org.iplantc.service.systems.model.enumerations.StorageProtocolType.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.zip.Adler32;
import java.util.zip.CheckedInputStream;

import org.apache.commons.io.FileUtils;
import org.iplantc.service.systems.dao.SystemDao;
import org.iplantc.service.systems.model.StorageSystem;
import org.iplantc.service.systems.model.SystemsModelTestCommon;
import org.iplantc.service.systems.model.enumerations.StorageProtocolType;
import org.iplantc.service.transfer.RemoteDataClient;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.google.common.io.Files;

/**
 * @author dooley
 *
 */
@Test(groups= {"performance", "sftp"})
public class SFTPPerformanceTest extends AbstractPerformanceTest {

	@Override
	protected RemoteDataClient getRemoteDataClient() throws Exception {
		return super.createStorageSystem(SFTP);
	}

}
