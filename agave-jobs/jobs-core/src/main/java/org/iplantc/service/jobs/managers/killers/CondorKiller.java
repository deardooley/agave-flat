package org.iplantc.service.jobs.managers.killers;

import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.systems.model.ExecutionSystem;

public class CondorKiller extends AbstractJobKiller {

	public CondorKiller(Job job, ExecutionSystem executionSystem)
    {
        super(job, executionSystem);
    }

    @Override
    protected String getCommand() {
        return getExecutionSystem().getScheduler().getBatchKillCommand() + " " + getJob().getLocalJobId();
    }
}
