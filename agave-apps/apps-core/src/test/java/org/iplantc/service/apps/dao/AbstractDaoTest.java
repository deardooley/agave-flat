/**
 * 
 */
package org.iplantc.service.apps.dao;

import static org.iplantc.service.apps.model.JSONTestDataUtil.*;

import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.iplantc.service.apps.exceptions.SoftwareException;
import org.iplantc.service.apps.model.JSONTestDataUtil;
import org.iplantc.service.apps.model.Software;
import org.iplantc.service.common.persistence.HibernateUtil;
import org.iplantc.service.systems.dao.SystemDao;
import org.iplantc.service.systems.model.ExecutionSystem;
import org.iplantc.service.systems.model.RemoteSystem;
import org.iplantc.service.systems.model.StorageSystem;
import org.json.JSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author dooley
 *
 */
public class AbstractDaoTest 
{	
	protected ObjectMapper mapper = new ObjectMapper();
	
	public static final String ADMIN_USER = "dooley";
	public static final String TENANT_ADMIN = "dooley";
	public static final String SYSTEM_OWNER = "testuser";
	public static final String SYSTEM_SHARE_USER = "testshare";
	public static final String SYSTEM_PUBLIC_USER = "public";
	public static final String SYSTEM_UNSHARED_USER = "testother";
	public static final String SYSTEM_INTERNAL_USERNAME = "test_user";
	public static final String EXECUTION_SYSTEM_TEMPLATE_DIR = "src/test/resources/systems/execution";
	public static final String STORAGE_SYSTEM_TEMPLATE_DIR = "src/test/resources/systems/storage";
	public static final String SOFTWARE_SYSTEM_TEMPLATE_DIR = "src/test/resources/software";
	public static final String INTERNAL_USER_TEMPLATE_DIR = "src/test/resources/internal_users";
	public static final String CREDENTIALS_TEMPLATE_DIR = "src/test/resources/credentials";
	
	
	protected JSONTestDataUtil jtd;
	protected SystemDao systemDao;
	protected StorageSystem privateStorageSystem;
	protected ExecutionSystem privateExecutionSystem;
	public Software software;
	
	@BeforeClass
	protected void beforeClass() throws Exception
	{
		systemDao = new SystemDao();
		
		jtd = JSONTestDataUtil.getInstance();

        initSystems();
        clearSoftware();
	}
	
	protected void initSystems() throws Exception
	{
		clearSystems();
		
		privateExecutionSystem = ExecutionSystem.fromJSON(jtd.getTestDataObject(TEST_EXECUTION_SYSTEM_FILE));
		privateExecutionSystem.setOwner(TEST_OWNER);
		systemDao.persist(privateExecutionSystem);
		
		privateStorageSystem = StorageSystem.fromJSON(jtd.getTestDataObject(TEST_STORAGE_SYSTEM_FILE));
		privateStorageSystem.setOwner(TEST_OWNER);
		privateStorageSystem.getUsersUsingAsDefault().add(TEST_OWNER);
        systemDao.persist(privateStorageSystem);
	}
	
	protected void clearSystems()
	{
	    Session session = null;
        try
        {
            HibernateUtil.beginTransaction();
            session = HibernateUtil.getSession();
            session.clear();
            HibernateUtil.disableAllFilters();

            session.createQuery("DELETE ExecutionSystem").executeUpdate();
            session.createQuery("DELETE StorageSystem").executeUpdate();
            session.flush();
        }
        finally
        {
            try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
        }
	}
	
	protected Software createSoftware() throws Exception
	{
		JSONObject json = jtd.getTestDataObject(TEST_SOFTWARE_SYSTEM_FILE);
		Software software = Software.fromJSON(json, TEST_OWNER);
		software.setExecutionSystem(privateExecutionSystem);
		software.setOwner(SYSTEM_OWNER);
		software.setName(software.getUuid());
		
		return software;
	}
	
	protected void clearSoftware() throws Exception
	{
	    Session session = null;
        try
        {
            HibernateUtil.beginTransaction();
            session = HibernateUtil.getSession();
            session.clear();
            HibernateUtil.disableAllFilters();

            session.createQuery("DELETE Software").executeUpdate();
            session.flush();
        }
        finally
        {
            try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
        }
	}
	
	
	@AfterClass
	public void afterClass() throws Exception
	{
		clearSoftware();
		clearSystems();
	}
}
