package org.iplantc.service.jobs.managers.killers;

import org.apache.commons.lang.StringUtils;
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

	@Override
	protected String getAltCommand() {
		// if the response was empty, the job could be done, but the scheduler could only 
		// recognize numeric job ids. Let's try again with just the numeric part
    	String numericJobId = getJob().getNumericLocalJobId();
    	
		if (StringUtils.isEmpty(numericJobId) || 
				StringUtils.equals(numericJobId, getJob().getLocalJobId())) {
			return null;
		}
		else {
			return getExecutionSystem().getScheduler().getBatchKillCommand() + " " 
					+ numericJobId;
		}
	}
}
