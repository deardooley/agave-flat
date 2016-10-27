package org.iplantc.service.jobs.managers.launchers.parsers;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.iplantc.service.jobs.exceptions.RemoteJobIDParsingException;

/**
 * Parses the output from a llsubmit command into a local job id 
 * that can be used for querying later on.
 * 
 * @author dooley
 *
 */
public class LoadLevelerJobIdParser implements RemoteJobIdParser {

	@Override
	public String getJobId(String output) throws RemoteJobIDParsingException
	{
		String jobID = null;
		Pattern pattern = Pattern.compile("([0-9]+).*");
		
		String lines[] = output.replaceAll("\r", "\n").split("\n");
		for (int idx=0; idx<lines.length; idx++) {
			String line = lines[idx].trim();
			Matcher matcher = pattern.matcher(line);
			if (matcher.matches()) {
				jobID = line.split(" ")[0];
				break;
			}
		}
		
		if (StringUtils.isEmpty(jobID)) {
			throw new RemoteJobIDParsingException(output);
		} else {
			return jobID;
		}
	}

}
