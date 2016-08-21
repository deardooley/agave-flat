/**
 * 
 */
package org.iplantc.service.apps.queue.actions;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.channels.ClosedByInterruptException;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.apps.Settings;
import org.iplantc.service.apps.dao.SoftwareDao;
import org.iplantc.service.apps.exceptions.SoftwareException;
import org.iplantc.service.apps.managers.SoftwareEventProcessor;
import org.iplantc.service.apps.model.Software;
import org.iplantc.service.apps.model.enumerations.SoftwareEventType;
import org.iplantc.service.apps.util.ZipUtil;
import org.iplantc.service.common.dao.TenantDao;
import org.iplantc.service.common.exceptions.DependencyException;
import org.iplantc.service.common.exceptions.DomainException;
import org.iplantc.service.common.exceptions.PermissionException;
import org.iplantc.service.common.model.Tenant;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.common.util.HTMLizer;
import org.iplantc.service.io.dao.LogicalFileDao;
import org.iplantc.service.io.dao.QueueTaskDao;
import org.iplantc.service.io.model.FileEvent;
import org.iplantc.service.io.model.LogicalFile;
import org.iplantc.service.io.model.enumerations.FileEventType;
import org.iplantc.service.io.model.enumerations.StagingTaskStatus;
import org.iplantc.service.notification.util.EmailMessage;
import org.iplantc.service.systems.dao.SystemDao;
import org.iplantc.service.systems.exceptions.RemoteCredentialException;
import org.iplantc.service.systems.exceptions.SystemUnavailableException;
import org.iplantc.service.systems.exceptions.SystemUnknownException;
import org.iplantc.service.systems.manager.SystemManager;
import org.iplantc.service.systems.manager.SystemRoleManager;
import org.iplantc.service.systems.model.ExecutionSystem;
import org.iplantc.service.systems.model.RemoteSystem;
import org.iplantc.service.systems.model.StorageSystem;
import org.iplantc.service.systems.model.enumerations.SystemStatusType;
import org.iplantc.service.transfer.RemoteDataClient;
import org.iplantc.service.transfer.URLCopy;
import org.iplantc.service.transfer.dao.TransferTaskDao;
import org.iplantc.service.transfer.exceptions.RemoteDataException;
import org.iplantc.service.transfer.exceptions.TransferException;
import org.iplantc.service.transfer.local.Local;
import org.iplantc.service.transfer.model.TransferTask;

import com.google.common.io.Files;

/**
 * @author dooley
 *
 */
public class PublishAction extends AbstractWorkerAction<Software> {
    
    private static Logger log = Logger.getLogger(PublishAction.class);
    private Software publishedSoftware;
    private String publishingUsername;
    private String publishedSoftwareName;
    private String publishedSoftwareVersion;
    private ExecutionSystem publishedSoftwareExecutionSystem;
    private String publishedSoftwareExecutionSystemId;
    private SoftwareEventProcessor eventProcessor;
    
    /**
     * Basic constructor for publishing software under same name, version, etc.
     * @param software
     * @param publishingUsername
     */
    public PublishAction(Software software, String publishingUsername) {
        this(software, publishingUsername, software.getName(), software.getVersion(), null);
//        setPublishingUsername(getPublishingUsername());
//        this.publishedSoftwareName = software.getName();
//        this.publishedSoftwareVersion = software.getVersion();
    }
    
    /**
     * Full arg constructor for publishing app with different name, version, etc.
     * 
     * @param software
     * @param publishedSoftware
     * @param publishingUsername
     * @param publishedSoftwareName
     * @param publishedSoftwareVersion
     * @param publishedSoftwareExecutionSystem
     * @param publishedSoftwareStorageSystem
     * @param publishedSoftwareDeploymentPath
     */
    public PublishAction(Software software, String publishingUsername,
            String publishedSoftwareName, 
            String publishedSoftwareVersion,
            String publishedSoftwareExecutionSystemId) {
        super(software);
        setPublishingUsername(publishingUsername);
        setPublishedSoftwareName(publishedSoftwareName);
        setPublishedSoftwareVersion(publishedSoftwareVersion);
        setPublishedSoftwareExecutionSystemId(publishedSoftwareExecutionSystemId);
        setEventProcessor(new SoftwareEventProcessor());
    }

    /**
     * This method attempts to archive a job's output by retrieving the
     * .agave.archive shadow file from the remote job directory and staging
     * everything not in there to the user-supplied Job.archivePath on the 
     * Job.archiveSystem
     * 
     * @param job
     * @throws SystemUnavailableException
     * @throws SystemUnknownException
     * @throws JobException
     */
    @Override
    public void run() 
    throws SystemUnavailableException, SystemUnknownException, DependencyException, DomainException, ClosedByInterruptException, PermissionException
    {
        try {
    	    setPublishedSoftware(publish());
            
            // send publish event on success
            SoftwareEventProcessor eventProcessor = new SoftwareEventProcessor();
            eventProcessor.processPublishEvent(getEntity(), publishedSoftware, getPublishingUsername());
        }
        catch (SystemUnknownException e) {
        	getEventProcessor().processSoftwareContentEvent(getEntity(), 
        			SoftwareEventType.PUBLISHING_FAILED, 
        			"A publishing action failed for this app. " + e.getMessage(),
                    getPublishingUsername());
//            ApplicationManager.addEvent(getEntity(), 
//                    SoftwareEventType.PUBLISHING_FAILED, 
//                    "A publishing action failed for this app. " + e.getMessage(),
//                    getPublishingUsername());
            throw e;
        }
        catch (SystemUnavailableException e) {
        	getEventProcessor().processSoftwareContentEvent(
        			getEntity(), 
                    SoftwareEventType.PUBLISHING_FAILED, 
                    "A publishing action failed for this app. " + e.getMessage(),
                    getPublishingUsername());
//            ApplicationManager.addEvent(getEntity(), 
//                    SoftwareEventType.PUBLISHING_FAILED, 
//                    "A publishing action failed for this app. " + e.getMessage(),
//                    getPublishingUsername());
            throw e;
        }
        catch (SoftwareException | DependencyException e) {
        	getEventProcessor().processSoftwareContentEvent(
        			getEntity(), 
		            SoftwareEventType.PUBLISHING_FAILED, 
		            "A publishing action failed for this app due to an unexpected error. Please try again.",
		            getPublishingUsername());
//            ApplicationManager.addEvent(getEntity(), 
//                    SoftwareEventType.PUBLISHING_FAILED, 
//                    "A publishing action failed for this app due to an unexpected error. Please try again.",
//                    getPublishingUsername());
            throw new DomainException(e.getMessage(), e);
        }
    }
    
    /**
     * Handles the actual record creation, cloning, and data staging of the app assets.
     * @return published software record
     */
    protected Software publish() 
    throws SystemUnknownException, SystemUnavailableException, DependencyException
    {
        // throw exception if the app is already public. can't republish
        if (getEntity().isPubliclyAvailable()) {
            throw new SoftwareException("Software is already public");
        }
        else 
        {
            File stagingDir = null;
            File zippedFile = null;
            RemoteDataClient sourceSoftwareDataClient = null;
            RemoteDataClient publishedSoftwareDataClient = null;
            try 
            {
                publishedSoftware = getEntity().clone();
                publishedSoftware.setName(StringUtils.isEmpty(publishedSoftwareName) ? getEntity().getName() : publishedSoftwareName);
                publishedSoftware.setVersion(StringUtils.isEmpty(publishedSoftwareVersion) ? getEntity().getVersion() : publishedSoftwareVersion);
                
                // if the user supplied an exectuion system, check that its public and valid
                if (StringUtils.isNotEmpty(publishedSoftwareExecutionSystemId)) 
                {
                    publishedSoftwareExecutionSystem = new SystemDao().getUserExecutionSystem(publishingUsername, publishedSoftwareExecutionSystemId);
                    
                    // system does not exist
                    if (publishedSoftwareExecutionSystem == null) {
                        throw new SystemUnknownException("No public execution system found with id " + publishedSoftwareExecutionSystemId);
                    } 
                    // system exists, but is not public
                    else if (!publishedSoftwareExecutionSystem.isPubliclyAvailable()) {
                        throw new DependencyException("The target execution system, " 
                                + publishedSoftwareExecutionSystemId 
                                + ", is not publicly available. "
                                + "Please either publish the execution system or specify another "
                                + "execution system on which the published app should run.");
                    }
                }
                // otherwise, try the original execution system
                else 
                {
                    // if the system is no longer present
                    if (getEntity().getExecutionSystem() == null) {
                        throw new SystemUnknownException(
                            "Please specify a public execution system on which this app should run.");
                    } 
                    // otherwise, if it is present, but not public
                    else if (!getEntity().getExecutionSystem().isPubliclyAvailable()) {
                        throw new DependencyException("No public execution system was specified and "
                                + "the execution system of the original app, " 
                                + getEntity().getExecutionSystem().getSystemId() 
                                + ", is not publicly available. "
                                + "Please either publish the execution system or specify another "
                                + "execution system on which the published app should run.");
                    } 
                    // otherwise we're golden. we're using the original execution system
                    // from the app we're publishing
                    else {
                        publishedSoftwareExecutionSystem = getEntity().getExecutionSystem();
                    }
                }
                
                // verify permission to publish on the publishedSoftwareExecutionSystem.
                SystemRoleManager roleManager = new SystemRoleManager(publishedSoftwareExecutionSystem);
                
                if (!roleManager.getEffectiveRoleForPrincipal(getPublishingUsername()).canPublish()) {
                	throw new PermissionException("Permission denied. User does not a PUBLISHER role on " 
                			+ publishedSoftwareExecutionSystem.getSystemId() + ".");
                }
                
                publishedSoftware.setExecutionSystem(publishedSoftwareExecutionSystem);
                
                int latestRevision = SoftwareDao.getMaxRevisionForPublicSoftware(publishedSoftware.getName(), publishedSoftware.getVersion());
                
                publishedSoftware.setPubliclyAvailable(true);
                publishedSoftware.setRevisionCount(latestRevision + 1);
                
                // get handles to the destination public storage system
                StorageSystem publishedSoftwareStorageSystem = new SystemManager().getDefaultStorageSystem();
                sourceSoftwareDataClient = getSourceRemoteDataClient();
                
                // get handles to the original software storage system
                publishedSoftwareDataClient = getDestinationRemoteDataClient(publishedSoftwareStorageSystem);
                
                // pull the app assets and create a zip archive
                File stagedDir = fetchSoftwareDeploymentPath(sourceSoftwareDataClient);
                File tempDir = new File(Settings.TEMP_DIRECTORY);
                if (!tempDir.exists() || !tempDir.isDirectory()) {
                    tempDir.mkdirs();
                }
                zippedFile = new File(tempDir, publishedSoftware.getUniqueName() + ".zip");
                ZipUtil.zip(stagedDir, zippedFile);
                
                // ensure the destination directory is present
                
                resolveAndCreatePublishedDeploymentPath(publishedSoftwareStorageSystem, publishedSoftwareDataClient); 
                    
                copyPublicAppArchiveToDeploymentSystem(publishedSoftwareDataClient, zippedFile);
                
                // save the app
                SoftwareDao.persist(publishedSoftware);
                
                // TODO: Make this an option through the tenant admin preferences. 
                // schedulePublicAppAssetBundleBackup();
                
                return publishedSoftware;
            } 
            catch (SoftwareException e) {
                throw e;
            }
            catch (DependencyException e) {
                throw e;
            }
            catch (Exception e) 
            {
                throw new SoftwareException("Application publishing failed: " + e.getMessage(), e);
            } 
            finally 
            {
                try { stagingDir.delete(); } catch (Exception e) {}
                try { zippedFile.delete(); } catch (Exception e) {}
                try { sourceSoftwareDataClient.disconnect(); } catch (Exception e) {}
                try { publishedSoftwareDataClient.disconnect(); } catch (Exception e) {}
            }
        }
    }
    
    /**
     * Reliably transfers the zipped {@link Software} archive to the remote {@link StoragSystem} and
     * sets the checksum of the file for future reference.
     * 
     * @param publishedSoftwareDataClient
     * @param zippedFile
     * @throws FileNotFoundException
     * @throws IOException
     * @throws RemoteDataException
     * @throws TransferException
     */
    private void copyPublicAppArchiveToDeploymentSystem(RemoteDataClient publishedSoftwareDataClient, File zippedFile)
    throws FileNotFoundException, IOException, RemoteDataException, TransferException 
    {
        TransferTask transferTask = new TransferTask(
                "agave://" + getEntity().getStorageSystem().getSystemId() + "/" + getEntity().getDeploymentPath(), 
                "agave://" + publishedSoftware.getStorageSystem().getSystemId() + "/" + publishedSoftware.getDeploymentPath(), 
                publishingUsername, 
                null, null);
        TransferTaskDao.persist(transferTask);
        
        try {
            this.urlCopy = new URLCopy(new Local(), publishedSoftwareDataClient);
            transferTask = urlCopy.copy(zippedFile.getAbsolutePath(), publishedSoftware.getDeploymentPath(), transferTask);
        } catch (Exception e) {
            throw new SoftwareException("Failed to copy public application assets to the default public storage system, " 
                    + publishedSoftware.getStorageSystem().getSystemId(), e);
        }
        
         if (publishedSoftwareDataClient.doesExist(publishedSoftware.getDeploymentPath())) 
        {
            String localChecksum = DigestUtils.md5Hex(new FileInputStream(zippedFile));
            publishedSoftware.setChecksum(localChecksum);
        } 
        else 
        {
            throw new FileNotFoundException("Failed to copy public app to iPlant Data Store");
        }
    }

    /**
     * Builds the remote path to which the zipped {@link Software#getDeploymentPath()} will be deployed on
     * the public {@link StorageSystem}. 
     * @param publishedSoftwareStorageSystem
     * @param publishedSoftwareDataClient
     * @return agave paths to the app on the remote system.
     * 
     * @throws IOException
     * @throws RemoteDataException
     */
    protected String resolveAndCreatePublishedDeploymentPath(StorageSystem publishedSoftwareStorageSystem, RemoteDataClient publishedSoftwareDataClient) 
    throws IOException, RemoteDataException 
    {
     // resolve the path to the folder for public apps on the public default storage system
        // note that this may change over time, but it's saved with the app, so that's fine.
        String remotePublicAppsFolder = publishedSoftwareStorageSystem.getStorageConfig().getPublicAppsDir();;
        if (StringUtils.isEmpty(remotePublicAppsFolder)) {
            remotePublicAppsFolder = Settings.PUBLIC_APPS_DEFAULT_DIRECTORY;
        }
        if (remotePublicAppsFolder.endsWith("/")) {
            remotePublicAppsFolder = StringUtils.substring(remotePublicAppsFolder, 0, -1);
        }
        
        String bundlePath = remotePublicAppsFolder + "/" + publishedSoftware.getUniqueName() + ".zip";
        
        publishedSoftware.setDeploymentPath(bundlePath);
        publishedSoftware.setStorageSystem(publishedSoftwareStorageSystem);
        
        if (!publishedSoftwareDataClient.doesExist(remotePublicAppsFolder)) {
            publishedSoftwareDataClient.mkdirs(remotePublicAppsFolder);
        }
        
        return bundlePath;
    }

    /**
     * Copies the {@link Software#getDeploymentPath()} from {@link Software#getStorageSystem()} 
     * to the local system for bundling and archiving.
     * 
     * @return {@link File} reference to the location of the cached deployment path on the local system.
     */
    protected File fetchSoftwareDeploymentPath(RemoteDataClient sourceSoftwareDataClient) 
    {
        File stagingDir = Files.createTempDir();
        
        String originalAppDeploymentPath = getEntity().getDeploymentPath();
        if (originalAppDeploymentPath.endsWith("/")) {
            originalAppDeploymentPath = StringUtils.substring(originalAppDeploymentPath, 0, -1);
        }
        
        try {
            sourceSoftwareDataClient.get(originalAppDeploymentPath, stagingDir.getAbsolutePath());
            
            return new File(stagingDir, FilenameUtils.getName(originalAppDeploymentPath));
        }
        catch (FileNotFoundException e) {
            throw new SoftwareException("The original application deployment path, "
                    + getEntity().getDeploymentPath() + ", was not found on the deployment system " 
                    + getEntity().getStorageSystem().getSystemId(), e);
        }
        catch (Exception e) {
            throw new SoftwareException("Failed to retrieve application assets from the original application storage system, " 
                    + getEntity().getStorageSystem().getSystemId(), e);
        }
    }

    /**
     * Creates a {@link RemoteDataClient} to the {@link Software#getStorageSystem()} representing
     * the default {@link StorageSystem} for the tenant. The {@link RemoteDataClient} is then
     * authenticated and returned for use.
     * 
     * @return pre-authenticated {@link RemoteDataClient} to the destination storage system
     * @throws RemoteDataException
     * @throws RemoteCredentialException
     * @throws SystemUnavailableException
     * @throws SystemUnknownException
     */
    protected RemoteDataClient getDestinationRemoteDataClient(StorageSystem defaultStorageSystem) 
    throws RemoteDataException, RemoteCredentialException, SystemUnavailableException, SystemUnknownException
    {    
        // apps must be saved on a public default storage system
        if (defaultStorageSystem == null) {
            throw new SystemUnavailableException("No public deployment system " 
                    + "is defined for your tenant. Please contact your tenant administrator "
                    + "to configure your tenant for app publishing.");
        }
        // if the public deployment host is unavailable
        else if (!defaultStorageSystem.isAvailable()) {
            throw new SystemUnknownException("The public deployment system " 
                    + defaultStorageSystem.getSystemId() 
                    + " is not currently available. App publishing will fail until " 
                    + "the system comes back online.");
        } 
        // if the system is in down time or otherwise unavailable...
        else if (defaultStorageSystem.getStatus() != SystemStatusType.UP)
        {
            throw new SystemUnavailableException("The public deployment system " 
                    + defaultStorageSystem.getSystemId() 
                    + " is currently " + defaultStorageSystem.getStatus().name() 
                    + ". App publishing will fail until " 
                    + "the system us UP again.");
        }
        
        // get a handle on the public default storage system
        RemoteDataClient destDataClient = defaultStorageSystem.getRemoteDataClient();
        
        try 
        {
            destDataClient.authenticate();
            
            return destDataClient;
        }
        catch (Exception e) {
            throw new SoftwareException("Failed to authenticate to the default public storage system, " 
                    + defaultStorageSystem.getSystemId(), e);
        }
        
    }
    
    /**
     * Creates a {@link RemoteDataClient} to the {@link Software#getStorageSystem()} of the
     * the original {@link Software}. The {@link RemoteDataClient} is then
     * authenticated and returned for use.
     * 
     * @return pre-authenticated {@link RemoteDataClient} to the original {@link Software#getStorageSystem()}
     * @throws RemoteDataException
     * @throws RemoteCredentialException
     * @throws SystemUnavailableException
     * @throws SystemUnknownException
     */
    protected RemoteDataClient getSourceRemoteDataClient() 
    throws RemoteDataException, RemoteCredentialException, SystemUnavailableException, SystemUnknownException
    {    
        StorageSystem appDeploymentSystem = getEntity().getStorageSystem();
        
        // the app assets must be available 
        if (appDeploymentSystem == null) {
            throw new SystemUnavailableException("No public deployment system " 
                    + "is defined for your tenant. Please contact your tenant administrator "
                    + "to configure your tenant for app publishing.");
        }
        // if the app deployment host is unavailable
        else if (!appDeploymentSystem.isAvailable()) {
            throw new SystemUnknownException("The app deployment system " 
                    + appDeploymentSystem.getSystemId() 
                    + " is not currently available. App publishing will fail until " 
                    + "the system comes back online and the app assets are accessible.");
        } 
        // if the system is in down time or otherwise unavailable...
        else if (appDeploymentSystem.getStatus() != SystemStatusType.UP)
        {
            throw new SystemUnavailableException("The app deployment system " 
                    + appDeploymentSystem.getSystemId() 
                    + " is currently " + appDeploymentSystem.getStatus().name() 
                    + ". App publishing will fail until " 
                    + "the system us UP again and the app assets are accessible.");
        }
        
        // get a handle on the public default storage system
        RemoteDataClient srcDataClient = appDeploymentSystem.getRemoteDataClient();
        
        try 
        {
            srcDataClient.authenticate();
            
            return srcDataClient;
        }
        catch (Exception e) {
            throw new SoftwareException("Failed to authenticate to the app deployment system, " 
                    + appDeploymentSystem.getSystemId(), e);
        }
        
    }

    public String getPublishingUsername() {
        return publishingUsername;
    }

    public void setPublishingUsername(String publishingUsername) {
        this.publishingUsername = publishingUsername;
    }

    /**
     * @return
     */
    public Software getPublishedSoftware() {
        return this.publishedSoftware;
    }

    /**
     * @param publishedSoftware the publishedSoftware to set
     */
    public void setPublishedSoftware(Software publishedSoftware) {
        this.publishedSoftware = publishedSoftware;
    }
    
    /**
     * Backs up an app's assets to the platform cloud storage in a folder 
     * named after the tenant.
     * 
     * @throws SoftwareException
     */
    public void schedulePublicAppAssetBundleBackup() 
    {
        String srcUrl = "";
        String destUrl = "";
        
        try 
        {
            SystemDao dao = new SystemDao();
            RemoteSystem platformArchiveSystem = new SystemManager().getPlatformStorageSystem();
            RemoteDataClient platformArchiveSystemDataClient = platformArchiveSystem.getRemoteDataClient();
            
            StorageSystem appStorageSystem = publishedSoftware.getStorageSystem();
            srcUrl = "agave://" + appStorageSystem.getSystemId() + "/" + publishedSoftware.getDeploymentPath();
            destUrl = "agave://" + org.iplantc.service.common.Settings.PLATFORM_STORAGE_SYSTEM_ID + "/" + publishedSoftware.getTenantId() + "/" + publishedSoftware.getDeploymentPath();
            
            if (StringUtils.isEmpty(org.iplantc.service.common.Settings.PLATFORM_STORAGE_SYSTEM_ID)) 
            {
                throw new SystemUnknownException("While attempting to backup the public app bundle for " + publishedSoftware.getUniqueName() + 
                        " the platform storage system was not defined for the current tenant. You may " + 
                        " manually back up the public app bundle by defining a platform backup storage system " + 
                        " and copying it from " + srcUrl +
                        " to  " + publishedSoftware.getTenantId() + "/" + publishedSoftware.getDeploymentPath() + " on the new sytem.");
            } 
            else 
            {
                RemoteSystem system = dao.findBySystemId(org.iplantc.service.common.Settings.PLATFORM_STORAGE_SYSTEM_ID);
                
                if (system == null) 
                {
                    throw new SystemUnknownException("While attempting to backup the public app bundle for " + publishedSoftware.getUniqueName() + 
                                " the platform storage system " + org.iplantc.service.common.Settings.PLATFORM_STORAGE_SYSTEM_ID + 
                                ", an error occurred preventing the backup from succeeding. You may " + 
                                " manually back up the public app bundle by copying it from " + srcUrl +
                                " to " + destUrl);
                } 
                else
                {
                    LogicalFile logicalFile = new LogicalFile();
                    logicalFile.setStatus(StagingTaskStatus.STAGING_QUEUED);
                    logicalFile.setOwner("dooley");
                    logicalFile.setInternalUsername(null);
                    logicalFile.setSourceUri(srcUrl);
                    logicalFile.setNativeFormat("RAW");
                    
                    logicalFile.setSystem(platformArchiveSystem);
                    logicalFile.setName(FilenameUtils.getName(publishedSoftware.getDeploymentPath()));
                    logicalFile.setPath(platformArchiveSystemDataClient.resolvePath("/" + publishedSoftware.getTenantId() + "/" + publishedSoftware.getDeploymentPath()));
                    
                    LogicalFileDao.persist(logicalFile);
                    logicalFile.addContentEvent(new FileEvent(logicalFile,
                                    FileEventType.STAGING_QUEUED,
                                    "Public app archive queued for cloud backup.",
                                    publishedSoftware.getOwner()));
                    LogicalFileDao.persist(logicalFile);
                    
                    // add the logical file to the staging queue
                    QueueTaskDao.enqueueStagingTask(logicalFile, getPublishingUsername());
        
                    LogicalFileDao.updateTransferStatus(logicalFile, StagingTaskStatus.STAGING_QUEUED, "dooley");
                }
            }
            
        }
        catch (SystemUnknownException e) {
            try {
                Tenant tenant = new TenantDao().findByTenantId(TenancyHelper.getCurrentTenantId());
                EmailMessage.send(tenant.getContactName(), 
                        tenant.getContactEmail(), 
                        "Failed to back up app bundle for " + publishedSoftware.getUniqueName(),
                        e.getMessage(), HTMLizer.htmlize(e.getMessage()));
            }
            catch (Throwable t) {
                log.error("Failed to notify admin that backup of " + publishedSoftware.getUniqueName() + " app bundle failed.", t);
            }
            
            throw new SoftwareException("Failed to backup the public app bundle from " + 
                    srcUrl + " to " + destUrl);
        }
        catch (Throwable e) 
        {
            try {
                Tenant tenant = new TenantDao().findByTenantId(TenancyHelper.getCurrentTenantId());
                String message =  
                        "While attempting to backup the public app bundle for " + publishedSoftware.getUniqueName() + 
                        " to the platform storage system " + org.iplantc.service.common.Settings.PLATFORM_STORAGE_SYSTEM_ID + 
                        ", an error occurred preventing the backup from succeeding. You may " + 
                        " manually back up the public app bundle by copying it from " + srcUrl +
                        " to " + destUrl;
                EmailMessage.send(tenant.getContactName(), 
                        tenant.getContactEmail(), 
                        "Failed to back up app bundle for " + publishedSoftware.getUniqueName(),
                        message, HTMLizer.htmlize(message));
            }
            catch (Throwable t) {
                log.error("Failed to notify admin that backup of " + publishedSoftware.getUniqueName() + " app bundle failed.", t);
            }
            
            throw new SoftwareException("Failed to backup the public app bundle from " + 
                    srcUrl + " to " + destUrl);
        }
    }

	/**
	 * @return the eventProcessor
	 */
	public SoftwareEventProcessor getEventProcessor() {
		return eventProcessor;
	}

	/**
	 * @param eventProcessor the eventProcessor to set
	 */
	public void setEventProcessor(SoftwareEventProcessor eventProcessor) {
		this.eventProcessor = eventProcessor;
	}

	/**
	 * @return the publishedSoftwareName
	 */
	public String getPublishedSoftwareName() {
		return publishedSoftwareName;
	}

	/**
	 * @param publishedSoftwareName the publishedSoftwareName to set
	 */
	public void setPublishedSoftwareName(String publishedSoftwareName) {
		this.publishedSoftwareName = publishedSoftwareName;
	}

	/**
	 * @return the publishedSoftwareVersion
	 */
	public String getPublishedSoftwareVersion() {
		return publishedSoftwareVersion;
	}

	/**
	 * @param publishedSoftwareVersion the publishedSoftwareVersion to set
	 */
	public void setPublishedSoftwareVersion(String publishedSoftwareVersion) {
		this.publishedSoftwareVersion = publishedSoftwareVersion;
	}

	/**
	 * @return the publishedSoftwareExecutionSystem
	 */
	public ExecutionSystem getPublishedSoftwareExecutionSystem() {
		return publishedSoftwareExecutionSystem;
	}

	/**
	 * @param publishedSoftwareExecutionSystem the publishedSoftwareExecutionSystem to set
	 */
	public void setPublishedSoftwareExecutionSystem(
			ExecutionSystem publishedSoftwareExecutionSystem) {
		this.publishedSoftwareExecutionSystem = publishedSoftwareExecutionSystem;
	}

	/**
	 * @return the publishedSoftwareExecutionSystemId
	 */
	public String getPublishedSoftwareExecutionSystemId() {
		return publishedSoftwareExecutionSystemId;
	}

	/**
	 * @param publishedSoftwareExecutionSystemId the publishedSoftwareExecutionSystemId to set
	 */
	public void setPublishedSoftwareExecutionSystemId(
			String publishedSoftwareExecutionSystemId) {
		this.publishedSoftwareExecutionSystemId = publishedSoftwareExecutionSystemId;
	}
}
