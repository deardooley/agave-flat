package org.iplantc.service.jobs.managers.launchers;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.ClosedByInterruptException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.apps.dao.SoftwareDao;
import org.iplantc.service.apps.exceptions.SoftwareException;
import org.iplantc.service.apps.model.Software;
import org.iplantc.service.apps.model.SoftwareInput;
import org.iplantc.service.apps.model.SoftwareParameter;
import org.iplantc.service.apps.model.enumerations.SoftwareParameterType;
import org.iplantc.service.apps.util.ServiceUtils;
import org.iplantc.service.apps.util.ZipUtil;
import org.iplantc.service.common.dao.TenantDao;
import org.iplantc.service.common.model.Tenant;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.common.util.HTMLizer;
import org.iplantc.service.jobs.dao.JobDao;
import org.iplantc.service.jobs.exceptions.JobException;
import org.iplantc.service.jobs.exceptions.SchedulerException;
import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.jobs.model.JobEvent;
import org.iplantc.service.jobs.model.enumerations.JobStatusType;
import org.iplantc.service.jobs.model.enumerations.StartupScriptJobVariableType;
import org.iplantc.service.jobs.model.enumerations.WrapperTemplateAttributeVariableType;
import org.iplantc.service.jobs.model.enumerations.WrapperTemplateStatusVariableType;
import org.iplantc.service.jobs.util.Slug;
import org.iplantc.service.notification.util.EmailMessage;
import org.iplantc.service.remote.RemoteSubmissionClient;
import org.iplantc.service.systems.dao.SystemDao;
import org.iplantc.service.systems.exceptions.SystemUnavailableException;
import org.iplantc.service.systems.model.ExecutionSystem;
import org.iplantc.service.systems.model.enumerations.StartupScriptSystemVariableType;
import org.iplantc.service.systems.model.enumerations.StorageProtocolType;
import org.iplantc.service.systems.model.enumerations.SystemStatusType;
import org.iplantc.service.transfer.RemoteDataClient;
import org.iplantc.service.transfer.RemoteTransferListener;
import org.iplantc.service.transfer.URLCopy;
import org.iplantc.service.transfer.dao.TransferTaskDao;
import org.iplantc.service.transfer.local.Local;
import org.iplantc.service.transfer.model.TransferTask;
import org.iplantc.service.transfer.util.MD5Checksum;
import org.joda.time.DateTime;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.io.Files;

/**
 * Interface to define how to launch applications on various resources
 * 
 * @author dooley
 * 
 */
public abstract class AbstractJobLauncher implements JobLauncher 
{	
	private static final Logger log = Logger.getLogger(AbstractJobLauncher.class);
	public static final String ARCHIVE_FILENAME = ".agave.archive";
	
	private AtomicBoolean stopped = new AtomicBoolean(false);
    
	protected File 						tempAppDir = null;
	protected String 					step;
    protected Job						job;
    protected Software 					software;
    protected ExecutionSystem 			executionSystem;
//	protected RemoteDataClient	 		remoteExecutionDataClient;
//	protected RemoteDataClient	 		remoteSoftwareDataClient;
	protected RemoteDataClient          localDataClient;
	protected RemoteSubmissionClient 	submissionClient = null;
	protected URLCopy                   urlCopy;
	protected TransferTask              transferTask;
	
	protected AbstractJobLauncher() {}
	
	public AbstractJobLauncher(Job job) {
		this.setJob(job);
        this.setSoftware(SoftwareDao.getSoftwareByUniqueName(job.getSoftwareName()));
        this.setExecutionSystem((ExecutionSystem) new SystemDao().findBySystemId(job.getSystem()));
        
        localDataClient = new Local(null, "/", Files.createTempDir().getAbsolutePath());
	}
	
	/* (non-Javadoc)
	 * @see org.iplantc.service.jobs.managers.launchers.JobLauncher#isStopped()
	 */
	@Override
    public synchronized boolean isStopped() {
        return stopped.get();
    }

    /* (non-Javadoc)
     * @see org.iplantc.service.jobs.managers.launchers.JobLauncher#setStopped(boolean)
     */
    @Override
    public synchronized void setStopped(boolean stopped) {
        this.stopped.set(stopped);
        
        if (getUrlCopy() != null) {
            getUrlCopy().setKilled(true);
        }
        
        if (transferTask != null) {
            
        }
    }
    
    /* (non-Javadoc)
     * @see org.iplantc.service.jobs.managers.launchers.JobLauncher#getUrlCopy()
     */
    @Override
    public synchronized URLCopy getUrlCopy() {
        return urlCopy;
    }
    
    /* (non-Javadoc)
     * @see org.iplantc.service.jobs.managers.launchers.JobLauncher#setUrlCopy(org.iplantc.service.transfer.URLCopy)
     */
    @Override
    public synchronized void setUrlCopy(URLCopy urlCopy) {
        this.urlCopy = urlCopy;
    }
    
    /* (non-Javadoc)
     * @see org.iplantc.service.jobs.managers.launchers.JobLauncher#checkStopped()
     */
    @Override
    public void checkStopped() throws ClosedByInterruptException {
        if (isStopped()) {
            throw new ClosedByInterruptException();
        }
    }
	
	/* (non-Javadoc)
	 * @see org.iplantc.service.jobs.managers.launchers.JobLauncher#launch()
	 */
	@Override
	public abstract void launch() throws JobException, ClosedByInterruptException, SchedulerException, IOException, SystemUnavailableException;
	
	/**
	 * Removes all the user-provided job status macros that agave inserts into the wrapper template
	 * that would otherwise screw things up. 
	 * @param wrapperTemplate
	 * @return
	 */
	public String filterRuntimeStatusMacros(String wrapperTemplate) {
		WrapperTemplateStatusVariableType[] userAccessibleStatuCallbacks = WrapperTemplateStatusVariableType.getUserAccessibleStatusCallbacks();
		for (WrapperTemplateStatusVariableType macro: WrapperTemplateStatusVariableType.values()) {
			if (!ArrayUtils.contains(userAccessibleStatuCallbacks, macro)) {
				wrapperTemplate = StringUtils.replace(wrapperTemplate, "${" + macro.name() + "}", "");
			}
		}
		
		return wrapperTemplate;
	}
	
	/* (non-Javadoc)
	 * @see org.iplantc.service.jobs.managers.launchers.JobLauncher#resolveRuntimeNotificationMacros(java.lang.String)
	 */
	@Override
	public String resolveRuntimeNotificationMacros(String wrapperTemplate) {
		
		Pattern emptyCallbackPattern = Pattern.compile("(?s).*\\$\\{AGAVE_JOB_CALLBACK_NOTIFICATION\\}.*");
        Matcher callbackMatcher = emptyCallbackPattern.matcher(wrapperTemplate);
        while (callbackMatcher.matches()) {
        	String callbackSnippet = WrapperTemplateStatusVariableType.resolveNotificationEventMacro(
                    getJob(), "JOB_RUNTIME_CALLBACK_EVENT", new String[]{});
            
            wrapperTemplate = StringUtils.replace(wrapperTemplate, callbackMatcher.group(0), callbackSnippet);
            
            callbackMatcher = emptyCallbackPattern.matcher(wrapperTemplate);
        }
        
	    // process the notification template first so there is no confusion or namespace conflict prior to resolution
        Pattern defaultCallbackPattern = Pattern.compile("(?s)(?:.*)?(?:(\\$\\{AGAVE_JOB_CALLBACK_NOTIFICATION\\|(?:([a-zA-Z0-9_,\\s]*))\\}))(?:.*)?");
        callbackMatcher = defaultCallbackPattern.matcher(wrapperTemplate);
        while (callbackMatcher.matches()) {
        	String callbackSnippet = WrapperTemplateStatusVariableType.resolveNotificationEventMacro(
                    getJob(), "JOB_RUNTIME_CALLBACK_EVENT", StringUtils.split(callbackMatcher.group(2), ","));
            
            wrapperTemplate = StringUtils.replace(wrapperTemplate, callbackMatcher.group(1), callbackSnippet);
            
            callbackMatcher = defaultCallbackPattern.matcher(wrapperTemplate);
        }
        
        Pattern customCallbackPattern = Pattern.compile("(?s)(?:.*)?(?:(\\$\\{AGAVE_JOB_CALLBACK_NOTIFICATION\\|(?:(\\s*[a-zA-Z0-9_]*\\s*))\\|(?:([a-zA-Z0-9_,\\s]*))\\}))(?:.*)?");
        callbackMatcher = customCallbackPattern.matcher(wrapperTemplate);
        while (callbackMatcher.matches()) {
            String callbackSnippet = WrapperTemplateStatusVariableType.resolveNotificationEventMacro(
                    getJob(), callbackMatcher.group(2), StringUtils.split(callbackMatcher.group(3), ","));
            
            wrapperTemplate = StringUtils.replace(wrapperTemplate, callbackMatcher.group(1), callbackSnippet);
            
            callbackMatcher = customCallbackPattern.matcher(wrapperTemplate);
        }
        
        return wrapperTemplate;
	}
	
	/* (non-Javadoc)
	 * @see org.iplantc.service.jobs.managers.launchers.JobLauncher#resolveMacros(java.lang.String)
	 */
	@Override
	public String resolveMacros(String wrapperTemplate) {
		
		for (WrapperTemplateAttributeVariableType macro: WrapperTemplateAttributeVariableType.values()) {
			wrapperTemplate = StringUtils.replace(wrapperTemplate, "${" + macro.name() + "}", macro.resolveForJob(getJob()));
		}
		
		for (WrapperTemplateStatusVariableType macro: WrapperTemplateStatusVariableType.values()) {
//			if (macro != WrapperTemplateStatusVariableType.AGAVE_JOB_CALLBACK_NOTIFICATION) {
				wrapperTemplate = StringUtils.replace(wrapperTemplate, "${" + macro.name() + "}", macro.resolveForJob(getJob()));
//			}
		}
		
		return wrapperTemplate;
	}
	
	/* (non-Javadoc)
	 * @see org.iplantc.service.jobs.managers.launchers.JobLauncher#resolveStartupScriptMacros(java.lang.String)
	 */
	public String resolveStartupScriptMacros(String startupScript) {
		if (StringUtils.isBlank(startupScript)) {
			return null;
		}
		else {
			String resolvedStartupScript = startupScript;
			for (StartupScriptSystemVariableType macro: StartupScriptSystemVariableType.values()) {
				resolvedStartupScript = StringUtils.replace(resolvedStartupScript, "${" + macro.name() + "}", macro.resolveForSystem(getExecutionSystem()));
			}
			
			for (StartupScriptJobVariableType macro: StartupScriptJobVariableType.values()) {
				resolvedStartupScript = StringUtils.replace(resolvedStartupScript, "${" + macro.name() + "}", macro.resolveForJob(getJob()));
			}
			
			return resolvedStartupScript;
		}
	}
	
	/**
	 * Generates the command to source the {@link ExecutionSystem#getStartupScript()} and log the 
	 * response to the {@code .agave.log} file in the job work directory.
	 * @return the properly escaped command to be run on the remote system.
	 * @throws SystemUnavailableException
	 */
	public String getStartupScriptCommand(String absoluteRemoteWorkPath) throws SystemUnavailableException {
		String startupScriptCommand = "";
		if (!StringUtils.isEmpty(getExecutionSystem().getStartupScript())) {
			String resolvedstartupScript = resolveStartupScriptMacros(getExecutionSystem().getStartupScript());
			
			if (resolvedstartupScript != null) {
				startupScriptCommand = String.format("printf \"[%%s] %%b\\n\" $(date '+%%Y-%%m-%%dT%%H:%%M:%%S%%z') \"$(source %s 2>&1)\" >> %s/.agave.log ",
						resolvedstartupScript,
						absoluteRemoteWorkPath);
			}
		}
		return startupScriptCommand;
	}
	
//	/**
//	 * Returns the name of the 
//	 * @param apiUsername
//	 * @param internalUsername
//	 * @param inputValue
//	 * @return
//	 * @throws Exception
//	 */
//	protected String getInputFileName(String apiUsername, String internalUsername, String inputValue) 
//	throws Exception {
//		try
//		{
//			if (StringUtils.isEmpty(inputValue)) {
//				return "";
//			} else {
//				URI uri = new URI(inputValue);
//				RemoteDataClientFactory factory = new RemoteDataClientFactory();
//				RemoteDataClient rdc = factory.getInstance(apiUsername, internalUsername, uri);
//				return rdc.resolvePath(uri.getPath());
//			}
//		}
//		catch (URISyntaxException e)
//		{
//			return FilenameUtils.getName(inputValue);
//		}
//	}
//	
	/**
     * sets up the application dir for job launch
     * @throws JobException 
     */
    protected void createTempAppDir() throws JobException {
        step = "Creating cache directory to process " + getJob().getSoftwareName() + 
                " wrapper template for job " + getJob().getUuid();
        log.debug(step);
        
        // # local temp directory on server for staging execution folders.
        // this should have been created by the staging task, but in case
        // there were no inputs, we need to create it ourself.
        if (StringUtils.isEmpty(getJob().getWorkPath())) {
        	String remoteWorkPath = null;
        	
			if (!StringUtils.isEmpty(getSoftware().getExecutionSystem().getScratchDir())) {
				remoteWorkPath = getSoftware().getExecutionSystem().getScratchDir();
			} else if (!StringUtils.isEmpty(getSoftware().getExecutionSystem().getWorkDir())) {
				remoteWorkPath = getSoftware().getExecutionSystem().getWorkDir();
			}
			
			if (!StringUtils.isEmpty(remoteWorkPath)) {
				if (!remoteWorkPath.endsWith("/")) remoteWorkPath += "/";
			} else {
				remoteWorkPath = "";
			}
			
			remoteWorkPath += getJob().getOwner() +
					"/job-" + getJob().getUuid() + "-" + Slug.toSlug(getJob().getName());
			
			getJob().setWorkPath(remoteWorkPath);
			
			JobDao.persist(getJob());
        }
        
        tempAppDir = new File(FileUtils.getTempDirectory(), FilenameUtils.getName(getJob().getWorkPath()));
        
        if (tempAppDir.exists()) {
        	FileUtils.deleteQuietly(tempAppDir);
        }
        
        tempAppDir.mkdirs();
    }
    
    /**
     * copies the deployment path in irods to the local tempAppDir in preparation of condor_submit
     */
    protected void copySoftwareToTempAppDir() throws JobException, SystemUnavailableException 
    {
        step = "Fetching app assets for job " + getJob().getUuid() + " from " +
                "agave://" + getSoftware().getStorageSystem().getSystemId() + "/" + 
                getSoftware().getDeploymentPath() + " to temp application directory " + 
                tempAppDir.getAbsolutePath();
        
        log.debug(step);
        
        TransferTask transferTask;
        RemoteDataClient remoteSoftwareDataClient = null;
		try 
		{
        	if (getSoftware().getStorageSystem() == null) 
        	{
        		throw new JobException("Unable to submit job. Storage system with app assets is " + 
        				" no longer a registered system.");
        	} 
        	else if(!getSoftware().getStorageSystem().isAvailable() || 
        			!getSoftware().getStorageSystem().getStatus().equals(SystemStatusType.UP)) 
        	{
        		throw new SystemUnavailableException("Unable to fetch app assets. Storage system " + 
        				getSoftware().getStorageSystem().getSystemId() + " is not currently available for use.");
        	} 
        	else 
        	{
        		remoteSoftwareDataClient = getSoftware().getStorageSystem().getRemoteDataClient();
        		
	        	if (remoteSoftwareDataClient != null) {
	        		remoteSoftwareDataClient.authenticate();
	        		
	        		checkStopped();
	        		
	        		// what we really want is to just copy contents into tempAppDir, so we work around default behavior
	        		// first copy the remote data here
	        		transferTask = new TransferTask(
	    					"agave://" + getSoftware().getStorageSystem().getSystemId() + "/" + getSoftware().getDeploymentPath(), 
	    					"https://workers.prod.agaveapi.co/" + tempAppDir.getAbsolutePath(), 
	    					getJob().getOwner(), null, null);
	    			
//	    			transferTask.setTotalSize(-1);
//	    			transferTask.setBytesTransferred(0);
//	    			transferTask.setAttempts(1);
//	    			transferTask.setStatus(TransferStatusType.TRANSFERRING);
//	    			transferTask.setStartTime(new DateTime().toDate());
//	    			TransferTaskDao.persist(transferTask);
	    			
	        		TransferTaskDao.persist(transferTask);
	    			
	        		JobDao.refresh(getJob());
	    			
	    			getJob().addEvent(new JobEvent(JobStatusType.STAGING_JOB, 
	    					"Fetching app assets from "  + transferTask.getSource(), 
	    					null,
	    					getJob().getOwner()));
	    			
	        		JobDao.persist(getJob());
	        			        		
//	        		urlCopy.copy(software.getDeploymentPath(), tempAppDir.getAbsolutePath(), transferTask);
	        		remoteSoftwareDataClient.get(getSoftware().getDeploymentPath(), tempAppDir.getAbsolutePath(), new RemoteTransferListener(transferTask));
	        		
	    			checkStopped();
                    
	        		// now copy the contents of the deployment folder to the parent dir, which is tempAppDir
	        		File copiedDeploymentFolder = new File(tempAppDir, FilenameUtils.getName(StringUtils.removeEnd(getSoftware().getDeploymentPath(), "/")));
	        		if (!copiedDeploymentFolder.getAbsoluteFile().equals(tempAppDir) && copiedDeploymentFolder.exists() && copiedDeploymentFolder.isDirectory()) {
	        			FileUtils.copyDirectory(copiedDeploymentFolder, tempAppDir, null, true);
		        		// now delete the redundant deployment folder
		        		FileUtils.deleteQuietly(copiedDeploymentFolder);
	        		}
	        		
	        		checkStopped();
                    
                    
	        		if (getSoftware().isPubliclyAvailable()) {
	    				// validate the checksum to make sure the app itself hasn't  changed
	    				File zippedFile = new File(tempAppDir, FilenameUtils.getName(getSoftware().getDeploymentPath()));
	    				String checksum = MD5Checksum.getMD5Checksum(zippedFile);
	    				if (getSoftware().getChecksum() == null 
	    						|| StringUtils.equals(checksum, getSoftware().getChecksum()) 
	    						|| getSoftware().getStorageSystem().getStorageConfig().getProtocol().equals(StorageProtocolType.IRODS))
	    				{
	    					ZipUtil.unzip(zippedFile, tempAppDir);
	    					if (tempAppDir.list().length > 1) {
	    						zippedFile.delete();
	    					} else {
	    						throw new SoftwareException("Failed to unpack the application bundle.");
	    					}
	    				} else {
//	    					software.setAvailable(false);
//	    					software.setLastUpdated(new DateTime().toDate());
//	    					SoftwareDao.persist(software);
	    					Tenant tenant = new TenantDao().findByTenantId(TenancyHelper.getCurrentTenantId());
	    					String message ="While submitting a job, the Job Service noticed that the checksum " +
                                    "of the public app " + getSoftware().getUniqueName() + " had changed. This " +
                                    "will impact provenance and could impact experiment reproducability. " +
                                    "Please restore the application zip bundle from archive and re-enable " + 
                                    "the application via the admin console.\n\n" +
                                    "Name: " + getSoftware().getUniqueName() + "\n" + 
                                    "User: " + getJob().getOwner() + "\n" +
                                    "Job: " + getJob().getUuid() + "\n" +
                                    "Time: " + new DateTime(getJob().getCreated()).toString();
	    					try {
	    						EmailMessage.send(tenant.getContactName(), 
					    							tenant.getContactEmail(), 
					    							"Public app " + getSoftware().getUniqueName() + " has been corrupted.", 
					    							message, HTMLizer.htmlize(message));
	    					}
	    					catch (Throwable e) {
	    						log.error("Failed to notify admin that " + message, e);
	    					}
	    					
	    					throw new SoftwareException("Public app bundle for " + getSoftware().getUniqueName() + 
	    					        " has changed. Please verify this app and try again.");
	    				}
	    				
	    				File standardLocation = new File(tempAppDir, new File(getSoftware().getDeploymentPath()).getName());
	    				if (standardLocation.exists()) {
	    					tempAppDir = standardLocation.getAbsoluteFile();
	    				} else {
	    					standardLocation = new File(tempAppDir, getSoftware().getExecutablePath());
	    					
	    					if (!standardLocation.exists()) {
	    						// need to go searching for the path. no idea how this could happen
	    						boolean foundDeploymentPath = false;
	    						for (File child: tempAppDir.listFiles()) 
	    						{
	    							if (child.isDirectory()) {
	    								standardLocation = new File(child, getSoftware().getExecutablePath());
	    								if (standardLocation.exists()) {
	    									File copyDir = new File(tempAppDir.getAbsolutePath()+".copy");
	    									FileUtils.moveDirectory(child, copyDir);
	    									FileUtils.deleteDirectory(tempAppDir);
	    									copyDir.renameTo(tempAppDir);
	    									//tempAppDir = child;
	    									foundDeploymentPath = true;
	    									break;
	    								}
	    							}
	    						}
	    						
	    						if (!foundDeploymentPath) {
	    							log.error("Unable to find app path for public app " + getSoftware().getUniqueName());
	    							throw new SoftwareException("Unable to find the deployment path for the public app " + getSoftware().getUniqueName());
	    						}
	    					}
	    				}
	    			}
	        	} else {
	        		throw new JobException("Unable to obtain a remote data client for " +
	        				"the storage system.");
	        	}
        	}
        
		}
		catch (ClosedByInterruptException e) {
            log.debug("Submission task for job " + getJob().getUuid() + " aborted due to interrupt by worker process.");
        } 
		catch (JobException e) {
        	throw e;
        } 
		catch (Exception e) {
            throw new JobException("Remote data connection to " + getSoftware().getExecutionSystem().getSystemId() + " threw exception and stopped job execution", e);
        } 
		finally {
        	 try { remoteSoftwareDataClient.disconnect(); } catch (Exception e) {}
        }
    }

    public abstract File processApplicationTemplate() throws JobException;
    
    /**
     * Pushes the app assets which are currently on the staging server to the job 
     * work directory on the execution system. 
     * TODO: use a server-side copy when possible and avoid bringing the apps
     * assets to the staging server alltogther. All we <i>really</i> need is the
     * wrapper template.
     * 
     * @throws JobException
     */
    protected void stageSofwareApplication() throws JobException 
    {	
    	TransferTask transferTask;
    	RemoteDataClient remoteExecutionDataClient = null;
		try {
			log.debug("Staging " + getJob().getSoftwareName() + " app dependencies for job " + getJob().getUuid() + " to agave://" + getJob().getSystem() + "/" + getJob().getWorkPath());
			remoteExecutionDataClient = new SystemDao().findBySystemId(getJob().getSystem()).getRemoteDataClient(getJob().getInternalUsername());
			remoteExecutionDataClient.authenticate();
			remoteExecutionDataClient.mkdirs(getJob().getWorkPath(), getJob().getOwner());
			
			checkStopped();
			
			transferTask = new TransferTask(
					"https://workers.prod.agaveapi.co/" + tempAppDir.getAbsolutePath(), 
					"agave://" + getJob().getSystem() + "/" + getJob().getWorkPath(), 
					getJob().getOwner(), null, null);
			
			transferTask.setTotalSize(FileUtils.sizeOfDirectory(tempAppDir));
//			transferTask.setBytesTransferred(0);
//			transferTask.setAttempts(1);
//			transferTask.setStatus(TransferStatusType.TRANSFERRING);
//			transferTask.setStartTime(new DateTime().toDate());
			
			TransferTaskDao.persist(transferTask);
			
			JobDao.refresh(getJob());
			
			getJob().addEvent(new JobEvent(JobStatusType.STAGING_JOB, 
					"Staging runtime assets to "  + transferTask.getDest(), 
					null, 
					getJob().getOwner()));
			
			JobDao.persist(getJob());
    		
			// first time around we copy everything
			if (getJob().getRetries() <= 0) {
				remoteExecutionDataClient.put(tempAppDir.getAbsolutePath(), 
											new File(getJob().getWorkPath()).getParent(), 
											new RemoteTransferListener(transferTask));
			} 
			// after that we try to save some time on retries by only copying the assets 
			// that are missing or changed since the last attempt.
			else {
				remoteExecutionDataClient.syncToRemote(tempAppDir.getAbsolutePath(), 
											new File(getJob().getWorkPath()).getParent(), 
											new RemoteTransferListener(transferTask));
			}
		} 
		catch (Exception e) { 
			throw new JobException("Failed to stage application dependencies to execution system", e);
		} 
    	finally {
			try { remoteExecutionDataClient.disconnect();} catch (Exception e) {} 
		}
    }

    /**
     *  This method creates an archive log file
     * @param logFileName
     * @return
     * @throws JobException
     */
    protected File createArchiveLog(String logFileName) throws JobException {
        FileWriter logWriter = null;
        try 
        {
            File archiveLog = new File(tempAppDir, logFileName);
            archiveLog.createNewFile();
            logWriter = new FileWriter(archiveLog);

            printListing(tempAppDir, tempAppDir, logWriter);
            
            return archiveLog;
        } 
        catch (IOException e) 
        {
            step = "Creating an archive manifest file for job " + getJob().getUuid();
            log.debug(step);
            throw new JobException("Failed to create manifest file for job " + getJob().getUuid(), e);
        }
		finally {
            try { logWriter.close(); } catch (Exception e) {}
        }
    }
    
    protected void printListing(File file, File baseFolder, FileWriter writer) throws IOException
    {
        if (file.isFile())
        {
        	String relativeFilePath = StringUtils.removeStart(file.getPath(), baseFolder.getPath());
        	if (relativeFilePath.startsWith("/"))
        		relativeFilePath = relativeFilePath.substring(1);
            writer.append(relativeFilePath + "\n");
        }
        else
        {
            for(File child: file.listFiles())
            {
            	String relativeChildPath = StringUtils.removeStart(child.getPath(), baseFolder.getPath());
            	if (relativeChildPath.startsWith("/"))
            		relativeChildPath = relativeChildPath.substring(1);
                writer.append(relativeChildPath + "\n");

                if (child.isDirectory())
                {
                    printListing(child, baseFolder, writer);
                }
            }
        }
    }
    
    protected void printJobPropertyEnvironmentVariables(File file) throws JobException
    {
        FileWriter writer = null;
        try 
        {
            File agaverc = new File(tempAppDir, ".agaverc");
            agaverc.createNewFile();
            writer = new FileWriter(agaverc);
            writer.append("#!/bin/bash \n\n");
            
            for (WrapperTemplateAttributeVariableType macro: WrapperTemplateAttributeVariableType.values()) {
                if (StringUtils.startsWith(macro.name(), "AGAVE")) {
                    writer.append(String.format("%s=\"%s\"\n", macro.name(), macro.resolveForJob(getJob())));
                }
            }
            
            writer.close();
        } 
        catch (IOException e) 
        {
            step = "Creating the .agaverc " + getJob().getUuid();
            log.debug(step);
            throw new JobException("Failed to create runcom file for job " + getJob().getUuid(), e);
        }
        finally {
            try { writer.close(); } catch (Exception e) {}
        }
    }
        
        
    /**
     * Make the remote call to start the job on the remote system. This will need to
     * handle the invocation command, remote connection, parsing of the response to
     * get the job id, and updating of the job status on success or failure.
     * 
     * @return
     * @throws JobException
     * @throws SchedulerException
     */
    protected abstract String submitJobToQueue() throws JobException, SchedulerException;
    
    /* (non-Javadoc)
	 * @see org.iplantc.service.jobs.managers.launchers.JobLauncher#parseSoftwareParameterValueIntoTemplateVariableValue(org.iplantc.service.apps.model.SoftwareParameter, com.fasterxml.jackson.databind.JsonNode)
	 */
    @Override
	public String parseSoftwareParameterValueIntoTemplateVariableValue(SoftwareParameter softwareParameter, JsonNode jsonJobParamValue)
    {
    	// check for arrays of values. enquote if needed, then join as space-delimited values
		String[] paramValues = null;
		try 
		{	
			if (jsonJobParamValue == null || jsonJobParamValue.isNull() || (jsonJobParamValue.isArray() && jsonJobParamValue.size() == 0)) 
			{
				// null value for bool parameter is interpreted as false. It should not happen here, though,
				// as a null bool value passed into the job should go false
				if (softwareParameter.getType().equals(SoftwareParameterType.bool)) 
				{
					// filter the param value to a zero or 1
					String singleParamValue = "0";
					
					if (softwareParameter.isEnquote()) {
						singleParamValue = ServiceUtils.enquote(singleParamValue);
					}
					if (softwareParameter.isShowArgument()) {
						paramValues = new String[]{ softwareParameter.getArgument() + singleParamValue };
					} else {
						paramValues = new String[]{ singleParamValue };
					}
				} 
				else
				{
					paramValues = new String[]{};
				}
			} 
			else if (jsonJobParamValue.isArray()) 
			{
				paramValues = new String[jsonJobParamValue.size()];
				
				if (softwareParameter.getType().equals(SoftwareParameterType.flag)) 
				{
					// show the flag only if present and value is true
					if (StringUtils.equals(jsonJobParamValue.iterator().next().asText(), "1") || 
							jsonJobParamValue.iterator().next().asBoolean(false)) 
					{
						paramValues[0] = softwareParameter.getArgument();
					}
				} 
				else if (softwareParameter.getType().equals(SoftwareParameterType.bool)) 
				{
					// filter the param value to a zero or 1
					String singleParamValue = null;
					if (StringUtils.equals(jsonJobParamValue.iterator().next().asText(), "1") || 
							jsonJobParamValue.iterator().next().asBoolean(false)) 
					{
						singleParamValue = "1";
					}
					else
					{
						singleParamValue = "0";
					}
					
					if (softwareParameter.isEnquote()) {
						singleParamValue = ServiceUtils.enquote(singleParamValue);
					}
					if (softwareParameter.isShowArgument()) {
						paramValues[0] = softwareParameter.getArgument() + singleParamValue;
					} else {
						paramValues[0] = singleParamValue;
					}
				} 
				else
				{
					int childIndex = 0;
					for(Iterator<JsonNode> i = jsonJobParamValue.iterator(); i.hasNext();) 
					{
						JsonNode child = i.next();
						String singleParamValue = child.asText();
						if (softwareParameter.isEnquote()) {
							singleParamValue = ServiceUtils.enquote(singleParamValue);
						}
						
						if (softwareParameter.isShowArgument() && (softwareParameter.isRepeatArgument() || childIndex == 0)) {
							paramValues[childIndex] = softwareParameter.getArgument() + singleParamValue;
						} else {
							paramValues[childIndex] = singleParamValue;
						}
						childIndex++;
					}
				}
			}
			else // textual node
			{
				paramValues = new String[1];
				
				if (softwareParameter.getType().equals(SoftwareParameterType.flag)) 
				{
					// show the flag only if present and value is true
					if (StringUtils.equals(jsonJobParamValue.asText(), "1") || 
							jsonJobParamValue.asBoolean(false)) 
					{
						paramValues[0] = softwareParameter.getArgument();
					}
				} 
				else if (softwareParameter.getType().equals(SoftwareParameterType.bool)) 
				{
					// filter the param value to a zero or 1
					String singleParamValue = null;
					if (StringUtils.equals(jsonJobParamValue.asText(), "1") || 
							jsonJobParamValue.asBoolean(false)) 
					{
						singleParamValue = "1";
					}
					else
					{
						singleParamValue = "0";
					}
					
					if (softwareParameter.isEnquote()) {
						singleParamValue = ServiceUtils.enquote(singleParamValue);
					}
					if (softwareParameter.isShowArgument()) {
						paramValues[0] = softwareParameter.getArgument() + singleParamValue;
					} else {
						paramValues[0] = singleParamValue;
					}
				} 
				else 
				{
					String singleParamValue = jsonJobParamValue.asText();
					
					if (softwareParameter.isEnquote()) {
						singleParamValue = ServiceUtils.enquote(singleParamValue);
					}
					if (softwareParameter.isShowArgument()) {
						paramValues[0] = softwareParameter.getArgument() + singleParamValue;
					} else {
						paramValues[0] = singleParamValue;
					}
				}
			}
		} 
		catch (Exception e) 
		{
			String singleParamValue = jsonJobParamValue.textValue();
			
			if (softwareParameter.isEnquote()) {
				singleParamValue = ServiceUtils.enquote(singleParamValue);
			}
			
			if (softwareParameter.isShowArgument()) {
				paramValues[0] = softwareParameter.getArgument() + singleParamValue;
			} else {
				paramValues[0] = singleParamValue;
			}
		}
		
		return StringUtils.join(paramValues, " ");
    }
    
    /* (non-Javadoc)
	 * @see org.iplantc.service.jobs.managers.launchers.JobLauncher#parseSoftwareInputValueIntoTemplateVariableValue(org.iplantc.service.apps.model.SoftwareInput, com.fasterxml.jackson.databind.JsonNode)
	 */
    @Override
	public String parseSoftwareInputValueIntoTemplateVariableValue(SoftwareInput softwareInput, JsonNode jsonJobInputValue) 
    throws URISyntaxException
    {
    	// check for arrays of values. enquote if needed, then join as space-delimited values
		String[] paramValues = null;
		try 
		{
			if (jsonJobInputValue == null || jsonJobInputValue.isNull() || (jsonJobInputValue.isArray() && jsonJobInputValue.size() == 0)) 
			{
				paramValues = new String[]{};
			} 
			else 
			{
				if (jsonJobInputValue.isArray()) 
				{
					paramValues = new String[jsonJobInputValue.size()];
					
					int childIndex = 0;
					for(Iterator<JsonNode> i = jsonJobInputValue.iterator(); i.hasNext(); ) 
					{
						JsonNode child = i.next();
						String singleInputRawValue = child.asText();
						URI uri = new URI(singleInputRawValue);
						
						// TODO: we should handle url decoding/escaping here so it is possible to use enquote without fear of having to do shellfoo magic to make a script bulletproof 
						String singleInputValue = FilenameUtils.getName(uri.getPath());
						
						if (softwareInput.isEnquote()) {
							singleInputValue = ServiceUtils.enquote(singleInputValue);
						}
						
						if (softwareInput.isShowArgument() && (softwareInput.isRepeatArgument() || childIndex == 0)) {
							paramValues[childIndex] = softwareInput.getArgument() + singleInputValue;
						} else {
							paramValues[childIndex] = singleInputValue;
						}
						childIndex++;
					}
				}
				else // textual node
				{	
					paramValues = new String[1];
					
					String singleInputRawValue = jsonJobInputValue.asText();
					URI uri = new URI(singleInputRawValue);
					String singleInputValue = FilenameUtils.getName(uri.getPath());
					
					if (softwareInput.isEnquote()) {
						singleInputValue = ServiceUtils.enquote(singleInputValue);
					}
					if (softwareInput.isShowArgument()) {
						paramValues[0] = softwareInput.getArgument() + singleInputValue;
					} else {
						paramValues[0] = singleInputValue;
					}
				}
			}
		} 
		catch (Exception e) 
		{
			String singleInputRawValue = jsonJobInputValue.asText();
			URI uri = new URI(singleInputRawValue);
			String singleInputValue = FilenameUtils.getName(uri.getPath());
			
			if (softwareInput.isEnquote()) {
				singleInputValue = ServiceUtils.enquote(singleInputValue);
			}
			if (softwareInput.isShowArgument()) {
				paramValues[0] = softwareInput.getArgument() + singleInputValue;
			} else {
				paramValues[0] = singleInputValue;
			}
		}
		
		return StringUtils.join(paramValues, " ");
    }
    
    /* (non-Javadoc)
	 * @see org.iplantc.service.jobs.managers.launchers.JobLauncher#getTempAppDir()
	 */
    @Override
	public File getTempAppDir()
	{
		return tempAppDir;
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.jobs.managers.launchers.JobLauncher#setTempAppDir(java.io.File)
	 */
	@Override
	public void setTempAppDir(File tempAppDir)
	{
		this.tempAppDir = tempAppDir;
	}
	
	/* (non-Javadoc)
	 * @see org.iplantc.service.jobs.managers.launchers.JobLauncher#getJob()
	 */
	@Override
	public synchronized Job getJob() {
	    return this.job;
	}

	/**
	 * @return the executionSystem
	 */
	public ExecutionSystem getExecutionSystem() {
		return executionSystem;
	}

	/**
	 * @param executionSystem the executionSystem to set
	 */
	public void setExecutionSystem(ExecutionSystem executionSystem) {
		this.executionSystem = executionSystem;
	}

	/**
	 * @return the software
	 */
	public Software getSoftware() {
		return software;
	}

	/**
	 * @param software the software to set
	 */
	public void setSoftware(Software software) {
		this.software = software;
	}

	/**
	 * @param job the job to set
	 */
	public void setJob(Job job) {
		this.job = job;
	}
}
