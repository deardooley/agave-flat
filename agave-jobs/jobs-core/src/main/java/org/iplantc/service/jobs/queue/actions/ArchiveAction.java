/**
 * 
 */
package org.iplantc.service.jobs.queue.actions;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.channels.ClosedByInterruptException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.StaleObjectStateException;
import org.iplantc.service.jobs.dao.JobDao;
import org.iplantc.service.jobs.exceptions.JobException;
import org.iplantc.service.jobs.managers.JobManager;
import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.jobs.model.JobEvent;
import org.iplantc.service.jobs.model.JobUpdateParameters;
import org.iplantc.service.jobs.model.enumerations.JobStatusType;
import org.iplantc.service.jobs.phases.workers.IPhaseWorker;
import org.iplantc.service.systems.exceptions.SystemUnavailableException;
import org.iplantc.service.systems.exceptions.SystemUnknownException;
import org.iplantc.service.systems.model.ExecutionSystem;
import org.iplantc.service.systems.model.RemoteSystem;
import org.iplantc.service.systems.model.enumerations.SystemStatusType;
import org.iplantc.service.transfer.RemoteDataClient;
import org.iplantc.service.transfer.URLCopy;
import org.iplantc.service.transfer.dao.TransferTaskDao;
import org.iplantc.service.transfer.exceptions.TransferException;
import org.iplantc.service.transfer.model.TransferTask;
import org.iplantc.service.transfer.model.enumerations.TransferStatusType;
import org.joda.time.DateTime;

import com.google.common.io.Files;

/**
 * @author dooley
 *
 */
public class ArchiveAction extends AbstractWorkerAction {
    
    private static Logger log = Logger.getLogger(ArchiveAction.class);
    
    public ArchiveAction(Job job, IPhaseWorker worker) {
        super(job, worker);
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
    throws SystemUnavailableException, SystemUnknownException, JobException, ClosedByInterruptException
    {
        ExecutionSystem executionSystem = JobManager.getJobExecutionSystem(this.job);
        
        if (executionSystem == null || !executionSystem.isAvailable()) {
            throw new SystemUnavailableException("Job execution system " 
                    + job.getSystem() + " is not currently available");
        } 
        // if the system is in down time or otherwise unavailable...
        else if (executionSystem.getStatus() != SystemStatusType.UP)
        {
            throw new SystemUnavailableException("Job execution system " 
                    + executionSystem.getSystemId() + " is currently " 
                    + executionSystem.getStatus());
        }
        
        log.debug("Beginning archive outputs for job " + getJob().getUuid() + " " + getJob().getName());
        
        RemoteDataClient archiveDataClient = null;
        RemoteDataClient executionDataClient = null;
        RemoteSystem remoteArchiveSystem = null;
        
        // we should be able to archive from anywhere. Given that we can stage in condor 
        // job data from remote systems, we should be able to stage it out as well. At 
        // this point we are guaranteed that the worker running this bit of code has
        // access to the job output folder. The RemoteDataClient abstraction will handle
        // the rest.
        File tempDir = Files.createTempDir();
        File agaveIgnoreFile = null;
        try 
        {
            try 
            {
                executionDataClient = executionSystem.getRemoteDataClient(getJob().getInternalUsername());
                executionDataClient.authenticate();
            }
            catch (Exception e) 
            {
                throw new JobException("Failed to authenticate to the execution system " 
                        + executionSystem.getSystemId());
            }
            
            // copy remote archive file to temp space
            String remoteAgaveIgnoreFile = getJob().getWorkPath() + File.separator + ".agave.archive";
            
            agaveIgnoreFile = new File(tempDir, ".agave.archive");
            
            // pull remote .archive file and parse it for a list of paths relative
            // to the job.workDir to exclude from archiving. Generally this will be
            // the application binaries, but the app itself may have added or removed
            // things from this file, so we need to process it anyway.
            List<String> jobFileList = new ArrayList<String>();
            try
            {
                if (!executionDataClient.doesExist(getJob().getWorkPath())) {
                    throw new TransferException("Job work directory no longer exists.", new FileNotFoundException());
                }
                else if (executionDataClient.doesExist(remoteAgaveIgnoreFile) && 
                        executionDataClient.isFile(remoteAgaveIgnoreFile)) 
                { 
                    executionDataClient.get(remoteAgaveIgnoreFile, agaveIgnoreFile.getAbsolutePath());
                    
                    // read it in to find the original job files
                    if (agaveIgnoreFile.exists() && agaveIgnoreFile.length() > 0) {
                        for (String ignoreItem: FileUtils.readLines(agaveIgnoreFile)) {
                            jobFileList.add(getJob().getWorkPath() + File.separator + ignoreItem);
                        }
                    }
                }
                
                if (jobFileList.isEmpty()) 
                {
                    if (log.isDebugEnabled())
                        log.debug("No archive file found for job " + getJob().getUuid() + " on system " + 
                            executionSystem.getSystemId() + " at " + remoteAgaveIgnoreFile + 
                            ". Entire job directory will be archived.");
                    this.job = JobManager.updateStatus(getJob(), JobStatusType.ARCHIVING, 
                            "No archive file found. Entire job directory will be archived.");
                }
            } 
            catch (TransferException e) {
                throw new JobException("Failed to archive job output directory " + getJob().getWorkPath() + 
                        " to " + getJob().getArchivePath() + 
                        ". Job output directory no longer exists.", e);
            }
            catch (Exception e)
            {
                if (log.isDebugEnabled())
                    log.debug("Unable to parse archive file for job " + getJob().getUuid() + " on system " + 
                        executionSystem.getSystemId() + " at " + remoteAgaveIgnoreFile + 
                        ". Entire job directory will be archived.");
                this.job = JobManager.updateStatus(getJob(), JobStatusType.ARCHIVING, 
                        "Unable to parse job archive file. Entire job directory will be archived.");
            }
            
            remoteArchiveSystem = getJob().getArchiveSystem();
            
            if (remoteArchiveSystem == null) 
            {
                throw new SystemUnknownException("Unable to archive job output. No archive system could be found.");
            } 
            else if (!remoteArchiveSystem.isAvailable() || !remoteArchiveSystem.getStatus().equals(SystemStatusType.UP))
            {
                throw new SystemUnavailableException("Unable to archive job output from system " + 
                        remoteArchiveSystem.getSystemId() + ". The system is currently unavailable.");
            } 
            else 
            {
                try 
                {
                    archiveDataClient = remoteArchiveSystem.getRemoteDataClient(getJob().getInternalUsername());
                    archiveDataClient.authenticate();
                }
                catch (Exception e) 
                {
                    throw new JobException("Failed to authenticate to the archive system " 
                            + remoteArchiveSystem.getSystemId(), e);
                }
            }
            
            try
            {
                if (StringUtils.isEmpty(getJob().getArchivePath())) {
                    String defaultArchivePath = getJob().getOwner() + "/archive/jobs/job-" + getJob().getUuid();
                    getJob().setArchivePath(defaultArchivePath);
                    JobUpdateParameters jobUpdateParameters = new JobUpdateParameters();
                    jobUpdateParameters.setArchivePath(defaultArchivePath);
                    JobDao.update(getJob(), jobUpdateParameters);
                }
                
                if (!archiveDataClient.doesExist(getJob().getArchivePath())) {
                    archiveDataClient.mkdirs(getJob().getArchivePath(), job.getOwner());
                }
            }
            catch (Exception e) 
            {
                throw new JobException("Failed to create archive directory " 
                        + getJob().getArchivePath() + " on " + remoteArchiveSystem.getSystemId(), e);
            }
            
            // iterate over the work folder and archive everything that wasn't
            // listed in the archive file. We use URL copy here to abstract the 
            // third party transfer we would like to do. If possible, URLCopy will
            // do a 3rd party transfer. When not possible, such as when we're going
            // cross-protocol, it will proxy the transfer.
            this.rootTask = new TransferTask(
                    "agave://" + getJob().getSystem() + "/" + getJob().getWorkPath(), 
                    "agave://" + getJob().getArchiveSystem().getSystemId() + "/" +getJob().getArchivePath(), 
                    getJob().getOwner(), 
                    null, 
                    null);
            TransferTaskDao.persist(rootTask);
            
            getJob().addEvent(new JobEvent(
                    getJob().getStatus(), 
                    "Archiving " + rootTask.getSource() + " to " + rootTask.getDest(), 
                    rootTask, 
                    getJob().getOwner()));
            
            urlCopy = new URLCopy(executionDataClient, archiveDataClient);
            
            try 
            {
                rootTask = urlCopy.copy(getJob().getWorkPath(), getJob().getArchivePath(), rootTask, jobFileList);
            } 
            catch (ClosedByInterruptException e) {
               throw e; 
            } 
            catch (TransferException e) {
                throw new JobException("Failed to archive job output directory " + getJob().getWorkPath() + 
                        " to " + getJob().getArchivePath() + 
                        " due to an error persisting the transfer record.", e);
            }
            catch (Exception e) {
                throw new JobException("Failed to archive file " + getJob().getWorkPath() + 
                        " to " + getJob().getArchivePath() + 
                        " due to an error during transfer ", e);
            }
            
            try 
            {
                if (getJob().getStatus() == JobStatusType.ARCHIVING) {
                    rootTask.setStatus(TransferStatusType.COMPLETED);
                } else {
                    rootTask.setStatus(TransferStatusType.FAILED);
                }
                
                rootTask.setEndTime(new DateTime().toDate());
                
                TransferTaskDao.persist(rootTask);
            }
            catch (Exception e) {
                
            }
            
            // if it all worked as expected, then delete the job work directory
            try 
            {
                if (!isStopped()) {
                    executionDataClient.delete(getJob().getWorkPath());
                    this.job = JobManager.updateStatus(getJob(), JobStatusType.ARCHIVING_FINISHED, 
                                                       "Job archiving completed successfully.");
                }
            }
            catch (Exception e) {
                log.error("Archiving of job " + getJob().getUuid() + " completed, "
                    + "but an error occurred deleting the remote work directory " 
                    + getJob().getUuid(), e);
            }
        }
        catch (StaleObjectStateException e) {
            throw e;
        }
        catch (ClosedByInterruptException e) 
        {
            if (getUrlCopy() != null) getUrlCopy().setKilled(true);
            throw e;
        }
        catch (SystemUnavailableException | SystemUnknownException | JobException e) 
        {
            throw e;
        }
        catch (Exception e) {
            throw new JobException("Failed to archive data due to internal failure.", e);
        }
        finally 
        {
            // clean up the local archive file
            FileUtils.deleteQuietly(tempDir);
            try {
                if (archiveDataClient.isPermissionMirroringRequired() && StringUtils.isEmpty(getJob().getInternalUsername())) {
                    archiveDataClient.setOwnerPermission(getJob().getOwner(), getJob().getArchivePath(), true);
                }
            } catch (Exception e) {}
            try { archiveDataClient.disconnect(); } catch (Exception e) {}
            try { executionDataClient.disconnect(); } catch (Exception e) {}
        }
    }
}
