package org.iplantc.service.jobs.submission;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.iplantc.service.apps.dao.SoftwareDao;
import org.iplantc.service.apps.exceptions.SoftwareException;
import org.iplantc.service.apps.model.Software;
import org.iplantc.service.apps.model.SoftwareInput;
import org.iplantc.service.apps.model.SoftwareParameter;
import org.iplantc.service.common.persistence.HibernateUtil;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.io.util.GrepUtil;
import org.iplantc.service.jobs.dao.JobDao;
import org.iplantc.service.jobs.exceptions.JobException;
import org.iplantc.service.jobs.exceptions.JobProcessingException;
import org.iplantc.service.jobs.managers.JobManager;
import org.iplantc.service.jobs.managers.launchers.JobLauncher;
import org.iplantc.service.jobs.managers.launchers.JobLauncherFactory;
import org.iplantc.service.jobs.model.JSONTestDataUtil;
import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.jobs.model.JobEvent;
import org.iplantc.service.jobs.model.enumerations.JobStatusType;
import org.iplantc.service.jobs.queue.actions.StagingAction;
import org.iplantc.service.jobs.queue.actions.SubmissionAction;
import org.iplantc.service.systems.dao.SystemDao;
import org.iplantc.service.systems.manager.SystemManager;
import org.iplantc.service.systems.model.BatchQueue;
import org.iplantc.service.systems.model.ExecutionSystem;
import org.iplantc.service.systems.model.RemoteSystem;
import org.iplantc.service.systems.model.StorageSystem;
import org.iplantc.service.systems.model.enumerations.LoginProtocolType;
import org.iplantc.service.systems.model.enumerations.RemoteSystemType;
import org.iplantc.service.systems.model.enumerations.StorageProtocolType;
import org.iplantc.service.transfer.RemoteDataClient;
import org.iplantc.service.transfer.RemoteDataClientFactory;
import org.iplantc.service.transfer.dao.TransferTaskDao;
import org.iplantc.service.transfer.model.TransferTask;
import org.iplantc.service.transfer.model.enumerations.TransferStatusType;
import org.joda.time.DateTime;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.io.Files;

public class AbstractJobSubmissionTest {

    public static final BatchQueue shortQueue = new BatchQueue("short", (long)1000, (long)10, (long)1, 16.0, (long)16, "01:00:00", null, true);
    public static final BatchQueue mediumQueue = new BatchQueue("medium", (long)100, (long)10, (long)1, 16.0, (long)16, "12:00:00", null, false);
    public static final BatchQueue longQueue = new BatchQueue("long", (long)10, (long)4, (long)1, 16.0, (long)16, "48:00:00", null, false);
    public static final BatchQueue dedicatedQueue = new BatchQueue("dedicated", (long)1,        (long)1,        (long)1,        16.0,   (long)16,       "144:00:00", null, false);
    public static final BatchQueue unlimitedQueue = new BatchQueue("unlimited", (long)10000000, (long)10000000, (long)10000000, 2048.0, (long)10000000, "999:00:00", null, false);
    
	public static String EXECUTION_SYSTEM_TEMPLATE_DIR = "target/test-classes/systems/execution";
	public static String STORAGE_SYSTEM_TEMPLATE_DIR = "target/test-classes/systems/storage";
	public static String SOFTWARE_SYSTEM_TEMPLATE_DIR = "target/test-classes/software";
	public static String INTERNAL_USER_TEMPLATE_DIR = "target/test-classes/internal_users";
	public static String CREDENTIALS_TEMPLATE_DIR = "target/test-classes/credentials";
	public static String TEST_DATA_DIR = "target/test-classes/transfer";
	public static String TEST_INPUT_FILE = TEST_DATA_DIR + "/test_upload.txt";
	public static String SOFTWARE_WRAPPER_FILE = SOFTWARE_SYSTEM_TEMPLATE_DIR + "/fork-1.0.0/wrapper.sh";
	
	public static final String SYSTEM_OWNER = "testuser";
	public static final String SYSTEM_OWNER_SHARED = "testshareuser";
	
	protected Map<String, Object> jobRequestMap;
	protected JSONTestDataUtil jtd;
	protected Job job;
	protected SystemDao systemDao = new SystemDao();
	protected StorageSystem storageSystem = null;
	protected ExecutionSystem executionSystem = null;
	protected SystemManager systemManager = new SystemManager();
	protected ObjectMapper mapper = new ObjectMapper();
	
	/**
	 * Initalizes the test db and adds the test app 
	 */
	@BeforeClass
	protected void beforeClass() throws Exception {
	    
		clearData();
        clearJobs();
        clearSoftware();
        clearSystems();
	    
        jtd = JSONTestDataUtil.getInstance();
		
	    initSystems();
        initSoftware();
	}
	
	@AfterClass
	protected void afterClass() throws Exception
    {
        clearData();
        clearJobs();
        clearSoftware();
        clearSystems();
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
	    storageSystem = (StorageSystem) getNewInstanceOfRemoteSystem(RemoteSystemType.STORAGE, "storage");
        storageSystem.setOwner(SYSTEM_OWNER);
        storageSystem.setPubliclyAvailable(true);
        storageSystem.setGlobalDefault(true);
        storageSystem.getUsersUsingAsDefault().add(SYSTEM_OWNER);
        systemDao.persist(storageSystem);
        
        executionSystem = 
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
    
    
	/**
     * Creates and persists a {@link Software} object for every {@link ExecutionSystem} and 
     * {@link BatchQueue}.
     * 
     * @throws Exception
     */
    protected void initSoftware() throws Exception 
    {
        JSONObject json = jtd.getTestDataObject(SOFTWARE_SYSTEM_TEMPLATE_DIR + "/system-software.json");
        RemoteSystem deploymentSystem = systemManager.getUserDefaultStorageSystem(SYSTEM_OWNER);
        
        for (ExecutionSystem executionSystem: systemDao.getAllExecutionSystems()) {
            for (BatchQueue q: executionSystem.getBatchQueues()) {
                json.put("executionSystem", executionSystem.getSystemId());
                json.put("deploymentSystem", deploymentSystem.getSystemId());
                Software original = Software.fromJSON(json, SYSTEM_OWNER);
                Software software = original.clone();
                software.setName("test-" + executionSystem.getSystemId() + "-" + q.getName() );
                software.setOwner(SYSTEM_OWNER);
                software.setDefaultQueue(q.getName());
                software.setDefaultMaxRunTime(q.getMaxRequestedTime());
                software.setDefaultMemoryPerNode(q.getMaxMemoryPerNode() == null ? 1024 : q.getMaxMemoryPerNode());
                software.setDefaultNodes(q.getMaxNodes() == null ? 1024 : q.getMaxNodes());
                software.setDefaultProcessorsPerNode(q.getMaxProcessorsPerNode() == null ? 1024 : q.getMaxProcessorsPerNode());
                software.setAvailable(true);
                software.setExecutionType(executionSystem.getExecutionType());
                SoftwareDao.persist(software);
            }
        }
    }
    
   /**
    * Creates the {@link Software#getDeploymentPath()} folder on the
    * {@link Software#getExecutionSystem()} and copies the {@link Software#getExecutablePath()}
    * file to that folder.
    * 
    * @throws Exception
    */
   protected void stageSofwareAssets(Software software) throws Exception {
       RemoteDataClient storageClient = null;
       try {
           storageClient = software.getStorageSystem().getRemoteDataClient();
           storageClient.authenticate();
           storageClient.mkdirs(software.getDeploymentPath());
           storageClient.put(SOFTWARE_WRAPPER_FILE, software.getDeploymentPath());
           Assert.assertTrue(storageClient.doesExist(software.getDeploymentPath() + "/" + software.getExecutablePath()), 
                   "Failed to copy sofware assets to  agave://" + software.getStorageSystem().getSystemId() 
                   + "/" + software.getDeploymentPath() + "/" + software.getExecutablePath());
       } catch (Exception e) {
           Assert.fail("Failed to copy sofware assets to  " + software.getStorageSystem().getSystemId() 
                   + "/" + software.getDeploymentPath() + "/" + software.getExecutablePath(), e);
       } finally {
           storageClient.disconnect();
       }
   }
   
   /**
    * Copies local test data to the location specified as default values
    * for the {@link SoftwareInput} associated with the given {@code software}
    * value.
    * @param software
    * @throws Exception
    */
   protected void stageSoftwareInputDefaultData(Software software) throws Exception {
       RemoteDataClient remoteDataClient = null;
       URI uri = null;
       try {
           for(SoftwareInput input: software.getInputs()) {
               for (Iterator<JsonNode> iter = input.getDefaultValueAsJsonArray().iterator(); iter.hasNext();) {
                   String inputVal = iter.next().asText();
                   uri = URI.create(inputVal);
                   remoteDataClient = new RemoteDataClientFactory().getInstance(SYSTEM_OWNER, null, uri);
                   remoteDataClient.authenticate();
                   remoteDataClient.mkdirs(FilenameUtils.getPathNoEndSeparator(uri.getPath()));
                   remoteDataClient.put(TEST_INPUT_FILE, uri.getPath());
                   Assert.assertTrue(remoteDataClient.doesExist(uri.getPath()), 
                           "Failed to copy software default input file " + TEST_INPUT_FILE 
                           + "to " + uri);
               }
           }
       } catch (Exception e) {
           Assert.fail("Failed to copy software default input file " + TEST_INPUT_FILE 
                   + "to " + uri, e);
       } finally {
           remoteDataClient.disconnect();
       }
   }
   
   /**
    * Copies local test data to the location specified as default values
    * for the {@link SoftwareInput} associated with the given {@code software}
    * value.
    * @param software
    * @throws Exception
    */
   protected void stageJobInputs(Job job) throws Exception {
       try {
           StagingAction staging = new StagingAction(job);
           staging.run();
       } catch (Exception e) {
           Assert.fail("Failed to stage input for job " + job.getUuid(), e);
       } 
   }
   
	/**
	 * Removes all software assests on remote systems for all
	 * persisted {@link Software} objects. Removes all wor
	 * @throws Exception
	 */
	public void clearData() throws Exception {
	    deleteAllSoftwareAssets();
        deleteAllJobWorkDirectories();
        deleteAllJobArchiveDirectories();
	}
	
	/**
     * Removes all software assests on remote systems for all
     * persisted {@link Software} objects. 
     * @throws Exception
     */
	public void deleteAllSoftwareAssets() throws Exception {
	    
        for (Software software: SoftwareDao.getAll())
        {
            deleteSoftwareAssets(software);
        }
	}
	
	/**
	 * Removes software asset for a single {@link Software} object
	 */
	public void deleteSoftwareAssets(Software software) throws Exception {
        // clean up the app data and archive data on the storage sytem
        RemoteDataClient remoteDataClient = null;
        
        try {
            remoteDataClient = software.getStorageSystem().getRemoteDataClient();
            remoteDataClient.authenticate();
            if (remoteDataClient.doesExist(software.getDeploymentPath())) {
                remoteDataClient.delete(software.getDeploymentPath());
                Assert.assertFalse(remoteDataClient.doesExist(software.getDeploymentPath()), 
                        "Failed to delete software deployment path agave://" 
                      + software.getStorageSystem().getSystemId() 
                      + "/" + software.getDeploymentPath() + "after test");
            }
        } catch (Exception e) {
            Assert.fail("Failed to delete software deployment path agave://" 
                    + software.getStorageSystem().getSystemId() 
                    + "/" + software.getDeploymentPath() + " after test", e);
        } finally {
            remoteDataClient.disconnect();
        }
	}
	
	/**
     * Removes all the {@link Job#workPath} on the job's remote {@link ExecutionSystem}
     * @throws Exception
     */
    public void deleteAllJobWorkDirectories() throws Exception {
        
        // for each job run, clean up the work data
        for (Job job: JobDao.getAll()) 
        {   
            deleteJobWorkDirectory(job);
        }
    }
    
    /**
     * Removes a single {@link Job#workPath} on the job {@link ExecutionSystem}
     * 
     * @param job
     * @throws Exception
     */
    public void deleteJobWorkDirectory(Job job) throws Exception {
        RemoteDataClient remoteDataClient = null;
        RemoteSystem executionSystem = systemDao.findBySystemId(job.getSystem());
        
        // system may have been deleted during the test run.
        if (executionSystem == null) return;
        
        try {
            
            remoteDataClient = executionSystem.getRemoteDataClient(job.getInternalUsername());
            remoteDataClient.authenticate();
            if (remoteDataClient.doesExist(job.getWorkPath())) {
                remoteDataClient.delete(job.getWorkPath());
                Assert.assertFalse(remoteDataClient.doesExist(job.getWorkPath()), 
                        "Failed to delete job work directory agave://" 
                                + executionSystem.getSystemId() 
                                + "/" + job.getWorkPath() + " after test");
            }
        } catch (Exception e) {
            Assert.fail("Failed to delete job work directory agave://" 
                    + executionSystem.getSystemId() 
                    + "/" + job.getWorkPath() + " after test", e);
        } finally {
            try {remoteDataClient.disconnect();} catch (Exception e) {}
        }
    }
    
    /**
     * Removes all {@link Job#workPath} on the {@link Job#archiveSystem}
     * @throws Exception
     */
    public void deleteAllJobArchiveDirectories() throws Exception {
    
        // for each job run, clean up the work data and archive folder of it exists.
        for (Job job: JobDao.getAll()) 
        {  
            if (job.isArchiveOutput()) {
                deleteJobArchiveDirectory(job);
            }
        }
    }
    
    /**
     * Removes a single {@link Job#archivePath} on the {@link Job#archiveSystem}
     * 
     * @param job
     * @throws Exception
     */
    public void deleteJobArchiveDirectory(Job job) throws Exception {
        RemoteDataClient remoteDataClient = null;
        RemoteSystem archiveSystem = job.getArchiveSystem();
        
        try 
        {
            remoteDataClient = archiveSystem.getRemoteDataClient(job.getInternalUsername());
            remoteDataClient.authenticate();
            if (remoteDataClient.doesExist(job.getArchivePath())) {
                remoteDataClient.delete(job.getArchivePath());
                Assert.assertFalse(remoteDataClient.doesExist(job.getArchivePath()), 
                        "Failed to delete job work directory agave://" 
                        + archiveSystem.getSystemId() 
                        + "/" + job.getArchivePath() + " after test");
            }
        } catch (Exception e) {
            Assert.fail("Failed to delete job work directory agave://" 
                    + archiveSystem.getSystemId() 
                    + "/" + job.getArchivePath() + " after test", e);
        } finally {
            remoteDataClient.disconnect();
        }
    }

	protected void clearSystems()
	{	
		for(RemoteSystem s: systemDao.getAll()) {
			systemDao.remove(s);
		}
	}
	
	@SuppressWarnings("unchecked")
	protected void clearSoftware() throws Exception
	{
		Session session = null;
		try
		{
			HibernateUtil.beginTransaction();
			session = HibernateUtil.getSession();
			for (Software software: (List<Software>)session.createQuery("from Software").list()) {
				session.delete(software);
			}
			session.flush();
		}
		catch (HibernateException ex) {
			throw new SoftwareException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction();} catch (Exception e) {}
		}
	}
	
	@SuppressWarnings("unchecked")
	protected void clearJobs() throws Exception
	{
		Session session = null;
		try
		{
			HibernateUtil.beginTransaction();
			session = HibernateUtil.getSession();
			session.clear();
			for (Job job: (List<Job>)session.createQuery("from Job").list()) {
				session.delete(job);
			}
			session.flush();
		}
		catch (HibernateException ex) {
			throw new JobException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction();} catch (Exception e) {}
		}
	}

	/**
     * Creates a {@link Job} object with the given {@code status} using a random 
     * {@link Software} object pulled from the apps available to {@code username}. This method 
     * calls the {@link #createJob(JobStatusType, Software, ExecutionSystem, BatchQueue, String, StorageSystem)}
     * method using the {@link ExecutionSytems} and {@link ExecutionSystem#getDefaultQueue()} 
     * from the {@link Software} object.
     * 
     * The returned job has <em>NOT</em> been persisted.
     * 
	 * @param status
	 * @param username if null, {@link #SYSTEM_OWNER} will be used
	 * @return unsaved {@link Job} object
	 * @throws Exception
	 */
	public Job createJob(JobStatusType status, String username)
	throws Exception 
	{   
	    if (StringUtils.isEmpty(username)) username = SYSTEM_OWNER;
	    
	    Software software = SoftwareDao.getUserApps(username, true).get(0);
	    
	    return createJob(status, 
                software, 
                software.getExecutionSystem(), 
                software.getExecutionSystem().getDefaultQueue(), 
                username, 
                (StorageSystem)systemDao.getGlobalDefaultSystemForTenant(RemoteSystemType.STORAGE, TenancyHelper.getCurrentTenantId()));
	}
	
	/**
     * Creates a {@link Job} object with the given {@code status} using the provided
     * {@code software} object pulled from the user's available apps. This method 
     * calls the {@link #createJob(JobStatusType, Software, ExecutionSystem, BatchQueue, String, StorageSystem)}
     * method using the {@link ExecutionSytems} and {@link ExecutionSystem#getDefaultQueue()} 
     * from the {@link Software} object.
     * 
     * The returned job has <em>NOT</em> been persisted.
     * 
     * @param status
     * @param software
     * @param username if null, {@link #SYSTEM_OWNER} will be used
     * @return unsaved {@link Job} object
     * @throws Exception
     */
    public Job createJob(JobStatusType status, Software software, String username)
    throws Exception 
    {
        if (StringUtils.isEmpty(username)) username = SYSTEM_OWNER;
        
        return createJob(status, 
        	            software, 
        	            software.getExecutionSystem(), 
        	            software.getExecutionSystem().getQueue(software.getDefaultQueue()), 
        	            username, 
        	            (StorageSystem)systemDao.getGlobalDefaultSystemForTenant(RemoteSystemType.STORAGE, TenancyHelper.getCurrentTenantId()));
    }
	
	/**
	 * Creates a {@link Job} object from the given parameters. This will essentially walk 
	 * the created job through a full lifecycle up to the given {@code status} value, 
	 * creating all {@link JobEvent} entities that would normally be created by a job
	 * at that {@link JobStatusType}. 
	 * 
	 * The returned job has <em>NOT</em> been persisted.
	 * 
	 * @param status
	 * @param software
	 * @param executionSystem
	 * @param queue
	 * @param username
	 * @param archiveSystem
	 * @return unsaved {@link Job} object
	 * @throws Exception
	 */
	public Job createJob(JobStatusType status, Software software, ExecutionSystem executionSystem, BatchQueue queue, String username, StorageSystem archiveSystem)
    throws Exception {
        
		Job job = new Job();
        job.setName("test-" + executionSystem.getName() + "_" + queue.getName());
        job.setWorkPath(executionSystem.getScratchDir() + "/" + username + "job-" + job.getUuid());
        job.setOwner(username);
        job.setInternalUsername(null);
        
        job.setArchiveOutput(true);
        job.setArchiveSystem(archiveSystem);
        job.setArchivePath(username + "/archive/test-job-999");
        
        job.setSoftwareName(software.getUniqueName());
        job.setSystem(executionSystem.getSystemId());
        job.setBatchQueue(queue.getName());
        job.setMaxRunTime(StringUtils.isEmpty(software.getDefaultMaxRunTime()) ? "00:30:00" : software.getDefaultMaxRunTime());
        job.setMemoryPerNode((software.getDefaultMemoryPerNode() == null) ? (double)1 : software.getDefaultMemoryPerNode());
        job.setNodeCount((software.getDefaultNodes() == null) ? (long)1 : software.getDefaultNodes());
        job.setProcessorsPerNode((software.getDefaultProcessorsPerNode() == null) ? (long)1 : software.getDefaultProcessorsPerNode());
        job.setWorkPath(
                (executionSystem.getScratchDir() == null ? "" :  executionSystem.getScratchDir()) 
                + "/" + job.getOwner() 
                + "/job-" + job.getUuid());
            
        ObjectNode inputs = mapper.createObjectNode();
        for(SoftwareInput swInput: software.getInputs()) {
            inputs.put(swInput.getKey(), swInput.getDefaultValueAsJsonArray());
        }
        job.setInputsAsJsonObject(inputs);
        
        ObjectNode parameters = mapper.createObjectNode();
        for(SoftwareParameter swParameter: software.getParameters()) {
            parameters.put(swParameter.getKey(), swParameter.getDefaultValueAsJsonArray());
        }
        job.setParametersAsJsonObject(parameters);
        int minutesAgoJobWasCreated = RandomUtils.nextInt(360)+1;
        DateTime created = new DateTime().minusMinutes(minutesAgoJobWasCreated+20);
        job.setCreated(created.toDate());
        
        if (JobStatusType.isExecuting(status) || status == JobStatusType.CLEANING_UP) {
            job.setLocalJobId("q." + System.currentTimeMillis());
            job.setSchedulerJobId(job.getUuid());
            job.setStatus(status, status.getDescription());
            
            int minutesAgoJobWasSubmitted = RandomUtils.nextInt(minutesAgoJobWasCreated)+1;
            int minutesAgoJobWasStarted = minutesAgoJobWasSubmitted + RandomUtils.nextInt(minutesAgoJobWasSubmitted);
            job.setSubmitTime(created.plusMinutes(minutesAgoJobWasSubmitted).toDate());
            job.setStartTime(created.plusMinutes(minutesAgoJobWasStarted).toDate());
            
            if (status == JobStatusType.CLEANING_UP) {
                int minutesAgoJobWasEnded =  minutesAgoJobWasStarted + RandomUtils.nextInt(minutesAgoJobWasStarted);
                job.setEndTime(created.plusMinutes(minutesAgoJobWasEnded).toDate());
            }
        } else if (JobStatusType.isFinished(status) || JobStatusType.isArchived(status)) {
            job.setLocalJobId("q." + System.currentTimeMillis());
            job.setSchedulerJobId(job.getUuid());
            job.setStatus(status, status.getDescription());
            
            int minutesAgoJobWasSubmitted = RandomUtils.nextInt(minutesAgoJobWasCreated)+1;
            int minutesAgoJobWasStarted = minutesAgoJobWasSubmitted + RandomUtils.nextInt(minutesAgoJobWasSubmitted);
            int minutesAgoJobWasEnded = minutesAgoJobWasStarted + RandomUtils.nextInt(minutesAgoJobWasStarted);
            job.setSubmitTime(created.plusMinutes(minutesAgoJobWasSubmitted).toDate());
            job.setStartTime(created.plusMinutes(minutesAgoJobWasStarted).toDate());
            job.setEndTime(created.plusMinutes(minutesAgoJobWasEnded).toDate());
            
        } else if (status == JobStatusType.STAGING_INPUTS) {
            
            int minutesAgoJobStartedStaging = RandomUtils.nextInt(minutesAgoJobWasCreated)+1;
            DateTime stagingTime = created.plusMinutes(minutesAgoJobStartedStaging);
            
            for(SoftwareInput input: software.getInputs()) 
            {
                for (Iterator<JsonNode> iter = input.getDefaultValueAsJsonArray().iterator(); iter.hasNext();)
                {
                    String val = iter.next().asText();
                    
                    TransferTask stagingTransferTask = new TransferTask(
                            val, 
                            "agave://" + job.getSystem() + "/" + job.getWorkPath() + "/" + FilenameUtils.getName(URI.create(val).getPath()), 
                            job.getOwner(), 
                            null, 
                            null);
                    stagingTransferTask.setStatus(TransferStatusType.TRANSFERRING);
                    stagingTransferTask.setCreated(stagingTime.toDate());
                    stagingTransferTask.setLastUpdated(stagingTime.toDate());
                    
                    TransferTaskDao.persist(stagingTransferTask);
                    
                    JobEvent event = new JobEvent(
                            JobStatusType.STAGING_INPUTS, 
                            "Copy in progress", 
                            stagingTransferTask, 
                            job.getOwner());
                    event.setCreated(stagingTime.toDate());
                    
                    job.setStatus(JobStatusType.STAGING_INPUTS, event);
                }
            }
            job.setLastUpdated(stagingTime.toDate());
            
        } else if (status == JobStatusType.ARCHIVING) {
            
            DateTime stagingTime = created.plusMinutes(5);
            DateTime stagingEnded = stagingTime.plusMinutes(1);
            DateTime startTime = stagingEnded.plusMinutes(1);
            DateTime endTime = startTime.plusMinutes(1);
            DateTime archiveTime = endTime.plusMinutes(1);
            
            for(SoftwareInput input: software.getInputs()) 
            {
                for (Iterator<JsonNode> iter = input.getDefaultValueAsJsonArray().iterator(); iter.hasNext();)
                {
                    String val = iter.next().asText();
                    
                    TransferTask stagingTransferTask = new TransferTask(
                            val, 
                            "agave://" + job.getSystem() + "/" + job.getWorkPath() + "/" + FilenameUtils.getName(URI.create(val).getPath()), 
                            job.getOwner(), 
                            null, 
                            null);
                    
                    stagingTransferTask.setStatus(TransferStatusType.COMPLETED);
                    stagingTransferTask.setCreated(stagingTime.toDate());
                    stagingTransferTask.setStartTime(stagingTime.toDate());
                    stagingTransferTask.setEndTime(stagingEnded.toDate());
                    stagingTransferTask.setLastUpdated(stagingEnded.toDate());
                    
                    TransferTaskDao.persist(stagingTransferTask);
                    
                    JobEvent event = new JobEvent(
                            JobStatusType.STAGING_INPUTS, 
                            "Staging completed", 
                            stagingTransferTask, 
                            job.getOwner());
                    event.setCreated(stagingTime.toDate());
                    
                    job.setStatus(JobStatusType.STAGING_INPUTS, event);
                }
            }
            
            job.setStatus(JobStatusType.STAGED, JobStatusType.STAGED.getDescription());
            job.setLocalJobId("q." + System.currentTimeMillis());
            job.setSchedulerJobId(job.getUuid());
            job.setStatus(status, status.getDescription());
            
            job.setSubmitTime(stagingEnded.toDate());
            job.setStartTime(startTime.toDate());
            job.setEndTime(endTime.toDate());
            
            TransferTask archivingTransferTask = new TransferTask(
                    "agave://" + job.getSystem() + "/" + job.getWorkPath(),
                    job.getArchiveCanonicalUrl(), 
                    job.getOwner(), 
                    null, 
                    null);
            
            archivingTransferTask.setCreated(archiveTime.toDate());
            archivingTransferTask.setStartTime(archiveTime.toDate());
            TransferTaskDao.persist(archivingTransferTask);
            
            JobEvent event = new JobEvent(
                    JobStatusType.ARCHIVING ,
                    JobStatusType.ARCHIVING.getDescription(), 
                    archivingTransferTask, 
                    job.getOwner());
            event.setCreated(archiveTime.toDate());
            job.setStatus(status, event);
            
            job.setLastUpdated(archiveTime.toDate());
        }
        else {
            job.setStatus(status, status.getDescription());
        }
        
        return job;
    }

	/**
	 * Generic test case for job submission tests. Tries to submit the form, asserts that an 
	 * exception was thrown if shouldThrowExceptino was true.
	 *
     * @param attribute
	 * @param value
	 * @param message
	 * @param shouldThrowException
	 */
	protected void genericJobSubmissionTestCase(String attribute, String value, String message, boolean shouldThrowException) 
	{
		boolean actuallyThrewException = false;
		String exceptionMsg = message;
		
		Map<String, Object> testJobRequestMap = updateJobSubmissionForm(jobRequestMap, attribute, value);
        
		try
        {
        	job = JobManager.processJob(testJobRequestMap, SYSTEM_OWNER, null);
        }
        catch(JobProcessingException se)
        {
        	actuallyThrewException = true;
            exceptionMsg = "Error submitting job: " + message;
            if (actuallyThrewException != shouldThrowException) se.printStackTrace();
        }
		
        System.out.println(" exception thrown?  expected " + shouldThrowException + " actual " + actuallyThrewException);
		Assert.assertTrue(actuallyThrewException == shouldThrowException, exceptionMsg);
		
	}
	
	/**
     * Generic submission test used by all the methods testing job launching in some form or fashion.
     * 
     * @param job
     * @param queuedOrRunning
     * @param message
     * @param shouldThrowException
     */
	protected Job genericRemoteSubmissionTestCase(Job job, boolean queuedOrRunning, String message, boolean shouldThrowException) 
	{
		boolean actuallyThrewException = false;
		String exceptionMsg = message;
		
		SubmissionAction submissionAction = new SubmissionAction(job);
		InputStream in = null;
		try
        {
//		    job.setArchivePath(job.getOwner() + "/archive/jobs/job-" + job.getUuid() + "-" + Slug.toSlug(job.getName()));
//		    
//			JobDao.persist(job);
//			
			submissionAction.run();
            
            job = submissionAction.getJob();
			
            Assert.assertEquals(job.getStatus() == JobStatusType.RUNNING || 
        			job.getStatus() == JobStatusType.QUEUED, queuedOrRunning, 
                    "Job status did not match queued or running after submission");
            if (queuedOrRunning) {
            	Assert.assertNotNull(submissionAction.getJob().getLocalJobId(),
                        "Local job id was not obtained during submission");
                Assert.assertTrue(job.getRetries() == 0,
                        "Job should only submit once when it does not fail during submission");
                Assert.assertNotNull(job.getSubmitTime(),
                    "Job submit time was not updated during job submission");
                RemoteSystem system = JobManager.getJobExecutionSystem(job);
                
                in = system.getRemoteDataClient().getInputStream(job.getOutputPath() + "/.agave.log", false);
                List<String> jobLogs = IOUtils.readLines(in);
                
                Assert.assertFalse(jobLogs.isEmpty(), "Agave log file should not be empty.");
                
                Assert.assertFalse(jobLogs.get(0).contains("error"), "Submission was not written to job log");
                
            } else {
                Assert.assertNull(job.getLocalJobId(),
                        "Local job id should be null if submission fails");
                Assert.assertNull(job.getSubmitTime(),
                    "Job submit time should not be updated if the job did not submit.");
            }
        }
        catch (Exception e)
		{
			actuallyThrewException = true;
            exceptionMsg = "Error placing job into queue on " + job.getSystem() + ": " + message;
            if (actuallyThrewException != shouldThrowException) e.printStackTrace();
		}
		finally {
		    job = submissionAction.getJob();
		    try { in.close(); }catch(Exception e) {}
		}
		
        System.out.println(" exception thrown?  expected " + shouldThrowException + " actual " + actuallyThrewException);
		Assert.assertTrue(actuallyThrewException == shouldThrowException, exceptionMsg);
		
		return job;
		
	}
	
	/**
     * Generic submission test used by all the methods testing job submission is some
     * form or fashion.
     * 
     * @param job
     * @param message
     * @param shouldThrowException
     */
	protected Job genericRemoteSubmissionTestCase(Job job, JobStatusType expectedStatus, String message, boolean shouldThrowException) 
	{
		boolean actuallyThrewException = false;
		String exceptionMsg = message;
		
		SubmissionAction submissionAction = new SubmissionAction(job);
		
		try
        {
//		    job.setArchivePath(job.getOwner() + "/archive/jobs/job-" + job.getUuid() + "-" + Slug.toSlug(job.getName()));
//		    
//			JobDao.persist(job);
//			
			submissionAction.run();
            
            job = submissionAction.getJob();
			
            Assert.assertEquals(job.getStatus(), expectedStatus,
                    "Job status did not match " + expectedStatus + " after submission");
            if (expectedStatus == JobStatusType.RUNNING || 
                    expectedStatus == JobStatusType.QUEUED ) {
                Assert.assertNotNull(submissionAction.getJob().getLocalJobId(),
                        "Local job id was not obtained during submission");
                Assert.assertTrue(job.getRetries() == 0,
                        "Job should only submit once when it does not fail during submission");
                Assert.assertNotNull(job.getSubmitTime(),
                    "Job submit time was not updated during job submission");
            } else {
                Assert.assertNull(job.getLocalJobId(),
                        "Local job id should be null if submission fails");
                Assert.assertNull(job.getSubmitTime(),
                    "Job submit time should not be updated if the job did not submit.");
            }
        }
        catch (Exception e)
		{
			actuallyThrewException = true;
            exceptionMsg = "Error placing job into queue on " + job.getSystem() + ": " + message;
            if (actuallyThrewException != shouldThrowException) e.printStackTrace();
		}
		finally {
		    job = submissionAction.getJob();
		}
		
        System.out.println(" exception thrown?  expected " + shouldThrowException + " actual " + actuallyThrewException);
		Assert.assertTrue(actuallyThrewException == shouldThrowException, exceptionMsg);
		
		return job;
		
	}
	
	protected Job genericRemoteJobLogTestCase(Job job, String regex, boolean shouldMatch, String message) 
	{
		SubmissionAction submissionAction = new SubmissionAction(job);
		RemoteSystem executionSystem = null;
		RemoteDataClient remoteClient = null;
		
		try
        {
			executionSystem = JobManager.getJobExecutionSystem(job);
			remoteClient = executionSystem.getRemoteDataClient();
			String command = submissionAction.getJobLauncher().getStartupScriptCommand(job.getWorkPath());
			List<String> response = org.iplantc.service.common.Settings.fork(command);
			
			submissionAction.run();
			
			do {
				// sleep for 2 seconds giving the job time to complete
				Thread.sleep(2000);
			}
			while (remoteClient.doesExist(job.getWorkPath() + "/.agave.pid"));
            
			GrepUtil grep = new GrepUtil(regex);
			
			boolean found = grep.grep(remoteClient, job.getWorkPath() + "/.agave.log");
			
            Assert.assertEquals(found, shouldMatch, message);
        }
        catch (Exception e)
		{
			Assert.fail("Checking remote logs should not throw an exception", e);
		}
		finally {
		    try { remoteClient.disconnect();; }catch(Exception e) {}
		}
		
		return job;
		
	}
	
//	/**
//     * Generic submission test used by all the methods testing job submission is some
//     * form or fashion.
//     * 
//     * @param job
//     * @param message
//     * @param shouldThrowException
//     */
//	protected Job genericRemoteSubmissionTestCase(Job job, JobStatusType expectedStatus, String message, boolean shouldThrowException) 
//	{
//		boolean actuallyThrewException = false;
//		String exceptionMsg = message;
//		
//		SubmissionAction submissionAction = new SubmissionAction(job);
//		
//		try
//        {
////		    job.setArchivePath(job.getOwner() + "/archive/jobs/job-" + job.getUuid() + "-" + Slug.toSlug(job.getName()));
////		    
////			JobDao.persist(job);
////			
//			submissionAction.run();
//            
//            job = submissionAction.getJob();
//			
//            Assert.assertEquals(job.getStatus(), expectedStatus,
//                    "Job status did not match " + expectedStatus + " after submission");
//            if (expectedStatus == JobStatusType.RUNNING || 
//                    expectedStatus == JobStatusType.QUEUED ) {
//                Assert.assertNotNull(submissionAction.getJob().getLocalJobId(),
//                        "Local job id was not obtained during submission");
//                Assert.assertTrue(job.getRetries() == 0,
//                        "Job should only submit once when it does not fail during submission");
//                Assert.assertNotNull(job.getSubmitTime(),
//                    "Job submit time was not updated during job submission");
//            } else {
//                Assert.assertNull(job.getLocalJobId(),
//                        "Local job id should be null if submission fails");
//                Assert.assertNull(job.getSubmitTime(),
//                    "Job submit time should not be updated if the job did not submit.");
//            }
//        }
//        catch (Exception e)
//		{
//			actuallyThrewException = true;
//            exceptionMsg = "Error placing job into queue on " + job.getSystem() + ": " + message;
//            if (actuallyThrewException != shouldThrowException) e.printStackTrace();
//		}
//		finally {
//		    job = submissionAction.getJob();
//		}
//		
//        System.out.println(" exception thrown?  expected " + shouldThrowException + " actual " + actuallyThrewException);
//		Assert.assertTrue(actuallyThrewException == shouldThrowException, exceptionMsg);
//		
//		return job;
//		
//	}
//	
	/**
	 * Updates the given name value pair in the form and returns a copy of the updated form.
	 * 
	 * @param form
	 * @param name
	 * @param value
	 * @return
	 */
	protected Map<String, Object> updateJobSubmissionForm(Map<String, Object> jobRequestMap, String name, String value)
	{
	    Map<String, Object> newJobRequestMap = new HashMap<String, Object>();
	    newJobRequestMap.putAll(jobRequestMap);
		
	    newJobRequestMap.put(name, value);
		
		return newJobRequestMap;
	}
	
	/**
	 * Verfies the variables were resolved in the template and the callback urls resolved to the 
	 * proper tenant paths.
	 * 
	 * @param job
	 * @param message
	 * @param shouldThrowException
	 */
	protected void genericProcessApplicationTemplate(Job job, String expectedString, String message, boolean shouldThrowException)
	throws Exception 
	{
		JobLauncher launcher = JobLauncherFactory.getInstance(job);
		File ipcexeFile = launcher.processApplicationTemplate();
		String contents = FileUtils.readFileToString(ipcexeFile);
		
		Assert.assertTrue(contents.contains(expectedString), message);
	}
}
