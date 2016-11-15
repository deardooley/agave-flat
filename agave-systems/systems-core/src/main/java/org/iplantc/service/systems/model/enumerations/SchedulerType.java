/**
 * 
 */
package org.iplantc.service.systems.model.enumerations;

/**
 * Types of batch schedulers.
 * 
 * @author dooley
 * 
 */
public enum SchedulerType
{
	LSF, CUSTOM_LSF, LOADLEVELER, CUSTOM_LOADLEVELER, PBS, CUSTOM_PBS, SGE, CUSTOM_GRIDENGINE, CONDOR, CUSTOM_CONDOR, FORK, COBALT, TORQUE, CUSTOM_TORQUE, MOAB, SLURM, CUSTOM_SLURM, UNKNOWN;

	public String getBatchSubmitCommand() 
	{
		switch (this) 
		{
			case LSF:
			case CUSTOM_LSF:
			case COBALT:
				return "bsub";
			case LOADLEVELER:
			case CUSTOM_LOADLEVELER:
				return "llsub";
			case TORQUE:
			case CUSTOM_TORQUE:
			case CUSTOM_PBS:
			case MOAB:
			case PBS:
			case SGE:
			case CUSTOM_GRIDENGINE:
				return "qsub";
			case CUSTOM_CONDOR:
			case CONDOR:
				return "condor_submit";
			case UNKNOWN:
			case FORK:
				return "";
			case SLURM:
			case CUSTOM_SLURM:
				return "sbatch";
			default:
				return "qsub";
				
		}
	}
	
	public String getBatchKillCommand() 
	{
		switch (this) 
		{
			case LSF:
			case CUSTOM_LSF:
			case COBALT:
				return "bkill ";
			case LOADLEVELER:
			case CUSTOM_LOADLEVELER:
				return "llcancel ";
			case TORQUE:
			case CUSTOM_TORQUE:
			case CUSTOM_PBS:
			case MOAB:
			case PBS:
			case SGE:
			case CUSTOM_GRIDENGINE:
				return "qdel ";
			case CUSTOM_CONDOR:
			case CONDOR:
				return "condor_rm ";
			case UNKNOWN:
			case FORK:
				return "kill -9 ";
			case SLURM:
			case CUSTOM_SLURM:
				return "scancel ";
			default:
				return "qdel ";
		}
	}
	
	@Override
	public String toString() {
		return name();
	}

	public String getBatchQueryCommand()
	{
		switch (this) 
		{
			case LSF:
			case CUSTOM_LSF:
			case COBALT:
				return "bhist";
			case LOADLEVELER:
			case CUSTOM_LOADLEVELER:
				return "llq";
			case PBS:
			case CUSTOM_PBS:
			case TORQUE:
			case CUSTOM_TORQUE:
			case MOAB:
			case SGE:
			case CUSTOM_GRIDENGINE:
				return "qstat -w | grep ";
			case CUSTOM_CONDOR:
			case CONDOR:
				return "condor_q -format '%d'  JobStatus";
			case UNKNOWN:
			case FORK:
				return "ps -o pid= -o comm= -p ";
			case SLURM:
			case CUSTOM_SLURM:
//				return "sacct -p -o 'JobIDRaw,State,ExitCode,Start,End,Elapsed,CPUTimeRaw,DerivedExitCode,NCPUS,NNodes' -j ";
				return "sacct -p -o 'JOBID,State,ExitCode' -n -j ";
			default:
				return "qstat";
		}
	}
}