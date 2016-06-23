/**
 * 
 */
package org.iplantc.service.metadata.dao;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.iplantc.service.common.persistence.HibernateUtil;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.metadata.exceptions.MetadataException;
import org.iplantc.service.metadata.model.MetadataSchemaPermission;
import org.iplantc.service.metadata.util.ServiceUtils;

import java.util.List;

/**
 * DAO class for metadata schemata
 * 
 * @author dooley
 * 
 */
public class MetadataSchemaPermissionDao {

	protected static Session getSession() {
		HibernateUtil.beginTransaction();
		Session session = HibernateUtil.getSession();
		//session.clear();
		session.enableFilter("schemaPemTenantFilter").setParameter("tenantId", TenancyHelper.getCurrentTenantId());
		return session;
	}
	
	/**
	 * Returns all metadata schema permissions for the given schemaId.
	 *
	 * @param schemaId
	 * @return
	 * @throws org.iplantc.service.metadata.exceptions.MetadataException
	 */
	@SuppressWarnings("unchecked")
	public static List<MetadataSchemaPermission> getBySchemaId(String schemaId)
	throws MetadataException
	{

		if (!ServiceUtils.isValid(schemaId))
			throw new MetadataException("Schema id cannot be null");

		try
		{
			Session session = getSession();

			String hql = "from MetadataSchemaPermission where schemaId = :schemaId order by username asc";
			List<MetadataSchemaPermission> pems = session.createQuery(hql).setString("schemaId", schemaId).list();

			session.flush();

			return pems;
		}
		catch (HibernateException ex)
		{
			throw new MetadataException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}

	/**
	 * Gets the metadata permissions for the specified username and iod
	 *
	 * @param username
	 * @param schemaId
	 * @return
	 * @throws org.iplantc.service.metadata.exceptions.MetadataException
	 */
	@SuppressWarnings("unchecked")
	public static MetadataSchemaPermission getByUsernameAndSchemaId(String username, String schemaId) throws MetadataException
	{
		if (!ServiceUtils.isValid(username))
			throw new MetadataException("Username cannot be null");

		try
		{
			Session session = getSession();

			String hql = "from MetadataSchemaPermission where schemaId = :schemaId and username = :username";
			List<MetadataSchemaPermission> pems = session.createQuery(hql)
					.setString("username", username)
					.setString("schemaId", schemaId)
					.list();

			session.flush();

			return pems.isEmpty() ? null : pems.get(0);
		}
		catch (HibernateException ex)
		{
			throw new MetadataException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}

	/**
	 * Saves a new metadata permission. Upates existing ones.
	 * @param pem
	 * @throws org.iplantc.service.metadata.exceptions.MetadataException
	 */
	public static void persist(MetadataSchemaPermission pem) throws MetadataException
	{
		if (pem == null)
			throw new MetadataException("Permission cannot be null");

		try
		{
			HibernateUtil.beginTransaction();
			Session session = HibernateUtil.getSession();
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

			throw new MetadataException("Failed to save metadata Schema permission.", ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}

	/**
	 * Deletes the given iod permission.
	 *
	 * @param pem
	 * @throws org.iplantc.service.metadata.exceptions.MetadataException
	 */
	public static void delete(MetadataSchemaPermission pem) throws MetadataException
	{
		if (pem == null)
			throw new MetadataException("Permission cannot be null");

		try
		{
			HibernateUtil.beginTransaction();
			Session session = HibernateUtil.getSession();
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

			throw new MetadataException("Failed to delete metadata schema permission.", ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}

	/**
	 * Deletes all permissions for the metadata schema with given schemaId
	 *
	 * @param schemaId
	 * @throws org.iplantc.service.metadata.exceptions.MetadataException
	 */
	public static void deleteBySchemaId(String schemaId) throws MetadataException
	{
		if (schemaId == null) {
			return;
		}

		try
		{
			Session session = getSession();

			String sql = "delete from MetadataSchemaPermission where schemaId = :schemaId";
			session.createQuery(sql)
					.setString("schemaId", schemaId)
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
			
			throw new MetadataException("Failed to delete metadata schema permission", ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}

}
