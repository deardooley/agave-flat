package org.iplantc.service.jobs.managers.launchers;

import static org.iplantc.service.systems.model.enumerations.StartupScriptSystemVariableType.*;
import static org.iplantc.service.jobs.model.enumerations.StartupScriptJobVariableType.*;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.common.util.Slug;
import org.iplantc.service.common.uuid.AgaveUUID;
import org.iplantc.service.common.uuid.UUIDType;
import org.iplantc.service.jobs.exceptions.JobException;
import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.systems.model.ExecutionSystem;
import org.iplantc.service.jobs.model.JSONTestDataUtil;
import org.iplantc.service.jobs.model.enumerations.StartupScriptJobVariableType;
import org.iplantc.service.jobs.submission.AbstractJobSubmissionTest;
import org.iplantc.service.systems.model.enumerations.StartupScriptSystemVariableType;
import org.json.JSONObject;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public abstract class AbstractJobLauncherMacroResolutionTest extends AbstractJobSubmissionTest
{
	private ExecutionSystem executionSystem = null;

	@BeforeClass
	protected void beforeClass() throws Exception {
		String systemJson = FileUtils.readFileToString(new File(JSONTestDataUtil.TEST_EXECUTION_SYSTEM_FILE));
		
		executionSystem = ExecutionSystem.fromJSON(new JSONObject(systemJson));
		executionSystem.getStorageConfig().setResource("testResource");
		executionSystem.getStorageConfig().setZone("testZone");
		executionSystem.getStorageConfig().setContainerName("testContainerName");
		
		Assert.assertNotNull(executionSystem, "Unable to initialize execution system for tests");
	}
	
	@DataProvider
	protected Object[][] parseSoftwareInputValueIntoTemplateVariableValueProvider() 
	throws Exception
	{
		return new Object[][] {
				
		};
		
	}
	
	//@Test(dataProvider="parseSoftwareInputValueIntoTemplateVariableValueProvider")
	public void parseSoftwareInputValueIntoTemplateVariableValue()
	{
		throw new RuntimeException("Test not implemented");
	}
	
	@DataProvider
	protected Object[][] parseSoftwareParameterValueIntoTemplateVariableValueProvider() 
	throws Exception
	{
		return new Object[][] {
				
		};
		
	}

	//@Test
	public void parseSoftwareParameterValueIntoTemplateVariableValue()
	{
		throw new RuntimeException("Test not implemented");
	}
	
	@DataProvider
	protected Object[][] resolveMacrosProvider() 
	throws Exception
	{
		return new Object[][] {
				
		};
		
	}

	//@Test
	public void resolveMacros()
	{
		throw new RuntimeException("Test not implemented");
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
				{ SYSTEM_ID, String.format("${%s}", SYSTEM_ID), executionSystem.getSystemId()},
		};
	}
	
	@Test(dataProvider="resolveForSystemProvider", enabled=true)
	public void resolveForSystemProvider(StartupScriptSystemVariableType variable, String startupScriptValue, String expectedValue) {
		
		Job job = Mockito.mock(Job.class);
		when(job.getUuid()).thenReturn(new AgaveUUID(UUIDType.JOB).toString());
		when(job.getTenantId()).thenReturn(TenancyHelper.getCurrentTenantId());
		when(job.getUpdateToken()).thenReturn(UUID.randomUUID().toString());
		CLILauncher launcher = new CLILauncher(job);
		launcher.setExecutionSystem(executionSystem);
		String resolvedStartupScript = launcher.resolveStartupScriptMacros(startupScriptValue);
		
		Assert.assertEquals(resolvedStartupScript, expectedValue);
	}
	
	@DataProvider 
	protected Object[][] resolveForSystemJobAttributeProvider() throws JobException {
		
		Job job = new Job();
		job.setName("test job name");
		job.setSoftwareName("test_app-1.0");
		job.setArchivePath("path/to/archive/dir");
		job.setOwner(JSONTestDataUtil.TEST_OWNER);
		job.setSystem(executionSystem.getSystemId());
		
		return new Object[][] {
				{ AGAVE_JOB_NAME, String.format("${%s}", AGAVE_JOB_NAME), job, Slug.toSlug(job.getName()) },
				{ AGAVE_JOB_ID, String.format("${%s}", AGAVE_JOB_ID), job, job.getUuid()},
				{ AGAVE_JOB_APP_ID, String.format("${%s}", AGAVE_JOB_APP_ID), job, job.getSoftwareName()},
				{ AGAVE_JOB_EXECUTION_SYSTEM, String.format("${%s}", AGAVE_JOB_EXECUTION_SYSTEM), job, job.getSystem()},
				{ AGAVE_JOB_ARCHIVE_PATH, String.format("${%s}", AGAVE_JOB_ARCHIVE_PATH), job, job.getArchivePath()},
				{ AGAVE_JOB_OWNER, String.format("${%s}", AGAVE_JOB_OWNER), job, job.getOwner()},
				{ AGAVE_JOB_TENANT, String.format("${%s}", AGAVE_JOB_TENANT), job, job.getTenantId()}
		};
	}
	
	@Test(dataProvider="resolveForSystemJobAttributeProvider", dependsOnMethods={"resolveForSystemProvider"}, enabled=true)
	public void resolveForSystemJobAttribute(StartupScriptJobVariableType variable, String startupScriptValue, Job job, String expectedValue) {
		
		CLILauncher clilLauncher = new CLILauncher(job);
		CLILauncher launcher = Mockito.spy(clilLauncher);
		launcher.setExecutionSystem(executionSystem);
		when(launcher.resolveStartupScriptMacros(startupScriptValue)).thenCallRealMethod();
		String resolvedStartupScript = launcher.resolveStartupScriptMacros(startupScriptValue);
		
		Assert.assertEquals(resolvedStartupScript, expectedValue);
	}
	
	@DataProvider
	protected Object[][] resolveRuntimeNotificationMacrosWithoutEventAndDataProvider() 
	throws Exception
	{
		return new Object[][] {
				{ "${AGAVE_JOB_CALLBACK_NOTIFICATION|}", "JOB_RUNTIME_CALLBACK_EVENT" },
				{ "${AGAVE_JOB_CALLBACK_NOTIFICATION| }", "JOB_RUNTIME_CALLBACK_EVENT" },
				{ "${AGAVE_JOB_CALLBACK_NOTIFICATION|,}", "JOB_RUNTIME_CALLBACK_EVENT"},
				{ "${AGAVE_JOB_CALLBACK_NOTIFICATION|,,}", "JOB_RUNTIME_CALLBACK_EVENT" }
		};
	}
	
	@Test(dataProvider="resolveRuntimeNotificationMacrosWithoutEventAndDataProvider", dependsOnMethods={"resolveForSystemJobAttribute"})
	public void resolveRuntimeNotificationMacrosWithoutEventAndData(String wrapperTemplate, String expectedEvent) {
		
		Job job = Mockito.mock(Job.class);
		when(job.getUuid()).thenReturn(new AgaveUUID(UUIDType.JOB).toString());
		when(job.getTenantId()).thenReturn(TenancyHelper.getCurrentTenantId());
		when(job.getUpdateToken()).thenReturn(UUID.randomUUID().toString());
		CLILauncher launcher = new CLILauncher(job);
		String resolvedWrapperCode = launcher.resolveRuntimeNotificationMacros(wrapperTemplate);
		
		Assert.assertTrue(resolvedWrapperCode.contains("\"CUSTOM_USER_JOB_EVENT_NAME\": \""+expectedEvent+"\""), "Empty user event name should write default custom user event name");
	}
	
	@DataProvider
	protected Object[][] resolveRuntimeNotificationMacrosWithoutEventProvider() 
	throws Exception
	{
		return new Object[][] {
				{ "${AGAVE_JOB_CALLBACK_NOTIFICATION|VALUE1}", "JOB_RUNTIME_CALLBACK_EVENT", Arrays.asList("VALUE1") },
				{ "${AGAVE_JOB_CALLBACK_NOTIFICATION|VALUE1,VALUE2}", "JOB_RUNTIME_CALLBACK_EVENT", Arrays.asList("VALUE1","VALUE2") },
				{ "${AGAVE_JOB_CALLBACK_NOTIFICATION|VALUE1,,VALUE2}", "JOB_RUNTIME_CALLBACK_EVENT", Arrays.asList("VALUE1","VALUE2") },
				{ "${AGAVE_JOB_CALLBACK_NOTIFICATION|VALUE1,,,}", "JOB_RUNTIME_CALLBACK_EVENT", Arrays.asList("VALUE1") },
				{ "${AGAVE_JOB_CALLBACK_NOTIFICATION|,VALUE1}", "JOB_RUNTIME_CALLBACK_EVENT", Arrays.asList("VALUE1") },
				{ "${AGAVE_JOB_CALLBACK_NOTIFICATION|,,VALUE1}", "JOB_RUNTIME_CALLBACK_EVENT", Arrays.asList("VALUE1") },
				{ "${AGAVE_JOB_CALLBACK_NOTIFICATION|VALUE1,VALUE1}", "JOB_RUNTIME_CALLBACK_EVENT", Arrays.asList("VALUE1") },
			};	
		
	}
	
	@Test(dataProvider="resolveRuntimeNotificationMacrosWithoutEventProvider", dependsOnMethods={"resolveRuntimeNotificationMacrosWithoutEventAndData"})
	public void resolveRuntimeNotificationMacrosWithoutEvent(String wrapperTemplate, String expectedEvent, List<String> expectedValues) {
		
		Job job = Mockito.mock(Job.class);
		when(job.getUuid()).thenReturn(new AgaveUUID(UUIDType.JOB).toString());
		when(job.getTenantId()).thenReturn(TenancyHelper.getCurrentTenantId());
		when(job.getUpdateToken()).thenReturn(UUID.randomUUID().toString());
		CLILauncher launcher = new CLILauncher(job);
		String resolvedWrapperCode = launcher.resolveRuntimeNotificationMacros(wrapperTemplate);

		for (String uniqueName: new HashSet<String>(expectedValues)) {
			String expectedString = String.format("echo '  \"%s\": \"'$(printf %%q \"$%s\")'\",' >> \"$AGAVE_CALLBACK_FILE\"\n", uniqueName, uniqueName);
//			String expectedString = String.format("echo '  \"%s\": '$(printf %%q \"$%s\")'\",\\\n' >> \"$AGAVE_CALLBACK_FILE\"\n", uniqueName, uniqueName);
			Assert.assertTrue(resolvedWrapperCode.contains(expectedString), "User custom data should be written into the output data");
			resolvedWrapperCode = StringUtils.replaceOnce(resolvedWrapperCode,expectedString, "");
			Assert.assertFalse(resolvedWrapperCode.contains(expectedString), "Duplicate user variable names should be filtered out prior to writing the callback output");
		}
		Assert.assertTrue(resolvedWrapperCode.contains("\"CUSTOM_USER_JOB_EVENT_NAME\": \""+expectedEvent+"\""), "Empty user event name should write default custom user event name");
	}
	
	@DataProvider
	protected Object[][] resolveRuntimeNotificationMacrosWithoutDataProvider() 
	throws Exception
	{
		return new Object[][] {
				{ "${AGAVE_JOB_CALLBACK_NOTIFICATION| | , , }", "JOB_RUNTIME_CALLBACK_EVENT"},
				{ "${AGAVE_JOB_CALLBACK_NOTIFICATION||}", "JOB_RUNTIME_CALLBACK_EVENT" },
				{ "${AGAVE_JOB_CALLBACK_NOTIFICATION|| }", "JOB_RUNTIME_CALLBACK_EVENT" },
				{ "${AGAVE_JOB_CALLBACK_NOTIFICATION||,}", "JOB_RUNTIME_CALLBACK_EVENT" },
				{ "${AGAVE_JOB_CALLBACK_NOTIFICATION|| , }", "JOB_RUNTIME_CALLBACK_EVENT" },
				{ "${AGAVE_JOB_CALLBACK_NOTIFICATION||, , }", "JOB_RUNTIME_CALLBACK_EVENT" },
				{ "${AGAVE_JOB_CALLBACK_NOTIFICATION||,,}", "JOB_RUNTIME_CALLBACK_EVENT" },
				{ "${AGAVE_JOB_CALLBACK_NOTIFICATION| |}", "JOB_RUNTIME_CALLBACK_EVENT" },
				{ "${AGAVE_JOB_CALLBACK_NOTIFICATION|MAPPED_JOB|}", "MAPPED_JOB" },
				{ "${AGAVE_JOB_CALLBACK_NOTIFICATION|MAPPED_JOB| }", "MAPPED_JOB" },
				{ "${AGAVE_JOB_CALLBACK_NOTIFICATION|MAPPED_JOB|,}", "MAPPED_JOB"},
				{ "${AGAVE_JOB_CALLBACK_NOTIFICATION|MAPPED_JOB|,,}", "MAPPED_JOB" }
		};
	}
	
	@Test(dataProvider="resolveRuntimeNotificationMacrosWithoutDataProvider", dependsOnMethods={"resolveRuntimeNotificationMacrosWithoutEvent"})
	public void resolveRuntimeNotificationMacrosWithoutData(String wrapperTemplate, String expectedEvent) {
		
		Job job = Mockito.mock(Job.class);
		when(job.getUuid()).thenReturn(new AgaveUUID(UUIDType.JOB).toString());
		when(job.getTenantId()).thenReturn(TenancyHelper.getCurrentTenantId());
		when(job.getUpdateToken()).thenReturn(UUID.randomUUID().toString());
		CLILauncher launcher = new CLILauncher(job);
		String resolvedWrapperCode = launcher.resolveRuntimeNotificationMacros(wrapperTemplate);
		
		Assert.assertTrue(resolvedWrapperCode.contains("\"CUSTOM_USER_JOB_EVENT_NAME\": \""+expectedEvent+"\""), "Empty user event name should write default custom user event name");
	}
	
	@DataProvider
	protected Object[][] resolveRuntimeNotificationMacrosProvider() 
	throws Exception
	{
		return new Object[][] {
				{ "${AGAVE_JOB_CALLBACK_NOTIFICATION||VALUE1}", "JOB_RUNTIME_CALLBACK_EVENT", Arrays.asList("VALUE1") },
				{ "${AGAVE_JOB_CALLBACK_NOTIFICATION||VALUE1,VALUE2}", "JOB_RUNTIME_CALLBACK_EVENT", Arrays.asList("VALUE1","VALUE2") },
				{ "${AGAVE_JOB_CALLBACK_NOTIFICATION||VALUE1,,VALUE2}", "JOB_RUNTIME_CALLBACK_EVENT", Arrays.asList("VALUE1","VALUE2") },
				{ "${AGAVE_JOB_CALLBACK_NOTIFICATION||VALUE1,,,}", "JOB_RUNTIME_CALLBACK_EVENT", Arrays.asList("VALUE1") },
				{ "${AGAVE_JOB_CALLBACK_NOTIFICATION||,VALUE1}", "JOB_RUNTIME_CALLBACK_EVENT", Arrays.asList("VALUE1") },
				{ "${AGAVE_JOB_CALLBACK_NOTIFICATION||,,VALUE1}", "JOB_RUNTIME_CALLBACK_EVENT", Arrays.asList("VALUE1") },
				{ "${AGAVE_JOB_CALLBACK_NOTIFICATION||VALUE1,VALUE1}", "JOB_RUNTIME_CALLBACK_EVENT", Arrays.asList("VALUE1") },
				{ "${AGAVE_JOB_CALLBACK_NOTIFICATION|MAPPED_JOB|VALUE1}", "MAPPED_JOB", Arrays.asList("VALUE1") },
				{ "${AGAVE_JOB_CALLBACK_NOTIFICATION|MAPPED_JOB|VALUE1,VALUE2}", "MAPPED_JOB", Arrays.asList("VALUE1","VALUE2") },
				{ "${AGAVE_JOB_CALLBACK_NOTIFICATION|MAPPED_JOB|VALUE1,,VALUE2}", "MAPPED_JOB", Arrays.asList("VALUE1","VALUE2") },
				{ "${AGAVE_JOB_CALLBACK_NOTIFICATION|MAPPED_JOB|VALUE1,,,}", "MAPPED_JOB", Arrays.asList("VALUE1") },
				{ "${AGAVE_JOB_CALLBACK_NOTIFICATION|MAPPED_JOB|,VALUE1}", "MAPPED_JOB", Arrays.asList("VALUE1") },
				{ "${AGAVE_JOB_CALLBACK_NOTIFICATION|MAPPED_JOB|,,VALUE1}", "MAPPED_JOB", Arrays.asList("VALUE1") },
				{ "${AGAVE_JOB_CALLBACK_NOTIFICATION|MAPPED_JOB|VALUE1,VALUE1}", "MAPPED_JOB", Arrays.asList("VALUE1") },
		};	
	}
	
	@Test(dataProvider="resolveRuntimeNotificationMacrosProvider", dependsOnMethods={"resolveRuntimeNotificationMacrosWithoutData"})
	public void resolveRuntimeNotificationMacros(String wrapperTemplate, String expectedEvent, List<String> expectedValues) {
		
		Job job = Mockito.mock(Job.class);
		when(job.getUuid()).thenReturn(new AgaveUUID(UUIDType.JOB).toString());
		when(job.getTenantId()).thenReturn(TenancyHelper.getCurrentTenantId());
		when(job.getUpdateToken()).thenReturn(UUID.randomUUID().toString());
		CLILauncher launcher = new CLILauncher(job);
		String resolvedWrapperCode = launcher.resolveRuntimeNotificationMacros(wrapperTemplate);
		
		for (String uniqueName: new HashSet<String>(expectedValues)) {
			String expectedString = String.format("echo '  \"%s\": \"'$(printf %%q \"$%s\")'\",' >> \"$AGAVE_CALLBACK_FILE\"\n", uniqueName, uniqueName);
//			String expectedString = String.format("echo '  \"%s\": '$(printf %%q \"$%s\")'\",\\\n' >> \"$AGAVE_CALLBACK_FILE\"\n", uniqueName, uniqueName);
			Assert.assertTrue(resolvedWrapperCode.contains(expectedString), "User custom data should be written into the output data");
			resolvedWrapperCode = StringUtils.replaceOnce(resolvedWrapperCode,expectedString, "");
			Assert.assertFalse(resolvedWrapperCode.contains(expectedString), "Duplicate user variable names should be filtered out prior to writing the callback output");
		}
		
		Assert.assertTrue(resolvedWrapperCode.contains("\"CUSTOM_USER_JOB_EVENT_NAME\": \""+expectedEvent+"\""), "Empty user event name should write default custom user event name");
		
	}
	
	@Test()
	public void resolveRuntimeNotificationMacrosInFullWrapperTemplate() throws IOException {
		
		String wrapperTemplate = FileUtils.readFileToString(new File("target/test-classes/software/fork-1.0.0/wrapper.sh"));
		String expectedEvent = "JOB_RUNTIME_CALLBACK_EVENT";
		List<String> expectedValues = Arrays.asList("CALLBACK");
 		
		Job job = Mockito.mock(Job.class);
		when(job.getUuid()).thenReturn(new AgaveUUID(UUIDType.JOB).toString());
		when(job.getTenantId()).thenReturn(TenancyHelper.getCurrentTenantId());
		when(job.getUpdateToken()).thenReturn(UUID.randomUUID().toString());
		CLILauncher launcher = new CLILauncher(job);
		String resolvedWrapperCode = launcher.resolveRuntimeNotificationMacros(wrapperTemplate);
		
		for (String uniqueName: new HashSet<String>(expectedValues)) {
			String expectedString = String.format("echo '  \"%s\": \"'$(printf %%q \"$%s\")'\",' >> \"$AGAVE_CALLBACK_FILE\"\n", uniqueName, uniqueName);
//			String expectedString = String.format("echo '  \"%s\": '$(printf %%q \"$%s\")'\",\\\n' >> \"$AGAVE_CALLBACK_FILE\"\n", uniqueName, uniqueName);
			Assert.assertTrue(resolvedWrapperCode.contains(expectedString), "User custom data should be written into the output data");
			resolvedWrapperCode = StringUtils.replaceOnce(resolvedWrapperCode,expectedString, "");
			Assert.assertFalse(resolvedWrapperCode.contains(expectedString), "Duplicate user variable names should be filtered out prior to writing the callback output");
		}
		
		Assert.assertTrue(resolvedWrapperCode.contains("\"CUSTOM_USER_JOB_EVENT_NAME\": \""+expectedEvent+"\""), "Empty user event name should write default custom user event name");
		
	}
}
