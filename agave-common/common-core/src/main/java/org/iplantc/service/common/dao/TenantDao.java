/**
 * 
 */
package org.iplantc.service.common.dao;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.hibernate.CacheMode;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.iplantc.service.common.exceptions.TenantException;
import org.iplantc.service.common.model.Tenant;
import org.iplantc.service.common.persistence.HibernateUtil;

/**
 * @author dooley
 *
 */
public class TenantDao extends AbstractDao
{
    // Tracing.
    private static final Logger _log = Logger.getLogger(TenantDao.class);

	private static ConcurrentHashMap<String, Tenant> tenantCache = new ConcurrentHashMap<String, Tenant>();
	
	protected Session getSession() {
		HibernateUtil.beginTransaction();
		Session session = HibernateUtil.getSession();
		return session;
	}
	
	/**
	 * Save or update a tenant
	 * 
	 * @param tenant
	 * @throws TenantException 
	 */
	public void persist(Tenant tenant) throws TenantException
	{
		if (tenant == null)
			throw new TenantException("Tenant cannot be null");

		try
		{
			Session session = getSession();
			
			session.saveOrUpdate(tenant);
			session.flush();
		}
		catch (HibernateException ex)
		{
			try
			{
				if (HibernateUtil.getSession().isOpen())
				{
					HibernateUtil.rollbackTransaction();
					HibernateUtil.getSession().close();
				}
			}
			catch (Exception e)
			{
			}
			throw new TenantException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction();} catch (Exception e) {}
		}
	}

	/**
	 * Deletes a tenant from the db.
	 * @param tenant
	 * @throws TenantException
	 */
	public void delete(Tenant tenant) throws TenantException
	{
		if (tenant == null)
			throw new TenantException("Job cannot be null");

		try
		{
			Session session = getSession();
			session.clear();
			session.delete(tenant);
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
			catch (Throwable e) {}
				
			throw new TenantException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction();} catch (Exception e) {}
		}
	}
	
	/**
	 * Retrieves a tenant by its unique tenantId
	 * @param tenantId
	 * @return
	 */
	public Tenant findByTenantId(String tenantId) throws TenantException
	{
		if (!tenantCache.containsKey(tenantId)) {
			try
			{
				Session session = getSession();
				session.clear();
				Tenant tenant = (Tenant) session.createQuery("from Tenant t where t.tenantCode = :tenantCode")
						.setCacheable(true)
						.setCacheMode(CacheMode.NORMAL)
						.setString("tenantCode",  tenantId)
						.uniqueResult();
				
				session.flush();
				
				if (tenant != null) {
					tenantCache.put(tenantId, tenant);
				}
			}
			catch (HibernateException ex)
			{
				throw new TenantException(ex);
			}
			finally {
				try { HibernateUtil.commitTransaction();} catch (Exception e) {}
			}
		}
		
		return tenantCache.get(tenantId);
	}

	/**
	 * Checks for the existence of a tenant with the given {@link Tenant#getTenantCode()}
	 * @param tenantId
	 * @return true if a tenant with the given tenantCode exists. False otherwise
	 * @throws TenantException
	 */
	public boolean exists(String tenantCode) throws TenantException
	{
		try
		{
			Session session = getSession();
			session.clear();
			Long matches = ((Long) session.createQuery("select count(id) from Tenant t where t.tenantCode = :tenantCode")
					.setString("tenantCode",  tenantCode)
					.uniqueResult()).longValue();
			
			session.flush();
			
			return matches > 0;
		}
		catch (HibernateException ex)
		{
			throw new TenantException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction();} catch (Exception e) {}
		}
	}

	/**
	 * Looks up a tenant by the {@link Tenant#getBaseUrl()} hostname.
	 * @param hostname
	 * @return
	 * @throws TenantException
	 */
	public Tenant findByBaseUrl(String hostname) 
	throws TenantException
	{
		try
		{
			Session session = getSession();
			session.clear();
			Tenant tenant = (Tenant) session.createQuery("from Tenant t where t.baseUrl like :httpUrl or t.baseUrl like :sslUrl")
					.setString("httpUrl",  "http://" + hostname + "%")
					.setString("sslUrl",  "https://" + hostname + "%")
					.setMaxResults(1)
					.uniqueResult();
			
			session.flush();
			
			return tenant;
		}
		catch (HibernateException ex)
		{
			throw new TenantException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction();} catch (Exception e) {}
		}
	}
	
	/** Return a list of tenant ids.  Set the activeOnly flag to true to
	 * avoid retrieving ids of disabled tenants. 
	 * 
	 * @param activeOnly true returns tenant with active status, 
	 *                   false returns all tenants including disabled ones
	 * @return list of tenant ids
	 */
	public List<String> getTenantIds(boolean activeOnly)
	throws TenantException
	{
	    // Result list.
	    List<String> idList;
	    
	    // Create the where clause depending on input.
	    String whereClause = "";
	    if (activeOnly)
	        whereClause = "where status != 'DISABLED'";
	    
        try
        {
            Session session = getSession();
            session.clear();
            
            // Create the query.
            String sql = "select tenant_id from tenants " + whereClause +
                         " order by tenant_id";
            
            // Issue the query and commit.
            idList = session.createSQLQuery(sql).list();
            HibernateUtil.commitTransaction();
        }
        catch (HibernateException ex)
        {
            // Log the error.
            String msg = "Unable to retrieve tenant IDs.";
            _log.error(msg, ex);
            
            // Rollback if possible before throwing exception.
            try {HibernateUtil.rollbackTransaction();} catch (Exception e){}
            throw new TenantException(msg, ex);
        }
	    
	    return idList;
	}
}
