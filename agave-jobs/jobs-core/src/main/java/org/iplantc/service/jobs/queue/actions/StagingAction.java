/**
 * 
 */
package org.iplantc.service.jobs.queue.actions;

import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URI;
import java.nio.channels.ClosedByInterruptException;
import java.util.Date;
import java.util.Map;
import java.util.regex.Matcher;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.apps.dao.SoftwareDao;
import org.iplantc.service.apps.model.Software;
import org.iplantc.service.common.exceptions.PermissionException;
import org.iplantc.service.common.uri.AgaveUriRegex;
import org.iplantc.service.common.uri.UrlPathEscaper;
import org.iplantc.service.io.dao.LogicalFileDao;
import org.iplantc.service.io.model.LogicalFile;
import org.iplantc.service.io.permissions.PermissionManager;
import org.iplantc.service.jobs.dao.JobDao;
import org.iplantc.service.jobs.exceptions.JobDependencyException;
import org.iplantc.service.jobs.exceptions.JobException;
import org.iplantc.service.jobs.exceptions.MissingDataException;
import org.iplantc.service.jobs.managers.JobManager;
import org.iplantc.service.jobs.managers.JobPermissionManager;
import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.jobs.model.JobEvent;
import org.iplantc.service.jobs.model.enumerations.JobStatusType;
import org.iplantc.service.jobs.util.Slug;
import org.iplantc.service.systems.dao.SystemDao;
import org.iplantc.service.systems.exceptions.SystemUnavailableException;
import org.iplantc.service.systems.exceptions.SystemUnknownException;
import org.iplantc.service.systems.model.ExecutionSystem;
import org.iplantc.service.systems.model.RemoteSystem;
import org.iplantc.service.systems.model.enumerations.SystemStatusType;
import org.iplantc.service.systems.util.ApiUriUtil;
import org.iplantc.service.transfer.RemoteDataClient;
import org.iplantc.service.transfer.RemoteDataClientFactory;
import org.iplantc.service.transfer.URLCopy;
import org.iplantc.service.transfer.dao.TransferTaskDao;
import org.iplantc.service.transfer.exceptions.AuthenticationException;
import org.iplantc.service.transfer.exceptions.RemoteDataException;
import org.iplantc.service.transfer.model.TransferTask;
import org.joda.time.DateTime;

/**
 * @author dooley
 *
 */
public class StagingAction extends AbstractWorkerAction {
    
    private static Logger log = Logger.getLogger(StagingAction.class);
    
    public StagingAction(Job job) {
        super(job);
    }
    
    /**
     * This method attempts to archive a job's output by retirieving the
     * .agave.archive shadow file from the remote job directory and staging
     * everything not in there to the user-supplied Job.archivePath on the 
     * Job.archiveSystem
     * 
     * @param job
     * @throws SystemUnavailableException
     * @throws SystemUnknownException
     * @throws JobException
     * @throws JobDependencyException 
     */
    public void run() 
    throws SystemUnavailableException, SystemUnknownException, JobException, 
            ClosedByInterruptException, JobDependencyException
    {
        ExecutionSystem system = (ExecutionSystem) new SystemDao().findBySystemId(this.job.getSystem());
        
        log.debug("Beginning staging inputs for job " + this.job.getUuid() + " " + this.job.getName());
        // we need a way to parallelize this task. Ideally we'd just throw each input
        // file to the staging queue and let 'em rip 
        JobManager.updateStatus(this.job, JobStatusType.PROCESSING_INPUTS);
        
        // Get a well-formed map of user-supplied + default + hidden/required inputs for the job
        // Inputs are stored as a JSON object with the Job table, so we use this convenience
        // method to make it less nasty to work with
        Map<String, String[]> jobInputMap = JobManager.getJobInputMap(getJob());
        
        for (String inputKey : jobInputMap.keySet())
        {
            checkStopped();
            
            String[] rawInputValues = jobInputMap.get(inputKey);
            
            URI singleRawInputUri = null;
            RemoteSystem remoteStorageSystem = null;
            RemoteDataClient remoteStorageDataClient = null;
            RemoteDataClient remoteExecutionDataClient = null;
            try
            {
                // inputs can have multiple values provided to them by the user and/or their defaults.
                // here we iterate over each input value, staging them to the remote system.
                for (String singleRawInputValue: rawInputValues) 
                {   
                    checkStopped();
                    
                    singleRawInputUri = new URI(singleRawInputValue);
                    String remotePath = null;
                    
                    if (ApiUriUtil.isInternalURI(singleRawInputUri))
                    {
                        remoteStorageSystem = ApiUriUtil.getRemoteSystem(this.job.getOwner(), singleRawInputUri);
                        
                        if (remoteStorageSystem == null) 
                        {
                            throw new SystemUnknownException("Unable to stage input " + singleRawInputValue + ". No system " + 
                                    "was found for user " + this.job.getOwner() + " satisfying the URI.");
                        } 
                        else if (remoteStorageSystem == null || !remoteStorageSystem.isAvailable() || !remoteStorageSystem.getStatus().equals(SystemStatusType.UP))
                        {
                            throw new SystemUnavailableException("Unable to stage input " + singleRawInputValue + " from system " + 
                                    remoteStorageSystem.getSystemId() + ". The system is currently unavailable.");
                        } 
                        else if (system == null || !system.isAvailable() || !system.getStatus().equals(SystemStatusType.UP))
                        {
                            throw new SystemUnavailableException("Unable to stage input " + singleRawInputValue + " to execution system " + 
                                    remoteStorageSystem.getSystemId() + ". The system is currently unavailable.");
                        } 
                        else 
                        {
                            try
                            {
                                remoteStorageDataClient = remoteStorageSystem.getRemoteDataClient(this.job.getInternalUsername());
                                remoteStorageDataClient.authenticate();
                            }
                            catch (Exception e) 
                            {
                                throw new AuthenticationException("Unable to stage input " + singleRawInputValue + 
                                        ". Failed to authenticate to " + remoteStorageSystem.getSystemId(), e);
                            }
                        }
                        
                        String absolutePath = UrlPathEscaper.decode(ApiUriUtil.getAbsolutePath(this.job.getOwner(), singleRawInputUri));
                        
                        LogicalFile logicalFile = LogicalFileDao.findBySystemAndPath(
                                remoteStorageSystem, absolutePath);
                        
                        PermissionManager pm = new PermissionManager(
                                remoteStorageSystem, remoteStorageDataClient, logicalFile, this.job.getOwner());
                        
                        if (logicalFile == null) {
                            remotePath = new LogicalFile(this.job.getOwner(), remoteStorageSystem, absolutePath)
                                                .getAgaveRelativePathFromAbsolutePath();
                        } else {
                            // normalize remote path to relative path for copying later on
                            remotePath = logicalFile.getAgaveRelativePathFromAbsolutePath();
                        }
                        
                        if (remoteStorageDataClient.doesExist(remotePath))
                        {  
                            if (!pm.canRead(absolutePath))
                            {
                            	// the file permission check won't catch the job permissions implicitly granted on
                            	// the output folder because we don't have a complete manifest of the job output folder.
                            	// If file permissions fail, we still need to manually check the job permissions.
                            	if (AgaveUriRegex.JOBS_URI.matches(singleRawInputUri)) {
                            		Matcher jobOutputMatcher = AgaveUriRegex.getMatcher(singleRawInputUri);
                            		String referencedJobIdFromInputUrl = jobOutputMatcher.group(1);
                            		Job referenceJob = JobDao.getByUuid(referencedJobIdFromInputUrl);
                            		
                            		if (!new JobPermissionManager(referenceJob, this.job.getOwner()).canRead(this.job.getOwner())) {
                            			throw new PermissionException("User does not have permission to access " + 
                                                "the output folder of job " + referencedJobIdFromInputUrl);
                            		}
                            	}
                            	else {
	                                throw new PermissionException("User does not have permission to access " + 
	                                        remotePath + " on " + remoteStorageSystem.getSystemId());
                            	}
                            }
                        }
                        else
                        {
                            if (AgaveUriRegex.JOBS_URI.matches(singleRawInputUri)) 
                            {
                                Matcher jobOutputMatcher = AgaveUriRegex.getMatcher(singleRawInputUri);
                                throw new MissingDataException("Unable to locate " + jobOutputMatcher.group(2) + 
                                        " in output/archive directory of job " + jobOutputMatcher.group(1));
                            } 
                            else 
                            {
                                throw new MissingDataException("Unable to locate " + 
                                        remotePath + " on " + remoteStorageSystem.getSystemId());
                            }
                        }
                    }
                    else
                    {
                        remoteStorageDataClient = new RemoteDataClientFactory().getInstance(
                                this.job.getOwner(), this.job.getInternalUsername(), singleRawInputUri);
                        
                        if (remoteStorageDataClient == null) 
                        {
                            throw new SystemUnknownException("Unable to stage input " + singleRawInputValue + ". No system " + 
                                    "was found for user " + this.job.getOwner() + " satisfying the URI.");
                        } 
                        
                        try
                        {
                            remoteStorageDataClient.authenticate();
                        }
                        catch (Exception e) 
                        {
                            throw new AuthenticationException("Unable to stage input " + singleRawInputValue + 
                                    ". Failed to authenticate to remote system" + remoteStorageDataClient.getHost(), e);
                        }
                        
                        remotePath = singleRawInputUri.getPath();
                    }
                    
                    // copy to remote execution work directory
                    remoteExecutionDataClient = system.getRemoteDataClient(this.job.getInternalUsername());
                    
                    try
                    {
                        remoteExecutionDataClient.authenticate();
                    }
                    catch (Exception e) 
                    {
                        throw new AuthenticationException("Unable to stage input " + singleRawInputValue + 
                                ". Failed to authenticate to execution system " + system.getSystemId(), e);
                    }
                    
                    Software software = SoftwareDao.getSoftwareByUniqueName(this.job.getSoftwareName());
                    
                    String remoteWorkPath = null;
                    
                    if (StringUtils.isNotEmpty(software.getExecutionSystem().getScratchDir())) {
                        remoteWorkPath = software.getExecutionSystem().getScratchDir();
                    } else if (!StringUtils.isEmpty(software.getExecutionSystem().getWorkDir())) {
                        remoteWorkPath = software.getExecutionSystem().getWorkDir();
                    }
                    
                    if (!StringUtils.isEmpty(remoteWorkPath)) {
                        if (!remoteWorkPath.endsWith("/")) remoteWorkPath += "/";
                    } else {
                        remoteWorkPath = "";
                    }
                    
                    remoteWorkPath += this.job.getOwner() +
                            "/job-" + this.job.getUuid() + "-" + Slug.toSlug(job.getName());
                    
                    try
                    {
                        if (!remoteExecutionDataClient.doesExist(remoteWorkPath)) {
                            remoteExecutionDataClient.mkdirs(remoteWorkPath, job.getOwner());
                        }
                    } 
                    catch (RemoteDataException e) 
                    {
                        if (e.getMessage().toLowerCase().contains("permission")) {
                            throw new RemoteDataException("Unable to create the remote job directory " + 
                                    remoteWorkPath + " on " + system.getSystemId() + 
                                    ". System responeded with a permission denied error. Please make sure " +
                                    "you have permission to write to " + 
                                    remoteExecutionDataClient.resolvePath(remoteWorkPath) + 
                                    " and try again.", e);
                        } else {
                            throw new RemoteDataException("Failed to create the remote job directory " + 
                                    remoteWorkPath + " on " + system.getSystemId(), e);
                        }
                    }
                    catch (Exception e)
                    {
                        throw new RemoteDataException("Failed to create the remote job directory " + 
                                remoteWorkPath + " on " + system.getSystemId(), e);
                    }
                    
                    this.job.setWorkPath(remoteWorkPath);
//                  this.job.setStatus(JobStatusType.STAGING_INPUTS, "Staging " + singleRawInputValue + " to remote job directory");
                    String destPath = remoteWorkPath;
                    
                    if (remoteExecutionDataClient.doesExist(destPath)) 
                    {
                        if (remoteExecutionDataClient.isDirectory(destPath)) 
                        {
                            if (StringUtils.isEmpty(destPath)) {
                                destPath = FilenameUtils.getName(remotePath);
                            } else if (StringUtils.endsWith(destPath, "/")) {
                                destPath += FilenameUtils.getName(remotePath);
                            } else {
                                destPath += "/" + FilenameUtils.getName(remotePath);
                            }
                        }
                    }
                    else if (!remoteExecutionDataClient.doesExist(destPath + (StringUtils.isEmpty(destPath) ? ".." : "/..")))
                    {
                        throw new FileNotFoundException("Job work directory not found.");
                    }
                    
                    checkStopped();
                    
                    // see if we can skip this transfer due to prior success
                    
                    boolean skipTransfer = false;
                    // if destination is a file and it was already transferred
                    if (remoteExecutionDataClient.doesExist(destPath) && 
                    		remoteExecutionDataClient.length(destPath) == remoteStorageDataClient.length(remotePath)) {
                    	
                		// verify the checksums are the same before skipping?
                		try {
                			String sourceChecksum = remoteStorageDataClient.checksum(remotePath);
                			String destChecksum = remoteExecutionDataClient.checksum(destPath);
                			
                			if (StringUtils.equals(sourceChecksum, destChecksum)) {
                				// they're the same! skip this transfer
                				log.debug("Input file " + singleRawInputValue + " of idential size was found in the work folder of job "
        						+ job.getUuid() + ". The checksums were identical. This input will not be recopied.");
                				
                				skipTransfer = true;
                			}
                			else {
                				log.debug("Input file " + singleRawInputValue + " of idential size was found in the work folder of job "
                						+ job.getUuid() + ". The checksums did not match, so the input file will be transfered to the "
                						+ "target system and overwrite the existing file.");
                			}
                		} 
                		catch (NotImplementedException  e) {
                			// should we find another way, or just copy the files?
                			// we'll err on the side of caution and recopy
                			log.debug("Input file " + singleRawInputValue + " of idential size was found in the work folder of job "
            						+ job.getUuid() + ". Unable to calculate checksums. This input will not be recopied.");
                			
                			skipTransfer = true;
                		}
                		catch (Throwable e) {
                			// couldn't calculate the checksum due to server side error
                			// we'll err on the side of caution and recopy
                			log.debug("Input file " + singleRawInputValue + " of idential size was found in the work folder of job "
            						+ job.getUuid() + ". Unable to calculate checksums. This input will not be recopied.");
                		}
                		finally {
                			if (skipTransfer) {
                                try { remoteExecutionDataClient.disconnect(); } catch (Exception e) {}
                                try { remoteStorageDataClient.disconnect(); } catch(Exception e) {};
                                
                                continue;
                			}
                		}
                    }
                    
                    
                    // nope. still have to copy them. proceed
                    TransferTask rootTask = new TransferTask(
                            singleRawInputValue, 
                            "agave://" + this.job.getSystem() + "/" + destPath, 
                            this.job.getOwner(), 
                            null, 
                            null);
                    
                    TransferTaskDao.persist(rootTask);
                    
                    JobEvent event = new JobEvent(
                            JobStatusType.STAGING_INPUTS, 
                            "Copy in progress", 
                            rootTask, 
                            this.job.getOwner());
                    
                    this.job.setStatus(JobStatusType.STAGING_INPUTS, event);
                    this.job.setLastUpdated(new DateTime().toDate());
                    JobDao.persist(this.job);
                    
                    urlCopy = new URLCopy(remoteStorageDataClient, remoteExecutionDataClient);
                    
                    try
                    {
                    	rootTask = urlCopy.copy(remotePath, destPath, rootTask);
                    }
                    catch (ClosedByInterruptException e) {
                        throw e;
                    }
                    catch (Exception e)
                    {   
                        // we may not be able to kill the gridftp threads associated with this transfer,
                        // so in that event, the transfer will time out and we can catch the exception 
                        // here to rethrow as a ClosedByInterruptException.
                        checkStopped();
                        if (urlCopy.isKilled()) {
                            throw new ClosedByInterruptException();
                        }
                        throw new RemoteDataException("Failed to transfer input " + singleRawInputValue, e);
                    }
                    finally 
                    {
                        try { remoteExecutionDataClient.disconnect(); } catch (Exception e) {}
                        try { remoteStorageDataClient.disconnect(); } catch(Exception e) {};
                    }
                }
            }
            catch (ClosedByInterruptException e) {
                throw e;
            }
            catch (MissingDataException e) {
                log.error("Failed to locate input for job " + this.job.getUuid(), e);
                throw new JobDependencyException(e.getMessage(), e);
            }
            catch (SystemUnknownException e) {
                log.error("Failed to locate a user provided input system for job " + this.job.getUuid(), e);
                throw new JobDependencyException(e.getMessage(), e);
            }
            catch (PermissionException e) {
                log.error("User lacks permissions to access input for job " + this.job.getUuid(), e);
                throw new JobDependencyException(e.getMessage(), e);
            }
            catch (MalformedURLException e) 
            {
                log.error("Invalid input string provided for job " + this.job.getUuid(), e);
                throw new JobDependencyException(e.getMessage(), e);
            }
            catch (AuthenticationException e) 
            {
                log.error("Unable to authenticate to stage input file for job " + this.job.getUuid(), e);
                throw new JobDependencyException(e.getMessage(), e);
            }
            catch (NotImplementedException e) {
                log.error("Invalid input protocol provided for job " + this.job.getUuid(), e);
                throw new JobDependencyException(e.getMessage(), e);
            }
            catch (Throwable e)
            {
                log.error("Failed to stage input for job " + this.job.getUuid(), e);
                throw new JobException(e.getMessage(), e);
            }
            finally
            {
                try { remoteExecutionDataClient.disconnect(); } catch (Exception e) {}
                try { remoteStorageDataClient.disconnect(); } catch(Exception e) {};
            }
        }
                
        if (!isStopped()) {
            // status should have been updated in job object if anything was
            // staged
            if (this.job.getStatus() == JobStatusType.STAGING_INPUTS)
            {
                JobManager.updateStatus(this.job, JobStatusType.STAGED);
                log.debug("Completed staging inputs for job " + this.job.getUuid() + " " + this.job.getName());
            }
        }
        
    }
}
