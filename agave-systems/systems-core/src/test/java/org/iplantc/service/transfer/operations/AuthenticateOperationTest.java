/**
 *
 */
package org.iplantc.service.transfer.operations;

import org.apache.log4j.Logger;
import org.iplantc.service.transfer.RemoteDataClient;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Coverage tests for {@link RemoteDataClient#authenticate}.
 * @author dooley
 *
 */
@Test(singleThreaded=true, groups= {"auth","integration"})
public class AuthenticateOperationTest extends BaseRemoteDataClientOperationTest {

    private static final Logger log = Logger.getLogger(AuthenticateOperationTest.class);

    public AuthenticateOperationTest(String systemJsonFilePath,
            TransferOperationAfterMethod afterMethodTeardown,
            TransferOperationBeforeMethod beforeMethodSetup,
            ForbiddenPathProvider forbiddenPathProvider)
    {
        super(systemJsonFilePath, afterMethodTeardown, beforeMethodSetup, forbiddenPathProvider);
    }
    
    @AfterClass
    @Override
    protected void afterClass() throws Exception 
    {
        try {
//            clearSystems();
        }
        finally {
            try { getClient().disconnect(); } catch (Exception e) {}
        }
    }
    
    @BeforeMethod
    @Override
    protected void beforeMethod() throws Exception 
    {
        // no remote call is needed here
    }
    
    @AfterMethod
    @Override
    protected void afterMethod() throws Exception 
    {
     // no remote call is needed here
    }
    
    public void authenticate()
    {
        _authenticate();
    }

    protected void _authenticate()
    {
//        boolean actuallyThrewException = false;
//
//        boolean shouldThrowException = true;
//        String message = "Invalid storage auth should fail";
//        RemoteDataClient client = null;
//        try
//        {
//            JSONObject json = getSystemJson();
//            RemoteSystem system = (StorageSystem)StorageSystem.fromJSON(json);
//            system.setOwner(SYSTEM_USER);
//            system.setSystemId("qwerty12345");
//
//            salt = system.getSystemId() + system.getStorageConfig().getHost() +
//                    system.getStorageConfig().getDefaultAuthConfig().getUsername();
//
//            system.getStorageConfig().setHomeDir(system.getStorageConfig().getHomeDir() + "/agave-data-unittests");
//            system.getStorageConfig().getDefaultAuthConfig().setPassword("qwerty12345");
//            system.getStorageConfig().getDefaultAuthConfig().encryptCurrentPassword(salt);
//
//            system.getStorageConfig().getDefaultAuthConfig().setCredential("qwerty12345");
//            system.getStorageConfig().getDefaultAuthConfig().encryptCurrentCredential(salt);
//
//            system.getStorageConfig().getDefaultAuthConfig().setPublicKey("qwerty12345");
//            system.getStorageConfig().getDefaultAuthConfig().encryptCurrentPublicKey(salt);
//
//            system.getStorageConfig().getDefaultAuthConfig().setPrivateKey("qwerty12345");
//            system.getStorageConfig().getDefaultAuthConfig().encryptCurrentPrivateKey(salt);
//
//            SystemDao dao = new SystemDao();
//            dao.persist(system);
//            client = system.getRemoteDataClient();
//            dao.remove(system);
//
//            client.authenticate();
//        }
//        catch (Exception e) {
//            actuallyThrewException = true;
//            if (!shouldThrowException)
//                Assert.fail(message, e);
//        }
//        finally {
//            try { client.disconnect(); } catch (Exception e) {}
//        }
//
//        Assert.assertEquals(actuallyThrewException, shouldThrowException, message);
    }


}
