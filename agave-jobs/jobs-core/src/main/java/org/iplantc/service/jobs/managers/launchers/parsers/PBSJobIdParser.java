package org.iplantc.service.jobs.managers.launchers.parsers;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.iplantc.service.jobs.exceptions.JobException;
import org.iplantc.service.jobs.exceptions.RemoteJobIDParsingException;
import org.iplantc.service.jobs.exceptions.SchedulerException;

/**
 * Parses the output from a qsub command into a local job id 
 * that can be used for querying later on.
 * 
 * @author dooley
 *
 */
public class PBSJobIdParser implements RemoteJobIdParser {

	@Override
	public String getJobId(String output) throws RemoteJobIDParsingException, JobException, SchedulerException
	{
		String jobID = null;
		Pattern pattern = Pattern.compile("([0-9]+\\.[^\\s]*)");
		
		String lines[] = output.replaceAll("\r", "\\n").split("\n");
		for (int idx=0; idx<lines.length; idx++) {
			Matcher matcher = pattern.matcher(lines[idx]);
			if (matcher.matches()) {
				jobID = matcher.group(1);
				break;
			}
		}
		
		if (StringUtils.isEmpty(jobID)) {
			if (output.contains("qsub") || output.contains("submit error")) {
				throw new SchedulerException(output); 
			}
			else {
				throw new RemoteJobIDParsingException(output);
			}
		} else {
			return jobID;
		}
	}

}
