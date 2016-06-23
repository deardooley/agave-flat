package org.iplantc.service.io.queue;

import java.io.IOException;
import java.net.URI;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.data.transform.FileTransform;
import org.iplantc.service.io.BaseTestCase;
import org.iplantc.service.io.Settings;
import org.iplantc.service.io.dao.LogicalFileDao;
import org.iplantc.service.io.dao.QueueTaskDao;
import org.iplantc.service.io.exceptions.RemoteCopyException;
import org.iplantc.service.io.model.EncodingTask;
import org.iplantc.service.io.model.LogicalFile;
import org.iplantc.service.io.model.enumerations.StagingTaskStatus;
import org.iplantc.service.io.model.enumerations.TransformTaskStatus;
import org.iplantc.service.systems.dao.SystemDao;
import org.iplantc.service.systems.exceptions.SystemArgumentException;
import org.iplantc.service.systems.model.RemoteSystem;
import org.iplantc.service.systems.model.StorageSystem;
import org.iplantc.service.systems.model.SystemRole;
import org.iplantc.service.systems.model.enumerations.RemoteSystemType;
import org.iplantc.service.systems.model.enumerations.RoleType;
import org.iplantc.service.systems.model.enumerations.StorageProtocolType;
import org.iplantc.service.transfer.RemoteDataClient;
import org.iplantc.service.transfer.RemoteDataClientFactory;
import org.iplantc.service.transfer.URLCopy;
import org.iplantc.service.transfer.http.HTTP;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test(singleThreaded=true)
public class TransformJobTest extends BaseTestCase {

	private LogicalFile file;
	private EncodingTask task;
	private RemoteDataClient remoteClient;
	
	private SystemDao systemDao = new SystemDao();
	
	@BeforeClass
	@Override
	protected void beforeClass() throws Exception {
		super.beforeClass();
		
		initSystems();
		initAllStorageSystems();
		
//		httpUri = new URI("http://docker.example.com:10080/public/test_upload.bin");
	}
	
	@Override
	public void initAllStorageSystems() throws Exception
	{
	    super.initAllStorageSystems();
	    
	    for(StorageSystem system: systemDao.getAllStorageSystems()) {
	        if (system.getStorageConfig().getProtocol() == StorageProtocolType.SFTP) {
	            system.setGlobalDefault(true);
	            system.setPubliclyAvailable(true);
	        }
//	        system.addRole(new SystemRole(SYSTEM_OWNER, RoleType.ADMIN));
//            system.addRole(new SystemRole(SYSTEM_SHARE_USER, RoleType.ADMIN));
//            system.addRole(new SystemRole(SYSTEM_UNSHARED_USER, RoleType.ADMIN));
            systemDao.persist(system);
	    }
	}
	
//	private StorageSystem initSystem(String type, boolean setDefault, boolean setPublic) 
//	throws JSONException, IOException, SystemArgumentException 
//	{
//	    try {
//    		String systemFile = STORAGE_SYSTEM_TEMPLATE_DIR + "/" + StringUtils.lowerCase(type) + ".example.com.json";
//    		JSONObject systemJson = jtd.getTestDataObject(systemFile);
//    		systemJson.remove("id");
//    		systemJson.put("id", this.getClass().getSimpleName() + "." + type + (setPublic? ".public" : ""));
//    		StorageSystem system = (StorageSystem) StorageSystem.fromJSON(systemJson);
//    		system.setPubliclyAvailable(setPublic);
//    		system.setGlobalDefault(setDefault);
//    		system.setOwner(SYSTEM_OWNER);
//    		String testHomeDir = (StringUtils.isEmpty(system.getStorageConfig().getHomeDir()) ? "" : system.getStorageConfig().getHomeDir());
//    		system.getStorageConfig().setHomeDir( testHomeDir + "/" + this.getClass().getSimpleName() + "-staging");
//    		systemDao.persist(system);
//    		
//    		system.addRole(new SystemRole(SYSTEM_OWNER, RoleType.ADMIN));
//    		system.addRole(new SystemRole(SYSTEM_SHARE_USER, RoleType.ADMIN));
//    		system.addRole(new SystemRole(SYSTEM_UNSHARED_USER, RoleType.ADMIN));
//    		system.addRole(new SystemRole(system.getStorageConfig().getDefaultAuthConfig().getUsername(), RoleType.ADMIN));
//    		system.addRole(new SystemRole(Settings.COMMUNITY_USERNAME, RoleType.ADMIN));
//    		systemDao.persist(system);
//    		return system;
//	    }
//	    catch (Throwable t) {
//	        t.printStackTrace();
//	        throw t;
//	    }
//	}
	
	@AfterMethod
	protected void afterMethod() 
	{
		try { LogicalFileDao.remove(file); } catch (Exception e) {}
		try { QueueTaskDao.remove(task); } catch (Exception e) {}

//		// delete the remote file after each test so we don't get a false positive
//        try { remoteClient.delete(file.getAgaveRelativePathFromAbsolutePath());} catch (Exception e) {} 
//        try { remoteClient.disconnect();} catch (Exception e){}
	}

	@Test
	public void testEncodingJobCalledWhenEmpty() 
	{
		// no task is in the queue
		try 
		{
			EncodingJob encodingJob = new EncodingJob();
			encodingJob.execute(null);
		} 
		catch (Exception e) 
		{
			Assert.fail("No queued transforms should return without exception", e);
		}
	}
	
	@Test(dependsOnMethods={"testEncodingJobCalledWhenEmpty"})
	public void testTransformNextJobRawHandle() 
	{
		try 
		{
			// load the db with dummy records to stage a http-accessible file
		    EncodingTask encodingTask = createTransformTaskForUrl(httpUri);
			
		    EncodingJob transformJob = new EncodingJob();
		    transformJob.setQueueTask(encodingTask);
			transformJob.doExecute();
			
			task = QueueTaskDao.getTransformTaskByCallBackKey(task.getCallbackKey());
			
			Assert.assertEquals(task.getStatus(), TransformTaskStatus.TRANSFORMING_COMPLETED,
					"task status not updated after raw transform. expected TRANSFORMING_COMPLETED, found " + task.getStatus());
			
			file = task.getLogicalFile();
			
			Assert.assertEquals(file.getStatus(), TransformTaskStatus.TRANSFORMING_COMPLETED.name(), 
					"file status not updated after raw transform. expected TRANSFORMING_COMPLETED, found " + file.getStatus());
		} 
		catch (Exception e) 
		{
			Assert.fail("File staging should not throw an exception", e);
		}
	}
	
	private EncodingTask createTransformTaskForUrl(URI sUri) throws Exception 
	{
		RemoteDataClient destClient = null;
		StorageSystem defaultStorageSystem = (StorageSystem)systemDao.getGlobalDefaultSystemForTenant(RemoteSystemType.STORAGE, TenancyHelper.getCurrentTenantId());
		
		try
		{
			
			destClient = defaultStorageSystem.getRemoteDataClient();
			destClient.authenticate();
			
			try {
				destClient.mkdirs("");
				destClient.put(LOCAL_BINARY_FILE, FilenameUtils.getName(LOCAL_BINARY_FILE));
				Assert.assertTrue(destClient.doesExist(FilenameUtils.getName(LOCAL_BINARY_FILE)), 
						"File not staged to " + defaultStorageSystem.getSystemId());
			} catch (Exception e) {
				Assert.fail("Failed to verify the existence of the test file " 
						+ httpUri + " in " + defaultStorageSystem.getSystemId() + " at " + 
						destClient.resolvePath(FilenameUtils.getName(LOCAL_BINARY_FILE)), e);
			}
			
			file = new LogicalFile(username, defaultStorageSystem, httpUri, destPath);
			file.setPath(destClient.resolvePath(FilenameUtils.getName(LOCAL_BINARY_FILE)));
			file.setStatus(StagingTaskStatus.STAGING_COMPLETED);
			LogicalFileDao.persist(file);
			
			FileTransform rawTransform = FileTransform.getDefault();
			EncodingTask task = new EncodingTask(file, defaultStorageSystem, destPath, destPath, rawTransform.getName(), rawTransform.getEncodingChain().getFirstFilter().getName(), file.getOwner());
		
			QueueTaskDao.persist(task);
			
			return task;
		} 
		finally {
			try { destClient.disconnect();} catch (Exception e){}
		}
	} 
}
