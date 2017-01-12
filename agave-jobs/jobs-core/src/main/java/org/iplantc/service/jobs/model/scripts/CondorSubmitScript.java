package org.iplantc.service.jobs.model.scripts;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.apps.dao.SoftwareDao;
import org.iplantc.service.apps.model.Software;
import org.iplantc.service.apps.model.SoftwareInput;
import org.iplantc.service.jobs.Settings;
import org.iplantc.service.jobs.exceptions.JobException;
import org.iplantc.service.jobs.exceptions.JobProcessingException;
import org.iplantc.service.jobs.managers.JobManager;
import org.iplantc.service.jobs.model.Job;

import com.fasterxml.jackson.databind.JsonNode;


/**
 * This class holds methods to dynamically generate a Condor Submit file
 */
public class CondorSubmitScript extends AbstractSubmitScript {
	private final static Logger log = Logger.getLogger(CondorSubmitScript.class);

	public CondorSubmitScript(Job job)
	{
		super(job);
	}

	public enum CondorUniverse {
		vanilla, standard, scheduler, local, java, vm, grid, docker;
	}
	
    String executable 				= "transfer_wrapper.sh";
    CondorUniverse universe;
    String logFilename 				= "runtime.log";
    String initialDir 				= "";
    String shouldTransferFiles    	= "YES";
    String whenToTransferOutput 	= "ON_EXIT";
    String transferInputFiles     	= "transfer.tar.gz";
    String transferOutputFiles    	= "output.tar.gz";
    String arguments;
    String requirements 			= "( OpSys == \"LINUX\" ) ";
    String notification				= "never";
    //String queue 					= "1";
    Properties condorSubmitProperties = new Properties();
    
    static final String Queue = "queue 1";

    /**
     * Creates a String representing a Condor Submit file
     *
     * @return String contents of a Condor Submit file
     */
    @Override
    public String getScriptText() throws JobException
    {
    	try {
	        StringBuilder sb = new StringBuilder();
	        // no custom directives are provided by the batch queue.
	        sb.append("universe     = " + getUniverse() + "\n\n");
	        
	        // no directives, default to linux operating system
	        if (StringUtils.isEmpty(queue.getCustomDirectives())) {
	            sb.append("requirements = " + requirements);
	        }
	        // condor directives are provided, make sure no duplicates are given
	        else {
	        	sb.append(queue.getCustomDirectives());
	        } 
	        sb.append("\n\n");
	        
	        sb.append("# retry job 3 times, pause 5 min between retries\n");
	        sb.append("periodic_release =  (NumJobStarts < " + Settings.MAX_SUBMISSION_RETRIES + ") && ((CurrentTime - EnteredCurrentStatus) > (15*60))\n\n");
	        
	        sb.append("# stay in queue on failures");
	        sb.append("on_exit_hold = (ExitBySignal == True) || (ExitCode != 0)\n\n");
	        
	        sb.append("request_cpus = " + job.getProcessorsPerNode() + "\n");
	        sb.append("request_memory = " + job.getMemoryPerNode() * 1024 + "\n\n");
	        
	        sb.append("executable   = " + executable + "\n");
	        sb.append("input        = " + transferInputFiles + "\n");
	        //sb.append("requirements = " + Requirements + "\n");
	        sb.append("error        = " + standardErrorFile + "\n");
	        sb.append("output       = " + standardOutputFile + "\n");
	        sb.append("log          = " + logFilename + "\n");
	        // sb.append("initialdir   = " + initialdir + "\n");
	        sb.append("should_transfer_files  = " + shouldTransferFiles + "\n");
	        sb.append("when_to_transfer_output = " + whenToTransferOutput+"\n");
	        
	        List<String> inputs = getInputFileTransferList();
	        
	        if (inputs.isEmpty()) {
	        	sb.append("transfer_input_files = " + transferInputFiles + "\n");
	        } else {
	        	sb.append("transfer_input_files = " + transferInputFiles + ", " + StringUtils.join(inputs, ", ") + "\n");
	        }
	        
	        sb.append("notification = never\n");
	        
	        if (!isInCustomDirectives("arguments")) {
	        	
	        }
	        // in the docker universe, arguments are critical to container startup
	        else if (getUniverse() == CondorUniverse.docker) {
	        	String arguments = getParameterArgumentString();
	        	sb.append("arguments = " + arguments + "\n\n");
	        }
	        // arguments will always be empty because we are not invoking the end-user app, but a 
	        // wrapper script that unpacks the job assets on the remote condor execution host 
	        // and starts the agave app wrapper script.
	        else if ( !StringUtils.isEmpty(arguments) ) {
	        		sb.append("arguments    = " + arguments + "\n\n");
	        }
	        
	        // always single job submission
	        sb.append(Queue);
	        sb.append("\n\n");
	        
	        return sb.toString();
    	}
    	catch (JobProcessingException e) {
    		throw new JobException(e.getMessage(), e);
    	}
    }
    
    /**
     * Takes the job parameters and default values for the hidden app parameters and
     * creates a command line argument. Order of the app parameters is preserved. 
     * 
     * The output will contain macros which will be resolved at runtime.
     * 
     * @return
     * @throws JobProcessingException
     * @throws JobException
     */
    protected String getParameterArgumentString() 
	throws JobProcessingException, JobException 
	{
    	StringBuilder arguments = new StringBuilder("arguments = ");
    
		if (StringUtils.isEmpty(getCustomDirectiveValue("arguments"))) {
			JsonNode jobInputs = job.getInputsAsJsonObject();
			Software software = SoftwareDao.getSoftwareByUniqueName(job.getSoftwareName());
 			for (SoftwareInput input: software.getInputs())
 			{
 				if (jobInputs.has(input.getKey()) || !input.isVisible()) 
 				{
 					// add the macro to the argument string. it will be resolved by the launcher
 					// along with the macros. 
 					arguments.append("${" + input.getKey() + "} " );
 				}
 			}	
		}
		
		return arguments.toString();
	}

	/**
     * Determines whether or not the given {@code condorDirective} is 
     * provided in the {@link BatchQueue#getCustomDirectives}.
     * 
     * @param condorDirective
     * @return true of an assignment is made to the {@code condorDirective}, false otherwise. 
     */
    protected boolean isInCustomDirectives(String condorDirective) {
    	if (StringUtils.isEmpty(condorDirective)) {
    		return false;
    	}
    	
    	if (StringUtils.isEmpty(queue.getCustomDirectives())) {
    		return false;
    	}
    	
    	Pattern pattern = Pattern.compile(condorDirective + "(?:\\s)*=");
    	Matcher matcher = pattern.matcher(queue.getCustomDirectives());
    	
    	return matcher.find();
    }
    
    /**
     * Parses the custom directives into a {@link Properties} object and
     * returns the value of the corresponding condor directive in a case-insensitive way.
     * @param condorDirective
     * @return the value of the corresponding {@code condorDirective} or null if not present 
     * @throws JobProcessingException
     */
    protected String getCustomDirectiveValue(String condorDirective) 
    throws JobProcessingException{
    	
    	if (StringUtils.isEmpty(condorDirective)) {
    		return null;
    	}
    	else if (StringUtils.isEmpty(queue.getCustomDirectives())) {
    		return null;
    	}
    	
    	if (this.condorSubmitProperties.isEmpty()) {
	    	this.condorSubmitProperties = new Properties();
	    	ByteArrayInputStream bis = null;
	    	try {
	    		bis = new ByteArrayInputStream(queue.getCustomDirectives().getBytes());
	    		condorSubmitProperties.load(bis);
	    	}
	    	catch (Exception e) {
	    		throw new JobProcessingException(500, "Unable to parse batch queue custom directives for the " 
	    				+ condorDirective + " parameter.", e);
	    	}
	    	finally {
	    		try { bis.close(); } catch (Exception e) {}
	    	}
    	}
    	
    	// keys are case insensitive
    	String searchKey = condorDirective.toLowerCase();
    	
    	// search through the keys for a case insensitive match
    	for (Object key: condorSubmitProperties.keySet()) {
    		if (StringUtils.equalsIgnoreCase((String)key, searchKey)) {
    			return condorSubmitProperties.getProperty((String) key);
    		}
    	}
    	
    	// if not found, return null
    	return null;
    }

	/**
	 * @return
	 * @throws JobException
	 */
	protected List<String> getInputFileTransferList() throws JobException {
		List<String> inputs = new ArrayList<String>();
        try 
        {
        	// add the job inputs
        	Map<String,String[]> jobInputMap = JobManager.getJobInputMap(job);
        	
			for (String inputKey: jobInputMap.keySet()) 
			{
				for (String rawJobInputValue: jobInputMap.get(inputKey))
				{
					try 
					{
						URI rawJobInputUri = new URI(rawJobInputValue);
						String name = FilenameUtils.getName(rawJobInputUri.getPath());
						if (!StringUtils.isEmpty(name)) {
							inputs.add(name);
						}
					} 
					catch (Throwable e) 
					{
						throw new IOException("Failed to add " + rawJobInputValue + " for app input " + 
								inputKey + " to the list of inputs in the condor submit script for job " 
								+ job.getUuid(), e);
					}
				}
			}
		} 
        catch (IOException e) {
        	throw new JobException(e.getMessage());
        }
        catch (JobException e) {
			throw new JobException("Failed to add inputs to the condor submit script for job " + job.getUuid(), e);
		}
		return inputs;
	}

    public String getExecutable() {
        return executable;
    }

    public void setExecutable(String executable) {
        this.executable = executable;
    }

    public CondorUniverse getUniverse() 
    throws JobProcessingException 
    {
    	if (this.universe == null) {
    		String sUniverse = null;
    		try {
    			sUniverse = getCustomDirectiveValue("universe");
    		
	    		if (sUniverse == null) {
	    			this.universe = CondorUniverse.vanilla;
	    		}
	    		else {
	    			this.universe = CondorUniverse.valueOf(sUniverse);
	    		}
    		}
    		catch (JobProcessingException e) {
    			throw e;
    		}
    		catch (IllegalArgumentException e) {
    			throw new JobProcessingException(400, "Unknown condor universe " + sUniverse, e);
    		}
    	}
    	
    	return this.universe;
        
    }

    public void setUniverse(CondorUniverse universe) {
    	this.universe = universe;
    }
    
    public String getLogFilename() {
        return logFilename;
    }

    public void setLogFilename(String log) {
        logFilename = log;
    }

    public String getInitialdir() {
        return initialDir;
    }

    public void setInitialDir(String initialdir) {
        this.initialDir = initialdir;
    }

    public String getShouldTransferFiles() {
        return shouldTransferFiles;
    }

    public void setShouldTransferFiles(String shouldTransferFiles) {
        this.shouldTransferFiles = shouldTransferFiles;
    }

    public String getWhenToTransferOutput() {
        return whenToTransferOutput;
    }

    public void setWhenToTransferOutput(String whenToTransferOutput) {
        this.whenToTransferOutput = whenToTransferOutput;
    }

    public String getTransferInputFiles() {
        return transferInputFiles;
    }

    public void setTransferInputFiles(String transferInputFiles) {
        this.transferInputFiles = transferInputFiles;
    }

    public String getArguments() {
        return arguments;
    }

    public void setArguments(String arguments) {
        this.arguments = arguments;
    }
}