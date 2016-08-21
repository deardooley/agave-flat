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
import org.iplantc.service.systems.exceptions.SystemRoleException;
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
public class SystemRoleDao extends AbstractDao {

    private static final Logger log = Logger.getLogger(SystemRoleDao.class);

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

    public void persist(SystemRole systemRole) {
        try {
            Session session = getSession();
            session.saveOrUpdate(systemRole);
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
     * Returns roles for a given {@link RemoteSystem} id. No existence check is made for a
     * {@link RemoteSystem} with the given {@code systemId}.  
     * @param systemId
     * @throws SystemRoleException
     */
    @SuppressWarnings("unchecked")
	public List<SystemRole> getRolesForSystem(String systemId) 
	throws SystemRoleException 
	{
    	try {
            Session session = getSession();
            
            String hql = "SELECT s.roles "
            		+ "FROM RemoteSystem s left join s.roles r \n"
            	    + "WHERE s.systemId = :systemid ";
            
            List<SystemRole> roles = session.createQuery(hql)
				.setString("systemid", systemId)
				.list();

            session.flush();
        
            return roles;
            
    	} catch (HibernateException ex) {
            try {
                if (HibernateUtil.getSession().isOpen()) {
                    HibernateUtil.rollbackTransaction();
                }
            } catch (Exception e) {
            }
            throw new SystemRoleException("Failed to fetch all roles for " + systemId, ex);
        } 
    	finally {
            try {
                HibernateUtil.commitTransaction();
            } catch (Throwable e) {
            }
        }
    }
    
    /**
     * Returns roles for a given user on a system {@link RemoteSystem} id. No existence check is made for a
     * {@link RemoteSystem} with the given {@code systemId}.  
     * @param systemId
     * @throws SystemRoleException
     */
    @SuppressWarnings("unchecked")
	public List<SystemRole> getUserRolesForSystem(String systemId, String principal) 
	throws SystemRoleException 
	{
    	try {
            Session session = getSession();
            
            String hql = "SELECT s.roles "
            		+ "FROM RemoteSystem s left join s.roles r \n"
            	    + "WHERE s.systemId = :systemid "
            	    + "		 AND ( \n" 
                    + "        s.owner = :principal \n"
                    + "        OR ( \n " 
                    + "            r.username = :owner and r.role <> :none \n" 
                    + "        ) \n"
                    + "    )";
            
            List<SystemRole> roles = session.createQuery(hql)
				.setString("systemid", systemId)
				.setString("principal", principal)
				.list();

            session.flush();
        
            return roles;
            
    	} catch (HibernateException ex) {
            try {
                if (HibernateUtil.getSession().isOpen()) {
                    HibernateUtil.rollbackTransaction();
                }
            } catch (Exception e) {
            }
            throw new SystemRoleException("Failed to fetch roles for " + principal + " on system " + systemId, ex);
        } 
    	finally {
            try {
                HibernateUtil.commitTransaction();
            } catch (Throwable e) {
            }
        }
    }
    
}
