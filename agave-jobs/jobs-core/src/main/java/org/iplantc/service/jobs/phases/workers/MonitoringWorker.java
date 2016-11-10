package org.iplantc.service.jobs.phases.workers;

import java.nio.channels.ClosedByInterruptException;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.StaleObjectStateException;
import org.hibernate.UnresolvableObjectException;
import org.iplantc.service.common.persistence.HibernateUtil;
import org.iplantc.service.jobs.Settings;
import org.iplantc.service.jobs.exceptions.JobException;
import org.iplantc.service.jobs.exceptions.JobWorkerException;
import org.iplantc.service.jobs.managers.JobManager;
import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.jobs.model.enumerations.JobStatusType;
import org.iplantc.service.jobs.queue.actions.MonitoringAction;
import org.iplantc.service.systems.exceptions.SystemUnavailableException;
import org.iplantc.service.systems.model.enumerations.StorageProtocolType;
import org.joda.time.DateTime;

/**
 * @author rcardone
 *
 */
public final class MonitoringWorker 
 extends AbstractPhaseWorker
{
    /* ********************************************************************** */
    /*                               Constants                                */
    /* ********************************************************************** */
    // Tracing.
    private static final Logger _log = Logger.getLogger(MonitoringWorker.class);
    
    /* ********************************************************************** */
    /*                              Constructors                              */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* constructor:                                                           */
    /* ---------------------------------------------------------------------- */
    public MonitoringWorker(PhaseWorkerParms parms) 
    {
        super(parms);
    }
    
    /* ********************************************************************** */
    /*                           Protected Methods                            */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* processJob:                                                            */
    /* ---------------------------------------------------------------------- */
    @Override
    protected void processJob(Job job) throws JobWorkerException
    {
        // Assign job field for the duration of this method
        // to maintain compatibility with legacy code.
        _job = job;

        // Exceptions thrown by any of the called methods abort processing.
        // This structure maintains compatibility with legacy code.
        try {
            // ----- Check date.
            checkExpiration();
            
            // ----- Check storage locality
            checkSoftwareLocalityUsingJobManager();
            
            // ----- Monitor the running job
            monitor();
        }
        catch (Exception e) {
            // All logging and state changes have been handled
            // by the called routine that threw the exception.
            // This thread lives on.
        }
        finally {
            
            // TODO: Check whether disconnect is a good idea.
            // Hibernate magic...
            try { HibernateUtil.flush(); } catch (Exception e) {}
            try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
            try { HibernateUtil.disconnectSession(); } catch (Exception e) {} 
            
            // Remove dangling references to job-specific data.
            reset();
        }
    }
    
    /* ---------------------------------------------------------------------- */
    /* checkExpiration:                                                       */
    /* ---------------------------------------------------------------------- */
    private void checkExpiration() throws JobWorkerException, JobException
    {
      // kill jobs past their max lifetime
      if (_job.getEndTime() != null) {
          
          _job = JobManager.updateStatus(_job, JobStatusType.FINISHED, 
                  "Setting job " + _job.getUuid() + 
                  " status to FINISHED due to previous completion event.");
          
          String msg = "Skipping watch on job " + _job.getUuid() + 
                       " due to previous completion. " +
                       "Setting job status to FINISHED due to previous completion event.";
          if (_log.isDebugEnabled()) _log.debug(msg);
          
          // Signal to abort monitoring.
          throw new JobWorkerException(msg);
      } 
      else if (_job.calculateExpirationDate().before(new DateTime().toDate())) 
      {
          if (_log.isDebugEnabled()) _log.debug("Terminating job " + _job.getUuid() + 
                  " after for not completing prior to the expiration date " + 
                  new DateTime(_job.calculateExpirationDate()).toString());
          _job = JobManager.updateStatus(_job, JobStatusType.KILLED, 
                  "Terminating job " + _job.getUuid() + 
                  " after for not completing prior to the expiration date " + 
                  new DateTime(_job.calculateExpirationDate()).toString());
          _job = JobManager.updateStatus(_job, JobStatusType.FAILED, 
                  "Job " + _job.getUuid() + " did not complete by " + 
                  new DateTime(_job.calculateExpirationDate()).toString() +
                  ". Job cancelled.");
          
          // Signal to abort monitoring.
          throw new JobWorkerException("Job expired");
      } 
    }
    
    /* ---------------------------------------------------------------------- */
    /* checkSoftwareLocalityUsingJobManager:                                  */
    /* ---------------------------------------------------------------------- */
    private void checkSoftwareLocalityUsingJobManager() 
      throws SystemUnavailableException, JobWorkerException
    {
        // TODO: See if what the preferred version of this check is (do we need 2 versions?)
        // if the execution system for this job has a local storage config,
        // all other transfer workers will pass on it.
        if (!StringUtils.equals(Settings.LOCAL_SYSTEM_ID, _job.getSystem()) &&
            JobManager.getJobExecutionSystem(_job).getStorageConfig().getProtocol().equals(StorageProtocolType.LOCAL))
        {
            // This is not really an error, but we need to throw some exception
            // to signal that this phase's processing should end for this job.
            String msg = "Job " + _job.getName() + " (" + _job.getUuid() +
                         ") failed the software locality check.";
            throw new JobWorkerException(msg);
        }
    }
    
    /* ---------------------------------------------------------------------- */
    /* monitor:                                                               */
    /* ---------------------------------------------------------------------- */
    private void monitor() throws JobWorkerException 
    {
        try {
            _job = JobManager.updateStatus(_job,  _job.getStatus(), _job.getErrorMessage());
        
            setWorkerAction(new MonitoringAction(_job));
        
            try {
                // Wrap this in a try/catch so we can update the local reference to the job.
                getWorkerAction().run();
            }
            finally {_job = getWorkerAction().getJob();}
        }
        catch (ClosedByInterruptException e) {
            String msg = "Monitoring task for job " + _job.getUuid() + " aborted due to interrupt by worker process.";
            _log.debug(msg);
            throw new JobWorkerException(msg, e);
        }
        catch (StaleObjectStateException | UnresolvableObjectException e) {
            String msg = "Job " + _job.getUuid() + " already being processed by another thread. Ignoring.";
            _log.error(msg, e);
            throw new JobWorkerException(msg, e);
        }
        catch (SystemUnavailableException e) {
            String msg = "Monitoring task for job " + _job.getUuid() 
                    + ". Execution system " + _job.getSystem() + " is currently unavailable. ";
            _log.debug(msg);
            try {_job = JobManager.updateStatus(_job,  _job.getStatus(), msg);}
            catch (JobException e1) {
                _log.error("Monitoring task failed to update job " + _job.getUuid() + " timestamp", e1);
            }
            throw new JobWorkerException(e);
        }
        catch (HibernateException e) {
            String msg = "Failed to retrieve job information from db";
            _log.error(msg, e);
            throw new JobWorkerException(msg, e);
        }
        catch (Throwable e) {
            String msg = "Monitoring task for job " + _job.getUuid() + " failed.";
            _log.error(msg, e);
            throw new JobWorkerException(msg, e);
        }
    }
}
