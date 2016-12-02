/**
 * 
 */
package org.iplantc.service.jobs.queue.actions;

import java.nio.channels.ClosedByInterruptException;

import org.apache.log4j.Logger;
import org.hibernate.StaleObjectStateException;
import org.hibernate.UnresolvableObjectException;
import org.iplantc.service.jobs.exceptions.JobDependencyException;
import org.iplantc.service.jobs.exceptions.JobException;
import org.iplantc.service.jobs.exceptions.JobFinishedException;
import org.iplantc.service.jobs.managers.monitors.JobMonitor;
import org.iplantc.service.jobs.managers.monitors.JobMonitorFactory;
import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.jobs.phases.workers.IPhaseWorker;
import org.iplantc.service.systems.exceptions.SystemUnavailableException;
import org.iplantc.service.systems.exceptions.SystemUnknownException;

/**
 * @author dooley
 *
 */
public class MonitoringAction extends AbstractWorkerAction {
    
    private static Logger log = Logger.getLogger(MonitoringAction.class);
    
    private JobMonitor jobMonitor = null;
    
    public MonitoringAction(Job job, IPhaseWorker worker) {
        super(job, worker);
    }
    
    /**
     * This method attempts to archive a job's output by retrieving the
     * .agave.archive shadow file from the remote job directory and staging
     * everything not in there to the user-supplied Job.archivePath on the 
     * Job.archiveSystem
     * 
     * @param job
     * @throws SystemUnavailableException
     * @throws SystemUnknownException
     * @throws JobException
     * @throws JobDependencyException 
     */
    public void run() 
    throws SystemUnavailableException, SystemUnknownException, JobException, 
            ClosedByInterruptException, JobDependencyException, JobFinishedException
    {
        try 
        {
            setJobMonitor(JobMonitorFactory.getInstance(getJob(), worker));
            
            if (log.isDebugEnabled()) log.debug("Checking status of job " + job.getUuid());
            
            this.job = getJobMonitor().monitor();
        } 
        catch (ClosedByInterruptException | JobFinishedException e) {
            throw e;
        }
        catch (StaleObjectStateException | UnresolvableObjectException e) {
            throw e;
        }
        catch (SystemUnavailableException e) {
            throw e;
        } 
        catch (Throwable e) {
            throw new JobException("Failed to check status of job " + job.getUuid(), e);
        }
    }

    public synchronized JobMonitor getJobMonitor() {
        return jobMonitor;
    }

    public synchronized void setJobMonitor(JobMonitor jobMonitor) {
        this.jobMonitor = jobMonitor;
    }
}
