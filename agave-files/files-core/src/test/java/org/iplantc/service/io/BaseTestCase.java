/**
 *
 */
package org.iplantc.service.io;

import static org.iplantc.service.systems.model.enumerations.StorageProtocolType.*;
import java.io.File;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URI;
import java.net.URLEncoder;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.iplantc.service.common.persistence.HibernateUtil;
import org.iplantc.service.io.model.JSONTestDataUtil;
import org.iplantc.service.common.dao.TenantDao;
import org.iplantc.service.common.model.Tenant;
import org.iplantc.service.systems.dao.SystemDao;
import org.iplantc.service.systems.exceptions.SystemException;
import org.iplantc.service.systems.manager.SystemManager;
import org.iplantc.service.systems.model.BatchQueue;
import org.iplantc.service.systems.model.ExecutionSystem;
import org.iplantc.service.systems.model.RemoteSystem;
import org.iplantc.service.systems.model.StorageSystem;
import org.iplantc.service.systems.model.enumerations.LoginProtocolType;
import org.iplantc.service.systems.model.enumerations.RemoteSystemType;
import org.iplantc.service.systems.model.enumerations.StorageProtocolType;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

/**
 * @author dooley
 *
 */
public class BaseTestCase {

    public static final BatchQueue shortQueue = new BatchQueue("short", (long)1000, (long)10, (long)1, 16.0, (long)16, "01:00:00", null, true);
    public static final BatchQueue mediumQueue = new BatchQueue("medium", (long)100, (long)10, (long)1, 16.0, (long)16, "12:00:00", null, false);
    public static final BatchQueue longQueue = new BatchQueue("long", (long)10, (long)4, (long)1, 16.0, (long)16, "48:00:00", null, false);
    public static final BatchQueue dedicatedQueue = new BatchQueue("dedicated", (long)1,        (long)1,        (long)1,        16.0,   (long)16,       "144:00:00", null, false);
    public static final BatchQueue unlimitedQueue = new BatchQueue("unlimited", (long)10000000, (long)10000000, (long)10000000, 2048.0, (long)10000000, "999:00:00", null, false);

	public static final String SYSTEM_OWNER = "testuser";
	public static final String SYSTEM_SHARE_USER = "testshareuser";
	public static final String SYSTEM_PUBLIC_USER = "public";
	public static final String SYSTEM_UNSHARED_USER = "testotheruser";
	public static final String SYSTEM_INTERNAL_USERNAME = "test_internal_user";

	public static final String SHARED_SYSTEM_USER = "testshareuser";
	public static final String TEST_PROPERTIES_FILE = "test.properties";
	public static String EXECUTION_SYSTEM_TEMPLATE_DIR = "systems/execution";
	public static String STORAGE_SYSTEM_TEMPLATE_DIR = "systems/storage";
	public static String SOFTWARE_SYSTEM_TEMPLATE_DIR = "software";
	public static String INTERNAL_USER_TEMPLATE_DIR = "internal_users";
	public static String CREDENTIALS_TEMPLATE_DIR = "credentials";

	// standard directories and files for io tests
	protected static String MISSING_DIRECTORY = "I/Do/Not/Exist/unless/some/evil/person/has/deliberately/created/a/ludicrous/directory/structure/just/to/break/this/test";
	protected static String MISSING_FILE = "I/Do/Not/Exist/unless/some/evil/person/has/deliberately/created/a/ludicrous/directory/structure/just/to/break/this/test.txt";
	protected static String LOCAL_DIR = "target/test-classes/transfer";
	protected static String LOCAL_DOWNLOAD_DIR = "target/test-classes/download";
	protected static String LOCAL_TXT_FILE = "target/test-classes/transfer/test_upload.txt";
	protected static String LOCAL_BINARY_FILE = "target/test-classes/transfer/test_upload.bin";

	protected static String SOURCE_DIRNAME = "transfer";
    protected static String DEST_DIRNAME = "transfer.copy";
    protected static String SOURCE_FILENAME = "test_upload.bin";
    protected static String DEST_FILENAME = "test_upload.bin.copy";

	protected JSONTestDataUtil jtd;
	protected JSONObject jsonTree;

	protected String username;
	protected String password;
	protected String uploadFilePath;
	protected File testFileAnalysisDirectory;
	protected String destPath;
	protected URI ftpUri;
	protected URI gridFtpUri;
	protected URI httpUri = URI.create("http://docker.example.com:10080/public/" + SOURCE_FILENAME);
	protected URI httpsUri = URI.create("https://docker.example.com:10443/public/" + SOURCE_FILENAME);
	protected URI s3Uri;
	protected URI sftpUri;

	protected SystemDao systemDao = new SystemDao();

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

		destPath = "/testparentparentparent/testparentparent/testparent/test.dat";

		TenantDao tenantDao = new TenantDao();
		Tenant tenant = tenantDao.findByTenantId("iplantc.org");
		if (tenant == null) {
			tenant = new Tenant("iplantc.org", "https://agave.iplantc.org", "dooley@tacc.utexas.edu", "Test Admin");
			tenantDao.persist(tenant);
		}
	}

	@AfterClass
	protected void afterClass() throws Exception
	{
		clearSystems();
		clearLogicalFiles();
	}

	protected void clearSystems()
    {
	    Session session = null;
        try
        {
            HibernateUtil.beginTransaction();
            session = HibernateUtil.getSession();
            session.clear();
            HibernateUtil.disableAllFilters();

            session.createQuery("DELETE RemoteSystem").executeUpdate();
            session.flush();
        }
        finally
        {
            try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
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

			session.createQuery("DELETE LogicalFile").executeUpdate();
			session.createQuery("DELETE EncodingTask").executeUpdate();
//			session.createQuery("DELETE FROM DecodingTask s WHERE 1=1").executeUpdate();
			session.createQuery("DELETE StagingTask").executeUpdate();
			session.createQuery("DELETE RemoteFilePermission").executeUpdate();
			session.createQuery("DELETE TransferTask").executeUpdate();
			session.createQuery("DELETE Notification").executeUpdate();
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


    /**
     * Create a single {@link StorageSystem} and {@link ExecutionSystem} for
     * basic testing. Subclasses will want to override this class to build
     * a larger permutation matrix of test cases.
     *
     * Templates used for these systems are taken from the
     * {@code src/test/resources/systems/execution/execute.example.com.json} and
     * {@code src/test/resources/systems/storage/storage.example.com.json} files.
     *
     * @throws Exception
     */
    protected void initSystems() throws Exception {
        StorageSystem storageSystem = (StorageSystem) getNewInstanceOfRemoteSystem(RemoteSystemType.STORAGE, "storage");
        storageSystem.setOwner(SYSTEM_OWNER);
        storageSystem.setPubliclyAvailable(true);
        storageSystem.setGlobalDefault(true);
        storageSystem.getUsersUsingAsDefault().add(SYSTEM_OWNER);
        systemDao.persist(storageSystem);

        ExecutionSystem executionSystem =
                (ExecutionSystem) getNewInstanceOfRemoteSystem(RemoteSystemType.EXECUTION, "execute");
        executionSystem.setOwner(SYSTEM_OWNER);
        executionSystem.getBatchQueues().clear();
        executionSystem.addBatchQueue(dedicatedQueue.clone());
        executionSystem.addBatchQueue(longQueue.clone());
        executionSystem.addBatchQueue(mediumQueue.clone());
        executionSystem.addBatchQueue(shortQueue.clone());
        executionSystem.setPubliclyAvailable(true);
        executionSystem.setType(RemoteSystemType.EXECUTION);
        systemDao.persist(executionSystem);
    }

    /**
     * Creates and persists an {@link StorageSystem} for every template
     * with file name matching {@link StorageProtocolType}.example.com.json
     * in the {@code src/test/resources/systems/storage} folder.
     * @throws Exception
     */
    protected void initAllStorageSystems() throws Exception {
        for (StorageProtocolType protocol: StorageProtocolType.values())
        {
            if (protocol == AZURE || protocol == SWIFT) continue;
            StorageSystem storageSystem = (StorageSystem)
                    getNewInstanceOfRemoteSystem(RemoteSystemType.STORAGE, protocol.name());
            storageSystem.setOwner(SYSTEM_OWNER);
            storageSystem.setPubliclyAvailable(true);
            storageSystem.setGlobalDefault(true);
            storageSystem.getUsersUsingAsDefault().add(SYSTEM_OWNER);
            systemDao.persist(storageSystem);
        }
    }

    /**
     * Creates and persists an {@link ExecutionSystem} for every template
     * with file name matching {@link LoginProtocolType}.example.com.json
     * in the {@code src/test/resources/systems/execution} folder.
     * @throws Exception
     */
    protected void initAllExecutionSystems() throws Exception
    {
        for (LoginProtocolType protocol: LoginProtocolType.values())
        {
            ExecutionSystem executionSystem = (ExecutionSystem)
                    getNewInstanceOfRemoteSystem(RemoteSystemType.EXECUTION, protocol.name());
            executionSystem.setOwner(SYSTEM_OWNER);
            executionSystem.getBatchQueues().clear();
            executionSystem.addBatchQueue(dedicatedQueue.clone());
            executionSystem.addBatchQueue(longQueue.clone());
            executionSystem.addBatchQueue(mediumQueue.clone());
            executionSystem.addBatchQueue(shortQueue.clone());
            executionSystem.setPubliclyAvailable(true);
            executionSystem.setType(RemoteSystemType.EXECUTION);
            systemDao.persist(executionSystem);
        }
    }

    /**
     * Reads a new {@link RemoteSystem} of the given {@code type} and {@code protocol}
     * from one of the test templates. The returned {@link RemoteSystem} is unsaved and
     * unaltered from the original template.
     *
     * @param type
     * @param protocol valid {@link StorageProtocolType} or {@link LoginProtocolType} value
     * @return unaltered test template {@link RemoteSystem}
     * @throws Exception
     */
    protected RemoteSystem getNewInstanceOfRemoteSystem(RemoteSystemType type, String protocol)
    throws Exception
    {
        return getNewInstanceOfRemoteSystem(type, protocol, null);
    }

    /**
     * Reads a new {@link RemoteSystem} of the given {@code type} and {@code protocol}
     * from one of the test templates. The returned {@link RemoteSystem} is unsaved and
     * unaltered from the original template.
     *
     * @param type
     * @param protocol valid {@link StorageProtocolType} or {@link LoginProtocolType} value
     * @param systemId custom systemid to assign to the new system
     * @return unaltered test template {@link RemoteSystem}
     * @throws Exception
     */
    protected RemoteSystem getNewInstanceOfRemoteSystem(RemoteSystemType type, String protocol, String systemId)
    throws Exception
    {
        SystemManager systemManager = new SystemManager();
        JSONObject json = null;
        if (type == RemoteSystemType.STORAGE) {
            json = jtd.getTestDataObject(STORAGE_SYSTEM_TEMPLATE_DIR +
                    File.separator + protocol.toLowerCase() + ".example.com.json");
        }
        else {
            json = jtd.getTestDataObject(EXECUTION_SYSTEM_TEMPLATE_DIR +
                    File.separator + protocol.toLowerCase() + ".example.com.json");
        }

        if (StringUtils.isNotEmpty(systemId)) {
            json.put("id", systemId);
        }

        return systemManager.parseSystem(json, SYSTEM_OWNER, null);
    }

    /**
     * Returns a new, unsaved {@link StorageProtocolType#SFTP} {@link StorageSystem}.
     * @return
     * @throws Exception
     */
    protected StorageSystem getNewInstanceOfStorageSystem()
    throws Exception
    {
        return (StorageSystem)getNewInstanceOfRemoteSystem(RemoteSystemType.STORAGE, StorageProtocolType.SFTP.name());
    }

    /**
     * Returns a new, unsaved {@link StorageProtocolType#SFTP} {@link StorageSystem}.
     * @param systemId the custom systemId to assign to the new system
     * @return
     * @throws Exception
     */
    protected StorageSystem getNewInstanceOfStorageSystem(String systemId)
    throws Exception
    {
        return (StorageSystem)getNewInstanceOfRemoteSystem(RemoteSystemType.STORAGE, StorageProtocolType.SFTP.name(), systemId);
    }

    /**
     * Returns a new, unsaved {@link StorageProtocolType#SSH} {@link ExecutionSystem}.
     * @return
     * @throws Exception
     */
    protected ExecutionSystem getNewInstanceOfExecutionSystem()
    throws Exception
    {
        return (ExecutionSystem)getNewInstanceOfRemoteSystem(RemoteSystemType.EXECUTION, LoginProtocolType.SSH.name());
    }

    /**
     * Returns a new, unsaved {@link StorageProtocolType#SFTP} {@link ExecutionSystem}.
     * @param systemId the custom systemId to assign to the new system
     * @return
     * @throws Exception
     */
    protected ExecutionSystem getNewInstanceOfExecutionSystem(String systemId)
    throws Exception
    {
        return (ExecutionSystem)getNewInstanceOfRemoteSystem(RemoteSystemType.EXECUTION, LoginProtocolType.SSH.name(), systemId);
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
