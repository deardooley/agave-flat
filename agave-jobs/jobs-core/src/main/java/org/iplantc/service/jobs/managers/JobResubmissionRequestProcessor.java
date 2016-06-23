/**
 * 
 */
package org.iplantc.service.jobs.managers;

import org.iplantc.service.jobs.exceptions.JobProcessingException;
import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.systems.model.RemoteSystem;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Handles requests for job resubmission
 * @author dooley
 *
 */
public class JobResubmissionRequestProcessor extends JobRequestProcessor {

	private boolean ignoreInputConflicts = false;
	private boolean ignoreParameterConflicts = false;
	private boolean preserveNotifications = false;
	
	/**
	 * @param jobRequestOwner
	 * @param internalUsername
	 */
	public JobResubmissionRequestProcessor(String username, String internalUsername) {
		this(username, internalUsername, false, false, false);
	}
	
	public JobResubmissionRequestProcessor(String username,
			String internalUsername, boolean ignoreInputConflicts, 
			boolean ignoreParameterConflicts, boolean preserveNotifications) {
		
		super(username, internalUsername);
		setIgnoreInputConflicts(ignoreInputConflicts);
		setIgnoreParameterConflicts(ignoreParameterConflicts);
		setPreserveNotifications(preserveNotifications);
	}

	/**
	 * Takes the previous job representation and treats it like a new job
	 * submission. This is equivalent to calling {@link #processJob(JsonNode, false, false, false) }.
	 *
	 * @param json a JsonNode containing the job request
	 * @return validated job object ready for submission
	 * @throws JobProcessingException
	 */
	@Override
	public Job processJob(JsonNode json)
	throws JobProcessingException
	{
		((ObjectNode)json).remove("archivePath");
        ((ObjectNode)json).remove("archiveSystem");
        
		Job resubmittedJob = super.processJob(json);
		
		return resubmittedJob;
	}
	
	/* (non-Javadoc)
	 * @see org.iplantc.service.jobs.managers.JobRequestProcessor#createArchivePath(org.iplantc.service.systems.model.RemoteSystem, java.lang.String)
	 */
	@Override
	public boolean createArchivePath(RemoteSystem archiveSystem,
			String archivePath) throws JobProcessingException {
		return super.createArchivePath(archiveSystem, archivePath);
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.jobs.managers.JobRequestProcessor#getInputProcessor()
	 */
	@Override
	public JobRequestInputProcessor getInputProcessor() {
		if (this.inputProcessor == null) {
			this.inputProcessor = 
				new JobResubmissionRequestInputProcessor(username, internalUsername, 
						getSoftware(), isIgnoreInputConflicts());
		}
		return inputProcessor;
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.jobs.managers.JobRequestProcessor#setInputProcessor(org.iplantc.service.jobs.managers.JobRequestInputProcessor)
	 */
	@Override
	public void setInputProcessor(JobRequestInputProcessor inputProcessor) {
		super.setInputProcessor(inputProcessor);
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.jobs.managers.JobRequestProcessor#getParameterProcessor()
	 */
	@Override
	public JobRequestParameterProcessor getParameterProcessor() {
		if (parameterProcessor == null) {
			parameterProcessor = new JobResubmissionRequestParameterProcessor(getSoftware(), isIgnoreParameterConflicts());
		}
		return parameterProcessor;
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.jobs.managers.JobRequestProcessor#setParameterProcessor(org.iplantc.service.jobs.managers.JobRequestParameterProcessor)
	 */
	@Override
	public void setParameterProcessor(
			JobRequestParameterProcessor parameterProcessor) {
		super.setParameterProcessor(parameterProcessor);
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.jobs.managers.JobRequestProcessor#getNotificationProcessor()
	 */
	@Override
	public JobRequestNotificationProcessor getNotificationProcessor() {
		return super.getNotificationProcessor();
	}
	
	/**
	 * @return the ignoreInputConflicts
	 */
	public boolean isIgnoreInputConflicts() {
		return ignoreInputConflicts;
	}

	/**
	 * @param ignoreInputConflicts the ignoreInputConflicts to set
	 */
	public void setIgnoreInputConflicts(boolean ignoreInputConflicts) {
		this.ignoreInputConflicts = ignoreInputConflicts;
	}

	/**
	 * @return the ignoreParameterConflicts
	 */
	public boolean isIgnoreParameterConflicts() {
		return ignoreParameterConflicts;
	}

	/**
	 * @param ignoreParameterConflicts the ignoreParameterConflicts to set
	 */
	public void setIgnoreParameterConflicts(boolean ignoreInputConflicts) {
		this.ignoreParameterConflicts = ignoreInputConflicts;
	}

	/**
	 * @return the preserveNotifications
	 */
	public boolean isPreserveNotifications() {
		return preserveNotifications;
	}

	/**
	 * @param preserveNotifications the preserveNotifications to set
	 */
	public void setPreserveNotifications(boolean preserveNotifications) {
		this.preserveNotifications = preserveNotifications;
	}

}
