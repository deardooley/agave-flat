/**
 *
 */
package org.iplantc.service.transfer;

import java.io.File;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URI;
import java.net.URLEncoder;
import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.iplantc.service.common.persistence.HibernateUtil;
import org.iplantc.service.systems.exceptions.SystemException;
import org.iplantc.service.systems.model.JSONTestDataUtil;
import org.iplantc.service.systems.model.StorageConfig;
import org.iplantc.service.systems.model.StorageSystem;
import org.json.JSONObject;
import org.testng.annotations.BeforeClass;

/**
 * @author dooley
 *
 */
public class BaseTransferTestCase {

	public static final String SYSTEM_USER = "testuser";
	public static final String SHARED_SYSTEM_USER = "testshareuser";
	public static final String TEST_PROPERTIES_FILE = "test.properties";
	public static String EXECUTION_SYSTEM_TEMPLATE_DIR = "target/test-classes/systems/execution";
	public static String STORAGE_SYSTEM_TEMPLATE_DIR = "target/test-classes/systems/storage";
	public static String SOFTWARE_SYSTEM_TEMPLATE_DIR = "target/test-classes/software";
	public static String INTERNAL_USER_TEMPLATE_DIR = "target/test-classes/internal_users";
	public static String CREDENTIALS_TEMPLATE_DIR = "target/test-classes/credentials";

	// standard directories and files for io tests
	protected static String MISSING_DIRECTORY = "I/Do/Not/Exist/unless/some/evil/person/has/this/test";
	protected static String MISSING_FILE = "I/Do/Not/Exist/unless/some/evil/person/this/test.txt";
	protected static String LOCAL_DIR = "target/test-classes/transfer";
	protected static String LOCAL_DOWNLOAD_DIR = "target/test-classes/download";
	protected static String LOCAL_TXT_FILE = "target/test-classes/transfer/test_upload.txt";
	protected static String LOCAL_BINARY_FILE = "target/test-classes/transfer/test_upload.bin";
	
	protected static String LOCAL_DIR_NAME = "transfer";
	protected static String LOCAL_TXT_FILE_NAME = "test_upload.txt";
	protected static String LOCAL_BINARY_FILE_NAME = "test_upload.bin";

	protected RemoteDataClient client;
	protected StorageConfig storageConfig;
    protected StorageSystem system;
    protected String credential;
    protected String salt;

	protected JSONTestDataUtil jtd;
	protected JSONObject jsonTree;

	protected String username;
	protected String password;
	protected String uploadFilePath;
	protected File testFileAnalysisDirectory;
	protected String destPath;
	protected URI ftpUri;
	protected URI gridFtpUri;
	protected URI httpUri;
	protected URI httpsUri;
	protected URI s3Uri;
	protected URI sftpUri;

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	@BeforeClass
	protected void beforeClass() throws Exception
	{
		jtd = JSONTestDataUtil.getInstance();

		Properties props = new Properties();
		props.load(getClass().getClassLoader().getResourceAsStream(TEST_PROPERTIES_FILE));

		username = (String)props.getProperty("test.iplant.username");
		password = (String)props.getProperty("test.iplant.password");
		String urlencodedcredentials = URLEncoder.encode(username, "utf-8") + ":" + URLEncoder.encode(password, "utf-8");

		uploadFilePath = (String)props.getProperty("test.file.path");
		testFileAnalysisDirectory = new File((String)props.getProperty("test.file.analysis.dir.path"));
		//ftpUri = new URI("ftp://" + urlencodedcredentials + "@" + (String)props.getProperty("test.ftp.uri")));
		gridFtpUri = new URI("gsiftp://" + urlencodedcredentials + "@" + (String)props.getProperty("test.gridftp.uri"));
		httpUri = new URI("https://" + urlencodedcredentials + "@" + (String)props.getProperty("test.http.uri"));
		httpsUri = new URI("https://" + urlencodedcredentials + "@" + (String)props.getProperty("test.https.uri"));
		s3Uri = new URI((String)props.getProperty("test.s3.uri"));
		sftpUri = new URI("sftp://" + urlencodedcredentials + "@" + (String)props.getProperty("test.sftp.uri"));

		destPath = username + "/test.dat";

	}

	protected void clearSystems() throws Exception {
		Session session = null;
		try
		{
			HibernateUtil.beginTransaction();
			session = HibernateUtil.getSession();
			session.clear();
			HibernateUtil.disableAllFilters();

			session.createQuery("delete RemoteSystem").executeUpdate();
			session.createQuery("delete BatchQueue").executeUpdate();
			session.createQuery("delete StorageConfig").executeUpdate();
			session.createQuery("delete LoginConfig").executeUpdate();
			session.createQuery("delete AuthConfig").executeUpdate();
			session.createQuery("delete SystemRole").executeUpdate();
			session.createQuery("delete CredentialServer").executeUpdate();
		}
		catch (HibernateException ex)
		{
			throw new SystemException(ex);
		}
		finally
		{
			try { session.close(); } catch (Exception e) {}
		}
	}

	protected void clearLogicalFiles() throws Exception {
		Session session = null;
		try
		{
			HibernateUtil.beginTransaction();
			session = HibernateUtil.getSession();
			session.clear();
			HibernateUtil.disableAllFilters();

//			session.createQuery("delete LogicalFile").executeUpdate();
//			session.createQuery("delete EncodingTask").executeUpdate();
//			session.createQuery("delete DecodingTask").executeUpdate();
//			session.createQuery("delete StagingTask").executeUpdate();
			session.createQuery("delete RemoteFilePermission").executeUpdate();
		}
		catch (HibernateException ex)
		{
			throw new SystemException(ex);
		}
		finally
		{
			try { session.close(); } catch (Exception e) {}
		}
	}

	protected void clearTransferTasks() throws Exception {
		Session session = null;
		try
		{
			HibernateUtil.beginTransaction();
			session = HibernateUtil.getSession();
			session.clear();
			HibernateUtil.disableAllFilters();
			
			session.createQuery("delete TransferTask").executeUpdate();
		}
		catch (HibernateException ex)
		{
			throw new SystemException(ex);
		}
		finally
		{
			try { session.close(); } catch (Exception e) {}
		}
	}

	protected String getMacAddress() throws Exception
	{
		InetAddress ip = InetAddress.getLocalHost();
		System.out.println("Current IP address : " + ip.getHostAddress());

        // For some reason this returns null for me on the VM. Rather than worry about debugging a test,
        // I am just returning the ip address. While there are just 2 of us testing this, we should be safe
        // for now
        // Todo Investigate this more thoroughly and get it working
		NetworkInterface network = NetworkInterface.getByInetAddress(ip);

        if (network != null) {
            byte[] mac = network.getHardwareAddress();

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < mac.length; i++) {
                sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : ""));
            }
            return sb.toString();
        } else {
             return ip.toString();
        }
	}

}
