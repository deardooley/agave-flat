package org.iplantc.service.apps.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.POJONode;

import org.apache.commons.io.IOUtils;
import org.iplantc.service.systems.model.enumerations.ExecutionType;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: steve
 * Date: 3/19/12
 * Time: 2:31 PM
 * To change this template use File | Settings | File Templates.
 */
public class JSONTestDataUtil 
{
	public static String TEST_SOFTWARE_FOLDER = "src/test/resources/software/";
	public static String TEST_SOFTWARE_FILE = TEST_SOFTWARE_FOLDER + "head-lonestar.tacc.teragrid.org.json";
	public static String TEST_SYSTEM_FOLDER = "src/test/resources/systems/";
	public static String TEST_SOFTWARE_SYSTEM_FILE = TEST_SOFTWARE_FOLDER + "system-software.json";
	public static String TEST_EXECUTION_SYSTEM_FILE = TEST_SYSTEM_FOLDER + "execution/execute.example.com.json";
	public static String TEST_STORAGE_SYSTEM_FILE = TEST_SYSTEM_FOLDER + "storage/storage.example.com.json";
//	public static String TEST_IRODS_STORAGE_SYSTEM_FILE = TEST_SYSTEM_FOLDER + "storage/data.iplantcollaborative.org.json";
	public static String TEST_AUTHENTICATION_SYSTEM_FILE = "test/authentication-system.json";
    
	public static JSONTestDataUtil jsonTestData;
	public static String TEST_OWNER = "api_sample_user";
	public static String TEST_SHARED_OWNER = "ipctestshare";
	public static String TEST_PUBLIC_OWNER = "guest";
	
    /**
     *
     */
    protected String[] data = {"name","parallelism","version","author","helpURI","datePublished",
            "label","shortDescription","longDescription","tags","ontology",
            "executionSystem","executionType","deploymentSystem","deploymentPath","templatePath",
            "testPath","checkpointable","modules","inputs","parameters","outputs", 
            "defaultNodeCount", "defaultMemoryPerNode","defaultProcessorsPerNode","defaultMaxRunTime"};

/*
    private Object[][] softwareFields(){
        JSONTestDataUtil.concatAll()
    }
*/

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

    /**
     * Get a test data file from disk and deserializes to a JSONObject.
     *
     * @return An ObjectNode which can be traversed using json.org api
     * @throws IOException
     * @throws JsonProcessingException
     */
    public JSONObject getTestDataObject(File pfile) throws JSONException, IOException
    {
        InputStream in = null;
        try
        {
            in = new FileInputStream(pfile.getAbsoluteFile());
            String json = IOUtils.toString(in, "UTF-8");

            return new JSONObject(json);
        }
        catch (JSONException e) {
        	System.out.println(pfile.getName());
        	throw e;
        }
        finally {
            try { in.close(); } catch (Exception e) {}
        }
    }

    /**
     * Used in conjunction with jackson data mapping
     * @param obj pojo to be mapped
     * @return  a POJONode after mapping.
     */
    public POJONode makeJsonObj(Object obj)
    {
    	ObjectMapper mapper = new ObjectMapper();
//    	return mapper.getNodeFactory().POJONode(obj);
    	return (POJONode)mapper.getNodeFactory().pojoNode(obj);
    }


    protected String arbitraryArray = "{\"one\", \"two\", \"three\", \"four\", \"five\", \"six\", \"seven\", \"eight\"}";

    protected String emptyJSONFile, malformedJSONFile_1, malformedJSONFile_2 = "{}";

    protected Object[][] dataTestSoftwareFieldsEmpty = {
            {"name","","set name to empty string",true},
            {"parallelism","","set parallelism to empty string",true},
            {"version","","set version to empty string",true},
            {"helpURI","","set helpURI to empty string",false},
            {"label","","set label to empty string",false},
            {"shortDescription","","set shortDescription to empty string",false},
            {"longDescription","","set longDescription to empty string",false},
            {"tags","","tags cannot be an empty string",true},
            {"ontology","","ontology cannot be an empty string",true},
            {"executionSystem","","set executionSystem to empty string",true},
            {"executionType","","set executionType to empty string",true},
            {"deploymentPath","","set deploymentPath to empty string",true},
            {"templatePath","","set templatePath to empty string",true},
            {"testPath","","set testPath to empty string",true},             // requires a string larger than empty
            {"checkpointable","","set checkpointable to empty string",true},
            {"modules","","modules cannot be an empty string",true},
            {"inputs","","set inputs to empty string",true},
            {"parameters","","set parameters to empty string",true},
            {"outputs","","set outputs to empty string",true},
            {"defaultQueue","","set defaultQueue to empty string",true},
            {"defaultNodeCount","","set defaultNodes to empty string",true},
            {"defaultMemoryPerNode","","set defaultMemoryPerNode to empty string",true},
            {"defaultProcessorsPerNode","","set defaultProcessorsPerNode to empty string",true},
            {"defaultMaxRunTime","","set defaultMaxRunTime to empty string",true}
    };

    protected Object[][] dataTestSoftwareFieldsNull = {
            {"name",null,"set name to null",true},
            {"parallelism",null,"set parallelism to null",true},
            {"version",null,"set version to null",true},
            {"helpURI",null,"set helpURI to null",false},
            {"label",null,"set label to null",false},
            {"shortDescription",null,"set shortDescription to null",false},
            {"longDescription",null,"set longDescription to null",false},
            {"tags",null,"set tags to null",false},
            {"ontology",null,"set ontology to null",false},
            {"executionSystem",null,"set executionSystem to null",true},
            {"executionType",null,"set executionType to null",true},
            {"deploymentPath",null,"set deploymentPath to null",true},
            {"templatePath",null,"set templatePath to null",true},
            {"testPath",null,"set testPath to null",true},             // requires a string larger than empty
            {"checkpointable",null,"set checkpointable to null",false},
            {"modules",null,"set modules to null",false},
            {"inputs",null,"set inputs to null",false},
            {"parameters",null,"set parameters to null",false},
            {"outputs",null,"set outputs to null",false},
            {"defaultQueue",null,"set defaultQueue to null",false},
            {"defaultNodeCount",null,"set defaultNodes to null",false},
            {"defaultMemoryPerNode",null,"set defaultMemoryPerNode to null",false},
            {"defaultProcessorsPerNode",null,"set defaultProcessorsPerNode to null",false},
            {"defaultMaxRunTime",null,"set defaultMaxRunTime to null",false}
    };
    
    protected Object[][] dataTestSoftwareFieldsEmptyObject = {
            {"name",new JSONObject(),"set name to empty object",true},
            {"parallelism",new JSONObject(),"set parallelism to empty object",true},
            {"version",new JSONObject(),"set version to empty object",true},
            {"helpURI",new JSONObject(),"set helpURI to empty object",true},
            {"label",new JSONObject(),"set label to empty object",true},
            {"shortDescription",new JSONObject(),"set shortDescription to empty object",true},
            {"longDescription",new JSONObject(),"set longDescription to empty object",true},
            {"tags",new JSONObject(),"set tags to empty object",true},
            {"ontology",new JSONObject(),"set ontology to empty object",true},
            {"executionSystem",new JSONObject(),"set executionSystem to empty object",true},
            {"executionType",new JSONObject(),"set executionType to empty object",true},
            {"deploymentPath",new JSONObject(),"set deploymentPath to empty object",true},
            {"templatePath",new JSONObject(),"set templatePath to empty object",true},
            {"testPath",new JSONObject(),"set testPath to empty object",true},             // requires a string larger than empty
            {"checkpointable",new JSONObject(),"set checkpointable to empty object",true},
            {"modules",new JSONObject(),"set modules to empty object",true},
            {"inputs",new JSONObject(),"set inputs to empty object",true},
            {"parameters",new JSONObject(),"set parameters to empty object",true},
            {"outputs",new JSONObject(),"set outputs to empty object",true},
            {"defaultQueue",new JSONObject(),"set defaultQueue to empty object",true},
            {"defaultNodeCount",new JSONObject(),"set defaultNodes to empty object",true},
            {"defaultMemoryPerNode",new JSONObject(),"set defaultMemoryPerNode to empty object",true},
            {"defaultProcessorsPerNode",new JSONObject(),"set defaultProcessorsPerNode to empty object",true},
            {"defaultMaxRunTime",new JSONObject(),"set defaultMaxRunTime to empty object",true}
    };
    
    protected Object[][] dataTestSoftwareFieldsArray = {
            {"name",Arrays.asList(new JSONObject()),"set name to array with empty object",true},
            {"parallelism",Arrays.asList(new JSONObject()),"set parallelism to array with empty object",true},
            {"version",Arrays.asList(new JSONObject()),"set version to array with empty object",true},
            {"helpURI",Arrays.asList(new JSONObject()),"set helpURI to array with empty object",true},
            {"label",Arrays.asList(new JSONObject()),"set label to array with empty object",true},
            {"shortDescription",Arrays.asList(new JSONObject()),"set shortDescription to array with empty object",true},
            {"longDescription",Arrays.asList(new JSONObject()),"set longDescription to array with empty object",true},
            {"tags",Arrays.asList(new JSONObject()),"set tags to array with empty object",true},
            {"ontology",Arrays.asList(new JSONObject()),"set ontology to array with empty object",true},
            {"executionSystem",Arrays.asList(new JSONObject()),"set executionSystem to array with empty object",true},
            {"executionType",Arrays.asList(new JSONObject()),"set executionType to array with empty object",true},
            {"deploymentPath",Arrays.asList(new JSONObject()),"set deploymentPath to array with empty object",true},
            {"templatePath",Arrays.asList(new JSONObject()),"set templatePath to array with empty object",true},
            {"testPath",Arrays.asList(new JSONObject()),"set testPath to array with empty object",true},             // requires a string larger than empty
            {"checkpointable",Arrays.asList(new JSONObject()),"set checkpointable to array with empty object",true},
            {"modules",Arrays.asList(new JSONObject()),"set modules to array with empty object",true},
            {"inputs",Arrays.asList(new JSONObject()),"set inputs to array with empty object",true},
            {"parameters",Arrays.asList(new JSONObject()),"set parameters to array with empty object",true},
            {"outputs",Arrays.asList(new JSONObject()),"set outputs to array with empty object",true},
            {"defaultQueue",Arrays.asList(new JSONObject()),"set defaultQueue to array with empty object",true},
            {"defaultNodeCount",Arrays.asList(new JSONObject()),"set defaultNodes to array with empty object",true},
            {"defaultMemoryPerNode",Arrays.asList(new JSONObject()),"set defaultMemoryPerNode to array with empty object",true},
            {"defaultProcessorsPerNode",Arrays.asList(new JSONObject()),"set defaultProcessorsPerNode to array with empty object",true},
            {"defaultMaxRunTime",Arrays.asList(new JSONObject()),"set defaultMaxRunTime to array with empty object",true}
    };
    
    protected Object[][] dataTestSoftwareFieldsInvalid = {
            {"parallelism",Boolean.TRUE,"set parallelism to TRUE",true},
            {"parallelism",Boolean.FALSE,"set parallelism to FALSE",true},
            {"parallelism",new Integer(4),"set parallelism to 4",true},
            {"parallelism","concurrent","set parallelism to invalid string",true},
            {"version","1-1-1","set version to invalid dash notation, 1-1-1",true},
            {"version","1-1-1","set version to invalid underscore notation, 1_1_1",true},
            {"version","a.b.c","set version to invalid alpha characters, a.b.c",true},
            {"version","1-1-1","set version to invalid mixed underscore notation, 1.1_1",true},
            {"version","1-1-1","set version to invalid mixed dash notation, 1.1-1",true},
            {"helpURI","ut website","set helpURI to invalid url",true},
            //{"tags",Arrays.asList("abcd","abcd"),"set tags to array with duplicate entries",true},
            //{"ontology",Arrays.asList("abcd","abcd"),"set ontology to array with duplicate entries",true},
            {"executionSystem",Boolean.TRUE,"set executionSystem to TRUE",true},
            {"executionSystem",Boolean.FALSE,"set executionSystem to FALSE",true},
            {"executionSystem",new Integer(4),"set executionSystem to 4",true},
            {"executionSystem","ranch","set executionSystem to invalid system",true},
            {"executionType",Boolean.TRUE,"set executionType to TRUE",true},
            {"executionType",Boolean.FALSE,"set executionType to FALSE",true},
            {"executionType",new Integer(4),"set executionType to 4",true},
            {"executionType","ranch","set executionType to invalid system",true},
            {"checkpointable",new Integer(1),"set checkpointable to 1",true},
            {"checkpointable","yes","set checkpointable to yes",true},
            {"checkpointable","no","set checkpointable to no",true},
            {"defaultQueue",Boolean.TRUE,"set executionType to TRUE",true},
            {"defaultQueue",Boolean.FALSE,"set executionType to FALSE",true},
            {"defaultQueue",new Integer(4),"set executionType to 4",true},
            {"defaultQueue","blahblah","set executionType to invalid queue",true},
            {"defaultNodeCount",Boolean.TRUE,"set defaultNodes to TRUE",true},
            {"defaultNodeCount",Boolean.FALSE,"set defaultNodes to FALSE",true},
            {"defaultNodeCount",Long.MAX_VALUE,"set defaultNodes to " + Long.MAX_VALUE,true},
            {"defaultNodeCount",new Long(-1),"defaultNodes cannot be negative",true},
            {"defaultNodeCount",new Long(0),"defaultNodes cannot be 0",true},
            {"defaultNodeCount","blahblah","set defaultNodes to invalid value",true},
            {"defaultMemoryPerNode",Boolean.TRUE,"set defaultMemory to TRUE",true},
            {"defaultMemoryPerNode",Boolean.FALSE,"set defaultMemory to FALSE",true},
            {"defaultMemoryPerNode",Double.MAX_VALUE,"set defaultMemory to " + Double.MAX_VALUE,true},
            {"defaultMemoryPerNode",Double.MIN_VALUE,"set defaultMemory to " + Double.MIN_VALUE,true},
            {"defaultMemoryPerNode","blahblah","set defaultMemory to invalid value",true},
            {"defaultMemoryPerNode",new Long(-1),"defaultNodes cannot be negative",true},
            {"defaultMemoryPerNode",new Long(0),"defaultNodes cannot be 0",true},
            {"defaultProcessorsPerNode",Boolean.TRUE,"set defaultProcessors to TRUE",true},
            {"defaultProcessorsPerNode",Boolean.FALSE,"set defaultProcessors to FALSE",true},
            {"defaultProcessorsPerNode",Long.MAX_VALUE,"set defaultProcessors to " + Long.MAX_VALUE,true},
            {"defaultProcessorsPerNode","blahblah","set defaultProcessors to invalid value",true},
            {"defaultProcessorsPerNode",new Long(-1),"defaultNodes cannot be negative",true},
            {"defaultProcessorsPerNode",new Long(0),"defaultNodes cannot be 0",true},
            {"defaultMaxRunTime",Boolean.TRUE,"set defaultMaxRunTime to TRUE",true},
            {"defaultMaxRunTime",Boolean.FALSE,"set defaultMaxRunTime to FALSE",true},
            {"defaultMaxRunTime",Integer.MAX_VALUE,"set defaultMaxRunTime to " + Integer.MAX_VALUE,true},
            {"defaultMaxRunTime","blahblah","set defaultMaxRunTime to invalid value",true},
            {"defaultMaxRunTime","9999:59:59","defaultMaxRunTime cannot exceed max queue time",true},
            //{"modules",Arrays.asList("abcd","abcd"),"set modules to array with duplicate modules",true},
    };
    
    protected Object[][] dataTestSoftwareFieldsValid = {
    		{"name","abcd","set name to valid string",false},
    		{"parallelism","PARALLEL","set parallelism to PARALLEL",false},
    		{"parallelism","parallel","set parallelism to parallel",false},
    		{"parallelism","PTHREAD","set parallelism to PTHREAD",false},
    		{"parallelism","pthread","set parallelism to pthread",false},
    		{"parallelism","SERIAL","set parallelism to SERIAL",false},
    		{"parallelism","serial","set parallelism to serial",false},
    		{"version","1.1.0","set version to valid version '1.1.0'",false},
            {"helpURI","http://example.com","set helpURI to valid uri 'http://example.com'",false},
            {"label","my label","set label to valid string 'my label'",false},
            {"shortDescription","short description","set shortDescription to valid string 'short description'",false},
            {"longDescription","long description","set longDescription to valid string 'long description'",false},
            {"tags",Arrays.asList("abcd","efgh","ijkl"),"set tags to valid array of string",false},
            //{"ontology",Arrays.asList("abcd","efgh","ijkl"),"set ontology to invalid array of strings",true},
            {"ontology",Arrays.asList("http://example.com","http://www2.example.com","http://api.example.com"),"set ontology to valid array of urls",false},
            {"executionSystem","execute.example.com","set executionSystem to 'execute.example.com'",false},
            {"executionType",ExecutionType.ATMOSPHERE.name(),"set executionType to " + ExecutionType.ATMOSPHERE.name(),true},
            {"executionType",ExecutionType.ATMOSPHERE.name().toLowerCase(),"set executionType to " + ExecutionType.ATMOSPHERE.name().toLowerCase(),true},
            {"executionType",ExecutionType.CLI.name(),"set executionType to " + ExecutionType.CLI.name(),false},
            {"executionType",ExecutionType.CLI.name().toLowerCase(),"set executionType to " + ExecutionType.CLI.name().toLowerCase(),false},
            {"executionType",ExecutionType.CONDOR.name(),"set executionType to " + ExecutionType.CONDOR.name(),true},
            {"executionType",ExecutionType.CONDOR.name().toLowerCase(),"set executionType to " + ExecutionType.CONDOR.name().toLowerCase(),true},
            {"executionType",ExecutionType.HPC.name(),"set executionType to " + ExecutionType.HPC.name(),false},
            {"executionType",ExecutionType.HPC.name().toLowerCase(),"set executionType to " + ExecutionType.HPC.name().toLowerCase(),false},
            {"deploymentPath","/path/to/app","set deploymentPath to '/path/to/app'",false},
            {"templatePath","path/to/wrapper","set templatePath to 'path/to/wrapper'",false},
            {"testPath","path/to/test","set testPath to 'path/to/test'",false},             // requires a string larger than empty
            {"checkpointable",Boolean.TRUE,"set checkpointable to TRUE",false},
            {"checkpointable",Boolean.TRUE,"set checkpointable to FALSE",false},
            {"modules",Arrays.asList("abcd","efgh","ijkl"),"set modules to valid array of strings",false},
            {"defaultQueue","testqueue","set defaultQueue to valid queue",false},
            {"defaultNodeCount",new Integer(1),"set defaultNodes to 1",false},
            {"defaultProcessorsPerNode",new Integer(1),"set defaultProcessorsPerNode to 1",false},
            {"defaultMemoryPerNode","2GB","set defaultMemory to 2GB",false},
            {"defaultMaxRunTime","00:30:00","set defaultMaxRunTime to valid string 00:30:00",false},
    };
    
    

//    // Empty arrays test for JSONArray objects in json message
//    protected Object[][] dataTest3 = {
//            {"tags",new JSONObject(),"set tags to an empty array",true},
//            {"tags",arbitraryArray,"set tags to an array of values",true},
//            {"ontology",new JSONObject(),"set tags to an empty array",true},
//            {"ontology",arbitraryArray,"set ontology to an array of values",true},
//            {"modules",new JSONObject(),"set tags to an empty array",true},
//            {"modules",arbitraryArray,"set modules to an array of values",true}
//    };

    
    /************************************************************************
     * 				Software Input Test Data
     ************************************************************************/

    /*
        The values of each individual array element are
        'json label','value to set on the json label','the message for the test','if this test data should throw an exception'
     */
            // test data with a null value as the change will not work with these Object arrays the corresponding array
            // element just disappears array[1] = null just doesn't exist.
    public Object[][] dataTestInputs = {
            {"id","","set input.id to empty string",true},
            {"id",null,"set input.id to null",true},
            {"value",new JSONObject(),"set input.value to empty object",false},
            {"value",new JSONArray(),"set input.value to empty array",true},
			{"value","","set input.value to empty string",true},
			{"value","abcd","set input.value to random string",true},
            {"value",null,"set input.value to null",false},
            {"details",new JSONObject(),"set input.details to empty object",false},
			{"details",new JSONArray(),"set input.details to empty array",true},
			{"details","","set input.details to empty string",true},
			{"details","abcd","set input.details to random string",true},
            {"details",null,"set input.details to null",false},
			{"semantics",new JSONObject(),"set input.semantics to empty object",false},
			{"semantics",new JSONArray(),"set input.semantics to empty array",true},
			{"semantics","","set input.semantics to empty string",true},
			{"semantics","abcd","set input.semantics to random string",true},
            {"semantics",null,"set input.semantics to null",false},
    };

    public Object[][] dataTestInputsValue = {
            {"default","","set input.value.default to empty string", false},
            {"default",new JSONObject(),"set input.value.default to empty object",true},
            {"default",Arrays.asList(new JSONObject(),new JSONObject(),new JSONObject()),"set input.value.default to array of empty objects",true},
            {"default", null,"set value.default to null",false},
			{"default","abracadabra","set value.default to null on hidden input",false},
			{"default",new Integer(25),"set input.value.default to 25 on hidden input",true},
            {"validator","","set input.value.validator to empty string",false},
            {"validator",new JSONObject(),"set input.value.validator to empty object",true},
            {"validator",Arrays.asList(new JSONObject(),new JSONObject(),new JSONObject()),"set input.value.validator to array of empty objects",true},
            {"validator",new String("/(foo"),"set validator to bad regex string",true},
            {"validator",Arrays.asList(new String("/(foo"),new String("/(foo2"),new String("/(foou")),"set input.value.validator to array of bad regex strings",true},
            {"validator",".+","set validator to valid regex string",false},
            {"validator",Arrays.asList(new String("^(\\/)?(\\/[a-z_\\-\\s0-9\\.]+)+"), new String(".pdf"), new String("[A-Z][a-z]{1,}")),"set input.value.validator to array of valid regex strings",true},
            {"validator",null,"set input.value.validator to null",false},
            
            {"required","","set input.value.required to empty string",true},
            {"required",Boolean.TRUE,"set input.value.required to true",false},
            {"required",Boolean.FALSE,"set input.value.required to false",false},
            {"required",null,"set input.value.required to null",false},
            {"required","hello","set input.value.visible to false",true},
            {"required",new Integer(0),"set input.value.required to 0",false},
            {"required",new Integer(1),"set input.value.required to 1",false},
            {"required",new Integer(-1),"set input.value.required to integer > 2",true},
            {"required",new Integer(-1),"set input.value.required to negative integer",true},
            {"required",new Float(-1.5),"set input.value.required to float",true},
            {"required",new Float(.5),"set input.value.required to float between zero and 1", true},
            
            {"visible","","set input.value.visible to empty string",true},
            {"visible",Boolean.TRUE,"set input.value.visible to true",false},
            {"visible",Boolean.FALSE,"set input.value.visible to false",false},
            {"visible","hello","set input.value.visible to false",true},
            {"visible",null,"set input.value.visible to null",false},
            {"visible",new Integer(0),"set input.value.visible to 0",false},
            {"visible",new Integer(1),"set input.value.visible to 1",false},
            {"visible",new Integer(3),"set input.value.visible to integer > 2",true},
            {"visible",new Integer(-1),"set input.value.visible to negative integer",true},
            {"visible",new Float(-1.5),"set input.value.visible to negative float",true},
            {"visible",new Float(.5),"set input.value.visible to float between zero and 1", true},
            
            {"enquote","","set input.value.enquote to empty string",true},
            {"enquote",Boolean.TRUE,"set input.value.enquote to true",false},
            {"enquote",Boolean.FALSE,"set input.value.enquote to false",false},
            {"enquote",null,"set input.value.enquote to null",false},
            {"enquote","hello","set input.value.visible to false",true},
            {"enquote",new Integer(0),"set input.value.enquote to 0",false},
            {"enquote",new Integer(1),"set input.value.enquote to 1",false},
            {"enquote",new Integer(3),"set input.value.enquote to integer > 2",true},
            {"enquote",new Integer(-1),"set input.value.enquote to negative integer",true},
            {"enquote",new Float(-1.5),"set input.value.enquote to negative float",true},
            {"enquote",new Float(.5),"set input.value.enquote to float between zero and 1", true},
            
            {"order","","set input.semantics.order to empty string should throw exception", true},
            {"order",null,"set input.semantics.order to null should not throw exception", false},
			{"order","abc","set input.semantics.order to non-numeric should throw exception", true},
			{"order","1","set input.semantics.order to numeric string should throw exception", true},
			{"order",new Integer(0),"set input.semantics.order to integer 0 should be allowed", false},
			{"order",new Integer(1),"set input.semantics.order to integer should be allowed", false},
			{"order",new Integer(-1),"set input.semantics.order to negative integer should throw exception", true},
			{"order",new Double(0.0),"set input.semantics.order to float 0.0 should be allowed", false},
			{"order",new Double(1.0),"set input.semantics.order to float 1.0 should be allowed", false},
			{"order",new Double(-1.5),"set input.semantics.order to negative decimal should throw exception", true},
			{"order",new Double(1.5),"set input.semantics.order to positive decimal should throw exception", true},
			{"order",new JSONObject(),"set input.semantics.order to object should throw exception", true},
			{"order",Arrays.asList(),"set input.semantics.order to array should throw exception", true},
	
    };

	public Object[][] dataTestInputsDetails = {
            {"label","","set input.details.label to empty string",false},
            {"label",new JSONObject(),"set input.details.label to empty object",true},
            {"label",Arrays.asList(new JSONObject()),"set input.details.label to list of empty objects",true},
            {"label",null,"set input.details.label to null",false},
            {"label","Sample label","set input.details.label to valid string",false},
            
            {"description","","set input.details.description to empty string",false},
            {"description",new JSONObject(),"set input.details.description to empty object",true},
            {"description",Arrays.asList(new JSONObject()),"set input.details.description to list of empty objects",true},
            {"description",null,"set input.details.description to null",false},
            {"description","Sample description","set input.details.description to valid string",false},
            
            {"argument","","input.details.argument can be an empty string",false},
            {"argument",new JSONObject(),"set input.details.argument to empty object",true},
            {"argument",Arrays.asList(new JSONObject()),"set input.details.argument to list of empty objects should throw exception",true},
            {"argument",Arrays.asList("foo", "bar"),"set input.details.argument to list of strings should throw exception",true},
            {"argument",null,"set input.details.argument to null",false},
            {"argument","Sample argument","set input.details.argument to valid string",false},
            {"argument",new Long(1),"set parameter.value.argument to integer",true},
            {"argument",new Float(1.5),"set parameter.value.argument to float",true},
            {"argument",Boolean.TRUE,"set parameter.value.argument to true",true},
            {"argument",Boolean.FALSE,"set parameter.value.argument to false",true},
            
            {"showArgument","","set input.details.showArgument to empty string should throw exception",true},
            {"showArgument",new JSONObject(),"set input.details.showArgument to empty object should throw exception",true},
            {"showArgument",Arrays.asList(new JSONObject()),"set input.details.showArgument to list of empty objects should throw exception",true},
            // todo {"showArgument",null,"set input.details.showArgument to null",true},
            {"showArgument","Sample argument","set input.details.showArgument to valid string should throw exception",true},
            {"showArgument",Boolean.TRUE,"set parameter.value.showArgument to true should not throw exception",false},
            {"showArgument",Boolean.FALSE,"set parameter.value.showArgument to false should not throw exception",false},
            {"showArgument",new Integer(0),"set parameter.value.showArgument to 0 should not throw exception",false},
            {"showArgument",new Integer(1),"set parameter.value.showArgument to 1 should not throw exception",false},
            {"showArgument",new Integer(-1),"set parameter.value.showArgument to integer > 2 should throw exception",true},
            {"showArgument",new Integer(-1),"set parameter.value.showArgument to negative integer should throw exception",true},
            {"showArgument",new Float(-1.5),"set parameter.value.showArgument to float should throw exception",true},
            {"showArgument",new Float(.5),"set parameter.value.showArgument to float between zero and 1 should throw exception",true},
            
            {"repeatArgument","","set input.details.repeatArgument to empty string",true},
            {"repeatArgument",new JSONObject(),"set input.details.repeatArgument to empty object",true},
            {"repeatArgument",Arrays.asList(new JSONObject()),"set input.details.repeatArgument to list of empty objects",true},
            // todo {"repeatArgument",null,"set input.details.repeatArgument to null",true},
            {"repeatArgument","Sample argument","set parameter.details.repeatArgument to valid string",true},
            {"repeatArgument",Boolean.TRUE,"set input.details.repeatArgument to true",false},
            {"repeatArgument",Boolean.FALSE,"set input.details.repeatArgument to false",false},
            {"repeatArgument",new Integer(0),"set input.details.repeatArgument to 0",false},
            {"repeatArgument",new Integer(1),"set input.details.repeatArgument to 1",false},
            {"repeatArgument",new Integer(-1),"set input.details.repeatArgument to integer > 2",true},
            {"repeatArgument",new Integer(-1),"set input.details.repeatArgument to negative integer",true},
            {"repeatArgument",new Float(-1.5),"set input.details.repeatArgument to float",true},
            {"repeatArgument",new Float(.5),"set input.details.repeatArgument to float between zero and 1",true},
            
    };
	
    public Object[][] dataTestHiddenInputsValue = {
			{"default","","set input.value.default to empty string on hidden input",true},
			{"default",null,"set input.value.default to null on hidden input",true},
			{"default","abracadabra","set input.value.default to random string on hidden input",false},
			{"default",new Integer(25),"set input.value.default to null on hidden input",true},
	};

	public Object[][] dataTestInputsSemantics = {
			{"ontology","","set input.semantics.ontology to empty object",true},
			{"ontology",new JSONObject(),"set input.semantics.ontology to empty object",true},
            {"ontology",Arrays.asList(new JSONObject()),"set input.semantics.ontology to empty list of objects",true},
            {"ontology","abcdef","set input.semantics.ontology to valid string",true},
            {"ontology",Arrays.asList("abcdef","abcdef","abcdef"),"set input.semantics.ontology to valid list of strings",false},
            {"ontology",null,"set input.semantics.ontology to null",false},
            
            {"minCardinality","","set input.semantics.minCardinality to empty string should throw exception", true},
            {"minCardinality",null,"set input.semantics.minCardinality to null should not throw exception", false},
			{"minCardinality","abc","set input.semantics.minCardinality to non-numeric should throw exception", true},
			{"minCardinality","1","set input.semantics.minCardinality to numeric string should throw exception", true},
			{"minCardinality",new Integer(0),"set input.semantics.minCardinality to integer 0 should be allowed", false},
			{"minCardinality",new Integer(1),"set input.semantics.minCardinality to integer should be allowed", false},
			{"minCardinality",new Integer(-1),"set input.semantics.minCardinality to negative integer should throw exception", true},
			{"minCardinality",new Double(0.0),"set input.semantics.minCardinality to float 0 should be allowed", false},
			{"minCardinality",new Double(1.0),"set input.semantics.minCardinality to float 1 should be allowed", false},
			{"minCardinality",new Double(-1.5),"set input.semantics.minCardinality to negative decimal should throw exception", true},
			{"minCardinality",new Float(1.5),"set input.semantics.minCardinality to positive decimal should throw exception", true},
			{"minCardinality",new JSONObject(),"set input.semantics.minCardinality to object should throw exception", true},
			{"minCardinality",Arrays.asList(),"set input.semantics.minCardinality to array should throw exception", true},
			
			{"maxCardinality","","set input.semantics.maxCardinality to empty string should throw exception", true},
            {"maxCardinality",null,"set input.semantics.maxCardinality to null should not throw exception", false},
			{"maxCardinality","abc","set input.semantics.maxCardinality to non-numeric should throw exception", true},
			{"maxCardinality","1","set input.semantics.maxCardinality to numeric string should throw exception", true},
			{"maxCardinality",new Integer(0),"set input.semantics.maxCardinality cannot be zero", true},
			{"maxCardinality",new Integer(1),"set input.semantics.maxCardinality to integer should be allowed", false},
			{"maxCardinality",new Double(0.0),"set input.semantics.maxCardinality to float 0 should throw exception", true},
			{"maxCardinality",new Double(1.0),"set input.semantics.maxCardinality to float 1 should be allowed", false},
			{"maxCardinality",new Integer(-1),"set input.semantics.maxCardinality to -1 is allowed", false},
			{"maxCardinality",new Float(-2),"set input.semantics.maxCardinality to negative integer less than -1 should throw exception", true},
			{"maxCardinality",new Float(-1.5),"set input.semantics.maxCardinality to negative decimal should throw exception", true},
			{"maxCardinality",new Float(1.5),"set input.semantics.maxCardinality to positive decimal should throw exception", true},
			{"maxCardinality",new JSONObject(),"set input.semantics.maxCardinality to object should throw exception", true},
			{"maxCardinality",Arrays.asList(),"set input.semantics.maxCardinality to array should throw exception", true},
			
			{"fileTypes",new JSONObject(),"set input.semantics.fileTypes to empty object",true}, // not supported on inputs
            {"fileTypes",null,"set input.semantics.fileTypes to null",false},
            {"fileTypes","","set input.semantics.fileTypes to empty string",true},
            {"fileTypes","abcdf","set input.semantics.fileTypes to valid string",true},
            {"fileTypes",Arrays.asList("abcdef","abcdef","abcdef"),"set input.semantics.fileTypes to list of strings",false},
            {"fileTypes",Arrays.asList(new JSONObject()),"set input.semantics.fileTypes to list of objects",true},
    }; 

	/************************************************************************
     * 				Software Argument Test Data
     ************************************************************************/

    protected Object[][] softwareArgumentDefaultValueData = {
            {"","set SoftwareArgument defaultValue to empty string",true},
            {null,"set SoftwareArgument defaultValue to null",true},
            {1,"set SoftwareArgument defaultValue to int 1",true},
            {"test","set SoftwareArgument defaultValue to string test",true},
            {new JSONObject(),"set SoftwareArgument defaultValue to empty object",true},
            {Arrays.asList(new JSONObject()),"set SoftwareArgument defaultValue to empty list of objects",true},
    };

    protected Object[][] softwareArgumentIdData = {
            {"","set default to empty string",true},
            {null,"set default to null",true},
            {"test","set default to string test",false},
            {Integer.valueOf(25),"set default to 25",false},
    };
	
	
	protected Object[][] softwareArgumentValueData = {
            {"","set value to empty string",true},
            {null,"set default to null",true},
            {"test","set default to string test",true},
            {Integer.valueOf(25),"set default to 25",true},
    };
	
    protected Object[][] softwareArgumentSemanticsData = {
			{"","set semantics to empty string",true},
			{new JSONObject(),"set semantics to empty object",false},
            {Arrays.asList(new JSONObject()),"set semantics.ontology to empty list of objects",true},
            {"abcdef","set semantics to valid string",true},
            {Arrays.asList("abcdef","abcdef","abcdef"),"set semantics to valid list of strings",true},
            {null,"set semantics to null",false},
           
	};
    
    /************************************************************************
     * 				Software Parameter Test Data
     ************************************************************************/

    public Object[][] dataTestParameter = {
            {"id","","set parameter.id to empty string should throw exception",true},
            {"id",null,"set parameter.id to null should throw exception",true},
            {"id",new Float(1.0),"set parameter.id to decimal should throw exception",true},
            {"id",new Integer(1),"set parameter.id to integer should throw exception",true},
            {"id",new ArrayList<String>(),"set parameter.id to array should throw exception",true},
            {"value",new JSONObject(),"set parameter.value to empty object",true},
            {"value",new ArrayList<String>(),"set parameter.value to empty array",true},
			{"value","","set parameter.value to empty string",true},
			{"value","abcd","set parameter.value to random string",true},
            {"value",null,"set parameter.value to null",true},
            {"details",new JSONObject(),"set parameter.details to empty object",false},
			{"details",new ArrayList<String>(),"set parameter.details to empty array",true},
			{"details","","set parameter.details to empty string",true},
			{"detaiinput.detailsarameter.details to random string",true},
            {"details",null,"set parameter.details to null",false},
			{"semantics",new JSONObject(),"set parameter.semantics to empty object",false},
			{"semantics",new JSONArray(),"set parameter.semantics to empty array",true},
			{"semantics","","set parameter.semantics to empty string",true},
			{"semantics","abcd","set parameter.semantics to random string",true},
            {"semantics",null,"set parameter.semantics to null",false},
    };

    public Object[][] dataTestParameterValue = {
            {"default","","parameter.value.default set to empty string should not throw exception",false},
            {"default",Arrays.asList("foo","bar","bat"),"parameter.value.default set to array of strings should not throw exception",false},
            {"default",new JSONObject(),"parameter.value.default set to object should throw exception",true},
            {"default",Arrays.asList(new JSONObject(),new JSONObject(),new JSONObject()),"parameter.value.default set to array of objects should throw exception",true},
            {"default",null,"parameter.value.default set to null should not throw exception",false},
            
			{"validator","","set parameter.value.validator to empty string should not throw exception",false},
            {"validator",new JSONObject(),"set parameter.value.validator to empty object should throw exception",true},
            {"validator",Arrays.asList(),"set parameter.value.validator to empty array should throw exception",true},
            {"validator",Arrays.asList(new JSONObject(),new JSONObject(),new JSONObject()),"set parameter.value.validator to array of empty objects should throw exception",true},
            {"validator",new String("/(foo"),"set validator to bad regex string should throw exception",true},
            {"validator",Arrays.asList(new String("/(foo"),new String("/(foo2"),new String("/(foou")),"set parameter.value.validator to array of bad regex strings should throw exception",true},
            {"validator",".+","set validator to valid regex string should not throw exception",false},
            {"validator",Arrays.asList(new String(".+"), new String(".pdf"), new String("[A-Z][a-z]{1,}")),"set parameter.value.validator to array of valid regex strings should throw exception",true},
            {"validator",null,"set parameter.value.validator to null should not throw exception",false},
            
            {"required","","set parameter.value.required to empty string should throw exception",true},
            {"required",Boolean.TRUE,"set parameter.value.required to true should not throw exception",false},
            {"required",Boolean.FALSE,"set parameter.value.required to false should not throw exception",false},
            {"required",null,"set parameter.value.required to null should not throw exception",false},
            {"required","hello","set input.value.required to string should throw exception",true},
            {"required",new Integer(0),"set parameter.value.required to 0 should not throw exception",false},
            {"required",new Integer(1),"set parameter.value.required to 1 should not throw exception",false},
            {"required",new Integer(-1),"set parameter.value.required to integer > 2 should throw exception",true},
            {"required",new Integer(-1),"set parameter.value.required to negative integer should throw exception",true},
            {"required",new Float(-1.5),"set parameter.value.required to negative float should throw exception",true},
            {"required",new Float(.5),"set input.value.required to float between zero and 1 should throw exception", true},
            
            {"visible","","set parameter.value.visible to empty string should throw exception",true},
            {"visible",Boolean.TRUE,"set parameter.value.visible to true should not throw exception",false},
            {"visible",Boolean.FALSE,"set parameter.value.visible to false should not throw exception",false},
            {"visible",null,"set parameter.value.visible to null should not throw exception",false},
            {"visible","hello","set input.value.visible to string should throw exception",true},
            {"visible",new Integer(0),"set parameter.value.visible to 0 should not throw exception",false},
            {"visible",new Integer(1),"set parameter.value.visible to 1 should not throw exception",false},
            {"visible",new Integer(3),"set parameter.value.visible to integer > 2 should throw exception",true},
            {"visible",new Integer(-1),"set parameter.value.visible to negative integer should throw exception",true},
            {"visible",new Float(-1.5),"set parameter.value.visible to negative float should throw exception",true},
            {"visible",new Float(.5),"set parameter.value.visible to float between zero and 1 should throw exception", true},
            
            {"enquote","","set parameter.value.enquote to empty string",true},
            {"enquote",Boolean.TRUE,"set parameter.value.enquote to true",false},
            {"enquote",Boolean.FALSE,"set parameter.value.enquote to false",false},
            {"enquote",null,"set parameter.value.enquote to null",false},
            {"enquote","hello","set parameter.value.visible to false",true},
            {"enquote",new Integer(0),"set parameter.value.enquote to 0",false},
            {"enquote",new Integer(1),"set parameter.value.enquote to 1",false},
            {"enquote",new Integer(3),"set parameter.value.enquote to integer > 2",true},
            {"enquote",new Integer(-1),"set parameter.value.enquote to negative integer",true},
            {"enquote",new Float(-1.5),"set parameter.value.enquote to negative float",true},
            {"enquote",new Float(.5),"set parameter.value.enquote to float between zero and 1", true},
            
            {"order","","set parameter.semantics.order to empty string should throw exception", true},
            {"order",null,"set parameter.semantics.order to null should not throw exception", false},
			{"order","abc","set parameter.semantics.order to non-numeric should throw exception", true},
			{"order","1","set parameter.semantics.order to numeric string should throw exception", true},
			{"order",new Integer(0),"set parameter.semantics.order to integer 0 should be allowed", false},
			{"order",new Integer(1),"set parameter.semantics.order to integer should be allowed", false},
			{"order",new Integer(-1),"set parameter.semantics.order to negative integer should throw exception", true},
			{"order",new Double(0.0),"set parameter.semantics.order to float 0.0 should be allowed", false},
			{"order",new Double(1.0),"set parameter.semantics.order to float 1.0 should be allowed", false},
			{"order",new Double(-1.5),"set parameter.semantics.order to negative decimal should throw exception", true},
			{"order",new Double(1.5),"set parameter.semantics.order to positive decimal should throw exception", true},
			{"order",new JSONObject(),"set parameter.semantics.order to object should throw exception", true},
			{"order",Arrays.asList(),"set parameter.semantics.order to array should throw exception", true},
    };

    public Object[][] dataTestHiddenParameterValue = {
			{"default","","set parameter.value.default to empty string on hidden input",true},
			{"default",null,"set parameter.value.default to null on hidden input",true},
			{"default","abracadabra","set parameter.value.default to random string on hidden input",false},
			{"default",new Integer(25),"set parameter.value.default to null on hidden input",true},
	};

	public Object[][] dataTestParameterSemanticsOntology = {
			{"ontology","","set parameter.semantics.ontology to empty object",true},
			{"ontology",new JSONObject(),"set parameter.semantics.ontology to empty object",true},
            {"ontology",Arrays.asList(new JSONObject()),"set parameter.semantics.ontology to empty list of objects",true},
            {"ontology","abcdef","set parameter.semantics.ontology to valid string",true},
            {"ontology",Arrays.asList("abcdef","abcdef","abcdef"),"set parameter.semantics.ontology to valid list of strings",false},
            {"ontology",null,"set parameter.semantics.ontology to null",false},
	};
	public Object[][] dataTestParameterSemanticsFileTypes = {
			{"fileTypes",new JSONObject(),"set parameter.semantics.fileTypes to empty array",true},
            {"fileTypes",null,"set parameter.semantics.fileTypes to null",true},
            {"fileTypes","","set parameter.semantics.fileTypes to empty string",true},
            {"fileTypes","abcdf","set parameter.semantics.fileTypes to valid string",true},
            {"fileTypes",Arrays.asList("abcdef","abcdef","abcdef"),"set parameter.semantics.fileTypes to list of strings",true},
            {"fileTypes",Arrays.asList(new JSONObject()),"set parameter.semantics.fileTypes to list of objects",true},
    };

   
    
    /************************************************************************
     * 				Software Output Test Data
     ************************************************************************/

    public Object[][] dataTestOutput = {
            {"id","","set output.id to empty string",true},
            {"id","{}","set output.id to empty string resembling an object should throw an exception",false},
            {"id",new Float(1.0),"set output.id to decimal should throw exception",true},
            {"id",new Integer(1),"set output.id to integer should throw exception",true},
            {"id",new JSONObject(),"set output.id to empty object should throw exception",true},
            {"id",Arrays.asList(),"set output.id to empty array should throw exception",true},
            {"id",Arrays.asList(new JSONObject()),"set output.id to array with json object should throw exception",true},
            {"id",null,"set output.id to null should throw exception",true},
            {"value",new JSONObject(),"set output.value to empty object",false},
            {"value",new JSONArray(),"set output.value to empty array",true},
            {"value",new Float(1.0),"set output.value to decimal should throw exception",true},
            {"value",new Integer(1),"set output.value to integer should throw exception",true},
            {"value","","set output.value to empty string",true},
			{"value","abcd","set output.value to random string",true},
            {"value",null,"set output.value to null",false},
            {"details",new JSONObject(),"set output.details to empty object",false},
			{"details",new JSONArray(),"set output.details to empty array",true},
			{"details",new Float(1.0),"set output.details to decimal should throw exception",true},
            {"details",new Integer(1),"set output.details to integer should throw exception",true},
            {"details","","set output.details to empty string",true},
			{"details","abcd","set output.details to random string",true},
            {"details",null,"set output.details to null",false},
			{"semantics",new JSONObject(),"set output.semantics to empty object",false},
			{"semantics",new JSONArray(),"set output.semantics to empty array",true},
			{"semantics",new Float(1.0),"set output.semantics to decimal should throw exception",true},
            {"semantics",new Integer(1),"set output.semantics to integer should throw exception",true},
            {"semantics","","set output.semantics to empty string",true},
			{"semantics","abcd","set output.semantics to random string",true},
            {"semantics",null,"set output.semantics to null",false},
    };

    public  Object[][] dataTestOutputValue = {
    		{"default","","set output.value.default to empty string", false},
            {"default",new JSONObject(),"set output.value.default to empty object",true},
            {"default",Arrays.asList(new JSONObject(),new JSONObject(),new JSONObject()),"set output.value.default to array of empty objects",true},
            {"default", null,"set value.default to null",false},
			{"default","abracadabra","set value.default to null on hidden output",false},
			{"default",new Integer(25),"set output.value.default to 25 on hidden output",true},
			
            {"validator","","set output.value.validator to empty string",false},
            {"validator",new JSONObject(),"set output.value.validator to empty object",true},
            {"validator",Arrays.asList(new JSONObject(),new JSONObject(),new JSONObject()),"set output.value.validator to array of empty objects",true},
            {"validator",new String("/(foo"),"set validator to bad regex string",true},
            {"validator",Arrays.asList(new String("/(foo"),new String("/(foo2"),new String("/(foou")),"set output.value.validator to array of bad regex strings",true},
            {"validator",".+","set validator to valid regex string",false},
            {"validator",Arrays.asList(new String("^(\\/)?(\\/[a-z_\\-\\s0-9\\.]+)+"), new String(".pdf"), new String("[A-Z][a-z]{1,}")),"set output.value.validator to array of valid regex strings",true},
            {"validator",null,"set output.value.validator to null",false},
            
            {"order","","set output.semantics.order to empty string should throw exception", true},
            {"order",null,"set output.semantics.order to null should not throw exception", false},
			{"order","abc","set output.semantics.order to non-numeric should throw exception", true},
			{"order","1","set output.semantics.order to numeric string should throw exception", true},
			{"order",new Integer(0),"set output.semantics.order to integer 0 should be allowed", false},
			{"order",new Integer(1),"set output.semantics.order to integer should be allowed", false},
			{"order",new Integer(-1),"set output.semantics.order to negative integer should throw exception", true},
			{"order",new Double(0.0),"set output.semantics.order to float 0.0 should be allowed", false},
			{"order",new Double(1.0),"set output.semantics.order to float 1.0 should be allowed", false},
			{"order",new Double(-1.5),"set output.semantics.order to negative decimal should throw exception", true},
			{"order",new Double(1.5),"set output.semantics.order to positive decimal should throw exception", true},
			{"order",new JSONObject(),"set output.semantics.order to object should throw exception", true},
			{"order",Arrays.asList(),"set output.semantics.order to array should throw exception", true},
    };

    public Object[][] dataTestOutputDetails = {
            {"label","","set output.details.label to empty string",false},
            {"label",null,"set output.details.label to null",false},
            {"label","Sample label","set output.details.label to null",false},
            {"description","","set output.details.description to empty string",false},
            {"description",null,"set output.details.description to null",false},
            {"description","Sample description.","set output.details.description to null",false},
            {"visible","","set output.details.visible to empty string",false},
            {"visible",null,"set output.details.visible to null",false},
            {"visible",true,"set output.details.visible to true",false},
            {"visible",false,"set output.details.visible to false",false},
            {"visible","alpha","set output.details.visible to 'alpha'",false},
            {"visible",new Integer(1),"set output.details.visible to 1",false},
    };

    public Object[][] dataTestOutputSemantics = {
    		{"ontology","","set output.semantics.ontology to empty object",true},
			{"ontology",new JSONObject(),"set output.semantics.ontology to empty object",true},
            {"ontology",Arrays.asList(new JSONObject()),"set output.semantics.ontology to empty list of objects",true},
            {"ontology","abcdef","set output.semantics.ontology to valid string",true},
            {"ontology",Arrays.asList("abcdef","abcdef","abcdef"),"set output.semantics.ontology to valid list of strings",false},
            {"ontology",null,"set output.semantics.ontology to null",false},
            
            {"minCardinality","","set output.semantics.minCardinality to empty string should throw exception", true},
            {"minCardinality",null,"set output.semantics.minCardinality to null should not throw exception", false},
			{"minCardinality","abc","set output.semantics.minCardinality to non-numeric should throw exception", true},
			{"minCardinality","1","set output.semantics.minCardinality to numeric string should throw exception", true},
			{"minCardinality",new Integer(0),"set output.semantics.minCardinality to integer 0 should be allowed", false},
			{"minCardinality",new Integer(1),"set output.semantics.minCardinality to integer should be allowed", false},
			{"minCardinality",new Integer(-1),"set output.semantics.minCardinality to negative integer should throw exception", true},
			{"minCardinality",new Double(0.0),"set output.semantics.minCardinality to float 0 should be allowed", false},
			{"minCardinality",new Double(1.0),"set output.semantics.minCardinality to float 1 should be allowed", false},
			{"minCardinality",new Double(-1.5),"set output.semantics.minCardinality to negative decimal should throw exception", true},
			{"minCardinality",new Float(1.5),"set output.semantics.minCardinality to positive decimal should throw exception", true},
			{"minCardinality",new JSONObject(),"set output.semantics.minCardinality to object should throw exception", true},
			{"minCardinality",Arrays.asList(),"set output.semantics.minCardinality to array should throw exception", true},
			
			{"maxCardinality","","set output.semantics.maxCardinality to empty string should throw exception", true},
            {"maxCardinality",null,"set output.semantics.maxCardinality to null should not throw exception", false},
			{"maxCardinality","abc","set output.semantics.maxCardinality to non-numeric should throw exception", true},
			{"maxCardinality","1","set output.semantics.maxCardinality to numeric string should throw exception", true},
			{"maxCardinality",new Integer(0),"set output.semantics.maxCardinality cannot be zero", true},
			{"maxCardinality",new Integer(1),"set output.semantics.maxCardinality to integer should be allowed", false},
			{"maxCardinality",new Double(0.0),"set output.semantics.maxCardinality to float 0 should throw exception", true},
			{"maxCardinality",new Double(1.0),"set output.semantics.maxCardinality to float 1 should be allowed", false},
			{"maxCardinality",new Integer(-1),"set output.semantics.maxCardinality to -1 is allowed", false},
			{"maxCardinality",new Float(-2),"set output.semantics.maxCardinality to negative integer less than -1 should throw exception", true},
			{"maxCardinality",new Float(-1.5),"set output.semantics.maxCardinality to negative decimal should throw exception", true},
			{"maxCardinality",new Float(1.5),"set output.semantics.maxCardinality to positive decimal should throw exception", true},
			{"maxCardinality",new JSONObject(),"set output.semantics.maxCardinality to object should throw exception", true},
			{"maxCardinality",Arrays.asList(),"set output.semantics.maxCardinality to array should throw exception", true},
			
			{"fileTypes",new JSONObject(),"set output.semantics.fileTypes to empty object",true}, // not supported on outputs
            {"fileTypes",null,"set output.semantics.fileTypes to null",false},
            {"fileTypes","","set output.semantics.fileTypes to empty string",true},
            {"fileTypes","abcdf","set output.semantics.fileTypes to valid string",true},
            {"fileTypes",Arrays.asList("abcdef","abcdef","abcdef"),"set output.semantics.fileTypes to list of strings",false},
            {"fileTypes",Arrays.asList(new JSONObject()),"set output.semantics.fileTypes to list of objects",true},
    };


    private JSONTestDataUtil()
    {
    	init();
    }
    
    public static JSONTestDataUtil getInstance() 
    {
    	if (jsonTestData == null) {
    		jsonTestData = new JSONTestDataUtil();
    	}
    	
    	return jsonTestData;
    }
    
    private void init() {}
    
    public static <T> T[] concatAll(T[] first, T[]... rest) {
    	int totalLength = first.length;
    	for (T[] array : rest) {
    		totalLength += array.length;
    	}
    	T[] result = Arrays.copyOf(first, totalLength);
    	int offset = first.length;
    	for (T[] array : rest) {
    		System.arraycopy(array, 0, result, offset, array.length);
    		offset += array.length;
    	}
    	return result;
	}

/**
 *
 * @return ArrayList of json text strings each in an array, needed as input to the fromJSON method
 */
    public Object[][] genJsonTestMessages() {
        // String name, String changeValue, String message, String expected
        List<Object[]> holder = new ArrayList<Object[]>();

        Collections.addAll(holder, dataTestSoftwareFieldsValid);
        Collections.addAll(holder, dataTestSoftwareFieldsInvalid);
        Collections.addAll(holder, dataTestSoftwareFieldsEmpty);
        Collections.addAll(holder, dataTestSoftwareFieldsEmptyObject);
        Collections.addAll(holder, dataTestSoftwareFieldsNull);
        Collections.addAll(holder, dataTestSoftwareFieldsArray);

        Object[][] result = holder.toArray(new Object[holder.size()][]);
        return result;
    }


}




/*
            // create a new base JSONObject message from base text
            def jsonMsgObjTemp = getBaseJsonObjFromText()
            // apply the change (index 1) to the label(index 0) in  base json message
            jsonMsgObjTemp.put(valueSet{0},valueSet{1})
            // swap out the current valueSet{2} value with the new changed json message
            valueSet{2} = jsonMsgObjTemp
*/

/*
this.dataTest1.each { valueSet ->
    // create a new JsonTestDataObject from valueSet
    def testObject = new JsonTestDataObject(valueSet)
    // turn the array of values into a JsonTestDataObject
    valueSet = testObject
}
*/

//
//    protected Object[][] dataTestParameterStringTypesValue = {
//    		{SoftwareArgumentValueType.string,"default",Boolean.TRUE,"set value.default with value.type bool to boolean true",true},
//    		{SoftwareArgumentValueType.string,"default",Boolean.FALSE,"set value.default with value.type bool to boolean false",true},
//    		{SoftwareArgumentValueType.string,"default","abracadabra","set value.default with value.type string to valid string",false},
//		    {SoftwareArgumentValueType.string,"default","","set value.default with value.type string to empty string",false},
//		    {SoftwareArgumentValueType.string,"default",null,"set value.default with value.type string to valid string",false},
//		    {SoftwareArgumentValueType.string,"default",new Integer(4),"set value.default with value.type string to integer",true},
//		    {SoftwareArgumentValueType.string,"default",new Float(4.0),"set value.default with value.type string to float",true}
//    };
//
//    protected Object[][] dataTestParameterFileTypesValue = {
//    		{SoftwareArgumentValueType.file,"default",Boolean.TRUE,"set value.default with value.type bool to boolean true",true},
//    		{SoftwareArgumentValueType.file,"default",Boolean.FALSE,"set value.default with value.type bool to boolean false",true},
//    		{SoftwareArgumentValueType.file,"default","abracadabra","set value.default with value.type file to valid string",false},
//		    {SoftwareArgumentValueType.file,"default","","set value.default with value.type file to empty string",false},
//		    {SoftwareArgumentValueType.file,"default",null,"set value.default with value.type file to valid string",false},
//		    {SoftwareArgumentValueType.file,"default",new Integer(4),"set value.default with value.type file to integer",true},
//		    {SoftwareArgumentValueType.file,"default",new Float(4.0),"set value.default with value.type file to float",true},
//    };
//
//    protected Object[][] dataTestParameterBoolTypesValue = {
//    		{SoftwareArgumentValueType.bool,"default",Boolean.TRUE,"set value.default with value.type bool to boolean true",false},
//    		{SoftwareArgumentValueType.bool,"default",Boolean.FALSE,"set value.default with value.type bool to boolean false",false},
//    		{SoftwareArgumentValueType.bool,"default","","set value.default with value.type bool to empty string",false},
//		    {SoftwareArgumentValueType.bool,"default",null,"set value.default with value.type bool to valid string",false},
//		    {SoftwareArgumentValueType.bool,"default","abracadabra","set value.default with value.type bool to valid string",true},
//		    {SoftwareArgumentValueType.bool,"default",new Integer(4),"set value.default with value.type bool to integer",true},
//		    {SoftwareArgumentValueType.bool,"default",new Float(4.0),"set value.default with value.type bool to float",true},
//    };
//
//    protected Object[][] dataTestParameterNumericTypesValue = {
//    		{SoftwareArgumentValueType.number,"default",Boolean.TRUE,"set value.default with value.type number to boolean true",true},
//    		{SoftwareArgumentValueType.number,"default",Boolean.FALSE,"set value.default with value.type number to boolean false",true},
//    		{SoftwareArgumentValueType.number,"default","","set value.default with value.type number to empty string",false},
//		    {SoftwareArgumentValueType.number,"default",null,"set value.default with value.type number to valid string",false},
//		    {SoftwareArgumentValueType.number,"default","abracadabra","set value.default with value.type number to valid string",true},
//		    {SoftwareArgumentValueType.number,"default",new Integer(4),"set value.default with value.type number to integer",false},
//		    {SoftwareArgumentValueType.number,"default",new Float(4.0),"set value.default with value.type number to float",false},
//    };
//
//    protected Object[][] dataTestParameterIntegerTypesValue = {
//    		{SoftwareArgumentValueType.integer,"default",Boolean.TRUE,"set value.default with value.type integer to boolean true",true},
//    		{SoftwareArgumentValueType.integer,"default",Boolean.FALSE,"set value.default with value.type integer to boolean false",true},
//    		{SoftwareArgumentValueType.integer,"default","","set value.default with value.type integer to empty string",false},
//		    {SoftwareArgumentValueType.integer,"default",null,"set value.default with value.type integer to valid string",false},
//		    {SoftwareArgumentValueType.integer,"default","abracadabra","set value.default with value.type integer to valid string",true},
//		    {SoftwareArgumentValueType.integer,"default",new Integer(4),"set value.default with value.type integer to integer",false},
//		    {SoftwareArgumentValueType.integer,"default",new Float(4.0),"set value.default with value.type integer to float",true},
//    };
//
//    protected Object[][] dataTestParameterDecimalTypesValue = {
//    		{SoftwareArgumentValueType.decimal,"default",Boolean.TRUE,"set value.default with value.type decimal to boolean true",true},
//    		{SoftwareArgumentValueType.decimal,"default",Boolean.FALSE,"set value.default with value.type decimal to boolean false",true},
//    		{SoftwareArgumentValueType.decimal,"default","","set value.default with value.type decimal to empty string",false},
//		    {SoftwareArgumentValueType.decimal,"default",null,"set value.default with value.type decimal to valid string",false},
//		    {SoftwareArgumentValueType.decimal,"default","abracadabra","set value.default with value.type decimal to valid string",true},
//		    {SoftwareArgumentValueType.decimal,"default",new Integer(4),"set value.default with value.type decimal to integer",true},
//		    {SoftwareArgumentValueType.decimal,"default",new Float(4.0),"set value.default with value.type decimal to float",false},
//    };
//
//    protected Object[][] dataTestParameterFlagTypesValue = {
//    		{SoftwareArgumentValueType.flag,"default",Boolean.TRUE,"set value.default with value.type decimal to boolean true",true},
//    		{SoftwareArgumentValueType.flag,"default",Boolean.FALSE,"set value.default with value.type decimal to boolean false",true},
//    		{SoftwareArgumentValueType.flag,"default","","set value.default with value.type decimal to empty string",true},
//		    {SoftwareArgumentValueType.flag,"default",null,"set value.default with value.type decimal to valid string",true},
//		    {SoftwareArgumentValueType.flag,"default","abracadabra","set value.default with value.type decimal to valid string",true},
//		    {SoftwareArgumentValueType.flag,"default",new Integer(4),"set value.default with value.type decimal to integer",true},
//		    {SoftwareArgumentValueType.flag,"default",new Float(4.0),"set value.default with value.type decimal to float",true},
//    };
//

/************************************************************************
 * 				Software Output Test Data
 ************************************************************************/


//    protected Object[][] dataTestOutput = {
//            {"id","","set output.id to empty string",true},
//            {"id",null,"set output.id to null",true},
//            {"value","{}","set output.value to empty array",true},
//            {"value",null,"set output.value to null",true},
//            {"details","{}","set output.details to empty array",true},
//            {"details",null,"set output.details to null",true},
//            {"semantics","{}","set output.semantics to empty array",true},
//            {"semantics",null,"set output.semantics to null",true},
//    };
//
//    protected  Object[][] dataTestOutputValue = {
//            {"default","","set output.value.default to empty string",true},
//            {"default",null,"set output.value.default to null",true},
//            {"validator","","set output.value.validator to empty string",true},
//            {"validator",null,"set output.value.validator to null",true},
//            {"required","","set output.value.required to empty string",true},
//            {"required",null,"set output.value.required to null",true},
//            {"required","abcd","set output.value.required to \"abcd\"",true},
//            {"required","true","set output.value.required to \"true\"",true},
//            {"required","false","set output.value.required to \"false\"",true},
//            {"required",true,"set output.value.required to true",false},
//            {"required",false,"set output.value.required to false",false},
//    };
//
//    protected Object[][] dataTestOutputDetails = {
//            {"label","","set output.details.label to empty string",true},
//            {"label",null,"set output.details.label to null",true},
//            {"label","Sample label","set output.details.label to null",true},
//            {"description","","set output.details.description to empty string",true},
//            {"description",null,"set output.details.description to null",true},
//            {"description","Sample description.","set output.details.description to null",true},
//            {"visible","","set output.details.visible to empty string",true},
//            {"visible",null,"set output.details.visible to null",true},
//            {"visible",true,"set output.details.visible to true",true},
//            {"visible",false,"set output.details.visible to false",true},
//            {"visible","alpha","set output.details.visible to 'alpha'",true},
//            {"visible",new Integer(1),"set output.details.visible to 1",true},
//    };
//
//    protected Object[][] dataTestOutputSemantics = {
//            {"ontology","{}","set ontology to empty array",true},
//            {"ontology",null,"set ontology to null",true},
//            {"minCardinality","","set minCardinality to empty string",true},
//            {"minCardinality",null,"set minCardinality to null",true},
//            {"maxCardinality","","set maxCardinality to empty string",true},
//            {"maxCardinality",null,"set maxCardinality to null",true},
//            {"fileTypes","{}","set fileTypes to empty array",true},
//            {"fileTypes",null,"set fileTypes to null",true},
//            {"fileTypes",arbitraryArray,"set fileTypes to arbitrary values",true},
//    };
