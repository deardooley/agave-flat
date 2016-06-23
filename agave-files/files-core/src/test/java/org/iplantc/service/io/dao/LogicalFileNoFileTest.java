package org.iplantc.service.io.dao;

import org.iplantc.service.io.BaseTestCase;
import org.iplantc.service.io.model.LogicalFile;
import org.iplantc.service.systems.dao.SystemDao;
import org.iplantc.service.systems.manager.SystemManager;
import org.iplantc.service.io.model.JSONTestDataUtil;
import org.iplantc.service.systems.model.RemoteSystem;
import org.iplantc.service.systems.model.StorageSystem;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Why is this a separate test and what is it actually testing?
 * @author dooley
 *
 */
public class LogicalFileNoFileTest extends BaseTestCase {
	
	private StorageSystem system;
	private SystemDao systemDao = new SystemDao();
	
    @BeforeMethod
    public void setup() throws Exception {
    	clearSystems();
    	clearLogicalFiles();
    	
    	system = StorageSystem.fromJSON(jtd.getTestDataObject(JSONTestDataUtil.TEST_STORAGE_SYSTEM_FILE));
		system.setOwner(SYSTEM_OWNER);
		system.setPubliclyAvailable(true);
		system.setGlobalDefault(true);
		system.setAvailable(true);
		
		systemDao.persist(system);
    }
    
    public void afterMethod() throws Exception {
    	clearLogicalFiles();
    }

    @Test
    public void noFile() {

    	String path = "jmccurdy/abyss-1.2.7.json";
        try {
            SystemManager sysManager = new SystemManager();
            RemoteSystem system = sysManager.getUserDefaultStorageSystem(SYSTEM_OWNER);
            LogicalFile logicalFile = LogicalFileDao.findBySystemAndPath(system, path);
            
            Assert.assertTrue(logicalFile == null);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

}
