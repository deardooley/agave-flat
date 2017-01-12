package org.iplantc.service.jobs.managers.launchers.parsers;


import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.iplantc.service.jobs.exceptions.JobException;
import org.iplantc.service.jobs.exceptions.RemoteJobIDParsingException;
import org.iplantc.service.jobs.exceptions.SchedulerException;

/**
 * Parses the output from a bsub command into a local job id 
 * that can be used for querying later on.
 * 
 * @author dooley
 *
 */
public class LSFJobIdParser implements RemoteJobIdParser {

	@Override
	public String getJobId(String output) throws RemoteJobIDParsingException, JobException, SchedulerException
	{
		String jobID = null;
		Pattern pattern = Pattern.compile("Job <([\\d]+)> is submitted (?:.*)");
		Matcher matcher = pattern.matcher(output);
		
		// if the response matches the standard LSF verbose bsub response, 
		// return the matching job id
		if (matcher.find())
		{
			jobID = matcher.group(1);
		}
		// otherwise, check for multiline and terse responses
		else {
			pattern = Pattern.compile("([0-9]+).*");
			String lines[] = output.replaceAll("\r", "\n").split("\n");
			
			for (int i=0; i<lines.length; i++) {
				String line = lines[i].trim();
				matcher = pattern.matcher(line);
				if (matcher.matches()) {
					jobID = line.split(" ")[0];
					break;
				}
			}
		}
		
		// YES!!! it worked.
		if (!StringUtils.isEmpty(jobID)) {
			return jobID;
		} 
		// check for repsonses where the scheduler rejects the batch options
		else if (StringUtils.containsIgnoreCase(output, "Job not submitted")) {
			throw new SchedulerException("Job was rejected with the following message: " + output);
		}
		// no idea what happened. just forward the response out
		else {
			throw new JobException("Failed to submit job. Response from the scheduler was: " + output); 
		}
	}
}
