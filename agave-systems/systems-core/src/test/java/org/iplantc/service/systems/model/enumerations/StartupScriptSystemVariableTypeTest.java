package org.iplantc.service.systems.model.enumerations;

import static org.iplantc.service.systems.model.enumerations.StartupScriptSystemVariableType.*;

import org.iplantc.service.systems.model.ExecutionSystem;
import org.iplantc.service.systems.model.JSONTestDataUtil;
import org.iplantc.service.systems.model.SystemsModelTestCommon;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

@Test(groups={"integration"})
public class StartupScriptSystemVariableTypeTest extends SystemsModelTestCommon {
	
	protected ExecutionSystem executionSystem = null;
	
	@BeforeClass
	@Override
    public void beforeClass() throws Exception {
        super.beforeClass();
        jsonTree = jtd.getTestDataObject(JSONTestDataUtil.TEST_EXECUTION_SYSTEM_FILE);
        executionSystem = ExecutionSystem.fromJSON(jsonTree);
    }
    
    @AfterClass
	public void afterClass() throws Exception
	{
		super.clearSystems();
	}

	@DataProvider 
	protected Object[][] resolveForSystemProvider() {
		return new Object[][] {
				{ SYSTEM_STORAGE_PROTOCOL, String.format("${%s}", SYSTEM_STORAGE_PROTOCOL), executionSystem.getStorageConfig().getProtocol().name() },
				{ SYSTEM_STORAGE_HOST, String.format("${%s}", SYSTEM_STORAGE_HOST), executionSystem.getStorageConfig().getHost()},
				{ SYSTEM_STORAGE_PORT, String.format("${%s}", SYSTEM_STORAGE_PORT), String.valueOf(executionSystem.getStorageConfig().getPort())},
				{ SYSTEM_STORAGE_RESOURCE, String.format("${%s}", SYSTEM_STORAGE_RESOURCE), executionSystem.getStorageConfig().getResource()},
				{ SYSTEM_STORAGE_ZONE, String.format("${%s}", SYSTEM_STORAGE_ZONE), executionSystem.getStorageConfig().getZone()},
				{ SYSTEM_STORAGE_ROOTDIR, String.format("${%s}", SYSTEM_STORAGE_ROOTDIR), executionSystem.getStorageConfig().getRootDir()},
				{ SYSTEM_STORAGE_HOMEDIR, String.format("${%s}", SYSTEM_STORAGE_HOMEDIR), executionSystem.getStorageConfig().getHomeDir()},
				{ SYSTEM_STORAGE_AUTH_TYPE, String.format("${%s}", SYSTEM_STORAGE_AUTH_TYPE), executionSystem.getStorageConfig().getDefaultAuthConfig().getType().name()},
				{ SYSTEM_STORAGE_CONTAINER, String.format("${%s}", SYSTEM_STORAGE_CONTAINER), executionSystem.getStorageConfig().getContainerName()},
				{ SYSTEM_LOGIN_PROTOCOL, String.format("${%s}", SYSTEM_LOGIN_PROTOCOL), executionSystem.getLoginConfig().getProtocol().name()},
				{ SYSTEM_LOGIN_HOST, String.format("${%s}", SYSTEM_LOGIN_HOST), executionSystem.getLoginConfig().getHost()},
				{ SYSTEM_LOGIN_PORT, String.format("${%s}", SYSTEM_LOGIN_PORT), String.valueOf(executionSystem.getLoginConfig().getPort())},
				{ SYSTEM_LOGIN_AUTH_TYPE, String.format("${%s}", SYSTEM_LOGIN_AUTH_TYPE), executionSystem.getLoginConfig().getDefaultAuthConfig().getType().name()},
				{ SYSTEM_UUID, String.format("${%s}", SYSTEM_UUID), executionSystem.getUuid()},
				{ SYSTEM_OWNER, String.format("${%s}", SYSTEM_OWNER), executionSystem.getUuid()},
				{ SYSTEM_ID, String.format("${%s}", SYSTEM_ID), executionSystem.getSystemId()},
		};
	}
	
	@Test(dataProvider="resolveForSystemProvider", enabled=true)
	public void resolveForSystemProvider(StartupScriptSystemVariableType variable, String startupScriptValue, String expectedValue) {
		Assert.assertEquals(variable.resolveForSystem(executionSystem), expectedValue);
	}
}


