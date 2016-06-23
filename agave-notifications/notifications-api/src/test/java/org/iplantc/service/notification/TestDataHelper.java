package org.iplantc.service.notification;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.POJONode;

/**
 * Convenience class to set up the environment and obtain test data.
 * 
 * @author dooley
 *
 */
public class TestDataHelper {
	
	public static final String NOTIFICATION_CREATOR = "testuser";
	public static final String NOTIFICATION_STRANGER = "bob";
	public static final String TEST_EMAIL_NOTIFICATION = "src/test/resources/notifications/email_notif.json";
	public static final String TEST_WEBHOOK_NOTIFICATION = "src/test/resources/notifications/webhook_notif.json";
	public static final String TEST_MULTIPLE_EMAIL_NOTIFICATION = "src/test/resources/notifications/email_multi_notif.json";
	public static final String TEST_MULTIPLE_WEBHOOK_NOTIFICATION = "src/test/resources/notifications/webhook_multi_notif.json";
	public static final String TEST_REALTIME_NOTIFICATION = "src/test/resources/notifications/realtime_notif.json";
	
	public static TestDataHelper testDataHelper;
	
	/**
     * Get a test data file from disk and deserializes to a JSONObject.
     *
     * @return An ObjectNode which can be traversed using json.org api
     * @throws IOException 
     */
    public ObjectNode getTestDataObject(String file) throws IOException
    {
    	InputStream in = null;
    	try 
    	{	
    		ObjectMapper mapper = new ObjectMapper();
    		in = new FileInputStream(file);
    		
	    	return (ObjectNode)mapper.readTree(in);
    	} 
    	finally {
    		try { in.close(); } catch (Exception e) {}
    	}
    }

    public POJONode makeJsonObj(Object obj) 
    {
    	ObjectMapper mapper = new ObjectMapper();
    	return mapper.getNodeFactory().POJONode(obj);
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
