/**
 * 
 */
package org.iplantc.service.jobs.managers.launchers;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.channels.ClosedByInterruptException;
import java.util.Date;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.apps.model.SoftwareInput;
import org.iplantc.service.apps.model.SoftwareParameter;
import org.iplantc.service.common.persistence.HibernateUtil;
import org.iplantc.service.jobs.dao.JobDao;
import org.iplantc.service.jobs.exceptions.JobException;
import org.iplantc.service.jobs.exceptions.RemoteJobIDParsingException;
import org.iplantc.service.jobs.exceptions.SchedulerException;
import org.iplantc.service.jobs.exceptions.SoftwareUnavailableException;
import org.iplantc.service.jobs.managers.launchers.parsers.RemoteJobIdParser;
import org.iplantc.service.jobs.managers.launchers.parsers.RemoteJobIdParserFactory;
import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.jobs.model.enumerations.JobStatusType;
import org.iplantc.service.jobs.model.scripts.CommandStripper;
import org.iplantc.service.jobs.model.scripts.SubmitScript;
import org.iplantc.service.jobs.model.scripts.SubmitScriptFactory;
import org.iplantc.service.jobs.util.Slug;
import org.iplantc.service.systems.exceptions.SystemUnavailableException;
import org.iplantc.service.systems.model.enumerations.ExecutionType;
import org.iplantc.service.systems.model.enumerations.SystemStatusType;
import org.iplantc.service.transfer.RemoteDataClient;
import org.joda.time.DateTime;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * @author dooley
 * 
 */
public class HPCLauncher extends AbstractJobLauncher 
{
	private final Logger log = Logger.getLogger(HPCLauncher.class);

	protected String batchScriptName = null;
    
	/**
	 * Creates an instance of a JobLauncher capable of submitting jobs to batch
	 * queuing systems on HPC resources.
	 */
	public HPCLauncher(Job job)
	{
		super(job);
	}

	/*
	 * Put the job in the batch scheduling queue
	 */
	@Override
	public void launch() throws JobException, ClosedByInterruptException, SchedulerException, IOException, SystemUnavailableException
	{
		File tempAppDir = null;
		try
		{
			if (software == null || !software.isAvailable()) {
				throw new SoftwareUnavailableException("Application " + job.getSoftwareName() + " is not longer available for execution");
			}
			
			// if the system is down, return it to the queue to wait for the system
			// to come back up.
			if (executionSystem == null || !executionSystem.getStatus().equals(SystemStatusType.UP)) 
			{
				throw new SystemUnavailableException();
			} 
			
			// sets up the application directory to execute this job launch; see comments in method
            createTempAppDir();
            
            checkStopped();
            
            // copy our application package from the software.deploymentPath to our tempAppDir
            copySoftwareToTempAppDir();
            
            checkStopped();
            
            // prepend the application template with call back to let the Job service know the job has started
            // parse the application template and replace tokens with the inputs, parameters and outputs.
            processApplicationTemplate();
			
            checkStopped();
            
            // create the shadow file containing the exclusion files for archiving
            createArchiveLog(ARCHIVE_FILENAME);
			
            checkStopped();
            
            // move the local temp directory to the remote system
            stageSofwareApplication();
			
            checkStopped();
            
            String remoteJobId = submitJobToQueue();
            
//            JobDao.refresh(job);
            
            job.setSubmitTime(new DateTime().toDate()); // Date job submitted to queue
            job.setLastUpdated(new DateTime().toDate());  // Date job started by queue
            job.setLocalJobId(remoteJobId);
            
            if (!job.getStatus().equals(JobStatusType.RUNNING)) {
                String message = "Job successfully submitted to execution system as local id " + job.getLocalJobId();
	            if (executionSystem.getExecutionType() == ExecutionType.HPC) {
	            	message = "HPC job successfully placed into " + 
	            			job.getBatchQueue() + " queue as local job " + job.getLocalJobId();
	            } else if (executionSystem.getExecutionType() == ExecutionType.CLI) {
	            	message = "CLI job successfully forked as process id " + job.getLocalJobId();
	            }
	            
	            log.debug(message.replaceFirst("job", "job " + job.getUuid()));
	            
	            job.setStatus(JobStatusType.QUEUED, message);
	            
	            //   Forked jobs start running right away. if they bomb out right after submission,
	            // then they would stay in the queued state for the entire job runtime before being
	            // cleaned up. By setting the job status to running here, we can activate the monitor
	            // immediately and keep the feedback loop on failed jobs tight. 
	            //   It's worth noting that the RUNNING status on valid jobs will still come through, 
	            // but it will be ignored since the job state is already running. no harm no foul.  
	            if (software.getExecutionType() == ExecutionType.CLI) {
	            	job.setStatus(JobStatusType.RUNNING, message);
	            }
            }
            else
            {
                log.debug("Callback already received for job " + job.getUuid() 
                        + " skipping duplicate status update.");    
            }
            
            JobDao.persist(job);
		}
		catch (ClosedByInterruptException e) {
            throw e;
        }
        catch (SchedulerException e) {
			throw e;
		}
		catch (Exception e)
		{
			throw new JobException(e);
		}
		finally
		{
			try { HibernateUtil.closeSession(); } catch (Exception e) {}
			try { FileUtils.deleteDirectory(tempAppDir); } catch (Exception e) {}
//			try { remoteSoftwareDataClient.disconnect(); } catch (Exception e) {}
//			try { remoteExecutionDataClient.disconnect(); } catch (Exception e) {}
			try { submissionClient.close(); } catch (Exception e) {}
		}
	}
	
	/* (non-Javadoc)
	 * @see org.iplantc.service.jobs.managers.launchers.AbstractJobLauncher#processApplicationTemplate()
	 */
	@Override
    public File processApplicationTemplate() throws JobException 
    {
		step = "Process the " + job.getSoftwareName() + " wrapper template for job " + job.getUuid();
		 
        log.debug(step);
        FileWriter batchWriter = null;
        String appTemplate = null;
        File ipcexeFile = null;
        RemoteDataClient remoteExecutionDataClient = null;
        try 
		{
			// create the submit script in the temp folder
        	batchScriptName = Slug.toSlug(job.getName()) + ".ipcexe";
        	ipcexeFile = new File(tempAppDir + File.separator + batchScriptName);
			batchWriter = new FileWriter(ipcexeFile);
			
			SubmitScript script = SubmitScriptFactory.getScript(job);
	
			// write the script header
			batchWriter.write(script.getScriptText());
			
			remoteExecutionDataClient = software.getExecutionSystem().getRemoteDataClient(job.getInternalUsername());
			
			String absWorkDir = remoteExecutionDataClient.resolvePath(job.getWorkPath());
			
			batchWriter.write("\ncd " + absWorkDir + "\n");
			
			// write the callback to trigger status update at start
			batchWriter.write(resolveMacros("\n${AGAVE_JOB_CALLBACK_RUNNING} \n\n"));
//					"\ncurl -sSk \"" + Settings.IPLANT_JOB_SERVICE
//					+ "trigger/job/" + job.getUuid() + "/token/"
//					+ job.getUpdateToken() + "/status/" + JobStatusType.RUNNING
//					+ "?pretty=true\" 1>&2 \n\n");
			
			
			batchWriter.write("\n\n# Environmental settings for "
					+ job.getSoftwareName() + ":\n\n");
	
			
			// add modules. The irods module is required for all HPC systems
			// we add it last so it can't be purged by app module commands
			for (String module : software.getModulesAsList()) {
				batchWriter.write("module " + module + "\n");
			}
			
			// add in any custom environment variables that need to be set
            if (!StringUtils.isEmpty(software.getExecutionSystem().getEnvironment())) {
            	batchWriter.write(software.getExecutionSystem().getEnvironment() + "\n");
        	}
            
            //batchWriter.write("module load irods\n"); // already loaded by default
			
			// read in the template file
			File appTemplateFile = new File(tempAppDir
					+ File.separator + software.getExecutablePath());
			
			if (!appTemplateFile.exists()) {
				throw new FileNotFoundException("Unable to locate wrapper script for \"" + 
						software.getUniqueName() + "\" at " + 
						appTemplateFile.getAbsolutePath());
			}
			
			appTemplate = FileUtils.readFileToString(appTemplateFile);
			
			// replace the parameters with their passed in values
			JsonNode jobParameters = job.getParametersAsJsonObject();
			
			for (SoftwareParameter param: software.getParameters())
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
			JsonNode jobInputs = job.getInputsAsJsonObject();
			
			for (SoftwareInput input: software.getInputs())
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
			
			// strip out all references to banned commands such as icommands, etc
			if (executionSystem.isPubliclyAvailable()) {
				appTemplate = CommandStripper.strip(appTemplate); 
			}
			
			// strip out premature completion callbacks that might result in archiving starting before
			// the script exists.
			appTemplate = filterRuntimeStatusMacros(appTemplate);
			
			// add the success statement after the template by default. The user can add failure catches
			// in their scripts that will trump a later success status.
			appTemplate = appTemplate + "\n\n${AGAVE_JOB_CALLBACK_CLEANING_UP}\n";
			
			// Replace all the runtime callback notifications
			appTemplate = resolveRuntimeNotificationMacros(appTemplate);
						
			// Replace all the iplant template tags
			appTemplate = resolveMacros(appTemplate);
			
			batchWriter.write(appTemplate);
			
			return ipcexeFile;
		} 
        catch (IOException e) {
            e.printStackTrace();
            throw new JobException("FileUtil operation failed", e);
        } 
        catch (JobException e) {
            throw new JobException("Json failure from job inputs or parameters", e);
        }
		catch (URISyntaxException e) {
			e.printStackTrace();
            throw new JobException("Failed to parse input URI", e);
		} 
        catch (Exception e) {
			e.printStackTrace();
			throw new JobException("Failed to resolve remote execution system prior to staging app", e);
		} 
        finally {
            try {
                batchWriter.close();
            } catch (IOException e) {
                log.debug("Failed to close batchWriter on Exception");
            }
            try {remoteExecutionDataClient.disconnect();} catch (Exception e) {}
        }
    }
	
	/* (non-Javadoc)
	 * @see org.iplantc.service.jobs.managers.launchers.AbstractJobLauncher#submitJobToQueue()
	 */
	@Override
	protected String submitJobToQueue() throws JobException, SchedulerException
	{
		String submissionResponse = null;
		RemoteDataClient remoteExecutionDataClient = null;
		try
		{
			submissionClient = executionSystem.getRemoteSubmissionClient(job.getInternalUsername());
			
			remoteExecutionDataClient = executionSystem.getRemoteDataClient(job.getInternalUsername());
			
			String cdCommand = "source ~/.bashrc; cd " + remoteExecutionDataClient.resolvePath(job.getWorkPath());
			
			String chmodCommand = "chmod +x " + batchScriptName;
			
			String submitCommand = executionSystem.getScheduler().getBatchSubmitCommand() + " "
					+ batchScriptName;
			
			submissionResponse = submissionClient.runCommand(
					cdCommand + "; " + chmodCommand + "; " + submitCommand);
					
			if (StringUtils.isEmpty(StringUtils.trimToNull(submissionResponse))) 
			{
				// retry once just in case it was a flickr
				submissionResponse = submissionClient.runCommand(
						cdCommand + "; " + chmodCommand + "; " + submitCommand);
				
				if (StringUtils.isEmpty(StringUtils.trimToNull(submissionResponse))) 
					throw new JobException("Failed to submit job. " + submissionResponse);
			}
			
			RemoteJobIdParser jobIdParser = 
					new RemoteJobIdParserFactory().getInstance(executionSystem.getScheduler());
			
			return jobIdParser.getJobId(submissionResponse);
		}
		catch (RemoteJobIDParsingException e) {
			throw new JobException(e.getMessage(), e);
		}
		catch (JobException e) {
			throw e;
		}
		catch (SchedulerException e) {
			throw new JobException(e.getMessage(), e);
		}
		catch (Exception e)
		{
			throw new JobException("Failed to submit job to batch queue", e);
		}
		finally
		{
			try { submissionClient.close(); } catch (Exception e){}
			try {remoteExecutionDataClient.disconnect();} catch (Exception e){}
		}
	}
}