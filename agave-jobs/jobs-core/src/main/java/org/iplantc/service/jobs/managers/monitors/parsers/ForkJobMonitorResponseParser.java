package org.iplantc.service.jobs.managers.monitors.parsers;

import org.apache.commons.lang.StringUtils;
import org.iplantc.service.jobs.exceptions.RemoteJobMonitorResponseParsingException;

public class ForkJobMonitorResponseParser implements JobMonitorResponseParser {

	@Override
	public boolean isJobRunning(String output) throws RemoteJobMonitorResponseParsingException
	{
		if (!StringUtils.isEmpty(output)) {
			String[] lines = output.replaceAll("\r", "\n").split("\n");
			for(String line: lines) {
				String trimmedLine = line.trim();
				// bad syntax...possible on some OS, in theory
				if (StringUtils.isEmpty(trimmedLine) || StringUtils.startsWithAny(trimmedLine, new String[]{"[", "usage", "ps"})) {
					continue;
				}
				// pid is invalid
				else if (StringUtils.startsWith(trimmedLine, "ps: Invalid process id")) {
					throw new RemoteJobMonitorResponseParsingException(output); 
				}
				// response isn't a show stopper out of the box. parse for more info
				else {
					String[] tokens = StringUtils.split(trimmedLine);
					String localId = StringUtils.trim(tokens[0]);
					if (localId == null || !StringUtils.isNumeric(localId)) {
						throw new RemoteJobMonitorResponseParsingException(output); 
					}
					else {
						return true;
					}
				}
			}
		}
		return false;
//		throw new RemoteJobMonitorResponseParsingException("No response from server when checking for process id");
	}

}
