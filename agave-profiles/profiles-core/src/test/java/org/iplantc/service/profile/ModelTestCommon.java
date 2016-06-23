package org.iplantc.service.profile;

import java.util.Collection;

import org.iplantc.service.profile.model.InternalUser;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.Assert;


/**
 * Common test methods for use within the actual test class.
 * @author dooley
 *
 */
public class ModelTestCommon 
{
	protected TestDataHelper dataHelper;
	protected JSONObject jsonTree;

	public void setUp() throws Exception {
		dataHelper = TestDataHelper.getInstance();
	}
	
	public void commonInternalUserFromJSON(String name, Object changeValue, String message, boolean exceptionThrown)
	throws Exception 
	{
		boolean exceptionFlag = false;
		String exceptionMsg = message;
		
		jsonTree = updateTestData(name, changeValue);
		
		try 
		{
			InternalUser.fromJSON(jsonTree);
		}
		catch(Exception se){
			exceptionFlag = true;
			exceptionMsg = "Invalid InternalUser JSON submitted, attribute " + name + " " + message + " \n\"" + name + "\" = \"" + changeValue + "\"\n" + se.getMessage();
			if (!exceptionThrown) 
				se.printStackTrace();
		}

		System.out.println(" exception thrown?  expected " + exceptionThrown + " actual " + exceptionFlag);

		Assert.assertTrue(exceptionFlag == exceptionThrown, exceptionMsg);
	}
	
	protected JSONObject updateTestData(String attribute, Object newValue)
	throws JSONException
	{
		if (newValue == null) 
			jsonTree.put(attribute,  (String)null);
		else if (newValue instanceof Collection) {
			jsonTree.put(attribute, newValue);
		}
		else if (newValue instanceof String) {
			jsonTree.put(attribute, newValue);
		}
		else if (newValue instanceof Integer) {
			jsonTree.put(attribute, ( (Integer) newValue ).intValue());
		}
		else if (newValue instanceof Float) {
			jsonTree.put(attribute, ( (Float) newValue ).intValue());
		}
		else if (newValue instanceof Object) {
			jsonTree.put(attribute, new JSONObject(newValue));
		}
		else {
			jsonTree.put(attribute, newValue);
		}
		
		return jsonTree;
	}
}
