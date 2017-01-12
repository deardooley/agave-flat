package org.iplantc.service.jobs.managers.launchers;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.channels.ClosedByInterruptException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.apps.exceptions.SoftwareException;
import org.iplantc.service.apps.model.SoftwareInput;
import org.iplantc.service.apps.model.SoftwareParameter;
import org.iplantc.service.common.persistence.HibernateUtil;
import org.iplantc.service.jobs.exceptions.JobException;
import org.iplantc.service.jobs.exceptions.JobFinishedException;
import org.iplantc.service.jobs.managers.JobManager;
import org.iplantc.service.jobs.managers.launchers.parsers.CondorJobIdParser;
import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.jobs.model.JobUpdateParameters;
import org.iplantc.service.jobs.model.enumerations.JobStatusType;
import org.iplantc.service.jobs.model.scripts.CommandStripper;
import org.iplantc.service.jobs.model.scripts.SubmitScript;
import org.iplantc.service.jobs.model.scripts.SubmitScriptFactory;
import org.iplantc.service.jobs.phases.workers.IPhaseWorker;
import org.iplantc.service.remote.RemoteSubmissionClient;
import org.iplantc.service.remote.local.CmdLineProcessHandler;
import org.iplantc.service.remote.local.CmdLineProcessOutput;
import org.iplantc.service.systems.dao.SystemDao;
import org.iplantc.service.systems.exceptions.SystemUnavailableException;
import org.iplantc.service.systems.model.ExecutionSystem;
import org.iplantc.service.systems.model.enumerations.SystemStatusType;
import org.iplantc.service.transfer.RemoteDataClient;
import org.joda.time.DateTime;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Created with IntelliJ IDEA.
 * User: steve
 * Date: 11/4/12
 * Time: 7:35 PM
 * To change this template use File | Settings | File Templates.
 */
@SuppressWarnings("unused")
public class CondorLauncher extends AbstractJobLauncher {
    
	private static final Logger log = Logger.getLogger(CondorLauncher.class);
    
    private SubmitScript submitFileObject;
    private File condorSubmitFile;
    private String timeMark;
    private String tag;
    
	private boolean jobFailed = false;

    /**
     * Creates an instance of a JobLauncher capable of submitting jobs to batch
     * queuing systems on Condor resources.
     */
    public CondorLauncher(Job job, IPhaseWorker worker) {
        super(job, worker);
        this.submitFileObject = SubmitScriptFactory.getScript(job);
    }

    /**
     * This method gets the current working directory
     *
     * @return String of directory path
     */
    private String getWorkDirectory() {
        String wd = "";
        try {
            wd = new File("test.txt").getCanonicalPath().replace("/test.txt", "");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return wd;
    }

    /**
     * makes and sets new time marker for creation of job run directories
     */
    private void setTimeMarker() {
    	DateTime date = new DateTime(getJob().getCreated());
        Long time = date.getMillis();
        timeMark = time.toString();
        tag = getJob().getName() + "-" + timeMark;
    }

    /**
     * generates a condor submit file for execution
     *
     * @param time string
     * @return a string representing the condor_submit file name
     */
    private String createSubmitFileName(String time) {
        return "condorSubmit-" + getJob().getName() + "-" + time;
    }

    /**
     * Creates the condor submit file to be handed to condor_submit
     *
     * @param timeMark current time
     */
    private void createCondorSubmitFile(String timeMark) throws JobException
    {
        step = "Creating the " + getJob().getSoftwareName() + " condor submit file for job " + getJob().getUuid();
        log.debug(step);

        // todo need to add classAd info to submit file
        submitFileObject = SubmitScriptFactory.getScript(getJob());
        
        try 
        {
	        // generate a condor submit script. the user may have overridden certain 
        	// directives or the system owner may have templatized their custom directives,
        	// so we need to resolve the macros here.
	        String condorSubmitFileContents = resolveTemplateMacros(submitFileObject.getScriptText());
	        
	        String submitFileName = "condorSubmit";   //createSubmitFileName(timeMark);
	        condorSubmitFile = new File(tempAppDir, submitFileName);
        
            FileUtils.writeStringToFile(condorSubmitFile, condorSubmitFileContents);
        }
        catch (JobException e) {
        	throw e;
        } 
        catch (IOException | URISyntaxException e) {
            throw new JobException("Failed to write condor submit file to local cache.", e);
        }
    }

    /**
     * Method to help with multiple calls to outside processes for a variety of tasks
     *
     * @param command String of command line parameters
     * @param cmdName Name of the command being executed
     * @return an Object that includes int exit code, String out and String err from the execution
     * @throws JobException if the exitCode is not equal to 0
     */
    private CmdLineProcessOutput executeLocalCommand(String command, String cmdName) throws JobException {
        CmdLineProcessHandler cmdLineProcessHandler = new CmdLineProcessHandler();
        int exitCode = cmdLineProcessHandler.executeCommand(command);

        if (exitCode != 0) {
            throw new JobException("Job exited with error code " + exitCode + " please check your arguments and try again.");
        }
        return cmdLineProcessHandler.getProcessOutput();
    }

    /**
     * Takes a String representing the directory path and extracts the last directory name
     *
     * @param path String directory path
     * @return String of last directory name in path
     * @throws Exception if path is null or empty
     */
    private String getDirectoryName(String path) throws Exception {
        String name = null;

        // is it null or empty throw Exception
        if (path == null || path.isEmpty()) {
            throw new Exception("path can't be null or empty");
        }
        path = path.trim();
        String separator = File.separator;

        StringTokenizer st = new StringTokenizer(path, separator);
        while (st.hasMoreElements()) {
            name = (String) st.nextElement();
        }
        return name;
    }

    /**
	 * This method will write the generic transfer_wrapper.sh file that condor_submit
	 * uses as it's executable. The file wraps the defined executable for the job along
	 * with the data all tar'd and zipped to transfer to OSG
	 */
	private void createTransferWrapper() throws JobException {
	    step = "Creating Condor submission transfer wrapper to run " + 
	            getJob().getSoftwareName() + " for job " + getJob().getUuid();
	    log.debug(step);
	    StringBuilder transferScript = new StringBuilder();
	    FileWriter transferWriter = null;
	    String executablePath;
	
	    try {
	        transferWriter = new FileWriter(tempAppDir + File.separator + "transfer_wrapper.sh");
	
	        transferScript.append("#!/bin/bash\n\n");
	        transferScript.append("tar xzvf transfer.tar.gz\n");
	        transferScript.append("# we supply the executable and path from the software definition\n");
	        executablePath = getSoftware().getExecutablePath();
	        if (executablePath.startsWith("/")) {
	        	executablePath = executablePath.substring(1);
	        }
	        //transferScript.append("chmod u+x " + executablePath + " \n");
	        transferScript.append("./" + executablePath + "  # this in turn wraps the final executable along with inputs, parameters and output.\n");
	
	        // write the transfer_wrapper.sh file
	        transferWriter.write(transferScript.toString());
	        transferWriter.flush();
	    } catch (IOException e) {
	        e.printStackTrace();
	        throw new JobException("Failure to write transfer script for Condor submission to " + tempAppDir);
	    } finally {
	        try {
	            transferWriter.close();
	        } catch (IOException e) {
	            log.info("failed to close transferWriter outputStream");
	            e.printStackTrace();
	        }
	    }
	
	    // need to make sure that transfer_wrapper.sh is executable
	    String chmodCmdString = new String("cd " + tempAppDir + "; " + "chmod +x transfer_wrapper.sh; chmod +x " + executablePath);
	    executeLocalCommand(chmodCmdString, "chmod +x transfer_wrapper.sh; chmod +x " + executablePath);
	}

    /* (non-Javadoc)
     * @see org.iplantc.service.jobs.managers.launchers.AbstractJobLauncher#processApplicationTemplate()
     */
    @Override
    public File processApplicationTemplate() throws JobException 
    {
        step = "Processing " + getJob().getSoftwareName() + " wrapper template for job " + getJob().getUuid();
        log.debug(step);

        // need tempAppDir + getSoftware().getExecutablePath()
        // read in the template file
        // create the submit script in the temp folder
        File appTemplateFile = new File(tempAppDir + File.separator + getSoftware().getExecutablePath());
        // replace the executable script file references with the file names
        Map<String, String> inputMap = null;
        String appTemplate = null;
        StringBuilder batchScript;
        FileWriter batchWriter = null;

        // throw exception if file not found
        try {
            if (!appTemplateFile.exists()) {
                throw new JobException("Unable to locate wrapper script for \"" +
                        getSoftware().getUniqueName() + "\" at " +
                        getSoftware().getDeploymentPath() + "/" + getSoftware().getExecutablePath());
            }
            appTemplate = FileUtils.readFileToString(appTemplateFile);

            batchScript = new StringBuilder();
            
            // agave log file environment variable
            batchScript.append("##########################################################\n");
            batchScript.append("# Agave Environment Settings \n");
            batchScript.append("##########################################################\n\n");
            
            batchScript.append("# Location of agave job lifecycle log file\n");
            batchScript.append("AGAVE_LOG_FILE=$(pwd)/.agave.log\n\n\n");
			
            batchScript.append("##########################################################\n");
            batchScript.append("# Agave Utility functions \n");
            batchScript.append("##########################################################\n\n");
            
            // datetime function
            batchScript.append("# cross-plaltform function to print an ISO8601 formatted timestamp \n");
            batchScript.append("function agave_datetime_iso() { \n  date '+%Y-%m-%dT%H:%M:%S%z'; \n} \n\n");
	
			// logging function
            batchScript.append("# standard logging function to write agave job lifecycle logs\n");
            batchScript.append("function agave_log_response() { \n  echo \"[$(agave_datetime_iso)] ${@}\"; \n} 2>&1 >> \"${AGAVE_LOG_FILE}\"\n\n");
         			
            
            // write the callback to trigger status update at start
            batchScript.append("# Callback to signal the job has started executing user-defined logic\n");
            batchScript.append(resolveMacros("${AGAVE_JOB_CALLBACK_RUNNING}"));
            
            
            batchScript.append("##########################################################\n");
            batchScript.append("# Agave App and System Environment Settings \n");
            batchScript.append("##########################################################\n\n");
            
            
            List<String> appModules = getSoftware().getModulesAsList();
            if (!appModules.isEmpty()) {
	            batchScript.append("# App specific module commands\n");
	            
	            // add modules if specified by the app. Generally these won't be used in a condor app,
	            // but in the event they're running mpi or gliding in, these are available.
				for (String module : appModules) {
					batchScript.append("module " + module + "\n");
				}
				batchScript.append("\n");
            }
            else {
            	batchScript.append("# No modules commands configured for this app\n\n");   
            }

            // add in any custom environment variables that need to be set
            if (!StringUtils.isEmpty(getSoftware().getExecutionSystem().getEnvironment())) {
            	batchScript.append(getSoftware().getExecutionSystem().getEnvironment());
        	}
            if (!StringUtils.isEmpty(getSoftware().getExecutionSystem().getEnvironment())) {
            	batchScript.append("# App specific environment variables\n");
            	batchScript.append(getSoftware().getExecutionSystem().getEnvironment());
            	batchScript.append("\n\n\n");
         	}
            else {
            	batchScript.append("# No custom environment variables configured for this app\n\n\n");   
            }
            
             
            batchScript.append("##########################################################\n");
            batchScript.append("# Begin App Wrapper Template Logic \n");
            batchScript.append("##########################################################\n\n");
           
            appTemplate = appTemplate + 
					"\n\n\n" +
            		"##########################################################\n" +
            		"# End App Wrapper Template Logic \n" +
            		"##########################################################\n\n";
            
            // replace the parameters with their passed in values
            appTemplate = resolveTemplateMacros(appTemplate);

            // append the template with
            batchScript.append(appTemplate);
            batchWriter = new FileWriter(appTemplateFile);
            // write new contents to appTemplate for execution
            if (batchWriter != null) {
                batchWriter.write(batchScript.toString());
            }
            
            return appTemplateFile;
        } 
        catch (IOException e) {
            throw new JobException("FileUtil operation failed", e);
        } 
        catch (JobException e) {
            throw new JobException("Json failure from job inputs or parameters", e);
        }
		catch (URISyntaxException e) {
			throw new JobException("Failed to parse input URI", e);
		} finally {
            try {
                batchWriter.close();
            } catch (IOException e) {
                log.debug("failed to close batchWriter on Exception");
            }
        }

    }

	/**
	 * Parses all the inputs, parameters, and macros for the given 
	 * string. This is appropriate for both resolving wrapper templates 
	 * and condor submit scripts.
	 * 
	 * @param appTemplate
	 * @return
	 * @throws JobException
	 * @throws URISyntaxException
	 */
	protected String resolveTemplateMacros(String appTemplate)
	throws JobException, URISyntaxException {
		// replace the parameters with their passed in values
		JsonNode jobParameters = getJob().getParametersAsJsonObject();
		
		for (SoftwareParameter param: getSoftware().getParameters())
		{
			if (jobParameters.has(param.getKey())) 
			{
				JsonNode jobJsonParam = jobParameters.get(param.getKey());

				// serialized the runtime parameters into a string of space-delimited 
				// values after enquoting and adding relevant argument(s)
				String templateVariableValue = parseSoftwareParameterValueIntoTemplateVariableValue(param, jobJsonParam);
				
				// now actually filter the template for this parameter
				appTemplate = appTemplate.replaceAll("(?i)\\$\\{" + param.getKey() + "\\}", templateVariableValue);
			}
			else if (!param.isVisible())
			{
				// serialized the runtime parameters into a string of space-delimited 
				// values after enquoting and adding relevant argument(s)
				String templateVariableValue = parseSoftwareParameterValueIntoTemplateVariableValue(param, param.getDefaultValueAsJsonArray());
				
				// now actually filter the template for this parameter
				appTemplate = appTemplate.replaceAll("(?i)\\$\\{" + param.getKey() + "\\}", templateVariableValue);
			}
			else 
			{
				appTemplate = appTemplate.replaceAll("(?i)\\$\\{" + param.getKey() + "\\}", "");
			}
		}
		
		// replace the parameters with their passed in values
		JsonNode jobInputs = getJob().getInputsAsJsonObject();
		
		for (SoftwareInput input: getSoftware().getInputs())
		{
			if (jobInputs.has(input.getKey())) 
			{
				JsonNode jobJsonInput = jobInputs.get(input.getKey());

				// serialized the runtime parameters into a string of space-delimited 
				// values after enquoting and adding relevant argument(s)
				String templateVariableValue = parseSoftwareInputValueIntoTemplateVariableValue(input, jobJsonInput);
				
				// now actually filter the template for this parameter
				appTemplate = appTemplate.replaceAll("(?i)\\$\\{" + input.getKey() + "\\}", templateVariableValue);
			}
			else if (!input.isVisible())
			{
				// serialized the runtime parameters into a string of space-delimited 
				// values after enquoting and adding relevant argument(s)
				String templateVariableValue = parseSoftwareInputValueIntoTemplateVariableValue(input, input.getDefaultValueAsJsonArray());
				
				// now actually filter the template for this parameter
				appTemplate = appTemplate.replaceAll("(?i)\\$\\{" + input.getKey() + "\\}", templateVariableValue);
			}
			else 
			{
				appTemplate = appTemplate.replaceAll("(?i)\\$\\{" + input.getKey() + "\\}", "");
			}
		}
		
		// strip out all references to icommands and it irods shadow files
		if (getExecutionSystem().isPubliclyAvailable()) {
			appTemplate = CommandStripper.strip(appTemplate);
		}
		
		// strip out premature completion callbacks that might result in archiving starting before
		// the script exists.
		appTemplate = filterRuntimeStatusMacros(appTemplate);
		
		// Replace all the runtime callback notifications
		appTemplate = resolveRuntimeNotificationMacros(appTemplate);
							
		// Replace all the iplant template tags
		appTemplate = resolveMacros(appTemplate);
		return appTemplate;
	}
    
    private void createRemoteTransferPackage() throws Exception {
        step = "Creating the " + getJob().getSoftwareName() + " transfer package for job " + getJob().getUuid();
        log.debug(step);
        
        String createTransferPackageCmd = new String("cd " + getJob().getWorkPath() + "; "
                + "tar czvf ./transfer.tar.gz  --exclude condorSubmit --warning=no-file-changed .");
        ExecutionSystem system = (ExecutionSystem) new SystemDao().findBySystemId(getJob().getSystem());
        
    	RemoteSubmissionClient remoteSubmissionClient = system.getRemoteSubmissionClient(getJob().getInternalUsername());
    	String response = remoteSubmissionClient.runCommand(createTransferPackageCmd);
    	response = StringUtils.lowerCase(response);
    	if (StringUtils.contains(response, "cannot") || StringUtils.contains(response, "command not found")) {
    		throw new JobException("Failed to create transfer package for condor submission. \n" + response);
    	}
    	
    	remoteSubmissionClient.close();

    }

    /**
     * Currently for the OSG condor submit host, IRods files pushed to the execution system lose their executable setting so they
     * need to be reset.
     *
     * @throws JobException
     */
    private void addExecutionPermissionsToWrapper(){
        step = "Changing execute permissions on transfer_wrapper and executable for job " + getJob().getUuid();
        log.debug(step);

        String changePermissionsCmd = new String("cd " + getJob().getWorkPath() + "; "
                + "chmod +x *.sh");
        ExecutionSystem system = (ExecutionSystem) new SystemDao().findBySystemId(getJob().getSystem());

        RemoteSubmissionClient remoteSubmissionClient = null;
        try {
            remoteSubmissionClient = system.getRemoteSubmissionClient(getJob().getInternalUsername());
            String response = remoteSubmissionClient.runCommand(changePermissionsCmd);
            if (response.contains("Cannot")) {
                throw new JobException("Failed to create transfer package for condor submission. \n" + response);
            }
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }finally{
            remoteSubmissionClient.close();

        }
    }

    @Override
    public void launch() 
      throws JobException, SystemUnavailableException, ClosedByInterruptException, JobFinishedException
    {
        String condorSubmitCmdString = null;
        try 
        {
        	if (getSoftware() == null || !getSoftware().isAvailable()) {
				throw new SoftwareException("App is not longer available for execution");
			}
        	
        	// if the system is down, return it to the queue to wait for the system
			// to come back up.
			if (getExecutionSystem() == null)
			{
				throw new JobException("Execution system " + getJob().getSystem() + 
						" is no longer a registered system.");
			} 
			else if (!getExecutionSystem().getStatus().equals(SystemStatusType.UP) || 
					!getExecutionSystem().isAvailable()) 
			{
				throw new SystemUnavailableException("Execution system " + getJob().getSystem() + 
						" is not currently available for job submission.");
			} 
        				
            // this is used to set the tempAppDir identifier so that we can come back
            // later to find the directory when the job is done (Failed for cleanup or Successful for archiving)
            setTimeMarker();
            
            checkStopped();
            
            // sets up the application directory to execute this job launch; see comments in method
            createTempAppDir();
            
            checkStopped();

            // copy our application package from the software.deploymentPath to our tempAppDir
            copySoftwareToTempAppDir();

            checkStopped();
            
            // create the generic transfer wrapper that condor_submit will use to un-tar the execution
            // package after the files have been transferred to OSG
            createTransferWrapper();

            checkStopped();
            
            // prepend the application template with call back to let the Job service know the job has started
            // parse the application template and replace tokens with the inputs, parameters and outputs.
            processApplicationTemplate();

            checkStopped();
            
            // create the shadow file containing the exclusion files for archiving
            createArchiveLog(ARCHIVE_FILENAME);
			
            checkStopped();
            
            // create the Condor submit file should include the path to executable, the transfer.tar.gz and input(s)
            // default classAds for Linux
            createCondorSubmitFile(timeMark);
            
            checkStopped();
            
            // move the local temp directory to the remote system
            stageSofwareApplication();

            checkStopped();
            
            // change permissions to executable for wrapper.sh and transfer_wrapper.sh
            addExecutionPermissionsToWrapper();
            
            checkStopped();
            
            // tar up the entire executable with input data on the remote system
            createRemoteTransferPackage();
            
            checkStopped();
            
            // run condor_submit
            String condorJobId = submitJobToQueue();
            
            // Update job record.
            Date curDate = new DateTime().toDate();
            JobUpdateParameters jobUpdateParameters = new JobUpdateParameters();
            jobUpdateParameters.setSubmitTime(curDate);
            jobUpdateParameters.setLastUpdated(curDate);
            jobUpdateParameters.setLocalJobId(condorJobId);
            setJob(JobManager.updateStatus(getJob(), JobStatusType.QUEUED, 
                                            "Condor job successfully placed into queue", 
                                            jobUpdateParameters));
        }
        catch (ClosedByInterruptException | JobFinishedException e) {
            throw e;
        } 
        catch (JobException e) {
        	jobFailed = true;
        	log.error(step);
        	setJob(JobManager.updateStatus(getJob(), JobStatusType.FAILED, e.getMessage()));
        	throw e;
        } 
        catch (SystemUnavailableException e) {
        	jobFailed = true;
        	log.error(step);
        	throw e;
        } 
        catch (Exception e) {
            jobFailed = true;
            log.error(step);
            setJob(JobManager.updateStatus(getJob(), JobStatusType.FAILED, e.getMessage()));
            throw new JobException("Failed to invoke app: \"" + getSoftware().getUniqueName() + "\n\"   with command:  " + condorSubmitCmdString + "\n" + e.getMessage(), e);
        } 
        finally {
        	FileUtils.deleteQuietly(tempAppDir);
        }
    }

    @Override
    protected String submitJobToQueue() throws JobException
    {
    	RemoteDataClient remoteExecutionDataClient = null;
		try {
	    	submissionClient = getExecutionSystem().getRemoteSubmissionClient(getJob().getInternalUsername());
	        //String[] condorSubmitCmdString = new String[]{"/bin/bash", "-cl", "cd " + tempAppDir.getAbsolutePath() + "; condor_submit " + condorSubmitFile.getName()};
    		remoteExecutionDataClient = getExecutionSystem().getRemoteDataClient(getJob().getInternalUsername());
			
    		// Get the remote work directory for the log file
			String remoteWorkPath = remoteExecutionDataClient.resolvePath(getJob().getWorkPath());
						
			// Resolve the startupScript and generate the command to run it and log the response to the
			// remoteWorkPath + "/.agave.log" file
			String startupScriptCommand = getStartupScriptCommand(remoteWorkPath);
						
			// command to cd to the remoteWorkPath
			String cdCommand = "cd " + remoteWorkPath;
			
			// command to submit the condor submit file we built to wrap the 
			// job assets to the remote condor master
			String submitCommand = "condor_submit " + condorSubmitFile.getName();
			
			// run the aggregate command on the remote system
			String submissionResponse = submissionClient.runCommand(
			        startupScriptCommand + " ; " + cdCommand + " ; " + submitCommand);
	    	
			if (StringUtils.isBlank(submissionResponse)) 
			{
				// retry the remote command once just in case it was a flicker
				submissionResponse = submissionClient.runCommand(
						startupScriptCommand + " ; " + cdCommand + " ; " + submitCommand);
				
				// blank response means the job didn't go in...twice. Fail the attempt
				if (StringUtils.isBlank(submissionResponse)) 
					throw new JobException("Failed to submit condor job. " + submissionResponse);
			}
			
			// parse the response from the remote command invocation to get the localJobId
			// by which we'll reference the job during monitoring, etc.
			CondorJobIdParser jobIdParser = new CondorJobIdParser();
	        
	        return jobIdParser.getJobId(submissionResponse);
    	} 
    	catch (JobException e) {
    		throw e;
    	} 
    	catch (Exception e) {
    		throw new JobException("Failed to submit job to condor queue", e);
    	}
    	finally {
    		try { submissionClient.close(); } catch (Exception e) {}
			try { remoteExecutionDataClient.disconnect(); } catch (Exception e) {}
    	}
    }
    
    /**
     * Cleanup all the loose ends if the job failed
     */
    private void cleanup() {
        try {
            HibernateUtil.closeSession();
        } catch (Exception e) {}
        try {
            FileUtils.deleteDirectory(tempAppDir);
        } catch (Exception e) {}
    }


}
