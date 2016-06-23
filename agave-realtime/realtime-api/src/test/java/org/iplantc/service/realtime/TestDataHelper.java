package org.iplantc.service.realtime;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.iplantc.service.realtime.TestDataHelper;
import org.json.JSONException;
import org.json.JSONObject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Convenience class to set up the environment and obtain test data.
 * 
 * @author dooley
 *
 */
public class TestDataHelper {
	
	public static final String SYSTEM_OWNER = "testuser";
	public static final String SYSTEM_SHARE_USER = "bob";
	public static final String SYSTEM_PUBLIC_USER = "public";
	public static final String SYSTEM_UNSHARED_USER = "dan";
	public static final String SYSTEM_INTERNAL_USERNAME = "test_user";
	
	public static final String TEST_STORAGE_SYSTEM_FILE = "target/test-classes/systems/storage/sftp.example.com.json";
	public static final String TEST_EXECUTION_SYSTEM_FILE = "target/test-classes/systems/execution/ssh.example.com.json";
	public static final String TEST_STORAGE_MONITOR = "target/test-classes/monitors/storage_system_monitor.json";
	public static final String TEST_EXECUTION_MONITOR = "target/test-classes/monitors/execution_system_monitor.json";
	public static final String TEST_URL_MONITOR = "target/test-classes/monitors/url_monitor.json";
	
	public static TestDataHelper testDataHelper;
	
    /**
     * Get a test data file from disk and deserializes to a JsonNode.
     *
     * @return An ObjectNode which can be traversed using json.org api
     * @throws IOException 
     * @throws JsonProcessingException 
     */
    public JsonNode getTestDataObject(String file) throws JsonProcessingException, IOException
    {
    	InputStream in = null;
    	try 
    	{	
    		in = new FileInputStream(file);
    		ObjectMapper mapper = new ObjectMapper();
        	return mapper.readTree(in);
    	} 
    	finally {
    		try { in.close(); } catch (Exception e) {}
    	}
    }
    
    /**
     * Get a test data file from disk and deserializes to a JSONObject.
     *
     * @return A JSONObject which can be traversed using json.org api
     * @throws IOException 
     * @throws JSONException 
     */
    public JSONObject getTestDataObjectAsJSONObject(String file) throws JSONException, IOException
    {
    	return new JSONObject(getTestDataObject(file).toString());
    }

    private TestDataHelper() 
    {
    	init();
    }
    
    public static TestDataHelper getInstance() 
    {
    	if (testDataHelper == null) {
    		testDataHelper = new TestDataHelper();
    	}
    	
    	return testDataHelper;
    }
    
    private void init() {}
}
