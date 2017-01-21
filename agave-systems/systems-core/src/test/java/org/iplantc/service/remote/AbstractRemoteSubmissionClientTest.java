/**
 * 
 */
package org.iplantc.service.remote;

import java.io.File;
import java.io.IOException;

import static org.iplantc.service.systems.model.JSONTestDataUtil.*;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.systems.dao.SystemDao;
import org.iplantc.service.systems.exceptions.RemoteCredentialException;
import org.iplantc.service.systems.model.ExecutionSystem;
import org.iplantc.service.systems.model.JSONTestDataUtil;
import org.iplantc.service.systems.model.LoginConfig;
import org.iplantc.service.systems.model.StorageConfig;
import org.iplantc.service.systems.model.StorageSystem;
import org.iplantc.service.transfer.AbstractRemoteDataClientTest;
import org.iplantc.service.transfer.RemoteDataClient;
import org.iplantc.service.transfer.exceptions.RemoteDataException;
import org.json.JSONException;
import org.json.JSONObject;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @author dooley
 *
 */
@Test(singleThreaded=true, groups= {"transfer", "irods.filesystem.init","broken"})
public abstract class AbstractRemoteSubmissionClientTest {

	protected ThreadLocal<RemoteSubmissionClient> threadClient = new ThreadLocal<RemoteSubmissionClient>();
    
    private static final Logger log = Logger.getLogger(AbstractRemoteSubmissionClientTest.class);
    
	protected RemoteSubmissionClient client;
	protected LoginConfig loginConfig;
    protected ExecutionSystem system;
	protected JSONTestDataUtil jtd;
	protected JSONObject jsonTree;

	@BeforeClass
	protected void beforeClass() throws Exception
	{
		jtd = JSONTestDataUtil.getInstance();
		
		JSONObject json = getSystemJson();
        json.remove("id");
        json.put("id", this.getClass().getSimpleName());
        system = ExecutionSystem.fromJSON(json);
        system.setOwner(TEST_OWNER);
        String homeDir = system.getStorageConfig().getHomeDir();
        homeDir = StringUtils.isEmpty(homeDir) ? "" : homeDir;
        system.getStorageConfig().setHomeDir( homeDir + "/" + getClass().getSimpleName());
//        StorageConfig storageConfig = system.getStorageConfig();
//        String salt = system.getSystemId() + storageConfig.getHost() + 
//                storageConfig.getDefaultAuthConfig().getUsername();
        
        SystemDao dao = Mockito.mock(SystemDao.class);
        Mockito.when(dao.findBySystemId(Mockito.anyString()))
            .thenReturn(system);
	}
	
	@AfterClass(alwaysRun=true)
    protected void afterClass() throws Exception {
        try { getClient().close(); } catch (Exception e) {}
    }
	
	/**
     * Returns a {@link JSONObject} representing the system to test.
     * 
     * @return 
     * @throws JSONException
     * @throws IOException
     */
    protected abstract JSONObject getSystemJson() throws JSONException, IOException;
    
    /**
     * Gets getClient() from current thread
     * @return
     * @throws RemoteCredentialException 
     * @throws RemoteDataException 
     */
    protected RemoteSubmissionClient getClient() 
    {
        RemoteSubmissionClient client;
        try {
            if (threadClient.get() == null) {
                client = system.getRemoteSubmissionClient(null);
                threadClient.set(client);
            } 
        } catch (Exception e) {
        	Assert.fail("Failed to get client", e);
		}
        
        return threadClient.get();
    }
    
}
