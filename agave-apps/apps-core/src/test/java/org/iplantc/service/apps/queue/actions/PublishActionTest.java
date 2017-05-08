package org.iplantc.service.apps.queue.actions;

import static org.iplantc.service.apps.model.JSONTestDataUtil.*;

import org.apache.log4j.Logger;
import org.iplantc.service.apps.model.Software;
import org.iplantc.service.systems.model.ExecutionSystem;
import org.iplantc.service.systems.model.StorageSystem;
import org.iplantc.service.transfer.RemoteDataClient;
import org.iplantc.service.apps.queue.actions.AbstractWorkerActionTest;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

@Test(groups={"notReady", "integration"})
public class PublishActionTest extends AbstractWorkerActionTest {
    
    private static final Logger log = Logger.getLogger(PublishActionTest.class);
    private PublishAction publishAction;
    
    
    @DataProvider
    protected Object[][] runProvider() throws Exception {
        return new Object[][] {
                {}
        };
    }
    
    @Test(dataProvider="runProvider")
    public void run(ExecutionSystem newExecutionSystem, StorageSystem newStorageSystem, String newName, String newVersion, Software privateSoftware, boolean shouldThrowException, String message) {
        
        PublishAction publishAction = new PublishAction(software, TEST_OWNER);
        RemoteDataClient client = null;
        try {
//            stageRemoteSoftwareAssets();
            publishAction.run();
            Software publishedSoftware = publishAction.getPublishedSoftware();
            
            Assert.assertNotNull(publishedSoftware);
            client = publishedSoftware.getStorageSystem().getRemoteDataClient();
            client.authenticate();
            Assert.assertTrue(publishedSoftware.getDeploymentPath().endsWith(".zip"));
            Assert.assertTrue(client.doesExist(publishedSoftware.getDeploymentPath()));
            Assert.assertEquals(publishedSoftware.getExecutionSystem().getSystemId(), newExecutionSystem.getSystemId());
            Assert.assertEquals(publishedSoftware.getName(), newName);
            Assert.assertEquals(publishedSoftware.getVersion(), newVersion, "Expected version of software did not match action version of published software.");
            Assert.assertTrue(publishedSoftware.getRevisionCount() == 1, "Revision count was not reset after newly published app");
        }
        catch (Throwable t) {
            if (!shouldThrowException) {
                Assert.fail("Publishing happy path should not throw exception", t);
            }
        }
    }

    
    @Test
    public void copyPublicAppArchiveToDeploymentSystem(Software software, String username ) {
        
    }
//
//    @Test
//    public void fetchSoftwareDeploymentPath() {
//
//    }
//
//    @Test
//    public void getDestinationRemoteDataClient() {
//        throw new RuntimeException("Test not implemented");
//    }
//
//    @Test
//    public void getPublishedSoftware() {
//        throw new RuntimeException("Test not implemented");
//    }
//
//    @Test
//    public void getPublishingUsername() {
//        throw new RuntimeException("Test not implemented");
//    }
//
//    @Test
//    public void getSourceRemoteDataClient() {
//        throw new RuntimeException("Test not implemented");
//    }
//
//    @Test
//    public void publish() {
//        throw new RuntimeException("Test not implemented");
//    }
//
//    @Test
//    public void resolveAndCreatePublishedDeploymentPath() {
//        throw new RuntimeException("Test not implemented");
//    }

    @Test
    public void schedulePublicAppAssetBundleBackup() {
        throw new RuntimeException("Test not implemented");
    }

    @Test
    public void setPublishedSoftware() {
        throw new RuntimeException("Test not implemented");
    }

    @Test
    public void setPublishingUsername() {
        throw new RuntimeException("Test not implemented");
    }
}
