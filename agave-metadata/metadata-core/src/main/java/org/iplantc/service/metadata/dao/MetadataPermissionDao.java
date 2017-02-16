/**
 * 
 */
package org.iplantc.service.metadata.dao;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.hibernate.CacheMode;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.iplantc.service.common.Settings;
import org.iplantc.service.common.persistence.HibernateUtil;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.metadata.exceptions.MetadataException;
import org.iplantc.service.metadata.model.MetadataPermission;
import org.iplantc.service.metadata.model.enumerations.PermissionType;
import org.iplantc.service.metadata.util.ServiceUtils;

/**
 * DAO for metadata permissions.
 * 
 * @author dooley
 * 
 */
public class MetadataPermissionDao {

	protected static Session getSession() {
		HibernateUtil.beginTransaction();
		Session session = HibernateUtil.getSession();
		//session.clear();
		session.enableFilter("metadataPemTenantFilter").setParameter("tenantId", TenancyHelper.getCurrentTenantId());
		return session;
	}
	
	/**
	 * Returns all metadata permissions for the given oid.
	 * 
	 * @param uuid
	 * @return
	 * @throws MetadataException
	 */
	public static List<MetadataPermission> getByUuid(String uuid)
	throws MetadataException
	{
		return getByUuid(uuid, 0, -1);
	}
	
	/**
	 * Returns all metadata permissions for the given oid.
	 * 
	 * @param uuid
	 * @param offset
	 * @param limit
	 * @return
	 * @throws MetadataException
	 */
	@SuppressWarnings("unchecked")
	public static List<MetadataPermission> getByUuid(String uuid, int offset, int limit)
	throws MetadataException
	{

		if (!ServiceUtils.isValid(uuid))
			throw new MetadataException("Object id cannot be null");

		try
		{
			Session session = getSession();
			
			session.clear();
			
			String hql = "FROM MetadataPermission "
					+ "WHERE uuid = :uuid "
					+ "ORDER BY username ASC";
			Query query = session.createQuery(hql)
					.setCacheMode(CacheMode.IGNORE)
					.setCacheable(false)
					.setString("uuid", uuid);
			
			if (offset > 0) {
				query.setFirstResult(offset);
            }
            
            if (limit >= 0) {
            	query.setMaxResults(limit);
            }
            
            List<MetadataPermission> pems = query.list();
			
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
     * Returns the {@link MetadataPermission#getUuid()} of {@link MetadataItem} to which 
     * the user has read permission. Delegates to {@link #getUuidOfAllSharedMetataItemReadableByUser(String, int, int)}
     * 
     * @param username the user for whom to look up permission grants
     * @return list of uuid to which the user has been granted read access.
     * @throws MetadataException
     */
    public static List<String> getUuidOfAllSharedMetataItemReadableByUser(String username)
    throws MetadataException
    {
        return getUuidOfAllSharedMetataItemReadableByUser(username, 0, -1);
    }
	
	/**
	 * Returns the {@link MetadataPermission#getUuid()} of {@link MetadataItem} to which 
     * the user has read permission.
     * 
	 * @param username the user for whom to look up permission grants
	 * @return list of uuid to which the user has been granted read access.
	 * @throws MetadataException
	 */
	@SuppressWarnings("unchecked")
    public static List<String> getUuidOfAllSharedMetataItemReadableByUser(String username, int offset, int limit)
    throws MetadataException
    {

	    if (StringUtils.isEmpty(username)) {
            throw new MetadataException("Username cannot be null in permission lookup");
        }
	    
        try
        {
            Session session = getSession();
            
            String sql = "select distinct uuid from metadata_permissions "
            		+ "where tenant_id = :tenantid "
            		+ "		and (permission = :allpem or permission like '%READ%') "
            		+ "		and username in (:owner, :world, :public) "
            		+ "order by last_updated DESC";	
            Query query = session.createSQLQuery(sql)
                .setString("tenantid", TenancyHelper.getCurrentTenantId())
                .setString("owner", username)
                .setString("world", Settings.WORLD_USER_USERNAME)
                .setString("public", Settings.PUBLIC_USER_USERNAME)
            	.setString("allpem", PermissionType.ALL.name());
            
            if (offset > 0) {
            	query.setFirstResult(offset);
            }
            
            if (limit >= 0) {
            	query.setMaxResults(limit);
            }
            
            List<String> metadataUUIDGrantedToUser = query.list();
            
//            String sql = "select uuid from MetadataPermission "
//                    + "where username = :username and  "
//                    + "      (permission like :readable or "
//                    + "       permission like :ownership";
//            List<String> metadataUUIDGrantedToUser = session.createSQLQuery(sql)
//                    .setString("username", username)
//                    .setString("readable", "%" + PermissionType.READ.name() + "%")
//                    .setString("ownership", PermissionType.ALL.name())
//                    .list();
            
            session.flush();
            
            return metadataUUIDGrantedToUser;
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
	 * @param uuid
	 * @return
	 * @throws MetadataException
	 */
	@SuppressWarnings("unchecked")
	public static MetadataPermission getByUsernameAndUuid(String username, String uuid) throws MetadataException
	{
		if (!ServiceUtils.isValid(username))
			throw new MetadataException("Username cannot be null");

		try
		{
			Session session = getSession();
			
			String hql = "from MetadataPermission where uuid = :uuid and username = :username";
			List<MetadataPermission> pems = session.createQuery(hql)
					.setString("username", username)
					.setString("uuid", uuid)
					.setCacheMode(CacheMode.IGNORE)
					.setCacheable(false)
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
	 * @throws MetadataException
	 */
	public static void persist(MetadataPermission pem) throws MetadataException
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
			
			throw new MetadataException("Failed to save metadata permission.", ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}

	/**
	 * Deletes the given iod permission.
	 * 
	 * @param pem
	 * @throws MetadataException
	 */
	public static void delete(MetadataPermission pem) throws MetadataException
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
			
			throw new MetadataException("Failed to delete metadata permission.", ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}

	/**
	 * Deletes all permissions for the metadata with given oid
	 * 
	 * @param uuid
	 * @throws MetadataException
	 */
	public static void deleteByUuid(String uuid) throws MetadataException
	{
		if (uuid == null) {
			return;
		}

		try
		{
			Session session = getSession();

			String sql = "delete from MetadataPermission where uuid = :uuid";
			session.createQuery(sql)
					.setString("uuid", uuid)
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
			
			throw new MetadataException("Failed to delete metadata permission", ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}

}
