package org.iplantc.service.jobs.managers.monitors.parsers;

import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.jobs.exceptions.RemoteJobMonitorEmptyResponseException;
import org.iplantc.service.jobs.exceptions.RemoteJobMonitorResponseParsingException;
import org.iplantc.service.jobs.exceptions.RemoteJobUnrecoverableStateException;
import org.iplantc.service.jobs.managers.JobManager;
import org.iplantc.service.jobs.model.enumerations.JobStatusType;
import org.joda.time.DateTime;

public class DefaultHPCMonitorResponseParser implements JobMonitorResponseParser {
	
	private static final Logger log = Logger
			.getLogger(DefaultHPCMonitorResponseParser.class);
	
	@Override
	public boolean isJobRunning(String result) throws RemoteJobMonitorEmptyResponseException, RemoteJobMonitorResponseParsingException, RemoteJobUnrecoverableStateException
	{
		if (StringUtils.isEmpty(result)) {
			return false;
			//throw new RemoteJobMonitorEmptyResponseException("Empty response received from job status check on the remote system");
		}
		else if (result.toLowerCase().contains("unknown")
				|| result.toLowerCase().contains("error") 
				|| result.toLowerCase().contains("not ")) 
		{
			return false;
		}
		else if (java.util.Arrays.asList(StringUtils.split(result)).contains("Eqw")) 
		{
			throw new RemoteJobUnrecoverableStateException();
		}
		
//		if (!StringUtils.isEmpty(output)) {
//			String[] lines = output.replaceAll("\r", "\n").split("\n");
//			for(String line: lines) {
//				String trimmedLine = line.trim();
//				// bad syntax...possible on some OS, in theory
//				if (StringUtils.isEmpty(trimmedLine) || StringUtils.startsWithAny(trimmedLine, new String[]{"[", "usage", "ps"})) {
//					continue;
//				}
//				// pid is invalid
//				else if (StringUtils.startsWith(trimmedLine, "ps: Invalid process id")) {
//					throw new RemoteJobMonitorResponseParsingException(output); 
//				}
//				// response isn't a show stopper out of the box. parse for more info
//				else {
//					String[] tokens = StringUtils.split(trimmedLine);
//					String localId = StringUtils.trim(tokens[0]);
//					if (localId == null || !StringUtils.isNumeric(localId)) {
//						throw new RemoteJobMonitorResponseParsingException(output); 
//					}
//					else {
//						return true;
//					}
//				}
		
		return true;
		
//		throw new RemoteJobMonitorResponseParsingException("No response from server when checking for process id");
	}

}
