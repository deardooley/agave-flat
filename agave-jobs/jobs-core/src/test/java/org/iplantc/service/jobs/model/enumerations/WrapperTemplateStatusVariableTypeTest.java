package org.iplantc.service.jobs.model.enumerations;

import java.util.Arrays;
import java.util.HashSet;

import org.apache.commons.lang.StringUtils;
import org.iplantc.service.jobs.model.Job;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class WrapperTemplateStatusVariableTypeTest {

	@DataProvider 
	protected Object[][] resolveNotificationEventMacroAddsCustomEventToCallbackDataProvider() {
		return new Object[][] {
				{ "my_custom_event", "my_custom_event", "Custom user event name should be written into the callback data" },
				{ "", "JOB_RUNTIME_CALLBACK_EVENT", "Empty user event name should write default custom user event name" },
				{ null, "JOB_RUNTIME_CALLBACK_EVENT", "null user event name should write default custom user event name" },
		};
	}
	
	@Test(dataProvider="resolveNotificationEventMacroAddsCustomEventToCallbackDataProvider", enabled=true)
	public void resolveNotificationEventMacroAddsCustomEventToCallbackData(String providedEventName, String expectedCallbackEventName, String message) {
		Job job = new Job();
		String resolvedWrapperCode = WrapperTemplateStatusVariableType
				.resolveNotificationEventMacro(job, providedEventName, new String[] {});
		
		Assert.assertTrue(resolvedWrapperCode.contains("\"CUSTOM_USER_JOB_EVENT_NAME\": \"" + expectedCallbackEventName + "\""), message);
	}
	
	@DataProvider 
	protected Object[][] resolveNotificationEventMacroAddsCustomDataToCallbackDataProvider() {
		return new Object[][] {
				{ new String[] {"FOO"}, "Single user variable should be written to callback data" },
				{ new String[] {"FOO", "BAR"}, "Multiple user variable should be written to callback data" },
				{ new String[] {"foo"}, "Single user variable case should be preserved and written to callback data" },
				{ new String[] {"fOO"}, "Single user variable case should be preserved and written to callback data" },
				{ new String[] {"FoO"}, "Single user variable case should be preserved and written to callback data" },
				{ new String[] {"FOO", "BAR"}, "Multiple user variable should be written to callback data" },				
		};
	}
	
	@Test(dataProvider="resolveNotificationEventMacroAddsCustomDataToCallbackDataProvider", enabled=true)
	public void resolveNotificationEventMacroAddsCustomDataToCallbackData(String[] customVariableNames, String message ) {
		Job job = new Job();
		String resolvedWrapperCode = WrapperTemplateStatusVariableType
				.resolveNotificationEventMacro(job, "", customVariableNames);
//		System.out.println(resolvedWrapperCode);
		for (String uniqueName: new HashSet<String>(Arrays.asList(customVariableNames))) {
			String expectedString = String.format("echo '  \"%s\": '$(printf %%q \"$%s\")'\",\\\n' >> \"$AGAVE_CALLBACK_FILE\"\n", uniqueName, uniqueName);
			Assert.assertTrue(resolvedWrapperCode.contains(expectedString), "User custom data should be written into the output data");
			resolvedWrapperCode = StringUtils.replaceOnce(resolvedWrapperCode,expectedString, "");
			Assert.assertFalse(resolvedWrapperCode.contains(expectedString), "Duplicate user variable names should be filtered out prior to writing the callback output");
		}
		
		Assert.assertTrue(resolvedWrapperCode.contains("\"CUSTOM_USER_JOB_EVENT_NAME\": \"JOB_RUNTIME_CALLBACK_EVENT\""), "Empty user event name should write default custom user event name");

	}
	
	@DataProvider 
	protected Object[][] resolveNotificationEventMacroTrimsVariableNamesProvider() {
		return new Object[][] {
				{ new String[] {" FOO "}, "Single user variable should be written to callback data" },
				{ new String[] {" FOO ", "BAR"}, "Multiple user variable should be written to callback data" },
				{ new String[] {"   foo    "}, "Single user variable case should be preserved and written to callback data" },
				{ new String[] {" FOO ", "    BAR "}, "Multiple user variable should be written to callback data" },				
		};
	}
	
	@Test(dataProvider="resolveNotificationEventMacroTrimsVariableNamesProvider", enabled=true)
	public void resolveNotificationEventMacroTrimsVariableNames(String[] customVariableNames, String message ) {
		Job job = new Job();
		String resolvedWrapperCode = WrapperTemplateStatusVariableType
				.resolveNotificationEventMacro(job, "", customVariableNames);
		
		for (String uniqueName: new HashSet<String>(Arrays.asList(customVariableNames))) {
			String expectedString = String.format("echo '  \"%s\": '$(printf %%q \"$%s\")'\",\\\n' >> \"$AGAVE_CALLBACK_FILE\"\n", StringUtils.trim(uniqueName), StringUtils.trim(uniqueName));
			Assert.assertTrue(resolvedWrapperCode.contains(expectedString), "User custom data should be written into the output data");
			resolvedWrapperCode = StringUtils.replaceOnce(resolvedWrapperCode,expectedString, "");
			Assert.assertFalse(resolvedWrapperCode.contains(expectedString), "Duplicate user variable names should be filtered out prior to writing the callback output");
		}
		
		Assert.assertTrue(resolvedWrapperCode.contains("\"CUSTOM_USER_JOB_EVENT_NAME\": \"JOB_RUNTIME_CALLBACK_EVENT\""), "Empty user event name should write default custom user event name");

	}
	
	@DataProvider 
	protected Object[][] resolveNotificationEventMacroStripsEmptyVariableNamesProvider() {
		return new Object[][] {
				{ new String[] {""}, "Empty user variable name should be excluded from callback data" },
				{ new String[] {"", "FOO"}, "Empty user variable name should be excluded from callback data" },
				{ new String[] {"FOO", ""}, "Empty user variable name should be excluded from callback data" },
				{ new String[] {"", ""}, "Multiple Empty user variable names should be excluded from callback data" },
				{ new String[] {"", "FOO", ""}, "Multiple Empty user variable names should be excluded from callback data" },
				{ new String[] {"", "", "FOO"}, "Multiple Empty user variable names should be excluded from callback data" },
				{ new String[] {"FOO", "", ""}, "Multiple Empty user variable names should be excluded from callback data" },
				{ new String[] {"", "", "FOO", "", ""}, "Multiple Empty user variable names should be excluded from callback data" },
				
				{ new String[] {"FOO", "FOO"}, "Duplicate user variable should be filtered for uniqueness before being written to callback data" },
				{ new String[] {"FOO", "FOO", "", ""}, "Duplicate empty and duplicate user variable should be filtered for uniqueness before being written to callback data" },
				{ new String[] {"", "", "FOO", "FOO"}, "Duplicate empty and duplicate user variable should be filtered for uniqueness before being written to callback data" },
				{ new String[] {"", "", "FOO", "FOO", "", ""}, "Duplicate empty and duplicate user variable should be filtered for uniqueness before being written to callback data" },
				{ new String[] {"FOO", "", "", "FOO"}, "Duplicate empty and duplicate user variable should be filtered for uniqueness before being written to callback data" },
		};
	}
	
	@Test(dataProvider="resolveNotificationEventMacroStripsEmptyVariableNamesProvider", enabled=true)
	public void resolveNotificationEventMacroStripsEmptyVariableNames(String[] customVariableNames, String message ) {
		Job job = new Job();
		String resolvedWrapperCode = WrapperTemplateStatusVariableType
				.resolveNotificationEventMacro(job, "", customVariableNames);
		
		String expectedString = "echo '  \"null\": '$(printf %%q \"$null\")'\",\\\n' >> \"$AGAVE_CALLBACK_FILE\"\n";
		Assert.assertFalse(resolvedWrapperCode.contains(expectedString), message);
		
		expectedString = "echo '  \"\": '$(printf %%q \"$\")'\",\\\n' >> \"$AGAVE_CALLBACK_FILE\"\n";
		Assert.assertFalse(resolvedWrapperCode.contains(expectedString), message);
		
		for (String uniqueName: new HashSet<String>(Arrays.asList(customVariableNames))) {
			if (StringUtils.trimToNull(uniqueName) == null) continue; 
			expectedString = String.format("echo '  \"%s\": '$(printf %%q \"$%s\")'\",\\\n' >> \"$AGAVE_CALLBACK_FILE\"\n", StringUtils.trim(uniqueName), StringUtils.trim(uniqueName));
			Assert.assertTrue(resolvedWrapperCode.contains(expectedString), "User custom data should be written into the output data");
			resolvedWrapperCode = StringUtils.replaceOnce(resolvedWrapperCode,expectedString, "");
			Assert.assertFalse(resolvedWrapperCode.contains(expectedString), "Duplicate user variable names should be filtered out prior to writing the callback output");
		}
		
		Assert.assertTrue(resolvedWrapperCode.contains("\"CUSTOM_USER_JOB_EVENT_NAME\": \"JOB_RUNTIME_CALLBACK_EVENT\""), "Empty user event name should write default custom user event name");

	}
	
	@DataProvider 
	protected Object[][] resolveNotificationEventMacroStripsBlankVariableNamesProvider() {
		return new Object[][] {
				{ new String[] {" "}, "Blank user variable name should be excluded from callback data" },
				{ new String[] {"          "}, "Blank user variable name should be excluded from callback data" },
				{ new String[] {" ", "FOO"}, "Blank user variable name should be excluded from callback data" },
				{ new String[] {" ", "FOO"}, "Blank user variable name should be excluded from callback data" },
				{ new String[] {"FOO", " "}, "Blank user variable name should be excluded from callback data" },
				{ new String[] {" ", " "}, "Multiple Blank user variable names should be excluded from callback data" },
				{ new String[] {" ", "FOO", " "}, "Multiple Blank user variable names should be excluded from callback data" },
				{ new String[] {" ", " ", "FOO"}, "Multiple Blank user variable names should be excluded from callback data" },
				{ new String[] {"FOO", " ", " "}, "Multiple Blank user variable names should be excluded from callback data" },
				{ new String[] {" ", " ", "FOO", " ", " "}, "Multiple Blank user variable names should be excluded from callback data" },
				
				{ new String[] {"FOO", "FOO", " ", " "}, "Duplicate blank and duplicate user variable should be filtered for uniqueness before being written to callback data" },
				{ new String[] {" ", " ", "FOO", "FOO"}, "Duplicate blank and duplicate user variable should be filtered for uniqueness before being written to callback data" },
				{ new String[] {" ", " ", "FOO", "FOO", " ", " "}, "Duplicate blank and duplicate user variable should be filtered for uniqueness before being written to callback data" },
				{ new String[] {"FOO", " ", " ", "FOO"}, "Duplicate blank and duplicate user variable should be filtered for uniqueness before being written to callback data" },
		};
	}
	
	@Test(dataProvider="resolveNotificationEventMacroStripsBlankVariableNamesProvider", enabled=true)
	public void resolveNotificationEventMacroStripsBlankVariableNames(String[] customVariableNames, String message ) {
		Job job = new Job();
		String resolvedWrapperCode = WrapperTemplateStatusVariableType
				.resolveNotificationEventMacro(job, "", customVariableNames);
		
		String expectedString = "echo '  \"null\": '$(printf %%q \"$null\")'\",\\\n' >> \"$AGAVE_CALLBACK_FILE\"\n";
		Assert.assertFalse(resolvedWrapperCode.contains(expectedString), message);
		
		expectedString = "echo '  \"\": '$(printf %%q \"$\")'\",\\\n' >> \"$AGAVE_CALLBACK_FILE\"\n";
		Assert.assertFalse(resolvedWrapperCode.contains(expectedString), message);
		
		for (String uniqueName: new HashSet<String>(Arrays.asList(customVariableNames))) {
			if (StringUtils.trimToNull(uniqueName) == null) continue; 
			expectedString = String.format("echo '  \"%s\": '$(printf %%q \"$%s\")'\",\\\n' >> \"$AGAVE_CALLBACK_FILE\"\n", StringUtils.trim(uniqueName), StringUtils.trim(uniqueName));
			Assert.assertTrue(resolvedWrapperCode.contains(expectedString), "User custom data should be written into the output data");
			resolvedWrapperCode = StringUtils.replaceOnce(resolvedWrapperCode,expectedString, "");
			Assert.assertFalse(resolvedWrapperCode.contains(expectedString), "Duplicate user variable names should be filtered out prior to writing the callback output");
		}
		
		Assert.assertTrue(resolvedWrapperCode.contains("\"CUSTOM_USER_JOB_EVENT_NAME\": \"JOB_RUNTIME_CALLBACK_EVENT\""), "Empty user event name should write default custom user event name");

	}
		
	@DataProvider 
	protected Object[][] resolveNotificationEventMacroStripsNullVariableNamesProvider() {
		return new Object[][] {
				{ new String[] {null}, "null user variable name should be excluded from callback data" },
				{ new String[] {null, "FOO"}, "null user variable name should be excluded from callback data" },
				{ new String[] {"FOO", null}, "null user variable name should be excluded from callback data" },
				{ new String[] {null, null}, "Multiple null user variable names should be excluded from callback data" },
				{ new String[] {null, "FOO", null}, "Multiple null user variable names should be excluded from callback data" },
				{ new String[] {null, null, "FOO"}, "Multiple null user variable names should be excluded from callback data" },
				{ new String[] {"FOO", null, null}, "Multiple null user variable names should be excluded from callback data" },
				{ new String[] {null, null, "FOO", null, null}, "Multiple null user variable names should be excluded from callback data" },
				
				{ new String[] {"FOO", "FOO", null, null}, "Duplicate null and duplicate user variable should be filtered for uniqueness before being written to callback data" },
				{ new String[] {null, null, "FOO", "FOO"}, "Duplicate null and duplicate user variable should be filtered for uniqueness before being written to callback data" },
				{ new String[] {null, null, "FOO", "FOO", null, null}, "Duplicate null and duplicate user variable should be filtered for uniqueness before being written to callback data" },
				{ new String[] {"FOO", null, null, "FOO"}, "Duplicate null and duplicate user variable should be filtered for uniqueness before being written to callback data" },
		};
	}
	
	@Test(dataProvider="resolveNotificationEventMacroStripsNullVariableNamesProvider", enabled=true)
	public void resolveNotificationEventMacroStripsNullVariableNames(String[] customVariableNames, String message ) {
		Job job = new Job();
		String resolvedWrapperCode = WrapperTemplateStatusVariableType
				.resolveNotificationEventMacro(job, "", customVariableNames);
		
		String expectedString = "echo '  \"null\": '$(printf %%q \"$null\")'\",\\\n' >> \"$AGAVE_CALLBACK_FILE\"\n";
		Assert.assertFalse(resolvedWrapperCode.contains(expectedString), message);
		
		expectedString = "echo '  \"\": '$(printf %%q \"$\")'\",\\\n' >> \"$AGAVE_CALLBACK_FILE\"\n";
		Assert.assertFalse(resolvedWrapperCode.contains(expectedString), message);
		
		for (String uniqueName: new HashSet<String>(Arrays.asList(customVariableNames))) {
			if (StringUtils.trimToNull(uniqueName) == null) continue; 
			expectedString = String.format("echo '  \"%s\": '$(printf %%q \"$%s\")'\",\\\n' >> \"$AGAVE_CALLBACK_FILE\"\n", StringUtils.trim(uniqueName), StringUtils.trim(uniqueName));
			Assert.assertTrue(resolvedWrapperCode.contains(expectedString), "User custom data should be written into the output data");
			resolvedWrapperCode = StringUtils.replaceOnce(resolvedWrapperCode,expectedString, "");
			Assert.assertFalse(resolvedWrapperCode.contains(expectedString), "Duplicate user variable names should be filtered out prior to writing the callback output");
		}
		
		Assert.assertTrue(resolvedWrapperCode.contains("\"CUSTOM_USER_JOB_EVENT_NAME\": \"JOB_RUNTIME_CALLBACK_EVENT\""), "Empty user event name should write default custom user event name");

	}
}
