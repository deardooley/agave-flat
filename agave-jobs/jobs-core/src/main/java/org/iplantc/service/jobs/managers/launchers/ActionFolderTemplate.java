package org.iplantc.service.jobs.managers.launchers;

import java.io.File;

import org.apache.commons.lang.StringUtils;
import org.iplantc.service.apps.dao.SoftwareDao;
import org.iplantc.service.apps.model.Software;
import org.iplantc.service.apps.util.ServiceUtils;
import org.iplantc.service.jobs.managers.JobManager;
import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.jobs.model.enumerations.WrapperTemplateAttributeVariableType;
import org.iplantc.service.jobs.model.enumerations.WrapperTemplateStatusVariableType;
import org.iplantc.service.systems.model.ExecutionSystem;
import org.iplantc.service.systems.model.enumerations.SchedulerType;

class ActionFolderTemplate {
	private String	baseUrl;
	private Job		job;

	public ActionFolderTemplate(Job job, String baseUrl)
	{
		this.job = job;
		this.baseUrl = baseUrl;
	}

	public String toString()
	{
		Software software = SoftwareDao.getSoftwareByUniqueName(job.getSoftwareName());
		
		String wrapperTemplate = ServiceUtils.getContents(
				new File(JobManager.class.getClassLoader().getResource("submit_action.template").getPath()));
		
		for (WrapperTemplateAttributeVariableType macro: WrapperTemplateAttributeVariableType.values()) {
            wrapperTemplate = StringUtils.replace(wrapperTemplate, "${" + macro.name() + "}", macro.resolveForJob(job));
        }
        
        for (WrapperTemplateStatusVariableType macro: WrapperTemplateStatusVariableType.values()) {
            wrapperTemplate = StringUtils.replace(wrapperTemplate, "${" + macro.name() + "}", macro.resolveForJob(job));
        }
        
        return wrapperTemplate;
	}

	private String getSubmitCommandForScheduler(SchedulerType schedulerType)
	{
		if (schedulerType.equals(SchedulerType.SGE)
				|| schedulerType.equals(SchedulerType.PBS)
				|| schedulerType.equals(SchedulerType.COBALT))
		{

			return "qsub";

		}
		else if (schedulerType.equals(SchedulerType.LOADLEVELER))
		{

			return "llsubmit";

		}
		else if (schedulerType.equals(SchedulerType.LSF))
		{

			return "bsub";

		}
		else if (schedulerType.equals(SchedulerType.SGE))
		{

			return "condor-submit";

		}
		else
		{ // fork scheduler
			return "sh";
		}
	}
}
