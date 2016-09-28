package org.iplantc.service.systems.dao;

// Generated Feb 1, 2013 6:15:53 PM by Hibernate Tools 3.4.0.CR1

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.CacheMode;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.hibernate.transform.AliasToEntityMapResultTransformer;
import org.hibernate.transform.DistinctRootEntityResultTransformer;
import org.hibernate.transform.Transformers;
import org.hibernate.type.StandardBasicTypes;
import org.iplantc.service.common.Settings;
import org.iplantc.service.common.auth.AuthorizationHelper;
import org.iplantc.service.common.dao.AbstractDao;
import org.iplantc.service.common.exceptions.PermissionException;
import org.iplantc.service.common.persistence.HibernateUtil;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.common.search.SearchTerm;
import org.iplantc.service.metadata.util.ServiceUtils;
import org.iplantc.service.systems.exceptions.SystemException;
import org.iplantc.service.systems.manager.SystemManager;
import org.iplantc.service.systems.model.AuthConfig;
import org.iplantc.service.systems.model.ExecutionSystem;
import org.iplantc.service.systems.model.RemoteConfig;
import org.iplantc.service.systems.model.RemoteSystem;
import org.iplantc.service.systems.model.StorageSystem;
import org.iplantc.service.systems.model.SystemRole;
import org.iplantc.service.systems.model.enumerations.RemoteSystemType;
import org.iplantc.service.systems.model.enumerations.SystemStatusType;
import org.iplantc.service.systems.search.SystemSearchResult;
import org.iplantc.service.transfer.model.enumerations.PermissionType;

/**
 * Home object for domain model class RemoteSystem.
 * 
 * @see org.iplantc.service.systems.model.RemoteSystem
 * @author Hibernate Tools
 */
public class SystemDao extends AbstractDao {

    private static final Logger log = Logger.getLogger(SystemDao.class);

    /*
     * (non-Javadoc)
     * 
     * @see org.iplantc.service.common.persistence.AbstractDao#getSession()
     */
    @Override
    protected Session getSession() {
        HibernateUtil.beginTransaction();
        Session session = HibernateUtil.getSession();
        // session.clear();
        session.enableFilter("systemTenantFilter").setParameter("tenantId",
                TenancyHelper.getCurrentTenantId());
        return session;
    }

    public void persist(RemoteSystem system) {
        try {
            Session session = getSession();
            session.saveOrUpdate(system);
            session.flush();
        } catch (HibernateException ex) {
            try {
                if (HibernateUtil.getSession().isOpen()) {
                    HibernateUtil.rollbackTransaction();
                }
            } catch (Exception e) {
            }
            throw ex;
        } finally {
            try {
                HibernateUtil.commitTransaction();
            } catch (Throwable e) {
            }
        }
    }
    
    /**
     * Performs an atomic update of the {@link RemoteSystem} status and 
     * {@link RemoteSystem#getLastUpdated()} timestamp.
     * 
     * @param system
     * @param newStatus
     */
    public void updateStatus(RemoteSystem system, SystemStatusType newStatus) {
        try {
            Session session = getSession();
            String sql = "update systems set `status` = :newstatus, `last_updated` = :rightnow where uuid = :uuid";
    		session.createSQLQuery(sql)
    			.setString("newstatus", newStatus.name())
    			.setDate("rightnow", new Date())
    			.setString("uuid", system.getUuid())
    			.executeUpdate();
            
            session.flush();
        } catch (HibernateException ex) {
            try {
                if (HibernateUtil.getSession().isOpen()) {
                    HibernateUtil.rollbackTransaction();
                }
            } catch (Exception e) {
            }
            throw ex;
        } finally {
            try {
                HibernateUtil.commitTransaction();
            } catch (Throwable e) {
            }
        }
    }

    public RemoteSystem merge(RemoteSystem system) {
        log.debug("persisting RemoteSystem instance");

        try {
            Session session = getSession();

            RemoteSystem mergedSystem = (RemoteSystem) session.merge(system);

            // session.flush();

            return mergedSystem;
        } catch (HibernateException ex) {
            try {
                if (HibernateUtil.getSession().isOpen()) {
                    HibernateUtil.rollbackTransaction();
                }
            } catch (Exception e) {
            }
            throw ex;
        } finally {
            try {
                HibernateUtil.commitTransaction();
            } catch (Throwable e) {
            }
        }
    }

    public void remove(RemoteSystem system) throws SystemException {

        if (system == null) throw new SystemException("System cannot be null");

        try {
            Session session = getSession();
            session.setCacheMode(CacheMode.IGNORE);
            
            for (SystemRole role : system.getRoles()) {
                session.delete(role);
            }

            for (AuthConfig auth : system.getStorageConfig().getAuthConfigs()) {
                if (auth.getCredentialServer() != null) {
                    session.delete(auth.getCredentialServer());
                }
                session.delete(auth);
            }

            if (system instanceof ExecutionSystem) {
                for (AuthConfig auth : ((ExecutionSystem) system).getLoginConfig().getAuthConfigs()) {
                    if (auth.getCredentialServer() != null) {
                        session.delete(auth.getCredentialServer());
                    }
                    session.delete(auth);
                }
            }

            session.delete(system);

            session.flush();
        } catch (HibernateException ex) {
            try {
                if (HibernateUtil.getSession().isOpen()) {
                    HibernateUtil.rollbackTransaction();
                }
            } catch (Exception e) {
            }
        } finally {
            try {
                HibernateUtil.commitTransaction();
            } catch (Throwable e) {
            }
        }
    }

    public RemoteSystem findById(Long id) {
        try {
            Session session = getSession();

            RemoteSystem system = (RemoteSystem) session.createCriteria(RemoteSystem.class)
                    .add(Restrictions.idEq(id)).add(Restrictions.eq("available", true))
                    .uniqueResult();

            session.flush();

            return system;
        } 
        catch (ObjectNotFoundException e) {
            return null;
        } 
        catch (HibernateException ex) {
            throw ex;
        } 
        finally {
            try {
                HibernateUtil.commitTransaction();
            } 
            catch (Throwable e) {}
        }
    }
    
    /**
     * Finds system by systemId regardless of avaialbility and ownership.
     * @param systemId
     * @return
     */
    public RemoteSystem findActiveAndInactiveSystemBySystemId(String systemId) {
        try {
            Session session = getSession();
            
            String hql = "from RemoteSystem " + "where systemId = :systemId "
                    + "		and tenantId = :tenantId ";
            
            RemoteSystem system = (RemoteSystem) session.createQuery(hql)
                    .setString("systemId", systemId)
                    .setString("tenantId", TenancyHelper.getCurrentTenantId())
                    .setMaxResults(1).uniqueResult();

            session.flush();

            return system;
        } catch (ObjectNotFoundException ex) {
            return null;
        } catch (HibernateException ex) {
            throw ex;
        } finally {
            try {
                HibernateUtil.commitTransaction();
            } catch (Throwable e) {
            }
        }
    }

    public RemoteSystem findBySystemId(String systemId) {
        try {
            Session session = getSession();
            
            String hql = "from RemoteSystem " + "where systemId = :systemId "
                    + "		and tenantId = :tenantId " 
                    + "		and available = :available";
            
            RemoteSystem system = (RemoteSystem) session.createQuery(hql)
                    .setString("systemId", systemId)
                    .setString("tenantId", TenancyHelper.getCurrentTenantId())
                    .setBoolean("available", Boolean.TRUE).setMaxResults(1).uniqueResult();

            session.flush();

            return system;
        } catch (ObjectNotFoundException ex) {
            return null;
        } catch (HibernateException ex) {
            throw ex;
        } finally {
            try {
                HibernateUtil.commitTransaction();
            } catch (Throwable e) {
            }
        }
    }

    @SuppressWarnings("unchecked")
    public List<RemoteSystem> findByExample(String name, Object value) {
        try {
            Session session = getSession();
            // System.out.println("Current tenant is " +
            // TenancyHelper.getCurrentTenantId());

            session.clear();
            // session.disableFilter("systemTenantFilter");
            Criteria criteria = session.createCriteria(RemoteSystem.class).add(
                    Restrictions.eq("available", true));

            if (!name.equals("available")) {
                criteria.add(Restrictions.eq(name, value));
            }

            List<RemoteSystem> systems = (List<RemoteSystem>) criteria.list();

            session.flush();

            return systems;
        } catch (ObjectNotFoundException ex) {
            return null;
        } catch (HibernateException ex) {
            throw ex;
        } finally {
            try {
                HibernateUtil.commitTransaction();
            } catch (Throwable e) {
            }
        }
    }

    /**
     * Returns the default storage system
     * 
     * @return
     */
    public RemoteSystem getPlatformStorageSystem() {
        try {
            Session session = HibernateUtil.getSession();

            RemoteSystem system = (RemoteSystem) session.createCriteria(RemoteSystem.class)
                    .add(Restrictions.eq("systemId", Settings.PLATFORM_STORAGE_SYSTEM_ID))
                    .uniqueResult();

            return system;
        } catch (ObjectNotFoundException ex) {
            return null;
        } catch (HibernateException ex) {
            throw new SystemException("Failed to retrieve the platform strage sytem "
                    + Settings.PLATFORM_STORAGE_SYSTEM_ID, ex);
        }
    }

    public List<RemoteSystem> getUserSystems(String apiUsername, boolean includePublic) {
        return getUserSystems(apiUsername, includePublic, null);
    }

    @SuppressWarnings("unchecked")
    public List<RemoteSystem> getUserSystems(String apiUsername, boolean includePublic, RemoteSystemType systemType) {
        String hql = "SELECT sys FROM RemoteSystem AS sys " + "left JOIN sys.roles AS r "
                + "WHERE sys.available = :available ";

        if (systemType != null) {
            hql += "AND sys.type = '" + systemType.name() + "' ";
        }

        if (includePublic) {
            hql += "AND ( sys.owner = :pemUser "
                    + "	OR (r.username = :pemUser AND r.role != 'NONE') "
                    + "	OR sys.publiclyAvailable = :publiclyAvailable ) "
                    + "ORDER BY sys.name DESC";
        } else {
            hql += "AND sys.publiclyAvailable = :publiclyAvailable " + "AND (sys.owner = :pemUser "
                    + "OR (r.username = :pemUser AND r.role != 'NONE')) "
                    + "ORDER BY sys.name DESC";
        }

        try {
            Session session = getSession();

            Query query = session.createQuery(hql).setBoolean("available", true)
                    .setBoolean("publiclyAvailable", includePublic)
                    .setString("pemUser", apiUsername)
                    .setResultTransformer(DistinctRootEntityResultTransformer.INSTANCE);

            List<RemoteSystem> systems = (List<RemoteSystem>) query.list();

            session.flush();

            return systems;
        } catch (HibernateException ex) {
            try {
                if (HibernateUtil.getSession().isOpen()) {
                    HibernateUtil.rollbackTransaction();
                }
            } catch (Exception e) {
            }

            throw ex;
        } finally {
            try {
                HibernateUtil.commitTransaction();
            } catch (Throwable e) {
            }
        }
    }

    public boolean isSystemIdUnique(String systemId) {
        try {
            Session session = getSession();
            session.clear();
            boolean unique = (session.createCriteria(RemoteSystem.class)
                    .add(Restrictions.eq("systemId", systemId)).list().size() == 0);

            session.flush();

            return unique;
        } catch (HibernateException ex) {
            throw ex;
        } finally {
            try {
                HibernateUtil.commitTransaction();
            } catch (Throwable e) {
            }
        }
    }

    /**
     * Returns a list of public systems set as defaults for all users
     * that have not otherwise set their default systems.
     *
     * @return List of RemoteSystem objects
     */
    @SuppressWarnings("unchecked")
    public List<RemoteSystem> getDefaultSystems() {
        String hql = "FROM RemoteSystem AS sys " + "WHERE sys.available = :available "
                + "AND sys.publiclyAvailable = :publiclyAvailable "
                + "AND sys.globalDefault = :globalDefault " + "ORDER BY sys.name DESC";

        try {
            Session session = getSession();
            session.clear();
            List<RemoteSystem> systems = session.createQuery(hql).setBoolean("available", true)
                    .setBoolean("publiclyAvailable", true).setBoolean("globalDefault", true).list();

            session.flush();

            return systems;
        } catch (HibernateException ex) {
            try {
                if (HibernateUtil.getSession().isOpen()) {
                    HibernateUtil.rollbackTransaction();
                }
            } catch (Exception e) {
            }

            throw ex;
        } finally {
            try {
                HibernateUtil.commitTransaction();
            } catch (Throwable e) {
            }
        }
    }

    /**
     * Returns the system of the specified type that has been globally
     * set as a default.
     * 
     * @param remoteSystemType
     * @param tenantId
     * @return
     */
    public RemoteSystem getGlobalDefaultSystemForTenant(RemoteSystemType remoteSystemType, String tenantId) 
    {
        String hql = "FROM RemoteSystem AS sys " 
        		   + "WHERE sys.available = :available "
                   + "		AND sys.publiclyAvailable = :publiclyAvailable "
                   + "		AND sys.globalDefault = :globalDefault "
                   + "		AND sys.tenantId = :tenantId ";

        if (remoteSystemType != null) {
            hql   += "		AND sys.type = :sysType ";
        }

        hql 	  += "ORDER BY sys.tenantId, sys.name DESC ";

        try {
            Session session = getSession();
            session.disableFilter("systemTenantFilter");
            session.clear();
            Query query = session.createQuery(hql)
                    .setBoolean("available", true)
                    .setBoolean("publiclyAvailable", true)
                    .setBoolean("globalDefault", true)
                    .setString("tenantId", tenantId);

            if (remoteSystemType != null) {
                query.setParameter("sysType", remoteSystemType);
            }

            RemoteSystem defaultSystem = (RemoteSystem) query.setMaxResults(1).uniqueResult();

            session.flush();

            return defaultSystem;
        } catch (HibernateException ex) {
            try {
                if (HibernateUtil.getSession().isOpen()) {
                    HibernateUtil.rollbackTransaction();
                }
            } catch (Exception e) {
            }

            throw ex;
        } finally {
            try {
                HibernateUtil.commitTransaction();
            } catch (Throwable e) {
            }
        }
    }

    /**
     * Returns all systems the user has set as their defaults. This will not
     * include public systems that are global defaults. These need to be
     * manually added if missing.
     * 
     * @param apiUsername
     * @return List of RemoteSystem objects represeting the user default systems
     */
    @SuppressWarnings("unchecked")
    public List<RemoteSystem> findUserDefaultSystems(String apiUsername) {
        String hql = "SELECT s.system " + "FROM RemoteSystem s "
                + "WHERE uds.system.available = :available "
                + "AND :username in elements( s.usersUsingAsDefault) " + "ORDER BY sys.name DESC";

        try {
            Session session = getSession();
            session.clear();
            List<RemoteSystem> systems = (List<RemoteSystem>) session.createQuery(hql)
                    .setString("username", apiUsername).setBoolean("available", true).list();

            session.flush();

            return systems;
        } catch (HibernateException ex) {
            try {
                if (HibernateUtil.getSession().isOpen()) {
                    HibernateUtil.rollbackTransaction();
                }
            } catch (Exception e) {
            }

            throw ex;
        } finally {
            try {
                HibernateUtil.commitTransaction();
            } catch (Throwable e) {
            }
        }
    }

    /**
     * Returns the system of the specified type that the user set as
     * their default. This will not include public systems that are
     * global defaults. These need to be manually added if missing.
     * 
     * @param type
     * @return RemoteSystem
     */
    public RemoteSystem findUserDefaultSystem(String apiUsername, RemoteSystemType type) {
        try {
            Session session = getSession();
            session.clear();
            String hql = "SELECT s " 
            		+ "FROM RemoteSystem s " 
            		+ "WHERE s.type = :type "
            		+ "AND s.available = :available "
                    + "AND s.publiclyAvailable = :publiclyAvail "
                    + "AND s.globalDefault = :globalDefault " 
                    + "AND :username in elements( s.usersUsingAsDefault) ";

            RemoteSystem defaultSystem = (RemoteSystem) session.createQuery(hql)
                    .setString("username", apiUsername)
                    .setParameter("type", type)
                    .setBoolean("available", true)
                    .setBoolean("publiclyAvail", false)
                    .setBoolean("globalDefault", false)
                    .setMaxResults(1)
                    .uniqueResult();

            session.flush();

            return defaultSystem;
        } catch (ObjectNotFoundException ex) {
            return null;
        } catch (HibernateException ex) {
            throw ex;
        } finally {
            try {
                HibernateUtil.commitTransaction();
            } catch (Exception e) {
            }
        }

    }

    @SuppressWarnings("unchecked")
    public List<RemoteSystem> getAll() {
        try {
            Session session = getSession();
            session.clear();
            List<RemoteSystem> systems = (List<RemoteSystem>) session.createQuery(
                    "from RemoteSystem").list();

            session.flush();

            return systems;
        } catch (HibernateException ex) {
            throw ex;
        } finally {
            try {
                HibernateUtil.commitTransaction();
            } catch (Throwable e) {
            }
        }
    }

    @SuppressWarnings("unchecked")
    public List<ExecutionSystem> getAllExecutionSystems() {
        try {
            Session session = getSession();
            session.clear();
            List<ExecutionSystem> systems = (List<ExecutionSystem>) session.createQuery(
                    "from ExecutionSystem").list();

            session.flush();

            return systems;
        } catch (HibernateException ex) {
            throw ex;
        } finally {
            try {
                HibernateUtil.commitTransaction();
            } catch (Throwable e) {
            }
        }
    }

    @SuppressWarnings("unchecked")
    public List<StorageSystem> getAllStorageSystems() {
        try {
            Session session = getSession();
            session.clear();
            List<StorageSystem> systems = (List<StorageSystem>) session.createQuery(
                    "from StorageSystem").list();

            session.flush();

            return systems;
        } catch (HibernateException ex) {
            throw ex;
        } finally {
            try {
                HibernateUtil.commitTransaction();
            } catch (Throwable e) {
            }
        }
    }

    public void removeRemoteAuthConfig(RemoteConfig remoteConfig, Long loginAuthConfigId) {
        if (remoteConfig == null) throw new SystemException("RemoteConfig cannot be null");

        if (loginAuthConfigId == null) throw new SystemException("AuthConfig cannot be null");

        try {
            Session session = getSession();

            Set<AuthConfig> configs = remoteConfig.getAuthConfigs();
            for (AuthConfig authConfig : configs) {
                if (authConfig.getId() == loginAuthConfigId) {
                    // configs.remove(authConfig);
                    // s.saveOrUpdate(loginConfig);
                    session.delete(authConfig);
                    break;
                }
            }

            session.flush();
        } catch (HibernateException ex) {
            try {
                if (HibernateUtil.getSession().isOpen()) {
                    HibernateUtil.rollbackTransaction();
                }
            } catch (Exception e) {
            }
        } finally {
            try {
                HibernateUtil.commitTransaction();
            } catch (Throwable e) {
            }
        }
    }

    public void removeRemoteAuthConfigForInternalUsername(RemoteConfig remoteConfig, String internalUsername) {
        if (remoteConfig == null) throw new SystemException("RemoteConfig cannot be null");

        if (internalUsername == null) throw new SystemException("internalUsername cannot be null");

        try {
            Session session = getSession();

            Set<AuthConfig> configs = remoteConfig.getAuthConfigs();
            for (AuthConfig authConfig : configs) {
                if (authConfig.getInternalUsername() != null
                        && authConfig.getInternalUsername().equals(internalUsername)) {
                    session.delete(authConfig);
                }
            }

            session.flush();
        } catch (HibernateException ex) {
            try {
                if (HibernateUtil.getSession().isOpen()) {
                    HibernateUtil.rollbackTransaction();
                }
            } catch (Exception e) {
            }
        } finally {
            try {
                HibernateUtil.commitTransaction();
            } catch (Throwable e) {
            }
        }
    }

    public RemoteSystem findUserSystemBySystemId(String username, String systemId) {
        return findUserSystemBySystemId(username, systemId, null);
    }

    public RemoteSystem findUserSystemBySystemId(String username, String systemId, RemoteSystemType systemType) {
        if (StringUtils.isEmpty(systemId)) {
            return null;
        } 
        else 
        {
            if (ServiceUtils.isAdmin(username)) {
                RemoteSystem system = findBySystemId(systemId);
                if (system != null) {
                    if (systemType == null || system.getType() == systemType) {
                        return system;
                    }
                } 
                
                return null;
            } 
            else 
            {
                String hql = "SELECT sys FROM RemoteSystem AS sys " 
                        + "left JOIN sys.roles AS r "
                        + "WHERE sys.available = :available " 
                        + "	AND sys.systemId = :systemId "
                        + "	AND ( sys.owner = :pemUser "
                        + "		OR (r.username = :pemUser AND r.role != 'NONE') "
                        + "		OR sys.publiclyAvailable = :publiclyAvailable ) ";

                if (systemType != null) {
                    hql += "AND sys.type = :systemType ";
                }
                
                hql += "ORDER BY sys.name DESC";

                try {
                    Session session = getSession();
                    session.clear();
                    Query query = session.createQuery(hql)
                            .setBoolean("available", true)
                            .setBoolean("publiclyAvailable", true)
                            .setString("pemUser", username)
                            .setString("systemId", systemId);
                    
                    if (systemType != null) {
                        query.setString("systemType", systemType.name());
                    }
                    
                    RemoteSystem system = (RemoteSystem) query 
                            .setResultTransformer(DistinctRootEntityResultTransformer.INSTANCE)
                            .setMaxResults(1)
                            .uniqueResult();

                    session.flush();

                    return system;
                } catch (HibernateException ex) {
                    throw ex;
                } finally {
                    try {
                        HibernateUtil.commitTransaction();
                    } catch (Throwable e) {}
                }
            }
        }
    }

    public void updateAuthConfig(AuthConfig authConfig) {
        log.debug("Updating auth config");

        try {
            HibernateUtil.beginTransaction();
            Session session = HibernateUtil.getSession();
            session.clear();
            session.saveOrUpdate(authConfig);
            session.flush();
        } catch (HibernateException ex) {
            try {
                if (HibernateUtil.getSession().isOpen()) {
                    HibernateUtil.rollbackTransaction();
                }
            } catch (Exception e) {
            }
            throw ex;
        } finally {
            try {
                HibernateUtil.commitTransaction();
            } catch (Throwable e) {
            }
        }
    }

    /**
     * Returns the {@link RemoteSystem} capable of accessing the job output
     * data.
     * Depending on the job status, the returned system may change over time.
     * 
     * @param jobUuid
     * @return
     * @throws PermissionException
     */
    @SuppressWarnings("unchecked")
    public RemoteSystem getRemoteSystemForJobOutput(String jobUuid, String username, String tenantId)
            throws PermissionException {

        if (StringUtils.isEmpty(jobUuid)) {
            throw new HibernateException("Invalid job id");
        }

        Session session = null;
        try {
            session = HibernateUtil.getSession();

            String sql = "select j.uuid, j.owner, j.status, j.work_path, j.archive_path, j.archive_system, j.archive_output, j.execution_system "
                    + "from jobs j left join job_permissions p on j.id = p.job_id "
                    + "where j.uuid = :uuid "
                    + "     and ( "
                    + "         j.owner = :owner "
                    + "         or ("
                    + "             p.username = :owner "
                    + "             and p.permission in ('READ','READ_WRITE','READ_EXECUTE','ALL','READ_PERMISSION','READ_WRITE_PERMISSION') "
                    + "             ) "
                    + "         or ( 1 = :isadmin )"
                    + "     ) "
                    + "     and j.visible = :visible "
                    + "     and j.tenant_id = :tenantid "
                    + "order by j.id desc";

            List<Map<String, Object>> aliasToValueMapList = session.createSQLQuery(sql)
                    .addScalar("uuid", StandardBasicTypes.STRING)
                    .addScalar("owner", StandardBasicTypes.STRING)
                    .addScalar("status", StandardBasicTypes.STRING)
                    .addScalar("work_path", StandardBasicTypes.STRING)
                    .addScalar("archive_path", StandardBasicTypes.STRING)
                    .addScalar("archive_system", StandardBasicTypes.LONG)
                    .addScalar("archive_output", StandardBasicTypes.BOOLEAN)
                    .addScalar("execution_system", StandardBasicTypes.STRING)
                    .setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE)
                    .setString("uuid", jobUuid).setString("tenantid", tenantId)
                    .setString("owner", username)
                    .setInteger("isadmin", ServiceUtils.isAdmin(username) ? 1 : 0)
                    .setBoolean("visible", Boolean.TRUE).setMaxResults(1).list();

            if (aliasToValueMapList.isEmpty()) {
                throw new PermissionException("No job found for user matching " + jobUuid);
            } else {
                Map<String, Object> row = aliasToValueMapList.get(0);

                // non-archived jobs always hold data on the execution system
                if (!((Boolean) row.get("archive_output"))) {
                    return this.findBySystemId((String) row.get("execution_system"));
                }
                // archived jobs will have data in different places depending on
                // the job status
                else {
                    List<String> prearchiveStates = Arrays.asList("ARCHIVING_FAILED", "FAILED",
                            "KILLED", "PENDING", "STAGING_INPUTS", "STAGING_JOB", "RUNNING",
                            "PAUSED", "QUEUED", "SUBMITTING", "PROCESSING_INPUTS", "STAGED",
                            "CLEANING_UP", "STOPPED");

                    // if the job could not have generated output, use the
                    // execution system
                    if (prearchiveStates.contains((String) row.get("status"))) {
                        return this.findBySystemId((String) row.get("execution_system"));
                    }
                    // otherwise use the archvie system
                    else {
                        return this.findById((Long) row.get("archive_system"));
                    }
                }
            }
        } catch (HibernateException ex) {
            log.error("Failed to get job output system for job " + jobUuid, ex);

            try {
                if (HibernateUtil.getSession().isOpen()) {
                    HibernateUtil.rollbackTransaction();
                }
            } catch (Exception e) {
            }

            throw ex;
        }
    }

    /**
     * Returns the current path to the {@link org.iplantc.service.jobs.model.Job} output data on a
     * {@link RemoteSystem}.
     * Depending on the job status, the returned system may change over time.
     * 
     * @param jobUuid
     * @return
     * @throws PermissionException
     */
    @SuppressWarnings("unchecked")
    public String getRelativePathForJobOutput(String jobUuid, String username, String tenantId)
            throws PermissionException {

        if (StringUtils.isEmpty(jobUuid)) {
            throw new HibernateException("Invalid job id");
        }

        Session session = null;
        try {
            session = HibernateUtil.getSession();

            String sql = "select j.uuid, j.owner, j.status, j.work_path, j.archive_path, j.archive_system, j.archive_output, j.execution_system "
                    + "from jobs j left join job_permissions p on j.id = p.job_id "
                    + "where j.uuid = :uuid "
                    + "     and ( "
                    + "         j.owner = :owner "
                    + "         or ("
                    + "             p.username = :owner "
                    + "             and p.permission in ('READ','READ_WRITE','READ_EXECUTE','ALL','READ_PERMISSION','READ_WRITE_PERMISSION') "
                    + "             ) "
                    + "         or ( 1 = :isadmin )"
                    + "     ) "
                    + "     and j.visible = :visible "
                    + "     and j.tenant_id = :tenantid "
                    + "order by j.id desc";

            List<Map<String, Object>> aliasToValueMapList = session.createSQLQuery(sql)
                    .addScalar("uuid", StandardBasicTypes.STRING)
                    .addScalar("owner", StandardBasicTypes.STRING)
                    .addScalar("status", StandardBasicTypes.STRING)
                    .addScalar("work_path", StandardBasicTypes.STRING)
                    .addScalar("archive_path", StandardBasicTypes.STRING)
                    .addScalar("archive_system", StandardBasicTypes.LONG)
                    .addScalar("archive_output", StandardBasicTypes.BOOLEAN)
                    .addScalar("execution_system", StandardBasicTypes.STRING)
                    .setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE)
                    .setString("uuid", jobUuid).setString("tenantid", tenantId)
                    .setString("owner", username)
                    .setInteger("isadmin", ServiceUtils.isAdmin(username) ? 1 : 0)
                    .setBoolean("visible", Boolean.TRUE).setMaxResults(1).list();

            if (aliasToValueMapList.isEmpty()) {
                throw new PermissionException("No job found for user matching " + jobUuid);
            } else {
                Map<String, Object> row = aliasToValueMapList.get(0);

                // non-archived jobs always hold data on the execution system
                if (!((Boolean) row.get("archive_output"))) {
                    return (String) row.get("work_path");
                }
                // archived jobs will have data in different places depending on
                // the job status
                else {
                    List<String> prearchiveStates = Arrays.asList("ARCHIVING_FAILED", "FAILED",
                            "KILLED", "PENDING", "STAGING_INPUTS", "STAGING_JOB", "RUNNING",
                            "PAUSED", "QUEUED", "SUBMITTING", "PROCESSING_INPUTS", "STAGED",
                            "CLEANING_UP", "STOPPED", "PAUSED");

                    // if the job could not have generated output, use the
                    // execution system
                    if (prearchiveStates.contains((String) row.get("status"))) {
                        return (String) row.get("work_path");
                    }
                    // otherwise use the archvie system
                    else {
                        return (String) row.get("archive_path");
                    }
                }
            }
        } catch (HibernateException ex) {
            log.error("Failed to get job output system for job " + jobUuid, ex);

            try {
                if (HibernateUtil.getSession().isOpen()) {
                    HibernateUtil.rollbackTransaction();
                }
            } catch (Exception e) {
            }

            throw ex;
        }
    }
    
    /**
     * Searches for {@link RemoteSystem}s by the given user who matches the
     * given set of parameters. Permissions are honored in this query. Results
     * are limited to at most {@link Settings#DEFAULT_PAGE_SIZE}.
     *
     * @param username
     * @param searchCriteria
     * @return
     * @throws JobException
     */
    public List findMatching(String username, Map<SearchTerm, Object> searchCriteria)
    throws SystemException {
        return findMatching(username, searchCriteria, Settings.DEFAULT_PAGE_SIZE, 0, false);
    }

    /**
     * Searches for {@link RemoteSystem}s by the given user who matches the
     * given set of parameters. Permissions are honored in this query.
     *
     * @param username
     * @param searchCriteria
     * @param offset
     * @param limit
     * @return
     * @throws JobException
     */
    @SuppressWarnings("unchecked")
    public List findMatching(String username, Map<SearchTerm, Object> searchCriteria, int limit, int offset, boolean fullResponse)
    throws SystemException {
        
        try {
            Class<?> transformClass = SystemSearchResult.class;
            
            Session session = getSession();
            session.clear();
            String hql = "";
            if (fullResponse) {
            	hql = "SELECT distinct s \n "; 
            }
            else {
            	hql = "SELECT distinct s.id as id, \n"
                    + "     s.name as name, \n"
                    + "     s.type as type, \n"
                    + "     s.description as description, \n"
                    + "     s.status as status, \n"
                    + "     s.publiclyAvailable as publiclyAvailable, \n"
                    + "     s.lastUpdated as lastUpdated, \n"
                    + "     s.systemId as systemId \n";
            }
            // this polymorphic relationship is not handled by Hibernate, so 
            // we determine the system type prior to match. We essentially
            // just need to know whether it's an ExecutionSystem (ie. has a
            // LoginConfig) or anything else. 
            String from = null;
            for (SearchTerm searchTerm: searchCriteria.keySet()) {
                if (searchTerm.getMappedField().startsWith("login") 
                        || searchTerm.getMappedField().startsWith("queue")
                        || searchTerm.getMappedField().startsWith("workDir")
                        || searchTerm.getMappedField().startsWith("scratchDir")
                        || searchTerm.getMappedField().startsWith("maxSystemJobs")
                        || searchTerm.getMappedField().startsWith("maxSystemJobsPerUser")
                        || searchTerm.getMappedField().startsWith("customDirectives")
                        || searchTerm.getMappedField().startsWith("startupScript")
                        || searchTerm.getMappedField().startsWith("executionType")
                        || searchTerm.getMappedField().startsWith("environment")
                        || searchTerm.getMappedField().startsWith("scheduler")
                        || searchTerm.getMappedField().startsWith("executionType")) {
                    from = "FROM ExecutionSystem s \n"
                        +  "    left join s.loginConfig loginConfig \n"
                        +  "    left join s.batchQueues queue \n";
                    break;
                } else if (searchTerm.getMappedField().startsWith("type")) {
                    if (searchCriteria.get(searchTerm) == RemoteSystemType.EXECUTION) {
                        from = "FROM ExecutionSystem s \n"
                             + "    left join s.loginConfig loginConfig \n";
                        break;
                    }
                }       
            }
            if (from == null) {
                from = "FROM RemoteSystem s \n";
            }
            
            hql += from + "    left join s.storageConfig storageConfig \n";
            
            SearchTerm publicSearchTerm = null;
            for (SearchTerm searchTerm: searchCriteria.keySet()) {
                if (searchTerm.getMappedField().startsWith("public")) {
                    publicSearchTerm = searchTerm;
                }
            }
            if (!AuthorizationHelper.isTenantAdmin(username)) {
                // no public search term specified. return public and private
                if (publicSearchTerm == null) {
                    hql += "    left join s.roles r \n"
                         + "WHERE ( \n" 
                         + "        s.owner = :owner \n"
                         + "        OR s.publiclyAvailable = true \n"
                         + "        OR ( \n " 
                         + "            r.username = :owner and r.role <> :none \n" 
                         + "        ) \n"
                         + "    ) AND ";
                } 
                // public = false || public.eq = false || public.neq = true, return only private
                else if ((publicSearchTerm.getOperator() == SearchTerm.Operator.EQ && 
                            !(Boolean)searchCriteria.get(publicSearchTerm)) || 
                        (publicSearchTerm.getOperator() == SearchTerm.Operator.NEQ && 
                            (Boolean)searchCriteria.get(publicSearchTerm))) {
                    hql += "    left join s.roles r \n"
                            + "WHERE ( \n" 
                            + "        s.owner = :owner \n"
                            + "        OR ( \n " 
                            + "            r.username = :owner and r.role <> :none \n" 
                            + "        ) \n"
                            + "    ) AND ";
                } 
                // else return public apps. they will be included in the general where clause
                else {
                    
                }
            } else {
                hql += "WHERE ";
            }

            hql += "        s.tenantId = :tenantid ";

            for (SearchTerm searchTerm : searchCriteria.keySet()) {
                if (searchTerm.getSearchField().startsWith("default")) {
                	SystemManager systemManager = new SystemManager();
            		List<String> defaultSystemList = new ArrayList<String>();
            	    for (RemoteSystem system: systemManager.getUserDefaultSystems(username)) {
            	    	if (system != null) {
            	    		defaultSystemList.add(system.getSystemId());
            	    	}
            	    }
                	String defaultCriteria = String.format("%s %sin ('%s') ",
                							searchTerm.getMappedField().replaceAll("%s", searchTerm.getPrefix()),
                							searchTerm.getOperator() == SearchTerm.Operator.NEQ || !((Boolean)searchCriteria.get(searchTerm)) ? "not " : "",
                							StringUtils.join(defaultSystemList, "','"));
                	
                    hql += "\n       AND       " + defaultCriteria;
                }
                else if (searchCriteria.get(searchTerm) == null 
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
                hql += "\n       AND s.available = :availablebydefault \n";
            }

            hql += " ORDER BY s.lastUpdated DESC\n";

            String q = hql;

            Query query = session.createQuery(hql);
            if (!fullResponse) {
            	query.setResultTransformer(Transformers.aliasToBean(transformClass));
            } 
            query.setString("tenantid", TenancyHelper.getCurrentTenantId());

            q = q.replaceAll(":tenantid", "'" + TenancyHelper.getCurrentTenantId() + "'");

            if (hql.contains(":availablebydefault")) {
                query.setBoolean("availablebydefault", Boolean.TRUE);

                q = q.replaceAll(":availablebydefault", "1");
            }

            if (hql.contains(":owner")) {
                query.setString("owner", username);
                q = q.replaceAll(":owner", "'" + username + "'");
            }
            
            if (hql.contains(":none")) {
                query.setString("none", PermissionType.NONE.name());
                q = q.replaceAll(":none", "'NONE'");
            }

            for (SearchTerm searchTerm : searchCriteria.keySet()) {
            	if (searchTerm.getSearchField().startsWith("default")) {
            		// this has already been set.
            	}
            	else if (searchTerm.getOperator() == SearchTerm.Operator.BETWEEN) {
                    List<String> formattedDates = (List<String>)searchTerm.getOperator().applyWildcards(searchCriteria.get(searchTerm));
                    for(int i=0;i<formattedDates.size(); i++) {
                        query.setString(searchTerm.getSafeSearchField()+i, formattedDates.get(i));
                        q = q.replaceAll(":" + searchTerm.getSafeSearchField(), "'" + formattedDates.get(i) + "'");
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
                    q = StringUtils.replace(q, ":" + searchTerm.getSafeSearchField(), 
                            Pattern.quote("'" + String.valueOf(searchTerm.getOperator().applyWildcards(searchCriteria.get(searchTerm))) + "'"));
                }
            }

            log.debug(q);

            List<SystemSearchResult> transferTasks = query.setFirstResult(offset).setMaxResults(limit).list();

            session.flush();

            return transferTasks;

        } catch (Throwable ex) {
            throw new SystemException(ex);
        } finally {
            try {
                HibernateUtil.commitTransaction();
            } catch (Exception e) {
            }
        }
    }

    /**
     * Gets @link ExecutionSystem} with the given id and available to the user.
     * 
     * @param username
     * @param systemId
     * @return {@link ExecutionSystem} with the given id or null if no match.
     */
    public ExecutionSystem getUserExecutionSystem(String username, String systemId) {
        return (ExecutionSystem) findUserSystemBySystemId(username, systemId,
                RemoteSystemType.EXECUTION);
    }

    /**
     * Gets @link StorageSystem} with the given id and available to the user.
     * 
     * @param username
     * @param systemId
     * @return {@link StorageSystem} with the given id or null if no match.
     */
    public StorageSystem getUserStorageSystem(String username, String systemId) {
        return (StorageSystem) findUserSystemBySystemId(username, systemId,
                RemoteSystemType.STORAGE);
    }

	@SuppressWarnings("unchecked")
	public List<String> getUserOwnedAppsForSystemId(String username, Long systemId) {
		
		if (StringUtils.isEmpty(username)) {
            throw new HibernateException("Invalid app owner username");
        }
		
		if (systemId < 1) {
            throw new HibernateException("Invalid systemId");
        }

        Session session = null;
        try {
            session = HibernateUtil.getSession();

            String sql = "select s.uuid "
            		+ "from softwares s "
            		+ "where s.owner = :owner "
            		+ "		and (s.system_id = :systemid or s.storage_system_id = :systemid) "
                    + "     and s.`available` = :available "
                    + "     and s.tenant_id = :tenantid "
                    + "order by s.name asc";

            return (List<String>)session.createSQLQuery(sql)
            		.setString("tenantid", TenancyHelper.getCurrentTenantId())
                    .setString("owner", username)
                    .setLong("systemid", systemId)
                    .setBoolean("available", true)
                    .list();
            
            
        } catch (Throwable ex) {
            throw new SystemException(ex);
        } finally {
            try {
                HibernateUtil.commitTransaction();
            } catch (Exception e) {
            }
        }
	}	
}
