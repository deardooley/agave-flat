package org.iplantc.service.jobs.managers;

import org.iplantc.service.jobs.exceptions.JobException;
import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.systems.dao.SystemDao;
import org.iplantc.service.systems.exceptions.SystemUnavailableException;
import org.iplantc.service.systems.model.ExecutionSystem;
import org.iplantc.service.systems.model.enumerations.SystemStatusType;

public class SystemAvailabilityCheck 
{
	private Job job;
	
	public SystemAvailabilityCheck(Job job)
	{
		this.job = job;
	}
	
	public void check() throws JobException, SystemUnavailableException
	{
		ExecutionSystem system = (ExecutionSystem) new SystemDao().findBySystemId(job.getSystem());
		
		if (system == null || !system.isAvailable() || !system.getStatus().equals(SystemStatusType.UP)) {
			throw new SystemUnavailableException("Job execution system " + job.getSystem() + " is not available.");
		}
	}
}
