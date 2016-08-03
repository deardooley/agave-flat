package org.iplantc.service.jobs.managers.parsers;

import org.apache.commons.lang.StringUtils;
import org.iplantc.service.jobs.exceptions.RemoteJobIDParsingException;

public class ForkJobIdParser implements RemoteJobIdParser {

	@Override
	public String getJobId(String output) throws RemoteJobIDParsingException
	{
		if (!StringUtils.isEmpty(output))
		{
			String[] lines = output.replaceAll("\r", "\n").split("\n");
			for(String line: lines) {
				if (StringUtils.startsWith(line, "[")) continue;
				
				if (StringUtils.isNumeric(line.trim())) {
					return line.trim();
				} else {
					throw new RemoteJobIDParsingException(output); 
				}
			}
		}
		throw new RemoteJobIDParsingException("No response from server upon job launch");
	}

}
