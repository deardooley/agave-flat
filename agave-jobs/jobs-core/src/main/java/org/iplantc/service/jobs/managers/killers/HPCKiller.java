package org.iplantc.service.jobs.managers.killers;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.systems.model.ExecutionSystem;

public class HPCKiller extends AbstractJobKiller {

	public HPCKiller(Job job, ExecutionSystem executionSystem)
	{
	    super(job, executionSystem);
	}

    /* (non-Javadoc)
     * @see org.iplantc.service.jobs.managers.killers.AbstractJobKiller#getCommand()
     */
    @Override
    protected String getCommand() {
        return getExecutionSystem().getScheduler().getBatchKillCommand() + " " 
        		+ getJob().getLocalJobId();
    }
    
    /* (non-Javadoc)
     * @see org.iplantc.service.jobs.managers.killers.AbstractJobKiller#getAltCommand()
     */
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
