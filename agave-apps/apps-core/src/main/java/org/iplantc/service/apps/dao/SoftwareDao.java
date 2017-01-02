package org.iplantc.service.apps.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.hibernate.transform.DistinctRootEntityResultTransformer;
import org.hibernate.transform.Transformers;
import org.iplantc.service.apps.Settings;
import org.iplantc.service.apps.exceptions.SoftwareException;
import org.iplantc.service.apps.model.Software;
import org.iplantc.service.apps.util.ServiceUtils;
import org.iplantc.service.common.auth.AuthorizationHelper;
import org.iplantc.service.common.persistence.HibernateUtil;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.common.search.SearchTerm;
import org.iplantc.service.transfer.model.enumerations.PermissionType;
import org.joda.time.DateTime;


public class SoftwareDao
{
	private static final Logger	log	= Logger.getLogger(SoftwareDao.class.getName());

	protected static Session getSession() {
		HibernateUtil.beginTransaction();
		Session session = HibernateUtil.getSession();
		//session.clear();
		session.enableFilter("softwareTenantFilter").setParameter("tenantId", TenancyHelper.getCurrentTenantId());
		return session;
	}

	@SuppressWarnings({ "unchecked" })
	public static Software getSoftwareByUniqueName(String uniqueName)
	{
		if (StringUtils.isEmpty(uniqueName)) {
			return null;
		} else {
			uniqueName = uniqueName.toLowerCase();
		}

		try
		{
			String where = null;

			if (uniqueName.matches(".*u\\d+")) {
				where = "CONCAT(lower({alias}.name), '-', lower({alias}.version), 'u', {alias}.revision_count) = lower(?)";
			} else {
				where = "CONCAT(lower({alias}.name), '-', lower({alias}.version)) = lower(?)";
			}

			Session session = getSession();

			List<Software> apps = (List<Software>) session.createCriteria(Software.class)
					.add(Restrictions.sqlRestriction(where, uniqueName.toLowerCase(), org.hibernate.type.StandardBasicTypes.STRING ))
					.list();

			Software software = null;
			if (!apps.isEmpty()) {
				for(Software sw: apps) {
					if (sw.getUniqueName().equalsIgnoreCase(uniqueName)) {
						software = sw;
						break;
					}
				}
			}

			session.flush();

			return software;
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

			throw new SoftwareException(ex);
		}
		finally
		{
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}

	public static Software get(Long softwareId)
	{
		try
		{
			HibernateUtil.beginTransaction();
			Session session = HibernateUtil.getSession();
			session.clear();
			Software software = (Software) session.get(Software.class, softwareId);
//			session.flush();
			return software;
		}
		catch (HibernateException ex)
		{
			try
			{
				if (HibernateUtil.getSession().isOpen())
				{
					HibernateUtil.rollbackTransaction();
				}
			}
			catch (Exception e)
			{
			}
			throw new SoftwareException(ex);
		}
		finally
		{
//			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}

	public static Software getSoftwareByNameAndVersion(String softwareName, String version)
	{
		String hql = "from Software where name = :name and version = :version";

		try
		{
			Session session = getSession();

			Software app = (Software) session.createQuery(hql)
					.setString("name", softwareName)
					.setString("version", version)
					.setMaxResults(1)
					.uniqueResult();

			session.flush();

			return app;
		}
		catch (HibernateException ex)
		{
			try
			{
				if (HibernateUtil.getSession().isOpen())
				{
					HibernateUtil.rollbackTransaction();
				}
			}
			catch (Exception e)
			{
			}
			throw new SoftwareException(ex);
		}
		finally
		{
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}

	public static Software get(String softwareName)
	{
		List<Software> apps = getAllByAttribute("name", softwareName, false);

		if (apps.isEmpty()) {
			return null;
		} else {
			return apps.get(0);
		}
	}

	@SuppressWarnings("unchecked")
	private static List<Software> getAll(boolean isPublic)
	{
		try
		{
			Session session = getSession();

			String hql = "from Software where publiclyAvailable = :publiclyAvailable";
			List<Software> apps = (List<Software>) session.createQuery(hql)
				.setBoolean("publiclyAvailable", isPublic)
				.list();

			session.flush();

			return apps;
		}
		catch (HibernateException ex)
		{
			try
			{
				if (HibernateUtil.getSession().isOpen())
				{
					HibernateUtil.rollbackTransaction();
				}
			}
			catch (Exception e)
			{
			}
			throw new SoftwareException(ex);
		}
		finally
		{
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}

	public static List<Software> getAllPublic()
	{
		return getAll(true);
	}

	public static List<Software> getAllPrivate()
	{
		return getAll(false);
	}
	
	
    /**
     * Returns all users apps up to the {@link Settings#DEFAULT_PAGE_SIZE}.
     * 
     * @param username
     * @param includePublic
     * @return
     */
    public static List<Software> getUserApps(String username, boolean includePublic)
    {
	    return getUserApps(username, includePublic, 0, Settings.DEFAULT_PAGE_SIZE);
    }

	/**
	 * Returns all users apps with pagination support.
	 * @param username
	 * @param includePublic
	 * @param offset
	 * @param limit
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static List<Software> getUserApps(String username, boolean includePublic, int offset, int limit)
	{
		String hql = "FROM Software AS sw " +
				"LEFT JOIN fetch sw.permissions AS pems " +
				"WHERE ";

		if (includePublic) {
			hql +=  "sw.owner = :pemUser " +
					"OR pems.username = :pemUser " +
					"OR pems.username = :worldUser " +
					"OR sw.publiclyAvailable = :publiclyAvailable " +
				"ORDER BY sw.name, sw.version DESC";
		} else {
			hql +=  "sw.publiclyAvailable = :publiclyAvailable " +
					"AND (sw.owner = :pemUser " +
					"OR pems.username = :worldUser " +
					"OR pems.username = :pemUser) " +
				"ORDER BY sw.name DESC, sw.version DESC";
		}

		try
		{
			Session session = getSession();

			List<Software> apps = (List<Software>) session.createQuery(hql)
				.setBoolean("publiclyAvailable", includePublic)
				.setString("pemUser",username)
				.setString("worldUser", Settings.WORLD_USER_USERNAME)
				.setFirstResult(offset)
                .setMaxResults(limit)
				.setResultTransformer(DistinctRootEntityResultTransformer.INSTANCE)
				.list();

			session.flush();

			return apps;
		}
		catch (HibernateException ex)
		{
			try
			{
				if (HibernateUtil.getSession().isOpen())
				{
					HibernateUtil.rollbackTransaction();
				}
			}
			catch (Exception e)
			{
			}
			throw new SoftwareException(ex);
		}
		finally
		{
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}
	
	

	@SuppressWarnings("unchecked")
	public static List<Software> getAll()
	{
		try
		{
			Session session = getSession();
			session.clear();
			List<Software> apps =
					(List<Software>) session.createQuery("from Software").list();
//			session.flush();
			return apps;

		}
		catch (HibernateException ex)
		{
			try
			{
				if (HibernateUtil.getSession().isOpen())
				{
					HibernateUtil.rollbackTransaction();
				}
			}
			catch (Exception e)
			{
			}
			throw new SoftwareException(ex);
		}
		finally
		{
//			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}

	public static Software merge(Software software)
	{
		log.debug("merging Software instance");

		try
		{
			Session session = getSession();

			Software mergedSoftware = (Software)session.merge(software);

			//session.flush();

			return mergedSoftware;
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
			throw ex;
		}
		finally
		{
			try { HibernateUtil.commitTransaction(); } catch (Throwable e) {}
		}
	}

	public static void persist(Software software) throws SoftwareException
	{
		if (software == null)
			throw new SoftwareException("Software cannot be null");

		try
		{
			Session session = getSession();
			session.saveOrUpdate(software);
			session.flush();
		}
		catch (Exception ex)
		{
			try
			{
				if (HibernateUtil.getSession().isOpen())
				{
					HibernateUtil.rollbackTransaction();
				}
			}
			catch (Exception e){}

			throw new SoftwareException("Failed to save application. " + ex.getMessage(), ex);
		}
		finally
		{
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}

	public static void delete(Software software) throws SoftwareException
	{

		if (software == null)
			throw new SoftwareException("Software cannot be null");

		try
		{
			Session session = getSession();
			session.disableFilter("softwareTenantFilter");
			session.delete(software);
			session.flush();
		}
		catch (HibernateException ex)
		{
			try
			{
				if (HibernateUtil.getSession().isOpen())
				{
					HibernateUtil.rollbackTransaction();
				}
			}
			catch (Exception e)
			{
			}
			throw new SoftwareException("Failed to delete application", ex);
		}
		finally
		{
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}

	public static List<Software> getAllPublicByName(String name) {
		return getByName(name, true, false);
	}

	public static List<Software> getAllPublic(String name) {
		return getByName("", true, false);
	}

	public static List<Software> getAllPrivateByName(String name) {
		return getByName(name, false, false);
	}

	public static List<Software> getAllPrivate(String name) {
		return getByName("", false, false);
	}

	@SuppressWarnings({ "unchecked", "deprecation" })
	public static List<Software> getByName(String name, boolean trueIfPublicFalseIfPrivate, boolean includeInactive)
	{
		if (name == null) {
			name = "";
		} else {
			name = name.trim().toLowerCase();
		}

		try
		{
			Session session = getSession();

			Criteria criteria =  session.createCriteria(Software.class)
					.add(Restrictions.eq("publiclyAvailable", trueIfPublicFalseIfPrivate))
					.add(Restrictions.sqlRestriction("CONCAT(lower({alias}.name), '-', lower({alias}.version)) like lower(?)",
							"%" + name.toLowerCase() + "%", Hibernate.STRING))
					.addOrder(Order.asc("name"))
					.addOrder(Order.asc("version"))
					.addOrder(Order.asc("revisionCount"));

			if (!includeInactive) {
				criteria.add(Restrictions.eq("available", true));
			}

			session.flush();

			return (List<Software>)criteria.list();
		}
		catch (HibernateException ex)
		{
			try
			{
				if (HibernateUtil.getSession().isOpen())
				{
					HibernateUtil.rollbackTransaction();
				}
			}
			catch (Exception e)
			{
			}
			throw new SoftwareException(ex);
		}
		finally
		{
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}

	}

	public static List<Software> getAllByTag(String tag)
	{
		return getAllByAttribute("tags", tag, false);
	}

	public static List<Software> getAllBySemanticName(String name)
	{
		return getAllByAttribute("ontology", name, false);
	}

	@SuppressWarnings("unchecked")
	public static List<Software> getAllByAttribute(String attribute, Object value, boolean trueIfPublicFalseIfPrivate)
	{
		if (attribute == null)
			return new ArrayList<Software>();
		if (value == null)
			return new ArrayList<Software>();

		try
		{
			Session session = getSession();

			String hql = "";
			if (attribute.equals("name"))
			{
				String name = (String)value;
				if (name.contains("-"))
				{
					String rawname = name.substring(0, name.lastIndexOf("-"));
					String version = name.substring(name.lastIndexOf("-") + 1);

					hql = "from Software where name like '%" + rawname
							+ "%' and version like '%" + version
							+ "%' and publiclyAvailable = :publiclyAvailable";
				}
				else
				{
					hql = "from Software where name like '%" + name
							+ "%' and publiclyAvailable = :publiclyAvailable";
				}
			}
			else
			{
				hql = "from Software where " + attribute + " like '%"
					+ value + "%' and publiclyAvailable = :publiclyAvailable";
			}

			List<Software> apps = (List<Software>) session.createQuery(hql)
					.setBoolean("publiclyAvailable", trueIfPublicFalseIfPrivate)
					.list();

			session.flush();

			return apps;

		}
		catch (HibernateException ex)
		{
			try
			{
				if (HibernateUtil.getSession().isOpen())
				{
					HibernateUtil.rollbackTransaction();
				}
			}
			catch (Exception e)
			{
			}
			throw new SoftwareException(ex);
		}
		finally
		{
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}

	@SuppressWarnings("unchecked")
	public static List<Software> getAllBySystemId(String systemId)
	{
		if (systemId == null)
			return new ArrayList<Software>();

		try
		{
			Session session = getSession();
			String hql = "select s from Software s join s.executionSystem r where r.systemId = :systemid";
			List<Software> apps = (List<Software>) session.createQuery(hql)
					.setString("systemid", systemId)
					.list();

			session.flush();

			return apps;
		}
		catch (HibernateException ex)
		{
			throw new SoftwareException(ex);
		}
		finally
		{
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}

	@SuppressWarnings("unchecked")
	public static List<Software> getUserAppsBySystemId(String username, String systemId)
	{
		if (systemId == null)
			return new ArrayList<Software>();

		try
		{
			Session session = getSession();
			Query query = null;
			String hql = "FROM Software AS sw " +
					"LEFT JOIN fetch sw.permissions AS pems " +
					"LEFT JOIN fetch sw.executionSystem as sys " +
					"WHERE sys.systemId = :systemid ";

			if (!ServiceUtils.isAdmin(username))
			{
				hql += " AND (sw.owner = :pemUser " +
					"OR pems.username = :pemUser " +
					"OR pems.username = :worldUser " +
					"OR sw.publiclyAvailable = :publiclyAvailable) ";
			}

			hql += " ORDER BY sw.name, sw.version DESC";

			query = session.createQuery(hql);

			if (!ServiceUtils.isAdmin(username))
			{
				query.setBoolean("publiclyAvailable", true)
					.setString("pemUser",username)
					.setString("worldUser", Settings.WORLD_USER_USERNAME);
			}

			List<Software> apps = (List<Software>) query
					.setString("systemid", systemId)
					.setResultTransformer(DistinctRootEntityResultTransformer.INSTANCE)
					.list();

			session.flush();

			return apps;
		}
		catch (HibernateException ex)
		{
			throw new SoftwareException(ex);
		}
		finally
		{
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}

	public static int getMaxRevisionForPublicSoftware(String name, String version)
	{
		if (!ServiceUtils.isValid(name)) {
			throw new SoftwareException("Invalid software name");
		}

		if (!ServiceUtils.isValid(version)) {
			throw new SoftwareException("Invalid software version");
		}

		try
		{
			Session session = getSession();

			Software software = (Software) session.createCriteria(Software.class)
					.add(Restrictions.eq("name", name))
					.add(Restrictions.eq("version", version))
					.add(Restrictions.eq("publiclyAvailable", true))
					.addOrder(Order.desc("revisionCount"))
					.setMaxResults(1)
					.uniqueResult();

			session.flush();

			if (software != null) {
				return software.getRevisionCount();
			} else {
				return 0;
				//throw new SoftwareException("No public apps found for the given name and version.");
			}
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

			throw new SoftwareException(ex);
		}
		finally
		{
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}

	/**
	 * Returns max version for any public software having the given name.
	 *
	 * @param name
	 * @return
	 */
	public static String getMaxVersionForSoftwareName(String name) {

		if (!ServiceUtils.isValid(name)) {
			throw new SoftwareException("Invalid software name");
		}

		try
		{
			Session session = getSession();

			Software software = (Software) session.createCriteria(Software.class)
					.add(Restrictions.eq("name", name))
					//.add(Restrictions.eq("publiclyAvailable", true))
					.addOrder(Order.desc("version"))
					.setMaxResults(1)
					.uniqueResult();

			session.flush();

			if (software != null) {
				return software.getVersion();
			} else {
				throw new SoftwareException("No apps found for the given name and version.");
			}
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

			throw new SoftwareException(ex);
		}
		finally
		{
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}

	@SuppressWarnings({ "unchecked" })
	public static List<Software> getUserAppsByAttribute(String attribute, String tag, String username, boolean includePublic)
	{
		String hql = "FROM Software AS sw " +
				"LEFT JOIN fetch sw.permissions AS pems ";

		if (attribute.equalsIgnoreCase("name")) {
			hql += "WHERE CONCAT(sw.name, '-', sw.version) like '%"+ tag + "%' ";
		} else {
			hql += "WHERE sw." + attribute + " like '%"+ tag + "%' ";
		}

		if (includePublic) {
			hql +=  "AND (sw.owner = :pemUser " +
					"OR pems.username = :pemUser " +
					"OR pems.username = :worldUser " +
					"OR sw.publiclyAvailable = :publiclyAvailable) " +
				"ORDER BY sw.name, sw.version DESC";
		} else {
			hql +=  "AND sw.publiclyAvailable = :publiclyAvailable " +
					"AND (sw.owner = :pemUser " +
					"OR pems.username = :worldUser " +
					"OR pems.username = :pemUser) " +
				"ORDER BY sw.name DESC, sw.version DESC";
		}

		try
		{
			Session session = getSession();

			List<Software> apps = (List<Software>) session.createQuery(hql)
				.setBoolean("publiclyAvailable", includePublic)
				.setString("pemUser",username)
				.setString("worldUser", Settings.WORLD_USER_USERNAME)
				.setResultTransformer(DistinctRootEntityResultTransformer.INSTANCE)
				.list();

			session.flush();

			return apps;
		}
		catch (HibernateException ex)
		{
			try
			{
				if (HibernateUtil.getSession().isOpen())
				{
					HibernateUtil.rollbackTransaction();
				}
			}
			catch (Exception e)
			{
			}
			throw new SoftwareException(ex);
		}
		finally
		{
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}
	
	/**
     * Searches for software by the given user who matches the given set of 
     * parameters. Permissions are honored in this query.
     * 
     * @param username
     * @param searchCriteria Map of key value pairs by which to query.
     * @return
     * @throws SoftwareException
     */
    public static List<Software> findMatching(String username,
            Map<SearchTerm, Object> searchCriteria, boolean getfullResponse) throws SoftwareException
    {
        return findMatching(username, searchCriteria, 0, Settings.DEFAULT_PAGE_SIZE, getfullResponse);
    }
    
    /**
     * Searches for software by the given user who matches the given set of 
     * parameters. Permissions are honored in this query as in pagination
     *
     * @param username
     * @param searchCriteria
     * @param offset
     * @param limit
     * @return
     * @throws SoftwareException
     */
    @SuppressWarnings("unchecked")
    public static List<Software> findMatching(String username,
            Map<SearchTerm, Object> searchCriteria,
            int offset, int limit, boolean getfullResponse) throws SoftwareException
    {
        try
        {
            Session session = getSession();
            session.clear();
            String hql =  " SELECT distinct s.id as id, \n"
	                    + "     s.name as name, \n"
	                    + "     s.version as version, \n"
	                    + "     s.revisionCount as revisionCount, \n"
	                    + "     system as executionSystem, \n"
	                    + "     s.shortDescription as shortDescription, \n"
	                    + "     s.longDescription as longDescription, \n"
	                    + "     s.publiclyAvailable as publiclyAvailable, \n"
	                    + "     s.label as label, \n"
	                    + "     s.owner as owner, \n"
	                    + "     s.created as created, \n"
	                    + "     s.lastUpdated as lastUpdated, \n"
	                    + "     s.uuid as uuid \n";
            if (getfullResponse) {
            		hql += "     ,s.icon as icon, \n"
    	    			+ "     s.parallelism as parallelism, \n"
    	    			+ "     s.defaultProcessorsPerNode as defaultProcessorsPerNode, \n"
    	    			+ "     s.defaultMemoryPerNode as defaultMemoryPerNode, \n"
    	    			+ "     s.defaultNodes as defaultNodes, \n"
    	    			+ "     s.defaultMaxRunTime as defaultMaxRunTime, \n"
    	    			+ "     s.defaultQueue as defaultQueue, \n"
    	    			+ "     s.tags as tags, \n"
    	    			+ "     s.ontology as ontology, \n"
    	    			+ "     s.executionType as executionType, \n"
    	    			+ "     s.deploymentPath as deploymentPath, \n"
    	    			+ "     s.storageSystem as storageSystem, \n"
    	    			+ "     s.executablePath as executablePath, \n"
    	    			+ "     s.testPath as testPath, \n"
    	    			+ "     s.checkpointable as checkpointable, \n"
    	    			+ "     s.modules as modules, \n "
    	    			+ "     s.available as available \n";
            } 
            
               hql += " FROM Software s \n"
                    + "     left join s.inputs input \n"
                    + "     left join s.parameters parameter \n" 
                    + "     left join s.outputs output \n"
                    + "     left join s.executionSystem system \n";
            
            SearchTerm publicSearchTerm = null;
            for (SearchTerm searchTerm: searchCriteria.keySet()) {
                if (searchTerm.getMappedField().startsWith("public")) {
                    publicSearchTerm = searchTerm;
                }
            }
            if (!AuthorizationHelper.isTenantAdmin(username)) {
                // no public search term specified. return public and private
                if (publicSearchTerm == null) {
                    hql +=  " WHERE ( \n" +
                            "       s.owner = :owner OR \n" +
                            "       s.publiclyAvailable = true OR \n" +
                            "       s.id in ( \n" + 
                            "               SELECT sp.software.id FROM SoftwarePermission sp \n" +
                            "               WHERE sp.username = :owner AND sp.permission <> :none \n" +
                            "              ) \n" +
                            "      ) AND \n";
                } 
                // public = false || public.eq = false || public.neq = true, return only private
                else if ((publicSearchTerm.getOperator() == SearchTerm.Operator.EQ && 
                            !(Boolean)searchCriteria.get(publicSearchTerm)) || 
                        (publicSearchTerm.getOperator() == SearchTerm.Operator.NEQ && 
                            (Boolean)searchCriteria.get(publicSearchTerm))) {
                    hql +=  " WHERE ( \n" +
                            "       s.owner = :owner OR \n" +
                            "       s.id in ( \n" + 
                            "               SELECT sp.software.id FROM SoftwarePermission sp \n" +
                            "               WHERE sp.username = :owner AND sp.permission <> :none \n" +
                            "              ) \n" +
                            "      ) AND \n";
                } 
                // else return public apps. they will be included in the general where clause
                else {
                    
                }
            } else {
                hql += " WHERE ";
            }
            
            hql +=  "        s.tenantId = :tenantid "; 
            
            for (SearchTerm searchTerm: searchCriteria.keySet()) 
            {
                if (searchCriteria.get(searchTerm) == null 
                        || StringUtils.equalsIgnoreCase(searchCriteria.get(searchTerm).toString(), "null")) 
                {
                	if (searchTerm.getOperator() == SearchTerm.Operator.NEQ ) {
                        hql += "\n       AND       " + String.format(searchTerm.getMappedField(), searchTerm.getPrefix()) + " is not null ";
                    } else if (searchTerm.getOperator() == SearchTerm.Operator.EQ ) {
                        hql += "\n       AND       " + String.format(searchTerm.getMappedField(), searchTerm.getPrefix()) + " is null ";
                    } else {
                        hql += "\n       AND       " + searchTerm.getExpression();
                    }
                } else {
                    hql += "\n       AND       " + searchTerm.getExpression();
                }
            }
            
            if (!hql.contains("s.available")) {
                hql +=  "\n       AND s.available = :visiblebydefault \n";
            }
            
            hql +=  " ORDER BY s.lastUpdated DESC\n";
            
            String q = hql;
            
            Query query = session.createQuery(hql)
            			.setResultTransformer(Transformers.aliasToBean(Software.class))
			            .setString("tenantid", TenancyHelper.getCurrentTenantId());
            
            q = q.replaceAll(":tenantid", "'" + TenancyHelper.getCurrentTenantId() + "'");
            
            if (hql.contains(":visiblebydefault") ) {
                query.setBoolean("visiblebydefault", Boolean.TRUE);
                
                q = q.replaceAll(":visiblebydefault", "1");
            }
            
            if (!ServiceUtils.isAdmin(username)) {
                query.setString("owner",username)
                    .setString("none",PermissionType.NONE.name());
                q = q.replaceAll(":owner", "'" + username + "'")
                    .replaceAll(":none", "'NONE'");
            }
            
            for (SearchTerm searchTerm: searchCriteria.keySet()) 
            {
                if (searchTerm.getOperator() == SearchTerm.Operator.BETWEEN || searchTerm.getOperator() == SearchTerm.Operator.ON) {
                    List<String> formattedDates = (List<String>)searchTerm.getOperator().applyWildcards(searchCriteria.get(searchTerm));
                    for(int i=0;i<formattedDates.size(); i++) {
                        query.setString(searchTerm.getSafeSearchField()+i, formattedDates.get(i));
                        q = q.replaceAll(":" + searchTerm.getSafeSearchField()+i, "'" + formattedDates.get(i) + "'");
                    }
                }
                else if (searchTerm.getOperator().isSetOperator()) 
                {
                    query.setParameterList(searchTerm.getSafeSearchField(), (List<Object>)searchCriteria.get(searchTerm));
                    q = q.replaceAll(":" + searchTerm.getSafeSearchField(), "('" + StringUtils.join((List<String>)searchTerm.getOperator().applyWildcards(searchCriteria.get(searchTerm)), "','") + "')" );
                }
                else if (searchCriteria.get(searchTerm) == null 
                        || StringUtils.equalsIgnoreCase(searchCriteria.get(searchTerm).toString(), "null")
                        && (searchTerm.getOperator() == SearchTerm.Operator.NEQ || searchTerm.getOperator() == SearchTerm.Operator.EQ )) {
                    // this was explicitly set to 'is null' or 'is not null'
                }
                else 
                {
                    query.setParameter(searchTerm.getSafeSearchField(), 
                            searchTerm.getOperator().applyWildcards(searchCriteria.get(searchTerm)));
                    q = q.replaceAll(":" + searchTerm.getSafeSearchField(), "'" + String.valueOf(searchTerm.getOperator().applyWildcards(searchCriteria.get(searchTerm))) + "'");
                }
                
            }
            
            log.debug(q);
            
            List<Software> softwares = query
                    .setFirstResult(offset)
                    .setMaxResults(limit)
                    .list();
            
            session.flush();
            
            return softwares;

        }
        catch (Throwable ex)
        {
            throw new SoftwareException(ex);
        }
        finally {
            try { HibernateUtil.commitTransaction();} catch (Exception e) {}
        }
    }

	/**
	 * Returns all previously published versions of the software.
	 * 
	 * @param publishedSoftware
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<String> getPreviousVersionsOfPublshedSoftware(Software publishedSoftware) 
	throws SoftwareException
	{
		if (publishedSoftware == null) {
			throw new SoftwareException("No public software provided");
		}

		try
		{
			Session session = getSession();
			String sql = "SELECT uuid as swUUID FROM softwares "
					+ "WHERE name = :swname "
					+ "		 and version = :swversion "
					+ "		 and publicly_available = :publiclyavailable "
					+ "		 and revision_count < :swrevisioncount "
					+ "		 and tenant_id = :tenantid ";
			
			List<String> uuids = (List<String>) session.createSQLQuery(sql)
					.addScalar("swUUID")
					.setString("swname", publishedSoftware.getName())
					.setString("swversion", publishedSoftware.getVersion())
					.setBoolean("publiclyavailable", true)
					.setInteger("swrevisioncount", publishedSoftware.getRevisionCount())
					.setString("tenantid", publishedSoftware.getTenantId())
					.list();
			
			session.flush();

			return uuids;
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

			throw new SoftwareException(ex);
		}
		finally
		{
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}
	
	/**
	 * Returns a list of uuid for all active jobs running using this {@link Software}
	 * unique name.
	 * @param software
	 * @return
	 * @throws SoftwareException
	 */
	@SuppressWarnings("unchecked")
	public List<String> getActiveJobsForSoftware(Software software) 
	throws SoftwareException
	{
		if (software == null ) {
            throw new SoftwareException("Invalid app");
        }
		
        Session session = null;
        try {
            session = HibernateUtil.getSession();

            String sql = "SELECT uuid FROM jobs j"
            		+ "WHERE j.software_name = :uniquename "
            		+ "		and j.status not in ('FINISHED', 'FAILED', 'STOPPED', 'ARCHIVING', 'CLEANING_UP') "
                    + "     and j.tenant_id = :tenantid ";

            return (List<String>)session.createSQLQuery(sql)
            		.addScalar("jobUUID", org.hibernate.type.StringType.INSTANCE)
            		.setString("tenantid", software.getTenantId())
                    .setString("uniquename", software.getUniqueName())
                    .list();
        
        } 
        catch (Throwable ex) {
            throw new SoftwareException(ex);
        } 
        finally {
            try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
        }
	}

	/**
	 * Returns a list of uuid for all active jobs for the given {@code jobOwner} 
	 * running using this {@link Software} unique name.
	 * 
	 * @param software the {@link Software} for which to look up active jobs
	 * @param jobOwner the owner of the job
	 * @return
	 * @throws SoftwareException
	 */
	@SuppressWarnings("unchecked")
	public List<String> getActiveUserJobsForSoftware(Software software, String jobOwner)
	throws SoftwareException 
	{
		if (software == null ) {
            throw new SoftwareException("Invalid app");
        }
		
        Session session = null;
        try {
            session = HibernateUtil.getSession();

            String sql = "SELECT uuid as jobUUID FROM jobs j "
            		+ "WHERE j.software_name = :uniquename "
            		+ "		and j.status not in ('FINISHED', 'FAILED', 'STOPPED', 'ARCHIVING', 'CLEANING_UP') "
            		+ " 	and j.owner = :jobowner "
                    + "     and j.tenant_id = :tenantid ";

            return (List<String>)session.createSQLQuery(sql)
            		.addScalar("jobUUID", org.hibernate.type.StringType.INSTANCE)
            		.setString("uniquename", software.getUniqueName())
                    .setString("jobowner", jobOwner)
                    .setString("tenantid", software.getTenantId())
            		.list();
        
        } 
        catch (Throwable ex) {
            throw new SoftwareException(ex);
        } 
        finally {
            try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
        }
	}
	
}
