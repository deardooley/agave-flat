/**
 * 
 */
package org.iplantc.service.remote;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.systems.model.ExecutionSystem;
import org.iplantc.service.systems.model.enumerations.RemoteShell;

/**
 * @author dooley
 *
 */
public class RemoteSubmssionClientUtils {

	
	private static final Logger log = Logger
			.getLogger(RemoteSubmssionClientUtils.class);
	
	private ExecutionSystem system;
	private RemoteSubmissionClient submissionClient;
	
	/**
	 * 
	 */
	public RemoteSubmssionClientUtils(ExecutionSystem system) {
		setSystem(system);
		
	}
	
	/**
	 * 
	 */
	public RemoteSubmssionClientUtils(ExecutionSystem system, RemoteSubmissionClient submissionClient) {
		setSystem(system);
	}
	
	@SuppressWarnings("unused")
	private RemoteShell findRemoteShell() throws Exception
	{
	    log.debug("Fetching remote shell for system " + getSystem().getSystemId());
	    String submissionResponse = getSubmissionClient().runCommand("echo $SHELL");
		
		if (StringUtils.isBlank(submissionResponse)) {
			return RemoteShell.BASH;
		} 
		else {
			submissionResponse = FilenameUtils.getName(StringUtils.trim(submissionResponse));
		}
		
		try {
			return RemoteShell.valueOf(submissionResponse.toUpperCase());
		} catch (Exception e) {
			log.error("Unrecognized remote shell: " +  submissionResponse + ". Falling back to BASH instead.");
			return RemoteShell.BASH;
		}
	}
	
	@SuppressWarnings("unused")
	private RemoteShell isLustreFileSystem() throws Exception
	{
	    log.debug("Fetching remote shell for system " + getSystem().getSystemId());
	    String submissionResponse = getSubmissionClient().runCommand("( which lfs 2>&1 1>/dev/null ) && echo true || echo false");
		
		if (StringUtils.isBlank(submissionResponse)) {
			return RemoteShell.BASH;
		} 
		else {
			submissionResponse = FilenameUtils.getName(StringUtils.trim(submissionResponse));
		}
		
		try {
			return RemoteShell.valueOf(submissionResponse.toUpperCase());
		} catch (Exception e) {
			log.error("Unrecognized remote shell: " +  submissionResponse + ". Falling back to BASH instead.");
			return RemoteShell.BASH;
		}
	}

	/**
	 * @return the system
	 */
	public ExecutionSystem getSystem() {
		return system;
	}

	/**
	 * @param system the system to set
	 */
	public void setSystem(ExecutionSystem system) {
		this.system = system;
	}

	/**
	 * @return the submissionClient
	 */
	public RemoteSubmissionClient getSubmissionClient() {
		return submissionClient;
	}

	/**
	 * @param submissionClient the submissionClient to set
	 */
	public void setSubmissionClient(RemoteSubmissionClient submissionClient) {
		this.submissionClient = submissionClient;
	}

}
