package org.iplantc.service.jobs.managers.launchers;

import static org.mockito.Mockito.*;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.common.uuid.AgaveUUID;
import org.iplantc.service.common.uuid.UUIDType;
import org.iplantc.service.jobs.model.Job;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class AbstractJobLauncherTest
{

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
	
	@Test(dataProvider="resolveRuntimeNotificationMacrosWithoutEventAndDataProvider")
	public void resolveRuntimeNotificationMacrosWithoutEventAndData(String wrapperTemplate, String expectedEvent) {
		
		Job job = Mockito.mock(Job.class);
		when(job.getUuid()).thenReturn(new AgaveUUID(UUIDType.JOB).toString());
		when(job.getUuid()).thenReturn(TenancyHelper.getCurrentTenantId());
		when(job.getUpdateToken()).thenReturn(UUID.randomUUID().toString());
		CLILauncher launcher = new CLILauncher(job);
		String resolvedWrapperCode = launcher.resolveRuntimeNotificationMacros(wrapperTemplate);
		System.out.println(resolvedWrapperCode);
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
		when(job.getUuid()).thenReturn(TenancyHelper.getCurrentTenantId());
		when(job.getUpdateToken()).thenReturn(UUID.randomUUID().toString());
		CLILauncher launcher = new CLILauncher(job);
		String resolvedWrapperCode = launcher.resolveRuntimeNotificationMacros(wrapperTemplate);
		System.out.println(resolvedWrapperCode);
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
//				{ "${AGAVE_JOB_CALLBACK_NOTIFICATION| | , , }", "JOB_RUNTIME_CALLBACK_EVENT"},
//				{ "${AGAVE_JOB_CALLBACK_NOTIFICATION||}", "JOB_RUNTIME_CALLBACK_EVENT" },
//				{ "${AGAVE_JOB_CALLBACK_NOTIFICATION|| }", "JOB_RUNTIME_CALLBACK_EVENT" },
//				{ "${AGAVE_JOB_CALLBACK_NOTIFICATION||,}", "JOB_RUNTIME_CALLBACK_EVENT" },
//				{ "${AGAVE_JOB_CALLBACK_NOTIFICATION|| , }", "JOB_RUNTIME_CALLBACK_EVENT" },
//				{ "${AGAVE_JOB_CALLBACK_NOTIFICATION||, , }", "JOB_RUNTIME_CALLBACK_EVENT" },
//				{ "${AGAVE_JOB_CALLBACK_NOTIFICATION||,,}", "JOB_RUNTIME_CALLBACK_EVENT" },
				{ "${AGAVE_JOB_CALLBACK_NOTIFICATION| |}", "JOB_RUNTIME_CALLBACK_EVENT" },
//				{ "${AGAVE_JOB_CALLBACK_NOTIFICATION|MAPPED_JOB|}", "MAPPED_JOB" },
//				{ "${AGAVE_JOB_CALLBACK_NOTIFICATION|MAPPED_JOB| }", "MAPPED_JOB" },
//				{ "${AGAVE_JOB_CALLBACK_NOTIFICATION|MAPPED_JOB|,}", "MAPPED_JOB"},
//				{ "${AGAVE_JOB_CALLBACK_NOTIFICATION|MAPPED_JOB|,,}", "MAPPED_JOB" }
		};
	}
	
	@Test(dataProvider="resolveRuntimeNotificationMacrosWithoutDataProvider", dependsOnMethods={"resolveRuntimeNotificationMacrosWithoutEvent"})
	public void resolveRuntimeNotificationMacrosWithoutData(String wrapperTemplate, String expectedEvent) {
		
		Job job = Mockito.mock(Job.class);
		when(job.getUuid()).thenReturn(new AgaveUUID(UUIDType.JOB).toString());
		when(job.getUuid()).thenReturn(TenancyHelper.getCurrentTenantId());
		when(job.getUpdateToken()).thenReturn(UUID.randomUUID().toString());
		CLILauncher launcher = new CLILauncher(job);
		String resolvedWrapperCode = launcher.resolveRuntimeNotificationMacros(wrapperTemplate);
		System.out.println(resolvedWrapperCode);
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
		when(job.getUuid()).thenReturn(TenancyHelper.getCurrentTenantId());
		when(job.getUpdateToken()).thenReturn(UUID.randomUUID().toString());
		CLILauncher launcher = new CLILauncher(job);
		String resolvedWrapperCode = launcher.resolveRuntimeNotificationMacros(wrapperTemplate);
		System.out.println(resolvedWrapperCode);
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
		when(job.getUuid()).thenReturn(TenancyHelper.getCurrentTenantId());
		when(job.getUpdateToken()).thenReturn(UUID.randomUUID().toString());
		CLILauncher launcher = new CLILauncher(job);
		String resolvedWrapperCode = launcher.resolveRuntimeNotificationMacros(wrapperTemplate);
		System.out.println(resolvedWrapperCode);
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
