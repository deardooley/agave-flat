/**
 * 
 */
package org.iplantc.service.jobs.managers.launchers;

import org.apache.log4j.Logger;
import org.iplantc.service.apps.dao.SoftwareDao;
import org.iplantc.service.apps.model.Software;
import org.iplantc.service.jobs.exceptions.JobException;
import org.iplantc.service.jobs.exceptions.MissingSoftwareDependencyException;
import org.iplantc.service.jobs.exceptions.SoftwareUnavailableException;
import org.iplantc.service.jobs.managers.JobManager;
import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.jobs.phases.workers.IPhaseWorker;
import org.iplantc.service.systems.dao.SystemDao;
import org.iplantc.service.systems.exceptions.SystemUnavailableException;
import org.iplantc.service.systems.model.ExecutionSystem;
import org.iplantc.service.systems.model.enumerations.ExecutionType;
import org.iplantc.service.systems.model.enumerations.SystemStatusType;
import org.iplantc.service.transfer.RemoteDataClient;

/**
 * @author dooley
 * 
 */
public class JobLauncherFactory 
{
	private static final Logger log = Logger.getLogger(JobLauncherFactory.class);
	
	/**
	 * Returns an intance of a {@link JobLauncher} based on the parameters of the job.
	 * Prior to creating the {@link JobLaunch}, this method validates the availability 
	 * of the {@link Software} and {@link ExecutionSystem}.
	 * 
	 * @param job
	 * @return
	 * @throws JobException
	 * @throws SystemUnavailableException
	 * @throws SoftwareUnavailableException
	 */
	public static JobLauncher getInstance(Job job, IPhaseWorker worker) 
	 throws JobException, SystemUnavailableException, SoftwareUnavailableException
	{
	    Software software = SoftwareDao.getSoftwareByUniqueName(job.getSoftwareName());
		ExecutionSystem executionSystem = JobManager.getJobExecutionSystem(job);
		
		// if the system is unavailable or missing...
		if (!executionSystem.isAvailable()) {
		    throw new SystemUnavailableException("Job execution system " 
                    + executionSystem.getSystemId() + " is not currently available");
        } 
		// if the system is in down time or otherwise unavailable...
        else if (executionSystem.getStatus() != SystemStatusType.UP)
        {
            throw new SystemUnavailableException("Job execution system " 
                    + executionSystem.getSystemId() + " is currently " 
                    + executionSystem.getStatus());
        }
		// if the software app is missing...
        else if (software == null) 
		{ 
			throw new JobException(job.getSoftwareName()
				+ " is not a recognized application."); 
		}
		// if the software app is unavailable...
		else if (!software.isAvailable())
		{
			throw new SoftwareUnavailableException("Application is not available for execution"); 
		}
		// if the software deployment system is unavailable...
        else if (software.getStorageSystem() == null || !software.getStorageSystem().isAvailable()) 
		{
		    throw new SystemUnavailableException("Software deployment system " 
		            + software.getStorageSystem().getSystemId() + " is not currently available");
		} 
		// if the software deployment system is unavailable...
		else if (software.getStorageSystem().getStatus() != SystemStatusType.UP)
		{
		    throw new SystemUnavailableException("Software deployment system " 
                    + software.getStorageSystem().getSystemId() + " is currently " 
		            + software.getStorageSystem().getStatus());
		}
		// if the software assets are missing...
        else 
		{
			RemoteDataClient remoteDataClient = null;
			try {
				
				remoteDataClient = software.getStorageSystem().getRemoteDataClient();
				remoteDataClient.authenticate();
				
				if (software.isPubliclyAvailable())
				{	
					if (!remoteDataClient.doesExist(software.getDeploymentPath()))
					{
//					    // TODO: no point doing this until the underlying systems get more reliable
////					    if (Settings.DISABLE_MISSING_SOFTWARE) {
////    						software.setAvailable(false);
////    						SoftwareDao.persist(software);
////    						EmailMessage.send("Rion Dooley", 
////    								"dooley@tacc.utexas.edu", 
////    								"Public app " + software.getUniqueName() + " is missing.", 
////    								"While submitting a job, the Job Service noticed that the app bundle " +
////    								"of the public app " + software.getUniqueName() + " was missing. This " +
////    								"will impact provenance and could impact experiment reproducability. " +
////    								"Please restore the application zip bundle from archive and re-enable " + 
////    								"the application via the admin console.\n\n" +
////    								"Name: " + software.getUniqueName() + "\n" + 
////    								"User: " + job.getOwner() + "\n" +
////    								"Job: " + job.getUuid() + "\n" +
////    								"Time: " + job.getCreated().toString() + "\n\n");
////					    }
//						throw new MissingSoftwareDependencyException("Application executable is missing. Software is not available.");
					    throw new MissingSoftwareDependencyException();
					} 
				}
			    else if (!remoteDataClient.doesExist(software.getDeploymentPath() + '/' + software.getExecutablePath())) 
				{
					throw new MissingSoftwareDependencyException();
				}
			
			} catch (MissingSoftwareDependencyException e) {
			    throw new SoftwareUnavailableException(
			            "Unable to locate the application wrapper template at agave://" 
                        + software.getStorageSystem().getSystemId() + "/" 
                        + software.getDeploymentPath() + '/' + software.getExecutablePath() 
                        + ". Job cannot run until the template is restored.");
			} catch (Throwable e) {
				throw new JobException("Unable to verify the availability of the application executable. Software is not available.", e);
			} 
			finally {
				try { remoteDataClient.disconnect(); } catch(Exception e) {}
			}
		}

		// now submit the job to the target system using the correct launcher.
		if (software.getExecutionSystem().getExecutionType().equals(ExecutionType.HPC))
		{
			return new HPCLauncher(job, worker);
		}
		else if (software.getExecutionSystem().getExecutionType().equals(ExecutionType.CONDOR))
		{
			return new CondorLauncher(job, worker);
		}
		else
		{
			return new CLILauncher(job, worker);
		}
	}
}
