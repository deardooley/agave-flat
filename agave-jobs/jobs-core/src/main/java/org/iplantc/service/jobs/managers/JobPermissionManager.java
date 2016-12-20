package org.iplantc.service.jobs.managers;

import org.apache.commons.lang.StringUtils;
import org.iplantc.service.apps.exceptions.SoftwareException;
import org.iplantc.service.apps.util.ServiceUtils;
import org.iplantc.service.jobs.Settings;
import org.iplantc.service.jobs.dao.JobPermissionDao;
import org.iplantc.service.jobs.exceptions.JobException;
import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.jobs.model.JobEvent;
import org.iplantc.service.jobs.model.JobPermission;
import org.iplantc.service.jobs.model.enumerations.PermissionType;

public class JobPermissionManager {

	private Job	job;
	private String invokingUsername;

	public JobPermissionManager(Job job, String invokingUsername) throws JobException
	{
		if (job == null) { throw new JobException("Job cannot be null"); }
		this.job = job;
		this.invokingUsername = invokingUsername;
	}

	public boolean hasPermission(String username,
			PermissionType jobPermissionType) throws JobException
	{

		if (StringUtils.isEmpty(username) || StringUtils.equals(username, "null")) { return false; }

		if (job.getOwner().equals(username) || ServiceUtils.isAdmin(username))
			return true;

		for (JobPermission pem : JobPermissionDao.getByJobId(job.getId()))
		{
			if (pem.getUsername().equals(username) || 
					pem.getUsername().equals(Settings.WORLD_USER_USERNAME)) { 
				return pem.getPermission().equals(jobPermissionType); 
			}
		}

		return false;
	}

	public boolean canRead(String username) throws JobException
	{

		if (StringUtils.isEmpty(username) || StringUtils.equals(username, "null")) { return false; }

		if (job.getOwner().equals(username) || ServiceUtils.isAdmin(username))
			return true;

		for (JobPermission pem : JobPermissionDao.getByJobId(job.getId()))
		{
			if (pem.getUsername().equals(username) || 
					pem.getUsername().equals(Settings.WORLD_USER_USERNAME)) { 
				return pem.canRead(); 
			}
		}

		return false;
	}

	public boolean canWrite(String username) throws JobException
	{

		if (StringUtils.isEmpty(username) || StringUtils.equals(username, "null")) { return false; }

		if (job.getOwner().equals(username) || ServiceUtils.isAdmin(username))
			return true;

		for (JobPermission pem : JobPermissionDao.getByJobId(job.getId()))
		{
			if (pem.getUsername().equals(username) || 
					pem.getUsername().equals(Settings.WORLD_USER_USERNAME)) { 
				return pem.canWrite(); 
			}
		}

		return false;
	}

	public void setPermission(String username, String sPermission)
			throws JobException
	{
		if (StringUtils.isEmpty(username) || StringUtils.equals(username, "null")) { 
			throw new JobException("Invalid username"); }

		if (job.getOwner().equals(username))
			return;

		// if the permission is empty or null, delete it
		if (!ServiceUtils.isValid(sPermission) || sPermission.equalsIgnoreCase("none") 
				|| sPermission.equalsIgnoreCase("null"))
		{
			for (JobPermission pem : JobPermissionDao.getByJobId(job.getId()))
			{
				if (pem.getUsername().equals(username))
				{
					job.addEvent(new JobEvent(
							"PERMISSION_REVOKE", 
							"All permissions revoked for " + pem.getUsername(),
							invokingUsername));
					
					JobPermissionDao.delete(pem);
					return;
				}
			}
			return;
		}
		
		// or update the existing permission
		PermissionType permissionType = PermissionType
				.valueOf(sPermission.toUpperCase());

		for (JobPermission pem : JobPermissionDao.getByJobId(job.getId()))
		{
			if (pem.getUsername().equals(username))
			{
				job.addEvent(new JobEvent(
						"PERMISSION_GRANT", 
						pem.getPermission().name() + " permission granted to " + username,
						invokingUsername));
				
				pem.setPermission(permissionType);
				JobPermissionDao.persist(pem);
				return;
			}
		}
		
		// evidently they didn't have a permission, so add a new one
		JobPermission pem = new JobPermission(job, username, permissionType);
		JobPermissionDao.persist(pem);
		
		job.addEvent(new JobEvent(
				"PERMISSION_GRANT", 
				permissionType.name() + " permission granted to " + username,
				invokingUsername));
	}
	
	public void clearPermissions() throws JobException
	{
		if (job == null) {
			throw new SoftwareException("No job specified");
		}
		job.addEvent(new JobEvent("PERMISSION_REVOKE", "All permissions revoked", invokingUsername));
		try {
			JobPermissionDao.deleteByJobId(job.getId());
		}
		catch (JobException e) {
			throw new JobException("Failed to delete permissions for job " + job.getUuid(), e);
		}
	}
}
