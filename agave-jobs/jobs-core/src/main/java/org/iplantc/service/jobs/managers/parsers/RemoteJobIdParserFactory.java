package org.iplantc.service.jobs.managers.parsers;

import org.iplantc.service.systems.model.enumerations.SchedulerType;

public class RemoteJobIdParserFactory {

	public RemoteJobIdParser getInstance(SchedulerType schedulerType) {
		switch (schedulerType) 
		{
			case CONDOR: 
				return new CondorJobIdParser();
			case LOADLEVELER: 
				return new LoadLevelerJobIdParser();
			case LSF: 
				return new LSFJobIdParser();
			case MOAB: 
				return new MoabJobIdParser();
			case CUSTOM_PBS:
			case PBS: 
				return new PBSJobIdParser();
			case CUSTOM_GRIDENGINE:
			case SGE: 
				return new SGEJobIdParser();
			case CUSTOM_SLURM:
			case SLURM: 
				return new SlurmJobIdParser();
			case CUSTOM_TORQUE:
			case TORQUE: 
				return new TorqueJobIdParser();
			case FORK:
			default: 
				return new ForkJobIdParser();
		}
	}
}
