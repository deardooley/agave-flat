package org.iplantc.service.jobs.model.scripts;

import org.apache.commons.lang.StringUtils;
import org.iplantc.service.jobs.Settings;
import org.iplantc.service.jobs.model.enumerations.JobStatusType;

public class CommandStripper {

	public static String strip(String text) {

		for (String command: Settings.BLACKLIST_COMMANDS) {
			text = StringUtils.replace(text, command, "echo 'Invalid Command " + command + "'");
		}

		// remove all callback status macros except JobStatusType.FAILED
		for (JobStatusType status: JobStatusType.values()) {
			if (!status.equals(JobStatusType.FAILED))
			{
				text = text.replaceAll("(?i)\\$\\{IPLANT_" + status.name() + "\\}", "");
				text = text.replaceAll("(?i)\\$\\{AGAVE_JOB_CALLBACK_" + status.name() + "\\}", "");
			}
		}

		return text;
	}
}
