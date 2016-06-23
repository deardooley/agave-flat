package org.iplantc.service.jobs.schedulers;

import static org.iplantc.service.jobs.model.enumerations.JobStatusType.CLEANING_UP;
import static org.iplantc.service.jobs.model.enumerations.JobStatusType.PENDING;
import static org.iplantc.service.jobs.model.enumerations.JobStatusType.STAGED;

import java.util.Set;

import org.iplantc.service.common.Settings;
import org.iplantc.service.common.discovery.ServiceCapability;
import org.iplantc.service.common.exceptions.TaskSchedulerException;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.common.schedulers.AgaveTaskScheduler;
import org.iplantc.service.jobs.dao.JobDao;
import org.iplantc.service.jobs.exceptions.JobException;
import org.iplantc.service.jobs.model.enumerations.JobStatusType;

/**
 * First in first out scheduler for trivial processing of tasks. This is
 * appropriate for data staging, etc, but not for job submission or input staging.
 * @author dooley
 *
 */
public class FairScheduler implements AgaveTaskScheduler
{
    private JobStatusType status;
    
    public FairScheduler(JobStatusType status) {
        this.status = status;
    }
	
    /* (non-Javadoc)
	 * @see org.iplantc.service.jobs.schedulers.AgaveTaskScheduler#getNextTaskId(java.util.Set)
	 */
	@Override
	public String getNextTaskId(Set<ServiceCapability> capabilities) throws TaskSchedulerException
	{
		if (status == PENDING) {
		    return getNextQueueJob(capabilities);
		} else if (status == STAGED) {
		    return getNextQueueJob(capabilities);
		} else if (status == CLEANING_UP) {
		    return getNextCleaningUpJob(capabilities);
		} else {
		    throw new TaskSchedulerException("No scheduling implemented for jobs with status " + status);
		}
	}
	
	/**
	 * Fetches next job with status {@link JobStatusType#CLEANING_UP} that can be
	 * processed by the work with its assigned set of {@link ServiceCapability}.
	 * 
	 * @param capabilities ignored
	 * @return uuid of the next job
	 * @throws TaskSchedulerException
	 */
	public String getNextCleaningUpJob(Set<ServiceCapability> capabilities) throws TaskSchedulerException
    {
        try {
            return JobDao.getFairButRandomJobUuidForNextArchivingTask(
                    TenancyHelper.getDedicatedTenantIdForThisService(),
                    Settings.getDedicatedUsernamesFromServiceProperties(),
                    Settings.getDedicatedSystemIdsFromServiceProperties());
        } catch (JobException e) {
            throw new TaskSchedulerException(e);
        }
    }
	
	/**
     * Fetches next job with this scheduler instance's {@link #status} that can be
     * processed by the work with its assigned set of {@link ServiceCapability}.
     * 
     * @param capabilities  ignored
     * @return uuid of the next job
     * @throws TaskSchedulerException
     */
    public String getNextQueueJob(Set<ServiceCapability> capabilities) throws TaskSchedulerException
    {
        try {
            return JobDao.getNextQueuedJobUuid(status,
                    TenancyHelper.getDedicatedTenantIdForThisService(),
                    Settings.getDedicatedUsernamesFromServiceProperties(),
                    Settings.getDedicatedSystemIdsFromServiceProperties());
        } catch (JobException e) {
            throw new TaskSchedulerException(e);
        }
    }

}
