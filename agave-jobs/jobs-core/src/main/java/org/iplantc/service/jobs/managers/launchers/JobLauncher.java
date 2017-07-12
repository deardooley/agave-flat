package org.iplantc.service.jobs.managers.launchers;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.channels.ClosedByInterruptException;

import org.apache.commons.lang.StringUtils;
import org.iplantc.service.apps.model.Software;
import org.iplantc.service.apps.model.SoftwareInput;
import org.iplantc.service.apps.model.SoftwareParameter;
import org.iplantc.service.jobs.exceptions.JobException;
import org.iplantc.service.jobs.exceptions.QuotaViolationException;
import org.iplantc.service.jobs.exceptions.SchedulerException;
import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.systems.exceptions.SystemUnavailableException;
import org.iplantc.service.systems.model.ExecutionSystem;
import org.iplantc.service.transfer.URLCopy;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * This interface defines the methods required of all job launching
 * implementing classes. Individual classes are responsible for 
 * communicating with the remote system or scheduler, filtering 
 * the job wrapper template, resolving macros, validating inputs 
 * and parameters, etc.
 * 
 * @author dooley
 *
 */
public interface JobLauncher
{
    /**
     * Checks whether this {@link JobLauncher} has been stopped. 
     * @return true if it has been stopped, false otherwise
     */
    public boolean isStopped();
    
    /**
     * Stops the submission task asynchronously.
     * 
     * @param stopped
     */
    public void setStopped(boolean stopped);
    
	/** 
	 * Performs the tasks required to start a job on a remote execution system.
	 *  
	 * @throws JobException
	 * @throws SchedulerException
	 * @throws IOException
	 * @throws QuotaViolationException
	 * @throws SystemUnavailableException
	 */
	public void launch() throws JobException, SchedulerException, IOException, SystemUnavailableException;

	/**
	 * Resolves a parameter JSON value or JSON array of values into a serialized string of variables
	 * adding in the appropriate argument value(s) and applying enquote as needed.
	 *  
	 * @param softwareParameter The SoftwareParameter associated with this value.
	 * @param jsonJobParamValue JsonNode representing value or ArrayNode of values for this parameter
	 * @return serialized String of space-delimited values after enquoting and adding relevant argument(s) 
	 */
	public String parseSoftwareParameterValueIntoTemplateVariableValue(SoftwareParameter softwareParameter, JsonNode jsonJobParamValue);
	
	/**
	 * Takes the wrapper template for an app, resolves all template variables for registered software inputs and parameters, 
	 * callbacks, black and whitelist commands, etc. Resulting content is used to create the *.ipcexe file that will be staged
	 * to the remote system and invoked to start the job.
	 * @throws JobException
	 */
	public File processApplicationTemplate() throws JobException;
	
	/**
	 * Resolves a input JSON value or JSON array of values into a serialized string of variables
	 * adding in the appropriate argument value(s) and applying enquote as needed.
	 *  
	 * @param softwareParameter The SoftwareInput associated with this value.
	 * @param jsonJobParamValue JsonNode representing value or ArrayNode of values for this input
	 * @return serialized String of space-delimited values after enquoting and adding relevant argument(s) 
	 * @throws URISyntaxException 
	 */
	public String parseSoftwareInputValueIntoTemplateVariableValue(SoftwareInput softwareInput, JsonNode jsonJobInputValue) throws URISyntaxException;
	
	/**
	 * Replaces all job macros in a wrapper template with the tenant and job-specific values for this job.
	 *   
	 * @param wrapperTemplate
	 * @return content of wrapper template with all macros resolved.
	 */
	public String resolveMacros(String wrapperTemplate);
	
	/**
	 * Returns the temp directory used by this {@link JobLauncher} to cache app assets as 
	 * they move to the execution system.
	 * 
	 * @return {@link File} object reference to the local folder.
	 */
	public File getTempAppDir();

	/**
	 * Sets the temp directory used by this {@link JobLauncher} to cache the intermediate app
	 * assets during the submission process.
	 * 
	 * @param tempAppDir
	 */
	public void setTempAppDir(File tempAppDir);
    
    /**
     * Checks whether this launcher has been stopped and if so, 
     * throws a {@link ClosedByInterruptException}
     * 
     * @throws ClosedByInterruptException
     */
    void checkStopped() throws ClosedByInterruptException;

    /**
     * Returns the local reference to the {@link URLCopy} instanced used
     * for this job submission.
     * 
     * @return 
     */
    public URLCopy getUrlCopy();

    /**
     * Sets the {@link URLCopy} instance used for data transfer 
     * by this {@link JobLauncher}.
     * 
     * @param urlCopy
     */
    public void setUrlCopy(URLCopy urlCopy);
    
    /**
     * Threadsafe getter of the job passed to the launcher.
     * @return
     */
    public Job getJob();
    
    /**
     * Replaces all {@link WrapperTemplateStatusVariableType#AGAVE_JOB_CALLBACK_NOTIFICATION} macros in a 
     * wrapper template with a code snippet that will take a comma-separated list of environment variable 
     * names and post them back to the API to be forwarded as a JSON object to a notification event. 
     * The form of the macros with variables.
     *   
     * @param wrapperTemplate
     * @return content of wrapper template with all macros resolved.
     */
    public String resolveRuntimeNotificationMacros(String wrapperTemplate);
    
    /**
	 * Replaces all {@link ExecutionSystem#startupScript} macros with the system and job-specific 
	 * values for this job.
	 *   
	 * @param startupScript
	 * @return null if the value is blank, the value of the {@code resolveStartupScriptMacros) 
	 * filtered with job and system macros otherwise.
	 */
	public String resolveStartupScriptMacros(String startupScript);
	
	/**
	 * @return the executionSystem
	 */
	public ExecutionSystem getExecutionSystem();

	/**
	 * @param executionSystem the executionSystem to set
	 */
	public void setExecutionSystem(ExecutionSystem executionSystem);

	/**
	 * @return the software
	 */
	public Software getSoftware();

	/**
	 * @param software the software to set
	 */
	public void setSoftware(Software software);

	/**
	 * @param job the job to set
	 */
	public void setJob(Job job);

	/**
	 * Generates the command to source the {@link ExecutionSystem#getStartupScript()} and log the 
	 * response to the {@code .agave.log} file in the job work directory.
	 * @return the properly escaped command to be run on the remote system.
	 * @throws SystemUnavailableException
	 */
	public abstract String getStartupScriptCommand(String absoluteRemoteWorkPath)
			throws SystemUnavailableException;

}