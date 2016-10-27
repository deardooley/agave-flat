/**
 * 
 */
package org.iplantc.service.jobs.managers.monitors.parsers;

import static org.iplantc.service.systems.model.enumerations.ExecutionType.*;
import static org.iplantc.service.systems.model.enumerations.SchedulerType.*;

import org.iplantc.service.systems.dao.SystemDao;
import org.iplantc.service.systems.exceptions.SystemUnavailableException;
import org.iplantc.service.systems.exceptions.SystemUnknownException;
import org.iplantc.service.systems.manager.SystemManager;
import org.iplantc.service.systems.model.ExecutionSystem;
import org.iplantc.service.systems.model.enumerations.ExecutionType;
import org.iplantc.service.systems.model.enumerations.RemoteSystemType;
import org.iplantc.service.systems.model.enumerations.SchedulerType;
import org.iplantc.service.apps.model.Software;
import org.iplantc.service.jobs.managers.JobManager;
import org.iplantc.service.jobs.model.Job;

/**
 * Factory class to get the appropraite {@link JobMonitorResponseParser} for a given {@link Job}
 * 
 * @author dooley
 *
 */
public class JobMonitorResponseParserFactory {

	public static JobMonitorResponseParser getInstance(Job job) throws SystemUnknownException, SystemUnavailableException {
		Software software = JobManager.getJobSoftwarem(job);
		ExecutionSystem system = (ExecutionSystem)new SystemDao().findUserSystemBySystemId(job.getOwner(), job.getSystem(), RemoteSystemType.EXECUTION);
		
		if (system == null) {
			throw new SystemUnknownException("Unable to determine execution system for job.");
		}
		else if (software == null) {
			return _getInstance(system.getExecutionType(), system.getScheduler());
		}
		else {
			return _getInstance(software.getExecutionType(), system.getScheduler());
		}
	}
	
	protected static JobMonitorResponseParser _getInstance(ExecutionType executionType, SchedulerType schedulerType) {
		
		JobMonitorResponseParser parser = null;
		switch (executionType) {
		case CLI:
		case ATMOSPHERE:
			parser = new ForkJobMonitorResponseParser();
			break;
		case CONDOR:
			parser = new CondorJobMonitorResponseParser();
			break;
		case HPC:
			switch (schedulerType) {
				case PBS:
				case CUSTOM_PBS:
				case TORQUE:
				case CUSTOM_TORQUE:
				case MOAB:
				case SGE:
				case CUSTOM_GRIDENGINE:
					parser = new PBSHPCMonitorResponseParser();
					break;
				case SLURM:
				case CUSTOM_SLURM:
					parser = new SlurmHPCMonitorResponseParser();
					break;
				default:
					parser = new DefaultHPCMonitorResponseParser();
					break;
			}
			break;
		default:
			parser = new ForkJobMonitorResponseParser();
			break;
		}
		
		return parser;
	}

}
