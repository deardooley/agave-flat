/**
 * 
 */
package org.iplantc.service.apps.queue.actions;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.channels.ClosedByInterruptException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.apps.Settings;
import org.iplantc.service.apps.dao.SoftwareDao;
import org.iplantc.service.apps.exceptions.SoftwareException;
import org.iplantc.service.apps.exceptions.SoftwareResourceException;
import org.iplantc.service.apps.managers.SoftwareEventProcessor;
import org.iplantc.service.apps.managers.SoftwarePermissionManager;
import org.iplantc.service.apps.model.Software;
import org.iplantc.service.apps.model.enumerations.SoftwareEventType;
import org.iplantc.service.apps.util.ZipUtil;
import org.iplantc.service.common.exceptions.DependencyException;
import org.iplantc.service.common.exceptions.DomainException;
import org.iplantc.service.common.exceptions.PermissionException;
import org.iplantc.service.common.util.TimeUtils;
import org.iplantc.service.io.dao.LogicalFileDao;
import org.iplantc.service.io.model.LogicalFile;
import org.iplantc.service.io.permissions.PermissionManager;
import org.iplantc.service.systems.dao.SystemDao;
import org.iplantc.service.systems.exceptions.RemoteCredentialException;
import org.iplantc.service.systems.exceptions.SystemUnavailableException;
import org.iplantc.service.systems.exceptions.SystemUnknownException;
import org.iplantc.service.systems.manager.SystemManager;
import org.iplantc.service.systems.model.BatchQueue;
import org.iplantc.service.systems.model.ExecutionSystem;
import org.iplantc.service.systems.model.RemoteSystem;
import org.iplantc.service.systems.model.StorageSystem;
import org.iplantc.service.systems.model.enumerations.RoleType;
import org.iplantc.service.systems.model.enumerations.SystemStatusType;
import org.iplantc.service.transfer.RemoteDataClient;
import org.iplantc.service.transfer.RemoteTransferListener;
import org.iplantc.service.transfer.dao.TransferTaskDao;
import org.iplantc.service.transfer.exceptions.RemoteDataException;
import org.iplantc.service.transfer.exceptions.TransferException;
import org.iplantc.service.transfer.model.TransferTask;
import org.iplantc.service.transfer.model.enumerations.PermissionType;
import org.iplantc.service.transfer.util.MD5Checksum;

import com.google.common.io.Files;

/**
 * Handles registration, validation, and data management of cloning {@link Software}.
 * @author dooley
 *
 */
public class CloneAction extends AbstractWorkerAction<Software> {
    
    private static Logger log = Logger.getLogger(CloneAction.class);
    private Software clonedSoftware;
    private String clonedSoftwareOwner;
    private String clonedSoftwareName;
    private String clonedSoftwareVersion;
    private String clonedSoftwareDeploymentPath;
    private String clonedSoftwareStorageSystemId;
    private String clonedSoftwareExecutionSystemId;
    private StorageSystem clonedSoftwareStorageSystem;
    private ExecutionSystem clonedSoftwareExecutionSystem;
    protected SystemDao systemDao = new SystemDao();
    protected SystemManager systemManager = new SystemManager();
    protected SoftwarePermissionManager softwarePermissionManager;
    private SoftwareEventProcessor eventProcessor;
    
    /**
     * Basic constructor for publishing software under same name, version, etc.
     * @param software
     * @param destOwner
     */
    public CloneAction(Software software, 
                       String clonedSoftwareOwner, 
                       String clonedSoftwareName, 
                       String clonedSoftwareVersion, 
                       String clonedSoftwareExecutionSystemId,
                       String clonedSoftwareStorageSystemId,
                       String clonedSoftwareDeploymentPath) 
    {
        super(software);
        this.clonedSoftwareOwner = clonedSoftwareOwner;
        this.clonedSoftwareExecutionSystemId = clonedSoftwareExecutionSystemId;
        this.clonedSoftwareExecutionSystem = systemDao.getUserExecutionSystem(clonedSoftwareOwner, clonedSoftwareExecutionSystemId);
        this.clonedSoftwareStorageSystemId = clonedSoftwareStorageSystemId;
        this.clonedSoftwareStorageSystem = systemDao.getUserStorageSystem(clonedSoftwareOwner, clonedSoftwareStorageSystemId);
        this.clonedSoftwareDeploymentPath = clonedSoftwareDeploymentPath;
        this.clonedSoftwareVersion = clonedSoftwareVersion;
        this.clonedSoftwareName = clonedSoftwareName;
        this.softwarePermissionManager = new SoftwarePermissionManager(getEntity());
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
    throws SystemUnavailableException, SystemUnknownException, DependencyException, DomainException, PermissionException, ClosedByInterruptException
    {
        try {
            if (getEntity().isPubliclyAvailable()) {
                this.clonedSoftware = clonePublicApplication();
            } 
            else {
                this.clonedSoftware = clonePrivateApplication();
            }
            
            // send publish event on success
            getEventProcessor().processCloneEvent(getEntity(), clonedSoftware, clonedSoftwareOwner);
        }
        catch (RemoteDataException e) {
        	getEventProcessor().processSoftwareContentEvent(getEntity(), 
                    SoftwareEventType.CLONING_FAILED, 
                    "A cloning action failed for this app. " + e.getMessage(),
                    clonedSoftwareOwner);
            throw new DomainException("Failed to publish app. " + e.getMessage(), e);
        }
        catch (PermissionException | SystemUnknownException e) {
        	getEventProcessor().processSoftwareContentEvent(getEntity(), 
                    SoftwareEventType.CLONING_FAILED, 
                    "A cloning action failed for this app. " + e.getMessage(),
                    clonedSoftwareOwner);
            throw e;
        }
        catch (SystemUnavailableException e) {
        	getEventProcessor().processSoftwareContentEvent(getEntity(), 
                    SoftwareEventType.CLONING_FAILED, 
                    "A cloning action failed for this app. " + e.getMessage(),
                    clonedSoftwareOwner);
            throw e;
        }
        catch (SoftwareException e) {
        	getEventProcessor().processSoftwareContentEvent(getEntity(), 
                    SoftwareEventType.CLONING_FAILED, 
                    "A cloning action failed for this app due to an unexpected error. Please try again.",
                    clonedSoftwareOwner);
            throw new DomainException("Failed to publish app. " + e.getMessage(), e);
        }
    }
    
    /**
     * Creates a private copy of a public {@link Software} resource. The user default 
     * {@link StorageSystem} and {@link ExecutionSystem} are used if none is specified to store the uncompressed copy of the public application assets. App
     * name and version will be updated as provided in the arguments.
     *  
     * @throws SoftwareResourceException
     * @throws SystemUnknownException
     * @throws SystemUnavailableException
     * @throws DomainException
     * @throws RemoteDataException
     * @throws PermissionException
     */
    public Software clonePublicApplication() 
    throws SoftwareResourceException, SystemUnknownException, SystemUnavailableException, DomainException, RemoteDataException, PermissionException
    {
        RemoteDataClient publicSoftwareDataClient = null;
        RemoteDataClient clonedSoftwareDataClient = null;
        File locallyCachedPublicSoftwareDeploymentPath = null;
        try
        {
            // verify and/or get a valid execution system
            clonedSoftwareExecutionSystem = verifyClonedSoftwareExecutionSystem(clonedSoftwareExecutionSystem);

            // verify and/or get a valid storage system
            clonedSoftwareStorageSystem = verifyClonedSoftwareStorageSystem(clonedSoftwareStorageSystem);
            
            // verify the public storage system is available
            verifySourceSoftwareStorageSystem(getEntity().getStorageSystem());
            
            // clone the record
            Software clonedSoftware = getEntity().clone();
            clonedSoftware.setName(StringUtils.isEmpty(clonedSoftwareName) ? getEntity().getName() : clonedSoftwareName);
            clonedSoftware.setVersion(StringUtils.isEmpty(clonedSoftwareVersion) ? getEntity().getVersion() : clonedSoftwareVersion);
            clonedSoftware.setOwner(clonedSoftwareOwner);
            clonedSoftware.setPubliclyAvailable(false);
            clonedSoftware.setAvailable(true);
            clonedSoftware.setRevisionCount(1);
            clonedSoftware.setStorageSystem(clonedSoftwareStorageSystem);
            clonedSoftware.setExecutionSystem(clonedSoftwareExecutionSystem);
            clonedSoftware.setExecutionType(clonedSoftwareExecutionSystem.getExecutionType());
            
            // check for name conflict in new system
            if (SoftwareDao.getSoftwareByUniqueName(clonedSoftware.getUniqueName()) != null) {
                throw new SoftwareException("An app identified by " + clonedSoftware.getUniqueName() 
                        + " already exists. Please select a new name or version to create a unique app id.");
            }
            
            // sanity check the default settings against the new exection system
            recalculateDefaultQueueSettings(clonedSoftware);
            
            // get a data client for the destination system to be used when staging data
            clonedSoftwareDataClient = getRemoteDataClient(clonedSoftware.getStorageSystem());
            
            // get a data client for the source system to be used when fetching data
            publicSoftwareDataClient = getRemoteDataClient(getEntity().getStorageSystem());
            
            // fetch the zip archive to local cache and unpack it
            locallyCachedPublicSoftwareDeploymentPath = fetchPublicAppArchiveFromDeploymentSystem(publicSoftwareDataClient);
            
            // create remote deployment path if it does not exist
            this.clonedSoftwareDeploymentPath = resolveAndCreatePublishedDeploymentPath(clonedSoftware, clonedSoftwareDataClient);
            clonedSoftware.setDeploymentPath(this.clonedSoftwareDeploymentPath);
            
            // copy local cached deployment folder to remote system
            copyLocalCachedDeploymentPathToClonedSoftwareDeploymentSystem(clonedSoftware, clonedSoftwareDataClient, locallyCachedPublicSoftwareDeploymentPath);
                
            // finally save the cloned software instance
            SoftwareDao.persist(clonedSoftware);
            
            return clonedSoftware;
            
        }
        catch (SoftwareException | SystemUnknownException | SystemUnavailableException | RemoteDataException e) {
            throw e;
        }
        catch (TransferException | PermissionException | RemoteCredentialException e)
        {
            throw new DomainException(e.getMessage(), e);
        } 
        catch (IOException e) {
            throw new DomainException("Failed to cache app assets from deployment system prior to cloning", e);
        } 
        finally 
        {
            try { FileUtils.deleteDirectory(locallyCachedPublicSoftwareDeploymentPath); } catch (Exception e) {}
            try { clonedSoftwareDataClient.disconnect(); } catch (Exception e) {}
            try { publicSoftwareDataClient.disconnect(); } catch (Exception e) {}
        }
    }
    
    /**
     * Creates a private copy of another private {@link Software} resource. The user default 
     * {@link StorageSystem} and {@link ExecutionSystem} are used if none is specified to 
     * store the copy of the application assets. App name and version will be updated as 
     * provided in the arguments.
     * 
     * @return cloned software record
     * @throws SoftwareResourceException
     * @throws SystemUnknownException
     * @throws SystemUnavailableException
     * @throws DomainException
     * @throws RemoteDataException
     * @throws PermissionException
     */
    public Software clonePrivateApplication() 
    throws SoftwareResourceException, SystemUnknownException, SystemUnavailableException, DomainException, RemoteDataException, PermissionException
    {
        RemoteDataClient privateSoftwareDataClient = null;
        RemoteDataClient clonedSoftwareDataClient = null;
        File locallyCachedPrivateSoftwareDeploymentPath = null;
        try 
        {
            // verify and/or get a valid execution system
            clonedSoftwareExecutionSystem = verifyClonedSoftwareExecutionSystem(clonedSoftwareExecutionSystem);
    
            // verify and/or get a valid storage system
            clonedSoftwareStorageSystem = verifyClonedSoftwareStorageSystem(clonedSoftwareStorageSystem);
            
            // verify the public storage system is available
            verifySourceSoftwareStorageSystem(getEntity().getStorageSystem());
            
            // update the software description
            Software clonedSoftware = getEntity().clone();
            clonedSoftware.setName(StringUtils.isEmpty(clonedSoftwareName) ? getEntity().getName() : clonedSoftwareName);
            clonedSoftware.setVersion(StringUtils.isEmpty(clonedSoftwareVersion) ? getEntity().getVersion() : clonedSoftwareVersion);
            clonedSoftware.setOwner(clonedSoftwareOwner);
            clonedSoftware.setPubliclyAvailable(false);
            clonedSoftware.setAvailable(true);
            clonedSoftware.setRevisionCount(1);
            clonedSoftware.setStorageSystem(clonedSoftwareStorageSystem);
            clonedSoftware.setExecutionSystem(clonedSoftwareExecutionSystem);
            clonedSoftware.setExecutionType(clonedSoftwareExecutionSystem.getExecutionType());
            
            if (SoftwareDao.getSoftwareByUniqueName(clonedSoftware.getUniqueName()) != null) {
                throw new SoftwareException("An app identified by " + clonedSoftware.getUniqueName() 
                        + " already exists. Please select a new name or version to create a unique app id.");
            }
            
            recalculateDefaultQueueSettings(clonedSoftware);
                
            // get a data client for the destination system to be used when staging data
            clonedSoftwareDataClient = getRemoteDataClient(clonedSoftware.getStorageSystem());
            
            // get a data client for the source system to be used when fetching data
            privateSoftwareDataClient = getRemoteDataClient(getEntity().getStorageSystem());
            
            // fetch the zip archive to local cache and unpack it
            locallyCachedPrivateSoftwareDeploymentPath = fetchPrivateAppArchiveFromDeploymentSystem(privateSoftwareDataClient);
            
            // create remote deployment path if it does not exist
            this.clonedSoftwareDeploymentPath = resolveAndCreatePublishedDeploymentPath(clonedSoftware, clonedSoftwareDataClient);
            clonedSoftware.setDeploymentPath(this.clonedSoftwareDeploymentPath);
            
            // copy local cached deployment folder to remote system
            copyLocalCachedDeploymentPathToClonedSoftwareDeploymentSystem(clonedSoftware, clonedSoftwareDataClient, locallyCachedPrivateSoftwareDeploymentPath);
                
            SoftwareDao.persist(clonedSoftware);
            
            return clonedSoftware;
        }
        catch (SoftwareException | SystemUnknownException | SystemUnavailableException | RemoteDataException e) {
            throw e;
        }
        catch (TransferException | PermissionException | RemoteCredentialException e)
        {
            throw new DomainException(e.getMessage(), e);
        } 
        catch (IOException e) {
            throw new DomainException("Failed to cache app assets from deployment system prior to cloning", e);
        } 
        finally 
        {
            try { FileUtils.deleteDirectory(locallyCachedPrivateSoftwareDeploymentPath); } catch (Exception e) {}
            try { clonedSoftwareDataClient.disconnect(); } catch (Exception e) {}
            try { privateSoftwareDataClient.disconnect(); } catch (Exception e) {}
        }
        
    }
    
    /**
     * Adjust the default queue settings on a {@link Software} instance based on 
     * the execution system parameters.
     *  
     * @param software
     */
    protected void recalculateDefaultQueueSettings(Software software) {
        // if the old app had a default queue that does not exist on the new execution
        // system, set the default queue to the new system default queue 
        if (StringUtils.isNotEmpty(software.getDefaultQueue())) 
        {
            BatchQueue batchQueue = null;
            
            if (StringUtils.equals(software.getExecutionSystem().getSystemId(), getEntity().getExecutionSystem().getSystemId())) {
                // if the execution system has changed, leave them be
            }
            // if the execution system has changed, we need to update
            else 
            {
                // if the software default queue does not exist on the execution system
                if ((batchQueue = software.getExecutionSystem().getQueue(software.getDefaultQueue())) == null) {
                    batchQueue = software.getExecutionSystem().getDefaultQueue();
                }
                
                if (batchQueue != null) {
                    software.setDefaultQueue(batchQueue.getName());
                    
                    if (StringUtils.isNotEmpty(software.getDefaultMaxRunTime())) {
                        if (TimeUtils.compareRequestedJobTimes(software.getDefaultMaxRunTime(), batchQueue.getMaxRequestedTime()) > 0) {
                            software.setDefaultMaxRunTime(batchQueue.getMaxRequestedTime());
                        }
                    }
                    
                    if (software.getDefaultNodes() != null) {
                        if (batchQueue.getMaxNodes() > 0) {
                            if (software.getDefaultNodes() > batchQueue.getMaxNodes()) {
                                software.setDefaultNodes(batchQueue.getMaxNodes());
                            }   
                        }
                    }
                    
                    if (software.getDefaultProcessorsPerNode() != null) {
                        if (batchQueue.getMaxProcessorsPerNode() > 0) {
                            if (software.getDefaultProcessorsPerNode() > batchQueue.getMaxProcessorsPerNode()) {
                                software.setDefaultNodes(batchQueue.getMaxProcessorsPerNode());
                            }   
                        }
                    }
                    
                    if (software.getDefaultMemoryPerNode() != null) {
                        if (batchQueue.getMaxMemoryPerNode() > 0) {
                            if (software.getDefaultMemoryPerNode() > batchQueue.getMaxMemoryPerNode()) {
                                software.setDefaultMemoryPerNode(batchQueue.getMaxMemoryPerNode());
                            }   
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Verifies that the {@link ExecutionSystem} is a valid execution system for the user
     * 
     * @param executionSystem to validate 
     * @return provided {@link ExecutionSystem} if {@code executionSystem} is not null, the user's default {@link ExecutionSystem} otherwise.
     * @throws SystemUnknownException
     */
    protected ExecutionSystem verifyClonedSoftwareExecutionSystem(ExecutionSystem executionSystem) 
    throws SystemUnknownException, PermissionException
    {
        if (executionSystem == null) {
            if (StringUtils.isNotEmpty(clonedSoftwareExecutionSystemId)) {
                throw new SystemUnknownException("No execution system found matching " 
                        + clonedSoftwareExecutionSystemId);
            } else {
                executionSystem = this.systemManager.getUserDefaultExecutionSystem(getClonedSoftwareOwner());
            }
        }
        
        if (executionSystem == null) {
            throw new SystemUnknownException("No target execution system was specified, and the user "
                    + "has no default execution system. Please specify a target execution system "
                    + "on which to run the cloned app or define a default execution system.");
        } 
        else if (!executionSystem.getUserRole(getClonedSoftwareOwner()).canPublish()) {
            throw new PermissionException(
                    "User does not have permission to publish applications on the target " + 
                    "execution system. Verify you have been granted a " + RoleType.PUBLISHER +
                    "role on the target execution system " + executionSystem.getSystemId());
        }
        
        return executionSystem;
    }
    
    /**
     * Verifies that the {@link StorageSystem} is a valid storage system for the user
     * @param storageSystem {@link StorageSystem} to validate 
     * @return provided {@link StorageSystem} if {@code storageSystem} is not null, the user's default {@link StorageSystem} otherwise.
     * @throws SystemUnknownException
     * @throws SystemUnavailableException
     * @throws PermissionException
     */
    protected StorageSystem verifyClonedSoftwareStorageSystem(StorageSystem storageSystem) throws SystemUnknownException, SystemUnavailableException, PermissionException
    {
        if (storageSystem == null) {
            if (StringUtils.isNotEmpty(clonedSoftwareStorageSystemId)) {
                throw new SystemUnknownException("No storage system found matching " 
                        + clonedSoftwareStorageSystemId);
            } else {
                storageSystem = this.systemManager.getUserDefaultStorageSystem(getClonedSoftwareOwner());
            }
        }
        
        // apps must be saved on a public default storage system
        if (storageSystem == null) {
            throw new SystemUnknownException("No target deployment system was specified, and the user "
                    + "has no default storage system. Please specify a target deployment system "
                    + "on which to store the cloned app assets or define a default storage system.");
        }
        // if the public deployment host is unavailable
        else if (!storageSystem.isAvailable()) {
            throw new SystemUnknownException("The target deployment system " 
                    + storageSystem.getSystemId() 
                    + " is not currently available. Cloning of apps whose "
                    + "assets reside on this system will fail until " 
                    + "the system comes back online.");
        } 
        // if the system is in down time or otherwise unavailable...
        else if (storageSystem.getStatus() != SystemStatusType.UP)
        {
            throw new SystemUnavailableException("The target deployment system " 
                    + storageSystem.getSystemId() 
                    + " is currently " + storageSystem.getStatus().name() 
                    + ". Cloning of apps whose assets reside on this system will fail until " 
                    + "the system returns to service.");
        }
        
        return storageSystem;
    }
    
    /**
     * Verifies the existence and availability of the source {@link Software#getStorageSystem()}.
     *   
     * @param storageSystem
     * @throws SystemUnknownException
     * @throws SystemUnavailableException
     */
    protected void verifySourceSoftwareStorageSystem(StorageSystem storageSystem) throws SystemUnknownException, SystemUnavailableException {
        
        // apps must be saved on a public default storage system
        if (storageSystem == null) {
            throw new SystemUnknownException("The deployment system containing the assets of"
                    + "the public app was not found as a registered system. Cloning of this app cannot proceed.");
        }
        // if the public deployment host is unavailable
        else if (!storageSystem.isAvailable()) {
            throw new SystemUnknownException("The public deployment system " 
                    + storageSystem.getSystemId() 
                    + " is not currently available. Cloning of apps whose "
                    + "assets reside on this system will fail until " 
                    + "the system comes back online.");
        } 
        // if the system is in down time or otherwise unavailable...
        else if (storageSystem.getStatus() != SystemStatusType.UP)
        {
            throw new SystemUnavailableException("The public deployment system " 
                    + storageSystem.getSystemId() 
                    + " is currently " + storageSystem.getStatus().name() 
                    + ". Cloning of apps whose assets reside on this system will fail until " 
                    + "the system returns to service.");
        }
    }
    
    /**
     * Reliably transfers the zipped {@link Software} archive to the remote {@link StoragSystem} and
     * sets the checksum of the file for future reference.
     * 
     * @param clonedSoftwareDataClient
     * @param localCachedSoftwareDeploymentSystem
     * @throws FileNotFoundException
     * @throws IOException
     * @throws RemoteDataException
     * @throws TransferException
     */
    private void copyLocalCachedDeploymentPathToClonedSoftwareDeploymentSystem(Software clonedSoftware, RemoteDataClient clonedSoftwareDataClient, File localCachedSoftwareDeploymentSystem)
    throws FileNotFoundException, IOException, RemoteDataException, TransferException 
    {
        TransferTask transferTask = new TransferTask(
                "agave://" + getEntity().getStorageSystem().getSystemId() + "/" + getEntity().getDeploymentPath(), 
                "agave://" + clonedSoftware.getStorageSystem().getSystemId() + "/" + clonedSoftware.getDeploymentPath(), 
                clonedSoftware.getOwner(), 
                null, null);
        TransferTaskDao.persist(transferTask);
        
        try {
            // rename the local folder so it will copy to the remote system with the proper folder name
            File tempDir = new File(Settings.TEMP_DIRECTORY);
            if (!tempDir.exists() || !tempDir.isDirectory()) {
                tempDir.mkdirs();
            }
            File namedLocalDeploymentPath = new File(tempDir, FilenameUtils.getName(clonedSoftware.getDeploymentPath()));
            localCachedSoftwareDeploymentSystem.renameTo(namedLocalDeploymentPath);
            
            RemoteTransferListener listener = new RemoteTransferListener(transferTask);
            
            // copy the properly named local cache folder to the parent directory of the deployment path
            // on the remote system so we guarantee the folder name preserves.
            clonedSoftwareDataClient.put(namedLocalDeploymentPath.getAbsolutePath(), FilenameUtils.getPathNoEndSeparator(clonedSoftware.getDeploymentPath()), listener);
            
        } catch (Exception e) {
            throw new SoftwareException("Failed to copy the deploymentPath for " + getEntity().getUniqueName() + 
            		" on " +  getEntity().getStorageSystem().getSystemId() + 
            		" to the new deployment folder on " + clonedSoftware.getStorageSystem().getSystemId(), e);
        } finally {
            try {
                if (clonedSoftwareDataClient.isPermissionMirroringRequired()) {
                    if (clonedSoftwareDataClient.doesExist(clonedSoftware.getDeploymentPath())) {
                        clonedSoftwareDataClient.setOwnerPermission(clonedSoftwareDataClient.getUsername(), clonedSoftware.getDeploymentPath(), true);
                        clonedSoftwareDataClient.setOwnerPermission(clonedSoftware.getOwner(), clonedSoftware.getDeploymentPath(), true);
                    }
                }
            } catch (Exception e) {
                log.error("Failed to set app deploymentPath permissions on " + clonedSoftware.getDeploymentPath() + 
                        " for app " + clonedSoftware.getUniqueName() + " on behalf of user " + clonedSoftware.getOwner(), e);
            }
        }
        
    }

    /**
     * Calculates the {@link Software#getDeploymentPath()} to which the cloned app assets will be copied.
     * 
     * @param clonedSoftware
     * @param publishedSoftwareDataClient
     * @return  {@link Software#getDeploymentPath()} for the cloned app
     * 
     * @throws IOException
     * @throws RemoteDataException
     * @throws PermissionException 
     */
    protected String resolveAndCreatePublishedDeploymentPath(Software clonedSoftware, RemoteDataClient publishedSoftwareDataClient) 
    throws IOException, RemoteDataException, PermissionException 
    {
        String remoteDeploymentPath = this.clonedSoftwareDeploymentPath;
        if (StringUtils.isEmpty(remoteDeploymentPath)) {
            remoteDeploymentPath = String.format("%s/applications/%s", 
                                                clonedSoftware.getOwner(), 
                                                clonedSoftware.getUniqueName());
        }
        
        LogicalFile logicalFile = null;
        try {
            logicalFile=LogicalFileDao.findBySystemAndPath(clonedSoftware.getStorageSystem(), 
                                                            publishedSoftwareDataClient.resolvePath(remoteDeploymentPath));
        } catch(Exception e) {}
        
        PermissionManager pm = new PermissionManager(clonedSoftware.getStorageSystem(), 
                publishedSoftwareDataClient,
                logicalFile, 
                clonedSoftware.getOwner());
        
        // resolve the path to the folder for the original apps on the app's storage system
        // note that this may change over time, but it's saved with the app, so that's fine.
        if (!publishedSoftwareDataClient.doesExist(remoteDeploymentPath)) {
            if (pm.canReadWrite(publishedSoftwareDataClient.resolvePath(remoteDeploymentPath))) {
                publishedSoftwareDataClient.mkdirs(remoteDeploymentPath, clonedSoftware.getOwner());
            } else {
                throw new PermissionException(
                        "User does not have permission to access the deployment path " + 
                        remoteDeploymentPath + " on the target " + 
                        "deployment system. Verify you have been granted at lease  " + PermissionType.READ_WRITE +
                        " permission on the target execution system " + clonedSoftware.getStorageSystem().getSystemId());
            }
        } else if (!pm.canReadWrite(publishedSoftwareDataClient.resolvePath(remoteDeploymentPath))) {
            throw new PermissionException(
                    "User does not have permission to access the deployment path " + 
                    remoteDeploymentPath + " on the target " + 
                    "deployment system. Verify you have been granted at lease  " + PermissionType.READ_WRITE +
                    " permission on the target execution system " + clonedSoftware.getStorageSystem().getSystemId());
        }
        
        
        return remoteDeploymentPath;
    }       
        
    /**
     * Copies the zip archive for the public {@link Software} instance to the local file system 
     * for unpacking and relaying to the cloned {@link Software#getDeploymentPath()}.
     * 
     * @return {@link File} reference to the location of the cached deployment path on the local system.
     * @throws IOException 
     */
    protected File fetchPublicAppArchiveFromDeploymentSystem(RemoteDataClient sourceSoftwareDataClient) 
    throws IOException 
    {  
        File tempAppDir = Files.createTempDir();
        
        try {
            sourceSoftwareDataClient.get(getEntity().getDeploymentPath(), tempAppDir.getAbsolutePath());
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
        
        // now copy the contents of the deployment folder to the parent dir, which is tempAppDir
        File copiedDeploymentFolder = new File(tempAppDir, FilenameUtils.getName(getEntity().getDeploymentPath()));
        if (!copiedDeploymentFolder.getAbsoluteFile().equals(tempAppDir) && copiedDeploymentFolder.exists() && copiedDeploymentFolder.isDirectory()) {
            FileUtils.copyDirectory(copiedDeploymentFolder, tempAppDir, null, true);
            // now delete the redundant deployment folder
            FileUtils.deleteQuietly(copiedDeploymentFolder);
        }
        
        // validate the checksum to make sure the app itself hasn't  changed
        File zippedFile = new File(tempAppDir, FilenameUtils.getName(getEntity().getDeploymentPath()));
        String checksum = MD5Checksum.getMD5Checksum(zippedFile);
        if (getEntity().getChecksum() == null || StringUtils.equals(checksum, getEntity().getChecksum()))
        {
            ZipUtil.unzip(zippedFile, tempAppDir);
            if (tempAppDir.list().length > 1) {
                zippedFile.delete();
            } else {
                throw new SoftwareException("Failed to unpack the application bundle.");
            }
        } 
        else 
        {
            throw new SoftwareException("The checksum calculated for the current public app bundle " +
                    "does not match the value calculated when the app was originally published. " +
                    "This may indicate data corruption. If this problem persists, please contact your " +
                    "tenant administrator for help resolving the problem.");
        }
        
        File localDeploymentPath = new File(tempAppDir, FilenameUtils.getName(getEntity().getExecutablePath()));

        // check to see whether the zip archive bundled everything into a top level folder. We can
        // identify the root of the deploymentPath in the zipped archive by finding the first parent
        // which properly resolves the wrapper template relative path
        if (localDeploymentPath.exists()) {
            // the zipped archive expanded the contents into the temp directory. No wrapping
            // folder was present. use the temp dir as is.
            return tempAppDir;
        } 
        else
        {
            // need to go searching for the path. no idea how this could happen
            for (File child: tempAppDir.listFiles()) 
            {
                if (child.isDirectory()) {
                    File buriedWrapperTemplateFile = new File(child, getEntity().getExecutablePath());
                    if (buriedWrapperTemplateFile.exists()) {
                        File deploymentSubPath = Files.createTempDir();
                        FileUtils.moveDirectory(child, deploymentSubPath);
                        FileUtils.deleteDirectory(tempAppDir);
                        return deploymentSubPath;
                    }
                }
            }
            
            log.error("Unable to find deployment path for public app " + getEntity().getUniqueName() + " while attempting to clone the app.");
            throw new SoftwareException("Unable to find the deployment path for the public app " + getEntity().getUniqueName() + " while attempting to clone.");
        }
    }
    
    /**
     * Copies the {@link Software#getDeploymentPath()} from {@link Software#getStorageSystem()} 
     * to the local system for bundling and archiving.
     * 
     * @return {@link File} reference to the location of the cached deployment path on the local system.
     */
    protected File fetchPrivateAppArchiveFromDeploymentSystem(RemoteDataClient sourceSoftwareDataClient) 
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
     * Creates a {@link RemoteDataClient} to a {@link RemoteSystem} representing either the 
     * source or destination {@link Software#getStorageSystem()}. The {@link RemoteDataClient} is then
     * authenticated and returned for use.
     * 
     * @param remoteSystem
     * @return
     * @throws RemoteDataException
     * @throws RemoteCredentialException
     */
    protected RemoteDataClient getRemoteDataClient(RemoteSystem remoteSystem) 
    throws RemoteDataException, RemoteCredentialException
    {    
        // get a handle on the public default storage system
        RemoteDataClient destDataClient = remoteSystem.getRemoteDataClient();
        
        try 
        {
            destDataClient.authenticate();
            
            return destDataClient;
        }
        catch (Exception e) {
            throw new SoftwareException("Failed to authenticate to the remote system, " 
                    + remoteSystem.getSystemId(), e);
        }
    }
    

    public String getClonedSoftwareOwner() {
        return clonedSoftwareOwner;
    }

    public void setClonedSoftwareOwner(String clonedSoftwareOwner) {
        this.clonedSoftwareOwner = clonedSoftwareOwner;
    }

    /**
     * @return
     */
    public Software getClonedSoftware() {
        return this.clonedSoftware;
    }

    /**
     * @param clonedSoftware the clonedSoftware to set
     */
    public void setClonedSoftware(Software clonedSoftware) {
        this.clonedSoftware = clonedSoftware;
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
    
}
