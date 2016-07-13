package org.iplantc.service.systems;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.POJONode;

public class AbstractSystemsTest {
	public static String EXECUTION_SYSTEM_TEMPLATE_DIR = "src/test/resources/systems/execution";
	public static String STORAGE_SYSTEM_TEMPLATE_DIR = "src/test/resources/systems/storage";
	public static String SOFTWARE_SYSTEM_TEMPLATE_DIR = "src/test/resources/software";
	public static String INTERNAL_USER_TEMPLATE_DIR = "src/test/resources/internal_users";
	public static String CREDENTIALS_TEMPLATE_DIR = "src/test/resources/credentials";
	
	protected JSONObject jsonTree;
	
	/**
     * Get a test data file from disk and deserializes to a JSONObject.
     *
     * @return An ObjectNode which can be traversed using json.org api
     * @throws IOException 
     * @throws JsonProcessingException 
     */
    public JSONObject getTestDataObject(String file) throws JSONException, IOException
    {
    	InputStream in = null;
    	try 
    	{
            File pfile = new File(file);
    		in = new FileInputStream(pfile.getAbsoluteFile());
    		String json = IOUtils.toString(in, "UTF-8");
    		
	    	return new JSONObject(json);
    	} 
    	finally {
    		try { in.close(); } catch (Exception e) {}
    	}
    }

    public POJONode makeJsonObj(Object obj) 
    {
    	ObjectMapper mapper = new ObjectMapper();
//    	return (POJONode) mapper.getNodeFactory().POJONode(obj);
    	return (POJONode)mapper.getNodeFactory().pojoNode(obj);
    }
}
