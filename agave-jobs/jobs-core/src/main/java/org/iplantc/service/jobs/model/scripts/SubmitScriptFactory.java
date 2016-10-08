/**
 * 
 */
package org.iplantc.service.jobs.model.scripts;

import org.iplantc.service.apps.dao.SoftwareDao;
import org.iplantc.service.apps.model.Software;
import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.systems.model.ExecutionSystem;
import org.iplantc.service.systems.model.enumerations.SchedulerType;

/**
 * Factory for instantiating a
 * 
 * @author dooley
 * 
 */
public class SubmitScriptFactory {

	public static SubmitScript getScript(Job job)
	{
		Software software = SoftwareDao.getSoftwareByUniqueName(job.getSoftwareName());
		ExecutionSystem system = software.getExecutionSystem();
		SchedulerType scheduler = system.getScheduler();
		
		if (scheduler == null)
		{
			return new SgeSubmitScript(job);
		}
		else if (scheduler.equals(SchedulerType.SGE))
		{
			return new SgeSubmitScript(job);
		}
		else if (scheduler.equals(SchedulerType.LSF))
		{
			return new LsfSubmitScript(job);
		}
		else if (scheduler.equals(SchedulerType.PBS)
				|| scheduler.equals(SchedulerType.COBALT))
		{
			return new PbsSubmitScript(job);
		}
		else if (scheduler.equals(SchedulerType.TORQUE) ||
				scheduler.equals(SchedulerType.MOAB))
		{
			return new TorqueSubmitScript(job);
		}
		else if (scheduler.equals(SchedulerType.SLURM))
		{
			return new SlurmSubmitScript(job);
		}
		else if (scheduler.equals(SchedulerType.LOADLEVELER))
		{
			return new LoadLevelerSubmitScript(job);
		}
		else if (scheduler == SchedulerType.CUSTOM_GRIDENGINE) {
			return new CustomGridEngineSubmitScript(job);
		}
		else if (scheduler == SchedulerType.CUSTOM_TORQUE) {
			return new CustomTorqueSubmitScript(job);
		}
		else if (scheduler == SchedulerType.CUSTOM_PBS) {
			return new CustomPbsSubmitScript(job);
		}
		else if (scheduler == SchedulerType.CUSTOM_SLURM) {
			return new CustomSlurmSubmitScript(job);
		}
		else if (scheduler == SchedulerType.CONDOR) {
			return new CondorSubmitScript(job);
		}
		else if (scheduler == SchedulerType.CUSTOM_CONDOR) {
			return new CustomCondorSubmitScript(job);
		}
		else
		{
			return new ForkSubmitScript(job);
		}
	}

}
