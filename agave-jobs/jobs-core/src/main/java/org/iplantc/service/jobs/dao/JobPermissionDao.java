/**
 * 
 */
package org.iplantc.service.jobs.dao;

import java.util.List;

import org.hibernate.CacheMode;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.iplantc.service.apps.util.ServiceUtils;
import org.iplantc.service.common.persistence.HibernateUtil;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.jobs.exceptions.JobException;
import org.iplantc.service.jobs.model.JobPermission;

/**
 * Model class for interacting with job permissions. Permissions are
 * not persisted as mapped entities in the Job class due to their
 * potentially large size and poor lazy mapping.
 * 
 * @author dooley
 * 
 */
public class JobPermissionDao
{
	protected static Session getSession() {
		HibernateUtil.beginTransaction();
		Session session = HibernateUtil.getSession();
		session.clear();
		session.enableFilter("jobPermissionTenantFilter").setParameter("tenantId", TenancyHelper.getCurrentTenantId());
		return session;
	}

	/**
	 * Returns all job permissions for the job with the given id.
	 * 
	 * @param jobId
	 * @return
	 * @throws JobException
	 */
	@SuppressWarnings("unchecked")
	public static List<JobPermission> getByJobId(Long jobId)
	throws JobException
	{

		if (!ServiceUtils.isValid(jobId))
			throw new JobException("Job id cannot be null");

		try
		{
			Session session = getSession();
			
			String hql = "from JobPermission where jobId = :jobid order by username asc";
			List<JobPermission> pems = session.createQuery(hql)
			        .setLong("jobid", jobId)
			        .setCacheable(false)
			        .setCacheMode(CacheMode.IGNORE)
			        .list();
			
			session.flush();
			
			return pems;
		}
		catch (HibernateException ex)
		{
			throw new JobException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}

	/**
	 * Gets the job permissions for the specified username and job
	 * 
	 * @param username
	 * @param jobId
	 * @return
	 * @throws JobException
	 */
	@SuppressWarnings("unchecked")
	public static JobPermission getByUsernameAndJobId(String username,
			long jobId) throws JobException
	{
		if (!ServiceUtils.isValid(username))
			throw new JobException("Username cannot be null");

		try
		{
			Session session = getSession();
			
			String hql = "from JobPermission where jobId = :jobid and username = :username";
			List<JobPermission> pems = session.createQuery(hql)
					.setString("username", username)
					.setLong("jobid", jobId)
					.list();
			
			session.flush();
			
			return pems.isEmpty() ? null : pems.get(0);
		}
		catch (HibernateException ex)
		{
			throw new JobException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}

	/**
	 * Saves a new job permission. Upates existing ones.
	 * @param pem
	 * @throws JobException
	 */
	public static void persist(JobPermission pem) throws JobException
	{
		if (pem == null)
			throw new JobException("Permission cannot be null");

		try
		{
			Session session = getSession();
			session.saveOrUpdate(pem);
			session.flush();
		}
		catch (HibernateException ex)
		{
			try
			{
				if (HibernateUtil.getSession().isOpen()) {
					HibernateUtil.rollbackTransaction();
				}
			}
			catch (Exception e) {}
			
			throw new JobException("Failed to save job.", ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}

	/**
	 * Deletes the give job permission.
	 * 
	 * @param pem
	 * @throws JobException
	 */
	public static void delete(JobPermission pem) throws JobException
	{
		if (pem == null)
			throw new JobException("Permission cannot be null");

		try
		{
			Session session = getSession();
			session.delete(pem);
			session.flush();
		}
		catch (HibernateException ex)
		{
			try
			{
				if (HibernateUtil.getSession().isOpen()) {
					HibernateUtil.rollbackTransaction();
				}
			}
			catch (Exception e) {}
			
			throw new JobException("Failed to delete job.", ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}

	/**
	 * Deletes all permissions for the job with given id
	 * 
	 * @param jobId
	 * @throws JobException
	 */
	public static void deleteByJobId(Long jobId) throws JobException
	{
		if (jobId == null) {
			return;
		}

		try
		{
			Session session = getSession();

			String hql = "delete from JobPermission where jobId = :jobid";
			session.createQuery(hql)
					.setLong("jobid", jobId)
					.executeUpdate();
			
			session.flush();
		}
		catch (HibernateException ex)
		{
			try
			{
				if (HibernateUtil.getSession().isOpen()) {
					HibernateUtil.rollbackTransaction();
				}
			}
			catch (Exception e) {}
			
			throw new JobException("Failed to delete permissions for job " + jobId, ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}

}
