/**
 * 
 */
package org.iplantc.service.transfer.dao;

import java.math.BigInteger;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.iplantc.service.common.exceptions.PermissionException;
import org.iplantc.service.common.persistence.HibernateUtil;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.transfer.exceptions.RemoteDataException;
import org.iplantc.service.transfer.model.RemoteFilePermission;
import org.iplantc.service.transfer.util.ServiceUtils;

/**
 * Model class for interacting with logical file permissions. Permissions are
 * not persisted as mapped entities in the LogicalFile class due to their
 * potentially large size and poor lazy mapping.
 * 
 * @author dooley
 * 
 */
public class RemoteFilePermissionDao
{
	
	protected static Session getSession() {
		HibernateUtil.beginTransaction();
		Session session = HibernateUtil.getSession();
		session.clear();
		session.enableFilter("remoteFilePermissionTenantFilter").setParameter("tenantId", TenancyHelper.getCurrentTenantId());
		return session;
	}
	
	/**
	 * Returns all logical file permissions for the logical file with the given id.
	 * 
	 * @param logicalFileId
	 * @return
	 * @throws RemoteDataException
	 */
	@SuppressWarnings("unchecked")
	public static List<RemoteFilePermission> getBylogicalFileId(Long logicalFileId)
	throws PermissionException
	{
		if (!ServiceUtils.isValid(logicalFileId))
			throw new PermissionException("LogicalFile id cannot be null");

		try
		{
			Session session = getSession();

			String hql = "from RemoteFilePermission where logicalFileId = :logicalFileId order by username asc";
			List<RemoteFilePermission> pems = session.createQuery(hql)
					.setLong("logicalFileId", logicalFileId)
					.list();
			
			session.flush();
			
			return pems;
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
			
			throw new PermissionException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}

	/**
	 * Gets the logical file permissions for the specified username and logical file
	 * 
	 * @param username
	 * @param logicalFileId
	 * @return
	 * @throws RemoteDataException
	 */
	@SuppressWarnings("unchecked")
	public static RemoteFilePermission getByUsernameAndlogicalFileId(String username, long logicalFileId) 
	throws PermissionException
	{
		if (!ServiceUtils.isValid(username))
			throw new PermissionException("Username cannot be null");

		try
		{
			Session session = getSession();

			String hql = "from RemoteFilePermission where logicalFileId = :logicalFileId and username = :username";
			List<RemoteFilePermission> pems = session.createQuery(hql)
					.setString("username", username)
					.setLong("logicalFileId", logicalFileId)
					.list();
			
			session.flush();
			
			return pems.isEmpty() ? null : pems.get(0);
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
			
			throw new PermissionException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}
	
	/**
	 * Deletes all logical file permissions for the specified username and logical file
	 * 
	 * @param username
	 * @param logicalFileId
	 * @return
	 * @throws RemoteDataException
	 */
	public static void deleteByUsernameAndlogicalFileId(String username, long logicalFileId) 
	throws PermissionException
	{
		if (!ServiceUtils.isValid(username))
			throw new PermissionException("Username cannot be null");

		try
		{
			Session session = getSession();

			String hql = "DELETE FROM `remotefilepermissions` "
					+ "WHERE logical_file_id = :logicalfileid AND "
					+ "		username = :username AND"
					+ "		tenant_id = :tenantid";
			
			session.createSQLQuery(hql)
					.setString("username", username)
					.setLong("logicalfileid", logicalFileId)
					.setString("tenantid", TenancyHelper.getCurrentTenantId())
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
			
			throw new PermissionException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}
	
	/**
	 * Bulk deletes all logical file permissions for the specified username and 
	 * list of logical file ids
	 * 
	 * @param username
	 * @param logicalFileId
	 * @return
	 * @throws RemoteDataException
	 */
	public static void bulkDeleteByUsernameAndlogicalFileId(String username, List<BigInteger> logicalFileIds) 
	throws PermissionException
	{
		if (!ServiceUtils.isValid(username)) {
			throw new PermissionException("Username cannot be null");
		}
		
		if (logicalFileIds == null || logicalFileIds.isEmpty()) {
			return;
		}
		
		try
		{
			Session session = getSession();

			String hql = "DELETE FROM `remotefilepermissions` \n"
					+ "WHERE logical_file_id IN ( '"  
					+ StringUtils.join(logicalFileIds,"', '") 
					+ "' ) AND \n"
					+ "		username = :username AND \n"
					+ "		tenant_id = :tenantid";
			
			
			session.createSQLQuery(hql)
					.setString("username", username)

					.setString("tenantid", TenancyHelper.getCurrentTenantId())
					.executeUpdate();
			System.out.println(hql.replace(":username", username).replace(":tenantid", TenancyHelper.getCurrentTenantId()));
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
			
			throw new PermissionException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}

	/**
	 * Saves a new logical file permission. Upates existing ones.
	 * @param pem
	 * @throws RemoteDataException
	 */
	public static void persist(RemoteFilePermission pem) throws PermissionException
	{
		if (pem == null)
			throw new PermissionException("Permission cannot be null");

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
			
			throw new PermissionException("Failed to save file permission", ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}

	/**
	 * Deletes the give logical file permission.
	 * 
	 * @param pem
	 * @throws RemoteDataException
	 */
	public static void delete(RemoteFilePermission pem) throws PermissionException
	{
		if (pem == null) return;

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
			
			throw new PermissionException("Failed to delete file permission", ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}

	/**
	 * Deletes all permissions for the logical file with given id
	 * 
	 * @param logicalFileId
	 * @throws RemoteDataException
	 */
	public static void deleteBylogicalFileId(Long logicalFileId) throws PermissionException
	{
		if (logicalFileId == null) return;

		try
		{
			Session session = getSession();

			String hql = "delete RemoteFilePermission where logicalFileId = :logicalFileId";
			session.createQuery(hql)
					.setLong("logicalFileId", logicalFileId)
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
			
			throw new PermissionException("Failed to delete file permission", ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}

}
