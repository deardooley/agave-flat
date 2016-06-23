package org.iplantc.service.jobs.managers.killers;

import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.systems.model.ExecutionSystem;

public class HPCKiller extends AbstractJobKiller {

	public HPCKiller(Job job, ExecutionSystem executionSystem)
	{
	    super(job, executionSystem);
	}

    @Override
    protected String getCommand() {
        return getExecutionSystem().getScheduler().getBatchKillCommand() + " " + getJob().getLocalJobId();
    }

}
