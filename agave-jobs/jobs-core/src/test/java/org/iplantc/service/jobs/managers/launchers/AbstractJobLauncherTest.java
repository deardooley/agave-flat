package org.iplantc.service.jobs.managers.launchers;

import java.util.Arrays;

import org.iplantc.service.jobs.model.Job;
import org.mockito.Mockito;
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
	protected Object[][] resolveRuntimeNotificationMacrosProvider() 
	throws Exception
	{
		return new Object[][] {
				{ "${AGAVE_JOB_CALLBACK_NOTIFICATION|MAPPED_JOB|}", "MAPPED_JOB", null },
				{ "${AGAVE_JOB_CALLBACK_NOTIFICATION|MAPPED_JOB| }", "MAPPED_JOB", null },
				{ "${AGAVE_JOB_CALLBACK_NOTIFICATION|MAPPED_JOB|,}", "MAPPED_JOB", null },
				{ "${AGAVE_JOB_CALLBACK_NOTIFICATION|MAPPED_JOB|,,}", "MAPPED_JOB", null },
				{ "${AGAVE_JOB_CALLBACK_NOTIFICATION|MAPPED_JOB|VALUE1}", "MAPPED_JOB", Arrays.asList("VALUE1") },
				{ "${AGAVE_JOB_CALLBACK_NOTIFICATION|MAPPED_JOB|VALUE1,VALUE2}", "MAPPED_JOB", Arrays.asList("VALUE1","VALUE2") },
				{ "${AGAVE_JOB_CALLBACK_NOTIFICATION|MAPPED_JOB|VALUE1,,VALUE2}", "MAPPED_JOB", Arrays.asList("VALUE1","VALUE2") },
				{ "${AGAVE_JOB_CALLBACK_NOTIFICATION|MAPPED_JOB|VALUE1,,,}", "MAPPED_JOB", Arrays.asList("VALUE1") },
				{ "${AGAVE_JOB_CALLBACK_NOTIFICATION|MAPPED_JOB|,VALUE1}", "MAPPED_JOB", Arrays.asList("VALUE1") },
				{ "${AGAVE_JOB_CALLBACK_NOTIFICATION|MAPPED_JOB|,,VALUE1}", "MAPPED_JOB", Arrays.asList("VALUE1") },
				{ "${AGAVE_JOB_CALLBACK_NOTIFICATION|MAPPED_JOB|VALUE1,VALUE1}", "MAPPED_JOB", Arrays.asList("VALUE1") },
				
				
		};
		
	}
	
	@Test
	public void resolveRuntimeNotificationMacros(String wrapperTemplate) {
		
		Job job = Mockito.mock(Job.class);
		CLILauncher launcher = new CLILauncher(job);
		
	}
}
