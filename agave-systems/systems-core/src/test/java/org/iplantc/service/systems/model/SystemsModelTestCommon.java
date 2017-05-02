package org.iplantc.service.systems.model;

import java.util.Collection;

import org.hibernate.Session;
import org.iplantc.service.common.persistence.HibernateUtil;
import org.iplantc.service.systems.exceptions.SystemArgumentException;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

/**
 * Created with IntelliJ IDEA.
 * User: steve
 * Date: 4/4/12
 * Time: 11:09 AM
 * To change this template use File | Settings | File Templates.
 */
@Test(groups={"integration"})
public class SystemsModelTestCommon 
{
    public static final String TENANT_ADMIN = "dooley";
	public static final String SYSTEM_OWNER = "testuser";
	public static final String SYSTEM_SHARE_USER = "bob";
	public static final String SYSTEM_PUBLIC_USER = "public";
	public static final String SYSTEM_UNSHARED_USER = "dan";
	public static final String SYSTEM_INTERNAL_USERNAME = "test_user";
	
	public static String EXECUTION_SYSTEM_TEMPLATE_DIR = "target/test-classes/systems/execution";
	public static String STORAGE_SYSTEM_TEMPLATE_DIR = "target/test-classes/systems/storage";
	public static String SOFTWARE_SYSTEM_TEMPLATE_DIR = "target/test-classes/software";
	public static String INTERNAL_USER_TEMPLATE_DIR = "target/test-classes/internal_users";
	public static String CREDENTIALS_TEMPLATE_DIR = "target/test-classes/credentials";
	
	protected JSONTestDataUtil jtd;
	protected JSONObject jsonTree;

	@BeforeTest
	protected void beforeClass() throws Exception {
		jtd = JSONTestDataUtil.getInstance();
	}
	
	protected void clearSystems() throws Exception 
	{
	    Session session = null;
        try
        {
            HibernateUtil.beginTransaction();
            session = HibernateUtil.getSession();
            session.clear();
            HibernateUtil.disableAllFilters();
            session.createQuery("DELETE RemoteSystem").executeUpdate();
            session.flush();
        }
        catch (Throwable t) {
        	t.printStackTrace();
        }
        finally
        {
            try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
        }
	}
	
	public void commonBatchQueueFromJSON(String name, Object changeValue, String message, boolean exceptionThrown)
	throws Exception 
	{
		boolean exceptionFlag = false;
		String exceptionMsg = message;
		
		jsonTree = updateTestData(name, changeValue);
		
		try 
		{
			BatchQueue.fromJSON(jsonTree);
		}
		catch(Exception se){
			exceptionFlag = true;
			exceptionMsg = "Invalid iPlant JSON submitted, attribute " + name + " " + message + " \n\"" + name + "\" = \"" + changeValue + "\"\n" + se.getMessage();
			if (!exceptionThrown) 
				Assert.fail(exceptionMsg, se);
		}

		System.out.println(" exception thrown?  expected " + exceptionThrown + " actual " + exceptionFlag);

		Assert.assertTrue(exceptionFlag == exceptionThrown, exceptionMsg);
	}
	
	public void commonAuthConfigFromJSON(String name, Object changeValue, String message, boolean exceptionThrown)
	throws Exception 
	{
		boolean exceptionFlag = false;
		String exceptionMsg = message;
		
		jsonTree = updateTestData(name, changeValue);
		
		try 
		{
			AuthConfig.fromJSON(jsonTree);
		}
		catch(Exception se){
			exceptionFlag = true;
			exceptionMsg = "Invalid iPlant JSON submitted, attribute " + name + " " + message + " \n\"" + name + "\" = \"" + changeValue + "\"\n" + se.getMessage();
			if (!exceptionThrown) 
				se.printStackTrace();
		}

		System.out.println(" exception thrown?  expected " + exceptionThrown + " actual " + exceptionFlag);

		Assert.assertTrue(exceptionFlag == exceptionThrown, exceptionMsg);
	}
	
	public void commonStorageConfigFromJSON(String name, Object changeValue, String message, boolean exceptionThrown)
	throws Exception 
	{
		jsonTree = updateTestData(name, changeValue);
		
		StorageConfig.fromJSON(jsonTree);
	}
	
	public void commonLoginConfigFromJSON(String name, Object changeValue, String message, boolean exceptionThrown)
	throws Exception 
	{
		boolean exceptionFlag = false;
		String exceptionMsg = message;
		
		jsonTree = updateTestData(name, changeValue);
		
		try 
		{
			LoginConfig.fromJSON(jsonTree);
		}
		catch(SystemArgumentException se){
			exceptionFlag = true;
			exceptionMsg = "Invalid iPlant JSON submitted, attribute " + name + " " + message + " \n\"" + name + "\" = \"" + changeValue + "\"\n" + se.getMessage();
			if (!exceptionThrown) 
				se.printStackTrace();
            throw new SystemArgumentException(exceptionMsg);
		}

		System.out.println(" exception thrown?  expected " + exceptionThrown + " actual " + exceptionFlag);

		// Assert.assertTrue(exceptionFlag == exceptionThrown, exceptionMsg);
	}

	public void commonExecutionSystemFromJSON(String name, Object changeValue, String message, boolean exceptionThrown)
	throws Exception 
	{
		boolean exceptionFlag = false;
		String exceptionMsg = message;
		
		jsonTree = updateTestData(name, changeValue);
		
		try 
		{
			ExecutionSystem.fromJSON(jsonTree);
		}
		catch(Exception se){
			exceptionFlag = true;
			exceptionMsg = "Invalid iPlant JSON submitted, attribute " + name + " " + message + " \n\"" + name + "\" = \"" + changeValue + "\"\n" + se.getMessage();
			if (!exceptionThrown) 
				se.printStackTrace();
		}

		System.out.println(" exception thrown?  expected " + exceptionThrown + " actual " + exceptionFlag);

		Assert.assertTrue(exceptionFlag == exceptionThrown, exceptionMsg);
	}


	public void commonStorageSystemFromJSON(String name, Object changeValue, String message, boolean exceptionThrown)
	throws Exception 
	{
		boolean exceptionFlag = false;
		String exceptionMsg = message;
		
		jsonTree = updateTestData(name, changeValue);
		
		try 
		{
			StorageSystem.fromJSON(jsonTree);
		}
		catch(Exception se){
			exceptionFlag = true;
			exceptionMsg = "Invalid iPlant JSON submitted, attribute " + name + " " + message + " \n\"" + name + "\" = \"" + changeValue + "\"\n" + se.getMessage();
			if (!exceptionThrown) 
				se.printStackTrace();
		}

		System.out.println(" exception thrown?  expected " + exceptionThrown + " actual " + exceptionFlag);

		Assert.assertTrue(exceptionFlag == exceptionThrown, exceptionMsg);
	}

	public void commonAuthenticationSystemFromJSON(String name, Object changeValue, String message, boolean exceptionThrown)
	throws Exception 
	{
		boolean exceptionFlag = false;
		String exceptionMsg = message;
		
		jsonTree = updateTestData(name, changeValue);
		
		try 
		{
			CredentialServer.fromJSON(jsonTree);
		}
		catch(Exception se){
			exceptionFlag = true;
			exceptionMsg = "Invalid iPlant JSON submitted, attribute " + name + " " + message + " \n\"" + name + "\" = \"" + changeValue + "\"\n" + se.getMessage();
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
			jsonTree.put(attribute,  (Object)null);
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
			jsonTree.put(attribute, ( (Float) newValue ).floatValue());
		}
		else if (newValue instanceof Double) {
			jsonTree.put(attribute, ( (Double) newValue ).doubleValue());
		}
		else if (newValue instanceof Long) {
			jsonTree.put(attribute, ( (Long) newValue ).longValue());
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
