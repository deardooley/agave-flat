package org.iplantc.service.jobs.managers.parsers;

import org.iplantc.service.jobs.exceptions.JobException;
import org.iplantc.service.jobs.exceptions.RemoteJobIDParsingException;
import org.iplantc.service.jobs.exceptions.SchedulerException;

public interface RemoteJobIdParser {

	/**
	 * Takes in the output from a job submission to a scheduler and 
	 * returns the local job id on that scheduler.
	 * 
	 * @param output
	 * @return
	 * @throws SchedulerException 
	 * @throws JobException 
	 */
	public String getJobId(String output) 
	throws RemoteJobIDParsingException, JobException, SchedulerException;
}
