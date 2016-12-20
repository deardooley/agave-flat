/**
 *
 */
package org.iplantc.service.jobs.util;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.iplantc.service.apps.dao.SoftwareDao;
import org.iplantc.service.apps.model.Software;
import org.iplantc.service.apps.model.enumerations.ExecutionType;
import org.iplantc.service.apps.util.ServiceUtils;
import org.iplantc.service.common.uri.UrlPathEscaper;
import org.iplantc.service.jobs.dao.JobDao;
import org.iplantc.service.jobs.exceptions.JobException;
import org.iplantc.service.jobs.exceptions.MissingDataException;
import org.iplantc.service.jobs.managers.JobManager;
import org.iplantc.service.jobs.model.FileBean;
import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.jobs.model.JobUpdateParameters;
import org.iplantc.service.systems.exceptions.SystemUnavailableException;
import org.iplantc.service.systems.model.RemoteSystem;
import org.iplantc.service.systems.model.enumerations.SystemStatusType;
import org.iplantc.service.transfer.RemoteDataClient;
import org.iplantc.service.transfer.RemoteFileInfo;
import org.iplantc.service.transfer.Settings;
import org.iplantc.service.transfer.exceptions.RemoteDataException;

/**
 * This class resolves the location of job data at any given time by examining job 
 * history, status, and the existence of data at the expected location.
 * 
 * @author dooley
 *
 */
public class DataLocator {

	private Job					job;

	/**
	 *
	 */
	public DataLocator(Job job)
	{
		this.job = job;
	}

	/**
	 * Returns the {@link RemoteSystem} capable of accessing the job output data.
     * Depending on the job status, the returned system may change over time.
     *
	 * @return
	 * @throws RemoteDataException
	 * @throws SystemUnavailableException
	 */
	public RemoteSystem findOutputSystemForJobData() throws RemoteDataException, SystemUnavailableException
	{
	    if (!JobManager.isJobDataFullyArchived(job))
		{
		    // get files from remote system...if they have job access, they should have system access
		    return JobManager.getJobExecutionSystem(job);
		}
		else
		{
			return job.getArchiveSystem();
		}
	}

	/**
	 * Returns a directory listing of {@link RemoteFileInfo} objects from the {@code path}
	 * relative to the job output directory.
	 *
	 * @param path
	 * @return
	 * @throws RemoteDataException
	 * @throws MissingDataException
	 * @throws JobException
	 * @throws SystemUnavailableException
	 */
	public List<FileBean> listOutputDirectory(String path) throws RemoteDataException,
			MissingDataException, JobException, SystemUnavailableException
	{
		RemoteDataClient remoteExecutionSystemDataClient = null;
		try
		{
			if (!JobManager.isJobDataFullyArchived(job))
			{
			    // get files from remote system
				Software software = SoftwareDao.getSoftwareByUniqueName(job.getSoftwareName());

				if (software == null) {
				    throw new JobException(
						"Failed to find the software record for this job");
				}

				if (software.getExecutionType().name().equals(ExecutionType.HPC.name()) ||
						software.getExecutionType().name().equals(ExecutionType.CLI.name()))
				{
				    RemoteSystem executionSystem = null;
					try
					{
						executionSystem = JobManager.getJobExecutionSystem(job);

						remoteExecutionSystemDataClient = executionSystem.getRemoteDataClient(job.getInternalUsername());

						if (!executionSystem.isAvailable()) {
			                throw new SystemUnavailableException("Job output system "
			                        + job.getSystem() + " is not currently available");
			            }
			            // if the system is in down time or otherwise unavailable...
			            else if (executionSystem.getStatus() != SystemStatusType.UP)
			            {
			                throw new SystemUnavailableException("Job output system "
			                        + executionSystem.getSystemId() + " is currently "
			                        + executionSystem.getStatus());
			            }

						// probably didn't have any inputs associated with it
						if (!ServiceUtils.isValid(job.getWorkPath()))
						{
							String remoteWorkPath = null;

							if (!StringUtils.isEmpty(software.getExecutionSystem().getScratchDir())) {
								remoteWorkPath = software.getExecutionSystem().getScratchDir();
							} else if (!StringUtils.isEmpty(software.getExecutionSystem().getWorkDir())) {
								remoteWorkPath = software.getExecutionSystem().getWorkDir();
							}

							if (!StringUtils.isEmpty(remoteWorkPath)) {
								if (!remoteWorkPath.endsWith("/")) remoteWorkPath += "/";
							} else {
								remoteWorkPath = "";
							}

							remoteWorkPath += job.getOwner() +
									"/job-" + job.getUuid() + "-" + Slug.toSlug(job.getName());

							job.setWorkPath(remoteWorkPath);
							JobUpdateParameters jobUpdateParameters = new JobUpdateParameters();
							jobUpdateParameters.setWorkPath(job.getWorkPath());
							JobDao.update(job.getUuid(), job.getTenantId(), jobUpdateParameters);
							JobDao.refresh(job);
						}

						remoteExecutionSystemDataClient.authenticate();

						if (remoteExecutionSystemDataClient.doesExist(job.getWorkPath() + path))
						{
							RemoteFileInfo fileInfo = remoteExecutionSystemDataClient.getFileInfo(job.getWorkPath() + path);

							if (fileInfo.isDirectory()) {
								return parseRemoteListing(remoteExecutionSystemDataClient.ls(job.getWorkPath() + path), path, executionSystem.getSystemId());
							} else {
							    fileInfo.setName(FilenameUtils.getName(fileInfo.getName()));
                                List<RemoteFileInfo> remoteFileList = new ArrayList<RemoteFileInfo>();
								remoteFileList.add(fileInfo);
								return parseRemoteListing(remoteFileList, FilenameUtils.getPath(path), executionSystem.getSystemId());
							}
						} else {
							throw new RemoteDataException(
								"Unable to locate job data. Work folder no longer exists.");
						}
					}
					catch (SystemUnavailableException e) {
					    throw e;
					}
					catch (RemoteDataException e)
					{
						throw e;
					}
					catch (Exception e)
					{
						throw new RemoteDataException(
								"Failed to list output folder " + job.getWorkPath() + path
										+ " for job " + job.getUuid(), e);
					}
				}
				else if (software.getExecutionType().name().equals(ExecutionType.ATMOSPHERE.name()))
				{ // job is an atmo job
					throw new RemoteDataException(
							"Cannot retrieve data from running Atmosphere jobs");
				}
				else if (software.getExecutionType().name().equals(ExecutionType.CONDOR.name()))
				{
					if (job.isFinished() && !job.isArchiveOutput()) {
						throw new RemoteDataException(
							"Job was not archived and data cannot be retrieved from condor systems.");
					} else {
						throw new RemoteDataException(
							"Data cannot be retrieved from condor systems.");
					}
				}
				else
				{
					throw new RemoteDataException("Unknown execution system type.");
				}

			}
			else if (job.isArchiveOutput())
			{
				// look in the user's archive
				RemoteSystem system = null;
				try
				{
					// no permission checks for the user on the system because those
					// happened at submit time. Once the job runs, their data is
					// their data and should be accessible after the fact. This may,
					// however change in the future.
					system = job.getArchiveSystem();

					if (system == null) {
						throw new SystemUnavailableException("The archive system for this job " +
								"is not longer available.");
					}
					else if (!system.isAvailable()) {
                        throw new SystemUnavailableException("Job archive system "
                                + job.getSystem() + " is not currently available");
                    }
                    // if the system is in down time or otherwise unavailable...
                    else if (system.getStatus() != SystemStatusType.UP)
                    {
                        throw new SystemUnavailableException("Job archive system "
                                + system.getSystemId() + " is currently "
                                + system.getStatus());
                    }
					else
					{
						remoteExecutionSystemDataClient = system.getRemoteDataClient(job.getInternalUsername());

						if (ServiceUtils.isValid(job.getArchivePath()))
						{
							remoteExecutionSystemDataClient.authenticate();

							if (remoteExecutionSystemDataClient.doesExist(job.getArchivePath() + path))
							{
								RemoteFileInfo fileInfo = remoteExecutionSystemDataClient.getFileInfo(job.getArchivePath() + path);

								if (fileInfo.isDirectory()) {
									return parseRemoteListing(remoteExecutionSystemDataClient.ls(job.getArchivePath() + path), path, system.getSystemId());
								} else {
								    fileInfo.setName(FilenameUtils.getName(fileInfo.getName()));
									List<RemoteFileInfo> remoteFileList = new ArrayList<RemoteFileInfo>();
									remoteFileList.add(fileInfo);
									return parseRemoteListing(remoteFileList, FilenameUtils.getPath(path), system.getSystemId());
								}
							} else {
								throw new RemoteDataException(
									"Unable to locate job data. Work folder no longer exists.");
							}
						}
						else
						{
							throw new RemoteDataException("No work directory specified for this job. Usually this occurs when a job failed during submission.");
						}
					}
				}
				catch (SystemUnavailableException e) {
				    throw e;
				}
				catch (Exception e)
				{
					throw new RemoteDataException("Failed to list output folder "
							+ job.getWorkPath() + " for job " + job.getUuid(), e);
				}
			}
			else
			{
				throw new MissingDataException(
						"Unable to locate job data because the job was not archived.");
			}
		} finally {
			try { remoteExecutionSystemDataClient.disconnect(); } catch (Exception e) {}
		}

	}

	/**
	 * Parses response from listing into a response identical to that returned by the Files API.
	 *
	 * @param listing
	 * @param path
	 * @param systemId
	 * @return
	 */
	private List<FileBean> parseRemoteListing(List<RemoteFileInfo> listing, String path, String systemId)
	{
		List<FileBean> beans = new ArrayList<FileBean>();
		
		// adjust path to strip end slash, but not prefix
//		path = StringUtils.length(path) > 1 ? StringUtils.removeEnd(path, "/") : path;
		
		for (RemoteFileInfo entry : listing)
		{
			FileBean bean = new FileBean();
			bean.setName(entry.getName());
			bean.setPath((path + "/" + entry.getName()).replaceAll("/+", "/"));
			bean.setOwner(entry.getOwner());
			try
			{
				bean.setLastModified(entry.getLastModified());
			}
			catch (Exception e)
			{
				bean.setLastModified(null);
			}

			bean.setLength(entry.getSize());
			bean.setDirectory(entry.isDirectory());
			bean.setReadable(true);
			bean.setWritable(entry.userCanWrite());
			bean.setSystemId(systemId);
			bean.setJobId(job.getUuid());
			bean.setUrl(Settings.IPLANT_JOB_SERVICE + job.getUuid() + "/outputs/media" + UrlPathEscaper.escape(bean.getPath()));
			bean.setParent(path);

			beans.add(bean);
		}

		return beans;
	}
}
